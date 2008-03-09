/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClosetRequest
	extends TransferItemRequest
{
	private static final Pattern CLOSETMEAT_PATTERN = Pattern.compile( "<b>Your closet contains ([\\d,]+) meat\\.</b>" );
	private static final Pattern STORAGEMEAT_PATTERN =
		Pattern.compile( "<b>You have ([\\d,]+) meat in long-term storage.</b>" );

	private static final Pattern PULLS_PATTERN = Pattern.compile( "(\\d+) more" );
	private static final Pattern STORAGE_PATTERN = Pattern.compile( "name=\"whichitem1\".*?</select>", Pattern.DOTALL );
	private static final Pattern OPTION_PATTERN =
		Pattern.compile( "<option[^>]* value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" );

	private int moveType;

	public static final int EMPTY_STORAGE = -1;
	public static final int RETRIEVE_STORAGE = 0;
	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int MEAT_TO_CLOSET = 4;
	public static final int MEAT_TO_INVENTORY = 5;
	public static final int STORAGE_TO_INVENTORY = 6;
	public static final int PULL_MEAT_FROM_STORAGE = 7;

	public ClosetRequest()
	{
		super( "storage.php" );
		this.moveType = ClosetRequest.RETRIEVE_STORAGE;
	}

	public ClosetRequest( final int moveType )
	{
		this( moveType, new Object[ 0 ] );
		this.moveType = moveType;
	}

	public ClosetRequest( final int moveType, final int amount )
	{
		this( moveType, new Object[] { new AdventureResult( AdventureResult.MEAT, amount ) } );
	}

	public ClosetRequest( final int moveType, final Object[] attachments )
	{
		super( "closet.php", attachments );
		this.moveType = moveType;

		// Figure out the actual URL information based on the
		// different request types.

		switch ( moveType )
		{
		case MEAT_TO_CLOSET:
			this.constructURLString( "closet.php?action=addmeat" );
			break;

		case MEAT_TO_INVENTORY:
			this.constructURLString( "closet.php?action=takemeat" );
			break;

		case PULL_MEAT_FROM_STORAGE:
			this.constructURLString( "storage.php?action=takemeat" );
			break;

		case EMPTY_STORAGE:
			this.constructURLString( "storage.php?action=takeall" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case STORAGE_TO_INVENTORY:
			this.constructURLString( "storage.php?action=take" );
			this.source = KoLConstants.storage;
			this.destination = KoLConstants.inventory;
			break;

		case INVENTORY_TO_CLOSET:
			this.constructURLString( "closet.php?action=put" );
			this.source = KoLConstants.inventory;
			this.destination = KoLConstants.closet;
			break;

		case CLOSET_TO_INVENTORY:
			this.constructURLString( "closet.php?action=take" );
			this.source = KoLConstants.closet;
			this.destination = KoLConstants.inventory;
			break;
		}

		// Now, make sure that every request has a password hash
		// attached to it.

		this.addFormField( "pwd" );
	}

	protected boolean retryOnTimeout()
	{
		return this.moveType == ClosetRequest.RETRIEVE_STORAGE;
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

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new ClosetRequest( this.moveType, attachments );
	}

	public String getSuccessMessage()
	{
		switch ( this.moveType )
		{
		case STORAGE_TO_INVENTORY:
			return "moved from storage to inventory";
		case INVENTORY_TO_CLOSET:
			return "moved from inventory to closet";
		case CLOSET_TO_INVENTORY:
			return "moved from closet to inventory";
		}

		return "";
	}

	public final void run()
	{
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.canInteract() )
		{
			switch ( this.moveType )
			{
			case EMPTY_STORAGE:
			case RETRIEVE_STORAGE:
			case STORAGE_TO_INVENTORY:
			case PULL_MEAT_FROM_STORAGE:
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Hagnk's Storage is not available in Bad Moon until you free King Ralph." );
				return;
			}
		}

		super.run();
	}

	public void processResults()
	{
		super.processResults();

		switch ( this.moveType )
		{
		case EMPTY_STORAGE:
			while ( !KoLConstants.storage.isEmpty() )
			{
				ResultProcessor.processResult( (AdventureResult) KoLConstants.storage.remove( 0 ) );
			}

			if ( !this.containsUpdate )
			{
				CharPaneRequest.getInstance().run();
			}

			break;

		case STORAGE_TO_INVENTORY:
		case RETRIEVE_STORAGE:
			this.parseStorage();
			this.handleMeat();
			break;

		case PULL_MEAT_FROM_STORAGE:
			this.parseStorage();
			this.handleMeat();
			break;

		case MEAT_TO_CLOSET:
		case MEAT_TO_INVENTORY:
			this.handleMeat();
			break;
		}
	}

	private void handleMeat()
	{
		if ( this.moveType == ClosetRequest.PULL_MEAT_FROM_STORAGE || this.moveType == ClosetRequest.RETRIEVE_STORAGE || this.moveType == ClosetRequest.STORAGE_TO_INVENTORY )
		{
			Matcher meatInStorageMatcher = ClosetRequest.STORAGEMEAT_PATTERN.matcher( this.responseText );

			if ( meatInStorageMatcher.find() )
			{
				KoLCharacter.setStorageMeat( StringUtilities.parseInt( meatInStorageMatcher.group( 1 ) ) );
			}
			else
			{
				KoLCharacter.setStorageMeat( 0 );
			}

			return;
		}

		// Now, determine how much is left in your closet
		// by locating "Your closet contains x meat" and
		// update the display with that information.

		int beforeMeatInCloset = KoLCharacter.getClosetMeat();
		int afterMeatInCloset = 0;

		Matcher meatInClosetMatcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher( this.responseText );

		if ( meatInClosetMatcher.find() )
		{
			afterMeatInCloset = StringUtilities.parseInt( meatInClosetMatcher.group( 1 ) );
		}

		KoLCharacter.setClosetMeat( afterMeatInCloset );
		ResultProcessor.processResult(
			new AdventureResult( AdventureResult.MEAT, beforeMeatInCloset - afterMeatInCloset ) );
	}

	private void parseStorage()
	{
		List storageContents = KoLConstants.storage;

		// Compute the number of pulls remaining based
		// on the response text.

		Matcher storageMatcher = ClosetRequest.PULLS_PATTERN.matcher( this.responseText );
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

		storageMatcher = ClosetRequest.STORAGE_PATTERN.matcher( this.responseText );
		if ( !storageMatcher.find() )
		{
			return;
		}

		Matcher optionMatcher = ClosetRequest.OPTION_PATTERN.matcher( storageMatcher.group() );
		while ( optionMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( optionMatcher.group( 1 ) );

			if ( ItemDatabase.getItemName( itemId ) == null )
			{
				ItemDatabase.registerItem( itemId, optionMatcher.group( 2 ).trim() );
			}

			AdventureResult result = new AdventureResult( itemId, StringUtilities.parseInt( optionMatcher.group( 3 ) ) );
			AdventureResult.addResultToList( storageContents, result );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "closet.php" ) && urlString.indexOf( "action=take" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"take from closet", urlString, KoLConstants.closet, KoLConstants.inventory, "amt", 0 );
		}

		if ( urlString.startsWith( "closet.php" ) && urlString.indexOf( "action=put" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"add to closet", urlString, KoLConstants.inventory, KoLConstants.closet, "amt", 0 );
		}

		// Only other option is storage transfers.  Therefore,
		// if it's clearly not handling of item transfers in
		// storage, return.

		if ( !urlString.startsWith( "storage.php" ) || urlString.indexOf( "action=takemeat" ) != -1 )
		{
			return false;
		}

		if ( urlString.indexOf( "action=takeall" ) != -1 )
		{
			while ( !KoLConstants.storage.isEmpty() )
			{
				KoLCharacter.processResult( (AdventureResult) KoLConstants.storage.remove( 0 ) );
			}

			return true;
		}

		return TransferItemRequest.registerRequest(
			"pull", urlString, KoLConstants.storage, KoLConstants.inventory, "amt", 0 );
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
			return "Pulling items from storage";

		case RETRIEVE_STORAGE:
			return "Retrieving storage list";

		case INVENTORY_TO_CLOSET:
			return "Placing items into closet";

		case CLOSET_TO_INVENTORY:
			return "Removing items from closet";

		case MEAT_TO_CLOSET:
			return "Placing meat into closet";

		case MEAT_TO_INVENTORY:
			return "Removing meat from closet";

		case PULL_MEAT_FROM_STORAGE:
			return "Pulling meat from storage";

		default:
			return "Unknown request type";
		}
	}
}
