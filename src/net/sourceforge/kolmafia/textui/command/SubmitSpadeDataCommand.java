package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.SendMailRequest;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SubmitSpadeDataCommand
	extends AbstractCommand
{
	public SubmitSpadeDataCommand()
	{
		this.usage = " [prices <URL>] - submit automatically gathered data.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.startsWith( "prices" ) )
		{
			MallPriceDatabase.submitPrices( parameters.substring( 6 ).trim() );
			return;
		}

		String[] data = Preferences.getString( "spadingData" ).split( "\\|" );
		if ( data.length < 3 )
		{
			KoLmafia.updateDisplay( "No spading data has been collected yet. " + "Please try again later." );
			return;
		}
		for ( int i = 0; i < data.length - 2; i += 3 )
		{
			String contents = data[ i ];
			String recipient = data[ i + 1 ];
			String explanation = data[ i + 2 ];
			if ( InputFieldUtilities.confirm( "Would you like to send the data \"" + contents + "\" to " + recipient + "?\nThis information will be used " + explanation ) )
			{
				RequestThread.postRequest( new SendMailRequest( recipient, contents ) );
			}
		}
		Preferences.setString( "spadingData", "" );
	}
}
