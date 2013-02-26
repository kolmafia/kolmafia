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
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class TransferItemRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item[^=&]*\\d*=([-\\d]+)" );

	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany\\d*=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty\\d*=([\\d]+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity\\d*=([\\d,]+)" );

	public static final Pattern RECIPIENT_PATTERN = Pattern.compile( "towho=([^=&]+)" );

	private static boolean hadSendMessageFailure = false;
	private static boolean updateDisplayOnFailure = true;

	public Object[] attachments;
	public List source = KoLConstants.inventory;
	public List destination = new ArrayList();
	public boolean isSubInstance = false;

	public TransferItemRequest( final String formSource )
	{
		super( formSource );
		this.attachments = new Object[ 0 ];
	}

	public TransferItemRequest( final String formSource, final AdventureResult attachment )
	{
		this( formSource );

		this.attachments = new Object[ 1 ];
		this.attachments[ 0 ] = attachment;
	}

	public TransferItemRequest( final String formSource, final Object[] attachments )
	{
		this( formSource );
		this.attachments = attachments;
	}

	public void attachItem( final AdventureResult item, final int index )
	{
		String which, quantity;

		if ( this.getCapacity() > 1 )
		{
			which = this.getItemField() + index;
			quantity = this.getQuantityField() + index;
		}
		else if ( this.alwaysIndex() )
		{
			which = this.getItemField() + "1";
			quantity = this.getQuantityField() + "1";
		}
		else
		{
			which = this.getItemField();
			quantity = this.getQuantityField();
		}

		this.addFormField( which, String.valueOf( item.getItemId() ) );
		this.addFormField( quantity, String.valueOf( item.getCount() ) );
	}

	public boolean alwaysIndex()
	{
		return false;
	}

	public boolean forceGETMethod()
	{
		return false;
	}

	public abstract String getItemField();

	public abstract String getQuantityField();

	public abstract String getMeatField();

	public abstract int getCapacity();

	public abstract TransferItemRequest getSubInstance( Object[] attachments );

	public abstract String getStatusMessage();

	private void runSubInstances()
	{
		// Generate the subinstances. Subclasses can override how this
		// is done. The first request will have the Meat, if any.

		ArrayList subinstances = this.generateSubInstances();

		// Run the subinstances.
		TransferItemRequest[] requests = new TransferItemRequest[ subinstances.size() ];
		subinstances.toArray( requests );

		String status = this.getStatusMessage();
		for ( int i = 0; i < requests.length; ++i )
		{
			if ( requests.length == 1 )
			{
				KoLmafia.updateDisplay( status + "..." );
			}
			else
			{
				KoLmafia.updateDisplay( status + " (request " + ( i + 1 ) + " of " + requests.length + ")..." );
			}

			requests[ i ].run();
		}
	}

	public ArrayList generateSubInstances()
	{
		ArrayList<TransferItemRequest> subinstances = new ArrayList<TransferItemRequest>();

		if ( KoLmafia.refusesContinue() )
		{
			return subinstances;
		}

		boolean allowNoDisplay = this.allowUndisplayableTransfer();
		boolean allowNoGift = this.allowUngiftableTransfer();
		boolean allowSingleton = this.allowSingletonTransfer();
		boolean allowNoTrade = this.allowUntradeableTransfer();
		boolean allowMemento = !Preferences.getBoolean( "mementoListActive" ) || this.allowMementoTransfer();
		int capacity = this.getCapacity();

		int meatAttachment = 0;

		ArrayList<AdventureResult> nextAttachments = new ArrayList<AdventureResult>();
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

				if ( item.getName().equals( AdventureResult.MEAT ) )
				{
					meatAttachment += item.getCount();
					continue;
				}

				if ( !allowNoDisplay && ItemDatabase.isQuestItem( item.getItemId() ) )
				{
					continue;
				}

				if ( !allowNoGift && !ItemDatabase.isGiftable( item.getItemId() ) )
				{
					continue;
				}

				if ( !allowNoTrade && !ItemDatabase.isTradeable( item.getItemId() ) )
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

		if ( subinstances.size() == 0 )
		{
			// This can only happen if we are sending no items
			this.isSubInstance = true;
			subinstances.add( this );
		}

		if ( meatAttachment > 0 )
		{
			// Attach all the Meat to the first request
			TransferItemRequest first = (TransferItemRequest) subinstances.get(0);
			first.addFormField( this.getMeatField(), String.valueOf( meatAttachment ) );

		}

		return subinstances;
	}

	public int keepSingleton( final AdventureResult item, final int count )
	{
		// We're doing something dangerous with a singleton item

		// If we are wearing the item, that counts as keeping one
		if ( KoLCharacter.hasEquipped( item ) )
		{
			return count;
		}

		// If there is one in the closet, all is well.
		if ( item.getCount( KoLConstants.closet ) > 0 )
		{
			return count;
		}

		// Otherwise, make sure at least one remains in inventory.
		int icount = item.getCount( KoLConstants.inventory );
		return ( count < icount ) ? count : ( icount > 0 ) ? icount - 1 : 0;
	}

	/**
	 * Runs the request. Note that this does not report an error if it
	 * fails; it merely parses the results to see if any gains were made.
	 */

	@Override
	public void run()
	{
		// First, check to see how many attachments are to be
		// transferred. If there are too many, then you'll need to
		// break up the request

		if ( !this.isSubInstance )
		{
			this.runSubInstances();
			return;
		}

		for ( int i = 1; i <= this.attachments.length; ++i )
		{
			AdventureResult it = (AdventureResult) this.attachments[ i - 1 ];
			if ( it != null && it.isItem() )
			{
				this.attachItem( it, i );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from GenericRequest
		// to execute the request.

		TransferItemRequest.hadSendMessageFailure = false;
		if ( this.forceGETMethod() )
		{
			this.constructURLString( this.getFullURLString(), false );
		}
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.parseTransfer() )
		{
			return;
		}

		TransferItemRequest.hadSendMessageFailure = true;
		if ( !TransferItemRequest.updateDisplayOnFailure )
		{
			return;
		}

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			AdventureResult item = (AdventureResult) this.attachments[ i ];
			KoLmafia.updateDisplay( MafiaState.ERROR,
						"Transfer failed for " + item.toString() );
		}

		int totalMeat = StringUtilities.parseInt( this.getFormField( this.getMeatField() ) );
		if ( totalMeat != 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR,
						"Transfer failed for " + totalMeat + " meat" );
		}
	}

	public abstract boolean parseTransfer();

	public static final boolean hadSendMessageFailure()
	{
		return TransferItemRequest.hadSendMessageFailure;
	}

	public static final boolean willUpdateDisplayOnFailure()
	{
		return TransferItemRequest.updateDisplayOnFailure;
	}

	public static final void setUpdateDisplayOnFailure( final boolean shouldUpdate )
	{
		TransferItemRequest.updateDisplayOnFailure = shouldUpdate;
	}

	public abstract boolean allowMementoTransfer();

	public boolean allowSingletonTransfer()
	{
		return true;
	}

	public abstract boolean allowUntradeableTransfer();

	public boolean allowUndisplayableTransfer()
	{
		return false;
	}

	public boolean allowUngiftableTransfer()
	{
		return false;
	}

	public static final void transferItems( final String urlString,
		final List source, final List destination,
		final int defaultQuantity )
	{
		TransferItemRequest.transferItems(
			urlString,
			TransferItemRequest.ITEMID_PATTERN,
			TransferItemRequest.HOWMANY_PATTERN,
			source, destination, defaultQuantity );
	}

	public static final void transferItems( final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final List destination,
		final int defaultQuantity )
	{
		ArrayList itemList = TransferItemRequest.getItemList( urlString, itemPattern, quantityPattern, source, defaultQuantity );

		if ( itemList.isEmpty() )
		{
			return;
		}

		TransferItemRequest.transferItems( itemList, source, destination );
	}

	public static final int transferItems( ArrayList itemList,
		final List source, final List destination )
	{
		int count = 0;
		for ( int i = 0; i < itemList.size(); ++i )
		{
			AdventureResult item = ( (AdventureResult) itemList.get( i ) );
			count += item.getCount();
			if ( source != null )
			{
				AdventureResult remove = item.getNegation();
				if ( source == KoLConstants.inventory )
				{
					ResultProcessor.processResult( remove );
				}
				else
				{
					AdventureResult.addResultToList( source, remove );
				}
			}

			if ( destination == KoLConstants.collection )
			{
				if ( !KoLConstants.collection.contains( item ) )
				{
					List shelf = (List) DisplayCaseManager.getShelves().get( 0 );
					if ( shelf != null )
					{
						shelf.add( item );
					}
				}

				AdventureResult.addResultToList( KoLConstants.collection, item );
			}
			else if ( destination == KoLConstants.inventory )
			{
				ResultProcessor.processResult( item );
			}
			else if ( destination != null )
			{
				AdventureResult.addResultToList( destination, item );
			}
		}

		return count;
	}

	public static final ArrayList getItemList( final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final int defaultQuantity )
	{
		ArrayList<AdventureResult> itemList = new ArrayList<AdventureResult>();

		Matcher itemMatcher = itemPattern.matcher( urlString );
		Matcher quantityMatcher = quantityPattern == null ? null : quantityPattern.matcher( urlString );

		while ( itemMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			// One of the "select" options is a zero value for the
			// item id field.  Trying to parse it generates an
			// exception, so skip it for now.

			if ( name == null )
			{
				continue;
			}

			int quantity = defaultQuantity;
			if ( quantityMatcher != null && quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			AdventureResult item = new AdventureResult( itemId, quantity );

			if ( quantity < 1 )
			{
				quantity = quantity + item.getCount( source );
			}

			itemList.add( item.getInstance( quantity ) );
		}

		return itemList;
	}

	public static final ArrayList getItemList( final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source )
	{
		// Return only items that are on the source list - no default

		ArrayList<AdventureResult> itemList = new ArrayList<AdventureResult>();

		Matcher itemMatcher = itemPattern.matcher( urlString );
		Matcher quantityMatcher = quantityPattern == null ? null : quantityPattern.matcher( urlString );

		while ( itemMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			// One of the "select" options is a zero value for the
			// item id field.  Trying to parse it generates an
			// exception, so skip it for now.

			if ( name == null )
			{
				continue;
			}

			int quantity = 0;
			if ( quantityMatcher != null && quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			if ( quantity == 0 )
			{
				continue;
			}

			AdventureResult item = new AdventureResult( itemId, quantity );
			if ( item.getCount( source ) == 0 )
			{
				continue;
			}

			itemList.add( item );
		}

		return itemList;
	}

	public static final void transferItems( final String responseText,
		final Pattern itemPattern,
		final List source, final List destination )
	{
		ArrayList itemList = TransferItemRequest.getItemList( responseText, itemPattern );

		if ( itemList.isEmpty() )
		{
			return;
		}

		TransferItemRequest.transferItems( itemList, source, destination );
	}

	public static final Pattern ITEM_PATTERN1 = Pattern.compile( "(.*?) \\((\\d+)\\)" );
	public static final Pattern ITEM_PATTERN2 = Pattern.compile( "^(\\d+) ([^,]*)" );

	public static final ArrayList getItemList( final String responseText, final Pattern itemPattern )
	{
		return TransferItemRequest.getItemList( responseText, itemPattern, TransferItemRequest.ITEM_PATTERN1, TransferItemRequest.ITEM_PATTERN2 );
	}

	public static final ArrayList getItemList( final String responseText, final Pattern outerPattern,
						   final Pattern innerPattern1, final Pattern innerPattern2 )
	{
		ArrayList<AdventureResult> itemList = new ArrayList<AdventureResult>();
		Matcher itemMatcher = outerPattern.matcher( responseText );

		while ( itemMatcher.find() )
		{
			String match = itemMatcher.group( 1 );
			TransferItemRequest.getItemCount( itemList, match, innerPattern1 );
			TransferItemRequest.getCountItem( itemList, match, innerPattern2 );
		}

		return itemList;
	}

	public static final void getItemCount( List<AdventureResult> list, String text, final Pattern pattern )
	{
		if ( pattern == null )
		{
			return;
		}

		Matcher m = pattern.matcher( text );
		while ( m.find() )
		{
			String name = m.group( 1 );
			int count = StringUtilities.parseInt( m.group( 2 ) );
			int itemId = ItemDatabase.getItemId( name, count,  true );
			AdventureResult item = new AdventureResult( itemId, count );
			list.add( item );
		}
	}

	public static final void getCountItem( List<AdventureResult> list, String text, final Pattern pattern )
	{
		if ( pattern == null )
		{
			return;
		}

		Matcher m = pattern.matcher( text );
		while ( m.find() )
		{
			String name = m.group( 2 );
			int count = StringUtilities.parseInt( m.group( 1 ) );
			int itemId = ItemDatabase.getItemId( name, count,  true );
			AdventureResult item = new AdventureResult( itemId, count );
			list.add( item );
		}
	}

	public static final int transferredMeat( final String urlString, final String field )
	{
		if ( field == null )
		{
			return 0;
		}

		Pattern pattern = Pattern.compile( field + "=([\\d,]+)" );
		Matcher matcher = pattern.matcher( GenericRequest.decodeField( urlString ) );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group(1) );
	}

	public static final boolean registerRequest( final String command, final String urlString,
		final List source, final int defaultQuantity )
	{
		return TransferItemRequest.registerRequest(
			command, urlString, source, defaultQuantity, null );
	}

	public static final boolean registerRequest( final String command, final String urlString, final List source, final int defaultQuantity, final String meatField )
	{
		return TransferItemRequest.registerRequest(
			command, urlString,
			TransferItemRequest.ITEMID_PATTERN,
			TransferItemRequest.HOWMANY_PATTERN,
			source, defaultQuantity, meatField );
	}

	public static final boolean registerRequest( final String command, final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final int defaultQuantity )
	{
		return TransferItemRequest.registerRequest(
			command, urlString,
			itemPattern, quantityPattern,
			source, defaultQuantity, null );
	}

	public static final boolean registerRequest( final String command, final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final int defaultQuantity,
		final String meatField )
	{
		Matcher recipientMatcher = TransferItemRequest.RECIPIENT_PATTERN.matcher( urlString );
		boolean recipients = recipientMatcher.find();
		ArrayList itemList = TransferItemRequest.getItemList( urlString, itemPattern, quantityPattern, source, defaultQuantity );
		int meat = TransferItemRequest.transferredMeat( urlString, meatField );

		if ( !recipients && itemList.isEmpty() && meat == 0 )
		{
			return false;
		}

		StringBuilder itemListBuffer = new StringBuilder();
		itemListBuffer.append( command );

		if ( recipients )
		{
			itemListBuffer.append( " to " );
			itemListBuffer.append( ContactManager.getPlayerName( recipientMatcher.group( 1 ) ) );
		}

		if ( !itemList.isEmpty() || meat != 0 )
		{
			itemListBuffer.append( ": " );
		}

		boolean addedItem = false;
		for ( int i = 0; i < itemList.size(); ++i )
		{
			AdventureResult item = ( (AdventureResult) itemList.get( i ) );
			String name = item.getName();
			int quantity = item.getCount();

			if ( addedItem )
			{
				itemListBuffer.append( ", " );
			}
			else
			{
				addedItem = true;
			}

			itemListBuffer.append( quantity );
			itemListBuffer.append( " " );
			itemListBuffer.append( name );
		}

		if ( meat != 0 )
		{
			if ( addedItem )
			{
				itemListBuffer.append( ", " );
			}
			itemListBuffer.append( meat );
			itemListBuffer.append( " Meat" );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( itemListBuffer.toString() );

		return true;
	}
}
