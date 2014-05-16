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

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class Type69Request
	extends GenericRequest
{
	// Types: "Items", Bookshelf Books", "Skills", "Familiars", "Clan Items".

	private final static List<String> itemMap = new ArrayList<String>();
	private final static List<String> bookshelfMap = new ArrayList<String>();
	private final static List<String> familiarMap = new ArrayList<String>();
	private final static List<String> skillMap = new ArrayList<String>();
	private final static List<String> clanMap = new ArrayList<String>();
	// There is a Miscellaneous category that doesn't seem useful

	private static final Type69Request INSTANCE = new Type69Request();
	private static boolean running = false;

	private static boolean initialized = false;

	public static void reset()
	{
		Type69Request.initialized = false;
		Type69Request.itemMap.clear();
		Type69Request.bookshelfMap.clear();
		Type69Request.familiarMap.clear();
		Type69Request.skillMap.clear();
		Type69Request.clanMap.clear();
	}

	public static void initialize()
	{
		if ( !Type69Request.initialized )
		{
			RequestThread.postRequest( Type69Request.INSTANCE );
		}
	}

	private static List<String> typeToList( final String type )
	{
		return	
			type.equals( "Items" ) ? Type69Request.itemMap :
			type.equals( "Bookshelf Books" ) ? Type69Request.bookshelfMap :
			type.equals( "Skills" ) ? Type69Request.skillMap :
			type.equals( "Familiars" ) ? Type69Request.familiarMap :
			type.equals( "Clan Items" ) ? Type69Request.clanMap :
			null;
	}

	private static boolean isNotRestricted( final List<String> list, final String key )
	{
		Type69Request.initialize();
		return list.indexOf( key.toLowerCase() ) == -1;
	}

	public static boolean isNotRestricted( final String type, final String key )
	{
		if ( !KoLCharacter.getRestricted() )
		{
			return true;
		}
		List<String> list = Type69Request.typeToList( type );
		return list != null && Type69Request.isNotRestricted( list, key );
	}

	public static boolean isAllowed( String type, final String key )
	{
		if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( type, key ) )
		{
			return false;
		}
		if ( !KoLCharacter.getRestricted() )
		{
			return true;
		}

		if ( type.equals( "Bookshelf" ) )
		{
			type = "Bookshelf Books";
		}
		else if ( type.equals( "Clan Item" ) )
		{
			type = "Clan Items";
		}

		List<String> list = Type69Request.typeToList( type );
		return list != null && Type69Request.isNotRestricted( list, key );
	}

	public Type69Request()
	{
		super( "type69.php" );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( Type69Request.running )
		{
			return;
		}

		Type69Request.running = true;
		KoLmafia.updateDisplay( "Seeing what's still unrestricted today..." );
		super.run();
		Type69Request.running = false;
	}

	@Override
	protected boolean processOnFailure()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		if ( this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( "KoL returned a blank page. Giving up." );
			KoLmafia.forceContinue();
			Type69Request.initialized = true;
			return;
		}

		Type69Request.parseResponse( this.getURLString(), this.responseText );
		KoLmafia.updateDisplay( "Done checking allowed items." );
	}

	// <b>Bookshelf Books</b><p><span class="i">, </span><span class="i">Gygaxian Libram, </span>
	// <span class="i">Libram of BRICKOs, </span><span class="i">Libram of Candy Heart Summoning, </span>
	// <span class="i">Libram of Divine Favors, </span><span class="i">Libram of Love Songs, </span>
	// <span class="i">McPhee's Grimoire of Hilarious Object Summoning, </span><span class="i">Tome of Clip Art, </span>
	// <span class="i">Tome of Snowcone Summoning, </span>
	// <span class="i">Tome of Sugar Shummoning</span><p>
	// <b>Skills</b>

	private static final Pattern TYPE69_PATTERN = Pattern.compile( "<b>(.*?)</b><p>(.*?)<p>" );

	public static final void parseResponse( final String location, final String responseText )
	{
		TrendyRequest.reset();

		Matcher matcher = Type69Request.TYPE69_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String type = matcher.group( 1 );
			List<String> list = Type69Request.typeToList( type );
			if ( list == null )
			{
				continue;
			}


			String objects = matcher.group( 2 );
			String[] splits = objects.split( "<span class=\"i\">" );
			for ( int i = 0; i < splits.length; ++i )
			{
				String object = splits[ i ].trim().toLowerCase();
				if ( object.length() == 0 )
				{
					continue;
				}
				int sub = ( i == splits.length - 1 ? 7 : 9 );
				object = object.substring( 0, object.length() - sub );
				list.add( object );
			}
		}

		Type69Request.initialized = true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "type69.php" ) )
		{
			return false;
		}

		// We don't need to register this in the gCLI or the session log
		return true;
	}
}
