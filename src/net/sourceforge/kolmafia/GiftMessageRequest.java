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
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class GiftMessageRequest extends KoLRequest
{
	private String recipient, outsideMessage, insideMessage;
	private Object [] attachments;
	private GiftWrapper wrappingType;
	private int maxCapacity, materialCost;

	public static final LockableListModel PACKAGES = new LockableListModel();
	static
	{
		PACKAGES.add( new GiftWrapper( "plain brown wrapper", 1, 1, 0 ) );
		PACKAGES.add( new GiftWrapper( "less-than-three-shaped box", 2, 2, 100 ) );
	}

	private static class GiftWrapper
	{
		private StringBuffer name;
		private int radio, maxCapacity, materialCost;

		public GiftWrapper( String name, int radio, int maxCapacity, int materialCost )
		{
			this.radio = radio;
			this.maxCapacity = maxCapacity;
			this.materialCost = materialCost;

			this.name = new StringBuffer();
			this.name.append( name );
			this.name.append( " - " );
			this.name.append( materialCost );
			this.name.append( " meat - Capacity: " );
			this.name.append( maxCapacity );
			this.name.append( " item" );

			if ( maxCapacity > 1 )
				this.name.append( 's' );
		}

		public String toString()
		{	return name.toString();
		}
	}

	public static final int PLAIN_BROWN_WRAPPER = 1;
	public static final int LESS_THAN_THREE_SHAPED_BOX = 2;

	private static final int [] CAPACITIES = { 0, 1, 2 };
	private static final int [] MATERIAL_COST = { 0, 0, 100 };

	public GiftMessageRequest( KoLmafia client, String recipient, String outsideMessage, String insideMessage,
		Object wrappingType, Object [] attachments, int meatAttachment )
	{
		super( client, "town_sendgift.php" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", "Yep." );
		addFormField( "towho", recipient );
		addFormField( "note", outsideMessage );
		addFormField( "insidenote", insideMessage );

		this.recipient = client.getMessenger() == null ? recipient : client.getPlayerID( recipient );
		this.outsideMessage = outsideMessage;
		this.insideMessage = insideMessage;
		this.attachments = attachments;

		this.wrappingType = (GiftWrapper) wrappingType;
		this.maxCapacity = this.wrappingType.maxCapacity;
		this.materialCost = this.wrappingType.materialCost;

		addFormField( "whichpackage", String.valueOf( this.wrappingType.radio ) );
		addFormField( "sendmeat", String.valueOf( meatAttachment ) );
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
			if ( attachments.length > maxCapacity )
			{
				int currentBaseIndex = 0;
				int remainingItems = attachments.length;

				Object [] itemHolder = null;

				while ( remainingItems > 0 )
				{
					itemHolder = new Object[ remainingItems < maxCapacity ? remainingItems : maxCapacity ];

					for ( int i = 0; i < itemHolder.length; ++i )
						itemHolder[i] = attachments[ currentBaseIndex + i ];

					// For each broken-up request, you create a new ItemStorage request
					// which will create the appropriate data to post.

					if ( client.permitsContinue() )
						(new GiftMessageRequest( client, recipient, outsideMessage, insideMessage, wrappingType, itemHolder, 0 )).run();

					currentBaseIndex += maxCapacity;
					remainingItems -= maxCapacity;
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
		client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - materialCost ) );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Make sure that the outsideMessage was actually sent -
		// the person could have input an invalid player ID

		if ( responseText.indexOf( "<td>Package sent.</td>" ) != -1 )
		{
			// With that done, the client needs to be updated
			// to note that the items were sent.

			for ( int i = 0; i < attachments.length; ++i )
				client.processResult( ((AdventureResult) attachments[i]).getNegation() );
		}
		else
		{
			client.cancelRequest();
			return;
		}
	}
}
