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
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

/* 
 * Instead of packing and unpacking a giant treemap into user preference files, this is a way of persisting a variable across sessions.
 * Uses the Java Serializable interface.
 */

public class AdventureQueueDatabase
	implements Serializable
{
	private static final long serialVersionUID = -180241952508113931L;

	private static TreeMap<String, RollingLinkedList> ADVENTURE_QUEUE = new TreeMap<String, RollingLinkedList>();

	// debugging tool
	public static void showQueue()
	{
		Set<String> keys = ADVENTURE_QUEUE.keySet();

		for ( String key : keys )
		{
			RollingLinkedList zoneQueue = ADVENTURE_QUEUE.get( key );

			StringBuilder builder = new StringBuilder( key + ": " );

			for ( Object it : zoneQueue )
			{
				if ( it != null )
					builder.append( it.toString() + " | " );
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
		AdventureQueueDatabase.ADVENTURE_QUEUE = new TreeMap<String, RollingLinkedList>();

		List< ? > list = AdventureDatabase.getAsLockableListModel();

		for ( Object ob : list )
		{
			KoLAdventure adv = (KoLAdventure) ob;
			AdventureQueueDatabase.ADVENTURE_QUEUE.put( adv.getAdventureName(), new RollingLinkedList( 5 ) );
		}

		if ( serializeAfterwards )
		{
			AdventureQueueDatabase.serialize();
		}
	}

	public static void enqueue( KoLAdventure adv, String monster )
	{
		AdventureQueueDatabase.enqueue( adv.getAdventureName(), monster );
	}

	public static void enqueue( String adventureName, String monster )
	{
		if ( adventureName == null || monster == null )
			return;

		RollingLinkedList zoneQueue = ADVENTURE_QUEUE.get( adventureName );

		if ( zoneQueue == null )
			return;

		zoneQueue.add( monster );
	}

	public static RollingLinkedList getZoneQueue( KoLAdventure adv )
	{
		return AdventureQueueDatabase.getZoneQueue( adv.getAdventureName() );
	}

	public static RollingLinkedList getZoneQueue( String adv )
	{
		return ADVENTURE_QUEUE.get( adv );
	}

	public static void serialize()
	{
		File file = new File( KoLConstants.DATA_DIRECTORY, KoLCharacter.baseUserName() + "_" + "queue.ser" );

		try
		{
			FileOutputStream fileOut = new FileOutputStream( file );
			ObjectOutputStream out = new ObjectOutputStream( fileOut );
			out.writeObject( ADVENTURE_QUEUE );
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
		File file = new File( KoLConstants.DATA_DIRECTORY, KoLCharacter.baseUserName() + "_" + "queue.ser" );

		if ( !file.exists() )
		{
			AdventureQueueDatabase.resetQueue( false );
			return;
		}
		try
		{
			FileInputStream fileIn = new FileInputStream( file );
			ObjectInputStream in = new ObjectInputStream( fileIn );

			ADVENTURE_QUEUE = (TreeMap<String, RollingLinkedList>) in.readObject();

			in.close();
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
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
