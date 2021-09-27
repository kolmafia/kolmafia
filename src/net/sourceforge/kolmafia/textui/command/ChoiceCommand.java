package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChoiceCommand
	extends AbstractCommand
{
	public ChoiceCommand()
	{
		this.usage = " [<number> [FIELD=VALUE]... [always]] - list or choose choice adventure options.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !ChoiceManager.handlingChoice || ChoiceManager.lastResponseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You aren't in a choice adventure." );
			return;
		}
		if ( parameters.equals( "" ) )
		{
			ChoiceUtilities.printChoices( ChoiceManager.lastResponseText );
			return;
		}
		boolean always = false;
		if ( parameters.endsWith(" always") )
		{
		    always = true;
		    parameters = parameters.substring( 0, parameters.length() - 7 ).trim();
		}

		// DECISION [FIELD=VALUE]...
		String[] fields = parameters.split( " +" );
		String decision = fields[ 0 ];

		if ( !StringUtilities.isNumeric( decision ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Decision '" + decision + "' must be a number." );
			return;
		}

		StringBuilder buf = new StringBuilder();
		for ( int i = 1; i < fields.length; ++i )
		{
			String field = fields[ i ];
			if ( field.indexOf( "=" ) == -1 )
			{
				RequestLogger.printLine( "Field '" + field + "' must have a value; ignoring." );
				continue;
			}
			if ( buf.length() > 0 )
			{
				buf.append( "&" );
			}
			buf.append( field );
		}

		String extraFields = buf.toString();

		String error = ChoiceUtilities.validateChoiceFields( decision, extraFields, ChoiceManager.lastResponseText );
		if ( error != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, error );
			return;
		}

		if (always)
		{
			String pref = "choiceAdventure" + ChoiceManager.currentChoice();
			String value = decision;
			if ( !extraFields.equals( "" ) )
			{
				value += "&" + extraFields;
			}
			RequestLogger.printLine( pref + " => " + value );
			Preferences.setString( pref, value );
		}

		ChoiceManager.processChoiceAdventure( decision, extraFields, true );
	}
}
