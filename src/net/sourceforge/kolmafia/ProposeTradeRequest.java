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

public class ProposeTradeRequest extends KoLRequest
{
	private String recipient, message;
	private Object [] attachments;

	public ProposeTradeRequest( KoLmafia client )
	{
		super( client, "makeoffer.php" );
		attachments = new Object[0];
	}

	public ProposeTradeRequest( KoLmafia client, String action, String offerID )
	{
		super( client, "makeoffer.php" );
		addFormField( "action", action );

		if ( action.equals( "accept" ) )
			addFormField( "pwd", client.getPasswordHash() );

		addFormField( "whichoffer", offerID );
		attachments = new Object[0];
	}

	public ProposeTradeRequest( KoLmafia client, String offerID, String recipient, String message, Object [] attachments )
	{
		super( client, "counteroffer.php" );
		addFormField( "action", "counter" );
		addFormField( "whichoffer", offerID );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );

		this.recipient = client.getPlayerID( recipient );
		this.message = message;
		this.attachments = attachments;
	}

	public ProposeTradeRequest( KoLmafia client, String recipient, String message, Object [] attachments )
	{
		super( client, "makeoffer.php" );
		addFormField( "action", "proposeoffer" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "towho", recipient );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );

		this.recipient = client.getPlayerID( recipient );
		this.message = message;
		this.attachments = attachments;
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

		boolean attachedMeat = false;

		for ( int i = 0; i < attachments.length; ++i )
		{
			AdventureResult result = (AdventureResult) attachments[i];

			if ( !result.getName().equals( AdventureResult.MEAT ) )
			{
				int index = attachedMeat ? i : i + 1;
				addFormField( "whichitem" + index, String.valueOf( result.getItemID() ) );
				addFormField( "howmany" + index, String.valueOf( result.getCount() ) );
			}
			else
			{
				addFormField( "offermeat", String.valueOf( result.getCount() ) );
				attachedMeat = true;
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

		AdventureResult currentResult, negatedResult;
		for ( int i = 0; i < attachments.length; ++i )
		{
			currentResult = (AdventureResult) attachments[i];

			if ( !currentResult.getName().equals( AdventureResult.MEAT ) )
				negatedResult = new AdventureResult( currentResult.getItemID(), 0 - currentResult.getCount() );
			else
				negatedResult = new AdventureResult( AdventureResult.MEAT, 0 - currentResult.getCount() );

			client.processResult( negatedResult );
		}

		responseText = responseText.substring( 0, responseText.lastIndexOf( "<b>Propose" ) ).replaceAll(
			"</?[ct].*?>", "" ).replaceAll( "</b>", "</b><br>" ).replaceAll( "[Mm]eat:", "Please respond with " );
	}
}
