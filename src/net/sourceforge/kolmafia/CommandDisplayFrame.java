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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.ConcurrentWorker;
import net.sourceforge.foxtrot.Job;
import net.sourceforge.foxtrot.Worker;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CommandDisplayFrame extends KoLFrame
{
	private static final CommandQueueHandler handler = new CommandQueueHandler();

	private JTextField entryField;
	private RequestPane outputDisplay;

	private static String lastPlayer = null;
	private static int lastCommandIndex = 0;
	private static boolean shouldQueueRequests = false;

	private static final ArrayList commandQueue = new ArrayList();
	private static final ArrayList commandHistory = new ArrayList();

	public CommandDisplayFrame()
	{
		super( "Graphical CLI" );
		framePanel.add( new CommandDisplayPanel(), BorderLayout.CENTER );
	}

	public boolean useSidePane()
	{	return true;
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( entryField != null )
			entryField.requestFocus();
	}

	public void dispose()
	{
		commandBuffer.setChatDisplay( null );
		super.dispose();
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JButton entryButton;

		public CommandDisplayPanel()
		{
			RequestPane outputDisplay = new RequestPane();
			JScrollPane scrollPane = commandBuffer.setChatDisplay( outputDisplay );
			JComponentUtilities.setComponentSize( scrollPane, 400, 300 );

			JPanel entryPanel = new JPanel( new BorderLayout() );
			entryField = new JTextField();
			entryField.addKeyListener( new CommandEntryListener() );

			entryButton = new JButton( "exec" );
			entryButton.addActionListener( new CommandEntryListener() );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			setLayout( new BorderLayout( 1, 1 ) );
			add( scrollPane, BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
		}

		private class CommandEntryListener extends KeyAdapter implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	submitCommand();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( lastCommandIndex <= 0 )
						return;

					entryField.setText( (String) commandHistory.get( --lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( lastCommandIndex + 1 >= commandHistory.size() )
						return;

					entryField.setText( (String) commandHistory.get( ++lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitCommand();
			}

			private void submitCommand()
			{
				String command = entryField.getText().trim();
				entryField.setText( "" );
				executeCommand( command );
			}
		}
	}

	public static boolean hasQueuedCommands()
	{	return !commandQueue.isEmpty();
	}

	public static void executeCommand( String command )
	{
		if ( command.length() == 0 )
			return;

		if ( command.equalsIgnoreCase( "clear" ) || command.equalsIgnoreCase( "cls" ) )
		{
			KoLmafia.commandBuffer.clearBuffer();
			return;
		}

		if ( command.startsWith( "abort" ) )
		{
			RequestThread.declareWorldPeace();
			return;
		}

		synchronized ( commandQueue )
		{
			commandQueue.add( command );
			commandHistory.add( command );
			lastCommandIndex = commandHistory.size();

			if ( handler.isRunning() )
			{
				RequestLogger.printLine();
				RequestLogger.printLine( " > QUEUED: " + command );
				RequestLogger.printLine();

				return;
			}
		}

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
				handler.run();
			else if ( StaticEntity.getBooleanProperty( "allowRequestQueueing" ) )
				Worker.post( handler );
			else
				ConcurrentWorker.post( handler );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		return;
	}

	private static class CommandQueueHandler extends Job
	{
		private boolean isRunning = false;

		public boolean isRunning()
		{	return isRunning;
		}

		public void run()
		{
			if ( isRunning )
				return;

			isRunning = true;
			RequestThread.openRequestSequence();

			do
			{
				String command;

				synchronized ( commandQueue )
				{
					command = (String) commandQueue.get(0);
				}

				RequestLogger.printLine();
				RequestLogger.printLine( " > " + StaticEntity.globalStringReplace( command, "<", "&lt;" ) );
				RequestLogger.printLine();

				try
				{
					KoLmafia.forceContinue();
					DEFAULT_SHELL.executeLine( command );
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}

				synchronized ( commandQueue )
				{
					commandQueue.remove(0);
				}
			}
			while ( !KoLmafia.refusesContinue() && !commandQueue.isEmpty() );

			// Always clear the list of queued commands when
			// you've finished some sequence of commands.

			commandQueue.clear();
			RequestThread.closeRequestSequence();
			isRunning = false;
		}
	}
}
