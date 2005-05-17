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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class MuseumRequest extends KoLRequest
{
	private Object [] items;
	private boolean isDeposit;
	private boolean isManagement;
	private List source, destination;

	public MuseumRequest( KoLmafia client )
	{
		super( client, "managecollection.php" );
		this.isManagement = false;
	}

	public MuseumRequest( KoLmafia client, boolean isDeposit, Object [] items )
	{
		super( client, "managecollection.php" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", isDeposit ? "put" : "take" );

		this.items = items;
		this.isManagement = true;
		this.isDeposit = isDeposit;

		this.source = isDeposit ? client.getInventory() : client.getCollection();
		this.destination = isDeposit ? client.getCollection() : client.getInventory();

	}

	public void run()
	{
		if ( isManagement )
		{
			if ( items == null || items.length == 0 )
				return;

			if ( items.length > 11 )
			{
				int currentBaseIndex = 0;
				int remainingItems = items.length;

				Object [] itemHolder = null;

				while ( remainingItems > 0 )
				{
					itemHolder = new Object[ remainingItems < 11 ? remainingItems : 11 ];

					for ( int i = 0; i < itemHolder.length; ++i )
						itemHolder[i] = items[ currentBaseIndex + i ];

					// For each broken-up request, you create a new ItemStorage request
					// which will create the appropriate data to post.

					(new MuseumRequest( client, isDeposit, itemHolder )).run();

					currentBaseIndex += 11;
					remainingItems -= 11;
				}

				// Since all the sub-requests were run, there's nothing left
				// to do - simply return from this method.

				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult result = (AdventureResult) items[i];
				int itemID = result.getItemID();

				if ( itemID != -1 )
				{
					addFormField( "whichitem" + (i+1), String.valueOf( itemID ) );
					addFormField( "howmany" + (i+1), String.valueOf( result.getCount() ) );
				}
			}
		}

		if ( isManagement && isDeposit )
			updateDisplay( DISABLED_STATE, "Placing items inside display case..." );
		else if ( isManagement )
			updateDisplay( DISABLED_STATE, "Removing items from display case..." );

		super.run();

		// With that done, the items need to be formally
		// removed from the appropriate list and then
		// replaced into the opposing list.

		if ( isManagement )
		{
			AdventureResult currentResult, negatedResult;
			for ( int i = 0; i < items.length; ++i )
			{
				currentResult = (AdventureResult) items[i];
				negatedResult = new AdventureResult( currentResult.getItemID(), 0 - currentResult.getCount() );

				if ( isDeposit )
				{
					client.processResult( negatedResult );
					AdventureResult.addResultToList( destination, currentResult );
				}
				else
				{
					AdventureResult.addResultToList( source, negatedResult );
					client.processResult( currentResult );
				}
			}
		}

		Matcher displayMatcher = Pattern.compile( "<b>Take:.*?</select>" ).matcher( responseText );
		if ( displayMatcher.find() )
		{
			String content = displayMatcher.group();
			List resultList = client.getCharacterData().getCollection();
			resultList.clear();

			int lastFindIndex = 0;
			Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( content );
			while ( optionMatcher.find( lastFindIndex ) )
			{
				try
				{
					lastFindIndex = optionMatcher.end();
					int itemID = df.parse( optionMatcher.group(1) ).intValue();

					AdventureResult result =
						TradeableItemDatabase.getItemName( itemID ) != null ?
							new AdventureResult( itemID, df.parse( optionMatcher.group(3) ).intValue() ) :
								new AdventureResult( optionMatcher.group(2).trim(), df.parse( optionMatcher.group(3) ).intValue() );

					AdventureResult.addResultToList( resultList, result );
				}
				catch ( Exception e )
				{
					// If an exception occurs during the parsing, just
					// continue after notifying the LogStream of the
					// error.  This could be handled better, but not now.

					logStream.println( e );
					e.printStackTrace( logStream );
				}
			}
		}
	}
}