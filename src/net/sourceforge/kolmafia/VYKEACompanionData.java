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

package net.sourceforge.kolmafia;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class VYKEACompanionData
{
	public final static int NONE = 0;

	public final static int BOOKSHELF = 1;
	public final static int CEILING_FAN = 2;
	public final static int COUCH = 3;
	public final static int DISHRACK = 4;
	public final static int DRESSER = 5;
	public final static int LAMP = 6;

	public final static int FRENZY = 1;
	public final static int BLOOD = 2;
	public final static int LIGHTNING = 3;

	private int type;
	private int level;
	private int rune;
	private String name;

	public static final VYKEACompanionData NO_COMPANION = new VYKEACompanionData();
	public static VYKEACompanionData currentCompanion = VYKEACompanionData.NO_COMPANION;

	public static void initialize( final boolean loadSettings )
	{
		VYKEACompanionData.currentCompanion = VYKEACompanionData.NO_COMPANION;
		if ( loadSettings )
		{
			VYKEACompanionData.settingsToVYKEACompanion();
		}
	}

	private static void setVYKEACompanion( final VYKEACompanionData companion, final boolean setSettings )
	{
		VYKEACompanionData.currentCompanion = companion;

		if ( setSettings )
		{

			Preferences.setString( "_VYKEACompanionName", companion.name );
			Preferences.setInteger( "_VYKEACompanionLevel", companion.level );
			Preferences.setString( "_VYKEACompanionType", companion.typeToString() );
			Preferences.setString( "_VYKEACompanionRune", companion.runeToString() );
		}
	}

	public static final VYKEACompanionData currentCompanion()
	{
		return VYKEACompanionData.currentCompanion;
	}

	public VYKEACompanionData()
	{
		this.type = NONE;
		this.level = 0;
		this.rune = NONE;
		this.name = "";
	}

	public VYKEACompanionData( final int type, final int level, final int rune, final String name)
	{
		this.type = type;
		this.level = level;
		this.rune = rune;
		this.name = name;
	}

	public int getType()
	{
		return this.type;
	}

	public void setType( final int type )
	{
		this.type = ( type >= BOOKSHELF && type <= LAMP ) ? type : NONE;
	}

	public int getLevel()
	{
		return this.level;
	}

	public void setLevel( final int level )
	{
		this.level = ( level >= 1 && level <= 5 ) ? level : 0;
	}

	public int getRune()
	{
		return this.rune;
	}

	public void setRune( final int rune )
	{
		this.rune = ( rune >= BLOOD && rune <= LIGHTNING ) ? rune : NONE;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	public static String typeToString( final int type )
	{
		switch( type )
		{
		case BOOKSHELF:
			return "bookshelf";
		case CEILING_FAN:
			return "ceiling fan";
		case COUCH:
			return "couch";
		case DISHRACK:
			return "dishrack";
		case DRESSER:
			return "dresser";
		case LAMP:
			return "lamp";
		}
		return "unknown";
	}

	public String typeToString()
	{
		return VYKEACompanionData.typeToString( this.type );
	}

	public static int stringToType( final String type )
	{
		return  type == null ?
			NONE :
			type.equals( "bookshelf" ) ?
			BOOKSHELF :
			type.equals( "ceiling fan" ) ?
			CEILING_FAN :
			type.equals( "couch" ) ?
			COUCH :
			type.equals( "dishrack" ) ?
			DISHRACK :
			type.equals( "dresser" ) ?
			DRESSER :
			type.equals( "lamp" ) ?
			LAMP :
			NONE;
	}

	public static String runeToString( final int rune )
	{
		switch( rune )
		{
		case BLOOD:
			return "blood";
		case FRENZY:
			return "frenzy";
		case LIGHTNING:
			return "lightning";
		}
		return "";
	}

	public String runeToString()
	{
		return VYKEACompanionData.runeToString( this.rune );
	}

	public static int stringToRune( final String rune )
	{
		return  rune == null ?
			NONE :
			rune.equals( "blood" ) ?
			BLOOD :
			rune.equals( "frenzy" ) ?
			FRENZY :
			rune.equals( "lightning" ) ?
			LIGHTNING :
			NONE;
	}

	// CHEBLI the level 5 lamp
	private final static Pattern COMPANION_PATTERN = Pattern.compile( "<b>(.*?)</b> the level (\\d).*(bookshelf|ceiling fan|couch|dishrack|dresser|lamp)" );

	public static void parseCompanion( final String string )
	{
		// Once you have created a companion today, you can't change it.
		// Don't waste time parsing it.
		if ( VYKEACompanionData.currentCompanion != VYKEACompanionData.NO_COMPANION )
		{
			return;
		}

		Matcher matcher = COMPANION_PATTERN.matcher( string );
		if ( matcher.find() )
		{
			String name = matcher.group( 1 );
			int level = StringUtilities.parseInt( matcher.group( 2 ) );
			String typeString = matcher.group( 3 );
			int type = VYKEACompanionData.stringToType( typeString );
			// Use last saved rune
			int rune = VYKEACompanionData.stringToRune( Preferences.getString( "_VYKEACompanionRune" ) );

			VYKEACompanionData companion = new VYKEACompanionData( type, level, rune, name );
			VYKEACompanionData.setVYKEACompanion( companion, true );
		}
	}

	public static void settingsToVYKEACompanion()
	{
		String name = Preferences.getString( "_VYKEACompanionName" );
		int level = Preferences.getInteger( "_VYKEACompanionLevel" );
		int type = VYKEACompanionData.stringToType( Preferences.getString( "_VYKEACompanionType" ) );
		int rune = VYKEACompanionData.stringToRune( Preferences.getString( "_VYKEACompanionRune" ) );

		VYKEACompanionData companion = type == NONE ? NO_COMPANION : new VYKEACompanionData( type, level, rune, name);
		VYKEACompanionData.setVYKEACompanion( companion, false );
	}

	@Override
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		if ( this.name != null && !this.name.equals( "" ) )
		{
			buffer.append( this.name );
			buffer.append( ", the " );
		}
		buffer.append( "level " );
		buffer.append( String.valueOf( this.level ) );
		if ( this.rune != VYKEACompanionData.NONE )
		{
			buffer.append( " " );
			buffer.append( this.runeToString() );
		}
		buffer.append( " " );
		buffer.append( this.typeToString() );
		return buffer.toString();
	}

	// <span class='guts'>You bolt 5 more rails onto the piece of furniture and take a step back to admire your new... lamp.  It's a lamp!<p>You decide to name it... <b>&Aring;VOB&Eacute;</b></span>
	private final static Pattern CREATION_PATTERN = Pattern.compile( "<span class='guts'>.*?It's a (bookshelf|ceiling fan|couch|dishrack|dresser|lamp).*?<b>(.*?)</b></span>", Pattern.DOTALL );

	public static void assembleCompanion( final int choice, final int decision, final String text )
 	{
		// choice 1120 - Some Assembly Required.
		// 1 - Start with 5 planks -> choice 1121 (if you have a rune) or choice 1122 (if you don't)
		// 2 - Start with 5 rails -> 1121 (if you have a rune) or choice 1122 (if you don't)
		// 6 - don't build anything
		if ( choice == 1120 )
		{
			switch ( decision )
			{
			case 1:
				// Start with 5 planks -> bookshelf, ceiling fan, dresser
				ResultProcessor.processItem( ItemPool.VYKEA_PLANK, -5 );
				break;
			case 2:
				// Start with 5 rails -> couch, dishrack, lamp
				ResultProcessor.processItem( ItemPool.VYKEA_RAIL, -5 );
				break;
			case 6:
				// Do nothing
				return;
			default:
				// Invalid decision, presumably from URL manipulation.
				return;
			}

			// You've started construction and cannot abort from
			// here on. Remove the instructions from inventory.
			ResultProcessor.processItem( ItemPool.VYKEA_INSTRUCTIONS, -1 );

			// Initialize preferences
			Preferences.setString( "_VYKEACompanionName", "" );
			Preferences.setInteger( "_VYKEACompanionLevel", 0 );
			Preferences.setString( "_VYKEACompanionType", "" );
			Preferences.setString( "_VYKEACompanionRune", "" );

			return;
		}

		// choice 1121 - Some Assembly Required
		// 1 - Add a frenzy rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
		// 2 - Add a blood rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
		// 3 - Add a lightning rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
		// 6 - Don't add any runes -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)

		if ( choice == 1121 )
		{
			int rune = NONE;
			int itemId = -1;

			switch ( decision )
			{
			case 1:
				rune = FRENZY;
				itemId = ItemPool.VYKEA_FRENZY_RUNE;
				break;
			case 2:
				rune = BLOOD;
				itemId = ItemPool.VYKEA_BLOOD_RUNE;
				break;
			case 3:
				rune = LIGHTNING;
				itemId = ItemPool.VYKEA_LIGHTNING_RUNE;
				break;
			case 6:
				// Don't add any runes
				break;
			default:
				// Invalid decision, presumably from URL manipulation.
				return;
			}

			// Save the rune in the preference
			Preferences.setString( "_VYKEACompanionRune", VYKEACompanionData.runeToString( rune ) );

			// Remove the rune from inventory
			if ( rune != NONE )
			{
				ResultProcessor.processItem( itemId, -1 );
			}

			return;
		}

		// choice 1122 - Some Assembly Required
		// 1 - Add 1 dowel -> choice 1123
		// 2 - Add 11 dowels -> choice 1123
		// 3 - Add 23 dowels -> choice 1123
		// 4 - Add 37 dowels -> choice 1123
		// 6 - Don't add any dowels -> choice 1123

		if ( choice == 1122 )
		{
			int level = 1;
			int dowels = 0;

			switch ( decision )
			{
			case 1:
				level = 2;
				dowels = 1;
				break;
			case 2:
				level = 3;
				dowels = 11;
				break;
			case 3:
				level = 4;
				dowels = 23;
				break;
			case 4:
				level = 5;
				dowels = 37;
				break;
			case 6:
				// Do not add any dowels
				break;
			default:
				// Invalid decision, presumably from URL manipulation.
				return;
			}

			// Save the level in the preference
			Preferences.setInteger( "_VYKEACompanionLevel", level );

			// Remove the dowels from inventory
			if ( dowels > 0 )
			{
				ResultProcessor.processItem( ItemPool.VYKEA_DOWEL, -dowels );
			}
			return;
		}

		// choice 1123 - Some Assembly Required
		// 1 - Add 5 planks
		// 2 - Add 5 rails
		// 3 - Add 5 brackets
		if ( choice == 1123 )
		{
			switch ( decision )
			{
			case 1:
				// Add 5 planks -> bookshelf, couch
				ResultProcessor.processItem( ItemPool.VYKEA_PLANK, -5 );
				break;
			case 2:
				// Add 5 rails -> dresser, lamp
				ResultProcessor.processItem( ItemPool.VYKEA_RAIL, -5 );
				break;
			case 3:
				// Add 5 brackets -> ceiling fan, dishrack
				ResultProcessor.processItem( ItemPool.VYKEA_BRACKET, -5 );
				break;
			default:
				// Invalid decision, presumably from URL manipulation.
				return;
			}

			// Parse companion name and type from the result text
			Matcher matcher = CREATION_PATTERN.matcher( text );
			if ( !matcher.find() )
			{
				// Unexpected. We'll pick it up from the charpane.
				System.out.println( "creation parse failed" );
				return;
			}

			String name = matcher.group( 2 );
			String typeString = matcher.group( 1 );
			int type = VYKEACompanionData.stringToType( typeString );

			// Set them into preferences
			Preferences.setString( "_VYKEACompanionName", name );
			Preferences.setString( "_VYKEACompanionType", typeString );

			int level = Preferences.getInteger( "_VYKEACompanionLevel" );
			int rune = VYKEACompanionData.stringToRune( Preferences.getString( "_VYKEACompanionRune" ) );

			// Create the companion
			VYKEACompanionData companion = new VYKEACompanionData( type, level, rune, name);
			VYKEACompanionData.setVYKEACompanion( companion, false );

			// Adjust modifiers
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
		}
	}
}
