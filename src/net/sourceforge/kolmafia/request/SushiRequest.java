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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SushiRequest
	extends CreateItemRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "whichsushi=(\\d+)" );
	private static final Pattern CONSUME_PATTERN = Pattern.compile( "You eat the (.*)\\." );

	public static final Object[][] SUSHI =
	{
		{ new Integer(1), "beefy nigiri" },
		{ new Integer(2), "glistening nigiri" },
		{ new Integer(3), "slick nigiri" },
		{ new Integer(4), "beefy maki" },
		{ new Integer(5), "glistening maki" },
		{ new Integer(6), "slick maki" },
	};

	public SushiRequest( final String name )
	{
		super( "sushi.php", name );
		this.addFormField( "action", "Yep." );
		this.addFormField( "whichsushi", String.valueOf( SushiRequest.nameToId( name ) ) );
	}

	public void reconstructFields()
	{
	}

	public boolean noCreation()
	{
		return true;
	}

	public void run()
	{
		// Make sure a sushi-rolling mat is available.

		if ( !InventoryManager.retrieveItem( ItemPool.SUSHI_ROLLING_MAT ) )
		{
			return;
		}

		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		for ( int i = 1; i <= this.getQuantityNeeded(); ++i )
		{
			KoLmafia.updateDisplay( "Creating/consuming " + this.getName() + " (" + i + " of " + this.getQuantityNeeded() + ")..." );
			super.run();
			SushiRequest.parseConsumption( this.getURLString(), this.responseText );
		}

	}

	public static void parseConsumption( final String location, final String responseText )
	{
		if ( !location.startsWith( "sushi.php" ) )
		{
			return;
		}

		// "That looks good, but you're way too full to eat it right
		// now."

		if ( responseText.indexOf( "too full to eat it" ) != -1 )
		{
			return;
		}

		Matcher m = CONSUME_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			return;
		}

		String name = m.group(1);

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( name );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			ResultProcessor.processResult( ingredient.getInstance( -1 * ingredient.getCount() ) );
		}

		int fullness = ItemDatabase.getFullness( name );
		if ( fullness > 0 )
		{
			Preferences.increment( "currentFullness", fullness );
		}
	}

	public static String idToName( final int id )
	{
		for ( int i = 0; i < SUSHI.length; ++i )
		{
			Object [] sushi = SUSHI[i];
			if ( ((Integer)sushi[0]).intValue() == id )
			{
				return (String)sushi[1];
			}
		}

		return "unknown";
	}

	public static int nameToId( final String name )
	{
		for ( int i = 0; i < SUSHI.length; ++i )
		{
			Object [] sushi = SUSHI[i];
			if ( name.equals( (String)sushi[1] ) )
			{
				return ((Integer)sushi[0]).intValue();
			}
		}

		return -1;
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = SushiRequest.WHICH_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int id = StringUtilities.parseInt( matcher.group( 1 ) );
                String name = SushiRequest.idToName( id );

		StringBuffer buf = new StringBuffer();
		buf.append( "Roll and eat " );
		buf.append( name );
                buf.append( " from " );

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( name );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			if ( i > 0 )
			{
				buf.append( ", " );
			}

			buf.append( ingredient.getCount() );
			buf.append( " " );
			buf.append( ingredient.getName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( buf.toString() );

		return true;
	}
}
