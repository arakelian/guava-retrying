<!---
  Copyright 2012-2015 Ray Holder
  Modifications copyright 2017-2018 Robert Huffman

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
     http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

[![Build Status](https://travis-ci.org/rhuffman/re-retrying.svg?branch=master)](https://travis-ci.org/rhuffman/re-retrying)
[![Latest Version](http://img.shields.io/badge/latest-3.0.0-brightgreen.svg)](https://github.com/rhuffman/re-retrying/releases/tag/v3.0.0-rc.1)
[![License](http://img.shields.io/badge/license-apache%202-brightgreen.svg)](https://github.com/rhuffman/re-retrying/blob/master/LICENSE)

## What is this?
The guava-retrying module provides a general purpose method for retrying arbitrary Java code with specific stop, retry, and exception handling capabilities that are enhanced by Guava's predicate matching.

This is a fork of the [guava-retrying](https://github.com/rholder/guava-retrying) library by Ryan Holder (rholder), which is itself a fork of the [RetryerBuilder](http://code.google.com/p/guava-libraries/issues/detail?id=490) by Jean-Baptiste Nizet (JB). The guava-retrying project added a Gradle build for pushing it up to Maven Central, and exponential and Fibonacci backoff [WaitStrategies](http://rholder.github.io/guava-retrying/javadoc/2.0.0/com/github/rholder/retry/WaitStrategies.html) that might be useful for situations where more well-behaved service polling is preferred.

## Reason for Fork

* Add Java 11 support
* Make compatible with latest versions of Guava
* Fix all errorprone warnings in original source code

## Installation

The library is available on [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.arakelian%22%20AND%20a%3A%22elastic-indexer%22).

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central</id>
        <name>Central Repository</name>
        <url>http://repo.maven.apache.org/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>

...

<dependency>
    <groupId>com.arakelian</groupId>
    <artifactId>guava-retrying</artifactId>
    <version>3.0.1</version>
    <scope>test</scope>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  testCompile 'com.arakelian:guava-retrying:3.0.1'
}
```

## Licence

Apache Version 2.0

## Quickstart

Given a function that reads an integer:
```java

public int readAnInteger() throws IOException {
   ...
}
```

The following will retry if the result of the method is zero, if an `IOException` is thrown, or if any other `RuntimeException` is thrown from the `call()` method. It will stop after attempting to retry 3 times and throw a `RetryException` that contains information about the last failed attempt. If any other `Exception` pops out of the `call()` method it's wrapped and rethrown in an `ExecutionException`.

```java
    Retryer retryer = RetryerBuilder.newBuilder()
        .retryIfResult(Predicates.equalTo(0))
        .retryIfExceptionOfType(IOException.class)
        .retryIfRuntimeException()
        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
        .build();
    try {
      retryer.call(this::readAnInteger);
    } catch (RetryException | ExecutionException e) {
      e.printStackTrace();
    }
```

## Exponential Backoff

Create a `Retryer` that retries forever, waiting after every failed retry in increasing exponential backoff intervals until at most 5 minutes. After 5 minutes, retry from then on in 5 minute intervals.

```java
Retryer retryer = RetryerBuilder.newBuilder()
        .retryIfExceptionOfType(IOException.class)
        .retryIfRuntimeException()
        .withWaitStrategy(WaitStrategies.exponentialWait(100, 5, TimeUnit.MINUTES))
        .withStopStrategy(StopStrategies.neverStop())
        .build();
```
You can read more about [exponential backoff](http://en.wikipedia.org/wiki/Exponential_backoff) and the historic role it played in the development of TCP/IP in [Congestion Avoidance and Control](http://ee.lbl.gov/papers/congavoid.pdf).

## Fibonacci Backoff

Create a `Retryer` that retries forever, waiting after every failed retry in increasing Fibonacci backoff intervals until at most 2 minutes. After 2 minutes, retry from then on in 2 minute intervals.

```java
Retryer retryer = RetryerBuilder.newBuilder()
        .retryIfExceptionOfType(IOException.class)
        .retryIfRuntimeException()
        .withWaitStrategy(WaitStrategies.fibonacciWait(100, 2, TimeUnit.MINUTES))
        .withStopStrategy(StopStrategies.neverStop())
        .build();
```

Similar to the `ExponentialWaitStrategy`, the `FibonacciWaitStrategy` follows a pattern of waiting an increasing amount of time after each failed attempt.

Instead of an exponential function it's (obviously) using a [Fibonacci sequence](https://en.wikipedia.org/wiki/Fibonacci_numbers) to calculate the wait time.

Depending on the problem at hand, the `FibonacciWaitStrategy` might perform better and lead to better throughput than the `ExponentialWaitStrategy` - at least according to [A Performance Comparison of Different Backoff Algorithms under Different Rebroadcast Probabilities for MANETs](http://www.comp.leeds.ac.uk/ukpew09/papers/12.pdf).

The implementation of `FibonacciWaitStrategy` is using an iterative version of the Fibonacci because a (naive) recursive version will lead to a [StackOverflowError](http://docs.oracle.com/javase/7/docs/api/java/lang/StackOverflowError.html) at a certain point (although very unlikely with useful parameters for retrying).

Inspiration for this implementation came from [Efficient retry/backoff mechanisms](https://paperairoplane.net/?p=640).

## Documentation
Javadoc can be found [here](http://rholder.github.io/guava-retrying/javadoc/2.0.0).

## License
The re-retrying module is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

## Contributors
* Jean-Baptiste Nizet (JB)
* Jason Dunkelberger (dirkraft)
* Diwaker Gupta (diwakergupta)
* Jochen Schalanda (joschi)
* Shajahan Palayil (shasts)
* Olivier Grégoire (fror)
* Andrei Savu (andreisavu)
* (tchdp)
* (squalloser)
* Yaroslav Matveychuk (yaroslavm)
* Stephan Schroevers (Stephan202)
* Chad (voiceinsideyou)
* Kevin Conaway (kevinconaway)
* Alberto Scotto (alb-i986)
* Ryan Holder(rholder)
* Robert Huffman (rhuffman)

