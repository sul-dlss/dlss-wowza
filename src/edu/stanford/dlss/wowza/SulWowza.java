package edu.stanford.dlss.wowza;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.config.StandardConfigContext;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.ApplicationInstance;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;

/** Stanford University Libraries Wowza Plugin Code */
public class SulWowza extends ModuleBase
{
    static String stacksTokenVerificationBaseUrl;
    static String stacksUrlErrorMsg = "rejecting due to invalid stacksURL property (" + stacksTokenVerificationBaseUrl + ")";
    static final String HONEYBADGER_API_KEY_ENV_VAR = "WOWZA_HONEYBADGER_API_KEY";
    static final String HONEYBADGER_ENV_NAME_ENV_VAR = "WOWZA_HONEYBADGER_ENV";
    static int stacksConnectionTimeout;
    static int stacksReadTimeout;
    static NoticeReporter noticeReporter;
    StandardConfigContext honeybadgerConfig;
    SulEnvironment environment;


    /** configuration is invalid if the stacks url is malformed */
    boolean invalidConfiguration = false;

    public SulWowza()
    {}

    public SulWowza(SulEnvironment se)
    {
        environment = se;
    }

    /** invoked when a Wowza application instance is started;
     * defined in the IModuleOnApp interface */
    public void onAppStart(IApplicationInstance appInstance)
    {
        initHoneybadger();
        registerUncaughtExceptionHandler();
        initNoticeReporter();
        setStacksConnectionTimeout(appInstance);
        setStacksReadTimeout(appInstance);
        stacksTokenVerificationBaseUrl = getStacksUrl(appInstance);
        try
        {
            new URL(stacksTokenVerificationBaseUrl);
            getLogger().info(this.getClass().getSimpleName() + " stacksURL is " + stacksTokenVerificationBaseUrl);
        }
        catch (MalformedURLException e)
        {
            invalidConfiguration = true;
            String msg = this.getClass().getSimpleName() + " unable to initialize module due to bad stacksURL ";
            getLogger().error(msg, e);
            reportNotice(msg, e);
        }
    }

    /** Invoked when an HTTP MPEGDash Streaming session is created;
     * defined in IModuleOnHTTPMPEGDashStreamingSession module interfaces
     * rejectSession immediately if invalidConfiguration */
    public void onHTTPMPEGDashStreamingSessionCreate(HTTPStreamerSessionMPEGDash httpSession)
    {
        if (invalidConfiguration)
        {
            getLogger().error(this.getClass().getSimpleName() + " onHTTPMPEGDashStreamingSessionCreate: " + stacksUrlErrorMsg + "; streamName: " + httpSession.getStreamName());
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
            getLogger().error(this.getClass().getSimpleName() + " onHTTPCupertinoStreamingSessionCreate: " + stacksUrlErrorMsg + "; streamName: " + httpSession.getStreamName());
            httpSession.rejectSession();
        }
        else
        {
            getLogger().info(this.getClass().getSimpleName() + " onHTTPCupertinoStreamingSessionCreate: " + httpSession.getStreamName());
            authorizeSession(httpSession);
        }
    }

    /**
     * Invoked when a media file starts playing. Defined in ModuleCore. This seems to be the only place
     * we can reliably intercept a Flash connection and get the name of the stream.
     */
    public void play(IClient client, RequestFunction function, AMFDataList params)
	{
        String streamName = params.getString(PARAM1);

        //get the real stream name if this is an alias.
        streamName = ((ApplicationInstance)client.getAppInstance()).internalResolvePlayAlias(streamName, client);

        if (invalidConfiguration)
        {
            getLogger().error(this.getClass().getSimpleName() + " play: " + stacksUrlErrorMsg + "; streamName:  " + streamName);
            client.shutdownClient();
        }
        else
        {
            String queryString = client.getQueryStr();
            // we've empirically observed that the query string is coming over in the streamName, at least some of the time
            if (queryString == null || queryString.length() == 0)
                queryString = streamName;
            String clientIP = client.getIp();

            if (authorizePlay(queryString, clientIP, streamName))
                this.invokePrevious(client, function, params);
            else
            {
                getLogger().error(this.getClass().getSimpleName() + " failed to authorize streamName " + streamName + ", queryStr " + queryString);
                sendClientOnStatusError(client, "NetStream.Play.Failed", "Rejected due to invalid token");
                client.shutdownClient();
            }
        }
	}

    /**
     * Initalizes the Honeybadger error reporting tool. This is a public method so we can call
     * it from the tests. It's outside the constructor, since testing constructors with Mockito is a pain.
     */
    public void initHoneybadger()
    {
        if(environment == null)
            environment = new SulEnvironment();

        String apiKey = environment.getEnvironmentVariable(HONEYBADGER_API_KEY_ENV_VAR);
        String honeybadgerEnv = environment.getEnvironmentVariable(HONEYBADGER_ENV_NAME_ENV_VAR);
        if(apiKey == null)
        {
            getLogger().error(this.getClass().getSimpleName() + " unable to set up Honeybadger error reporting (missing API key environment variable?)");
            invalidConfiguration = true;
        }
        else if (honeybadgerEnv == null)
        {
            getLogger().error(this.getClass().getSimpleName() + " unable to set up Honeybadger error reporting (missing Honeybadger environment specification?)");
            invalidConfiguration = true;
        }
        else
        {
            honeybadgerConfig = new StandardConfigContext();
            honeybadgerConfig.setApiKey(apiKey)
                             .setEnvironment(honeybadgerEnv)
                             .setApplicationPackage(this.getClass().getPackage().getName());
        }
    }


    // --------------------------------- the public API is above this line ----------------------------------------

    /** default setting for stacks service connection timeout (time to establish a connection), in seconds */
    public static final int DEFAULT_STACKS_CONNECTION_TIMEOUT = 20;

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
            String msg = this.getClass().getSimpleName() + " unable to read stacksConnectionTimeout from properties; using default ";
            getLogger().info(msg, e);
            reportNotice(msg, e);
            stacksConnectionTimeout = DEFAULT_STACKS_CONNECTION_TIMEOUT;
        }
        getLogger().info(this.getClass().getSimpleName() + " stacksConnectionTimeout is " + String.valueOf(stacksConnectionTimeout));
    }

    /** default setting for stacks service read timeout (time for reading stream after connection is established),
     * in seconds */
    public static final int DEFAULT_STACKS_READ_TIMEOUT = 20;

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
            String msg = this.getClass().getSimpleName() + " unable to read stacksReadTimeout from properties; using default ";
            getLogger().info(msg, e);
            reportNotice(msg, e);
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
            String msg = this.getClass().getSimpleName() + " unable to read stacksURL from properties ";
            getLogger().info(msg, e);
            reportNotice(msg, e);
            return "";
        }
    }

    // the user's actual IP address will be the first entry in a comma-separated list
    // of IP addresses in the "x-forwarded-for" header (as there might be proxies between
    // the user and wowza).
    // returns an empty string if it can't parse out an IP address.
    String getUserIp(Map<String,String> httpReqHeaders)
    {
        String xForwardedFor = httpReqHeaders.get("x-forwarded-for");
        getLogger().debug(this.getClass().getSimpleName() + " x-forwarded-for: " + xForwardedFor);

        // nothing in the header field to parse, return empty string
        if(xForwardedFor == null || xForwardedFor.length() == 0)
            return "";

        String[] userIpArr = xForwardedFor.split(",");
        return userIpArr[0].trim();
    }

    void authorizeSession(IHTTPStreamerSession httpSession)
    {
        String queryStr = httpSession.getQueryStr();
        String stacksToken = getStacksToken(queryStr);
        String userIp = getUserIp(httpSession.getHTTPHeaderMap());
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
                getLogger().info(this.getClass().getSimpleName() + " druid: " + druid + " filename:" + filename);
                httpSession.rejectSession();
        }
        else
            httpSession.rejectSession();
    }

    boolean authorizePlay(String queryStr, String userIp, String streamName)
    {
        String stacksToken = getStacksToken(queryStr);
        if (validateStacksToken(stacksToken) && validateUserIp(userIp) && validateStreamName(streamName))
        {
            String druid = getDruid(streamName);
            String filename = getFilename(streamName);

            getLogger().debug(this.getClass().getSimpleName() + " userIp: " + userIp);
            getLogger().debug(this.getClass().getSimpleName() + " streamName: " + streamName);
            if (druid != null && filename != null && verifyStacksToken(stacksToken, druid, filename, userIp))
                return true;
            else
                return false;
        }
        else
            return false;
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

    boolean isValidInetAddr(String inetAddress)
    {
        return InetAddressValidator.getInstance().isValid(inetAddress);
    }

    /** it's possible for something like "1.1" or "1" to be a valid IP address, but we want a full four octet address.
      * this implicitly restricts us to IPv4 for now, though that seems like a safe assumption at the moment.
      * see also: http://docs.oracle.com/javase/6/docs/api/java/net/Inet4Address.html#format
     */
    boolean isDottedQuadString(String ipAddr)
    {
        return ipAddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

    boolean validateUserIp(String userIp)
    {
        if (userIp != null && isValidInetAddr(userIp) && isDottedQuadString(userIp))
            return true;
        else
        {
            // unlike, e.g., stream name or token validation, where what's being validated is essentially user input,
            // user IP isn't something we really expect to be invalid, since it should be determined by internal mechanisms.
            // hence, invalid IPs seem more worth reporting (as opposed to just logging).
            String msg = this.getClass().getSimpleName() + ": User IP missing or invalid" + (userIp == null ? "" : ": " + userIp);
            getLogger().error(msg);
            reportNotice(msg);
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
        String myStreamName = streamName;
        // remove query string suffix if present
        int questionMarkIndex = streamName.indexOf('?');
        if (questionMarkIndex > 0)
            myStreamName = streamName.substring(0, questionMarkIndex);

        String filename = myStreamName.substring(myStreamName.lastIndexOf('/') + 1);
        // remove protocol prefix if present
        int colonIndex = filename.indexOf(':');
        if (colonIndex > 0)
            filename = filename.substring(colonIndex + 1);

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
        String queryStr = "stacks_token=" + escapeFormParam(stacksToken) + "&user_ip=" + escapeFormParam(userIp);
        String fullUrl = stacksTokenVerificationBaseUrl + "/media/" +
                        escapePathSegment(druid) + "/" + escapePathSegment(filename) +
                        "/verify_token?" + queryStr;

        try
        {
            return new URL(fullUrl);
        }
        catch (MalformedURLException e)
        {
            String msg = this.getClass().getSimpleName() + " bad URL for stacks_token verification: ";
            getLogger().error(msg, e);
            reportNotice(msg, e);
            return null;
        }
    }

    /** Assumption: verifyStacksTokenUrl is a valid URL */
    boolean verifyTokenAgainstStacksService(URL verifyStacksTokenUrl)
    {
        try
        {
            HttpURLConnection stacksConn = getStacksHttpURLConn(verifyStacksTokenUrl, "HEAD");
            stacksConn.connect();
            int status = stacksConn.getResponseCode();
            getLogger().info(this.getClass().getSimpleName() + " sent verify_token request to " + verifyStacksTokenUrl);
            getLogger().info(this.getClass().getSimpleName() + " verify_token response code is " + String.valueOf(status));
            if (status == HttpURLConnection.HTTP_OK)
                return true;
            else
                return false;
        }
        catch (SocketTimeoutException e)
        {
            // the connect timeout expired before a connection was established, OR
            // the read timeout expired before there was data available for read
            String msg = this.getClass().getSimpleName() + " unable to verify stacks token at " + verifyStacksTokenUrl + " ";
            getLogger().error(msg, e);
            reportNotice(msg, e);
        }
        catch (IOException e)
        {
            String msg = this.getClass().getSimpleName() + " unable to verify stacks token at " + verifyStacksTokenUrl + " ";
            getLogger().error(msg, e);
            reportNotice(msg, e);
        }
        return false;
    }

    HttpURLConnection getStacksHttpURLConn(URL stacksUrl, String requestMethod)
            throws IOException
    {
        HttpURLConnection stacksConn = (HttpURLConnection) stacksUrl.openConnection();
        stacksConn.setRequestMethod(requestMethod);
        stacksConn.setConnectTimeout(stacksConnectionTimeout * 1000); // need milliseconds
        stacksConn.setReadTimeout(stacksReadTimeout * 1000);  // need milliseconds
        return stacksConn;
    }

    /** We define these escape methods, because the most obvious method to use (URLEncoder.encode) works fine
     * for form parameters, but does the wrong thing for URL path segments (a space should be encoded as "+" in
     * form params, but as "%20" in path segments).  Additionally, it was rather difficult finding a Java method
     * that does the right thing for encoding path segments:  some java.net.URI constructors take a path, but not
     * individual path segments, which restricts the ability to use something like a slash, since a method encoding
     * a whole path string will necessarily avoid touching slashes; Google's Guava provide's UrlEscapers, but the
     * urlPathSegmentEscaper() allows too large a set of characters to remain unencoded, compared to what the RFC says
     * is reserved; etc.  However, Guava's UrlEscapers are a light wrapper on its more configurable PercentEscaper,
     * and that works just fine when passed the characters to omit from escaping (which is a short list), and a flag
     * indicating whether to encode a space as "+" or "%20".
     * See https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1 regarding form element encoding.
     * Otherwise, the most relevant looking RFC is https://www.ietf.org/rfc/rfc3986.txt, in particular section 2.2,
     * which lists reserved chars:
     *   reserved    = gen-delims / sub-delims
     *   gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
     *   sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
     *   unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * Note that the tilde is unreserved, contradicting the older RFC 1738, and some things that refer to it.
     * RFC 1738 is from 12/1994, whereas 3986 was updated in 12/2005.
     *   https://www.ietf.org/rfc/rfc1738.txt
     *   https://www.cs.tut.fi/~jkorpela/tilde.html (note: this functioning URL contradicts advice given by referrant)
     *
     * See also: https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
     *
     * finally: i observed that the "%20" path segment encoding problem in java is widely lamented on stack
     * exchange, but the guava solution is not widely referenced.  many people suggest just writing the encoding
     * method yourself, or using the one they pasted inline in an answer, both of which which seem like bad
     * advice.  - @jmartin-sul, 7/2016
     */
    public static final String UNESCAPED_URL_CHARS = "-._~";
    static Escaper formParamEscaper()
    {
        return new PercentEscaper(UNESCAPED_URL_CHARS, true);
    }

    static Escaper pathSegmentEscaper()
    {
        // "+" is actually allowed in path segments, and should be treated literally when encountered,
        // but it seemed safer to encode it, as "%2B" should also be perfectly acceptable.
        // note also, this permits fewer unescaped characters than Guava's built-in urlPathSegmentEscaper().
        return new PercentEscaper(UNESCAPED_URL_CHARS, false);
    }

    static String escapeFormParam(String rawParamVal)
    {
        return formParamEscaper().escape(rawParamVal);
    }

    static String escapePathSegment(String rawPathSegment)
    {
        return pathSegmentEscaper().escape(rawPathSegment);
    }

    /**
     * The methods below set up Honeybadger and report errors to it.
     */
    void registerUncaughtExceptionHandler()
    {
        HoneybadgerUncaughtExceptionHandler.registerAsUncaughtExceptionHandler(honeybadgerConfig);
    }

    void initNoticeReporter()
    {
        noticeReporter = new HoneybadgerReporter(honeybadgerConfig);
    }

    NoticeReporter getNoticeReporter()
    {
        if (noticeReporter == null)
            initNoticeReporter();
        return noticeReporter;
    }

    void reportNotice(String msg)
    {
        reportNotice(msg, null);
    }

    void reportNotice(String msg, Throwable cause)
    {
        Throwable t = new Throwable(msg, cause);
        getNoticeReporter().reportError(t);
    }
}
