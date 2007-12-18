/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.WindowConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CommandDisplayFrame
	extends KoLFrame
{
	private static final CommandQueueHandler handler = new CommandQueueHandler();
	static
	{
		CommandDisplayFrame.handler.start();
	}

	private static final ArrayList commandQueue = new ArrayList();
	private static final ArrayList commandHistory = new ArrayList();
	private static int lastCommandIndex = 0;

	private AutoHighlightField entryField;

	public CommandDisplayFrame()
	{
		super( "Graphical CLI" );
		this.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		this.framePanel.add( new CommandDisplayPanel(), BorderLayout.CENTER );

		// Workaround for Swing bug in UnfocusedTabbedPane
		// http://www.javalobby.org/java/forums/t43667.html

		this.getContentPane().setFocusCycleRoot( true );
		this.getContentPane().setFocusTraversalPolicy( new EntryFieldFocuser() );
	}

	private class EntryFieldFocuser
		extends LayoutFocusTraversalPolicy
	{
		public Component getDefaultComponent( final Container c )
		{
			return CommandDisplayFrame.this.entryField;
		}
	}

	public UnfocusedTabbedPane getTabbedPane()
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

	public void requestFocus()
	{
		this.entryField.requestFocus();
	}

	public void dispose()
	{
		KoLConstants.commandBuffer.setChatDisplay( null );
		super.dispose();
	}

	private class CommandDisplayPanel
		extends JPanel
	{
		private final JButton entryButton;

		public CommandDisplayPanel()
		{
			RequestPane outputDisplay = new RequestPane();
			JScrollPane scrollPane = KoLConstants.commandBuffer.setChatDisplay( outputDisplay );
			JComponentUtilities.setComponentSize( scrollPane, 400, 300 );

			JPanel entryPanel = new JPanel( new BorderLayout() );
			CommandDisplayFrame.this.entryField = new AutoHighlightField();
			CommandDisplayFrame.this.entryField.addKeyListener( new CommandEntryListener() );

			this.entryButton = new JButton( "exec" );
			this.entryButton.addActionListener( new CommandEntryListener() );

			entryPanel.add( CommandDisplayFrame.this.entryField, BorderLayout.CENTER );
			entryPanel.add( this.entryButton, BorderLayout.EAST );

			this.setLayout( new BorderLayout( 1, 1 ) );
			this.add( scrollPane, BorderLayout.CENTER );
			this.add( entryPanel, BorderLayout.SOUTH );
		}

		private class CommandEntryListener
			extends KeyAdapter
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				this.submitCommand();
			}

			public void keyReleased( final KeyEvent e )
			{
				if ( e.isConsumed() )
				{
					return;
				}

				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( CommandDisplayFrame.lastCommandIndex <= 0 )
					{
						return;
					}

					CommandDisplayFrame.this.entryField.setText( (String) CommandDisplayFrame.commandHistory.get( --CommandDisplayFrame.lastCommandIndex ) );
					e.consume();
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( CommandDisplayFrame.lastCommandIndex + 1 >= CommandDisplayFrame.commandHistory.size() )
					{
						return;
					}

					CommandDisplayFrame.this.entryField.setText( (String) CommandDisplayFrame.commandHistory.get( ++CommandDisplayFrame.lastCommandIndex ) );
					e.consume();
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				{
					this.submitCommand();
					e.consume();
				}
			}

			private void submitCommand()
			{
				String command = CommandDisplayFrame.this.entryField.getText().trim();
				CommandDisplayFrame.this.entryField.setText( "" );

				CommandDisplayFrame.commandHistory.add( command );

				if ( CommandDisplayFrame.commandHistory.size() > 10 )
				{
					CommandDisplayFrame.commandHistory.remove( 0 );
				}

				CommandDisplayFrame.lastCommandIndex = CommandDisplayFrame.commandHistory.size();
				CommandDisplayFrame.executeCommand( command );
			}
		}
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

		if ( command.equalsIgnoreCase( "clear" ) || command.equalsIgnoreCase( "cls" ) )
		{
			KoLConstants.commandBuffer.clearBuffer();
			return;
		}

		if ( command.startsWith( "abort" ) )
		{
			RequestThread.declareWorldPeace();
			return;
		}

		if ( !CommandDisplayFrame.commandQueue.isEmpty() )
		{
			RequestLogger.printLine();

			if ( !CommandDisplayFrame.handler.command.equals( "" ) )
			{
				RequestLogger.printLine( " > <b>CURRENT</b>: " + StaticEntity.globalStringReplace(
					CommandDisplayFrame.handler.command, "<", "&lt;" ) );
			}

			RequestLogger.printLine( " > <b>QUEUED</b>: " + StaticEntity.globalStringReplace( command, "<", "&lt;" ) );
			RequestLogger.printLine();
		}

		CommandDisplayFrame.commandQueue.add( command );
	}

	private static final class CommandQueueHandler
		extends Thread
	{
		private String command = "";

		public void run()
		{
			while ( true )
			{
				while ( CommandDisplayFrame.commandQueue.isEmpty() )
				{
					KoLRequest.delay( 500 );
				}

				this.handleQueue();
			}
		}

		public void handleQueue()
		{
			RequestThread.openRequestSequence();

			while ( !CommandDisplayFrame.commandQueue.isEmpty() )
			{
				this.command = (String) CommandDisplayFrame.commandQueue.get( 0 );

				RequestLogger.printLine();
				RequestLogger.printLine( " > " + StaticEntity.globalStringReplace( this.command, "<", "&lt;" ) );
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
					CommandDisplayFrame.commandQueue.remove( 0 );
				}
			}

			RequestThread.closeRequestSequence();
		}
	}
}
