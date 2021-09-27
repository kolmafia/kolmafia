package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class ToggleCommand
	extends AbstractCommand
{
	public ToggleCommand()
	{
		this.usage = " [effect] - Toggle an effect to another effect";
	}

	// The plan is to have this be a more generic command for toggling effects, in case that becomes relevant.
	// Since there is only a single pair of effects to toggle currently, the parameter isn't actually used.
	@Override
	public void run( final String cmd, String parameters )
	{
		if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.INTENSELY_INTERESTED ) ) ||
		     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.SUPERFICIALLY_INTERESTED ) ) )
		{
			GenericRequest request = new CharSheetRequest();
			request.addFormField( "action", "newyouinterest" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have an effect to toggle." );
		}
	}

}
