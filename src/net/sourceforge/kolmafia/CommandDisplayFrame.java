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

public class CommandDisplayFrame extends KoLFrame
{
	private LimitedSizeChatBuffer buffer;

	public CommandDisplayFrame( KoLmafia client, LimitedSizeChatBuffer buffer )
	{
		super( "KoLmafia: Graphical CLI", client );
		this.buffer = buffer;

		getContentPane().setLayout( new BorderLayout( 0, 0 ) );
		getContentPane().add( new CommandDisplayPanel() );
		setSize( new Dimension( 400, 300 ) );
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JTextField entryField;
		private JScrollPane scrollPane;
		private JEditorPane outputDisplay;

		public CommandDisplayPanel()
		{
			outputDisplay = new JEditorPane();
			outputDisplay.setEditable( false );

			buffer.setChatDisplay( outputDisplay );

			scrollPane = new JScrollPane( outputDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			scrollPane.setVerticalScrollBar( new CommandScrollBar() );
			buffer.setScrollPane( scrollPane );

			JPanel entryPanel = new JPanel();
			entryField = new JTextField();
			entryField.addKeyListener( new CommandEntryListener() );

			JButton entryButton = new JButton( "exec" );
			entryButton.addActionListener( new CommandEntryListener() );
			entryPanel.setLayout( new BoxLayout( entryPanel, BoxLayout.X_AXIS ) );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			setLayout( new BorderLayout( 1, 1 ) );
			add( scrollPane, BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
		}

		private class CommandScrollBar extends JScrollBar
		{
			private boolean autoscroll;

			public CommandScrollBar()
			{
				super( VERTICAL );
				this.autoscroll = true;
			}

			public void setValue( int value )
			{
				if ( getValueIsAdjusting() )
					autoscroll = getMaximum() - getVisibleAmount() - getValue() < 10;

				if ( autoscroll || getValueIsAdjusting() )
					super.setValue( value );
			}

			protected void fireAdjustmentValueChanged( int id, int type, int value )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.fireAdjustmentValueChanged( id, type, value );
			}

			public void setValues( int newValue, int newExtent, int newMin, int newMax )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.setValues( newValue, newExtent, newMin, newMax );
				else
					super.setValues( getValue(), newExtent, newMin, newMax );
			}
		}

		public JScrollPane getScrollPane()
		{	return scrollPane;
		}

		public JEditorPane getOutputDisplay()
		{	return outputDisplay;
		}

		public boolean hasFocus()
		{	return entryField.hasFocus() || outputDisplay.hasFocus();
		}

		public void requestFocus()
		{	entryField.requestFocus();
		}

		/**
		 * An action listener responsible for sending the text
		 * contained within the entry panel to the KoL chat
		 * server for processing.  This listener spawns a new
		 * request to the server which then handles everything
		 * that's needed.
		 */

		private class CommandEntryListener extends KeyAdapter implements ActionListener
		{
			private String previousCommand;

			public void actionPerformed( ActionEvent e )
			{	submitCommand();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitCommand();
			}

			private void submitCommand()
			{
				(new CommandEntryThread( entryField.getText().trim() )).start();
				entryField.setText( "" );
			}

			private class CommandEntryThread extends RequestThread
			{
				private String command;

				public CommandEntryThread( String command )
				{	this.command = command;
				}

				public void run()
				{
					try
					{
						buffer.append( "<font color=olive>&nbsp;&gt;&nbsp;" + command + "</font><br>" );

						if ( command.toLowerCase().equals( "login" ) )
							buffer.append( "<font color=red>This command is not available in the GCLI</font><br>" );
						else
						{
							KoLmafiaCLI instance = new KoLmafiaCLI( client, (String) null );
							instance.previousCommand = previousCommand;
							instance.executeLine( command );

							if ( command.startsWith( "repeat" ) )
								previousCommand = command;
						}

						buffer.append( "<br>" );
					}
					catch ( Exception e )
					{
					}
				}
			}
		}
	}
}
