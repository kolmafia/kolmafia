/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

public class StandardRequest
	extends GenericRequest
{
	// Types: "Items", Bookshelf Books", "Skills", "Familiars", "Clan Items".

	private final static List<String> itemMap = new ArrayList<String>();
	private final static List<String> bookshelfMap = new ArrayList<String>();
	private final static List<String> familiarMap = new ArrayList<String>();
	private final static List<String> skillMap = new ArrayList<String>();
	private final static List<String> clanMap = new ArrayList<String>();
	// There is a Miscellaneous category that doesn't seem useful

	private static final StandardRequest INSTANCE = new StandardRequest();
	private static boolean running = false;

	private static boolean initialized = false;

	public static void reset()
	{
		StandardRequest.initialized = false;
		StandardRequest.itemMap.clear();
		StandardRequest.bookshelfMap.clear();
		StandardRequest.familiarMap.clear();
		StandardRequest.skillMap.clear();
		StandardRequest.clanMap.clear();
	}

	public static void initialize()
	{
		if ( !StandardRequest.initialized && KoLCharacter.getLimitmode() == null )
		{
			RequestThread.postRequest( StandardRequest.INSTANCE );
		}
	}

	public static void initialize( final boolean force )
	{
		if ( KoLCharacter.getLimitmode() == null )
		{
			RequestThread.postRequest( new StandardRequest( force ) );
		}
	}

	private static List<String> typeToList( final String type )
	{
		return	
			type.equals( "Items" ) ? StandardRequest.itemMap :
			type.equals( "Bookshelf Books" ) ? StandardRequest.bookshelfMap :
			type.equals( "Skills" ) ? StandardRequest.skillMap :
			type.equals( "Familiars" ) ? StandardRequest.familiarMap :
			type.equals( "Clan Items" ) ? StandardRequest.clanMap :
			null;
	}

	private static boolean isNotRestricted( final List<String> list, final String key )
	{
		StandardRequest.initialize();
		return list.indexOf( key.toLowerCase() ) == -1;
	}

	public static boolean isNotRestricted( final String type, final String key )
	{
		if ( !KoLCharacter.getRestricted() )
		{
			return true;
		}
		List<String> list = StandardRequest.typeToList( type );
		return list != null && StandardRequest.isNotRestricted( list, key );
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

		return StandardRequest.isAllowedInStandard( type, key );
	}

	public static boolean isAllowedInStandard( String type, final String key )
	{
		if ( type.equals( "Bookshelf" ) )
		{
			type = "Bookshelf Books";
		}
		else if ( type.equals( "Clan Item" ) )
		{
			type = "Clan Items";
		}

		if ( type.equals( "Bookshelf Books" ) )
		{
			// Work around a KoL bug: most restricted books are
			// listed both under Bookshelf Books and Items, but
			// 3 are listed under only one or the other.
			return  StandardRequest.isNotRestricted( "Bookshelf Books", key ) &&
				StandardRequest.isNotRestricted( "Items", key );
		}

		List<String> list = StandardRequest.typeToList( type );
		return list != null && StandardRequest.isNotRestricted( list, key );
	}

	public StandardRequest()
	{
		super( "standard.php" );
	}

	public StandardRequest( final boolean force )
	{
		super( "standard.php" );
		if ( force )
		{
			// Two years before current year
			this.addFormField( "date", "2015-01-01" );
			// Must use GET
			this.constructURLString( this.getFullURLString(), false );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( StandardRequest.running )
		{
			return;
		}

		StandardRequest.running = true;
		KoLmafia.updateDisplay( "Seeing what's still unrestricted today..." );
		super.run();
		StandardRequest.running = false;
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
			StandardRequest.initialized = true;
			return;
		}

		StandardRequest.parseResponse( this.getURLString(), this.responseText );
		KoLmafia.updateDisplay( "Done checking allowed items." );
	}

	// <b>Bookshelf Books</b><p><span class="i">, </span><span class="i">Gygaxian Libram, </span>
	// <span class="i">Libram of BRICKOs, </span><span class="i">Libram of Candy Heart Summoning, </span>
	// <span class="i">Libram of Divine Favors, </span><span class="i">Libram of Love Songs, </span>
	// <span class="i">McPhee's Grimoire of Hilarious Object Summoning, </span><span class="i">Tome of Clip Art, </span>
	// <span class="i">Tome of Snowcone Summoning, </span>
	// <span class="i">Tome of Sugar Shummoning</span><p>
	// <b>Skills</b>

	private static final Pattern STANDARD_PATTERN = Pattern.compile( "<b>(.*?)</b><p>(.*?)<p>" );
	private static final Pattern OBJECT_PATTERN = Pattern.compile( "<span class=\"i\">(.*?)(, )?</span>" );

	public static final void parseResponse( final String location, final String responseText )
	{
		TrendyRequest.reset();

		Matcher matcher = StandardRequest.STANDARD_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String type = matcher.group( 1 );
			List<String> list = StandardRequest.typeToList( type );
			if ( list == null )
			{
				continue;
			}

			Matcher objectMatcher = StandardRequest.OBJECT_PATTERN.matcher( matcher.group( 2 ) );
			while ( objectMatcher.find() )
			{
				String object = objectMatcher.group( 1 ).trim().toLowerCase();
				if ( object.length() > 0 )
				{
					list.add( object );
				}
			}
		}

		// Buggy items that should be on the list but aren't.
		if ( !itemMap.isEmpty() )
		{
			itemMap.add( "actual reality goggles" );
		}

		StandardRequest.initialized = true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "standard.php" ) )
		{
			return false;
		}

		// We don't need to register this in the gCLI or the session log
		return true;
	}
}
