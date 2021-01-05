/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class StandardRequest
	extends GenericRequest
{
	// Types: "Items", "Bookshelf Books", "Skills", "Familiars", "Clan Items".

	private final static Set<String> itemSet = new HashSet<String>();
	private final static Set<String> bookshelfSet = new HashSet<String>();
	private final static Set<String> familiarSet = new HashSet<String>();
	private final static Set<String> skillSet = new HashSet<String>();
	private final static Set<String> clanSet = new HashSet<String>();
	// There is a Miscellaneous category that doesn't seem useful

	private static boolean running = false;

	private static boolean initialized = false;

	public static void reset()
	{
		StandardRequest.initialized = false;
		StandardRequest.itemSet.clear();
		StandardRequest.bookshelfSet.clear();
		StandardRequest.familiarSet.clear();
		StandardRequest.skillSet.clear();
		StandardRequest.clanSet.clear();
	}

	public static void initialize( final boolean force )
	{
		// If we are not logged or are under a Limitmode, don't do this.
		if ( GenericRequest.passwordHash.equals( "" ) || KoLCharacter.getLimitmode() != null )
		{
			return;
		}

		if ( force )
		{
			StandardRequest.reset();
		}

		if ( !StandardRequest.initialized )
		{
			RequestThread.postRequest( new StandardRequest() );
		}
	}

	private static Set<String> typeToSet( final String type )
	{
		return	
			type.equals( "Items" ) ? StandardRequest.itemSet :
			type.equals( "Bookshelf Books" ) ? StandardRequest.bookshelfSet :
			type.equals( "Skills" ) ? StandardRequest.skillSet :
			type.equals( "Familiars" ) ? StandardRequest.familiarSet :
			type.equals( "Clan Items" ) ? StandardRequest.clanSet :
			null;
	}

	private static boolean isNotRestricted( final Set<String> set, final String key )
	{
		StandardRequest.initialize( false );
		return !set.contains( key.toLowerCase() );
	}

	public static boolean isNotRestricted( final String type, final String key )
	{
		if ( !KoLCharacter.getRestricted() )
		{
			return true;
		}
		Set<String> set = StandardRequest.typeToSet( type );
		return set != null && StandardRequest.isNotRestricted( set, key );
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

		Set<String> set = StandardRequest.typeToSet( type );
		return set != null && StandardRequest.isNotRestricted( set, key );
	}

	public StandardRequest()
	{
		super( "standard.php" );
		// Two years before current year
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get( Calendar.YEAR );
		this.addFormField( "date", ( year - 2 ) + "-01-02" );
		// Must use GET
		this.constructURLString( this.getFullURLString(), false );
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
			Set<String> set = StandardRequest.typeToSet( type );
			if ( set == null )
			{
				continue;
			}

			Matcher objectMatcher = StandardRequest.OBJECT_PATTERN.matcher( matcher.group( 2 ) );
			while ( objectMatcher.find() )
			{
				String object = objectMatcher.group( 1 ).trim().toLowerCase();
				if ( object.length() > 0 )
				{
					set.add( object );
				}
			}
		}

		// Buggy items and skills that should be listed but aren't.
		if ( !itemSet.isEmpty() )
		{
			itemSet.add( "actual reality goggles" );
			skillSet.add( "fifteen minutes of flame" );
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
