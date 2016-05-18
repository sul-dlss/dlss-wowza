package edu.stanford.dlss.wowza;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;
import com.wowza.wms.module.ModuleBase;

/** Stanford University Libraries Wowza Plugin Code */
public class SulWowza extends ModuleBase
{

    static String stacksTokenVerificationBaseUrl;
    static int stacksConnectionTimeout;
    static int stacksReadTimeout;

    /** configuration is invalid if the stacks url is malformed */
    boolean invalidConfiguration = false;

    /** invoked when a Wowza application instance is started;
     * defined in the IModuleOnApp interface */
    public void onAppStart(IApplicationInstance appInstance)
    {
        setStacksConnectionTimeout(appInstance);
        setStacksReadTimeout(appInstance);
        stacksTokenVerificationBaseUrl = getStacksUrl(appInstance);
        try
        {
            new URL(stacksTokenVerificationBaseUrl);
            getLogger().info(this.getClass().getSimpleName() + "stacksURL is " + stacksTokenVerificationBaseUrl);
        }
        catch (MalformedURLException e)
        {
            invalidConfiguration = true;
            getLogger().error(this.getClass().getSimpleName() + " unable to initialize module due to bad stacksURL: ", e);
        }
    }

    /** Invoked when an HTTP MPEGDash Streaming session is created;
     * defined in IModuleOnHTTPMPEGDashStreamingSession module interfaces
     * rejectSession immediately if invalidConfiguration */
    public void onHTTPMPEGDashStreamingSessionCreate(HTTPStreamerSessionMPEGDash httpSession)
    {
        if (invalidConfiguration)
        {
            getLogger().error(this.getClass().getSimpleName() + " onHTTPMPEGDashStreamingSessionCreate: rejecting session due to invalid stacksURL property " + httpSession.getStreamName());
            httpSession.rejectSession();
        }
        else
        {
            getLogger().info(this.getClass().getSimpleName() + " onHTTPMPEGDashStreamingSessionCreate: " + httpSession.getStreamName());
            authorizeSession(httpSession);
        }
    }

    /** Invoked when an HTTP Cupertino Streaming session is created  (for hls protocol (Safari, iphones));
     * defined in IModuleOnHTTPCupertinoStreamingSession interface
     * rejectSession immediately if invalidConfiguration */
    public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession)
    {
        if (invalidConfiguration)
        {
            getLogger().error(this.getClass().getSimpleName() + " onHTTPCupertinoStreamingSessionCreate: rejecting session due to invalid stacksURL property " + httpSession.getStreamName());
            httpSession.rejectSession();
        }
        else
        {
            getLogger().info(this.getClass().getSimpleName() + " onHTTPCupertinoStreamingSessionCreate: " + httpSession.getStreamName());
            authorizeSession(httpSession);
        }
    }


    // --------------------------------- the public API is above this line ----------------------------------------

    /** default setting for stacks service connection timeout (time to establish a connection), in seconds */
    public static final int DEFAULT_STACKS_CONNECTION_TIMEOUT = 30;

    // TODO:  this approach expects the properties to be set in Application.xml
    //   maybe that's good, or maybe we want to load a java properties file from our plugin jar itself?
    // NOTE:  aware of duplicated code;  shameless green phase
    /** gets stacksConnectionTimeout from properties; uses default if invalid */
    void setStacksConnectionTimeout(IApplicationInstance appInstance)
    {
        try
        {
            stacksConnectionTimeout = appInstance.getProperties().getPropertyInt("stacksConnectionTimeout", DEFAULT_STACKS_CONNECTION_TIMEOUT);
            if (stacksConnectionTimeout < 1)
                stacksConnectionTimeout = DEFAULT_STACKS_CONNECTION_TIMEOUT;
        }
        catch (Exception e)
        {
            getLogger().info(this.getClass().getSimpleName() +
                            " unable to read stacksConnectionTimeout from properties; using default: " + e);
            stacksConnectionTimeout = DEFAULT_STACKS_CONNECTION_TIMEOUT;
        }
        getLogger().info(this.getClass().getSimpleName() + " stacksConnectionTimeout is " + String.valueOf(stacksConnectionTimeout));
    }

    /** default setting for stacks service read timeout (time for reading stream after connection is established),
     * in seconds */
    public static final int DEFAULT_STACKS_READ_TIMEOUT = 30;

    // TODO:  this approach expects the properties to be set in Application.xml
    //   maybe that's good, or maybe we want to load a java properties file from our plugin jar itself?
    // NOTE:  aware of duplicated code;  shameless green phase
    /** gets stacksReadTimeout from properties; uses default if invalid */
    void setStacksReadTimeout(IApplicationInstance appInstance)
    {
        try
        {
            stacksReadTimeout = appInstance.getProperties().getPropertyInt("stacksReadTimeout", DEFAULT_STACKS_READ_TIMEOUT);
            if (stacksReadTimeout < 1)
                stacksReadTimeout = DEFAULT_STACKS_READ_TIMEOUT;
        }
        catch (Exception e)
        {
            getLogger().info(this.getClass().getSimpleName() +
                            " unable to read stacksReadTimeout from properties; using default: " + e);
            stacksReadTimeout = DEFAULT_STACKS_READ_TIMEOUT;
        }
        getLogger().info(this.getClass().getSimpleName() + " stacksReadTimeout is " + String.valueOf(stacksReadTimeout));
    }

    public static final String DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL = "http://localhost:3000";

    // TODO:  this approach expects the properties to be set in Application.xml
    //   maybe that's good, or maybe we want to load a java properties file from our plugin jar itself?
    /** reads stacksUrl from properties */
    String getStacksUrl(IApplicationInstance appInstance)
    {
        try
        {
            return appInstance.getProperties().getPropertyStr("stacksURL", DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL);
        }
        catch (Exception e)
        {
            getLogger().info(this.getClass().getSimpleName() + " unable to read stacksURL from properties: " + e);
            return "";
        }
    }

    void authorizeSession(IHTTPStreamerSession httpSession)
    {
        String queryStr = httpSession.getQueryStr();
        String stacksToken = getStacksToken(queryStr);
        String userIp = httpSession.getIpAddress();
        getLogger().debug(this.getClass().getSimpleName() + " userIp: " + userIp);
        String streamName = httpSession.getStreamName();
        getLogger().debug(this.getClass().getSimpleName() + " streamName: " + streamName);
        if (validateStacksToken(stacksToken) && validateUserIp(userIp) && validateStreamName(streamName))
        {
            String druid = getDruid(streamName);
            String filename = getFilename(streamName);

            if (druid != null && filename != null && verifyStacksToken(stacksToken, druid, filename, userIp))
                httpSession.acceptSession();
            else
                httpSession.rejectSession();
        }
        else
            httpSession.rejectSession();
    }

    /** Assumption: stacksToken, druid, userIp and filename are all reasonable values (non-null, not empty, etc.) */
    boolean verifyStacksToken(String stacksToken, String druid, String filename, String userIp)
    {
        URL fullUrl = getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
        if (fullUrl != null)
            return verifyTokenAgainstStacksService(fullUrl);
        else
            return false;
    }

    String getStacksToken(String queryStr)
    {
        if (queryStr != null && queryStr.length() > 13) // "stacks_token=" is 13 chars
        {
            String[] parts = queryStr.split("\\?");
            String query = parts[parts.length - 1];
            List<NameValuePair> httpParams = URLEncodedUtils.parse(query, Charset.defaultCharset());
            for (NameValuePair param : httpParams)
                if (param.getName().equals("stacks_token"))
                    return param.getValue();
        }
        return null;
    }

    /** stacksToken is created by rails encryption in digital_stacks_rails app;
     *  we have chosen a min length of 10 here as a "safe" minimum length */
    private static final int MIN_STACKS_TOKEN_LENGTH = 10;

    boolean validateStacksToken(String stacksToken)
    {
        if (stacksToken != null && stacksToken.length() > MIN_STACKS_TOKEN_LENGTH)
            return true;
        else
        {
            getLogger().error(this.getClass().getSimpleName() + ": stacksToken missing or implausibly short" +
                                (stacksToken == null ? "" : ": " + stacksToken));
            return false;
        }
    }

    private static final int MIN_IP_LENGTH = "1.1.1.1".length();

    boolean validateUserIp(String userIp)
    {
        // could validate it's an IP address format, but since we obtain it from http session, we trust it
        if (userIp != null && userIp.length() >= MIN_IP_LENGTH)
            return true;
        else
        {
            getLogger().error(this.getClass().getSimpleName() + ": User IP missing or implausibly short" +
                                (userIp == null ? "" : ": " + userIp));
            return false;
        }
    }

    private static final int DRUID_TREE_LENGTH = "aa/123/bb/4567".length();

    /** require streamName to have format "aa/000/aa/0000/sample.mp4"  (druid tree)/(media_fname) */
    boolean validateStreamName(String streamName)
    {
        if (streamName == null || streamName.length() <= DRUID_TREE_LENGTH)
        {
            getLogger().error(this.getClass().getSimpleName() + ": streamName missing or implausibly short" +
                                (streamName == null ? "" : ": " + streamName));
            return false;
        }
        String[] pieces = streamName.split("/");
        if (pieces.length == 5)
            return true;
        else
        {
            getLogger().error(this.getClass().getSimpleName() + ": unable to parse druid and filename from streamName " + streamName);
            return false;
        }
    }

    static private final Pattern DRUID_REGEX_PATTERN = Pattern.compile("[a-z]{2}\\d{3}[a-z]{2}\\d{4}");

    /** the druid of the media file to be streamed;  needed to verify stacks token
     * Assumption:  validateStreamName() was already called */
    String getDruid(String streamName)
    {
        String[] pieces = streamName.split("/");
        String druid = pieces[0] + pieces[1] + pieces[2] + pieces[3];
        if (DRUID_REGEX_PATTERN.matcher(druid).matches())
            return druid;
        else
        {
            getLogger().error(this.getClass().getSimpleName() + ": can't parse druid from streamName" +
                    (streamName == null ? "" : ": " + streamName));
            return null;
        }
    }

    /** the media file to be streamed, as stored under the druid-tree;  needed to verify stacks token
     * Assumption:  validateStreamName() was already called */
    String getFilename(String streamName)
    {
        String filename = streamName.substring(streamName.lastIndexOf('/') + 1);
        if (filename.length() > 0)
            return filename;
        else
        {
            getLogger().error(this.getClass().getSimpleName() + ": can't parse fileName from streamName" +
                    (streamName == null ? "" : ": " + streamName));
            return null;
        }
    }

    /** Assumption: stacksToken, druid, userIp and filename are all reasonable values (non-null, not empty, etc.) */
    URL getVerifyStacksTokenUrl(String stacksToken, String druid, String filename, String userIp)
    {
        // TODO:  Url encode anything?   utf-8 charset affect anything? (filename, stacksToken)
        String queryStr = "stacks_token=" + urlEncode(stacksToken) + "&user_ip=" + urlEncode(userIp);
        String fullUrl = stacksTokenVerificationBaseUrl + "/media/" + urlEncode(druid) + "/" + urlEncode(filename) + "/verify_token?" + queryStr;
        try
        {
            return new URL(fullUrl);
        }
        catch (MalformedURLException err)
        {
            getLogger().error(this.getClass().getSimpleName() + " bad URL for stacks_token verification: ", err);
            return null;
        }
    }

    /** Assumption: verifyStacksTokenUrl is a valid URL */
    boolean verifyTokenAgainstStacksService(URL verifyStacksTokenUrl)
    {
        try
        {
            HttpURLConnection stacksConn = getStacksUrlConn(verifyStacksTokenUrl, "HEAD");
            stacksConn.connect();
            int status = stacksConn.getResponseCode();
            getLogger().info(this.getClass().getSimpleName() + " sent verify_token request to " + verifyStacksTokenUrl);
            getLogger().debug(this.getClass().getSimpleName() + " verify_token response code is " + String.valueOf(status));
            if (status == HttpURLConnection.HTTP_OK)
                return true;
            else
                return false;
        }
        catch (SocketTimeoutException e)
        {
            // the connect timeout expired before a connection was established, OR
            // the read timeout expired before there was data available for read
            getLogger().error(this.getClass().getSimpleName() + " unable to verify stacks token at " + verifyStacksTokenUrl + e);
        }
        catch (IOException e)
        {
            getLogger().error(this.getClass().getSimpleName() + " unable to verify stacks token at " + verifyStacksTokenUrl + e);
        }
        return false;
    }

    HttpURLConnection getStacksUrlConn(URL stacksUrl, String requestMethod) throws IOException
    {
        HttpURLConnection stacksConn = (HttpURLConnection) stacksUrl.openConnection();
        stacksConn.setRequestMethod(requestMethod);
        stacksConn.setConnectTimeout(stacksConnectionTimeout * 1000); // need milliseconds
        stacksConn.setReadTimeout(stacksReadTimeout * 1000);  // need milliseconds
        return stacksConn;
    }

    static String urlEncode(String urlComponent)
    {
        try
        {
            // the javadocs say that encode without an explicitly specified encoding is deprecated
            return URLEncoder.encode(urlComponent, java.nio.charset.StandardCharsets.UTF_8.toString());
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            getLogger().error(SulWowza.class.getSimpleName() + " this should never happen, since we should be using the JDK UTF-8 constant: " + urlComponent + " ; " + e);
            throw new RuntimeException(e);
        }
    }
}
