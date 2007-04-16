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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class EquipmentDatabase extends KoLDatabase
{
	private static IntegerArray power = new IntegerArray();
	private static IntegerArray hands = new IntegerArray();
	private static StringArray type = new StringArray();
	private static StringArray requirement = new StringArray();

	private static TreeMap outfitPieces = new TreeMap();
	private static SpecialOutfitArray normalOutfits = new SpecialOutfitArray();
	private static SpecialOutfitArray weirdOutfits = new SpecialOutfitArray();

	static
	{
		BufferedReader reader = getReader( "equipment.txt" );

		String [] data;
		int itemId;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				itemId = TradeableItemDatabase.getItemId( data[0] );

				if ( itemId > 0 )
				{
					power.set( itemId, parseInt( data[1] ) );
					requirement.set( itemId, data[2] );

					int hval = 0;
					String tval = null;
					if ( data.length >= 4 )
					{
						String str = data[3];
						hval = parseInt( str.substring(0,1) );
						tval = str.substring( str.indexOf( " " ) + 1 );
					}

					hands.set( itemId, hval );
					type.set( itemId, tval );
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

		reader = getReader( "outfits.txt" );

		int outfitId, arrayIndex;
		SpecialOutfitArray outfitList;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
			{
				outfitId = parseInt( data[0] );

				if ( outfitId == 0 )
				{
					arrayIndex = weirdOutfits.size();
					outfitList = weirdOutfits;
				}
				else
				{
					arrayIndex = outfitId;
					outfitList = normalOutfits;
				}

				outfitList.set( arrayIndex, new SpecialOutfit( outfitId, data[1] ) );

				String [] pieces = data[2].split( "\\s*,\\s*" );
				for ( int i = 0; i < pieces.length; ++i )
				{
					outfitPieces.put( getCanonicalName( pieces[i] ), new Integer( outfitId ) );
					outfitList.get( arrayIndex ).addPiece( new AdventureResult( pieces[i], 1, false ) );
				}
			}
		}
	}

	public static int getOutfitWithItem( int itemId )
	{
		if ( itemId < 0 )
			return -1;

		String itemName = TradeableItemDatabase.getItemName( itemId );
		if ( itemName == null )
			return -1;

		Integer result = (Integer) outfitPieces.get( getCanonicalName( itemName ) );
		return result == null ? -1 : result.intValue();
	}

	public static int getOutfitCount()
	{	return normalOutfits.size();
	}

	public static boolean contains( String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );
		return itemId > 0 && requirement.get( itemId ) != null;
	}

	public static boolean canEquip( String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 || requirement.get( itemId ) == null )
			return false;

		if ( requirement.get( itemId ).startsWith( "Mus:" ) )
			return KoLCharacter.getBaseMuscle() >= parseInt( requirement.get( itemId ).substring(5) );

		if ( requirement.get( itemId ).startsWith( "Mys:" ) )
			return KoLCharacter.getBaseMysticality() >= parseInt( requirement.get( itemId ).substring(5) );

		if ( requirement.get( itemId ).startsWith( "Mox:" ) )
			return KoLCharacter.getBaseMoxie() >= parseInt( requirement.get( itemId ).substring(5) );

		return true;
	}

	public static int getPower( int itemId )
	{	return power.get( itemId );
	}

	public static int getPower( String itemName )
	{
		if ( itemName == null )
			return 0;

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
			return 0;

		return getPower( itemId );
	}

	public static int getHands( int itemId )
	{	return hands.get( itemId );
	}

	public static int getHands( String itemName )
	{
		if ( itemName == null )
			return 0;

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
			return 0;

		return getHands( itemId );
	}

	public static String getType( int itemId )
	{	return type.get( itemId );
	}

	public static String getType( String itemName )
	{
		if ( itemName == null )
			return null;

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
			return null;

		return getType( itemId );
	}

	public static boolean isRanged( int itemId )
	{
		String req = requirement.get( itemId );
		return req != null && req.startsWith( "Mox:" );
	}

	public static boolean isRanged( String itemName )
	{
		if ( itemName == null )
			return false;

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
			return false;

		return isRanged( itemId );
	}

	public static boolean isStaff( int itemId )
	{
		String type = getType( itemId );
		return type != null && type.equals( "staff" );
	}

	public static boolean isStaff( String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
			return false;

		return isStaff( itemId );
	}

	public static boolean hasOutfit( int id )
	{	return KoLCharacter.getOutfits().contains( normalOutfits.get( id ) );
	}

	public static SpecialOutfit getOutfit( int id )
	{	return normalOutfits.get( id );
	}

	public static void updateOutfits()
	{
		ArrayList available = new ArrayList();

		for ( int i = 0; i < normalOutfits.size(); ++i )
			if ( normalOutfits.get(i) != null && normalOutfits.get(i).hasAllPieces() )
				available.add( normalOutfits.get(i) );

		for ( int i = 0; i < weirdOutfits.size(); ++i )
			if ( weirdOutfits.get(i) != null && weirdOutfits.get(i).hasAllPieces() )
				available.add( weirdOutfits.get(i) );

		Collections.sort( available );
		KoLCharacter.getOutfits().clear();

		// Start with the two constant outfits
		KoLCharacter.getOutfits().add( SpecialOutfit.NO_CHANGE );
		KoLCharacter.getOutfits().add( SpecialOutfit.BIRTHDAY_SUIT );

		// Then any custom outfits
		KoLCharacter.getOutfits().addAll( KoLCharacter.getCustomOutfits() );

		// Finally any standard outfits
		KoLCharacter.getOutfits().addAll( available );
	}

	/**
	 * Utility method which determines whether or not the equipment
	 * corresponding to the given outfit is already equipped.
	 */

	public static boolean isWearingOutfit( int outfitId )
	{
		if ( outfitId < 0 )
			return true;

		if ( outfitId == 0 )
			return false;

		return isWearingOutfit( normalOutfits.get( outfitId ) );
	}

	/**
	 * Utility method which determines whether or not the equipment
	 * corresponding to the given outfit is already equipped.
	 */

	public static boolean isWearingOutfit( SpecialOutfit outfit )
	{	return outfit != null && outfit.isWearing();
	}

	public static int getOutfitId( KoLAdventure adventure )
	{
		String adventureId = adventure.getAdventureId();

		// Knob goblin treasury has the elite guard outfit
		if ( adventureId.equals( "41" ) )
			return 5;

		// Knob goblin harem has the harem girl disguise
		if ( adventureId.equals( "42" ) || adventure.getFormSource().equals( "knob.php" ) )
			return 4;

		// The mine has mining gear
		if ( adventureId.equals( "61" ) )
			return 8;

		// The slope has eXtreme cold weather gear
		if ( adventureId.equals( "63" ) )
			return 7;

		// Hippies have a filthy hippy disguise
		if ( adventureId.equals( "26" ) || adventureId.equals( "65" ) )
			return 2;

		// Frat house has a frat house ensemble
		if ( adventureId.equals( "27" ) || adventureId.equals( "29" ) )
			return 3;

		// Pirates have a swashbuckling getup
		if ( adventureId.equals( "66" ) || adventureId.equals( "67" ) )
			return 9;

		// Choose the uniform randomly
		if ( adventureId.equals( "85" ) )
			return RNG.nextInt(2) == 0 ? 23 : 24;

		// Cloaca area requires cloaca uniforms
		if ( adventureId.equals( "86" ) )
			return 23;

		// Dyspepsi area requires dyspepsi uniforms
		if ( adventureId.equals( "87" ) )
			return 24;

		// No outfit existed for this area
		return -1;
	}

	public static boolean addOutfitConditions( KoLAdventure adventure )
	{
		int outfitId = getOutfitId( adventure );
		if ( outfitId <= 0 )
			return false;

		addOutfitConditions( outfitId );
		return true;
	}

	public static boolean retrieveOutfit( int outfitId )
	{
		if ( outfitId < 0 )
			return true;

		AdventureResult [] pieces = normalOutfits.get( outfitId ).getPieces();

		for ( int i = 0; i < pieces.length; ++i )
			if ( !KoLCharacter.hasEquipped( pieces[i] ) && !AdventureDatabase.retrieveItem( pieces[i] ) )
				return false;

		return true;
	}

	public static void addOutfitConditions( int outfitId )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitId < 0 )
			return;

		AdventureResult [] pieces = normalOutfits.get( outfitId ).getPieces();
		for ( int i = 0; i < pieces.length; ++i )
			if ( !KoLCharacter.hasEquipped( pieces[i] ) )
				DEFAULT_SHELL.executeConditionsCommand( "set " + pieces[i].getName() );
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
