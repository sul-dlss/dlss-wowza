package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import org.apache.log4j.*;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

import java.io.ByteArrayOutputStream;

public class SulWowzaTester {

	@Test
	public void testOnAppStartBadUrl()
	{
		// the only visible result of onAppStart is a log message
		//  logger is the rootLogger, per test/resources/log4j.properties
		Logger logger = Logger.getRootLogger();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Layout layout = new SimpleLayout();
		Appender appender = new WriterAppender(layout, out);
		logger.addAppender(appender);

		SulWowza testModule = new SulWowza();

		WMSProperties mockProperties = mock(WMSProperties.class);
		when(mockProperties.getPropertyStr("stacksURL", testModule.defaultUrl)).thenReturn("badUrl");
		IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
		when(appInstanceMock.getProperties()).thenReturn(mockProperties);

		try {
			testModule.onAppStart(appInstanceMock);
			String logMsg = out.toString();
			assertThat(logMsg, allOf(containsString("Unable to initialize"),
									containsString("java.net.MalformedURLException"),
									containsString("no protocol")));
		} finally {
			logger.removeAppender(appender);
		}
	}

	@Test
	public void testOnAppStartDefaultUrl()
	{
		// the only visible result of onAppStart is a log message
		//  logger is the rootLogger, per test/resources/log4j.properties
		Logger logger = Logger.getRootLogger();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Layout layout = new SimpleLayout();
		Appender appender = new WriterAppender(layout, out);
		logger.addAppender(appender);

		SulWowza testModule = new SulWowza();

		WMSProperties mockProperties = mock(WMSProperties.class);
		when(mockProperties.getPropertyStr("stacksURL", testModule.defaultUrl)).thenReturn(testModule.defaultUrl);
		IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
		when(appInstanceMock.getProperties()).thenReturn(mockProperties);

		try {
			testModule.onAppStart(appInstanceMock);
			String logMsg = out.toString();
			assertThat(logMsg, containsString("Initialized " + testModule.getClass().getSimpleName() + " at " + testModule.defaultUrl));
		} finally {
			logger.removeAppender(appender);
		}
	}

	@Test
	public void testOnAppStartValidPropertyUrl()
	{
		// the only visible result of onAppStart is a log message
		//  logger is the rootLogger, per test/resources/log4j.properties
		Logger logger = Logger.getRootLogger();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Layout layout = new SimpleLayout();
		Appender appender = new WriterAppender(layout, out);
		logger.addAppender(appender);

		SulWowza testModule = new SulWowza();

		WMSProperties mockProperties = mock(WMSProperties.class);
		when(mockProperties.getPropertyStr("stacksURL", testModule.defaultUrl)).thenReturn("http://example.org");
		IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
		when(appInstanceMock.getProperties()).thenReturn(mockProperties);

		try {
			testModule.onAppStart(appInstanceMock);
			String logMsg = out.toString();
			assertThat(logMsg, containsString("Initialized " + testModule.getClass().getSimpleName() + " at http://example.org"));
		} finally {
			logger.removeAppender(appender);
		}
	}

//	@Test
	public void testOnHTTPMPEGDashStreamingSessionCreate()
	{
		fail("Not yet implemented");
	}

//	@Test
	public void testOnHTTPCupertinoStreamingSessionCreate()
	{
		fail("Not yet implemented");
	}

	@Test
	public void testAuthorizeSessionAcceptsIfAuthorized()
	{
		String queryStr = "authorized=yes";

		SulWowza testModule = new SulWowza();
		SulWowza spyModule = spy(testModule);
		when(spyModule.authorize(queryStr)).thenReturn(true);

		IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
		when(sessionMock.getQueryStr()).thenReturn(queryStr);

		spyModule.authorizeSession(sessionMock);

		verify(sessionMock).acceptSession();
	}

	@Test
	public void testAuthorizeSessionRejectsIfNotAuthorized()
	{
		String queryStr = "authorized=no";

		SulWowza testModule = new SulWowza();
		SulWowza spyModule = spy(testModule);
		when(spyModule.authorize(queryStr)).thenReturn(false);

		IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
		when(sessionMock.getQueryStr()).thenReturn(queryStr);

		spyModule.authorizeSession(sessionMock);

		verify(sessionMock).rejectSession();
	}

//	@Test
	public void testAuthorize()
	{
		fail("Not yet implemented");
	}

//	@Test
	public void testGetAuthToken()
	{
		fail("Not yet implemented");
	}

//	@Test
	public void testValidateAuthToken()
	{
		fail("Not yet implemented");
	}
}
