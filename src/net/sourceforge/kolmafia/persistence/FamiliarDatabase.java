/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarDatabase
	extends KoLDatabase
{
	private static final String DEFAULT_ITEM = "steaming evil";
	private static final Integer DEFAULT_LARVA = new Integer( 666 );

	private static final Map familiarById = new TreeMap();
	private static final Map familiarByName = new TreeMap();
	private static final Map familiarByLarva = new TreeMap();
	private static final Map familiarByItem = new TreeMap();

	private static final Map familiarItemById = new TreeMap();
	private static final Map familiarLarvaById = new TreeMap();

	private static final Map familiarImageById = new TreeMap();

	private static final BooleanArray combatById = new BooleanArray();
	private static final BooleanArray volleyById = new BooleanArray();
	private static final BooleanArray sombreroById = new BooleanArray();
	private static final BooleanArray meatDropById = new BooleanArray();
	private static final BooleanArray fairyById = new BooleanArray();
	private static final BooleanArray puppyById = new BooleanArray();

	private static final Map[] eventSkillByName = new TreeMap[ 4 ];

	static
	{
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ] = new TreeMap();
		}

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = FileUtilities.getVersionedReader( "familiars.txt", KoLConstants.FAMILIARS_VERSION );

		String[] data;
		Integer familiarId, familiarLarva;
		String familiarName, familiarType, familiarItemName;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length != 9 )
			{
				continue;
			}

			try
			{
				familiarId = Integer.valueOf( data[ 0 ] );
				familiarName = StringUtilities.getDisplayName( data[ 1 ] );
				familiarType = data[ 2 ];
				familiarLarva = Integer.valueOf( data[ 3 ] );
				familiarItemName = StringUtilities.getDisplayName( data[ 4 ] );

				FamiliarDatabase.familiarById.put( familiarId, familiarName );
				FamiliarDatabase.familiarByName.put( StringUtilities.getCanonicalName( data[ 1 ] ), familiarId );
				FamiliarDatabase.familiarByLarva.put( familiarLarva, familiarId );
				FamiliarDatabase.familiarByItem.put( StringUtilities.getCanonicalName( data[ 4 ] ), familiarId );

				FamiliarDatabase.familiarItemById.put( familiarId, familiarItemName );
				FamiliarDatabase.familiarLarvaById.put( familiarId, familiarLarva );

				FamiliarDatabase.combatById.set( familiarId.intValue(), familiarType.indexOf( "combat" ) != -1 );
				FamiliarDatabase.volleyById.set( familiarId.intValue(), familiarType.indexOf( "stat0" ) != -1 );
				FamiliarDatabase.sombreroById.set( familiarId.intValue(), familiarType.indexOf( "stat1" ) != -1 );
				FamiliarDatabase.fairyById.set( familiarId.intValue(), familiarType.indexOf( "item0" ) != -1 );
				FamiliarDatabase.puppyById.set( familiarId.intValue(), familiarType.indexOf( "item1" ) != -1 );
				FamiliarDatabase.meatDropById.set( familiarId.intValue(), familiarType.indexOf( "meat0" ) != -1 );

				for ( int i = 0; i < 4; ++i )
				{
					FamiliarDatabase.eventSkillByName[ i ].put(
						StringUtilities.getCanonicalName( data[ 1 ] ), Integer.valueOf( data[ i + 5 ] ) );
				}
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Temporarily adds a familiar to the familiar database. This is used whenever KoLmafia encounters an unknown
	 * familiar on login and is designed to minimize crashing as a result.
	 */

	public static final void registerFamiliar( final int familiarId, final String familiarName )
	{
		if ( FamiliarDatabase.familiarByName.containsKey( StringUtilities.getCanonicalName( familiarName ) ) )
		{
			return;
		}

		RequestLogger.updateDebugLog( "New familiar: \"" + familiarId + "\" (" + familiarName + ")" );

		// Because I'm intelligent, assume that both the familiar item
		// and the familiar larva are the steaming evil (for now).

		Integer dummyId = new Integer( familiarId );

		FamiliarDatabase.familiarById.put( dummyId, familiarName );
		FamiliarDatabase.familiarByName.put( StringUtilities.getCanonicalName( familiarName ), dummyId );
		FamiliarDatabase.familiarByLarva.put( FamiliarDatabase.DEFAULT_LARVA, dummyId );
		FamiliarDatabase.familiarItemById.put( dummyId, FamiliarDatabase.DEFAULT_ITEM );
		FamiliarDatabase.familiarByItem.put( StringUtilities.getCanonicalName( FamiliarDatabase.DEFAULT_ITEM ), dummyId );
	}

	/**
	 * Returns the name for an familiar, given its Id.
	 *
	 * @param familiarId The Id of the familiar to lookup
	 * @return The name of the corresponding familiar
	 */

	public static final String getFamiliarName( final int familiarId )
	{
		return (String) FamiliarDatabase.familiarById.get( new Integer( familiarId ) );
	}

	/**
	 * Returns the Id number for an familiar, given its larval stage.
	 *
	 * @param larvaId The larva stage of the familiar to lookup
	 * @return The Id number of the corresponding familiar
	 */

	public static final FamiliarData growFamiliarLarva( final int larvaId )
	{
		Object familiarId = FamiliarDatabase.familiarByLarva.get( new Integer( larvaId ) );
		return familiarId == null ? null : new FamiliarData( ( (Integer) familiarId ).intValue() );
	}

	/**
	 * Returns the Id number for an familiar, given its name.
	 *
	 * @param substring The name of the familiar to lookup
	 * @return The Id number of the corresponding familiar
	 */

	public static final int getFamiliarId( final String substring )
	{
		String searchString = substring.toLowerCase();

		String[] familiarNames = new String[ FamiliarDatabase.familiarByName.size() ];
		FamiliarDatabase.familiarByName.keySet().toArray( familiarNames );

		for ( int i = 0; i < familiarNames.length; ++i )
		{
			if ( familiarNames[ i ].indexOf( searchString ) != -1 )
			{
				Object familiarId = FamiliarDatabase.familiarByName.get( familiarNames[ i ] );
				return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
			}
		}

		return -1;
	}

	public static final boolean isCombatType( final int familiarId )
	{
		return FamiliarDatabase.combatById.get( familiarId );
	}

	public static final boolean isVolleyType( final int familiarId )
	{
		return FamiliarDatabase.volleyById.get( familiarId );
	}

	public static final boolean isSombreroType( final int familiarId )
	{
		return FamiliarDatabase.sombreroById.get( familiarId );
	}

	public static final boolean isFairyType( final int familiarId )
	{
		return FamiliarDatabase.fairyById.get( familiarId );
	}

	public static final boolean isPuppyType( final int familiarId )
	{
		return FamiliarDatabase.puppyById.get( familiarId );
	}

	public static final boolean isMeatDropType( final int familiarId )
	{
		return FamiliarDatabase.meatDropById.get( familiarId );
	}

	public static final String getFamiliarItem( final int familiarId )
	{
		return (String) FamiliarDatabase.familiarItemById.get( new Integer( familiarId ) );
	}

	public static final int getFamiliarByItem( final String item )
	{
		Object familiarId = FamiliarDatabase.familiarByItem.get( StringUtilities.getCanonicalName( item ) );
		return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
	}

	public static final void setFamiliarImageLocation( final int familiarId, final String location )
	{
		FamiliarDatabase.familiarImageById.put( new Integer( familiarId ), location );
	}

	public static final String getFamiliarImageLocation( final int familiarId )
	{
		String location = (String) FamiliarDatabase.familiarImageById.get( new Integer( familiarId ) );
		if ( location != null )
		{
			return location;
		}

		// If the HTML on the familiar page changes, then the map lookup
		// strategy will not work.  Rather than maintaining a database of
		// images, here, though, just return an unknown image.

		return "debug.gif";
	}

	private static final void downloadFamiliarImage( final int familiarId )
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/" + FamiliarDatabase.getFamiliarImageLocation( familiarId ) );
	}

	public static final ImageIcon getFamiliarImage( final int familiarId )
	{
		FamiliarDatabase.downloadFamiliarImage( familiarId );
		return JComponentUtilities.getImage( FamiliarDatabase.getFamiliarImageLocation( familiarId ) );
	}

	public static final ImageIcon getFamiliarImage( final String name )
	{
		return FamiliarDatabase.getFamiliarImage( FamiliarDatabase.getFamiliarId( name ) );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String familiarName )
	{
		return FamiliarDatabase.familiarByName.containsKey( StringUtilities.getCanonicalName( familiarName ) );
	}

	public static final Integer getFamiliarSkill( final String name, final int event )
	{
		return (Integer) FamiliarDatabase.eventSkillByName[ event - 1 ].get( StringUtilities.getCanonicalName( name ) );
	}

	public static final int[] getFamiliarSkills( final int id )
	{
		String name = StringUtilities.getCanonicalName( FamiliarDatabase.getFamiliarName( id ) );
		int skills[] = new int[ 4 ];
		for ( int i = 0; i < 4; ++i )
		{
			skills[ i ] = ( (Integer) FamiliarDatabase.eventSkillByName[ i ].get( name ) ).intValue();
		}
		return skills;
	}

	public static final void setFamiliarSkills( final String name, final int[] skills )
	{
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ].put(
				StringUtilities.getCanonicalName( name ), new Integer( skills[ i ] ) );
		}

		// After familiar skills are reset, rewrite the data
		// file override.

		FamiliarDatabase.saveDataOverride();
	}

	/**
	 * Returns the set of familiars keyed by name
	 *
	 * @return The set of familiars keyed by name
	 */

	public static final Set entrySet()
	{
		return FamiliarDatabase.familiarById.entrySet();
	}

	private static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "familiars.txt" );
		LogStream writer = LogStream.openStream( output, true );

		writer.println( "# Original familiar arena stats from Vladjimir's arena data" );
		writer.println( "# http://www.the-rye.dreamhosters.com/familiars/" );
		writer.println();

		Integer[] familiarIds = new Integer[ FamiliarDatabase.familiarById.size() ];
		FamiliarDatabase.familiarById.keySet().toArray( familiarIds );

		for ( int i = 0; i < familiarIds.length; ++i )
		{
			writer.print( familiarIds[ i ].intValue() );
			writer.print( "\t" );

			writer.print( FamiliarDatabase.familiarLarvaById.get( familiarIds[ i ] ) );
			writer.print( "\t" );

			writer.print( FamiliarDatabase.getFamiliarName( familiarIds[ i ].intValue() ) );
			writer.print( "\t" );

			writer.print( FamiliarDatabase.getFamiliarItem( familiarIds[ i ].intValue() ) );

			int[] skills = FamiliarDatabase.getFamiliarSkills( familiarIds[ i ].intValue() );
			for ( int j = 0; j < skills.length; ++j )
			{
				writer.print( "\t" );
				writer.print( skills[ j ] );
			}

			writer.println();
		}
	}
}
