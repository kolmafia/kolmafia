/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarDatabase
{
	private static final Map<Integer,String> familiarById = new TreeMap<Integer,String>();
	private static final Map<String,Integer> familiarByName = new TreeMap<String,Integer>();

	private static final Map<Integer, String> familiarItemById = new HashMap<Integer,String>();
	private static final Map<String, Integer> familiarByItem = new HashMap<String,Integer>();

	private static final Map<Integer, Integer> familiarLarvaById = new HashMap<Integer,Integer>();
	private static final Map<Integer, Integer> familiarByLarva = new HashMap<Integer,Integer>();

	private static final Map<Integer,String> familiarImageById = new HashMap<Integer,String>();
	private static final Map<String,Integer> familiarByImage = new HashMap<String,Integer>();

	private static final BooleanArray volleyById = new BooleanArray();
	private static final BooleanArray sombreroById = new BooleanArray();
	private static final BooleanArray meatDropById = new BooleanArray();
	private static final BooleanArray fairyById = new BooleanArray();

	private static final BooleanArray combat0ById = new BooleanArray();
	private static final BooleanArray combat1ById = new BooleanArray();
	private static final BooleanArray blockById = new BooleanArray();
	private static final BooleanArray delevelById = new BooleanArray();
	private static final BooleanArray meat1ById = new BooleanArray();
	private static final BooleanArray stat2ById = new BooleanArray();
	private static final BooleanArray hp0ById = new BooleanArray();
	private static final BooleanArray mp0ById = new BooleanArray();
	private static final BooleanArray other0ById = new BooleanArray();

	private static final BooleanArray hp1ById = new BooleanArray();
	private static final BooleanArray mp1ById = new BooleanArray();
	private static final BooleanArray stat3ById = new BooleanArray();
	private static final BooleanArray other1ById = new BooleanArray();

	private static final BooleanArray passiveById = new BooleanArray();
	private static final BooleanArray dropById = new BooleanArray();
	private static final BooleanArray underwaterById = new BooleanArray();

	private static final BooleanArray noneById = new BooleanArray();
	private static final BooleanArray variableById = new BooleanArray();

	private static final Map<String,Integer>[] eventSkillByName = new HashMap[ 4 ];

	public static boolean newFamiliars = false;
	public static int maxFamiliarId = 0;

	static
	{
		FamiliarDatabase.newFamiliars = false;
		for ( int i = 0; i < 4; ++i )
		{
			FamiliarDatabase.eventSkillByName[ i ] = new HashMap<String,Integer>();
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
				String familiarName = new String( data[ 1 ] );
				String familiarImage = new String( data[ 2 ] );
				String familiarType = new String( data[ 3 ] );
				String familiarLarvaName = new String( data[ 4 ] );
				Integer familiarLarva = Integer.valueOf( ItemDatabase.getItemId( familiarLarvaName ) );
				String familiarItemName = new String( data[ 5 ] );

				FamiliarDatabase.familiarById.put( familiarId, StringUtilities.getDisplayName( familiarName ) );
				FamiliarDatabase.familiarByName.put( StringUtilities.getCanonicalName( familiarName ), familiarId );

				FamiliarDatabase.familiarImageById.put( familiarId, familiarImage );
				FamiliarDatabase.familiarByImage.put( familiarImage, familiarId );

				// Kludge: Happy Medium has 4 different images
				if ( id == FamiliarPool.HAPPY_MEDIUM )
				{
					FamiliarDatabase.familiarByImage.put( "medium_1.gif", familiarId );
					FamiliarDatabase.familiarByImage.put( "medium_2.gif", familiarId );
					FamiliarDatabase.familiarByImage.put( "medium_3.gif", familiarId );
				}
				
				FamiliarDatabase.familiarLarvaById.put( familiarId, familiarLarva );
				FamiliarDatabase.familiarByLarva.put( familiarLarva, familiarId );

				FamiliarDatabase.familiarItemById.put( familiarId, familiarItemName );
				FamiliarDatabase.familiarByItem.put( familiarItemName, familiarId );

				FamiliarDatabase.volleyById.set( familiarId.intValue(), familiarType.contains( "stat0" ) );
				FamiliarDatabase.sombreroById.set( familiarId.intValue(), familiarType.contains( "stat1" ) );
				FamiliarDatabase.fairyById.set( familiarId.intValue(), familiarType.contains( "item0" ) );
				FamiliarDatabase.meatDropById.set( familiarId.intValue(), familiarType.contains( "meat0" ) );

				// The following are "combat" abilities
				FamiliarDatabase.combat0ById.set( familiarId.intValue(), familiarType.contains( "combat0" ) );
				FamiliarDatabase.combat1ById.set( familiarId.intValue(), familiarType.contains( "combat1" ) );
				FamiliarDatabase.blockById.set( familiarId.intValue(), familiarType.contains( "block" ) );
				FamiliarDatabase.delevelById.set( familiarId.intValue(), familiarType.contains( "delevel" ) );
				FamiliarDatabase.hp0ById.set( familiarId.intValue(), familiarType.contains( "hp0" ) );
				FamiliarDatabase.mp0ById.set( familiarId.intValue(), familiarType.contains( "mp0" ) );
				FamiliarDatabase.meat1ById.set( familiarId.intValue(), familiarType.contains( "meat1" ) );
				FamiliarDatabase.stat2ById.set( familiarId.intValue(), familiarType.contains( "stat2" ) );
				FamiliarDatabase.other0ById.set( familiarId.intValue(), familiarType.contains( "other0" ) );

				// The following are "after combat" abilities
				FamiliarDatabase.hp1ById.set( familiarId.intValue(), familiarType.contains( "hp1" ) );
				FamiliarDatabase.mp1ById.set( familiarId.intValue(), familiarType.contains( "mp1" ) );
				FamiliarDatabase.stat3ById.set( familiarId.intValue(), familiarType.contains( "stat3" ) );
				FamiliarDatabase.other1ById.set( familiarId.intValue(), familiarType.contains( "other1" ) );

				// The following are other abilities that deserve their own category
				FamiliarDatabase.passiveById.set( familiarId.intValue(), familiarType.contains( "passive" ) );
				FamiliarDatabase.dropById.set( familiarId.intValue(), familiarType.contains( "drop" ) );
				FamiliarDatabase.underwaterById.set( familiarId.intValue(), familiarType.contains( "underwater" ) );

				FamiliarDatabase.noneById.set( familiarId.intValue(), familiarType.contains( "none" ) );
				FamiliarDatabase.variableById.set( familiarId.intValue(), familiarType.contains( "variable" ) );

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

	private static final Integer ZERO = IntegerPool.get( 0 );
	public static final void registerFamiliar( final int familiarId, final String familiarName, final String image )
	{
		FamiliarDatabase.registerFamiliar( familiarId, familiarName, image, FamiliarDatabase.ZERO );
	}

	// Hatches into:</b><br><table cellpadding=5 style='border: 1px solid black;'><tr><td align=center><a class=nounder href=desc_familiar.php?which=154><img border=0 src=http://images.kingdomofloathing.com/itemimages/groose.gif width=30 height=30><br><b>Bloovian Groose</b></a></td></tr></table>

	private static final Pattern FAMILIAR_PATTERN = Pattern.compile( "Hatches into:.*?<table.*?which=(\\d*).*?itemimages/(.*?) .*?<b>(.*?)</b>");

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
		Integer familiarId = FamiliarDatabase.familiarByLarva.get( IntegerPool.get( larvaId ) );
		return familiarId == null ? null : new FamiliarData( familiarId.intValue() );
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

	public static final boolean isCombatType( final int familiarId )
	{
		return  FamiliarDatabase.combat0ById.get( familiarId ) ||
			FamiliarDatabase.combat1ById.get( familiarId ) ||
			FamiliarDatabase.blockById.get( familiarId ) ||
			FamiliarDatabase.delevelById.get( familiarId ) ||
			FamiliarDatabase.hp0ById.get( familiarId ) ||
			FamiliarDatabase.mp0ById.get( familiarId ) ||
			FamiliarDatabase.other0ById.get( familiarId );
	}

	public static final boolean isCombat0Type( final int familiarId )
	{
		return  FamiliarDatabase.combat0ById.get( familiarId );
	}

	public static final boolean isCombat1Type( final int familiarId )
	{
		return  FamiliarDatabase.combat1ById.get( familiarId );
	}

	public static final boolean isDropType( final int familiarId )
	{
		return  FamiliarDatabase.dropById.get( familiarId );
	}

	public static final boolean isBlockType( final int familiarId )
	{
		return  FamiliarDatabase.blockById.get( familiarId );
	}

	public static final boolean isDelevelType( final int familiarId )
	{
		return  FamiliarDatabase.delevelById.get( familiarId );
	}

	public static final boolean isHp0Type( final int familiarId )
	{
		return  FamiliarDatabase.hp0ById.get( familiarId );
	}

	public static final boolean isMp0Type( final int familiarId )
	{
		return  FamiliarDatabase.mp0ById.get( familiarId );
	}

	public static final boolean isMeat1Type( final int familiarId )
	{
		return  FamiliarDatabase.meat1ById.get( familiarId );
	}

	public static final boolean isStat2Type( final int familiarId )
	{
		return  FamiliarDatabase.stat2ById.get( familiarId );
	}

	public static final boolean isOther0Type( final int familiarId )
	{
		return  FamiliarDatabase.other0ById.get( familiarId );
	}

	public static final boolean isHp1Type( final int familiarId )
	{
		return  FamiliarDatabase.hp1ById.get( familiarId );
	}

	public static final boolean isMp1Type( final int familiarId )
	{
		return  FamiliarDatabase.mp1ById.get( familiarId );
	}

	public static final boolean isStat3Type( final int familiarId )
	{
		return  FamiliarDatabase.stat3ById.get( familiarId );
	}

	public static final boolean isNoneType( final int familiarId )
	{
		return  FamiliarDatabase.noneById.get( familiarId );
	}

	public static final boolean isOther1Type( final int familiarId )
	{
		return  FamiliarDatabase.other1ById.get( familiarId );
	}

	public static final boolean isPassiveType( final int familiarId )
	{
		return  FamiliarDatabase.passiveById.get( familiarId );
	}

	public static final boolean isUnderwaterType( final int familiarId )
	{
		return  FamiliarDatabase.underwaterById.get( familiarId );
	}

	public static final boolean isVariableType( final int familiarId )
	{
		return  FamiliarDatabase.variableById.get( familiarId );
	}

	public static final String getFamiliarItem( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarItem( IntegerPool.get( familiarId ) );
	}

	public static final String getFamiliarItem( final Integer familiarId )
	{
		return FamiliarDatabase.familiarItemById.get( familiarId );
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
		Integer familiarId = FamiliarDatabase.familiarByItem.get( StringUtilities.getCanonicalName( item ) );
		return familiarId == null ? -1 : familiarId.intValue();
	}

	public static final int getFamiliarLarva( final int familiarId )
	{
		return FamiliarDatabase.getFamiliarLarva( IntegerPool.get( familiarId ) );
	}

	public static final int getFamiliarLarva( final Integer familiarId )
	{
		Integer id = FamiliarDatabase.familiarLarvaById.get( familiarId );
		return id == null ? 0 : id.intValue();
	}

	public static final String getFamiliarType( final int familiarId )
	{
		StringBuilder buffer = new StringBuilder();
		String sep = "";

		// Base types: Leprechaun, Fairy, Volleyball, Sombrero
		if ( FamiliarDatabase.meatDropById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "meat0" );
		}
		if ( FamiliarDatabase.fairyById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "item0" );
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

		// Combat abilities
		if ( FamiliarDatabase.combat0ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "combat0" );
		}
		if ( FamiliarDatabase.combat1ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "combat1" );
		}
		if ( FamiliarDatabase.blockById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "block" );
		}
		if ( FamiliarDatabase.delevelById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "delevel" );
		}
		if ( FamiliarDatabase.hp0ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "hp0" );
		}
		if ( FamiliarDatabase.mp0ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "mp0" );
		}
		if ( FamiliarDatabase.other0ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "other0" );
		}
		if ( FamiliarDatabase.meat1ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "meat1" );
		}
		if ( FamiliarDatabase.stat2ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "stat2" );
		}

		// After Combat abilities
		if ( FamiliarDatabase.hp1ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "hp1" );
		}
		if ( FamiliarDatabase.mp1ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "mp1" );
		}
		if ( FamiliarDatabase.other1ById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "other1" );
		}

		if ( FamiliarDatabase.passiveById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "passive" );
		}
		if ( FamiliarDatabase.underwaterById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "underwater" );
		}

		if ( FamiliarDatabase.variableById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "variable" );
		}

		// Special items
		if ( FamiliarDatabase.dropById.get( familiarId ) )
		{
			buffer.append( sep );
			sep = ",";
			buffer.append( "drop" );
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

	private static final ImageIcon getFamiliarIcon( final String location )
	{
		if ( location == null || location.equals( "debug.gif" ) )
		{
			return FamiliarDatabase.getNoFamiliarImage();
		}
		String url = KoLmafia.imageServerPath() + "itemimages/" + location;
		File file = FileUtilities.downloadImage( url );
		if ( file == null )
		{
			return FamiliarDatabase.getNoFamiliarImage();
		}
		ImageIcon icon = JComponentUtilities.getImage( "itemimages/" + location );
		return icon != null ? icon : FamiliarDatabase.getNoFamiliarImage();
	}

	public static final ImageIcon getNoFamiliarImage()
	{
		return JComponentUtilities.getImage( "debug.gif" );
	}

	public static final ImageIcon getFamiliarImage( final String name )
	{
		return FamiliarDatabase.getFamiliarImage( FamiliarDatabase.getFamiliarId( name ) );
	}

	public static final ImageIcon getFamiliarImage( final int familiarId )
	{
		String location = FamiliarDatabase.getFamiliarImageLocation( familiarId );
		return FamiliarDatabase.getFamiliarIcon( location );
	}

	public static final ImageIcon getCurrentFamiliarImage()
	{
		String location = KoLCharacter.getFamiliarImage();
		return FamiliarDatabase.getFamiliarIcon( location );
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
		FamiliarDatabase.writeFamiliars( new File( KoLConstants.DATA_LOCATION, "familiars.txt" ) );
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
			int larvaId = FamiliarDatabase.getFamiliarLarva( nextInteger ) ;
			int itemId = FamiliarDatabase.getFamiliarItemId( nextInteger );
			int[] skills = FamiliarDatabase.getFamiliarSkills( nextInteger );

			FamiliarDatabase.writeFamiliar( writer, familiarId, name, image, type, larvaId, itemId, skills );
		}
	}

	public static void writeFamiliar( final PrintStream writer,
					  final int familiarId, final String name, final String image,
					  final String type, final int larvaId, final int itemId, final int [] skills )
	{
		writer.println( FamiliarDatabase.familiarString( familiarId, name, image, type, larvaId, itemId, skills ) );
	}

	public static String familiarString( final int familiarId, final String name, final String image,
					     final String type, final int larvaId, final int itemId, final int [] skills )
	{
		String larva  = larvaId == -1 ? "" : ItemDatabase.getItemDataName( larvaId );
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
