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

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CustomItemDatabase extends Properties implements KoLConstants
{
	private static final CustomItemDatabase INSTANCE = new CustomItemDatabase();
	private static final File KILT_FILE = new File( DATA_LOCATION, "tehkilt.txt" );

	private static final Pattern EQUIP_PATTERN = Pattern.compile( "<tr><td width=30 height=30><img src=\"[^\"]+\" class=hand onClick='descitem\\(\\d+\\)'></td><td valign=center><b>([^<]+)</b></td></tr>" );

	private static final int CUSTOM_FLAG_COUNT = 31;

	// Only provide very limited support for item types.  Sure, people
	// can create custom familiar accessories and/or custom containers,
	// but KoLmafia doesn't have to support viewing them.

	private static final int [] CUSTOM_TYPES = new int [] { -1, KoLCharacter.HAT, -1,
		KoLCharacter.SHIRT, KoLCharacter.WEAPON, KoLCharacter.OFFHAND, KoLCharacter.PANTS,
		KoLCharacter.ACCESSORY1, KoLCharacter.ACCESSORY2, KoLCharacter.ACCESSORY3, -1 };

	private static void initialize()
	{
		// Initialize the data if the file does not yet exist or
		// the file hasn't been updated for a week.

		String thisWeek = WEEKLY_FORMAT.format( new Date() );
		if ( KILT_FILE.exists() && StaticEntity.getProperty( "lastCustomItemUpdate" ).equals( thisWeek ) )
		{
			try
			{
				INSTANCE.load( new FileInputStream( KILT_FILE ) );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			return;
		}

		updateParticipantList();
		StaticEntity.setProperty( "lastCustomItemUpdate", thisWeek );
	}

	private static void updateParticipantList()
	{
		// Clear out all existing data when downloading an update.
		// It will get refreshed the next time the profile is seen.

		try
		{
			INSTANCE.clear();
			BufferedReader reader = KoLDatabase.getReader( "http://kol.upup.us/scripts/cust/fetch.php?who" );

			String line;
			while ( (line = reader.readLine()) != null )
				INSTANCE.setProperty( line + ".0", "" );

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

		if ( !INSTANCE.getProperty( playerId + ".0" ).equals( "" ) )
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

			if ( pieces.length < CUSTOM_FLAG_COUNT )
			{
				INSTANCE.remove( playerId );
				INSTANCE.remove( playerId + ".0" );
			}

			for ( int i = 0; i < CUSTOM_FLAG_COUNT; ++i )
				INSTANCE.setProperty( playerId + "." + i, pieces[i] );

			reader.close();
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static void linkCustomItem( LocalRelayRequest request )
	{
		// First, some preliminary checks to see if custom data
		// should be added to this request.

		if ( !StaticEntity.getBooleanProperty( "relayViewsCustomItems" ) )
			return;

		String urlString = request.getURLString();
		if ( urlString.indexOf( "showplayer.php" ) == -1 )
			return;

		initialize();

		String playerId = urlString.substring( urlString.indexOf( "=" ) + 1 );
		if ( !INSTANCE.containsKey( playerId + ".0" ) )
			return;

		// If it gets this far, that means there's some custom data
		// which should be added.  Make sure it gets loaded.

		updateItem( playerId );

		int customType = StaticEntity.parseInt( INSTANCE.getProperty( playerId + ".3" ) );
		if ( customType < 0 || customType >= CUSTOM_TYPES.length )
			return;

		customType = CUSTOM_TYPES[ customType ];
		if ( customType == -1 )
			return;

		boolean addedItem = false;

		String customItemString = "<tr><td width=30 height=30><img src=\"" + INSTANCE.getProperty( playerId + ".0" ) +
			"\" onClick=item('custom" + playerId + "')></td><td valign=center><b>" + INSTANCE.getProperty( playerId + ".1" ) + "</b></td></tr>";

		String lastDataString = null;
		Matcher equipMatcher = EQUIP_PATTERN.matcher( request.responseText );

		while ( equipMatcher.find() && !addedItem )
		{
			lastDataString = equipMatcher.group();
			int itemType = EquipmentRequest.chooseEquipmentSlot( TradeableItemDatabase.getConsumptionType( equipMatcher.group(1) ) );

			if ( itemType < customType )
				continue;

			if ( itemType == customType )
				request.responseText = StaticEntity.singleStringReplace( request.responseText, lastDataString, customItemString );
			else if ( itemType > customType )
				request.responseText = StaticEntity.singleStringReplace( request.responseText, lastDataString, lastDataString + customItemString );

			addedItem = true;
		}

		if ( !addedItem && lastDataString != null )
			request.responseText = StaticEntity.singleStringReplace( request.responseText, lastDataString, lastDataString + customItemString );
	}

	public static String retrieveCustomItem( String playerId )
	{
		if ( playerId == null || !INSTANCE.containsKey( playerId + ".0" ) || INSTANCE.get( playerId + ".0" ).equals( "" ) )
			return null;

		StringBuffer content = new StringBuffer();

		content.append( "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" );
		content.append( LINE_BREAK );
		content.append( "<html><head><title>Item Description</title>" );
		content.append( LINE_BREAK );
		content.append( "<link rel=\"stylesheet\" type=\"text/css\" href=\"/images/styles.css\">" );
		content.append( LINE_BREAK );
		content.append( "</head><body>" );

		content.append( LINE_BREAK );
		content.append( LINE_BREAK );

		// [00]  image location
		// [01]  item name
		// [02]  item description

		content.append( "<div id=\"description\" class=small><center><img src=\"" );
		content.append( INSTANCE.getProperty( playerId + ".0" ) );
		content.append( "\" height=30 width=30><br><b>" );
		content.append( INSTANCE.getProperty( playerId + ".1" ) );
		content.append( "</b><p></center><blockquote>" );
		content.append( INSTANCE.getProperty( playerId + ".2" ) );
		content.append( "<br><br>" );


		// [25]  Cocktailcrafting ingredient
		// [26]  Meatsmithing component

		appendItemFlag( content, playerId, "(Meatsmithing component)", "26" );
		appendItemFlag( content, playerId, "(Cocktailcrafting ingredient)", "25" );

		// [03]  basic type name

		content.append( "Type: <b>" );

		int customType = StaticEntity.parseInt( INSTANCE.getProperty( playerId + ".3" ) );
		if ( customType < 0 || customType >= CUSTOM_TYPES.length )
			return null;

		customType = CUSTOM_TYPES[ customType ];
		if ( customType == -1 )
			return null;

		switch ( customType )
		{
		case KoLCharacter.HAT:
			content.append( "hat" );
			break;
		case KoLCharacter.OFFHAND:
			content.append( "off-hand item" );
			break;
		case KoLCharacter.PANTS:
			content.append( "pants" );
			break;
		case KoLCharacter.ACCESSORY1:
		case KoLCharacter.ACCESSORY2:
		case KoLCharacter.ACCESSORY3:
			content.append( "accessory" );
			break;
		}

		// [04]  custom type name
		// [05]  shield flag
		// [06]  ranged weapon flag
		// [08]  number of hands required

		if ( customType == KoLCharacter.OFFHAND && !INSTANCE.getProperty( playerId + ".5" ).equals( "" ) )
			content.append( " (shield)" );

		if ( customType == KoLCharacter.WEAPON )
		{
			if ( !INSTANCE.getProperty( playerId + ".5" ).equals( "" ) )
				content.append( "ranged " );

			content.append( "weapon (" );
			content.append( INSTANCE.getProperty( playerId + ".8" ) );
			content.append( "-handed " );
			content.append( INSTANCE.getProperty( playerId + ".4" ) );
			content.append( ")" );
		}

		content.append( "</b><br>" );

		// [07]  damage reduction
		// [09]  item power
		// [10]  container capacity

		appendItemData( content, playerId, "Power", "9" );
		appendItemData( content, playerId, "Damage Reduction", "7" );
		appendItemData( content, playerId, "Capacity", "10" );

		// [12]  elemental attack type
		// [13]  stat requirement

		int statType = StaticEntity.parseInt( INSTANCE.getProperty( playerId + ".12" ) ) - 2;

		switch ( statType )
		{
		case 0:
			appendItemData( content, playerId, "Muscle Required", "13" );
			break;
		case 1:
			appendItemData( content, playerId, "Mysticality Required", "13" );
			break;
		case 2:
			appendItemData( content, playerId, "Moxie Required", "13" );
			break;
		}

		// [14]  autosell value
		// [15]  part of which outfit

		appendItemData( content, playerId, "Outfit", "15" );
		appendItemData( content, playerId, "Selling Price", "Meat.", "14" );

		// In general, the following flags would be displayed for an item,
		// but people tend to get carried away with them.  Thus, do not
		// render them in the KoLmafia version of Teh Kilt.

		// [21]  Gift Item
		// [22]  Cannot be traded
		// [23]  Cannot be discarded
		// [28]  Quest Item
		// [30]  Free pull from Hagnk's

		// appendItemFlag( content, playerId, "Cannot be discarded", "23" );
		// appendItemFlag( content, playerId, "Cannot be traded", "22" );
		// appendItemFlag( content, playerId, "Free pull from Hagnk's", "30" );
		// appendItemFlag( content, playerId, "Quest Item", "28", true );
		// appendItemFlag( content, playerId, "Gift Item", "21", true );

		// [16]  Intrinsic effect:
		// [17]  Intrinsic effect:
		// [18]  Intrinsic effect:
		// [19]  Intrinsic effect:

		content.append( "<br><center>" );
		int insertionPoint = content.length();

		for ( int i = 16; i <= 19; ++i )
			appendIntrinsicEffect( content, playerId, String.valueOf(i) );

		if ( content.length() > insertionPoint )
			content.insert( insertionPoint, "Enchantment:<br>" );

		// In general, the following flags would be displayed for an item,
		// but people tend to get carried away with them.  Thus, do not
		// render them in the KoLmafia version of Teh Kilt.

		// [24]  NOTE: You may not equip more than one of this item at a time.
		// [27]  NOTE: This item cannot be equipped while in Hardcore.
		// [29]  NOTE: Items that reduce the MP cost of skills will not do so by more than 3 points in total.

		// appendItemFlag( content, playerId, "<b>NOTE</b>: You may not equip more than one of this item at a time.", "29", true, true );
		// appendItemFlag( content, playerId, "<b>NOTE</b>: This item cannot be equipped while in Hardcore.", "27", true, true );
		// appendItemFlag( content, playerId, "<b>NOTE</b>: You may not equip more than one of this item at a time.", "24" );

		// All flags have been added.  Go ahead and return the custom
		// item that was created.

		content.append( "</center><br><br>" );

		content.append( "<font size=-1>* This is player-created content, displayed on an opt-in basis, and is not hosted by the creators of this game.  " );
		content.append( "Neither Asymmetric Publications nor the creator of this script is responsible for the content of this item.</font>" );

		content.append( "</blockquote>" );
		content.append( LINE_BREAK );

		content.append( "<script type=\"text/javascript\"><!-- var description=document.getElementById(\"description\"); if (document.all) self.resizeTo(300,description.offsetHeight+95); else window.innerHeight=(description.offsetHeight+50); //--></script>" );
		content.append( LINE_BREAK );

		content.append( "</div></body></html>" );
		return content.toString();
	}

	private static void appendIntrinsicEffect( StringBuffer content, String playerId, String id )
	{
		if ( INSTANCE.getProperty( playerId + "." + id ).equals( "" ) )
			return;

		content.append( "<b><font color=blue>" );
		content.append( INSTANCE.getProperty( playerId + "." + id ) );
		content.append( "</font></b><br>" );
	}

	private static void appendItemFlag( StringBuffer content, String playerId, String name, String id )
	{	appendItemFlag( content, playerId, name, id, false );
	}

	private static void appendItemFlag( StringBuffer content, String playerId, String name, String id, boolean isBold )
	{	appendItemFlag( content, playerId, name, id, isBold, false );
	}

	private static void appendItemFlag( StringBuffer content, String playerId, String name, String id, boolean isBold, boolean isBlue )
	{
		if ( StaticEntity.parseInt( INSTANCE.getProperty( playerId + "." + id ) ) == 0 )
			return;

		if ( isBold )
			content.append( "<b>" );

		if ( isBlue )
			content.append( "<font color=blue>" );

		content.append( name );

		if ( isBlue )
			content.append( "</font>" );

		if ( isBold )
			content.append( "</b>" );

		content.append( "<br>" );
	}

	private static void appendItemData( StringBuffer content, String playerId, String prefix, String id )
	{	appendItemData( content, playerId, prefix, "", id );
	}

	private static void appendItemData( StringBuffer content, String playerId, String prefix, String suffix, String id )
	{
		if ( StaticEntity.parseInt( INSTANCE.getProperty( playerId + "." + id ) ) == 0 )
			return;

		content.append( prefix );
		content.append( ": <b>" );
		content.append( INSTANCE.getProperty( playerId + "." + id ) );

		content.append( " " );
		content.append( suffix );
		content.append( "</b><br>" );
	}

	public static void saveItemData()
	{
		if ( INSTANCE.isEmpty() )
			return;

		DATA_LOCATION.mkdirs();

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