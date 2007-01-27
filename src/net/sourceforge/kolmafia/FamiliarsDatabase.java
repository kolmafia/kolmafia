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
import java.io.PrintStream;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class FamiliarsDatabase extends KoLDatabase
{
	private static final String DEFAULT_ITEM = "steaming evil";
	private static final Integer DEFAULT_LARVA = new Integer( 666 );

	private static Map familiarById = new TreeMap();
	private static Map familiarByName = new TreeMap();
	private static Map familiarByLarva = new TreeMap();
	private static Map familiarByItem = new TreeMap();

	private static Map familiarItemById = new TreeMap();
	private static Map familiarLarvaById = new TreeMap();

	private static Map familiarImageById = new TreeMap();

	private static Map [] eventSkillByName = new TreeMap[4];

	static
	{
		for ( int i = 0; i < 4; ++i )
			eventSkillByName[i] = new TreeMap();

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = getReader( "familiars.txt" );

		String [] data;
		Integer familiarId, familiarLarva;
		String familiarName, familiarItemName;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 8 )
			{
				try
				{
					familiarId = Integer.valueOf( data[0] );
					familiarLarva = Integer.valueOf( data[1] );
					familiarName = getDisplayName( data[2] );
					familiarItemName = getDisplayName( data[3] );

					familiarById.put( familiarId, familiarName );
					familiarByName.put( getCanonicalName( data[2] ), familiarId );
					familiarByLarva.put( familiarLarva, familiarId );
					familiarByItem.put( getCanonicalName( data[3] ), familiarId );

					familiarItemById.put( familiarId, familiarItemName );
					familiarLarvaById.put( familiarId, familiarLarva );

					for ( int i = 0; i < 4; ++i )
						eventSkillByName[i].put( getCanonicalName( data[2] ), Integer.valueOf( data[i+4] ) );
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					printStackTrace( e );
				}
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

			printStackTrace( e );
		}
	}

	/**
	 * Temporarily adds a familiar to the familiar database.  This
	 * is used whenever KoLmafia encounters an unknown familiar on
	 * login and is designed to minimize crashing as a result.
	 */

	public static void registerFamiliar( int familiarId, String familiarName )
	{
		if ( familiarByName.containsKey( getCanonicalName( familiarName ) ) )
			return;

		KoLmafia.getDebugStream().println( "New familiar: \"" + familiarId + "\" (" + familiarName + ")" );

		// Because I'm intelligent, assume that both the familiar item
		// and the familiar larva are the steaming evil (for now).

		Integer dummyId = new Integer( familiarId );

		familiarById.put( dummyId, familiarName );
		familiarByName.put( getCanonicalName( familiarName ), dummyId );
		familiarByLarva.put( DEFAULT_LARVA, dummyId );
		familiarItemById.put( dummyId, DEFAULT_ITEM );
		familiarByItem.put( getCanonicalName( DEFAULT_ITEM ), dummyId );
	}

	/**
	 * Returns the name for an familiar, given its Id.
	 * @param	familiarId	The Id of the familiar to lookup
	 * @return	The name of the corresponding familiar
	 */

	public static final String getFamiliarName( int familiarId )
	{	return (String) familiarById.get( new Integer( familiarId ) );
	}

	/**
	 * Returns the Id number for an familiar, given its larval stage.
	 * @param	larvaId	The larva stage of the familiar to lookup
	 * @return	The Id number of the corresponding familiar
	 */

	public static final FamiliarData growFamiliarLarva( int larvaId )
	{
		Object familiarId = familiarByLarva.get( new Integer( larvaId ) );
		return familiarId == null ? null : new FamiliarData( ((Integer)familiarId).intValue() );
	}

	/**
	 * Returns the Id number for an familiar, given its name.
	 * @param	substring	The name of the familiar to lookup
	 * @return	The Id number of the corresponding familiar
	 */

	public static final int getFamiliarId( String substring )
	{
		String searchString = substring.toLowerCase();

		String [] familiarNames = new String[ familiarByName.size() ];
		familiarByName.keySet().toArray( familiarNames );

		for ( int i = 0; i < familiarNames.length; ++i )
		{
			if ( familiarNames[i].indexOf( searchString ) != -1 )
			{
				Object familiarId = familiarByName.get( familiarNames[i] );
				return familiarId == null ? -1 : ((Integer)familiarId).intValue();
			}
		}

		return -1;
	}

	public static final String getFamiliarItem( int familiarId )
	{	return (String) familiarItemById.get( new Integer( familiarId ) );
	}

	public static final int getFamiliarByItem( String item )
	{
		Object familiarId = familiarByItem.get( getCanonicalName( item ) );
		return familiarId == null ? -1 : ((Integer)familiarId).intValue();
	}

	public static void setFamiliarImageLocation( int familiarId, String location )
	{
		familiarImageById.put( new Integer( familiarId ), location );
	}

	public static String getFamiliarImageLocation( int familiarId )
	{
		String location = (String) familiarImageById.get( new Integer( familiarId ) );
		if ( location != null )
			return location;

		// If the HTML on the familiar page changes, then the map lookup
		// strategy will not work.  Rather than maintaining a database of
		// images, here, though, just return an unknown image.

		return "debug.gif";
	}

	private static void downloadFamiliarImage( int familiarId )
	{	RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/" + getFamiliarImageLocation( familiarId ) );
	}

	public static ImageIcon getFamiliarImage( int familiarId )
	{
		downloadFamiliarImage( familiarId );
		return JComponentUtilities.getImage( getFamiliarImageLocation( familiarId ) );
	}

	public static ImageIcon getFamiliarImage( String name )
	{	return getFamiliarImage( getFamiliarId( name ) );
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String familiarName )
	{	return familiarByName.containsKey( getCanonicalName( familiarName ) );
	}

	public static Integer getFamiliarSkill( String name, int event )
	{	return (Integer) eventSkillByName[ event - 1 ].get( getCanonicalName( name ) );
	}

	public static int [] getFamiliarSkills( int id )
	{
		String name = getCanonicalName( getFamiliarName( id ) );
		int skills [] = new int[4];
		for ( int i = 0; i < 4; ++i )
			skills[i] = ((Integer)eventSkillByName[i].get( name )).intValue();
		return skills;
	}

	public static void setFamiliarSkills( String name, int [] skills )
	{
		for ( int i = 0; i < 4; ++i )
			eventSkillByName[i].put( getCanonicalName ( name ), new Integer( skills[i] ) );

		// After familiar skills are reset, rewrite the data
		// file override.

		saveDataOverride();
	}

	/**
	 * Returns the set of familiars keyed by name
	 * @return	The set of familiars keyed by name
	 */

	public static Set entrySet()
	{	return familiarById.entrySet();
	}

	private static void saveDataOverride()
	{
		File output = new File( DATA_DIRECTORY, "familiars.txt" );
		PrintStream writer = LogStream.openStream( output, true );

		writer.println( "# Original familiar arena stats from Vladjimir's arena data" );
		writer.println( "# http://www.the-rye.dreamhosters.com/familiars/" );
		writer.println();

		Integer [] familiarIds = new Integer[ familiarById.size() ];
		familiarById.keySet().toArray( familiarIds );

		for ( int i = 0; i < familiarIds.length; ++i )
		{
			writer.print( familiarIds[i].intValue() );
			writer.print( "\t" );

			writer.print( familiarLarvaById.get( familiarIds[i] ) );
			writer.print( "\t" );

			writer.print( getFamiliarName( familiarIds[i].intValue() ) );
			writer.print( "\t" );

			writer.print( getFamiliarItem( familiarIds[i].intValue() ) );

			int [] skills = getFamiliarSkills( familiarIds[i].intValue() );
			for ( int j = 0; j < skills.length; ++j )
			{
				writer.print( "\t" );
				writer.print( skills[j] );
			}

			writer.println();
		}
	}
}
