/**
 * Copyright (c) 2005-2019, KoLmafia development team
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

import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class TCRSDatabase
{
	// Item attributes that vary by class/sign in a Two Random Crazy Summer run
	public static class TCRS
	{
		public final String name;
		public final int size;
		public final String quality;
		public final String modifiers;

		TCRS( String name, int size, String quality, String modifiers )
		{
			this.name = name;
			this.size = size;
			this.quality = quality;
			this.modifiers = modifiers;
		}
	}

	private static String characterClass;	// Character class
	private static String characterSign;	// Zodiac Sign

	// Sorted by itemId
	private static Map<Integer, TCRS> TCRSMap = new TreeMap<Integer, TCRS>();
	private static Map<Integer, TCRS> TCRSBoozeMap = new TreeMap<Integer, TCRS>( new CafeDatabase.InverseIntegerOrder() );
	private static Map<Integer, TCRS> TCRSFoodMap = new TreeMap<Integer, TCRS>( new CafeDatabase.InverseIntegerOrder() );

	static
	{
		TCRSDatabase.reset();
	}

	public static void reset()
	{
		characterClass = "";
		characterSign = "";
		TCRSMap.clear();
		TCRSBoozeMap.clear();
		TCRSFoodMap.clear();
	}

	public static String getTCRSName( int itemId )
	{
		TCRS tcrs = TCRSMap.get( itemId );
		return ( tcrs == null ) ? ItemDatabase.getDataName( itemId ) : tcrs.name;
	}

	public static String filename()
	{
		return filename( KoLCharacter.getClassType(), KoLCharacter.getSign(), "" );
	}

	public static String filename( String cclass, String csign, String suffix )
	{
		if ( !Arrays.asList( KoLCharacter.STANDARD_CLASSES ).contains( cclass) ||
		     !Arrays.asList( KoLCharacter.ZODIACS ).contains( csign) )
		{
			return null;
		}

		return "TCRS_" + StringUtilities.globalStringReplace( cclass, " ", "_" ) + "_" + csign + suffix + ".txt";
	}

	public static boolean load( final boolean verbose )
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}
		return load( KoLCharacter.getClassType(), KoLCharacter.getSign(), verbose );
	}

	public static boolean load( String cclass, String csign, final boolean verbose )
	{
		if ( load( filename( cclass, csign, "" ), TCRSMap, verbose ) )
		{
			characterClass = cclass;
			characterSign = csign;
		}
		load( filename( cclass, csign, "_cafe_booze" ), TCRSBoozeMap, verbose );
		load( filename( cclass, csign, "_cafe_food" ), TCRSFoodMap, verbose );
		return true;
	}

	private static boolean load( String fileName, Map< Integer, TCRS> map, final boolean verbose )
	{
		map.clear();

		BufferedReader reader = FileUtilities.getReader( fileName );

		// No reader, no file
		if ( reader == null )
		{
			if ( verbose )
			{
				RequestLogger.printLine( "Could not read file " + fileName );
			}
			return false;
		}

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 5 )
			{
				continue;
			}
			int itemId = StringUtilities.parseInt( data[ 0 ] );
			String name = data[ 1 ];
			int size = StringUtilities.parseInt( data[ 2 ] );
			String quality = data[ 3 ];
			String modifiers = data[ 4 ];

			TCRS item = new TCRS( name, size, quality, modifiers );
			map.put( itemId, item );
		}

		if ( verbose )
		{
			RequestLogger.printLine( "Read file " + fileName );
		}

		return true;
	}

	public static boolean save( final boolean verbose )
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}
		return save( KoLCharacter.getClassType(), KoLCharacter.getSign(), verbose );
	}

	public static boolean save( String cclass, String csign, final boolean verbose )
	{
		save( filename( cclass, csign, "" ), TCRSMap, verbose );
		save( filename( cclass, csign, "_cafe_booze" ), TCRSBoozeMap, verbose );
		save( filename( cclass, csign, "_cafe_food" ), TCRSFoodMap, verbose );
		return true;
	}

	private static boolean save(  final String fileName, final Map<Integer, TCRS> map, final boolean verbose )
	{
		if ( fileName == null )
		{
			return false;
		}

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, fileName ), true );

		// No writer, no file
		if ( writer == null )
		{
			if ( verbose )
			{
				RequestLogger.printLine( "Could not write file " + fileName );
			}
			return false;
		}

		for ( Entry<Integer, TCRS> entry : map.entrySet() )
		{
			TCRS tcrs = entry.getValue();
			Integer itemId = entry.getKey();
			String name = tcrs.name;
			Integer size = tcrs.size;
			String quality = tcrs.quality;
			String modifiers = tcrs.modifiers;
			String line = itemId + "\t" + name + "\t" + size + "\t" + quality + "\t" + modifiers;
			writer.println( line );
		}

		writer.close();

		if ( verbose )
		{
			RequestLogger.printLine( "Wrote file " + fileName );
		}

		return true;
	}

	public static boolean derive( final boolean verbose )
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}

		deriveNonCafe( verbose );
		deriveCafe( verbose );
		return true;
	}

	private static boolean deriveNonCafe( final boolean verbose )
	{
		String myClass = KoLCharacter.getClassType();
		String mySign =  KoLCharacter.getSign();

		// If we don't currently have data for this class/sign, start fresh
		if ( !myClass.equals( characterClass ) || !mySign.equals( characterSign ) )
		{
			reset();
		}

		Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

		if ( verbose )
		{
			KoLmafia.updateDisplay( "Deriving TCRS item adjustments for all real items..." );
		}

		for ( Integer id : keys )
		{
			derive( id );
		}


		characterClass = myClass;
		characterSign = mySign;

		if ( verbose )
		{
			KoLmafia.updateDisplay( "Done!" );
		}

		return true;
	}

	public static boolean derive( final int itemId )
	{
		// Don't do this if we already know the item
		if ( TCRSMap.containsKey( itemId ) )
		{
			return false;
		}

		TCRS tcrs = deriveItem( itemId );
		if ( tcrs == null )
		{
			return false;
		}
		
		TCRSMap.put( itemId, tcrs );

		return true;
	}

	public static TCRS deriveItem( final int itemId )
	{
		// Read the Item Description
		String text = DebugDatabase.itemDescriptionText( itemId, false );
		if ( text == null )
		{
			return null;
		}
		return deriveItem( text );
	}

	private static TCRS deriveItem( final String text )
	{
		// Parse the things that are changed in TCRS
		String name = DebugDatabase.parseName( text );
		int size = DebugDatabase.parseConsumableSize( text );
		String quality = DebugDatabase.parseQuality( text );
		ArrayList<String> unknown = new ArrayList<String>();
		String modifiers = DebugDatabase.parseItemEnchantments( text, unknown, -1 );

		// Create and return the TCRS object
		return new TCRS( name, size, quality, modifiers );
	}

	private static boolean deriveCafe( final boolean verbose)
	{
		if ( verbose )
		{
			KoLmafia.updateDisplay( "Deriving TCRS item adjustments for all cafe booze items..." );
		}

		for ( Integer id : CafeDatabase.cafeBoozeKeySet() )
		{
			deriveCafe( id, CafeDatabase.boozeDescId( id ), TCRSBoozeMap  );
		}

		if ( verbose )
		{
			KoLmafia.updateDisplay( "Done!" );
		}

		if ( verbose )
		{
			KoLmafia.updateDisplay( "Deriving TCRS item adjustments for all cafe food items..." );
		}

		for ( Integer id : CafeDatabase.cafeFoodKeySet() )
		{
			deriveCafe( id, CafeDatabase.foodDescId( id ), TCRSFoodMap );
		}

		if ( verbose )
		{
			KoLmafia.updateDisplay( "Done!" );
		}

		return true;
	}

	private  static boolean deriveCafe( final int itemId, String descId, Map<Integer, TCRS> map  )
	{
		// Don't do this if we already know the item
		if ( map.containsKey( itemId ) )
		{
			return false;
		}

		String text = DebugDatabase.cafeItemDescriptionText( descId );

		TCRS tcrs = deriveItem( text );
		if ( tcrs == null )
		{
			return false;
		}
		
		map.put( itemId, tcrs );

		return true;
	}

	public static boolean applyModifiers()
	{
		// Adjust non-cafe item data to have TCRS modifiers
		for ( Entry<Integer, TCRS> entry : TCRSMap.entrySet() )
		{
			Integer id = entry.getKey();
			TCRS tcrs = entry.getValue();
			String name = ItemDatabase.getItemDataName( id.intValue() );

			// If the path name is the same as the standard name,
			// leave modifiers intact.
			if ( !tcrs.name.equals( name ) )
			{
				// Otherwise, make the changes.
				applyModifiers( id, tcrs );
			}
		}

		// Do the same for cafe consumable
		for ( Entry<Integer, TCRS> entry : TCRSBoozeMap.entrySet() )
		{
			Integer id = entry.getKey();
			TCRS tcrs = entry.getValue();
			String name = CafeDatabase.getCafeBoozeName( id.intValue() );
			if ( !tcrs.name.equals( name ) )
			{
				applyConsumableModifiers( KoLConstants.CONSUME_DRINK, name,  tcrs );
			}
		}

		for ( Entry<Integer, TCRS> entry : TCRSFoodMap.entrySet() )
		{
			Integer id = entry.getKey();
			TCRS tcrs = entry.getValue();
			String name = CafeDatabase.getCafeFoodName( id.intValue() );
			if ( !tcrs.name.equals( name ) )
			{
				applyConsumableModifiers( KoLConstants.CONSUME_EAT, name,  tcrs );
			}
		}

		ConcoctionDatabase.refreshConcoctions();
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
		return true;
	}

	public static boolean applyModifiers( int itemId )
	{
		if ( ItemDatabase.isFamiliarEquipment( itemId ) )
		{
			return false;
		}
		Integer id = IntegerPool.get( itemId );
		return applyModifiers( id, TCRSMap.get( id ) );
	}

	private static int qualityMultiplier( String quality )
	{
		return  "EPIC".equals( quality ) ? 5 :
			"awesome".equals( quality ) ? 4 :
			"good".equals( quality ) ? 3 :
			"decent".equals( quality ) ? 2 :
			"crappy".equals( quality ) ? 1 :
			0;
	}

	public static boolean applyModifiers( final Integer itemId, final TCRS tcrs )
	{
		// Adjust item data to have TCRS modifiers
		if ( tcrs == null )
		{
			return false;
		}

		String itemName = ItemDatabase.getItemName( itemId );
		if ( itemName == null )
		{
			return false;
		}

		int usage = ItemDatabase.getConsumptionType( itemId );
		if ( usage == KoLConstants.CONSUME_EAT || usage == KoLConstants.CONSUME_DRINK || usage == KoLConstants.CONSUME_SPLEEN )
		{
			applyConsumableModifiers( usage, itemName, tcrs );
		}

		// Set modifiers
		Modifiers.updateItem( itemName, tcrs.modifiers );

		return true;
	}

	private static void applyConsumableModifiers( final int usage, final String itemName, final TCRS tcrs )
	{
		Integer lint = ConsumablesDatabase.getLevelReqByName( itemName );
		int level = lint == null ? 0 : lint.intValue();
		// Guess
		int adv = ( usage == KoLConstants.CONSUME_SPLEEN ) ? 0 : (tcrs.size * qualityMultiplier( tcrs.quality ) );
		int mus = 0;
		int mys = 0;
		int mox = 0;
		// Could include effect
		String comment = "Unspaded";
		ConsumablesDatabase.updateConsumableSize( itemName, usage, tcrs.size );
		ConsumablesDatabase.updateConsumable( itemName, tcrs.size, level, tcrs.quality, String.valueOf( adv ),
						      String.valueOf( mus ), String.valueOf( mys ), String.valueOf( mox ),
						      comment );
	}

	public static boolean resetModifiers()
	{
		// Adjust item data to have non-TCRS modifiers
		return true;
	}

	// *** support for fetching TCRS files from KoLmafia's SVN repository

	// Remote files we have fetched this session
	private static Set<String> remoteFetched = new HashSet<String>(); //remote files fetched this session

	// *** Fetching files from the SVN repository, in two parts, since the
	// non-cafe code was released a week before the cafe code, and some
	// class/signs have only the non-cafe file

	public static boolean fetch( final String classType, final String sign,  final boolean verbose )
	{
		boolean retval = fetchRemoteFile( filename( classType, sign, "" ), verbose );
		return retval;
	}

	public static boolean fetchCafe( final String classType, final String sign,  final boolean verbose )
	{
		boolean retval = true;
		retval &= fetchRemoteFile( filename( classType, sign, "_cafe_booze" ), verbose );
		retval &= fetchRemoteFile( filename( classType, sign, "_cafe_food" ), verbose );
		return retval;
	}

	// *** If we want to get all three files at once - and count it a
	// success as long as the non-cafe file is present -use these.
	// Not recommended.

	public static boolean fetchRemoteFiles( final boolean verbose )
	{
		return fetchRemoteFiles( KoLCharacter.getClassType(), KoLCharacter.getSign(), verbose );
	}

	public static boolean fetchRemoteFiles( String classType, String sign, final boolean verbose )
	{
		boolean retval = fetchRemoteFile( filename( classType, sign, "" ), verbose );
		fetchRemoteFile( filename( classType, sign, "_cafe_booze" ), verbose );
		fetchRemoteFile( filename( classType, sign, "_cafe_food" ), verbose );
		return retval;
	}

	// *** Primitives for checking presence of local file, checking
	// presence of remote file, and fetching the remote file.

	public static boolean localFileExists( String localFilename, final boolean verbose )
	{
		File localFile = new File( KoLConstants.DATA_LOCATION, localFilename );
		return localFileExists( localFile, verbose );
	}

	public static boolean localFileExists( File localFile, final boolean verbose )
	{
		boolean exists = localFile.exists() && localFile.length() > 0;
		if ( verbose )
		{
			RequestLogger.printLine( "Local file " + localFile.getName() + " " + ( exists ? "already exists" : "does not exist" ) + "." );
		}
		return exists;
	}

	// *** Primitives for fetching a file from the SVN repository, overwriting existing file, if any.

	public static boolean fetchRemoteFile( String localFilename, final boolean verbose )
	{
		String remoteFileName = "https://sourceforge.net/p/kolmafia/code/HEAD/tree/data/TCRS/" +
				localFilename + "?format=raw";
		if ( remoteFetched.contains( remoteFileName ) )
		{
			if ( verbose )
			{
				RequestLogger.printLine( "Already fetched remote version of " + localFilename + " in this session." );
			}
			return true;
		}

		// Because we know we want a remote file the directory and override parameters will be ignored.
		BufferedReader remoteReader = DataUtilities.getReader( "", remoteFileName, false );
		File output = new File( KoLConstants.DATA_LOCATION, localFilename );

		try
		{
			PrintWriter writer = new PrintWriter( new FileWriter( output ) );
			String aLine;
			while (( aLine = remoteReader.readLine() ) != null )
			{
				// if the remote copy uses a different EOl than
				// the local OS then this will implicitly convert
				writer.println( aLine );
			}
			remoteReader.close();
			writer.close();
			if ( verbose )
			{
				RequestLogger.printLine( "Fetched remote version of " + localFilename + " from the repository." );
			}
		}
		catch ( IOException exception )
		{
			// The reader and writer should be closed but since
			// that can throw an exception...
			RequestLogger.printLine( "IO Exception for " + localFilename + ": "+ exception.toString() );
			return false;
		}

		if ( output.length() <= 0 )
		{
			// Do we care if we delete a file that is known to
			// exist and is empty?  No.
			if ( verbose )
			{
				RequestLogger.printLine( "File " + localFilename + " is empty. Deleting." );
			}
			output.delete();
			return false;
		}

		remoteFetched.add( remoteFileName );
		return true;
	}
}
