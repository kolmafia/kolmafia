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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * adventure in the Kingdom of Loathing.  As the class is developed, it will also
 * provide other adventure-related functionality, such as inventory management
 * and mall purchases.  Its content panel will also change, pending the activity
 * executed at that moment.
 */

public class AdventureFrame extends KoLFrame
{
	private JTabbedPane tabs;
	private AdventureSelectPanel adventureSelect;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 *
	 * @param	client	Client/session associated with this frame
	 * @param	availableAdventures	Adventures available to the user
	 * @param	resultsTally	Tally of adventuring results
	 */

	public AdventureFrame( KoLmafia client, LockableListModel availableAdventures, LockableListModel resultsTally )
	{
		super( "KoLmafia: " + client.getLoginName(), client );
		setResizable( false );

		tabs = new JTabbedPane();

		addAdventuringPanel( availableAdventures, resultsTally );
		addInventoryPanel();  tabs.setEnabledAt( 1, false );
		addMallBrowsingPanel();  tabs.setEnabledAt( 2, false );

		getContentPane().add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		updateDisplay( ENABLED_STATE, " " );
		addWindowListener( new LogoutRequestAdapter() );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		addMenuBar();
	}

	/**
	 * Utility method which adds the adventure selection panel to the
	 * pre-constructed tabs.  The adventure selection panel also keeps
	 * track of the tally of results and the adventures which are
	 * available to the user.
	 *
	 * @param	availableAdventures	Adventures available to the user
	 * @param	resultsTally	Tally of adventuring results
	 */

	private void addAdventuringPanel( LockableListModel availableAdventures, LockableListModel resultsTally )
	{
		JPanel summaryPanel = new AdventureResultsPanel( resultsTally );
		adventureSelect = new AdventureSelectPanel( availableAdventures );

		JPanel adventuringPanel = new JPanel();
		adventuringPanel.setLayout( new BorderLayout( 10, 10 ) );
		adventuringPanel.add( summaryPanel, BorderLayout.SOUTH );
		adventuringPanel.add( contentPanel, BorderLayout.NORTH );

		tabs.addTab( "Adventure Select", adventuringPanel );
	}

	/**
	 * Utility method which adds the inventory management panel to the
	 * pre-constructed tabs.  Because inventory management has not yet
	 * been implemented, this method adds a blank panel.
	 */

	private void addInventoryPanel()
	{
		tabs.addTab( "Inventory / Equipment", new JPanel() );
	}

	/**
	 * Utility method which adds the mall browsing panel to the
	 * pre-constructed tabs.  Because mall browsing has not yet
	 * been implemented, this method adds a blank panel.
	 */

	private void addMallBrowsingPanel()
	{
		tabs.addTab( "Mall of Loathing", new JPanel() );
	}

	/**
	 * Utility method used to add a menu bar to the <code>AdventureFrame</code>.
	 * The menu bar contains configuration options and the general license
	 * information associated with <code>KoLmafia</code>.  In addition, the
	 * method adds an item which allows the user to view their character sheet.
	 */

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu menu = new JMenu("View");
		menu.setMnemonic( KeyEvent.VK_V );
		menuBar.add( menu );

		JMenuItem menuItem = new JMenuItem( "Character Sheet", KeyEvent.VK_C );
		menuItem.addActionListener( new ViewCharacterSheetListener() );

		menu.add( menuItem );

		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	protected class AdventureSelectPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;
		private JLabel serverReplyLabel;

		JComboBox locationField;
		JTextField countField;

		public AdventureSelectPanel( LockableListModel list )
		{
			super( "begin", "cancel", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

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
			countField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			contentPanel = adventureSelect;
			(new AdventureRequestThread()).start();
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

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the adventuring requests.
		 */

		private class AdventureRequestThread extends Thread
		{
			public AdventureRequestThread()
			{
				super( "Adv-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					int count = Integer.parseInt( countField.getText() );
					updateDisplay( DISABLED_STATE, "Request 1 in progress..." );
					Runnable request = (Runnable) locationField.getSelectedItem();
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
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the
	 * results in the <code>AdventureFrame</code>.  Note that all of the
	 * tallying functionality is handled by the <code>LockableListModel</code>
	 * provided, so this functions as a container for that list model.
	 */

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

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing a character sheet.
	 */

	private class ViewCharacterSheetListener extends Thread implements ActionListener
	{
		public ViewCharacterSheetListener()
		{
			super( "CSheet-Display-Thread" );
			setDaemon( true );
		}

		public void run()
		{
			CharsheetFrame csheet = new CharsheetFrame( client );
			csheet.pack();  csheet.setVisible( true );
			csheet.requestFocus();
			updateDisplay( NOCHANGE_STATE, "" );
		}

		public void actionPerformed( ActionEvent e )
		{
			updateDisplay( NOCHANGE_STATE, "Retrieving character data..." );
			this.start();
		}
	}

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	private class LogoutRequestAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{	(new LogoutRequest( client )).run();
		}
	}
}