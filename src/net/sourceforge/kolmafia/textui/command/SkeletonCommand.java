/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UseItemRequest;

public class SkeletonCommand
	extends AbstractCommand
{
	public SkeletonCommand()
	{
		this.usage = " warrior | cleric | wizard | rogue | buddy";
	}

	public static final int WARRIOR = 1;
	public static final int CLERIC = 2;
	public static final int WIZARD = 3;
	public static final int ROGUE = 4;
	public static final int BUDDY = 5;


	public static final Object [][] SKELETONS = new Object[][]
	{
		{ "warrior", IntegerPool.get( WARRIOR ) },
		{ "cleric", IntegerPool.get( CLERIC ) },
		{ "wizard", IntegerPool.get( WIZARD ) },
		{ "rogue", IntegerPool.get( ROGUE ) },
		{ "buddy", IntegerPool.get( BUDDY ) },
	};

	public static final int findSkeleton( final String name )
	{
		for ( int i = 0; i < SKELETONS.length; ++i )
		{
			String skeleton = (String) SKELETONS[i][0];
			if ( name.equals( skeleton ) )
			{
				Integer index = (Integer) SKELETONS[i][1];
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
			RequestLogger.printLine( "Usage: skeleton" + this.usage );
			RequestLogger.printLine( "warrior: damage, delevel" );
			RequestLogger.printLine( "cleric: hot damage, hp" );
			RequestLogger.printLine( "wizard: cold damage, mp" );
			RequestLogger.printLine( "rogue: damage, meat" );
			RequestLogger.printLine( "buddy: delevel, exp" );
			return;
		}

		int option;
		option = SkeletonCommand.findSkeleton( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what a '" + parameters + "' skeleton is." );
			return;
		}

		Preferences.setString( "choiceAdventure603", String.valueOf( option ) );
		AdventureResult skeleton = ItemPool.get( ItemPool.SKELETON, 1 );
		RequestThread.postRequest( UseItemRequest.getInstance( skeleton ) );
	}
}
