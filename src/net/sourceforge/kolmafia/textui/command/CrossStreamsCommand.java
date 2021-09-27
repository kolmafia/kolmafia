package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrossStreamsCommand
	extends AbstractCommand
{
	private static final AdventureResult PROTON_ACCELERATOR = ItemPool.get( ItemPool.PROTON_ACCELERATOR, 1 );

	public CrossStreamsCommand()
	{
		this.usage = " [ <target> ] - Cross streams with the target, default target in preference \"streamCrossDefaultTarget\"";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		Boolean equipped = KoLCharacter.hasEquipped( CrossStreamsCommand.PROTON_ACCELERATOR, EquipmentManager.CONTAINER );
		// Check if Protonic Accelerator Pack equipped or owned
		if ( !InventoryManager.hasItem( CrossStreamsCommand.PROTON_ACCELERATOR ) && !equipped )
		{
			KoLmafia.updateDisplay( "Do not have a Proton Accelerator Pack" );
			return;
		}

		// Check if previously used
		if ( Preferences.getBoolean( "_streamsCrossed" ) )
		{
			KoLmafia.updateDisplay( "Have already crossed streams today" );
			return;
		}

		// Validate target
		String targetName = null;
		String targetId = null;

		parameters = parameters.trim();

		// If no target given, use default
		if ( parameters.equals( "" ) )
		{
			parameters = Preferences.getString( "streamCrossDefaultTarget" );
		}

		if ( StringUtilities.isNumeric( parameters ) )
		{
			// Target ID given, so get Target Name
			targetId = parameters;
			targetName = ContactManager.getPlayerName( targetId, true );
		}
		else
		{
			// Target Name given, so get Target Id
			targetName = parameters;
			targetId = ContactManager.getPlayerId( targetName, true );
		}

		// If names weren't found, they don't exist
		// Contact manager returns Id if looking up Name, and visa versa, so they match
		if ( targetId == targetName )
		{
			KoLmafia.updateDisplay( "Cannot find target " + parameters );
			return;
		}

		// Equip if not equipped
		try ( Checkpoint checkpoint = new Checkpoint( equipped ) )
		{
			if ( !equipped )
			{
				RequestThread.postRequest( new EquipmentRequest( CrossStreamsCommand.PROTON_ACCELERATOR, EquipmentManager.CONTAINER ) );
			}

			// Cross Streams
			KoLmafia.updateDisplay( "Crossing Streams with " + targetName );
			if ( KoLmafia.permitsContinue() )
			{
				RequestThread.postRequest( new GenericRequest( "showplayer.php?action=crossthestreams&who=" + targetId ) );
			}
		}
	}
}
