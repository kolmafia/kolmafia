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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoSellRequest
	extends TransferItemRequest
{
	public static final Pattern AUTOSELL_PATTERN = Pattern.compile( "for ([\\d,]+) [Mm]eat" );
	private static final Pattern EMBEDDED_ID_PATTERN = Pattern.compile( "item(\\d+)" );

	public AutoSellRequest( final AdventureResult item )
	{
		this( new AdventureResult[] { item } );
	}

	public AutoSellRequest( final Object[] items )
	{
		super( AutoSellRequest.getSellPage(), items );
	}

	public String getItemField()
	{
		return "whichitem";
	}

	public String getQuantityField()
	{
		return "qty";
	}

	public String getMeatField()
	{
		return "sendmeat";
	}

	private static final String getSellPage()
	{
		// Get the autosell mode the first time we need it
		if ( KoLCharacter.getAutosellMode().equals( "" ) )
		{
			( new AccountRequest() ).run();
		}

		return KoLCharacter.getAutosellMode().equals( "detailed" ) ?
			"sellstuff_ugly.php" : "sellstuff.php";
	}

	public void attachItem( final AdventureResult item, final int index )
	{
		// Autosell: "compact" or "detailed" mode

		// Verify that item actually is autosellable
		if ( ItemDatabase.getPriceById( item.getItemId() ) <= 0 )
		{
			return;
		}

		this.addFormField( "action", "sell" );

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			if ( this.getCapacity() == 1 )
			{
				this.addFormField( "quantity", String.valueOf( item.getCount() ) );
			}

			String itemId = String.valueOf( item.getItemId() );
			this.addFormField( "item" + itemId, itemId );
		}
		else
		{
			if ( this.getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				this.addFormField( "type", "quant" );
				this.addFormField( "howmany", String.valueOf( item.getCount() ) );
			}
			else
			{
				// Otherwise, we are selling all.  As of
				// 2/1/2006, must specify a quantity field even
				// for this - but the value is ignored

				this.addFormField( "type", "all" );
				this.addFormField( "howmany", "1" );
			}

			// This is a multiple selection input field.
			// Therefore, you can give it multiple items.

			this.addFormField( "whichitem[]", String.valueOf( item.getItemId() ), true );
		}
	}

	public int getCapacity()
	{
		// If you are autoselling multiple items, then it depends on
		// which mode you are using.
		return KoLCharacter.getAutosellMode().equals( "detailed" ) ?
			this.getDetailedModeCapacity() :
			this.getCompactModeCapacity();
	}

	public int getCompactModeCapacity()
	{
		for ( int i = 0; i < this.attachments.length; ++i )
		{
			AdventureResult currentAttachment = (AdventureResult) this.attachments[ i ];
			int inventoryCount = currentAttachment.getCount( KoLConstants.inventory );
			if ( inventoryCount == 0 )
			{
				continue;
			}

			int attachmentCount = currentAttachment.getCount();

			// We are in compact mode. If we are not selling
			// everything, we must do it one item at a time
			if ( attachmentCount < inventoryCount )
			{
				return 1;
			}

			// Otherwise, look at remaining items
		}

		return Integer.MAX_VALUE;
	}

	public int getDetailedModeCapacity()
	{
		int mode = 1;

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			AdventureResult currentAttachment = (AdventureResult) this.attachments[ i ];

			int inventoryCount = currentAttachment.getCount( KoLConstants.inventory );
			if ( inventoryCount == 0 )
			{
				continue;
			}

			int attachmentCount = currentAttachment.getCount();

			switch ( mode )
			{
			case 1:
				// We are in detailed "sell all" mode.
				if ( attachmentCount >= inventoryCount )
				{
					continue;
				}

				// ...but no longer
				if ( i == 0 && attachmentCount == inventoryCount - 1 )

				{
					// First item and we're selling one
					// less than max. Switch to detailed
					// "all but one" mode
					mode = 2;
					continue;
				}

				// Switch to "quantity" mode
				this.addFormField( "mode", "3" );
				return 1;

			case 2:
				// We are in detailed "all but one" mode. This
				// item had better also be "all but one"

				if ( attachmentCount == inventoryCount - 1 )
				{
					continue;
				}

				// Nope. Switch to "quantity" mode
				this.addFormField( "mode", "3" );
				return 1;
			}
		}

		// Add detailed "mode" field
		this.addFormField( "mode", String.valueOf( mode ) );

		return Integer.MAX_VALUE;
	}

	public ArrayList generateSubInstances()
	{
		// *** Look at all of the attachments and divide them into
		// *** groups: all, all but one, specific quantities.

		ArrayList subinstances = new ArrayList();

		if ( KoLmafia.refusesContinue() )
		{
			return subinstances;
		}

		// Autosell singleton items only if we buy them again
		boolean allowSingleton = KoLCharacter.canInteract();

		// Autosell memento items only if player doesn't care
		boolean allowMemento = !Preferences.getBoolean( "mementoListActive" );

		int capacity = this.getCapacity();

		ArrayList nextAttachments = new ArrayList();
		int index = 0;

		while ( index < this.attachments.length )
		{
			nextAttachments.clear();

			do
			{
				AdventureResult item = (AdventureResult) this.attachments[ index++ ];

				if ( item == null )
				{
					continue;
				}

				if ( !allowMemento && KoLConstants.mementoList.contains( item ) )
				{
					continue;
				}

				int availableCount = item.getCount( this.source );

				if ( !allowSingleton && KoLConstants.singletonList.contains( item ) )
				{
					availableCount = keepSingleton( item, availableCount );
				}

				if ( availableCount <= 0 )
				{
					continue;
				}

				nextAttachments.add( item.getInstance( Math.min( item.getCount(), availableCount ) ) );
			}
			while ( index < this.attachments.length && nextAttachments.size() < capacity );

			// For each broken-up request, create a new request
			// which will has the appropriate data to post.

			if ( !nextAttachments.isEmpty() )
			{
				TransferItemRequest subinstance = this.getSubInstance( nextAttachments.toArray() );
				subinstance.isSubInstance = true;
				subinstances.add( subinstance );
			}
		}

		return subinstances;
	}

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new AutoSellRequest( attachments );
	}

	public void processResults()
	{
		super.processResults();
		KoLmafia.updateDisplay( "Items sold." );
	}

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

		TransferItemRequest.transferItems( urlString, 
			TransferItemRequest.ITEMID_PATTERN,
			null,
			KoLConstants.inventory, null, quantity );

		KoLCharacter.updateStatus();
		ConcoctionDatabase.refreshConcoctions();
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

		TransferItemRequest.transferItems( urlString, 
			AutoSellRequest.EMBEDDED_ID_PATTERN,
			null,
			KoLConstants.inventory, null, quantity );

		// "You sell your 2 disturbing fanfics to an organ
		// grinder's monkey for 264 Meat."

		Matcher matcher = AutoSellRequest.AUTOSELL_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int amount = StringUtilities.parseInt( matcher.group( 1 ) );
			ResultProcessor.processMeat( amount );

			String message = "You gain " + KoLConstants.COMMA_FORMAT.format( amount ) + " Meat";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		KoLCharacter.updateStatus();
		ConcoctionDatabase.refreshConcoctions();
		return true;
	}

	public boolean allowMementoTransfer()
	{
		return false;
	}

	public boolean allowSingletonTransfer()
	{
		return KoLCharacter.canInteract();
	}

	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	public boolean allowUndisplayableTransfer()
	{
		return true;
	}

	public boolean allowUngiftableTransfer()
	{
		return true;
	}

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
