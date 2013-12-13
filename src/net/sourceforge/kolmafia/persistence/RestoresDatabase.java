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

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

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

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class RestoresDatabase
	extends KoLDatabase
{
	private static final Map<String, String> typeByName = new HashMap<String, String>();
	private static final Map<String, String> hpMinByName = new HashMap<String, String>();
	private static final Map<String, String> hpMaxByName = new HashMap<String, String>();
	private static final Map<String, String> mpMinByName = new HashMap<String, String>();
	private static final Map<String, String> mpMaxByName = new HashMap<String, String>();
	private static final Map<String, Integer> advCostByName = new HashMap<String, Integer>();
	private static final Map<String, String> usesLeftByName = new HashMap<String, String>();
	private static final Map<String, String> notesByName = new HashMap<String, String>();

	static
	{
		RestoresDatabase.reset();
	}

	public static void reset()
	{
		RestoresDatabase.typeByName.clear();
		RestoresDatabase.hpMinByName.clear();
		RestoresDatabase.hpMaxByName.clear();
		RestoresDatabase.mpMinByName.clear();
		RestoresDatabase.mpMaxByName.clear();
		RestoresDatabase.advCostByName.clear();
		RestoresDatabase.usesLeftByName.clear();
		RestoresDatabase.notesByName.clear();

		RestoresDatabase.readData();
	}

	private static void readData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "restores.txt", KoLConstants.RESTORES_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 7 )
			{
				continue;
			}
			
			String name = data[ 0 ];
			RestoresDatabase.typeByName.put( name, data[ 1 ] );
			RestoresDatabase.hpMinByName.put( name, data[ 2 ] );
			RestoresDatabase.hpMaxByName.put( name, data[ 3 ] );
			RestoresDatabase.mpMinByName.put( name, data[ 4 ] );
			RestoresDatabase.mpMaxByName.put( name, data[ 5 ] );
			int advCost = StringUtilities.parseInt( data[ 6 ] );
			RestoresDatabase.advCostByName.put( name, IntegerPool.get( advCost ) );
			
			if ( data.length > 7 )
			{
				RestoresDatabase.usesLeftByName.put( name, data[ 7 ] );
			}
			else
			{
				RestoresDatabase.usesLeftByName.put( name, "unlimited" );
			}
			
			if ( data.length > 8 )
			{
				RestoresDatabase.notesByName.put( name, data[ 8 ] );
			}
			else
			{
				RestoresDatabase.notesByName.put( name, "" );
			}
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
	
	private static final int getValue( String stringValue, String name )
	{
		if ( stringValue == null )
		{
			return -1;
		}
		int lb = stringValue.indexOf( "[" );
		if ( lb == -1 )
		{
			return Integer.parseInt( stringValue );
		}
		int rb = stringValue.indexOf( "]", lb );
		RestoreExpression expr = new RestoreExpression( stringValue.substring( lb + 1, rb ), name );
		if( expr.hasErrors() )
		{
			KoLmafia.updateDisplay( "Error in restores.txt for item " + name + ", invalid expression " + stringValue );
			return -1;
		}
		return (int) expr.eval();
	}

	public static final String getType( String name )
	{
		if ( name == null )
		{
			return null;
		}

		return RestoresDatabase.typeByName.get( name );
	}

	public static final int getHPMin( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		String hpMin = RestoresDatabase.hpMinByName.get( name );
		if ( hpMin == null )
		{
			return 0;
		}
		return (int) Math.floor( RestoresDatabase.getValue( hpMin, name ) );
	}

	public static final int getHPMax( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		String hpMax = RestoresDatabase.hpMaxByName.get( name );
		if ( hpMax == null )
		{
			return 0;
		}
		return (int) Math.ceil( RestoresDatabase.getValue( hpMax, name ) );
	}

	public static final int getMPMin( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		String mpMin = RestoresDatabase.mpMinByName.get( name );
		if ( mpMin == null )
		{
			return 0;
		}
		return (int) Math.floor( RestoresDatabase.getValue( mpMin, name ) );
	}

	public static final Integer getMPMax( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		String mpMax = RestoresDatabase.mpMaxByName.get( name );
		if ( mpMax == null )
		{
			return 0;
		}
		return (int) Math.ceil( RestoresDatabase.getValue( mpMax, name ) );
	}

	public static final double getHPAverage( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		return ( RestoresDatabase.getHPMax( name ) + RestoresDatabase.getHPMin( name ) ) / 2.0;
	}

	public static final double getMPAverage( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		return ( RestoresDatabase.getMPMax( name ) + RestoresDatabase.getMPMin( name ) ) / 2.0;
	}

	public static final String getHPRange( String name )
	{
		if ( name == null )
		{
			return null;
		}

		int hpMin = RestoresDatabase.getHPMin( name );
		int hpMax = RestoresDatabase.getHPMax( name );
		if ( hpMin == 0 && hpMax == 0 )
		{
			return null;
		}
		if ( hpMin == hpMax )
		{
			return Integer.toString( hpMin );
		}
		return ( Integer.toString( hpMin ) + "-" + Integer.toString( hpMax ) );
	}

	public static final String getMPRange( String name )
	{
		if ( name == null )
		{
			return null;
		}

		int mpMin = RestoresDatabase.getMPMin( name );
		int mpMax = RestoresDatabase.getMPMax( name );
		if ( mpMin == 0 && mpMax == 0 )
		{
			return null;
		}
		if ( mpMin == mpMax )
		{
			return Integer.toString( mpMin );
		}
		return ( Integer.toString( mpMin ) + "-" + Integer.toString( mpMax ) );
	}

	public static final int getAdvCost( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer advCost = RestoresDatabase.getAdvCost( name );
		if ( advCost == null )
		{
			return 0;
		}
		return ( (int) advCost );
	}

	public static final int getUsesLeft( String name )
	{
		if ( name == null )
		{
			return 0;
		}

		String usesLeft = RestoresDatabase.usesLeftByName.get( name );
		if ( usesLeft == null )
		{
			return 0;
		}
		if ( usesLeft.equals( "unlimited" ) )
		{
			return -1;
		}
		return (int) Math.floor( RestoresDatabase.getValue( usesLeft, name ) );
	}

	public static final String getNotes( String name )
	{
		if ( name == null )
		{
			return null;
		}

		String notes = RestoresDatabase.notesByName.get( name );
		if ( notes == null)
		{
			return "";
		}
		return notes;
	}
}
