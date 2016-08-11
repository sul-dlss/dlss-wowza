package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import org.apache.log4j.*;

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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
    public void onAppStart_calls_setStacksConnectionTimeout()
    {
        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        spyModule.onAppStart(appInstanceMock);

        verify(spyModule).setStacksConnectionTimeout(appInstanceMock);
    }

    @Test
    public void onAppStart_calls_setStacksReadTimeout()
    {
        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        spyModule.onAppStart(appInstanceMock);

        verify(spyModule).setStacksReadTimeout(appInstanceMock);
    }

    @Test
    public void onAppStart_calls_getStacksUrl()
    {
        SulWowza spyModule = spy(testModule);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
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
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("stream.mp4");

        spyModule.play(clientMock, rfMock, amfMock);

        verify(clientMock).shutdownClient();
        verify(spyModule, never()).authorizePlay(null, null, "stream.mp4");
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
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("stream.mp4");

        spyModule.play(clientMock, rfMock, amfMock);

        verify(spyModule).authorizePlay(null, null, "stream.mp4");
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
        when(spyModule.verifyStacksToken(token, druid, filename, userIp)).thenReturn(true);

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
        when(spyModule.verifyStacksToken(token, druid, filename, userIp)).thenReturn(false);

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
        when(spyModule.validateUserIp("1")).thenReturn(true);

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
        when(appInstanceMock.internalResolvePlayAlias(null, clientMock)).thenReturn("stream.mp4");

        spyModule.play(clientMock, rfMock, amfMock);
        verify(clientMock).getIp();
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
    public void getStacksUrl_returnsEmptyString_ifExceptionThrown()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr("stacksURL", SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL)).thenThrow(new java.lang.ClassCastException());
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        assertEquals("", testModule.getStacksUrl(appInstanceMock));
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
        Map<String, String> mockHttpHeaderMap = new HashMap<String, String>();
        mockHttpHeaderMap.put("x-forwarded-for", userIp);
        when(sessionMock.getHTTPHeaderMap()).thenReturn(mockHttpHeaderMap);
        when(sessionMock.getQueryStr()).thenReturn(queryStr);
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
    /** verifyStacksToken calls getVerifyStacksTokenUrl */
    public void verifyStacksToken_getsVerifyStacksTokenUrl()
    {
        SulWowza spyModule = spy(testModule);
        spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(spyModule).getVerifyStacksTokenUrl(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    /** verifyStacksToken calls verifyTokenAgainstStacksService */
    public void verifyStacksToken_verifiesStacksTokenUrl()
            throws MalformedURLException
    {
        String expUrlStr = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + "/media///verify_token?stacks_token=&user_ip=";
        URL expURL = new URL(expUrlStr);
        SulWowza spyModule = spy(testModule);
        when(spyModule.getVerifyStacksTokenUrl(anyString(), anyString(), anyString(), anyString())).thenReturn(expURL);

        spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        verify(spyModule).verifyTokenAgainstStacksService(expURL);
    }

    @Test
    /** if the result to getVerifyStacksTokenUrl is null, don't call verifyTokenAgainstStacksService and return false */
    public void verifyStacksToken_rejectsForNullUrl()
    {
        SulWowza spyModule = spy(testModule);
        when(spyModule.getVerifyStacksTokenUrl(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        boolean result = spyModule.verifyStacksToken(anyString(), anyString(), anyString(), anyString());
        assertFalse(result);
        verify(spyModule, never()).verifyTokenAgainstStacksService(null);
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
    public void validateUserIp_tooFewOctets()
    {
        assertFalse(testModule.validateUserIp("1.1.1"));
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
            String invalidIp = "invalid.ip";
            testModule.validateUserIp(invalidIp);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("User IP missing or invalid"),
                                     containsString(invalidIp)));
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
    public void getVerifyStacksTokenUrl_validUrl()
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
        String expUrlStr = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + expPath + expQueryStr;

        URL resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
        assertEquals(expUrlStr, resultUrl.toString());
    }

    @Test
    public void verifyTokenAgainstStacksService_filenameNeedsEncoding()
    {
        WMSProperties mockProperties = mock(WMSProperties.class);
        when(mockProperties.getPropertyStr(anyString(), anyString())).thenReturn(SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL);
        IApplicationInstance appInstanceMock = mock(IApplicationInstance.class);
        when(appInstanceMock.getProperties()).thenReturn(mockProperties);
        testModule.onAppStart(appInstanceMock);
        String druid = "oo000oo0000";
        String filename = "{([special-chars])}: ü@?;=&#$%20^*.|-_+!,~'/\"`";
        String userIp = "0.0.0.0";
        // specifically list the expected encodings, as we've run into trouble with encoding methods that were encoding in unexpected ways
        String encodedFilename = "%7B%28%5Bspecial-chars%5D%29%7D%3A%20%C3%BC%40%3F%3B%3D%26%23%24%2520%5E%2A.%7C-_%2B%21%2C~%27%2F%22%60";
        String expPath = "/media/" + druid + "/" + encodedFilename + "/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=" + userIp;
        String expUrlStr = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + expPath + expQueryStr;

        URL resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
        assertEquals(expUrlStr, resultUrl.toString());
    }

    @Test
    public void verifyTokenAgainstStacksService_stacksTokenNeedsEncoding()
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
        String stacksTokenWithWeirdChars = "{([special-chars])}: ü@?;=&#$%20^*.|-_+!,~'/\"`";
        // specifically list the expected encodings, as we've run into trouble with encoding methods that were encoding in unexpected ways
        String encodedToken = "%7B%28%5Bspecial-chars%5D%29%7D%3A+%C3%BC%40%3F%3B%3D%26%23%24%2520%5E%2A.%7C-_%2B%21%2C~%27%2F%22%60";
        String expQueryStr = "?stacks_token=" + encodedToken + "&user_ip=" + userIp;
        String expUrlStr = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + expPath + expQueryStr;

        URL resultUrl = testModule.getVerifyStacksTokenUrl(stacksTokenWithWeirdChars, druid, filename, userIp);
        assertEquals(expUrlStr, resultUrl.toString());
    }

    @Test
    /** expect it to return null and log an error message */
    public void getVerifyStacksTokenUrl_malformedUrl()
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
            URL resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, "oo000oo0000", "filename.ext", "0.0.0.0");
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

    @Test
    /** returns true and logs request made */
    public void verifyTokenAgainstStacksService_getsStacksHttpURLConn()
            throws IOException
    {
        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:3000" + expPath + expQueryStr;
        URL stacksURL = new URL(urlStr);

        SulWowza spyModule = spy(testModule);
        spyModule.verifyTokenAgainstStacksService(stacksURL);
        verify(spyModule).getStacksHttpURLConn(stacksURL, "HEAD");
    }

    @Test
    /** returns true and logs request made */
    public void verifyTokenAgainstStacksService_HTTP_OK()
            throws IOException
    {
        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:3000" + expPath + expQueryStr;
        URL stacksURL = new URL(urlStr);

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            SulWowza spyModule = spy(testModule);
            HttpURLConnection mockStacksConn = mock(HttpURLConnection.class);
            when(spyModule.getStacksHttpURLConn(stacksURL, "HEAD")).thenReturn(mockStacksConn);
            when(mockStacksConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            assertTrue(spyModule.verifyTokenAgainstStacksService(stacksURL));
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("INFO"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("sent verify_token request to"),
                                     containsString(urlStr)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    /** returns false and logs request made */
    public void verifyTokenAgainstStacksService_HTTP_FORBIDDEN()
            throws IOException
    {
        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:3000" + expPath + expQueryStr;
        URL stacksURL = new URL(urlStr);

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            SulWowza spyModule = spy(testModule);
            HttpURLConnection mockStacksConn = mock(HttpURLConnection.class);
            when(spyModule.getStacksHttpURLConn(stacksURL, "HEAD")).thenReturn(mockStacksConn);
            when(mockStacksConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
            assertFalse(spyModule.verifyTokenAgainstStacksService(stacksURL));
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("INFO"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("sent verify_token request to"),
                                     containsString(urlStr)));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

    @Test
    /** it logs an error and returns false */
    public void verifyTokenAgainstStacksService_wException()
            throws MalformedURLException
    {
        String expPath = "/media/oo000oo0000/filename.ext/verify_token";
        String expQueryStr = "?stacks_token=" + stacksToken + "&user_ip=0.0.0.0";
        String urlStr = "http://localhost:6666" + expPath + expQueryStr;
        URL stacksURL = new URL(urlStr);

        // logger is the rootLogger, per test/resources/log4j.properties
        Logger logger = Logger.getRootLogger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        try
        {
            boolean result = testModule.verifyTokenAgainstStacksService(stacksURL);
            assertFalse(result);
            String logMsg = out.toString();
            assertThat(logMsg, allOf(containsString("ERROR"),
                                     containsString(testModule.getClass().getSimpleName()),
                                     containsString("unable to verify stacks token at"),
                                     containsString(urlStr),
                                     containsString("Exception")));
        }
        finally
        {
            logger.removeAppender(appender);
        }
    }

}
