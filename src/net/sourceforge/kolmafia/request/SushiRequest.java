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

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SushiRequest
	extends CreateItemRequest
{
	private static final Pattern SUSHI_PATTERN = Pattern.compile( "whichsushi=(\\d+)" );
	private static final Pattern TOPPING_PATTERN = Pattern.compile( "whichtopping=(\\d+)" );
	private static final Pattern FILLING1_PATTERN = Pattern.compile( "whichfilling1=(\\d+)" );

	private static final Pattern CONSUME_PATTERN = Pattern.compile( "You eat the ([^.]*)\\." );

	public static final String [] SUSHI =
	{
		"beefy nigiri",
		"glistening nigiri",
		"slick nigiri",
		"beefy maki",
		"glistening maki",
		"slick maki",
		"ancient serpent roll",
		"giant dragon roll",
		"musclebound rabbit roll",
		"python roll",
		"slippery snake roll",
		"sneaky rabbit roll",
		"tricky dragon roll",
		"white rabbit roll",
		"wise dragon roll",
		"Jack LaLanne roll",
		"wizened master roll",
		"eleven oceans roll",
		"magical ancient serpent roll",
		"magical beefy maki",
		"magical giant dragon roll",
		"magical glistening maki",
		"magical musclebound rabbit roll",
		"magical python roll",
		"magical slick maki",
		"magical slippery snake roll",
		"magical sneaky rabbit roll",
		"magical tricky dragon roll",
		"magical white rabbit roll",
		"magical wise dragon roll",
		"magical Jack LaLanne roll",
		"magical wizened master roll",
		"magical eleven oceans roll",
		"salty ancient serpent roll",
		"salty beefy maki",
		"salty giant dragon roll",
		"salty glistening maki",
		"salty musclebound rabbit roll",
		"salty python roll",
		"salty slick maki",
		"salty slippery snake roll",
		"salty sneaky rabbit roll",
		"salty tricky dragon roll",
		"salty white rabbit roll",
		"salty wise dragon roll",
		"salty Jack LaLanne roll",
		"salty wizened master roll",
		"salty eleven oceans roll",
		"electric ancient serpent roll",
		"electric beefy maki",
		"electric giant dragon roll",
		"electric glistening maki",
		"electric musclebound rabbit roll",
		"electric python roll",
		"electric slick maki",
		"electric slippery snake roll",
		"electric sneaky rabbit roll",
		"electric tricky dragon roll",
		"electric white rabbit roll",
		"electric wise dragon roll",
		"electric Jack LaLanne roll",
		"electric wizened master roll",
		"electric eleven oceans roll",
	};

	private static final Object[][] BASE_SUSHI =
	{
		{ IntegerPool.get(1), "beefy nigiri" },
		{ IntegerPool.get(2), "glistening nigiri" },
		{ IntegerPool.get(3), "slick nigiri" },
		{ IntegerPool.get(4), "beefy maki" },
		{ IntegerPool.get(5), "glistening maki" },
		{ IntegerPool.get(6), "slick maki" },
	};

	private static String idToName( final int id )
	{
		for ( int i = 0; i < BASE_SUSHI.length; ++i )
		{
			Object [] sushi = BASE_SUSHI[i];
			if ( ((Integer)sushi[0]).intValue() == id )
			{
				return (String)sushi[1];
			}
		}

		return null;
	}

	private static int nameToId( final String name )
	{
		// Check for base sushi
		for ( int i = 0; i < BASE_SUSHI.length; ++i )
		{
			Object [] sushi = BASE_SUSHI[i];
			if ( name.equals( (String)sushi[1] ) )
			{
				return ((Integer)sushi[0]).intValue();
			}
		}

		// Check for filled sushi
		for ( int i = 0; i < FILLING1.length; ++i )
		{
			Object [] sushi = FILLING1[i];
			if ( name.indexOf( (String)sushi[0] ) != -1 )
			{
				return SushiRequest.nameToId( (String)sushi[1] );
			}
		}

		// Check for topped sushi
		for ( int i = 0; i < TOPPING.length; ++i )
		{
			Object [] sushi = TOPPING[i];
			if ( !name.startsWith( (String)sushi[0] ) )
			{
				continue;
			}
			int index = name.indexOf( " " );
			if ( index != -1 )
			{
				return SushiRequest.nameToId( name.substring( index + 1 ) );
			}
		}

		return -1;
	}

	private static final Object[][] TOPPING =
	{
		{ "salty", IntegerPool.get( ItemPool.SEA_SALT_CRYSTAL ) },
		{ "magical", IntegerPool.get( ItemPool.DRAGONFISH_CAVIAR ) },
		{ "electric", IntegerPool.get( ItemPool.EEL_SAUCE ) },
	};

	private static String toppingToName( final String baseName, final int topping )
	{
		for ( int i = 0; i < TOPPING.length; ++i )
		{
			Object [] sushi = TOPPING[i];
			if ( topping == ((Integer)sushi[1]).intValue() )
			{
				return (String)sushi[0] + " " + baseName;
			}
		}

		return baseName;
	}

	private static int nameToTopping( final String name )
	{
		for ( int i = 0; i < TOPPING.length; ++i )
		{
			Object [] sushi = TOPPING[i];
			if ( name.startsWith( (String)sushi[0] ) )
			{
				return ((Integer)sushi[1]).intValue();
			}
		}

		return -1;
	}

	private static final Object[][] FILLING1 =
	{
		{ "giant dragon roll",
		  "beefy maki", IntegerPool.get( ItemPool.SEA_CUCUMBER ) },
		{ "musclebound rabbit roll",
		  "beefy maki", IntegerPool.get( ItemPool.SEA_CARROT ) },
		{ "python roll",
		  "beefy maki", IntegerPool.get( ItemPool.SEA_AVOCADO ) },
		{ "jack lalanne roll",
		  "beefy maki", IntegerPool.get( ItemPool.SEA_RADISH ) },

		{ "wise dragon roll",
		  "glistening maki", IntegerPool.get( ItemPool.SEA_CUCUMBER ) },
		{ "white rabbit roll",
		  "glistening maki", IntegerPool.get( ItemPool.SEA_CARROT ) },
		{ "ancient serpent roll",
		  "glistening maki", IntegerPool.get( ItemPool.SEA_AVOCADO ) },
		{ "wizened master roll",
		  "glistening maki", IntegerPool.get( ItemPool.SEA_RADISH ) },

		{ "tricky dragon roll",
		  "slick maki", IntegerPool.get( ItemPool.SEA_CUCUMBER ) },
		{ "sneaky rabbit roll",
		  "slick maki", IntegerPool.get( ItemPool.SEA_CARROT ) },
		{ "slippery snake roll",
		  "slick maki", IntegerPool.get( ItemPool.SEA_AVOCADO ) },
		{ "eleven oceans roll",
		  "slick maki", IntegerPool.get( ItemPool.SEA_RADISH ) },
	};

	private static String filling1ToName( final String baseName, final int filling1 )
	{
		for ( int i = 0; i < FILLING1.length; ++i )
		{
			Object [] sushi = FILLING1[i];
			if ( baseName.equals( sushi[1] ) &&
			     filling1 == ((Integer)sushi[2]).intValue() )
			{
				return (String)sushi[0];
			}
		}

		return baseName;
	}

	private static int nameToFilling1( final String name )
	{
		for ( int i = 0; i < FILLING1.length; ++i )
		{
			Object [] sushi = FILLING1[i];
			if ( name.indexOf( (String)sushi[0] ) != -1 )
			{
				return ((Integer)sushi[2]).intValue();
			}
		}

		return -1;
	}

	private static String sushiName( final int id, final int topping, final int filling1 )
	{
		String name = SushiRequest.idToName( id );

		if ( name == null )
		{
			return "unknown";
		}

		if ( filling1 > 0 )
		{
			name = SushiRequest.filling1ToName( name, filling1 );
		}

		if ( topping > 0 )
		{
			name = SushiRequest.toppingToName( name, topping );
		}

		return name;
	}

	private static final String sushiName( final String urlString )
	{
		Matcher matcher = SushiRequest.SUSHI_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}

		int id = StringUtilities.parseInt( matcher.group( 1 ) );
		int topping = 0;
		int filling1 = 0;

		matcher = SushiRequest.TOPPING_PATTERN.matcher( urlString );

		if ( matcher.find() )
		{
			topping = StringUtilities.parseInt( matcher.group( 1 ) );
		}

		matcher = SushiRequest.FILLING1_PATTERN.matcher( urlString );

		if ( matcher.find() )
		{
			filling1 = StringUtilities.parseInt( matcher.group( 1 ) );
		}

		return SushiRequest.sushiName( id, topping, filling1 );
	}

	public SushiRequest( Concoction conc )
	{
		super( "sushi.php", conc );
		this.addFormField( "action", "Yep." );

		// Lower-case it
		String name = StringUtilities.getCanonicalName( conc.getName() );

		int sushi = SushiRequest.nameToId( name );
		if ( sushi > 0 )
		{
			this.addFormField( "whichsushi", String.valueOf( sushi ) );
		}

		int topping = SushiRequest.nameToTopping( name );
		if ( topping > 0 )
		{
			this.addFormField( "whichtopping", String.valueOf( topping ) );
		}

		int filling1 = SushiRequest.nameToFilling1( name );
		if ( filling1 > 0 )
		{
			this.addFormField( "whichfilling1", String.valueOf( filling1 ) );
		}
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
	public boolean noCreation()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Make sure a sushi-rolling mat is available.

		if ( !KoLCharacter.hasSushiMat() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need a sushi rolling mat installed in your kitchen in order to roll sushi." );
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

		String name = SushiRequest.sushiName( location );
		if ( name == null )
		{
			return;
		}

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
			KoLCharacter.updateStatus();
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "sushi.php" ) )
		{
			return false;
		}

		String name = SushiRequest.sushiName( urlString );
		if ( name == null )
		{
			return false;
		}

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
