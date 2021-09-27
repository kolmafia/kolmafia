package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.HellKitchenRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KitchenCommand
	extends AbstractCommand
{
	public KitchenCommand()
	{
		this.usage = "[?] <item> - consumes item at Hell's Kitchen, if available.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KitchenCommand.visit( parameters );
	}

	public static boolean visit( final String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			return false;
		}

		if ( KoLConstants.kitchenItems.isEmpty() )
		{
			HellKitchenRequest.getMenu();
		}

		if ( parameters.equals( "" ) )
		{
			return false;
		}

		String[] splitParameters = AbstractCommand.splitCountAndName( parameters );
		String countString = splitParameters[ 0 ];
		String nameString = splitParameters[ 1 ];

		if ( nameString.startsWith( "\u00B6" ) )
		{
			String name = ItemDatabase.getItemName( StringUtilities.parseInt( nameString.substring( 1 ) ) );
			if ( name != null )
			{
				nameString = name;
			}
		}

		for ( int i = 0; i < KoLConstants.kitchenItems.size(); ++i )
		{
			String name = KoLConstants.kitchenItems.get( i );

			if ( !StringUtilities.substringMatches( name.toLowerCase(), nameString, false ) )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 : StringUtilities.parseInt( countString );

			if ( count == 0 )
			{
				if ( name.equals( "Imp Ale" ) )
				{
					int inebriety = ConsumablesDatabase.getInebriety( name );
					if ( inebriety > 0 )
					{
						count = ( KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety() ) / inebriety;
					}
				}
				else
				{
					int fullness = ConsumablesDatabase.getFullness( name );
					if ( fullness > 0 )
					{
						count = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) / fullness;
					}
				}
			}

			for ( int j = 0; j < count; ++j )
			{
				RequestThread.postRequest( new HellKitchenRequest( name ) );
			}

			return true;
		}

		return false;
	}
}
