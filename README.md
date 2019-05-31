[![pipeline status](https://gitlab.com/ebo/blueprintsprocessor/badges/master/pipeline.svg)](https://gitlab.com/ebo/blueprintsprocessor/commits/master)
[![coverage report](https://gitlab.com/ebo/blueprintsprocessor/badges/master/coverage.svg)](https://ebo.gitlab.io/blueprintsprocessor/jacoco/)
[![spock reports](https://img.shields.io/badge/spock-reports-blue.svg)](https://ebo.gitlab.io/blueprintsprocessor/spock/)
[![pitest reports](https://img.shields.io/badge/pitest-reports-violet.svg)](https://ebo.gitlab.io/blueprintsprocessor/pitest/)

## Overview

The project is a fork of https://github.com/onap/ccsdk-cds and is aimed to support:

- Restore of existing tests that were eventually disabled;
- Add more Unit/Integration tests;
- Increase tests code-coverage to more acceptable production-level (>= 70%);
- Implement mutation-tests to validate the tests.

## Building the application

To build the application from the sources just run:

```
./gradlew build
```

