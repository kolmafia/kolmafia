/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class NemesisManager
{
	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( !location.startsWith( "cave.php" ) )
		{
			return;
		}

		if ( location.indexOf( "action=door4" ) != -1 )
		{
			String password = NemesisManager.getPassword();
			if ( password != null )
			{
				int index = buffer.indexOf( "name=\"say\"" );
				if ( index != -1 )
				{
					buffer.insert( index+10, " value=\"" + password + "\"" );
				}
			}
			return;
		}
	}

	// Support for paper strips

	public static final AdventureResult [] PAPER_STRIPS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.CREASED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.CRINKLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.CRUMPLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.FOLDED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RAGGED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RIPPED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.RUMPLED_PAPER_STRIP, 1 ),
		ItemPool.get( ItemPool.TORN_PAPER_STRIP, 1 ),
	};

	public static final void getPaperStrips()
	{
		int lastAscension = Preferences.getInteger( "lastPaperStripReset" );
		int current = KoLCharacter.getAscensions();
		if ( lastAscension < current )
		{
			// If we have all the paper strips, identify them
			for ( int i = 0; i < PAPER_STRIPS.length; ++i )
			{
				AdventureResult it = PAPER_STRIPS[ i ];
				if ( !KoLConstants.inventory.contains( it ) )
				{
					return;
				}
			}

			NemesisManager.identifyPaperStrips();
			return;
		}

		for ( int i = 0; i < PAPER_STRIPS.length; ++i )
		{
			int itemId = PAPER_STRIPS[ i ].getItemId();
			Preferences.setString( "lastPaperStrip" + itemId, "" );
		}
	}

	public static final boolean identifyPaperStrips()
	{
		int lastAscension = Preferences.getInteger( "lastPaperStripReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
		{
			return true;
		}

		KoLmafia.updateDisplay( "Identifying paper strips..." );

		// Identify the eight paper strips

		boolean success = true;
		for ( int i = 0; i < PAPER_STRIPS.length; ++i )
		{
			AdventureResult it = PAPER_STRIPS[ i ];
			if ( !identifyPaperStrip( it.getItemId() ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Could not identify " + it.getName() );
				success = false;
			}
		}

		if ( !success )
		{
			return false;
		}

		Preferences.setInteger( "lastPaperStripReset", KoLCharacter.getAscensions() );

		return true;
	}

	private static final Pattern STRIP_PATTERN = Pattern.compile( "title=\"A (.*?) tear\".*title=\"A (.*?) tear\".*?<b>([A-Z]*)</b></font>", Pattern.DOTALL );

	private static final boolean identifyPaperStrip( final int itemId )
	{
		String description = DebugDatabase.rawItemDescriptionText( itemId, true );
		if ( description == null )
		{
			return false;
		}
		Matcher matcher = NemesisManager.STRIP_PATTERN.matcher( description );
		if ( !matcher.find() )
		{
			return false;
		}

		String left = matcher.group( 1 );
		String right = matcher.group( 2 );
		String word = matcher.group( 3 );

		Preferences.setString( "lastPaperStrip" + itemId, left + ":" + word + ":" + right );
		return true;
	}

	public static final String getPassword()
	{
		if ( !NemesisManager.identifyPaperStrips() )
		{
			return null;
		}

		TreeMap left = new TreeMap();
		TreeMap right = new TreeMap();
		for ( int i = 0; i < PAPER_STRIPS.length; ++i )
		{
			PaperStrip strip = new PaperStrip( PAPER_STRIPS[ i ] ); 
			left.put( strip.left, strip );
			right.put( strip.right, strip );
		}

		PaperStrip[] array = new PaperStrip[ PAPER_STRIPS.length ];

		// Find leftmost paper strip
		Iterator it = left.values().iterator();
		while ( it.hasNext() )
		{
			PaperStrip strip = (PaperStrip) it.next();
			if ( !right.containsKey( strip.left ) )
			{
				array[ 0 ] = strip;
				break;
			}
		}

		// Find remaining paper strips
		PaperStrip strip = array[0];
		for ( int i = 1; i < array.length; ++i )
		{
			strip = (PaperStrip) left.get( strip.right );
			array[ i ] = strip;
		}

		String password = "";
		for ( int i = 0; i < array.length; ++i )
		{
			password += array[ i ].code;
		}

		return password;
	}

	private static class PaperStrip
	{
		public final int itemId;
		public final String left;
		public final String right;
		public final String code;

		public PaperStrip( final AdventureResult item )
		{
			this.itemId = item.getItemId();
			String[] words = Preferences.getString( "lastPaperStrip" + this.itemId ).split( ":" );
			this.left = words.length == 3 ? words[0] : "";
			this.code = words.length == 3 ? words[1] : "";
			this.right = words.length == 3 ? words[2] : "";
		}
	}

	public static final void faceNemesis()
	{
	}
}
