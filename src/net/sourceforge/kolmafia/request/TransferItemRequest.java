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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class TransferItemRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item[^=&]*\\d*=([-\\d]+)" );

	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany\\d*=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty\\d+=([\\d]+)" );
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

	public abstract String getItemField();

	public abstract String getQuantityField();

	public abstract String getMeatField();

	public abstract int getCapacity();

	public abstract TransferItemRequest getSubInstance( Object[] attachments );

	public abstract String getStatusMessage();

	private void runSubInstances()
	{
		boolean allowNoDisplay = this.allowUndisplayableTransfer();
		boolean allowNoGift = this.allowUngiftableTransfer();
		boolean allowSingleton = this.allowSingletonTransfer();
		boolean allowNoTrade = this.allowUntradeableTransfer();
		boolean allowMemento = !Preferences.getBoolean( "mementoListActive" ) || this.allowMementoTransfer();
		int capacity = this.getCapacity();

		ArrayList subinstances = new ArrayList();
		int meatAttachment = 0;

		ArrayList nextAttachments = new ArrayList();
		int index1 = 0;

		while ( index1 < this.attachments.length )
		{
			nextAttachments.clear();

			do
			{
				AdventureResult item = (AdventureResult) this.attachments[ index1++ ];

				if ( item == null )
				{
					continue;
				}

				if ( item.getName().equals( AdventureResult.MEAT ) )
				{
					meatAttachment += item.getCount();
					continue;
				}

				if ( !allowNoDisplay && !ItemDatabase.isDisplayable( item.getItemId() ) )
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

				if ( !allowSingleton && KoLConstants.singletonList.contains( item ) && !KoLConstants.closet.contains( item ) )
				{
					continue;
				}

				int availableCount = item.getCount( this.source );
				if ( availableCount > 0 )
				{
					nextAttachments.add( item.getInstance( Math.min( item.getCount(), availableCount ) ) );
				}
			}
			while ( index1 < this.attachments.length && nextAttachments.size() < capacity );

			// For each broken-up request, you create a new sending
			// request which will create the appropriate data to
			// post.

			if ( !KoLmafia.refusesContinue() && !nextAttachments.isEmpty() )
			{
				TransferItemRequest subinstance = this.getSubInstance( nextAttachments.toArray() );
				subinstance.isSubInstance = true;
				subinstances.add( subinstance );
			}
		}

		// Now that you've determined all the sub instances, run
		// all of them.

		if ( subinstances.size() == 0 )
		{
			if ( meatAttachment > 0 )
			{
				this.addFormField( this.getMeatField(), String.valueOf( meatAttachment ) );
			}

			KoLmafia.updateDisplay( this.getStatusMessage() + "..." );
			super.run();

			return;
		}

		TransferItemRequest[] requests = new TransferItemRequest[ subinstances.size() ];
		subinstances.toArray( requests );

		RequestThread.openRequestSequence();

		if ( meatAttachment > 0 )
		{
			requests[ 0 ].addFormField( this.getMeatField(), String.valueOf( meatAttachment ) );
		}

		for ( int i = 0; i < requests.length; ++i )
		{
			if ( requests.length == 1 )
			{
				KoLmafia.updateDisplay( this.getStatusMessage() + "..." );
			}
			else
			{
				KoLmafia.updateDisplay( this.getStatusMessage() + " (request " + ( i + 1 ) + " of " + requests.length + ")..." );
			}

			requests[ i ].run();
		}

		RequestThread.closeRequestSequence();
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public void run()
	{
		// First, check to see how many attachments are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( !this.isSubInstance )
		{
			this.runSubInstances();
			return;
		}

		int capacity = this.getCapacity();

		if ( capacity > 1 )
		{
			for ( int i = 1; i <= this.attachments.length; ++i )
			{
				if ( this.attachments[ i - 1 ] != null )
				{
					this.attachItem( (AdventureResult) this.attachments[ i - 1 ], i );
				}
			}
		}
		else if ( capacity == 1 )
		{
			if ( this.attachments[ 0 ] != null )
			{
				this.attachItem( (AdventureResult) this.attachments[ 0 ], 0 );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from GenericRequest
		// to execute the request.

		TransferItemRequest.hadSendMessageFailure = false;
		super.run();
	}

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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Transfer failed for " + item.toString() );
		}

		int totalMeat = StringUtilities.parseInt( this.getFormField( this.getMeatField() ) );
		if ( totalMeat != 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
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

                for ( int i = 0; i < itemList.size(); ++i )
                {
                        AdventureResult item = ( (AdventureResult) itemList.get( i ) );
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
	}

	public static final ArrayList getItemList( final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final int defaultQuantity )
	{
		ArrayList itemList = new ArrayList();

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

	public static final int transferredMeat( final String urlString, final String field )
	{
		if ( field == null )
		{
			return 0;
		}

		Pattern pattern = Pattern.compile( field + "=([\\d,]+)" );
		Matcher matcher = pattern.matcher( urlString );
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
			command, urlString,
			TransferItemRequest.ITEMID_PATTERN,
			TransferItemRequest.HOWMANY_PATTERN,
			source, defaultQuantity );
	}

	public static final boolean registerRequest( final String command, final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern,
		final List source, final int defaultQuantity )
	{
		ArrayList itemList = TransferItemRequest.getItemList( urlString, itemPattern, quantityPattern, source, defaultQuantity );

		if ( itemList.isEmpty() )
		{
			return false;
		}

		StringBuffer itemListBuffer = new StringBuffer();

		itemListBuffer.append( command );

		Matcher recipientMatcher = TransferItemRequest.RECIPIENT_PATTERN.matcher( urlString );
		if ( recipientMatcher.find() )
		{
			itemListBuffer.append( " to " );
			itemListBuffer.append( KoLmafia.getPlayerName( recipientMatcher.group( 1 ) ) );
		}

		itemListBuffer.append( ": " );

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

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( itemListBuffer.toString() );

		return true;
	}
}
