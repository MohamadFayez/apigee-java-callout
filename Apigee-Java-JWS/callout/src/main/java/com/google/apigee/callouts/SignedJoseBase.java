// SignedJoseBase.java
//
// Copyright (c) 2018-2022 Google LLC.
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

package com.google.apigee.callouts;

import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class SignedJoseBase extends AbstractCallout implements Execution {
  protected static LoadingCache<String, JWKSet> jwksRemoteCache;
  private static final int MAX_CACHE_ENTRIES = 128;
  private static final int CACHE_EXPIRY_IN_MINUTES = 5;

  String getVarPrefix() {
    return "jws_";
  }

  static {
    jwksRemoteCache =
        Caffeine.newBuilder()
            // .concurrencyLevel(4)
            .maximumSize(MAX_CACHE_ENTRIES)
            .expireAfterAccess(CACHE_EXPIRY_IN_MINUTES, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, JWKSet>() {
                  public JWKSet load(String uri)
                      throws MalformedURLException, IOException, ParseException {
                    // NB: this will throw an IOException on HTTP error.
                    return JWKSet.load(new URL(uri));
                  }
                });
  }

  public SignedJoseBase(Map properties) {
    super(properties);
  }

  protected String getOutputVar(MessageContext msgCtxt) throws Exception {
    return _getStringProp(msgCtxt, "output", varName("output"));
  }

  protected JWSAlgorithm getAlgorithm(MessageContext msgCtxt) throws Exception {
    String alg = _getRequiredString(msgCtxt, "algorithm");
    alg = resolveVariableReferences(alg.trim(), msgCtxt);
    if (alg == null || alg.equals("")) {
      throw new IllegalStateException("algorithm resolves to null or empty.");
    }
    alg = alg.toUpperCase();
    try {
      return JWSAlgorithm.parse(alg);
    } catch (Exception e) {
      throw new IllegalStateException("that algorithm name is unsupported.", e);
    }
  }

  protected Set<String> getCriticalHeaders(MessageContext msgCtxt) throws Exception {
    String critHeaders = _getStringProp(msgCtxt, "critical-headers", null);
    if (critHeaders == null) return new HashSet<String>(); // empty set

    return new HashSet<String>(Arrays.asList(critHeaders.split("\\s*,\\s*")));
  }

  boolean isRSA(JWSAlgorithm jwsAlgorithm) {
    return jwsAlgorithm.toString().startsWith("RS") || jwsAlgorithm.toString().startsWith("PS");
  }

  boolean isHMAC(JWSAlgorithm jwsAlgorithm) {
    return jwsAlgorithm.toString().startsWith("HS");
  }

  boolean isECDSA(JWSAlgorithm jwsAlgorithm) {
    return jwsAlgorithm.toString().startsWith("ES");
  }
}
