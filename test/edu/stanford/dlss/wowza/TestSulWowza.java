package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import org.apache.log4j.*;

import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;
import com.wowza.wms.request.RequestFunction;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class TestSulWowza
{
    SulWowza testModule;
    NoticeReporter mockNoticeReporter;
    final static String stacksToken = "encryptedStacksMediaToken";
    final static String queryStr = "stacks_token=" + stacksToken;

    @Before
    public void setUp()
    {
        testModule = new SulWowza();
        mockNoticeReporter = mock(HoneybadgerReporter.class);
        testModule.setNoticeReporter(mockNoticeReporter);
    }

    @Test
    public void onAppStart_calls_setStacksConnectionTimeout()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("test_key");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).setStacksConnectionTimeout(appInstanceMock);
    }

    @Test
    public void onAppStart_calls_setStacksReadTimeout()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("test_key");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).setStacksReadTimeout(appInstanceMock);
    }

    @Test
    public void onAppStart_calls_getStacksUrl()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("test_key");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).getStacksUrl(appInstanceMock);
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
    public void onAppStart_badUrl_setsInvalidConfiguration()
    {
        String badUrlStr = "badUrl";
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(badUrlStr);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals(badUrlStr, SulWowza.stacksTokenVerificationBaseUrl);
        assertTrue(testModule.invalidConfiguration);
    }

    @Test
    /** not sure if getPropertyStr("propname", actual_value) can return empty string, but just in case */
    public void onAppStart_urlEmptyStr_setsInvalidConfiguration()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn("");
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals("", SulWowza.stacksTokenVerificationBaseUrl);
        assertTrue(testModule.invalidConfiguration);
    }

    @Test
    /** not sure if getPropertyStr("propname", actual_value) can return null, but just in case */
    public void onAppStart_urlNull_setsInvalidConfiguration()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(null);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        assertEquals(null, SulWowza.stacksTokenVerificationBaseUrl);
        assertTrue(testModule.invalidConfiguration);
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
    public void onAppStart_badUrl_callsHoneybadger()
    {
        MalformedURLException mue = new MalformedURLException("no protocol: badUrl");
        SulWowza spyModule = spy(testModule);
        String badUrlStr = "badUrl";
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenReturn(badUrlStr);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).reportNotice(anyString(), any());
    }

    @Test
    public void onAppStart_setsUpUncaughtExceptionHandler()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("test_key");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).registerUncaughtExceptionHandler();
    }

    @Test
    public void onAppStart_initializesHoneybadger()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("test_key");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        spyModule.onAppStart(appInstanceMock);
        verify(spyModule).initHoneybadger(appInstanceMock);
    }

    @Test
    public void initHoneybadger_failsWithoutAPIKey()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn(null);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza localTestModule = new SulWowza();
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        localTestModule.initHoneybadger(appInstanceMock);
        assertTrue(localTestModule.invalidConfiguration);
    }

    @Test
    public void initHoneybadger_failsWithoutHoneybadgerEnv()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("abcd");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn(null);

        SulWowza localTestModule = new SulWowza();
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        localTestModule.initHoneybadger(appInstanceMock);
        assertTrue(localTestModule.invalidConfiguration);
    }

    @Test
    public void initHoneybadger_succeedsWithAPIKeyAndHoneybadgerEnv()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("abcd");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        SulWowza localTestModule = new SulWowza();
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        localTestModule.initHoneybadger(appInstanceMock);
        assertFalse(localTestModule.invalidConfiguration);
    }


    @Test
    public void registerUncaughtExceptionHandler_registersHoneybadger()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_API_KEY_ENV_VAR)).thenReturn("abcd");
        when(mockProperties.getPropertyStr(SulWowza.HONEYBADGER_ENV_NAME_ENV_VAR)).thenReturn("test_env");

        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        testModule.initHoneybadger(appInstanceMock);
        testModule.registerUncaughtExceptionHandler();

        HoneybadgerUncaughtExceptionHandler curThreadUncaughtExceptionHandler =
            (HoneybadgerUncaughtExceptionHandler) (Thread.getDefaultUncaughtExceptionHandler());
        assertThat(curThreadUncaughtExceptionHandler, instanceOf(HoneybadgerUncaughtExceptionHandler.class));
    }

    @Test
    public void onHTTPCupertinoStreamingSessionCreate_calls_authorizeSession_ifValidConfiguration()
    {
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionCupertino sessionMock = mock(HTTPStreamerSessionCupertino.class);
        spyModule.invalidConfiguration = false;
        spyModule.onHTTPCupertinoStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
    }

    @Test
    public void onHTTPCupertinoStreamingSessionCreate_rejectsSession_ifInvalidConfiguration()
    {
        testModule.invalidConfiguration = true;
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionCupertino sessionMock = mock(HTTPStreamerSessionCupertino.class);
        testModule.onHTTPCupertinoStreamingSessionCreate(sessionMock);

        verify(sessionMock).rejectSession();
        verify(spyModule, never()).authorizeSession(sessionMock);
    }

    @Test
    public void onHTTPMPEGDashStreamingSessionCreate_calls_authorizeSession_ifValidConfiguration()
    {
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionMPEGDash sessionMock = mock(HTTPStreamerSessionMPEGDash.class);
        spyModule.invalidConfiguration = false;
        spyModule.onHTTPMPEGDashStreamingSessionCreate(sessionMock);

        verify(spyModule).authorizeSession(sessionMock);
    }

    @Test
    public void onHTTPMPEGDashStreamingSessionCreate_rejectsSession_ifInvalidConfiguration()
    {
        testModule.invalidConfiguration = true;
        SulWowza spyModule = spy(testModule);
        HTTPStreamerSessionMPEGDash sessionMock = mock(HTTPStreamerSessionMPEGDash.class);
        spyModule.onHTTPMPEGDashStreamingSessionCreate(sessionMock);

        verify(sessionMock).rejectSession();
        verify(spyModule, never()).authorizeSession(sessionMock);
    }

    @Test
    public void play_shutsDownClient_ifInvalidConfiguration()
    {
        testModule.invalidConfiguration = true;
        SulWowza spyModule = spy(testModule);
        IClient clientMock = mock(IClient.class);
        RequestFunction rfMock = mock(RequestFunction.class);
        AMFDataList amfMock = mock(AMFDataList.class);
        ApplicationInstance appInstanceMock = mock(ApplicationInstance.class);
        when(clientMock.getAppInstance()).thenReturn(appInstanceMock);
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("streamName");

        spyModule.play(clientMock, rfMock, amfMock);
        verify(clientMock).shutdownClient();
        verify(spyModule, never()).authorizePlay(null, null, "streamName");
    }

    @Test
    public void play_calls_AuthorizePlay_ifValidConfiguration()
    {
        testModule.invalidConfiguration = false;
        SulWowza spyModule = spy(testModule);
        IClient clientMock = mock(IClient.class);
        RequestFunction rfMock = mock(RequestFunction.class);
        AMFDataList amfMock = mock(AMFDataList.class);
        ApplicationInstance appInstanceMock = mock(ApplicationInstance.class);
        when(clientMock.getAppInstance()).thenReturn(appInstanceMock);
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("streamName");
        when(clientMock.getQueryStr()).thenReturn("queryStr");

        spyModule.play(clientMock, rfMock, amfMock);
        verify(spyModule).authorizePlay("queryStr", null, "streamName");
    }

    @Test
    public void play_getsClientIp()
    {
        testModule.invalidConfiguration = false;
        SulWowza spyModule = spy(testModule);
        IClient clientMock = mock(IClient.class);
        RequestFunction rfMock = mock(RequestFunction.class);
        AMFDataList amfMock = mock(AMFDataList.class);
        ApplicationInstance appInstanceMock = mock(ApplicationInstance.class);
        when(clientMock.getAppInstance()).thenReturn(appInstanceMock);
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("streamName");

        spyModule.play(clientMock, rfMock, amfMock);
        verify(clientMock).getIp();
    }

    @Test
    public void play_usesStreamNameIfNoStacksTokenFromQueryStr()
    {
        String streamName = "oo/000/oo/0000/stream.mp4?queryStr";
        testModule.invalidConfiguration = false;
        SulWowza spyModule = spy(testModule);
        IClient clientMock = mock(IClient.class);
        RequestFunction rfMock = mock(RequestFunction.class);
        AMFDataList amfMock = mock(AMFDataList.class);
        ApplicationInstance appInstanceMock = mock(ApplicationInstance.class);
        when(clientMock.getAppInstance()).thenReturn(appInstanceMock);
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn(streamName);
        when(spyModule.getStacksToken(anyString())).thenReturn(null);

        spyModule.play(clientMock, rfMock, amfMock);
        verify(spyModule).authorizePlay(streamName, null, streamName);
    }

    @Test
    public void setStacksConnectionTimeout_validPropertyValue()
    {
        int exampleValue = 5;
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksConnectionTimeout", SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT)).thenReturn(exampleValue);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksConnectionTimeout(appInstanceMock);
        assertEquals(exampleValue, SulWowza.stacksConnectionTimeout);
    }

    @Test
    public void setStacksConnectionTimeout_defaultValue()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksConnectionTimeout", SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT)).thenReturn(SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksConnectionTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT, SulWowza.stacksConnectionTimeout);
    }

    @Test
    public void setStacksConnectionTimeout_negativePropertyValue_revertsToDefault()
    {
        int exampleValue = -4;
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksConnectionTimeout", SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT)).thenReturn(exampleValue);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksConnectionTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT, SulWowza.stacksConnectionTimeout);
    }

    @Test
    public void setStacksConnectionTimeout_revertsToDefault_ifExceptionThrown()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksConnectionTimeout", SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT)).thenThrow(new java.lang.ClassCastException());
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksConnectionTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT, SulWowza.stacksConnectionTimeout);
    }

    @Test
    public void setStacksConnectionTimeout_callsHoneybadger_ifExceptionThrown()
    {
        ClassCastException cce = new java.lang.ClassCastException();
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksConnectionTimeout", SulWowza.DEFAULT_STACKS_CONNECTION_TIMEOUT)).thenThrow(cce);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        SulWowza spyModule = spy(testModule);
        spyModule.setStacksConnectionTimeout(appInstanceMock);
        verify(spyModule).reportNotice(spyModule.getClass().getSimpleName() +
                                       " unable to read stacksConnectionTimeout from properties; using default ",
                                       cce);
    }

    @Test
    public void setStacksReadTimeout_validPropertyValue()
    {
        int exampleValue = 5;
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksReadTimeout", SulWowza.DEFAULT_STACKS_READ_TIMEOUT)).thenReturn(exampleValue);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksReadTimeout(appInstanceMock);
        assertEquals(exampleValue, SulWowza.stacksReadTimeout);
    }

    @Test
    public void setStacksReadTimeout_defaultValue()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksReadTimeout", SulWowza.DEFAULT_STACKS_READ_TIMEOUT)).thenReturn(SulWowza.DEFAULT_STACKS_READ_TIMEOUT);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksReadTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_READ_TIMEOUT, SulWowza.stacksReadTimeout);
    }

    @Test
    public void setStacksReadTimeout_negativePropertyValue_revertsToDefault()
    {
        int exampleValue = -4;
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksReadTimeout", SulWowza.DEFAULT_STACKS_READ_TIMEOUT)).thenReturn(exampleValue);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksReadTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_READ_TIMEOUT, SulWowza.stacksReadTimeout);
    }

    @Test
    public void setStacksReadTimeout_revertsToDefault_ifExceptionThrown()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksReadTimeout", SulWowza.DEFAULT_STACKS_READ_TIMEOUT)).thenThrow(new java.lang.ClassCastException());
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.setStacksReadTimeout(appInstanceMock);
        assertEquals(SulWowza.DEFAULT_STACKS_READ_TIMEOUT, SulWowza.stacksReadTimeout);
    }

    @Test
    public void setStacksReadTimeout_callsHoneybadger_ifExceptionThrown()
    {
        ClassCastException cce = new ClassCastException();
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyInt("stacksReadTimeout", SulWowza.DEFAULT_STACKS_READ_TIMEOUT)).thenThrow(cce);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        SulWowza spyModule = spy(testModule);
        spyModule.setStacksReadTimeout(appInstanceMock);
        verify(spyModule).reportNotice(spyModule.getClass().getSimpleName() +
                                       " unable to read stacksReadTimeout from properties; using default ",
                                       cce);
    }

    @Test
    public void getStacksUrl_returnsEmptyString_ifExceptionThrown()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenThrow(new java.lang.ClassCastException());
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        assertEquals("", testModule.getStacksUrl(appInstanceMock));
    }

    @Test
    public void getStacksUrl_callsHoneybadger_ifExceptionThrown()
    {
        ClassCastException cce = new ClassCastException();
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenThrow(cce);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);

        SulWowza spyModule = spy(testModule);
        spyModule.getStacksUrl(appInstanceMock);
        verify(spyModule).reportNotice(spyModule.getClass().getSimpleName() +
                                       " unable to read stacksURL from properties ",
                                       cce);
    }

    @Test
    public void authorizeSession_acceptsIfAuthorized()
    {
        String filename = "ignored";
        String streamName = "oo/000/oo/0000/" + filename;
        String druid = "oo000oo0000";
        String userIp = "1.1.1.1";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        String xForwardedFor = "1.1.1.1, 2.2.2.2, 3.3.3.3";
        mockHttpHeaderMap.put("x-forwarded-for", xForwardedFor);
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        doReturn(true).when(spyModule).verifyStacksToken(stacksToken, druid, filename, userIp);

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
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", userIp);
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        when(sessionMock.getStreamName()).thenReturn(streamName);
        SulWowza spyModule = spy(testModule);
        doReturn(false).when(spyModule).verifyStacksToken(stacksToken, druid, filename, userIp);

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
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        Map<String, String> spyHttpHeaderMap = spy(mockHttpHeaderMap);
        when(sessionMock.getHTTPHeaderMap()).thenReturn(spyHttpHeaderMap);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(sessionMock).getHTTPHeaderMap();
        verify(spyHttpHeaderMap).get("x-forwarded-for");
    }

    @Test
    public void authorizeSession_validatesUserIp()
    {
        String ipAddr = "userIpAddr";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", ipAddr);
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        SulWowza spyModule = spy(testModule);

        spyModule.authorizeSession(sessionMock);
        verify(spyModule).validateUserIp(ipAddr);
    }

    @Test
    public void validatesUserIp_callsHoneybadgerWhenIpIsInvalid()
    {
        SulWowza spyModule = spy(testModule);
        spyModule.validateUserIp(null);
        verify(spyModule).reportNotice(spyModule.getClass().getSimpleName() + ": User IP missing or invalid");
    }

    @Test
    public void authorizeSession_validatesStreamName()
    {
        String streamName = "anything";
        IHTTPStreamerSession sessionMock = mock(IHTTPStreamerSession.class);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", "127.6.6.6");  // userIp
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
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
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", "127.6.6.6"); // userIp
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
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
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", "127.6.6.6"); // userIp
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
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
    public void authorizePlay_trueIfAuthorized()
    {
        String filename = "ignored";
        String streamName = "oo/000/oo/0000/" + filename;
        String druid = "oo000oo0000";
        String queryString = "query";
        String userIp = "1.1.1.1";
        String token = "abcd";
        SulWowza spyModule = spy(testModule);

        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);
        when(spyModule.getDruid(streamName)).thenReturn(druid);
        when(spyModule.getFilename(streamName)).thenReturn(filename);
        doReturn(true).when(spyModule).verifyStacksToken(token, druid, filename, userIp);

        assertEquals(true, spyModule.authorizePlay(queryString, userIp, streamName));
    }

    @Test
    public void authorizePlay_falseIfNotAuthorized()
    {
        String filename = "ignored";
        String streamName = "oo/000/oo/0000/" + filename;
        String druid = "oo000oo0000";
        String queryString = "query";
        String userIp = "1.1.1.1";
        String token = "abcd";
        SulWowza spyModule = spy(testModule);

        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);
        when(spyModule.getDruid(streamName)).thenReturn(druid);
        when(spyModule.getFilename(streamName)).thenReturn(filename);
        doReturn(false).when(spyModule).verifyStacksToken(token, druid, filename, userIp);

        assertEquals(false, spyModule.authorizePlay(queryString, userIp, streamName));
    }

    @Test
    public void authorizePlay_validatesStacksToken()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "stream.mp4";
        String token = "abcd";

        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        doReturn(true).when(spyModule).validateUserIp("1");

        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).validateStacksToken(token);
    }

    @Test
    public void authorizePlay_validatesUserIp()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "stream.mp4";
        String token = "abcd";

        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);

        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).validateUserIp(userIp);
    }

    @Test
    public void authorizePlay_validatesStreamName()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "stream.mp4";
        String token = "abcd";

        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);

        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).validateStreamName(streamName);
    }

    @Test
    public void authorizePlay_falseIfNullDruid()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "aa/123/bb/1234/filename.ext";
        String token = "abcd";

        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);
        when(spyModule.getDruid(streamName)).thenReturn(null);

        assertEquals(false, spyModule.authorizePlay(queryString, userIp, streamName));
        verify(spyModule, never()).verifyStacksToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void authorizePlay_falseIfNullFilename()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "aa/123/bb/1234/filename.ext";
        String token = "abcd";

        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);
        when(spyModule.getFilename(streamName)).thenReturn(null);

        assertEquals(false, spyModule.authorizePlay(queryString, userIp, streamName));
        verify(spyModule, never()).verifyStacksToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void authorizePlay_getsStacksToken()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "aa/123/bb/1234/filename.ext";

        SulWowza spyModule = spy(testModule);
        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).getStacksToken(queryString);
    }

    @Test
    public void authorizePlay_getsFilenameFromStreamName()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "aa/123/bb/1234/filename.ext";
        String token = "abcd";
        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);

        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).getFilename(streamName);
    }

    @Test
    public void authorizePlay_getsDruidFromStreamName()
    {
        String queryString = "query";
        String userIp = "1.1.1.1";
        String streamName = "aa/123/bb/1234/filename.ext";
        String token = "abcd";
        SulWowza spyModule = spy(testModule);
        when(spyModule.getStacksToken(queryString)).thenReturn(token);
        when(spyModule.validateStacksToken(token)).thenReturn(true);
        when(spyModule.validateUserIp(userIp)).thenReturn(true);
        when(spyModule.validateStreamName(streamName)).thenReturn(true);

        spyModule.authorizePlay(queryString, userIp, streamName);
        verify(spyModule).getDruid(streamName);
    }
}
