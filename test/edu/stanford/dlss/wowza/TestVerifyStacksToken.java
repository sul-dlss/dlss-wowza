package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import org.apache.log4j.*;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TestVerifyStacksToken
{
    SulWowza testModule;
    final static String stacksToken = "encryptedStacksMediaToken";

    @Before
    public void setUp()
    {
        testModule = new SulWowza();
        testModule.initHoneybadger();
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
    public void verifyTokenAgainstStacksService_missingStacksToken()
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
        String stacksToken = null;
        // specifically list the expected encodings, as we've run into trouble with encoding methods that were encoding in unexpected ways
        String expQueryStr = "?stacks_token=&user_ip=" + userIp;
        String expUrlStr = SulWowza.DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL + expPath + expQueryStr;

        URL resultUrl = testModule.getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
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
