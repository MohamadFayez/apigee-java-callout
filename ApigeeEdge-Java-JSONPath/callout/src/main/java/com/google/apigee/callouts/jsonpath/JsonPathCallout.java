// JsonPathCallout.java
//
// A callout for Apigee Edge that performs a JsonPath query.
//
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
//
// Example configuration
//
// <JavaCallout name='Java-JsonPath'>
//   <Properties>
//     <!-- specify the XSLT itself in one of these ways -->
//     <Property name='xslt'>file://xslt-filename.xsl</Property> <!-- resource in jar -->
//     <Property name='xslt'>http://hostname/url-returning-an-xslt</Property>
//     <Property name='xslt'>immediate-string-containing-xslt</Property>
//     <Property name='xslt'>{variable-containing-one-of-the-above}</Property>
//
//     <!-- source for the transform.  If of type Message, then use x.content -->
//     <Property name='input'>name-of-variable-containing-message-or-string</Property>
//
//     <!-- where to put the transformed data. If none, put in message.content -->
//     <Property name='output'>name-of-variable-to-hold-output</Property>
//
//     <!-- arbitrary params to pass to the XSLT -->
//     <Property name='param_x'>string value of param</Property>
//     <Property name='param_y'>{variable-containing-value-of-param}</Property>
//     <Property name='param_z'>file://something.xsd</Property> <!-- resource in jar -->
//   </Properties>
//   <ClassName>com.dinochiesa.xslt.XsltCallout</ClassName>
//   <ResourceURL>java://edgecallout-xslt.jar</ResourceURL>
// </JavaCallout>
//
// ----------------------------------------------------------
//
// This software is licensed under the Apache Source license 2.0.
// See the accompanying LICENSE file.
//
//

package com.google.apigee.callouts.jsonpath;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.IOIntensive;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.callouts.CalloutBase;
import com.google.apigee.util.CalloutUtil;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.util.Map;

@IOIntensive
public class JsonPathCallout extends CalloutBase implements Execution {
  // The default cap on the number of "sleeping" instances in the pool.
  private static final String varPrefix = "json.";

  public JsonPathCallout(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varPrefix;
  }

  private String getSource() {
    String source = (String) this.properties.get("source");
    if (source == null || source.equals("")) {
      return "message";
    }
    return source; // should be the name of a message
  }

  private String getOutput(MessageContext msgCtxt) throws Exception {
    String output = (String) getSimpleOptionalProperty("output", msgCtxt);
    if (output == null) {
      return varName("output");
    }
    return output; // a variable name
  }

  protected boolean getWantList() {
    String wantList = (String) this.properties.get("want-list");
    return (wantList != null) && Boolean.parseBoolean(wantList);
  }
  protected boolean getReturnFirstElement() {
    String wantFirst = (String) this.properties.get("return-first-element");
    return (wantFirst != null) && Boolean.parseBoolean(wantFirst);
  }

  private String getQuery(MessageContext msgCtxt) throws Exception {
    String query = (String) getSimpleRequiredProperty("jsonpath", msgCtxt);
    return query;
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    ExecutionResult calloutResult = ExecutionResult.ABORT;
    Boolean isValid = false;
    boolean debug = getDebug();

    try {
      clearVariables(msgCtxt);
      ConfigurationBuilder configBuilder = Configuration.builder();

      if (getWantList()) configBuilder.options(Option.ALWAYS_RETURN_LIST);

      // Maybe TODO? : optionally return path list
      // .options(Option.AS_PATH_LIST)

      Configuration config = configBuilder.build();
      String messageContent = null;
      Object untypedSource = msgCtxt.getVariable(getSource());
      if (untypedSource.getClass().getName().equals("java.lang.String")) {
        messageContent = (String) untypedSource;
      }
      else if (untypedSource instanceof Message) {
        Message source = (Message) untypedSource;
        messageContent = source.getContent();
      }

      String query = getQuery(msgCtxt);
      JsonPath jsonPath = JsonPath.compile(query);
      Object result = jsonPath.read(messageContent, config);
      if (result instanceof net.minidev.json.JSONArray) {
        net.minidev.json.JSONArray jsonArray = (net.minidev.json.JSONArray) result;
        if (getReturnFirstElement()){
          result = CalloutUtil.asJSONString(jsonArray.get(0));
        }
        else {
          result = CalloutUtil.asJSONString(jsonArray);
        }
      } else if (!(result instanceof String)
          && (result instanceof Long
              || result instanceof Double
              || result instanceof Float
              || result instanceof Integer
              || result instanceof Boolean)) {
        result = String.valueOf(result);
      }
      msgCtxt.setVariable(getOutput(msgCtxt), result);
    } catch (Exception e) {
      if (debug) {
        // e.printStackTrace();
        String stacktrace = CalloutUtil.getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    } finally {
    }

    return ExecutionResult.SUCCESS;
  }
}
