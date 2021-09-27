package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

public class GrimCommand
	extends AbstractCommand
{
	public GrimCommand()
	{
		this.usage = " init|hpmp|damage - get a Grim Brother buff";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( KoLCharacter.findFamiliar( FamiliarPool.GRIM_BROTHER ) == null )
		{
			KoLmafia.updateDisplay( "You don't have a Grim Brother" );
			return;
		}
		if ( Preferences.getBoolean( "_grimBuff" ) )
		{
			KoLmafia.updateDisplay( "You already received a Grim Brother effect today" );
			return;
		}

		int option = 0;
		if ( parameters.startsWith( "init" ) || parameters.startsWith( "soles" ) )
		{
			option = 1;
		}
		else if ( parameters.startsWith( "hpmp" ) || parameters.startsWith( "angry" ) )
		{
			option = 2;
		}
		else if ( parameters.startsWith( "damage" ) || parameters.startsWith( "grumpy" ) )
		{
			option = 3;
		}

		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Pick a valid Grim Brother buff" );
			return;
		}

		RequestThread.postRequest( new GenericRequest( "familiar.php?action=chatgrim" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=835&option=" + option ) );
	}
}
