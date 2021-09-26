package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.LatteRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class LatteCommand
	extends AbstractCommand
{
	public LatteCommand()
	{
		this.usage = " unlocks | unlocked | refill ingredient1 ingredient2 ingredient2 - Shows unlocks, unlocked items, or refills latte";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] params = parameters.trim().split( "\\s+" );
		String command = params[0];

		if ( !InventoryManager.hasItem( ItemPool.LATTE_MUG ) && !KoLCharacter.hasEquipped( ItemPool.LATTE_MUG, EquipmentManager.OFFHAND ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a latte lovers member's mug first." );
			return;
		}
		if ( command.equalsIgnoreCase( "unlocks" ) )
		{
			LatteRequest.listUnlocks( true );
		}
		else if ( command.equalsIgnoreCase( "unlocked" ) )
		{
			LatteRequest.listUnlocks( false );
		}
		else if ( command.equalsIgnoreCase( "refill" ) )
		{
			if ( params.length < 4 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Use command 'latte refill ingredient1 ingredient2 ingredient3'. Use 'latte unlocked' to show available ingredients." );
				return;
			}			
			LatteRequest.refill( params[1].trim().toLowerCase(), params[2].trim().toLowerCase(), params[3].trim().toLowerCase() );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, " Use command latte " + this.usage );
		}
	}
}
