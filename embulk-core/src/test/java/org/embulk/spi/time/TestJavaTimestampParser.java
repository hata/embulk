package org.embulk.spi.time;

import static org.junit.Assert.*;

import java.util.Locale;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.junit.Rule;
import org.junit.Test;

public class TestJavaTimestampParser
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testParse() throws TimestampParseException
    {
        // NOTE: SimpleDateFormat cannot handle micro/nano seconds.
        JavaTimestampParser parser = new JavaTimestampParser("yyyy-MM-dd HH:mm:ss.SSS z", createTestTask());
        assertEquals("Verify milliseconds is supported.",
                Timestamp.ofEpochMilli(1416365189123L),
                parser.parse("2014-11-19 02:46:29.123 UTC"));
    }

    @Test
    public void testParseWithoutTimeZone() throws TimestampParseException
    {
        JavaTimestampParser parser = new JavaTimestampParser("yyyy-MM-dd HH:mm:ss.SSS", createTestTask("UTC"));
        assertEquals("Verify default timezone is used for no timezone format.",
                Timestamp.ofEpochMilli(1416365189123L),
                parser.parse("2014-11-19 02:46:29.123"));
    }

    @Test
    public void testIsTimeZoneParsed()
    {
        JavaTimestampParser parser = new JavaTimestampParser("z", createTestTask());
        assertTrue("Verify timezone is parsed", parser.isTimeZoneParsed());
    }

    @Test
    public void testIsTimeZoneParsedForRFC822()
    {
        JavaTimestampParser parser = new JavaTimestampParser("Z", createTestTask());
        assertTrue("Verify timezone is parsed", parser.isTimeZoneParsed());
    }

    @Test
    public void testIsTimeZoneParsedForNoTimezoneFormat()
    {
        JavaTimestampParser parser = new JavaTimestampParser("yyyy", createTestTask());
        assertFalse("Verify timezone is parsed", parser.isTimeZoneParsed());
    }

    @Test
    public void testToLocaleLanguage()
    {
        Locale locale = JavaTimestampParser.toLocale("ja");
        assertEquals("Verify locale language", "ja", locale.getLanguage());
    }

    @Test
    public void testToLocaleCountry()
    {
        Locale locale = JavaTimestampParser.toLocale("_JP");
        assertEquals("Verify locale language", "", locale.getLanguage());
        assertEquals("Verify locale country", "JP", locale.getCountry());
    }

    @Test
    public void testToLocaleLanguageAndCountry()
    {
        Locale locale = JavaTimestampParser.toLocale("ja_JP");
        assertEquals("Verify locale language", "ja", locale.getLanguage());
        assertEquals("Verify locale country", "JP", locale.getCountry());
    }

    @Test
    public void testToLocaleLanguageAndCountryAndBlank()
    {
        Locale locale = JavaTimestampParser.toLocale("ja_JP_");
        assertEquals("Verify locale language", "ja", locale.getLanguage());
        assertEquals("Verify locale country", "JP", locale.getCountry());
        assertEquals("Verify locale country", "", locale.getVariant());
    }

    @Test
    public void testToLocaleVariant()
    {
        Locale locale = JavaTimestampParser.toLocale("__Var_");
        assertEquals("Verify locale language", "", locale.getLanguage());
        assertEquals("Verify locale country", "", locale.getCountry());
        assertEquals("Verify locale variant", "Var_", locale.getVariant());
    }

    @Test
    public void testToLocaleLangCountryVariant()
    {
        Locale locale = JavaTimestampParser.toLocale("ja_JP_Var_");
        assertEquals("Verify locale language", "ja", locale.getLanguage());
        assertEquals("Verify locale country", "JP", locale.getCountry());
        assertEquals("Verify locale variant", "Var_", locale.getVariant());
    }

    @Test
    public void testNoLocale()
    {
        assertNull("Verify null is returned for null", JavaTimestampParser.toLocale(null));
        assertNull("Verify null is returned for zero length", JavaTimestampParser.toLocale(""));
    }
    
    @Test
    public void testGetFormat()
    {
        JavaTimestampParser parser = new JavaTimestampParser("yyyy", createTestTask());
        assertEquals("Verify format", "yyyy", parser.getFormat());
    }
    
    private TimestampParser.ParserTask createTestTask()
    {
        ConfigSource config = Exec.newConfigSource();
        return config.loadConfig(TimestampParser.ParserTask.class);
    }

    private TimestampParser.ParserTask createTestTask(String timeZone)
    {
        ConfigSource config = Exec.newConfigSource().set("default_timezone", timeZone);
        return config.loadConfig(TimestampParser.ParserTask.class);
    }
}
