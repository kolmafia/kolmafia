/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanStashRequest
	extends TransferItemRequest
{
	private static final Pattern LIST_PATTERN = Pattern.compile( "<form name=takegoodies.*?</select>" );
	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<option value=([\\d]+) descid=([\\d]+)>(.*?)( \\(([\\d,]+)\\))?( \\(-[\\d,]*\\))?</option>" );

	private final int moveType;

	public static final int REFRESH_ONLY = 0;
	public static final int ITEMS_TO_STASH = 1;
	public static final int MEAT_TO_STASH = 2;
	public static final int STASH_TO_ITEMS = 3;

	public ClanStashRequest()
	{
		super( "clan_stash.php" );
		this.moveType = ClanStashRequest.REFRESH_ONLY;
		this.destination = new ArrayList();
	}

	/**
	 * Constructs a new <code>ClanStashRequest</code>.
	 *
	 * @param amount The amount of meat involved in this transaction
	 */

	public ClanStashRequest( final int amount )
	{
		super( "clan_stash.php", new AdventureResult( AdventureResult.MEAT, amount ) );
		this.addFormField( "action", "contribute" );

		this.moveType = ClanStashRequest.MEAT_TO_STASH;
		this.destination = new ArrayList();
	}

	public ClanStashRequest( Object attachment, final int moveType )
	{
		this( new Object[] { attachment }, moveType );
	}

	/**
	 * Constructs a new <code>ClanStashRequest</code>.
	 *
	 * @param attachments The list of attachments involved in the request
	 */

	public ClanStashRequest( final Object[] attachments, final int moveType )
	{
		super( "clan_stash.php", attachments );
		this.moveType = moveType;

		if ( moveType == ClanStashRequest.ITEMS_TO_STASH )
		{
			this.addFormField( "action", "addgoodies" );
			this.source = KoLConstants.inventory;
			this.destination = ClanManager.getStash();
		}
		else
		{
			this.addFormField( "action", "takegoodies" );
			this.source = ClanManager.getStash();
			this.destination = KoLConstants.inventory;
		}

	}

	@Override
	protected boolean retryOnTimeout()
	{
		return this.moveType == ClanStashRequest.REFRESH_ONLY;
	}

	@Override
	public String getItemField()
	{
		return this.moveType == ClanStashRequest.ITEMS_TO_STASH ? "item" : "whichitem";
	}

	@Override
	public String getQuantityField()
	{
		return this.moveType == ClanStashRequest.ITEMS_TO_STASH ? "qty" : "quantity";
	}

	@Override
	public String getMeatField()
	{
		return "howmuch";
	}

	public int getMoveType()
	{
		return this.moveType;
	}

	public List getItems()
	{
		List itemList = new ArrayList();

		if ( this.attachments == null )
		{
			return itemList;
		}

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			itemList.add( this.attachments[ i ] );
		}

		return itemList;
	}

	@Override
	public int getCapacity()
	{
		return this.moveType == ClanStashRequest.STASH_TO_ITEMS ? 1 : 11;
	}

	@Override
	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new ClanStashRequest( attachments, this.moveType );
	}

	@Override
	public void processResults()
	{
		super.processResults();

		switch ( this.moveType )
		{
		case REFRESH_ONLY:
			KoLmafia.updateDisplay( "Stash list retrieved." );
			return;

		case MEAT_TO_STASH:
			KoLmafia.updateDisplay( "Clan donation attempt complete." );
			break;

		case STASH_TO_ITEMS:
		case ITEMS_TO_STASH:

			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Movement of items failed." );
			}

			break;
		}
	}

	@Override
	public boolean parseTransfer()
	{
		return ClanStashRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final Pattern ITEM_PATTERN1 = Pattern.compile( "You add (.*?) to the Goodies Hoard" );
	public static final Pattern ITEM_PATTERN2 = Pattern.compile( "(\\d+) (.+?)(?:, (?=\\d)|, and| and (?=\\d)|$)" );

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "takegoodies" ) != -1 )
		{
			// If you ask for too many of an item:
			//     There aren't that many of that item in the stash.
			//
			// If you ask for (and are allowed to take) items:
			//     You acquire 5 xxx
			if ( responseText.indexOf( "You acquire" ) == -1 )
			{
				return false;
			}

			// Since "you acquire" items, they have already been
			// added to inventory
			TransferItemRequest.transferItems( urlString,
					TransferItemRequest.ITEMID_PATTERN,
					TransferItemRequest.QUANTITY_PATTERN,
					ClanManager.getStash(),
					null, 0 );
		}
		else if ( urlString.indexOf( "addgoodies" ) != -1 )
		{
			// If you didn't have the items you wanted to drop into
			// the stash:
			//     You didn't actually have any of the items you
			//     selected. Tsk, tsk.
			// Otherwise:
			//     You add 5 xxx to the Goodies Hoard.
			if ( responseText.indexOf( "to the Goodies Hoard" ) == -1 )
			{
				return false;
			}

			// Parse the actual number of items moved from the
			// responseText, rather than believing the URL
			ArrayList items = TransferItemRequest.getItemList( responseText,
						ITEM_PATTERN1, (Pattern) null, ITEM_PATTERN2 );
			TransferItemRequest.transferItems( items,
					KoLConstants.inventory,
					ClanManager.getStash() );
		}
		else if ( urlString.indexOf( "action=contribute" ) != -1 )
		{
			int meat = TransferItemRequest.transferredMeat( urlString, "howmuch" );
			ResultProcessor.processMeat( 0 - meat );
		}

		KoLCharacter.updateStatus();

		ClanManager.setStashRetrieved();
		ClanStashRequest.parseStash( responseText );

		return true;
	}

	private static void parseStash( final String responseText )
	{
		// In the event that the request was broken up into pieces,
		// there's nothing to look at. Return from the function call.

		if ( responseText == null || responseText.length() == 0 )
		{
			return;
		}

		// Start with an empty list

		SortedListModel stashContents = ClanManager.getStash();
		Matcher stashMatcher = ClanStashRequest.LIST_PATTERN.matcher( responseText );

		// If there's nothing inside the goodies hoard,
		// return because there's nothing to parse

		if ( !stashMatcher.find() )
		{
			stashContents.clear();
			return;
		}

		Matcher matcher = ClanStashRequest.ITEM_PATTERN.matcher( stashMatcher.group() );

		int lastFindIndex = 0;
		ArrayList intermediateList = new ArrayList();

		while ( matcher.find( lastFindIndex ) )
		{
			lastFindIndex = matcher.end();
			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			String descId = matcher.group( 2 );
			String itemString = matcher.group( 3 );

			String quantityString = matcher.group( 5 );
			int quantity = 1;

			if ( quantityString != null )
			{
				quantity = StringUtilities.parseInt( quantityString );
			}

			// If this is a previously unknown item, register it.
			if ( ItemDatabase.getItemName( itemId ) == null )
			{
				ItemDatabase.registerItem( itemId, itemString, descId );
			}

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
			currentResult = (AdventureResult) intermediateList.get( i );
			currentCount = currentResult.getCount( stashContents );
			if ( currentCount != currentResult.getCount() )
			{
				if ( currentCount > 0 )
				{
					stashContents.remove( currentResult );
				}

				stashContents.add( currentResult );
			}
		}
	}

	@Override
	public boolean allowMementoTransfer()
	{
		return true;
	}

	@Override
	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_stash.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "takegoodies" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"remove from stash", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.QUANTITY_PATTERN,
				ClanManager.getStash(), 0 );
		}

		if ( urlString.indexOf( "addgoodies" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"add to stash", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.QTY_PATTERN,
				KoLConstants.inventory, 0 );
		}

		if ( urlString.indexOf( "action=contribute" ) != -1 )
		{
			int meat = TransferItemRequest.transferredMeat( urlString, "howmuch" );
			String message = "add to stash: " + meat + " Meat";
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}

	@Override
	public String getStatusMessage()
	{
		switch ( this.moveType )
		{
		case ClanStashRequest.ITEMS_TO_STASH:
			return "Dropping items into stash";
		case ClanStashRequest.STASH_TO_ITEMS:
			return "Pulling items from stash";
		case ClanStashRequest.MEAT_TO_STASH:
			return "Donating meat to stash";
		}
		return "Refreshing stash contents";
	}
}
