package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class OutfitCommand
	extends AbstractCommand
{
	public OutfitCommand()
	{
		this.usage = " [list <filter>] | save <name> | checkpoint | <name> - list, save, restore, or change outfits.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.startsWith( "list" ) )
		{
			ShowDataCommand.show( "outfits " + parameters.substring( 4 ).trim() );
			return;
		}
		else if ( parameters.startsWith( "save " ) )
		{
			RequestThread.postRequest( new EquipmentRequest( parameters.substring( 4 ).trim() ) );
			return;
		}
		else if ( parameters.length() == 0 )
		{
			ShowDataCommand.show( "outfits" );
			return;
		}
		else if ( parameters.equalsIgnoreCase( "checkpoint" ) )
		{
			SpecialOutfit.restoreExplicitCheckpoint();
			return;
		}

		SpecialOutfit intendedOutfit = EquipmentManager.getMatchingOutfit( parameters );

		if ( intendedOutfit == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No outfit found matching: " + parameters );
			return;
		}

		if ( intendedOutfit != SpecialOutfit.PREVIOUS_OUTFIT && !EquipmentManager.retrieveOutfit( intendedOutfit ) )
		{
			return;
		}

		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}
}
