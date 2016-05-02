# dlss-wowza
Wowza server side modules.

To build:

javac -d classes -cp .:libs/wms-restlet-2.2.2.jar:libs/wms-server.jar:libs/wms-httpstreamer-cupertinostreaming.jar:libs/wms-httpstreamer-mpegdashstreaming.jar:libs/log4j-1.2.17.jar:libs/junit.jar src/edu/stanford/dlss/wowza/SulWowza.java src/edu/stanford/dlss/wowza/SulWowzaTester.java

jar cf dlss-wowza.jar classes/edu/stanford/dlss/wowza/SulWowza* conf
