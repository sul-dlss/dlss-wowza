[![Build Status](https://travis-ci.org/sul-dlss/dlss-wowza.svg?branch=master)](https://travis-ci.org/sul-dlss/dlss-wowza) [![Coverage Status](https://coveralls.io/repos/github/sul-dlss/dlss-wowza/badge.svg?branch=master)](https://coveralls.io/github/sul-dlss/dlss-wowza?branch=master)

# dlss-wowza
Wowza server side modules.  Uses Gradle (with the Gradle wrapper) as the build tool (akin to Ruby's rake).

### To compile and run tests of this code:

  ./gradlew build

### Using the SulWowza plugin

#### Adding the SulWowza plugin to your Wowza Application

In the Wowza GUI interface, select your Wowza Application, and click on the modules tab. Add a module with the class name `edu.stanford.dlss.wowza.SulWowza` (the name and description are arbitrary strings).

Alternatively, you can manually edit Application.xml and then reload the Wowza application.  The `<Modules>` element in `Application.xml` should include something like this:

```
  <Module>
    <Name>SulWowza</Name>
    <Description>SUL Authorization against Stacks</Description>
    <Class>edu.stanford.dlss.wowza.SulWowza</Class>
  </Module>
```

#### Properties settings used by SulWowza plugin

You can configure the SulWowza plugin using Wowza's GUI interface, but you will also need to add some properties.The three properties our SulWowza plugin uses are:

- stacksURL
  - Required
  - the baseURL for stacks_token verifications
- stacksConnectionTimeout
  - stacks service connection timeout (time to establish a connection), in seconds; default is 30
- stacksReadTimeout
  - stacks service connection timeout (time for reading stream after connection is established), in seconds; default is 30

You can add these properties using the Wowza GUI interface: select your Wowza application, click on the properties tab;  scroll to the bottom for custom properties and add them.

Alternatively, you can manually edit Application.xml and then reload the Wowza application.  The `<Properties>` element in `Application.xml` should include something like this:

```
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

### To deploy plugin code to VM:

- (ensure Wowza application on VM has the properties above)
- (update version file)
- (tag version on github)
- (trigger jenkins build that creates versioned deployment jar artifact)
- (update puppet script to use latest version)

### To deploy locally (e.g. on dev laptop)

- install Wowza on laptop
- create Wowza application instance
- ensure Wowza application instance has properties above
  - example Application.xml file  at  conf/example/Application.xml
- build the jar using gradlew
- deploy jar to /your/wowza/lib
- start up Wowza

### To see Gradle tasks

  ./gradlew tasks

### For help with Gradle

  ./gradlew --help


## (below to be removed from README??)

### To manually compile

```
javac -d classes -cp .:libs/wms-restlet-2.2.2.jar:libs/wms-server.jar:libs/wms-httpstreamer-cupertinostreaming.jar:libs/wms-httpstreamer-mpegdashstreaming.jar:libs/log4j-1.2.17.jar:libs/junit.jar src/edu/stanford/dlss/wowza/SulWowza.java src/edu/stanford/dlss/wowza/SulWowzaTester.java
```

### To manually create jar

```
jar cf dlss-wowza.jar classes/edu/stanford/dlss/wowza/SulWowza* conf
```
