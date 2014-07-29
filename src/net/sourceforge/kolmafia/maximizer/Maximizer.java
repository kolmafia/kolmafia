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

package net.sourceforge.kolmafia.maximizer;

import java.util.Collections;
import java.util.Iterator;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.Type69Request;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class Maximizer
{
	private static boolean firstTime = true;

	public static final LockableListModel boosts = new LockableListModel();
	public static Evaluator eval;

	public static String [] maximizationCategories =
	{
		"_hoboPower",
		"_brimstone",
		"_cloathing",
		"_slimeHate",
		"_stickers",
		"_folderholder",
		"_cardsleeve",
		"_smithsness",
	};

	static MaximizerSpeculation best;
	static int bestChecked;
	static long bestUpdate;

	public static boolean maximize( String maximizerString, int maxPrice, int priceLevel, boolean isSpeculationOnly )
	{
		MaximizerFrame.expressionSelect.setSelectedItem( maximizerString );
		int equipLevel = isSpeculationOnly ? 1 : -1;

		// iECOC has to be turned off before actually maximizing as
		// it would cause all item lookups during the process to just
		// print the item name and return null.

		KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

		Maximizer.maximize( equipLevel, maxPrice, priceLevel, false );

		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		Modifiers mods = Maximizer.best.calculate();
		Modifiers.overrideModifier( "_spec", mods );

		return !Maximizer.best.failed;
	}

	public static void maximize( int equipLevel, int maxPrice, int priceLevel, boolean includeAll )
	{
		KoLmafia.forceContinue();
		String maxMe = (String) MaximizerFrame.expressionSelect.getSelectedItem();
		KoLConstants.maximizerMList.addItem( maxMe );
		Maximizer.eval = new Evaluator( maxMe );

		// parsing error
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		double current = Maximizer.eval.getScore( KoLCharacter.getCurrentModifiers() );

		if ( maxPrice <= 0 )
		{
			maxPrice = Math.min( Preferences.getInteger( "autoBuyPriceLimit" ),
					     KoLCharacter.getAvailableMeat() );
		}

		KoLmafia.updateDisplay( Maximizer.firstTime ?
			"Maximizing (1st time may take a while)..." : "Maximizing..." );
		Maximizer.firstTime = false;

		Maximizer.boosts.clear();
		if ( equipLevel != 0 )
		{
			if ( equipLevel > 1 )
			{
				Maximizer.boosts.add( new Boost( "", "(folding equipment is not considered yet)", -1, null, 0.0 ) );
			}
			Maximizer.best = new MaximizerSpeculation();
			Maximizer.best.getScore();
			// In case the current outfit scores better than any tried combination,
			// due to some newly-added constraint (such as +melee):
			Maximizer.best.failed = true;
			Maximizer.bestChecked = 0;
			Maximizer.bestUpdate = System.currentTimeMillis() + 5000;
			try
			{
				Maximizer.eval.enumerateEquipment( equipLevel, maxPrice, priceLevel );
			}
			catch ( MaximizerExceededException e )
			{
				Maximizer.boosts.add( new Boost( "", "(maximum achieved, no further combinations checked)", -1, null, 0.0 ) );
			}
			catch ( MaximizerLimitException e )
			{
				Maximizer.boosts.add( new Boost( "", "<font color=red>(hit combination limit, optimality not guaranteed)</font>", -1, null, 0.0 ) );
			}
			catch ( MaximizerInterruptedException e )
			{
				KoLmafia.forceContinue();
				Maximizer.boosts.add( new Boost( "", "<font color=red>(interrupted, optimality not guaranteed)</font>", -1, null, 0.0 ) );
			}
			MaximizerSpeculation.showProgress();

			boolean[] alreadyDone = new boolean[ EquipmentManager.ALL_SLOTS ];

			for ( int slot = EquipmentManager.ACCESSORY1; slot <= EquipmentManager.ACCESSORY3; ++slot )
			{
				if ( Maximizer.best.equipment[ slot ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE &&
					EquipmentManager.getEquipment( slot ).getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE )
				{
					equipLevel = Maximizer.emitSlot( slot, equipLevel, priceLevel, current );
					alreadyDone[ slot ] = true;
				}
			}

			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{
				if ( !alreadyDone[ slot ] )
				{
					equipLevel = Maximizer.emitSlot( slot, equipLevel, priceLevel, current );
				}
			}
		}

		current = Maximizer.eval.getScore(
			KoLCharacter.getCurrentModifiers() );

		Iterator<String> i = Modifiers.getAllModifiers();
		while ( i.hasNext() )
		{
			String name = i.next();
			if ( !EffectDatabase.contains( name ) )
			{
				continue;
			}

			double delta;
			boolean isSpecial = false;
			MaximizerSpeculation spec = new MaximizerSpeculation();
			AdventureResult effect = new AdventureResult( name, 1, true );
			name = effect.getName();
			boolean hasEffect = KoLConstants.activeEffects.contains( effect );
			Iterator<String> sources;
			String cmd, text;
			int price = 0;
			int advCost = 0;
			int mpCost = 0;
			int soulsauceCost = 0;
			int duration = 0;
			int usesRemaining = 0;
			int itemsRemaining = 0;
			if ( !hasEffect )
			{
				spec.addEffect( effect );
				delta = spec.getScore() - current;
				if ( (spec.getModifiers().getRawBitmap( Modifiers.MUTEX_VIOLATIONS )
					& ~KoLCharacter.currentRawBitmapModifier( Modifiers.MUTEX_VIOLATIONS )) != 0 )
				{	// This effect creates a mutex problem that the player
					// didn't already have.  In the future, perhaps suggest
					// uneffecting the conflicting effect, but for now just skip.
					continue;
				}
				switch ( Maximizer.eval.checkConstraints(
					Modifiers.getModifiers( name ) ) )
				{
				case -1:
					continue;
				case 0:
					if ( delta <= 0.0 ) continue;
					break;
				case 1:
					isSpecial = true;
				}
				if ( Maximizer.eval.checkEffectConstraints( name ) )
				{
					continue;
				}
				sources = EffectDatabase.getAllActions( name );
				cmd = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !sources.hasNext() )
				{
					if ( includeAll )
					{
						sources = Collections.singletonList(
							"(no known source of " + name + ")" ).iterator();
					}
					else continue;
				}
			}
			else
			{
				spec.removeEffect( effect );
				delta = spec.getScore() - current;
				switch ( Maximizer.eval.checkConstraints(
					Modifiers.getModifiers( name ) ) )
				{
				case 1:
					continue;
				case 0:
					if ( delta <= 0.0 ) continue;
					break;
				case -1:
					isSpecial = true;
				}
				cmd = MoodManager.getDefaultAction( "gain_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						cmd = "(find some way to remove " + name + ")";
					}
					else continue;
				}
				sources = Collections.singletonList( cmd ).iterator();
			}

			boolean haveVipKey = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean orFlag = false;
			while ( sources.hasNext() )
			{
				cmd = text = sources.next();
				AdventureResult item = null;

				if ( cmd.startsWith( "#" ) )	// usage note, no command
				{
					if ( includeAll )
					{
						if ( cmd.indexOf( "BM" ) != -1 &&
							!KoLCharacter.inBadMoon() )
						{
							continue;	// no use displaying this in non-BM
						}
						text = (orFlag ? "(...or get " : "(get ")
							+ name + " via " + cmd.substring( 1 ) + ")";
						orFlag = false;
						cmd = "";
					}
					else continue;
				}

				if ( hasEffect &&
					cmd.toLowerCase().indexOf( name.toLowerCase() ) == -1 )
				{
					text = text + " (to remove " + name + ")";
				}

				if ( cmd.startsWith( "(" ) )	// preformatted note
				{
					cmd = "";
					orFlag = false;
				}
				else if ( cmd.startsWith( "use " ) || cmd.startsWith( "chew " ) ||
					cmd.startsWith( "drink " ) || cmd.startsWith( "eat " ) )
				{
					// Hardcoded exception for "Trivia Master", which has a non-standard use command.
					if ( cmd.contains( "use 1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?" ) && !MoodManager.canMasterTrivia() )
					{
						continue;
					}
					// Can get Box of Sunshine in hardcore/ronin, but can't use it
					else if ( !KoLCharacter.canInteract() && cmd.startsWith( "use 1 box of sunshine" ) )
					{
						continue;
					}
					else
					{
						String iName = cmd.substring( cmd.indexOf( " " ) + 3 ).trim();
						item = ItemFinder.getFirstMatchingItem( iName, false );
						Modifiers effMod = Modifiers.getModifiers( iName );
						if ( effMod != null )
						{
							duration = (int) effMod.get( Modifiers.EFFECT_DURATION );
						}
						// Hot Dogs don't have items
						if ( item == null && ClanLoungeRequest.isHotDog( iName ) )
						{
							if ( KoLCharacter.inBadMoon() )
							{
								continue;
							}
							else if ( !Type69Request.isAllowed( "Clan Item", "Hot Dog Stand" ) )
							{
								continue;
							}
							// Jarlsberg and Zombie characters can't eat hot dogs
							else if ( KoLCharacter.isJarlsberg() || KoLCharacter.isZombieMaster() )
							{
								continue;
							}
							else if ( !haveVipKey )
							{
								if ( includeAll )
								{
									text = "( get access to the VIP lounge )";
									cmd = "";
								}
								else continue;
							}
							// Fullness available?
							int full = ClanLoungeRequest.hotdogNameToFullness( iName );
							if ( full > 0 &&
								KoLCharacter.getFullness() + full > KoLCharacter.getFullnessLimit() )
							{
								continue;
							}
							// Is it Fancy and has one been used?
							if ( ClanLoungeRequest.isFancyHotDog( iName ) &&
								Preferences.getBoolean( "_fancyHotDogEaten" ) )
							{
								continue;
							}
							else
							{
								usesRemaining = 1;
							}
						}
						// Speakeasy drinks don't have items
						else if ( item == null && ClanLoungeRequest.isSpeakeasyDrink( iName ) )
						{
							if ( KoLCharacter.inBadMoon() )
							{
								continue;
							}
							else if ( !Type69Request.isAllowed( "Clan Item", "Speakeasy" ) )
							{
								continue;
							}
							// Jarlsberg and Highschool characters can't drink at the speakeasy
							else if ( KoLCharacter.isJarlsberg() || KoLCharacter.inHighschool() )
							{
								continue;
							}
							else if ( !haveVipKey )
							{
								if ( includeAll )
								{
									text = "( get access to the VIP lounge )";
									cmd = "";
								}
								else continue;
							}
							// Inebriety available? We won't suggest things that'll cause overdrinking
							int inebriety = ClanLoungeRequest.speakeasyNameToInebriety( iName );
							if ( inebriety > 0 &&
								KoLCharacter.getInebriety() + inebriety > KoLCharacter.getInebrietyLimit() )
							{
								continue;
							}
							// Have we had all three?
							usesRemaining = 3 - Preferences.getInteger( "_speakeasyDrinksDrunk" );
							if ( usesRemaining <= 0 )
							{
								continue;
							}
						}
						else if ( item == null && !cmd.contains( "," ) )
						{
							if ( includeAll )
							{
								text = "(identify & " + cmd + ")";
								cmd = "";
							}
							else continue;
						}
						else if ( item != null )
						{
							int itemId = item.getItemId();
							usesRemaining = UseItemRequest.maximumUses( itemId );
							if ( usesRemaining <= 0 )
							{
								continue;
							}
						}
					}
				}
				else if ( cmd.startsWith( "gong " ) )
				{
					item = ItemPool.get( ItemPool.GONG, 1 );
					advCost = 3;
					duration = 20;
				}
				else if ( cmd.startsWith( "cast " ) )
				{
					String skillName = UneffectRequest.effectToSkill( name );
					int skillId = SkillDatabase.getSkillId( skillName );
					mpCost = SkillDatabase.getMPConsumptionById( skillId );
					advCost = SkillDatabase.getAdventureCost( skillId );
					soulsauceCost = SkillDatabase.getSoulsauceCost( skillId );
					duration = SkillDatabase.getEffectDuration( skillId );
					UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( skillName );
					if ( skill != null )
					{
						usesRemaining = skill.getMaximumCast();
					}
					if ( !KoLCharacter.hasSkill( skillName ) || usesRemaining == 0 )
					{
						if ( includeAll )
						{
							boolean isBuff = SkillDatabase.isBuff( skillId );
							text = "(learn to " + cmd + (isBuff ? ", or get it from a buffbot)" : ")");
							cmd = "";
						}
						else continue;
					}
				}
				else if ( cmd.startsWith( "friars " ) )
				{
					int lfc = Preferences.getInteger( "lastFriarCeremonyAscension" );
					int ka = Preferences.getInteger( "knownAscensions" );
					if ( lfc < ka )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "friarsBlessingReceived" ) )
					{
						cmd = "";
					}
					duration = 20;
					usesRemaining = Preferences.getBoolean( "friarsBlessingReceived" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "hatter " ) )
				{
					boolean haveEffect = KoLConstants.activeEffects.contains( EffectPool
						.get( Effect.DOWN_THE_RABBIT_HOLE ) );
					boolean havePotion = InventoryManager.hasItem( ItemPool.DRINK_ME_POTION );
					if ( !havePotion && !haveEffect )
					{
						continue;
					}
					else if ( !RabbitHoleManager.hatLengthAvailable( StringUtilities.parseInt( cmd
						.substring( 7 ) ) ) )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_madTeaParty" ) )
					{
						cmd = "";
					}
					duration = 30;
					usesRemaining = Preferences.getBoolean( "_madTeaParty" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "summon " ) )
				{
					if ( !Preferences.getString( Quest.MANOR.getPref() ).equals( QuestDatabase.FINISHED ) )
					{
						continue;
					}
					int onHand = InventoryManager.getAccessibleCount( ItemPool.EVIL_SCROLL );
					int creatable = CreateItemRequest.getInstance( ItemPool.EVIL_SCROLL )
						.getQuantityPossible();

					if ( !KoLCharacter.canInteract() && ( onHand + creatable ) < 1 )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "demonSummoned" ) )
					{
						cmd = "";
					}
					else
					{
						try
						{
							int num = Integer.parseInt( cmd.split( " " )[ 1 ] );
							if ( Preferences.getString( "demonName" + num ).equals( "" ) )
							{
								cmd = "";
							}
						}
						catch ( Exception e )
						{
						}
					}
					// Existential Torment is 20 turns, but won't appear here as the effects are unknown
					duration = 30;
					usesRemaining = Preferences.getBoolean( "demonSummoned" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "concert " ) )
				{
					String side = Preferences.getString( "sidequestArenaCompleted" );
					boolean available = false;

					if ( side.equals( "none" ) )
					{
						continue;
					}
					else if ( side.equals( "fratboy" ) )
					{
						available = cmd.indexOf( "Elvish" ) != -1 ||
						            cmd.indexOf( "Winklered" ) != -1 ||
						            cmd.indexOf( "White-boy Angst" ) != -1;
					}
					else if ( side.equals( "hippy" ) )
					{
						available = cmd.indexOf( "Moon" ) != -1 ||
						            cmd.indexOf( "Dilated" ) != -1 ||
						            cmd.indexOf( "Optimist" ) != -1;
					}

					if ( !available )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "concertVisited" ) )
					{
						cmd = "";
					}
					duration = 20;
					usesRemaining = Preferences.getBoolean( "concertVisited" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "telescope " ) )
				{
					if ( Preferences.getInteger( "telescopeUpgrades" ) == 0 )
					{
						if ( includeAll )
						{
							text = "( get a telescope )";
							cmd = "";
						}
						else continue;
					}
					else if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "telescopeLookedHigh" ) )
					{
						cmd = "";
					}
					duration = 10;
					usesRemaining = Preferences.getBoolean( "telescopeLookedHigh" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "ballpit" ) )
				{
					if ( !KoLCharacter.canInteract() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_ballpit" ) )
					{
						cmd = "";
					}
					duration = 20;
					usesRemaining = Preferences.getBoolean( "_ballpit" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "jukebox" ) )
				{
					if ( !KoLCharacter.canInteract() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_jukebox" ) )
					{
						cmd = "";
					}
					duration = 10;
					usesRemaining = Preferences.getBoolean( "_jukebox" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "pool " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( !Type69Request.isAllowed( "Clan Item", "Pool Table" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getInteger( "_poolGames" ) >= 3 )
					{
						cmd = "";
					}
					duration = 10;
					usesRemaining = 3 - Preferences.getInteger( "_poolGames" );
				}
				else if ( cmd.startsWith( "shower " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( !Type69Request.isAllowed( "Clan Item", "April Shower" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getBoolean( "_aprilShower" ) )
					{
						cmd = "";
					}
					duration = 50;
					usesRemaining = Preferences.getBoolean( "_aprilShower" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "swim " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( !Type69Request.isAllowed( "Clan Item", "Swimming Pool" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getBoolean( "_olympicSwimmingPool" ) )
					{
						cmd = "";
					}
					duration = 50;
					usesRemaining = Preferences.getBoolean( "_olympicSwimmingPool" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "styx " ) )
				{
					if ( !KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "styxPixieVisited" ) )
					{
						cmd = "";
					}
					duration = 10;
					usesRemaining = Preferences.getBoolean( "styxPixieVisited" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "skate " ) )
				{
					String status = Preferences.getString( "skateParkStatus" );
					int buff = SkateParkRequest.placeToBuff( cmd.substring( 6 ) );
					Object [] data = SkateParkRequest.buffToData( buff );
					String buffPref = (String) data[4];
					String buffStatus = (String) data[6];

					if ( !status.equals( buffStatus ) )
					{
						continue;
					}
					else if ( Preferences.getBoolean( buffPref ) )
					{
						cmd = "";
					}
					duration = 30;
					usesRemaining = Preferences.getBoolean( buffPref ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "gap" ) )
				{
					AdventureResult pants = EquipmentManager.getEquipment( EquipmentManager.PANTS );
					if ( InventoryManager.getAccessibleCount( ItemPool.GREAT_PANTS ) == 0 )
					{
						if ( includeAll )
						{
							text = "(acquire and equip Greatest American Pants for " + name + ")";
							cmd = "";
						}
						else
						{
							continue;
						}
					}
					else if ( Preferences.getInteger( "_gapBuffs" ) >= 5 )
					{
						cmd = "";
					}
					else if ( pants == null || ( pants.getItemId() != ItemPool.GREAT_PANTS ) )
					{
						text = "(equip Greatest American Pants for " + name + ")";
						cmd = "";
					}
					if ( name.equals( "Super Skill" ) )
					{
						duration = 5;
					}
					else if ( name.equals( "Super Structure" ) || name.equals( "Super Accuracy" ) )
					{
						duration = 10;
					}
					else if ( name.equals( "Super Vision" ) || name.equals( "Super Speed" ) )
					{
						duration = 20;
					}
					usesRemaining = 5 - Preferences.getInteger( "_gapBuffs" );
				}
				else if ( cmd.startsWith( "grim" ) )
				{
					FamiliarData fam = KoLCharacter.findFamiliar( FamiliarPool.GRIM_BROTHER );
					if ( fam == null )
					{
						if ( includeAll )
						{
							text = "(get a Grim Brother familiar for " + name + ")";
							cmd = "";
						}
						else
						{
							continue;
						}
					}
					else if ( Preferences.getBoolean( "_grimBuff" ) )
					{
						cmd = "";
					}
					duration = 30;
					usesRemaining = Preferences.getBoolean( "_grimBuff" ) ? 0 : 1;
				}
				else if ( cmd.startsWith( "skeleton " ) )
				{
					item = ItemPool.get( ItemPool.SKELETON, 1 );
					duration = 30;
				}

				if ( item != null )
				{
					String iname = item.getName();

					if ( KoLCharacter.inBeecore() &&
						KoLCharacter.getBeeosity( iname ) > 0 )
					{
						continue;
					}

					int full = ItemDatabase.getFullness( iname );
					if ( full > 0 &&
						KoLCharacter.getFullness() + full > KoLCharacter.getFullnessLimit() )
					{
						cmd = "";
					}
					full = ItemDatabase.getInebriety( iname );
					if ( full > 0 &&
						KoLCharacter.getInebriety() + full > KoLCharacter.getInebrietyLimit() )
					{
						cmd = "";
					}
					full = ItemDatabase.getSpleenHit( iname );
					if ( full > 0 && cmd.indexOf( "chew" ) == -1 )
					{
						RequestLogger.printLine( "(Note: extender for " +
							name + " is a spleen item that doesn't use 'chew')" );
					}
					if ( full > 0 &&
						KoLCharacter.getSpleenUse() + full > KoLCharacter.getSpleenLimit() )
					{
						cmd = "";
					}
					if ( !ItemDatabase.meetsLevelRequirement( iname ) )
					{
						if ( includeAll )
						{
							text = "level up & " + text;
							cmd = "";
						}
						else continue;
					}

					if ( cmd.length() > 0 )
					{
						Concoction c = ConcoctionPool.get( item );
						price = c.price;
						int count = Math.max( 0, item.getCount() - c.initial );
						if ( count > 0 )
						{
							int create = Math.min( count, c.creatable );
							count -= create;
							if ( create > 0 )
							{
								text = create > 1 ? "make " + create + " & " + text
									: "make & " + text;
							}
							int buy = price > 0 ? Math.min( count, KoLCharacter.getAvailableMeat() / price ) : 0;
							count -= buy;
							if ( buy > 0 )
							{
								text = buy > 1 ? "buy " + buy + " & " + text
									: "buy & " + text;
								cmd = "buy " + buy + " \u00B6" + item.getItemId() +
									";" + cmd;
							}
							if ( count > 0 )
							{
								if ( !KoLCharacter.canInteract() ||
									!ItemDatabase.isTradeable( item.getItemId() ) )
								{
									continue;
								}
								text = count > 1 ? "acquire " + count + " & " + text
									: "acquire & " + text;
							}
						}
						if ( priceLevel == 2 || (priceLevel == 1 && count > 0) )
						{
							if ( price <= 0 && KoLCharacter.canInteract() &&
								ItemDatabase.isTradeable( item.getItemId() ) )
							{
								if ( MallPriceDatabase.getPrice( item.getItemId() )
									> maxPrice * 2 )
								{
									continue;
								}

								price = StoreManager.getMallPrice( item );
							}
						}
						if ( price > maxPrice || price == -1 ) continue;
					}
					else if ( item.getCount( KoLConstants.inventory ) == 0 )
					{
						continue;
					}
					itemsRemaining = item.getCount( KoLConstants.inventory );
				}

				text = text + " (";
				if ( advCost > 0 )
				{
					text += advCost + " adv, ";
				}
				if ( mpCost > 0 )
				{
					text += mpCost + " mp, ";
				}
				if ( soulsauceCost > 0 )
				{
					text += soulsauceCost + " soulsauce, ";
				}
				if ( price > 0 )
				{
					text += KoLConstants.COMMA_FORMAT.format( price ) + " meat, ";
				}
				text += KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
				if ( Preferences.getBoolean( "verboseMaximizer" ) )
				{
					boolean show = duration > 0 ||
									( usesRemaining > 0 && usesRemaining < Integer.MAX_VALUE ) ||
									itemsRemaining > 0;
					int count = 0;
					if ( show )
					{
						text += " [";
					}
					if ( duration > 0 )
					{
						if ( duration == 999 )
						{
							text += "intrinsic";
						}
						else if ( duration == 1 )
						{
							text += "1 adv duration";
						}
						else
						{
							text += duration + " advs duration";
						}
						count++;
					}
					if ( usesRemaining > 0 && usesRemaining < Integer.MAX_VALUE )
					{
						if ( count > 0 )
						{
							text += ", ";
						}
						if ( usesRemaining == 1 )
						{
							text += "1 use remaining";
							count++;
						}
						else
						{
							text += usesRemaining + " uses remaining";
							count++;
						}
					}
					if ( itemsRemaining > 0 )
					{
						if ( count > 0 )
						{
							text += ", ";
						}
						text += itemsRemaining + " in inventory";
						count++;
					}
					if ( show )
					{
						text += "]";
					}
				}
				if ( orFlag )
				{
					text = "...or " + text;
				}
				Maximizer.boosts.add( new Boost( cmd, text, effect, hasEffect,
					item, delta, isSpecial ) );
				orFlag = true;
			}
		}

		if ( Maximizer.boosts.size() == 0 )
		{
			Maximizer.boosts.add( new Boost( "", "(nothing useful found)", 0, null, 0.0 ) );
		}

		Maximizer.boosts.sort();
	}

	private static int emitSlot( int slot, int equipLevel, int priceLevel, double current )
	{
		if ( slot == EquipmentManager.FAMILIAR )
		{	// Insert any familiar switch at this point
			FamiliarData fam = Maximizer.best.getFamiliar();
			if ( !fam.equals( KoLCharacter.getFamiliar() ) )
			{
				MaximizerSpeculation spec = new MaximizerSpeculation();
				spec.setFamiliar( fam );
				double delta = spec.getScore() - current;
				String cmd, text;
				cmd = "familiar " + fam.getRace();
				text = cmd + " (" +
					KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";

				Boost boost = new Boost( cmd, text, fam, delta );
				if ( equipLevel == -1 )
				{	// called from CLI
					boost.execute( true );
					if ( !KoLmafia.permitsContinue() ) equipLevel = 1;
				}
				else
				{
					Maximizer.boosts.add( boost );
				}
			}
		}

		String slotname = EquipmentRequest.slotNames[ slot ];
		AdventureResult item = Maximizer.best.equipment[ slot ];
		FamiliarData enthroned = Maximizer.best.getEnthroned();
		FamiliarData bjorned = Maximizer.best.getBjorned();
		AdventureResult curr = EquipmentManager.getEquipment( slot );
		FamiliarData currEnthroned = KoLCharacter.getEnthroned();
		FamiliarData currBjorned = KoLCharacter.getBjorned();
		if ( curr.equals( item ) &&
			!( item.getItemId() == ItemPool.HATSEAT && enthroned != currEnthroned ) &&
			!( item.getItemId() == ItemPool.BUDDY_BJORN && bjorned != currBjorned ) )
		{
			if ( slot >= EquipmentManager.SLOTS ||
			     curr.equals( EquipmentRequest.UNEQUIP ) ||
			     equipLevel == -1 )
			{
				return equipLevel;
			}
			Maximizer.boosts.add( new Boost( "", "keep " + slotname + ": " + item.getName(), -1, item, 0.0 ) );
			return equipLevel;
		}
		MaximizerSpeculation spec = new MaximizerSpeculation();
		spec.equip( slot, item );
		if ( item.getItemId() == ItemPool.HATSEAT )
		{
			spec.setEnthroned( enthroned );
		}
		if ( item.getItemId() == ItemPool.BUDDY_BJORN )
		{
			spec.setBjorned( bjorned );
		}
		double delta = spec.getScore() - current;
		String cmd, text;
		if ( item == null || item.equals( EquipmentRequest.UNEQUIP ) )
		{
			item = curr;
			cmd = "unequip " + slotname;
			text = cmd + " (" + curr.getName() + ", " +
				KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
		}
		else
		{
			if ( item.getItemId() == ItemPool.HATSEAT && enthroned != currEnthroned )
			{
				cmd = "enthrone " + enthroned.getRace();
			}
			else if ( item.getItemId() == ItemPool.BUDDY_BJORN && bjorned != currBjorned )
			{
				cmd = "bjornify " + bjorned.getRace();
			}
			else
			{
				cmd = "equip " + slotname + " " + item.getName();
			}
			text = cmd + " (";

			CheckedItem checkedItem = (CheckedItem) item;

			int price = 0;

			// The "initial" quantity comes from InventoryManager.getAccessibleCount.
			// It can include inventory, closet, and storage.  However, anything that
			// is included should also be supported by retrieveItem(), so we don't need
			// to take any special action here.  Displaying the method that will be used
			// would still be useful, though.
			if ( curr.equals( item ) )
			{
			}
			else if ( checkedItem.initial != 0 )
			{
				String method = InventoryManager.simRetrieveItem( item.getInstance( 1 ) );
				if ( !method.equals( "have" ) )
				{
					text = method + " & " + text;
				}
			}
			else if ( checkedItem.creatable != 0 )
			{
				text = "make & " + text;
			}
			else if ( checkedItem.npcBuyable != 0 )
			{
				text = "buy & " + text;
				cmd = "buy 1 \u00B6" + item.getItemId() +
						";" + cmd;
				price = ConcoctionPool.get( item ).price;
			}
			else if ( checkedItem.foldable != 0 )
			{
				text = "fold & " + text;
				cmd = "fold \u00B6" + item.getItemId() +
						";" + cmd;
			}
			else if ( checkedItem.pullable != 0 )
			{
				text = "pull & " + text;
				cmd = "pull 1 \u00B6" + item.getItemId() +
						";" + cmd;
			}
			else 	// Mall buyable
			{
				text = "acquire & " + text;
				if ( priceLevel > 0 )
				{
					price = StoreManager.getMallPrice( item );
				}
			}

			if ( price > 0 )
			{
				text = text + KoLConstants.COMMA_FORMAT.format( price ) +
					" meat, ";
			}
			text = text + KoLConstants.MODIFIER_FORMAT.format(
				delta ) + ")";
		}

		Boost boost = new Boost( cmd, text, slot, item, delta, enthroned, bjorned );
		if ( equipLevel == -1 )
		{	// called from CLI
			boost.execute( true );
			if ( !KoLmafia.permitsContinue() )
			{
				equipLevel = 1;
				Maximizer.boosts.add( boost );
			}
		}
		else
		{
			Maximizer.boosts.add( boost );
		}
		return equipLevel;
	}

}
