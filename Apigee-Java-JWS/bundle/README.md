# Example bundle for JWS Callout

This directory contains is an example API Proxy bundle that demonstrates the
JWS Java calout. The callout can generate and verify JWS.

## Pre-requisites

1. Deploy the API Proxy into an Apigee organization + environment.

## Invoke it

1. generate an example JWS
   ```
   $endpoint=https://foo.bar.com
   curl -i ${endpoint}/jws/generate_jws -d ''
   ```

   You should see something like this as output:
   ```
   HTTP/2 200
   content-type: text/plain
   content-length: 755
   date: Mon, 16 Nov 2020 19:28:59 GMT
   server: apigee
   via: 1.1 google
   alt-svc: clear

   eyJ0eXAiOiJKV1Q....eXX258QmzYdBA
   ```

2. generate an  example JWS with no encoding:

   ```
   curl -i ${endpoint}/jws/generate_jws_no_encode -d ''
   ```

   Again you should see a valid JWS emitted.
