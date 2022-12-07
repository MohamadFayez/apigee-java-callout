// CalloutTestBase.java
//
// Copyright (c) 2018-2021 Google LLC
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

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.MessageContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.RestrictedResourceRetriever;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import mockit.Mock;
import mockit.MockUp;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public abstract class CalloutTestBase {

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

  protected final static String privateKey1 =
"-----BEGIN RSA PRIVATE KEY-----\n"
+ "MIIEowIBAAKCAQEArouIADal6Q1l3I5RfBaNLtvb826+Djm4UrfI5jpO54K6j3Gs\n"
+ "vCRMYpz++SQ45sP31gFpl3jvBVyQ83DlUTWsyb1zpjftLLHK04NJeFawS1Nbtj+2\n"
+ "V56t7Zbl1byLbr8Rw1c8IO04oqnycrcAU33KEdF5vluCvg8qpVCJz+AV1ZVNLWiL\n"
+ "flyCVsF1RYlS/OfXVxeKQTE6k3UPDkg/5UOhZYZ1W96KyJwNM4lrziGqBWJIl6da\n"
+ "YsJuT34Z4iOTVsDHPE9yeXFsaftdaPLe0augk6B/5we1CbQeijhPUmcnzmf6ArAG\n"
+ "mtwooPLjowFjwOv1HS7sG67ODvzZY791hcbExQIDAQABAoIBACmoz+sNIAhB1GAR\n"
+ "78zoLQZUH2k4s0/94sqLZv3cSNzkzNZT0WCOYVTgF9MrHBGoEE0ZxTQL/zCOaWJR\n"
+ "PcpmPzlfaGzxyD/0p25YVX7NYgJ4gNk8166OBwFAFNcwyy7Bl+HBvm41cGESovVS\n"
+ "TFehHEuobaBLgycNw6X1VQ8ycsOpG+UbRTJ/QV0KU/OW+CrEHGvaGxLy0ycxjjoC\n"
+ "feHW17+Us2qeBvNXOaxPHeoLg9+0wln2WuoHOHRKD+JJWhOCK9rQYK0BwjnRmYyI\n"
+ "czOPTL1aOkIwb+u2t9kesoA5E4znlPhOKQj+niqHhTNoRAJdSZwZrBYfFvZ4FueM\n"
+ "8sAnGvkCgYEA3Jucwoxrt5JaZUP/Zjbiby9mnYK2B7+vl7BVk3hkCKbuQIGnbn6G\n"
+ "ZJV6EIMUWLkb8+nloeSvy7+1AkWxXY7VYwuzqvWqhrmoXjBygHr6KtrLsz7Ogmij\n"
+ "EZrsZCK3/3DWJgylZOv5PB1rj8V6L7QePmj83gI4/FYJprPVJJnQaPMCgYEAyowd\n"
+ "QDnH4PzWmSfzlso00RAde6LsF0Qpq2so+nQxkLfYJjMPYWXWuvznz+6wyNEPRiI9\n"
+ "XomgB/EfiR8PlNq8j85Xksr+2XQqOQYgVgZC8040vpNLybgqS1uqIPNVJbbpGDXA\n"
+ "w+9f+a+oMgE/dqZtnKBOVTKUVz6+JigUC4LUCWcCgYEArsmoYUhKjC6r6nH+qCiy\n"
+ "LW+7+O44dVk9sYynsOkBMQ251WgklVov9v+rr+t7MnSvngjixOthEai5rKw1RDBI\n"
+ "B2qdFsYALzBoIwB1qDBHh67FGCaaDh8DnI5H32rWp8/qDEmWvahtV2Dj+Qx4q9Uk\n"
+ "5UPfnbLbHaq5iNgQ9yfbRVsCgYAulAAaB++WJq6288AJmiCBP0J4byP5ybwHZpI6\n"
+ "3kOTsyNqzW0pCcFSqNwqLgrLc4AesbsJJX7+tI16/ACaS573Nw1efX4Txan8CROg\n"
+ "lLoKt55bgQX5sndPcxnxj+Ox05lQ7vOQW1jn02RLc4wDngww65B3+TSxx4T0w1yw\n"
+ "tPpL2wKBgAkX/+M6w38bKZ740Kf8Hu8qoUtpu/icf3zkqtjHGQyIxWgq+vDenJJM\n"
+ "GZev6o3c0OtTndUYwFIrxzZaL1gP6Tb8QGuIA49VVMEvWXJl/rPaa5Ip17ee0YnX\n"
+ "BhkCjT+pD2dW1X9S9C6IgcTF8f6Ta27omyw3aqpxefpiVVSbV/I9\n"
+ "-----END RSA PRIVATE KEY-----\n";

  protected final static String privateKey2 =
"-----BEGIN PRIVATE KEY-----\n"
+ "MIIBeQIBADCCAQMGByqGSM49AgEwgfcCAQEwLAYHKoZIzj0BAQIhAP////8AAAAB\n"
+"AAAAAAAAAAAAAAAA////////////////MFsEIP////8AAAABAAAAAAAAAAAAAAAA\n"
+"///////////////8BCBaxjXYqjqT57PrvVV2mIa8ZR0GsMxTsPY7zjw+J9JgSwMV\n"
+"AMSdNgiG5wSTamZ44ROdJreBn36QBEEEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg\n"
+"9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9QIhAP////8A\n"
+"AAAA//////////+85vqtpxeehPO5ysL8YyVRAgEBBG0wawIBAQQgFQ89T4ICkOlo\n"
+"YFfnXHccyZmkisYNmlzeqaUVI5M1f76hRANCAARRTSI8JxrEnMg0SXlXisdNfwmS\n"
+"GbPcKxnqVk9p/ILkcMk/dPugXuJVQbUuxG3cmte6Zs7wamzQJUfqHHtihqb0\n"
+  "-----END PRIVATE KEY-----\n";


  protected final static String publicKey1 =
"-----BEGIN PUBLIC KEY-----\n"
+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArouIADal6Q1l3I5RfBaN\n"
+ "Ltvb826+Djm4UrfI5jpO54K6j3GsvCRMYpz++SQ45sP31gFpl3jvBVyQ83DlUTWs\n"
+ "yb1zpjftLLHK04NJeFawS1Nbtj+2V56t7Zbl1byLbr8Rw1c8IO04oqnycrcAU33K\n"
+ "EdF5vluCvg8qpVCJz+AV1ZVNLWiLflyCVsF1RYlS/OfXVxeKQTE6k3UPDkg/5UOh\n"
+ "ZYZ1W96KyJwNM4lrziGqBWJIl6daYsJuT34Z4iOTVsDHPE9yeXFsaftdaPLe0aug\n"
+ "k6B/5we1CbQeijhPUmcnzmf6ArAGmtwooPLjowFjwOv1HS7sG67ODvzZY791hcbE\n"
+ "xQIDAQAB\n"
+ "-----END PUBLIC KEY-----\n";


  protected final static String publicKey2 =
"-----BEGIN PUBLIC KEY-----\n"
+"MIIBSzCCAQMGByqGSM49AgEwgfcCAQEwLAYHKoZIzj0BAQIhAP////8AAAABAAAA\n"
+"AAAAAAAAAAAA////////////////MFsEIP////8AAAABAAAAAAAAAAAAAAAA////\n"
+"///////////8BCBaxjXYqjqT57PrvVV2mIa8ZR0GsMxTsPY7zjw+J9JgSwMVAMSd\n"
+"NgiG5wSTamZ44ROdJreBn36QBEEEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg9KE5\n"
+"RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9QIhAP////8AAAAA\n"
+"//////////+85vqtpxeehPO5ysL8YyVRAgEBA0IABFFNIjwnGsScyDRJeVeKx01/\n"
+"CZIZs9wrGepWT2n8guRwyT90+6Be4lVBtS7Ebdya17pmzvBqbNAlR+oce2KGpvQ=\n"
+    "-----END PUBLIC KEY-----\n";

  protected static final String jws1 =
    "eyJjdHkiOiJ0ZXh0XC9wbGFpbiIsInR5cCI6IkpPU0UiLCJoZHIxIjoxMjMsImFsZyI6IlJTMjU2In0.cGF5bG9hZGhlcmU.W_g6newfYaOPF8bmgyFtGFPYZ-e4ZrvNoyes0DUOlj11J6D2B_5BA_OeGdOWWrSRAz6K768FIQaJnVI-MaATJ-HcrL0CyoFZwHUA9k5bbQXSX1MssWcrxVXWPz5m3zqS41CKT8Jk-m0b88wm42-zklDRK3EmG5Blcqufz8WoiBSAeHtjdFpY7zGJCtXLjTc7biioNWcPWpRzQRJ5favh9CkkXLWbqL-e0jd0C68QvZfYte3uCkyatAokbhYHkl_Rylpn4aleDdjnSIhV5_MmPRe7xcxj5MVsyygFmwv11QIK4MTyzyBFfTI33V2H91_JFva40AAvTViWbXQuX2qHKQ";


  protected void reportThings(Map<String, String> props) {
    String prefix = "jws";
    String test = props.get("testname");
    System.out.println("test  : " + test);
    String header = msgCtxt.getVariable(prefix + "_header");
    System.out.println("header: " + header);
    String payload = msgCtxt.getVariable(prefix + "_payload");
    System.out.println("payload: " + payload);

    String alg = msgCtxt.getVariable(prefix +"_alg");
    System.out.println("alg: " + alg);

    String error = msgCtxt.getVariable(prefix + "_error");
    System.out.println("error : " + error);
  }
}
