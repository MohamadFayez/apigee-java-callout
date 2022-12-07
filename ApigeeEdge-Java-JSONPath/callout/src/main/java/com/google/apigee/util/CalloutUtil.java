// CalloutUtil.java
//
// This is a utility class for custom policies in Apigee Edge.
// For full details see the Readme accompanying this source file.
//
// Copyright (c) 2018-2019 Google LLC
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

package com.google.apigee.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public final class CalloutUtil {

  public static Map<String, String> genericizeMap(Map properties) {
    // convert an untyped Map to a generic map
    Map<String, String> m = new HashMap<String, String>();
    Iterator iterator = properties.keySet().iterator();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      Object value = properties.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        m.put((String) key, (String) value);
      }
    }
    return Collections.unmodifiableMap(m);
  }

  public static String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  @SuppressWarnings("unchecked")
  public static String asJSONString(Object obj) throws Exception {
    // System.out.printf(
    //     "asJSONString type(%s) toString(%s)\n", obj.getClass().getName(), obj.toString());
    if (obj.getClass().getName().equals("java.util.ArrayList")) {
      return JSONArray.toJSONString((java.util.ArrayList<Object>) obj);
    }
    if (obj.getClass().getName().equals("java.util.HashMap")) {
      return JSONObject.toJSONString((java.util.HashMap<String,Object>) obj);
    }
    if (obj.getClass().getName().equals("java.util.LinkedHashMap")) {
      return JSONObject.toJSONString((java.util.LinkedHashMap<String,Object>) obj);
    }
    return obj.toString();
  }
}
