package org.embulk.spi.time;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class JavaTimestampParser extends TimestampParser
{
    private static final char LOCALE_SEPARATOR = '_';

    private final SimpleDateFormat dateFormat;
    private final boolean timeZoneParsed;

    static Locale toLocale(String localeText)
    {
        if (localeText == null || localeText.length() == 0) {
            return null;
        }

        String language;
        int sepPos = localeText.indexOf(LOCALE_SEPARATOR);
        if (sepPos == -1) {
            return new Locale(localeText);
        } else {
            language = localeText.substring(0, sepPos);
            localeText = localeText.substring(sepPos + 1);
        }

        sepPos = localeText.indexOf(LOCALE_SEPARATOR);
        return sepPos == -1 ?
                new Locale(language, localeText) :
                    new Locale(language,
                        localeText.substring(0, sepPos),
                        localeText.substring(sepPos + 1));
    }

    public JavaTimestampParser(String javaFormatText, ParserTask task)
    {
        super(javaFormatText, task);
        Locale locale = toLocale(task.getLocale());
        dateFormat = locale == null ?
                new SimpleDateFormat(javaFormatText) :
                    new SimpleDateFormat(javaFormatText, locale);
        timeZoneParsed = isTimeZoneParsed();

        if (!timeZoneParsed) {
            dateFormat.setTimeZone(getDefaultTimeZone().toTimeZone());
        }
    }

    @Override
    public Timestamp parse(String text) throws TimestampParseException
    {
        try {
            return Timestamp.ofEpochMilli(dateFormat.parse(text).getTime());
        } catch (ParseException e) {
            throw new TimestampParseException(e);
        }
    }

    public String getFormat()
    {
        return dateFormat.toPattern();
    }

    boolean isTimeZoneParsed()
    {
        AttributedCharacterIterator it = dateFormat.formatToCharacterIterator(new Date());
        return it.getAllAttributeKeys().contains(DateFormat.Field.TIME_ZONE);
    }
}
