package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

public class WorldPeaceListener
	extends ThreadedListener
{
	@Override
	protected void execute()
	{
		if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) )
		{
			KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

			Component owner = manager.getFocusOwner();

			if ( owner == null )
			{
				owner = manager.getFocusedWindow();
			}

			if ( owner != null )
			{
				RequestLogger.printLine( owner.getClass().getName() + " has focus" );

				Container parent = owner.getParent();

				while ( parent != null )
				{
					RequestLogger.printLine( " -> " + parent.getClass().getName() );
					parent = parent.getParent();
				}
			}
			else
			{
				RequestLogger.printLine( "No component has focus" );
			}
		}

		RequestThread.declareWorldPeace();
	}
}

