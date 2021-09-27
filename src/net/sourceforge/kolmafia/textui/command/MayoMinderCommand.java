package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class MayoMinderCommand
	extends AbstractCommand
{
	public MayoMinderCommand()
	{
		this.usage = " mayodiol | drunk | mayoflex | adv | mayonex | bmc | mayostat | food | mayozapine | stat ";
	}

	public static final int MAYODIOL = 2;
	public static final int MAYOFLEX = 5;
	public static final int MAYONEX = 1;
	public static final int MAYOSTAT = 3;
	public static final int MAYOZAPINE = 4;

	public static final Object [][] MAYO = new Object[][]
	{
		{ "mayodiol", "drunk", IntegerPool.get( MAYODIOL ) },
		{ "mayoflex", "adv", IntegerPool.get( MAYOFLEX ) },
		{ "mayonex", "bmc", IntegerPool.get( MAYONEX ) },
		{ "mayostat", "food", IntegerPool.get( MAYOSTAT ) },
		{ "mayozapine", "stat", IntegerPool.get( MAYOZAPINE ) },
	};

	public static final int findMayo( final String name )
	{
		for ( int i = 0; i < MAYO.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) MAYO[i][0] ) || name.equalsIgnoreCase( (String) MAYO[i][1] ) )
			{
				Integer index = (Integer) MAYO[i][2];
				return index.intValue();
			}
		}

		return 0;
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "Usage: mayominder" + this.usage );
			RequestLogger.printLine( "mayodiol or drunk: 1 full from next food converted to drunk" );
			RequestLogger.printLine( "mayoflex or adv: 1 adv from next food" );
			RequestLogger.printLine( "mayonex or bmc: adventures from next food converted to BMC" );
			RequestLogger.printLine( "mayostat or food: return some of next food" );
			RequestLogger.printLine( "mayozapine or stat: double stat gain of next food" );
			return;
		}

		int option = MayoMinderCommand.findMayo( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what '" + parameters + "' mayo is." );
			return;
		}

		AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
		if ( workshed == null || workshed.getItemId() != ItemPool.MAYO_CLINIC )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Mayo clinic not installed" );
			return;
		}

		if ( !InventoryManager.hasItem( ItemPool.MAYO_MINDER ) )
		{
			if ( !InventoryManager.checkpointedRetrieveItem( ItemPool.MAYO_MINDER ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot obtain a Mayo Minder" );
				return;
			}			
		}

		GenericRequest request = new GenericRequest( "inv_use.php?which=3&whichitem=" + ItemPool.MAYO_MINDER ) ;
		RequestThread.postRequest( request );
		request.constructURLString( "choice.php?whichchoice=1076&option=" + option );
		RequestThread.postRequest( request );

		RequestLogger.printLine( "Mayo Minder&trade; now set to " + Preferences.getString( "mayoMinderSetting" ) );
	}
}
