package net.sourceforge.kolmafia.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GenericRequest;


public class SentMessageEntry
	extends HistoryEntry
{
	private static final Pattern DOJAX_PATTERN =
		Pattern.compile( "<!--js\\(\\s*dojax\\((.*?)\\)-->" );

	private static final Pattern DOJAX_URL_PATTERN =
		Pattern.compile( "['\"]([^'\"]+\\.php[^'\"]+)['\"]" );

	private static final GenericRequest DOJAX_VISITOR = new GenericRequest( "" );

	private final boolean isRelayRequest;

	public SentMessageEntry( final String responseText, final long localLastSeen, boolean isRelayRequest )
	{
		super( responseText, localLastSeen );

		this.isRelayRequest = isRelayRequest;
	}

	public boolean isRelayRequest()
	{
		return this.isRelayRequest;
	}

	public void executeAjaxCommand()
	{
		if ( this.isRelayRequest )
		{
			return;
		}

		String content = getContent();

		if ( content == null )
		{
			return;
		}

		Matcher dojax = SentMessageEntry.DOJAX_PATTERN.matcher( content );

		GenericRequest request = SentMessageEntry.DOJAX_VISITOR;
		while ( dojax.find() )
		{
			String commands = dojax.group( 1 );

			Matcher dojaxURLs = SentMessageEntry.DOJAX_URL_PATTERN.matcher( commands );

			while ( dojaxURLs.find() )
			{
				// Force a GET, just like the Browser
				request.constructURLString( dojaxURLs.group( 1 ), false );
				RequestThread.postRequest( request );
			}
		}

		dojax.reset();

		content = dojax.replaceAll( "" );

		this.setContent( content );
	}
}
