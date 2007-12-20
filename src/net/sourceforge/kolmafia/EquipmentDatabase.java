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
import java.util.Collections;
import java.util.TreeMap;

public class EquipmentDatabase
	extends KoLDatabase
{
	private static final IntegerArray power = new IntegerArray();
	private static final IntegerArray hands = new IntegerArray();
	private static final StringArray type = new StringArray();
	private static final StringArray requirement = new StringArray();

	private static final TreeMap outfitPieces = new TreeMap();
	private static final SpecialOutfitArray normalOutfits = new SpecialOutfitArray();
	private static final SpecialOutfitArray weirdOutfits = new SpecialOutfitArray();

	static
	{
		BufferedReader reader = KoLDatabase.getVersionedReader( "equipment.txt", KoLConstants.EQUIPMENT_VERSION );

		String[] data;
		int itemId;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				itemId = TradeableItemDatabase.getItemId( data[ 0 ] );

				if ( itemId > 0 )
				{
					EquipmentDatabase.power.set( itemId, StaticEntity.parseInt( data[ 1 ] ) );
					EquipmentDatabase.requirement.set( itemId, data[ 2 ] );

					int hval = 0;
					String tval = null;
					if ( data.length >= 4 )
					{
						String str = data[ 3 ];
						hval = StaticEntity.parseInt( str.substring( 0, 1 ) );
						tval = str.substring( str.indexOf( " " ) + 1 );
					}

					EquipmentDatabase.hands.set( itemId, hval );
					EquipmentDatabase.type.set( itemId, tval );
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

			StaticEntity.printStackTrace( e );
		}

		reader = KoLDatabase.getVersionedReader( "outfits.txt", KoLConstants.OUTFITS_VERSION );

		int outfitId, arrayIndex;
		SpecialOutfitArray outfitList;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 3 )
			{
				outfitId = StaticEntity.parseInt( data[ 0 ] );

				if ( outfitId == 0 )
				{
					arrayIndex = EquipmentDatabase.weirdOutfits.size();
					outfitList = EquipmentDatabase.weirdOutfits;
				}
				else
				{
					arrayIndex = outfitId;
					outfitList = EquipmentDatabase.normalOutfits;
				}

				outfitList.set( arrayIndex, new SpecialOutfit( outfitId, data[ 1 ] ) );

				String[] pieces = data[ 2 ].split( "\\s*,\\s*" );
				for ( int i = 0; i < pieces.length; ++i )
				{
					EquipmentDatabase.outfitPieces.put( KoLDatabase.getCanonicalName( pieces[ i ] ), new Integer(
						outfitId ) );
					outfitList.get( arrayIndex ).addPiece( new AdventureResult( pieces[ i ], 1, false ) );
				}
			}
		}
	}

	public static final int getOutfitWithItem( final int itemId )
	{
		if ( itemId < 0 )
		{
			return -1;
		}

		String itemName = TradeableItemDatabase.getItemName( itemId );
		if ( itemName == null )
		{
			return -1;
		}

		Integer result = (Integer) EquipmentDatabase.outfitPieces.get( KoLDatabase.getCanonicalName( itemName ) );
		return result == null ? -1 : result.intValue();
	}

	public static final int getOutfitCount()
	{
		return EquipmentDatabase.normalOutfits.size();
	}

	public static final boolean contains( final String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );
		return itemId > 0 && EquipmentDatabase.requirement.get( itemId ) != null;
	}

	public static final boolean canEquip( final String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 || EquipmentDatabase.requirement.get( itemId ) == null )
		{
			return false;
		}

		if ( EquipmentDatabase.requirement.get( itemId ).startsWith( "Mus:" ) )
		{
			return KoLCharacter.getBaseMuscle() >= StaticEntity.parseInt( EquipmentDatabase.requirement.get( itemId ).substring(
				5 ) );
		}

		if ( EquipmentDatabase.requirement.get( itemId ).startsWith( "Mys:" ) )
		{
			return KoLCharacter.getBaseMysticality() >= StaticEntity.parseInt( EquipmentDatabase.requirement.get(
				itemId ).substring( 5 ) );
		}

		if ( EquipmentDatabase.requirement.get( itemId ).startsWith( "Mox:" ) )
		{
			return KoLCharacter.getBaseMoxie() >= StaticEntity.parseInt( EquipmentDatabase.requirement.get( itemId ).substring(
				5 ) );
		}

		return true;
	}

	public static final int getPower( final int itemId )
	{
		return EquipmentDatabase.power.get( itemId );
	}

	public static final int getPower( final String itemName )
	{
		if ( itemName == null )
		{
			return 0;
		}

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return 0;
		}

		return EquipmentDatabase.getPower( itemId );
	}

	public static final int getHands( final int itemId )
	{
		return EquipmentDatabase.hands.get( itemId );
	}

	public static final int getHands( final String itemName )
	{
		if ( itemName == null )
		{
			return 0;
		}

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return 0;
		}

		return EquipmentDatabase.getHands( itemId );
	}

	public static final String getType( final int itemId )
	{
		String res = EquipmentDatabase.type.get( itemId );
		return res == null ? "" : res;
	}

	public static final String getType( final String itemName )
	{
		if ( itemName == null )
		{
			return "";
		}

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return "";
		}

		return EquipmentDatabase.getType( itemId );
	}

	public static final String getReq( final int itemId )
	{
		String req = EquipmentDatabase.requirement.get( itemId );
		if ( req != null )
		{
			return req;
		}
		return "none";
	}

	public static final String getReq( final String itemName )
	{
		if ( itemName == null )
		{
			return "none";
		}

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return "none";
		}

		return EquipmentDatabase.getReq( itemId );
	}

	public static final int equipStat( final int itemId )
	{
		String req = EquipmentDatabase.requirement.get( itemId );

		if ( req != null )
		{
			if ( req.startsWith( "Mox:" ) )
			{
				return KoLConstants.MOXIE;
			}
			if ( req.startsWith( "Mys:" ) )
			{
				return KoLConstants.MYSTICALITY;
			}
		}
		return KoLConstants.MUSCLE;
	}

	public static final int equipStat( final String itemName )
	{
		if ( itemName == null )
		{
			return KoLConstants.MUSCLE;
		}

		int itemId = TradeableItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return KoLConstants.MUSCLE;
		}

		return EquipmentDatabase.equipStat( itemId );
	}

	public static final boolean isRanged( final int itemId )
	{
		return EquipmentDatabase.equipStat( itemId ) == KoLConstants.MOXIE;
	}

	public static final boolean isRanged( final String itemName )
	{
		return EquipmentDatabase.equipStat( itemName ) == KoLConstants.MOXIE;
	}

	public static final boolean hasOutfit( final int id )
	{
		return KoLCharacter.getOutfits().contains( EquipmentDatabase.normalOutfits.get( id ) );
	}

	public static final SpecialOutfit getOutfit( final int id )
	{
		return EquipmentDatabase.normalOutfits.get( id );
	}

	public static final void updateOutfits()
	{
		ArrayList available = new ArrayList();

		for ( int i = 0; i < EquipmentDatabase.normalOutfits.size(); ++i )
		{
			if ( EquipmentDatabase.normalOutfits.get( i ) != null && EquipmentDatabase.normalOutfits.get( i ).hasAllPieces() )
			{
				available.add( EquipmentDatabase.normalOutfits.get( i ) );
			}
		}

		for ( int i = 0; i < EquipmentDatabase.weirdOutfits.size(); ++i )
		{
			if ( EquipmentDatabase.weirdOutfits.get( i ) != null && EquipmentDatabase.weirdOutfits.get( i ).hasAllPieces() )
			{
				available.add( EquipmentDatabase.weirdOutfits.get( i ) );
			}
		}

		Collections.sort( available );
		KoLCharacter.getOutfits().clear();

		// Start with the two constant outfits
		KoLCharacter.getOutfits().add( SpecialOutfit.NO_CHANGE );
		KoLCharacter.getOutfits().add( SpecialOutfit.BIRTHDAY_SUIT );

		// Finally any standard outfits
		KoLCharacter.getOutfits().addAll( available );
	}

	/**
	 * Utility method which determines whether or not the equipment corresponding to the given outfit is already
	 * equipped.
	 */

	public static final boolean isWearingOutfit( final int outfitId )
	{
		if ( outfitId < 0 )
		{
			return true;
		}

		if ( outfitId == 0 )
		{
			return false;
		}

		return EquipmentDatabase.isWearingOutfit( EquipmentDatabase.normalOutfits.get( outfitId ) );
	}

	/**
	 * Utility method which determines whether or not the equipment corresponding to the given outfit is already
	 * equipped.
	 */

	public static final boolean isWearingOutfit( final SpecialOutfit outfit )
	{
		return outfit != null && outfit.isWearing();
	}

	/**
	 * Utility method which determines the outfit ID the character is currently wearing
	 */

	public static final SpecialOutfit currentOutfit()
	{
		for ( int id = 1; id <= EquipmentDatabase.normalOutfits.size(); ++id )
		{
			SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( id );
			if ( outfit == null )
			{
				continue;
			}
			if ( outfit.isWearing() )
			{
				return outfit;
			}
		}

		return null;
	}

	public static final int getOutfitId( final KoLAdventure adventure )
	{
		String adventureId = adventure.getAdventureId();

		// Knob goblin treasury has the elite guard outfit
		if ( adventureId.equals( "41" ) )
		{
			return 5;
		}

		// Knob goblin harem has the harem girl disguise
		if ( adventureId.equals( "42" ) || adventure.getFormSource().equals( "knob.php" ) )
		{
			return 4;
		}

		// The mine has mining gear
		if ( adventureId.equals( "61" ) )
		{
			return 8;
		}

		// The slope has eXtreme cold weather gear
		if ( adventureId.equals( "63" ) )
		{
			return 7;
		}

		// Hippies have a filthy hippy disguise
		if ( adventureId.equals( "26" ) || adventureId.equals( "65" ) )
		{
			return 2;
		}

		// Frat house has a frat house ensemble
		if ( adventureId.equals( "27" ) || adventureId.equals( "29" ) )
		{
			return 3;
		}

		// Pirates have a swashbuckling getup
		if ( adventureId.equals( "66" ) || adventureId.equals( "67" ) )
		{
			return 9;
		}

		// Choose the uniform randomly
		if ( adventureId.equals( "85" ) )
		{
			return KoLConstants.RNG.nextInt( 2 ) == 0 ? 23 : 24;
		}

		// Cloaca area requires cloaca uniforms
		if ( adventureId.equals( "86" ) )
		{
			return 23;
		}

		// Dyspepsi area requires dyspepsi uniforms
		if ( adventureId.equals( "87" ) )
		{
			return 24;
		}

		// No outfit existed for this area
		return -1;
	}

	public static final boolean addOutfitConditions( final KoLAdventure adventure )
	{
		int outfitId = EquipmentDatabase.getOutfitId( adventure );
		if ( outfitId <= 0 )
		{
			return false;
		}

		EquipmentDatabase.addOutfitConditions( outfitId );
		return true;
	}

	public static final boolean retrieveOutfit( final int outfitId )
	{
		if ( outfitId < 0 || outfitId == Integer.MAX_VALUE )
		{
			return true;
		}

		AdventureResult[] pieces = EquipmentDatabase.normalOutfits.get( outfitId ).getPieces();

		for ( int i = 0; i < pieces.length; ++i )
		{
			if ( !KoLCharacter.hasEquipped( pieces[ i ] ) && !AdventureDatabase.retrieveItem( pieces[ i ] ) )
			{
				return false;
			}
		}

		return true;
	}

	public static final void addOutfitConditions( final int outfitId )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitId < 0 )
		{
			return;
		}

		AdventureResult[] pieces = EquipmentDatabase.normalOutfits.get( outfitId ).getPieces();
		for ( int i = 0; i < pieces.length; ++i )
		{
			if ( !KoLCharacter.hasEquipped( pieces[ i ] ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeConditionsCommand( "set " + pieces[ i ].getName() );
			}
		}
	}

	/**
	 * Internal class which functions exactly an array of concoctions, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	private static class SpecialOutfitArray
	{
		private final ArrayList internalList = new ArrayList();

		public SpecialOutfit get( final int index )
		{
			return index < 0 || index >= this.internalList.size() ? null : (SpecialOutfit) this.internalList.get( index );
		}

		public void set( final int index, final SpecialOutfit value )
		{
			for ( int i = this.internalList.size(); i <= index; ++i )
			{
				this.internalList.add( null );
			}

			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
