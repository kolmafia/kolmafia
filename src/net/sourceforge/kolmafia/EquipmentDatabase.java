/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class EquipmentDatabase extends KoLDatabase
{
	private static TreeMap outfitPieces = new TreeMap();
	private static IntegerArray power = new IntegerArray();
	private static IntegerArray hands = new IntegerArray();
	private static StringArray type = new StringArray();
	private static StringArray requirement = new StringArray();
	private static SpecialOutfitArray outfits = new SpecialOutfitArray();

	static
	{
		BufferedReader reader = getReader( "equipment.dat" );

		String [] data;
		int itemID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				itemID = TradeableItemDatabase.getItemID( data[0] );

				if ( itemID != -1 )
				{
					power.set( itemID, parseInt( data[1] ) );
					requirement.set( itemID, data[2] );

					int hval = 0;
					String tval = null;
					if ( data.length >= 4 )
					{
						String str = data[3];
						hval = parseInt( str.substring(0,1) );
						tval = str.substring( str.indexOf( " " ) + 1 );
					}

					hands.set( itemID, hval );
					type.set( itemID, tval );
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

		reader = getReader( "outfits.dat" );
		int outfitID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
			{
				outfitID = parseInt( data[0] );
				outfits.set( outfitID, new SpecialOutfit( outfitID, data[1] ) );

				String [] pieces = data[2].split( "\\s*,\\s*" );
				for ( int i = 0; i < pieces.length; ++i )
				{
					outfitPieces.put( getCanonicalName( pieces[i] ), new Integer( outfitID ) );
					outfits.get( outfitID ).addPiece( new AdventureResult( pieces[i], 1, false ) );
				}
			}
		}
	}

	public static int getOutfitWithItem( int itemID )
	{
		String itemName = getCanonicalName( TradeableItemDatabase.getItemName( itemID ) );
		Integer result = (Integer) outfitPieces.get( itemName );
		return result == null ? -1 : result.intValue();
	}

	public static int getOutfitCount()
	{	return outfits.size();
	}

	public static boolean contains( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );
		return itemID != -1 && requirement.get( itemID ) != null;
	}

	public static boolean canEquip( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 || requirement.get( itemID ) == null )
			return false;

		if ( requirement.get( itemID ).startsWith( "Mus:" ) )
			return KoLCharacter.getBaseMuscle() >= parseInt( requirement.get( itemID ).substring(5) );

		if ( requirement.get( itemID ).startsWith( "Mys:" ) )
			return KoLCharacter.getBaseMysticality() >= parseInt( requirement.get( itemID ).substring(5) );

		if ( requirement.get( itemID ).startsWith( "Mox:" ) )
			return KoLCharacter.getBaseMoxie() >= parseInt( requirement.get( itemID ).substring(5) );

		return true;
	}

	public static int getPower( int itemID )
	{	return power.get( itemID );
	}

	public static int getPower( String itemName )
	{
		if ( itemName == null )
			return 0;

		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return getPower( itemID );
	}

	public static int getHands( int itemID )
	{	return hands.get( itemID );
	}

	public static int getHands( String itemName )
	{
		if ( itemName == null )
			return 0;

		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return getHands( itemID );
	}

	public static String getType( int itemID )
	{	return type.get( itemID );
	}

	public static String getType( String itemName )
	{
		if ( itemName == null )
			return null;

		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return null;

		return getType( itemID );
	}

	public static boolean isRanged( int itemID )
	{
		String req = requirement.get( itemID );
		return req != null && req.startsWith( "Mox:" );
	}

	public static boolean isRanged( String itemName )
	{
		if ( itemName == null )
			return false;

		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return false;

		return isRanged( itemID );
	}

	public static boolean isStaff( int itemID )
	{
		String type = getType( itemID );
		return type != null && type.equals( "staff" );
	}

	public static boolean isStaff( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return false;

		return isStaff( itemID );
	}

	public static boolean hasOutfit( int id )
	{	return KoLCharacter.getOutfits().contains( outfits.get( id ) );
	}

	public static SpecialOutfit getOutfit( int id )
	{	return outfits.get( id );
	}

	public static void updateOutfits()
	{
		ArrayList available = new ArrayList();
		for ( int i = 0; i < outfits.size(); ++i )
			if ( outfits.get(i) != null && outfits.get(i).hasAllPieces() )
				available.add( outfits.get(i) );

		Collections.sort( available );

		// Rebuild the list of outfits
		List outfits = KoLCharacter.getOutfits();
		outfits.clear();

		// Start with the two constant outfits
		outfits.add( SpecialOutfit.NO_CHANGE );
		outfits.add( SpecialOutfit.BIRTHDAY_SUIT );

		// Then any custom outfits
		outfits.addAll( KoLCharacter.getCustomOutfits() );

		// Finally any standard outfits
		outfits.addAll( available );
	}

	/**
	 * Utility method which determines whether or not the equipment
	 * corresponding to the given outfit is already equipped.
	 */

	public static boolean isWearingOutfit( int outfitID )
	{	return outfits.get( outfitID ) != null && outfits.get( outfitID ).isWearing();
	}

	public static int getOutfitID( KoLAdventure adventure )
	{
		String adventureID = adventure.getAdventureID();

		// Knob goblin treasury has the elite guard outfit
		if ( adventureID.equals( "41" ) )
			return 5;

		// Knob goblin harem has the harem girl disguise
		if ( adventureID.equals( "42" ) || adventure.getFormSource().equals( "knob.php" ) )
			return 4;

		// The mine has mining gear
		if ( adventureID.equals( "61" ) )
			return 8;

		// The slope has eXtreme cold weather gear
		if ( adventureID.equals( "63" ) )
			return 7;

		// Hippies have a filthy hippy disguise
		if ( adventureID.equals( "26" ) || adventureID.equals( "65" ) )
			return 2;

		// Frat house has a frat house ensemble
		if ( adventureID.equals( "27" ) || adventureID.equals( "29" ) )
			return 3;

		// Pirates have a swashbuckling getup
		if ( adventureID.equals( "66" ) || adventureID.equals( "67" ) )
			return 9;

		// Choose the uniform randomly
		if ( adventureID.equals( "85" ) )
			return RNG.nextInt(2) == 0 ? 23 : 24;

		// Cloaca area requires cloaca uniforms
		if ( adventureID.equals( "86" ) )
			return 23;

		// Dyspepsi area requires dyspepsi uniforms
		if ( adventureID.equals( "87" ) )
			return 24;

		// No outfit existed for this area
		return 0;
	}

	public static boolean addOutfitConditions( KoLAdventure adventure )
	{
		int outfitID = getOutfitID( adventure );
		if ( outfitID == 0 )
			return false;

		addOutfitConditions( outfitID );
		return true;
	}

	public static void retrieveOutfit( int outfitID )
	{
		String [] pieces = outfits.get( outfitID ).getPieces();
		for ( int i = 0; i < pieces.length; ++i )
			if ( !KoLCharacter.hasEquipped( new AdventureResult( pieces[i], 1, false ) ) )
				DEFAULT_SHELL.executeLine( "acquire " + pieces[i] );
	}

	public static void addOutfitConditions( int outfitID )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitID < 1 )
			return;

		String [] pieces = outfits.get( outfitID ).getPieces();
		for ( int i = 0; i < pieces.length; ++i )
			if ( !KoLCharacter.hasEquipped( new AdventureResult( pieces[i], 1, false ) ) )
				DEFAULT_SHELL.executeConditionsCommand( "add " + pieces[i] );
	}

	/**
	 * Internal class which functions exactly an array of concoctions,
	 * except it uses "sets" and "gets" like a list.  This could be
	 * done with generics (Java 1.5) but is done like this so that
	 * we get backwards compatibility.
	 */

	private static class SpecialOutfitArray
	{
		private ArrayList internalList = new ArrayList();

		public SpecialOutfit get( int index )
		{	return index < 0 || index >= internalList.size() ? null : (SpecialOutfit) internalList.get( index );
		}

		public void set( int index, SpecialOutfit value )
		{
			for ( int i = internalList.size(); i <= index; ++i )
				internalList.add( null );

			internalList.set( index, value );
		}

		public int size()
		{	return internalList.size();
		}
	}
}
