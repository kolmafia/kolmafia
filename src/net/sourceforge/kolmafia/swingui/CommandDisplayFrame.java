package net.sourceforge.kolmafia.swingui;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JTabbedPane;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.panel.CommandDisplayPanel;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CommandDisplayFrame
	extends GenericFrame
{
	private static final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<String>();
	private static final CommandQueueHandler handler = new CommandQueueHandler();

	static
	{
		CommandDisplayFrame.handler.start();
	}

	public CommandDisplayFrame()
	{
		super( "Graphical CLI" );

		this.setCenterComponent( new CommandDisplayPanel() );
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	@Override
	public boolean shouldAddStatusBar()
	{
		return false;
	}

	@Override
	public boolean useSidePane()
	{
		return true;
	}

	public static final boolean hasQueuedCommands()
	{
		return !CommandDisplayFrame.commandQueue.isEmpty() || handler.command != null;
	}

	public static final void executeCommand( final String command )
	{
		if ( command.length() == 0 )
		{
			return;
		}

		if ( command.equals( "abort" ) || command.equals( "--" ) )
		{
			RequestThread.declareWorldPeace();
			return;
		}

		if ( command.startsWith( "jstack" ) || command.startsWith( "graygui" ) || command.startsWith( "greygui" ) ||
			command.equalsIgnoreCase( "clear" ) || command.equalsIgnoreCase( "cls" ) || command.equalsIgnoreCase( "reset" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( command );
			return;
		}

		if ( CommandDisplayFrame.hasQueuedCommands() || KoLmafia.isAdventuring() )
		{
			RequestLogger.printLine();
			
			if ( !KoLmafia.isAdventuring() )
			{
				RequestLogger.printLine( " > <b>CURRENT</b>: " + handler.command );
			}

			Iterator<String> commandIterator = CommandDisplayFrame.commandQueue.iterator();

			int i;
			for ( i = 1; commandIterator.hasNext(); ++i )
			{
				String cmd = StringUtilities.globalStringReplace( commandIterator.next(), "<", "&lt;" );

				RequestLogger.printLine( " > <b>QUEUED " + i + "</b>: " + cmd );
			}

			RequestLogger.printLine( " > <b>QUEUED " + i + "</b>: " +
				StringUtilities.globalStringReplace( command, "<", "&lt;" ) );
			RequestLogger.printLine();
		}

		CommandDisplayFrame.commandQueue.add( command );
	}

	private static final class CommandQueueHandler
		extends Thread
	{
		private String command = null;
		private final PauseObject pauser = new PauseObject();

		public CommandQueueHandler()
		{
			super( "CommandQueueHandler" );
		}

		@Override
		public void run()
		{
			while ( true )
			{
				try
				{
					this.command = CommandDisplayFrame.commandQueue.take();
				}
				catch ( InterruptedException e )
				{
					StaticEntity.printStackTrace( e );
					continue;
				}

				Integer requestId = RequestThread.openRequestSequence();
				try
				{
					this.handleQueue();
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}
				finally
				{
					RequestThread.closeRequestSequence( requestId );
				}
			}
		}

		public void handleQueue()
		{
			do
			{
				// Don't try running commands whilst running adventures, it causes unexpected results
				while ( !KoLmafia.refusesContinue() && KoLmafia.isAdventuring() )
				{
					this.pauser.pause( 500 );
				}

				RequestLogger.printLine();
				RequestLogger.printLine( " > " + StringUtilities.globalStringReplace( this.command, "<", "&lt;" ) );
				RequestLogger.printLine();

				try
				{
					KoLmafia.forceContinue();
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.command );
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}

				if ( KoLmafia.refusesContinue() )
				{
					CommandDisplayFrame.commandQueue.clear();
				}

				this.command = CommandDisplayFrame.commandQueue.poll();

			}
			while ( this.command != null );
		}
	}
}
