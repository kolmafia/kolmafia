/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.RequestFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CharPaneRequest
	extends GenericRequest
{
	private static final AdventureResult ABSINTHE = new AdventureResult( "Absinthe-Minded", 1, true );

	private static long lastResponseTimestamp = 0;
	private static String lastResponse = "";

	private static boolean canInteract = false;
	private static int turnsThisRun = 0;

	private static boolean inValhalla = false;
	private static boolean checkNewLocation = false;

	public static boolean compactCharacterPane = false;
	public static boolean familiarBelowEffects = false;

	public CharPaneRequest()
	{
		super( "charpane.php" );
	}

	public static final void reset()
	{
		CharPaneRequest.lastResponseTimestamp = 0;
		CharPaneRequest.lastResponse = "";
		CharPaneRequest.canInteract = false;
		CharPaneRequest.turnsThisRun = 0;
		CharPaneRequest.inValhalla = false;
		CharPaneRequest.checkNewLocation = false;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public String getHashField()
	{
		return null;
	}

	public static final boolean canInteract()
	{
		return CharPaneRequest.canInteract;
	}

	public static final boolean inValhalla()
	{
		return CharPaneRequest.inValhalla;
	}

	public static final void setInteraction()
	{
		CharPaneRequest.setInteraction( CharPaneRequest.checkInteraction() );
	}

	public static final void setInteraction( final boolean interaction )
	{
		CharPaneRequest.canInteract = interaction;
		if ( interaction )
		{
			ConcoctionDatabase.setPullsRemaining( -1 );
		}
	}

	public static final void setCheckNewLocation( final boolean check )
	{
		CharPaneRequest.checkNewLocation = check;
	}

	public static boolean processResults( String responseText )
	{
		return CharPaneRequest.processResults( CharPaneRequest.lastResponseTimestamp, responseText );
	}

	public static boolean processResults( long responseTimestamp, String responseText )
	{
		if ( CharPaneRequest.lastResponseTimestamp > responseTimestamp )
		{
			return false;
		}

		CharPaneRequest.lastResponseTimestamp = responseTimestamp;
		CharPaneRequest.lastResponse = responseText;

		// We can deduce whether we are in compact charpane mode

		CharPaneRequest.compactCharacterPane = responseText.indexOf( "<br>Lvl. " ) != -1;

		// If we are in Valhalla, do special processing
		if ( responseText.indexOf( "otherimages/spirit.gif" ) != -1 ||
		     responseText.indexOf( "<br>Lvl. <img" ) != -1 )
		{
			processValhallaCharacterPane( responseText );
			return true;
		}

		CharPaneRequest.inValhalla = false;

		// KoL now includes Javascript variables in each charpane
		//
		// var turnsplayed = 232576;
		// var turnsthisrun = 232576;
		// var rollover = 1268537400;
		// var rightnow = 1268496181;
		// var pwdhash = "...";
		//
		// "turnsThisRun" is of interest for several reasons: we can
		// use it to order (some) charpane requests, even if the
		// timestamp is the same, and we can use it to synchronize
		// KolMafia with KoL's turn counter

		int turnsThisRun = parseTurnsThisRun( responseText );
		int mafiaTurnsThisRun = KoLCharacter.getCurrentRun();

		if ( turnsThisRun < CharPaneRequest.turnsThisRun ||
		     turnsThisRun < mafiaTurnsThisRun )
		{
			return false;
		}

		CharPaneRequest.turnsThisRun = turnsThisRun;

		// Since we believe this update, synchronize with it
		ResultProcessor.processAdventuresUsed( turnsThisRun - mafiaTurnsThisRun );

		// The easiest way to retrieve the character pane data is to
		// use regular expressions. But, the only data that requires
		// synchronization is the modified stat values, health and
		// mana.

		if ( CharPaneRequest.compactCharacterPane )
		{
			CharPaneRequest.handleCompactMode( responseText );
		}
		else
		{
			CharPaneRequest.handleExpandedMode( responseText );
		}

		CharPaneRequest.checkNewLocation( responseText );
		CharPaneRequest.refreshEffects( responseText );
		CharPaneRequest.setInteraction( CharPaneRequest.checkInteraction() );

		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();

		if ( KoLCharacter.inAxecore() )
		{
			CharPaneRequest.checkClancy( responseText );
		}
		else if ( KoLCharacter.isJarlsberg() )
		{
			CharPaneRequest.checkCompanion( responseText );
		}
		else
		{
			CharPaneRequest.checkFamiliar( responseText );
		}

		// Mana cost adjustment may have changed

		KoLConstants.summoningSkills.sort();
		KoLConstants.usableSkills.sort();
		RequestFrame.refreshStatus();

		return true;
	}

	public static final String getLastResponse()
	{
		return CharPaneRequest.lastResponse;
	}

	// <td align=center><img src="http://images.kingdomofloathing.com/itemimages/karma.gif" width=30 height=30 alt="Karma" title="Karma"><br>0</td>
	public static final Pattern KARMA_PATTERN = Pattern.compile( "karma.gif.*?<br>([^<]*)</td>" );
	// <td align=right>Karma:</td><td align=left><b>122</b></td>
	public static final Pattern KARMA_PATTERN_COMPACT = Pattern.compile( "Karma:.*?<b>([^<]*)</b>" );

	private static final void processValhallaCharacterPane( final String responseText )
	{
		// We are in Valhalla
		CharPaneRequest.inValhalla = true;

		// We have no stats as an Astral Spirit
		KoLCharacter.setStatPoints( 1, 0L, 1, 0L, 1, 0L );
		KoLCharacter.setHP( 1, 1, 1 );
		KoLCharacter.setMP( 1, 1, 1 );
		KoLCharacter.setAvailableMeat( 0 );
		KoLCharacter.setAdventuresLeft( 0 );
		KoLCharacter.setMindControlLevel( 0 );

		// No active status effects
		KoLConstants.recentEffects.clear();
		KoLConstants.activeEffects.clear();

		// No modifiers
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();

		// You certainly can't interact with the "real world"
		CharPaneRequest.setInteraction( false );

		// You do, however, have Karma available to spend in Valhalla.
		Pattern pattern = CharPaneRequest.compactCharacterPane ?
			CharPaneRequest.KARMA_PATTERN_COMPACT :
			CharPaneRequest.KARMA_PATTERN ;
		Matcher matcher = pattern.matcher( responseText );
		int karma = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		Preferences.setInteger( "bankedKarma", karma );
	}

	public static final Pattern TURNS_PATTERN = Pattern.compile( "var turnsthisrun = (\\d*);" );

	private static final int parseTurnsThisRun( final String responseText )
	{
		Matcher matcher = CharPaneRequest.TURNS_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			return StringUtilities.parseInt( matcher.group( 1 ) );
		}

		return -1;
	}

	private static final boolean checkInteraction()
	{
		// If he's freed the king, that's good enough
		if ( KoLCharacter.kingLiberated() )
		{
			return true;
		}

		// If he's in Hardcore, nope
		if ( KoLCharacter.isHardcore() )
		{
			return false;
		}

		// If he's in Bad Moon, nope
		if ( KoLCharacter.inBadMoon() )
		{
			return false;
		}

		// If the charsheet does not say he can't interact or api.php
		// says roninleft =0, ok.
		// (this will be true for any Casual run, for an unascended
		// character, or for a sufficiently lengthy softcore run)
		if ( !KoLCharacter.inRonin() )
		{
			return true;
		}

		// Last time we checked the char sheet or api.php, he was still
		// in ronin. See if he still is.
		if ( KoLCharacter.getCurrentRun() >= 1000 )
		{
			return true;
		}

		// Otherwise, no way.
		return false;
	}

	private static final void handleCompactMode( final String responseText )
	{
		try
		{
			CharPaneRequest.handleStatPoints( responseText, CharPaneRequest.compactStatsPattern );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		try
		{
			CharPaneRequest.handleMiscPoints( responseText, CharPaneRequest.MISC_PATTERNS[ KoLCharacter.inZombiecore() ? 2 : 0 ] );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		try
		{
			CharPaneRequest.handleMindControl( responseText, CharPaneRequest.compactMCPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		try
		{
			CharPaneRequest.handleInebriety( responseText, CharPaneRequest.compactInebrietyPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Do NOT read fullness from the charpane; it is optional, so
		// we have to track it manually, anyway
	}

	private static final void handleExpandedMode( final String responseText )
	{
		try
		{
			CharPaneRequest.handleStatPoints( responseText, CharPaneRequest.expandedStatsPattern );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		try
		{
			CharPaneRequest.handleMiscPoints( responseText, CharPaneRequest.MISC_PATTERNS[ KoLCharacter.inZombiecore() ? 3 : 1 ] );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		try
		{
			CharPaneRequest.handleMindControl( responseText, CharPaneRequest.expandedMCPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		try
		{
			CharPaneRequest.handleInebriety( responseText, CharPaneRequest.expandedInebrietyPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Do NOT read fullness from the charpane; it is optional, so
		// we have to track it manually, anyway
	}

	private static final Pattern makeStatPattern( final String musString, final String mysString, final String moxString )
	{
		return Pattern.compile( musString + ".*?<b>(.*?)</b>.*?" + mysString + ".*?<b>(.*?)</b>.*?" + moxString + ".*?<b>(.*?)</b>" );
	}

	private static Pattern compactStatsPattern =
		CharPaneRequest.makeStatPattern( "Mus", "Mys", "Mox" );
	private static Pattern expandedStatsPattern =
		CharPaneRequest.makeStatPattern( "Muscle", "Mysticality", "Moxie" );

	private static Pattern modifiedPattern =
		Pattern.compile( "<font color=blue>(\\d+)</font>&nbsp;\\((\\d+)\\)" );

	private static final void handleStatPoints( final String responseText, final Pattern pattern )
		throws Exception
	{
		Matcher statMatcher = pattern.matcher( responseText );
		if ( !statMatcher.find() )
		{
			return;
		}

		int[] modified = new int[ 3 ];
		for ( int i = 0; i < 3; ++i )
		{
			Matcher modifiedMatcher = modifiedPattern.matcher( statMatcher.group( i + 1 ) );

			if ( modifiedMatcher.find() )
			{
				modified[ i ] = StringUtilities.parseInt( modifiedMatcher.group( 1 ) );
			}
			else
			{
				modified[ i ] =
					StringUtilities.parseInt( statMatcher.group( i + 1 ).replaceAll( "<[^>]*>", "" ).replaceAll(
									  "[^\\d]+", "" ) );
			}
		}

		KoLCharacter.setStatPoints( modified[ 0 ],
					    KoLCharacter.getTotalMuscle(),
					    modified[ 1 ],
					    KoLCharacter.getTotalMysticality(),
					    modified[ 2 ],
					    KoLCharacter.getTotalMoxie() );
	}

	private static final Pattern [][] MISC_PATTERNS =
	{
		// Compact
		{
			Pattern.compile( "HP.*?<b>(.*?)/(.*?)</b>" ),
			Pattern.compile( "MP.*?<b>(.*?)/(.*?)</b>" ),
			Pattern.compile( "Meat.*?<b>(.*?)</b>" ),
			Pattern.compile( "Adv.*?<b>(.*?)</b>" ),
		},

		// Expanded
		{
			Pattern.compile( "/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>" ),
			Pattern.compile( "/(?:slim)?mp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>" ),
			Pattern.compile( "/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>" ),
			Pattern.compile( "/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>" ),
		},

		// Compact Zombiecore
		{
			Pattern.compile( "HP.*?<b>(.*?)/(.*?)</b>" ),
			Pattern.compile( "Horde: (\\d+)" ),
			Pattern.compile( "Meat.*?<b>(.*?)</b>" ),
			Pattern.compile( "Adv.*?<b>(.*?)</b>" ),
		},

		// Expanded Zombiecore
		{
			Pattern.compile( "/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>" ),
			Pattern.compile( "/(?:slim)?zombies/horde.*?\\.gif.*?Horde: (\\d+)" ),
			Pattern.compile( "/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>" ),
			Pattern.compile( "/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>" ),
		},
	};

	private static final int HP = 0;
	private static final int MP = 1;
	private static final int MEAT = 2;
	private static final int ADV = 3;

	private static final void handleMiscPoints( final String responseText, final Pattern [] patterns )
		throws Exception
	{
		// Health and all that good stuff is complicated, has nested
		// images, and lots of other weird stuff. Handle it in a
		// non-modular fashion.

		Pattern pattern = patterns[ HP ];
		Matcher matcher = pattern == null ? null : pattern.matcher( responseText );
		if ( matcher != null && matcher.find() )
		{
			int currentHP = StringUtilities.parseInt( matcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			int maximumHP = StringUtilities.parseInt( matcher.group( 2 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			KoLCharacter.setHP( currentHP, maximumHP, maximumHP );
		}

		pattern = patterns[ MP ];
		matcher = pattern == null ? null : pattern.matcher( responseText );
		if ( matcher != null && matcher.find() )
		{
			int currentMP = 0;
			int maximumMP = 0;
			if ( KoLCharacter.inZombiecore() )
			{
				String currentHorde = matcher.group( 1 );
				currentMP = maximumMP = StringUtilities.parseInt( currentHorde );
			}
			else
			{
				currentMP = StringUtilities.parseInt( matcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
				maximumMP = StringUtilities.parseInt( matcher.group( 2 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			}
			KoLCharacter.setMP( currentMP, maximumMP, maximumMP );
		}

		pattern = patterns[ MEAT ];
		matcher = pattern == null ? null : pattern.matcher( responseText );
		if ( matcher != null && matcher.find() )
		{
			int availableMeat = StringUtilities.parseInt( matcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			KoLCharacter.setAvailableMeat( availableMeat );
		}

		pattern = patterns[ ADV ];
		matcher = pattern == null ? null : pattern.matcher( responseText );
		if ( matcher != null && matcher.find() )
		{
			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = StringUtilities.parseInt( matcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" ) );
			ResultProcessor.processAdventuresLeft( newAdventures - oldAdventures );
		}
	}

	private static final void handleMindControl( final String text, final Pattern [] patterns )
	{
		for ( int i = 0; i < patterns.length; ++i )
		{
			int level = CharPaneRequest.handleMindControl( text, patterns[i] );
			if ( level > 0 )
			{
				KoLCharacter.setMindControlLevel( level );
				return;
			}
		}

		KoLCharacter.setMindControlLevel( 0 );
	}

	private static final Pattern makeMCPattern( final String mcString )
	{
		return Pattern.compile( mcString + "</a>: ?(?:</td><td>)?<b>(\\d+)</b>" );
	}

	private static Pattern [] compactMCPatterns =
	{
		CharPaneRequest.makeMCPattern( "MC" ),
		CharPaneRequest.makeMCPattern( "Radio" ),
		CharPaneRequest.makeMCPattern( "AOT5K" ),
		CharPaneRequest.makeMCPattern( "HH" ),
	};

	private static Pattern [] expandedMCPatterns =
	{
		CharPaneRequest.makeMCPattern( "Mind Control" ),
		CharPaneRequest.makeMCPattern( "Detuned Radio" ),
		CharPaneRequest.makeMCPattern( "Annoy-o-Tron 5k" ),
		CharPaneRequest.makeMCPattern( "Heartbreaker's" ),
	};

	private static final int handleMindControl( final String responseText, final Pattern pattern )
	{
		Matcher matcher = pattern.matcher( responseText );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final Pattern makeConsumptionPattern( final String consumptionString )
	{
		return Pattern.compile( consumptionString + ": ?</td><td(?: align=left)?><b>(\\d+)</b>" );
	}

	private static Pattern [] compactInebrietyPatterns =
	{
		CharPaneRequest.makeConsumptionPattern( "Drunk" ),
	};

	private static Pattern [] expandedInebrietyPatterns =
	{
		CharPaneRequest.makeConsumptionPattern( "Drunkenness" ),
		CharPaneRequest.makeConsumptionPattern( "Inebriety" ),
		CharPaneRequest.makeConsumptionPattern( "Temulency" ),
		CharPaneRequest.makeConsumptionPattern( "Tipsiness" ),
	};

	private static final int handleConsumption( final String responseText, final Pattern pattern )
	{
		Matcher matcher = pattern.matcher( responseText );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final void handleInebriety( final String text, final Pattern [] patterns )
	{
		for ( int i = 0; i < patterns.length; ++i )
		{
			int level = CharPaneRequest.handleConsumption( text, patterns[i] );
			if ( level > 0 )
			{
				KoLCharacter.setInebriety( level );
				return;
			}
		}

		KoLCharacter.setInebriety( 0 );
	}

	public static final AdventureResult extractEffect( final String responseText, int searchIndex )
	{
		String effectName = null;
		int durationIndex = -1;

		if ( CharPaneRequest.compactCharacterPane )
		{
			int startIndex = responseText.indexOf( "alt=\"", searchIndex ) + 5;
			effectName = responseText.substring( startIndex, responseText.indexOf( "\"", startIndex ) );
			durationIndex = responseText.indexOf( "<td>(", startIndex ) + 5;
		}
		else
		{
			int startIndex = responseText.indexOf( "<font size=2", searchIndex );
			startIndex = responseText.indexOf( ">", startIndex ) + 1;
			durationIndex = responseText.indexOf( "</font", startIndex );
			durationIndex = responseText.lastIndexOf( "(", durationIndex ) + 1;
			if ( durationIndex < 0 )
			{
				return null;
			}
			effectName = responseText.substring( startIndex, durationIndex - 1 ).trim();
		}

		searchIndex = responseText.indexOf( "onClick='eff", searchIndex );
		searchIndex = responseText.indexOf( "(", searchIndex ) + 1;

		String descId = responseText.substring( searchIndex + 1, responseText.indexOf( ")", searchIndex ) - 1 );
		String durationString = responseText.substring( durationIndex, responseText.indexOf( ")", durationIndex ) );

		int duration;
		if ( durationString.equals( "&infin;" ) )
		{
			duration = Integer.MAX_VALUE;
		}
		else if ( durationString.indexOf( "&" ) != -1 || durationString.indexOf( "<" ) != -1 )
		{
			return null;
		}
		else
		{
			duration = StringUtilities.parseInt( durationString );
		}

		return CharPaneRequest.extractEffect( descId, effectName, duration );
	}

	public static final AdventureResult extractEffect( final String descId, String effectName, int duration )
	{
		int effectId = EffectDatabase.getEffect( descId );

		if ( effectId == -1 )
		{
			effectId = EffectDatabase.learnEffectId( effectName, descId );
		}
		else
		{
			effectName = EffectDatabase.getEffectName( effectId );
		}

		if ( effectName.equals( "A Little Bit Evil" ) )
		{
			effectName = effectName + " (" + KoLCharacter.getClassType() + ")";
		}

		if ( duration == Integer.MAX_VALUE )
		{
			// Intrinsic effect
		}

		return new AdventureResult( effectName, duration, true );
	}

	private static final void refreshEffects( final String responseText )
	{
		int searchIndex = 0;
		int onClickIndex = 0;

		KoLConstants.recentEffects.clear();
		ArrayList<AdventureResult> visibleEffects = new ArrayList<AdventureResult>();

		while ( onClickIndex != -1 )
		{
			onClickIndex = responseText.indexOf( "onClick='eff", onClickIndex + 1 );

			if ( onClickIndex == -1 )
			{
				continue;
			}

			searchIndex = responseText.lastIndexOf( "<", onClickIndex );

			AdventureResult effect = CharPaneRequest.extractEffect( responseText, searchIndex );
			if ( effect == null )
			{
				continue;
			}

			int currentCount = effect.getCount();
			if ( currentCount == 0 )
			{
				// This is an expired effect. We don't need to
				// explicitly remove it from activeEffects,
				// since we'll simply not retain it.
				continue;
			}

			int activeCount = effect.getCount( KoLConstants.activeEffects );

			if ( currentCount != activeCount )
			{
				ResultProcessor.processResult( effect.getInstance( currentCount - activeCount ) );
			}

			visibleEffects.add( effect );
		}

		KoLmafia.applyEffects();
		KoLConstants.activeEffects.retainAll( visibleEffects );

		CharPaneRequest.startCounters();
	}

	private static final void startCounters()
	{
		if ( TurnCounter.isCounting( "Wormwood" ) )
		{
			return;
		}

		int absintheCount = CharPaneRequest.ABSINTHE.getCount( KoLConstants.activeEffects );

		if ( absintheCount > 8 )
		{
			TurnCounter.startCounting( absintheCount - 9, "Wormwood loc=151 loc=152 loc=153 wormwood.php", "tinybottle.gif" );
		}
		else if ( absintheCount > 4 )
		{
			TurnCounter.startCounting( absintheCount - 5, "Wormwood loc=151 loc=152 loc=153 wormwood.php", "tinybottle.gif" );
		}
		else if ( absintheCount > 0 )
		{
			TurnCounter.startCounting( absintheCount - 1, "Wormwood loc=151 loc=152 loc=153 wormwood.php", "tinybottle.gif" );
		}
	}

	private static Pattern compactLastAdventurePattern =
		Pattern.compile( "<td align=right><a onclick=[^<]+ title=\"Last Adventure: ([^\"]+)\" target=mainpane href=\"adventure.php\\?snarfblat=([\\d]+)\">.*?</a>:</td>" );
	private static Pattern expandedLastAdventurePattern =
		Pattern.compile( ">Last Adventure.*?<a.*? href=\"adventure.php\\?snarfblat=([^\"]+)\">(.*?)</a>.*?</table>" );

	private static final void checkNewLocation( final String responseText )
	{
		if ( !CharPaneRequest.checkNewLocation )
		{
			return;
		}

		CharPaneRequest.checkNewLocation = false;

		boolean compact = CharPaneRequest.compactCharacterPane;

		Pattern pattern = compact ?
			CharPaneRequest.compactLastAdventurePattern :
			CharPaneRequest.expandedLastAdventurePattern;
		Matcher lastAdventureMatcher = pattern.matcher( responseText );
		if ( !lastAdventureMatcher.find() )
		{
			return;
		}

		String adventureName = compact ? lastAdventureMatcher.group( 1 ) : lastAdventureMatcher.group( 2 );
		String adventureId = compact ? lastAdventureMatcher.group( 2 ) : lastAdventureMatcher.group( 1 );

		CharPaneRequest.checkNewLocation( adventureName, adventureId );
	}

	private static final void checkNewLocation( final String adventureName, final String adventureId )
	{
		// check if we already know this location
		String adventureURL = "adventure.php?snarfblat=" + adventureId;
		KoLAdventure adventure = AdventureDatabase.getAdventureByURL( adventureURL );
		if ( adventure != null )
		{
			return;
		}

		RequestLogger.printLine( "Adding new location: " + adventureName + " - " + adventureURL );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "location", adventureId + " " + adventureName );
	}

	private static Pattern compactFamiliarWeightPattern =
		Pattern.compile( "<br>([\\d]+) lb" );
	private static Pattern expandedFamiliarWeightPattern =
		Pattern.compile( "<b>([\\d]+)</b> pound" );
	private static Pattern familiarImagePattern =
		Pattern.compile( "<a.*?class=\"familiarpick\"><img.*?itemimages/(.*?\\.gif)" );

	private static final void checkFamiliar( final String responseText )
	{
		Pattern pattern = CharPaneRequest.compactCharacterPane ?
			CharPaneRequest.compactFamiliarWeightPattern :
			CharPaneRequest.expandedFamiliarWeightPattern;
		Matcher matcher = pattern.matcher( responseText );
		if ( matcher.find() )
		{
			int weight = StringUtilities.parseInt( matcher.group(1) );
			boolean feasted = responseText.indexOf( "well-fed" ) != -1;
			KoLCharacter.getFamiliar().checkWeight( weight, feasted );
		}

		pattern = CharPaneRequest.familiarImagePattern;
		matcher = pattern.matcher( responseText );
		String image = matcher.find() ? new String( matcher.group( 1 ) ) : null;
		if ( image != null )
		{
			if ( image.startsWith( "snow" ) )
			{
				CharPaneRequest.checkSnowsuit( image );
			}
			else
			{
				KoLCharacter.setFamiliarImage( image );
				CharPaneRequest.checkMedium( responseText );
				CharPaneRequest.checkMiniAdventurer( image );
			}
		}
	}

	private static Pattern compactClancyPattern =
		Pattern.compile( "otherimages/clancy_([123])(_att)?.gif.*?L\\. (\\d+)", Pattern.DOTALL );
	private static Pattern expandedClancyPattern =
		Pattern.compile( "<b>Clancy</b>.*?Level <b>(\\d+)</b>.*?otherimages/clancy_([123])(_att)?.gif", Pattern.DOTALL );
	private static Pattern mediumPattern =
		Pattern.compile( "images/medium_([0123]).gif", Pattern.DOTALL );
	private static Pattern snowsuitPattern =
		Pattern.compile( "snowface([1-5]).gif" );

	public static AdventureResult SACKBUT = ItemPool.get( ItemPool.CLANCY_SACKBUT, 1 );
	public static AdventureResult CRUMHORN = ItemPool.get( ItemPool.CLANCY_CRUMHORN, 1 );
	public static AdventureResult LUTE = ItemPool.get( ItemPool.CLANCY_LUTE, 1 );

	private static final void checkClancy( final String responseText )
	{
		Pattern pattern = CharPaneRequest.compactCharacterPane ?
			CharPaneRequest.compactClancyPattern :
			CharPaneRequest.expandedClancyPattern;
		Matcher clancyMatcher = pattern.matcher( responseText );
		if ( clancyMatcher.find() )
		{
			String level = clancyMatcher.group( CharPaneRequest.compactCharacterPane ? 3 : 1 );
			String image = clancyMatcher.group( CharPaneRequest.compactCharacterPane ? 1 : 2 );
			boolean att = clancyMatcher.group( CharPaneRequest.compactCharacterPane ? 2 : 3 ) != null;
			AdventureResult instrument =
				image.equals( "1" ) ? CharPaneRequest.SACKBUT :
				image.equals( "2" ) ? CharPaneRequest.CRUMHORN :
				image.equals( "3" ) ? CharPaneRequest.LUTE :
				null;
			KoLCharacter.setClancy( StringUtilities.parseInt( level ), instrument, att );
		}
	}

	public enum Companion
	{
		EGGMAN( "Eggman", "jarl_eggman.gif" ),
		RADISH( "Radish Horse", "jarl_horse.gif" ),
		HIPPO( "Hippotatomous", "jarl_hippo.gif" ),
		CREAM( "Cream Puff", "jarl_creampuff.gif" );

		private final String name;
		private final String image;

		private Companion( String name, String image )
		{
			this.name = name;
			this.image = image;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public String imageName()
		{
			return this.image;
		}
	}

	private static final void checkCompanion( final String responseText )
	{
		if ( responseText.contains( "the Eggman" ) )
		{
			KoLCharacter.setCompanion( Companion.EGGMAN );
		}
		else if ( responseText.contains( "the Radish Horse" ) )
		{
			KoLCharacter.setCompanion( Companion.RADISH );
		}
		else if ( responseText.contains( "the Hippotatomous" ) )
		{
			KoLCharacter.setCompanion( Companion.HIPPO );
		}
		else if ( responseText.contains( "the Cream Puff" ) )
		{
			KoLCharacter.setCompanion( Companion.CREAM );
		}
		else
		{
			KoLCharacter.setCompanion( null );
		}
	}

	private static final void checkMedium( final String responseText )
	{
		Pattern pattern = CharPaneRequest.mediumPattern;
		Matcher mediumMatcher = pattern.matcher( responseText );
		if ( mediumMatcher.find() )
		{
			int aura = StringUtilities.parseInt( mediumMatcher.group( 1 ) );
			FamiliarData fam = KoLCharacter.findFamiliar( FamiliarPool.HAPPY_MEDIUM );
			if ( fam == null )
			{
				// Another familiar has turned into a Happy Medium
				return;
			}
			fam.setCharges( aura );
		}
	}

	public static void checkMiniAdventurer( final String image )
	{
		if ( image.startsWith( "miniadv" ) )
		{
			String miniAdvClass = image.substring( 7, 8 );
			if ( !miniAdvClass.equals( Preferences.getString( "miniAdvClass" ) ) )
			{
				Preferences.setString( "miniAdvClass", miniAdvClass );
				KoLCharacter.recalculateAdjustments();
			}
		}
	}

	public enum Snowsuit
	{
		EYEBROWS( 1 ),
		SMIRK( 2 ),
		NOSE( 3 ),
		GOATEE( 4 ),
		HAT( 5 ),
		;

		private int suitValue;

		private Snowsuit( int suitValue )
		{
			this.suitValue = suitValue;
		}

		public static Snowsuit getSnowsuit( int snowValue )
		{
			for ( Snowsuit snowsuit : Snowsuit.values() )
			{
				if ( snowValue == snowsuit.suitValue )
				{
					return snowsuit;
				}
			}
			return null;
		}
	}

	private static final void checkSnowsuit( final String responseText )
	{
		Matcher matcher = CharPaneRequest.snowsuitPattern.matcher( responseText );
		if ( matcher.find() )
		{
			int snow = StringUtilities.parseInt( matcher.group( 1 ) );
			KoLCharacter.setSnowsuit( Snowsuit.getSnowsuit( snow ) );
		}
	}

	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		int turnsThisRun = JSON.getInt( "turnsthisrun" );
		int mafiaTurnsThisRun = KoLCharacter.getCurrentRun();
		CharPaneRequest.turnsThisRun = turnsThisRun;
		ResultProcessor.processAdventuresUsed( turnsThisRun - mafiaTurnsThisRun );

		JSONObject lastadv = JSON.getJSONObject( "lastadv" );
		String adventureId = lastadv.getString( "id" );
		String adventureName = lastadv.getString( "name" );
		String adventureURL = lastadv.getString( "link" );
		if ( adventureURL.startsWith( "adventure.php" ) )
		{
			CharPaneRequest.checkNewLocation( adventureName, adventureId );
		}

		int hp = JSON.getInt( "hp" );
		int maxhp = JSON.getInt( "maxhp" );
		KoLCharacter.setHP( hp, maxhp, maxhp );

		// *** Temporary, I hope: api.php reports the size of your
		// *** Horde as mp/maxmp - but sometimes is inaccurate.
		// *** Bug report filed.
		if ( !KoLCharacter.inZombiecore() )
		{
			int mp = JSON.getInt( "mp" );
			int maxmp = JSON.getInt( "maxmp" );
			KoLCharacter.setMP( mp, maxmp, maxmp );
		}

		int meat = JSON.getInt( "meat" );
		KoLCharacter.setAvailableMeat( meat );

		int drunk = JSON.getInt( "drunk" );
		KoLCharacter.setInebriety( drunk );

		int full = JSON.getInt( "full" );
		KoLCharacter.setFullness( full );

		int spleen = JSON.getInt( "spleen" );
		KoLCharacter.setSpleenUse( spleen );

		int adventures = JSON.getInt( "adventures" );
		KoLCharacter.setAdventuresLeft( adventures );

		int mcd = JSON.getInt( "mcd" );
		KoLCharacter.setMindControlLevel( mcd );

		int classType = JSON.getInt( "class" );
		KoLCharacter.setClassType( classType );

		int pvpFights = JSON.getInt( "pvpfights" );
		KoLCharacter.setAttacksLeft( pvpFights );

		CharPaneRequest.refreshEffects( JSON );

		boolean hardcore = JSON.getInt( "hardcore" ) == 1;
		KoLCharacter.setHardcore( hardcore );

		//boolean casual = JSON.getInt( "casual" ) == 1;
		int roninLeft = JSON.getInt( "roninleft" );

		// *** Assume that roninleft always equals 0 if casual
		KoLCharacter.setRonin( roninLeft > 0 );

		CharPaneRequest.setInteraction();

		if ( KoLCharacter.inAxecore() )
		{
			int level = JSON.getInt( "clancy_level" );
			int itype = JSON.getInt( "clancy_instrument" );
			boolean att = JSON.getBoolean( "clancy_wantsattention" );
			AdventureResult instrument =
				itype == 1 ? CharPaneRequest.SACKBUT :
				itype == 2 ? CharPaneRequest.CRUMHORN :
				itype == 3 ? CharPaneRequest.LUTE :
				null;
			KoLCharacter.setClancy( level, instrument, att );
		}
		else if ( KoLCharacter.isJarlsberg() )
		{
			if ( JSON.has( "jarlcompanion" ) )
			{
				int companion = JSON.getInt( "jarlcompanion" );
				switch ( companion )
				{
				case 1:
					KoLCharacter.setCompanion( Companion.EGGMAN );
					break;
				case 2:
					KoLCharacter.setCompanion( Companion.RADISH );
					break;
				case 3:
					KoLCharacter.setCompanion( Companion.HIPPO );
					break;
				case 4:
					KoLCharacter.setCompanion( Companion.CREAM );
					break;
				}
			}
			else
			{
				KoLCharacter.setCompanion( null );
			}
		}
		else
		{
			int famId = JSON.getInt( "familiar" );
			int famExp = JSON.getInt( "familiarexp" );
			FamiliarData familiar = FamiliarData.registerFamiliar( famId, famExp );
			KoLCharacter.setFamiliar( familiar );

			String image = JSON.getString( "familiarpic" );
			KoLCharacter.setFamiliarImage( image.equals( "" ) ? null : image + ".gif" );

			int weight = JSON.getInt( "famlevel" );
			boolean feasted = JSON.getInt( "familiar_wellfed" ) == 1;
			familiar.checkWeight( weight, feasted );

			// Set charges from the Medium's image
			
			if ( famId == FamiliarPool.HAPPY_MEDIUM )
			{
				int aura = StringUtilities.parseInt( image.substring( 7, 8 ) );
				FamiliarData medium = KoLCharacter.findFamiliar( FamiliarPool.HAPPY_MEDIUM );
				medium.setCharges( aura );
			}
		}
	}

	private static final void refreshEffects( final JSONObject JSON )
		throws JSONException
	{
		ArrayList<AdventureResult> visibleEffects = new ArrayList<AdventureResult>();

		Object o = JSON.get( "effects" );
		if ( o instanceof JSONObject )
		{
			// KoL returns an empty JSON array if there are no effects
			JSONObject effects = (JSONObject) o;

			Iterator keys = effects.keys();
			while ( keys.hasNext() )
			{
				String descId = (String) keys.next();
				JSONArray data = effects.getJSONArray( descId );
				String effectName = data.getString( 0 );
				int count = data.getInt( 1 );

				AdventureResult effect = CharPaneRequest.extractEffect( descId, effectName, count );
				if ( effect != null )
				{
					visibleEffects.add( effect );
				}

			}
		}

		o = JSON.get( "intrinsics" );
		if ( o instanceof JSONObject )
		{
			JSONObject intrinsics = (JSONObject) o;

			Iterator keys = intrinsics.keys();
			while ( keys.hasNext() )
			{
				String descId = (String) keys.next();
				JSONArray data = intrinsics.getJSONArray( descId );
				String effectName = data.getString( 0 );

				AdventureResult effect = CharPaneRequest.extractEffect( descId, effectName, Integer.MAX_VALUE );
				if ( effect != null )
				{
					visibleEffects.add( effect );
				}

			}
		}

		KoLConstants.recentEffects.clear();
		KoLConstants.activeEffects.clear();
		KoLConstants.activeEffects.addAll( visibleEffects );
		KoLConstants.activeEffects.sort();

		// If we are Absinthe Minded, start absinthe counters
		CharPaneRequest.startCounters();

		// Do this now so that familiar weight effects are accounted for
		KoLCharacter.recalculateAdjustments();
	}
}
