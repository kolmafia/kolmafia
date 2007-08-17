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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private static final int HISTORY_LIMIT = 4;
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\"\'>]+" );

	private static final ArrayList sideBarFrames = new ArrayList();

	private int locationIndex = 0;
	private ArrayList history = new ArrayList();
	private ArrayList shownHTML = new ArrayList();

	private String currentLocation;
	private LimitedSizeChatBuffer mainBuffer;
	private LimitedSizeChatBuffer sideBuffer;

	public RequestPane sideDisplay;
	public RequestPane mainDisplay;

	private JComboBox scriptSelect;
	private BrowserComboBox functionSelect, gotoSelect;
	private JTextField locationField = new JTextField();

	public RequestFrame()
	{
		this( "Mini-Browser" );

		this.setDefaultCloseOperation( HIDE_ON_CLOSE );
		this.displayRequest( KoLRequest.VISITOR.constructURLString( "main.php" ) );
	}

	public RequestFrame( String title )
	{
		super( title );

		this.mainDisplay = new RequestPane();
		this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( false );
		JScrollPane mainScroller = this.mainBuffer.setChatDisplay( this.mainDisplay );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		constructSideBar( mainScroller );

		// Add toolbar pieces so that people can quickly
		// go to locations they like.

		JToolBar toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
		this.getContentPane().add( toolbarPanel, BorderLayout.NORTH );

		toolbarPanel.add( new BackButton() );
		toolbarPanel.add( new ForwardButton() );
		toolbarPanel.add( new HomeButton() );
		toolbarPanel.add( new ReloadButton() );

		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( this.locationField );
		toolbarPanel.add( new JToolBar.Separator() );

		GoButton button = new GoButton();
		toolbarPanel.add( button );
	}

	private void constructSideBar( JScrollPane mainScroller )
	{
		if ( !this.hasSideBar() )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			this.framePanel.setLayout( new BorderLayout() );
			this.framePanel.add( mainScroller, BorderLayout.CENTER );
			return;
		}

		sideBarFrames.add( this );

		this.sideDisplay = new RequestPane();
		this.sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

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

		for ( int i = 0; i < FUNCTION_MENU.length; ++i )
			this.functionSelect.addItem( new BrowserComboBoxItem( FUNCTION_MENU[i][0], FUNCTION_MENU[i][1] ) );

		// Add the browser "goto" menu, because people
		// are familiar with seeing this as well.  But,
		// place it all inside of a "travel" menu.

		this.gotoSelect = new BrowserComboBox();
		this.gotoSelect.addItem( new BrowserComboBoxItem( "- Select -", "" ) );
		for ( int i = 0; i < GOTO_MENU.length; ++i )
			this.gotoSelect.addItem( new BrowserComboBoxItem( GOTO_MENU[i][0], GOTO_MENU[i][1] ) );

		JPanel topMenu = new JPanel();
		topMenu.setOpaque( true );
		topMenu.setBackground( Color.white );

		topMenu.add( new JLabel( "Function:" ) );
		topMenu.add( this.functionSelect );
		topMenu.add( new JLabel( "Go To:" ) );
		topMenu.add( this.gotoSelect );
		topMenu.add( Box.createHorizontalStrut( 20 ) );

		RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" );
		RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" );

		topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" ) ) );
		topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" ) ) );

		topMenu.add( Box.createHorizontalStrut( 20 ) );

		this.scriptSelect = new JComboBox();
		String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );
		for ( int i = 0; i < scriptList.length; ++i )
			this.scriptSelect.addItem( (i+1) + ": " + scriptList[i] );

		topMenu.add( this.scriptSelect );
		topMenu.add( new ExecuteScriptButton() );

		this.functionSelect.setSelectedIndex( 0 );
		this.gotoSelect.setSelectedIndex( 0 );

		JPanel container = new JPanel( new BorderLayout() );
		container.add( topMenu, BorderLayout.NORTH );
		container.add( horizontalSplit, BorderLayout.CENTER );

		this.framePanel.setLayout( new BorderLayout() );
		this.framePanel.add( container, BorderLayout.CENTER );
		refreshStatus();
	}

	private class BrowserComboBox extends JComboBox
	{
		public BrowserComboBox()
		{	this.addActionListener( new BrowserComboBoxListener() );
		}
	}

	private class BrowserComboBoxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			BrowserComboBox source = (BrowserComboBox) e.getSource();
			BrowserComboBoxItem selected = (BrowserComboBoxItem) source.getSelectedItem();

			if ( !selected.getLocation().equals( "" ) )
				RequestFrame.this.refresh( KoLRequest.VISITOR.constructURLString( selected.getLocation() ) );

			source.setSelectedIndex( 0 );
		}
	}

	private class ExecuteScriptButton extends ThreadedButton
	{
		public ExecuteScriptButton()
		{	super( "exec" );
		}

		public void run()
		{
			String command = (String) RequestFrame.this.scriptSelect.getSelectedItem();
			if ( command == null )
				return;

			command = command.substring( command.indexOf( ":" ) + 1 ).trim();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( command );
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
		{	return this.name;
		}

		public String getLocation()
		{	return this.location;
		}
	}

	/**
	 * Returns whether or not this request frame has a side bar.
	 * This is used to ensure that bookmarks correctly use a
	 * new frame if this frame does not have one.
	 */

	public boolean hasSideBar()
	{	return true;
	}

	public String getCurrentLocation()
	{	return this.currentLocation;
	}

	/**
	 * Utility method which refreshes the current frame with
	 * data contained in the given request.  If the request
	 * has not yet been run, it will be run before the data
	 * is display in this frame.
	 */

	public void refresh( KoLRequest request )
	{
		if ( removedFrames.contains( this ) )
			removedFrames.remove( this );

		if ( !existingFrames.contains( this ) )
			existingFrames.add( this );

		this.displayRequest( request );

		if ( !this.isVisible() && StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( getFrameName() ) == -1 )
			this.setVisible( true );
	}

	public String getDisplayHTML( String responseText )
	{	return RequestEditorKit.getDisplayHTML( this.currentLocation, responseText );
	}

	/**
	 * Utility method which displays the given request.
	 */

	public void displayRequest( KoLRequest request )
	{
		if ( this.mainBuffer == null || request == null )
			return;

		if ( request instanceof FightRequest )
		{
			request = KoLRequest.VISITOR.constructURLString( request.getURLString() );
			request.responseText = FightRequest.lastResponseText;
		}

		this.currentLocation = request.getURLString();
		this.mainBuffer.clearBuffer();

		if ( request.responseText == null || request.responseText.length() == 0 )
		{
			// New prevention mechanism: tell the requests that there
			// will be no synchronization.

			String original = StaticEntity.getProperty( "showAllRequests" );
			StaticEntity.setProperty( "showAllRequests", "false" );

			RequestThread.postRequest( request );
			StaticEntity.setProperty( "showAllRequests", original );

			// If this resulted in a redirect, then update the display
			// to indicate that you were redirected and the display
			// cannot be shown in the minibrowser.

			if ( request.responseText == null || request.responseText.length() == 0 )
			{
				this.mainBuffer.append( "<b>Tried to access</b>: " + this.currentLocation );
				this.mainBuffer.append( "<br><b>Redirected</b>: " + request.redirectLocation );
				return;
			}
		}

		showHTML( this.currentLocation, request.responseText );

		if ( request.getClass() == KoLRequest.class )
			StaticEntity.externalUpdate( this.currentLocation, request.responseText );
	}

	public void showHTML( String location, String responseText )
	{
		// Function exactly like a history in a normal browser -
		// if you open a new frame after going back, all the ones
		// in the future get removed.

		String renderText = this.getDisplayHTML( responseText );

		this.history.add( location );
		this.shownHTML.add( renderText );

		if ( this.history.size() > HISTORY_LIMIT )
		{
			this.history.remove(0);
			this.shownHTML.remove(0);
		}

		location = location.substring( location.lastIndexOf( "/" ) + 1 );
		this.locationField.setText( location );

		this.locationIndex = this.shownHTML.size() - 1;

		Matcher imageMatcher = IMAGE_PATTERN.matcher( renderText );
		while ( imageMatcher.find() )
			RequestEditorKit.downloadImage( imageMatcher.group() );

		this.mainBuffer.append( renderText );
	}

	private class HomeButton extends ThreadedButton
	{
		public HomeButton()
		{	super( JComponentUtilities.getImage( "home.gif" ) );
		}

		public void run()
		{	RequestFrame.this.refresh( KoLRequest.VISITOR.constructURLString( "main.php" ) );
		}
	}

	private class BackButton extends ThreadedButton
	{
		public BackButton()
		{	super( JComponentUtilities.getImage( "back.gif" ) );
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

	private class ForwardButton extends ThreadedButton
	{
		public ForwardButton()
		{	super( JComponentUtilities.getImage( "forward.gif" ) );
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

	private class ReloadButton extends ThreadedButton
	{
		public ReloadButton()
		{	super( JComponentUtilities.getImage( "reload.gif" ) );
		}

		public void run()
		{
			if ( RequestFrame.this.currentLocation == null )
				return;

			RequestFrame.this.refresh( KoLRequest.VISITOR.constructURLString( RequestFrame.this.currentLocation ) );
		}
	}

	private class GoButton extends ThreadedButton
	{
		public GoButton()
		{
			super( "Go" );
			RequestFrame.this.locationField.addKeyListener( new GoAdapter() );
		}

		public void run()
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( RequestFrame.this.locationField.getText() );
			KoLRequest request = RequestEditorKit.extractRequest( adventure == null ? RequestFrame.this.locationField.getText() : adventure.getRequest().getURLString() );
			RequestFrame.this.refresh( request );
		}

		private class GoAdapter extends KeyAdapter
		{
			public void keyReleased( KeyEvent e )
			{
				if ( e.isConsumed() )
					return;

				if ( e.getKeyCode() != KeyEvent.VK_ENTER )
					return;

				GoButton.this.actionPerformed( null );
				e.consume();
			}
		}
	}

	public static final boolean sidebarFrameExists()
	{	return !sideBarFrames.isEmpty();
	}

	public static final void refreshStatus()
	{
		if ( sideBarFrames.isEmpty() )
			return;

		RequestFrame current;
		String displayHTML = RequestEditorKit.getDisplayHTML( "charpane.php", CharpaneRequest.getLastResponse() );

		for ( int i = 0; i < sideBarFrames.size(); ++i )
		{
			current = (RequestFrame) sideBarFrames.get(i);
			current.sideBuffer.clearBuffer();
			current.sideBuffer.append( displayHTML );
		}
	}

	public boolean containsText( String search )
	{	return mainBuffer.getBuffer().indexOf( search ) != -1;
	}

	public void dispose()
	{
		sideBarFrames.remove( this );

		this.history.clear();
		this.shownHTML.clear();
		super.dispose();
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}
}
