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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StarChartRequest
	extends CreateItemRequest
{
	private static final Pattern STAR_PATTERN = Pattern.compile( "numstars=(\\d+)" );
	private static final Pattern LINE_PATTERN = Pattern.compile( "numlines=(\\d+)" );

	private int stars, lines;

	public StarChartRequest( final Concoction conc )
	{
		super( "starchart.php", conc );

		AdventureResult[] ingredients = conc.getIngredients();
		if ( ingredients != null )
		{
			for ( int i = 0; i < ingredients.length; ++i )
			{
				switch ( ingredients[ i ].getItemId() )
				{
				case ItemPool.STAR:
					this.stars = ingredients[ i ].getCount();
					break;
				case ItemPool.LINE:
					this.lines = ingredients[ i ].getCount();
					break;
				}
			}
		}

		this.addFormField( "action", "makesomething" );
		this.addFormField( "numstars", String.valueOf( this.stars ) );
		this.addFormField( "numlines", String.valueOf( this.lines ) );
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		// Since we create one at a time, override processResults so
		// superclass method doesn't undo ingredient usage.

		if ( StarChartRequest.parseCreation( this.getURLString(), this.responseText ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't draw that." );
		}
	}

	public static final boolean parseCreation( final String urlString, final String responseText )
	{
                // Your stars and lines combine to form a new item!

		if ( responseText.indexOf( "Your stars and lines combine to form a new item!" ) == -1 )
		{
			return true;
		}

		Matcher starMatcher = StarChartRequest.STAR_PATTERN.matcher( urlString );
		Matcher lineMatcher = StarChartRequest.LINE_PATTERN.matcher( urlString );

		if ( !starMatcher.find() || !lineMatcher.find() )
		{
			return true;
		}

		int stars = StringUtilities.parseInt( starMatcher.group( 1 ) );
		int lines = StringUtilities.parseInt( lineMatcher.group( 1 ) );

		ResultProcessor.processItem( ItemPool.STAR, 0 - stars );
		ResultProcessor.processItem( ItemPool.LINE, 0 - lines );
		ResultProcessor.processItem( ItemPool.STAR_CHART, -1 );

		return false;
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher starMatcher = StarChartRequest.STAR_PATTERN.matcher( urlString );
		Matcher lineMatcher = StarChartRequest.LINE_PATTERN.matcher( urlString );

		if ( !starMatcher.find() || !lineMatcher.find() )
		{
			return true;
		}

		int stars = StringUtilities.parseInt( starMatcher.group( 1 ) );
		int lines = StringUtilities.parseInt( lineMatcher.group( 1 ) );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Draw " + stars + " stars with " + lines + " lines" );

		return true;
	}
}
