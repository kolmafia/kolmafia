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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StorageRequest
	extends TransferItemRequest
{
	private int moveType;

	public static final int REFRESH = 0;
	public static final int MEAT = 1;
	public static final int CONSUMABLES = 2;
	public static final int EQUIPMENT = 3;
	public static final int MISCELLANEOUS = 4;

	public static final int EMPTY_STORAGE = 5;
	public static final int STORAGE_TO_INVENTORY = 6;
	public static final int PULL_MEAT_FROM_STORAGE = 7;

	public StorageRequest()
	{
		super( "storage.php" );
		this.moveType = StorageRequest.REFRESH;
	}

	public StorageRequest( final int moveType )
	{
		this( moveType, new Object[ 0 ] );
		this.moveType = moveType;
	}

	public StorageRequest( final int moveType, final int amount )
	{
		this( moveType, new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public StorageRequest( final int moveType, final Object attachment )
	{
		this( moveType, new Object[] { attachment } );
	}

	public StorageRequest( final int moveType, final Object[] attachments )
	{
		super( "storage.php", attachments );
		this.moveType = moveType;

		// Figure out the actual URL information based on the
		// different request types.

		switch ( moveType )
		{
		case MEAT:
			this.addFormField( "which", "5" );
			break;
		case CONSUMABLES:
			this.addFormField( "which", "1" );
			break;
		case EQUIPMENT:
			this.addFormField( "which", "2" );
			break;
		case MISCELLANEOUS:
			this.addFormField( "which", "3" );
			break;

		case EMPTY_STORAGE:
			this.addFormField( "action", "pullall" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case STORAGE_TO_INVENTORY:
			// storage.php?action=pull&whichitem1=1649&howmany1=1&pwd
			this.addFormField( "action", "pull" );
			this.addFormField( "ajax", "1" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case PULL_MEAT_FROM_STORAGE:
			this.addFormField( "action", "takemeat" );
			break;
		}
	}

	protected boolean retryOnTimeout()
	{
		return this.moveType == StorageRequest.MEAT ||
		       this.moveType == StorageRequest.CONSUMABLES ||
		       this.moveType == StorageRequest.EQUIPMENT ||
		       this.moveType == StorageRequest.MISCELLANEOUS;
	}

	public int getMoveType()
	{
		return this.moveType;
	}

	public String getItemField()
	{
		return "whichitem";
	}

	public String getQuantityField()
	{
		return "howmany";
	}

	public String getMeatField()
	{
		return "amt";
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

	public int getCapacity()
	{
		return 11;
	}

	public boolean forceGETMethod()
	{
		return this.moveType == STORAGE_TO_INVENTORY;
	}

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new StorageRequest( this.moveType, attachments );
	}

	public void run()
	{
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.canInteract() )
		{
			switch ( this.moveType )
			{
			case EMPTY_STORAGE:
			case STORAGE_TO_INVENTORY:
			case PULL_MEAT_FROM_STORAGE:
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Hagnk's Storage is not available in Bad Moon until you free King Ralph." );
				return;
			}
		}

		if ( this.moveType == REFRESH )
		{
			// If we are refreshing storage, we need to do all four pages.
			KoLmafia.updateDisplay( "Refreshing storage..." );

			// Get the four pages of storage in succession
			KoLConstants.storage.clear();
			KoLConstants.freepulls.clear();
			RequestThread.postRequest( new StorageRequest( MEAT ) );
			RequestThread.postRequest( new StorageRequest( CONSUMABLES ) );
			RequestThread.postRequest( new StorageRequest( EQUIPMENT ) );
			RequestThread.postRequest( new StorageRequest( MISCELLANEOUS ) );
		}
		else
		{
			// If it's a transfer, let TransferItemRequest handle it
			super.run();
		}
	}

	public void processResults()
	{
		switch ( this.moveType )
		{
		case StorageRequest.REFRESH:
			return;
		case StorageRequest.MEAT:
		case StorageRequest.CONSUMABLES:
		case StorageRequest.EQUIPMENT:
		case StorageRequest.MISCELLANEOUS:
			StorageRequest.parseStorage( this.getURLString(), this.responseText );
			return;
		default:
			super.processResults();
		}
	}

	// <b>You have 178,634,761 meat in long-term storage.</b>
	private static final Pattern STORAGEMEAT_PATTERN =
		Pattern.compile( "<b>You have ([\\d,]+) meat in long-term storage.</b>" );

	private static final Pattern PULLS_PATTERN = Pattern.compile( "<span class=\"pullsleft\">(\\d+)</span>" );

	// With inventory images:
	//
	// <table class='item' id="ic4511" rel="id=4511&s=0&q=0&d=0&g=1&t=1&n=10&m=0&p=0&u=e"><td class="img"><img src="http://images.kingdomofloathing.com/itemimages/soupbowl.gif" class="hand ircm" onClick='descitem(569697802,0, event);'></td><td id='i4511' valign=top><b class="ircm">beautiful soup</b>&nbsp;<span>(10)</span><font size=1><br></font></td></table>
	//
	// Without inventory images:
	//
	// <table class='item' id="ic4511" rel="id=4511&s=0&q=0&d=0&g=1&t=1&n=10&m=0&p=0&u=e"><td id='i4511' valign=top><b class="ircm"><a onClick='javascript:descitem(569697802,0, event);'>beautiful soup</a></b>&nbsp;<span>(10)</span><font size=1><br></font></td></table>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<table class='item' id=\"ic([\\d]+)\".*?rel=\"([^\"]*)\">.*?<b class=\"ircm\">(?:<a[^>]*>)?(.*?)(?:</a>)?</b>(?:&nbsp;<span>\\(([\\d]+)\\)</span)?.*?</table>" );

	private static void parseStorage( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "storage.php" ) )
		{
			return;
		}

		// On the main page - which=5 - Hagnk tells you how much meat
		// you have in storage and how many pulls you have remaining.
		//
		// These data do not appear on the three item pages, and items
		// do not appear on page 5.

		if ( urlString.indexOf( "which=5" ) != -1 )
		{
			Matcher meatInStorageMatcher = StorageRequest.STORAGEMEAT_PATTERN.matcher( responseText );
			if ( meatInStorageMatcher.find() )
			{
				int meat = StringUtilities.parseInt( meatInStorageMatcher.group( 1 ) );
				KoLCharacter.setStorageMeat( meat );
			}
			else if ( responseText.indexOf( "Hagnk doesn't have any of your meat" ) != -1 )
			{
				KoLCharacter.setStorageMeat( 0 );
			}

			Matcher pullsMatcher = StorageRequest.PULLS_PATTERN.matcher( responseText );
			if ( pullsMatcher.find() )
			{
				ConcoctionDatabase.setPullsRemaining( StringUtilities.parseInt( pullsMatcher.group( 1 ) ) );
			}
			else if ( KoLCharacter.isHardcore() || !KoLCharacter.canInteract() )
			{
				ConcoctionDatabase.setPullsRemaining( 0 );
			}
			else
			{
				ConcoctionDatabase.setPullsRemaining( -1 );
			}
			return;
		}

		Matcher matcher = StorageRequest.ITEM_PATTERN.matcher( responseText );
		int lastFindIndex = 0;

		while ( matcher.find( lastFindIndex ) )
		{
			lastFindIndex = matcher.end();
			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			String relString = matcher.group( 2 );
			String countString = matcher.group( 4 );
			int count = ( countString == null ) ? 1 : StringUtilities.parseInt( countString );
			String itemName = StringUtilities.getCanonicalName( ItemDatabase.getItemDataName( itemId ) );
			String realName = matcher.group( 3 );
			String canonicalName = StringUtilities.getCanonicalName( realName );

			if ( itemName == null || !canonicalName.equals( itemName ) )
			{
				// Lookup item with api.php for additional info
				ItemDatabase.registerItem( itemId );
			}

			AdventureResult item = new AdventureResult( itemId, StringUtilities.parseInt( matcher.group( 4 ) ) );

			// Separate free pulls into a separate list
			boolean isFreePull = Modifiers.getBooleanModifier( item.getName(), "Free Pull" );

			// For now, special handling for the single item which
			// is a free pull only for a specific path. If more
			// path-specific free pulls are introduced, we'll
			// define a "Free Pull Path" modifier or something.
			if ( itemId == ItemPool.BORIS_HELM && !KoLCharacter.inAxecore() )
			{
				isFreePull = false;
			}

			List list = isFreePull ? KoLConstants.freepulls : KoLConstants.storage;

			int storageCount = item.getCount( list );

			// Add the difference between your existing count
			// and the original count.

			if ( storageCount != count )
			{
				item = item.getInstance( count - storageCount );
				AdventureResult.addResultToList( list, item );
			}
		}
	}

	public boolean parseTransfer()
	{
		return StorageRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action" ) == -1 )
		{
			StorageRequest.parseStorage( urlString, responseText );
			return true;

		}

		boolean transfer = false;

		if ( urlString.indexOf( "action=pullall" ) != -1 )
		{
			// Hagnk leans back and yells something
			// ugnigntelligible to a group of Knob Goblin teegnage
			// delignquegnts, who go and grab all of your stuff
			// from storage and bring it to you.

			if ( responseText.indexOf( "go and grab all of your stuff" ) != -1 )
			{
				KoLConstants.storage.clear();
				KoLConstants.freepulls.clear();
				KoLCharacter.setStorageMeat( 0 );

				// Doing a "pull all" in Hagnk's does not tell
				// you what went into inventory and what went
				// into the closet.
				//
				// Nor does tell you what was left in storage
				// because it was not Trendy enough or because
				// it was not one of your "favorite things".
				//
				// Therefore, refresh Inventory, the Closet,
				// and, if necessary, Storage.

				InventoryManager.refresh();
				CoinmastersFrame.externalUpdate();

				// If we are still in a Trendy run or are pulling only
				// "favorite things", we may have left items in storage.

				// Refresh and check.
				if ( KoLCharacter.isTrendy() || urlString.indexOf( "favonly=1" ) != -1 )
				{
					RequestThread.postRequest( new StorageRequest( REFRESH ) );
					KoLCharacter.updateStatus();
					return true;
				}

				transfer = true;
			}
		}

		else if ( urlString.indexOf( "action=takemeat" ) != -1 )
		{
			if ( responseText.indexOf( "Meat out of storage" ) != -1 )
			{
				StorageRequest.transferMeat( urlString );
				transfer = true;
			}
		}

		else if ( urlString.indexOf( "action=pull" ) != -1 )
		{
			if ( responseText.indexOf( "moved from storage to inventory" ) != -1 )
			{
				// Pull items from storage and/or freepulls
				StorageRequest.transferItems( responseText );
				transfer = true;
			}
		}

		if ( urlString.indexOf( "ajax=1" ) == -1 )
		{
			StorageRequest.parseStorage( urlString, responseText );
		}

		if ( KoLConstants.storage.isEmpty() && KoLConstants.freepulls.isEmpty() && KoLCharacter.getStorageMeat() == 0 )
		{
			Preferences.setInteger( "lastEmptiedStorage", KoLCharacter.getAscensions() );
		}
		else if ( Preferences.getInteger( "lastEmptiedStorage" ) == KoLCharacter.getAscensions() )
		{
			// Storage is not empty, but we erroneously thought it was
			Preferences.setInteger( "lastEmptiedStorage", -1 );
		}

		if ( transfer )
		{
			KoLCharacter.updateStatus();
		}

		return true;
	}

	// <b>star hat (1)</b> moved from storage to inventory.
	private static final Pattern PULL_ITEM_PATTERN = Pattern.compile( "<b>([^<]*) \\((\\d+)\\)</b> moved from storage to inventory" );

	private static final void transferItems( final String responseText )
	{
		// Transfer items from storage and/or freepulls

		Matcher matcher = StorageRequest.PULL_ITEM_PATTERN.matcher( responseText );
		int pulls = 0;

		while ( matcher.find() )
		{
			String name = matcher.group( 1 );
			int count = StringUtilities.parseInt( matcher.group( 2 ) );

			AdventureResult item = new AdventureResult( name, count, false );

			List source;

			if ( KoLConstants.freepulls.contains( item ) )
			{
				source = KoLConstants.freepulls;
			}
			else
			{
				source = KoLConstants.storage;
				pulls += count;
			}

			// Remove from storage
			AdventureResult.addResultToList( source, item.getNegation() );

			// Add to inventory
			ResultProcessor.processResult( item );
		}

		// If remaining is -1, pulls are unlimited.
		int remaining = ConcoctionDatabase.getPullsRemaining();
		if ( pulls > 0 && remaining >= pulls )
		{
			ConcoctionDatabase.setPullsRemaining( remaining - pulls );
		}
	}

	private static final void transferMeat( final String urlString )
	{
		int meat = TransferItemRequest.transferredMeat( urlString, "amt" );
		KoLCharacter.setStorageMeat( KoLCharacter.getStorageMeat() - meat );
		ResultProcessor.processMeat( meat );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "storage.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=pullall" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Emptying storage" );
			return true;
		}

		if ( urlString.indexOf( "action=takemeat" ) != -1 )
		{
			int meat = TransferItemRequest.transferredMeat( urlString, "amt" );
			String message = "pull: " + meat + " Meat";

			if ( meat > 0 )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( message );
			}

			return true;
		}

		if ( urlString.indexOf( "pull" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"pull", urlString, KoLConstants.storage, 0 );
		}

		return true;
	}

	public boolean allowMementoTransfer()
	{
		return true;
	}

	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	public boolean allowUngiftableTransfer()
	{
		return true;
	}

	public String getStatusMessage()
	{
		switch ( this.moveType )
		{
		case REFRESH:
			return "Retrieving storage list";

		case MEAT:
			return "Examining meat in storage";

		case CONSUMABLES:
			return "Examining consumables in storage";

		case EQUIPMENT:
			return "Examining equipment in storage";

		case MISCELLANEOUS:
			return "Examining miscellaneous items in storage";

		case EMPTY_STORAGE:
			return "Emptying storage";

		case STORAGE_TO_INVENTORY:
			return "Pulling items from storage";

		case PULL_MEAT_FROM_STORAGE:
			return "Pulling meat from storage";

		default:
			return "Unknown request type";
		}
	}


	/**
	 * Handle lots of items being received at once (specifically, from emptying Hangk's),
	 * deferring updates to the end as much as possible.
	 */
	private static void processBulkItems( Object[] items )
	{
		if ( items.length == 0 )
		{
			return;
		}

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing bulk items" );
		}

		KoLmafia.updateDisplay( "Processing, this may take a while..." );

		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult result = (AdventureResult) items[ i ];

			AdventureResult.addResultToList( KoLConstants.inventory, result );
			EquipmentManager.processResult( result );
		}

		// Assume that at least one item in the list required each of
		// these updates:

		CoinmastersFrame.externalUpdate();

		KoLmafia.updateDisplay( "Processing complete." );
	}
}
