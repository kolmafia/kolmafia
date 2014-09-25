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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.RollingLinkedList;

/*
 * Instead of packing and unpacking a giant treemap into user preference files, this is a way of persisting a variable across sessions.
 * Uses the Java Serializable interface.
 */

public class AdventureQueueDatabase
	implements Serializable
{
	private static final long serialVersionUID = -180241952508113931L;

	private static TreeMap<String, RollingLinkedList> COMBAT_QUEUE = new TreeMap<String, RollingLinkedList>();
	private static TreeMap<String, RollingLinkedList> NONCOMBAT_QUEUE = new TreeMap<String, RollingLinkedList>();

	// debugging tool
	public static void showQueue()
	{
		Set<String> keys = COMBAT_QUEUE.keySet();

		for ( String key : keys )
		{
			RollingLinkedList zoneQueue = COMBAT_QUEUE.get( key );

			StringBuilder builder = new StringBuilder( key + ": " );

			for ( Object it : zoneQueue )
			{
				if ( it != null )
					builder.append( it.toString() ).append( " | " );
			}
			RequestLogger.printLine( builder.toString() );
		}

		RequestLogger.printLine( );
		RequestLogger.printLine( "Noncombats:" );

		keys = NONCOMBAT_QUEUE.keySet();

		for ( String key : keys )
		{
			RollingLinkedList zoneQueue = NONCOMBAT_QUEUE.get( key );

			StringBuilder builder = new StringBuilder( key + ": " );

			for ( Object it : zoneQueue )
			{
				if ( it != null )
					builder.append( it.toString() ).append( " | " );
			}
			RequestLogger.printLine( builder.toString() );
		}
	}

	public static void resetQueue()
	{
		resetQueue( true );
	}

	private static void resetQueue( boolean serializeAfterwards )
	{
		AdventureQueueDatabase.COMBAT_QUEUE = new TreeMap<String, RollingLinkedList>();
		AdventureQueueDatabase.NONCOMBAT_QUEUE = new TreeMap<String, RollingLinkedList>();

		List< ? > list = AdventureDatabase.getAsLockableListModel();

		for ( Object ob : list )
		{
			KoLAdventure adv = (KoLAdventure) ob;
			AdventureQueueDatabase.COMBAT_QUEUE.put( adv.getAdventureName(), new RollingLinkedList( 5 ) );
			AdventureQueueDatabase.NONCOMBAT_QUEUE.put( adv.getAdventureName(), new RollingLinkedList( 5 ) );
		}

		if ( serializeAfterwards )
		{
			AdventureQueueDatabase.serialize();
		}
	}

	private static boolean checkZones()
	{
		// See if any zones aren't in the TreeMap.  Add them if so.

		List< ? > list = AdventureDatabase.getAsLockableListModel();
		Set<String> keys = COMBAT_QUEUE.keySet();

		boolean keyAdded = false;

		for ( Object ob : list )
		{
			KoLAdventure adv = (KoLAdventure) ob;
			if ( !keys.contains( adv.getAdventureName() ) )
			{
				AdventureQueueDatabase.COMBAT_QUEUE.put( adv.getAdventureName(), new RollingLinkedList( 5 ) );
				keyAdded = true;
			}
		}

		keys = NONCOMBAT_QUEUE.keySet();

		for ( Object ob : list )
		{
			KoLAdventure adv = (KoLAdventure) ob;
			if ( !keys.contains( adv.getAdventureName() ) )
			{
				AdventureQueueDatabase.NONCOMBAT_QUEUE.put( adv.getAdventureName(), new RollingLinkedList( 5 ) );
				keyAdded = true;
			}
		}

		return keyAdded;
	}

	public static void enqueue( KoLAdventure adv, String monster )
	{
		if ( adv == null || monster == null )
			return;
		AdventureQueueDatabase.enqueue( adv.getAdventureName(), monster );
	}

	public static void enqueueNoncombat( KoLAdventure adv, String name )
	{
		if ( adv == null || name == null )
			return;
		AdventureQueueDatabase.enqueueNoncombat( adv.getAdventureName(), name );
	}

	public static void enqueue( String adventureName, String monster )
	{
		if ( adventureName == null || monster == null )
			return;

		RollingLinkedList zoneQueue = COMBAT_QUEUE.get( adventureName );

		if ( zoneQueue == null )
			return;

		MonsterData mon = MonsterDatabase.findMonster( CombatActionManager.encounterKey( monster ), true );

		if ( mon == null )
		{
			// We /should/ have canonicalized the string by now (and matching correctly failed), but just in case see if stripping off "the" helps.
			// Other articles definitely should have been handled by now.
			if ( monster.startsWith( "the " ) || monster.startsWith( "The " ) )
			{
				mon = MonsterDatabase.findMonster( CombatActionManager.encounterKey( monster.substring( 4 ) ), true );
			}

			if ( mon == null )
				return;
		}

		zoneQueue.add( mon.getName() );
	}

	public static void enqueueNoncombat( String noncombatAdventureName, String name )
	{
		if ( noncombatAdventureName == null )
			return;

		RollingLinkedList zoneQueue = NONCOMBAT_QUEUE.get( noncombatAdventureName );

		if ( zoneQueue == null )
			return;

		zoneQueue.add( name );
	}

	public static RollingLinkedList getZoneQueue( KoLAdventure adv )
	{
		return AdventureQueueDatabase.getZoneQueue( adv.getAdventureName() );
	}

	public static RollingLinkedList getZoneQueue( String adv )
	{
		return COMBAT_QUEUE.get( adv );
	}

	public static RollingLinkedList getZoneNoncombatQueue( KoLAdventure adv )
	{
		return AdventureQueueDatabase.getZoneNoncombatQueue( adv.getAdventureName() );
	}

	public static RollingLinkedList getZoneNoncombatQueue( String adv )
	{
		return NONCOMBAT_QUEUE.get( adv );
	}

	public static void serialize()
	{
		File file = new File( KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "queue.ser" );

		try
		{
			FileOutputStream fileOut = new FileOutputStream( file );
			ObjectOutputStream out = new ObjectOutputStream( fileOut );

			// make a collection with combat queue first
			List<TreeMap<String, RollingLinkedList>> queues = new ArrayList<TreeMap<String, RollingLinkedList>>();
			queues.add( COMBAT_QUEUE );
			queues.add( NONCOMBAT_QUEUE );
			out.writeObject( queues );
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
	@SuppressWarnings( "unchecked" )
	public static void deserialize()
	{
		File file = new File( KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "queue.ser" );

		if ( !file.exists() )
		{
			AdventureQueueDatabase.resetQueue( false );
			return;
		}
		try
		{
			FileInputStream fileIn = new FileInputStream( file );
			ObjectInputStream in = new ObjectInputStream( fileIn );

			List<TreeMap<String, RollingLinkedList>> queues =
				(List<TreeMap<String, RollingLinkedList>>) in.readObject();

			// Combat queue is first
			COMBAT_QUEUE = queues.get( 0 );
			NONCOMBAT_QUEUE = queues.get( 1 );

			in.close();

			// after successfully loading, check if there were new zones added that aren't yet in the TreeMap.
			AdventureQueueDatabase.checkZones();
		}
		catch ( FileNotFoundException e )
		{
			AdventureQueueDatabase.resetQueue( false );
			return;
		}
		catch ( ClassNotFoundException e )
		{
			// Found the file, but the contents did not contain a properly-serialized treemap.
			// Wipe the bogus file.
			file.delete();
			AdventureQueueDatabase.resetQueue();
			return;
		}
		catch ( ClassCastException e )
		{
			// Old version of the combat queue handling.  Sorry, have to delete your queue.
			file.delete();
			AdventureQueueDatabase.resetQueue();
			return;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static double applyQueueEffects( double numerator, MonsterData monster, AreaCombatData data )
	{
		String zone = data.getZone();
		RollingLinkedList zoneQueue = COMBAT_QUEUE.get( zone );

		double denominator = data.totalWeighting();

		// without queue effects the result is just numerator/denominator.
		if ( zoneQueue == null )
		{
			return numerator / denominator;
		}

		// rate for monster IN the queue is 1 / (4a - 3b) and rate for monster NOT IN the queue is 4 / (4a - 3b) where
		// a = weight of monsters in the zone
		// b = weight of monsters in the queue

		HashSet<Object> zoneSet = new HashSet<Object>( zoneQueue ); // just care about unique elements

		// Ignore monsters in the queue that aren't actually part of the zone's normal monster list
		// This includes monsters that have special conditions to find and wandering monsters
		// that are not part of the location at all
		// Ignore olfacted monsters, as these are never rejected
		int queueWeight = 0;
		Iterator iter = zoneSet.iterator();
		while ( iter.hasNext() )
		{
			String mon = (String) iter.next();
			MonsterData queueMonster = MonsterDatabase.findMonster( mon, false );
			int index = data.getMonsterIndex( queueMonster );
			boolean olfacted = Preferences.getString( "olfactedMonster" ).equals( queueMonster.getName() ) && 
							KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL );
			if ( index != -1 && data.getWeighting( index ) > 0 && !olfacted )
			{
				queueWeight += data.getWeighting( index );
			}
		}

		boolean olfacted = Preferences.getString( "olfactedMonster" ).equals( monster.getName() ) && 
							KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL );
		double newNumerator = numerator * ( zoneQueue.contains( monster.getName() ) && !olfacted ? 1 : 4 );
		double newDenominator = ( 4 * denominator - 3 * queueWeight );

		return newNumerator / newDenominator;
	}
}
