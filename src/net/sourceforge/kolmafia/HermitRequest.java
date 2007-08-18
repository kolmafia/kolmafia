/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HermitRequest extends KoLRequest
{
	private static boolean checkedForClovers = false;

	public static final AdventureResult WORTHLESS_ITEM = new AdventureResult( 13, 1 );

	private static final int TRINKET_ID = 43;
	private static final int GEWGAW_ID = 44;
	private static final int KNICK_KNACK_ID = 45;

	public static final AdventureResult PERMIT = new AdventureResult( 42, 1 );

	public static final AdventureResult TRINKET = new AdventureResult( TRINKET_ID, 1 );
	public static final AdventureResult GEWGAW = new AdventureResult( GEWGAW_ID, 1 );
	public static final AdventureResult KNICK_KNACK = new AdventureResult( KNICK_KNACK_ID, 1 );

	private static final AdventureResult HACK_SCROLL = new AdventureResult( 567, 1 );
	private static final AdventureResult SUMMON_SCROLL = new AdventureResult( 553, 1 );

	private int itemId, quantity;

	/**
	 * Constructs a new <code>HermitRequest</code> that simply checks
	 * what items the hermit has available.
	 */

	public HermitRequest()
	{
		super( "hermit.php" );

		this.itemId = -1;
		this.quantity = 0;
	}

	/**
	 * Constructs a new <code>HermitRequest</code>.  Note that in order
	 * for the hermit request to successfully run, there must be
	 * <code>KoLSettings</code> specifying the trade that takes place.
	 */

	public HermitRequest( int itemId, int quantity )
	{
		super( "hermit.php" );

		this.itemId = itemId;
		this.quantity = quantity;

		this.addFormField( "action", "trade" );
		this.addFormField( "quantity", String.valueOf( quantity ) );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
		this.addFormField( "pwd" );
	}

	public static final boolean useHermitClover( String location )
	{
		if ( !location.startsWith( "hermit.php" ) || location.indexOf( "whichitem=24" ) == -1 )
			return false;

		if ( !KoLSettings.getBooleanProperty( "cloverProtectActive" ) )
			return false;

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );
		return true;
	}

	/**
	 * Executes the <code>HermitRequest</code>.  This will trade the item
	 * specified in the character's <code>KoLSettings</code> for their
	 * worthless trinket; if the character has no worthless trinkets, this
	 * method will report an error to the StaticEntity.getClient().
	 */

	public void run()
	{
		if ( this.itemId > 0 && this.quantity <= 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Zero is not a valid quantity." );
			return;
		}

		if ( KoLCharacter.hasItem( HACK_SCROLL ) )
			(new ConsumeItemRequest( HACK_SCROLL )).run();

		if ( KoLCharacter.getLevel() >= 9 && KoLCharacter.hasItem( SUMMON_SCROLL ) )
		{
			int itemCount = SUMMON_SCROLL.getCount( inventory );
			(new ConsumeItemRequest( SUMMON_SCROLL.getInstance( itemCount ) )).run();

			if ( KoLCharacter.hasItem( HACK_SCROLL ) )
			{
				(new ConsumeItemRequest( HACK_SCROLL )).run();
				(new ConsumeItemRequest( SUMMON_SCROLL.getInstance( itemCount - 1 ) )).run();
			}
		}

		if ( getWorthlessItemCount() < this.quantity )
			AdventureDatabase.retrieveItem( WORTHLESS_ITEM.getInstance( this.quantity ) );
		else if ( getWorthlessItemCount() == 0 )
			AdventureDatabase.retrieveItem( WORTHLESS_ITEM );

		if ( getWorthlessItemCount() == 0 )
			return;

		this.quantity = Math.min( this.quantity, getWorthlessItemCount() );
		KoLmafia.updateDisplay( "Robbing the hermit..." );

		super.run();
		useHermitClover( this.getURLString() );
	}

	public void processResults()
	{
		if ( !parseHermitTrade( this.getURLString(), this.responseText ) )
		{
			if ( !KoLCharacter.hasItem( PERMIT ) )
			{
				if ( AdventureDatabase.retrieveItem( PERMIT ) )
					this.run();

				return;
			}

			KoLmafia.updateDisplay( ERROR_STATE, "You're not allowed to visit the Hermit." );
			return;
		}

		if ( this.itemId == -1 )
			return;

		// If you don't have enough Hermit Permits, then retrieve the
		// number of hermit permits requested.

		if ( this.responseText.indexOf( "You don't have enough Hermit Permits" ) != -1 )
		{
			if ( AdventureDatabase.retrieveItem( PERMIT.getInstance( this.quantity ) ) )
				this.run();

			return;
		}

		// If the item is unavailable, assume he was asking for clover

		if ( this.responseText.indexOf( "doesn't have that item." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Today is not a clover day." );
			return;
		}

		// If you still didn't acquire items, what went wrong?

		if ( this.responseText.indexOf( "You acquire" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The hermit kept his stuff." );
			return;
		}

		KoLmafia.updateDisplay( "Hermit successfully looted!" );
	}

	public static final boolean parseHermitTrade( String urlString, String responseText )
	{
		// There should be a form, or an indication of item receipt,
		// for all valid hermit requests.

		if ( responseText.indexOf( "hermit.php" ) == -1 && responseText.indexOf( "You acquire" ) == -1 )
			return false;

		// Only check for clovers.  All other items at the hermit
		// are assumed to be static final.

		hermitItems.remove( SewerRequest.CLOVER );

		Matcher cloverMatcher = Pattern.compile( "(\\d+) left in stock for today" ).matcher( responseText );
		if ( cloverMatcher.find() )
			hermitItems.add( SewerRequest.CLOVER.getInstance( Integer.parseInt( cloverMatcher.group(1) ) ) );

		checkedForClovers = true;

		if ( !urlString.startsWith( "hermit.php?" ) )
			return true;

		// If you don't have enough Hermit Permits, then failure,
		// so don't subtract anything.

		if ( responseText.indexOf( "You don't have enough Hermit Permits" ) != -1 )
			return true;

		// If the item is unavailable, assume he was asking for clover

		if ( responseText.indexOf( "doesn't have that item." ) != -1 )
			return true;

		// If you still didn't acquire items, what went wrong?

		if ( responseText.indexOf( "You acquire" ) == -1 )
			return true;

		int quantity = 1;
		Matcher quantityMatcher = SendMessageRequest.QUANTITY_PATTERN.matcher( urlString );

		if ( quantityMatcher.find() )
			quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

		if ( quantity <= getWorthlessItemCount() )
		{
			Matcher itemMatcher = SendMessageRequest.ITEMID_PATTERN.matcher( urlString );
			if ( !itemMatcher.find() )
				return true;

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "hermit " + quantity + " " + TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) ) );

			// Subtract the worthless items in order of their priority;
			// as far as we know, the priority is the item Id.

			if ( responseText.indexOf( "looks confused for a moment" ) == -1 )
				StaticEntity.getClient().processResult( PERMIT.getInstance( 0 - quantity ) );

			quantity -= subtractWorthlessItems( TRINKET, quantity );
			quantity -= subtractWorthlessItems( GEWGAW, quantity );
			subtractWorthlessItems( KNICK_KNACK, quantity );
		}

		return true;
	}

	public static final boolean isWorthlessItem( int itemId )
	{	return itemId == TRINKET_ID || itemId == GEWGAW_ID || itemId == KNICK_KNACK_ID;
	}

	private static final int subtractWorthlessItems( AdventureResult item, int total )
	{
		int count = 0 - Math.min( total, item.getCount( inventory ) );
		StaticEntity.getClient().processResult( item.getInstance( count ) );
		return 0 - count;
	}

	public static final int getWorthlessItemCount()
	{	return TRINKET.getCount( inventory ) + GEWGAW.getCount( inventory ) + KNICK_KNACK.getCount( inventory );
	}

	public static final boolean isCloverDay()
	{
		if ( !checkedForClovers )
			(new HermitRequest()).run();

		return hermitItems.contains( SewerRequest.CLOVER );
	}
}
