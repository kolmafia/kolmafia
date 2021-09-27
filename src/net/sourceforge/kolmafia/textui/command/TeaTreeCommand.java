package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.PottedTeaTreeRequest;
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest.PottedTea;

public class TeaTreeCommand
	extends AbstractCommand
{

	public TeaTreeCommand()
	{
		this.usage = " shake | [tea name] - Harvest random or specific tea";
	}

	@Override
	public void run( final String cmd, String parameter )
	{
		PottedTea tea = null;
		parameter = parameter.trim();

		if ( parameter.equals( "" ) )
		{
			KoLmafia.updateDisplay( "Harvest what?" );
			return;
		}

		if ( parameter.startsWith( "random" ) || parameter.startsWith( "shake" ) )
		{
			tea = null;
		}
		else
		{
			List<String> matchingNames = PottedTeaTreeRequest.getMatchingNames( parameter );
			if ( matchingNames.isEmpty() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to harvest " + parameter );
				return;
			}

			if ( matchingNames.size() > 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + parameter + "' is an ambiguous tea name " );
				return;
			}

			String name = matchingNames.get( 0 );

			tea = PottedTeaTreeRequest.canonicalNameToTea( name );
			if ( tea == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to harvest " + parameter );
				return;
			}
		}

		PottedTeaTreeRequest request = tea == null ? new PottedTeaTreeRequest() : new PottedTeaTreeRequest( tea );

		RequestThread.postRequest( request );
	}
}
