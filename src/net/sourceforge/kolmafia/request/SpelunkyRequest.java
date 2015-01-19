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

package net.sourceforge.kolmafia.request;

import java.lang.StringBuilder;

import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpelunkyRequest
	extends GenericRequest
{
	private static final Pattern TURNS_PATTERN = Pattern.compile( "Ghost'><br><b>(\\d+)</b>" );
	private static final Pattern GOLD_PATTERN = Pattern.compile( "Gold: <b>\\$(\\d+)</b>" );
	private static final Pattern BOMB_PATTERN = Pattern.compile( "Bombs' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>" );
	private static final Pattern ROPE_PATTERN = Pattern.compile( "Ropes' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>" );
	private static final Pattern KEY_PATTERN = Pattern.compile( "Keys' width=30 height=30></td><td valign=center align=left><b>(\\d+)</b>" );
	private static final Pattern BUDDY_PATTERN = Pattern.compile( "Buddy:</b(?:.*)alt='(.*?)' " );
	private static final Pattern SHOP_PATTERN = Pattern.compile( "Buddy:</b(?:.*)alt='(.*?)' " );

	private static final Pattern TURNS_STATUS_PATTERN = Pattern.compile( "Turns: (\\d+)" );
	private static final Pattern GOLD_STATUS_PATTERN = Pattern.compile( "Gold: (\\d+)" );
	private static final Pattern BOMB_STATUS_PATTERN = Pattern.compile( "Bombs: (\\d+)" );
	private static final Pattern ROPE_STATUS_PATTERN = Pattern.compile( "Ropes: (\\d+)" );
	private static final Pattern KEY_STATUS_PATTERN = Pattern.compile( "Keys: (\\d+)" );
	private static final Pattern BUDDY_STATUS_PATTERN = Pattern.compile( "Buddy: (.*?)," );
	private static final Pattern UNLOCK_STATUS_PATTERN = Pattern.compile( "Unlocks: (.*)" );

	public SpelunkyRequest()
	{
		super( "place.php" );
	}

	public static void reset()
	{
		Preferences.resetToDefault( "spelunkyStatus" );
	}

	public static void parseCharpane( final String responseText )
	{
		if ( !responseText.contains( ">Last Spelunk</a>" ) )
		{
			return;
		}

		KoLCharacter.setLimitmode( KoLCharacter.SPELUNKY );

		Boolean ghostWaving = false;

		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );

		Matcher matcher = SpelunkyRequest.TURNS_PATTERN.matcher( responseText );
		int turnsLeft = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.GOLD_PATTERN.matcher( responseText );
		int gold = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.BOMB_PATTERN.matcher( responseText );
		int bombs = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.ROPE_PATTERN.matcher( responseText );
		int ropes = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.KEY_PATTERN.matcher( responseText );
		int keys = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.BUDDY_PATTERN.matcher( spelunkyStatus );
		String buddy = matcher.find() ? matcher.group( 1 ) : "";
		matcher = SpelunkyRequest.UNLOCK_STATUS_PATTERN.matcher( spelunkyStatus );
		String unlocks = matcher.find() ? matcher.group( 1 ) : "";

		if ( responseText.contains( "spelghostarms.gif" ) )
		{
			ghostWaving = true;
		}

		StringBuffer newUnlocks = new StringBuffer( unlocks );
		if ( responseText.contains( "'Sticky Bombs'" ) && !unlocks.contains( "Sticky Bombs" ) )
		{
			newUnlocks.append( ",Sticky Bombs" );
		}

		// Write status string
		StringBuffer statusString = new StringBuffer( "" );
		statusString.append( "Turns: " );
		statusString.append( turnsLeft );
		if ( ghostWaving )
		{
			statusString.append( ", Non-combat Due" );
		}
		statusString.append( ", Gold: " );
		statusString.append( gold );
		statusString.append( ", Bombs: " );
		statusString.append( bombs );
		statusString.append( ", Ropes: " );
		statusString.append( ropes );
		statusString.append( ", Keys: " );
		statusString.append( keys );
		statusString.append( ", Buddy: " );
		statusString.append( buddy );
		statusString.append( ", Unlocks: " );
		statusString.append( newUnlocks );
		Preferences.setString( "spelunkyStatus", statusString.toString() );

		String upgradeString = Preferences.getString( "spelunkyUpgrades" );
		StringBuffer newUpgradeString = new StringBuffer( upgradeString );

		// If we have all upgrades, no point looking at upgrades
		// If first turn, update upgrades unlocked
		if ( !upgradeString.equals( "YYYYYYYYY" ) && turnsLeft == 40 )
		{
			if ( gold == 100 )
			{
				newUpgradeString.replace( 3, 6, "YYY" );
			}
			else if ( bombs == 3 )
			{
				newUpgradeString.replace( 3, 4, "Y" );
			}
			if ( keys == 1 )
			{
				newUpgradeString.replace( 6, 9, "YYY" );
			}
			else if ( responseText.contains( "hobofedora.gif" ) )
			{
				newUpgradeString.replace( 6, 8, "YY" );
			}
			else if ( ropes == 3 )
			{
				newUpgradeString.replace( 6, 7, "Y" );
			}
			Preferences.setString( "spelunkyUpgrades", newUpgradeString.toString() );
		}
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichplace=spelunky" ) )
		{
			return;
		}

		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );

		Matcher matcher = SpelunkyRequest.TURNS_STATUS_PATTERN.matcher( spelunkyStatus );
		int turnsLeft = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.GOLD_STATUS_PATTERN.matcher( spelunkyStatus );
		int gold = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.BOMB_STATUS_PATTERN.matcher( spelunkyStatus );
		int bombs = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.ROPE_STATUS_PATTERN.matcher( spelunkyStatus );
		int ropes = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.KEY_STATUS_PATTERN.matcher( spelunkyStatus );
		int keys = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher( spelunkyStatus );
		String buddy = matcher.find() ? matcher.group( 1 ) : "";
		matcher = SpelunkyRequest.UNLOCK_STATUS_PATTERN.matcher( spelunkyStatus );
		String unlocks = matcher.find() ? matcher.group( 1 ) : "";
		Boolean ghostWaving = spelunkyStatus.contains( "Non-combat Due" );

		Boolean jungleUnlocked = unlocks.contains( "Jungle" ) || responseText.contains( "spelunky/jungle.gif" );
		Boolean iceCavesUnlocked = unlocks.contains( "Ice Caves" ) || responseText.contains( "spelunky/icecaves.gif" );
		Boolean templeRuinsUnlocked = unlocks.contains( "Temple Ruins" ) || responseText.contains( "spelunky/templeruins.gif" );
		Boolean snakePitUnlocked = unlocks.contains( "Snake Pit" ) || responseText.contains( "spelunky/snakepit.gif" );
		Boolean spiderHoleUnlocked = unlocks.contains( "Spider Hole" ) || responseText.contains( "spelunky/spiderhole.gif" );
		Boolean burialGroundUnlocked = unlocks.contains( "Burial Ground" ) || responseText.contains( "spelunky/burialground.gif" );
		Boolean beehiveUnlocked = unlocks.contains( "Beehive" ) || responseText.contains( "spelunky/beehive.gif" );
		Boolean altarUnlocked = unlocks.contains( "Altar" ) || responseText.contains( "spelunky/altar.gif" );
		Boolean LOLmecLairUnlocked = unlocks.contains( "LOLmec's Lair" ) || responseText.contains( "spelunky/lolmec.gif" );

		StringBuffer newUnlocks = new StringBuffer( unlocks );
		if ( jungleUnlocked && !unlocks.contains( "Jungle" ) )
		{
			if ( !unlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Jungle" );
		}
		if ( iceCavesUnlocked && !unlocks.contains( "Ice Caves" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Ice Caves" );
		}
		if ( templeRuinsUnlocked && !unlocks.contains( "Temple Ruins" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Temple Ruins" );
		}
		if ( snakePitUnlocked && !unlocks.contains( "SnakePit" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Snake Pit" );
		}
		if ( spiderHoleUnlocked && !unlocks.contains( "Spider Hole" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Spider Hole" );
		}
		if ( beehiveUnlocked && !unlocks.contains( "Beehive" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Beehive" );
		}
		if ( burialGroundUnlocked && !unlocks.contains( "Burial Ground" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Burial Ground" );
		}
		if ( spiderHoleUnlocked && !unlocks.contains( "Altar" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "Altar" );
		}
		if ( LOLmecLairUnlocked && !unlocks.contains( "LOLmec's Lair" ) )
		{
			if ( !unlocks.equals( "" ) || !newUnlocks.equals( "" ) )
			{
				newUnlocks.append( ", " );
			}
			newUnlocks.append( "LOLmec's Lair" );
		}

		// Write status string
		StringBuffer statusString = new StringBuffer( "" );
		statusString.append( "Turns: " );
		statusString.append( turnsLeft );
		if ( ghostWaving )
		{
			statusString.append( ", Non-combat Due" );
		}
		statusString.append( ", Gold: " );
		statusString.append( gold );
		statusString.append( ", Bombs: " );
		statusString.append( bombs );
		statusString.append( ", Ropes: " );
		statusString.append( ropes );
		statusString.append( ", Keys: " );
		statusString.append( keys );
		statusString.append( ", Buddy: " );
		statusString.append( buddy );
		statusString.append( ", Unlocks: " );
		statusString.append( newUnlocks );
		Preferences.setString( "spelunkyStatus", statusString.toString() );

		String upgradeString = Preferences.getString( "spelunkyUpgrades" );
		StringBuffer newUpgradeString = new StringBuffer( upgradeString );

		// If we have all upgrades, no point looking at upgrades
		// If first turn, update unlocks
		if ( !upgradeString.equals( "YYYYYYYYY" ) && turnsLeft == 40 )
		{
			if ( iceCavesUnlocked )
			{
				newUpgradeString.replace( 0, 2, "YY" );
			}
			else if ( jungleUnlocked )
			{
				newUpgradeString.replace( 0, 1, "Y" );
			}
			Preferences.setString( "spelunkyUpgrades", newUpgradeString.toString() );
		}
	}

	public static void queenBeeDefeated()
	{
		// Temp fix til we tell the two queen bees apart
		if ( KoLCharacter.getLimitmode() == KoLCharacter.SPELUNKY )
		{
			String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
			if ( !spelunkyStatus.contains( "Sticky Bombs" ) )
			{
				Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Sticky Bombs" ) );
			}
		}
	}

	public static void unlockSpiderHole()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Spider Hole" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Spider Hole" ) );
		}
	}

	public static void unlockSnakePit()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Snake Pit" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Snake Pit" ) );
		}
	}

	public static void unlockBurialGround()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Burial Ground" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Burial Ground" ) );
		}
	}

	public static void unlockBeehive()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Beehive" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Beehive" ) );
		}
	}

	public static void unlockAltar()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Altar" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Altar" ) );
		}
	}

	public static void unlockCrashedUFO()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "Crashed UFO" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", Crashed UFO" ) );
		}
	}

	public static void unlockCityOfGoooold()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		if ( !spelunkyStatus.contains( "City of Goooold" ) )
		{
			Preferences.setString( "spelunkyStatus", ( spelunkyStatus + ", City of Goooold" ) );
		}
	}

	public static void sacrifice()
	{
		String spelunkyStatus = Preferences.getString( "spelunkyStatus" );
		Matcher matcher = SpelunkyRequest.BUDDY_STATUS_PATTERN.matcher( spelunkyStatus );
		String buddy = matcher.find() ? matcher.group( 1 ) : "";

		StringBuffer upgradeMessage = new StringBuffer( "" );
		upgradeMessage.append( "You have sacrificed your Buddy, " );
		upgradeMessage.append( buddy );
		String message = upgradeMessage.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}
	
	public static void upgrade( final int choice )
	{
		String upgradeString = Preferences.getString( "spelunkyUpgrades" );
		StringBuffer newUpgradeString = new StringBuffer( upgradeString );

		if ( choice >= 1 && choice <= 9 && !upgradeString.equals( "YYYYYYYYY" ) )
		{
			newUpgradeString.replace( choice - 1, choice, "Y" );
			
			// Log upgrade
			StringBuffer upgradeMessage = new StringBuffer( "" );
			upgradeMessage.append( "Spelunky Finished. Upgrade chosen is " );
			switch( choice )
			{
			case 1:
				upgradeMessage.append( "Unlock Jungle." );
				break;
			case 2:
				upgradeMessage.append( "Unlock Ice Caves." );
				break;
			case 3:
				upgradeMessage.append( "Unlock Temple Ruins." );
				break;
			case 4:
				upgradeMessage.append( "Start with +2 bombs." );
				break;
			case 5:
				upgradeMessage.append( "More Shopkeeper items for sale." );
				break;
			case 6:
				upgradeMessage.append( "Begin with 100 gold." );
				break;
			case 7:
				upgradeMessage.append( "Start with +2 Ropes." );
				break;
			case 8:
				upgradeMessage.append( "Start with Fedora." );
				break;
			case 9:
				upgradeMessage.append( "Start with key." );
				break;
			}
			String message = upgradeMessage.toString();
			RequestLogger.printLine();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}
		Preferences.setString( "spelunkyUpgrades", newUpgradeString.toString() );
	}

	public static void logShop( final String responseText, final int decision )
	{
		// We are choosing to buy from shop
		Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int choice = StringUtilities.parseInt( matcher.group( 1 ) );
			String choiceText = matcher.group( 2 );

			if ( choice == decision && choice != 6 )
			{
				String message = "Bought" + choiceText.substring( 3 );
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			String upgradeString = Preferences.getString( "spelunkyUpgrades" );
			if ( choice == 4 && !upgradeString.equals( "YYYYYYYYY" ) )
			{
				StringBuffer newUpgradeString = new StringBuffer( upgradeString );
				newUpgradeString.replace( 4, 5, "Y" );
				Preferences.setString( "spelunkyUpgrades", newUpgradeString.toString() );
			}
		}
	}
}