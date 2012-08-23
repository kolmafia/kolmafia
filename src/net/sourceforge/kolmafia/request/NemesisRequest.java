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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NemesisRequest
	extends GenericRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public NemesisRequest()
	{
		super( "cave.php" );
	}

	public static String getAction( final String urlString )
	{
		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );

		// cave.php is strange:
		// - visit door1 = action=door1
		// - offer to door 1 = action=door1&action=dodoor1

		String action = null;
		while ( matcher.find() )
		{
			action = matcher.group(1);
		}

		return action;
	}

	private static int getItem( final String urlString )
	{
		Matcher matcher = ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return -1;
		}

		return StringUtilities.parseInt( matcher.group(1) );
	}

	@Override
	public void processResults()
	{
		NemesisRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "cave.php" ) )
		{
			return;
		}

		String action = NemesisRequest.getAction( location );
		if ( action == null )
		{
			return;
		}

		int item = NemesisRequest.getItem( location );
		if ( item == -1 )
		{
			return;
		}

		// You put your viking helmet in the hole -- there's a slight
		// resistance, almost as though you're pushing it through some
		// manner of magical field. Then it drops away into darkness,
		// and after a moment the stone slab slides into the ceiling
		// with a loud grinding noise, opening the path before you.

		// The insanely spicy bean burrito slides easily into the hole,
		// and disappears into the darkness. If this cave has a pile of
		// thousands of years worth of rotting burritos in a hole in
		// the wall, that would go a long way towards explaining the
		// smell you noticed earlier.

		// You drop your clown whip into the hole, and the stone slab
		// grinds slowly upward and out of sight, revealing a large
		// cavern. A large cavern with multiple figures moving around
		// inside it. Which is perfect, because you were starting to
		// get sick of this puzzly nonsense, and could really use a
		// regular old fight right about now

		if ( responseText.indexOf( "stone slab slides" ) != -1 ||
		     responseText.indexOf( "into the darkness" ) != -1 ||
		     responseText.indexOf( "stone slab grinds" ) != -1)
		{
			ResultProcessor.processItem( item, -1 );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "cave.php" ) )
		{
			return false;
		}

		String action = NemesisRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		int itemId = NemesisRequest.getItem( urlString );
		String itemName = ItemDatabase.getItemName( itemId );

		String message;

		if ( action.equals( "dodoor4" ) )
		{
			message = "Speaking password to door 4";
		}
		if ( action.equals( "sanctum" ) )
		{
			// Logged elsewhere
			return true;
		}
		else if ( itemId == -1 )
		{
			return true;
		}
		else if ( action.equals( "dodoor1" ) )
		{
			message = "Offering " + itemName + " to door 1";
		}
		else if ( action.equals( "dodoor2" ) )
		{
			message = "Offering " + itemName + " to door 2";
		}
		else if ( action.equals( "dodoor3" ) )
		{
			message = "Offering " + itemName + " to door 3";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
