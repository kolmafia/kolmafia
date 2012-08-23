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
import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoSellRequest
	extends TransferItemRequest
{
	public static final Pattern AUTOSELL_PATTERN = Pattern.compile( "for ([\\d,]+) [Mm]eat" );
	private static final Pattern EMBEDDED_ID_PATTERN = Pattern.compile( "item(\\d+)" );

	private boolean setMode = false;

	public AutoSellRequest( final AdventureResult item )
	{
		this( new AdventureResult[] { item } );
	}

	public AutoSellRequest( final Object[] items )
	{
		super( AutoSellRequest.getSellPage(), items );
		this.addFormField( "action", "sell" );
	}

	@Override
	public String getItemField()
	{
		return "whichitem";
	}

	@Override
	public String getQuantityField()
	{
		return "quantity";
	}

	@Override
	public String getMeatField()
	{
		return "sendmeat";
	}

	private static final String getSellPage()
	{
		// Get the autosell mode the first time we need it
		if ( KoLCharacter.getAutosellMode().equals( "" ) )
		{
			RequestThread.postRequest( new AccountRequest( AccountRequest.INVENTORY ) );
		}

		return KoLCharacter.getAutosellMode().equals( "detailed" ) ?
			"sellstuff_ugly.php" : "sellstuff.php";
	}

	@Override
	public void attachItem( final AdventureResult item, final int index )
	{
		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			this.attachDetailedItem( item );
		}
		else
		{
			this.attachCompactItem( item );
		}
	}

	public void attachDetailedItem( final AdventureResult item )
	{
		if ( !this.setMode )
		{
			int count = item.getCount();
			int icount = item.getCount( KoLConstants.inventory );

			if ( count == icount )
			{
				this.addFormField( "mode", "1" );
			}
			else if ( count == icount - 1 )
			{
				this.addFormField( "mode", "2" );
			}
			else
			{
				this.addFormField( "mode", "3" );
				this.addFormField( "quantity", String.valueOf( count ) );
			}

			this.setMode = true;
		}

		String itemId = String.valueOf( item.getItemId() );
		this.addFormField( "item" + itemId, itemId );
	}

	public void attachCompactItem( final AdventureResult item )
	{
		if ( !this.setMode )
		{
			int count = item.getCount();
			int icount = item.getCount( KoLConstants.inventory );

			if ( count == icount )
			{
				// As of 2/1/2006, must specify a quantity
				// field for this - but the value is ignored

				this.addFormField( "type", "all" );
				this.addFormField( "howmany", "1" );
			}
			else if ( count == icount - 1 )
			{
				// As of 2/1/2006, must specify a quantity
				// field for this - but the value is ignored

				this.addFormField( "type", "allbutone" );
				this.addFormField( "howmany", "1" );
			}
			else
			{
				this.addFormField( "type", "quant" );
				this.addFormField( "howmany", String.valueOf( count ) );
			}

			this.setMode = true;
		}

		// This is a multiple selection input field.
		// Therefore, you can give it multiple items.

		this.addFormField( "whichitem[]", String.valueOf( item.getItemId() ), true );
	}

	@Override
	public int getCapacity()
	{
		return Integer.MAX_VALUE;
	}

	@Override
	public ArrayList generateSubInstances()
	{
		ArrayList subinstances = new ArrayList();

		if ( KoLmafia.refusesContinue() )
		{
			return subinstances;
		}

		// Autosell singleton items only if we buy them again
		boolean allowSingleton = KoLCharacter.canInteract();

		// Autosell memento items only if player doesn't care
		boolean allowMemento = !Preferences.getBoolean( "mementoListActive" );

		// Look at all of the attachments and divide them into groups:
		// all, all but one, another quantity

		ArrayList all = new ArrayList();
		ArrayList allButOne = new ArrayList();
		HashSet others = new HashSet();

		for ( int index = 0; index < this.attachments.length; ++index )
		{
			AdventureResult item = (AdventureResult) this.attachments[ index ];

			if ( item == null )
			{
				continue;
			}

			if ( ItemDatabase.getPriceById( item.getItemId() ) <= 0 )
			{
				continue;
			}

			// If this item is already on the "sell all" list, skip
			if ( all.contains( item ) )
			{
				continue;
			}

			if ( !allowMemento && KoLConstants.mementoList.contains( item ) )
			{
				continue;
			}

			int inventoryCount = item.getCount( KoLConstants.inventory );
			int availableCount = inventoryCount;

			if ( !allowSingleton && KoLConstants.singletonList.contains( item ) )
			{
				availableCount = this.keepSingleton( item, availableCount );
			}

			if ( availableCount <= 0 )
			{
				continue;
			}

			int desiredCount = Math.min( item.getCount(), availableCount );
			AdventureResult desiredItem = item.getInstance( desiredCount );

			if ( desiredCount == inventoryCount )
			{
				all.add( desiredItem );
			}
			else if ( desiredCount == inventoryCount - 1 )
			{
				allButOne.add( desiredItem );
			}
			else
			{
				others.add( desiredItem );
			}
		}

		// For each group - individual quantities, all but one, all -
		// create a subinstance.

		// Iterate over remaining items. Each distinct count goes into
		// its own subinstance
		while ( others.size() > 0 )
		{
			ArrayList items = new ArrayList();
			Iterator it = others.iterator();

			int count = -1;
			while ( it.hasNext() )
			{
				AdventureResult item = (AdventureResult) it.next();
				int icount = item.getCount();
				if ( count == -1 )
				{
					count = icount;
				}
				if ( count == icount )
				{
					it.remove();
					items.add( item );
				}
			}

			TransferItemRequest subinstance = this.getSubInstance( items.toArray() );
			subinstance.isSubInstance = true;
			subinstances.add( subinstance );
		}

		if ( allButOne.size() > 0 )
		{
			TransferItemRequest subinstance = this.getSubInstance( allButOne.toArray() );
			subinstance.isSubInstance = true;
			subinstances.add( subinstance );
		}

		if ( all.size() > 0 )
		{
			TransferItemRequest subinstance = this.getSubInstance( all.toArray() );
			subinstance.isSubInstance = true;
			subinstances.add( subinstance );
		}

		return subinstances;
	}

	@Override
	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new AutoSellRequest( attachments );
	}

	@Override
	public void processResults()
	{
		super.processResults();
		KoLmafia.updateDisplay( "Items sold." );
	}

	@Override
	public boolean parseTransfer()
	{
		return AutoSellRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.startsWith( "sellstuff.php" ) )
		{
			return AutoSellRequest.parseCompactAutoSell( urlString, responseText );
		}
		if ( urlString.startsWith( "sellstuff_ugly.php" ) )
		{
			return AutoSellRequest.parseDetailedAutoSell( urlString, responseText );
		}
		return false;
	}

	public static final boolean parseCompactAutoSell( final String urlString, final String responseText )
	{
		int quantity = 1;

		Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
		if ( quantityMatcher.find() )
		{
			quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
		}

		if ( urlString.indexOf( "type=allbutone" ) != -1 )
		{
			quantity = -1;
		}
		else if ( urlString.indexOf( "type=all" ) != -1 )
		{
			quantity = 0;
		}

		ArrayList itemList = TransferItemRequest.getItemList( urlString,
			TransferItemRequest.ITEMID_PATTERN,
			null,
			KoLConstants.inventory, quantity );

		if ( !itemList.isEmpty() )
		{
			AutoSellRequest.processMeat( itemList, null );
			TransferItemRequest.transferItems( itemList, KoLConstants.inventory, null );
			KoLCharacter.updateStatus();
		}

		return true;
	}

	public static final boolean parseDetailedAutoSell( final String urlString, final String responseText )
	{
		int quantity = 1;

		Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( quantityMatcher.find() )
		{
			quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
		}

		if ( urlString.indexOf( "mode=1" ) != -1 )
		{
			quantity = 0;
		}
		else if ( urlString.indexOf( "mode=2" ) != -1 )
		{
			quantity = -1;
		}

		ArrayList itemList = TransferItemRequest.getItemList( urlString,
			AutoSellRequest.EMBEDDED_ID_PATTERN,
			null,
			KoLConstants.inventory, quantity );

		if ( !itemList.isEmpty() )
		{
			AutoSellRequest.processMeat( itemList, responseText );
			TransferItemRequest.transferItems( itemList, KoLConstants.inventory, null );
			KoLCharacter.updateStatus();
		}

		return true;
	}

	private static void processMeat( ArrayList itemList, String responseText )
	{
		if ( KoLCharacter.inFistcore() )
		{
			int donation = 0;

			for ( int i = 0; i < itemList.size(); ++i )
			{
				AdventureResult item = ( (AdventureResult) itemList.get( i ) );
				int price = ItemDatabase.getPriceById( item.getItemId() );
				int count = item.getCount();
				donation += price * count;
			}

			KoLCharacter.makeCharitableDonation( donation );
			return;
		}

		if ( responseText == null )
		{
			return;
		}

		// "You sell your 2 disturbing fanfics to an organ
		// grinder's monkey for 264 Meat."

		Matcher matcher = AutoSellRequest.AUTOSELL_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		int amount = StringUtilities.parseInt( matcher.group( 1 ) );
		ResultProcessor.processMeat( amount );

		String message = "You gain " + KoLConstants.COMMA_FORMAT.format( amount ) + " Meat";
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	@Override
	public boolean allowMementoTransfer()
	{
		return false;
	}

	@Override
	public boolean allowSingletonTransfer()
	{
		return KoLCharacter.canInteract();
	}

	@Override
	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	@Override
	public boolean allowUndisplayableTransfer()
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
		return "Autoselling items to NPCs";
	}

	public static final boolean registerRequest( final String urlString )
	{
		Pattern itemPattern = null;
		Pattern quantityPattern = null;
		int quantity = 1;

		if ( urlString.startsWith( "sellstuff.php" ) )
		{
			Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			if ( urlString.indexOf( "type=allbutone" ) != -1 )
			{
				quantity = -1;
			}
			else if ( urlString.indexOf( "type=all" ) != -1 )
			{
				quantity = 0;
			}

			itemPattern = TransferItemRequest.ITEMID_PATTERN;
		}
		else if ( urlString.startsWith( "sellstuff_ugly.php" ) )
		{
			Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			if ( urlString.indexOf( "mode=1" ) != -1 )
			{
				quantity = 0;
			}
			else if ( urlString.indexOf( "mode=2" ) != -1 )
			{
				quantity = -1;
			}

			itemPattern = AutoSellRequest.EMBEDDED_ID_PATTERN;
		}
		else
		{
			return false;
		}

		return TransferItemRequest.registerRequest(
			"autosell", urlString, itemPattern, quantityPattern, KoLConstants.inventory, quantity );
	}
}
