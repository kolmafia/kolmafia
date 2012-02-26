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

package net.sourceforge.kolmafia.webui;

import java.io.PrintStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.IslandArenaRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandDecorator
{
	private static AreaCombatData fratboyBattlefield =
		AdventureDatabase.getAreaCombatData( "Battlefield (Frat Uniform)" );
	private static AreaCombatData hippyBattlefield =
		AdventureDatabase.getAreaCombatData( "Battlefield (Hippy Uniform)" );

	private static final String progressLineStyle = "<td style=\"color: red;font-size: 80%\" align=center>";

	// Meat drops from dirty thieving brigands
	private static final int BRIGAND_MIN = 800;
	private static final int BRIGAND_MAX = 1250;

	private static String missingGremlinTool = null;

	private static int fratboysDefeated = 0;
	private static int fratboyImage = 0;
	private static int fratboyMin = 0;
	private static int fratboyMax = 0;

	private static int hippiesDefeated = 0;
	private static int hippyImage = 0;
	private static int hippyMin = 0;
	private static int hippyMax = 0;

	// Data about current fight
	private static boolean fratboy = false;
	private static int lastFratboysDefeated = 0;
	private static int lastHippiesDefeated = 0;

	// Data about sidequests
	private static String currentJunkyardTool = "";
	private static String currentJunkyardLocation = "";
	private static int lastNunneryMeat = 0;
	private static int currentNunneryMeat = 0;

	private static final Pattern MAP_PATTERN = Pattern.compile( "bfleft(\\d*).*bfright(\\d*)", Pattern.DOTALL );
	private static final Pattern JUNKYARD_PATTERN =
		Pattern.compile( "(?:The last time I saw my|muttering something about a(?: pair of)?) (.*?)(?:, it was|, they were| and) (.*?)[.<]", Pattern.DOTALL );

	public static final int NONE = 0;
	public static final int JUNKYARD = 1;
	public static final int ORCHARD = 2;
	public static final int ARENA = 3;
	public static final int FARM = 4;
	public static final int LIGHTHOUSE = 5;
	public static final int NUNS = 6;
	public static final int CAMP = 7;

	private static int quest = IslandDecorator.NONE;

	// KoLmafia images showing each quest area on bigisland.php

	private static final String IMAGE_ROOT = "http://images.kingdomofloathing.com/otherimages/bigisland/";
	private static final String LOCAL_ROOT = "/images/otherimages/bigisland/";

	private static final String[] SIDEQUEST_IMAGES =
	{
		null, // NONE
		IslandDecorator.IMAGE_ROOT + "2.gif", // JUNKYARD
		IslandDecorator.IMAGE_ROOT + "3.gif", // ORCHARD
		IslandDecorator.IMAGE_ROOT + "6.gif", // ARENA
		IslandDecorator.IMAGE_ROOT + "15.gif", // FARM
		IslandDecorator.IMAGE_ROOT + "17.gif", // LIGHTHOUSE
		IslandDecorator.IMAGE_ROOT + "19.gif", // NUNS
	};

	private static final String[] SIDEQUEST_PREFERENCES =
	{
		"sidequestArenaCompleted",
		"sidequestFarmCompleted",
		"sidequestJunkyardCompleted",
		"sidequestLighthouseCompleted",
		"sidequestNunsCompleted",
		"sidequestOrchardCompleted",
	};

	// Here are JHunz's replacement images for Big Island sidequest areas
	// from his BattlefieldCounter Greasemonkey script:
	//
	//	http://userscripts.org/scripts/show/11720

	private static final String[] FRAT_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		IslandDecorator.LOCAL_ROOT + "2F.gif",

		// ORCHARD = 2
		IslandDecorator.LOCAL_ROOT + "3F.gif",

		// ARENA = 3
		IslandDecorator.LOCAL_ROOT + "6F.gif",

		// FARM = 4
		IslandDecorator.LOCAL_ROOT + "15F.gif",

		// LIGHTHOUSE = 5
		IslandDecorator.LOCAL_ROOT + "17F.gif",

		// NUNS = 6
		IslandDecorator.LOCAL_ROOT + "19F.gif",
	};

	private static final String[] HIPPY_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		IslandDecorator.LOCAL_ROOT + "2H.gif",

		// ORCHARD = 2
		IslandDecorator.LOCAL_ROOT + "3H.gif",

		// ARENA = 3
		IslandDecorator.LOCAL_ROOT + "6H.gif",

		// FARM = 4
		IslandDecorator.LOCAL_ROOT + "15H.gif",

		// LIGHTHOUSE = 5
		IslandDecorator.LOCAL_ROOT + "17H.gif",

		// NUNS = 6
		IslandDecorator.LOCAL_ROOT + "19H.gif",
	};

	/*
	 * Methods to decorate the Fight page
	 */

	public static final void addNunneryMeat( final AdventureResult result )
	{
		int delta = result.getCount();
		IslandDecorator.lastNunneryMeat = IslandDecorator.currentNunneryMeat;
		IslandDecorator.currentNunneryMeat =
			Preferences.increment( "currentNunneryMeat", delta, 100000, false );

		int recovered = IslandDecorator.currentNunneryMeat;
		int remaining = 100000 - recovered;
		String message = "The nuns take " + KoLConstants.COMMA_FORMAT.format( delta ) + " Meat; " + KoLConstants.COMMA_FORMAT.format( recovered ) + " recovered, " + KoLConstants.COMMA_FORMAT.format( remaining ) + " left to recover.";
		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );
	}

	public static final void decorateThemtharFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		String meat = IslandDecorator.meatMessage();
		if ( meat != null )
		{
			String message = "<p><center>" + meat + "<br>";
			buffer.insert( index, message );
		}
	}

	private static final String meatMessage()
	{
		int current = IslandDecorator.currentNunneryMeat;
		if ( current >= 100000 )
		{
			return null;
		}

		float left = 100000 - current;
		float mod = ( KoLCharacter.currentNumericModifier( Modifiers.MEATDROP ) + 100.0f ) / 100.0f;
		float min = BRIGAND_MIN * mod;
		float max = BRIGAND_MAX * mod;

		int minTurns = (int) Math.ceil( left / max );
		int maxTurns = (int) Math.ceil( left / min );

		String turns = String.valueOf( minTurns );
		if ( minTurns != maxTurns )
		{
			turns += "-" + String.valueOf( maxTurns );
		}

		return KoLConstants.COMMA_FORMAT.format( current ) + " meat recovered, " + KoLConstants.COMMA_FORMAT.format( left ) + " left (" + turns + " turns).";
	}

	public static final int minimumBrigandMeat()
	{
		// Return the minimum without additional meat drop modifiers
		int remaining = 100000 - Preferences.getInteger( "currentNunneryMeat" );
		return Math.min( BRIGAND_MIN, remaining );
	}

	private static final String[] GREMLIN_TOOLS =
	{
		"It whips out a hammer",
		"He whips out a crescent wrench",
		"It whips out a pair of pliers",
		"It whips out a screwdriver",
	};

	public static final void decorateGremlinFight( final StringBuffer buffer )
	{
		// Color the tool in the monster spoiler text
		if ( IslandDecorator.missingGremlinTool == null && !IslandDecorator.currentJunkyardTool.equals( "" ) )
		{
			StringUtilities.singleStringReplace(
				buffer, IslandDecorator.currentJunkyardTool,
				"<font color=#DD00FF>" + IslandDecorator.currentJunkyardTool + "</font>" );
		}

		for ( int i = 0; i < IslandDecorator.GREMLIN_TOOLS.length; ++i )
		{
			String tool = IslandDecorator.GREMLIN_TOOLS[ i ];
			StringUtilities.singleStringReplace( buffer, tool, "<font color=#DD00FF>" + tool + "</font>" );
		}
	}

	public static final void appendMissingGremlinTool( final StringBuffer buffer )
	{
		if ( IslandDecorator.missingGremlinTool != null )
		{
			buffer.append( "<br />This gremlin does <b>NOT</b> have a " + IslandDecorator.missingGremlinTool );
		}
	}

	public static final void startJunkyardQuest()
	{
		resetGremlinTool();
	}

	public static final void resetGremlinTool()
	{
		IslandDecorator.missingGremlinTool = null;
		IslandDecorator.currentJunkyardTool = "";
		Preferences.setString( "currentJunkyardTool", "" );
		IslandDecorator.currentJunkyardLocation = "Yossarian";
		Preferences.setString( "currentJunkyardLocation", "Yossarian" );
	}

	private static final int[] AREA_UNLOCK =
	{
		64,
		192,
		458
	};

	private static final String[] HIPPY_AREA_UNLOCK =
	{
		"Lighthouse",
 		"Junkyard",
		"Arena"
	};

	private static final String[] FRATBOY_AREA_UNLOCK =
	{
		"Orchard",
		"Nunnery",
		"Farm"
	};

	private static final String areaMessage( final int last, final int current )
	{
		final String[] areas = IslandDecorator.fratboy ? IslandDecorator.HIPPY_AREA_UNLOCK : IslandDecorator.FRATBOY_AREA_UNLOCK;

		for ( int i = 0; i < IslandDecorator.AREA_UNLOCK.length; ++i )
		{
			int threshold = IslandDecorator.AREA_UNLOCK[ i ];
			if ( last < threshold && current >= threshold )
			{
				return "The " + areas[ i ] + " is now accessible in this uniform!";
			}
		}

		return null;
	}

	private static final String areaMessageHTML( final int last, final int current )
	{
		String message = areaMessage( last, current );
		return message == null ? "" : "<b>" + message + "</b><br>";
	}

	private static final int[] HERO_UNLOCK =
	{
		458,
		606,
		658,
		766,
		880,
	};

	private static final String[] HIPPY_HERO =
	{
		"Slow Talkin' Elliot",
 		"Neil",
		"Zim Merman",
		"the C.A.R.N.I.V.O.R.E. Operative",
		"the Glass of Orange Juice",
	};

	private static final String[] FRATBOY_HERO =
	{
		"the Next-Generation Frat Boy",
		"Monty Basingstoke-Pratt, IV",
		"Brutus, the toga-clad lout",
		"Danglin' Chad",
		"the War Frat Streaker",
	};

	private static final String heroMessage( final int last, final int current )
	{
		final String[] heroes = IslandDecorator.fratboy ? IslandDecorator.FRATBOY_HERO : IslandDecorator.HIPPY_HERO;

		for ( int i = 0; i < IslandDecorator.HERO_UNLOCK.length; ++i )
		{
			int threshold = IslandDecorator.HERO_UNLOCK[ i ];
			if ( last < threshold && current >= threshold )
			{
				return "Keep your eyes open for " + heroes[ i ] + "!";
			}
		}

		return null;
	}

	private static final String heroMessageHTML( final int last, final int current )
	{
		String message = heroMessage( last, current );
		return message == null ? "" : "<b>" + message + "</b><br>";
	}

	public static final void decorateBattlefieldFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		// Don't bother showing progress of the war if you've just won
		String monster = FightRequest.getLastMonsterName();
		if ( monster.equalsIgnoreCase( "The Big Wisniewski" ) || monster.equalsIgnoreCase( "The Man" ) )
		{
			return;
		}

		int last;
		int current;

		if ( IslandDecorator.fratboy )
		{
			last = IslandDecorator.lastFratboysDefeated;
			current = IslandDecorator.fratboysDefeated;
		}
		else
		{
			last = IslandDecorator.lastHippiesDefeated;
			current = IslandDecorator.hippiesDefeated;
		}

		if ( last == current )
		{
			return;
		}

		String message =
			"<p><center>" + victoryMessage( last, current ) + "<br>" + areaMessageHTML( last, current ) + heroMessageHTML( last, current ) + "</center>";

		buffer.insert( index, message );
	}

	public static final String victoryMessage( int last, int current )
	{
		int delta = current - last;
		String side;

		if ( IslandDecorator.fratboy )
		{
			side = delta == 1 ? "frat boy" : "frat boys";
		}
		else
		{
			side = delta == 1 ? "hippy" : "hippies";
		}

		return delta + " " + side + " defeated; " + current + " down, " + ( 1000 - current ) + " left.";
	}

	/*
	 * Method to decorate the Big Island map
	 */

	// Decorate the HTML with custom goodies
	public static final void decorateBigIsland( final String url, final StringBuffer buffer )
	{
		// Quest-specific page decorations
		IslandDecorator.decorateJunkyard( buffer );
		IslandDecorator.decorateArena( url, buffer );
		IslandDecorator.decorateNunnery( url, buffer );

		// Find the table that contains the map.
		String fratboyMessage =
			IslandDecorator.sideSummary(
				"frat boys", IslandDecorator.fratboysDefeated, IslandDecorator.fratboyImage, IslandDecorator.fratboyMin,
				IslandDecorator.fratboyMax );
		String hippyMessage =
			IslandDecorator.sideSummary(
				"hippies", IslandDecorator.hippiesDefeated, IslandDecorator.hippyImage, IslandDecorator.hippyMin, IslandDecorator.hippyMax );
		String row =
			"<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + fratboyMessage + "</td>" + IslandDecorator.progressLineStyle + hippyMessage + "</td>" + "</tr></table></td></tr>";

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Mysterious Island of Mystery</b></td>" );
		if ( tableIndex != -1 )
		{
			buffer.insert( tableIndex, row );
		}

		// Now replace sidequest location images for completed quests
		IslandDecorator.sidequestImage( buffer, "sidequestArenaCompleted", IslandDecorator.ARENA );
		IslandDecorator.sidequestImage( buffer, "sidequestFarmCompleted", IslandDecorator.FARM );
		IslandDecorator.sidequestImage( buffer, "sidequestJunkyardCompleted", IslandDecorator.JUNKYARD );
		IslandDecorator.sidequestImage( buffer, "sidequestLighthouseCompleted", IslandDecorator.LIGHTHOUSE );
		IslandDecorator.sidequestImage( buffer, "sidequestNunsCompleted", IslandDecorator.NUNS );
		IslandDecorator.sidequestImage( buffer, "sidequestOrchardCompleted", IslandDecorator.ORCHARD );
	}

	private static final String sideSummary( final String side, final int kills, final int image, int min, final int max )
	{
		if ( kills > min )
		{
			min = kills;
		}
		int minLeft = 1000 - max;
		int maxLeft = 1000 - min;
		String range =
			minLeft == maxLeft ? String.valueOf( minLeft ) : String.valueOf( minLeft ) + "-" + String.valueOf( maxLeft );
		return kills + " " + side + " defeated; " + range + " left (image " + image + ").";
	}

	private static final void sidequestImage( final StringBuffer buffer, final String setting, final int quest )
	{
		String status = Preferences.getString( setting );
		String image;
		if ( status.equals( "fratboy" ) )
		{
			image = IslandDecorator.FRAT_IMAGES[ quest ];
		}
		else if ( status.equals( "hippy" ) )
		{
			image = IslandDecorator.HIPPY_IMAGES[ quest ];
		}
		else
		{
			return;
		}

		String old = IslandDecorator.SIDEQUEST_IMAGES[ quest ];
		StringUtilities.singleStringReplace( buffer, old, image );
	}

	public static final void decorateJunkyard( final StringBuffer buffer )
	{
		if ( IslandDecorator.currentJunkyardLocation.equals( "" ) )
		{
			return;
		}

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Junkyard</b></td>" );
		if ( tableIndex == -1 )
		{
			return;
		}

		String message;

		if ( !InventoryManager.hasItem( ItemPool.MOLYBDENUM_MAGNET ) )
		{
			message = "Visit Yossarian in uniform to get a molybdenum magnet";
		}
		else if ( IslandDecorator.currentJunkyardTool.equals( "" ) )
		{
			message = "Visit Yossarian for your next assignment";
		}
		else
		{
			message = "Look for the " + IslandDecorator.currentJunkyardTool + " " + IslandDecorator.currentJunkyardLocation;
		}

		String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + ".</td>" + "</tr></table></td></tr>";

		buffer.insert( tableIndex, row );
	}

	public static final void decorateArena( final String urlString, final StringBuffer buffer )
	{
		// If he's not visiting the arena, punt
		if ( urlString.indexOf( "place=concert" ) == -1 )
		{
			return;
		}

		// If there's no concert available, see if quest is in progress
		if ( buffer.indexOf( "value=\"concert\"" ) == -1 )
		{
			if ( Preferences.getString( "warProgress" ).equals( "finished" ) ||
			     !Preferences.getString( "sidequestArenaCompleted" ).equals( "none" ) )
			{
				// War is over or quest is finished. Punt.
				return;
			}

			int tableIndex =
				buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Mysterious Island Arena</b></td>" );
			if ( tableIndex != -1 )
			{
				String message = RequestEditorKit.advertisingMessage();
				String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + "</td></tr></table></td></tr>";
				buffer.insert( tableIndex, row );
			}
			return;
		}

		String quest = Preferences.getString( "sidequestArenaCompleted" );
		String [][] array = quest.equals( "hippy" ) ? IslandArenaRequest.HIPPY_CONCERTS : IslandArenaRequest.FRATBOY_CONCERTS;

		String text = buffer.toString();
		buffer.setLength( 0 );

		int index1 = 0, index2;

		// Add first choice spoiler
		String choice = array[0][0] + ": " + array[0][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add second choice spoiler
		choice = array[1][0] + ": " + array[1][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add third choice spoiler
		choice = array[2][0] + ": " + array[2][1];
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Append remainder of buffer
		buffer.append( text.substring( index1 ) );
	}

	public static final void decorateNunnery( final String urlString, final StringBuffer buffer )
	{
		// If he's not visiting the nunnery, punt
		if ( urlString.indexOf( "place=nunnery" ) == -1 )
		{
			return;
		}

		// See if quest is in progress
		if ( Preferences.getString( "warProgress" ).equals( "finished" ) ||
		     !Preferences.getString( "sidequestNunsCompleted" ).equals( "none" ) )
		{
			// Either the war or quest is over. Punt
			return;
		}

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>Our Lady of Perpetual Indecision</b></td>" );
		if ( tableIndex == -1 )
		{
			return;
		}

		String message = IslandDecorator.meatMessage();
		String row = "<tr><td><center><table width=100%><tr>" + IslandDecorator.progressLineStyle + message + "</td></tr></table></td></tr>";
		buffer.insert( tableIndex, row );
	}

	public static final void startFight()
	{
		IslandDecorator.missingGremlinTool = null;
	}

	/*
	 * Methods to mine data from request responses
	 */

	public static void handleGremlin( final String responseText )
	{
		// Batwinged Gremlin has molybdenum hammer OR
		// "It does a bombing run over your head..."

		// Erudite Gremlin has molybdenum crescent wrench OR
		// "He uses the random junk around him to make an automatic
		// eyeball-peeler..."

		// Spider Gremlin has molybdenum pliers OR
		// "It bites you in the fibula with its mandibles..."

		// Vegetable Gremlin has molybdenum screwdriver OR
		// "It picks a beet off of itself and beats you with it..."
		// "It picks a radish off of itself and tosses it at you..."

		String text = responseText;
		if ( text.indexOf( "bombing run" ) != -1 )
		{
			IslandDecorator.missingGremlinTool = "molybdenum hammer";
		}
		else if ( text.indexOf( "eyeball-peeler" ) != -1 )
		{
			IslandDecorator.missingGremlinTool = "molybdenum crescent wrench";
		}
		else if ( text.indexOf( "fibula" ) != -1 )
		{
			IslandDecorator.missingGremlinTool = "molybdenum pliers";
		}
		else if ( text.indexOf( "off of itself" ) != -1 )
		{
			IslandDecorator.missingGremlinTool = "molybdenum screwdriver";
		}
	}

	private static final String[][] HIPPY_MESSAGES =
	{
		// 2 total
		{
			// You see one of your frat brothers take out an
			// M.C. Escher drawing and show it to a War Hippy
			// (space) Cadet. The hippy looks at it and runs away
			// screaming about how he doesn't know which way is
			// down.
			"M.C. Escher",

			// You see a hippy loading his didgeridooka, but before
			// he can fire it, he's dragged off the battlefield by
			// another hippy protesting the war.
			"protesting the war",

			// You see a "Baker Company" hippy take one bite too
			// many from a big plate of brownies, then curl up to
			// take a nap. Looks like he's out of commission for a
			// while.
			"out of commission",

			// You see a hippy a few paces away suddenly realize
			// that he's violating his deeply held pacifist
			// beliefs, scream in horror, and run off the
			// battlefield.
			"pacifist beliefs",

			// You look over and see a fellow frat brother
			// garotting a hippy shaman with the hippy's own
			// dreadlocks. "Right on, bra!" you shout.
			"garotting",

			// You glance over and see one of your frat brothers
			// hosing down a hippy with soapy water. You laugh and
			// run over for a high-five.
			"soapy water",

			// You glance out over the battlefield and see a hippy
			// from the F.R.O.G. division get the hiccups and knock
			// himself out on his own nasty breath.
			"his own nasty breath",

			// You see one of the War Hippy's "Jerry's Riggers"
			// sneeze midway through making a bomb, inadvertently
			// turning himself into smoke and dust. In the wind.
			"smoke and dust",

			// You see a frat boy hose down a hippy Airborne
			// Commander with sugar water. You applaud as the
			// Commander gets attacked by her own ferrets.
			"sugar water",

			// You see one of your frat brothers paddling a hippy
			// who seems to be enjoying it. You say "uh, keep up
			// the good work... bra... yeah."
			"seems to be enjoying it",

			// As the hippy falls, you see a hippy a few yards away
			// clutch his chest and fall over, too. Apparently the
			// hippy you were fighting was just the astral
			// projection of another hippy several yards
			// away. Freaky.
			"astral projection", },
		// 4 total
		{
			// You see a War Frat Grill Sergeant hose down three
			// hippies with white-hot chicken wing sauce. You love
			// the smell of jaba�ero in the morning. It smells like
			// victory.
			"three hippies",

			// As you finish your fight, you see a nearby Wartender
			// mixing up a cocktail of vodka and pain for a trio of
			// charging hippies. "Right on, bra!" you shout.
			"vodka and pain",

			// You see one of your frat brothers douse a trio of
			// nearby hippies in cheap aftershave. They scream and
			// run off the battlefield to find some incense to
			// burn.
			"cheap aftershave",

			// You see one of your frat brothers line up three
			// hippies for simultaneous paddling. Don't bathe --
			// that's a paddlin'. Light incense -- that's a
			// paddlin'. Paddlin' a homemade canoe -- oh, you
			// better believe that's a paddlin'.
			// "three hippies",

			// You see one of the "Fortunate 500" make a quick call
			// on his cell phone. Some mercenaries drive up, shove
			// three hippies into their bitchin' meat car, and
			// drive away.
			// "three hippies",

			// As you deliver the finishing blow, you see a frat
			// boy lob a sake bomb into a trio of nearby
			// hippies. "Nice work, bra!" you shout.
			"lob a sake bomb", },
		// 8 total
		{
			// You see one of your Beer Bongadier frat brothers use
			// a complicated beer bong to spray cheap, skunky beer
			// on a whole squad hippies at once. "Way to go, bra!"
			// you shout.
			"skunky beer",

			// You glance over and see one of the Roaring Drunks
			// from the 151st Division overturning a mobile sweat
			// lodge in a berserker rage. Several sweaty, naked
			// hippies run out and off the battlefield, brushing
			// burning coals out of their dreadlocks.
			"brushing burning coals",

			// You see one of your frat brothers punch an
			// F.R.O.G. in the solar plexus, then aim the
			// subsequent exhale at a squad of hippies standing
			// nearby. You watch all of them fall to the ground,
			// gasping for air.
			"solar plexus",

			// You see a Grillmaster flinging hot kabobs as fast as
			// he can make them. He skewers one, two, three, four,
			// five, six... seven! Seven hippies! Ha ha ha!
			"hot kabobs", },
		// 16 total
		{
			// A streaking frat boy runs past a nearby funk of
			// hippies. One look at him makes the hippies have to
			// go ponder their previous belief that the naked human
			// body is a beautiful, wholesome thing.
			"naked human body",

			// You see one of the Fortunate 500 call in an air
			// strike. His daddy's personal airship flies over and
			// dumps cheap beer all over a nearby funk of hippies.
			"personal airship",

			// You look over and see a platoon of frat boys round
			// up a funk of hippies and take them prisoner. Since
			// being a POW of the frat boys involves a lot of beer
			// drinking, you're slightly envious. Since it also
			// involves a lot of paddling, you're somewhat less so.
			"slightly envious",

			// You see a kegtank and a mobile sweat lodge facing
			// off in the distance. Since the kegtank's made of
			// steel and the sweat lodge is made of wood, you can
			// guess the outcome.
			"guess the outcome", },
		// 32 total
		{
			// You see an entire regiment of hippies throw down
			// their arms (and their weapons) in disgust and walk
			// off the battlefield. War! What is it good for?
			// Absolutely nothing!
			"Absolutely nothing",

			// You see a squadron of police cars drive up, and a
			// squad of policemen arrest a funk of hippies who were
			// sitting around inhaling smoke from some sort of
			// glass sculpture.
			"glass sculpture",

			// You see a kegtank rumble through the battlefield,
			// firing beer cans out of its top turret. It mows
			// down, like, 30 hippies in a row, but then runs out
			// of ammo. They really should have stocked one more
			// six-pack.
			"one more six-pack", },
		// 64 total
		{
			// You see the a couple of frat boys attaching big,
			// long planks of wood to either side of a
			// kegtank. Then they drive through the rank hippy
			// ranks, mass-paddling as they go. Dozens of hippies
			// flee the battlefield, tears in their filthy, filthy
			// eyes.
			"planks of wood",

			// You see one of the "Fortunate 500" hang up his PADL
			// phone, looking smug. Several SWAT vans of police in
			// full riot gear pull up, and one of them informs the
			// hippies through a megaphone that this is not a
			// "designated free speech zone." The hippies throw
			// rocks and bottles at the police, but most of them
			// end up shoved into paddy wagons in chains. Er, the
			// hippies are the ones in the chains. Not the wagons.
			"SWAT vans",

			// You see a couple of frat boys stick a fuse into a
			// huge wooden barrel, light the fuse, and roll it down
			// the hill to where the hippy forces are
			// fighting. Judging by the big bada boom that follows,
			// that barrel was either full of scotch or gunpowder,
			// and possibly both.
			"wooden barrel", },
	};

	private static final String[][] FRAT_MESSAGES =
	{
		// 2 total
		{
			// You look over and see a fellow hippy warrior using
			// his dreadlocks to garotte a frat warrior. "Way to
			// enforce karmic retribution!" you shout.
			"karmic retribution",

			// You see a Green Gourmet give a frat boy a plate of
			// herbal brownies. The frat boy scarfs them all, then
			// wanders off staring at his hands.
			"herbal brownies",

			// Elsewhere on the battlefield, you see a fellow hippy
			// grab a frat warrior's paddle and give the frat boy a
			// taste of his own medicine. I guess that could count
			// as homeopathic healing...
			"homeopathic healing",

			// You see a Wartender pour too much lighter fluid on
			// his grill and go up in a great ball of
			// fire. Goodness gracious!
			"lighter fluid",

			// You see a Fire Spinner blow a gout of flame onto a
			// Wartender's grill, charring all the Wartender's
			// meaty goodness. The Wartender wanders off crying.
			"meaty goodness",

			// Nearby, you see one of your sister hippies
			// explaining the rules of Ultimate Frisbee to a member
			// of the frat boys' "armchair infantry." His eyes
			// glaze and he passes out.
			"Ultimate Frisbee",

			// You see a member of the frat boy's 151st division
			// pour himself a stiff drink, knock it back, and
			// finally pass out from alcohol poisoning.
			"alcohol poisoning",

			// You glance over your shoulder and see a squadron of
			// winged ferrets descend on a frat warrior, entranced
			// by the sun glinting off his keg shield.
			"entranced by the sun",

			// You see a hippy shaman casting a Marxist spell over
			// a member of the "Fortunate 500" division of the frat
			// boy army. The frat boy gets on his cell phone and
			// starts redistributing his wealth.
			"Marxist spell",

			// You see a frat boy warrior pound a beer, smash the
			// can against his forehead, and pass out. You chuckle
			// to yourself.
			"smash the can",

			// You see an F.R.O.G. crunch a bulb of garlic in his
			// teeth and breathe all over a nearby frat boy, who
			// turns green and falls over.
			"bulb of garlic", },
		// 4 total
		{
			// You hear chanting behind you, and turn to see thick,
			// ropy (almost anime-esque) vines sprout from a War
			// Hippy Shaman's dreads and entangle three attacking
			// frat boy warriors.
			"three attacking",

			// Nearby, you see an Elite Fire Spinner take down
			// three frat boys in a whirl of flame and pain.
			"three frat boys",

			// You look over and see three ridiculously drunk
			// members of the 151st Division run together for a
			// three-way congratulatory headbutt, which turns into
			// a three-way concussion.
			"three-way",

			// You see a member of the Fortunate 500 take a phone
			// call, hear him holler something about a stock market
			// crash, then watch him and two of his fortunate
			// buddies run off the battlefield in a panic.
			"stock market crash",

			// Over the next hill, you see three frat boys abruptly
			// vanish into a cloud of green smoke. Apparently the
			// Green Ops Soldiers are on the prowl.
			// "three frat boys",

			// You hear excited chittering overhead, and look up to
			// see a squadron of winged ferrets making a
			// urine-based bombing run over three frat boys. The
			// frat boys quickly run off the field to find some
			// cheap aftershave to cover up the smell.
			// "three frat boys",
		},
		// 8 total
		{
			// Nearby, a War Hippy Elder Shaman nods almost
			// imperceptibly. A Kegtank hits a gopher hole and tips
			// over. A squad of confused frat boys stumble out and
			// off the battlefield.
			"gopher hole",

			// You leap out of the way of a runaway Mobile Sweat
			// Lodge, then watch it run over one, two, three, four,
			// five, six, seven! Seven frat boys! Ha ha ha!
			"runaway Mobile Sweat Lodge",

			// A few yards away, one of the Jerry's Riggers hippies
			// detonates a bomb underneath a Wartender's grill. An
			// entire squad of frat boys run from the battlefield
			// under the onslaught of red-hot coals.
			"red-hot coals",

			// You look over and see one of Jerry's Riggers placing
			// land mines he made out of paperclips, rubber bands,
			// and psychedelic mushrooms. A charging squad of frat
			// boys trips them, and is subsequently dragged off the
			// field ranting about the giant purple squirrels.
			"purple squirrels",
		},
		// 16 total
		{
			// You turn to see a nearby War Hippy Elder Shaman
			// making a series of complex hand gestures. A flock of
			// pigeons swoops down out of the sky and pecks the
			// living daylights out of a whole platoon of frat
			// boys.
			"platoon of",

			// You see a platoon of charging frat boys get mowed
			// down by a hippy. Remember, kids, a short-range
			// weapon (like a paddle) usually does poorly against a
			// long-range weapon (like a didgeridooka).
			// "platoon of",

			// You look over and see a funk of hippies round up a
			// bunch of frat boys to take as prisoners of
			// war. Since being a hippy prisoner involves lounging
			// around inhaling clouds of smoke and eating brownies,
			// you're somewhat jealous. Since it also involves
			// non-stop olfactory assault, you're somewhat less so.
			"funk of hippies",

			// Nearby, a platoon of frat boys is rocking a mobile
			// sweat lodge back and forth, trying to tip it
			// over. When they succeed, they seem surprised by the
			// hot coals and naked hippies that pour forth, and the
			// frat boys run away screaming.
			// "platoon of",
		},
		// 32 total
		{
			// A mobile sweat lodge rumbles into a regiment of frat
			// boys and the hippies inside open all of its vents
			// simultaneously. Steam that smells like a dozen
			// baking (and baked) hippies pours out, enveloping the
			// platoon and sending the frat boys into fits of
			// nauseated coughing.
			"regiment",

			// You see a squadron of police cars drive up, and a
			// squad of policemen arrest an entire regiment of frat
			// boys. You hear cries of "She told me she was 18,
			// bra!" and "I told you, I didn't hit her with a
			// roofing shingle!" as they're dragged off the
			// battlefield.
			// "regiment",

			// You see a regiment of frat boys decide they're tired
			// of drinking non-alcoholic beer and tired of not
			// hitting on chicks, so they throw down their arms,
			// and then their weapons, and head back to the frat
			// house.
			// "regiment",
		},
		// 64 total
		{
			// You see an airborne commander trying out a new
			// strategy: she mixes a tiny bottle of rum she found
			// on one of the frat boy casualties with a little of
			// the frat boy's blood, then adds that to the ferret
			// bait. A fleet of ferrets swoops down, eats the bait,
			// and goes berserk with alcohol/bloodlust. The frat
			// boys scream like schoolgirls as the ferrets decimate
			// their ranks.
			"scream like schoolgirls",

			// You see a couple of hippies rigging a mobile sweat
			// lodge with a public address system. They drive it
			// through the battlefield, blaring some concept album
			// about the dark side of Ronald. Frat boys fall asleep
			// en masse, helpless before music that's horribly
			// boring if you're not under the influence of
			// mind-altering drugs.
			"en masse",

			// You see an elder hippy shaman close her eyes, clench
			// her fists, and start to chant. She glows with an
			// eerie green light as storm clouds bubble and roil
			// overhead. A funnel cloud descends from the
			// thunderheads and dances through the frat boy ranks,
			// whisking them up and away like so many miniature
			// mobile homes.
			"mobile homes", },
	};

	private static final boolean findBattlefieldMessage( final String responseText, final String[] table )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			if ( responseText.indexOf( table[ i ] ) != -1 )
			{
				return true;
			}
		}
		return false;
	}

	public static final void handleBattlefield( final String responseText )
	{
		// Nothing to do until battle is done
		if ( responseText.indexOf( "WINWINWIN" ) == -1 )
		{
			return;
		}

		// Initialize settings if necessary
		IslandDecorator.ensureUpdatedBigIsland();

		IslandDecorator.lastFratboysDefeated = IslandDecorator.fratboysDefeated;
		IslandDecorator.lastHippiesDefeated = IslandDecorator.hippiesDefeated;

		// Just in case
		PrintStream sessionStream = RequestLogger.getSessionStream();

		// We only count known monsters
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		if ( monster == null )
		{
			// The monster is not in the monster database.
			sessionStream.println( "Unknown monster found on battlefield: " + FightRequest.getLastMonsterName() );
			return;
		}

		if ( responseText.indexOf( "Giant explosions in slow motion" ) != -1 )
		{
			// FightRequest can't handle this.
			ResultProcessor.processResults( true, responseText );
			IslandDecorator.handleEndOfWar( "both" );
			return;
		}

		String name = monster.getName();
		if ( name.equalsIgnoreCase( "The Big Wisniewski" ) )
		{
			IslandDecorator.handleEndOfWar( "hippies" );
			return;
		}

		if ( name.equalsIgnoreCase( "The Man" ) )
		{
			IslandDecorator.handleEndOfWar( "fratboys" );
			return;
		}

		// Decide whether we defeated a hippy or a fratboy warrior
		if ( IslandDecorator.fratboyBattlefield.hasMonster( monster ) )
		{
			IslandDecorator.fratboy = false;
		}
		else if ( IslandDecorator.hippyBattlefield.hasMonster( monster ) )
		{
			IslandDecorator.fratboy = true;
		}
		else
		{
			// Known but unexpected monster on battlefield.
			sessionStream.println( "Unexpected monster found on battlefield: " + FightRequest.getLastMonsterName() );
			return;
		}

		// Figure out how many enemies were defeated
		String[][] table = IslandDecorator.fratboy ? IslandDecorator.FRAT_MESSAGES : IslandDecorator.HIPPY_MESSAGES;

		int quests = 0;
		int delta = 1;
		int test = 2;

		for ( int i = 0; i < table.length; ++i )
		{
			if ( IslandDecorator.findBattlefieldMessage( responseText, table[ i ] ) )
			{
				quests = i + 1;
				delta = test;
				break;
			}
			test *= 2;
		}

		int last;
		int current;

		if ( IslandDecorator.fratboy )
		{
			IslandDecorator.fratboysDefeated = Preferences.increment( "fratboysDefeated", delta, 1000, false );
			last = IslandDecorator.lastFratboysDefeated;
			current = IslandDecorator.fratboysDefeated;
		}
		else
		{
			IslandDecorator.hippiesDefeated = Preferences.increment( "hippiesDefeated", delta, 1000, false );
			last = IslandDecorator.lastHippiesDefeated;
			current = IslandDecorator.hippiesDefeated;
		}

		String message = victoryMessage( last, current );

		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );

		message = areaMessage( last, current );

		if ( message != null )
		{
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
		}

		message = heroMessage( last, current );

		if ( message != null )
		{
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
		}
	}

	private static final void handleEndOfWar( final String loser )
	{
		String message;

		if ( loser.equals( "fratboys" ) )
		{
			IslandDecorator.fratboysDefeated = 1000;
			Preferences.setInteger( "fratboysDefeated", 1000 );
			message = "War finished: fratboys defeated";
		}
		else if ( loser.equals( "hippies" ) )
		{
			IslandDecorator.hippiesDefeated = 1000;
			Preferences.setInteger( "hippiesDefeated", 1000 );
			message = "War finished: hippies defeated";
		}
		else if ( loser.equals( "both" ) )
		{
			IslandDecorator.fratboysDefeated = 1000;
			Preferences.setInteger( "fratboysDefeated", 1000 );
			IslandDecorator.hippiesDefeated = 1000;
			Preferences.setInteger( "hippiesDefeated", 1000 );
			message = "War finished: both sides defeated";
		}
		else
		{
			// Say what?
			return;
		}

		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );

		Preferences.setString( "sideDefeated", loser );
		Preferences.setString( "warProgress", "finished" );
		QuestDatabase.setQuestProgress( QuestDatabase.ISLAND_WAR, QuestDatabase.FINISHED );
		CoinmastersFrame.externalUpdate();
	}

	// Crowther spaded how many kills it takes to display an image in:
	// http://jick-nerfed.us/forums/viewtopic.php?p=58270#58270

	private static final int[] IMAGES =
	{
		0,	// Image 0
		3,	// Image 1
		9,	// Image 2
		17,	// Image 3
		28,	// Image 4
		40,	// Image 5
		52,	// Image 6
		64,	// Image 7
		80,	// Image 8
		96,	// Image 9
		114,	// Image 10
		132,	// Image 11
		152,	// Image 12
		172,	// Image 13
		192,	// Image 14
		224,	// Image 15
		258,	// Image 16
		294,	// Image 17
		332,	// Image 18
		372,	// Image 19
		414,	// Image 20
		458,	// Image 21
		506,	// Image 22
		556,	// Image 23
		606,	// Image 24
		658,	// Image 25
		711,	// Image 26
		766,	// Image 27
		822,	// Image 28
		880,	// Image 29
		939,	// Image 30
		999,	// Image 31
		1000	// Image 32
	};

	public static final void parseBigIsland( final String location, final String responseText )
	{
		if ( !location.startsWith( "bigisland.php" ) )
		{
			return;
		}

		// Set variables from user settings
		IslandDecorator.ensureUpdatedBigIsland();
		Preferences.setString( "warProgress", "started" );

		// Parse the map and deduce how many soldiers remain
		IslandDecorator.parseBattlefield( responseText );

		// Deduce things about quests
		IslandDecorator.quest = IslandDecorator.parseQuest( location );

		switch ( IslandDecorator.quest )
		{
		case ARENA:
			IslandDecorator.parseArena( responseText );
			IslandArenaRequest.parseResponse( location, responseText );
			break;
		case JUNKYARD:
			IslandDecorator.parseJunkyard( responseText );
			break;
		case ORCHARD:
			IslandDecorator.parseOrchard( responseText );
			break;
		case FARM:
			IslandDecorator.parseFarm( responseText );
			break;
		case NUNS:
			IslandDecorator.parseNunnery( responseText );
			break;
		case LIGHTHOUSE:
			IslandDecorator.parseLighthouse( responseText );
			break;
		case CAMP:
			IslandDecorator.parseCamp( location, responseText );
			break;
		}
	}

	private static final int parseQuest( final String location )
	{
		if ( location.indexOf( "place=concert" ) != -1 ||
		     location.indexOf( "action=concert" ) != -1 )
		{
			return IslandDecorator.ARENA;
		}

		if ( location.indexOf( "action=junkman" ) != -1 )
		{
			return IslandDecorator.JUNKYARD;
		}

		if ( location.indexOf( "action=stand" ) != -1 )
		{
			return IslandDecorator.ORCHARD;
		}

		if ( location.indexOf( "action=farmer" ) != -1 )
		{
			return IslandDecorator.FARM;
		}

		if ( location.indexOf( "place=nunnery" ) != -1 )
		{
			return IslandDecorator.NUNS;
		}

		if ( location.indexOf( "action=pyro" ) != -1 )
		{
			return IslandDecorator.LIGHTHOUSE;
		}

		if ( location.indexOf( "whichcamp" ) != -1 )
		{
			return IslandDecorator.CAMP;
		}

		return IslandDecorator.NONE;
	}

	private static final void parseBattlefield( final String responseText )
	{
		Matcher matcher = IslandDecorator.MAP_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		IslandDecorator.fratboyImage = StringUtilities.parseInt( matcher.group( 1 ) );
		IslandDecorator.hippyImage = StringUtilities.parseInt( matcher.group( 2 ) );

		if ( IslandDecorator.fratboyImage >= 0 && IslandDecorator.fratboyImage <= 32 )
		{
			IslandDecorator.fratboyMin = IslandDecorator.IMAGES[ IslandDecorator.fratboyImage ];
			if ( IslandDecorator.fratboyMin == 1000 )
			{
				IslandDecorator.fratboyMax = 1000;
			}
			else
			{
				IslandDecorator.fratboyMax = IslandDecorator.IMAGES[ IslandDecorator.fratboyImage + 1 ] - 1;
			}
		}

		if ( IslandDecorator.hippyImage >= 0 && IslandDecorator.hippyImage <= 32 )
		{
			IslandDecorator.hippyMin = IslandDecorator.IMAGES[ IslandDecorator.hippyImage ];
			if ( IslandDecorator.hippyMin == 1000 )
			{
				IslandDecorator.hippyMax = 1000;
			}
			else
			{
				IslandDecorator.hippyMax = IslandDecorator.IMAGES[ IslandDecorator.hippyImage + 1 ] - 1;
			}
		}

		// Consistency check settings against map
		if ( IslandDecorator.fratboysDefeated < IslandDecorator.fratboyMin )
		{
			IslandDecorator.fratboysDefeated = IslandDecorator.fratboyMin;
			Preferences.setInteger( "fratboysDefeated", IslandDecorator.fratboysDefeated );
		}
		else if ( IslandDecorator.fratboysDefeated > IslandDecorator.fratboyMax )
		{
			IslandDecorator.fratboysDefeated = IslandDecorator.fratboyMax;
			Preferences.setInteger( "fratboysDefeated", IslandDecorator.fratboysDefeated );
		}

		if ( IslandDecorator.hippiesDefeated < IslandDecorator.hippyMin )
		{
			IslandDecorator.hippiesDefeated = IslandDecorator.hippyMin;
			Preferences.setInteger( "hippiesDefeated", IslandDecorator.hippiesDefeated );
		}
		else if ( IslandDecorator.hippiesDefeated > IslandDecorator.hippyMax )
		{
			IslandDecorator.hippiesDefeated = IslandDecorator.hippyMax;
			Preferences.setInteger( "hippiesDefeated", IslandDecorator.hippiesDefeated );
		}
	}

	private static final void parseArena( final String responseText )
	{
		// You roll up to the amphitheater and see that the Goat Cheese
		// Occurence is well into the first song of their four-hour,
		// one-song set.
		if ( responseText.indexOf( "well into the first song" ) != -1 )
		{
			Preferences.setString( "sidequestArenaCompleted", "hippy" );
			return;
		}

		// "Hey, man," he says laconically. "You did a, like, totally
		// awesome job promoting the concert, man. If you have any
		// flyers left, I'll take 'em; we can use them at the next
		// show. Speaking of which, they're hitting the stage in just a
		// couple of minutes -- you should come back in a few and check
		// 'em out. It's a totally awesome show, man."
		if ( responseText.indexOf( "I'll take 'em" ) != -1 )
		{
			Preferences.setString( "sidequestArenaCompleted", "hippy" );
			if ( InventoryManager.hasItem( ItemPool.JAM_BAND_FLYERS ) )
			{
				ResultProcessor.processItem( ItemPool.JAM_BAND_FLYERS, -1 );
			}
			return;
		}

		// You roll up to the amphitheater and see that Radioactive
		// Child has already taken the stage.
		if ( responseText.indexOf( "has already taken the stage" ) != -1 )
		{
			Preferences.setString( "sidequestArenaCompleted", "fratboy" );
			return;
		}

		// "Hey, bra," he says, "you did excellent work promoting the
		// show. If you have any flyers left, I'll take them; we can
		// use them at the next show."
		if ( responseText.indexOf( "I'll take them" ) != -1 )
		{
			Preferences.setString( "sidequestArenaCompleted", "fratboy" );
			if ( InventoryManager.hasItem( ItemPool.ROCK_BAND_FLYERS ) )
			{
				ResultProcessor.processItem( ItemPool.ROCK_BAND_FLYERS, -1 );
			}
			return;
		}

		// The stage at the Mysterious Island Arena is empty.

		if ( responseText.indexOf( "The stage at the Mysterious Island Arena is empty" ) != -1 )
		{
			// Didn't complete quest or defeated the side you
			// advertised for.
			Preferences.setString( "sidequestArenaCompleted", "none" );
		}
	}

	private static final String[][] JUNKYARD_AREAS =
	{
		{
                        "a barrel",
                        "next to that barrel with something burning in it",
                },
		{
                        "a refrigerator",
                        "near an abandoned refrigerator",
                },
		{
                        "some tires",
                        "over where the old tires are",
                },
		{
                        "a car",
                        "out by that rusted-out car",
                },
	};

	private static final void parseJunkyard( final String responseText )
	{
		String tool = IslandDecorator.currentJunkyardTool;
		String location = IslandDecorator.currentJunkyardLocation;
		boolean done = false;

		// The last time I saw my <tool> it was <location>.
		// (or, if not in uniform:)
		// He wanders off, muttering something about a <tool> and <location>
		//
		//	next to that barrel with something burning in it
		//	near an abandoned refrigerator
		//	over where the old tires are
		//	out by that rusted-out car

		Matcher matcher = IslandDecorator.JUNKYARD_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			tool = matcher.group( 1 );
			tool = "molybdenum " + ( tool.equals( "wrench" ) ? "crescent " : "" ) + tool;
			location = matcher.group( 2 );

                        // Convert out-of-uniform locations to standard location
                        for ( int i = 0; i < JUNKYARD_AREAS.length; ++i )
                        {
                                String [] locations = JUNKYARD_AREAS[i];
                                if ( location.equals( locations[0] ) )
                                {
                                        location = locations[1];
                                        break;
                                }
                        }
		}

		// As you turn to walk away, he taps you on the shoulder. "I
		// almost forgot. I made this while you were off getting my
		// tools. It was boring, but I figure the more time I spend
		// bored, the longer my life will seem. Anyway, I don't really
		// want it, so you might as well take it."

		else if ( responseText.indexOf( "I made this while you were off getting my tools" ) != -1 )
		{
			tool = "";
			location = "";
			done = true;
		}

		if ( location != IslandDecorator.currentJunkyardLocation )
		{
			IslandDecorator.currentJunkyardTool = tool;
			Preferences.setString( "currentJunkyardTool", tool );
			IslandDecorator.currentJunkyardLocation = location;
			Preferences.setString( "currentJunkyardLocation", location );
		}

		if ( !done )
		{
			return;
		}

		// Give the magnet and the tools to Yossarian

		ResultProcessor.processItem( ItemPool.MOLYBDENUM_MAGNET, -1 );
		ResultProcessor.processItem( ItemPool.MOLYBDENUM_HAMMER, -1 );
		ResultProcessor.processItem( ItemPool.MOLYBDENUM_SCREWDRIVER, -1 );
		ResultProcessor.processItem( ItemPool.MOLYBDENUM_PLIERS, -1 );
		ResultProcessor.processItem( ItemPool.MOLYBDENUM_WRENCH, -1 );

		if ( responseText.indexOf( "spark plug earring" ) != -1 ||
		     responseText.indexOf( "woven baling wire bracelets" ) != -1 ||
		     responseText.indexOf( "gearbox necklace" ) != -1 )
		{
			Preferences.setString( "sidequestJunkyardCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "rusty chain necklace" ) != -1 ||
			  responseText.indexOf( "sawblade shield" ) != -1 ||
			  responseText.indexOf( "wrench bracelet" ) != -1 )
		{
			Preferences.setString( "sidequestJunkyardCompleted", "fratboy" );
		}
	}

	private static final void parseOrchard( final String responseText )
	{
		// "Is that... it is! The heart of the filthworm queen! You've
		// done it! You've freed our orchard from the tyranny of
		// nature!"
		if ( responseText.indexOf( "tyranny of nature" ) == -1 )
		{
			return;
		}

		if ( InventoryManager.hasItem( ItemPool.FILTHWORM_QUEEN_HEART ) )
		{
			ResultProcessor.processItem( ItemPool.FILTHWORM_QUEEN_HEART, -1 );
		}

		String side = EquipmentManager.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		Preferences.setString( "sidequestOrchardCompleted", side );

		// The hippy store is available again.
		Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
		Preferences.setString( "currentHippyStore", side );
		ConcoctionDatabase.setRefreshNeeded( true );
	}

	private static final void parseFarm( final String responseText )
	{
		// "Well... How about dedicating a portion of your farm to
		// growing soybeans, to help feed the hippy army?"
		if ( responseText.indexOf( "growing soybeans" ) != -1 ||
		     responseText.indexOf( "blocks of megatofu" ) != -1 )
		{
			Preferences.setString( "sidequestFarmCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "growing hops" ) != -1 ||
			  responseText.indexOf( "bottles of McMillicancuddy" ) != -1 )
		{
			Preferences.setString( "sidequestFarmCompleted", "fratboy" );
		}
	}

	private static final void parseNunnery( final String responseText )
	{
		// "Hello, weary Adventurer! Please, allow us to tend to your
		// wounds."
		if ( responseText.indexOf( "tend to your wounds" ) != -1 )
		{
			Preferences.setString( "sidequestNunsCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "refreshing massage" ) != -1 )
		{
			Preferences.setString( "sidequestNunsCompleted", "fratboy" );
		}
		else if ( responseText.indexOf( "world-weary traveler" ) != -1 )
		{
			Preferences.setString( "sidequestNunsCompleted", "none" );
		}

		if ( responseText.indexOf( "The Sisters tend to your wounds" ) != -1 ||
		     responseText.indexOf( "The Sisters give you an invigorating massage" ) != -1 )
		{
			Preferences.increment( "nunsVisits", 1 );
		}
		else if ( responseText.indexOf( "all of the Sisters are busy right now" ) != -1 )
		{
			Preferences.setInteger( "nunsVisits", 99 );
		}
	}

	private static final void parseLighthouse( final String responseText )
	{
		// He gazes at you thoughtfully for a few seconds, then a smile
		// lights up his face and he says "My life... er... my bombs
		// for you. My bombs for you, bumpty-bumpty-bump!"
		if ( responseText.indexOf( "My bombs for you" ) == -1 )
		{
			return;
		}

		String side = EquipmentManager.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		Preferences.setString( "sidequestLighthouseCompleted", side );
		ResultProcessor.processItem( ItemPool.GUNPOWDER, -5 );
	}

	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	public static CoinmasterData findCampMaster( final String urlString )
	{
		Matcher campMatcher = IslandDecorator.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);

		if ( camp.equals( "1" ) )
		{
			return DimemasterRequest.HIPPY;
		}

		if ( camp.equals( "2" ) )
		{
			return QuartersmasterRequest.FRATBOY;
		}

		return null;
	}

	public static void parseCamp( final String location, final String responseText )
	{
		if ( location.indexOf( "whichcamp" ) == -1 )
		{
			return;
		}

		CoinmasterData data = IslandDecorator.findCampMaster( location );
		if ( data == null )
		{
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final void ensureUpdatedBigIsland()
	{
		int lastAscension = Preferences.getInteger( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastBattlefieldReset", KoLCharacter.getAscensions() );

			Preferences.setInteger( "fratboysDefeated", 0 );
			Preferences.setInteger( "hippiesDefeated", 0 );
			Preferences.setString( "sidequestArenaCompleted", "none" );
			Preferences.setString( "sidequestFarmCompleted", "none" );
			Preferences.setString( "sidequestJunkyardCompleted", "none" );
			Preferences.setString( "sidequestLighthouseCompleted", "none" );
			Preferences.setString( "sidequestNunsCompleted", "none" );
			Preferences.setString( "sidequestOrchardCompleted", "none" );
			Preferences.setString( "currentJunkyardTool", "" );
			Preferences.setString( "currentJunkyardLocation", "" );
			Preferences.setInteger( "currentNunneryMeat", 0 );
			Preferences.setInteger( "lastFratboyCall", -1 );
			Preferences.setInteger( "lastHippyCall", -1 );
			Preferences.setInteger( "availableDimes", 0 );
			Preferences.setInteger( "availableQuarters", 0 );
			Preferences.setString( "sideDefeated", "neither" );
			Preferences.setString( "warProgress", "unstarted" );
			Preferences.setInteger( "flyeredML", 0 );
		}

		// Set variables from user settings

		IslandDecorator.fratboysDefeated = Preferences.getInteger( "fratboysDefeated" );
		IslandDecorator.hippiesDefeated = Preferences.getInteger( "hippiesDefeated" );
		IslandDecorator.currentJunkyardTool = Preferences.getString( "currentJunkyardTool" );
		IslandDecorator.currentJunkyardLocation = Preferences.getString( "currentJunkyardLocation" );
		IslandDecorator.currentNunneryMeat = Preferences.getInteger( "currentNunneryMeat" );
		IslandDecorator.lastNunneryMeat = IslandDecorator.currentNunneryMeat;
	}

	public static final int fratboysDefeated()
	{
		IslandDecorator.ensureUpdatedBigIsland();
		return IslandDecorator.fratboysDefeated;
	}

	public static final int hippiesDefeated()
	{
		IslandDecorator.ensureUpdatedBigIsland();
		return IslandDecorator.hippiesDefeated;
	}

	public static final int fratboysDefeatedPerBattle()
	{
		return IslandDecorator.sidequestFactor( "hippy" );
	}

	public static final int hippiesDefeatedPerBattle()
	{
		return IslandDecorator.sidequestFactor( "fratboy" );
	}

	private static final int sidequestFactor( final String completer )
	{
		int factor = 1;
		for ( int i = 0; i < SIDEQUEST_PREFERENCES.length; ++i )
		{
			String pref = Preferences.getString( SIDEQUEST_PREFERENCES[i] );
			if ( pref.equals( completer ) )
			{
				factor *= 2;
			}
		}
		return factor;
	}

	public static final void parsePostwarIsland( final String location, final String responseText )
	{
		if ( !location.startsWith( "postwarisland.php" ) )
		{
			return;
		}

		// Set variables from user settings
		IslandDecorator.ensureUpdatedPostwarIsland();

		// Deduce which side was defeated
		IslandDecorator.deduceWinner( responseText );

		// Deduce things about quests
		IslandDecorator.quest = IslandDecorator.parseQuest( location );

		switch ( IslandDecorator.quest )
		{
		case ARENA:
			IslandDecorator.parseArena( responseText );
			IslandArenaRequest.parseResponse( location, responseText );
			break;
		case NUNS:
			IslandDecorator.parseNunnery( responseText );
			break;
		}
	}

	public static final void ensureUpdatedPostwarIsland()
	{
		int lastAscension = Preferences.getInteger( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastBattlefieldReset", KoLCharacter.getAscensions() );

			Preferences.setString( "sidequestArenaCompleted", "none" );
			Preferences.setString( "sidequestOrchardCompleted", Preferences.getString( "currentHippyStore" ) );
			Preferences.setString( "sidequestNunsCompleted", "none" );
		}
	}

	private static final void deduceWinner( final String responseText )
	{
		boolean hippiesLost = responseText.indexOf( "snarfblat=149" ) != -1;
		boolean fratboysLost = responseText.indexOf( "snarfblat=150" ) != -1;
		String loser = ( !hippiesLost ) ? "fratboys" : ( !fratboysLost ) ? "hippies" : "both";
		Preferences.setString( "sideDefeated", loser );
		Preferences.setString( "warProgress", "finished" );
		CoinmastersFrame.externalUpdate();
	}

	public static final void decoratePostwarIsland( final String url, final StringBuffer buffer )
	{
		// Quest-specific page decorations
		IslandDecorator.decorateArena( url, buffer );

		// Replace sidequest location images for completed quests

		// The arena is available after the war only if the fans of the
		// concert you promoted won the war.
		String arena = IslandDecorator.questCompleter( "sidequestArenaCompleted" );
		String winner = IslandDecorator.warWinner();
		if ( arena.equals( winner ) )
		{
			IslandDecorator.sidequestImage( buffer, "sidequestArenaCompleted", IslandDecorator.ARENA );
		}

		// If you aided the nuns during the war, they will help you
		// after the war, regardless of who won.
		IslandDecorator.sidequestImage( buffer, "sidequestNunsCompleted", IslandDecorator.NUNS );
	}

	public static String questCompleter( final String preference )
	{
		String quest = Preferences.getString( preference );
		if ( quest.equals( "hippy" ) )
			return "hippies";
		if ( quest.equals( "fratboy" ) )
			return "fratboys";
		return "none";
	}

	public static final String warWinner()
	{
		String loser = Preferences.getString( "sideDefeated" );
		if ( loser.equals( "hippies" ) )
		{
			return "fratboys";
		}
		if ( loser.equals( "fratboys" ) )
		{
			return "hippies";
		}
		return "neither";
	}

	public static final String currentIsland()
	{
		IslandDecorator.ensureUpdatedBigIsland();

		String progress = Preferences.getString( "warProgress" );
		if ( progress.equals( "finished" ) )
		{
			return "postwarisland.php";
		}
		if ( progress.equals( "started" ) )
		{
			return "bigisland.php";
		}
		return "bogus.php";
	}

	public static final boolean registerIslandRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) )
		{
			return false;
		}

		CoinmasterData data = IslandDecorator.findCampMaster( urlString );
		if ( data == null )
		{
			return false;
		}

		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
