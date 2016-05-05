package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import org.apache.log4j.*;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;

import java.io.ByteArrayOutputStream;

public class TestSulWowza
{
    SulWowza testModule;
    static String stacksToken = "encryptedStacksMediaToken";
    static String queryStr = "stacks_token=" + stacksToken;

    @Before
    public void setUp()
    {
        testModule = new SulWowza();
    }

    @Test
    public void onAppStart_badUrl_logsError()
    {
        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        String badUrlStr = "badUrl";
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(badUrlStr);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals(badUrlStr, SulWowza.stacksTokenVerificationBaseUrl);

        try
        {
            testModule.onAppStart(appInstanceMock);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("unable to initialize"),
                                     containsString("java.net.MalformedURLException"),
                                     containsString("no protocol")));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void onAppStart_defaultUrl()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL, SulWowza.stacksTokenVerificationBaseUrl);
    }

    @Test
    public void onAppStart_validPropertyUrl()
    {
        String exampleUrl = "http://example.org";
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(exampleUrl);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals(exampleUrl, SulWowza.stacksTokenVerificationBaseUrl);
    }

    @Test
    public void onHTTPMPEGDashStreamingSessionCreate()
    {
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionCupertino sessionMock = mock(HTTPStreamerSessionCupertino.class);
        spyModule.onHTTPCupertinoStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
    }

    @Test
    public void onHTTPCupertinoStreamingSessionCreate()
    {
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionMPEGDash sessionMock = mock(HTTPStreamerSessionMPEGDash.class);
        spyModule.onHTTPMPEGDashStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
    }

    @Test
    public void authorizeSession_acceptsIfAuthorized()
    {
        String filename = "ignored";
        String streamName = "oo/000/oo/0000/" + filename;
        String druid = "oo000oo0000";
        String userIp = "ignored";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn(userIp);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        when(spyModule.verifyStacksToken(stacksToken, druid, filename, userIp)).thenReturn(true);

        spyModule.authorizeSession(sessionMock);
        verify(sessionMock).acceptSession();
    }

    @Test
    public void authorizeSession_rejectsIfNotAuthorized()
    {
        String filename = "ignored";
        String streamName = "oo/000/oo/0000/" + filename;
        String druid = "oo000oo0000";
        String userIp = "ignored";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn(userIp);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        when(spyModule.verifyStacksToken(stacksToken, druid, filename, userIp)).thenReturn(false);

        spyModule.authorizeSession(sessionMock);
        verify(sessionMock).rejectSession();
    }

    @Test
    public void authorizeSession_getsStacksToken()
    {
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).getStacksToken(anyString());
    }

    @Test
    public void authorizeSession_validatesStacksToken()
    {
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).validateStacksToken(stacksToken);
    }

    @Test
    public void authorizeSession_getsUserIp()
    {
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(sessionMock).getIpAddress();
    }

    @Test
    public void authorizeSession_validatesUserIp()
    {
        String ipAddr = "anIpAddr";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn(ipAddr);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).validateUserIp(ipAddr);
    }

    @Test
    public void authorizeSession_validatesStreamName()
    {
        String streamName = "anything";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn("ignored");
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).validateStreamName(streamName);
    }

    @Test
    public void authorizeSession_getsDruidFromStreamName()
    {
        String streamName = "aa/123/bb/1234/e";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn("anIpAddr");
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).getDruid(streamName);
    }

    @Test
    public void authorizeSession_rejectsForNullDruid()
    {
        String streamName = "aa/123/bb/1234/filename.ext";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        when(spyModule.getDruid(streamName)).thenReturn(null);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule, never()).verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(sessionMock).rejectSession();
    }

    @Test
    public void authorizeSession_getsFilenameFromStreamName()
    {
        String streamName = "aa/123/bb/1234/filename.ext";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getIpAddress()).thenReturn("anIpAddr");
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).getFilename(streamName);
    }

    @Test
    public void authorizeSession_rejectsForNullFilename()
    {
        String streamName = "aa/123/bb/1234/filename.ext";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        when(spyModule.getFilename(streamName)).thenReturn(null);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule, never()).verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(sessionMock).rejectSession();
    }

    @Test
    /** verifyStacksToken(4 strings) calls getVerifyStacksTokenUrl */
    public void verifyStacksToken_getsVerifyStacksTokenUrl()
    {
        SulWowza spyModule = spy(testModule);
        spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(spyModule).getVerifyStacksTokenUrl(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    /** verifyStacksToken(4 strings) calls verifyStacksToken(UrlStr) */
    public void verifyStacksToken_verifiesStacksTokenUrl()
    {
        SulWowza spyModule = spy(testModule);
        spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(spyModule).verifyTokenAgainstStacksService(anyString());
    }

    @Test
    /** if the result to getVerifyStacksTokenUrl is null, don't call verifyStacksToken(urlStr) and return false */
    public void verifyStacksToken_rejectsForNullUrl()
    {
        SulWowza spyModule = spy(testModule);
        when(spyModule.getVerifyStacksTokenUrl(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        boolean result = spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        assertFalse(result);
        verify(spyModule, never()).verifyTokenAgainstStacksService(anyString());
    }

    @Test
    public void getStacksToken_singleParam()
    {
        assertEquals(stacksToken, testModule.getStacksToken(queryStr));
    }

    @Test
    public void getStacksToken_ignoresOtherParams()
    {
        String myQueryStr = "ignored=ignored&stacks_token=" + stacksToken + "&anything=anythingElse";
        assertEquals(stacksToken, testModule.getStacksToken(myQueryStr));
    }

    @Test
    public void getStacksToken_emptyTokenParam()
    {
        String myQueryStr = "ignored=ignored&stacks_token=&anything=anythingElse";
        assertEquals("", testModule.getStacksToken(myQueryStr));
    }

    @Test
    public void getStacksToken_missingTokenParam()
    {
        String myQueryStr = "ignored=ignored&anything=anythingElse";
        assertNull(testModule.getStacksToken(myQueryStr));
    }

    @Test
    public void getStacksToken_emptyQueryStr()
    {
        assertNull(testModule.getStacksToken(""));
    }

    @Test
    public void getStacksToken_nullQueryStr()
    {
        SulWowza testModule = new SulWowza();
        assertNull(testModule.getStacksToken(null));
    }

    //@Test
    public void getStacksToken_weirdChars()
    {
        // stacks provides a token that theoretically could have chars needing url encoding
        //  does our code get those chars properly?
        //  does it properly send them back to stacks/verify_token ?
        fail("what if stacks tokens have chars that are utf-8 or need to be url encoded?");
    }

    @Test
    public void validateStacksToken_goodEnough()
    {
        assertTrue(testModule.validateStacksToken(stacksToken));
    }

    @Test
    public void validateStacksToken_nullValue()
    {
        assertFalse(testModule.validateStacksToken(null));
    }

    @Test
    public void validateStacksToken_emptyString()
    {
        assertFalse(testModule.validateStacksToken(""));
    }

    @Test
    public void validateStacksToken_shortString()
    {
        assertFalse(testModule.validateStacksToken("too_short"));
    }

    @Test
    public void validateStacksToken_logsError()
    {
        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            String shortToken = "too_short";
            testModule.validateStacksToken(shortToken);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString(" stacksToken missing or implausibly short: "),
                                     containsString(shortToken)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void validateUserIp_goodEnough()
    {
        assertTrue(testModule.validateUserIp("1.1.1.1"));
    }

    @Test
    public void validateUserIp_nullValue()
    {
        assertFalse(testModule.validateUserIp(null));
    }

    @Test
    public void validateUserIp_emptyString()
    {
        assertFalse(testModule.validateUserIp(""));
    }

    @Test
    public void validateUserIp_shortString()
    {
        assertFalse(testModule.validateUserIp("short"));
    }

    @Test
    public void validateUserIp_logsError()
    {
        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            String shortIp = "short";
            testModule.validateUserIp(shortIp);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("User IP missing or implausibly short"),
                                     containsString(shortIp)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void validateStreamName_goodEnough()
    {
        assertTrue(testModule.validateStreamName("oo/00/oo/0000/filename.ext"));
    }

    @Test
    public void validateStreamName_nullValue()
    {
        assertFalse(testModule.validateStreamName(null));
    }

    @Test
    public void validateStreamName_emptyString()
    {
        assertFalse(testModule.validateStreamName(""));
    }

    @Test
    public void validateStreamName_shortString()
    {
        assertFalse(testModule.validateStreamName("short"));
    }

    @Test
    public void validateStreamName_not4Slashes()
    {
        assertFalse(testModule.validateStreamName("oo/00/oo/0000/extra/stream.mp4"));
        assertFalse(testModule.validateStreamName("a/a/a/a/a"));
        assertFalse(testModule.validateStreamName("stream.mp4"));
        assertFalse(testModule.validateStreamName("oo/00/oo/0000"));
        assertFalse(testModule.validateStreamName(""));
        assertFalse(testModule.validateStreamName(null));
    }

    @Test
    public void validateStreamName_logsError()
    {
        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            String badStreamName = "short";
            testModule.validateStreamName(badStreamName);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString(" streamName missing or implausibly short: "),
                                     containsString(badStreamName)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void getDruid()
    {
        assertEquals("oo000oo0000", testModule.getDruid("oo/000/oo/0000/stream.mp4")); // oo000oo000 has valid druid format
        assertEquals("oo000oo0000", testModule.getDruid("oo/000/oo/0000/a"));
        assertNull(testModule.getDruid("oo/00/oo/0000/"));  // oo00oo0000 is not a valid druid format
        assertNull(testModule.getDruid("a/b/c/d/anything.without%slash"));  // abcd is not a valid druid format
        assertNull(testModule.getDruid("a/b/c/d/"));
    }

    @Test
    public void getFilename()
    {
        assertEquals("stream.mp4", testModule.getFilename("oo/00/oo/0000/stream.mp4"));
        assertEquals("anything.without%slash", testModule.getFilename("a/b/c/d/anything.without%slash"));
        assertNull(testModule.getFilename("a/b/c/d/"));
    }

    @Test
    public void verifyTokenAgainstStacksService_validUrl()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(anyString(), anyString())).thenReturn(SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        String druid = "oo000oo0000";
        String filename = "filename.ext";
        String userIp = "0.0.0.0";
        String expPath = "/media/" + druid + "/" + filename + "/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=" + userIp;
        String expUrl = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + expPath + expQueryStr;

        String resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
        assertEquals(expUrl, resultUrl);
    }

    @Test
    /** expect it to return null and log an error message */
    public void verifyTokenAgainstStacksService_malformedUrl()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(anyString(), anyString())).thenReturn("badUrl");
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            String resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, "oo000oo0000", "filename.ext", "0.0.0.0");
            assertNull(resultUrl);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(" bad URL for stacks_token verification:"),
                                     containsString(" java.net.MalformedURLException"),
                                     containsString(" no protocol")));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    //@Test
    public void verifyTokenAgainstStacksService_wUrlString_logsRequestMade()
    {
        fail("don't know how to test this without spinning up a local stacks instance");

        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:3000" + expPath + expQueryStr;

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            boolean result = testModule.verifyTokenAgainstStacksService(urlStr);
            assertTrue(result);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("INFO"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("verify_token request made to"),
                                     containsString(urlStr)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    /** it logs an error and returns false */
    public void verifyTokenAgainstStacksService_wUrlString_IOException()
    {
        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:6666" + expPath + expQueryStr;

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            boolean result = testModule.verifyTokenAgainstStacksService(urlStr);
            assertFalse(result);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("unable to verify stacks token at"),
                                     containsString(urlStr)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

}
