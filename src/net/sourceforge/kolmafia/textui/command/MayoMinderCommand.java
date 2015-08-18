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

public class MayoMinderCommand
	extends AbstractCommand
{
	public MayoMinderCommand()
	{
		this.usage = " mayodiol | drunk | mayoflex | adv | mayonex | bmc | mayostat | food | mayozapine | stat ";
	}

	public static final int MAYODIOL = 2;
	public static final int MAYOFLEX = 5;
	public static final int MAYONEX = 1;
	public static final int MAYOSTAT = 3;
	public static final int MAYOZAPINE = 4;

	public static final Object [][] MAYO = new Object[][]
	{
		{ "mayodiol", "drunk", IntegerPool.get( MAYODIOL ) },
		{ "mayoflex", "adv", IntegerPool.get( MAYOFLEX ) },
		{ "mayonex", "bmc", IntegerPool.get( MAYONEX ) },
		{ "mayostat", "food", IntegerPool.get( MAYOSTAT ) },
		{ "mayozapine", "stat", IntegerPool.get( MAYOZAPINE ) },
	};

	public static final int findMayo( final String name )
	{
		for ( int i = 0; i < MAYO.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) MAYO[i][0] ) || name.equalsIgnoreCase( (String) MAYO[i][1] ) )
			{
				Integer index = (Integer) MAYO[i][2];
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
			RequestLogger.printLine( "Usage: mayominder" + this.usage );
			RequestLogger.printLine( "mayodiol or drunk: 1 full from next food converted to drunk" );
			RequestLogger.printLine( "mayoflex or adv: 1 adv from next food" );
			RequestLogger.printLine( "mayonex or bmc: adventures from next food converted to BMC" );
			RequestLogger.printLine( "mayostat or food: return some of next food" );
			RequestLogger.printLine( "mayozapine or stat: double stat gain of next food" );
			return;
		}

		int option = MayoMinderCommand.findMayo( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what '" + parameters + "' mayo is." );
			return;
		}

		AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
		if ( workshed == null || workshed.getItemId() != ItemPool.MAYO_CLINIC )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Mayo clinic not installed" );
			return;
		}
		if ( !InventoryManager.hasItem( ItemPool.MAYO_MINDER ) )
		{
			if ( !InventoryManager.retrieveItem( ItemPool.MAYO_MINDER ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot obtain a Mayo Minder" );
				return;
			}			
		}

		GenericRequest request = new GenericRequest( "inv_use.php?which=3&whichitem=" + ItemPool.MAYO_MINDER ) ;
		RequestThread.postRequest( request );
		request.constructURLString( "choice.php?whichchoice=1076&option=" + option );
		RequestThread.postRequest( request );

		RequestLogger.printLine( "Mayo Minder&trade; now set to " + Preferences.getString( "mayoMinderSetting" ) );
	}
}
