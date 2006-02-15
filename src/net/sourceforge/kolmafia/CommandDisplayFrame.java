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
import java.awt.CardLayout;
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
	private static int lastCommandIndex = 0;
	private static ArrayList recentCommands = new ArrayList();

	public CommandDisplayFrame( KoLmafia client )
	{
		super( client, "Graphical CLI" );
		KoLmafia.commandBuffer = new LimitedSizeChatBuffer( "KoLmafia: Graphical CLI", false );;

		addCompactPane();
		framePanel.add( new CommandDisplayPanel(), BorderLayout.CENTER );

		// If the user wishes to add toolbars, go ahead
		// and add the toolbar.

		if ( GLOBAL_SETTINGS.getProperty( "useToolbars" ).equals( "true" ) )
		{
			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", CouncilFrame.class ) );
			toolbarPanel.add( new MiniBrowserButton() );
			toolbarPanel.add( new InvocationButton( "Clear Scrollback", KoLmafia.commandBuffer, "clearBuffer" ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "IcePenguin Express", "mail.gif", MailboxFrame.class ) );
			toolbarPanel.add( new InvocationButton( "KoLmafia Chat", "chat.gif", KoLMessenger.class, "initialize" ) );
			toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", ClanManageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Player Status", "hp.gif", CharsheetFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", ItemManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment Manager", "equipment.gif", GearChangeFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", StoreManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", HagnkStorageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Run a Buffbot", "buff.gif", BuffBotFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", FamiliarTrainingFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Player vs. Player", "flower.gif", FlowerHunterFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", MushroomFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
		}
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JTextField entryField;
		private JButton entryButton;

		public CommandDisplayPanel()
		{
			JEditorPane outputDisplay = new JEditorPane();
			outputDisplay.setEditable( false );

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
			private String command;

			public void actionPerformed( ActionEvent e )
			{	submitCommand();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( lastCommandIndex <= 0 )
						return;

					entryField.setText( (String) recentCommands.get( --lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( lastCommandIndex + 1 >= recentCommands.size() )
						return;

					entryField.setText( (String) recentCommands.get( ++lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitCommand();
			}

			private void submitCommand()
			{
				this.command = entryField.getText().trim();

				recentCommands.add( command );
				lastCommandIndex = recentCommands.size();

				(new DaemonThread( this )).start();
				entryField.setText( "" );
			}

			public void run()
			{
				KoLmafia.commandBuffer.append( "<font color=olive>&nbsp;&gt;&nbsp;" + command + "</font><br>" );
				DEFAULT_SHELL.executeLine( command );
				DEFAULT_SHELL.printBlankLine();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( CommandDisplayFrame.class )).run();
	}
}
