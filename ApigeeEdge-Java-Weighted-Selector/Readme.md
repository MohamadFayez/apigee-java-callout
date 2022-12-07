# Java Weighted Selector callout

This directory contains the Java source code and pom.xml file required to
compile a simple Java callout for Apigee. The callout is performs a weighted
random selection, based on inputs.

## Building:

You do not need to build this callout in order to use it.  You can build if you want to. 

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache
  ```
  ./buildsetup.sh
  ```

2. Build with maven.
  ```
  mvn clean package
  ```

3. if you edit proxy bundles offline, copy the resulting jar file, available in  target/apigee-weighted-selector-20220104.jar  to your apiproxy/resources/java directory.  If you don't edit proxy bundles offline, upload the jar file into the API Proxy via the Apigee API Proxy Editor .

4. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:
   ```xml
   <JavaCallout name='Java-WeightedSelector'>
     <Properties>
       <Property name="weights">0, 1, 1, 2, 3, 5, 8, 13, 21, 34</Property>
     </Properties>
     <ClassName>com.google.apigee.callout.WeightedSelectorCallout</ClassName>
     <ResourceURL>java://apigee-custom-weighted-selector-20220104.jar</ResourceURL>
   </JavaCallout>
   ```

5. use the Edge UI or a command-line tool to
   import the proxy into an apigee organization, and then deploy the proxy .

6. Use a client to generate and send http requests to the proxy. Eg,
   ```
   curl -i $endpoint/weighted-selector/t1
   ```



## Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0
- Google Guava v30.0

These jars must be available on the classpath for the compile to
succeed. The buildsetup.sh script will download these files for
you automatically, and will insert them into your maven cache.

They are included in the Apigee runtime.

## Notes

There is one callout class, com.google.apigee.callout.WeightedSelectorCallout,
which does the weighted selection and then inserts the computed bucket value into a context variable.



## LICENSE

This material is [Copyright (c) 2017-2022 Google LLC](NOTICE).
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are few unit tests for this project.
