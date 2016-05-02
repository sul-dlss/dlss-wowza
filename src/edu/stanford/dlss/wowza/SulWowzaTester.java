package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;

import org.junit.Test;

public class SulWowzaTester {

	@Test
	public void testOnAppStart()
	{
		// test use of correct url
		// test use of default url if none exists
		SulWowza testModule = new SulWowza();
		
		fail("Not yet implemented");
	}

	@Test
	public void testOnHTTPMPEGDashStreamingSessionCreate()
	{
		// always succeed at the moment
		
	}

	@Test
	public void testOnHTTPCupertinoStreamingSessionCreate()
	{
		// always succeed at the moment
		fail("Not yet implemented");
	}
	
	@Test
	public void testAuthorizeSession()
	{
		fail("Not yet implemented");
	}
	
	@Test
	public void testAuthorize()
	{
		fail("Not yet implemented");
	}
	
	@Test
	public void testGetAuthToken()
	{
		fail("Not yet implemented");
	}
	
	@Test
	public void testValidateAuthToken()
	{
		fail("Not yet implemented");
	}
}
