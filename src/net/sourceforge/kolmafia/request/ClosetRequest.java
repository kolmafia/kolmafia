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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONException;
import org.json.JSONObject;

public class ClosetRequest
	extends TransferItemRequest
{
	private int moveType;

	public static final int REFRESH = 0;
	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int MEAT_TO_CLOSET = 3;
	public static final int MEAT_TO_INVENTORY = 4;
	public static final int EMPTY_CLOSET = 5;

	public static void refresh()
	{
		// Clear our current idea of the closet
		KoLConstants.closet.clear();

		// To refresh closet, we get meat from any page
		// and items from api.php

		RequestThread.postRequest( new ClosetRequest( REFRESH ) );
		RequestThread.postRequest( new ApiRequest( "closet" ) );
	}

	public ClosetRequest()
	{
		super( "closet.php" );
		this.moveType = REFRESH;
	}

	public ClosetRequest( final int moveType )
	{
		this( moveType, new Object[ 0 ] );
		this.moveType = moveType;
	}

	public ClosetRequest( final int moveType, final int amount )
	{
		this( moveType, new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public ClosetRequest( final int moveType, final Object attachment )
	{
		this( moveType, new Object[] { attachment } );
	}

	public ClosetRequest( final int moveType, final Object[] attachments )
	{
		super( ClosetRequest.pickURL( moveType ), attachments );
		this.moveType = moveType;

		// Figure out the actual URL information based on the
		// different request types.

		switch ( moveType )
		{
		case REFRESH:
			// It doesn't matter which page we visit to get Meat
			this.addFormField( "which", "1" );
			break;

		case MEAT_TO_CLOSET:
			// closet.php?action=addtakeclosetmeat&addtake=add&pwd&quantity=x
			this.addFormField( "action", "addtakeclosetmeat" );
			this.addFormField( "addtake", "add" );
			break;

		case MEAT_TO_INVENTORY:
			// closet.php?action=addtakeclosetmeat&addtake=take&pwd&quantity=x
			this.addFormField( "action", "addtakeclosetmeat" );
			this.addFormField( "addtake", "take" );
			break;

		case INVENTORY_TO_CLOSET:
			// fillcloset.php?action=closetpush&whichitem=4511&qty=xxx&pwd&ajax=1
			// fillcloset.php?action=closetpush&whichitem=4511&qty=all&pwd&ajax=1
			this.addFormField( "action", "closetpush" );
			this.addFormField( "ajax", "1" );
			this.source = KoLConstants.inventory;
			this.destination = KoLConstants.closet;
			break;

		case CLOSET_TO_INVENTORY:
			// closet.php?action=closetpull&whichitem=4511&qty=xxx&pwd&ajax=1
			// closet.php?action=closetpull&whichitem=4511&qty=all&pwd&ajax=1
			this.addFormField( "action", "closetpull" );
			this.addFormField( "ajax", "1" );
			this.source = KoLConstants.closet;
			this.destination = KoLConstants.inventory;
			break;

		case EMPTY_CLOSET:
			// closet.php?action=pullallcloset&pwd
			this.addFormField( "action", "pullallcloset" );
			this.source = KoLConstants.closet;
			this.destination = KoLConstants.inventory;
			break;
		}
	}

	private static String pickURL( final int moveType )
	{
		switch ( moveType )
		{
		case INVENTORY_TO_CLOSET:
		case CLOSET_TO_INVENTORY:
			return "inventory.php";
		default:
			return "closet.php";
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
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
		return "qty";
	}

	@Override
	public String getMeatField()
	{
		return "quantity";
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
		return 1;
	}

	@Override
	public boolean forceGETMethod()
	{
		return this.moveType == INVENTORY_TO_CLOSET || this.moveType == CLOSET_TO_INVENTORY;
	}

	@Override
	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new ClosetRequest( this.moveType, attachments );
	}

	@Override
	public boolean parseTransfer()
	{
		return ClosetRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	@Override
	public void run()
	{
		// If it's a transfer, let TransferItemRequest handle it
		super.run();
	}

	@Override
	public void processResults()
	{
		switch ( this.moveType )
		{
		case ClosetRequest.REFRESH:
			ClosetRequest.parseCloset( this.getURLString(), this.responseText );
			return;
		default:
			super.processResults();
		}
	}

	// Your closet contains <b>170,000,000</b> meat.
	private static final Pattern CLOSETMEAT_PATTERN = Pattern.compile( "Your closet contains <b>([\\d,]+)</b> meat\\." );

	public static void parseCloset( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "closet.php" ) )
		{
			return;
		}

		Matcher meatInClosetMatcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher( responseText );

		if ( meatInClosetMatcher.find() )
		{
			String meatInCloset = meatInClosetMatcher.group( 1 );
			KoLCharacter.setClosetMeat( StringUtilities.parseInt( meatInCloset ) );
		}

	}

	private static void addClosetItem( AdventureResult item )
	{
		List list = KoLConstants.closet;
		int count = item.getCount();
		int closetCount = item.getCount( list );

		// Add the difference between your existing count
		// and the original count.

		if ( closetCount != count )
		{
			item = item.getInstance( count - closetCount );
			AdventureResult.addResultToList( list, item );
		}
	}

	public static final void parseCloset( final JSONObject JSON )
		throws JSONException
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
			ClosetRequest.addClosetItem( item );
		}
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action" ) == -1 )
		{
			ClosetRequest.parseCloset( urlString, responseText );
			return true;
		}

		boolean success = false;

		if ( urlString.indexOf( "action=addtakeclosetmeat" ) != -1 )
		{
			// Determine how much meat is left in your closet by locating
			// "Your closet contains x meat" and update the display with
			// that information.

			Matcher matcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher( responseText );
			int before = KoLCharacter.getClosetMeat();
			int after = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;

			KoLCharacter.setClosetMeat( after );
			success = before != after;
		}
		else if ( urlString.indexOf( "action=closetpull" ) != -1 )
		{
			if ( responseText.indexOf( "You acquire" ) == -1 )
			{
				return false;
			}

			// Since "you acquire" items, they have already been
			// added to inventory

			TransferItemRequest.transferItems( urlString,
					TransferItemRequest.ITEMID_PATTERN,
					TransferItemRequest.QTY_PATTERN,
					KoLConstants.closet,
					null, 0 );
			success = true;
		}
		else if ( urlString.indexOf( "action=closetpush" ) != -1 )
		{
			if ( responseText.indexOf( "in your closet" ) != -1 )
			{
				TransferItemRequest.transferItems( urlString,
					TransferItemRequest.ITEMID_PATTERN,
					TransferItemRequest.QTY_PATTERN,
					KoLConstants.inventory,
					KoLConstants.closet, 0 );
				success = true;
			}
		}
		else if ( urlString.indexOf( "action=pullallcloset" ) != -1 )
		{
			if ( responseText.indexOf( "taken from your closet" ) == -1 )
			{
				return false;
			}

			TransferItemRequest.transferItems(
				new ArrayList( KoLConstants.closet ),
				KoLConstants.closet,
				KoLConstants.inventory );
			success = true;
		}

		if ( success )
		{
			KoLCharacter.updateStatus();
		}

		return success;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "closet.php" ) &&
		     !urlString.startsWith( "fillcloset.php" ) &&
		     !urlString.startsWith( "inventory.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=closetpull" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"take from closet", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.QTY_PATTERN,
				KoLConstants.closet, 0 );
		}

		if ( urlString.indexOf( "action=closetpush" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"add to closet", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.QTY_PATTERN,
				KoLConstants.inventory, 0 );
		}

		int meat = TransferItemRequest.transferredMeat( urlString, "quantity" );
		String message = null;

		if ( urlString.indexOf( "action=addtakeclosetmeat" ) != -1 )
		{
			if ( urlString.indexOf( "addtake=add" ) != -1 )
			{
				message = "add to closet: " + meat + " Meat";
			}
			else if ( urlString.indexOf( "addtake=take" ) != -1 )
			{
				message = "take from closet " + meat + " Meat";
			}
		}

		if ( meat > 0 && message != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
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
			return "Examining Meat in closet";

		case INVENTORY_TO_CLOSET:
			return "Placing items into closet";

		case CLOSET_TO_INVENTORY:
			return "Removing items from closet";

		case MEAT_TO_CLOSET:
			return "Placing meat into closet";

		case MEAT_TO_INVENTORY:
			return "Removing meat from closet";

		case EMPTY_CLOSET:
			return "Emptying closet";

		default:
			return "Unknown request type";
		}
	}
}
