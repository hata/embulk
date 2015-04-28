package org.embulk.spi.time;

import org.embulk.spi.time.TimestampParser.ParserTask;

public class TimestampParserFactory
{
    public enum ParserType
    {
        AUTO,
        JAVA,
        JRUBY
    }

    private static final char DATE_FORMAT_QUOTE_CHAR = '\'';
    private static final String[] RUBY_TO_JAVA_FORMAT_TABLE = new String[128];

    // Note: Some patterns like %c, %C, %e, %j, %N, %t, %U, and so on are not handled.
    static
    {
        RUBY_TO_JAVA_FORMAT_TABLE['a'] = "EEE";
        RUBY_TO_JAVA_FORMAT_TABLE['A'] = "EEEE";
        RUBY_TO_JAVA_FORMAT_TABLE['b'] = "MMM";
        RUBY_TO_JAVA_FORMAT_TABLE['B'] = "MMMM";
        RUBY_TO_JAVA_FORMAT_TABLE['d'] = "dd";
        RUBY_TO_JAVA_FORMAT_TABLE['D'] = "MM/dd/yy";
        RUBY_TO_JAVA_FORMAT_TABLE['F'] = "yyyy-MM-dd";
        RUBY_TO_JAVA_FORMAT_TABLE['h'] = "MMM";
        RUBY_TO_JAVA_FORMAT_TABLE['H'] = "HH";
        RUBY_TO_JAVA_FORMAT_TABLE['I'] = "hh";
        RUBY_TO_JAVA_FORMAT_TABLE['m'] = "MM";
        RUBY_TO_JAVA_FORMAT_TABLE['L'] = "SSS";
        RUBY_TO_JAVA_FORMAT_TABLE['M'] = "mm";
        RUBY_TO_JAVA_FORMAT_TABLE['p'] = "a";
        RUBY_TO_JAVA_FORMAT_TABLE['P'] = "a";
        RUBY_TO_JAVA_FORMAT_TABLE['S'] = "ss";
        RUBY_TO_JAVA_FORMAT_TABLE['T'] = "HH:mm:ss";
        RUBY_TO_JAVA_FORMAT_TABLE['x'] = "MM/dd/yy";
        RUBY_TO_JAVA_FORMAT_TABLE['X'] = "HH:mm:ss";
        RUBY_TO_JAVA_FORMAT_TABLE['y'] = "yy";
        RUBY_TO_JAVA_FORMAT_TABLE['Y'] = "yyyy";
        RUBY_TO_JAVA_FORMAT_TABLE['z'] = "Z";
        RUBY_TO_JAVA_FORMAT_TABLE['Z'] = "z";
        RUBY_TO_JAVA_FORMAT_TABLE['%'] = "%";
    }

    private final ParserType parserType;
    
    public TimestampParserFactory()
    {
        this(ParserType.AUTO);
    }
    
    public TimestampParserFactory(ParserType parserType)
    {
        this.parserType = parserType;
    }
    
    public TimestampParser newInstance(String format, ParserTask task)
    {
        String javaFormat;

        switch(parserType) {
        case JAVA:
            return new JavaTimestampParser(format, task);
        case JRUBY:
            return new TimestampParser(format, task);
        default: // AUTO
            if (format != null && format.indexOf('%') == -1) {
                return new JavaTimestampParser(format, task);
            } else {
                javaFormat = toJavaFormat(format);
                return javaFormat != null ?
                        new JavaTimestampParser(javaFormat, task) :
                            new TimestampParser(format, task);
            }
        }
    }

    String toJavaFormat(String format)
    {
        StringBuilder builder = new StringBuilder();
        int formatLen = format.length();
        boolean quoted = false;

        for (int i = 0;i < formatLen;i++) {
            char c = format.charAt(i);
            if (isJavaFormatReserved(c)) {
                if (!quoted) {
                    quoted = true;
                    builder.append(DATE_FORMAT_QUOTE_CHAR);
                }
                builder.append(c);
            } else {
                if (quoted) {
                    quoted = false;
                    builder.append(DATE_FORMAT_QUOTE_CHAR);
                }
                if (c == '%' && (i + 1) < formatLen) {
                    c = format.charAt(i + 1);
                    String convertedFormat = c < RUBY_TO_JAVA_FORMAT_TABLE.length ?
                            RUBY_TO_JAVA_FORMAT_TABLE[c] : null;
                    if (convertedFormat != null) {
                        builder.append(convertedFormat);
                        i++;
                    } else {
                        // If there is unsupported format, then return null to use Ruby TimestampParser.
                        return null;
                    }
                } else if (isJavaDateFormatQuoteChar(c)) {
                    builder.append("''");
                } else {
                    builder.append(c);
                }
            }
        }
        if (quoted) {
            builder.append(DATE_FORMAT_QUOTE_CHAR);
        }

        return builder.toString();
    }
    
    private boolean isJavaFormatReserved(char c)
    {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }
    
    private boolean isJavaDateFormatQuoteChar(char c)
    {
        return c == DATE_FORMAT_QUOTE_CHAR;
    }
}
