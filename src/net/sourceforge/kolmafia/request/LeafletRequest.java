package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

public class LeafletRequest
	extends GenericRequest
{
	private static final Pattern COMMAND_PATTERN = Pattern.compile( "command=([^&]*)" );
	private static final Pattern RESPONSE_PATTERN = Pattern.compile( "<td><b>(.*?)</b>" );
	private static final Pattern TCHOTCHKE_PATTERN = Pattern.compile( "A ([a-z ]*?) sits on the mantelpiece" );

	public LeafletRequest()
	{
		this(null);
	}

	public LeafletRequest( final String command )
	{
		super( "leaflet.php" );
		if ( command != null )
		{
			this.addFormField( "command", command );
		}
	}

	public void setCommand( final String command )
	{
		this.clearDataFields();
		this.addFormField( "command", command );
	}

	@Override
	public void processResults()
	{
		LeafletRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		Matcher matcher = RESPONSE_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

                matcher = TCHOTCHKE_PATTERN.matcher( matcher.group(1) );
		if ( matcher.find() )
		{
			RequestLogger.updateSessionLog( "(You see a " + matcher.group(1) + ")" );
		}
	}

	private static String getCommand( final String urlString )
	{
		Matcher matcher = COMMAND_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}

		return GenericRequest.decodeField( matcher.group( 1 ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "leaflet.php" ) )
		{
			return false;
		}

		String command = LeafletRequest.getCommand( urlString );
		if ( command == null )
		{
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Leaflet " + command );

		return true;
	}
}
