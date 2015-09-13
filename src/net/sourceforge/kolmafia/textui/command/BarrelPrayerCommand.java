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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class BarrelPrayerCommand
	extends AbstractCommand
{
	public BarrelPrayerCommand()
	{
		this.usage = " protection | glamour | vigor | buff ";
	}

	public static final int PROTECTION = 1;
	public static final int GLAMOUR = 2;
	public static final int VIGOR = 3;
	public static final int BUFF = 4;

	public static final Object [][] PRAYER = new Object[][]
	{
		{ "protection", "barrel lid", IntegerPool.get( PROTECTION ) },
		{ "glamour", "barrel hoop earring", IntegerPool.get( GLAMOUR ) },
		{ "vigor", "bankruptcy barrel", IntegerPool.get( VIGOR ) },
		{ "buff", "class buff", IntegerPool.get( BUFF ) },
	};

	public static final int findPrayer( final String name )
	{
		for ( int i = 0; i < PRAYER.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) PRAYER[i][0] ) || name.equalsIgnoreCase( (String) PRAYER[i][1] ) )
			{
				Integer index = (Integer) PRAYER[i][2];
				return index.intValue();
			}
		}

		return 0;
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Usage: barrelprayer" + this.usage );
			RequestLogger.printLine( "protection or barrel lid: get barrel lid (1/ascension)" );
			RequestLogger.printLine( "glamour or barrel hoop earring: get barrel hoop earring (1/ascension)" );
			RequestLogger.printLine( "vigor or bankruptcy barrel : get bankruptcy barrel (1/ascension)" );
			RequestLogger.printLine( "buff: get class buff" );
			return;
		}

		int option = BarrelPrayerCommand.findPrayer( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what '" + parameters + "' barrel prayer is." );
			return;
		}

		if ( !Preferences.getBoolean( "barrelShrineUnlocked" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Barrel Shrine not installed" );
			return;
		}

		if ( Preferences.getBoolean( "_barrelPrayer" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already prayed to the Barrel God today." );
			return;
		}

		if ( ( option == 1 && Preferences.getBoolean( "prayedForProtection" ) ) ||
			( option == 2 && Preferences.getBoolean( "prayedForGlamour" ) ) ||
			( option == 3 && Preferences.getBoolean( "prayedForVigor" ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already prayed for that item this ascension." );
			return;
		}

		GenericRequest request = new GenericRequest( "da.php?barrelshrine=1" ) ;
		RequestThread.postRequest( request );
		request.constructURLString( "choice.php?whichchoice=1100&option=" + option );
		RequestThread.postRequest( request );
	}
}
