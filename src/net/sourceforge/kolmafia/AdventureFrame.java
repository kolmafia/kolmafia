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
import java.awt.CardLayout;
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

// other imports
import java.text.DecimalFormat;
import java.text.ParseException;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * adventure in the Kingdom of Loathing.  As the class is developed, it will also
 * provide other adventure-related functionality, such as inventoryManage management
 * and mall purchases.  Its content panel will also change, pending the activity
 * executed at that moment.
 */

public class AdventureFrame extends KoLFrame
{
	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 *
	 * @param	client	Client/session associated with this frame
	 * @param	adventureList	Adventures available to the user
	 * @param	resultsTally	Tally of adventuring results
	 */

	public AdventureFrame( KoLmafia client, LockableListModel adventureList, LockableListModel resultsTally )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()), client );
		setResizable( false );

		JTabbedPane tabs = new JTabbedPane();

		adventureSelect = new AdventureSelectPanel( adventureList, resultsTally );
		tabs.addTab( "Adventure Select", adventureSelect );

		mallSearch = new MallSearchPanel();
		tabs.addTab( "Mall of Loathing", mallSearch );

		getContentPane().add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		updateDisplay( ENABLED_STATE, " " );
		addWindowListener( new LogoutRequestAdapter() );

		addMenuBar();
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

		JMenuItem csheetItem = new JMenuItem( "Character Sheet", KeyEvent.VK_C );
		csheetItem.addActionListener( new ViewCharacterSheetListener() );

		menu.add( csheetItem );

		JMenuItem imanageItem = new JMenuItem( "Item Manager", KeyEvent.VK_I );
		imanageItem.addActionListener( new ViewItemManagerListener() );

		menu.add( imanageItem );

		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComboBox locationField;
		private JTextField countField;

		public AdventureSelectPanel( LockableListModel adventureList, LockableListModel resultsTally )
		{
			super( "begin", "cancel", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			locationField = new JComboBox( adventureList );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Location: ", locationField );
			elements[1] = new VerifiableElement( "# of turns: ", countField );

			setContent( elements, resultsTally );
		}

		protected void setContent( VerifiableElement [] elements, LockableListModel resultsTally )
		{
			super.setContent( elements );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout( 10, 10 ) );
			southPanel.add( actionStatusPanel, BorderLayout.NORTH );
			southPanel.add( new AdventureResultsPanel( resultsTally ), BorderLayout.SOUTH );
			add( southPanel, BorderLayout.SOUTH );
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
		 * An internal class which represents the panel used for tallying the
		 * results in the <code>AdventureFrame</code>.  Note that all of the
		 * tallying functionality is handled by the <code>LockableListModel</code>
		 * provided, so this functions as a container for that list model.
		 */

		private class AdventureResultsPanel extends JPanel
		{
			public AdventureResultsPanel( LockableListModel resultsTally )
			{
				setLayout( new BorderLayout() );
				setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
				add( JComponentUtilities.createLabel( "Session Results Summary", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				JList tallyDisplay = new JList( resultsTally );
				tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
				tallyDisplay.setVisibleRowCount( 10 );

				add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
			}
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
					int count = df.parse( countField.getText() ).intValue();
					Runnable request = (Runnable) locationField.getSelectedItem();
					client.makeRequest( request, count );
				}
				catch ( ParseException e )
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
	 * An internal class which represents the panel used for mall
	 * searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel extends KoLPanel
	{
		private boolean currentlyBuying;

		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JTextField searchField;
		private JTextField countField;

		private LockableListModel results;
		private JList resultsDisplay;

		public MallSearchPanel()
		{
			super( "search", "buy selected", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			searchField = new JTextField();
			countField = new JTextField();
			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Search String: ", searchField );
			elements[1] = new VerifiableElement( "Limit Results: ", countField );

			setContent( elements );
			currentlyBuying = false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout( 10, 10 ) );
			southPanel.add( actionStatusPanel, BorderLayout.NORTH );
			southPanel.add( new SearchResultsPanel(), BorderLayout.SOUTH );
			add( southPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			resultsDisplay.setEnabled( isEnabled );
		}

		public void clear()
		{
			searchField.setText( "" );
			countField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			contentPanel = mallSearch;
			(new SearchMallRequestThread()).start();
		}

		protected void actionCancelled()
		{
			if ( currentlyBuying )
				return;

			contentPanel = mallSearch;
			(new PurchaseRequestThread()).start();
		}

		public void requestFocus()
		{	searchField.requestFocus();
		}

		/**
		 * An internal class which represents the panel used for tallying the
		 * results of the mall search request.  Note that all of the tallying
		 * functionality is handled by the <code>LockableListModel</code>
		 * provided, so this functions as a container for that list model.
		 */

		private class SearchResultsPanel extends JPanel
		{
			public SearchResultsPanel()
			{
				setLayout( new BorderLayout() );
				setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
				add( JComponentUtilities.createLabel( "Search Results", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				resultsDisplay = new JList( results );
				resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				resultsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
				resultsDisplay.setVisibleRowCount( 10 );

				add( new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the mall search request.
		 */

		private class SearchMallRequestThread extends Thread
		{
			public SearchMallRequestThread()
			{
				super( "Mall-Search-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					int storeCount = countField.getText().trim().length() == 0 ? 0 :
						Integer.parseInt( countField.getText() );
					updateDisplay( DISABLED_STATE, "Searching for items..." );
					(new SearchMallRequest( client, searchField.getText(), storeCount, results )).run();
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

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the mall search request.
		 */

		private class PurchaseRequestThread extends Thread
		{
			public PurchaseRequestThread()
			{
				super( "Purchase-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					updateDisplay( DISABLED_STATE, "Purchasing items..." );

					Object [] purchases = resultsDisplay.getSelectedValues();

					for ( int i = 0; i < purchases.length; ++i )
					{
						((Runnable)purchases[i]).run();
						results.remove( purchases[i] );
					}

					updateDisplay( ENABLED_STATE, "Purchases complete." );
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
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing a character sheet.
	 */

	private class ViewCharacterSheetListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			updateDisplay( NOCHANGE_STATE, "Retrieving character data..." );
			(new ViewCharacterSheetThread()).start();
		}

		private class ViewCharacterSheetThread extends Thread
		{
			public ViewCharacterSheetThread()
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
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing a character sheet.
	 */

	private class ViewItemManagerListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			updateDisplay( NOCHANGE_STATE, "Retrieving item data..." );
			(new ViewItemManagerThread()).start();
		}

		private class ViewItemManagerThread extends Thread
		{
			public ViewItemManagerThread()
			{
				super( "Item-Display-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				ItemManageFrame isheet = new ItemManageFrame( client );
				isheet.pack();  isheet.setVisible( true );
				isheet.requestFocus();
				updateDisplay( NOCHANGE_STATE, "" );
			}
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
		{
			if ( client != null )
			{
				client.deinitialize();
				(new LogoutRequestThread()).start();
			}
		}

		private class LogoutRequestThread extends Thread
		{
			public void run()
			{	(new LogoutRequest( client )).run();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new AdventureFrame( null, new LockableListModel(), new LockableListModel() );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}