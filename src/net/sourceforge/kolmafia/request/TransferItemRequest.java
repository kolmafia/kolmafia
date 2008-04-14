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
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item[^=]*\\d*=([-\\d]+)" );

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
		this.addFormField( "pwd" );
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

	public abstract String getSuccessMessage();

	public abstract String getStatusMessage();

	private void runSubInstances()
	{
		int capacity = this.getCapacity();
		ArrayList subinstances = new ArrayList();

		int index1 = 0;

		AdventureResult item = null;
		int availableCount;
		int meatAttachment = 0;

		ArrayList nextAttachments = new ArrayList();
		TransferItemRequest subinstance = null;

		boolean allowNoGift = this.allowUngiftableTransfer();
		boolean allowSingleton = this.allowSingletonTransfer();
		boolean allowNoTrade = this.allowUntradeableTransfer();
		boolean allowMemento = !Preferences.getBoolean( "mementoListActive" ) || this.allowMementoTransfer();

		while ( index1 < this.attachments.length )
		{
			nextAttachments.clear();

			do
			{
				item = (AdventureResult) this.attachments[ index1++ ];

				if ( item.getName().equals( AdventureResult.MEAT ) )
				{
					meatAttachment += item.getCount();
					continue;
				}

				if ( !ItemDatabase.isDisplayable( item.getItemId() ) )
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

				availableCount = item.getCount( this.source );
				if ( availableCount > 0 )
				{
					nextAttachments.add( item.getInstance( Math.min( item.getCount(), availableCount ) ) );
				}
			}
			while ( index1 < this.attachments.length && nextAttachments.size() < capacity );

			// For each broken-up request, you create a new sending request
			// which will create the appropriate data to post.

			if ( !KoLmafia.refusesContinue() && !nextAttachments.isEmpty() )
			{
				subinstance = this.getSubInstance( nextAttachments.toArray() );
				subinstance.isSubInstance = true;
				subinstances.add( subinstance );
			}
		}

		// Now that you've determined all the sub instances, run
		// all of them.

		TransferItemRequest[] requests = new TransferItemRequest[ subinstances.size() ];
		subinstances.toArray( requests );

		if ( requests.length == 0 )
		{
			KoLmafia.updateDisplay( this.getStatusMessage() + "..." );

			if ( meatAttachment > 0 )
			{
				this.addFormField( this.getMeatField(), String.valueOf( meatAttachment ) );
			}

			super.run();
			return;
		}

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
		// Make sure that the message was actually sent -
		// the person could have input an invalid player Id

		if ( this.getSuccessMessage().equals( "" ) || this.responseText.indexOf( this.getSuccessMessage() ) != -1 )
			return;

		TransferItemRequest.hadSendMessageFailure = true;
		boolean shouldUpdateDisplay = TransferItemRequest.willUpdateDisplayOnFailure();
		AdventureResult item;

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			item = (AdventureResult) this.attachments[ i ];

			if ( shouldUpdateDisplay )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "Transfer failed for " + item.toString() );
			}

			if ( this.source == KoLConstants.inventory )
			{
				ResultProcessor.processResult( item );
			}
			else
			{
				AdventureResult.addResultToList( this.source, item );
			}

			if ( this.destination == KoLConstants.inventory )
			{
				ResultProcessor.processResult( item.getNegation() );
			}
			else
			{
				AdventureResult.addResultToList( this.destination, item.getNegation() );
			}
		}

		int totalMeat = StringUtilities.parseInt( this.getFormField( this.getMeatField() ) );
		if ( totalMeat != 0 )
		{
			if ( shouldUpdateDisplay )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Transfer failed for " + totalMeat + " meat" );
			}
			if ( this.source == KoLConstants.inventory )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, totalMeat ) );
			}
		}
	}

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

	public boolean allowUngiftableTransfer()
	{
		return false;
	}

	public static final boolean registerRequest( final String command, final String urlString, final List source,
		final List destination, final String meatField, final int defaultQuantity )
	{
		return TransferItemRequest.registerRequest(
			command, urlString, TransferItemRequest.ITEMID_PATTERN, TransferItemRequest.HOWMANY_PATTERN, source,
			destination, meatField, defaultQuantity );
	}

	public static final boolean registerRequest( final String command, final String urlString,
		final Pattern itemPattern, final Pattern quantityPattern, final List source, final List destination,
		final String meatField, final int defaultQuantity )
	{
		ArrayList itemList = new ArrayList();
		StringBuffer itemListBuffer = new StringBuffer();

		Matcher itemMatcher = itemPattern.matcher( urlString );
		Matcher quantityMatcher = quantityPattern == null ? null : quantityPattern.matcher( urlString );

		itemListBuffer.append( command );

		Matcher recipientMatcher = TransferItemRequest.RECIPIENT_PATTERN.matcher( urlString );
		if ( recipientMatcher.find() )
		{
			itemListBuffer.append( " to " );
			itemListBuffer.append( KoLmafia.getPlayerName( recipientMatcher.group( 1 ) ) );
		}

		itemListBuffer.append( ": " );
		boolean addedItem = false;

		while ( itemMatcher.find() && ( quantityMatcher == null || quantityMatcher.find() ) )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			// One of the "select" options is a zero value for the item id field.
			// Trying to parse it generates an exception, so skip it for now.

			if ( name == null )
			{
				continue;
			}

			String quantityString = quantityMatcher == null ? "" :
				quantityMatcher.group( 1 ).trim();
			
			int quantity = quantityPattern == null ? defaultQuantity :
				quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );

			AdventureResult item = new AdventureResult( itemId, quantity );

			if ( quantity < 1 )
			{
				quantity = quantity + item.getCount( source );
			}

			if ( addedItem )
			{
				itemListBuffer.append( ", " );
			}

			itemList.add( item.getInstance( quantity ) );
			addedItem = true;

			itemListBuffer.append( quantity );
			itemListBuffer.append( " " );
			itemListBuffer.append( name );
		}

		if ( itemList.isEmpty() )
		{
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( itemListBuffer.toString() );

		if ( source != null )
		{
			AdventureResult item;

			for ( int i = 0; i < itemList.size(); ++i )
			{
				item = ( (AdventureResult) itemList.get( i ) ).getNegation();

				if ( source == KoLConstants.inventory )
				{
					ResultProcessor.processResult( item );
				}
				else
				{
					AdventureResult.addResultToList( source, item );
				}
			}
		}

		if ( destination == KoLConstants.collection )
		{
			if ( !KoLConstants.collection.isEmpty() )
			{
				AdventureResult current;
				for ( int i = 0; i < itemList.size(); ++i )
				{
					current = (AdventureResult) itemList.get( i );
					if ( !KoLConstants.collection.contains( current ) )
					{
						( (List) DisplayCaseManager.getShelves().get( 0 ) ).add( current );
					}

					AdventureResult.addResultToList( KoLConstants.collection, current );
				}
			}
		}
		else if ( destination != null )
		{
			AdventureResult item;

			for ( int i = 0; i < itemList.size(); ++i )
			{
				item = (AdventureResult) itemList.get( i );

				if ( destination == KoLConstants.inventory )
				{
					ResultProcessor.processResult( item );
				}
				else
				{
					AdventureResult.addResultToList( destination, item );
				}
			}
		}

		return true;
	}
}
