package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.request.GenericRequest;

public class SkeletonCommand
	extends AbstractCommand
{
	public SkeletonCommand()
	{
		this.usage = " warrior | cleric | wizard | rogue | buddy";
	}

	public static final int WARRIOR = 1;
	public static final int CLERIC = 2;
	public static final int WIZARD = 3;
	public static final int ROGUE = 4;
	public static final int BUDDY = 5;


	public static final Object [][] SKELETONS = new Object[][]
	{
		{ "warrior", IntegerPool.get( WARRIOR ) },
		{ "cleric", IntegerPool.get( CLERIC ) },
		{ "wizard", IntegerPool.get( WIZARD ) },
		{ "rogue", IntegerPool.get( ROGUE ) },
		{ "buddy", IntegerPool.get( BUDDY ) },
	};

	public static final int findSkeleton( final String name )
	{
		for ( int i = 0; i < SKELETONS.length; ++i )
		{
			String skeleton = (String) SKELETONS[i][0];
			if ( name.equals( skeleton ) )
			{
				Integer index = (Integer) SKELETONS[i][1];
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
			RequestLogger.printLine( "Usage: skeleton" + this.usage );
			RequestLogger.printLine( "warrior: damage, delevel" );
			RequestLogger.printLine( "cleric: hot damage, hp" );
			RequestLogger.printLine( "wizard: cold damage, mp" );
			RequestLogger.printLine( "rogue: damage, meat" );
			RequestLogger.printLine( "buddy: delevel, exp" );
			return;
		}

		int option;
		option = SkeletonCommand.findSkeleton( parameters );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I don't understand what a '" + parameters + "' skeleton is." );
			return;
		}

		if ( !InventoryManager.retrieveItem( ItemPool.SKELETON ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have no skeletons and can't get any with your current settings." );
			return;
		}

		GenericRequest request = new GenericRequest( "inv_use.php?which=3&whichitem=" + ItemPool.SKELETON ) ;
		RequestThread.postRequest( request );
		request.constructURLString( "choice.php?whichchoice=603&option=" + option );
		RequestThread.postRequest( request );
	}
}
