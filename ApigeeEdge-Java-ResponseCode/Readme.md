# Java Response Code

This directory contains the Java source code and pom.xml file required to
compile a simple Java callout for Apigee Edge. The callout is very simple: it demonstrates how you can set a response code from within the Java code.

## Building:

1. unpack (if you can read this, you've already done that).

2. cd to the callout directory

2. configure the build on your machine by loading the Apigee jars into your local cache
  ```./buildsetup.sh```

2. Build with maven.
  ```mvn clean package```

3. Deploy the API Proxy bundle in the bundle directory

6. Use a client to generate and send http requests to the proxy. Eg,
   ```curl -i http://ORGNAME-test.apigee.net/responsecode/t1```


## Notes

There is one callout class, com.dinochiesa.edgecallouts.ResponseCode ,
which calls System.nanoTime() and then inserts the retrieved value into a context variable.
It also sets a few other context variables.

These variables are then available for testing within the Context flow, and can be used to conditionally execute subsequent steps.


## LICENSE

This material is copyright 2017 Google, Inc.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are no unit tests for this project.
