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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	static
	{
		TCRSDatabase.reset();
	}

	public static void reset()
	{
		characterClass = "";
		characterSign = "";
		TCRSMap.clear();
	}

	public static String filename()
	{
		return filename( KoLCharacter.getClassType(), KoLCharacter.getSign() );
	}

	public static String filename( String cclass, String csign )
	{
		if ( !Arrays.asList( KoLCharacter.STANDARD_CLASSES ).contains( cclass) ||
		     !Arrays.asList( KoLCharacter.ZODIACS ).contains( csign) )
		{
			return null;
		}

		return "TCRS_" + StringUtilities.globalStringReplace( cclass, " ", "_" ) + "_" + csign + ".txt";
	}

	public static boolean load()
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}
		return load( KoLCharacter.getClassType(), KoLCharacter.getSign() );
	}

	public static boolean load( String cclass, String csign )
	{
		String fileName = filename( cclass, csign );
		if ( fileName == null )
		{
			return false;
		}

		TCRSMap.clear();

		BufferedReader reader = FileUtilities.getReader( fileName );

		// No reader, no file
		if ( reader == null )
		{
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
			TCRSMap.put( itemId, item );
		}

		characterClass = cclass;
		characterSign = csign;
		return true;
	}

	public static boolean save()
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}
		return save( KoLCharacter.getClassType(), KoLCharacter.getSign() );
	}

	public static boolean save( String cclass, String csign )
	{
		String fileName = filename( cclass, csign );
		if ( fileName == null )
		{
			return false;
		}

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, fileName ), true );

		for ( Entry<Integer, TCRS> entry : TCRSMap.entrySet() )
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

		return true;
	}

	public static boolean derive( final boolean verbose )
	{
		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			return false;
		}

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
			KoLmafia.updateDisplay( "Deriving TCRS item adjustments for all items..." );
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

		// Read the Item Description
		String text = DebugDatabase.itemDescriptionText( itemId, false );
		if ( text == null )
		{
			return false;
		}

		// Parse the things that are changed in TCRS
		String name = DebugDatabase.parseName( text );
		int size = DebugDatabase.parseConsumableSize( text );
		String quality = DebugDatabase.parseQuality( text );
		ArrayList<String> unknown = new ArrayList<String>();
		String modifiers = DebugDatabase.parseItemEnchantments( text, unknown, -1 );

		// Create and save the TCRS object
		TCRS tcrs = new TCRS( name, size, quality, modifiers );
		TCRSMap.put( itemId, tcrs );

		return true;
	}

	public static boolean applyModifiers()
	{
		// Adjust item data to have TCRS modifiers
		for ( Entry<Integer, TCRS> entry : TCRSMap.entrySet() )
		{
			applyModifiers( entry.getKey(), entry.getValue() );
		}
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
		return true;
	}

	public static boolean applyModifiers( int itemId )
	{
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

		// Set modifiers
		Modifiers.updateItem( itemName, tcrs.modifiers );

		return true;
	}

	public static boolean resetModifiers()
	{
		// Adjust item data to have non-TCRS modifiers
		return true;
	}
}
