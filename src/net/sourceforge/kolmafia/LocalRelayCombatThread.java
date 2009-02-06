/**
 *
 */
package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class LocalRelayCombatThread
	extends Thread
{
	private String desiredAction;
	private final PauseObject pauser = new PauseObject();

	public LocalRelayCombatThread()
	{
		super( "LocalRelayCombatThread" );
		this.setDaemon( true );
		this.start();
	}

	public void wake( final String desiredAction )
	{
		this.desiredAction = desiredAction;

		if ( !FightRequest.isTrackingFights() )
		{
			FightRequest.beginTrackingFights();
		}

		this.pauser.unpause();
	}

	public void run()
	{
		while ( true )
		{
			this.pauser.pause();

			if ( this.desiredAction == null )
			{
				FightRequest.INSTANCE.run();
			}
			else
			{
				FightRequest.INSTANCE.runOnce( this.desiredAction );
			}

			FightRequest.stopTrackingFights();
		}
	}
}