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

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HedgePuzzleRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.UseLinkDecorator.UseLink;

public abstract class SorceressLairManager
{
	private static final GenericRequest QUEST_HANDLER = new GenericRequest( "" );

	// Patterns for repeated usage.
	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );
	private static final Pattern LAIR6_PATTERN = Pattern.compile( "lair6.php\\?place=(\\d+)" );
	private static final Pattern GATE_PATTERN = Pattern.compile( "<p>&quot;Through the (.*?),.*?&quot;" );

	// Items for the entryway
	public static final AdventureResult NAGAMAR = ItemPool.get( ItemPool.WAND_OF_NAGAMAR, 1 );

	private static final AdventureResult WUSSINESS = EffectPool.get( Effect.WUSSINESS );
	private static final AdventureResult HARDLY_POISONED = EffectPool.get( Effect.HARDLY_POISONED );
	private static final AdventureResult TELEPORTITIS = EffectPool.get( Effect.TELEPORTITIS );
	public static final AdventureResult EARTHEN_FIST = EffectPool.get( Effect.EARTHEN_FIST );

	private static final AdventureResult STAR_SWORD = ItemPool.get( ItemPool.STAR_SWORD, 1 );
	private static final AdventureResult STAR_CROSSBOW = ItemPool.get( ItemPool.STAR_CROSSBOW, 1 );
	private static final AdventureResult STAR_STAFF = ItemPool.get( ItemPool.STAR_STAFF, 1 );
	private static final AdventureResult STAR_HAT = ItemPool.get( ItemPool.STAR_HAT, 1 );

	private static final AdventureResult [] STRINGED = new AdventureResult[]
	{
		ItemPool.get( ItemPool.ACOUSTIC_GUITAR, 1 ),
		ItemPool.get( ItemPool.HEAVY_METAL_GUITAR, 1 ),
		ItemPool.get( ItemPool.STONE_BANJO, 1 ),
		ItemPool.get( ItemPool.DISCO_BANJO, 1 ),
		ItemPool.get( ItemPool.SHAGADELIC_DISCO_BANJO, 1 ),
		ItemPool.get( ItemPool.SEEGERS_BANJO, 1 ),
		ItemPool.get( ItemPool.CRIMBO_UKELELE, 1 ),
		ItemPool.get( ItemPool.MASSIVE_SITAR, 1 ),
		ItemPool.get( ItemPool.ZIM_MERMANS_GUITAR, 1 ),
		ItemPool.get( ItemPool.GUITAR_4D, 1 ),
		ItemPool.get( ItemPool.HALF_GUITAR, 1 ),
		ItemPool.get( ItemPool.OOT_BIWA, 1 ),
		ItemPool.get( ItemPool.PLASTIC_GUITAR, 1 ),
	};

	private static final AdventureResult BROKEN_SKULL = ItemPool.get( ItemPool.BROKEN_SKULL, 1 );
	private static final AdventureResult BONE_RATTLE = ItemPool.get( ItemPool.BONE_RATTLE, 1 );
	private static final AdventureResult [] PERCUSSION = new AdventureResult[]
	{
		SorceressLairManager.BONE_RATTLE,
		ItemPool.get( ItemPool.TAMBOURINE, 1 ),
		ItemPool.get( ItemPool.JUNGLE_DRUM, 1 ),
		ItemPool.get( ItemPool.HIPPY_BONGO, 1 ),
		ItemPool.get( ItemPool.BASS_DRUM, 1 ),
		ItemPool.get( ItemPool.KETTLE_DRUM, 1 ),
		SorceressLairManager.BROKEN_SKULL
	};

	private static final AdventureResult [] ACCORDIONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.STOLEN_ACCORDION, 1 ),
		ItemPool.get( ItemPool.CALAVERA_CONCERTINA, 1 ),
		ItemPool.get( ItemPool.ROCK_N_ROLL_LEGEND, 1 ),
		ItemPool.get( ItemPool.SQUEEZEBOX_OF_THE_AGES, 1 ),
		ItemPool.get( ItemPool.TRICKSTER_TRIKITIXA, 1 ),
	};

	private static final AdventureResult CLOVER = ItemPool.get( ItemPool.TEN_LEAF_CLOVER, 1 );

	private static final AdventureResult DIGITAL = ItemPool.get( ItemPool.DIGITAL_KEY, 1 );
	private static final AdventureResult STAR_KEY = ItemPool.get( ItemPool.STAR_KEY, 1 );
	private static final AdventureResult SKELETON = ItemPool.get( ItemPool.SKELETON_KEY, 1 );
	private static final AdventureResult KEY_RING = ItemPool.get( ItemPool.SKELETON_KEY_RING, 1 );

	private static final AdventureResult BORIS = ItemPool.get( ItemPool.BORIS_KEY, 1 );
	private static final AdventureResult JARLSBERG = ItemPool.get( ItemPool.JARLSBERG_KEY, 1 );
	private static final AdventureResult SNEAKY_PETE = ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 );
	private static final AdventureResult BALLOON = ItemPool.get( ItemPool.BALLOON_MONKEY, 1 );

	// Results of key puzzles

	private static final AdventureResult STRUMMING = ItemPool.get( ItemPool.SINISTER_STRUMMING, 1 );
	private static final AdventureResult SQUEEZINGS = ItemPool.get( ItemPool.SQUEEZINGS_OF_WOE, 1 );
	private static final AdventureResult RHYTHM = ItemPool.get( ItemPool.REALLY_EVIL_RHYTHM, 1 );

	private static final AdventureResult BOWL = ItemPool.get( ItemPool.FISHBOWL, 1 );
	private static final AdventureResult TANK = ItemPool.get( ItemPool.FISHTANK, 1 );
	private static final AdventureResult HOSE = ItemPool.get( ItemPool.FISH_HOSE, 1 );
	private static final AdventureResult HOSE_TANK = ItemPool.get( ItemPool.HOSED_TANK, 1 );
	private static final AdventureResult HOSE_BOWL = ItemPool.get( ItemPool.HOSED_FISHBOWL, 1 );
	private static final AdventureResult SCUBA = ItemPool.get( ItemPool.SCUBA_GEAR, 1 );

	// Items for the shadow battle

	private static final AdventureResult MIRROR_SHARD = ItemPool.get( ItemPool.MIRROR_SHARD, 1 );

	private static final AdventureResult [] HEALING_ITEMS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.RED_PIXEL_POTION, 1 ),
		ItemPool.get( ItemPool.FILTHY_POULTICE, 1 ),
		ItemPool.get( ItemPool.GAUZE_GARTER, 1 ),
	};

	// Gates, what they look like through the Telescope, the effects you
	// need to pass them, and where to get it

	public static final String[][] GATE_DATA =
	{
		// Beecore gate
		{
			"gate of bees",
			"a mass of bees",
			"Float Like a Butterfly, Smell Like a Bee",
			"honeypot"
		},

		// The first gate: Miscellaneous effects
		{
			"gate of hilarity",
			"a banana peel",
			"Comic Violence",
			"gremlin juice"
		},
		{
			"gate of humility",
			"a cowardly-looking man",
			"Wussiness",
			"wussiness potion"
		},
		{
			"gate of morose morbidity and moping",
			"a glum teenager",
			"Rainy Soul Miasma",
			"thin black candle",
			"picture of a dead guy's girlfriend"
		},
		{
			"gate of slack",
			"a smiling man smoking a pipe",
			"Extreme Muscle Relaxation",
			"Mick's IcyVapoHotness Rub"
		},
		{
			"gate of spirit",
			"an armchair",
			"Woad Warrior",
			"pygmy pygment"
		},
		{
			"gate of the porcupine",
			"a hedgehog",
			"Spiky Hair",
			"super-spiky hair gel"
		},
		{
			"twitching gates of the suc rose",
			"a rose",
			"Sugar Rush",
			"Angry Farmer candy",
			"Tasty Fun Good rice candy",
			"marzipan skull",
			"Crimbo candied pecan",
			"Crimbo fudge",
			"Crimbo peppermint bark",
		},
		{
			"gate of the viper",
			"a coiled viper",
			"Deadly Flashing Blade",
			"adder bladder"
		},
		{
			"locked gate",
			"a raven",
			"Locks Like the Raven",
			"Black No. 2"
		},

		// The second gate: South of the Border effects
		{
			"gate of bad taste",
			"",
			"Spicy Limeness",
			"lime-and-chile-flavored chewing gum"
		},
		{
			"gate of flame",
			"",
			"Spicy Mouth",
			"jaba&ntilde;ero-flavored chewing gum"
		},
		{
			"gate of intrigue",
			"",
			"Mysteriously Handsome",
			"handsomeness potion"
		},
		{
			"gate of machismo",
			"",
			"Engorged Weapon",
			"Meleegra&trade; pills"
		},
		{
			"gate of mystery",
			"",
			"Mystic Pickleness",
			"pickle-flavored chewing gum"
		},
		{
			"gate of the dead",
			"",
			"Hombre Muerto Caminando",
			"marzipan skull"
		},
		{
			"gate of torment",
			"",
			"Tamarind Torment",
			"tamarind-flavored chewing gum"
		},
		{
			"gate of zest",
			"",
			"Spicy Limeness",
			"lime-and-chile-flavored chewing gum"
		},

		// The third gate: Bang potion effects
		{
			"gate of light",
			"",
			"Izchak's Blessing",
			"potion of blessing"
		},
		{
			"gate of that which is hidden",
			"",
			"Object Detection",
			"potion of detection"
		},
		{
			"gate of the mind",
			"",
			"Strange Mental Acuity",
			"potion of mental acuity"
		},
		{
			"gate of the observant",
			"",
			"Object Detection",
			"potion of detection"
		},
		{
			"gate of the ogre",
			"",
			"Strength of Ten Ettins",
			"potion of ettin strength"
		},
		{
			"gate that is not a gate",
			"",
			"Teleportitis",
			"potion of teleportitis",
			"ring of teleportation"
		},
	};

	public static String gateName( final String[] gateData )
	{
		return gateData[ 0 ];
	}

	public static String gateDescription( final String[] gateData )
	{
		return gateData[ 1 ];
	}

	public static String gateEffect( final String[] gateData )
	{
		return gateData[ 2 ];
	}

	public static String[] findGateByName( final String gateName )
	{
		for ( int i = 0; i < SorceressLairManager.GATE_DATA.length; ++i )
		{
			if ( gateName.equalsIgnoreCase( SorceressLairManager.GATE_DATA[ i ][ 0 ] ) )
			{
				return SorceressLairManager.GATE_DATA[ i ];
			}
		}
		return null;
	}

	public static String[] findGateByDescription( final String text )
	{
		for ( int i = 0; i < SorceressLairManager.GATE_DATA.length; ++i )
		{
			String desc = SorceressLairManager.GATE_DATA[ i ][ 1 ];
			if ( desc.equals( "" ) )
			{
				continue;
			}
			if ( text.indexOf( desc ) != -1 )
			{
				return SorceressLairManager.GATE_DATA[ i ];
			}
		}
		return null;
	}

	// Guardians, what they look like through the Telescope, and the items
	// that defeat them

	public static final String[][] GUARDIAN_DATA =
	{
		{
			"beer batter",
			"tip of a baseball bat",
			"baseball"
		},
		{
			"best-selling novelist",
			"writing desk",
			"plot hole"
		},
		{
			"big meat golem",
			"huge face made of Meat",
			"meat vortex"
		},
		{
			"bowling cricket",
			"fancy-looking tophat",
			"sonar-in-a-biscuit"
		},
		{
			"bronze chef",
			"bronze figure holding a spatula",
			"leftovers of indeterminate origin"
		},
		{
			"concert pianist",
			"long coattails",
			"Knob Goblin firecracker"
		},
		{
			"darkness",
			"strange shadow",
			"inkwell"
		},
		{
			"el diablo",
			"neck of a huge bass guitar",
			"mariachi G-string"
		},
		{
			"electron submarine",
			"periscope",
			"photoprotoneutron torpedo"
		},
		{
			"endangered inflatable white tiger",
			"giant white ear",
			"pygmy blowgun"
		},
		{
			"fancy bath slug",
			"slimy eyestalk",
			"fancy bath salts"
		},
		{
			"fickle finger of f8",
			"giant cuticle",
			"razor-sharp can lid"
		},
		{
			"flaming samurai",
			"flaming katana",
			"frigid ninja stars"
		},
		{
			"giant desktop globe",
			"the North Pole",
			"NG"
		},
		{
			"giant fried egg",
			"flash of albumen",
			"black pepper"
		},
		{
			"ice cube",
			"moonlight reflecting off of what appears to be ice",
			"hair spray"
		},
		{
			"malevolent crop circle",
			"amber waves of grain",
			"bronzed locust"
		},
		{
			"possessed pipe-organ",
			"pipes with steam shooting out of them",
			"powdered organs"
		},
		{
			"pretty fly",
			"translucent wing",
			"spider web"
		},
		{
			"tyrannosaurus tex",
			"large cowboy hat",
			"chaos butterfly"
		},
		{
			"vicious easel",
			"tall wooden frame",
			"disease"
		},
		// The possibilities exclusive to the 6th floor must be last
		{
			"enraged cow",
			"pair of horns",
			"barbed-wire fence"
		},
		{
			"giant bee",
			"formidable stinger",
			"tropical orchid"
		},
		{
			"collapsed mineshaft golem",
			"wooden beam",
			"stick of dynamite"
		},
	};

	public static String guardianName( final String[] guardianData )
	{
		return guardianData[ 0 ];
	}

	public static String guardianDescription( final String[] guardianData )
	{
		return guardianData[ 1 ];
	}

	public static String guardianItem( final String[] guardianData )
	{
		return guardianData[ 2 ];
	}

	public static String[] findGuardianByName( final String guardianName )
	{
		for ( int i = 0; i < SorceressLairManager.GUARDIAN_DATA.length; ++i )
		{
			if ( guardianName.equalsIgnoreCase( SorceressLairManager.GUARDIAN_DATA[ i ][ 0 ] ) )
			{
				return SorceressLairManager.GUARDIAN_DATA[ i ];
			}
		}
		return null;
	}

	public static String[] findGuardianByDescription( final String text )
	{
		for ( int i = 0; i < SorceressLairManager.GUARDIAN_DATA.length; ++i )
		{
			String desc = SorceressLairManager.GUARDIAN_DATA[ i ][ 1 ];
			if ( desc.equals( "" ) )
			{
				continue;
			}
			if ( text.indexOf( desc ) != -1 )
			{
				return SorceressLairManager.GUARDIAN_DATA[ i ];
			}
		}
		return null;
	}

	// Items for the Sorceress's Chamber

	private static final FamiliarData STARFISH = new FamiliarData( FamiliarPool.STARFISH );
	private static final AdventureResult STARFISH_ITEM = ItemPool.get( ItemPool.STAR_STARFISH, 1 );

	// Familiars and the familiars that defeat them
	private static final String[][] FAMILIAR_DATA =
	{
		{ "giant sabre-toothed lime", "Levitating Potato" },
		{ "giant mosquito", "Sabre-Toothed Lime" },
		{ "giant barrrnacle", "Angry Goat" },
		{ "giant goat", "Mosquito" },
		{ "giant potato", "Barrrnacle" }
	};

	private static final boolean checkPrerequisites( final int min, final int max )
	{
		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure you have a starfish.  If not,
		// acquire the item and use it; use the
		// default acquisition mechanisms.

		boolean needStarfish = !KoLCharacter.inAxecore() && !KoLCharacter.inZombiecore();

		if ( !KoLCharacter.getFamiliarList().contains( SorceressLairManager.STARFISH ) && needStarfish )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( SorceressLairManager.STARFISH_ITEM ) );
			if ( !KoLmafia.permitsContinue() )
			{
				return false;
			}
		}

		// Make sure he's been given the quest

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "main.php" ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair.php" ) == -1 )
		{
			// Visit the council to see if the quest can be
			// unlocked, but only if you've reached level 13.

			boolean unlockedQuest = false;
			if ( KoLCharacter.getLevel() >= 13 )
			{
				// We should theoretically be able to figure out
				// whether or not the quest is unlocked from the
				// HTML in the council request, but for now, use
				// this inefficient workaround.

				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "main.php" ) );
				unlockedQuest = SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair.php" ) != -1;
			}

			if ( !unlockedQuest )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR, "You haven't been given the quest to fight the Sorceress!" );
				return false;
			}
		}

		// Make sure he can get to the desired area

		// Deduce based on which image map is used:
		//
		// NoMap = lair1
		// Map = lair1, lair3
		// Map2 = lair1, lair3, lair4
		// Map3 = lair1, lair3, lair4, lair5
		// Map4 = lair1, lair3, lair4, lair5, lair6

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair.php" ) );
		Matcher mapMatcher = SorceressLairManager.MAP_PATTERN.matcher( SorceressLairManager.QUEST_HANDLER.responseText );

		if ( mapMatcher.find() )
		{
			String map = mapMatcher.group( 1 );
			int reached;

			if ( map.equals( "NoMap" ) )
			{
				reached = 1;
			}
			else if ( map.equals( "Map" ) )
			{
				reached = 3;
			}
			else if ( map.equals( "Map2" ) )
			{
				reached = 4;
			}
			else if ( map.equals( "Map3" ) )
			{
				reached = 5;
			}
			else if ( map.equals( "Map4" ) )
			{
				reached = 6;
			}
			else
			{
				reached = 0;
			}

			if ( reached < min )
			{
				switch ( min )
				{
				case 0:
				case 1:
					KoLmafia.updateDisplay( MafiaState.ERROR, "The sorceress quest has not yet unlocked." );
					return false;
				case 2:
				case 3:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the entryway first." );
					return false;
				case 4:
				case 5:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the hedge maze first." );
					return false;
				case 6:
					KoLmafia.updateDisplay( MafiaState.ERROR, "You must complete the tower first." );
					return false;
				}
			}

			if ( reached > max )
			{
				KoLmafia.updateDisplay( "You're already past this script." );
				return false;
			}
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	private static final AdventureResult pickOne( final AdventureResult[] itemOptions )
	{
		if ( itemOptions.length == 1 )
		{
			return itemOptions[ 0 ];
		}

		for ( int i = 0; i < itemOptions.length; ++i )
		{
			if ( KoLConstants.inventory.contains( itemOptions[ i ] ) )
			{
				return itemOptions[ i ];
			}
		}

		for ( int i = 0; i < itemOptions.length; ++i )
		{
			if ( SorceressLairManager.isItemAvailable( itemOptions[ i ] ) )
			{
				return itemOptions[ i ];
			}
		}

		return itemOptions[ 0 ];
	}

	private static final boolean isItemAvailable( final AdventureResult item )
	{
		return InventoryManager.hasItem( item, true );
	}

	public static final void completeCloveredEntryway()
	{
		SpecialOutfit.createImplicitCheckpoint();
		SorceressLairManager.completeEntryway( true );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public static final void completeCloverlessEntryway()
	{
		SpecialOutfit.createImplicitCheckpoint();
		SorceressLairManager.completeEntryway( false );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void completeEntryway( final boolean useCloverForSkeleton )
	{
		if ( !SorceressLairManager.checkPrerequisites( 1, 2 ) )
		{
			return;
		}

		// If you couldn't complete the gateway, then return
		// from this method call.

		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();
		if ( !SorceressLairManager.completeGateway() )
		{
			return;
		}

		List<AdventureResult> requirements = new ArrayList<AdventureResult>();

		// Next, figure out which instruments are needed for the final
		// stage of the entryway.

		requirements.add( SorceressLairManager.pickOne( STRINGED ) );
		AdventureResult percussion = SorceressLairManager.pickOne( PERCUSSION );
		requirements.add( percussion );
		requirements.add( SorceressLairManager.pickOne( ACCORDIONS ) );

		// If he brought a balloon monkey, get him an easter egg

		if ( SorceressLairManager.isItemAvailable( SorceressLairManager.BALLOON ) )
		{
			InventoryManager.retrieveItem( SorceressLairManager.BALLOON );
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.BALLOON.getItemId() ) );
		}

		// Now, iterate through each of the completion steps;
		// at the end, check to make sure you've completed
		// all the needed requirements.

		requirements.addAll( SorceressLairManager.retrieveRhythm( useCloverForSkeleton ) );
		requirements.addAll( SorceressLairManager.retrieveStrumming( originalFamiliar ) );
		requirements.addAll( SorceressLairManager.retrieveSqueezings() );
		requirements.addAll( SorceressLairManager.retrieveScubaGear() );

		if ( !KoLCharacter.inAxecore() )
		{
			RequestThread.postRequest( new FamiliarRequest( originalFamiliar ) );
		}

		if ( !KoLmafia.checkRequirements( requirements ) || KoLmafia.refusesContinue() )
		{
			return;
		}

		if ( InventoryManager.hasItem( SorceressLairManager.HOSE_BOWL ) && InventoryManager.hasItem( SorceressLairManager.TANK ) )
		{
			( new UntinkerRequest( SorceressLairManager.HOSE_BOWL.getItemId() ) ).run();
		}

		RequestThread.postRequest( new EquipmentRequest( SorceressLairManager.SCUBA, EquipmentManager.ACCESSORY1 ) );

		KoLmafia.updateDisplay( "Pressing switch beyond odor..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?action=odor" ) );

		// If you decided to use a broken skull because
		// you had no other items, untinker the key.

		if ( percussion == SorceressLairManager.BROKEN_SKULL )
		{
			RequestThread.postRequest( new UntinkerRequest( SorceressLairManager.SKELETON.getItemId() ) );
			CreateItemRequest request = CreateItemRequest.getInstance( SorceressLairManager.BONE_RATTLE );
			request.setQuantityNeeded( 1 );
			RequestThread.postRequest( request );
		}

		// Finally, arm the stone mariachis with their
		// appropriate instruments.

		KoLmafia.updateDisplay( "Arming stone mariachis..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?action=statues" ) );

		// "As the mariachis reach a dire crescendo (Hey, have you
		// heard my new band, Dire Crescendo?) the gate behind the
		// statues slowly grinds open, revealing the way to the
		// Sorceress' courtyard."

		// Just check to see if there is a link to lair3.php

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair3.php" ) == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to complete entryway." );
			return;
		}

		// This consumes the tablets

		ResultProcessor.processResult( SorceressLairManager.RHYTHM.getNegation() );
		ResultProcessor.processResult( SorceressLairManager.STRUMMING.getNegation() );
		ResultProcessor.processResult( SorceressLairManager.SQUEEZINGS.getNegation() );

		KoLmafia.updateDisplay( "Sorceress entryway complete." );
	}

	private static final boolean completeGateway()
	{
		// Check to see if the person has crossed through the
		// gates already.  If they haven't, then that's the
		// only time you need the special effects.

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair1.php" ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "gatesdone" ) == -1 &&
		     SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "cave1beesdone" ) == -1)
		{
			KoLmafia.updateDisplay( "Crossing three door puzzle..." );
			// Do not attempt to cross the gate of bees without the
			// correct effect active.
			if ( !KoLCharacter.inBeecore() )
			{
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair1.php?action=gates" ) );
			}

			if ( !SorceressLairManager.passThreeGatePuzzle() )
			{
				return false;
			}

			if ( Preferences.getBoolean( "removeMalignantEffects" ) )
			{
				// We want to remove unpleasant effects created by
				// consuming items used to pass the gates.

				if ( KoLConstants.activeEffects.contains( SorceressLairManager.WUSSINESS ) )
				{
					RequestThread.postRequest( new UneffectRequest( SorceressLairManager.WUSSINESS ) );
				}

				if ( KoLConstants.activeEffects.contains( SorceressLairManager.HARDLY_POISONED ) )
				{
					RequestThread.postRequest( new UneffectRequest( SorceressLairManager.HARDLY_POISONED ) );
				}

				if ( KoLConstants.activeEffects.contains( SorceressLairManager.TELEPORTITIS ) )
				{
					RequestThread.postRequest( new UneffectRequest( SorceressLairManager.TELEPORTITIS ) );
				}
			}
		}

		// Now, unequip all of your equipment and cross through
		// the mirror. Process the mirror shard that results.

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair2.php" ) == -1 )
		{
			RequestThread.postRequest( new FamiliarRequest( FamiliarData.NO_FAMILIAR ) );
			RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );

			// We will need to re-equip

			KoLmafia.updateDisplay( "Crossing mirror puzzle..." );
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair1.php?action=mirror" ) );
		}

		return true;
	}

	private static final boolean passThreeGatePuzzle()
	{
		// Visiting the gates with the correct effects opens them.
		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "gatesdone.gif" ) != -1 ||
		     SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "cave1beesdone.gif" ) != -1 )
		{
			return true;
		}

		// Get a list of items we need to consume to get effects
		// we don't already have

		List<AdventureResult> requirements = new ArrayList<AdventureResult>();
		if ( KoLCharacter.inBeecore() )
		{
			SorceressLairManager.addGateItem( "gate of bees", requirements );
		}
		else
		{
			Matcher gateMatcher = SorceressLairManager.GATE_PATTERN.matcher( SorceressLairManager.QUEST_HANDLER.responseText );

			SorceressLairManager.addGateItem( 1, gateMatcher, requirements );
			SorceressLairManager.addGateItem( 2, gateMatcher, requirements );
			SorceressLairManager.addGateItem( 3, gateMatcher, requirements );
		}

		// Punt now if we couldn't parse a gate
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		List<AdventureResult> missing = new ArrayList<AdventureResult>();

		// Get the necessary items into inventory
		for ( int i = 0; i < requirements.size(); ++i )
		{
			AdventureResult item = (AdventureResult) requirements.get( i );
			// See if it's an unknown bang potion
			if ( item.getItemId() == -1 )
			{
				missing.add( item );
				continue;
			}

			// See if the user aborted
			if ( !KoLmafia.permitsContinue() )
			{
				missing.add( item );
				continue;
			}

			// Otherwise, move the item into inventory
			if ( !InventoryManager.retrieveItem( item ) )
			{
				missing.add( item );
			}
		}

		// If we're missing any items, report all of them
		if ( missing.size() > 0 )
		{
			String items = "";
			for ( int i = 0; i < missing.size(); ++i )
			{
				AdventureResult item = (AdventureResult) missing.get( i );
				if ( i > 0 )
				{
					items += " and ";
				}
				items += "a " + item.getName();
			}
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need " + items );
			return false;
		}

		// See if the user aborted
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		// Use the necessary items
		for ( int i = 0; i < requirements.size(); ++i )
		{
			AdventureResult item = (AdventureResult) requirements.get( i );
			RequestThread.postRequest( UseItemRequest.getInstance( item ) );
		}

		// The gates should be passable. Visit them again.
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair1.php?action=gates" ) );
		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "gatesdone.gif" ) != -1 ||
		     SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "cave1beesdone.gif" ) != -1 )
		{
			return true;
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to pass gates!" );
		return false;
	}

	private static final void addGateItem( final int gate, final Matcher gateMatcher, final List<AdventureResult> requirements )
	{
		// Find the name of the gate from the responseText
		if ( !gateMatcher.find() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Gate " + gate + " is missing." );
			return;
		}

		String gateName = gateMatcher.group( 1 );
		if ( gateName == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to detect gate" + gate );
			return;
		}

		SorceressLairManager.addGateItem( gateName, requirements );
	}

	private static final void addGateItem( final String gateName, final List<AdventureResult> requirements )
	{
		// Find the gate in our data

		String[] gateData = SorceressLairManager.findGateByName( gateName );

		if ( gateData == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unrecognized gate: " + gateName );
			return;
		}

		// See if we have the needed effect already
		AdventureResult effect = new AdventureResult( SorceressLairManager.gateEffect( gateData ), 1, true );
		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			return;
		}

		// Pick an item that grants the effect
		AdventureResult[] items = new AdventureResult[ gateData.length - 3 ];
		for ( int i = 3; i < gateData.length; ++i )
		{
			String name = gateData[ i ];
			AdventureResult item = AdventureResult.pseudoItem( name );
			items[ i - 3 ] = item;
		}

		AdventureResult item = SorceressLairManager.pickOne( items );

		// Add the item to our list of requirements
		requirements.add( item );
	}

	private static final List<AdventureResult> retrieveRhythm( final boolean useCloverForSkeleton )
	{
		// Skeleton key and a clover unless you already have the
		// Really Evil Rhythms

		List<AdventureResult> requirements = new ArrayList<AdventureResult>();

		if ( SorceressLairManager.isItemAvailable( SorceressLairManager.RHYTHM ) )
		{
			return requirements;
		}

		if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.SKELETON ) && SorceressLairManager.isItemAvailable( SorceressLairManager.KEY_RING ) )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( SorceressLairManager.KEY_RING ) );
		}

		if ( !InventoryManager.retrieveItem( SorceressLairManager.SKELETON ) )
		{
			requirements.add( SorceressLairManager.SKELETON );
			return requirements;
		}

		if ( useCloverForSkeleton )
		{
			if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.CLOVER ) )
			{
				requirements.add( SorceressLairManager.CLOVER );
				return requirements;
			}
			// Temporarily disables clover protection in case we
			// "retrieve" the clover by buying it in the mall.
			GenericRequest.ascending = true;
			InventoryManager.retrieveItem( SorceressLairManager.CLOVER );
			GenericRequest.ascending = false;
		}
		else
		{	// we want any HP-increasing benefits of the player's equipment
			SpecialOutfit.restoreImplicitCheckpoint();
			SpecialOutfit.createImplicitCheckpoint();
		}

		do
		{
			// The character needs to have at least 50 HP, or 25% of
			// maximum HP (whichever is greater) in order to play
			// the skeleton dice game, UNLESS you have a clover.

			int healthNeeded = Math.max( KoLCharacter.getMaximumHP() / 4, 50 );
			RecoveryManager.recoverHP( healthNeeded + 1 );

			// Verify that you have enough HP to proceed with the
			// skeleton dice game.

			if ( KoLCharacter.getCurrentHP() <= healthNeeded )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR, "You must have more than " + healthNeeded + " HP to proceed." );
				return requirements;
			}

			// Next, handle the form for the skeleton key to
			// get the Really Evil Rhythm. This uses up the
			// clover you had, so process it.

			KoLmafia.updateDisplay( "Inserting skeleton key..." );
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.SKELETON.getItemId() ) );

			if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
			{
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=skel" ) );
			}
		}
		while ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "looks like I owe you a beating" ) != -1 );

		if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.RHYTHM ) )
		{
			requirements.add( SorceressLairManager.RHYTHM );
		}

		return requirements;
	}

	private static final List<AdventureResult> retrieveStrumming( FamiliarData originalFamiliar )
	{
		// Decide on which star weapon should be available for
		// this whole process.

		List<AdventureResult> requirements = new ArrayList<AdventureResult>();
		boolean inFistcore = KoLCharacter.inFistcore();
		boolean inAxecore = KoLCharacter.inAxecore();
		boolean needWeapon = !inFistcore && !inAxecore;

		if ( SorceressLairManager.isItemAvailable( SorceressLairManager.STRUMMING ) )
		{
			return requirements;
		}

		if ( !InventoryManager.retrieveItem( SorceressLairManager.STAR_KEY ) )
		{
			requirements.add( SorceressLairManager.STAR_KEY );
		}

		if ( !InventoryManager.retrieveItem( SorceressLairManager.STAR_HAT ) )
		{
			requirements.add( SorceressLairManager.STAR_HAT );
		}

		AdventureResult starWeapon = null;

		if ( needWeapon )
		{
			// See which ones are available

			boolean hasSword = InventoryManager.hasItem( SorceressLairManager.STAR_SWORD );
			boolean hasStaff = InventoryManager.hasItem( SorceressLairManager.STAR_STAFF );
			boolean hasCrossbow = InventoryManager.hasItem( SorceressLairManager.STAR_CROSSBOW );

			// See which ones he can use

			boolean canUseSword = EquipmentManager.canEquip( SorceressLairManager.STAR_SWORD );
			boolean canUseStaff = EquipmentManager.canEquip( SorceressLairManager.STAR_STAFF );
			boolean canUseCrossbow = EquipmentManager.canEquip( SorceressLairManager.STAR_CROSSBOW );

			// Pick one that he has and can use

			if ( hasSword && canUseSword )
			{
				starWeapon = SorceressLairManager.STAR_SWORD;
			}
			else if ( hasStaff && canUseStaff )
			{
				starWeapon = SorceressLairManager.STAR_STAFF;
			}
			else if ( hasCrossbow && canUseCrossbow )
			{
				starWeapon = SorceressLairManager.STAR_CROSSBOW;
			}
			else if ( canUseSword && SorceressLairManager.isItemAvailable( SorceressLairManager.STAR_SWORD ) )
			{
				starWeapon = SorceressLairManager.STAR_SWORD;
			}
			else if ( canUseStaff && SorceressLairManager.isItemAvailable( SorceressLairManager.STAR_STAFF ) )
			{
				starWeapon = SorceressLairManager.STAR_STAFF;
			}
			else if ( canUseCrossbow && SorceressLairManager.isItemAvailable( SorceressLairManager.STAR_CROSSBOW ) )
			{
				starWeapon = SorceressLairManager.STAR_CROSSBOW;
			}
			else if ( canUseSword )
			{
				starWeapon = SorceressLairManager.STAR_SWORD;
			}
			else if ( canUseStaff )
			{
				starWeapon = SorceressLairManager.STAR_STAFF;
			}
			else if ( canUseCrossbow )
			{
				starWeapon = SorceressLairManager.STAR_CROSSBOW;
			}
			else if ( hasSword )
			{
				starWeapon = SorceressLairManager.STAR_SWORD;
			}
			else if ( hasStaff )
			{
				starWeapon = SorceressLairManager.STAR_STAFF;
			}
			else if ( hasCrossbow )
			{
				starWeapon = SorceressLairManager.STAR_CROSSBOW;
			}
			else
			{
				starWeapon = SorceressLairManager.STAR_SWORD;
			}

			// Star equipment check.

			if ( !InventoryManager.retrieveItem( starWeapon ) )
			{
				requirements.add( starWeapon );
			}
		}

		if ( !requirements.isEmpty() )
		{
			return requirements;
		}

		// If you can't equip the appropriate weapon and hat,
		// then tell the player they lack the required stats.

		if ( needWeapon && !EquipmentManager.canEquip( starWeapon.getName() ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Stats too low to equip a star weapon." );
			return requirements;
		}

		if ( !EquipmentManager.canEquip( SorceressLairManager.STAR_HAT.getName() ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Stats too low to equip a star hat." );
			return requirements;
		}

		FamiliarData starfish = KoLCharacter.findFamiliar( "Star Starfish" );

		if ( !KoLCharacter.inAxecore() && !KoLCharacter.inZombiecore() && starfish == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a Star Starfish!" );
			return requirements;
		}

		// Now handle the form for the star key to get the Sinister
		// Strumming.  Note that this will require you to re-equip your
		// star weapon and a star buckler and switch to a starfish first.

		if ( inFistcore )
		{
			// Cast Worldpunch. Since you need it for the Tr4pz0r
			// quest, you must know it.
			if ( !KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) )
			{
				UseSkillRequest request = UseSkillRequest.getInstance( "Worldpunch" );
				request.setBuffCount( 1 );
				RequestThread.postRequest( request );
			}
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Cast Worldpunch and try again." );
				return requirements;
			}
		}
		else if ( inAxecore )
		{
			// You have to have Trusty equipped
			if ( !KoLCharacter.hasEquipped( EquipmentRequest.TRUSTY, EquipmentManager.WEAPON ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.TRUSTY, EquipmentManager.WEAPON ) );
			}
		}
		else if ( needWeapon )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.OFFHAND ) );
			RequestThread.postRequest( new EquipmentRequest( starWeapon, EquipmentManager.WEAPON ) );
		}

		RequestThread.postRequest( new EquipmentRequest( SorceressLairManager.STAR_HAT, EquipmentManager.HAT ) );

		if ( !KoLCharacter.inAxecore() && !KoLCharacter.inZombiecore() )
		{
			RequestThread.postRequest( new FamiliarRequest( starfish ) );
		}

		// In zombie runs, you don't need a starfish, but you do need a familiar.  Restore your original familiar a little earlier.
		if ( KoLCharacter.inZombiecore() )
		{
			RequestThread.postRequest( new FamiliarRequest( originalFamiliar ) );
		}

		KoLmafia.updateDisplay( "Inserting Richard's star key..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.STAR_KEY.getItemId() ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=starcage" ) );

			// For unknown reasons, this doesn't always work
			// Error check the possibilities

			// "You beat on the cage with your weapon, but
			// to no avail. It doesn't appear to be made
			// out of the right stuff."

			if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "right stuff" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to equip a star weapon." );
			}

			// "A fragment of a line hits you really hard
			// in the face, and it knocks you back into the
			// main cavern."

			if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "knocks you back" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to equip star hat." );
			}

			// "Trog creeps toward the pedestal, but is
			// blown backwards.  You give up, and go back
			// out to the main cavern."

			if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "You give up" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to equip star starfish." );
			}
		}

		return requirements;
	}

	private static final List<AdventureResult> retrieveSqueezings()
	{
		// Digital key unless you already have the Squeezings of Woe

		List<AdventureResult> requirements = new ArrayList<AdventureResult>();

		if ( SorceressLairManager.isItemAvailable( SorceressLairManager.SQUEEZINGS ) )
		{
			return requirements;
		}

		if ( !InventoryManager.retrieveItem( SorceressLairManager.DIGITAL ) )
		{
			requirements.add( SorceressLairManager.DIGITAL );
			return requirements;
		}

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		KoLmafia.updateDisplay( "Inserting digital key..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.DIGITAL.getItemId() ) );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sequence&seq1=up&seq2=up&seq3=down&seq4=down&seq5=left&seq6=right&seq7=left&seq8=right&seq9=b&seq10=a" ) );
		}

		return requirements;
	}

	private static final List<AdventureResult> retrieveScubaGear()
	{
		List<AdventureResult> requirements = new ArrayList<AdventureResult>();

		// The three hero keys are needed to get the SCUBA gear

		if ( SorceressLairManager.isItemAvailable( SorceressLairManager.SCUBA ) )
		{
			return requirements;
		}

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.BOWL ) && !SorceressLairManager.isItemAvailable( SorceressLairManager.HOSE_BOWL ) )
		{
			if ( !InventoryManager.retrieveItem( SorceressLairManager.BORIS ) )
			{
				requirements.add( SorceressLairManager.BORIS );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Boris's key..." );
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.BORIS.getItemId() ) );

				if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle1&answer=fish" ) );
				}
			}
		}

		if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.TANK ) && !SorceressLairManager.isItemAvailable( SorceressLairManager.HOSE_TANK ) )
		{
			if ( !InventoryManager.retrieveItem( SorceressLairManager.JARLSBERG ) )
			{
				requirements.add( SorceressLairManager.JARLSBERG );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Jarlsberg's key..." );
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.JARLSBERG.getItemId() ) );

				if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle2&answer=phish" ) );
				}
			}
		}

		if ( !SorceressLairManager.isItemAvailable( SorceressLairManager.HOSE ) && !SorceressLairManager.isItemAvailable( SorceressLairManager.HOSE_TANK ) && !SorceressLairManager.isItemAvailable( SorceressLairManager.HOSE_BOWL ) )
		{
			if ( !InventoryManager.retrieveItem( SorceressLairManager.SNEAKY_PETE ) )
			{
				requirements.add( SorceressLairManager.SNEAKY_PETE );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Sneaky Pete's key..." );
				RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLairManager.SNEAKY_PETE.getItemId() ) );

				if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle3&answer=fsh" ) );
				}
			}
		}

		// Equip the SCUBA gear.  Attempting to retrieve it
		// will automatically create it.

		if ( !InventoryManager.retrieveItem( SorceressLairManager.SCUBA ) )
		{
			requirements.add( SorceressLairManager.SCUBA );
			return requirements;
		}

		return requirements;
	}

	public static final void completeHedgeMaze()
	{
		if ( !SorceressLairManager.checkPrerequisites( 3, 3 ) )
		{
			return;
		}

		// Check to see if you've run out of puzzle pieces.
		// If you have, don't bother running the puzzle.

		if ( HedgePuzzleRequest.PUZZLE_PIECE.getCount( KoLConstants.inventory ) == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Ran out of puzzle pieces." );
			return;
		}

		HedgePuzzleRequest.completeHedgeMaze( true );

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Hedge maze quest complete." );
	}

	public static final int fightAllTowerGuardians()
	{
		FamiliarData oldFamiliar = KoLCharacter.getFamiliar();

		int itemId = SorceressLairManager.fightTowerGuardians( true );

		FamiliarData newFamiliar = KoLCharacter.getFamiliar();

		if ( oldFamiliar.getId() != newFamiliar.getId() )
		{
			RequestThread.postRequest( new FamiliarRequest( oldFamiliar ) );
		}

		return itemId;
	}

	public static final int fightMostTowerGuardians()
	{
		FamiliarData oldFamiliar = KoLCharacter.getFamiliar();

		int itemId = SorceressLairManager.fightTowerGuardians( false );

		FamiliarData newFamiliar = KoLCharacter.getFamiliar();

		if ( oldFamiliar.getId() != newFamiliar.getId() )
		{
			RequestThread.postRequest( new FamiliarRequest( oldFamiliar ) );
		}

		return itemId;
	}

	private static final int fightTowerGuardians( boolean fightFamiliarGuardians )
	{
		if ( !SorceressLairManager.checkPrerequisites( 4, 6 ) )
		{
			return -1;
		}

		// Disable automation while Form of... Bird! is active,
		// as it disables item usage.

		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return -1;
		}

		// Determine which level you actually need to start from.

		KoLmafia.updateDisplay( "Climbing the tower..." );

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair4.php" ) );
		int currentLevel = 0;

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "lair5.php" ) != -1 )
		{
			// There is a link to higher in the tower.

			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair5.php" ) );
			currentLevel = 3;
		}

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "value=\"level1\"" ) != -1 )
		{
			currentLevel += 1;
		}
		else if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "value=\"level2\"" ) != -1 )
		{
			currentLevel += 2;
		}
		else if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "value=\"level3\"" ) != -1 )
		{
			currentLevel += 3;
		}
		else
		{
			currentLevel += 4;
		}

		int requiredItemId = -1;
		for ( int towerLevel = currentLevel; towerLevel <= 6; ++towerLevel )
		{
			requiredItemId = SorceressLairManager.fightGuardian( towerLevel );
			if ( !KoLmafia.permitsContinue() )
			{
				return requiredItemId;
			}

			if ( !SorceressLairManager.QUEST_HANDLER.containsUpdate )
			{
				RequestThread.postRequest( new ApiRequest() );
			}

			RecoveryManager.runBetweenBattleChecks( false );

			if ( requiredItemId != -1 )
			{
				return requiredItemId;
			}
		}

		// You must have at least 70 in all stats before you can enter
		// the chamber.

		if ( KoLCharacter.getBaseMuscle() < 70 || KoLCharacter.getBaseMysticality() < 70 || KoLCharacter.getBaseMoxie() < 70 )
		{
			KoLmafia.updateDisplay(
				MafiaState.ERROR, "You can't enter the chamber unless all base stats are 70 or higher." );
			return -1;
		}

		// Figure out how far he's gotten into the Sorceress's Chamber
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair6.php" ) );
		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "ascend.php" ) != -1 )
		{
			KoLmafia.updateDisplay( "You've already beaten Her Naughtiness." );
			return -1;
		}

		int n = -1;

		Matcher placeMatcher = SorceressLairManager.LAIR6_PATTERN.matcher( SorceressLairManager.QUEST_HANDLER.responseText );
		if ( placeMatcher.find() )
		{
			n = StringUtilities.parseInt( placeMatcher.group( 1 ) );
		}

		if ( n < 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Server-side change detected.  Script aborted." );
			return -1;
		}

		if ( n == 0 )
		{
			RequestThread.postRequest( new ApiRequest() );

			SorceressLairManager.findDoorCode();

			if ( KoLmafia.permitsContinue() )
			{
				++n;
			}
			else
			{
				return -1;
			}
		}

		if ( n == 1 )
		{
			SorceressLairManager.reflectEnergyBolt();

			if ( KoLmafia.permitsContinue() )
			{
				++n;
			}
			else
			{
				return -1;
			}
		}

		if ( !fightFamiliarGuardians )
		{
			KoLmafia.updateDisplay( "Path to shadow cleared." );
			return -1;
		}

		if ( n == 2 )
		{
			SorceressLairManager.fightShadow();

			if ( KoLmafia.permitsContinue() )
			{
				++n;
			}
			else
			{
				return -1;
			}
		}

		if ( n == 3 )
		{
			SorceressLairManager.familiarBattle( 3 );

			if ( KoLmafia.permitsContinue() )
			{
				++n;
			}
			else
			{
				return -1;
			}
		}

		if ( n == 4 )
		{
			SorceressLairManager.familiarBattle( 4 );

			if ( KoLmafia.permitsContinue() )
			{
				++n;
			}
			else
			{
				return -1;
			}
		}

		KoLmafia.updateDisplay( "Her Naughtiness awaits." );
		return -1;
	}

	private static final int fightGuardian( final int towerLevel )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
			return -1;
		}

		KoLmafia.updateDisplay( "Fighting guardian on level " + towerLevel + " of the tower..." );

		// Boldly climb the stairs.

		SorceressLairManager.QUEST_HANDLER.constructURLString( towerLevel <= 3 ? "lair4.php" : "lair5.php" );
		SorceressLairManager.QUEST_HANDLER.addFormField( "action", "level" + ( ( towerLevel - 1 ) % 3 + 1 ) );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER );
		if ( !KoLmafia.permitsContinue() ) return -1;

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "You don't have time to mess around in the Tower." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
			return -1;
		}

		// Parse response to see which item we need.
		AdventureResult guardianItem = SorceressLairManager.getGuardianItem();

		// With the guardian item retrieved, check to see if you have
		// the item, and if so, use it and report success.  Otherwise,
		// run away and report failure.

		SorceressLairManager.QUEST_HANDLER.constructURLString( "fight.php" );

		if ( KoLConstants.inventory.contains( guardianItem ) )
		{
			SorceressLairManager.QUEST_HANDLER.addFormField( "action", "useitem" );
			SorceressLairManager.QUEST_HANDLER.addFormField( "whichitem", String.valueOf( guardianItem.getItemId() ) );
			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER );

			return -1;
		}

		// Since we don't have the item, run away

		SorceressLairManager.QUEST_HANDLER.addFormField( "action", "runaway" );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER );

		if ( InventoryManager.retrieveItem( guardianItem ) )
		{
			return SorceressLairManager.fightGuardian( towerLevel );
		}

		return guardianItem.getItemId();
	}

	private static final AdventureResult getGuardianItem()
	{
		String[] guardian = SorceressLairManager.findGuardianByName( FightRequest.getCurrentKey() );
		if ( guardian != null )
		{
			return new AdventureResult( SorceressLairManager.guardianItem( guardian ), 1, false );
		}

		// Shouldn't get here.

		KoLmafia.updateDisplay( MafiaState.ERROR, "Server-side change detected.  Script aborted." );
		return ItemPool.get( ItemPool.STEAMING_EVIL, 1 );
	}

	private static final void ensureUpdatedDoorCode()
	{
		int lastAscension = Preferences.getInteger( "lastDoorCodeReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastDoorCodeReset", KoLCharacter.getAscensions() );
			Preferences.setString( "doorCode", "" );
		}
	}

	private static final String getDoorCode()
	{
		SorceressLairManager.ensureUpdatedDoorCode();
		String code = Preferences.getString( "doorCode" );
		if ( !code.equals( "" ) )
		{
			return code;
		}

		GenericRequest request = SorceressLairManager.QUEST_HANDLER;
		RequestThread.postRequest( request.constructURLString( "lair6.php?&preaction=lightdoor" ) );
		return SorceressLairManager.setDoorCode( request.responseText );
	}

	private static final String setDoorCode( final String responseText )
	{
		SorceressLairManager.ensureUpdatedDoorCode();
		String code = SorceressLairManager.deduceCode( responseText );
		if ( code != null )
		{
			Preferences.setString( "doorCode", code );
		}
		return code;
	}

	private static final void findDoorCode()
	{
		KoLmafia.updateDisplay( "Cracking door code..." );

		// Enter the chamber
		GenericRequest request = SorceressLairManager.QUEST_HANDLER;
		RequestThread.postRequest( request.constructURLString( "lair6.php?place=0" ) );

		// Talk to the guards and crack the code
		String code = SorceressLairManager.getDoorCode();
		if ( code == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Couldn't solve door code. Do it yourself and come back!" );
			return;
		}

		// Enter the code and check for success
		RequestThread.postRequest( request.constructURLString( "lair6.php?action=doorcode&code=" + code ) );
		if ( request.responseText.indexOf( "the door slides open" ) == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I used the wrong code. Sorry." );
		}
	}

	private static final String deduceCode( final String text )
	{
		int start = text.indexOf( "<p>The guard playing South" );
		if ( start == -1 )
		{
			return null;
		}

		int end = text.indexOf( "<p>You roll your eyes." );
		if ( end == -1 )
		{
			return null;
		}

		// Pretty up the data
		String dialog = text.substring( start + 3, end ).replaceAll( "&quot;", "\"" );

		// Make an array of lines
		String lines[] = dialog.split( " *<p>" );
		if ( lines.length != 16 )
		{
			return null;
		}

		// Initialize the three digits of the code
		String digit1 = "0", digit2 = "0", digit3 = "0";
		Matcher matcher;

		// Check for variant, per Visual WIKI
		if ( lines[ 7 ].indexOf( "You're full of it" ) != -1 )
		{
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 5 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit1 = matcher.group( 1 );
			matcher = Pattern.compile( "it's (\\d)" ).matcher( lines[ 11 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit2 = matcher.group( 1 );
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 12 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit3 = matcher.group( 1 );
		}
		else
		{
			if ( lines[ 13 ].indexOf( "South" ) != -1 )
			{
				matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 5 ] );
			}
			else
			{
				matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[ 6 ] );
			}
			if ( !matcher.find() )
			{
				return null;
			}
			digit1 = matcher.group( 1 );
			matcher = Pattern.compile( "that's (\\d)" ).matcher( lines[ 8 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit2 = matcher.group( 1 );
			matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[ 13 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit3 = matcher.group( 1 );
		}

		return digit1 + digit2 + digit3;
	}

	private static final void reflectEnergyBolt()
	{
		boolean inFistcore = KoLCharacter.inFistcore();
		boolean inAxecore = KoLCharacter.inAxecore();
		boolean needWeapon = !inFistcore && !inAxecore;

		if ( needWeapon )
		{
			// Get current equipment
			SpecialOutfit.createImplicitCheckpoint();

			// Equip the huge mirror shard
			RequestThread.postRequest( new EquipmentRequest( SorceressLairManager.MIRROR_SHARD, EquipmentManager.WEAPON ) );
		}

		// Reflect the energy bolt
		KoLmafia.updateDisplay( "Reflecting energy bolt..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair6.php?place=1" ) );

		if ( needWeapon )
		{
			// If we unequipped anything, equip it again
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	private static final void fightShadow()
	{
		RecoveryManager.recoverHP( KoLCharacter.getMaximumHP() );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		int itemCount = 0;

		for ( int i = 0; i < SorceressLairManager.HEALING_ITEMS.length; ++i )
		{
			itemCount += SorceressLairManager.HEALING_ITEMS[ i ].getCount( KoLConstants.inventory );
		}

		if ( itemCount < 6 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient healing items to continue." );
			return;
		}

		KoLmafia.updateDisplay( "Fighting your shadow..." );

		// Start the battle!

		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair6.php?place=2" ) );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "You don't have time to mess around up here." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're out of adventures." );
			return;
		}

		int itemIndex = 0;

		do
		{
			SorceressLairManager.QUEST_HANDLER.constructURLString( "fight.php" );

			while ( !KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) )
			{
				++itemIndex;
			}

			SorceressLairManager.QUEST_HANDLER.addFormField( "action", "useitem" );
			SorceressLairManager.QUEST_HANDLER.addFormField( "whichitem", String.valueOf( SorceressLairManager.HEALING_ITEMS[ itemIndex ].getItemId() ) );

			if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				boolean needsIncrement =
					!KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) || SorceressLairManager.HEALING_ITEMS[ itemIndex ].getCount( KoLConstants.inventory ) < 2;

				if ( needsIncrement )
				{
					++itemIndex;
					while ( !KoLConstants.inventory.contains( SorceressLairManager.HEALING_ITEMS[ itemIndex ] ) )
					{
						++itemIndex;
					}
				}

				SorceressLairManager.QUEST_HANDLER.addFormField(
					"whichitem2", String.valueOf( SorceressLairManager.HEALING_ITEMS[ itemIndex ].getItemId() ) );
			}

			RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER );
		}
		while ( Preferences.getBoolean( "serverAddsCustomCombat" )
				? SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "(show old combat form)" ) != -1
				: SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "fight.php" ) != -1 );

		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "a veritable volcano of fey force" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your shadow has been defeated." );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to defeat your shadow." );
		}
	}

	public static final void makeGuardianItems()
	{
		if ( Preferences.getInteger( "lastTowerClimb" ) == KoLCharacter.getAscensions() )
		{
			return;
		}

		if ( KoLCharacter.inBeecore() )
		{
			AdventureResult orchids = ItemPool.get( ItemPool.TROPICAL_ORCHID, 6 );

			// This will create 6 orchids out of orchid seeds, if necessary
			if ( InventoryManager.hasItem( orchids, true ) )
			{
				InventoryManager.retrieveItem( orchids );
			}

			Preferences.setInteger( "lastTowerClimb", KoLCharacter.getAscensions() );
			return;
		}

		// Assume we will start getting tower items from the beginning
		// of the list
		int startGetting = 0;

		// Find out how good our telescope is.
		int upgrades = KoLCharacter.inBadMoon() ? 0 : KoLCharacter.getTelescopeUpgrades();
		if ( upgrades >= 6 )
		{
			// We have either a complete telescope or we are only
			// missing the last upgrade.
			startGetting = SorceressLairManager.GUARDIAN_DATA.length - ( upgrades == 6 ? 3 : 0 );

			// Make sure we've looked through our telescope since
			// we last ascended.
			KoLCharacter.checkTelescope();

			// Look at the guardians for floors 1 through 6 in turn.
			// They will be stored in telescope2 through telescope7
			for ( int i = 1; i < upgrades; ++i )
			{
				String prop = Preferences.getString( "telescope" + ( i + 1 ) );
				String[] desc = SorceressLairManager.findGuardianByDescription( prop );
				if ( desc == null )

				{
					// We couldn't identify the guardian.
					KoLmafia.updateDisplay( "Tower Guardian #" + i + ": \"" + prop + "\" unrecognized." );

					// Defensively acquire every known
					// tower item that we can.  This will
					// probably mean that we get tower
					// items that we don't need, but that
					// is better than going into a fight
					// without an item that could be easily
					// created.
					startGetting = 0;
					break;
				}

				// We recognize the guardian.
				SorceressLairManager.acquireGuardianItem( desc );
			}
		}

		// Get everything that we can't be sure about needing or not -
		// from the start if we don't have enough of a telescope to
		// rule out any possibilities, 3 from the end if we only have 6
		// upgrades and can't tell which Shore item is required,
		// nothing if we have all 7.
		for ( int i = startGetting; i < SorceressLairManager.GUARDIAN_DATA.length; ++i )
		{
			String [] desc = SorceressLairManager.GUARDIAN_DATA[ i ];
			SorceressLairManager.acquireGuardianItem( desc );
		}

		Preferences.setInteger( "lastTowerClimb", KoLCharacter.getAscensions() );
	}

	private static final void acquireGuardianItem( final String[] desc )
	{
		String name = SorceressLairManager.guardianItem( desc );
		AdventureResult item = ItemPool.get( name, 1 );
		if ( KoLConstants.inventory.contains( item ) )
		{
			return;
		}

		if ( SorceressLairManager.isItemAvailable( item ) || NPCStoreDatabase.contains( name ) )
		{
			InventoryManager.retrieveItem( item );
		}
	}

	private static final void familiarBattle( final int n )
	{
		// If you are an Avatar of Boris or a Zombie Master, you don't need - and can't use - familiars
		if ( KoLCharacter.inAxecore() || KoLCharacter.inZombiecore() )
		{
			SorceressLairManager.familiarBattle( n, false );
			return;
		}

		// Abort if you cannot heal to greater than 50 HP

		RecoveryManager.recoverHP( 51 );
		if ( KoLCharacter.getCurrentHP() <= 50 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You must have more than 50 HP to proceed." );
			return;
		}

		// Since we are facing this familiar for the first time, we
		// don't know what it is. Pick a random tower familiar.

		String current = KoLCharacter.getFamiliar().getRace();

		for ( int i = 0; i < SorceressLairManager.FAMILIAR_DATA.length; ++i )
		{
			String race = SorceressLairManager.FAMILIAR_DATA[ i ][ 1 ];

			// Pick a new familiar, if possible; if we've just
			// passed familiar #1, our current familiar is
			// guaranteed to be the wrong one for #2.
			if ( race.equals( current ) )
			{
				continue;
			}

			FamiliarData familiar = KoLCharacter.findFamiliar( race );
			if ( familiar != null )
			{
				RequestThread.postRequest( new FamiliarRequest( familiar ) );
				break;
			}
		}

		// Go visit the chamber with the currently selected familiar
		SorceressLairManager.familiarBattle( n, true );
	}

	private static final void familiarBattle( final int n, final boolean init )
	{
		// Take the current familiar in to face the Sorceress's pet
		KoLmafia.updateDisplay( "Facing giant familiar..." );
		RequestThread.postRequest( SorceressLairManager.QUEST_HANDLER.constructURLString( "lair6.php?place=" + n ) );

		// If you do not successfully pass the familiar, you will get a
		// "stomp off in a huff" message.
		if ( SorceressLairManager.QUEST_HANDLER.responseText.indexOf( "stomp off in a huff" ) == -1 )
		{
			return;
		}

		// If we failed, either we had the wrong familiar or it wasn't
		// heavy enough. Find the necessary familiar and see if the
		// player has one.

		String text = SorceressLairManager.QUEST_HANDLER.responseText;
		FamiliarData familiar = null;
		String race = null;

		for ( int i = 0; i < SorceressLairManager.FAMILIAR_DATA.length; ++i )
		{
			String [] data = SorceressLairManager.FAMILIAR_DATA[ i ];
			if ( text.indexOf( data[ 0 ] ) != -1 )
			{
				race = data[ 1 ];
				familiar = KoLCharacter.findFamiliar( race );
				break;
			}
		}

		// If we can't identify the Sorceress's familiar, give up
		if ( race == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Server side change: cannot identify Sorceress's familiar" );
			return;
		}

		// If not, tell the player to get one and come back.
		if ( familiar == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Come back with a 20 pound " + race );
			return;
		}

		// Switch to the required familiar
		if ( KoLCharacter.getFamiliar() != familiar )
		{
			RequestThread.postRequest( new FamiliarRequest( familiar ) );
		}

		// If we can buff it to above 20 pounds, try again.
		if ( familiar.getModifiedWeight() < 20 )
		{
			Maximizer.maximize( "familiar weight -tie", 0, 0, false );

			if ( familiar.getModifiedWeight() < 20 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Come back with a 20 pound " + race );
				return;
			}
		}

		// We're good to go. Fight!
		SorceressLairManager.familiarBattle( n, false );
	}

	/*
	 * Methods to inspect externally visited lair pages
	 */

	public static final void parseEntrywayResponse( final String urlString, final String responseText )
	{
		// lair2.php?preaction=key&whichkey=xxx
		if ( urlString.indexOf( "preaction=key" ) != -1 )
		{
			// Unexplained Jamaican Man says: "Don't get greedy,
			// mon. One balloon should be enough for anybody."
			if ( responseText.indexOf( "easter egg balloon" ) != -1 ||
			     responseText.indexOf( "One balloon should be enough" ) != -1 )
			{
				Preferences.setInteger( "lastEasterEggBalloon", KoLCharacter.getAscensions() );
			}
			SorceressLairManager.setDoorCode( responseText );
			return;
		}
	}

	public static final void parseChamberResponse( final String urlString, final String responseText )
	{
		// lair6.php
		if ( urlString.indexOf( "preaction=lightdoor" ) != -1 )
		{
			SorceressLairManager.setDoorCode( responseText );
			return;
		}
	}

	/*
	 * Methods to decorate lair pages for the Relay Browser
	 */

	public static final void decorateGates( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		Matcher gateMatcher = SorceressLairManager.GATE_PATTERN.matcher( buffer );
		SorceressLairManager.decorateGate( buffer, gateMatcher );
		SorceressLairManager.decorateGate( buffer, gateMatcher );
		SorceressLairManager.decorateGate( buffer, gateMatcher );
	}

	public static final void decorateGate( final StringBuffer buffer, final Matcher gateMatcher )
	{
		if ( !gateMatcher.find() )
		{
			return;
		}

		String gateName = gateMatcher.group( 1 );
		if ( gateName == null )
		{
			return;
		}

		// Find the gate in our data

		String[] gateData = SorceressLairManager.findGateByName( gateName );

		if ( gateData == null )
		{
			return;
		}

		// See if we have the needed effect already
		AdventureResult effect = new AdventureResult( SorceressLairManager.gateEffect( gateData ), 1, true );
		boolean effectActive = KoLConstants.activeEffects.contains( effect );

		// Pick an item that grants the effect
		AdventureResult[] items = new AdventureResult[ gateData.length - 3 ];
		for ( int i = 3; i < gateData.length; ++i )
		{
			String name = gateData[ i ];
			AdventureResult item = AdventureResult.pseudoItem( name );
			items[ i - 3 ] = item;
		}

		AdventureResult item = SorceressLairManager.pickOne( items );
		if ( item == null )
		{
			return;
		}

		String spoiler = "";
		if ( effectActive )
		{
			spoiler = "<br>(" + effect + " - ACTIVE)";
		}
		else if ( KoLConstants.inventory.contains( item ) )
		{
			UseLink link = new UseLink( item.getItemId(), "use", "inv_use.php?which=3&whichitem=" );
			spoiler = "<br>(" + effect + " - " + item + " " + link.getItemHTML() + " )";
		}
		else
		{
			spoiler = "<br>(" + effect + " - " + item + " NONE IN INVENTORY)";
		}

		String orig = gateMatcher.group(0);
		int index = buffer.indexOf( orig ) + orig.length();
		buffer.insert( index, spoiler );
	}

	public static final void decorateDigitalKey( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		SorceressLairManager.decorateDigitalKey( buffer, "seq1", "up" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq2", "up" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq3", "down" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq4", "down" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq5", "left" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq6", "right" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq7", "left" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq8", "right" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq9", "b" );
		SorceressLairManager.decorateDigitalKey( buffer, "seq10", "a" );
	}

	private static final void decorateDigitalKey( final StringBuffer buffer, final String control, final String option )
	{
		int index = buffer.indexOf( control );
		if ( index == -1 )
		{
			return;
		}
		String search = "option value=\"" + option + "\"";
		index = buffer.indexOf( search, index );
		if ( index == -1 )
		{
			return;
		}
		buffer.insert( index + search.length(), " selected" );
	}

	public static final void decorateHeavyDoor( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		String code = SorceressLairManager.getDoorCode();
		if ( code != null )
		{
			int index = buffer.indexOf( "name=code" );
			if ( index != -1 )
			{
				buffer.insert( index+9, " value=\"" + code + "\"" );
			}
		}
	}

	public static final void decorateFamiliars( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}
		StringUtilities.insertAfter( buffer, "manages to defeat you.", " <font color=#DD00FF>Angry Goat needed</font>" );
		StringUtilities.insertAfter( buffer, "eyeing you menacingly.", " <font color=#DD00FF>Barrrnacle needed</font>" );
		StringUtilities.insertAfter( buffer, "its... er... grin.", " <font color=#DD00FF>Levitating Potato needed</font>" );
		StringUtilities.insertAfter( buffer, "snip-snap your neck.", " <font color=#DD00FF>Mosquito needed</font>" );
		StringUtilities.insertAfter( buffer, "proboscis at the ready.", " <font color=#DD00FF>Sabre-Toothed Lime needed</font>" );
	}

	public static void handleQuestChange( String location, String responseText )
	{
		// lair.php and lair1-6.php all can check for the same things.
		// Work backwards from the end to see what zones are unlocked.
		if ( responseText.indexOf( "ascend.php" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, QuestDatabase.FINISHED );
		}
		else if ( responseText.indexOf( "#Map4" ) != -1 || responseText.indexOf( "towerup2.gif" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step5" );
		}
		else if ( responseText.indexOf( "#Map3" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step4" );
		}
		else if ( responseText.indexOf( "#Map2" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step4" );
		}
		// Cave done - third step
		else if ( responseText.indexOf( "#Map" ) != -1 || responseText.indexOf( "cave22done" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step3" );
		}
		// Huge mirror broken - second step
		else if ( responseText.indexOf( "cave1mirrordone" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step2" );
		}
		// Passed the three gates - first step
		else if ( responseText.indexOf( "cave1mirror.gif" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.FINAL, "step1" );
		}
	}
}
