/**
 * Copyright (c) 2005-2016, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class TimeSpinnerCommand
	extends AbstractCommand
{
	public TimeSpinnerCommand()
	{
		this.usage = " list (food|monsters [filter]) | eat (foodname) | prank (target [msg=(message)]) - Use the Time-Spinner";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.TIME_SPINNER ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a Time-Spinner." );
			return;
		}
		if ( parameters.startsWith( "eat " ) )
		{
			if ( Preferences.getInteger( "_timeSpinnerMinutesUsed" ) > 7 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have enough time to eat a past meal." );
				return;
			}
			parameters = parameters.substring( 4 );
			AdventureResult food = ItemFinder.getFirstMatchingItem( parameters, false, ItemFinder.FOOD_MATCH );
			if ( food == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't a valid food." );
				return;
			}

			String[] spinnerFoods = Preferences.getString( "_timeSpinnerFoodAvailable" ).split( "," );
			String foodIdString = String.valueOf( food.getItemId() );
			boolean found = false;
			for ( String temp : spinnerFoods )
			{
				if ( temp.equals( foodIdString ) )
				{
					found = true;
					break;
				}
			}
			if ( !found )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You haven't eaten this yet today." );
				return;
			}

			if ( ConsumablesDatabase.getFullness( food.getName() ) > ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are too full to eat that." );
				return;
			}

			//inv_use.php?whichitem=9104&ajax=1&pwd
			GenericRequest request = new GenericRequest( "inv_use.php" );
			request.addFormField( "whichitem", String.valueOf( ItemPool.TIME_SPINNER ) );
			request.addFormField( "ajax", "1" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			// Redirect to:
			//choice.php?forceoption=0
			// request = new GenericRequest( "choice.php" );
			// request.addFormField( "forceoption", "0" );
			// RequestThread.postRequest( request );

			//choice.php?pwd&whichchoice=1195&option=2
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1195" );
			request.addFormField( "option", "2" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			//choice.php?pwd&whichchoice=1197&option=1&foodid=2527
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1197" );
			request.addFormField( "option", "1" );
			request.addFormField( "foodid", foodIdString );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			// Redirect to:
			//inv_eat.php?pwd&whichitem=2527&ts=1
			// request = new GenericRequest( "inv_eat.php" );
			// request.addFormField( "whichitem", foodIdString );
			// request.addFormField( "ajax", "1" );
			// request.addFormField( "ts", "1" );
			// request.addFormField( "pwd", GenericRequest.passwordHash );
			// RequestThread.postRequest( request );
			return;
		}

		if ( parameters.startsWith( "prank " ) )
		{
			if ( Preferences.getInteger( "_timeSpinnerMinutesUsed" ) == 10 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have enough time to prank anyone." );
				return;
			}

			String target = parameters.substring( 6 );
			String message = null;
			int index = target.indexOf( "msg=" );
			if ( index != -1 )
			{
				message = target.substring( index + 4 ).trim();
				target = target.substring( 0, index ).trim();
			}

			GenericRequest request = new GenericRequest( "inv_use.php" );
			request.addFormField( "whichitem", String.valueOf( ItemPool.TIME_SPINNER ) );
			request.addFormField( "ajax", "1" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1195" );
			request.addFormField( "option", "5" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1198" );
			request.addFormField( "option", "1" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			request.addFormField( "pl", target );
			if ( message != null )
			{
				request.addFormField( "th", message );
			}
			RequestThread.postRequest( request );

			String responseText = request.responseText;
			if ( responseText.contains( "paradoxical time copy" ) )
			{
				return;
			}

			RequestLogger.printLine( "Somebody was already waiting to prank " + target );
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1198" );
			request.addFormField( "option", "2" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			return;
		}

		if ( parameters.trim().equals( "list food" ) )
		{
			if ( Preferences.getString( "_timeSpinnerFoodAvailable" ).equals( "" ) )
			{
				RequestLogger.printLine( "No food available." );
				return;
			}
			String[] spinnerFoods = Preferences.getString( "_timeSpinnerFoodAvailable" ).split( "," );
			RequestLogger.printLine( "Available food:" );
			for ( String food : spinnerFoods )
			{
				AdventureResult item = ItemPool.get( Integer.valueOf( food ) );
				RequestLogger.printLine( item.getName() );
			}
			return;
		}

		if ( parameters.startsWith( "list monsters" ) )
		{
			String filter = parameters.substring( 13 ).trim().toLowerCase();
			boolean filterExists = !filter.equals( "" );

			List<String> monsters = new ArrayList<String>();
			for ( KoLAdventure adv : AdventureDatabase.getAsLockableListModel() )
			{
				if ( !adv.getRequest().getURLString().startsWith( "adventure.php" ) )
				{
					continue;
				}
				for ( Object mon : AdventureQueueDatabase.getZoneQueue( adv ) )
				{
					String monster = (String) mon;
					if ( !monsters.contains( monster ) && 
					     ( !filterExists || monster.toLowerCase().contains( filter ) ) )
					{
						monsters.add( monster );
					}
				}
			}
			Collections.sort( monsters, String.CASE_INSENSITIVE_ORDER );
			if ( monsters.isEmpty() )
			{
				RequestLogger.printLine( "No monsters are available." );
				return;
			}

			RequestLogger.printLine( "Available monsters:" );
			for ( String monster : monsters )
			{
				RequestLogger.printLine( monster );
			}
			return;
		}
	}
}
