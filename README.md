```
Copyright (c) 2019 California Community Colleges Technology Center
Licensed under the MIT license.
A copy of this license may be found at https://opensource.org/licenses/mit-license.php
```

# College Adaptor API

This repository contains a Spring Boot application to expose College APIs via RESTful services

## Development Environment Setup

* IntelliJ Community or Ultimate are recommended. Community Edition is free.
* Maven is required - here: https://maven.apache.org - make sure to read the installation steps
* Groovy is required - here: http://groovy-lang.org/
* Java 1.8 (not older, not newer!)

See also the [Quickstart](quickstart.md) guide for quick setup steps.

## Additional Configuration

* JCE strong encryption libraries for Java. Follow the instructions at: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
* This project uses Lombok. It is highly recommended that you enable the plugin for IntelliJ to take advantage of it:
  https://projectlombok.org/setup/intellij
* The Tech Center Nexus repository must be configured for Maven. Instructions here: https://cccnext.jira.com/wiki/display/CE/Nexus
* Github package registry must be configured.  This will eventually replace Nexus.  Instructions here: https://cccnext.jira.com/wiki/spaces/CCCGLUE/pages/978485561/Github+Package+Registry+user+setup
* Install Docker: https://docs.docker.com/install/

Note that The Oracle JDBC driver is required. This driver is now being pulled from maven central.

## Compilation, Packaging, Installation, Tests

| Task                 | Maven Command |
|:---------------------|:--------------|
|Compile (no jar)      |`mvn compile`|
|Package (produces jar)|`mvn package`|
|Install into Docker   |`mvn install -Dmiscode={miscode}` (see below)|
|Run Unit tests        |`mvn test`|
|Run Code Coverage     |`mvn prepare-package`|
|Run Integration tests |`mvn process-test-resources -Ppostman,[dev ci or qa]` (see below)|

> Tip: put `clean` immediately after any `mvn` command to clean the target folder

## Running the College Adaptor Locally

### IntelliJ

To run the College Adaptor in IntelliJ you will first need to compile with maven to build certain resources. Run the
command `mvn clean compile`. You should only need to do this once, not every time you make a change as this generates
certain static files in the target folder that are required by the college adaptor (but do not change). If you clean or
re-build or remove the target folder you wil need to re-run `mvn clean compile` to generate these files again.

Next, you will need to set up a Run Configuration in IntelliJ. Go to Run -> Edit Configurations and create a new
configuration with the following:

    Main class: com.ccctc.adaptor.CollegeAdaptorApplication
    VM Options: -Dspring.profiles.active=dev,001,local -Dencrypt.key=ThisIsNotTheActualKey
    Environment Variables: miscode=configsystem;conductor_authClientSecret={AWS SSM /ci/cloud-config-service/admin-pw}

If you have any problems running the College Adaptor in IntelliJ, try running `mvn clean compile` again, and ensure
your profile has both a "spring.profiles.active" and "encrypt.key" arguments (as above).

### From the command line

To run the application in standalone mode, execute the following from the root of the project (you will first need to
package it via `mvn clean package`). Note this will use cloud config to get properties information for dev and 001 
respectively. See the college-adaptor-config repository.

    Environment Variables: miscode=configsystem;conductor_authClientSecret={AWS SSM /ci/cloud-config-service/admin-pw}
    
    java -jar target/college-adaptor-2.1.10-SNAPSHOT.jar --spring.profiles.active=dev,001  --encrypt.key=ThisIsNotTheActualKey


## Running in Docker

There are two steps required to run the College Adaptor in docker.

__Step 1: Create the image. It will be named ccctechcenter/college-adaptor with the tag "latest"__

    mvn clean install
    or
    mvn clean install -Dmaven.test.skip=true (skip unit tests)

__Step 2: Run the image in a new container:__

Option #1 - Docker-compose - 001 and 002 profiles are setup in docker.compose.yml

    docker-compose up -d college-adaptor-001
    or
    docker-compose up -d college-adaptor-002

Option #2 - Docker run - more verbose, but more control of individual parameters

    docker run -d -e miscode=001 -e SPRING_PROFILES_ACTIVE=dev,001 -e ENCRYPT_KEY=ThisIsNotTheActualKey -e conductor_authClientSecret={AWS SSM /ci/cloud-config-service/admin-pw} -p 8443:8443 --name college-adaptor ccctechcenter/college-adaptor


> Caution: You must run step #1 each time to create a new docker image. Otherwise you will just be launching a container
> from an older build of the college adaptor.

## Spring Profiles

The College Adaptor makes use of Spring Cloud

__See the college-adaptor-config repository for complete list of profiles__

1. dev         - Sets the app to run as a local developer machine using application-dev.properties
2. qa          - Sets the app to run as 'qa' using application-qa.properties
3. pilot       - Sets the app to run as 'pilot' using application-pilot.properties
4. prod        - Sets the app to run as 'prod' using the application-prod.properties
5. xxx         - Add a college-specific profile based on this miscode: application-${miscode}.properties


## Integration Testing - Postman

Postman tests are integration tests that are run after deployment to validate the install.
The postman tests for this service are checked into the postman-api-test repository.
Clone the test repository with `git clone git@bitbucket.org:cccnext/postman-api-test.git`

Review the README.md in that project for full instructions on updating, building, and running the test container.

Optionally, once you've setup authentication vs. our private docker registry ( reference: https://cccnext.jira.com/wiki/spaces/DEVOPS/pages/129302935?atlOrigin=eyJpIjoiM2YyMTJkOTU3Mzc2NDQ2ZGJiOTdiZjVjNzVjMDRiYjMiLCJwIjoiYyJ9 ), you can skip building the test container locally, and simply run the tests vs. the latest published test image

Example: To run the latest copy of the tests vs the 001 Mock adaptor QA environment:
```
docker run --rm -v $PWD:/etc/newman/newman -t registry.ccctechcenter.org:5000/ccctechcenter/postman-api-test environment=001-qa basename=CollegeAdaptor_build-tests folder="mock-adaptor-tests"
```

The value you use for environment determines which deployed adaptor the tests target.
If you choose 001-dev, they will be run vs a locally running adaptor (see Running the postman tests locally
following this section). If you choose 002-ci, or 001-qa, they will be run against the CI and QA deployments in AWS respectively.

See https://cccnext.jira.com/wiki/display/CE/CourseExchange+Environments for environment details.


### Running the Postman tests locally

There are many configurations that will work locally to run the postman tests. This is one technique:

* Compile the jar file for college adaptor. This will skip tests - so make sure your tests work first or omit that part:

```
mvn clean package -Dmaven.test.skip=true
```

* Run the college adaptor locally from the command line. The "qa" profile is  arbitrary, dev or ci would also work.
   The other two are important, however, as 001 specifies the mock 001 adaptor.
   Note that using 002 would also work as that is a valid mock adaptor as well. The CI environment uses the 002 profile
   when running integration tests and QA uses 001. Both 001 and 002 adaptors are deployed into QA.

```
java -DsisType=mock -jar target/college-adaptor-2.1.10-SNAPSHOT.jar --spring.profiles.active=qa,001 --encrypt.key=ThisIsNotTheActualKey
```

* After the college adaptor has started up, open another terminal window in the root folder of college adaptor and run
   this command.

> Tip: In a Windows command prompt first run `chcp 1250` to turn on UTF-8 output so it will look pretty!

```
docker run --net="host" --rm -v $PWD:/etc/newman/newman -t registry.ccctechcenter.org:5000/ccctechcenter/postman-api-test environment=001-dev basename=CollegeAdaptor_build-tests folder="mock-adaptor-tests"
```


## Troubleshooting

### Cipher or encryption errors

If you are getting encryption errors such as ``java.security.InvalidKeyException: Illegal key size`` or  ``Cannot
decrypt: key=spring.cloud.config.password`` then you most likely need to install the Java Cryptography Extension (JCE)
into __EVERY__ location where you have Java installed. The files are here:
http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html and they go into the lib/security
folder under the JRE you're using

### Startup issues when running in IntelliJ

IntelliJ might experience issues starting up Spring Boot due to some dependencies not being compiled through maven. To
resolve the issue, run the command `mvn clean compile` then try again.

<!--
Below used by the docusauraus : https://docusaurus.io/docs/en/doc-markdown
-->
----
hide_title: true
---
