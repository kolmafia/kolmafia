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

/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

// containers
import javax.swing.JList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

// spellcast-related imports
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class AdventureFrame extends KoLFrame
{
	public AdventureFrame( KoLmafia client, LockableListModel availableAdventures, LockableListModel resultsTally )
	{
		super( "KoLmafia: " + client.getLoginName(), client );
		setResizable( false );

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		JTabbedPane tabs = new JTabbedPane();

		addAdventuringPanel( tabs, availableAdventures, resultsTally );
		addInventoryPanel( tabs );
		addMallBrowsingPanel( tabs );

		getContentPane().add( tabs, "" );

		updateDisplay( ENABLED_STATE, " " );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		addMenuBar();
	}

	private void addAdventuringPanel( JTabbedPane tabs, LockableListModel availableAdventures, LockableListModel resultsTally )
	{
		JPanel summaryPanel = new AdventureResultsPanel( resultsTally );
		contentPanel = new AdventureSelectPanel( availableAdventures );

		JPanel adventuringPanel = new JPanel();
		adventuringPanel.setLayout( new BorderLayout( 10, 10 ) );
		adventuringPanel.add( summaryPanel, BorderLayout.SOUTH );
		adventuringPanel.add( contentPanel, BorderLayout.NORTH );

		tabs.add( adventuringPanel, "Adventure Select" );
	}

	private void addInventoryPanel( JTabbedPane tabs )
	{
		tabs.add( new JPanel(), "Inventory / Equipment" );
	}

	private void addMallBrowsingPanel( JTabbedPane tabs )
	{
		tabs.add( new JPanel(), "Mall of Loathing" );
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu menu = new JMenu("View");
		menu.setMnemonic( KeyEvent.VK_V );
		menuBar.add( menu );

		JMenuItem menuItem = new JMenuItem( "Character Sheet", KeyEvent.VK_C );
		menuItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				updateDisplay( NOCHANGE_STATE, "Retrieving character data..." );
				Runnable display = new Runnable()
				{
					public void run()
					{
						CharsheetFrame csheet = new CharsheetFrame( client );
						csheet.pack();  csheet.setVisible( true );
						csheet.requestFocus();
						updateDisplay( NOCHANGE_STATE, "" );
					}
				};
				SwingUtilities.invokeLater( display );
			}
		});

		menu.add( menuItem );
	}

	protected class AdventureSelectPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;
		private JLabel serverReplyLabel;

		JComboBox locationField;
		JTextField countField;

		public AdventureSelectPanel( LockableListModel list )
		{
			super( "begin", "cancel" );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			serverReplyLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( serverReplyLabel );

			locationField = new JComboBox( list );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Location: ", locationField );
			elements[1] = new VerifiableElement( "# of turns: ", countField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			locationField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		public void clear()
		{
			Runnable updateAComponent = new Runnable() {
				public void run()
				{
					countField.setText( "" );
					requestFocus();
				}
			};
			SwingUtilities.invokeLater(updateAComponent);
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.  For now, since there's
			// no actual functionality, simply parse the values.

			Runnable request = (Runnable) locationField.getSelectedItem();

			try
			{
				int count = Integer.parseInt( countField.getText() );
				updateDisplay( DISABLED_STATE, "Request 1 in progress..." );
				client.makeRequest( request, count );
			}
			catch ( NumberFormatException e )
			{
				// If the number placed inside of the count list was not
				// an actual integer value, pretend nothing happened.
				// Using exceptions for flow control is bad style, but
				// this will be fixed once we add functionality.
			}
		}

		protected void actionCancelled()
		{
			// Once the stubs are finished, this will notify the
			// client to terminate the loop early.  For now, since
			// there's no actual functionality, simply request focus

			updateDisplay( ENABLED_STATE, "Adventuring terminated." );
			client.cancelRequest();
			requestFocus();
		}

		public void requestFocus()
		{	locationField.requestFocus();
		}
	}

	protected class AdventureResultsPanel extends JPanel
	{
		public AdventureResultsPanel( LockableListModel tally )
		{
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
			add( JComponentUtilities.createLabel( "Adventure Results", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );

			JList tallyDisplay = new JList( tally );
			tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			tallyDisplay.setVisibleRowCount( 5 );

			add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
		}
	}
}