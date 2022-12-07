// VerifyJws.java
//
// handles verifying JWE, which are not treated as JWT.
// For full details see the Readme accompanying this source file.
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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VerifyJws extends SignedJoseBase implements Execution {

  public VerifyJws(Map properties) {
    super(properties);
  }

  static class PolicyConfig {
    public boolean debug;
    public JWSAlgorithm algorithm;
    public String jwksUri;
    public String key;
    public String keyEncoding;
    public Set<String> deferredCritHeaders;
    public String source;
    public String detachedPayload;
  }

  PolicyConfig getPolicyConfiguration(MessageContext msgCtxt) throws Exception {
    PolicyConfig config = new PolicyConfig();
    config.algorithm = getAlgorithm(msgCtxt);
    config.deferredCritHeaders = getCriticalHeaders(msgCtxt);
    config.source = _getRequiredString(msgCtxt, "source");
    config.jwksUri = _getOptionalString(msgCtxt, "jwks-uri");
    if (config.jwksUri == null) {
      config.key = _getOptionalString(msgCtxt, "key");
      config.keyEncoding = _getOptionalString(msgCtxt, "key-encoding");
    }
    config.detachedPayload = _getOptionalString(msgCtxt, "detached-payload");
    return config;
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    boolean debug = true;
    try {
      debug = _getBooleanProperty(msgCtxt, "debug", false);
      clearVariables(msgCtxt);
      PolicyConfig policyConfig = getPolicyConfiguration(msgCtxt);
      policyConfig.debug = debug;
      return verify(policyConfig, msgCtxt);
    } catch (Exception e) {
      if (debug) {
        // e.printStackTrace();
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
  }

  JWSVerifier getVerifier(JWSHeader jwsHeader, String jwksUri, String key, String keyEncoding)
      throws Exception {

    JWSAlgorithm jwsAlg = jwsHeader.getAlgorithm();
    boolean RSA = isRSA(jwsAlg);
    boolean ECDSA = !RSA && isECDSA(jwsAlg);
    boolean HMAC = !RSA && !ECDSA && isHMAC(jwsAlg);
    KeyType keytype = null;
    JWKSet jwkset = null;
    String keyId = jwsHeader.getKeyID();
    if (jwksUri != null) {
      jwkset = jwksRemoteCache.get(jwksUri);
      if (keyId == null || keyId.equals("")) {
        throw new IllegalStateException("the JWS header lacks a keyId. Cannot verify with a JWKS.");
      }
      keytype = (RSA) ? KeyType.RSA : (ECDSA) ? KeyType.EC : null;
    }

    if (RSA || ECDSA) {
      if (jwkset != null) {
        JWKMatcher matcher = new JWKMatcher.Builder().keyType(keytype).keyID(keyId).build();

        List<JWK> filtered = new JWKSelector(matcher).select(jwkset);

        if (filtered.size() == 0) {
          throw new IllegalStateException(
              String.format("a key with kid '%s' was not found.", keyId));
        }
        if (filtered.size() != 1) {
          throw new IllegalStateException(
              String.format("more than one key with kid '%s' found.", keyId));
        }
        JWK jwk = filtered.get(0);
        if (RSA) {
          PublicKey publicKey = ((RSAKey) jwk).toPublicKey();
          return new RSASSAVerifier((RSAPublicKey) publicKey, jwsHeader.getCriticalParams());
        }
        PublicKey publicKey = ((ECKey) jwk).toPublicKey();
        return new ECDSAVerifier((ECPublicKey) publicKey, jwsHeader.getCriticalParams());
      }

      if (key != null) {
        if (RSA) {
          RSAPublicKey publicKey = (RSAPublicKey) KeyUtil.decodePublicKey(key);
          return new RSASSAVerifier(publicKey, jwsHeader.getCriticalParams());
        }
        ECPublicKey publicKey = (ECPublicKey) KeyUtil.decodePublicKey(key);
        return new ECDSAVerifier(publicKey, jwsHeader.getCriticalParams());
      }
      throw new IllegalStateException("Neither jwks-uri nor key has been supplied.");
    }

    if (HMAC) {
      if (key == null) {
        throw new IllegalStateException("No key has been supplied.");
      }
      byte[] keyBytes = KeyUtil.decodeSecretKey(key, keyEncoding);
      // NB: this will throw if the string is not at least 16 chars long
      return new MACVerifier(keyBytes, jwsHeader.getCriticalParams());
    }

    throw new IllegalStateException("Unsupported algorithm.");
  }

  ExecutionResult verify(PolicyConfig policyConfig, MessageContext msgCtxt) throws Exception {
    Object v = msgCtxt.getVariable(policyConfig.source);
    if (v == null) throw new IllegalStateException("Cannot find JWS within source.");
    String jwsText = (String) v;
    JWSObject jwsObject =
        (policyConfig.detachedPayload != null)
            ? JWSObject.parse(jwsText, new Payload(policyConfig.detachedPayload))
            : JWSObject.parse(jwsText);
    JWSHeader header = jwsObject.getHeader();
    msgCtxt.setVariable(varName("header"), header.toString());
    setVariables(header.toJSONObject(), msgCtxt);

    // verify configured Alg matches actual alg
    if (!header.getAlgorithm().toString().equals(policyConfig.algorithm.toString())) {
      throw new IllegalStateException("JWS uses unacceptable Algorithm.");
    }

    JWSVerifier verifier =
        getVerifier(header, policyConfig.jwksUri, policyConfig.key, policyConfig.keyEncoding);
    boolean verified = jwsObject.verify(verifier);
    if (!verified) {
      msgCtxt.setVariable(varName("error"), "Signature does not verify");
      return ExecutionResult.ABORT;
    }
    return ExecutionResult.SUCCESS;
  }
}
