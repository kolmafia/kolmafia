/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
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

import org.json.JSONException;
import org.json.JSONObject;

public class StorageRequest
	extends TransferItemRequest
{
	private int moveType;

	public static final int REFRESH = 0;
	public static final int EMPTY_STORAGE = 1;
	public static final int STORAGE_TO_INVENTORY = 2;
	public static final int PULL_MEAT_FROM_STORAGE = 3;

	public static void refresh()
	{
		// To refresh storage, we get Meat and pulls from the main page
		// and items from api.php

		RequestThread.postRequest( new StorageRequest( REFRESH ) );
		RequestThread.postRequest( new ApiRequest( "storage" ) );
	}

	public static void emptyStorage()
	{
		RequestThread.postRequest( new StorageRequest( EMPTY_STORAGE ) );
	}

	public static final void parseStorage( final JSONObject JSON )
	{
		if ( JSON == null )
		{
			return;
		}

		ArrayList<AdventureResult> items = new ArrayList<AdventureResult>();
		ArrayList<AdventureResult> freepulls = new ArrayList<AdventureResult>();
		ArrayList<AdventureResult> nopulls = new ArrayList<AdventureResult>();

		try
		{
			// {"1":"1","2":"1" ... }
			Iterator< ? > keys = JSON.keys();

			while ( keys.hasNext() )
			{
				String key = (String) keys.next();
				int itemId = StringUtilities.parseInt( key );
				int count = JSON.getInt( key );
				String name = ItemDatabase.getItemDataName( itemId );
				if ( name == null )
				{
					// Fetch descid from api.php?what=item
					// and register new item.
					ItemDatabase.registerItem( itemId );
				}

				AdventureResult item = new AdventureResult( itemId, count );
				ArrayList list =
					KoLCharacter.canInteract() ? items :
					StorageRequest.isFreePull( item ) ? freepulls :
					StorageRequest.isNoPull( item ) ? nopulls :
					items;
				list.add( item );
			}
		}
		catch ( JSONException e )
		{
			ApiRequest.reportParseError( "storage", JSON.toString(), e );
			return;
		}

		KoLConstants.storage.clear();
		KoLConstants.storage.addAll( items );

		KoLConstants.freepulls.clear();
		KoLConstants.freepulls.addAll( freepulls );

		KoLConstants.nopulls.clear();
		KoLConstants.nopulls.addAll( nopulls );

		if ( Preferences.getBoolean( "autoSatisfyWithStorage" ) )
		{
			ConcoctionDatabase.refreshConcoctions( true );
		}
	}

	public StorageRequest()
	{
		super( "storage.php" );
		this.moveType = StorageRequest.REFRESH;
	}

	public StorageRequest( final int moveType )
	{
		this( moveType, new AdventureResult[ 0 ] );
		this.moveType = moveType;
	}

	public StorageRequest( final int moveType, final int amount )
	{
		this( moveType, new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public StorageRequest( final int moveType, final AdventureResult attachment )
	{
		this( moveType, new AdventureResult[] { attachment } );
	}

	public StorageRequest( final int moveType, final AdventureResult[] attachments )
	{
		super( "storage.php", attachments );
		this.moveType = moveType;

		// Figure out the actual URL information based on the
		// different request types.

		switch ( moveType )
		{
		case REFRESH:
			this.addFormField( "which", "5" );
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

	@Override
	protected boolean retryOnTimeout()
	{
		return this.moveType == StorageRequest.REFRESH;
	}

	public int getMoveType()
	{
		return this.moveType;
	}

	@Override
	public String getItemField()
	{
		return "whichitem";
	}

	@Override
	public String getQuantityField()
	{
		return "howmany";
	}

	@Override
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

	@Override
	public int getCapacity()
	{
		return 11;
	}

	@Override
	public boolean forceGETMethod()
	{
		return this.moveType == STORAGE_TO_INVENTORY;
	}

	@Override
	public TransferItemRequest getSubInstance( final AdventureResult[] attachments )
	{
		return new StorageRequest( this.moveType, attachments );
	}

	@Override
	public void run()
	{
		if ( KoLCharacter.isHardcore() )
		{
			switch ( this.moveType )
			{
			case EMPTY_STORAGE:
				KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot empty storage while in Hardcore." );
				return;
			case PULL_MEAT_FROM_STORAGE:
				KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot remove meat from storage while in Hardcore." );
				return;
			case STORAGE_TO_INVENTORY:
				for ( int i = 0; i < this.attachments.length; i++ )
				{
					if ( !KoLConstants.freepulls.contains( this.attachments[i] ) )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot pull a " + 
							  this.attachments[i].getName() + " in Hardcore." );
						return;
					}
				}
				break;
			}
		}

		if ( KoLCharacter.inFistcore() && this.moveType == PULL_MEAT_FROM_STORAGE )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot remove meat from storage in Fistcore" );
			return;
		}

		// Let TransferItemRequest handle it
		super.run();
	}

	@Override
	public void processResults()
	{
		switch ( this.moveType )
		{
		case StorageRequest.REFRESH:
			StorageRequest.parseStorage( this.getURLString(), this.responseText );
			return;
		default:
			super.processResults();
		}
	}

	// <b>You have 178,634,761 meat in long-term storage.</b>
	private static final Pattern STORAGEMEAT_PATTERN =
		Pattern.compile( "<b>You have ([\\d,]+) meat in long-term storage.</b>" );
	private static final Pattern STORAGEMEAT_FIST_PATTERN =
		Pattern.compile( "thinking about the ([\\d,]+) you currently have" );

	private static final Pattern PULLS_PATTERN = Pattern.compile( "<span class=\"pullsleft\">(\\d+)</span>" );

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

		if ( !urlString.contains( "which=5" ) )
		{
			return;
		}

		Matcher meatInStorageMatcher = KoLCharacter.inFistcore() ?
		        StorageRequest.STORAGEMEAT_FIST_PATTERN.matcher( responseText ) :
		        StorageRequest.STORAGEMEAT_PATTERN.matcher( responseText );
		if ( meatInStorageMatcher.find() )
		{
			int meat = StringUtilities.parseInt( meatInStorageMatcher.group( 1 ) );
			KoLCharacter.setStorageMeat( meat );
		}
		else if ( responseText.contains( "Hagnk doesn't have any of your meat" ) )
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

	public static boolean isFreePull( final AdventureResult item )
	{
		// For now, special handling for the few items which are a free
		// pull only for a specific path. If more path-specific free
		// pulls are introduced, we'll define a "Free Pull Path"
		// modifier or something.

		int itemId = item.getItemId();

		if ( ( itemId == ItemPool.BORIS_HELM || itemId == ItemPool.BORIS_HELM_ASKEW ) && !KoLCharacter.inAxecore() )
		{
			return false;
		}

		if ( ( itemId == ItemPool.JARLS_COSMIC_PAN || itemId == ItemPool.JARLS_PAN ) && !KoLCharacter.isJarlsberg() )
		{
			return false;
		}

		return Modifiers.getBooleanModifier( item.getName(), "Free Pull" );
	}

	public static boolean isNoPull( final AdventureResult item )
	{
		return Modifiers.getBooleanModifier( item.getName(), "No Pull" );
	}

	@Override
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

				InventoryManager.refresh();
				ClosetRequest.refresh();
				CoinmastersFrame.externalUpdate();

				// If we are still in a Trendy run or are pulling only
				// "favorite things", we may have left items in storage.

				if ( KoLCharacter.isTrendy() || urlString.indexOf( "favonly=1" ) != -1 )
				{
					StorageRequest.refresh();
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

	@Override
	public boolean allowUngiftableTransfer()
	{
		return true;
	}

	@Override
	public String getStatusMessage()
	{
		switch ( this.moveType )
		{
		case REFRESH:
			return "Examining Meat and pulls in storage";

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
	private static void processBulkItems( AdventureResult[] items )
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
			AdventureResult result = items[ i ];

			AdventureResult.addResultToList( KoLConstants.inventory, result );
			EquipmentManager.processResult( result );
		}

		// Assume that at least one item in the list required each of
		// these updates:

		CoinmastersFrame.externalUpdate();

		KoLmafia.updateDisplay( "Processing complete." );
	}
}
