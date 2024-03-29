[![Build Status](https://travis-ci.org/sul-dlss/dlss-wowza.svg?branch=master)](https://travis-ci.org/sul-dlss/dlss-wowza) [![Coverage Status](https://coveralls.io/repos/github/sul-dlss/dlss-wowza/badge.svg?branch=master)](https://coveralls.io/github/sul-dlss/dlss-wowza?branch=master)
[![GitHub version](https://badge.fury.io/gh/sul-dlss%2Fdlss-wowza.svg)](https://badge.fury.io/gh/sul-dlss%2Fdlss-wowza)

# dlss-wowza

Wowza server side modules.  Uses Wowza provided Java API to restrict access to content to authorized users.  Relies on Stacks for minting and verifying tokens.

## Build info

Uses Gradle (via Gradle wrapper) as the build tool (akin to Ruby's rake).

### Java version / Consider using jenv

We should be trying to use a recent and supported version of Java in local development, on CI/build servers, and on deployed Wowza servers.

For local development, `jenv` (akin to e.g. `rbenv` or `n`) can help if you need to manage multiple versions of Java.  [Documentation for jenv](https://github.com/jenv/jenv).

### See if there's a Gradle wrapper upgrade

If it's been a few weeks or months since the project has been touched, you can install the latest Gradle by running:

```sh
./gradlew wrapper --gradle-version=latest --distribution-type=bin # you can also install a specific version, e.g. --gradle-version=8.4
```

If updates are generated, please commit the changes (likely `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`; possibly `build.gradle` if you have to fix deprecated build config).

See "Upgrade with the Gradle Wrapper" in the [installation instructions](https://gradle.org/install/), and the more detailed [Gradle Wrapper doc](https://docs.gradle.org/8.4/userguide/gradle_wrapper.html).

### To compile and run tests of this code

```sh
./gradlew build
```

Before running this command, you'll need to set these environment variables:
- `WOWZA_HONEYBADGER_API_KEY`:  You can get the API key from our [Honeybadger](https://www.honeybadger.io/) project (`DLSS-Wowza`).  Note that you may need to get added to the `DLSS-Wowza` project as a user in order to obtain the key (someone from Devops should be able to add you).
- `WOWZA_HONEYBADGER_ENV`:  Anything other than the names of the deployed environments should be fine.  E.g., something like `dlss_wowza_laptop_$USER`.

If the environment variables are not set, your tests will fail.

### To just run tests and generate a coverage report

```sh
./gradlew check # will invoke build tasks as needed
```

#### Coverage report

The coverage report is available in the project directory at `build/reports/jacoco/test/html/index.html`.

## Using the SulWowza plugin

### Adding the SulWowza plugin to your Wowza Application

In the Wowza GUI interface, select your Wowza Application, and click on the modules tab. Add a module with the class name `edu.stanford.dlss.wowza.SulWowza` (the name and description are arbitrary strings).

Alternatively, you can manually edit Application.xml and then reload the Wowza application.  The `<Modules>` element in `Application.xml` should include something like this:

```xml
  <Module>
    <Name>SulWowza</Name>
    <Description>SUL Authorization against Stacks</Description>
    <Class>edu.stanford.dlss.wowza.SulWowza</Class>
  </Module>
```

### Properties settings used by SulWowza plugin

You can configure the SulWowza plugin using Wowza's GUI interface, but you will also need to add some properties. The three properties our SulWowza plugin uses are:

- `stacksURL`
  - Required
  - the baseURL for stacks_token verifications
- `stacksConnectionTimeout`
  - stacks service connection timeout (time to establish a connection), in seconds; default is 30
- `stacksReadTimeout`
  - stacks service connection timeout (time for reading stream after connection is established), in seconds; default is 30

You can add these properties using the Wowza GUI interface: select your Wowza application, click on the properties tab;  scroll to the bottom for custom properties and add them.

Alternatively, you can manually edit Application.xml and then reload the Wowza application.  The `<Properties>` element in `Application.xml` should include something like this:

```xml
  <Property>
    <!-- stacks token verification baseURL -->
    <Name>stacksURL</Name>
    <Value>https://stacks-test.stanford.edu</Value>
  </Property>
  <Property>
    <!-- stacks service connection timeout (time to establish a connection), in seconds; default is 30 -->
    <Name>stacksConnectionTimeout</Name>
    <Value>20</Value>
    <Type>Integer</Type>
  </Property>
  <Property>
    <!-- stacks service connection timeout (time for reading stream after connection is established),
      in seconds; default is 30 -->
    <Name>stacksReadTimeout</Name>
    <Value>20</Value>
    <Type>Integer</Type>
  </Property>
```

## To deploy a new version of the plugin code to the VM

- Configuring a Wowza application to use the plugin is described above, and can happen independently of the plugin Jar deployment.
- Release a new (tagged) version of the plugin:
  - Update `conf/version`.  This should be just the version number itself, e.g. `1.7.1` or `2.0.0-beta`.
  - Create a version tag and release notes:
    - Create a Github release for the tag with brief notes about changes since the last release.  Note:  when you publish a release in Github, the tag will be created for you if it doesn't exist already.
      - The git tag's version number should use the `v` prefix, e.g. `v1.7.1` or `v2.0.0-beta` (corresponding with the `conf/version` example).
    - In order to use the `deploymentJar` gradle task (a la Jenkins), the git tag (e.g `v1.7.1`) must match the contents of `conf/version` (e.g. `1.7.1`) other than the leading v, present only in the git tag.
  - Note: if you do choose to tag from the command line, don't forget to push tags to Github.
- Trigger the Jenkins build (`wowza-auth-plugin`) that creates the versioned .jar artifact for deployment.
  - Log into Jenkins, navigate to the 'wowza-auth-plugin' project, and invoke the 'Build Now' command.
  - Note:
    - the Jenkins build is configured to use the master branch;  the jar will have the code from the last commit to the master branch.
    - the jar created will be copied to an artifacts directory on jenkins, from which puppet will get the versioned jar.
    - TODO:  build the jar from the tagged branch indicated in conf/version (see issue #23)
- Update puppet to deploy the specific versioned artifact to the desired VM.
  - make a PR in puppet to do this, following puppet practices.  (or ask your friendly devops rep to help)

## To deploy locally (e.g. on dev laptop)

- install Wowza on laptop
- create Wowza application instance
- ensure Wowza application instance has properties above
  - example Application.xml file  at  conf/example/Application.xml
- build the jar using `./gradlew deploymentJar` or `./gradlew deploymentJarRelaxed`  (the latter is needed if `conf/version` doesn't match the head commit in the current branch)
- copy jar to /your/wowza/lib
- start up Wowza

## More useful Gradle pointers

### Get rid of existing build artifacts

```sh
./gradlew clean
```

Can be combined with other tasks, e.g. `./gradlew clean check`.

### To see Gradle tasks (with descriptions)

```sh
./gradlew tasks --all
```

### For help with Gradle

```sh
./gradlew --help
```


# Deprecated build instructions (possibly useful for troubleshooting, eventually remove?)

## To manually compile

```sh
javac -d classes -cp .:libs/wms-restlet-2.2.2.jar:libs/wms-server.jar:libs/wms-httpstreamer-cupertinostreaming.jar:libs/wms-httpstreamer-mpegdashstreaming.jar:libs/log4j-1.2.17.jar:libs/junit.jar src/edu/stanford/dlss/wowza/SulWowza.java src/edu/stanford/dlss/wowza/SulWowzaTester.java
```

## To manually create jar

```sh
jar cf dlss-wowza.jar classes/edu/stanford/dlss/wowza/SulWowza* conf
```
