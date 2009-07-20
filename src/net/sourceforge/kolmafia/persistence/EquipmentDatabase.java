/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentDatabase
	extends KoLDatabase
{
	private static final IntegerArray power = new IntegerArray();
	private static final IntegerArray hands = new IntegerArray();
	private static final StringArray itemTypes = new StringArray();
	private static final StringArray statRequirements = new StringArray();

	private static final HashMap outfitPieces = new HashMap();
	public static final SpecialOutfitArray normalOutfits = new SpecialOutfitArray();
	public static final SpecialOutfitArray weirdOutfits = new SpecialOutfitArray();
	
	private static final IntegerArray pulverize = new IntegerArray();
	// Values in pulverize are one of:
	//	0 - not initialized yet
	//	positive - ID of special-case pulverize result (worthless powder, epic wad, etc.)
	//	-1 - not pulverizable (quest item, Mr. Store item, etc.)
	//	other negative - bitmap, some combination of PULVERIZE_BITS, YIELD_x, ELEM_x
	public static final int PULVERIZE_BITS = 0x80000000;	// makes value negative
	public static final int YIELD_UNCERTAIN = 0x001;
	public static final int YIELD_1P = 0x002;
	public static final int YIELD_2P = 0x004;
	public static final int YIELD_3P = 0x008;
	public static final int YIELD_4P_1N = 0x010;
	public static final int YIELD_1N3P_2N = 0x020;
	public static final int YIELD_3N = 0x040;
	public static final int YIELD_4N_1W = 0x080;
	public static final int YIELD_1W3N_2W = 0x100;
	public static final int YIELD_3W = 0x200;
	public static final int MASK_YIELD = 0x3FF;
	public static final int ELEM_TWINKLY = 0x01000;
	public static final int ELEM_HOT = 0x02000;
	public static final int ELEM_COLD = 0x04000;
	public static final int ELEM_STENCH = 0x08000;
	public static final int ELEM_SPOOKY = 0x10000;
	public static final int ELEM_SLEAZE = 0x20000;
	public static final int ELEM_OTHER = 0x40000;
	public static final int MASK_ELEMENT = 0x7F000;
	public static final int MALUS_UPGRADE = 0x100000;
	
	public static final int[] IMPLICATIONS = {
		Modifiers.COLD_RESISTANCE, ELEM_HOT | ELEM_SPOOKY,
		Modifiers.HOT_RESISTANCE, ELEM_STENCH | ELEM_SLEAZE,
		Modifiers.SLEAZE_RESISTANCE, ELEM_COLD | ELEM_SPOOKY,
		Modifiers.SPOOKY_RESISTANCE, ELEM_HOT | ELEM_STENCH,
		Modifiers.STENCH_RESISTANCE, ELEM_COLD | ELEM_SLEAZE,
		Modifiers.COLD_DAMAGE, ELEM_COLD,
		Modifiers.HOT_DAMAGE, ELEM_HOT,
		Modifiers.SLEAZE_DAMAGE, ELEM_SLEAZE,
		Modifiers.SPOOKY_DAMAGE, ELEM_SPOOKY,
		Modifiers.STENCH_DAMAGE, ELEM_STENCH,
		Modifiers.COLD_SPELL_DAMAGE, ELEM_COLD,
		Modifiers.HOT_SPELL_DAMAGE, ELEM_HOT,
		Modifiers.SLEAZE_SPELL_DAMAGE, ELEM_SLEAZE,
		Modifiers.SPOOKY_SPELL_DAMAGE, ELEM_SPOOKY,
		Modifiers.STENCH_SPELL_DAMAGE, ELEM_STENCH,
	};

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "equipment.txt", KoLConstants.EQUIPMENT_VERSION );

		String[] data;
		int itemId;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 3 )
			{
				continue;
			}

			itemId = ItemDatabase.getItemId( data[ 0 ] );
			if ( itemId < 0 )
			{
				continue;
			}

			EquipmentDatabase.power.set( itemId, StringUtilities.parseInt( data[ 1 ] ) );
			EquipmentDatabase.statRequirements.set( itemId, data[ 2 ] );

			int hval = 0;
			String tval = null;

			if ( data.length >= 4 )
			{
				String str = data[ 3 ];
				int index = str.indexOf( " " );
				if ( index > 0 )
				{
					hval = StringUtilities.parseInt( str.substring( 0, 1 ) );
					tval = str.substring( index + 1 );
				}
				else
				{
					tval = str;
				}
			}

			EquipmentDatabase.hands.set( itemId, hval );
			EquipmentDatabase.itemTypes.set( itemId, tval );
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

		reader = FileUtilities.getVersionedReader( "outfits.txt", KoLConstants.OUTFITS_VERSION );

		int outfitId, arrayIndex;
		SpecialOutfitArray outfitList;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length == 3 )
			{
				outfitId = StringUtilities.parseInt( data[ 0 ] );

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
					EquipmentDatabase.outfitPieces.put( StringUtilities.getCanonicalName( pieces[ i ] ), new Integer(
						outfitId ) );
					outfitList.get( arrayIndex ).addPiece( new AdventureResult( pieces[ i ], 1, false ) );
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

		reader = FileUtilities.getVersionedReader( "pulverize.txt", KoLConstants.PULVERIZE_VERSION );

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 2 )
			{
				continue;
			}

			itemId = ItemDatabase.getItemId( data[ 0 ] );
			if ( itemId < 0 )
			{
				continue;
			}

			if ( data[ 1 ].equals( "nosmash" ) )
			{
				EquipmentDatabase.pulverize.set( itemId, -1 );
			}
			else if ( data[ 1 ].equals( "upgrade" ) )
			{
				EquipmentDatabase.pulverize.set( itemId, 
					EquipmentDatabase.deriveUpgrade( data[ 0 ] ) );
			}
			else
			{
				int resultId = ItemDatabase.getItemId( data[ 1 ] );
				EquipmentDatabase.pulverize.set( itemId, resultId );
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
	
	public static final int nextEquipmentItemId( int prevId )
	{
		while ( ++prevId < EquipmentDatabase.statRequirements.size() )
		{
			if ( EquipmentDatabase.statRequirements.get( prevId ).length() > 0 )
			{
				return prevId;
			}
		}
		return -1;
	}

	public static final int getOutfitWithItem( final int itemId )
	{
		if ( itemId < 0 )
		{
			return -1;
		}

		String itemName = ItemDatabase.getItemName( itemId );
		if ( itemName == null )
		{
			return -1;
		}

		Integer result = (Integer) EquipmentDatabase.outfitPieces.get( StringUtilities.getCanonicalName( itemName ) );
		return result == null ? -1 : result.intValue();
	}

	public static final int getOutfitCount()
	{
		return EquipmentDatabase.normalOutfits.size();
	}

	public static final boolean contains( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		return itemId > 0 && EquipmentDatabase.statRequirements.get( itemId ) != null;
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

		int itemId = ItemDatabase.getItemId( itemName );

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

		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return 0;
		}

		return EquipmentDatabase.getHands( itemId );
	}

	public static final String getEquipRequirement( final int itemId )
	{
		String req = EquipmentDatabase.statRequirements.get( itemId );

		if ( req != null )
		{
			return req;
		}

		return "none";
	}

	public static final String getEquipRequirement( final String itemName )
	{
		if ( itemName == null )
		{
			return "none";
		}

		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return "none";
		}

		return EquipmentDatabase.getEquipRequirement( itemId );
	}

	public static final String getItemType( final int itemId )
	{
		switch ( ItemDatabase.getConsumptionType( itemId ) )
		{
		case KoLConstants.CONSUME_EAT:
			return "food";
		case KoLConstants.CONSUME_DRINK:
			return "booze";
		case KoLConstants.CONSUME_FOOD_HELPER:
			return "food helper";
		case KoLConstants.CONSUME_DRINK_HELPER:
			return "drink helper";
		case KoLConstants.CONSUME_STICKER:
			return "sticker";
		case KoLConstants.GROW_FAMILIAR:
			return "familiar larva";
		case KoLConstants.CONSUME_ZAP:
			return "zap wand";
		case KoLConstants.EQUIP_FAMILIAR:
			return "familiar equipment";
		case KoLConstants.EQUIP_ACCESSORY:
			return "accessory";
		case KoLConstants.EQUIP_HAT:
			return "hat";
		case KoLConstants.EQUIP_PANTS:
			return "pants";
		case KoLConstants.EQUIP_SHIRT:
			return "shirt";
		case KoLConstants.EQUIP_WEAPON:
			return EquipmentDatabase.itemTypes.get( itemId );
		case KoLConstants.EQUIP_OFFHAND:
			String type = EquipmentDatabase.itemTypes.get( itemId );
			return type != null ? type : "offhand";
		case KoLConstants.MP_RESTORE:
			return "mp restore";
		case KoLConstants.HP_RESTORE:
			return "hp restore";
		case KoLConstants.HPMP_RESTORE:
			return "hp+mp restore";
		case KoLConstants.EQUIP_CONTAINER:
			return "container";
		default:
			return "";
		}
	}

	public static final int getWeaponStat( final int itemId )
	{
		int consumptionType = ItemDatabase.getConsumptionType( itemId );

		if ( consumptionType != KoLConstants.EQUIP_WEAPON )
		{
			return KoLConstants.NONE;
		}

		String req = EquipmentDatabase.getEquipRequirement( itemId );

		if ( req.startsWith( "Mox:" ) )
		{
			return KoLConstants.MOXIE;
		}

		if ( req.startsWith( "Mys:" ) )
		{
			return KoLConstants.MYSTICALITY;
		}

		return KoLConstants.MUSCLE;
	}

	public static final int getWeaponStat( final String itemName )
	{
		if ( itemName == null )
		{
			return KoLConstants.NONE;
		}

		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return KoLConstants.NONE;
		}

		return EquipmentDatabase.getWeaponStat( itemId );
	}

	public static final int getWeaponType( final int itemId )
	{
		switch ( EquipmentDatabase.getWeaponStat( itemId ) )
		{
		case KoLConstants.NONE:
			return KoLConstants.NONE;
		case KoLConstants.MOXIE:
			return KoLConstants.RANGED;
		default:
			return KoLConstants.MELEE;
		}
	}

	public static final int getWeaponType( final String itemName )
	{
		if ( itemName == null )
		{
			return KoLConstants.NONE;
		}

		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return KoLConstants.NONE;
		}

		return EquipmentDatabase.getWeaponType( itemId );
	}
	
	public static final int getPulverization( final String itemName )
	{
		if ( itemName == null )
		{
			return -1;
		}

		int itemId = ItemDatabase.getItemId( itemName );

		if ( itemId == -1 )
		{
			return -1;
		}

		return EquipmentDatabase.getPulverization( itemId );
	}
	
	public static final int getPulverization( final int id )
	{
		if ( id < 0 )
		{
			return -1;
		}
		int pulver = EquipmentDatabase.pulverize.get( id );
		if ( pulver == 0 )
		{
			pulver = EquipmentDatabase.derivePulverization( id );
			EquipmentDatabase.pulverize.set( id, pulver );
		}
		return pulver;
	}
	
	private static final int derivePulverization( final int id )
	{
		switch ( ItemDatabase.getConsumptionType( id ) )
		{
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
			break;
		
		default:
			return -1;
		}
	
		if ( !ItemDatabase.isDisplayable( id ) )
		{	// quest item
			return -1;
		}
		if ( ItemDatabase.isGiftable( id ) && !ItemDatabase.isTradeable( id ) )
		{	// gift item
			return ItemPool.USELESS_POWDER;
		}

		String name = ItemDatabase.getItemName( id );
		if ( NPCStoreDatabase.contains( name, false ) )
		{
			return ItemPool.USELESS_POWDER;
		}
		
		int pulver = PULVERIZE_BITS | ELEM_TWINKLY;
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{	// Apparently no enchantments at all, which would imply that this
			// item pulverizes to useless powder.  However, there are many items
			// with enchantments that don't correspond to a KoLmafia modifier
			// (the "They do nothing!" enchantment of beer goggles, for example),
			// so this can't safely be assumed, so for now all truly unenchanted
			// items will have to be explicitly listed in pulverize.txt.
			pulver |= EquipmentDatabase.ELEM_TWINKLY;
		}
		else
		{
			for ( int i = 0; i < IMPLICATIONS.length; i += 2 )
			{
				if ( mods.get( IMPLICATIONS[ i ] ) > 0.0f )
				{
					pulver |= IMPLICATIONS[ i+1 ];
				}
			}
		}
		
		int power = EquipmentDatabase.power.get( id );
		if ( power <= 0 )
		{
			// power is unknown, derive from requirement (which isn't always accurate)
			pulver |= YIELD_UNCERTAIN;
			String req = EquipmentDatabase.statRequirements.get( id );
			if ( req != null )
			{
				power = StringUtilities.parseInt( req ) * 2 + 30;
			}
		}
		if ( power >= 180 )
		{
			pulver |= YIELD_3W;
		}
		else if ( power >= 160 )
		{
			pulver |= YIELD_1W3N_2W;
		}
		else if ( power >= 140 )
		{
			pulver |= YIELD_4N_1W;
		}
		else if ( power >= 120 )
		{
			pulver |= YIELD_3N;
		}
		else if ( power >= 100 )
		{
			pulver |= YIELD_1N3P_2N;
		}
		else if ( power >= 80 )
		{
			pulver |= YIELD_4P_1N;
		}
		else if ( power >= 60 )
		{
			pulver |= YIELD_3P;
		}
		else if ( power >= 40 )
		{
			pulver |= YIELD_2P;
		}
		else
		{
			pulver |= YIELD_1P;
		}
		
		return pulver;
	}

	private static final int deriveUpgrade( final String name )
	{
		int pulver = PULVERIZE_BITS | MALUS_UPGRADE | YIELD_4N_1W;
		if ( name.endsWith( "powder" ) )
		{
			pulver |= YIELD_4P_1N;
		}
		
		if ( name.startsWith( "twinkly" ) )
		{
			pulver |= ELEM_TWINKLY;
		}
		else if ( name.startsWith( "hot" ) )
		{
			pulver |= ELEM_HOT;
		}
		else if ( name.startsWith( "cold" ) )
		{
			pulver |= ELEM_COLD;
		}
		else if ( name.startsWith( "stench" ) )
		{
			pulver |= ELEM_STENCH;
		}
		else if ( name.startsWith( "spook" ) )
		{
			pulver |= ELEM_SPOOKY;
		}
		else if ( name.startsWith( "sleaz" ) )
		{
			pulver |= ELEM_SLEAZE;
		}
		else
		{
			pulver |= ELEM_OTHER;
		}
		return pulver;
	}

	public static final SpecialOutfit getOutfit( final int id )
	{
		return EquipmentDatabase.normalOutfits.get( id );
	}

	public static final SpecialOutfit getAvailableOutfit( final int id )
	{
		SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( id );
		return EquipmentManager.getOutfits().contains( outfit ) ? outfit : null;
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

	/**
	 * Internal class which functions exactly an array of concoctions, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	public static class SpecialOutfitArray
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
