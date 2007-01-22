/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://sourceforge.net/
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

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.TreeMap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CustomItemDatabase extends Properties implements KoLConstants
{
	private static final CustomItemDatabase INSTANCE = new CustomItemDatabase();
	private static final File KILT_FILE = new File( DATA_DIRECTORY, "tehkilt.txt" );

	private static final String [] CUSTOM_FLAGS = new String []
	{
		/* [00] */	"image location",
		/* [01] */	"item name",
		/* [02] */	"item description",
		/* [03] */	"basic type name",
		/* [04] */	"custom type name",

		/* [05] */	"shield flag",
		/* [06] */	"ranged weapon flag",
		/* [07] */	"damage reduction",
		/* [08] */	"number of hands required",
		/* [09] */	"item power",
		/* [10] */	"container capacity",
		/* [11] */	"accessory slot",
		/* [12] */	"elemental attack type",
		/* [13] */	"muscle, mysticality, moxie",
		/* [14] */	"stat requirement",
		/* [15] */	"autosell value",
		/* [16] */	"part of which outfit",

		/* [17] */	"Intrinsic effect: ",
		/* [18] */	"Intrinsic effect: ",
		/* [19] */	"Intrinsic effect: ",
		/* [20] */	"Intrinsic effect: ",

		/* [21] */	"Gift Item",
		/* [22] */	"Cannot be traded",
		/* [23] */	"Cannot be discarded",
		/* [24] */	"NOTE: You may not equip more than one of this item at a time.",
		/* [25] */	"Cocktailcrafting ingredient",
		/* [26] */	"Meatsmithing component",
		/* [27] */	"NOTE: This item cannot be equipped while in Hardcore.",
		/* [28] */	"Quest Item",
		/* [29] */  "NOTE: Items that reduce the MP cost of skills will not do so by more than 3 points, in total.",
		/* [30] */	"Free pull from Hagnk's"
	};

	private static void initialize()
	{
		// Initialize the data if the file does not yet exist or
		// the file hasn't been updated for a week.

		String thisWeek = WEEKLY_FORMAT.format( new Date() );
		if ( KILT_FILE.exists() && StaticEntity.getProperty( "lastCustomItemUpdate" ).equals( thisWeek ) )
			return;

		updateItemList();
		StaticEntity.setProperty( "lastCustomItemUpdate", thisWeek );
	}

	private static void updateItemList()
	{
		// Clear out all existing data when downloading an update.
		// It will get refreshed the next time the profile is seen.

		try
		{
			INSTANCE.clear();
			BufferedReader reader = KoLDatabase.getReader( "http://kol.upup.us/scripts/cust/fetch.php?who" );

			String line;
			while ( (line = reader.readLine()) != null )
			{
				INSTANCE.setProperty( line, "PARTICIPATING" );
				INSTANCE.setProperty( line + ".0", "" );
			}

			reader.close();
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void updateItem( String playerId )
	{
		// Clear out all existing data when downloading an update.
		// It will get refreshed the next time the profile is seen.

		if ( !INSTANCE.containsKey( playerId ) || !INSTANCE.getProperty( playerId + ".0" ).equals( "" ) )
			return;

		try
		{
			BufferedReader reader = KoLDatabase.getReader( "http://kol.upup.us/scripts/cust/fetch.php?pid=" + playerId );

			String line;
			StringBuffer data = new StringBuffer();

			while ( (line = reader.readLine()) != null )
			{
				data.append( line );
				data.append( LINE_BREAK );
			}

			String [] pieces = data.toString().trim().split( "\t" );

			if ( pieces.length < CUSTOM_FLAGS.length )
			{
				INSTANCE.remove( playerId );
				INSTANCE.remove( playerId + ".0" );
			}

			for ( int i = 0; i < CUSTOM_FLAGS.length; ++i )
				INSTANCE.setProperty( playerId + "." + i, pieces[i] );

			reader.close();
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static void renderCustomItem( LocalRelayRequest request )
	{
		// First, some preliminary checks to see if custom data
		// should be added to this request.

		if ( !StaticEntity.getBooleanProperty( "relayViewsCustomItems" ) )
			return;

		String urlString = request.getURLString();
		if ( urlString.indexOf( "showplayer.php" ) == -1 )
			return;

		String playerId = urlString.substring( urlString.indexOf( "=" ) + 1 );
		if ( !INSTANCE.containsKey( playerId ) || !INSTANCE.getProperty( playerId ).equals( "PARTICIPATING" ) )
			return;

		// If it gets this far, that means there's some custom data
		// which should be added.  Make sure it gets loaded.

		initialize();
		updateItem( playerId );

		if ( !INSTANCE.containsKey( playerId ) )
			return;
	}

	public static String showCustomItem( String playerId )
	{	return null;
	}

	public static void saveItemData()
	{
		if ( INSTANCE.isEmpty() )
			return;

		DATA_DIRECTORY.mkdirs();

		try
		{
			if ( KILT_FILE.exists() )
				KILT_FILE.delete();

			// Determine the contents of the file by
			// actually printing them.

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			INSTANCE.store( ostream, VERSION_NAME );

			String [] lines = ostream.toString().split( LINE_BREAK );
			Arrays.sort( lines );

			ostream.reset();

			for ( int i = 0; i < lines.length; ++i )
			{
				if ( lines[i].startsWith( "#" ) )
					continue;

				ostream.write( lines[i].getBytes() );
				ostream.write( LINE_BREAK.getBytes() );
			}

			KILT_FILE.createNewFile();
			ostream.writeTo( new FileOutputStream( KILT_FILE ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}
}