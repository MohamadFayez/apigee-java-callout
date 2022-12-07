// TestCertificateParseCallout.java
//
// Copyright (c) 2018-2020 Google LLC
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

package com.google.apigee.callout;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.MessageContext;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestCertificateParseCallout {

  static {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  MessageContext msgCtxt;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void testSetup1() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String, Object> variables;

          public void $init() {
            getVariables();
          }

          private Map<String, Object> getVariables() {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            return variables;
          }

          @Mock()
          public Object getVariable(final String name) {
            return getVariables().get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            System.out.printf("set(%s) = %s\n", name, value.toString());
            getVariables().put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (getVariables().containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();
    System.out.printf("=============================================\n");
  }

  private static final String cert1 =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIDsjCCApoCCQD0UfCZpOe2+jANBgkqhkiG9w0BAQUFADCBmjELMAkGA1UEBhMC\n"
          + "VVMxEzARBgNVBAgMCldhc2hpbmd0b24xETAPBgNVBAcMCEtpcmtsYW5kMQ8wDQYD\n"
          + "VQQKDAZHb29nbGUxDjAMBgNVBAsMBUNsb3VkMR8wHQYDVQQDDBZjbGllbnQuYXBp\n"
          + "Z2VlLWRlbW8ubmV0MSEwHwYJKoZIhvcNAQkBFhJkY2hpZXNhQGdvb2dsZS5jb20w\n"
          + "HhcNMTgwNjE0MjIyNTAwWhcNMTkwNjE0MjIyNTAwWjCBmjELMAkGA1UEBhMCVVMx\n"
          + "EzARBgNVBAgMCldhc2hpbmd0b24xETAPBgNVBAcMCEtpcmtsYW5kMQ8wDQYDVQQK\n"
          + "DAZHb29nbGUxDjAMBgNVBAsMBUNsb3VkMR8wHQYDVQQDDBZjbGllbnQuYXBpZ2Vl\n"
          + "LWRlbW8ubmV0MSEwHwYJKoZIhvcNAQkBFhJkY2hpZXNhQGdvb2dsZS5jb20wggEi\n"
          + "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDBwZiZ9g+OWLhq7IUNIM0Wys/J\n"
          + "laMjPcSf4dm5nJHFc90yzYA/AjjevKIpNfmzq7J2wKssNkbPCV691ZZB7zEIHuRA\n"
          + "OLG+024sESnxHgoZ3zK3CsXnmiyfha6Be1ESDagFRGCD1elTybnRvoSpbK2/j80j\n"
          + "URLSLJdeayPgrdCO2/MPfcTC/eknkOHxYalydCVVAhv8uvbeE6nnKQ9H+oiRBXfE\n"
          + "yDeoeK2Bqu7Gbny64jN6a70zxR3JkxLSGASdBDpZAnZRHq1nFWLhSPPsrp91RL4x\n"
          + "zOvXsTtZeopPoeZtAoWOF/VWVWH0rE32pmFpjOmx1vEl8OUTxxOml7LsAHtJAgMB\n"
          + "AAEwDQYJKoZIhvcNAQEFBQADggEBAK7vaW2hBwMfBYgJbp+pZ6KN7qgT++denhdQ\n"
          + "nUeHMddj3oTQnrCkF3FCltwLj0jDTxevEjvTHuICmwWAmm5iKnh/91ePFUnwItWu\n"
          + "uHwFHg9leKJxcpKMOKpKTvjPiADVPQTRxoP1VGp2juSlqPz843ASqHPOesjM6Q3t\n"
          + "jIfhUdiDkBUQP22YP970IlE2OJ0QqYVNVd27pSKNtnW23TZxxZ12pZT19IKLEIEU\n"
          + "YnJUhi+QmprZptLOHAww+sbjM5izsszse5YsKy9hBeWjXUfn5XVkzgYWi59U/CBz\n"
          + "yaTdG+TE5nQqWLYKoQZY+VsEckKpws3Vp/YxRZUN4P0vrH8KB+I=\n"
          + "-----END CERTIFICATE-----\n";

  private static final String cert2 =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIFZDCCBEygAwIBAgISBGXZVXtrpBzVlq9v5duzrzybMA0GCSqGSIb3DQEBCwUA\n"
          + "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n"
          + "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0yMDEwMTQxMzQ4MzFaFw0y\n"
          + "MTAxMTIxMzQ4MzFaMCAxHjAcBgNVBAMTFTVnLWRldi5kaW5vY2hpZXNhLm5ldDCC\n"
          + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK13u2zVmJHN/kOr81Rki8Kp\n"
          + "4dXZb6tkZtUDsdJNh3rTl8MO2PUhApqzj+HV+omfh3LxlbTYEJL92OFaWxb0A5da\n"
          + "pP34aUeenDFvvlg6TVQ2+YR9KtnP/DM4m3/tuDeYj2TqAit+kN4rIT8vfqBdJU1L\n"
          + "Ns3Q+orpfzfkVcIV2wUbrXkgrwHVz3wjCHVAxZPW9SxecXtLAs4sTKLVZEjHjmvT\n"
          + "zDrbvvU2l/g426l1O3nrwvhbw5T2Y6VfO0stdNUbQO5ZPx5CPdU9Qm3U3fgwt1h3\n"
          + "Rd3DuTbPdkdsXeJcwFg+kDwEmQwaZmn8ERbJ/1I/i96o49h2GJdyvPV2a7Hr0p0C\n"
          + "AwEAAaOCAmwwggJoMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcD\n"
          + "AQYIKwYBBQUHAwIwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQU2/pIusXPsPvB5axm\n"
          + "d1LgyJl+8rUwHwYDVR0jBBgwFoAUqEpqYwR93brm0Tm3pkVl7/Oo7KEwbwYIKwYB\n"
          + "BQUHAQEEYzBhMC4GCCsGAQUFBzABhiJodHRwOi8vb2NzcC5pbnQteDMubGV0c2Vu\n"
          + "Y3J5cHQub3JnMC8GCCsGAQUFBzAChiNodHRwOi8vY2VydC5pbnQteDMubGV0c2Vu\n"
          + "Y3J5cHQub3JnLzAgBgNVHREEGTAXghU1Zy1kZXYuZGlub2NoaWVzYS5uZXQwTAYD\n"
          + "VR0gBEUwQzAIBgZngQwBAgEwNwYLKwYBBAGC3xMBAQEwKDAmBggrBgEFBQcCARYa\n"
          + "aHR0cDovL2Nwcy5sZXRzZW5jcnlwdC5vcmcwggEGBgorBgEEAdZ5AgQCBIH3BIH0\n"
          + "APIAdwD2XJQv0XcwIhRUGAgwlFaO400TGTO/3wwvIAvMTvFk4wAAAXUnlYaTAAAE\n"
          + "AwBIMEYCIQDN3nxymjt6afzNd0kYpgIKpglrHnRjapNO/gtfSo/Y/AIhAPwhWx4a\n"
          + "/CtVhJWyzQRAQJ1KNhPH2VAflKwcpBv9jRfnAHcAb1N2rDHwMRnYmQCkURX/dxUc\n"
          + "EdkCwQApBo2yCJo32RMAAAF1J5WGugAABAMASDBGAiEAuksDHIhdvODHALXielDN\n"
          + "QVzyYM6K0c3K7eOZnGe4gsQCIQCQ/fq7yF5ScOm0rRNv2VWRcsn+l24bzjkrbdft\n"
          + "tiY8UTANBgkqhkiG9w0BAQsFAAOCAQEAZANMxcHo6RShEKZtpG1nPj5tDw4Us0Wl\n"
          + "Jk5QWhf3ZeHg/TP5JNmlyjjjaf7wpuPlDgqIU+NRGEidriCMdHWZVXZaDMAAQAKM\n"
          + "mqPY27poUt7o0ormIUtxy46qDMcFi/mCfGD8FmCE7IsJrhZeBbsI6ZZUuwNmwNr2\n"
          + "qx/ygzOUBW/YLYEeklsDx7vuMcwiZ+/f3Idop3/2LGIcV0GHjgi54ft+VDBIKMYU\n"
          + "7dtl6qKQy5JNf1xodMYCeN5YmHAKTXwpqYsVRx+bE69Q/e3W5xW/x+XNaZxW9Xgv\n"
          + "Yf3d7i2Mxl1PtLvm8pro2Bc7omEqOo2YZ/FRO6geVJk5hP1ixFI8NQ==\n"
          + "-----END CERTIFICATE-----\n";

  private static final String cert3 =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIFSzCCBDOgAwIBAgIQTI4YcUs0516Nrvvo9kw6gjANBgkqhkiG9w0BAQsFADCB\n"
          + "kDELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n"
          + "A1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxNjA0BgNV\n"
          + "BAMTLUNPTU9ETyBSU0EgRG9tYWluIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZlciBD\n"
          + "QTAeFw0xNjA3MDcwMDAwMDBaFw0xNzA5MDUyMzU5NTlaMFkxITAfBgNVBAsTGERv\n"
          + "bWFpbiBDb250cm9sIFZhbGlkYXRlZDEdMBsGA1UECxMUUG9zaXRpdmVTU0wgV2ls\n"
          + "ZGNhcmQxFTATBgNVBAMMDCouYmFkc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEBBQAD\n"
          + "ggEPADCCAQoCggEBAMIE7PiM7gTCs9hQ1XBYzJMY61yoaEmwIrX5lZ6xKyx2PmzA\n"
          + "S2BMTOqytMAPgLaw+XLJhgL5XEFdEyt/ccRLvOmULlA3pmccYYz2QULFRtMWhyef\n"
          + "dOsKnRFSJiFzbIRMeVXk0WvoBj1IFVKtsyjbqv9u/2CVSndrOfEk0TG23U3AxPxT\n"
          + "uW1CrbV8/q71FdIzSOciccfCFHpsKOo3St/qbLVytH5aohbcabFXRNsKEqveww9H\n"
          + "dFxBIuGa+RuT5q0iBikusbpJHAwnnqP7i/dAcgCskgjZjFeEU4EFy+b+a1SYQCeF\n"
          + "xxC7c3DvaRhBB0VVfPlkPz0sw6l865MaTIbRyoUCAwEAAaOCAdUwggHRMB8GA1Ud\n"
          + "IwQYMBaAFJCvajqUWgvYkOoSVnPfQ7Q6KNrnMB0GA1UdDgQWBBSd7sF7gQs6R2lx\n"
          + "GH0RN5O8pRs/+zAOBgNVHQ8BAf8EBAMCBaAwDAYDVR0TAQH/BAIwADAdBgNVHSUE\n"
          + "FjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwTwYDVR0gBEgwRjA6BgsrBgEEAbIxAQIC\n"
          + "BzArMCkGCCsGAQUFBwIBFh1odHRwczovL3NlY3VyZS5jb21vZG8uY29tL0NQUzAI\n"
          + "BgZngQwBAgEwVAYDVR0fBE0wSzBJoEegRYZDaHR0cDovL2NybC5jb21vZG9jYS5j\n"
          + "b20vQ09NT0RPUlNBRG9tYWluVmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNybDCB\n"
          + "hQYIKwYBBQUHAQEEeTB3ME8GCCsGAQUFBzAChkNodHRwOi8vY3J0LmNvbW9kb2Nh\n"
          + "LmNvbS9DT01PRE9SU0FEb21haW5WYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3J0\n"
          + "MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5jb21vZG9jYS5jb20wIwYDVR0RBBww\n"
          + "GoIMKi5iYWRzc2wuY29tggpiYWRzc2wuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQB1\n"
          + "SIOInFUkNzAH6yZoyHkcXK6aApq1UnVErKntWWXQxkcmBI1XiRYucRhImGgc9jH1\n"
          + "JkvogUSx/1xlPXhUlMOGnUiW6DKv4Y+UR743jMPtTZe7xio3cgE6j4KkNETExPhQ\n"
          + "JEieGfDs4cYTRCa2ZeFiSYek9NjEOTx9QsikKlQFoNwK+CsilJN4Tmo2G9Ln6a6E\n"
          + "7RMdofeig4EDTJ4h+7+oMP7rAGixf7pd4l3/QR/W9aZciu+BgMjxUgAXndGWGn1e\n"
          + "0oOzgsI9RoOlHrQ2NTjEei7fC6GYY1gLHtBtgx/xck0JrJYaC+X2NEyrvLyZW4JZ\n"
          + "5mzT25jgzpU7z04Xw+46\n"
          + "-----END CERTIFICATE-----\n";

  private void reportThings(Map<String, String> props) {
    String test = props.get("testname");
    System.out.println("test  : " + test);

    String error = msgCtxt.getVariable("cert_error");
    System.out.println("error : " + error);
  }

  private void check(String propertyName, String expectedValue) {
    String actualValue = msgCtxt.getVariable("cert_" + propertyName);
    Assert.assertEquals(actualValue, expectedValue, propertyName);
  }

  @Test()
  public void parse1() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "parse1");
    properties.put("certificate", cert1);
    properties.put("debug", "true");

    CertificateParseCallout callout = new CertificateParseCallout(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);

    String error = msgCtxt.getVariable("cert_error");
    Assert.assertNull(error);

    check("serial", "f451f099a4e7b6fa");
    check("notAfter", "2019-06-14T22:25:00Z");
    check(
        "issuerDN",
        "C=US,ST=Washington,L=Kirkland,O=Google,OU=Cloud,CN=client.apigee-demo.net,E=dchiesa@google.com");
    check(
        "subjectDN",
        "C=US,ST=Washington,L=Kirkland,O=Google,OU=Cloud,CN=client.apigee-demo.net,E=dchiesa@google.com");
    check("sigAlgName", "SHA1WITHRSA");
  }

  @Test()
  public void parse2() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "parse2");
    properties.put("certificate", cert2);
    properties.put("debug", "true");

    final String publicKey =
        "-----BEGIN PUBLIC KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArXe7bNWYkc3+Q6vzVGSL\n"
            + "wqnh1dlvq2Rm1QOx0k2HetOXww7Y9SECmrOP4dX6iZ+HcvGVtNgQkv3Y4VpbFvQD\n"
            + "l1qk/fhpR56cMW++WDpNVDb5hH0q2c/8Mzibf+24N5iPZOoCK36Q3ishPy9+oF0l\n"
            + "TUs2zdD6iul/N+RVwhXbBRuteSCvAdXPfCMIdUDFk9b1LF5xe0sCzixMotVkSMeO\n"
            + "a9PMOtu+9TaX+DjbqXU7eevC+FvDlPZjpV87Sy101RtA7lk/HkI91T1CbdTd+DC3\n"
            + "WHdF3cO5Ns92R2xd4lzAWD6QPASZDBpmafwRFsn/Uj+L3qjj2HYYl3K89XZrsevS\n"
            + "nQIDAQAB\n"
            + "-----END PUBLIC KEY-----\n";

    CertificateParseCallout callout = new CertificateParseCallout(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    String error = msgCtxt.getVariable("cert_error");
    Assert.assertNull(error);

    check("serial", "465d9557b6ba41cd596af6fe5dbb3af3c9b");
    check("notAfter", "2021-01-12T13:48:31Z");
    check("issuerDN", "C=US,O=Let's Encrypt,CN=Let's Encrypt Authority X3");
    check("subjectDN", "CN=5g-dev.dinochiesa.net");
    check("sigAlgName", "SHA256WITHRSA");
    check("publickey", publicKey);
  }

  @Test()
  public void parse3() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("testname", "parse3");
    properties.put("certificate", cert3);
    properties.put("debug", "true");

    CertificateParseCallout callout = new CertificateParseCallout(properties);
    ExecutionResult result = callout.execute(msgCtxt, exeCtxt);

    reportThings(properties);
    Assert.assertEquals(result, ExecutionResult.SUCCESS);
    String error = msgCtxt.getVariable("cert_error");
    Assert.assertNull(error);

    check("serial", "4c8e18714b34e75e8daefbe8f64c3a82");
    check("notAfter", "2017-09-05T23:59:59Z");
    String secondsRemaining = msgCtxt.getVariable("cert_seconds_remaining");
    Assert.assertTrue(secondsRemaining.startsWith("-"));

    check(
        "issuerDN",
        "C=GB,ST=Greater Manchester,L=Salford,O=COMODO CA Limited,CN=COMODO RSA Domain Validation Secure Server CA");
    check("subjectDN", "OU=Domain Control Validated,OU=PositiveSSL Wildcard,CN=*.badssl.com");
    check("sigAlgName", "SHA256WITHRSA");
  }
}
