package net.sourceforge.kolmafia;
import java.util.StringTokenizer;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class KoLMailMessage
{
	private static SimpleDateFormat sdf = new SimpleDateFormat( "EEEE, MMMM d, hh:mmaa", Locale.US );

	private String messageHTML;

	private String messageID;
	private String senderID;
	private String senderName;
	private Date messageDate;

	public KoLMailMessage( String message )
	{
		this.messageHTML = message.substring( message.indexOf( "\">" ) + 2 );
		this.messageID = message.substring( message.indexOf( "name=" ) + 6, message.indexOf( "\">" ) );
		StringTokenizer messageParser = new StringTokenizer( message, "<>" );

		String lastToken = messageParser.nextToken();
		while ( !lastToken.startsWith( "a " ) )
			lastToken = messageParser.nextToken();

		this.senderID = lastToken.substring( lastToken.indexOf( "who=" ) + 4, lastToken.length() - 2 );
		this.senderName = messageParser.nextToken();

		while ( !messageParser.nextToken().startsWith( "Date" ) );
		messageParser.nextToken();

		try
		{
			// This attempts to parse the date from
			// the given string; note it may throw
			// an exception (but probably not)

			this.messageDate = sdf.parse( messageParser.nextToken().trim() );
		}
		catch ( Exception e )
		{
			// Initialize the date to the current time,
			// since that's about as close as it gets
			this.messageDate = new Date();
		}
	}

	public String toString()
	{	return senderName + " @ " + sdf.format( messageDate );
	}

	public boolean equals( Object o )
	{
		return o == null ? false :
			o instanceof KoLMailMessage ? equals( (KoLMailMessage)o ) : false;
	}

	public boolean equals( KoLMailMessage kmm )
	{	return messageID.equals( kmm.messageID );
	}

	public String getMessageID()
	{	return messageID;
	}

	public String getMessageHTML()
	{	return messageHTML;
	}
}