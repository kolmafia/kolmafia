/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class CharPaneRequest
	extends GenericRequest
{
	private static final AdventureResult ABSINTHE = new AdventureResult( "Absinthe-Minded", 1, true );

	private static boolean canInteract = false;
	private static boolean isRunning = false;
	private static String lastResponse = "";

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss z", Locale.US );

	private static long timestamp = 0;
	private static int turnsthisrun = 0;

	private static final CharPaneRequest instance = new CharPaneRequest();

	private CharPaneRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charpane.php" );
	}

	public static final void reset()
	{
		CharPaneRequest.lastResponse = "";
		CharPaneRequest.canInteract = false;
		CharPaneRequest.timestamp = 0;
		CharPaneRequest.turnsthisrun = 0;
	}

	public static final CharPaneRequest getInstance()
	{
		return CharPaneRequest.instance;
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
		if ( CharPaneRequest.isRunning )
		{
			return;
		}

		CharPaneRequest.isRunning = true;
		super.run();
		CharPaneRequest.isRunning = false;
	}

	public void processResults()
	{
		CharPaneRequest.processCharacterPane( this.responseText, this.date );
	}

	public static final String getLastResponse()
	{
		return CharPaneRequest.lastResponse;
	}

	public static final String decorateLastResponse()
	{
		StringBuffer buffer = new StringBuffer( CharPaneRequest.lastResponse );
		CharPaneDecorator.decorate( buffer );
		return buffer.toString();
	}

	public static final synchronized void processCharacterPane( final String responseText, final String date )
	{
		// The timestamp only has precision to the nearest second.
		//
		// On a fast connection, one can get successive charpanes
		// generated within the same second and thus with the same
		// timestamp.
		//
		// 1) If we accept updates with the same timestamp, we'll catch
		// fast updates, but be fooled if they arrive out of order.
		//
		// 2) If we reject updates unless the timestamp monotonically
		// increases, we'll skip out-of-order updates, but skip valid
		// updates sent within the same second.
		//
		// We can't have it both ways
		//
		// Case 1 (fast updates) seems more common than case 2
		// (out-of-order updates), so we use <, rather than <=

		long timestamp = parseTimestamp( date );
		if ( timestamp < CharPaneRequest.timestamp )
		{
			return;
		}

		// KoL now includes Javascript variables in each charpane
		//
		// var turnsplayed = 232576;
		// var turnsthisrun = 232576;
		// var rollover = 1268537400;
		// var rightnow = 1268496181;
		// var pwdhash = "...";
		//
		// "turnsthisrun" is of interest for several reasons: we can
		// use it to order (some) charpane requests, even if the
		// timestamp is the same, and we can use it to synchronize
		// KolMafia with KoL's turn counter

		int turnsthisrun = parseTurnsThisRun( responseText );
		int mafiaturnsthisrun = KoLCharacter.getCurrentRun();

		// In Valhalla, turnsthisrun equals 0. Always accept that.
		if ( turnsthisrun > 0 &&
		     ( turnsthisrun < CharPaneRequest.turnsthisrun ||
		       turnsthisrun < mafiaturnsthisrun ) )
		{
			return;
		}

		// Since we believe this update, synchronize with it
		ResultProcessor.processAdventuresUsed( turnsthisrun - mafiaturnsthisrun );

		CharPaneRequest.timestamp = timestamp;
		CharPaneRequest.turnsthisrun = turnsthisrun;
		CharPaneRequest.lastResponse = responseText;

		// We can deduce whether we are in compact charpane mode

		GenericRequest.compactCharacterPane = responseText.indexOf( "<br>Lvl. " ) != -1;

		// The easiest way to retrieve the character pane data is to
		// use regular expressions. But, the only data that requires
		// synchronization is the modified stat values, health and
		// mana.

		if ( responseText.indexOf( "<img src=\"http://images.kingdomofloathing.com/otherimages/inf_small.gif\">" ) == -1 )
		{
			if ( GenericRequest.compactCharacterPane )
			{
				CharPaneRequest.handleCompactMode( responseText );
			}
			else
			{
				CharPaneRequest.handleExpandedMode( responseText );
			}
		}
		else
		{
			KoLCharacter.setStatPoints( 1, 0L, 1, 0L, 1, 0L );
			KoLCharacter.setHP( 1, 1, 1 );
			KoLCharacter.setMP( 1, 1, 1 );
			KoLCharacter.setAvailableMeat( 0 );
			KoLCharacter.setAdventuresLeft( 0 );
			KoLCharacter.setMindControlLevel( 0 );
		}

		CharPaneRequest.refreshEffects( responseText );
		KoLCharacter.recalculateAdjustments();
		CharPaneRequest.checkFamiliar( responseText );
		KoLCharacter.updateStatus();

		CharPaneRequest.setInteraction( CharPaneRequest.checkInteraction( responseText ) );

	}

	private static final long parseTimestamp( final String date )
	{
		try
		{
			if ( date != null )
			{
				Date timestamp = CharPaneRequest.TIMESTAMP_FORMAT.parse( date );
				return timestamp.getTime();
			}
		}
		catch ( ParseException e )
		{
		}

		return 0;
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

		String descriptionId = responseText.substring( searchIndex + 1, responseText.indexOf( ")", searchIndex ) - 1 );
		int effectId = EffectDatabase.getEffect( descriptionId );

		String duration = responseText.substring( durationIndex, responseText.indexOf( ")", durationIndex ) );
		
		if ( effectName.equals( "A Little Bit Evil" ) )
		{
			effectName = effectName + " (" + KoLCharacter.getClassType() + ")";
		}

		if ( effectId == -1 )
		{
			effectId = EffectDatabase.getEffectId( effectName );

			if ( effectId != -1 )
			{
				EffectDatabase.addDescriptionId( effectId, effectName, descriptionId );
			}
		}
		else
		{
			effectName = EffectDatabase.getEffectName( effectId );
		}

		if ( duration.equals( "&infin;" ) )
		{
			duration = String.valueOf( Integer.MAX_VALUE );
			if ( effectName.equalsIgnoreCase( "Temporary Blindness" ) )
			{
				effectName = "Temporary Blindness (intrinsic)";
			}
		}
		if ( duration.indexOf( "&" ) != -1 || duration.indexOf( "<" ) != -1 )
		{
			return null;
		}

		return new AdventureResult( effectName, StringUtilities.parseInt( duration ), true );
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

		// If new effects have been detected, write override files
		KoLmafia.saveDataOverride();

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
}
