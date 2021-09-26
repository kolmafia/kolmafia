package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

public class UpdateDataCommand
	extends AbstractCommand
{
	public UpdateDataCommand()
	{
		this.usage =
			" clear | save | prices <URL or filename> - revert to built-in data or save override files for new objects.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equalsIgnoreCase( "clear" ) )
		{
			KoLmafia.deleteAdventureOverride();
			return;
		}

		if ( parameters.equalsIgnoreCase( "save" ) )
		{
			KoLmafia.saveDataOverride();
			return;
		}

		if ( parameters.startsWith( "prices" ) )
		{
			MallPriceDatabase.updatePrices( parameters.substring( 6 ).trim() );
			return;
		}

		KoLmafia.updateDisplay(
			MafiaState.ABORT,
			"\"update\" doesn't do what you think it does.	Please visit kolmafia.us and download a daily build if you'd like to keep KoLmafia up-to-date between major releases." );
	}
}
