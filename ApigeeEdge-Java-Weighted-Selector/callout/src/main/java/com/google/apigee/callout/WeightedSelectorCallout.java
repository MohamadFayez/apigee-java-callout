// WeightedSelectorCallout.java
//
// This is the source code for a Java callout for Apigee Edge.
//
// Copyright 2018-2022 Google LLC.
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
// ------------------------------------------------------------------

package com.google.apigee.callout;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeightedSelectorCallout implements Execution {
  private static final String varprefix = "wrs_";
  private LoadingCache<String, WeightedRandomSelector> selectorCache;

  private static String varName(String s) {
    return varprefix + s;
  }

  private static final Splitter splitter =
      Splitter.on(CharMatcher.anyOf(" ,")).trimResults().omitEmptyStrings();
  private static final String variableReferencePatternString = "(.*?)\\{([^\\{\\} ]+?)\\}(.*?)";
  private static final Pattern variableReferencePattern =
      Pattern.compile(variableReferencePatternString);
  private Map properties;

  public WeightedSelectorCallout(Map properties) {
    this.properties = properties;
    this.selectorCache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            // .weakKeys()
            .maximumSize(1048000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, WeightedRandomSelector>() {
                  public WeightedRandomSelector load(String weightsConfig) {
                    Iterable<Integer> weights =
                        Iterables.transform(splitter.split(weightsConfig), s -> Integer.valueOf(s));
                    return new WeightedRandomSelector(weights);
                  }
                });
  }

  private String getWeights(MessageContext msgCtxt) throws Exception {
    String weights = (String) this.properties.get("weights");
    if (weights == null || weights.equals("")) {
      throw new IllegalStateException("weights is not specified or is empty.");
    }
    weights = (String) resolvePropertyValue(weights, msgCtxt);
    if (weights == null || weights.equals("")) {
      throw new IllegalStateException("weights is null or empty.");
    }
    return weights;
  }

  private boolean getDebug() {
    String wantDebug = (String) this.properties.get("debug");
    boolean debug = (wantDebug != null) && Boolean.parseBoolean(wantDebug);
    return debug;
  }

  // If the value of a property contains a pair of curlies,
  // eg, {apiproxy.name}, then "resolve" the value by de-referencing
  // the context variable whose name appears between the curlies.
  private String resolvePropertyValue(String spec, MessageContext msgCtxt) {
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      Object v = msgCtxt.getVariable(matcher.group(2));
      if (v != null) {
        sb.append((String) v);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  protected static String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String weightsConfig = getWeights(msgCtxt);
      WeightedRandomSelector wrs = selectorCache.get(weightsConfig);
      // set a variable.
      msgCtxt.setVariable(varName("bucket"), Integer.toString(wrs.select()));
    } catch (Exception e) {
      if (getDebug()) {
        System.out.println(getStackTraceAsString(e));
      }
      String error = e.toString();
      msgCtxt.setVariable(varName("exception"), error);
      int ch = error.lastIndexOf(':');
      if (ch >= 0) {
        msgCtxt.setVariable(varName("error"), error.substring(ch + 2).trim());
      } else {
        msgCtxt.setVariable(varName("error"), error);
      }
      msgCtxt.setVariable(varName("stacktrace"), getStackTraceAsString(e));
      return ExecutionResult.ABORT;
    }

    return ExecutionResult.SUCCESS;
  }
}
