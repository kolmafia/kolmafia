package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RetroCapeCommand
	extends AbstractCommand
{
	public static final String[] SUPERHEROS = { "vampire", "heck", "robot" };
	public static final String[] WASHING_INSTRUCTIONS = { "hold", "thrill", "kiss", "kill" };

	public RetroCapeCommand()
	{
		this.usage = " [muscle | mysticality | moxie | vampire | heck | robot] [hold | thrill | kiss | kill]";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a knock-off retro superhero cape" );
			return;
		}

		parameters = parameters.trim();

		String[] params = parameters.split( " " );

		Integer superhero = null;
		Integer washingInstruction = null;

		for ( String param : params )
		{
			if ( param.contains( "mus" ) || param.equals( "vampire" ) )
			{
				superhero = 1;
			}
			else if ( param.contains( "mys" ) || param.equals( "heck" ) )
			{
				superhero = 2;
			}
			else if ( param.contains( "mox" ) || param.equals( "robot" ) )
			{
				superhero = 3;
			}
			else if ( param.equals( "hold" ) )
			{
				washingInstruction = 2;
			}
			else if ( param.equals( "thrill" ) )
			{
				washingInstruction = 3;
			}
			else if ( param.equals( "kiss" ) )
			{
				washingInstruction = 4;
			}
			else if ( param.equals( "kill" ) )
			{
				washingInstruction = 5;
			}
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.CONTAINER ).getItemId() != ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE )
		{
			AdventureResult retroCape = ItemPool.get( ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE );
			RequestThread.postRequest( new EquipmentRequest( retroCape, EquipmentManager.CONTAINER ) );
		}

		KoLmafia.updateDisplay( "Reconfiguring retro cape" );

		GenericRequest request = new GenericRequest( "inventory.php?action=hmtmkmkm", false );
		RequestThread.postRequest( request );

		if ( washingInstruction != null )
		{
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1437" );
			request.addFormField( "option", Integer.toString( washingInstruction ) );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}

		if ( superhero != null )
		{
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1437" );
			request.addFormField( "option", "1" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );	

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1438" );
			request.addFormField( "option", Integer.toString( superhero ) );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1438" );
			request.addFormField( "option", "4" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}

		request = new GenericRequest( "choice.php" );
		request.addFormField( "whichchoice", "1437" );
		request.addFormField( "option", "6" );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		RequestThread.postRequest( request );
	}
}
