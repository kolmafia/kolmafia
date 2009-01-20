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

public class CharPaneRequest
	extends GenericRequest
{
	private static final AdventureResult ABSINTHE = new AdventureResult( "Absinthe-Minded", 1, true );

	private static boolean canInteract = false;
	private static boolean isRunning = false;
	private static String lastResponse = "";

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss z", Locale.US );

	private static long timestamp = 0;

	private static final CharPaneRequest instance = new CharPaneRequest();

	private CharPaneRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charpane.php" );
	}

	public static final CharPaneRequest getInstance()
	{
		return CharPaneRequest.instance;
	}

	protected boolean retryOnTimeout()
	{
		return true;
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

	public static final synchronized void processCharacterPane( final String responseText, final String date )
	{
		long timestamp = parseTimestamp( date );
		if ( timestamp <= CharPaneRequest.timestamp )
		{
			return;
		}

		CharPaneRequest.timestamp = timestamp;
		CharPaneRequest.lastResponse = responseText;

		// By refreshing the KoLCharacter pane, you can
		// determine whether or not you are in compact
		// mode - be sure to refresh this value.

		GenericRequest.isCompactMode = responseText.indexOf( "<br>Lvl. " ) != -1;

		// The easiest way to retrieve the KoLCharacter pane data is to
		// use regular expressions. But, the only data that requires
		// synchronization is the modified stat values, health and
		// mana.

		if ( responseText.indexOf( "<img src=\"http://images.kingdomofloathing.com/otherimages/inf_small.gif\">" ) == -1 )
		{
			if ( GenericRequest.isCompactMode )
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
			KoLCharacter.setStatPoints( 1, 0, 1, 0, 1, 0 );
			KoLCharacter.setHP( 1, 1, 1 );
			KoLCharacter.setMP( 1, 1, 1 );
			KoLCharacter.setAvailableMeat( 0 );
			KoLCharacter.setAdventuresLeft( 0 );
			KoLCharacter.setMindControlLevel( 0 );
			KoLCharacter.setDetunedRadioVolume( 0 );
			KoLCharacter.setAnnoyotronLevel( 0 );
		}

		CharPaneRequest.refreshEffects( responseText );
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
			CharPaneRequest.handleStatPoints( responseText, "Mus", "Mys", "Mox" );
			CharPaneRequest.handleMiscPoints( responseText, "HP", "MP", "Meat", "Adv", "", "<b>", "</b>" );
			CharPaneRequest.handleMindControl( responseText, "MC" );
			CharPaneRequest.handleDetunedRadio( responseText, "Radio" );
			CharPaneRequest.handleAnnoyotron( responseText, "AOT5K" );
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
			CharPaneRequest.handleStatPoints( responseText, "Muscle", "Mysticality", "Moxie" );
			CharPaneRequest.handleMiscPoints(
				responseText, "hp\\.gif", "mp\\.gif", "meat\\.gif", "hourglass\\.gif", "&nbsp;", "<span.*?>", "</span>" );
			CharPaneRequest.handleMindControl( responseText, "Mind Control" );
			CharPaneRequest.handleDetunedRadio( responseText, "Detuned Radio" );
			CharPaneRequest.handleAnnoyotron( responseText, "Annoy-o-Tron 5k" );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void handleStatPoints( final String responseText, final String musString,
		final String mysString, final String moxString )
		throws Exception
	{
		int[] modified = new int[ 3 ];

		Matcher statMatcher =
			Pattern.compile(
				musString + ".*?<b>(.*?)</b>.*?" + mysString + ".*?<b>(.*?)</b>.*?" + moxString + ".*?<b>(.*?)</b>" ).matcher(
				responseText );

		if ( statMatcher.find() )
		{
			for ( int i = 0; i < 3; ++i )
			{
				Matcher modifiedMatcher =
					Pattern.compile( "<font color=blue>(\\d+)</font>&nbsp;\\((\\d+)\\)" ).matcher(
						statMatcher.group( i + 1 ) );

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

			KoLCharacter.setStatPoints(
				modified[ 0 ], KoLCharacter.getTotalMuscle(), modified[ 1 ], KoLCharacter.getTotalMysticality(),
				modified[ 2 ], KoLCharacter.getTotalMoxie() );
		}
	}

	private static final void handleMiscPoints( final String responseText, final String hpString,
		final String mpString, final String meatString, final String advString, final String spacer,
		final String openTag, final String closeTag )
		throws Exception
	{
		// On the other hand, health and all that good stuff is
		// complicated, has nested images, and lots of other weird
		// stuff. Handle it in a non-modular fashion.

		Matcher miscMatcher =
			Pattern.compile(
				hpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" + mpString + ".*?" + openTag + "(.*?)" + spacer + "/" + spacer + "(.*?)" + closeTag + ".*?" + meatString + ".*?" + openTag + "(.*?)" + closeTag + ".*?" + advString + ".*?" + openTag + "(.*?)" + closeTag ).matcher(
				responseText );

		if ( miscMatcher.find() )
		{
			String currentHP = miscMatcher.group( 1 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			String maximumHP = miscMatcher.group( 2 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

			String currentMP = miscMatcher.group( 3 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			String maximumMP = miscMatcher.group( 4 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );

			KoLCharacter.setHP(
				StringUtilities.parseInt( currentHP ), StringUtilities.parseInt( maximumHP ),
				StringUtilities.parseInt( maximumHP ) );
			KoLCharacter.setMP(
				StringUtilities.parseInt( currentMP ), StringUtilities.parseInt( maximumMP ),
				StringUtilities.parseInt( maximumMP ) );

			String availableMeat = miscMatcher.group( 5 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			KoLCharacter.setAvailableMeat( StringUtilities.parseInt( availableMeat ) );

			String adventuresLeft = miscMatcher.group( 6 ).replaceAll( "<[^>]*>", "" ).replaceAll( "[^\\d]+", "" );
			int oldAdventures = KoLCharacter.getAdventuresLeft();
			int newAdventures = StringUtilities.parseInt( adventuresLeft );

			if ( oldAdventures != newAdventures )
			{
				ResultProcessor.processAdventures( newAdventures - oldAdventures );
			}
		}
	}

	private static final void handleMindControl( final String responseText, final String mcString )
		throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
		{
			KoLCharacter.setMindControlLevel( StringUtilities.parseInt( matcher.group( 2 ) ) );
		}
		else
		{
			KoLCharacter.setMindControlLevel( 0 );
		}
	}

	private static final void handleDetunedRadio( final String responseText, final String mcString )
		throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
		{
			KoLCharacter.setDetunedRadioVolume( StringUtilities.parseInt( matcher.group( 2 ) ) );
		}
		else
		{
			KoLCharacter.setDetunedRadioVolume( 0 );
		}
	}

	private static final void handleAnnoyotron( final String responseText, final String mcString )
		throws Exception
	{
		Matcher matcher = Pattern.compile( mcString + "</a>: ?(</td><td>)?<b>(\\d+)</b>" ).matcher( responseText );

		if ( matcher.find() )
		{
			KoLCharacter.setAnnoyotronLevel( StringUtilities.parseInt( matcher.group( 2 ) ) );
		}
		else
		{
			KoLCharacter.setAnnoyotronLevel( 0 );
		}
	}

	public static final AdventureResult extractEffect( final String responseText, int searchIndex )
	{
		String effectName = null;
		int durationIndex = -1;

		if ( GenericRequest.isCompactMode )
		{
			int startIndex = responseText.indexOf( "alt=\"", searchIndex ) + 5;
			effectName = responseText.substring( startIndex, responseText.indexOf( "\"", startIndex ) );
			durationIndex = responseText.indexOf( "<td>(", startIndex ) + 5;
		}
		else
		{
			int startIndex = responseText.indexOf( "<font size=2>", searchIndex ) + 13;
			effectName = responseText.substring( startIndex, responseText.indexOf( "(", startIndex ) ).trim();
			durationIndex = responseText.indexOf( "(", startIndex ) + 1;
		}

		searchIndex = responseText.indexOf( "onClick='eff", searchIndex );
		searchIndex = responseText.indexOf( "(", searchIndex ) + 1;

		String descriptionId = responseText.substring( searchIndex + 1, responseText.indexOf( ")", searchIndex ) - 1 );
		int effectId = EffectDatabase.getEffect( descriptionId );

		String duration = responseText.substring( durationIndex, responseText.indexOf( ")", durationIndex ) );

		if ( effectId == -1 )
		{
			effectId = EffectDatabase.getEffectId( effectName );

			if ( effectId != -1 )
			{
				RequestLogger.printLine( "Status effect database updated." );
				EffectDatabase.addDescriptionId( effectId, descriptionId );
			}
		}

		if ( duration.equals( "&infin;" ) )
		{
			duration = String.valueOf( Integer.MAX_VALUE - 100 );
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

		if ( TurnCounter.isCounting( "Wormwood" ) )
		{
			return;
		}

		int absintheCount = CharPaneRequest.ABSINTHE.getCount( KoLConstants.activeEffects );

		if ( absintheCount > 8 )
		{
			TurnCounter.startCounting( absintheCount - 9, "Wormwood", "tinybottle.gif" );
		}
		else if ( absintheCount > 4 )
		{
			TurnCounter.startCounting( absintheCount - 5, "Wormwood", "tinybottle.gif" );
		}
		else if ( absintheCount > 0 )
		{
			TurnCounter.startCounting( absintheCount - 1, "Wormwood", "tinybottle.gif" );
		}
	}
}
