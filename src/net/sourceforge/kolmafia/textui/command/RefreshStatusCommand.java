package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

public class RefreshStatusCommand
	extends AbstractCommand
{
	public RefreshStatusCommand()
	{
		this.usage = " all | [status | effects] | [gear | equip | outfit] | inv | camp | storage | [familiar | terarrium] | stickers | quests | shop - resynchronize with KoL.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( parameters.equals( "all" ) )
		{
			KoLmafia.refreshSession();
			return;
		}
		else if ( parameters.equals( "status" ) || parameters.equals( "effects" ) )
		{
			ApiRequest.updateStatus();
		}
		else if ( parameters.equals( "gear" ) || parameters.startsWith( "equip" ) || parameters.equals( "outfit" ) )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			parameters = "equipment";
		}
		else if ( parameters.startsWith( "stick" ) )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.BEDAZZLEMENTS ) );
			parameters = "equipment";
		}
		else if ( parameters.startsWith( "inv" ) )
		{
			InventoryManager.refresh();
			return;
		}
		else if ( parameters.startsWith( "camp" ) )
		{
			if ( !Limitmode.limitCampground() && !KoLCharacter.isEd() && !KoLCharacter.inNuclearAutumn() )
			{
				RequestThread.postRequest( new CampgroundRequest() );
			}
			return;
		}
		else if ( parameters.equals( "storage" ) )
		{
			StorageRequest.refresh();
			return;
		}
		else if ( parameters.startsWith( "familiar" ) || parameters.equals( "terrarium" ) )
		{
			parameters = "familiars";
			GenericRequest request = KoLCharacter.inQuantum() ? new QuantumTerrariumRequest() : new FamiliarRequest();
			RequestThread.postRequest( request );
		}
		else if ( parameters.equals( "quests" ) )
		{
			RequestThread.postRequest( new QuestLogRequest() );
			return;
		}
		else if ( parameters.equals( "shop" ) )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
			return;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " cannot be refreshed." );
			return;
		}

        ShowDataCommand.show( parameters );
	}
}
