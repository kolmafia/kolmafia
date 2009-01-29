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

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClosetRequest
	extends TransferItemRequest
{
	private static final Pattern CLOSETMEAT_PATTERN = Pattern.compile( "<b>Your closet contains ([\\d,]+) meat\\.</b>" );
	private static final Pattern OPTION_PATTERN =
		Pattern.compile( "<option[^>]*? value='?([\\d]+)'?>(.*?)( \\(([\\d,]+)\\))?</option>" );

	private int moveType;

	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int MEAT_TO_CLOSET = 3;
	public static final int MEAT_TO_INVENTORY = 4;

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
                        this.addFormField( "action", "addmeat" );
			break;

		case MEAT_TO_INVENTORY:
                        this.addFormField( "action", "takemeat" );
			break;

		case INVENTORY_TO_CLOSET:
                        this.addFormField( "action", "put" );
			this.source = KoLConstants.inventory;
			this.destination = KoLConstants.closet;
			break;

		case CLOSET_TO_INVENTORY:
                        this.addFormField( "action", "take" );
			this.source = KoLConstants.closet;
			this.destination = KoLConstants.inventory;
			break;
		}
	}

	protected boolean retryOnTimeout()
	{
		return true;
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

	public boolean parseTransfer()
	{
		return ClosetRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		boolean success = false;

		// Determine how much meat is left in your closet by locating
		// "Your closet contains x meat" and update the display with
		// that information.

		Matcher matcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher( responseText );
		int before = KoLCharacter.getClosetMeat();
		int after = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;

		KoLCharacter.setClosetMeat( after );
		ResultProcessor.processMeat( before - after );

		if ( urlString.indexOf( "action=takemeat" ) != -1 )
		{
			success = before != after;
		}
		else if ( urlString.indexOf( "action=addmeat" ) != -1 )
		{
			success = before != after;
		}
		else if ( urlString.indexOf( "action=take" ) != -1 )
		{
			if ( responseText.indexOf( "moved from closet to inventory" ) != -1 )
			{
				TransferItemRequest.transferItems( urlString, 
					KoLConstants.closet,
					KoLConstants.inventory, 0 );
				success = true;
			}
		}
		else if ( urlString.indexOf( "action=put" ) != -1 )
		{
			if ( responseText.indexOf( "moved from inventory to closet" ) != -1 )
			{
				TransferItemRequest.transferItems( urlString, 
					KoLConstants.inventory,
					KoLConstants.closet, 0 );
				success = true;
			}
		}

		if ( success )
		{
			KoLCharacter.updateStatus();
			ConcoctionDatabase.refreshConcoctions();
		}

		return success;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "closet.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=take&" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"take from closet", urlString, KoLConstants.closet, 0 );
		}

		if ( urlString.indexOf( "action=put" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"add to closet", urlString, KoLConstants.inventory, 0 );
		}

		int meat = TransferItemRequest.transferredMeat( urlString, "amt" );
		String message = null;

		if ( urlString.indexOf( "action=addmeat" ) != -1 )
		{
			message = "add to closet: " + meat + " Meat";
		}

		if ( urlString.indexOf( "action=takemeat" ) != -1 )
		{
			message = "take from closet " + meat + " Meat";
		}

		if ( meat > 0 && message != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
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
		case INVENTORY_TO_CLOSET:
			return "Placing items into closet";

		case CLOSET_TO_INVENTORY:
			return "Removing items from closet";

		case MEAT_TO_CLOSET:
			return "Placing meat into closet";

		case MEAT_TO_INVENTORY:
			return "Removing meat from closet";

		default:
			return "Unknown request type";
		}
	}
}
