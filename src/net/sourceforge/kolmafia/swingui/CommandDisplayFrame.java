/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui;

import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JTabbedPane;

import net.sourceforge.kolmafia.KoLConstants;
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
	private static final LinkedList commandQueue = new LinkedList();
	private static final PauseObject pauser = new PauseObject();
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

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public boolean useSidePane()
	{
		return true;
	}

	public static final boolean hasQueuedCommands()
	{
		return !CommandDisplayFrame.commandQueue.isEmpty();
	}

	public static final void executeCommand( final String command )
	{
		if ( command.length() == 0 )
		{
			return;
		}

		if ( command.startsWith( "jstack" ) || command.startsWith( "graygui" ) || command.startsWith( "greygui" ) ||
			command.equalsIgnoreCase( "clear" ) || command.equalsIgnoreCase( "cls" ) || command.equalsIgnoreCase( "reset" ) ||
			command.startsWith( "abort" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( command );
			return;
		}

		if ( !CommandDisplayFrame.commandQueue.isEmpty() )
		{
			RequestLogger.printLine();

			Iterator commandIterator = CommandDisplayFrame.commandQueue.iterator();

			for ( int i = 0; commandIterator.hasNext(); ++i )
			{
				String cmd = StringUtilities.globalStringReplace( (String) commandIterator.next(), "<", "&lt;" );

				if ( i == 0 )
				{
					RequestLogger.printLine( " > <b>CURRENT</b>: " + cmd );
				}
				else
				{
					RequestLogger.printLine( " > <b>QUEUED " + i + "</b>: " + cmd );
				}
			}

			RequestLogger.printLine( " > <b>QUEUED " + CommandDisplayFrame.commandQueue.size() + "</b>: " + StringUtilities.globalStringReplace( command, "<", "&lt;" ) );
			RequestLogger.printLine();
		}

		CommandDisplayFrame.commandQueue.add( command );
	}

	private static final class CommandQueueHandler
		extends Thread
	{
		private String command = "";

		public CommandQueueHandler()
		{
			super( "CommandQueueHandler" );
		}

		public void run()
		{
			while ( true )
			{
				while ( CommandDisplayFrame.commandQueue.isEmpty() )
				{
					CommandDisplayFrame.pauser.pause( 200 );
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
			while ( !CommandDisplayFrame.commandQueue.isEmpty() )
			{
				this.command = (String) CommandDisplayFrame.commandQueue.get( 0 );

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
				else
				{
					CommandDisplayFrame.commandQueue.removeFirst();
				}
			}
		}
	}
}
