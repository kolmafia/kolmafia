/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GnomeTinkerRequest
	extends CreateItemRequest
{
	private final AdventureResult[] ingredients;
	private static final Pattern ITEM_PATTERN = Pattern.compile( "item[123]=(\\d+)" );

	public GnomeTinkerRequest( final Concoction conc )
	{
		super( "gnomes.php", conc );

		this.addFormField( "place", "tinker" );
		this.addFormField( "action", "tinksomething" );

		this.ingredients = conc.getIngredients();

		if ( this.ingredients != null && this.ingredients.length == 3 )
		{
			this.addFormField( "item1", String.valueOf( this.ingredients[ 0 ].getItemId() ) );
			this.addFormField( "item2", String.valueOf( this.ingredients[ 1 ].getItemId() ) );
			this.addFormField( "item3", String.valueOf( this.ingredients[ 2 ].getItemId() ) );
		}
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
	public void run()
	{
		// If this doesn't contain a valid number of ingredients,
		// just return from the method call to avoid hitting on
		// the server as a result of a bad mixture in the database.

		if ( this.ingredients == null || this.ingredients.length != 3 )
		{
			return;
		}

		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );
		this.addFormField( "qty", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}

	@Override
	public void processResults()
	{
		// Since we create one at a time, override processResults so
		// superclass method doesn't undo ingredient usage.

		if ( GnomeTinkerRequest.parseCreation( this.getURLString(), this.responseText ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't tinker those." );
		}
	}

	public static final boolean parseCreation( final String urlString, final String responseText )
	{
		// Gnorman deftly assembles your items into something new.
		// Gnorman deftly assembles your items into some new stuff.

		if ( !responseText.contains( "Gnorman deftly assembles your items" ) )
		{
			return true;
		}

		Matcher matcher = GnomeTinkerRequest.ITEM_PATTERN.matcher( urlString );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			ResultProcessor.processItem( itemId, -1 );
		}

		return false;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "gnomes.php" ) || !urlString.contains( "action=tinksomething" ) )
		{
			return false;
		}

		String line = CreateItemRequest.getCreationCommand( "Tinker", urlString, 1, false );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( line );

		return true;
	}
}
