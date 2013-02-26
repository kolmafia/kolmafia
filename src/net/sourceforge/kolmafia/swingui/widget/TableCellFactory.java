/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.widget;


import javax.swing.JButton;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.swingui.DatabaseFrame;
import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TableCellFactory
{
	public static Object get( int columnIndex, LockableListModel model, Object result, boolean[] flags,
			boolean isSelected )
	{
		return get( columnIndex, model, result, flags, isSelected, false );
	}

	public static Object get( int columnIndex, LockableListModel model, Object result, boolean[] flags,
			boolean isSelected, boolean raw )
	{
		if ( result instanceof AdventureResult )
		{
			AdventureResult advresult = (AdventureResult) result;

			if ( flags[ 0 ] ) // Equipment panel
			{
				return getEquipmentCell( columnIndex, isSelected, advresult, raw );
			}
			if ( flags[ 1 ] ) // Restores panel
			{
				return getRestoresCell( columnIndex, isSelected, advresult, raw );
			}
			if ( model == KoLConstants.storage )
			{
				return getStorageCell( columnIndex, isSelected, advresult, raw );
			}
			return getGeneralCell( columnIndex, isSelected, advresult, raw );
		}
		if ( result instanceof CreateItemRequest )
		{
			return getCreationCell( columnIndex, result, isSelected, raw );
		}
		if ( result instanceof LowerCaseEntry )
		{
			if ( model == DatabaseFrame.allItems )
			{
				return getAllItemsCell( columnIndex, isSelected, (LowerCaseEntry) result, raw );
			}
			return getGeneralDatabaseCell( columnIndex, isSelected, (LowerCaseEntry) result, raw );
		}
		if ( result instanceof String || result instanceof Integer || result instanceof JButton )
		{
			return result;
		}
		return null;
	}

	private static Object getGeneralDatabaseCell( int columnIndex, boolean isSelected, LowerCaseEntry result,
		boolean raw )
	{
		switch ( columnIndex )
		{
		case 0:
			return (String) result.getValue();
		case 1:
			return IntegerPool.get( (Integer) result.getKey() );
		}
		return null;
	}

	private static Object getAllItemsCell( int columnIndex, boolean isSelected, LowerCaseEntry result, boolean raw )
	{
		switch ( columnIndex )
		{
		case 0:
			return ItemDatabase.getCanonicalName( (Integer) result.getKey() );
		case 1:
			return IntegerPool.get( (Integer) result.getKey() );
		case 2:
			return IntegerPool.get( ItemDatabase.getPriceById( (Integer) result.getKey() ) );
		case 3:
			return IntegerPool.get( MallPriceDatabase.getPrice( (Integer) result.getKey() ) );
		case 4:
			return ItemDatabase.getFullness( (String) result.getValue() ) + ItemDatabase.getInebriety( (String) result.getValue() ) + ItemDatabase.getSpleenHit( (String) result.getValue() );
		case 5:
			return ItemDatabase.getAdvRangeByName( ItemDatabase.getCanonicalName( (Integer) result.getKey() ) );
		case 6:
			return ItemDatabase.getLevelReqByName( (String) result.getValue() );
		}
		return null;
	}

	private static Object getGeneralCell( int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw )
	{		
		Integer fill;

		switch ( columnIndex )
		{
		case 0:
			if ( raw )
			{
				return advresult.getName();
			}
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId(), raw );
		case 2:
			return IntegerPool.get( advresult.getCount() );
		case 3:
			Integer price = IntegerPool.get( MallPriceDatabase.getPrice( advresult.getItemId() ) );
			return ( price > 0 ) ? price : null;
		case 4:
			int power = EquipmentDatabase.getPower( advresult.getItemId() );
			return ( power > 0 ) ? IntegerPool.get( power ) : null;
		case 5:
			fill =
				IntegerPool.get( ItemDatabase.getFullness( advresult.getName() ) + ItemDatabase.getInebriety( advresult.getName() ) + ItemDatabase.getSpleenHit( advresult.getName() ) );
			return fill > 0 ? fill : null;
		case 6:
			double advRange = ItemDatabase.getAdventureRange( advresult.getName() );
			fill =
				IntegerPool.get( ItemDatabase.getFullness( advresult.getName() ) + ItemDatabase.getInebriety( advresult.getName() ) + ItemDatabase.getSpleenHit( advresult.getName() ) );
			if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
			{
				advRange = advRange / fill;
			}
			return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format( advRange ) : null;
		default:
			return null;
		}
	}

	private static Object getStorageCell( int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw )
	{
		Integer fill;

		switch ( columnIndex )
		{
		case 0:
			if ( raw )
			{
				return advresult.getName();
			}
			return "<html>" + addTag( ColorFactory.getStorageColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId(), raw );
		case 2:
			return IntegerPool.get( advresult.getCount() );
		case 3:
			Integer price = IntegerPool.get( MallPriceDatabase.getPrice( advresult.getItemId() ) );
			return ( price > 0 ) ? price : null;
		case 4:
			int power = EquipmentDatabase.getPower( advresult.getItemId() );
			return ( power > 0 ) ? IntegerPool.get( power ) : null;
		case 5:
			fill =
				IntegerPool.get( ItemDatabase.getFullness( advresult.getName() ) + ItemDatabase.getInebriety( advresult.getName() ) + ItemDatabase.getSpleenHit( advresult.getName() ) );
			return fill > 0 ? fill : null;
		case 6:
			double advRange = ItemDatabase.getAdventureRange( advresult.getName() );
			fill =
				IntegerPool.get( ItemDatabase.getFullness( advresult.getName() ) + ItemDatabase.getInebriety( advresult.getName() ) + ItemDatabase.getSpleenHit( advresult.getName() ) );

			if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
			{
				advRange = advRange / fill;
			}
			return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format( advRange ) : null;
		default:
			return null;
		}
	}

	private static Object getCreationCell( int columnIndex, Object result, boolean isSelected, boolean raw )
	{
		CreateItemRequest CIRresult = (CreateItemRequest) result;
		Integer fill;

		switch ( columnIndex )
		{
		case 0:
			if ( raw )
			{
				return CIRresult.getName();
			}
			return "<html>" + addTag( ColorFactory.getCreationColor( CIRresult ), isSelected )
				+ CIRresult.getName();
		case 1:
			return getAutosellString( CIRresult.getItemId(), raw );
		case 2:
			return IntegerPool.get( CIRresult.getQuantityPossible() );
		case 3:
			Integer price = IntegerPool.get( MallPriceDatabase.getPrice( CIRresult.getItemId() ) );
			return ( price > 0 ) ? price : null;
		case 4:
			fill =
				IntegerPool.get( ItemDatabase.getFullness( CIRresult.getName() ) + ItemDatabase.getInebriety( CIRresult.getName() ) + ItemDatabase.getSpleenHit( CIRresult.getName() ) );
			return fill > 0 ? fill : null;
		case 5:
			double advRange = ItemDatabase.getAdventureRange( CIRresult.getName() );
			fill =
				IntegerPool.get( ItemDatabase.getFullness( CIRresult.getName() ) + ItemDatabase.getInebriety( CIRresult.getName() ) + ItemDatabase.getSpleenHit( CIRresult.getName() ) );
			if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
			{
				advRange = advRange / fill;
			}
			return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format( advRange ) : null;
		case 6:
			Integer lev = ItemDatabase.getLevelReqByName( CIRresult.getName() );
			return lev != null ? IntegerPool.get( lev ) : null;
		default:
			return null;
		}
	}

	private static Object getEquipmentCell( int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw )
	{
		switch ( columnIndex )
		{
		case 0:
			if ( raw )
			{
				return advresult.getName();
			}
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return EquipmentDatabase.getPower( advresult.getItemId() );
		case 2:
			return IntegerPool.get( advresult.getCount() );
		case 3:
			Integer price = IntegerPool.get( MallPriceDatabase.getPrice( advresult.getItemId() ) );
			return ( price > 0 ) ? price : null;
		case 4:
			return getAutosellString( advresult.getItemId(), raw );
		default:
			return null;
		}
	}

	private static Object getRestoresCell( int columnIndex, boolean isSelected, AdventureResult advresult, boolean raw )
	{
		switch ( columnIndex )
		{
		case 0:
			if ( raw )
			{
				return advresult.getName();
			}
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId(), raw );
		case 2:
			return IntegerPool.get( advresult.getCount() );
		case 3:
			Integer price = IntegerPool.get( MallPriceDatabase.getPrice( advresult.getItemId() ) );
			return ( price > 0 ) ? price : null;
		case 4:
			int hpRestore = HPRestoreItemList.getHealthRestored( advresult.getName() );
			if ( hpRestore <= 0 )
			{
				return null;
			}
			int maxHP = KoLCharacter.getMaximumHP();
			if ( hpRestore > maxHP )
			{
				return IntegerPool.get( maxHP );
			}
			return IntegerPool.get( hpRestore );
		case 5:
			int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
			if ( mpRestore <= 0 )
			{
				return null;
			}
			int maxMP = KoLCharacter.getMaximumMP();
			if ( mpRestore > maxMP )
			{
				return IntegerPool.get( maxMP );
			}
			return IntegerPool.get( mpRestore );
		default:
			return null;
		}
	}

	private static String addTag( String itemColor, boolean isSelected )
	{
		if ( itemColor == null || isSelected )
		{
			return "";
		}
		return "<font color=" + itemColor + ">";
	}

	private static Object getAutosellString( int itemId, boolean raw )
	{
		int price = ItemDatabase.getPriceById( itemId );
		
		if ( raw )
		{
			//if ( price < 0 ) price = 0;
			return IntegerPool.get( price );
		}

		if ( price <= 0 )
		{
			return "no-sell";
		}
		return price + " meat";
	}

	public static String[] getColumnNames( LockableListModel originalModel, boolean[] flags )
	{
		if ( flags[ 0 ] ) // Equipment panel
		{
			return new String[]
			{
				"item name", "power", "quantity", "mallprice", "autosell"
			};
		}
		else if ( flags[ 1 ] ) // Restores panel
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "HP restore", "MP restore"
			};
		}
		else if ( originalModel == KoLConstants.inventory || originalModel == KoLConstants.tally
			|| originalModel == KoLConstants.freepulls || originalModel == KoLConstants.storage
			|| originalModel == KoLConstants.closet )
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "power", "fill", "adv/fill"
			};
		}

		else if ( originalModel == ConcoctionDatabase.getCreatables()
			|| originalModel == ConcoctionDatabase.getUsables() )
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "fill", "adv/fill", "level req"
			};
		}
		else if ( originalModel == DatabaseFrame.allItems )
		{
			return new String[]
			{
				"item name", "item ID", "autosell", "mallprice", "fill", "adv range", "level req"
			};
		}
		else if ( originalModel == DatabaseFrame.allFamiliars )
		{
			return new String[]
			{
				"familiar name", "familiar ID",
			};
		}
		else if ( originalModel == DatabaseFrame.allEffects )
		{
			return new String[]
			{
				"effect name", "effect ID",
			};
		}
		else if ( originalModel == DatabaseFrame.allSkills )
		{
			return new String[]
			{
				"skill name", "skill ID",
			};
		}
		return new String[]
		{
			"not implemented"
		};
	}

	public static String getTooltipText( Object value, boolean[] flags )
	{
		if ( value instanceof AdventureResult || value instanceof CreateItemRequest )
		{
			return getModifiers( value );
		}
		return null;
	}

	private static String getModifiers( Object value )
	{
		// Code almost entirely lifted from GearChangeFrame.

		int modifiersWidth = 100;
		String name = null;
		if ( value instanceof AdventureResult )
		{
			name = ( (AdventureResult) value ).getName();
		}
		else if ( value instanceof CreateItemRequest )
		{
			name = ( (CreateItemRequest) value ).getName();
		}
		
		if ( !EquipmentDatabase.isEquipment( ItemDatabase.getConsumptionType( name ) ) )
		{
			return null;
		}

		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return null;
		}
		name = mods.getString( Modifiers.INTRINSIC_EFFECT );
		if ( name.length() > 0 )
		{
			Modifiers newMods = new Modifiers();
			newMods.add( mods );
			newMods.add( Modifiers.getModifiers( name ) );
			mods = newMods;
		}

		StringBuilder buff = new StringBuilder();
		buff.append( "<html><table><tr><td width=" );
		buff.append( modifiersWidth );
		buff.append( ">" );

		for ( int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i )
		{
			double val = mods.get( i );
			if ( val == 0.0 ) continue;
			name = Modifiers.getModifierName( i );
			name = StringUtilities.singleStringReplace( name, "Familiar", "Fam" );
			name = StringUtilities.singleStringReplace( name, "Experience", "Exp" );
			name = StringUtilities.singleStringReplace( name, "Damage", "Dmg" );
			name = StringUtilities.singleStringReplace( name, "Resistance", "Res" );
			name = StringUtilities.singleStringReplace( name, "Percent", "%" );
			buff.append( name );
			buff.append( ":<div align=right>" );
			buff.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( val ) );
			buff.append( "</div>" );
		}

		boolean anyBool = false;
		for ( int i = 1; i < Modifiers.BITMAP_MODIFIERS; ++i )
		{
			if ( mods.getRawBitmap( i ) == 0 ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBitmapModifierName( i ) );
		}

		for ( int i = 1; i < Modifiers.BOOLEAN_MODIFIERS; ++i )
		{
			if ( !mods.getBoolean( i ) ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBooleanModifierName( i ) );
		}

		buff.append( "</td></tr></table></html>" );
		return buff.toString();
	}

}
