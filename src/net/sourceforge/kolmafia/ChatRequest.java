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

import java.net.URLEncoder;
import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.DataUtilities;

/**
 * Responsible for handling all requests related to the Kingdom of Loathing
 * chat.  Note that once this thread is started, it will only stop if the
 * chat buffer on the client is set to null.
 */

public class ChatRequest extends KoLRequest
{
	private static final int REFRESH_RATE = 8000;

	private int lastSeen;
	private KoLMessenger associatedMessenger;
	private boolean isContinuationRequest;
	private boolean isFriendAdditionRequest;

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
		addFormField( "playerid", "" + client.getUserID() );
		addFormField( "pwd", client.getPasswordHash() );

		associatedMessenger = client.getMessenger();
		String contactID = associatedMessenger == null ? null : associatedMessenger.getPlayerID( contact );

		String actualMessage = null;

		if ( contact == null || message.equals( "/exit" ) )
			actualMessage = message;
		else if ( message.equals( "/friend" ) || message.equals( "/ignore" ) || message.equals( "/baleet" ) )
			actualMessage = message + " " + contactID;
		else if ( contact.startsWith( "/" ) && (!message.startsWith( "/" ) || message.startsWith( "/me" )) )
			actualMessage = contact + " " + message;
		else if ( message.equals( "/who" ) && contact.startsWith( "/" ) )
			actualMessage = "/who " + contact.substring(1);
		else if ( contact.startsWith( "/" ) || message.startsWith( "/" ) )
			actualMessage = message;
		else
			actualMessage = "/msg " + contactID + " " + message;

		try
		{
			addFormField( "graf", URLEncoder.encode(
				actualMessage.startsWith( "/msg" ) ? DataUtilities.convertToHTML( actualMessage ) : actualMessage, "UTF-8" ) );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// UTF-8 is a very generic encoding scheme; this
			// exception should never be thrown.  But if it
			// is, just ignore it for now.  Better exception
			// handling when it becomes necessary.
		}

		isContinuationRequest = false;
		isFriendAdditionRequest = actualMessage.trim().startsWith( "/friend " );
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
		super( client, "newchatmessages.php", false );
		addFormField( "lasttime", "" + lastSeen );

		this.lastSeen = lastSeen;
		isContinuationRequest = true;
		associatedMessenger = client.getMessenger();
		isFriendAdditionRequest = false;
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

		// In the event of an error, anything can be the cause; for
		// now, simply return

		if ( isErrorState || responseCode != 200 )
			return;

		int index = replyContent.indexOf( "<!--lastseen:" );

		try
		{
			if ( index != -1 )
				lastSeen = df.parse( replyContent.substring( index + 13, index + 23 ) ).intValue();
		}
		catch ( Exception e )
		{
			// If any exception is thrown, it's possible that there is no
			// value for the last seen - in this case, just leave the old
			// last seen value.
		}

		if ( associatedMessenger != null && associatedMessenger == client.getMessenger() )
		{
			associatedMessenger.updateChat( replyContent );

			if ( isContinuationRequest )
				(new ChatContinuationThread()).start();
			if ( isFriendAdditionRequest )
				(new ChatRequest( client, null, "/friends" )).run();
		}
	}

	/**
	 * An internal class used so that the previous request thread
	 * can die and a new one can begin.
	 */

	private class ChatContinuationThread extends Thread
	{
		public ChatContinuationThread()
		{	setDaemon( true );
		}

		public void run()
		{
			try
			{
				// Before running the next request, you should wait for the
				// refresh rate indicated - this is likely the default rate
				// used for the KoLChat.

				this.sleep( REFRESH_RATE );
			}
			catch ( InterruptedException e )
			{
				// Because this thread is never passed to the outside world,
				// this should only happen on close - but since this is a
				// daemon thread, it'll automatically die anyway.
			}

			// Once the thread has waited for the stated amount of time,
			// the next chat request should be run.  Note that this is
			// only possible if the chat buffer has not been nulled.

			if ( associatedMessenger == client.getMessenger() )
			{
				if ( isFriendAdditionRequest )
					(new ChatRequest( client, null, "/friends" )).run();
				else if ( client.getMessenger() != null && client.getMessenger() == associatedMessenger )
					(new ChatRequest( client, lastSeen )).run();
			}
		}
	}
}