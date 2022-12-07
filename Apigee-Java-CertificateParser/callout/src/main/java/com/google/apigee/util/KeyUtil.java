// Copyright 2018-2020 Google LLC.
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
package com.google.apigee.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class KeyUtil {

  private KeyUtil() {} // uncomment if wanted

  private static String reformIndents(String s) {
    return s.trim().replaceAll("([\\r|\\n] +)", "\n");
  }

  public static Certificate parseCertificate(String certificateString)
      throws IllegalArgumentException {
    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
      certificateString = reformIndents(certificateString);
      Certificate certificate =
          certFactory.generateCertificate(
              new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8)));
      return certificate;
    } catch (Exception ex) {
      throw new IllegalArgumentException("cannot instantiate certificate from string", ex);
    }
  }

  public static String toPem(final PublicKey publicKey) throws IOException {
    final StringWriter sw = new StringWriter();
    try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
      jpw.writeObject(publicKey);
    }
    return sw.toString();
  }

  public static String toPem(final Certificate cert) throws IOException {
    final StringWriter sw = new StringWriter();
    try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
      jpw.writeObject(cert);
    }
    return sw.toString();
  }
}
