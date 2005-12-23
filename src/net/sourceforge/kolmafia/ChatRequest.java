/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;
import net.java.dev.spellcast.utilities.DataUtilities;

/**
 * Responsible for handling all requests related to the Kingdom of Loathing
 * chat.  Note that once this thread is started, it will only stop if the
 * chat buffer on the client is set to null.
 */

public class ChatRequest extends KoLRequest
{
	private static final int CHAT_DELAY = 8000;

	private static int lastSeen;
	private static ChatContinuationThread thread = null;

	/**
	 * Constructs a new <code>ChatRequest</code>.
	 * @param	client	The client to be updated
	 */

	public ChatRequest( KoLmafia client )
	{	this( client, 0 );
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given
	 * string to the server.
	 *
	 * @param	client	The client to be updated
	 * @param	message	The message to be sent
	 */

	public ChatRequest( KoLmafia client, String contact, String message )
	{
		super( client, "submitnewchat.php" );
		addFormField( "playerid", String.valueOf( KoLCharacter.getUserID() ) );
		addFormField( "pwd", client.getPasswordHash() );

		String contactID = client.getPlayerID( contact );
		String actualMessage = null;

		if ( contact == null || (message != null && message.equals( "/exit" )) )
			actualMessage = message;
		else if ( message.equals( "/friend" ) || message.equals( "/ignore" ) || message.equals( "/baleet" ) )
			actualMessage = message + " " + contactID;
		else if ( contact.startsWith( "/" ) && (!message.startsWith( "/" ) || message.startsWith( "/me" ) || message.startsWith( "/em" )) )
			actualMessage = contact + " " + message;
		else if ( contact.startsWith( "[" ) )
			actualMessage = message;
		else if ( (message.equals( "/who" ) || message.equals( "/w" )) && contact.startsWith( "/" ) )
			actualMessage = "/who " + contact.substring(1);
		else if ( message.startsWith( "/" ) )
			actualMessage = message;
		else
			actualMessage = "/msg " + contactID + " " + message;

		addFormField( "graf", actualMessage );

		if ( (actualMessage.equals( "/c" ) || actualMessage.equals( "/channel" )) && actualMessage.indexOf( " " ) != -1 )
			KoLMessenger.stopConversation();

		if ( (actualMessage.equals( "/s" ) || actualMessage.equals( "/switch" )) && actualMessage.indexOf( " " ) != -1 )
			KoLMessenger.switchConversation();

		if ( actualMessage.equals( "/exit" ) )
			KoLMessenger.dispose();
	}

	/**
	 * Constructs a new <code>ChatRequest</code> where the given parameter
	 * will be passed to the PHP file to indicate where you left off.  Note
	 * that this constructor is only available to the <code>ChatRequest</code>;
	 * this is done because only the <code>ChatRequest</code> knows what the
	 * appropriate value should be.
	 */

	private ChatRequest( KoLmafia client, int lastSeen )
	{
		super( client, "newchatmessages.php" );
		addFormField( "lasttime", String.valueOf( lastSeen ) );
		this.lastSeen = lastSeen;
	}

	/**
	 * Runs the chat request.  Note that if you are sending a chat message,
	 * there will be no refresh thread spawned from calling this procedure.
	 * However, if it is a non-send request, it will create a new thread just
	 * before this thread closes.
	 */

	public void run()
	{
		super.run();

		if ( thread == null )
		{
			thread = new ChatContinuationThread();
			thread.start();
		}

		// In the event of an error, anything can be the cause; for
		// now, simply return

		if ( responseCode != 200 || !KoLMessenger.isRunning() )
			return;

		int index = responseText.indexOf( "<!--lastseen:" );

		try
		{
			if ( index != -1 )
				lastSeen = df.parse( responseText.substring( index + 13, index + 23 ) ).intValue();
		}
		catch ( Exception e )
		{
			// If any exception is thrown, it's possible that there is no
			// value for the last seen - in this case, just leave the old
			// last seen value.
		}

		if ( !(client instanceof KoLmafiaCLI) )
			KoLMessenger.updateChat( responseText );

		// If the person is running the chat-based buffbot engine,
		// then go ahead and immediately process the buffs before
		// the next chat refresh iteration.

		if ( BuffBotHome.isBuffBotActive() && getProperty( "useChatBasedBuffBot" ).equals( "true" ) && responseText.indexOf( "New message" ) != -1 )
			BuffBotManager.runOnce();
	}

	/**
	 * An internal class used so that the previous request thread
	 * can die and a new one can begin.
	 */

	private class ChatContinuationThread extends DaemonThread
	{
		public void run()
		{
			ChatRequest request = new ChatRequest( client, lastSeen );

			while ( KoLMessenger.isRunning() )
			{
				// Before running the next request, you should wait for the
				// refresh rate indicated - this is likely the default rate
				// used for the KoLChat.

				ChatRequest.delay( CHAT_DELAY );
				request.run();

				request.addFormField( "lasttime", String.valueOf( lastSeen ) );
			}

			thread = null;
		}
	}
}
