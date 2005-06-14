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
import java.util.List;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class GreenMessageRequest extends KoLRequest
{
	private String recipient, message;
	private Object [] attachments;

	public GreenMessageRequest( KoLmafia client, String recipient, String message, AdventureResult attachment )
	{
		super( client, "sendmessage.php" );
		addFormField( "action", "send" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "towho", recipient );

		String saveOutgoingSetting = client.getSettings().getProperty( "saveOutgoing" );
		if ( saveOutgoingSetting == null || saveOutgoingSetting.equals( "true" ) )
			addFormField( "savecopy", "on" );

		addFormField( "message", message );

		this.recipient = client.getPlayerID( recipient );
		this.message = message;

		if ( attachment.getName().equals( AdventureResult.MEAT ) )
		{
			addFormField( "sendmeat", String.valueOf( attachment.getCount() ) );
			client.processResult( attachment.getNegation() );
			this.attachments = new Object[0];
		}
		else
		{
			this.attachments = new Object[1];
			attachments[0] = attachment;
		}
	}

	public GreenMessageRequest( KoLmafia client, String recipient, String message, Object [] attachments, int meatAttachment )
	{
		super( client, "sendmessage.php" );
		addFormField( "action", "send" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "towho", recipient );

		String saveOutgoingSetting = client.getSettings().getProperty( "saveOutgoing" );
		if ( saveOutgoingSetting == null || saveOutgoingSetting.equals( "true" ) )
			addFormField( "savecopy", "on" );

		addFormField( "message", message );
		addFormField( "sendmeat", String.valueOf( meatAttachment ) );

		this.recipient = client.getMessenger() == null ? recipient : client.getPlayerID( recipient );
		this.message = message;
		this.attachments = attachments;

		client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - meatAttachment ) );
	}

	/**
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public void run()
	{
		// First, check to see how many attachments are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( attachments != null && attachments.length != 0 )
		{
			if ( attachments.length > 11 )
			{
				int currentBaseIndex = 0;
				int remainingItems = attachments.length;

				Object [] itemHolder = null;

				while ( remainingItems > 0 )
				{
					itemHolder = new Object[ remainingItems < 11 ? remainingItems : 11 ];

					for ( int i = 0; i < itemHolder.length; ++i )
						itemHolder[i] = attachments[ currentBaseIndex + i ];

					// For each broken-up request, you create a new ItemStorage request
					// which will create the appropriate data to post.

					if ( client.permitsContinue() )
						(new GreenMessageRequest( client, recipient, message, itemHolder, 0 )).run();

					currentBaseIndex += 11;
					remainingItems -= 11;
				}

				// Since all the sub-requests were run, there's nothing left
				// to do - simply return from this method.

				return;
			}

			for ( int i = 0; i < attachments.length; ++i )
			{
				AdventureResult result = (AdventureResult) attachments[i];
				addFormField( "whichitem" + (i+1), String.valueOf( result.getItemID() ) );
				addFormField( "howmany" + (i+1), String.valueOf( result.getCount() ) );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from KoLRequest
		// to execute the request.

		client.resetContinueState();
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Make sure that the message was actually sent -
		// the person could have input an invalid player ID

		if ( responseText.indexOf( "<center>Message Sent.</center>" ) != -1 )
		{
			// With that done, the client needs to be updated
			// to note that the items were sent.

			for ( int i = 0; i < attachments.length; ++i )
				client.processResult( ((AdventureResult)attachments[i]).getNegation() );

			ConcoctionsDatabase.refreshConcoctions( client );
		}
		else
		{
			client.cancelRequest();
			return;
		}
	}
}
