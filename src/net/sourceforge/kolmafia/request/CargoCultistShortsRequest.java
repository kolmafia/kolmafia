/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CargoCultistShortsRequest
	extends GenericRequest
{
	public static final String EMPTY_POCKETS_PROPERTY = "cargoPocketsEmptied";
	public static final String PICKED_POCKET_PROPERTY = "_cargoPocketEmptied";

	public static final Set<Integer> pickedPockets = new TreeSet<>();

	public static void loadPockets()
	{
		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		pockets.clear();

		for ( String pocket : Preferences.getString( EMPTY_POCKETS_PROPERTY ).split( " *, *" ) )
		{
			if ( StringUtilities.isNumeric( pocket ) )
			{
				int num = StringUtilities.parseInt( pocket );
				if ( num >= 1 && num <= 666 )
				{
					pockets.add( Integer.valueOf( num ) );
				}
			}
		}
	}

	public static void savePockets()
	{
		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;

		StringBuilder buffer = new StringBuilder();
		for ( Integer pocket : pockets )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( "," );
			}
			buffer.append( pocket );
		}

		Preferences.setString( EMPTY_POCKETS_PROPERTY, buffer.toString() );
	}

	// *** The following should be in the Cargo Cultist Shorts pocket database
	// *** For now, I'm putting this here, since we need to know which pockets lead to fights.

	public static final Map<Integer, String> freeFights = new HashMap<>();

	static
	{
		CargoCultistShortsRequest.freeFights.put( 30,  "bookbat" );
		CargoCultistShortsRequest.freeFights.put( 47,  "dairy goat" );
		CargoCultistShortsRequest.freeFights.put( 136, "Knob Goblin Elite Guardsman" );
		CargoCultistShortsRequest.freeFights.put( 143, "dirty old lihc" );
		CargoCultistShortsRequest.freeFights.put( 191, "batrat" );
		CargoCultistShortsRequest.freeFights.put( 220, "lobsterfrogman" );
		CargoCultistShortsRequest.freeFights.put( 235, "modern zmobie" );
		CargoCultistShortsRequest.freeFights.put( 250, "Blooper" );
		CargoCultistShortsRequest.freeFights.put( 267, "creepy clown" );
		CargoCultistShortsRequest.freeFights.put( 279, "Hellion" );
		CargoCultistShortsRequest.freeFights.put( 299, "Knob Goblin Harem Girl" );
		CargoCultistShortsRequest.freeFights.put( 306, "big creepy spider" );
		CargoCultistShortsRequest.freeFights.put( 317, "Camel's Toe" );
		CargoCultistShortsRequest.freeFights.put( 363, "pufferfish" );
		CargoCultistShortsRequest.freeFights.put( 383, "Skinflute" );
		CargoCultistShortsRequest.freeFights.put( 402, "Fruit Golem" );
		CargoCultistShortsRequest.freeFights.put( 425, "eXtreme Orcish snowboarder" );
		CargoCultistShortsRequest.freeFights.put( 428, "Mob Penguin Thug" );
		CargoCultistShortsRequest.freeFights.put( 443, "War Hippy (space) cadet" );
		CargoCultistShortsRequest.freeFights.put( 448, "completely different spider" );
		CargoCultistShortsRequest.freeFights.put( 452, "pygmy shaman" );
		CargoCultistShortsRequest.freeFights.put( 490, "Booze Giant" );
		CargoCultistShortsRequest.freeFights.put( 565, "mountain man" );
		CargoCultistShortsRequest.freeFights.put( 568, "War Pledge" );
		CargoCultistShortsRequest.freeFights.put( 589, "Green Ops Soldier" );
		CargoCultistShortsRequest.freeFights.put( 646, "1335 HaXx0r" );
		CargoCultistShortsRequest.freeFights.put( 666, "smut orc pervert" );
	};

	// *** End of temporary code

	int pocket = 0;

	public CargoCultistShortsRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1420" );
		this.addFormField( "option", "2" );
		this.pocket = 0;
	}

	public CargoCultistShortsRequest( int pocket )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1420" );
		this.addFormField( "option", "1" );
		this.addFormField( "pocket", String.valueOf( pocket ) );
		this.pocket = pocket;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	public static String getMonsterFight( final String urlString )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		return CargoCultistShortsRequest.getMonsterFight( pocket );
	}

	public static String getMonsterFight( final int pocket )
	{
		return CargoCultistShortsRequest.freeFights.get( pocket );
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you can't get a pair of cargo cultist shorts, punt
		if ( !KoLCharacter.hasEquipped( ItemPool.CARGO_CULTIST_SHORTS, EquipmentManager.PANTS ) &&
		     !InventoryManager.retrieveItem( ItemPool.CARGO_CULTIST_SHORTS, 1, true ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a pair of Cargo Cultist Shorts available" );
			return;
		}

		if ( this.pocket != 0 && Preferences.getBoolean( PICKED_POCKET_PROPERTY ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already looted a pocket from your Cargo Cultist Shorts today" );
			return;
		}

		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		if ( this.pocket != 0 && pockets.contains( this.pocket ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already emptied that pocket this ascension." );
			return;
		}

		// If we are requesting a pocket which leads to a free fight,
		// recover first.
		if ( this.pocket != 0 && this.getMonsterFight( this.pocket ) != null )
		{
			// set location to "None" for the benefit of
			// betweenBattleScripts
			Preferences.setString( "nextAdventure", "None" );
			RecoveryManager.runBetweenBattleChecks( true );
		}

		GenericRequest useRequest = new GenericRequest( "inventory.php" );
		useRequest.addFormField( "action", "pocket" );
		useRequest.run();

		String responseText = useRequest.responseText;

		// No response because of I/O error
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// The request redirected to a choice and the available pockets
		// have been parsed.

		// If we are not looking to empty a pocket here, we are done.
		// We could just walk away from the choice, but lets exit it.

		if ( this.pocket == 0 )
		{
			super.run();
			return;
		}

		// If we have already emptied this pocket this ascension, it is not available.
		// ChoiceManager called parseAvailablePockets, which updated our Set

		if ( pockets.contains( this.pocket ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already emptied that pocket this ascension." );
			this.constructURLString( "choice.php?whichchoice=1420&option=2", true );
			super.run();
			return;
		}

		// Pick the pocket!
		super.run();

		// Some pockets redirect to a fight. If not, we'll be here.
		// ChoiceManager called parsePocketPick, which updated our Set

		responseText = this.responseText;
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// It seems like the power of the pockets has been exhausted for the day.
		if ( responseText.contains( "the power of the pockets has been exhausted for the day" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already picked a pocket today." );
			return;
		}

		// Those that did not, might leave us in the choice

		if ( ChoiceManager.handlingChoice )
		{
			this.constructURLString( "choice.php?whichchoice=1420&option=2", true );
			super.run();
		}
	}

	// <form method="post" action="choice.php" style="display: inline">
	public static final Pattern AVAILABLE_POCKET_PATTERN = Pattern.compile( "<form method=\"post\" action=\"choice.php\" style=\"display: inline\">.*?name=\"pocket\" value=\"(\\d+)\".*?</form>", Pattern.DOTALL );

	public static void parseAvailablePockets( final String responseText )
	{
		// Iterate over the pockets in the form and remove them from the set of all pockets
		if ( !responseText.contains( "There appear to be 666 pockets on these shorts." ) )
		{
			return;
		}

		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		pockets.clear();

		Matcher pocketMatcher = CargoCultistShortsRequest.AVAILABLE_POCKET_PATTERN.matcher( responseText );
		int expected = 1;
		int pocket = 0;
		while ( pocketMatcher.find() )
		{
			pocket = StringUtilities.parseInt( pocketMatcher.group( 1 ) );
			while ( expected < pocket )
			{
				pockets.add( Integer.valueOf( expected++ ) );
			}
			expected++;
		}

		while ( pocket < 666 )
		{
			pockets.add( Integer.valueOf( ++pocket ) );
		}

		// Save the set of pockets we have emptied in the property
		CargoCultistShortsRequest.savePockets();
	}

	public static void parsePocketPick( final String urlString, final String responseText )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );

		if ( pocket < 1 || pocket > 666 )
		{
			return;
		}

		// You decide to leave your pockets unplundered for now
		if ( responseText.contains( "leave your pockets unplundered" ) )
		{
			return;
		}

		// It seems like the power of the pockets has been exhausted for the day.
		if ( responseText.contains( "the power of the pockets has been exhausted for the day" ) )
		{
			Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );
			return;
		}

		// *** What is the message if you've already opened that pocket?
		// *** It doesn't really matter; we'll mark it as picked.

		if ( !CargoCultistShortsRequest.pickedPockets.contains( pocket ) )
		{
			Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );
			CargoCultistShortsRequest.pickedPockets.add( pocket );
			CargoCultistShortsRequest.savePockets();
		}
	}

	public static void registerPocketFight( final String urlString )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );
		CargoCultistShortsRequest.pickedPockets.add( pocket );
		CargoCultistShortsRequest.savePockets();
	}

	public static final Pattern URL_POCKET_PATTERN = Pattern.compile( "pocket=(\\d+)" );
	public static int extractPocketFromURL( final String urlString )
	{
		Matcher matcher = CargoCultistShortsRequest.URL_POCKET_PATTERN.matcher( urlString );
		return  matcher.find() ?
			StringUtilities.parseInt( matcher.group( 1 ) ) :
			0;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( urlString.equals( "inventory.php?action=pocket" ) )
		{
			String  message = "Inspecting Cargo Cultist Shorts";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1420 )
		{
			return false;
		}

		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		if ( pocket == 0 )
		{
			// Pocket must be from 1-666
			return true;
		}

		String  message = "picking pocket " + pocket;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
