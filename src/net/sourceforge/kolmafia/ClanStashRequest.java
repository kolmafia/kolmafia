/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ClanStashRequest extends SendMessageRequest
{
	private static final Pattern LIST_PATTERN = Pattern.compile( "<form name=takegoodies.*?</select>" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "<option value=([\\d]+).*?>(.*?)( \\([\\d,]+\\))?( \\(-[\\d,]*\\))?</option>" );

	private int moveType;

	public static final int REFRESH_ONLY = 0;
	public static final int ITEMS_TO_STASH = 1;
	public static final int MEAT_TO_STASH = 2;
	public static final int STASH_TO_ITEMS = 3;

	public ClanStashRequest()
	{
		super( "clan_stash.php" );
		this.moveType = REFRESH_ONLY;
		destination = new ArrayList();
	}

	/**
	 * Constructs a new <code>ClanStashRequest</code>.
	 * @param	amount	The amount of meat involved in this transaction
	 */

	public ClanStashRequest( int amount )
	{
		super( "clan_stash.php", new AdventureResult( AdventureResult.MEAT, amount ) );
		addFormField( "action", "contribute" );

		this.moveType = MEAT_TO_STASH;
		destination = new ArrayList();
	}

	/**
	 * Constructs a new <code>ClanStashRequest</code>.
	 * @param	attachments	The list of attachments involved in the request
	 */

	public ClanStashRequest( Object [] attachments, int moveType )
	{
		super( "clan_stash.php", attachments );
		this.moveType = moveType;

		if ( moveType == ITEMS_TO_STASH )
		{
			addFormField( "action", "addgoodies" );
			source = inventory;
			destination = ClanManager.getStash();
		}
		else
		{
			addFormField( "action", "takegoodies" );
			source = ClanManager.getStash();
			destination = inventory;
		}

	}

	public String getItemField()
	{	return moveType == ITEMS_TO_STASH ? "item" : "whichitem";
	}

	public String getQuantityField()
	{	return moveType == ITEMS_TO_STASH ? "qty" : "quantity";
	}

	public String getMeatField()
	{	return "howmuch";
	}


	public int getMoveType()
	{	return moveType;
	}

	public List getItems()
	{
		List itemList = new ArrayList();

		if ( attachments == null )
			return itemList;

		for ( int i = 0; i < attachments.length; ++i )
			itemList.add( attachments[i] );

		return itemList;
	}

	public int getCapacity()
	{	return moveType == STASH_TO_ITEMS ? 1 : 11;
	}

	public SendMessageRequest getSubInstance( Object [] attachments )
	{	return new ClanStashRequest( attachments, moveType );
	}

	public String getSuccessMessage()
	{	return moveType == STASH_TO_ITEMS ? "You acquire" : "to the Goodies Hoard";
	}

	public void processResults()
	{
		super.processResults();

		switch ( moveType )
		{
		case REFRESH_ONLY:
			parseStash();
			KoLmafia.updateDisplay( "Stash list retrieved." );
			return;

		case MEAT_TO_STASH:
			parseStash();
			KoLmafia.updateDisplay( "Clan donation attempt complete." );
			break;

		case STASH_TO_ITEMS:
		case ITEMS_TO_STASH:

			if ( !KoLmafia.permitsContinue() )
				KoLmafia.updateDisplay( ERROR_STATE, "Movement of items failed." );

			parseStash();
			break;
		}
	}

	private void parseStash()
	{
		// In the event that the request was broken up into pieces, there's nothing to look
		// at.  Return from the function call.

		if ( responseText == null || responseText.length() == 0 )
			return;

		// Start with an empty list

		SortedListModel stashContents = ClanManager.getStash();
		Matcher stashMatcher = LIST_PATTERN.matcher( responseText );

		// If there's nothing inside the goodies hoard,
		// return because there's nothing to parse

		if ( !stashMatcher.find() )
		{
			stashContents.clear();
			return;
		}

		Matcher matcher = ITEM_PATTERN.matcher( stashMatcher.group() );

		int lastFindIndex = 0;
		ArrayList intermediateList = new ArrayList();

		while ( matcher.find( lastFindIndex ) )
		{
			lastFindIndex = matcher.end();
			int itemId = StaticEntity.parseInt( matcher.group(1) );
			String itemString = matcher.group(2);
			int quantity = matcher.group(3) == null ? 1 :
				StaticEntity.parseInt( matcher.group(3) );

			// If this is a previously unknown item, register it.
			if ( TradeableItemDatabase.getItemName( itemId ) == null )
				TradeableItemDatabase.registerItem( itemId, itemString );

			intermediateList.add( new AdventureResult( itemId, quantity ) );
		}

		// Remove everything that is no longer in the
		// clan stash, and THEN update the quantities
		// of items which are still there.

		int currentCount;
		AdventureResult currentResult;
		stashContents.retainAll( intermediateList );

		for ( int i = 0; i < intermediateList.size(); ++i )
		{
			currentResult = (AdventureResult) intermediateList.get(i);
			currentCount = currentResult.getCount( stashContents );
			if ( currentCount != currentResult.getCount() )
			{
				if ( currentCount > 0 )
					stashContents.remove( currentResult );

				stashContents.add( currentResult );
			}
		}
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "clan_stash.php" ) == -1 || urlString.indexOf( "pwd" ) == -1 || urlString.indexOf( "contribute" ) != -1 )
			return false;

		boolean isWithdrawal = urlString.indexOf( "take" ) != -1;

		return processRequest( "stash " + (isWithdrawal ? "take" : "put"), urlString, ITEMID_PATTERN, QUANTITY_PATTERN,
			isWithdrawal ? ClanManager.getStash() : inventory, 0 );
	}

	public String getCommandForm()
	{
		StringBuffer commandString = new StringBuffer();

		AdventureResult [] items = new AdventureResult[ getItems().size() ];
		getItems().toArray( items );

		if ( moveType == ClanStashRequest.ITEMS_TO_STASH )
		{
			for ( int i = 0; i < items.length; ++i )
			{
				if ( i != 0 )
					commandString.append( LINE_BREAK );

				commandString.append( "stash " );

				commandString.append( '\"' );
				commandString.append( items[i].getName() );
				commandString.append( '\"' );
			}
		}

		return commandString.toString();
	}

	public String getStatusMessage()
	{
		return moveType == ITEMS_TO_STASH ? "Dropping items into stash" : moveType == STASH_TO_ITEMS ? "Pulling items from stash" :
			"Donating meat to stash";
	}
}
