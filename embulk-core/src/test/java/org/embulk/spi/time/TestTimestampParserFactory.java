package org.embulk.spi.time;

import static org.junit.Assert.*;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTimestampParserFactory
{

    static final FormatPattern[] TEST_PATTERNS = new FormatPattern[] {
        new FormatPattern("Simple format", "%Y-%m-%d %H:%M:%S.%L %Z", "yyyy-MM-dd HH:mm:ss.SSS z", "2014-11-19 02:46:29.123 UTC"),
        new FormatPattern("No timezone", "%Y-%m-%d %H:%M:%S", "yyyy-MM-dd HH:mm:ss", "2014-11-19 02:46:29"),
        new FormatPattern("pattern %D", "%D", "MM/dd/yy", "04/22/15"),
        new FormatPattern("pattern %x", "%x", "MM/dd/yy", "04/22/15"),
        new FormatPattern("pattern %D %T", "%D %T", "MM/dd/yy HH:mm:ss", "04/22/15 14:46:29"),
        new FormatPattern("pattern %x %X", "%x %X", "MM/dd/yy HH:mm:ss", "04/22/15 14:46:29"),
        new FormatPattern("pattern %D %T %a %b", "%D %T %a %b", "MM/dd/yy HH:mm:ss EEE MMM", "04/22/15 14:46:29 Wed Apr"),
        new FormatPattern("pattern %D %T %a %h", "%D %T %a %h", "MM/dd/yy HH:mm:ss EEE MMM", "04/22/15 14:46:29 Wed Apr"),
        new FormatPattern("pattern %D %T %A %B", "%D %T %A %B", "MM/dd/yy HH:mm:ss EEEE MMMM", "04/22/15 14:46:29 Wednesday April"),
        new FormatPattern("pattern %F", "%F", "yyyy-MM-dd", "2015-04-22"),
        new FormatPattern("pattern %F %I:%M:%S", "%F %I:%M:%S", "yyyy-MM-dd hh:mm:ss", "2015-04-22 02:46:29"),
        new FormatPattern("pattern %F %I:%M:%S %P", "%F %I:%M:%S %P", "yyyy-MM-dd hh:mm:ss a", "2015-04-22 02:46:29 pm"),
        new FormatPattern("pattern %F %I:%M:%S %p", "%F %I:%M:%S %p", "yyyy-MM-dd hh:mm:ss a", "2015-04-22 02:46:29 PM"),
        new FormatPattern("pattern %y", "%y-%m-%d", "yy-MM-dd", "15-04-22"),
        new FormatPattern("pattern %y %%", "%y-%m-%d %%", "yy-MM-dd %", "15-04-22 %"),
        new FormatPattern("pattern %z", "%Y-%m-%d %H:%M:%S %z", "yyyy-MM-dd HH:mm:ss Z", "2014-11-19 02:46:29 -0800"),
        new FormatPattern("pattern quote char", "%y-%m-%d '%%'time", "yy-MM-dd ''%'''time'", "15-04-22 '%'time"),
    };
    
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testNewInstanceDefault()
    {
        TimestampParserFactory factory = new TimestampParserFactory();
        assertFalse("Verify JRuby parser is returned", factory.newInstance("%N", createTestTask()) instanceof JavaTimestampParser);
        assertTrue("Verify Java parser is returned", factory.newInstance("%Y", createTestTask()) instanceof JavaTimestampParser);
        assertTrue("Verify Java parser is returned", factory.newInstance("yyyy", createTestTask()) instanceof JavaTimestampParser);
    }

    @Test
    public void testNewInstanceParserTypeAuto()
    {
        TimestampParserFactory factory = new TimestampParserFactory(TimestampParserFactory.ParserType.AUTO);
        assertFalse("Verify JRuby parser is returned", factory.newInstance("%N", createTestTask()) instanceof JavaTimestampParser);
        assertTrue("Verify Java parser is returned", factory.newInstance("%Y", createTestTask()) instanceof JavaTimestampParser);
        assertTrue("Verify Java parser is returned", factory.newInstance("yyyy", createTestTask()) instanceof JavaTimestampParser);
    }

    @Test
    public void testNewInstanceParserTypeJRuby()
    {
        TimestampParserFactory factory = new TimestampParserFactory(TimestampParserFactory.ParserType.JRUBY);
        assertFalse("Verify JRuby parser is returned", factory.newInstance("%N", createTestTask()) instanceof JavaTimestampParser);
        assertFalse("Verify JRuby parser is returned", factory.newInstance("%Y", createTestTask()) instanceof JavaTimestampParser);
        assertFalse("Verify JRuby parser is returned", factory.newInstance("yyyy", createTestTask()) instanceof JavaTimestampParser);
    }

    @Test
    public void testNewInstanceParserTypeJava()
    {
        TimestampParserFactory factory = new TimestampParserFactory(TimestampParserFactory.ParserType.JAVA);
        assertTrue("Verify Java parser is returned", factory.newInstance("yyyy", createTestTask()) instanceof JavaTimestampParser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceParserTypeJavaNonSupportedPattern()
    {
        TimestampParserFactory factory = new TimestampParserFactory(TimestampParserFactory.ParserType.JAVA);
        factory.newInstance("%N", createTestTask());
    }

    @Test
    public void testNewInstanceParserTypeJavaNoConvert()
    {
        TimestampParserFactory factory = new TimestampParserFactory(TimestampParserFactory.ParserType.JAVA);
        JavaTimestampParser parser = (JavaTimestampParser)factory.newInstance("%Y", createTestTask());
        assertEquals("Verify converted format", "%Y", parser.getFormat());
    }

    @Test
    public void testToJavaFormatCommonPattern()
    {
        TimestampParserFactory factory = new TimestampParserFactory();
        assertEquals("Verify common format", "yyyy-MM-dd HH:mm:ss z", factory.toJavaFormat("%Y-%m-%d %H:%M:%S %Z"));
    }

    @Test
    public void testToJavaFormatForQuoteChars()
    {
        TimestampParserFactory factory = new TimestampParserFactory();
        assertEquals("Verify common format", "yyyy-MM-dd ''HH:mm:ss z''", factory.toJavaFormat("%Y-%m-%d '%H:%M:%S %Z'"));
    }

    @Test
    public void testToJavaFormatForQuoteRequiredChars()
    {
        TimestampParserFactory factory = new TimestampParserFactory();
        assertEquals("Verify common format", "yyyy-MM-dd 'time'''HH:mm:ss z''", factory.toJavaFormat("%Y-%m-%d time'%H:%M:%S %Z'"));
    }

    @Test
    public void testToJavaFormatForQuoteRequiredForLastChars()
    {
        TimestampParserFactory factory = new TimestampParserFactory();
        assertEquals("Verify common format", "yyyy-MM-dd ''HH:mm:ss z'''time'", factory.toJavaFormat("%Y-%m-%d '%H:%M:%S %Z'time"));
    }

    @Test
    public void testJavaAndJRubyParse() throws Exception
    {
        TimestampParserFactory jrubyFactory = new TimestampParserFactory(TimestampParserFactory.ParserType.JRUBY);
        TimestampParserFactory javaFactory = new TimestampParserFactory(TimestampParserFactory.ParserType.JAVA);
        TimestampParserFactory autoFactory = new TimestampParserFactory(TimestampParserFactory.ParserType.AUTO);
        
        for (FormatPattern pattern : TEST_PATTERNS) {
            TimestampParser jrubyParser = jrubyFactory.newInstance(pattern.jrubyFormat, createTestTask());
            TimestampParser javaParser = javaFactory.newInstance(pattern.javaFormat, createTestTask());
            TimestampParser autoParser = autoFactory.newInstance(pattern.jrubyFormat, createTestTask());
            Timestamp jrubyTimestamp = jrubyParser.parse(pattern.testText);
            Timestamp javaTimestamp = javaParser.parse(pattern.testText);
            Timestamp autoTimestamp = autoParser.parse(pattern.testText);
            assertEquals("Verify jruby and java timestamp should be the same value for " + pattern.comment,
                    jrubyTimestamp, javaTimestamp);
            assertEquals("Verify jruby and auto timestamp should be the same value for " + pattern.comment,
                    jrubyTimestamp, autoTimestamp);
        }
    }

    private TimestampParser.ParserTask createTestTask()
    {
        ConfigSource config = Exec.newConfigSource().set("locale", "en");
        return config.loadConfig(TimestampParser.ParserTask.class);
    }


    static class FormatPattern
    {
        final String comment;
        final String jrubyFormat;
        final String javaFormat;
        final String testText;

        public FormatPattern(String comment, String jrubyFormat, String javaFormat, String testText)
        {
            this.comment = comment;
            this.jrubyFormat = jrubyFormat;
            this.javaFormat = javaFormat;
            this.testText = testText;
        }
    }
}
