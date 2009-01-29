/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StorageRequest
	extends TransferItemRequest
{
	private static final Pattern STORAGEMEAT_PATTERN =
		Pattern.compile( "<b>You have ([\\d,]+) meat in long-term storage.</b>" );

	private static final Pattern PULLS_PATTERN = Pattern.compile( "(\\d+) more" );
	private static final Pattern STORAGE_PATTERN = Pattern.compile( "name=\"whichitem1\".*?</select>", Pattern.DOTALL );
	private static final Pattern FREEPULLS_PATTERN = Pattern.compile( "<select name=whichitem>.*?</select>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN =
		Pattern.compile( "<option[^>]*? value='?([\\d]+)'?>(.*?)( \\(([\\d,]+)\\))?</option>" );

	private int moveType;

	// MeatTransferPanel depends on Closet and Storage meat transfer
	// constants being disjoint
	public static final int PULL_MEAT_FROM_STORAGE = -2;

	public static final int EMPTY_STORAGE = -1;
	public static final int RETRIEVE_STORAGE = 0;
	public static final int STORAGE_TO_INVENTORY = 1;
	public static final int FREEPULL_TO_INVENTORY = 2;

	public StorageRequest()
	{
		super( "storage.php" );
		this.moveType = StorageRequest.RETRIEVE_STORAGE;
	}

	public StorageRequest( final int moveType )
	{
		this( moveType, new Object[ 0 ] );
		this.moveType = moveType;
	}

	public StorageRequest( final int moveType, final int amount )
	{
		this( moveType, new Object[] { new AdventureResult( AdventureResult.MEAT, amount ) } );
	}

	public StorageRequest( final int moveType, final Object[] attachments )
	{
		super( "storage.php", attachments );
		this.moveType = moveType;

		// Figure out the actual URL information based on the
		// different request types.

		switch ( moveType )
		{
		case EMPTY_STORAGE:
                        this.addFormField( "action", "takeall" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case STORAGE_TO_INVENTORY:
                        this.addFormField( "action", "take" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case FREEPULL_TO_INVENTORY:
                        this.addFormField( "action", "freepull" );
			this.source = KoLConstants.freepulls;
			this.destination = KoLConstants.inventory;
			break;

		case PULL_MEAT_FROM_STORAGE:
                        this.addFormField( "action", "takemeat" );
			break;
		}
	}

	protected boolean retryOnTimeout()
	{
		return this.moveType == StorageRequest.RETRIEVE_STORAGE;
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
		return this.moveType == FREEPULL_TO_INVENTORY ? "quantity" : "howmany";
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
		return this.moveType == FREEPULL_TO_INVENTORY ? 1 : 11;
	}

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new StorageRequest( this.moveType, attachments );
	}

	public final void run()
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

		super.run();
	}

	public boolean parseTransfer()
	{
		return StorageRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		boolean success = true;
		boolean transfer = false;

		if ( urlString.indexOf( "freepull" ) != -1 )
		{
			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				// Since "you acquire" items, they have already
				// been added to inventory
				TransferItemRequest.transferItems( urlString, 
					TransferItemRequest.ITEMID_PATTERN,
					TransferItemRequest.QUANTITY_PATTERN,
					KoLConstants.freepulls,
					null, 0 );
				transfer = true;
			}
			else
			{
				success = false;
			}
		}

		else if ( urlString.indexOf( "take" ) != -1 )
		{
			if ( responseText.indexOf( "moved from storage to inventory" ) != -1 )
			{
				TransferItemRequest.transferItems( urlString, 
					KoLConstants.storage,
					KoLConstants.inventory, 0 );
				transfer = true;
			}
			else
			{
				success = false;
			}
		}

		else if ( urlString.indexOf( "action=takeall" ) != -1 )
		{
			// Hagnk leans back and yells something
			// ugnigntelligible to a group of Knob Goblin teegnage
			// delignquegnts, who go and grab all of your stuff
			// from storage and bring it to you.

			if ( responseText.indexOf( "go and grab all of your stuff" ) != -1 )
			{
				Object[] items = KoLConstants.storage.toArray();
				transfer = true;
			}
			else
			{
				success = false;
			}
		}

		else if ( urlString.indexOf( "action=takemeat" ) != -1 )
		{
			transfer = true;
		}

		StorageRequest.parseStorage( responseText );

		Matcher matcher = StorageRequest.STORAGEMEAT_PATTERN.matcher( responseText );
		int meat = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		KoLCharacter.setStorageMeat( meat );

		if ( KoLConstants.storage.isEmpty() && meat == 0 )
		{
			Preferences.setInteger( "lastEmptiedStorage", KoLCharacter.getAscensions() );
		}

		if ( transfer )
		{
			KoLCharacter.updateStatus();
			ConcoctionDatabase.refreshConcoctions();
		}

		return true;
	}

	private static void parseStorage( final String responseText )
	{
		List storageContents = KoLConstants.storage;

		// Compute the number of pulls remaining based
		// on the response text.

		Matcher storageMatcher = StorageRequest.PULLS_PATTERN.matcher( responseText );
		if ( storageMatcher.find() )
		{
			ItemManageFrame.setPullsRemaining( StringUtilities.parseInt( storageMatcher.group( 1 ) ) );
		}
		else if ( KoLCharacter.isHardcore() || !KoLCharacter.canInteract() )
		{
			ItemManageFrame.setPullsRemaining( 0 );
		}
		else
		{
			ItemManageFrame.setPullsRemaining( -1 );
		}

		// Start with an empty list

		if ( !storageContents.isEmpty() )
		{
			return;
		}

		// If there's nothing inside storage, return
		// because there's nothing to parse.

		storageMatcher = StorageRequest.STORAGE_PATTERN.matcher( responseText );
		if ( !storageMatcher.find() )
		{
			return;
		}

		Matcher optionMatcher = StorageRequest.OPTION_PATTERN.matcher( storageMatcher.group() );
		while ( optionMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( optionMatcher.group( 1 ) );
			if ( itemId == 0 )
			{
				continue;
			}

			if ( ItemDatabase.getItemName( itemId ) == null )
			{
				ItemDatabase.registerItem( itemId, optionMatcher.group( 2 ).trim() );
			}

			int count = optionMatcher.group(3) == null ? 1 : StringUtilities.parseInt( optionMatcher.group( 4 ) );
			AdventureResult result = new AdventureResult( itemId, count );
			AdventureResult.addResultToList( storageContents, result );
		}

		storageMatcher = StorageRequest.FREEPULLS_PATTERN.matcher( responseText );
		if ( !storageMatcher.find() )
		{
			return;
		}

		List freepullsContents = KoLConstants.freepulls;
		optionMatcher = StorageRequest.OPTION_PATTERN.matcher( storageMatcher.group() );
		while ( optionMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( optionMatcher.group( 1 ) );
			if ( itemId == 0 )
			{
				continue;
			}

			if ( ItemDatabase.getItemName( itemId ) == null )
			{
				ItemDatabase.registerItem( itemId, optionMatcher.group( 2 ).trim() );
			}

			int count = optionMatcher.group(3) == null ? 1 : StringUtilities.parseInt( optionMatcher.group( 4 ) );
			AdventureResult result = new AdventureResult( itemId, count );
			AdventureResult.addResultToList( freepullsContents, result );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "storage.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "freepull" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"pull", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.QUANTITY_PATTERN,
				KoLConstants.freepulls, 0 );
		}

		if ( urlString.indexOf( "action=takeall" ) != -1 )
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
				ConcoctionDatabase.refreshConcoctions();
			}

			return true;
		}

		if ( urlString.indexOf( "take" ) != -1 )
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
		case EMPTY_STORAGE:
			return "Emptying storage";

		case STORAGE_TO_INVENTORY:
		case FREEPULL_TO_INVENTORY:
			return "Pulling items from storage";

		case RETRIEVE_STORAGE:
			return "Retrieving storage list";

		case PULL_MEAT_FROM_STORAGE:
			return "Pulling meat from storage";

		default:
			return "Unknown request type";
		}
	}
}
