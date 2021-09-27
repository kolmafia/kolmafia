package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.chat.ChatManager;

public class ChatRequest
	extends GenericRequest
{
	protected final String graf;

	/**
	 * Constructs a new <code>ChatRequest</code> where the given parameter
	 * will be passed to the PHP file to indicate where you left off.
	 *
	 * @param lastSeen   The timestamp of the last message received
	 * @param tabbedChat true if "modern" chat, false if "older" chat
	 */

	public ChatRequest( final long lastSeen, final boolean tabbedChat, final boolean afk )
	{
		super( "", false );

		// Construct a URL to submit via GET, just like the browser
		StringBuilder newURLString = new StringBuilder( "newchatmessages.php?" );

		if ( tabbedChat )
		{
			newURLString.append( "j=1&" );
		}

		newURLString.append( "lasttime=" );
		newURLString.append( lastSeen );

		if ( !tabbedChat )
		{
			newURLString.append( "&afk=" );
			newURLString.append( afk ? "1" : "0" );
		}

		this.constructURLString( newURLString.toString(), false );

		this.graf = "";
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given
	 * string to the server.
	 *
	 * @param graf       The message to be sent
	 * @param tabbedChat true if "modern" chat, false if "older" chat
	 */

	public ChatRequest( final String graf, final boolean tabbedChat )
	{
		super( "", false );

		// Construct a URL to submit via GET, just like the browser
		StringBuilder newURLString = new StringBuilder( "submitnewchat.php?" );

		if ( tabbedChat )
		{
			newURLString.append( "j=1&" );
		}

		newURLString.append( "pwd=" );
		newURLString.append( GenericRequest.passwordHash );

		newURLString.append( "&playerid=" );
		newURLString.append( KoLCharacter.getUserId() );

		newURLString.append( "&graf=" );
		newURLString.append( GenericRequest.encodeURL( graf, "ISO-8859-1" ) );

		this.constructURLString( newURLString.toString(), false );

		this.graf = graf;
	}

	public String getGraf()
	{
		return this.graf;
	}

	@Override
	public void run()
	{
		if ( !ChatManager.chatLiterate() )
		{
			return;
		}

		super.run();
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}
}
