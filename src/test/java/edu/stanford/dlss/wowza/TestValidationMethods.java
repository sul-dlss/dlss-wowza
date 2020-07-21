package edu.stanford.dlss.wowza;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import org.apache.log4j.*;

import java.io.ByteArrayOutputStream;

public class TestValidationMethods
{
    SulWowza testModule;

    @Before
    public void setUp()
    {
        testModule = new SulWowza();
        testModule.initHoneybadger();
    }

    @Test
    public void validateStacksToken_goodEnough()
    {
        assertTrue(testModule.validateStacksToken("stacks_token=encryptedStacksMediaToken"));
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
}
