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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.Box;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.ListSelectionModel;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private static SidePaneRefresher REFRESHER = new SidePaneRefresher();
	private static boolean refreshStatusDisabled = false;

	private int locationIndex = 0;
	private ArrayList history = new ArrayList();
	private ArrayList shownHTML = new ArrayList();

	private int combatRound;
	private RequestFrame parent;
	private String currentLocation;
	private LimitedSizeChatBuffer mainBuffer;

	private boolean hasSideBar;
	private boolean isRefreshing = false;
	private LimitedSizeChatBuffer sideBuffer;

	protected JEditorPane sideDisplay;
	protected JEditorPane mainDisplay;

	private static BrowserComboBox functionSelect, gotoSelect;
	private static JCheckBox runBetweenBattleChecks;
	private JTextField locationField = new JTextField();

	public RequestFrame()
	{	this( "Mini-Browser", new KoLRequest( StaticEntity.getClient(), "main.php" ) );
	}

	public RequestFrame( KoLRequest request )
	{	this( "Mini-Browser", null, request );
	}

	public RequestFrame( String title, KoLRequest request )
	{	this( title, null, request );
	}

	public RequestFrame( RequestFrame parent, KoLRequest request )
	{	this( "Mini-Browser", parent, request );
	}

	public RequestFrame( String title, RequestFrame parent, KoLRequest request )
	{
		super( title );
		this.parent = parent;

		this.hasSideBar = getClass() == RequestFrame.class &&
			request != null && !request.getURLString().startsWith( "chat" ) && !request.getURLString().startsWith( "static" ) &&
			!request.getURLString().startsWith( "desc" ) && !request.getURLString().startsWith( "showplayer" ) &&
			!request.getURLString().startsWith( "doc" ) && !request.getURLString().startsWith( "searchp" );

		setCombatRound( request );

		this.mainDisplay = new JEditorPane();
		this.mainDisplay.setEditable( false );

		if ( !(this instanceof PendingTradesFrame) )
			this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( "Mini-Browser", false );
		JScrollPane mainScroller = this.mainBuffer.setChatDisplay( this.mainDisplay );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		if ( !hasSideBar )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			framePanel.setLayout( new BorderLayout() );
			framePanel.add( mainScroller, BorderLayout.CENTER );
		}
		else
		{
			this.sideDisplay = new JEditorPane();
			this.sideDisplay.setEditable( false );
			this.sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

			this.sideBuffer = new LimitedSizeChatBuffer( "", false );
			JScrollPane sideScroller = this.sideBuffer.setChatDisplay( sideDisplay );
			JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

			JSplitPane horizontalSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
			horizontalSplit.setOneTouchExpandable( true );
			JComponentUtilities.setComponentSize( horizontalSplit, 600, 450 );

			// Add the standard locations handled within the
			// mini-browser, including inventory, character
			// information, skills and account setup.

			functionSelect = new BrowserComboBox();
			functionSelect.addItem( new BrowserComboBoxItem( " - Function - ", "" ) );

			functionSelect.addItem( new BrowserComboBoxItem( "Consumables", "inventory.php?which=1" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Equipment", "inventory.php?which=2" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Miscellaneous", "inventory.php?which=3" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Character Sheet", "charsheet.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Terrarium", "familiar.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Usable Skills", "skills.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Read Messages", "messages.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Quest Log", "questlog.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Account Menu", "account.php" ) );

			// Add the browser "goto" menu, because people
			// are familiar with seeing this as well.  But,
			// place it all inside of a "travel" menu.

			gotoSelect = new BrowserComboBox();
			gotoSelect.addItem( new BrowserComboBoxItem( " - Goto (Maki) - ", "" ) );

			gotoSelect.addItem( new BrowserComboBoxItem( "Main Map", "main.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Seaside Town", "town.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Clan Hall", "clan_hall.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Campground", "campground.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Big Mountains", "mountains.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Mt. McLargeHuge", "mclargehuge.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Nearby Plains", "plains.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Above Beanstalk", "beanstalk.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Sorceress' Lair", "lair.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Desert Beach", "beach.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Distant Woods", "woods.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Mysterious Island", "island.php" ) );

			JPanel topMenu = new JPanel();
			topMenu.setOpaque( true );
			topMenu.setBackground( Color.white );

			topMenu.add( functionSelect );
			topMenu.add( gotoSelect );
			topMenu.add( Box.createHorizontalStrut( 20 ) );

			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" );
			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" );

			topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" ) ) );
			topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" ) ) );

			topMenu.add( Box.createHorizontalStrut( 20 ) );
			topMenu.add( runBetweenBattleChecks = new JCheckBox( "Run between-battle scripts" ) );
			runBetweenBattleChecks.setOpaque( false );

			functionSelect.setSelectedIndex( 0 );
			gotoSelect.setSelectedIndex( 0 );

			JPanel container = new JPanel( new BorderLayout() );
			container.add( topMenu, BorderLayout.NORTH );
			container.add( horizontalSplit, BorderLayout.CENTER );

			tabs = new JTabbedPane( JTabbedPane.BOTTOM );
			tabs.addTab( "Mini-Browser", container );

			JScrollPane restoreScroller = new JScrollPane( new RestoreOptionsPanel( false ),
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );
			tabs.add( "Between Battle Scripts", restoreScroller );

			framePanel.setLayout( new BorderLayout() );
			framePanel.add( tabs, BorderLayout.CENTER );
		}

		// Add toolbar pieces so that people can quickly
		// go to locations they like.

		JToolBar toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
		getContentPane().add( toolbarPanel, BorderLayout.NORTH );

		toolbarPanel.add( new BackButton() );
		toolbarPanel.add( new ForwardButton() );
		toolbarPanel.add( new HomeButton() );
		toolbarPanel.add( new ReloadButton() );

		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( locationField );
		toolbarPanel.add( new JToolBar.Separator() );

		GoButton button = new GoButton();
		toolbarPanel.add( button );

		// If this has a side bar, then it will need to be notified
		// whenever there are updates to the player status.

		REFRESHER.add( this );

		if ( this.hasSideBar )
			refreshStatus();

		(new DisplayRequestThread( getClass() == RequestFrame.class && StaticEntity.getClient().getCurrentRequest() instanceof FightRequest ?
			StaticEntity.getClient().getCurrentRequest() : request, true )).start();
	}

	private class BrowserComboBox extends JComboBox implements ActionListener
	{
		public BrowserComboBox()
		{	addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			BrowserComboBox source = (BrowserComboBox) e.getSource();
			BrowserComboBoxItem selected = (BrowserComboBoxItem) source.getSelectedItem();

			if ( !selected.getLocation().equals( "" ) )
				refresh( new KoLRequest( StaticEntity.getClient(), selected.getLocation() ) );

			source.setSelectedIndex( 0 );
		}
	}

	private class BrowserComboBoxItem
	{
		private String name;
		private String location;

		public BrowserComboBoxItem( String name, String location )
		{
			this.name = name;
			this.location = location;
		}

		public String toString()
		{	return name;
		}

		public String getLocation()
		{	return location;
		}
	}

	/**
	 * Returns whether or not this request frame has a side bar.
	 * This is used to ensure that bookmarks correctly use a
	 * new frame if this frame does not have one.
	 */

	public boolean hasSideBar()
	{	return hasSideBar;
	}
	
	public String getCurrentLocation()
	{	return currentLocation;
	}

	/**
	 * Utility method which refreshes the current frame with
	 * data contained in the given request.  If the request
	 * has not yet been run, it will be run before the data
	 * is display in this frame.
	 */

	public void refresh( KoLRequest request )
	{	refresh( request, false );
	}

	public void refresh( KoLRequest request, boolean shouldEnable )
	{
		String location = request.getURLString();

		if ( parent == null || location.startsWith( "search" ) || location.startsWith( "desc" ) )
		{
			setCombatRound( request );

			// Only record raw mini-browser requests
			if ( request.getClass() == KoLRequest.class )
				StaticEntity.getClient().getMacroStream().println( location );

			(new DisplayRequestThread( request, shouldEnable )).start();
		}
		else
			parent.refresh( request );
	}

	private void setCombatRound( KoLRequest request )
	{
		if ( request != null && request instanceof FightRequest )
			combatRound = ((FightRequest)request).getCombatRound();
		else
			combatRound = 1;
	}

	protected static String getDisplayHTML( String responseText )
	{	return RequestEditorKit.getDisplayHTML( responseText );
	}

	/**
	 * A special thread class which ensures that attempts to
	 * refresh the frame with data do not long the Swing thread.
	 */

	protected class DisplayRequestThread extends Thread
	{
		private KoLRequest request;
		private boolean shouldEnable;

		public DisplayRequestThread( KoLRequest request, boolean shouldEnable )
		{
			this.request = request;
			this.shouldEnable = shouldEnable;
		}

		public void run()
		{
			synchronized ( DisplayRequestThread.class )
			{
				mainBuffer.clearBuffer();
				mainBuffer.append( "Retrieving..." );

				currentLocation = request.getURLString();
				setupRequest();

				if ( request != null && request.responseText != null && request.responseText.length() != 0 )
				{
					StaticEntity.getClient().setCurrentRequest( request );
					displayRequest( request.responseText );
				}
				else
				{
					// If this resulted in a redirect, then update the display
					// to indicate that you were redirected and the display
					// cannot be shown in the minibrowser.

					mainBuffer.clearBuffer();
					mainBuffer.append( "Redirected to unknown page: &lt;" + request.redirectLocation + "&gt;" );
					return;
				}
			}

			// Have the StaticEntity.getClient() update occur outside of the
			// synchronization block so that the appearance
			// of a GUI lockup doesn't happen.

			updateClient();
		}

		private void setupRequest()
		{
			if ( request == null )
				return;

			if ( request.responseText == null || request.responseText.length() == 0 )
			{
				// New prevention mechanism: tell the requests that there
				// will be no synchronization.

				String original = getProperty( "synchronizeFightFrame" );
				setProperty( "synchronizeFightFrame", "false" );
				request.run();
				setProperty( "synchronizeFightFrame", original );
			}
		}

		private void displayRequest( String text )
		{
			mainBuffer.clearBuffer();

			// Function exactly like a history in a normal browser -
			// if you open a new frame after going back, all the ones
			// in the future get removed.

			while ( shownHTML.size() > locationIndex )
			{
				history.remove( locationIndex );
				shownHTML.remove( locationIndex );
			}

			String renderText = getDisplayHTML( text );
			
			history.add( request.getURLString() );
			shownHTML.add( renderText );

			// Only allow 11 locations in the locations buffer.  That
			// way, memory doesn't get sucked up by synchronization.

			while ( shownHTML.size() > 11 )
				shownHTML.remove( request );

			String location = request.getURLString();

			locationField.setText( location );
			locationIndex = shownHTML.size();

			mainBuffer.append( renderText );
			mainDisplay.setCaretPosition( 0 );
			System.gc();

			// One last thing which needs to be done is if the player
			// just ascended, you need to refresh everything.

			if ( location.equals( "main.php?refreshtop=true&noobmessage=true" ) )
			{
				StaticEntity.getClient().refreshSession();
				StaticEntity.getClient().enableDisplay();
			}
		}

		private void updateClient()
		{
			StaticEntity.externalUpdate( request.getURLString(), request.responseText );			
			if ( shouldEnable )
				StaticEntity.getClient().enableDisplay();
		}
	}

	private class HomeButton extends JButton implements ActionListener
	{
		public HomeButton()
		{
			super( JComponentUtilities.getImage( "home.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	refresh( new KoLRequest( StaticEntity.getClient(), "main.php" ) );
		}
	}

	private class BackButton extends JButton implements ActionListener
	{
		public BackButton()
		{
			super( JComponentUtilities.getImage( "back.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( locationIndex > 1 )
			{
				--locationIndex;
				mainBuffer.clearBuffer();
				mainBuffer.append( (String) shownHTML.get( locationIndex - 1 ) );
				locationField.setText( (String) history.get( locationIndex - 1 ) );
			}
		}
	}

	private class ForwardButton extends JButton implements ActionListener
	{
		public ForwardButton()
		{
			super( JComponentUtilities.getImage( "forward.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( locationIndex + 1 < shownHTML.size() )
			{
				mainBuffer.clearBuffer();
				mainBuffer.append( (String) shownHTML.get( locationIndex ) );
				locationField.setText( (String) history.get( locationIndex ) );
				++locationIndex;
			}
		}
	}

	private class ReloadButton extends JButton implements ActionListener
	{
		public ReloadButton()
		{
			super( JComponentUtilities.getImage( "reload.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	refresh( new KoLRequest( StaticEntity.getClient(), currentLocation, true ) );
		}
	}

	private class GoButton extends JButton implements ActionListener
	{
		public GoButton()
		{
			super( "Go" );
			addActionListener( this );
			locationField.addKeyListener( new GoAdapter() );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( locationField.getText() );
			KoLRequest request = RequestEditorKit.extractRequest( adventure == null ? locationField.getText() : adventure.getRequest().getURLString() );
			StaticEntity.getClient().getMacroStream().println( request.getURLString() );
			refresh( request );
		}

		private class GoAdapter extends KeyAdapter
		{
			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					actionPerformed( null );
			}
		}
	}

	private static class SidePaneRefresher extends ArrayList implements Runnable
	{
		public void run()
		{
			int adventuresBefore = KoLCharacter.getAdventuresLeft();

			CharpaneRequest instance = CharpaneRequest.getInstance();
			instance.run();

			if ( adventuresBefore > KoLCharacter.getAdventuresLeft() && runBetweenBattleChecks != null && runBetweenBattleChecks.isSelected() )
			{
				refreshStatus( "" );

				functionSelect.setEnabled( false );
				gotoSelect.setEnabled( false );
				runBetweenBattleChecks.setEnabled( false );

				StaticEntity.getClient().runBetweenBattleChecks();

				runBetweenBattleChecks.setEnabled( true );
				gotoSelect.setEnabled( true );
				functionSelect.setEnabled( true );

				instance.run();
			}

			refreshStatus( getDisplayHTML( instance.responseText ) );
		}

		public void refreshStatus( String text )
		{
			RequestFrame [] frames = new RequestFrame[ this.size() ];
			toArray( frames );

			for ( int i = 0; i < frames.length; ++i )
			{
				if ( frames[i].hasSideBar() )
				{
					frames[i].sideBuffer.clearBuffer();
					frames[i].sideBuffer.append( text );
					frames[i].sideDisplay.setCaretPosition( 0 );
				}
			}
		}
	}

	public static void refreshStatus()
	{
		if ( REFRESHER.isEmpty() || refreshStatusDisabled || (runBetweenBattleChecks != null && !runBetweenBattleChecks.isEnabled()) )
			return;

		(new Thread( REFRESHER )).start();
	}

	public static boolean willRefreshStatus()
	{	return !REFRESHER.isEmpty() && !refreshStatusDisabled && runBetweenBattleChecks != null && runBetweenBattleChecks.isEnabled();
	}

	public static void disableRefreshStatus( boolean disable )
	{
		refreshStatusDisabled = disable;

		if ( disable )
			REFRESHER.refreshStatus( "" );
	}

	public void dispose()
	{
		history.clear();
		shownHTML.clear();
		REFRESHER.remove( this );
		super.dispose();
	}
}
