package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

import org.apache.log4j.*;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;

import java.io.ByteArrayOutputStream;

public class SulWowzaTester
{

    @Test
    public void testOnAppStartBadUrl()
    {
        // the only visible result of onAppStart is a log message
        // logger is the rootLogger, per test/resources/log4j.properties
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

        try
        {
            testModule.onAppStart(appInstanceMock);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("Unable to initialize"),
                                     containsString("java.net.MalformedURLException"), 
                                     containsString("no protocol")));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void testOnAppStartDefaultUrl()
    {
        // the only visible result of onAppStart is a log message
        // logger is the rootLogger, per test/resources/log4j.properties
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

        try
        {
            testModule.onAppStart(appInstanceMock);
            String logMsg = out.toString();
            assertThat(logMsg, containsString(
                    "Initialized " + testModule.getClass().getSimpleName() + " at " + testModule.defaultUrl));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void testOnAppStartValidPropertyUrl()
    {
        // the only visible result of onAppStart is a log message
        // logger is the rootLogger, per test/resources/log4j.properties
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

        try
        {
            testModule.onAppStart(appInstanceMock);
            String logMsg = out.toString();
            assertThat(logMsg,
                    containsString("Initialized " + testModule.getClass().getSimpleName() + " at http://example.org"));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void testOnHTTPMPEGDashStreamingSessionCreate()
    {
        SulWowza testModule = new SulWowza();
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionCupertino sessionMock = mock(HTTPStreamerSessionCupertino.class);
        spyModule.onHTTPCupertinoStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
    }

    @Test
    public void testOnHTTPCupertinoStreamingSessionCreate()
    {
        SulWowza testModule = new SulWowza();
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionMPEGDash sessionMock = mock(HTTPStreamerSessionMPEGDash.class);
        spyModule.onHTTPMPEGDashStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
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

    @Test
    public void testAuthorize()
    {
        String queryStr = "authorize=me";
        String authToken = "token";

        SulWowza testModule = new SulWowza();
        SulWowza spyModule = spy(testModule);
        when(spyModule.getAuthToken(queryStr)).thenReturn(authToken);

        spyModule.authorize(queryStr);
        verify(spyModule).getAuthToken(queryStr);
        verify(spyModule).validateAuthToken(authToken);
    }

    @Test
    public void testGetAuthToken()
    {
        String queryStr = "token=encryptedStacksMediaToken";
        SulWowza testModule = new SulWowza();
        assertEquals("encryptedStacksMediaToken", testModule.getAuthToken(queryStr));
    }

    @Test
    public void testGetAuthTokenIgnoresOtherParams()
    {
        String queryStr = "ignored=ignored&token=encryptedStacksMediaToken&anything=anythingElse";
        SulWowza testModule = new SulWowza();
        assertEquals("encryptedStacksMediaToken", testModule.getAuthToken(queryStr));
    }

    @Test
    public void testGetAuthTokenEmptyTokenParam()
    {
        String queryStr = "ignored=ignored&token=&anything=anythingElse";
        SulWowza testModule = new SulWowza();
        assertEquals("", testModule.getAuthToken(queryStr));
    }

    @Test
    public void testGetAuthTokenMissingTokenParam()
    {
        String queryStr = "ignored=ignored&anything=anythingElse";
        SulWowza testModule = new SulWowza();
        assertNull(testModule.getAuthToken(queryStr));
    }

    @Test
    public void testGetAuthTokenEmptyQueryStr()
    {
        SulWowza testModule = new SulWowza();
        assertNull(testModule.getAuthToken(""));
    }

    @Test
    public void testGetAuthTokenNullQueryStr()
    {
        SulWowza testModule = new SulWowza();
        assertNull(testModule.getAuthToken(null));
    }

    // @Test
    public void testValidateAuthToken()
    {
        // TODO: need to implement this method first
        // tests for:
        // token is null (?) (missing param? missing value?)
        // token is empty string
        // user IP is missing
        // user IP is wrong
        // druid is missing
        // druid is wrong
        // filename is wrong
        // filename is wrong
        // token is expired
        fail("Not yet implemented");
    }
}
