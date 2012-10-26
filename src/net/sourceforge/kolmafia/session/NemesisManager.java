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

package net.sourceforge.kolmafia.session;

import java.util.Iterator;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.DebugDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class NemesisManager
{
	public static final String[][] DOOR_DATA =
	{
		// Class, door1 item, door2 item, door3 item
		{
			KoLCharacter.SEAL_CLUBBER,
			// viking helmet
			"value='37'",
			// insanely spicy bean burrito
			"value='316'",
			// clown whip
			"value='2478'",
		},
		{
			KoLCharacter.TURTLE_TAMER,
			// viking helmet
			"value='37'",
			// insanely spicy bean burrito
			"value='316'",
			// clown buckler
			"value='2477'",
		},
		{
			KoLCharacter.PASTAMANCER,
			// stalk of asparagus
			"value='560'",
			// insanely spicy enchanted bean burrito
			"value='319'",
			// boring spaghetti
			"value='579'",
		},
		{
			KoLCharacter.SAUCEROR,
			// stalk of asparagus
			"value='560'",
			// insanely spicy enchanted bean burrito
			"value='319'",
			// tomato juice of powerful power
			"value='420'",
		},
		{
			KoLCharacter.DISCO_BANDIT,
			// dirty hobo gloves
			"value='565'",
			// insanely spicy jumping bean burrito
			"value='1256'",
		},
		{
			KoLCharacter.ACCORDION_THIEF,
			// dirty hobo gloves
			"value='565'",
			// insanely spicy jumping bean burrito
			"value='1256'",
		},
	};

	private static final void selectDoorItem( final int door, final StringBuffer buffer )
	{
		String myClass = KoLCharacter.getClassType();
		for ( int i = 0; i < DOOR_DATA.length; ++i )
		{
			String [] data = DOOR_DATA[i];
			if ( myClass.equals( data[0] ) )
			{
				if ( data.length <= door )
				{
					return;
				}
				String item = data[ door ];
				int index = buffer.indexOf( item );
				if ( index != -1 )
				{
					buffer.insert( index+item.length(), " selected" );
				}
				return;
			}
		}
	}

	public static final void ensureUpdatedNemesisStatus()
	{
		if ( Preferences.getInteger( "lastNemesisReset" ) == KoLCharacter.getAscensions() )
		{
			return;
		}

		Preferences.setInteger( "dbNemesisSkill1", 0 );
		Preferences.setInteger( "dbNemesisSkill2", 0 );
		Preferences.setInteger( "dbNemesisSkill3", 0 );
		Preferences.setString( "raveCombo1", "" );
		Preferences.setString( "raveCombo2", "" );
		Preferences.setString( "raveCombo3", "" );
		Preferences.setString( "raveCombo4", "" );
		Preferences.setString( "raveCombo5", "" );
		Preferences.setString( "raveCombo6", "" );
		Preferences.setString( "volcanoMaze1", "" );
		Preferences.setString( "volcanoMaze2", "" );
		Preferences.setString( "volcanoMaze3", "" );
		Preferences.setString( "volcanoMaze4", "" );
		Preferences.setString( "volcanoMaze5", "" );
		Preferences.setInteger( "lastNemesisReset", KoLCharacter.getAscensions() );
	}

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( !location.startsWith( "cave.php" ) )
		{
			return;
		}
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		if ( location.indexOf( "action=door1" ) != -1 )
		{
			NemesisManager.selectDoorItem( 1, buffer );
			return;
		}

		if ( location.indexOf( "action=door2" ) != -1 )
		{
			NemesisManager.selectDoorItem( 2, buffer );
			return;
		}

		if ( location.indexOf( "action=door3" ) != -1 )
		{
			NemesisManager.selectDoorItem( 3, buffer );
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
				KoLmafia.updateDisplay( MafiaState.ERROR, "Could not identify " + it.getName() );
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

		TreeMap<String, PaperStrip> left = new TreeMap<String, PaperStrip>();
		TreeMap<String, PaperStrip> right = new TreeMap<String, PaperStrip>();
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
}
