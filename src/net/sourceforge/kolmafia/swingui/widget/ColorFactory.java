package net.sourceforge.kolmafia.swingui.widget;

import java.util.HashMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public final class ColorFactory
{
	private final static HashMap<String, String> colorPrefMap = new HashMap<String, String>();

	static
	{
		String rawPref = Preferences.getString( "textColors" );
		String[] splitPref = rawPref.split( "\\|" );

		for ( int i = 0; i < splitPref.length; ++i )
		{
			String[] it = splitPref[ i ].split( ":" );
			if ( it.length == 2 )
			{
				colorPrefMap.put( it[ 0 ], it[ 1 ] );
			}
		}
	}

	public static String getItemColor( AdventureResult ar )
	{
		String color = null;

		color = checkOptionalColors( ar.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
		{
			color = getMementoColor();
		}
		else if ( KoLConstants.junkList.contains( ar ) )
		{
			color = getJunkColor();
		}
		else
		{
			color = ColorFactory.getQualityColor( ar.getName() );
		}
		return color;
	}

	public static String getCreationColor( CreateItemRequest icr )
	{
		return ColorFactory.getCreationColor( icr, false );
	}

	public static String getCreationColor( CreateItemRequest icr, boolean isEquipment )
	{
		String color = null;

		color = checkOptionalColors( icr.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( KoLConstants.junkList.contains( icr.createdItem ) )
		{
			color = getJunkColor();
		}
		else if ( !isEquipment )
		{
			color = ColorFactory.getQualityColor( icr.getName() );
		}
		return color;
	}

	public static String getConcoctionColor( Concoction item )
	{
		String name = item.getName();

		String color = checkOptionalColors( item.getItemId() );

		if ( color != null )
		{
			return color;
		}

		return	ConsumablesDatabase.meetsLevelRequirement( name ) ?
			ColorFactory.getQualityColor( name ) :
			getNotAvailableColor();
	}

	public static String getStorageColor( AdventureResult ar )
	{
		String color = null;
		String name = ar.getName();

		color = checkOptionalColors( ar.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( !ConsumablesDatabase.meetsLevelRequirement( name ) || !EquipmentManager.canEquip( name ) )
		{
			color = getNotAvailableColor();
		}
		else
		{
			color = ColorFactory.getQualityColor( name );
		}
		return color;
	}

	public static final String getQualityColor( final String name )
	{
		String pref;
		String color = null;
		String quality = ConsumablesDatabase.getQuality( name );

		if ( quality == null )
		{
			return null;
		}
		if ( quality.equals( ConsumablesDatabase.CRAPPY ) )
		{
			pref = checkPref( "crappy" );
			color = pref != null ? pref : "#999999";
		}
		else if ( quality.equals( ConsumablesDatabase.DECENT ) )
		{
			pref = checkPref( "decent" );
			color = pref;
		}
		else if ( quality.equals( ConsumablesDatabase.GOOD ) )
		{
			pref = checkPref( "good" );
			color = pref != null ? pref : "green";
		}
		else if ( quality.equals( ConsumablesDatabase.AWESOME ) )
		{
			pref = checkPref( "awesome" );
			color = pref != null ? pref : "blue";
		}
		else if ( quality.equals( ConsumablesDatabase.EPIC ) )
		{
			pref = checkPref( "epic" );
			color = pref != null ? pref : "#8a2be2";
		}
		return color;
	}

	private static String getJunkColor()
	{
		String pref = checkPref( "junk" );
		if ( pref == null )
		{
			return "gray";
		}
		return pref;
	}

	private static String getMementoColor()
	{
		String pref = checkPref( "memento" );
		if ( pref == null )
		{
			return "olive";
		}
		return pref;
	}

	private static String getNotAvailableColor()
	{
		String pref = checkPref( "notavailable" );
		if ( pref == null )
		{
			return "gray";
		}
		return pref;
	}

	private static String getQuestColor()
	{
		return checkPref( "quest" );
	}

	private static String getNotTradeableColor()
	{
		return checkPref( "nontradeable" );
	}

	private static String getGiftColor()
	{
		return checkPref( "gift" );
	}

	private static String checkOptionalColors( int itemId )
	{
		if ( ItemDatabase.isGiftable( itemId ) && !ItemDatabase.isTradeable( itemId ) )
		{
			// gift items
			String it = getGiftColor();
			if ( it != null )
				return it;
		}
		if ( ItemDatabase.isQuestItem( itemId ) )
		{
			// quest items
			String it = getQuestColor();
			if ( it != null )
				return it;
		}
		if ( !ItemDatabase.isTradeable( itemId ) )
		{
			String it = getNotTradeableColor();
			if ( it != null )
				return it;
		}

		return null;
	}

	private static String checkPref( String pref )
	{
		Object it = colorPrefMap.get( pref );
		if ( it != null )
		{
			return it.toString();
		}
		return null;
	}
}
