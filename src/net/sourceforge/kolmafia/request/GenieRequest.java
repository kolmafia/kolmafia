/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class GenieRequest
	extends GenericRequest
{
	private static boolean usingPocketWish = false;
	private static final Pattern WISH_PATTERN = Pattern.compile( "You have (\\d) wish" );

	private String wish;

	public GenieRequest( final String wish )
	{
		super( "choice.php" );

		this.addFormField( "whichchoice", "1267" );
		this.addFormField( "wish", wish );
		this.addFormField( "option", "1" );
		this.wish = wish;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		int itemId = -1;
		if ( InventoryManager.hasItem( ItemPool.GENIE_BOTTLE ) && Preferences.getInteger( "_genieWishesUsed" ) < 3 )
		{
			itemId = ItemPool.GENIE_BOTTLE;
		}
		else if ( InventoryManager.hasItem( ItemPool.POCKET_WISH ) )
		{
			itemId = ItemPool.POCKET_WISH;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You do not have a genie bottle or pocket wish to use." );
			return;
		}

		GenericRequest useRequest = new GenericRequest( "inv_use.php" );
		useRequest.addFormField( "whichitem", String.valueOf( itemId ) );
		if ( this.getAdventuresUsed() > 0 )
		{
			// set location to "None" for the benefit of
			// betweenBattleScripts
			Preferences.setString( "nextAdventure", "None" );
			RecoveryManager.runBetweenBattleChecks( true );
		}

		useRequest.run();

		super.run();
	}

	@Override
	public int getAdventuresUsed()
	{
		return ( this.wish.startsWith( "to fight" ) || this.wish.equals( "for your freedom" ) ) ? 1 : 0;
	}

	// You are using a pocket wish!
	// You have 2 wishes left today.
	// You have 1 wish left today.

	public static void visitChoice( final String responseText )
	{
		Matcher matcher = GenieRequest.WISH_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int wishesLeft = Integer.parseInt( matcher.group( 1 ) );
			Preferences.setInteger( "_genieWishesUsed", 3 - wishesLeft );
			GenieRequest.usingPocketWish = false;
		}
		else if ( responseText.contains( "You are using a pocket wish!" ) )
		{
			GenieRequest.usingPocketWish = true;
		}
	}

	public static void postChoice( final String responseText )
	{
		if ( responseText.contains( "You acquire" ) ||
		     responseText.contains( "You gain" ) ||
		     responseText.contains( ">Fight!<" ) )
		{
			// Successful wish
			if ( GenieRequest.usingPocketWish )
			{
				ResultProcessor.removeItem( ItemPool.POCKET_WISH );
			}
			else
			{
				Preferences.increment( "_genieWishesUsed" );
			}
		}

		if ( responseText.contains( ">Fight!<" ) )
		{
			Preferences.increment( "_genieFightsUsed" );

			KoLAdventure.lastVisitedLocation = null;
			KoLAdventure.lastLocationName = null;
			KoLAdventure.lastLocationURL = "choice.php";
			KoLAdventure.setNextAdventure( "None" );

			EncounterManager.ignoreSpecialMonsters();

			String message = "[" + KoLAdventure.getAdventureCount() + "] genie summoned monster";
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}
	}

}
