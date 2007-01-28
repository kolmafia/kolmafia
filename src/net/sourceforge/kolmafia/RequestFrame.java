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
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import javax.swing.event.HyperlinkListener;
import javax.swing.JToolBar.Separator;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\"\'>]+" );
	private static boolean refreshStatusEnabled = true;

	private int locationIndex = 0;
	private boolean isLoading = true;
	private ArrayList history = new ArrayList();
	private ArrayList shownHTML = new ArrayList();

	private RequestFrame parent;
	private String currentLocation;
	private KoLRequest currentRequest;

	private LimitedSizeChatBuffer mainBuffer;
	private LimitedSizeChatBuffer sideBuffer;

	public RequestPane sideDisplay;
	public RequestPane mainDisplay;

	private JComboBox scriptSelect;
	private BrowserComboBox functionSelect, gotoSelect;
	private JTextField locationField = new JTextField();

	public RequestFrame()
	{	this( "Mini-Browser", new KoLRequest( "main.php", true ) );
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

		this.isLoading = true;
		this.parent = parent;

		setCurrentRequest( request );
		this.mainDisplay = new RequestPane();

		if ( !(this instanceof PendingTradesFrame) )
			this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( false );
		JScrollPane mainScroller = this.mainBuffer.setChatDisplay( this.mainDisplay );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		if ( !hasSideBar() )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			framePanel.setLayout( new BorderLayout() );
			framePanel.add( mainScroller, BorderLayout.CENTER );
		}
		else
		{
			this.sideDisplay = new RequestPane();
			this.sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

			this.sideBuffer = new LimitedSizeChatBuffer( false );
			JScrollPane sideScroller = this.sideBuffer.setChatDisplay( sideDisplay );
			JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

			JSplitPane horizontalSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
			horizontalSplit.setOneTouchExpandable( true );
			JComponentUtilities.setComponentSize( horizontalSplit, 600, 450 );

			// Add the standard locations handled within the
			// mini-browser, including inventory, character
			// information, skills and account setup.

			functionSelect = new BrowserComboBox();
			functionSelect.addItem( new BrowserComboBoxItem( "- Select -", "" ) );

			for ( int i = 0; i < FUNCTION_MENU.length; ++i )
				functionSelect.addItem( new BrowserComboBoxItem( FUNCTION_MENU[i][0], FUNCTION_MENU[i][1] ) );

			// Add the browser "goto" menu, because people
			// are familiar with seeing this as well.  But,
			// place it all inside of a "travel" menu.

			gotoSelect = new BrowserComboBox();
			gotoSelect.addItem( new BrowserComboBoxItem( "- Select -", "" ) );
			for ( int i = 0; i < GOTO_MENU.length; ++i )
				gotoSelect.addItem( new BrowserComboBoxItem( GOTO_MENU[i][0], GOTO_MENU[i][1] ) );

			JPanel topMenu = new JPanel();
			topMenu.setOpaque( true );
			topMenu.setBackground( Color.white );

			topMenu.add( new JLabel( "Function:" ) );
			topMenu.add( functionSelect );
			topMenu.add( new JLabel( "Go To:" ) );
			topMenu.add( gotoSelect );
			topMenu.add( Box.createHorizontalStrut( 20 ) );

			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" );
			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" );

			topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" ) ) );
			topMenu.add( new JLabel( JComponentUtilities.getImage( "itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" ) ) );

			topMenu.add( Box.createHorizontalStrut( 20 ) );

			scriptSelect = new JComboBox();
			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );
			for ( int i = 0; i < scriptList.length; ++i )
				scriptSelect.addItem( (i+1) + ": " + scriptList[i] );

			topMenu.add( scriptSelect );
			topMenu.add( new ExecuteScriptButton() );

			functionSelect.setSelectedIndex( 0 );
			gotoSelect.setSelectedIndex( 0 );

			JPanel container = new JPanel( new BorderLayout() );
			container.add( topMenu, BorderLayout.NORTH );
			container.add( horizontalSplit, BorderLayout.CENTER );

			framePanel.setLayout( new BorderLayout() );
			framePanel.add( container, BorderLayout.CENTER );
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

		if ( hasSideBar() )
			refreshStatus();

		displayRequest( request );
		this.isLoading = false;
	}

	private class BrowserComboBox extends JComboBox
	{
		public BrowserComboBox()
		{	addActionListener( new BrowserComboBoxListener() );
		}
	}

	private class BrowserComboBoxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			BrowserComboBox source = (BrowserComboBox) e.getSource();
			BrowserComboBoxItem selected = (BrowserComboBoxItem) source.getSelectedItem();

			if ( !selected.getLocation().equals( "" ) )
				refresh( new KoLRequest( selected.getLocation() ) );

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
			String command = (String) scriptSelect.getSelectedItem();
			if ( command == null )
				return;

			command = command.substring( command.indexOf( ":" ) + 1 ).trim();
			DEFAULT_SHELL.executeLine( command );
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
	{
		String location = currentRequest == null ? "" : currentRequest.getURLString();

		return currentRequest != null && !location.startsWith( "chat" ) && !location.startsWith( "static" ) &&
			!location.startsWith( "desc" ) && !location.startsWith( "showplayer" ) && !location.startsWith( "doc" ) &&
			!location.startsWith( "searchp" ) && location.indexOf( "action=message" ) == -1;
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
	{
		String location = request.getURLString();

		if ( parent == null || location.startsWith( "search" ) || location.startsWith( "desc" ) )
		{
			setCurrentRequest( request );
			displayRequest( request );
		}
		else
			parent.refresh( request );
	}

	private void setCurrentRequest( KoLRequest request )
	{	this.currentRequest = request;
	}

	public String getDisplayHTML( String responseText )
	{	return RequestEditorKit.getDisplayHTML( currentRequest.getURLString(), responseText );
	}

	private class DisplayRequestThread extends Thread
	{
		private KoLRequest request;

		public DisplayRequestThread( KoLRequest request )
		{	this.request = request;
		}

		public void run()
		{	dispatchRequest( this.request );
		}
	}

	/**
	 * Utility method which displays the given request.
	 */

	public void displayRequest( KoLRequest request )
	{
		if ( mainBuffer == null || request == null )
			return;

		if ( SwingUtilities.isEventDispatchThread() )
			(new DisplayRequestThread( request )).start();
		else
			dispatchRequest( request );
	}

	private void dispatchRequest( KoLRequest request )
	{
		currentLocation = request.getURLString();
		if ( request instanceof FightRequest )
		{
			request = new KoLRequest( currentLocation );
			request.responseText = FightRequest.INSTANCE.responseText;
		}

		mainBuffer.clearBuffer();
		mainBuffer.append( "Retrieving page..." );

		if ( request.responseText == null || request.responseText.length() == 0 )
		{
			// New prevention mechanism: tell the requests that there
			// will be no synchronization.

			String original = StaticEntity.getProperty( "showAllRequests" );
			StaticEntity.setProperty( "showAllRequests", "false" );

			request.run();
			StaticEntity.setProperty( "showAllRequests", original );
		}

		if ( request != null && request.responseText != null && request.responseText.length() != 0 )
		{
			// Function exactly like a history in a normal browser -
			// if you open a new frame after going back, all the ones
			// in the future get removed.

			String renderText = getDisplayHTML( request.responseText );

			history.add( request.getURLString() );
			shownHTML.add( renderText );

			if ( history.size() > 10 )
			{
				history.remove(0);
				shownHTML.remove(0);
			}

			String location = request.getURLString();

			location = location.substring( location.lastIndexOf( "/" ) + 1 );
			locationField.setText( location );

			locationIndex = shownHTML.size() - 1;

			Matcher imageMatcher = IMAGE_PATTERN.matcher( renderText );
			while ( imageMatcher.find() )
				RequestEditorKit.downloadImage( imageMatcher.group() );

			mainBuffer.clearBuffer();
			mainBuffer.append( renderText );
		}
		else
		{
			// If this resulted in a redirect, then update the display
			// to indicate that you were redirected and the display
			// cannot be shown in the minibrowser.

			mainBuffer.append( "<b>Tried to access</b>: " + currentLocation );
			mainBuffer.append( "<br><b>Redirected</b>: " + request.redirectLocation );
			return;
		}

		if ( request.getClass() == KoLRequest.class )
			StaticEntity.externalUpdate( request.getURLString(), request.responseText );
	}

	private class HomeButton extends ThreadedButton
	{
		public HomeButton()
		{	super( JComponentUtilities.getImage( "home.gif" ) );
		}

		public void run()
		{	refresh( new KoLRequest( "main.php", true ) );
		}
	}

	private class BackButton extends ThreadedButton
	{
		public BackButton()
		{	super( JComponentUtilities.getImage( "back.gif" ) );
		}

		public void run()
		{
			if ( locationIndex > 0 )
			{
				--locationIndex;
				mainBuffer.clearBuffer();
				mainBuffer.append( (String) shownHTML.get( locationIndex ) );
				locationField.setText( (String) history.get( locationIndex ) );
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
			if ( locationIndex + 1 < shownHTML.size() )
			{
				++locationIndex;
				mainBuffer.clearBuffer();
				mainBuffer.append( (String) shownHTML.get( locationIndex ) );
				locationField.setText( (String) history.get( locationIndex ) );
			}
		}
	}

	private class ReloadButton extends ThreadedButton
	{
		public ReloadButton()
		{	super( JComponentUtilities.getImage( "reload.gif" ) );
		}

		public void run()
		{	refresh( new KoLRequest( currentLocation, true ) );
		}
	}

	private class GoButton extends ThreadedButton
	{
		public GoButton()
		{
			super( "Go" );
			locationField.addKeyListener( new GoAdapter() );
		}

		public void run()
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( locationField.getText() );
			KoLRequest request = RequestEditorKit.extractRequest( adventure == null ? locationField.getText() : adventure.getRequest().getURLString() );
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

	public static void refreshStatus()
	{
		if ( !refreshStatusEnabled )
			return;

		RequestThread.postRequest( CharpaneRequest.getInstance() );
		refreshStatus( CharpaneRequest.getInstance().responseText );
	}

	private static void refreshStatus( String responseText )
	{
		if ( responseText == null )
			return;

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		if ( frames == null )
			return;

		for ( int i = 0; i < frames.length; ++i )
		{
			if ( frames[i] == null || !(frames[i] instanceof RequestFrame) || ((RequestFrame)frames[i]).sideBuffer == null )
				continue;

			((RequestFrame)frames[i]).sideBuffer.clearBuffer();
			((RequestFrame)frames[i]).sideBuffer.append( RequestEditorKit.getDisplayHTML( "charpane.php", responseText ) );
		}
	}

	public static boolean willRefreshStatus()
	{
		if ( !refreshStatusEnabled )
			return false;

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		if ( frames == null )
			return false;

		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] != null && frames[i] instanceof RequestFrame && ((RequestFrame)frames[i]).sideBuffer != null )
				return true;

		return false;
	}

	public static boolean isRefreshStatusEnabled()
	{	return refreshStatusEnabled;
	}

	public static void setRefreshStatusEnabled( boolean isEnabled )
	{
		refreshStatusEnabled = isEnabled;

		if ( isEnabled )
			refreshStatus();
		else
			refreshStatus( "" );
	}

	public void dispose()
	{
		history.clear();
		shownHTML.clear();
		super.dispose();
	}
}
