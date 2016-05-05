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

    public static final String DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL = "http://localhost:3000";
    static String stacksTokenVerificationBaseUrl;

    /** invoked when a Wowza application instance is started;
     * defined in the IModuleOnApp interface */
    public void onAppStart(IApplicationInstance appInstance)
    {
        String stacksURL = appInstance.getProperties().getPropertyStr("stacksURL", DEFAULT_STACKS_TOKEN_VERIFICATION_BASEURL);
        stacksTokenVerificationBaseUrl = stacksURL;
        try
        {
            new URL(stacksTokenVerificationBaseUrl);
            getLogger().info(this.getClass().getSimpleName() + " stacks token verification baseUrl is " + stacksTokenVerificationBaseUrl);
        }
        catch (MalformedURLException err)
        {
            getLogger().error(this.getClass().getSimpleName() + " unable to initialize module due to bad stacksURL: ", err);
        }
    }

    /** Invoked when an HTTP MPEGDash Streaming session is created;
     * defined in IModuleOnHTTPMPEGDashStreamingSession module interfaces */
    public void onHTTPMPEGDashStreamingSessionCreate(HTTPStreamerSessionMPEGDash httpSession)
    {
        getLogger().info(this.getClass().getSimpleName() + " onHTTPMPEGDashStreamingSessionCreate: " + httpSession.getStreamName());
        authorizeSession(httpSession);
    }

    /** Invoked when an HTTP Cupertino Streaming session is created  (for hls protocol (Safari, iphones));
     * defined in IModuleOnHTTPCupertinoStreamingSession interface */
    public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession)
    {
        getLogger().info(this.getClass().getSimpleName() + " onHTTPCupertinoStreamingSessionCreate: " + httpSession.getStreamName());
        authorizeSession(httpSession);
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
        String fullUrl = getVerifyStacksTokenUrl(stacksToken, druid, filename, userIp);
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
            return null;
    }

    /** the media file to be streamed, as stored under the druid-tree;  needed to verify stacks token
     * Assumption:  validateStreamName() was already called */
    String getFilename(String streamName)
    {
        String filename = streamName.substring(streamName.lastIndexOf('/') + 1);
        if (filename.length() > 0)
            return filename;
        else
            return null;
    }

    /** Assumption: stacksToken, druid, userIp and filename are all reasonable values (non-null, not empty, etc.) */
    String getVerifyStacksTokenUrl(String stacksToken, String druid, String filename, String userIp)
    {
        // TODO:  Url encode anything?   utf-8 charset affect anything? (filename, stacksToken)
        String queryStr = "stacks_token=" + stacksToken + "&user_ip=" + userIp;
        String fullUrl = stacksTokenVerificationBaseUrl + "/media/" + druid + "/" + filename + "/verify_token?" + queryStr;
        try
        {
            new URL(fullUrl);
            return fullUrl;
        }
        catch (MalformedURLException err)
        {
            getLogger().error(this.getClass().getSimpleName() + " bad URL for stacks_token verification: ", err);
            return null;
        }
    }

    /** Assumption: verifyStacksTokenRequestUrl is a valid URL */
    boolean verifyTokenAgainstStacksService(String verifyStacksTokenUrl)
    {
        try
        {
            URL tokenVerifyURL = new URL(verifyStacksTokenUrl);
            URLConnection stacksConn = tokenVerifyURL.openConnection();
            stacksConn.connect();
            int status = ((HttpURLConnection) stacksConn).getResponseCode();
            getLogger().info(this.getClass().getSimpleName() + " sent verify_token request to " + verifyStacksTokenUrl);
            getLogger().debug(this.getClass().getSimpleName() + " verify_token response code is " + String.valueOf(status));
            if (status == HttpURLConnection.HTTP_OK)
                return true;
            else
                return false;
        }
        catch (IOException e)
        {
            getLogger().error(this.getClass().getSimpleName() + " unable to verify stacks token at " + verifyStacksTokenUrl);
        }
        return false;
    }
}
