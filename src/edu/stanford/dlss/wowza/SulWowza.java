package edu.stanford.dlss.wowza;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.mpegdashstreaming.httpstreamer.HTTPStreamerSessionMPEGDash;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;


public class SulWowza extends ModuleBase
{
	
	public String defaultUrl = "http://localhost:3000/";
    protected URL baseAuthUrl;

    // onAppStart() is defined in the IModuleOnApp interface. This method is invoked when a Wowza application
    // instance is started.
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

    
    public void onHTTPMPEGDashStreamingSessionCreate(HTTPStreamerSessionMPEGDash httpSession)
    {
    	getLogger().info("Tommy: SulWowza onHTTPMPEGDashStreamingSessionCreate: " + httpSession.getSessionId());
    	authorizeSession(httpSession);
    }
    
    public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession)
    {
		getLogger().info("Tommy: SulWowza onHTTPCupertinoStreamingSessionCreate: ");
		authorizeSession(httpSession);
	}
    
    void authorizeSession(IHTTPStreamerSession httpSession)
    {
    	String query = httpSession.getQueryStr();
    	if(authorize(query))
    		httpSession.acceptSession();
    	else httpSession.rejectSession();
    }
    
    boolean authorize(String query)
    {
    	String authToken = getAuthToken(query);
    	return validateAuthToken(authToken);
    }
    
    String getAuthToken(String parameters)
    {
    	if (parameters != null)
    	{
    		String[] parts = parameters.split("\\?");
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
