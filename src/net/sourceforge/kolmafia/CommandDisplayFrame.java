/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

// layout
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JToolBar;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import java.util.ArrayList;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CommandDisplayFrame extends KoLFrame
{
	private JTextField entryField;
	private static int lastCommandIndex = 0;

	private static final ArrayList commandQueue = new ArrayList();
	private static ArrayList commandHistory = new ArrayList();

	public CommandDisplayFrame()
	{
		super( "Graphical CLI" );
		framePanel.add( new CommandDisplayPanel(), BorderLayout.CENTER );

		if ( getProperty( "scriptList" ).equals( "" ) )
			return;

		if ( StaticEntity.getProperty( "scriptButtonPosition" ).equals( "0" ) )
		{
			JToolBar toolbarPanel = getToolbar();
			String [] scriptList = getProperty( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
				toolbarPanel.add( new LoadScriptButton( i + 1, scriptList[i] ) );
		}
	}

	public boolean useSidePane()
	{	return true;
	}

	public void requestFocus()
	{
		super.requestFocus();
		entryField.requestFocus();
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JButton entryButton;

		public CommandDisplayPanel()
		{
			JEditorPane outputDisplay = new JEditorPane();
			JScrollPane scrollPane = KoLmafia.commandBuffer.setChatDisplay( outputDisplay );
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

		/**
		 * An action listener responsible for sending the text
		 * contained within the entry panel to the KoL chat
		 * server for processing.  This listener spawns a new
		 * request to the server which then handles everything
		 * that's needed.
		 */

		private class CommandEntryListener extends KeyAdapter implements ActionListener, Runnable
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

				if ( command.length() == 0 )
					return;

				if ( command.equalsIgnoreCase( "clear" ) || command.equalsIgnoreCase( "cls" ) )
				{
					KoLmafia.commandBuffer.clearBuffer();
					return;
				}

				if ( command.equals( "abort" ) )
				{
					commandQueue.clear();
					KoLmafia.declareWorldPeace();
					return;
				}

				commandQueue.add( command );
				commandHistory.add( command );
				lastCommandIndex = commandHistory.size();

				if ( commandQueue.size() > 1 )
				{
					KoLmafia.commandBuffer.append( "<br><font color=olive>&nbsp;&gt;&nbsp; <b>Queued command</b>: " + command + "</font><br><br>" );
					return;
				}

				(new RequestThread( this )).start();
			}

			public void run()
			{
				while ( !commandQueue.isEmpty() )
				{
					KoLmafia.forceContinue();
					executeQueuedCommand();
				}
			}

			private void executeQueuedCommand()
			{
				try
				{
					String command = (String) commandQueue.get(0);
					KoLmafia.commandBuffer.append( "<br><font color=olive>&nbsp;&gt;&nbsp;" + command + "</font><br><br>" );
					DEFAULT_SHELL.executeLine( command );
				}
				catch ( Exception e )
				{
					// This is usually a result of a user error.
					// Therefore, fall through and remove the
					// command to clear out the queue.
				}

				try
				{
					if ( !commandQueue.isEmpty() )
						commandQueue.remove(0);
				}
				catch ( Exception e )
				{
					// This is only due to a race condition
					// and should not happen.
				}
			}
		}
	}
}
