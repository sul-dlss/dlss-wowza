package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;

import org.junit.*;


/** test parsing of data from streamName and/or queryString */
public class TestParsingFromRequestInfo
{
    SulWowza testModule;
    final static String stacksToken = "encryptedStacksMediaToken";
    final static String queryStr = "stacks_token=" + stacksToken;
    final static String streamName = "aa/000/aa/0000/mp4:example.mp4";

    @Before
    public void setUp()
    {
        testModule = new SulWowza();
    }

    @Test
    public void getStacksToken_queryStrOnly()
    {
        assertEquals(stacksToken, testModule.getStacksToken(queryStr));
    }

    @Test
    public void getStacksToken_streamNameWithQueryStr()
    {
        assertEquals(stacksToken, testModule.getStacksToken(streamName + "?" + queryStr));
    }

    @Test
    public void getStacksToken_ignoresOtherQueryParams()
    {
        String myQueryStr = "ignored=ignored&stacks_token=" + stacksToken + "&anything=anythingElse";
        assertEquals(stacksToken, testModule.getStacksToken(myQueryStr));
        assertEquals(stacksToken, testModule.getStacksToken(streamName + "?" + myQueryStr));
    }

    @Test
    public void getStacksTokenEmptyWhenEmptyTokenParam()
    {
        String myQueryStr = "ignored=ignored&stacks_token=&anything=anythingElse";
        assertEquals("", testModule.getStacksToken(myQueryStr));
        assertEquals("", testModule.getStacksToken(streamName + "?" + myQueryStr));
    }

    @Test
    public void getStacksTokenNullWhenMissingTokenParam()
    {
        String myQueryStr = "ignored=ignored&anything=anythingElse";
        assertNull(testModule.getStacksToken(myQueryStr));
        assertNull(testModule.getStacksToken(streamName + "?" + myQueryStr));
    }

    @Test
    public void getStacksTokenNullWhenEmptyStr()
    {
        assertNull(testModule.getStacksToken(""));
    }

    @Test
    public void getStacksTokenNullWhenNullArg()
    {
        SulWowza testModule = new SulWowza();
        assertNull(testModule.getStacksToken(null));
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
    public void getDruidWhenQueryParams()
    {
        assertEquals("oo000oo0000", testModule.getDruid("oo/000/oo/0000/stream.mp4" + "?" + queryStr));
        assertEquals("oo000oo0000", testModule.getDruid("oo/000/oo/0000/a" + "?" + queryStr));
    }

    @Test
    public void getFilename()
    {
        assertEquals("stream.mp4", testModule.getFilename("oo/00/oo/0000/stream.mp4"));
        assertEquals("anything.without%slash", testModule.getFilename("a/b/c/d/anything.without%slash"));
        assertNull(testModule.getFilename("a/b/c/d/"));
    }

    @Test
    public void getFilenameWhenQueryParams()
    {
        assertEquals("stream.mp4", testModule.getFilename("oo/00/oo/0000/stream.mp4?" + queryStr));
        assertEquals("anything.without%slash", testModule.getFilename("a/b/c/d/anything.without%slash?" + queryStr));
        assertNull(testModule.getFilename("a/b/c/d/?" + queryStr));
    }
}
