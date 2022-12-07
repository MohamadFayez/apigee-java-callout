// GenerateJws.java
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

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.util.KeyUtil;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import java.net.URI;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateJws extends SignedJoseBase implements Execution {

  public GenerateJws(Map properties) {
    super(properties);
  }

  static class PolicyConfig {
    public boolean debug;
    public JWSAlgorithm algorithm;
    public String key;
    public String keyId;
    public String keyEncoding;
    public String additionalHeaders;
    public String payload;
    public boolean isDetach;
    public boolean isBase64UrlEncodeContent;
    public Set<String> criticalHeaders;
    public String outputVar;
  }

  PolicyConfig getPolicyConfig(MessageContext msgCtxt) throws Exception {
    PolicyConfig config = new PolicyConfig();
    config.algorithm = getAlgorithm(msgCtxt);
    config.keyId = _getOptionalString(msgCtxt, "key-id");
    config.payload = _getOptionalString(msgCtxt, "payload");
    config.additionalHeaders = _getOptionalString(msgCtxt, "additional-headers");
    config.criticalHeaders = getCriticalHeaders(msgCtxt);
    config.isDetach = _getBooleanProperty(msgCtxt, "detach", false);
    config.isBase64UrlEncodeContent = _getBooleanProperty(msgCtxt, "b64", true);
    config.outputVar = _getStringProp(msgCtxt, "output", varName("output"));
    return config;
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    boolean debug = true;
    try {
      debug = _getBooleanProperty(msgCtxt, "debug", false);
      clearVariables(msgCtxt);
      PolicyConfig policyConfig = getPolicyConfig(msgCtxt);
      policyConfig.debug = debug;
      sign(policyConfig, msgCtxt);
    } catch (Exception e) {
      if (debug) {
        msgCtxt.setVariable(varName("stacktrace"), getStackTraceAsString(e));
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
    return ExecutionResult.SUCCESS;
  }

  PrivateKey getPrivateKey(MessageContext msgCtxt) throws Exception {
    return KeyUtil.decodePrivateKey(
        _getRequiredString(msgCtxt, "private-key"),
        _getOptionalString(msgCtxt, "private-key-password"));
  }

  JWSSigner getSigner(PolicyConfig policyConfig, MessageContext msgCtxt) throws Exception {
    JWSAlgorithm jwsAlg = policyConfig.algorithm;
    boolean RSA = isRSA(jwsAlg);
    boolean ECDSA = !RSA && isECDSA(jwsAlg);
    boolean HMAC = !RSA && !ECDSA && isHMAC(jwsAlg);

    if (RSA || ECDSA) {
      PrivateKey privateKey = getPrivateKey(msgCtxt);
      return (RSA)
          ? new RSASSASigner((RSAPrivateKey) privateKey)
          : new ECDSASigner((ECPrivateKey) privateKey);
    }

    if (HMAC) {
      String key = _getRequiredString(msgCtxt, "secret-key");
      String keyEncoding = _getOptionalString(msgCtxt, "secret-key-encoding");
      byte[] keyBytes = KeyUtil.decodeSecretKey(key, keyEncoding);
      // NB: this will throw if the string is not at least 16 chars long
      return new MACSigner(keyBytes);
    }

    throw new IllegalStateException("Unsupported algorithm.");
  }

  void sign(PolicyConfig policyConfig, MessageContext msgCtxt) throws Exception {

    JWSSigner signer = getSigner(policyConfig, msgCtxt);
    JWSHeader.Builder headerBuilder = new JWSHeader.Builder(policyConfig.algorithm);
    String alg = policyConfig.algorithm.toString();
    msgCtxt.setVariable(varName("alg"), alg);
    Set<String> crit = new HashSet<String>();
    List<String> warnings = new ArrayList<String>();

    if (policyConfig.additionalHeaders != null) {
      JSONObjectUtils.parse(policyConfig.additionalHeaders)
          .forEach(
              (key, value) -> {
                switch (key) {
                  case "b64":
                  case "alg":
                  case "crit":
                    warnings.add(String.format("do not specify %s in additional headers; ignoring it"));
                    break;
                  case "x5t":
                  case "x5t256":
                  case "x5u":
                  case "jwk":
                  case "x5c":
                    warnings.add(String.format("support for header %s is not implemented yet"));
                    break;
                  case "kid":
                    headerBuilder.keyID(value.toString());
                    break;
                  case "jku":
                    try {
                      headerBuilder.jwkURL(new URI(value.toString()));
                    } catch (Exception e) {
                      warnings.add("invalid jku");
                    }
                    break;
                  case "typ":
                    headerBuilder.type(new JOSEObjectType(value.toString()));
                    break;
                  case "cty":
                    headerBuilder.contentType(value.toString());
                    break;
                  default:
                    headerBuilder.customParam(key, value);
                    break;
                }
              });
    }

    if (!policyConfig.isBase64UrlEncodeContent) {
      headerBuilder.base64URLEncodePayload(false);
      crit.add("b64");
    }

    crit.addAll(policyConfig.criticalHeaders);

    if (crit.size() > 0) {
      headerBuilder.criticalParams(crit);
    }

    JWSObject jwsObject = new JWSObject(headerBuilder.build(), new Payload(policyConfig.payload));

    jwsObject.sign(signer);

    String serialized = jwsObject.serialize(policyConfig.isDetach);
    msgCtxt.setVariable(policyConfig.outputVar, serialized);
    if (warnings.size() > 0) {
      msgCtxt.setVariable(varName("warnings"), warnings.stream().collect(Collectors.joining(", ")));
    }
  }
}
