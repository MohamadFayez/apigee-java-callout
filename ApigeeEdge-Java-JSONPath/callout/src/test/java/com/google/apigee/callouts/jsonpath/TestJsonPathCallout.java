// Copyright 2019-2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts.jsonpath;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.apigee.util.CalloutUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestJsonPathCallout {
  private static final String testDataDir = "src/test/resources/test-data";

  MessageContext msgCtxt;
  String messageContent;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void beforeMethod() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String,Object> variables;

          public void $init() {
            variables = new HashMap<String,Object>();
          }

          @Mock()
          @SuppressWarnings("unchecked")
          public <T> T getVariable(final String name) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            T value = (T) variables.get(name);
            System.out.printf("getVariable(%s) ==> %s\n", name, (value!=null)?value.toString():"null");
            return value;
          }

          @Mock()
          @SuppressWarnings("unchecked")
          public boolean setVariable(final String name, final Object value) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            System.out.printf("setVariable(%s, %s)\n", name, value.toString());
            variables.put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            if (variables.containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }

          @Mock()
          public Message getMessage() {
            return message;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          @Mock()
          public String getContent() {
            return messageContent;
          }
          // @Mock()
          // public InputStream getContentAsStream() {
          //   // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
          //   return messageContentStream;
          // }
        }.getMockInstance();
  }

  @DataProvider(name = "batch1")
  public static Object[][] getDataForBatch1() throws IOException, IllegalStateException {

    // @DataProvider requires the output to be a Object[][]. The inner
    // Object[] is the set of params that get passed to the test method.
    // So, if you want to pass just one param to the constructor, then
    // each inner Object[] must have length 1.

    ObjectMapper om = new ObjectMapper();
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Path currentRelativePath = Paths.get("");
    // String s = currentRelativePath.toAbsolutePath().toString();
    // System.out.println("Current relative path is: " + s);

    // read in all the *.json files in the test-data directory
    File testDir = new File(testDataDir);
    if (!testDir.exists()) {
      throw new IllegalStateException("no test directory.");
    }
    File[] files = testDir.listFiles();
    if (files.length == 0) {
      throw new IllegalStateException("no tests found.");
    }
    Arrays.sort(files);
    int c = 0;
    ArrayList<TestCase> list = new ArrayList<TestCase>();
    for (File file : files) {
      String name = file.getName();
      if (name.matches("^[0-9]+.+.json$")) {
        TestCase tc = om.readValue(file, TestCase.class);
        tc.setTestName(name.substring(0, name.length() - 5));
        list.add(tc);
      }
    }
    int n = list.size();
    Object[][] data = new Object[n][];
    for (int i = 0; i < data.length; i++) {
      data[i] = new Object[] {list.get(i)};
    }
    return data;
  }

  @Test
  public void testDataProviders() throws IOException {
    Assert.assertTrue(getDataForBatch1().length > 0);
  }

  @Test(dataProvider = "batch1")
  public void test2_Configs(TestCase tc) throws Exception {
    if (tc.getDescription() != null)
      System.out.printf("  %10s - %s\n", tc.getTestName(), tc.getDescription());
    else System.out.printf("  %10s\n", tc.getTestName());

    // set variables into message context
    for (Map.Entry<String, String> entry : tc.getContext().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      msgCtxt.setVariable(key, value);
    }
    messageContent = tc.getSourceAsString();
    msgCtxt.setVariable("message", message);
    // msgCtxt.setVariable("message.content", messageContent);

    JsonPathCallout callout = new JsonPathCallout(tc.getProperties());

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);

    String s = (String) (tc.getExpected().get("success"));
    ExecutionResult expectedResult =
        (s != null && s.toLowerCase().equals("true"))
            ? ExecutionResult.SUCCESS
            : ExecutionResult.ABORT;
    // check result and output
    if (expectedResult == actualResult) {
      if (expectedResult == ExecutionResult.SUCCESS) {
        String expectedOutput = CalloutUtil.asJSONString(tc.getExpected().get("output"));
        String actualOutput = (String) (msgCtxt.getVariable("json.output"));

        if (!expectedOutput.equals(actualOutput)) {
          System.err.printf("    got     : %s\n", actualOutput);
          System.err.printf("    expected: %s\n", expectedOutput);
        }
        Assert.assertEquals(actualOutput, expectedOutput, tc.getTestName() + " output");

      } else {
        String expectedError = (String) (tc.getExpected().get("error"));
        Assert.assertNotNull(expectedError, "broken test: no expected error specified");
        String actualError = msgCtxt.getVariable("json.error");
        Assert.assertEquals(actualError, expectedError, tc.getTestName() + " error");
      }
    } else {
      // String observedError = msgCtxt.getVariable("json_error");
      Assert.assertEquals(actualResult, expectedResult, "result not as expected");
    }
    System.out.println("=========================================================");
  }
}
