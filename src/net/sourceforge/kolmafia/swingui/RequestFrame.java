/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.awt.Component;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.FileUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class RequestFrame
	extends GenericFrame
{
	private static final int HISTORY_LIMIT = 4;
	private static final Pattern IMAGE_PATTERN =
		Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\"\'>]+" );
	private static final Pattern TOID_PATTERN = Pattern.compile( "toid=(\\d+)" );

	private static final ArrayList sideBarFrames = new ArrayList();

	private int locationIndex = 0;
	private final ArrayList history = new ArrayList();
	private final ArrayList shownHTML = new ArrayList();

	private String currentLocation;

	public RequestPane sideDisplay;
	public RequestPane mainDisplay;

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

		JScrollPane mainScroller =
			new JScrollPane(
				this.mainDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		this.constructSideBar( mainScroller );
		this.getToolbar();
	}

	public boolean showInWindowMenu()
	{
		return false;
	}

	private void constructSideBar( final JScrollPane mainScroller )
	{
		if ( !this.hasSideBar() )
		{
			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			this.setCenterComponent( mainScroller );
			return;
		}

		RequestFrame.sideBarFrames.add( this );

		this.sideDisplay = new RequestPane();
		this.sideDisplay.addHyperlinkListener( new RequestHyperlinkAdapter() );

		JScrollPane sideScroller =
			new JScrollPane(
				this.sideDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

		JSplitPane horizontalSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
		horizontalSplit.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( horizontalSplit, 600, 450 );

		this.setCenterComponent( horizontalSplit );
		RequestFrame.refreshStatus();
	}

	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar( true );

		// Add toolbar pieces so that people can quickly
		// go to locations they like.

		toolbarPanel.add( new ThreadedButton( JComponentUtilities.getImage( "back.gif" ), new BackRunnable() ) );
		toolbarPanel.add( new ThreadedButton( JComponentUtilities.getImage( "forward.gif" ), new ForwardRunnable() ) );
		toolbarPanel.add( new ThreadedButton( JComponentUtilities.getImage( "home.gif" ), new HomeRunnable() ) );
		toolbarPanel.add( new ThreadedButton( JComponentUtilities.getImage( "reload.gif" ), new ReloadRunnable() ) );

		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( this.locationField );
		toolbarPanel.add( new JToolBar.Separator() );

		ThreadedButton goButton = new ThreadedButton( "Go", new GoRunnable() );
		this.locationField.addKeyListener( goButton );

		toolbarPanel.add( goButton );

		return toolbarPanel;
	}

	public Component getCenterComponent()
	{
		return this.getFramePanel();
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
		if ( !this.isVisible() && !this.appearsInTab() )
		{
			this.setVisible( true );
		}

		this.displayRequest( request );
	}

	public String getDisplayHTML( final String responseText )
	{
		return RequestEditorKit.getDisplayHTML( this.currentLocation, responseText, true );
	}

	/**
	 * Utility method which displays the given request.
	 */

	public void displayRequest( GenericRequest request )
	{
		if ( this.mainDisplay == null || request == null )
		{
			return;
		}

		if ( request instanceof FightRequest )
		{
			request = new GenericRequest( request.getURLString() );
			request.responseText = FightRequest.lastResponseText;
		}

		this.currentLocation = request.getURLString();

		if ( request.responseText == null || request.responseText.length() == 0 )
		{
			RequestThread.runInParallel( new DisplayRequestRunnable( request ) );
		}
		else
		{
			this.showHTML( this.currentLocation, request.responseText );
		}
	}

	private class DisplayRequestRunnable
		implements Runnable
	{
		private GenericRequest request;

		public DisplayRequestRunnable( GenericRequest request )
		{
			this.request = request;
		}

		public void run()
		{
			// New prevention mechanism: tell the requests that there
			// will be no synchronization.

			boolean original = Preferences.getBoolean( "showAllRequests" );
			Preferences.setBoolean( "showAllRequests", false );
			this.request.run();
			Preferences.setBoolean( "showAllRequests", original );

			// If this resulted in a redirect, then update the display
			// to indicate that you were redirected and the display
			// cannot be shown in the minibrowser.

			if ( this.request.responseText == null || this.request.responseText.length() == 0 )
			{
				RequestFrame.this.mainDisplay.setText( "" );
				return;
			}
	 
			RequestFrame.this.showHTML( RequestFrame.this.currentLocation, this.request.responseText );
		}
	}

	public void showHTML( String location, String responseText )
	{
		// Function exactly like a history in a normal browser -
		// if you open a new frame after going back, all the ones
		// in the future get removed.

		responseText = this.getDisplayHTML( responseText );

		this.history.add( location );
		this.shownHTML.add( responseText );

		if ( this.history.size() > RequestFrame.HISTORY_LIMIT )
		{
			this.history.remove( 0 );
			this.shownHTML.remove( 0 );
		}

		location = location.substring( location.lastIndexOf( "/" ) + 1 );
		this.locationField.setText( location );

		this.locationIndex = RequestFrame.this.shownHTML.size() - 1;

		Matcher imageMatcher = RequestFrame.IMAGE_PATTERN.matcher( responseText );
		while ( imageMatcher.find() )
		{
			FileUtilities.downloadImage( imageMatcher.group() );
		}

		this.mainDisplay.setText( responseText );
	}

	public static final void refreshStatus()
	{
		RequestFrame current;
		String displayHTML = RequestEditorKit.getDisplayHTML( "charpane.php", CharPaneRequest.getLastResponse(), false );

		for ( int i = 0; i < RequestFrame.sideBarFrames.size(); ++i )
		{
			current = (RequestFrame) RequestFrame.sideBarFrames.get( i );

			if ( current.sideDisplay != null )
			{
				try
				{
					current.sideDisplay.setText( displayHTML );
				}
				catch ( Exception e )
				{
					// This should not happen. Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
				}
			}
		}
	}

	public boolean containsText( final String search )
	{
		return this.mainDisplay.getText().indexOf( search ) != -1;
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

	private class HomeRunnable
		implements Runnable
	{
		public void run()
		{
			RequestFrame.this.refresh( new GenericRequest( "main.php" ) );
		}
	}

	private class BackRunnable
		implements Runnable
	{
		public void run()
		{
			if ( RequestFrame.this.locationIndex > 0 )
			{
				--RequestFrame.this.locationIndex;
				RequestFrame.this.mainDisplay.setText( (String) RequestFrame.this.shownHTML.get( RequestFrame.this.locationIndex ) );
				RequestFrame.this.locationField.setText( (String) RequestFrame.this.history.get( RequestFrame.this.locationIndex ) );
			}
		}
	}

	private class ForwardRunnable
		implements Runnable
	{
		public void run()
		{
			if ( RequestFrame.this.locationIndex + 1 < RequestFrame.this.shownHTML.size() )
			{
				++RequestFrame.this.locationIndex;
				RequestFrame.this.mainDisplay.setText( (String) RequestFrame.this.shownHTML.get( RequestFrame.this.locationIndex ) );
				RequestFrame.this.locationField.setText( (String) RequestFrame.this.history.get( RequestFrame.this.locationIndex ) );
			}
		}
	}

	private class ReloadRunnable
		implements Runnable
	{
		public void run()
		{
			if ( RequestFrame.this.currentLocation == null )
			{
				return;
			}

			RequestFrame.this.refresh( new GenericRequest( RequestFrame.this.currentLocation ) );
		}
	}

	private class GoRunnable
		implements Runnable
	{
		public void run()
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( RequestFrame.this.locationField.getText() );
			GenericRequest request =
				RequestEditorKit.extractRequest( adventure == null ? RequestFrame.this.locationField.getText() : adventure.getRequest().getURLString() );
			RequestFrame.this.refresh( request );
		}
	}

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
				RelayLoader.openSystemBrowser( location );
			}
			else if ( location.startsWith( "sendmessage.php" ) || location.startsWith( "town_sendgift.php" ) )
			{
				// Attempts to send a message should open up
				// KoLmafia's built-in message sender.

				Matcher idMatcher = RequestFrame.TOID_PATTERN.matcher( location );

				String[] parameters = new String[]
				{ idMatcher.find() ? idMatcher.group( 1 ) : ""
				};
				GenericFrame.createDisplay( SendMessageFrame.class, parameters );
			}
			else if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
			{
				DescriptionFrame.showLocation( location );
				return;
			}
			else
			{
				RequestFrame.this.gotoLink( location );
			}
		}
	}

	public void gotoLink( final String location )
	{
		this.refresh( RequestEditorKit.extractRequest( location ) );
	}
}
