package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class MayosoakCommand
	extends AbstractCommand
{
	public MayosoakCommand()
	{
		this.usage = " - soak in the mayo tank";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
		if ( workshed == null || workshed.getItemId() != ItemPool.MAYO_CLINIC )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Mayo clinic not installed" );
			return;
		}
		if ( Preferences.getBoolean( "_mayoTankSoaked" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Already soaked in Mayo tank today" );
			return;
		}
		GenericRequest request = new GenericRequest( "shop.php?whichshop=mayoclinic&action=bacta" ) ;
		RequestThread.postRequest( request );
	}
}
