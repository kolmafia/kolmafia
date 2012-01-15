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

import java.util.regex.Matcher;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CombineMeatRequest
	extends CreateItemRequest
{
	public CombineMeatRequest( final Concoction conc )
	{
		super( "craft.php", conc );

		this.addFormField( "action", "makepaste" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
		this.addFormField( "ajax", "1" );
	}

	public static int getCost( int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.MEAT_PASTE:
			return 10;
		case ItemPool.MEAT_STACK:
			return 100;
		case ItemPool.DENSE_STACK:
			return 1000;
		}
		return 0;
	}

	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	public void run()
	{
		String name = this.getName();
		int count = this.getQuantityNeeded();
		int cost = CombineMeatRequest.getCost( this.getItemId() );

		if ( cost * count > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Insufficient funds to make " + count + " " + name );
			return;
		}

		KoLmafia.updateDisplay( "Creating " + count + " " + name + "..." );
		this.addFormField( "qty", String.valueOf( count ) );
		super.run();
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher itemMatcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return false;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int cost = CombineMeatRequest.getCost( itemId );

		if ( cost == 0 )
		{
			return false;
		}

		Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
		int quantity = quantityMatcher.find() ? StringUtilities.parseInt( quantityMatcher.group( 2 ) ) : 1;
		int total = cost * quantity;

		if ( total > KoLCharacter.getAvailableMeat() )
		{
			return true;
		}

		// We can combine meat either through crafting or via the
		// inventory. The former tells you how much meat you lost when
		// it delivers your items, the latter does not.

		if ( urlString.startsWith( "inventory.php" ) )
		{
			ResultProcessor.processMeat( 0 - total );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Create " + quantity + " " + ItemDatabase.getItemName( itemId ) );

		return true;
	}
}
