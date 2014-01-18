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

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RestoreExpression;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class BountyDatabase
	extends KoLDatabase
{
	private static final ArrayList<String> bountyNames = new ArrayList<String>();
	private static final Map<String, String> bountyByPlural = new HashMap<String, String>();
	private static final Map<String, String> pluralByName = new HashMap<String, String>();
	private static final Map<String, String> typeByName = new HashMap<String, String>();
	private static final Map<String, String> imageByName = new HashMap<String, String>();
	private static final Map<String, Integer> numberByName = new HashMap<String, Integer>();
	private static final Map<String, String> monsterByName = new HashMap<String, String>();

	static
	{
		BountyDatabase.reset();
	}

	public static void reset()
	{
		BountyDatabase.bountyNames.clear();
		BountyDatabase.bountyByPlural.clear();
		BountyDatabase.pluralByName.clear();
		BountyDatabase.typeByName.clear();
		BountyDatabase.imageByName.clear();
		BountyDatabase.numberByName.clear();
		BountyDatabase.monsterByName.clear();

		BountyDatabase.readData();
	}

	private static void readData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "bounty.txt", KoLConstants.BOUNTY_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 6 )
			{
				continue;
			}
			
			String name = data[ 0 ];
			BountyDatabase.bountyNames.add( name );
			BountyDatabase.bountyByPlural.put( data[ 1 ], name );
			BountyDatabase.pluralByName.put( name, data[ 1 ] );
			BountyDatabase.typeByName.put( name, data[ 2 ] );
			BountyDatabase.imageByName.put( name, data[ 3 ] );
			int number = StringUtilities.parseInt( data[ 4 ] );
			BountyDatabase.numberByName.put( name, IntegerPool.get( number ) );
			BountyDatabase.monsterByName.put( name, data[ 5 ] );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static final void setValue( String name, String plural, String type, String image, int number, String monster )
	{
		BountyDatabase.bountyNames.add( name );
		BountyDatabase.bountyByPlural.put( plural, name );
		BountyDatabase.pluralByName.put( name, plural );
		BountyDatabase.typeByName.put( name, type );
		BountyDatabase.imageByName.put( name, image );
		BountyDatabase.numberByName.put( name, IntegerPool.get( number ) );
		BountyDatabase.monsterByName.put( name, monster );
	}

	public static final String getName( String plural )
	{
		if ( plural == null )
		{
			return null;
		}
		
		return BountyDatabase.bountyByPlural.get( plural );
	}

	public static final String getPlural( String name )
	{
		if ( name == null )
		{
			return null;
		}
		
		return BountyDatabase.pluralByName.get( name );
	}

	public static final String getType( String name )
	{
		if ( name == null )
		{
			return null;
		}

		return BountyDatabase.typeByName.get( name );
	}

	public static final String getImage( String name )
	{
		if ( name == null )
		{
			return null;
		}

		return BountyDatabase.imageByName.get( name );
	}

	public static final int getNumber( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		return (int) BountyDatabase.numberByName.get( name );
	}

	public static final String getMonster( String name )
	{
		if ( name == null )
		{
			return null;
		}

		return BountyDatabase.monsterByName.get( name );
	}
}
