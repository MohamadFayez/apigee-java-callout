# Java callout for JSONPath

This directory contains the Java source code required to compile a Java callout
for Apigee that does JSONPath. There's a built-in ExtractVariables policy that
can evaluate json-path; this callout is a bit more flexible.

* the source can be a string or a message
* The jsonpath processor is more current


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Motivation

The builtin
[ExtractVariables](https://docs.apigee.com/api-platform/reference/policies/extract-variables-policy)
policy in Apigee relies on an older version of the jayway jsonpath library,
v0.8.0.  This version has some shortcomings.

1. It is not possible to filter on a boolean literal field. For example, in the case of this JSON:
   ```json
   [    {
         "isActive" : false,
         "resourceId" : 2
       },
       {
         "isActive" : true,
         "resourceId" : 3
       }
   ]
   ```

   This query does not work: `$[?(@.isActive == true)].resourceId`

2. compound filter predicates do not work.

3. the `in` and `nin` operators do not work. For example, this is invalid: `$.quotas.quota[?(@.appname in ['B','C'])].value`

4. It's not clear that the other aggregate operators like `subsetof` `anyof` `noneof` work.

4. slices return a single element, rather than an array.

To address those shortcomings, it would be nice to have a JSONPath mechanism that used v2.4.0 of the library.

That isn't possible in today's Apigee with builtin policies. This callout allows you to use any of these more current jsonpath features.


## Using this policy

To use the policy you must have an API Proxy configured with the JAR included
here, as well as all of its dependencies, in the resources/jaava directory.

You do not need to build the source code in order to use the policy in Apigee,
or to download all the dependencies. The pre-built JAR and the dependencies are
include in this repo.

But if you _want_ to build the policy from source code, you can do so.
The instructions to do so are at the bottom of this README.


To use the jar and dependencies included in this repo:

* copy all of the jar files available in [the
  repo](bundle/apiproxy/resources/java/), to your apiproxy/resources/java
  directory. You can do this offline in your filesystem, or you can do it
  graphically using the Proxy Editor in the Apigee Edge Admin UI.

To use the jar and the downloaded dependencies, _first_ build the project (see
instructions below), then after you build it:

* copy the jar file, available in target/apigee-callout-jsonpath-20220602.jar , if
  you have built the jar, or in [the
  repo](bundle/apiproxy/resources/java/apigee-callout-jsonpath-20220602.jar) if
  you have not, to your apiproxy/resources/java directory. Also copy all the
  required dependencies. (See below) You can do this offline, or using the
  graphical Proxy Editor in the Apigee Edge Admin Portal.


Then, in either case:

1. include a Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:
   ```xml
   <JavaCallout name="Java-JSON-Path-Multiple-Fields">
     <Properties>
       <Property name='jsonpath'>$[*]['id','name']</Property>
       <Property name='source'>contrivedMessage.content</Property>
     </Properties>
     <ClassName>com.google.apigee.callouts.jsonpath.JsonPathCallout</ClassName>
     <ResourceURL>java://apigee-callout-jsonpath-20220602.jar</ResourceURL>
   </JavaCallout>
   ```

5. use the Apigee UI, or a command-line tool like [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js-examples/blob/main/importAndDeploy.js) or [apigeecli](https://github.com/apigee/apigeecli) or similar to
   import the proxy into an Apigee organization, and then deploy the proxy .

6. use a client to generate and send http requests to invoke the proxy.



## Usage Notes

There is one callout class, com.google.apigee.callouts.jsonpath.JsonPathCallout ,
which performs a JSON Path read . This class depends on [the jayway jsonpath
library for Java](https://github.com/json-path/JsonPath), v2.7.0

You must configure the callout with Property elements in the policy
configuration.

| Property             | Description                                                                                                      |
|----------------------|------------------------------------------------------------------------------------------------------------------|
| jsonpath             | required. a string representing the query.                                                                       |
| source               | optional. name of a string variable that contains json, or name of a Message that has a json payload.            |
| return-first-element | optional. when the jsonpath returns a list, this property gets the first element of that list. (See notes below) |


Regarding `return-first-element`: the jsonpath spec allows for predicates, but
does not allow for indexers following predicates.  Given this source json:
```json
{
  "records" : [
  {
    "type" : "A",
    "value" : 1000
  },
  {
    "type" : "A",
    "value" : 8000
  },
  {
    "type" : "B",
    "value" : 900
  }
 ]
}
```

A json path of `$.records[?(@.type=='A')]` yields:
```json
[
   {
      "type" : "A",
      "value" : 1000
   },
   {
      "type" : "A",
      "value" : 8000
   }
]
```

This is an array. One might thing that using a jsonpath that appends a `[0]` to
the above query, giving you `$.records[?(@.type=='A')][0]`, would yield the
first element of that array, but [that is not
correct](https://github.com/json-path/JsonPath/issues/272). JsonPath does not
support that syntax for a query.


The `return-first-element` property allows you to retrieve just the first
element of the returned array.  If you need the 2nd element or ... something
else, then... you can get that by evaluating a 2nd jsonpath query.

## Building

Building from source requires Java 1.8, and Maven.

1. unpack (if you can read this, you've already done that).

2. Before building _the first time_, configure the build on your machine by loading the Apigee jars into your local cache:
  ```
  ./buildsetup.sh
  ```

3. Build with maven.
  ```
  mvn clean package
  ```
  This will build the jar and also run all the tests.


Pull requests are welcomed!


## Build Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0
- jayway json-path 2.7.0
- json-smart 2.4.8


## License

This material is Copyright (c) 2019-2022, Google LLC.  and is licensed under
the [Apache 2.0 License](LICENSE). This includes the Java code as well
as the API Proxy configuration.


## Support

This callout is open-source software, and is not a supported part of Apigee.
If you need assistance, you can try inquiring on
[The Apigee Community Site](https://www.googlecloudcommunity.com/gc/Apigee/bd-p/cloud-apigee).
There is no service-level
guarantee for responses to inquiries regarding this callout.


## Bugs

* The tests are incomplete.
