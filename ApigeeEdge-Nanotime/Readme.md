# Apigee Edge Java callout: Nanotime

This directory contains the Java source code and pom.xml file required to
compile a simple Java callout for Apigee Edge. The callout is very simple: it creates
a context variable with the value retrieved from System.nanoTime().

The jar is not included in the repo.
You must build this code to use the callout.

## Building

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache
  ```./buildsetup.sh```

2. Build with maven.
  ```mvn clean package```

3. if you edit proxy bundles offline, copy the resulting jar file, available in  target/edge-java-callout-nanotime-1.0.1.jar to your apiproxy/resources/java directory.  If you don't edit proxy bundles offline, upload the jar file into the API Proxy via the Edge API Proxy Editor .

4. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:
   ```xml
    <JavaCallout name='Java-nanotime'>
      <ClassName>com.google.apigee.edgecallouts.Nanotime</ClassName>
      <ResourceURL>java://edge-callout-nanotime-1.0.1.jar</ResourceURL>
    </JavaCallout>
   ```

5. use the Edge UI, or a command-line tool like [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js) or similar to
   import the proxy into an Edge organization, and then deploy the proxy .
   Eg,
   ```
   node importAndDeploy.js -v -n -o $ORG -e $ENV -d ./bundle
   ```

6. Use a client to generate and send http requests to the proxy. Eg,
   ```
   curl -i https://$ORG-$ENV.apigee.net/nanotime/t1
   ```



## Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0

These jars must be available on the classpath for the compile to
succeed. The buildsetup.sh script will download these files for
you automatically, and will insert them into your maven cache.

If you want to download them manually:

These jars are
produced by Apigee; contact Apigee support to obtain these jars to allow
the compile, or get them [on github](https://github.com/apigee/api-platform-samples/tree/master/doc-samples/java-cookbook/lib).


## Notes

There is one callout class, com.google.apigee.edgecallouts.Nanotime ,
which calls System.nanoTime() and then inserts the retrieved value into a context variable.

## Example Usage

```
$ curl -i https://ORGNAME-test.apigee.net/nanotime/t1
HTTP/1.1 200 OK
Date: Tue, 10 Nov 2015 19:41:39 GMT
Content-Type: application/json
Content-Length: 161
Connection: keep-alive
Server: Apigee Router

{
  "message" : "ok",
  "system.uuid" : "238753a9-3518-484b-a331-222bbb7c6128",
  "nano.time" : "11368962740872986",
  "now" : "Tue, 10 Nov 2015 19:41:39 UTC"
}
```

## LICENSE

This material is copyright 2015,2016 Apigee Corporation, 2019 Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are no unit tests for this project.
