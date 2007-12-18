/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class FamiliarsDatabase
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
	private static final BooleanArray itemDropById = new BooleanArray();

	private static final Map[] eventSkillByName = new TreeMap[ 4 ];

	static
	{
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarsDatabase.eventSkillByName[ i ] = new TreeMap();
		}

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = KoLDatabase.getVersionedReader( "familiars.txt", KoLConstants.FAMILIARS_VERSION );

		String[] data;
		Integer familiarId, familiarLarva;
		String familiarName, familiarType, familiarItemName;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length != 9 )
			{
				continue;
			}

			try
			{
				familiarId = Integer.valueOf( data[ 0 ] );
				familiarName = KoLDatabase.getDisplayName( data[ 1 ] );
				familiarType = data[ 2 ];
				familiarLarva = Integer.valueOf( data[ 3 ] );
				familiarItemName = KoLDatabase.getDisplayName( data[ 4 ] );

				FamiliarsDatabase.familiarById.put( familiarId, familiarName );
				FamiliarsDatabase.familiarByName.put( KoLDatabase.getCanonicalName( data[ 1 ] ), familiarId );
				FamiliarsDatabase.familiarByLarva.put( familiarLarva, familiarId );
				FamiliarsDatabase.familiarByItem.put( KoLDatabase.getCanonicalName( data[ 4 ] ), familiarId );

				FamiliarsDatabase.familiarItemById.put( familiarId, familiarItemName );
				FamiliarsDatabase.familiarLarvaById.put( familiarId, familiarLarva );

				FamiliarsDatabase.combatById.set( familiarId.intValue(), familiarType.indexOf( "combat" ) != -1 );
				FamiliarsDatabase.volleyById.set( familiarId.intValue(), familiarType.indexOf( "stat0" ) != -1 );
				FamiliarsDatabase.sombreroById.set( familiarId.intValue(), familiarType.indexOf( "stat1" ) != -1 );
				FamiliarsDatabase.itemDropById.set( familiarId.intValue(), familiarType.indexOf( "item0" ) != -1 );
				FamiliarsDatabase.meatDropById.set( familiarId.intValue(), familiarType.indexOf( "meat0" ) != -1 );

				for ( int i = 0; i < 4; ++i )
				{
					FamiliarsDatabase.eventSkillByName[ i ].put(
						KoLDatabase.getCanonicalName( data[ 1 ] ), Integer.valueOf( data[ i + 5 ] ) );
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
		if ( FamiliarsDatabase.familiarByName.containsKey( KoLDatabase.getCanonicalName( familiarName ) ) )
		{
			return;
		}

		RequestLogger.updateDebugLog( "New familiar: \"" + familiarId + "\" (" + familiarName + ")" );

		// Because I'm intelligent, assume that both the familiar item
		// and the familiar larva are the steaming evil (for now).

		Integer dummyId = new Integer( familiarId );

		FamiliarsDatabase.familiarById.put( dummyId, familiarName );
		FamiliarsDatabase.familiarByName.put( KoLDatabase.getCanonicalName( familiarName ), dummyId );
		FamiliarsDatabase.familiarByLarva.put( FamiliarsDatabase.DEFAULT_LARVA, dummyId );
		FamiliarsDatabase.familiarItemById.put( dummyId, FamiliarsDatabase.DEFAULT_ITEM );
		FamiliarsDatabase.familiarByItem.put( KoLDatabase.getCanonicalName( FamiliarsDatabase.DEFAULT_ITEM ), dummyId );
	}

	/**
	 * Returns the name for an familiar, given its Id.
	 * 
	 * @param familiarId The Id of the familiar to lookup
	 * @return The name of the corresponding familiar
	 */

	public static final String getFamiliarName( final int familiarId )
	{
		return (String) FamiliarsDatabase.familiarById.get( new Integer( familiarId ) );
	}

	/**
	 * Returns the Id number for an familiar, given its larval stage.
	 * 
	 * @param larvaId The larva stage of the familiar to lookup
	 * @return The Id number of the corresponding familiar
	 */

	public static final FamiliarData growFamiliarLarva( final int larvaId )
	{
		Object familiarId = FamiliarsDatabase.familiarByLarva.get( new Integer( larvaId ) );
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

		String[] familiarNames = new String[ FamiliarsDatabase.familiarByName.size() ];
		FamiliarsDatabase.familiarByName.keySet().toArray( familiarNames );

		for ( int i = 0; i < familiarNames.length; ++i )
		{
			if ( familiarNames[ i ].indexOf( searchString ) != -1 )
			{
				Object familiarId = FamiliarsDatabase.familiarByName.get( familiarNames[ i ] );
				return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
			}
		}

		return -1;
	}

	public static final boolean isCombatType( final int familiarId )
	{
		return FamiliarsDatabase.combatById.get( familiarId );
	}

	public static final boolean isVolleyType( final int familiarId )
	{
		return FamiliarsDatabase.volleyById.get( familiarId );
	}

	public static final boolean isSombreroType( final int familiarId )
	{
		return FamiliarsDatabase.sombreroById.get( familiarId );
	}

	public static final boolean isItemDropType( final int familiarId )
	{
		return FamiliarsDatabase.itemDropById.get( familiarId );
	}

	public static final boolean isMeatDropType( final int familiarId )
	{
		return FamiliarsDatabase.meatDropById.get( familiarId );
	}

	public static final String getFamiliarItem( final int familiarId )
	{
		return (String) FamiliarsDatabase.familiarItemById.get( new Integer( familiarId ) );
	}

	public static final int getFamiliarByItem( final String item )
	{
		Object familiarId = FamiliarsDatabase.familiarByItem.get( KoLDatabase.getCanonicalName( item ) );
		return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
	}

	public static final void setFamiliarImageLocation( final int familiarId, final String location )
	{
		FamiliarsDatabase.familiarImageById.put( new Integer( familiarId ), location );
	}

	public static final String getFamiliarImageLocation( final int familiarId )
	{
		String location = (String) FamiliarsDatabase.familiarImageById.get( new Integer( familiarId ) );
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
		RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/" + FamiliarsDatabase.getFamiliarImageLocation( familiarId ) );
	}

	public static final ImageIcon getFamiliarImage( final int familiarId )
	{
		FamiliarsDatabase.downloadFamiliarImage( familiarId );
		return JComponentUtilities.getImage( FamiliarsDatabase.getFamiliarImageLocation( familiarId ) );
	}

	public static final ImageIcon getFamiliarImage( final String name )
	{
		return FamiliarsDatabase.getFamiliarImage( FamiliarsDatabase.getFamiliarId( name ) );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 * 
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String familiarName )
	{
		return FamiliarsDatabase.familiarByName.containsKey( KoLDatabase.getCanonicalName( familiarName ) );
	}

	public static final Integer getFamiliarSkill( final String name, final int event )
	{
		return (Integer) FamiliarsDatabase.eventSkillByName[ event - 1 ].get( KoLDatabase.getCanonicalName( name ) );
	}

	public static final int[] getFamiliarSkills( final int id )
	{
		String name = KoLDatabase.getCanonicalName( FamiliarsDatabase.getFamiliarName( id ) );
		int skills[] = new int[ 4 ];
		for ( int i = 0; i < 4; ++i )
		{
			skills[ i ] = ( (Integer) FamiliarsDatabase.eventSkillByName[ i ].get( name ) ).intValue();
		}
		return skills;
	}

	public static final void setFamiliarSkills( final String name, final int[] skills )
	{
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarsDatabase.eventSkillByName[ i ].put(
				KoLDatabase.getCanonicalName( name ), new Integer( skills[ i ] ) );
		}

		// After familiar skills are reset, rewrite the data
		// file override.

		FamiliarsDatabase.saveDataOverride();
	}

	/**
	 * Returns the set of familiars keyed by name
	 * 
	 * @return The set of familiars keyed by name
	 */

	public static final Set entrySet()
	{
		return FamiliarsDatabase.familiarById.entrySet();
	}

	private static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "familiars.txt" );
		LogStream writer = LogStream.openStream( output, true );

		writer.println( "# Original familiar arena stats from Vladjimir's arena data" );
		writer.println( "# http://www.the-rye.dreamhosters.com/familiars/" );
		writer.println();

		Integer[] familiarIds = new Integer[ FamiliarsDatabase.familiarById.size() ];
		FamiliarsDatabase.familiarById.keySet().toArray( familiarIds );

		for ( int i = 0; i < familiarIds.length; ++i )
		{
			writer.print( familiarIds[ i ].intValue() );
			writer.print( "\t" );

			writer.print( FamiliarsDatabase.familiarLarvaById.get( familiarIds[ i ] ) );
			writer.print( "\t" );

			writer.print( FamiliarsDatabase.getFamiliarName( familiarIds[ i ].intValue() ) );
			writer.print( "\t" );

			writer.print( FamiliarsDatabase.getFamiliarItem( familiarIds[ i ].intValue() ) );

			int[] skills = FamiliarsDatabase.getFamiliarSkills( familiarIds[ i ].intValue() );
			for ( int j = 0; j < skills.length; ++j )
			{
				writer.print( "\t" );
				writer.print( skills[ j ] );
			}

			writer.println();
		}
	}
}
