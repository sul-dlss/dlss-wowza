package edu.stanford.dlss.wowza;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;
import com.wowza.wms.module.ModuleBase;


public class SulWowza extends ModuleBase
{

	public String defaultUrl = "http://localhost:3000/";
    protected URL baseAuthUrl;

    // invoked when a Wowza application instance is started;
    // defined in the IModuleOnApp interface
    public void onAppStart(IApplicationInstance appInstance)
    {
    	String stacksURL = appInstance.getProperties().getPropertyStr("stacksURL", defaultUrl);
    	try
    	{
    		baseAuthUrl = new URL(stacksURL);
    		getLogger().info("Initialized " + this.getClass().getSimpleName() + " at " + baseAuthUrl);
    	}
    	catch(MalformedURLException err)
    	{
    		getLogger().error("Unable to initialize " + this.getClass().getSimpleName() + " module", err);
    	}
    }

    // Invoked when an HTTP MPEGDash Streaming session is created; 
    // defined in IModuleOnHTTPMPEGDashStreamingSession module interfaces
    public void onHTTPMPEGDashStreamingSessionCreate(HTTPStreamerSessionMPEGDash httpSession)
    {
    	getLogger().info("Tommy: SulWowza onHTTPMPEGDashStreamingSessionCreate: " + httpSession.getSessionId());
    	authorizeSession(httpSession);
    }

    // Invoked when an HTTP Cupertino Streaming session is created; 
    // defined in IModuleOnHTTPCupertinoStreamingSession interface
    public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession)
    {
		getLogger().info("Tommy: SulWowza onHTTPCupertinoStreamingSessionCreate: ");
		authorizeSession(httpSession);
	}

    void authorizeSession(IHTTPStreamerSession httpSession)
    {
		String queryStr = httpSession.getQueryStr();
		if (authorize(queryStr))
			httpSession.acceptSession();
		else
			httpSession.rejectSession();
    }

    boolean authorize(String queryStr)
    {
		String authToken = getAuthToken(queryStr);
		return validateAuthToken(authToken);
    }

    String getAuthToken(String queryStr)
    {
		if (queryStr != null && queryStr.length() > 6)  // "token=" is 6 chars
    	{
			String[] parts = queryStr.split("\\?");
    		String query = parts[parts.length-1];
    		List<NameValuePair> httpParams = URLEncodedUtils.parse(query,Charset.defaultCharset());
    		for (NameValuePair param:httpParams)
    			if (param.getName().equals("token"))
    				return param.getValue();
    	}
    	return null;
    }

    boolean validateAuthToken(String token)
    {
    	getLogger().info("Tommy: SulWowza token = " + token);
    	return true;
    }
}
