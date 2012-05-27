package net.sourceforge.kolmafia.swingui.widget;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.request.CreateItemRequest;

public class TableCellFactory
{
	public static Object get( int columnIndex, LockableListModel model, Object result, boolean isEquipmentOnly )
	{
		if ( result instanceof AdventureResult )
		{
			AdventureResult advresult = (AdventureResult) result;

			if ( isEquipmentOnly )
			{
				switch ( columnIndex )
				{
				case 0:
					return "<html>" + advresult.getName();
				case 1:
					return EquipmentDatabase.getPower( advresult.getItemId() );
				case 2:
					return Integer.valueOf( advresult.getCount() );
				case 3:
					int price = MallPriceDatabase.getPrice( advresult.getItemId() );
					return ( price > 0 ) ? price : null;
				case 4:
					return getAutosellString( advresult.getItemId() );
				case 5:
					int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
					if ( mpRestore <= 0 )
					{
						return null;
					}
					int maxMP = KoLCharacter.getMaximumMP();
					if ( mpRestore > maxMP )
					{
						return maxMP;
					}
					return mpRestore;
				default:
					return null;
				}

			}
			switch ( columnIndex )
			{
			case 0:
				return "<html>" + advresult.getName();
			case 1:
				return getAutosellString( advresult.getItemId() );
			case 2:
				return Integer.valueOf( advresult.getCount() );
			case 3:
				int price = MallPriceDatabase.getPrice( advresult.getItemId() );
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
					return maxHP;
				}
				return hpRestore;
			case 5:
				int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
				if ( mpRestore <= 0 )
				{
					return null;
				}
				int maxMP = KoLCharacter.getMaximumMP();
				if ( mpRestore > maxMP )
				{
					return maxMP;
				}
				return mpRestore;
			default:
				return null;
			}
		}
		if ( result instanceof CreateItemRequest )
		{
			CreateItemRequest CIRresult = (CreateItemRequest) result;

			switch ( columnIndex )
			{
			case 0:
				return "<html>" + CIRresult.getName();
			case 1:
				return getAutosellString( CIRresult.getItemId() );
			case 2:
				return Integer.valueOf( CIRresult.getQuantityPossible() );
			case 3:
				int price = MallPriceDatabase.getPrice( CIRresult.getItemId() );
				return ( price > 0 ) ? price : null;
			default:
				return null;
			}
		}
		return null;
	}

	private static String getAutosellString( int itemId )
	{
		int price = ItemDatabase.getPriceById( itemId );

		if ( price <= 0 )
		{
			return "no-sell";
		}
		return price + " meat";
	}

}
