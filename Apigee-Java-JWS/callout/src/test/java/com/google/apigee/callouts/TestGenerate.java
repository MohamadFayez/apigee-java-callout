// TestGenerate.java
//
// Copyright (c) 2018-2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// @author: Dino Chiesa
//
// Note:
// If you use the Oracle JDK to run tests, this test, which does
// 256-bit crypto, requires the Unlimited Strength JCE.
//
// Without it, you may get an exception while running this test:
//
// java.security.InvalidKeyException: Illegal key size
//         at javax.crypto.Cipher.checkCryptoPerm(Cipher.java:1039)
//         ....
//
// See http://stackoverflow.com/a/6481658/48082
//
// If you use OpenJDK to run the tests, then it's not an issue.
// In that JDK, there's no restriction on key strength.
//

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestGenerate extends CalloutTestBase {

  @Test()
  public void RS256() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "RS256");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "RS256");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertTrue(parts[1].length() > 14);
  }

  @Test()
  public void RS256_b64_true() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "RS256_b64_true");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "RS256");
    properties.put("debug", "true");
    properties.put("b64", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);

    // parse and examine header
    byte[] b = Base64.getUrlDecoder().decode(parts[0]);
    String hdr = new String(b, StandardCharsets.UTF_8);
    Gson gson = new Gson();
    Map<String, Object> map = gson.fromJson(hdr, Map.class);
    Assert.assertNotNull(map.get("hdr1"));
    Assert.assertNotNull(map.get("cty"));
    Assert.assertNull(map.get("b64"));
    Assert.assertNotNull(map.get("alg"));
    Assert.assertNull(map.get("crit"));
  }

  @Test()
  public void PS256() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "PS256");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "PS256");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
  }

  @Test()
  public void sign_detach() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "sign_detach");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "RS256");
    properties.put("detach", "true");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertEquals(parts[1], "");
  }

  @Test()
  public void noEncode() {
      String payload =  "eyJpc3MiOiJEaW5vQ2hpZXNhLmdpdGh1Yi5pbyIsInN1YiI6ImFubmEiLCJhdWQiOiJuYXRhbGlhIiwiaWF0IjoxNjU2NjMzNjU0LCJleHAiOjE2NTY2MzcyNTQsInByb3BYIjpbdHJ1ZSwid2pyMHVkNW52eW8zemVsYXdiemsiXX0";
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "sign_noEncode");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "RS256");
    properties.put("b64", "false");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", payload);

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertEquals(parts[1], payload);

    // parse and examine header
    byte[] b = Base64.getUrlDecoder().decode(parts[0]);
    String hdr = new String(b, StandardCharsets.UTF_8);
    Gson gson = new Gson();
    Map<String, Object> map = gson.fromJson(hdr, Map.class);
    Assert.assertNotNull(map.get("hdr1"));
    Assert.assertNotNull(map.get("cty"));
    Assert.assertNotNull(map.get("b64"));
    Assert.assertNotNull(map.get("alg"));
    Assert.assertNotNull(map.get("crit"));
  }

  @Test()
  public void sign_detach_noEncode() {
      String payload =  "eyJpc3MiOiJEaW5vQ2hpZXNhLmdpdGh1Yi5pbyIsInN1YiI6ImFubmEiLCJhdWQiOiJuYXRhbGlhIiwiaWF0IjoxNjU2NjMzNjU0LCJleHAiOjE2NTY2MzcyNTQsInByb3BYIjpbdHJ1ZSwid2pyMHVkNW52eW8zemVsYXdiemsiXX0";
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "sign_detach_noEncode");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key1");
    properties.put("algorithm", "RS256");
    properties.put("b64", "false");
    properties.put("detach", "true");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", payload);

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertEquals(parts[1], "");
  }

  @Test()
  public void ES256() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "ES256");
    properties.put("private-key", privateKey2);
    properties.put("key-id", "key2");
    properties.put("algorithm", "ES256");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertTrue(parts[1].length() > 14);
  }

  @Test()
  public void criticalHeaders() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "criticalHeaders");
    properties.put("private-key", privateKey1);
    properties.put("key-id", "key2");
    properties.put("algorithm", "PS256");
    properties.put("debug", "true");
    properties.put("output", "output");
    properties.put(
        "additional-headers", "{\"hdr1\":123, \"cty\": \"text/plain\", \"typ\": \"JOSE\"}");
    properties.put("critical-headers", "hdr1,typ");
    properties.put("payload", "hello world");

    GenerateJws callout = new GenerateJws(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    // check result and output
    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    // retrieve output
    String error = msgCtxt.getVariable("jws_error");
    Assert.assertNull(error);

    // parse serialized JWS
    String jws = msgCtxt.getVariable("output");
    String[] parts = jws.split("\\.");
    Assert.assertEquals(parts.length, 3);
    Assert.assertTrue(parts[1].length() > 14);

    // parse and examine header
    byte[] b = Base64.getUrlDecoder().decode(parts[0]);
    String hdr = new String(b, StandardCharsets.UTF_8);
    Gson gson = new Gson();
    Map<String, Object> map = gson.fromJson(hdr, Map.class);
    Assert.assertNotNull(map.get("hdr1"));
    Assert.assertNotNull(map.get("cty"));
    Assert.assertNull(map.get("b64"));
    Assert.assertNotNull(map.get("alg"));
    Assert.assertNotNull(map.get("crit"));

    List list = (List) map.get("crit");
    Assert.assertEquals(list.size(), 2);
    Assert.assertTrue(list.contains("hdr1"));
    Assert.assertTrue(list.contains("typ"));
  }

}
