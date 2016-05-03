[![Build Status](https://travis-ci.org/sul-dlss/dlss-wowza.svg?branch=master)](https://travis-ci.org/sul-dlss/dlss-wowza)

# dlss-wowza
Wowza server side modules.  Uses Gradle (with the Gradle wrapper) as the build tool; akin to Ruby's rake.

### To compile and run tests:

  ./gradlew build

### To deploy:

- (TODO: use gradle to create a jar with the dlss-wowza classes we need)
- (TODO: write deployment scripts for putting jar from a released build into deployed wowza server)
- (TODO: determine how to deploy conf/Application.xml file and possibly other Wowza application specific info.  Also:  is any of it private?  Should that config info be in a private github repo?)

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
