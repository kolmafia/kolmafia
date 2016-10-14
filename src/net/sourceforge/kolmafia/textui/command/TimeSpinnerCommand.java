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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class TimeSpinnerCommand
	extends AbstractCommand
{
	public TimeSpinnerCommand()
	{
		this.usage = "";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
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

		if ( parameters.trim().equals( "list food" ) )
		{
			String[] spinnerFoods = Preferences.getString( "_timeSpinnerFoodAvailable" ).split( "," );
			RequestLogger.printLine( "Available foods:" );
			for ( String food : spinnerFoods )
			{
				AdventureResult item = ItemPool.get( Integer.valueOf( food ) );
				RequestLogger.printLine( item.getName() );
			}
			return;
		}
	}
}
