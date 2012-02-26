/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarDatabase
	extends KoLDatabase
{
	private static final Map familiarById = new TreeMap();
	private static final Map familiarByName = new TreeMap();

	private static final Map familiarItemById = new HashMap();
	private static final Map familiarByItem = new HashMap();

	private static final Map familiarLarvaById = new HashMap();
	private static final Map familiarByLarva = new HashMap();

	private static final Map familiarImageById = new HashMap();
	private static final Map familiarByImage = new HashMap();

	private static final BooleanArray combatById = new BooleanArray();
	private static final BooleanArray volleyById = new BooleanArray();
	private static final BooleanArray sombreroById = new BooleanArray();
	private static final BooleanArray meatDropById = new BooleanArray();
	private static final BooleanArray fairyById = new BooleanArray();

	private static final Map[] eventSkillByName = new HashMap[ 4 ];

	public static boolean newFamiliars = false;
	public static int maxFamiliarId = 0;

	static
	{
		FamiliarDatabase.newFamiliars = false;
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ] = new HashMap();
		}

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = FileUtilities.getVersionedReader( "familiars.txt", KoLConstants.FAMILIARS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length != 10 )
			{
				continue;
			}

			try
			{
				int id = StringUtilities.parseInt( data[ 0 ] );
				if ( id > FamiliarDatabase.maxFamiliarId )
				{
					FamiliarDatabase.maxFamiliarId = id;
				}

				Integer familiarId = IntegerPool.get( id );
				String familiarName = StringUtilities.getDisplayName( data[ 1 ] );
				String familiarImage = new String( data[ 2 ] );
				String familiarType = new String( data[ 3 ] );
				Integer familiarLarva = Integer.valueOf( data[ 4 ] );
				String familiarItemName = StringUtilities.getDisplayName( data[ 5 ] );

				FamiliarDatabase.familiarById.put( familiarId, familiarName );
				FamiliarDatabase.familiarByName.put( StringUtilities.getCanonicalName( data[ 1 ] ), familiarId );

				FamiliarDatabase.familiarImageById.put( familiarId, familiarImage );
				FamiliarDatabase.familiarByImage.put( familiarImage, familiarId );
				
				FamiliarDatabase.familiarLarvaById.put( familiarId, familiarLarva );
				FamiliarDatabase.familiarByLarva.put( familiarLarva, familiarId );

				FamiliarDatabase.familiarItemById.put( familiarId, familiarItemName );
				FamiliarDatabase.familiarByItem.put( StringUtilities.getCanonicalName( data[ 5 ] ), familiarId );


				FamiliarDatabase.combatById.set( familiarId.intValue(), familiarType.indexOf( "combat" ) != -1 );
				FamiliarDatabase.volleyById.set( familiarId.intValue(), familiarType.indexOf( "stat0" ) != -1 );
				FamiliarDatabase.sombreroById.set( familiarId.intValue(), familiarType.indexOf( "stat1" ) != -1 );
				FamiliarDatabase.fairyById.set( familiarId.intValue(), familiarType.indexOf( "item0" ) != -1 );
				FamiliarDatabase.meatDropById.set( familiarId.intValue(), familiarType.indexOf( "meat0" ) != -1 );

				String canonical = StringUtilities.getCanonicalName( data[ 1 ] );
				for ( int i = 0; i < 4; ++i )
				{
					FamiliarDatabase.eventSkillByName[ i ].put( canonical, Integer.valueOf( data[ i + 6 ] ) );
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
	 * Temporarily adds a familiar to the familiar database. This is used
	 * whenever KoLmafia encounters an unknown familiar on login
	 */

	private static Integer ZERO = IntegerPool.get( 0 );
	public static final void registerFamiliar( final int familiarId, final String familiarName, final String image )
	{
		FamiliarDatabase.registerFamiliar( familiarId, familiarName, image, FamiliarDatabase.ZERO );
	}

	// Hatches into:</b><br><table cellpadding=5 style='border: 1px solid black;'><tr><td align=center><a class=nounder href=desc_familiar.php?which=154><img border=0 src=http://images.kingdomofloathing.com/itemimages/groose.gif width=30 height=30><br><b>Bloovian Groose</b></a></td></tr></table>

	private static Pattern FAMILIAR_PATTERN = Pattern.compile( "Hatches into:.*?<table.*?which=(\\d*).*?itemimages/(.*?) .*?<b>(.*?)</b>");

	public static final void registerFamiliar( final Integer larvaId, final String text )
	{
		Matcher matcher = FAMILIAR_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			int familiarId = StringUtilities.parseInt( matcher.group( 1 ) );
			String image = matcher.group( 2 );
			String familiarName = matcher.group( 3 );
			FamiliarDatabase.registerFamiliar( familiarId, familiarName, image, larvaId );
		}
	}

	public static final void registerFamiliar( final int familiarId, final String familiarName, final String image, final Integer larvaId )
	{
		String canon = StringUtilities.getCanonicalName( familiarName );
		if ( FamiliarDatabase.familiarByName.containsKey( canon ) )
		{
			return;
		}

		RequestLogger.printLine( "New familiar: \"" + familiarName + "\" (" + familiarId + ") @ " + image );

		if ( familiarId > FamiliarDatabase.maxFamiliarId )
		{
			FamiliarDatabase.maxFamiliarId = familiarId;
		}

		Integer dummyId = IntegerPool.get( familiarId );

		FamiliarDatabase.familiarById.put( dummyId, familiarName );
		FamiliarDatabase.familiarByName.put( canon, dummyId );
		FamiliarDatabase.familiarImageById.put( dummyId, image );
		FamiliarDatabase.familiarByImage.put( image, dummyId );
		FamiliarDatabase.familiarLarvaById.put( dummyId, larvaId );
		FamiliarDatabase.familiarByLarva.put( larvaId, dummyId );
		FamiliarDatabase.familiarItemById.put( dummyId, "" );
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ].put( canon, FamiliarDatabase.ZERO );
		}
		FamiliarDatabase.newFamiliars = true;
	}

	/**
	 * Returns the name for an familiar, given its Id.
	 *
	 * @param familiarId The Id of the familiar to lookup
	 * @return The name of the corresponding familiar
	 */

	public static final String getFamiliarName( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarName( IntegerPool.get( familiarId ) );
	}

	public static final String getFamiliarName( final Integer familiarId )
	{
		return (String) FamiliarDatabase.familiarById.get( familiarId );
	}

	/**
	 * Returns the Id number for an familiar, given its larval stage.
	 *
	 * @param larvaId The larva stage of the familiar to lookup
	 * @return The Id number of the corresponding familiar
	 */

	public static final FamiliarData growFamiliarLarva( final int larvaId )
	{
		Object familiarId = FamiliarDatabase.familiarByLarva.get( IntegerPool.get( larvaId ) );
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
		Object familiarId = FamiliarDatabase.familiarByName.get( searchString );
		if ( familiarId != null )
		{
			return ( (Integer) familiarId ).intValue();
		}

		String[] familiarNames = new String[ FamiliarDatabase.familiarByName.size() ];
		FamiliarDatabase.familiarByName.keySet().toArray( familiarNames );

		for ( int i = 0; i < familiarNames.length; ++i )
		{
			if ( familiarNames[ i ].indexOf( searchString ) != -1 )
			{
				familiarId = FamiliarDatabase.familiarByName.get( familiarNames[ i ] );
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

	public static final boolean isMeatDropType( final int familiarId )
	{
		return FamiliarDatabase.meatDropById.get( familiarId );
	}

	public static final String getFamiliarItem( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarItem( IntegerPool.get( familiarId ) );
	}

	public static final String getFamiliarItem( final Integer familiarId )
	{
		return (String) FamiliarDatabase.familiarItemById.get( familiarId );
	}

	public static final int getFamiliarItemId( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarItemId( IntegerPool.get( familiarId ) );
	}

	public static final int getFamiliarItemId( final Integer familiarId )
	{
		String name = FamiliarDatabase.getFamiliarItem( familiarId );
		return name == null ? -1 : ItemDatabase.getItemId( name );
	}

	public static final int getFamiliarByItem( final String item )
	{
		Object familiarId = FamiliarDatabase.familiarByItem.get( StringUtilities.getCanonicalName( item ) );
		return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
	}

	public static final int getFamiliarLarva( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarLarva( IntegerPool.get( familiarId ) );
	}

	public static final int getFamiliarLarva( final Integer familiarId )
	{
		Integer id = (Integer) FamiliarDatabase.familiarLarvaById.get( familiarId );
		return id == null ? 0 : id.intValue();
	}

	public static final String getFamiliarType( final int familiarId )
	{
		StringBuffer buffer = new StringBuffer();
		String sep = "";
		if ( FamiliarDatabase.combatById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "combat" );
		}
		if ( FamiliarDatabase.volleyById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "stat0" );
		}
		if ( FamiliarDatabase.sombreroById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "stat1" );
		}
		if ( FamiliarDatabase.fairyById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "item0" );
		}
		if ( FamiliarDatabase.meatDropById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "meat0" );
		}
		if ( sep.equals( "" )  )
		{
			buffer.append( "none" );
		}
		return buffer.toString();
	}

	public static final void setFamiliarImageLocation( final int familiarId, final String location )
	{
		FamiliarDatabase.familiarImageById.put( IntegerPool.get( familiarId ), location );
	}

	public static final String getFamiliarImageLocation( final int familiarId )
	{
		String location = (String) FamiliarDatabase.familiarImageById.get( IntegerPool.get( familiarId ) );
		return ( location != null ) ? location : "debug.gif";
	}

	public static final int getFamiliarByImageLocation( final String image )
	{
		Object familiarId = FamiliarDatabase.familiarByImage.get( image );
		return familiarId == null ? -1 : ( (Integer) familiarId ).intValue();
	}

	private static final void downloadFamiliarImage( final int familiarId )
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + FamiliarDatabase.getFamiliarImageLocation( familiarId ) );
	}

	public static final ImageIcon getFamiliarImage( final int familiarId )
	{
		FamiliarDatabase.downloadFamiliarImage( familiarId );
		return JComponentUtilities.getImage( "itemimages/" + FamiliarDatabase.getFamiliarImageLocation( familiarId ) );
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
		return FamiliarDatabase.getFamiliarSkills( IntegerPool.get( id ) );
	}

	public static final int[] getFamiliarSkills( final Integer id )
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
		String canon = StringUtilities.getCanonicalName( name );
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ].put( canon, IntegerPool.get( skills[ i ] ) );
		}
		FamiliarDatabase.newFamiliars = true;
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

	public static final void saveDataOverride()
	{
		FamiliarDatabase.writeFamiliars( new File( UtilityConstants.DATA_LOCATION, "familiars.txt" ) );
		FamiliarDatabase.newFamiliars = false;
	}

	public static void writeFamiliars( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.FAMILIARS_VERSION );

		writer.println( "# Original familiar arena stats from Vladjimir's arena data" );
		writer.println( "# http://www.therye.org/familiars/" );
		writer.println();
		writer.println( "# no.	name	image	type	larva	item	CM	SH	OC	H&S" );
		writer.println();

		Integer[] familiarIds = new Integer[ FamiliarDatabase.familiarById.size() ];
		FamiliarDatabase.familiarById.keySet().toArray( familiarIds );

		int lastInteger = 1;
		for ( int i = 0; i < familiarIds.length; ++i )
		{
			Integer nextInteger = familiarIds[ i ];
			int familiarId = nextInteger.intValue();

			for ( int j = lastInteger; j < familiarId; ++j )
			{
				writer.println( j );
			}

			lastInteger = familiarId + 1;

			String name = FamiliarDatabase.getFamiliarName( nextInteger );
			String image = FamiliarDatabase.getFamiliarImageLocation( familiarId );
			String type = FamiliarDatabase.getFamiliarType( familiarId );
			int larva = FamiliarDatabase.getFamiliarLarva( nextInteger ) ;
			int itemId = FamiliarDatabase.getFamiliarItemId( nextInteger );
			int[] skills = FamiliarDatabase.getFamiliarSkills( nextInteger );

			FamiliarDatabase.writeFamiliar( writer, familiarId, name, image, type, larva, itemId, skills );
		}
	}

	public static void writeFamiliar( final PrintStream writer,
					  final int familiarId, final String name, final String image,
					  final String type, final int larva, final int itemId, final int [] skills )
	{
		writer.println( FamiliarDatabase.familiarString( familiarId, name, image, type, larva, itemId, skills ) );
	}

	public static String familiarString( final int familiarId, final String name, final String image,
					     final String type, final int larva, final int itemId, final int [] skills )
	{
		String item = itemId == -1 ? "" : ItemDatabase.getItemDataName( itemId );
		return familiarId + "\t" +
		       name + "\t" +
		       image + "\t" +
		       type + "\t" +
		       larva + "\t" +
		       item + "\t" +
		       skills[0] + "\t" +
		       skills[1] + "\t" +
		       skills[2] + "\t" +
		       skills[3];
	}
}
