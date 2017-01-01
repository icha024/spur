# TOTP Validation Server for Google Authenticator
[![Build Status](https://travis-ci.org/icha024/Google-Auth-TOTP-Server.svg?branch=master)](https://travis-ci.org/icha024/Google-Auth-TOTP-Server)

This is the server-side component for validating Google Authenticator's **Time-based One Time Password (TOTP)** as described in [RFC-6238](https://tools.ietf.org/html/rfc6238), commonly used for Two Factor Authentication (2FA).

The core logic is extracted from the Android version of [Google Authenticator](https://github.com/google/google-authenticator-android). This means you may fork the Google Authenticator project, or just download it from the app store, and it will work with that straight out the box!

# Packaged in two modules
- **Web** package is a pre-built, simple and high performance, micro-service using the TOTP validator library for you to deploy easily.
- **Validator** package is a library component, used by the web server, for validating the TOTP. This is convenient if you wish you build your own 2FA server for validation.

## (Web) Validation Server

#### Installing
(TODO)

#### Usage
(TODO)

#### High Performance Validator
Build on Undertow server, this can handle over 31,000 transactions per second on a Core i7 4790S (4GHz) dev box running Ubuntu 14.04

```
$ siege -t 60s -b http://localhost:8080/aaaabbbbcccc/121212
** SIEGE 3.0.5
** Preparing 15 concurrent users for battle.
The server is now under siege...
Lifting the server siege...      done.

Transactions:		     1886325 hits
Availability:		      100.00 %
Elapsed time:		       59.46 secs
Data transferred:	       34.18 MB
Response time:		        0.00 secs
Transaction rate:	    31724.27 trans/sec
Throughput:		        0.57 MB/sec
Concurrency:		       14.30
Successful transactions:     1886325
Failed transactions:	           0
Longest transaction:	        0.02
Shortest transaction:	        0.00
```

## (Validator) Validation Library
For anyone wanting to reuse the validation module and build their own 2FA service.

#### Installing with Maven
(TODO)

#### Usage
(TODO)

## License

Version 2.0 of the Apache License.

## Requirements
- Java 1.8+
