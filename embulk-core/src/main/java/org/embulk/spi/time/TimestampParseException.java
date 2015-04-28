package org.embulk.spi.time;

public class TimestampParseException
        extends Exception
{
    public TimestampParseException()
    {
        super();
    }

    public TimestampParseException(Throwable t)
    {
        super(t);
    }
}
