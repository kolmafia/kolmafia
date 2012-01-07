/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

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
	extends RelayRequest
{
	private static final AdventureResult ABSINTHE = new AdventureResult( "Absinthe-Minded", 1, true );

	private static boolean canInteract = false;
	private static boolean inValhalla = false;

	private static String lastResponse = "";
	private static long lastResponseTimestamp = 0;

	private static int turnsThisRun = 0;

	public CharPaneRequest()
	{
		super( true );

		this.constructURLString( "charpane.php", false );
	}

	public static final void reset()
	{
		CharPaneRequest.lastResponse = "";
		CharPaneRequest.canInteract = false;
		CharPaneRequest.turnsThisRun = 0;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

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
		CharPaneRequest.setInteraction( CharPaneRequest.checkInteraction( "" ) );
	}

	public static final void setInteraction( final boolean interaction )
	{
		CharPaneRequest.canInteract = interaction;
	}

	public void run()
	{
		super.run();

		if ( this.responseCode == 200 )
		{
			CharPaneRequest.lastResponse = responseText;
		}
	}

	public static boolean processResults( long responseTimestamp, String responseText )
	{
		if ( CharPaneRequest.lastResponseTimestamp > responseTimestamp )
		{
			return false;
		}

		CharPaneRequest.lastResponseTimestamp = responseTimestamp;

		// We can deduce whether we are in compact charpane mode

		GenericRequest.compactCharacterPane = responseText.indexOf( "<br>Lvl. " ) != -1;

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

		if ( GenericRequest.compactCharacterPane )
		{
			CharPaneRequest.handleCompactMode( responseText );
		}
		else
		{
			CharPaneRequest.handleExpandedMode( responseText );
		}

		CharPaneRequest.refreshEffects( responseText );
		KoLCharacter.recalculateAdjustments();
		CharPaneRequest.checkFamiliar( responseText );
		CharPaneRequest.setInteraction( CharPaneRequest.checkInteraction( responseText ) );

		KoLCharacter.updateStatus();

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
		Pattern pattern = GenericRequest.compactCharacterPane ?
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

	private static final boolean checkInteraction( final String responseText )
	{
		// If the charsheet does not say he can't interact, ok
		// (this will be true for any Casual run, for an unascended
		// character, or for a sufficiently lengthy softcore run)
		if ( !KoLCharacter.inRonin() )
		{
			return true;
		}

		// Last time we checked the char sheet, he was still in
		// ronin. See if he still it.
		if ( KoLCharacter.getCurrentRun() < 1000 )
		{
			return false;
		}

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

		// If character pane doesn't mention storage, ok.
		if ( responseText.indexOf( "storage.php" ) == -1 )
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
			CharPaneRequest.handleMiscPoints( responseText, CharPaneRequest.compactMiscPattern );
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
		try
		{
			CharPaneRequest.handleFullness( responseText, CharPaneRequest.compactFullnessPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
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
			CharPaneRequest.handleMiscPoints( responseText, CharPaneRequest.expandedMiscPattern );
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
		try
		{
			CharPaneRequest.handleFullness( responseText, CharPaneRequest.expandedFullnessPatterns );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
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

	private static final Pattern makeMiscPattern( final String hpString, final String mpString,
						      final String meatString, final String advString,
						      final String spacer, final String openTag, final String closeTag )
	{
		return Pattern.compile( hpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" + mpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" + meatString + ".*?" + openTag + "(.*?)" + closeTag + ".*?" + advString + ".*?" + openTag + "(.*?)" + closeTag );
	}

	private static Pattern compactMiscPattern =
		CharPaneRequest.makeMiscPattern( "HP", "MP", "Meat", "Adv", "", "<b>", "</b>" );
	private static Pattern expandedMiscPattern =
		CharPaneRequest.makeMiscPattern( "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );

	private static final void handleMiscPoints( final String responseText, final Pattern pattern )
		throws Exception
	{
		// On the other hand, health and all that good stuff is
		// complicated, has nested images, and lots of other weird
		// stuff. Handle it in a non-modular fashion.

		Matcher miscMatcher = pattern.matcher( responseText );

		if ( !miscMatcher.find() )
		{
                        return;
                }

                String currentHP = miscMatcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
                String maximumHP = miscMatcher.group( 2 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

                String currentMP = miscMatcher.group( 3 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
                String maximumMP = miscMatcher.group( 4 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

                KoLCharacter.setHP( StringUtilities.parseInt( currentHP ),
                                    StringUtilities.parseInt( maximumHP ),
                                    StringUtilities.parseInt( maximumHP ) );
                KoLCharacter.setMP( StringUtilities.parseInt( currentMP ),
                                    StringUtilities.parseInt( maximumMP ),
                                    StringUtilities.parseInt( maximumMP ) );

                String availableMeat = miscMatcher.group( 5 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
                KoLCharacter.setAvailableMeat( StringUtilities.parseInt( availableMeat ) );

                String adventuresLeft = miscMatcher.group( 6 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
                int oldAdventures = KoLCharacter.getAdventuresLeft();
                int newAdventures = StringUtilities.parseInt( adventuresLeft );
                ResultProcessor.processAdventuresLeft( newAdventures - oldAdventures );
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

	private static Pattern [] compactFullnessPatterns =
	{
		CharPaneRequest.makeConsumptionPattern( "Full" ),
	};

	private static Pattern [] expandedFullnessPatterns =
	{
		CharPaneRequest.makeConsumptionPattern( "Fullness" ),
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

	private static final void handleFullness( final String text, final Pattern [] patterns )
	{
		for ( int i = 0; i < patterns.length; ++i )
		{
			int level = CharPaneRequest.handleConsumption( text, patterns[i] );
			if ( level > 0 )
			{
				KoLCharacter.setFullness( level );
				return;
			}
		}
	}

	public static final AdventureResult extractEffect( final String responseText, int searchIndex )
	{
		String effectName = null;
		int durationIndex = -1;

		if ( GenericRequest.compactCharacterPane )
		{
			int startIndex = responseText.indexOf( "alt=\"", searchIndex ) + 5;
			effectName = responseText.substring( startIndex, responseText.indexOf( "\"", startIndex ) );
			durationIndex = responseText.indexOf( "<td>(", startIndex ) + 5;
		}
		else
		{
			int startIndex = responseText.indexOf( "<font size=2>", searchIndex ) + 13;
			durationIndex = responseText.indexOf( "</font", startIndex );
			durationIndex = responseText.lastIndexOf( "(", durationIndex ) + 1;
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
			if ( effectName.equalsIgnoreCase( "Temporary Blindness" ) )
			{
				effectName = "Temporary Blindness (intrinsic)";
			}
		}

		return new AdventureResult( effectName, duration, true );
	}

	private static final void refreshEffects( final String responseText )
	{
		int searchIndex = 0;
		int onClickIndex = 0;

		KoLConstants.recentEffects.clear();
		ArrayList visibleEffects = new ArrayList();

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

			int activeCount = effect.getCount( KoLConstants.activeEffects );

			if ( effect.getCount() != activeCount )
			{
				ResultProcessor.processResult( effect.getInstance( effect.getCount() - activeCount ) );
			}

			visibleEffects.add( effect );
		}

		KoLmafia.applyEffects();
		KoLConstants.activeEffects.retainAll( visibleEffects );

		if ( TurnCounter.isCounting( "Wormwood" ) )
		{
			return;
		}

		CharPaneRequest.startCounters();
	}

	private static final void startCounters()
	{
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

	private static Pattern compactFamiliarPattern =
		Pattern.compile( "<br>([\\d]+) lb" );
	private static Pattern expandedFamiliarPattern =
		Pattern.compile( "<b>([\\d]+)</b> pound" );

	private static final void checkFamiliar( final String responseText )
	{
		Pattern pattern = GenericRequest.compactCharacterPane ?
			CharPaneRequest.compactFamiliarPattern :
			CharPaneRequest.expandedFamiliarPattern;
		Matcher familiarMatcher = pattern.matcher( responseText );
		if ( familiarMatcher.find() )
		{
			int weight = StringUtilities.parseInt( familiarMatcher.group(1) );
			boolean feasted = responseText.indexOf( "well-fed" ) != -1;
			KoLCharacter.getFamiliar().checkWeight( weight, feasted );
		}
	}

	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		int turnsThisRun = JSON.getInt( "turnsthisrun" );
		CharPaneRequest.turnsThisRun = turnsThisRun;
		KoLCharacter.setCurrentRun( turnsThisRun );

		int hp = JSON.getInt( "hp" );
		int maxhp = JSON.getInt( "maxhp" );
		KoLCharacter.setHP( hp, maxhp, maxhp );

		int mp = JSON.getInt( "mp" );
		int maxmp = JSON.getInt( "maxmp" );
		KoLCharacter.setMP( mp, maxmp, maxmp );

		int meat = JSON.getInt( "meat" );
		KoLCharacter.setAvailableMeat( meat );

		int adventures = JSON.getInt( "adventures" );
		KoLCharacter.setAdventuresLeft( adventures );

		int mcd = JSON.getInt( "mcd" );
		KoLCharacter.setMindControlLevel( mcd );

		KoLConstants.recentEffects.clear();
		ArrayList visibleEffects = new ArrayList();

		int classType = JSON.getInt( "class" );
		KoLCharacter.setClassType( classType );

		JSONObject effects = JSON.getJSONObject( "effects" );
		Iterator keys = effects.keys();
		while ( keys.hasNext() )
		{
			String descId = (String) keys.next();
			JSONArray data = effects.getJSONArray( descId );
			String effectName = data.getString( 0 );
			int count = data.getInt( 1 );

			AdventureResult effect = CharPaneRequest.extractEffect( descId, effectName, count );
			if ( effect == null )
			{
				continue;
			}

			int activeCount = effect.getCount( KoLConstants.activeEffects );

			if ( count != activeCount )
			{
				ResultProcessor.processResult( effect.getInstance( count - activeCount ) );
			}

			visibleEffects.add( effect );
		}

		KoLmafia.applyEffects();
		KoLConstants.activeEffects.retainAll( visibleEffects );

		// If we are Absinthe Minded, start absinthe counters
		CharPaneRequest.startCounters();

		int famId = JSON.getInt( "familiar" );
		int famExp = JSON.getInt( "familiarexp" );
		int weight = JSON.getInt( "famlevel" );
		FamiliarData familiar = FamiliarData.registerFamiliar( famId, famExp );
		KoLCharacter.setFamiliar( familiar );

		// *** No current way to check feasted!
		boolean feasted = false;
		familiar.checkWeight( weight, feasted );

		KoLCharacter.updateStatus();

		boolean hardcore = JSON.getInt( "hardcore" ) == 1;
		KoLCharacter.setHardcore( hardcore );

		boolean casual = JSON.getInt( "casual" ) == 1;
		int roninLeft = JSON.getInt( "roninleft" );

		// *** Assume that roninleft always equals 0 if casual
		KoLCharacter.setRonin( roninLeft > 0 );

		CharPaneRequest.setInteraction();

		KoLCharacter.recalculateAdjustments();
	}
}
