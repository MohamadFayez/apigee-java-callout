# Certificate Parser callout

Apigee can require clients to use TLS authentication, and when that happens,
if the `VirtualHost` is configured with the `PropagateTLSInformation` element,
the apiproxy itself can receive the certificate that was presented by the client.

But the proxy receives it in raw (encoded) form.

This repo contains the Java source code for a java callout that parses the encoded certificate and extracts information from it including:

* the serial number in base16
* the issuer DN string
* the subject DN string
* the notBefore and notAfter times
* the public key in PEM format

## License

This code is Copyright (c) 2017-2020 Google LLC, and is released under the Apache Source License v2.0. For information see the [LICENSE](LICENSE) file.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Using the Custom Policy

You do not need to build the Jar in order to use the custom policy.

There is one Java class: `com.google.apigee.edgecallouts.CertificateParseCallout`.

## Policy Configuration

Simply specify the certificate. There are no other options.

  ```xml
  <JavaCallout name="Java-ParseCertificate">
    <Properties>
      <Property name='certificate'>{variable_containing_cert}</Property>
    </Properties>
    <ClassName>com.google.apigee.callout.CertificateParseCallout</ClassName>
    <ResourceURL>java://apigee-custom-certificate-parser-20201203.jar</ResourceURL>
  </JavaCallout>
  ```

The outputs will be available in context variables beginning with the `cert_` prefix.

## Example Bundle

There is an [example bundle](./bundle) that demonstrates the use of the callout within an API
Proxy.

Example request to generate an encrypted JWT:

```
ORG=myorg
ENV=myenv
curl -i -X POST https://$ORG-$ENV.apigee.net/certificate-parser/t1
```

The result will be show information about a hard-coded certificate embedded within the proxy:
```
HTTP/2 200
apiproxy: certificate-parser r1
content-type: application/json
content-length: 394
date: Thu, 03 Dec 2020 23:08:13 GMT
server: apigee
via: 1.1 google
alt-svc: clear

{
  "message" : "ok",
  "system.uuid" : "7c7f1c29-d477-4e29-a6d2-e23be31af3e6",
  "cert" : {
    "serial" : "465d9557b6ba41cd596af6fe5dbb3af3c9b",
    "notBefore" : "2020-10-14T13:48:31Z",
    "notAfter" : "2021-01-12T13:48:31Z",
    "issuerDN" : "C=US,O=Let's Encrypt,CN=Let's Encrypt Authority X3",
    "subjectDN" : "CN=5g-dev.dinochiesa.net"
  },
  "now" : "Thu, 3 Dec 2020 23:08:13 GMT"
}

```

## Building the Jar

You do not need to build the Jar in order to use the custom policy. The custom policy is
ready to use, with policy configuration. You need to re-build the jar only if you want
to modify the behavior of the custom policy. Before you do that, be sure you understand
all the configuration options - the policy may be usable for you without modification.

If you do wish to build the jar, you can use
[maven](https://maven.apache.org/download.cgi) to do so. The build requires
JDK8. Before you run the build the first time, you need to download the Apigee
Edge dependencies into your local maven repo.

Preparation, first time only: `./buildsetup.sh`

To build: `mvn clean package`

The Jar source code includes tests.

## Author

Dino Chiesa
godino@google.com

## Bugs & Limitations

* ??
