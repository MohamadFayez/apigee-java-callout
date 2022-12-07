# Java BCrypt Check

This directory contains the Java source code and pom.xml file required to
compile a simple Java callout for Apigee, that performs a BCrypt password hash check.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Notes

There is one callout class, com.google.apigee.callouts.BcryptCheck ,
which checks a plaintext password against a bcrypt hash.


## Example Usage

This is what the policy configuration looks like:

```
<JavaCallout name='Java-BcryptCheck'>
  <Properties>
    <Property name='hash'>{bcrypt_hash}</Property>
    <Property name='password'>{plaintext_password}</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.BcryptCheck</ClassName>
  <ResourceURL>java://apigee-bcrypt-password-check-20220104.jar</ResourceURL>
</JavaCallout>
```

There are two required properties:
* hash - the bcrypt hash
* password - the plaintext password to check against the hash

You can specify these with context variables using the curly-brace syntax shown above.

## BouncyCastle is a Runtime dependency

The BouncyCastle JAR is a runtime dependency.  Apigee includes that as an
undocumented library, so you do not need to provide it within your proxy.

## License

This material is copyright 2018-2021, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are no unit tests for this project.
