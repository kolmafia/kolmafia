package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.chat.ChatManager;

public class AltarOfLiteracyRequest
	extends GenericRequest
{
	public AltarOfLiteracyRequest()
	{
		super( "town_altar.php" );
	}

	@Override
	public void processResults()
	{
		AltarOfLiteracyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "town_altar.php" ) )
		{
			return;
		}

		// Congratulations! You have demonstrated the ability to both
		// read and write! You have been granted access to the Kingdom
		// of Loathing chat.

		if ( responseText.indexOf( "You have been granted access" ) != -1 )
		{
			String message = "You have proven yourself literate.";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			ChatManager.setChatLiteracy( true );
			return;
		}

		// You have already proven yourself literate!
		if ( responseText.indexOf( "You have already proven yourself literate" ) != -1 )
		{
			ChatManager.setChatLiteracy( true );
			return;
		}

		// At this time, you are not allowed to enter the chat.
		if ( responseText.indexOf( "you are not allowed to enter the chat" ) != -1 )
		{
			ChatManager.setChatLiteracy( false );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_altar.php" ) )
		{
			return false;
		}

		String message = null;

		if ( urlString.equals( "town_altar.php" ) )
		{
			message = "Visiting the Altar of Literacy";
		}

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}
}
