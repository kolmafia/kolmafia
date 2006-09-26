/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public abstract class SendMessageRequest extends KoLRequest
{
	protected static final Pattern ITEMID_PATTERN = Pattern.compile( "item[^=]*\\d*=(\\d*)" );
	protected static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany\\d*=(\\d*)" );
	protected static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity\\d*=([\\d,]+)" );
	protected static final Pattern RECIPIENT_PATTERN = Pattern.compile( "towho=([^=&]*)" );

	protected int meatAttachment;
	protected Object [] attachments;
	protected List source = inventory;
	protected List destination = new ArrayList();
	protected String whichField, quantityField;
	protected boolean isSubInstance = false;

	protected SendMessageRequest( String formSource )
	{
		super( formSource );

		this.meatAttachment = 0;
		this.attachments = new Object[0];

		this.whichField = "whichitem";
		this.quantityField = "howmany";
	}

	protected SendMessageRequest( String formSource, AdventureResult attachment )
	{
		super( formSource );

		if ( attachment.getName().equals( AdventureResult.MEAT ) )
		{
			this.meatAttachment = attachment.getCount();
			this.attachments = new Object[0];
		}
		else
		{
			this.meatAttachment = 0;
			this.attachments = new Object[1];
			this.attachments[0] = attachment;
		}

		this.whichField = "whichitem";
		this.quantityField = "howmany";
	}

	protected SendMessageRequest( String formSource, Object [] attachments, int meatAttachment )
	{
		super( formSource );

		this.meatAttachment = meatAttachment;

		// Check to see if there are any meat attachments in the
		// list of items to be sent.

		int currentSize = attachments.length;
		for ( int i = 0; i < attachments.length; ++i )
		{
			if ( attachments[i] == null )
				continue;

			if ( ((AdventureResult)attachments[i]).getName().equals( AdventureResult.MEAT ) )
			{
				this.meatAttachment += ((AdventureResult)attachments[i]).getCount();
				--currentSize;
			}
		}

		this.attachments = new Object[ currentSize ];
		currentSize = 0;

		for ( int i = 0; i < attachments.length; ++i )
		{
			if ( attachments[i] == null )
				continue;

			if ( !((AdventureResult)attachments[i]).getName().equals( AdventureResult.MEAT ) )
				this.attachments[ currentSize++ ] = attachments[i];
		}

		this.whichField = "whichitem";
		this.quantityField = "howmany";
	}

	protected void attachItem( AdventureResult item, int index )
	{
		String which, quantity;

		if ( getCapacity() > 1 )
		{
			which = whichField + index;
			quantity = quantityField + index;
		}
		else if ( alwaysIndex() )
		{
			which = whichField + "1";
			quantity = quantityField + "1";
		}
		else
		{
			which = whichField;
			quantity = quantityField;
		}

		addFormField( which, String.valueOf( item.getItemID() ) );
		addFormField( quantity, String.valueOf( item.getCount() ) );
	}

	protected boolean alwaysIndex()
	{	return false;
	}

	protected abstract int getCapacity();
	protected abstract SendMessageRequest getSubInstance( Object [] attachments );
	protected abstract String getSuccessMessage();
	protected abstract String getStatusMessage();

	private void runSubInstances()
	{
		int capacity = getCapacity();
		ArrayList subinstances = new ArrayList();

		int index1 = 0;

		AdventureResult item = null;
		int availableCount;

		ArrayList nextAttachments = new ArrayList();
		SendMessageRequest subinstance = null;

		while ( index1 < attachments.length )
		{
			nextAttachments.clear();

			do
			{
				item = (AdventureResult) attachments[index1++];

				if ( !allowUntradeableTransfer() && !TradeableItemDatabase.isTradeable( item.getItemID() ) )
					continue;

				availableCount = item.getCount( source );

				if ( availableCount > 0 )
				{
					if ( item.getCount() > availableCount )
						item = item.getInstance( availableCount );

					nextAttachments.add( item );
				}
			}
			while ( index1 < attachments.length && nextAttachments.size() < capacity );

			// For each broken-up request, you create a new sending request
			// which will create the appropriate data to post.

			if ( !KoLmafia.refusesContinue() && nextAttachments.size() > 0 )
			{
				subinstance = getSubInstance( nextAttachments.toArray() );
				subinstance.isSubInstance = true;
				subinstances.add( subinstance );
			}
		}

		// Now that you've determined all the sub instances, run
		// all of them.

		SendMessageRequest [] requests = new SendMessageRequest[ subinstances.size() ];
		subinstances.toArray( requests );

		if ( requests.length > 1 )
		{
			for ( int i = 0; i < requests.length; ++i )
			{
				KoLmafia.updateDisplay( getStatusMessage() + " (request " + (i+1) + " of " + requests.length + ")..." );
				requests[i].run();
			}
		}
		else if ( requests.length == 1 )
		{
			KoLmafia.updateDisplay( getStatusMessage() + "..." );
			requests[0].run();
		}
	}

	/**
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public final void run()
	{
		// First, check to see how many attachments are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( attachments != null && attachments.length != 0 )
		{
			int capacity = getCapacity();

			if ( !isSubInstance )
			{
				runSubInstances();
				return;
			}

			if ( capacity > 1 )
			{
				for ( int i = 1; i <= attachments.length; ++i )
					if ( attachments[i-1] != null )
						attachItem( (AdventureResult) attachments[i-1], i );
			}
			else if ( capacity == 1 )
			{
				if ( attachments[0] != null )
					attachItem( (AdventureResult) attachments[0], 0 );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from KoLRequest
		// to execute the request.

		super.run();
	}

	protected void processResults()
	{
		// Make sure that the message was actually sent -
		// the person could have input an invalid player ID

		if ( tallyItemTransfer() && (getSuccessMessage().equals( "" ) || responseText.indexOf( getSuccessMessage() ) != -1) )
		{
			// With that done, theneeds to be updated
			// to note that the items were sent.

			for ( int i = 0; i < attachments.length; ++i )
			{
				if ( attachments[i] == null )
					continue;

				if ( source == inventory )
					StaticEntity.getClient().processResult( ((AdventureResult)attachments[i]).getNegation() );
				else
					AdventureResult.addResultToList( source, ((AdventureResult)attachments[i]).getNegation() );

				if ( source == storage && destination == inventory && KoLCharacter.canInteract() )
					KoLCharacter.processResult( (AdventureResult) attachments[i] );
				else if ( destination == inventory )
					StaticEntity.getClient().processResult( (AdventureResult) attachments[i] );
				else
					AdventureResult.addResultToList( destination, (AdventureResult) attachments[i] );
			}

			if ( meatAttachment > 0 )
			{
				if ( source == inventory )
					KoLCharacter.setAvailableMeat( KoLCharacter.getAvailableMeat() - meatAttachment );
				else
					KoLCharacter.setAvailableMeat( KoLCharacter.getAvailableMeat() + meatAttachment );
			}
		}
		else if ( tallyItemTransfer() )
		{
			if ( attachments.length > 0 )
			{
				for ( int i = 0; i < attachments.length; ++i )
					KoLmafia.updateDisplay( PENDING_STATE, "Transfer may have failed for " + attachments[i].toString() );
			}
			else
			{
				KoLmafia.updateDisplay( PENDING_STATE, "Transfer failed when attempting to send " + meatAttachment + " meat." );
			}
		}
	}

	protected boolean allowUntradeableTransfer()
	{	return true;
	}

	protected boolean tallyItemTransfer()
	{	return true;
	}

	public static boolean processRequest( String command, String urlString, List source, int defaultQuantity )
	{	return processRequest( command, urlString, ITEMID_PATTERN, HOWMANY_PATTERN, source, defaultQuantity );
	}

	public static boolean processRequest( String command, String urlString, Pattern itemPattern, Pattern quantityPattern, List source, int defaultQuantity )
	{
		ArrayList itemList = new ArrayList();
		StringBuffer itemListBuffer = new StringBuffer();

		Matcher itemMatcher = itemPattern.matcher( urlString );
		Matcher quantityMatcher = quantityPattern == null ? null : quantityPattern.matcher( urlString );

		while ( itemMatcher.find() && (quantityMatcher == null || quantityMatcher.find()) )
		{
			int itemID = StaticEntity.parseInt( itemMatcher.group(1) );
			int quantity = quantityPattern == null ? defaultQuantity : StaticEntity.parseInt( quantityMatcher.group(1) );
			AdventureResult item = new AdventureResult( itemID, quantity );

			if ( quantity < 1 )
				quantity = quantity + item.getCount( source );

			if ( itemListBuffer.length() > 0 )
				itemListBuffer.append( ", " );

			itemList.add( item.getInstance( source == inventory ? 0 - quantity : quantity ) );

			itemListBuffer.append( quantity );
			itemListBuffer.append( " " );
			itemListBuffer.append( TradeableItemDatabase.getItemName( itemID ) );
		}

		Matcher recipientMatcher = RECIPIENT_PATTERN.matcher( urlString );
		if ( recipientMatcher.find() )
		{
			itemListBuffer.append( " to " );
			itemListBuffer.append( recipientMatcher.group(1) );
		}

		if ( itemList.isEmpty() )
			return true;

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( command + " " + itemListBuffer.toString() );

		for ( int i = 0; i < itemList.size(); ++i )
			StaticEntity.getClient().processResult( (AdventureResult) itemList.get(i) );

		return true;
	}
}
