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
import java.util.ArrayList;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class EquipmentDatabase extends KoLDatabase
{
	private static IntegerArray power = new IntegerArray();
	private static IntegerArray hands = new IntegerArray();
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
					power.set( itemID, Integer.parseInt( data[1] ) );
					requirement.set( itemID, data[2] );

					hands.set( itemID, ( data.length >= 4 ) ? Integer.parseInt( data[3] ) :
						TradeableItemDatabase.getConsumptionType( itemID ) == ConsumeItemRequest.EQUIP_WEAPON ? 1 : 0 );
				}
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}

		reader = getReader( "outfits.dat" );
		int outfitID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
			{
				outfitID = Integer.parseInt( data[0] );
				outfits.set( outfitID, new SpecialOutfit( outfitID, data[1] ) );

				String [] pieces = data[2].split( "\\s*,\\s*" );
				for ( int i = 0; i < pieces.length; ++i )
					outfits.get( outfitID ).addPiece( new AdventureResult( pieces[i], 1 ) );
			}
		}
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
			return KoLCharacter.getBaseMuscle() >= Integer.parseInt( requirement.get( itemID ).substring(5) );

		if ( requirement.get( itemID ).startsWith( "Mys:" ) )
			return KoLCharacter.getBaseMysticality() >= Integer.parseInt( requirement.get( itemID ).substring(5) );

		if ( requirement.get( itemID ).startsWith( "Mox:" ) )
			return KoLCharacter.getBaseMoxie() >= Integer.parseInt( requirement.get( itemID ).substring(5) );

		return true;
	}

	public static int getPower( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return power.get( itemID );
	}

	public static int getHands( int itemID )
	{	return hands.get( itemID );
	}

	public static int getHands( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return getHands( itemID );
	}

	public static boolean hasOutfit( int id )
	{	return KoLCharacter.getOutfits().contains( outfits.get( id ) );
	}

	public static SpecialOutfit getOutfit( int id )
	{	return outfits.get( id );
	}

	public static void updateOutfits()
	{
		SortedListModel available = new SortedListModel();
		for ( int i = 0; i < outfits.size(); ++i )
			if ( outfits.get(i) != null && outfits.get(i).hasAllPieces() )
					available.add( outfits.get(i) );

		// Rebuild the list of outfits
		LockableListModel outfits = KoLCharacter.getOutfits();
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

	public static boolean addOutfitConditions( KoLAdventure adventure )
	{
		LockableListModel conditions = client.getConditions();
		String adventureID = adventure.getAdventureID();

		if ( adventureID.equals( "41" ) )
		{
			addOutfitConditions( 5 );
			return true;
		}

		if ( adventureID.equals( "42" ) )
		{
			addOutfitConditions( 4 );
			return true;
		}

		if ( adventureID.equals( "61" ) )
		{
			addOutfitConditions( 8 );
			return true;
		}

		if ( adventureID.equals( "63" ) )
		{
			addOutfitConditions( 7 );
			return true;
		}

		if ( adventureID.equals( "26" ) )
		{
			addOutfitConditions( 2 );
			return true;
		}

		if ( adventureID.equals( "27" ) )
		{
			addOutfitConditions( 3 );
			return true;
		}

		if ( adventureID.equals( "66" ) )
		{
			addOutfitConditions( 9 );
			return true;
		}

		if ( adventureID.equals( "85" ) )
		{
			addOutfitConditions( 23 );
			return true;
		}

		// No outfit existed for this are; therefore,
		// return false.

		return false;
	}

	public static void addOutfitConditions( int outfitID )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitID < 1 )
			return;

		String [] missingPieces = outfits.get( outfitID ).getMissingPieces();
		for ( int i = 0; i < missingPieces.length; ++i )
			DEFAULT_SHELL.executeConditionsCommand( "add " + missingPieces[i] );
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
