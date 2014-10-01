/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;


/*
 * Instead of packing and unpacking a giant treemap into user preference files, this is a way of persisting a variable across sessions.
 * Uses the Java Serializable interface.
 */

public class AdventureSpentDatabase
	implements Serializable
{

	private static Map<String, Integer> TURNS = new TreeMap<String, Integer>();
	
	private static int lastTurnUpdated = -1;

	// debugging tool
	public static void showTurns()
	{
		Set<String> keys = TURNS.keySet();

		for ( String key : keys )
		{
			int turns = TURNS.get( key );
			RequestLogger.printLine( key + ": " + turns );
		}
	}

	public static void resetTurns()
	{
		resetTurns( true );
	}

	private static void resetTurns( boolean serializeAfterwards )
	{
		AdventureSpentDatabase.TURNS = new TreeMap<String, Integer>();

		List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();

		for ( KoLAdventure adv : list )
		{
			AdventureSpentDatabase.TURNS.put( adv.getAdventureName(), 0 );
		}

		if ( serializeAfterwards )
		{
			AdventureSpentDatabase.serialize();
		}
	}

	private static boolean checkZones()
	{
		// See if any zones aren't in the Map.  Add them if so.

		List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();
		Set<String> keys = TURNS.keySet();

		boolean keyAdded = false;

		for ( KoLAdventure adv : list )
		{
			if ( !keys.contains( adv.getAdventureName() ) )
			{
				AdventureSpentDatabase.TURNS.put( adv.getAdventureName(), 0 );
				keyAdded = true;
			}
		}

		return keyAdded;
	}

	public static void addTurn( KoLAdventure adv )
	{
		String name = adv.getAdventureName();
		AdventureSpentDatabase.addTurn( name );
	}

	public static void addTurn( final String loc )
	{
		if ( loc == null )
		{
			return;
		}
		int turns = AdventureSpentDatabase.TURNS.get( loc );
		AdventureSpentDatabase.TURNS.put( loc, turns + 1 );
	}

	public static void setTurns( final String loc, final int turns )
	{
		// This function should rarely be needed
		if ( loc == null )
		{
			return;
		}
		if ( !AdventureSpentDatabase.TURNS.containsKey( loc ) )
		{
			RequestLogger.printLine( loc + " is not a recognized location." );
			return;
		}
		AdventureSpentDatabase.TURNS.put( loc, turns );
	}

	public static int getTurns( KoLAdventure adv )
	{
		return AdventureSpentDatabase.TURNS.get( adv.getAdventureName() );
	}

	public static int getTurns( final String loc )
	{
		if ( !AdventureSpentDatabase.TURNS.containsKey( loc ) )
		{
			RequestLogger.printLine( loc + " is not a recognized location." );
			return -1;
		}
		return AdventureSpentDatabase.TURNS.get( loc );
	}

	public static void serialize()
	{
		File file = new File( KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "turns.ser" );

		try
		{
			FileOutputStream fileOut = new FileOutputStream( file );
			ObjectOutputStream out = new ObjectOutputStream( fileOut );

			out.writeObject( AdventureSpentDatabase.TURNS );
			out.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	/*
	 * Attempts to load saved adventure queue settings from <username>_queue.ser
	 */
	public static void deserialize()
	{
		File file = new File( KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "turns.ser" );

		if ( !file.exists() )
		{
			AdventureSpentDatabase.resetTurns( false );
			return;
		}
		try
		{
			FileInputStream fileIn = new FileInputStream( file );
			ObjectInputStream in = new ObjectInputStream( fileIn );

			AdventureSpentDatabase.TURNS = (TreeMap<String, Integer>) in.readObject();

			in.close();

			// after successfully loading, check if there were new zones added that aren't yet in the TreeMap.
			AdventureSpentDatabase.checkZones();
		}
		catch ( FileNotFoundException e )
		{
			AdventureSpentDatabase.resetTurns( false );
			return;
		}
		catch ( ClassNotFoundException e )
		{
			// Found the file, but the contents did not contain a properly-serialized treemap.
			// Wipe the bogus file.
			file.delete();
			AdventureSpentDatabase.resetTurns();
			return;
		}
		catch ( ClassCastException e )
		{
			// Old version of the combat queue handling.  Sorry, have to delete your queue.
			file.delete();
			AdventureSpentDatabase.resetTurns();
			return;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static final int getLastTurnUpdated()
	{
		return AdventureSpentDatabase.lastTurnUpdated;
	}

	public static final void setLastTurnUpdated( final int turnUpdated )
	{
		AdventureSpentDatabase.lastTurnUpdated = turnUpdated;
	}
}
