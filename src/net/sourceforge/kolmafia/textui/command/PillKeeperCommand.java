package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.InventoryManager;

public class PillKeeperCommand
	extends AbstractCommand
{
	public PillKeeperCommand()
	{
		this.usage = " [free] explode | extend | noncombat | element | stat | familiar | semirare | random";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.PILL_KEEPER ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need an Eight Days a Week Pill Keeper" );
			return;
		}

		boolean freePillUsed = Preferences.getBoolean( "_freePillKeeperUsed" );
		if ( ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) < 3 && freePillUsed )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		if ( parameters.contains( "free" ) && freePillUsed )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Free pill keeper use already spent" );
			return;
		}

		int choice = 0;
		String pilltext = "";
		if ( parameters.contains( "exp" ) )
		{
			choice = 1;
			pilltext = "Monday - Explodinall";
			if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.EVERYTHING_LOOKS_YELLOW ) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Everything already looks yellow" );
				return;
			}
		}
		else if ( parameters.contains( "ext" ) )
		{
			choice = 2;
			pilltext = "Tuesday - Extendicillin";
		}
		else if ( parameters.contains( "non" ) )
		{
			choice = 3;
			pilltext = "Wednesday - Sneakisol";
		}
		else if ( parameters.contains( "ele" ) )
		{
			choice = 4;
			pilltext = "Thursday - Rainbowolin";
		}
		else if ( parameters.contains( "sta" ) )
		{
			choice = 5;
			pilltext = "Friday - Hulkien";
		}
		else if ( parameters.contains( "fam" ) )
		{
			choice = 6;
			pilltext = "Saturday - Fidoxene";
		}
		else if ( parameters.contains( "sem" ) )
		{
			choice = 7;
			pilltext = "Sunday - Surprise Me";
		}
		else if ( parameters.contains( "ran" ) )
		{
			choice = 8;
			pilltext = "Funday - Telecybin";
		}
		if ( choice == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid choice" );
			return;
		}

		KoLmafia.updateDisplay( "Taking pills for " + pilltext );

		GenericRequest request = new GenericRequest( "main.php?eowkeeper=1", false );
		RequestThread.postRequest( request );

		request = new GenericRequest( "choice.php" );
		request.addFormField( "whichchoice", "1395" );
		request.addFormField( "option", Integer.toString( choice ) );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		RequestThread.postRequest( request );
	}
}
