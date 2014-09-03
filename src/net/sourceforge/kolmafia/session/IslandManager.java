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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandManager
{
	private static final AreaCombatData fratboyBattlefield = AdventureDatabase.getAreaCombatData( "The Battlefield (Frat Uniform)" );
	private static final AreaCombatData hippyBattlefield = AdventureDatabase.getAreaCombatData( "The Battlefield (Hippy Uniform)" );
	
	public static final boolean isBattlefieldMonster()
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		return IslandManager.isBattlefieldMonster( monster );
	}

	public static final boolean isBattlefieldMonster( final String name )
	{
		MonsterData monster = MonsterDatabase.findMonster( name, false );
		return IslandManager.isBattlefieldMonster( monster );
	}

	public static final boolean isBattlefieldMonster( MonsterData monster )
	{
		return	IslandManager.fratboyBattlefield.hasMonster( monster ) ||
			IslandManager.hippyBattlefield.hasMonster( monster );
	}

	public static final boolean isFratboyBattlefieldMonster( MonsterData monster )
	{
		return	IslandManager.fratboyBattlefield.hasMonster( monster );
	}

	public static final boolean isHippyBattlefieldMonster( MonsterData monster )
	{
		return	IslandManager.hippyBattlefield.hasMonster( monster );
	}

	private static final Pattern MAP_PATTERN = Pattern.compile( "bfleft(\\d*).*bfright(\\d*)", Pattern.DOTALL );
	private static final Pattern JUNKYARD_PATTERN =
		Pattern.compile( "(?:The last time I saw my|muttering something about a(?: pair of)?) (.*?)(?:, it was|, they were| and) (.*?)[.<]", Pattern.DOTALL );

	private static String missingGremlinTool = null;

	// Data about current fight
	private static boolean fratboy = false;
	private static int lastFratboysDefeated = 0;
	private static int lastHippiesDefeated = 0;

	private static int fratboysDefeated = 0;
	private static int fratboyImage = 0;
	private static int fratboyMin = 0;
	private static int fratboyMax = 0;

	private static int hippiesDefeated = 0;
	private static int hippyImage = 0;
	private static int hippyMin = 0;
	private static int hippyMax = 0;

	// Data about sidequests
	private static String currentJunkyardTool = "";
	private static String currentJunkyardLocation = "";
	private static int currentNunneryMeat = 0;

	public enum Quest
	{
		NONE,
		JUNKYARD,
		ORCHARD,
		ARENA,
		FARM,
		LIGHTHOUSE,
		NUNS,
		CAMP
	}

	public static final void resetIsland()
	{
		Preferences.setInteger( "fratboysDefeated", 0 );
		Preferences.setInteger( "hippiesDefeated", 0 );
		Preferences.setString( "sidequestArenaCompleted", "none" );
		Preferences.setString( "sidequestFarmCompleted", "none" );
		Preferences.setString( "sidequestJunkyardCompleted", "none" );
		Preferences.setString( "sidequestLighthouseCompleted", "none" );
		Preferences.setString( "sidequestNunsCompleted", "none" );
		Preferences.setString( "sidequestOrchardCompleted", Preferences.getString( "currentHippyStore" ) );
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

	public static final void ensureUpdatedBigIsland()
	{
		int lastAscension = Preferences.getInteger( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastBattlefieldReset", KoLCharacter.getAscensions() );
			IslandManager.resetIsland();
		}

		// Set variables from user settings

		IslandManager.fratboysDefeated = Preferences.getInteger( "fratboysDefeated" );
		IslandManager.hippiesDefeated = Preferences.getInteger( "hippiesDefeated" );
		IslandManager.currentJunkyardTool = Preferences.getString( "currentJunkyardTool" );
		IslandManager.currentJunkyardLocation = Preferences.getString( "currentJunkyardLocation" );
		IslandManager.currentNunneryMeat = Preferences.getInteger( "currentNunneryMeat" );
	}

	private static Quest quest = Quest.NONE;

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

	public static final boolean fratboy()
	{
		return IslandManager.fratboy;
	}

	public static final int lastFratboysDefeated()
	{
		return IslandManager.lastFratboysDefeated;
	}

	public static final int lastHippiesDefeated()
	{
		return IslandManager.lastHippiesDefeated;
	}

	public static final int fratboysDefeated()
	{
		return IslandManager.fratboysDefeated;
	}

	public static final int hippiesDefeated()
	{
		return IslandManager.hippiesDefeated;
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

	public static final String sideSummary( final String side )
	{
		return  side.equals( "frat boys" ) ?
			IslandManager.sideSummary( side,
						   IslandManager.fratboysDefeated,
						   IslandManager.fratboyImage,
						   IslandManager.fratboyMin,
						   IslandManager.fratboyMax ) :
			side.equals( "hippies" ) ?
			IslandManager.sideSummary( side,
						   IslandManager.hippiesDefeated,
						   IslandManager.hippyImage,
						   IslandManager.hippyMin,
						   IslandManager.hippyMax ) :
			"";
	}

	public static final int currentNunneryMeat()
	{
		return IslandManager.currentNunneryMeat;
	}

	public static final int fratboysDefeatedPerBattle()
	{
		return IslandManager.sidequestFactor( "hippy" ) +
		       ( Preferences.getString( "peteMotorbikeCowling" ).equals( "Rocket Launcher" ) ? 3 : 0 );
	}

	public static final int hippiesDefeatedPerBattle()
	{
		return IslandManager.sidequestFactor( "fratboy" ) +
		       ( Preferences.getString( "peteMotorbikeCowling" ).equals( "Rocket Launcher" ) ? 3 : 0 );
	}

	private static final String[] SIDEQUEST_PREFERENCES =
	{
		"sidequestArenaCompleted",
		"sidequestFarmCompleted",
		"sidequestJunkyardCompleted",
		"sidequestLighthouseCompleted",
		"sidequestNunsCompleted",
		"sidequestOrchardCompleted",
	};

	private static final int sidequestFactor( final String completer )
	{
		int factor = 1;
		for ( int i = 0; i < IslandManager.SIDEQUEST_PREFERENCES.length; ++i )
		{
			String pref = Preferences.getString( IslandManager.SIDEQUEST_PREFERENCES[ i ] );
			if ( pref.equals( completer ) )
			{
				factor *= 2;
			}
		}
		return factor;
	}

	public static final void startJunkyardQuest()
	{
		resetGremlinTool();
	}

	public static final void resetGremlinTool()
	{
		IslandManager.missingGremlinTool = null;
		IslandManager.currentJunkyardTool = "";
		Preferences.setString( "currentJunkyardTool", "" );
		IslandManager.currentJunkyardLocation = "Yossarian";
		Preferences.setString( "currentJunkyardLocation", "Yossarian" );
	}

	public static final String missingGremlinTool()
	{
		return IslandManager.missingGremlinTool;
	}

	public static final String currentJunkyardTool()
	{
		return IslandManager.currentJunkyardTool;
	}

	public static final String currentJunkyardLocation()
	{
		return IslandManager.currentJunkyardLocation;
	}

	public static final String warProgress()
	{
		return Preferences.getString( "warProgress" );
	}

	public static final String currentIsland()
	{
		String progress = IslandManager.warProgress();
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

	public static final void startFight()
	{
		IslandManager.missingGremlinTool = null;
	}

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
		if ( text.contains( "bombing run" ) )
		{
			IslandManager.missingGremlinTool = "molybdenum hammer";
		}
		else if ( text.contains( "eyeball-peeler" ) )
		{
			IslandManager.missingGremlinTool = "molybdenum crescent wrench";
		}
		else if ( text.contains( "fibula" ) )
		{
			IslandManager.missingGremlinTool = "molybdenum pliers";
		}
		else if ( text.contains( "off of itself" ) )
		{
			IslandManager.missingGremlinTool = "molybdenum screwdriver";
		}
	}

	public static final void addNunneryMeat( final AdventureResult result )
	{
		int delta = result.getCount();
		IslandManager.currentNunneryMeat = Preferences.increment( "currentNunneryMeat", delta, 100000, false );

		int recovered = IslandManager.currentNunneryMeat;
		int remaining = 100000 - recovered;
		String message = "The nuns take " + KoLConstants.COMMA_FORMAT.format( delta ) + " Meat; " + KoLConstants.COMMA_FORMAT.format( recovered ) + " recovered, " + KoLConstants.COMMA_FORMAT.format( remaining ) + " left to recover.";
		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );
	}

	/*
	 * Methods to mine data from request responses
	 */

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
			"subsequent exhale",

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
			"much lighter fluid",

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
			"red-hot coals.",

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
			if ( responseText.contains( table[ i ] ) )
			{
				return true;
			}
		}
		return false;
	}

	public static final String victoryMessage( int last, int current )
	{
		int delta = current - last;
		String side;

		if ( IslandManager.fratboy )
		{
			side = delta == 1 ? "frat boy" : "frat boys";
		}
		else
		{
			side = delta == 1 ? "hippy" : "hippies";
		}

		return delta + " " + side + " defeated; " + current + " down, " + ( 1000 - current ) + " left.";
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

	public static final String areaMessage( final int last, final int current )
	{
		final String[] areas = IslandManager.fratboy ? IslandManager.HIPPY_AREA_UNLOCK : IslandManager.FRATBOY_AREA_UNLOCK;

		for ( int i = 0; i < IslandManager.AREA_UNLOCK.length; ++i )
		{
			int threshold = IslandManager.AREA_UNLOCK[ i ];
			if ( last < threshold && current >= threshold )
			{
				return "The " + areas[ i ] + " is now accessible in this uniform!";
			}
		}

		return null;
	}

	private static final int[] HERO_UNLOCK =
	{
		501,
		601,
		701,
		801,
		901,
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

	public static final String heroMessage( final int last, final int current )
	{
		final String[] heroes = IslandManager.fratboy ? IslandManager.FRATBOY_HERO : IslandManager.HIPPY_HERO;

		for ( int i = 0; i < IslandManager.HERO_UNLOCK.length; ++i )
		{
			int threshold = IslandManager.HERO_UNLOCK[ i ];
			if ( last < threshold && current >= threshold )
			{
				return "Keep your eyes open for " + heroes[ i ] + "!";
			}
		}

		return null;
	}

	public static final void handleBattlefield( final String responseText )
	{
		// Nothing to do until battle is done
		if ( !responseText.contains( "WINWINWIN" ) )
		{
			return;
		}

		// We only count known monsters
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		if ( monster == null )
		{
			// The monster is not in the monster database.
			RequestLogger.updateSessionLog( "Unknown monster found on battlefield: " + MonsterStatusTracker.getLastMonsterName() );
			return;
		}

		if ( responseText.contains( "Giant explosions in slow motion" ) )
		{
			// FightRequest can't handle this.
			ResultProcessor.processResults( true, responseText );
			IslandManager.handleEndOfWar( "both" );
			return;
		}

		String name = monster.getName();
		if ( name.equalsIgnoreCase( "The Big Wisniewski" ) )
		{
			IslandManager.handleEndOfWar( "hippies" );
			return;
		}

		if ( name.equalsIgnoreCase( "The Man" ) )
		{
			IslandManager.handleEndOfWar( "fratboys" );
			return;
		}
	}

	private static final void handleEndOfWar( final String loser )
	{
		String message;

		if ( loser.equals( "fratboys" ) )
		{
			IslandManager.fratboysDefeated = 1000;
			Preferences.setInteger( "fratboysDefeated", 1000 );
			message = "War finished: fratboys defeated";
		}
		else if ( loser.equals( "hippies" ) )
		{
			IslandManager.hippiesDefeated = 1000;
			Preferences.setInteger( "hippiesDefeated", 1000 );
			message = "War finished: hippies defeated";
		}
		else if ( loser.equals( "both" ) )
		{
			IslandManager.fratboysDefeated = 1000;
			Preferences.setInteger( "fratboysDefeated", 1000 );
			IslandManager.hippiesDefeated = 1000;
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
		QuestDatabase.setQuestProgress( QuestDatabase.Quest.ISLAND_WAR, QuestDatabase.FINISHED );
		CoinmastersFrame.externalUpdate();
	}

	public static final void handleBattlefieldMonster( final String responseText, final String monsterName )
	{
		// Nothing to do until battle is done
		if ( !responseText.contains( "WINWINWIN" ) )
		{
			return;
		}

		// You can fax in monsters even after the war is over. Nothing
		// special to do in that case.
		if ( Preferences.getString( "warProgress" ).equals( "finished" ) )
		{
			return;
		}

		MonsterData monster = MonsterDatabase.findMonster( monsterName, false );

		// Decide whether we defeated a hippy or a fratboy warrior
		if ( IslandManager.isFratboyBattlefieldMonster( monster ) )
		{
			IslandManager.fratboy = false;
		}
		else if ( IslandManager.isHippyBattlefieldMonster( monster ) )
		{
			IslandManager.fratboy = true;
		}
		else
		{
			// Known but unexpected monster on battlefield.
			RequestLogger.updateSessionLog( "Unexpected monster found on battlefield: " + monsterName );
			return;
		}

		IslandManager.lastFratboysDefeated = IslandManager.fratboysDefeated;
		IslandManager.lastHippiesDefeated = IslandManager.hippiesDefeated;

		// Figure out how many enemies were defeated
		String[][] table = IslandManager.fratboy ? IslandManager.FRAT_MESSAGES : IslandManager.HIPPY_MESSAGES;

		int delta = 1;
		int test = 2;

		for ( int i = 0; i < table.length; ++i )
		{
			if ( IslandManager.findBattlefieldMessage( responseText, table[ i ] ) )
			{
				delta = test;
				break;
			}
			test *= 2;
		}

		// Handle Pete's Motorbike with Rocket Launcher
		if ( responseText.contains( "rocket launcher blasts 3 extra" ) )
		{
			delta += 3;
		}

		int last;
		int current;

		if ( IslandManager.fratboy )
		{
			IslandManager.fratboysDefeated = Preferences.increment( "fratboysDefeated", delta, 1000, false );
			last = IslandManager.lastFratboysDefeated;
			current = IslandManager.fratboysDefeated;
		}
		else
		{
			IslandManager.hippiesDefeated = Preferences.increment( "hippiesDefeated", delta, 1000, false );
			last = IslandManager.lastHippiesDefeated;
			current = IslandManager.hippiesDefeated;
		}

		String message = IslandManager.victoryMessage( last, current );

		RequestLogger.updateSessionLog( message );
		RequestLogger.printLine( message );

		message = IslandManager.areaMessage( last, current );

		if ( message != null )
		{
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
		}

		message = IslandManager.heroMessage( last, current );

		if ( message != null )
		{
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );
		}
	}

	public static final void parseIsland( final String location, final String responseText )
	{
		if ( location.startsWith( "bigisland.php" ) )
		{
			IslandManager.parseBigIsland( location, responseText );
		}
		else if ( location.startsWith( "postwarisland.php" ) )
		{
			IslandManager.parsePostwarIsland( location, responseText );
		}
	}

	public static final void parseBigIsland( final String location, final String responseText )
	{
		Preferences.setString( "warProgress", "started" );

		// Parse the map and deduce how many soldiers remain
		IslandManager.parseBattlefield( responseText );

		// Deduce things about quests
		IslandManager.quest = IslandManager.parseQuest( location );

		switch ( IslandManager.quest )
		{
		case ARENA:
			IslandManager.parseArena( responseText );
			break;
		case JUNKYARD:
			IslandManager.parseJunkyard( responseText );
			break;
		case ORCHARD:
			IslandManager.parseOrchard( responseText );
			break;
		case FARM:
			IslandManager.parseFarm( responseText );
			break;
		case NUNS:
			IslandManager.parseNunnery( responseText );
			break;
		case LIGHTHOUSE:
			IslandManager.parseLighthouse( responseText );
			break;
		case CAMP:
			IslandManager.parseCamp( location, responseText );
			break;
		}
	}

	private static final Quest parseQuest( final String location )
	{
		if ( location.contains( "place=concert" ) || location.contains( "action=concert" ) )
		{
			return Quest.ARENA;
		}

		if ( location.contains( "action=junkman" ) )
		{
			return Quest.JUNKYARD;
		}

		if ( location.contains( "action=stand" ) )
		{
			return Quest.ORCHARD;
		}

		if ( location.contains( "action=farmer" ) )
		{
			return Quest.FARM;
		}

		if ( location.contains( "place=nunnery" ) )
		{
			return Quest.NUNS;
		}

		if ( location.contains( "action=pyro" ) )
		{
			return Quest.LIGHTHOUSE;
		}

		if ( location.contains( "whichcamp" ) )
		{
			return Quest.CAMP;
		}

		return Quest.NONE;
	}

	private static final void parseBattlefield( final String responseText )
	{
		Matcher matcher = IslandManager.MAP_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		IslandManager.fratboyImage = StringUtilities.parseInt( matcher.group( 1 ) );
		IslandManager.hippyImage = StringUtilities.parseInt( matcher.group( 2 ) );

		if ( IslandManager.fratboyImage >= 0 && IslandManager.fratboyImage <= 32 )
		{
			IslandManager.fratboyMin = IslandManager.IMAGES[ IslandManager.fratboyImage ];
			if ( IslandManager.fratboyMin == 1000 )
			{
				IslandManager.fratboyMax = 1000;
			}
			else
			{
				IslandManager.fratboyMax = IslandManager.IMAGES[ IslandManager.fratboyImage + 1 ] - 1;
			}
		}

		if ( IslandManager.hippyImage >= 0 && IslandManager.hippyImage <= 32 )
		{
			IslandManager.hippyMin = IslandManager.IMAGES[ IslandManager.hippyImage ];
			if ( IslandManager.hippyMin == 1000 )
			{
				IslandManager.hippyMax = 1000;
			}
			else
			{
				IslandManager.hippyMax = IslandManager.IMAGES[ IslandManager.hippyImage + 1 ] - 1;
			}
		}

		// Consistency check settings against map
		if ( IslandManager.fratboysDefeated < IslandManager.fratboyMin )
		{
			IslandManager.fratboysDefeated = IslandManager.fratboyMin;
			Preferences.setInteger( "fratboysDefeated", IslandManager.fratboysDefeated );
		}
		else if ( IslandManager.fratboysDefeated > IslandManager.fratboyMax )
		{
			IslandManager.fratboysDefeated = IslandManager.fratboyMax;
			Preferences.setInteger( "fratboysDefeated", IslandManager.fratboysDefeated );
		}

		if ( IslandManager.hippiesDefeated < IslandManager.hippyMin )
		{
			IslandManager.hippiesDefeated = IslandManager.hippyMin;
			Preferences.setInteger( "hippiesDefeated", IslandManager.hippiesDefeated );
		}
		else if ( IslandManager.hippiesDefeated > IslandManager.hippyMax )
		{
			IslandManager.hippiesDefeated = IslandManager.hippyMax;
			Preferences.setInteger( "hippiesDefeated", IslandManager.hippiesDefeated );
		}
	}

	private static final void parseArena( final String responseText )
	{
		// You roll up to the amphitheater and see that the Goat Cheese
		// Occurence is well into the first song of their four-hour,
		// one-song set.
		if ( responseText.contains( "well into the first song" ) )
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
		if ( responseText.contains( "I'll take 'em" ) )
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
		if ( responseText.contains( "has already taken the stage" ) )
		{
			Preferences.setString( "sidequestArenaCompleted", "fratboy" );
			return;
		}

		// "Hey, bra," he says, "you did excellent work promoting the
		// show. If you have any flyers left, I'll take them; we can
		// use them at the next show."
		if ( responseText.contains( "I'll take them" ) )
		{
			Preferences.setString( "sidequestArenaCompleted", "fratboy" );
			if ( InventoryManager.hasItem( ItemPool.ROCK_BAND_FLYERS ) )
			{
				ResultProcessor.processItem( ItemPool.ROCK_BAND_FLYERS, -1 );
			}
			return;
		}

		// The stage at the Mysterious Island Arena is empty.

		if ( responseText.contains( "The stage at the Mysterious Island Arena is empty" ) )
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
		String tool = IslandManager.currentJunkyardTool;
		String location = IslandManager.currentJunkyardLocation;
		boolean done = false;

		// The last time I saw my <tool> it was <location>.
		// (or, if not in uniform:)
		// He wanders off, muttering something about a <tool> and <location>
		//
		//	next to that barrel with something burning in it
		//	near an abandoned refrigerator
		//	over where the old tires are
		//	out by that rusted-out car

		Matcher matcher = IslandManager.JUNKYARD_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			tool = matcher.group( 1 );
			tool = "molybdenum " + ( tool.equals( "wrench" ) ? "crescent " : "" ) + tool;
			location = matcher.group( 2 );

			// Convert out-of-uniform locations to standard location
			for ( int i = 0; i < IslandManager.JUNKYARD_AREAS.length; ++i )
			{
				String [] locations = IslandManager.JUNKYARD_AREAS[i];
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

		else if ( responseText.contains( "I made this while you were off getting my tools" ) )
		{
			tool = "";
			location = "";
			done = true;
		}

		if ( !location.equals( IslandManager.currentJunkyardLocation ) )
		{
			IslandManager.currentJunkyardTool = tool;
			Preferences.setString( "currentJunkyardTool", tool );
			IslandManager.currentJunkyardLocation = location;
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

		if ( responseText.contains( "spark plug earring" ) ||
		     responseText.contains( "woven baling wire bracelets" ) ||
		     responseText.contains( "gearbox necklace" ) )
		{
			Preferences.setString( "sidequestJunkyardCompleted", "hippy" );
		}
		else if ( responseText.contains( "rusty chain necklace" ) ||
			  responseText.contains( "sawblade shield" ) ||
			  responseText.contains( "wrench bracelet" ) )
		{
			Preferences.setString( "sidequestJunkyardCompleted", "fratboy" );
		}
	}

	private static final void parseOrchard( final String responseText )
	{
		// "Is that... it is! The heart of the filthworm queen! You've
		// done it! You've freed our orchard from the tyranny of
		// nature!"
		if ( !responseText.contains( "tyranny of nature" ) )
		{
			return;
		}

		if ( InventoryManager.hasItem( ItemPool.FILTHWORM_QUEEN_HEART ) )
		{
			ResultProcessor.processItem( ItemPool.FILTHWORM_QUEEN_HEART, -1 );
		}

		String side = EquipmentManager.isWearingOutfit( OutfitPool.WAR_HIPPY_OUTFIT ) ? "hippy" : "fratboy";
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
		if ( responseText.contains( "growing soybeans" ) ||
		     responseText.contains( "blocks of megatofu" ) )
		{
			Preferences.setString( "sidequestFarmCompleted", "hippy" );
		}
		else if ( responseText.contains( "growing hops" ) ||
			  responseText.contains( "bottles of McMillicancuddy" ) )
		{
			Preferences.setString( "sidequestFarmCompleted", "fratboy" );
		}
	}

	private static final void parseNunnery( final String responseText )
	{
		// "Hello, weary Adventurer! Please, allow us to tend to your
		// wounds."
		if ( responseText.contains( "tend to your wounds" ) )
		{
			Preferences.setString( "sidequestNunsCompleted", "hippy" );
		}
		else if ( responseText.contains( "refreshing massage" ) )
		{
			Preferences.setString( "sidequestNunsCompleted", "fratboy" );
		}
		else if ( responseText.contains( "world-weary traveler" ) )
		{
			Preferences.setString( "sidequestNunsCompleted", "none" );
		}

		if ( responseText.contains( "The Sisters tend to your wounds" ) ||
		     responseText.contains( "The Sisters give you an invigorating massage" ) )
		{
			Preferences.increment( "nunsVisits", 1 );
		}
		else if ( responseText.contains( "all of the Sisters are busy right now" ) )
		{
			Preferences.setInteger( "nunsVisits", 99 );
		}
	}

	private static final void parseLighthouse( final String responseText )
	{
		// He gazes at you thoughtfully for a few seconds, then a smile
		// lights up his face and he says "My life... er... my bombs
		// for you. My bombs for you, bumpty-bumpty-bump!"
		if ( !responseText.contains( "My bombs for you" ) )
		{
			return;
		}

		String side = EquipmentManager.isWearingOutfit( OutfitPool.WAR_HIPPY_OUTFIT ) ? "hippy" : "fratboy";
		Preferences.setString( "sidequestLighthouseCompleted", side );
		ResultProcessor.processItem( ItemPool.GUNPOWDER, -5 );
	}

	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	public static CoinmasterData findCampMaster( final String urlString )
	{
		Matcher campMatcher = IslandManager.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);
		return	camp.equals( "1" ) ?
			DimemasterRequest.HIPPY :
			camp.equals( "2" ) ?
			QuartersmasterRequest.FRATBOY:
			null;
	}

	public static void parseCamp( final String location, final String responseText )
	{
		if ( !location.contains( "whichcamp" ) )
		{
			return;
		}

		CoinmasterData data = IslandManager.findCampMaster( location );
		if ( data == null )
		{
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final void parsePostwarIsland( final String location, final String responseText )
	{
		// Deduce which side was defeated
		IslandManager.deduceWinner( responseText );

		// Deduce things about quests
		IslandManager.quest = IslandManager.parseQuest( location );

		switch ( IslandManager.quest )
		{
		case ARENA:
			IslandManager.parseArena( responseText );
			break;
		case NUNS:
			IslandManager.parseNunnery( responseText );
			break;
		}
	}

	private static final void deduceWinner( final String responseText )
	{
		boolean hippiesLost = responseText.contains( "snarfblat=149" );
		boolean fratboysLost = responseText.contains( "snarfblat=150" );
		String loser = ( !hippiesLost ) ? "fratboys" : ( !fratboysLost ) ? "hippies" : "both";
		Preferences.setString( "sideDefeated", loser );
		Preferences.setString( "warProgress", "finished" );
		CoinmastersFrame.externalUpdate();
	}
}
