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
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CurseRequest
	extends GenericRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern PLAYER_PATTERN = Pattern.compile( "(?=.*action=use).*targetplayer=([^&]*)" );
	private static final Pattern QTY_PATTERN = Pattern.compile( "You have ([\\d,]+) more |You don't have any more " );

	private AdventureResult itemUsed;

	public CurseRequest( final AdventureResult item )
	{
		this( item, KoLCharacter.getPlayerId(), "" );
	}

	public CurseRequest( final AdventureResult item, final String target, final String message )
	{
		super( "curse.php" );
		this.itemUsed = item;
		this.addFormField( "action", "use" );
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
		this.addFormField( "targetplayer", target );
	}

	public void run()
	{
		InventoryManager.retrieveItem( this.itemUsed );

		for ( int i = this.itemUsed.getCount(); KoLmafia.permitsContinue() && i > 0; --i )
		{
			KoLmafia.updateDisplay( "Throwing " + this.itemUsed.getName() +
				" at " + this.getFormField( "targetplayer" ) + "..." );
			super.run();
		}
	}

	public void processResults()
	{
		CurseRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "curse.php" ) )
		{
			return;
		}

		Matcher m = CurseRequest.ITEM_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}
		AdventureResult item = ItemPool.get( StringUtilities.parseInt( m.group( 1 ) ), 1 );

		m = CurseRequest.QTY_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			return;
		}
		int qty = m.group( 1 ) == null ? 0
			: StringUtilities.parseInt( m.group( 1 ) );
		qty = item.getCount( KoLConstants.inventory ) - qty;
		if ( qty != 0 )
		{
			item = item.getInstance( qty );
			ResultProcessor.processResult( item.getNegation() );
		}

		m = CurseRequest.PLAYER_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}

		if ( responseText.indexOf( "You don't have that item" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Have not, throw not." );
			return;
		}

		if ( responseText.indexOf( "No message?" ) != -1 ||
			responseText.indexOf( "no message" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "That item requires a message." );
			return;
		}

		if ( responseText.indexOf( "That player could not be found" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, m.group( 1 ) + " evaded your thrown item by the unusual strategy of being nonexistent." );
			return;
		}

		if ( responseText.indexOf( "try again later" ) != -1 ||
			responseText.indexOf( "cannot be used" ) != -1 ||
			responseText.indexOf( "can't use this item" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Can't use the item on that player at the moment." );
			return;
		}

		RequestLogger.updateSessionLog( "throw " + item +
			" at " + m.group( 1 ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "curse.php" ) )
		{
			return false;
		}

		return true;
	}
}
