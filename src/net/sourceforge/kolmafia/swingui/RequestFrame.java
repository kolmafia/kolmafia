/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LimitedSizeChatBuffer;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public class RequestFrame
	extends GenericFrame
{
	private static final int HISTORY_LIMIT = 4;
	private static final Pattern IMAGE_PATTERN =
		Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\"\'>]+" );

	private static final ArrayList sideBarFrames = new ArrayList();

	private int locationIndex = 0;
	private final ArrayList history = new ArrayList();
	private final ArrayList shownHTML = new ArrayList();

	private String currentLocation;
	private final LimitedSizeChatBuffer mainBuffer;
	private LimitedSizeChatBuffer sideBuffer;

	public RequestPane sideDisplay;
	public RequestPane mainDisplay;

	private JComboBox scriptSelect;
	private BrowserComboBox functionSelect, gotoSelect;
	private final AutoHighlightTextField locationField = new AutoHighlightTextField();

	public RequestFrame()
	{
		this( "Mini-Browser" );

		this.displayRequest( new GenericRequest( "main.php" ) );
	}

	public RequestFrame( final String title )
	{
		super( title );

		this.mainDisplay = new RequestPane();
		this.mainDisplay.addHyperlinkListener( new RequestHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( false );
		JScrollPane mainScroller = this.mainBuffer.setChatDisplay( this.mainDisplay );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		this.constructSideBar( mainScroller );
		this.getToolbar();
	}

	public boolean showInWindowMenu()
	{
		return false;
	}

	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar( true );

		// Add toolbar pieces so that people can quickly
		// go to locations they like.

		toolbarPanel.add( new BackButton() );
		toolbarPanel.add( new ForwardButton() );
		toolbarPanel.add( new HomeButton() );
		toolbarPanel.add( new ReloadButton() );

		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( this.locationField );
		toolbarPanel.add( new JToolBar.Separator() );

		GoButton button = new GoButton();
		toolbarPanel.add( button );

		return toolbarPanel;
	}

	private void constructSideBar( final JScrollPane mainScroller )
	{
		if ( !this.hasSideBar() )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			this.framePanel.setLayout( new BorderLayout() );
			this.framePanel.add( mainScroller, BorderLayout.CENTER );
			return;
		}

		RequestFrame.sideBarFrames.add( this );

		this.sideDisplay = new RequestPane();
		this.sideDisplay.addHyperlinkListener( new RequestHyperlinkAdapter() );

		this.sideBuffer = new LimitedSizeChatBuffer( false );
		JScrollPane sideScroller = this.sideBuffer.setChatDisplay( this.sideDisplay );
		JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

		JSplitPane horizontalSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
		horizontalSplit.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( horizontalSplit, 600, 450 );

		// Add the standard locations handled within the
		// mini-browser, including inventory, character
		// information, skills and account setup.

		this.functionSelect = new BrowserComboBox();
		this.functionSelect.addItem( new BrowserComboBoxItem( "- Select -", "" ) );

		for ( int i = 0; i < KoLConstants.FUNCTION_MENU.length; ++i )
		{
			this.functionSelect.addItem( new BrowserComboBoxItem(
				KoLConstants.FUNCTION_MENU[ i ][ 0 ], KoLConstants.FUNCTION_MENU[ i ][ 1 ] ) );
		}

		// Add the browser "goto" menu, because people
		// are familiar with seeing this as well.  But,
		// place it all inside of a "travel" menu.

		this.gotoSelect = new BrowserComboBox();
		this.gotoSelect.addItem( new BrowserComboBoxItem( "- Select -", "" ) );
		for ( int i = 0; i < KoLConstants.GOTO_MENU.length; ++i )
		{
			this.gotoSelect.addItem( new BrowserComboBoxItem(
				KoLConstants.GOTO_MENU[ i ][ 0 ], KoLConstants.GOTO_MENU[ i ][ 1 ] ) );
		}

		JPanel topMenu = new JPanel();
		topMenu.setOpaque( true );
		topMenu.setBackground( Color.white );

		topMenu.add( new JLabel( "Function:" ) );
		topMenu.add( this.functionSelect );
		topMenu.add( new JLabel( "Go To:" ) );
		topMenu.add( this.gotoSelect );
		topMenu.add( Box.createHorizontalStrut( 20 ) );

		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + HolidayDatabase.getRonaldPhase() + ".gif" );
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + HolidayDatabase.getGrimacePhase() + ".gif" );

		topMenu.add( new JLabel(
			JComponentUtilities.getImage( "itemimages/smoon" + HolidayDatabase.getRonaldPhase() + ".gif" ) ) );
		topMenu.add( new JLabel(
			JComponentUtilities.getImage( "itemimages/smoon" + HolidayDatabase.getGrimacePhase() + ".gif" ) ) );

		topMenu.add( Box.createHorizontalStrut( 20 ) );

		this.scriptSelect = new JComboBox();
		String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );
		for ( int i = 0; i < scriptList.length; ++i )
		{
			this.scriptSelect.addItem( i + 1 + ": " + scriptList[ i ] );
		}

		topMenu.add( this.scriptSelect );
		topMenu.add( new ExecuteScriptButton() );

		this.functionSelect.setSelectedIndex( 0 );
		this.gotoSelect.setSelectedIndex( 0 );

		JPanel container = new JPanel( new BorderLayout() );
		container.add( topMenu, BorderLayout.NORTH );
		container.add( horizontalSplit, BorderLayout.CENTER );

		this.framePanel.setLayout( new BorderLayout() );
		this.framePanel.add( container, BorderLayout.CENTER );
		RequestFrame.refreshStatus();
	}

	private class BrowserComboBox
		extends JComboBox
	{
		public BrowserComboBox()
		{
			this.addActionListener( new BrowserComboBoxListener() );
		}
	}

	private class BrowserComboBoxListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			BrowserComboBox source = (BrowserComboBox) e.getSource();
			BrowserComboBoxItem selected = (BrowserComboBoxItem) source.getSelectedItem();

			if ( !selected.getLocation().equals( "" ) )
			{
				RequestFrame.this.refresh( new GenericRequest( selected.getLocation() ) );
			}

			source.setSelectedIndex( 0 );
		}
	}

	private class ExecuteScriptButton
		extends ThreadedButton
	{
		public ExecuteScriptButton()
		{
			super( "exec" );
		}

		public void run()
		{
			String command = (String) RequestFrame.this.scriptSelect.getSelectedItem();
			if ( command == null )
			{
				return;
			}

			command = command.substring( command.indexOf( ":" ) + 1 ).trim();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( command );
		}
	}

	private class BrowserComboBoxItem
	{
		private final String name;
		private final String location;

		public BrowserComboBoxItem( final String name, final String location )
		{
			this.name = name;
			this.location = location;
		}

		public String toString()
		{
			return this.name;
		}

		public String getLocation()
		{
			return this.location;
		}
	}

	/**
	 * Returns whether or not this request frame has a side bar. This is used to ensure that bookmarks correctly use a
	 * new frame if this frame does not have one.
	 */

	public boolean hasSideBar()
	{
		return true;
	}

	public String getCurrentLocation()
	{
		return this.currentLocation;
	}

	/**
	 * Utility method which refreshes the current frame with data contained in the given request. If the request has not
	 * yet been run, it will be run before the data is display in this frame.
	 */

	public void refresh( final GenericRequest request )
	{
		this.displayRequest( request );

		if ( !this.isVisible() && !this.appearsInTab() )
		{
			this.setVisible( true );
		}
	}

	public String getDisplayHTML( final String responseText )
	{
		return RequestEditorKit.getDisplayHTML( this.currentLocation, responseText );
	}

	/**
	 * Utility method which displays the given request.
	 */

	public void displayRequest( GenericRequest request )
	{
		if ( this.mainBuffer == null || request == null )
		{
			return;
		}

		if ( request instanceof FightRequest )
		{
			request = new GenericRequest( request.getURLString() );
			request.responseText = FightRequest.lastResponseText;
		}

		this.currentLocation = request.getURLString();
		this.mainBuffer.clearBuffer();

		if ( request.responseText == null || request.responseText.length() == 0 )
		{
			// New prevention mechanism: tell the requests that there
			// will be no synchronization.

			boolean original = Preferences.getBoolean( "showAllRequests" );
			Preferences.setBoolean( "showAllRequests", false );

			RequestThread.postRequest( request );
			Preferences.setBoolean( "showAllRequests", original );

			// If this resulted in a redirect, then update the display
			// to indicate that you were redirected and the display
			// cannot be shown in the minibrowser.

			if ( request.responseText == null || request.responseText.length() == 0 )
			{
				this.mainBuffer.append( "<b>Tried to access</b>: " + this.currentLocation );
				this.mainBuffer.append( "<br><b>Redirected</b>: " + request.redirectLocation );
				return;
			}

			StaticEntity.externalUpdate( this.currentLocation, request.responseText );
		}

		this.showHTML( this.currentLocation, request.responseText );
	}

	public void showHTML( String location, final String responseText )
	{
		// Function exactly like a history in a normal browser -
		// if you open a new frame after going back, all the ones
		// in the future get removed.

		String renderText = this.getDisplayHTML( responseText );

		this.history.add( location );
		this.shownHTML.add( renderText );

		if ( this.history.size() > RequestFrame.HISTORY_LIMIT )
		{
			this.history.remove( 0 );
			this.shownHTML.remove( 0 );
		}

		location = location.substring( location.lastIndexOf( "/" ) + 1 );
		this.locationField.setText( location );

		this.locationIndex = this.shownHTML.size() - 1;

		Matcher imageMatcher = RequestFrame.IMAGE_PATTERN.matcher( renderText );
		while ( imageMatcher.find() )
		{
			FileUtilities.downloadImage( imageMatcher.group() );
		}

		this.mainBuffer.append( renderText );
	}

	private class HomeButton
		extends ThreadedButton
	{
		public HomeButton()
		{
			super( JComponentUtilities.getImage( "home.gif" ) );
		}

		public void run()
		{
			RequestFrame.this.refresh( new GenericRequest( "main.php" ) );
		}
	}

	private class BackButton
		extends ThreadedButton
	{
		public BackButton()
		{
			super( JComponentUtilities.getImage( "back.gif" ) );
		}

		public void run()
		{
			if ( RequestFrame.this.locationIndex > 0 )
			{
				--RequestFrame.this.locationIndex;
				RequestFrame.this.mainBuffer.clearBuffer();
				RequestFrame.this.mainBuffer.append( (String) RequestFrame.this.shownHTML.get( RequestFrame.this.locationIndex ) );
				RequestFrame.this.locationField.setText( (String) RequestFrame.this.history.get( RequestFrame.this.locationIndex ) );
			}
		}
	}

	private class ForwardButton
		extends ThreadedButton
	{
		public ForwardButton()
		{
			super( JComponentUtilities.getImage( "forward.gif" ) );
		}

		public void run()
		{
			if ( RequestFrame.this.locationIndex + 1 < RequestFrame.this.shownHTML.size() )
			{
				++RequestFrame.this.locationIndex;
				RequestFrame.this.mainBuffer.clearBuffer();
				RequestFrame.this.mainBuffer.append( (String) RequestFrame.this.shownHTML.get( RequestFrame.this.locationIndex ) );
				RequestFrame.this.locationField.setText( (String) RequestFrame.this.history.get( RequestFrame.this.locationIndex ) );
			}
		}
	}

	private class ReloadButton
		extends ThreadedButton
	{
		public ReloadButton()
		{
			super( JComponentUtilities.getImage( "reload.gif" ) );
		}

		public void run()
		{
			if ( RequestFrame.this.currentLocation == null )
			{
				return;
			}

			RequestFrame.this.refresh( new GenericRequest( RequestFrame.this.currentLocation ) );
		}
	}

	private class GoButton
		extends ThreadedButton
	{
		public GoButton()
		{
			super( "Go" );
			RequestFrame.this.locationField.addKeyListener( new GoAdapter() );
		}

		public void run()
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( RequestFrame.this.locationField.getText() );
			GenericRequest request =
				RequestEditorKit.extractRequest( adventure == null ? RequestFrame.this.locationField.getText() : adventure.getRequest().getURLString() );
			RequestFrame.this.refresh( request );
		}

		private class GoAdapter
			extends KeyAdapter
		{
			public void keyReleased( final KeyEvent e )
			{
				if ( e.isConsumed() )
				{
					return;
				}

				if ( e.getKeyCode() != KeyEvent.VK_ENTER )
				{
					return;
				}

				GoButton.this.actionPerformed( null );
				e.consume();
			}
		}
	}

	public static final void refreshStatus()
	{
		RequestFrame current;
		String displayHTML = RequestEditorKit.getDisplayHTML( "charpane.php", CharPaneRequest.getLastResponse() );

		for ( int i = 0; i < RequestFrame.sideBarFrames.size(); ++i )
		{
			current = (RequestFrame) RequestFrame.sideBarFrames.get( i );

			current.sideBuffer.clearBuffer();
			current.sideBuffer.append( displayHTML );
		}
	}

	public boolean containsText( final String search )
	{
		return this.mainBuffer.getBuffer().indexOf( search ) != -1;
	}

	public void dispose()
	{
		this.history.clear();
		this.shownHTML.clear();
		super.dispose();
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	private static final Pattern TOID_PATTERN = Pattern.compile( "toid=(\\d+)" );

	public class RequestHyperlinkAdapter
		extends HyperlinkAdapter
	{
		public void handleInternalLink( final String location )
		{
			if ( location.equals( "lchat.php" ) )
			{
				ChatManager.initialize();
			}
			else if ( location.startsWith( "makeoffer.php" ) || location.startsWith( "counteroffer.php" ) )
			{
				StaticEntity.getClient().openRelayBrowser( location );
			}
			else if ( location.startsWith( "sendmessage.php" ) || location.startsWith( "town_sendgift.php" ) )
			{
				// Attempts to send a message should open up
				// KoLmafia's built-in message sender.

				Matcher idMatcher = RequestFrame.TOID_PATTERN.matcher( location );

				String[] parameters = new String[] { idMatcher.find() ? idMatcher.group( 1 ) : "" };
				GenericFrame.createDisplay( SendMessageFrame.class, parameters );
			}
			else if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
			{
				DescriptionFrame.showLocation( location );
				return;
			}
			else
			{
				RequestFrame.this.refresh( RequestEditorKit.extractRequest( location ) );
			}
		}
	}
}
