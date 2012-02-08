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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
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
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class RequestFrame
	extends GenericFrame
{
	private static final int HISTORY_LIMIT = 4;
	private static final Pattern IMAGE_PATTERN =
		Pattern.compile( "http://images\\.kingdomofloathing\\.com/[^\\s\"\'>]+" );
	private static final Pattern TOID_PATTERN = Pattern.compile( "toid=(\\d+)" );

	private static final Pattern BOOKSHELF_PATTERN =
		Pattern.compile( "onClick=\"location.href='(.*?)';\"", Pattern.DOTALL );

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

	/**
	 * Utility method which converts the given text into a form which can be displayed properly in a
	 * <code>RequestPane</code>. This method is necessary primarily due to the bad HTML which is used but can still
	 * be properly rendered by post-3.2 browsers.
	 */

	protected String getDisplayHTML( final String responseText )
	{
		return RequestFrame.getDisplayHTML( this.currentLocation, responseText, true );
	}

	private static String getDisplayHTML( final String location, final String responseText, boolean logIt )
	{
		if ( responseText == null || responseText.length() == 0 )
		{
			return "";
		}

		logIt &= RequestLogger.isDebugging();

		if ( logIt )
		{
			RequestLogger.updateDebugLog( "Rendering hypertext..." );
		}

		String displayHTML = RequestEditorKit.getFeatureRichHTML( location, responseText, false );

		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		displayHTML = KoLConstants.SCRIPT_PATTERN.matcher( displayHTML ).replaceAll( "" );
		displayHTML = KoLConstants.STYLE_PATTERN.matcher( displayHTML ).replaceAll( "" );
		displayHTML = KoLConstants.COMMENT_PATTERN.matcher( displayHTML ).replaceAll( "" );
		displayHTML = KoLConstants.LINE_BREAK_PATTERN.matcher( displayHTML ).replaceAll( "" );

		displayHTML = displayHTML.replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" );
		displayHTML = displayHTML.replaceAll( "<[Hh][Rr].*?>", "<br>" );

		// The default Java browser doesn't display blank lines correctly

		displayHTML = displayHTML.replaceAll( "<br><br>", "<br>&nbsp;<br>" );

		// Fix all the tables which decide to put a row end,
		// but no row beginning.

		displayHTML = displayHTML.replaceAll( "</tr><td", "</tr><tr><td" );
		displayHTML = displayHTML.replaceAll( "</tr><table", "</tr></table><table" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML = displayHTML.replaceAll( "font-size: .8em;", "" );
		displayHTML = displayHTML.replaceAll( "<font size=[12]>", "" );
		displayHTML = displayHTML.replaceAll( " class=small", "" );
		displayHTML = displayHTML.replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML =
			displayHTML.replaceAll(
				"<td valign=center><table[^>]*?><tr><td([^>]*?) bgcolor=black([^>]*?)>.*?</table></td>", "" );

		displayHTML = displayHTML.replaceAll( "<tr[^>]*?><td[^>]*bgcolor=\'?\"?black(.*?)</tr>", "" );
		displayHTML = displayHTML.replaceAll( "<table[^>]*title=.*?</table>", "" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)?(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\((?<!discardconf\\()[\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML =
			displayHTML.replaceAll(
				"<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>", "</style>" );

		// Image links are mangled a little bit because they use
		// Javascript now -- fix them.

		displayHTML =
			displayHTML.replaceAll(
				"<img([^>]*?) onClick=\'descitem\\((\\d+)\\);\'>",
				"<a href=\"desc_item.php?whichitem=$2\"><img$1 border=0></a>" );

		// The last thing to worry about is the problems in
		// specific pages.

		// The first of these is the familiar page, where the
		// first "Take this one with you" link does not work.

		displayHTML =
			displayHTML.replaceFirst( "<input class=button type=submit value=\"Take this one with you\">", "" );

		// The second of these is the betting page.  Here, the
		// problem is an "onClick" in the input field, if the
		// Hagnk option is available.

		if ( displayHTML.indexOf( "whichbet" ) != -1 )
		{
			// Since the introduction of MMG bots, bets are usually
			// placed and taken instantaneously.  Therefore, the
			// search form is extraneous.

			displayHTML = displayHTML.replaceAll( "<center><b>Search.*?<center>", "<center>" );

			// Also, placing a bet is awkward through the KoLmafia
			// interface.  Remove this capability.

			displayHTML = displayHTML.replaceAll( "<center><b>Add.*?</form><br>", "<br>" );

			// Checkboxes were a safety which were added server-side,
			// but they do not really help anything and Java is not
			// very good at rendering them -- remove it.

			displayHTML = displayHTML.replaceFirst( "\\(confirm\\)", "" );
			displayHTML =
				displayHTML.replaceAll(
					"<input type=checkbox name=confirm>", "<input type=hidden name=confirm value=on>" );

			// In order to avoid the problem of having two submits,
			// which confuses the built-in Java parser, remove one
			// of the buttons and leave the one that makes sense.

			if ( KoLCharacter.canInteract() )
			{
				displayHTML =
					displayHTML.replaceAll(
						"whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
						"whichbet value='$1'><input type=hidden name=from value=0><input class=button type=submit value=\"On Hand\"><input type=hidden" );
			}
			else
			{
				displayHTML =
					displayHTML.replaceAll(
						"whichbet value='(\\d+)'><input type=hidden name=from value=0>.*?</td><td><input type=hidden",
						"whichbet value='$1'><input type=hidden name=from value=1><input class=button type=submit value=\"In Hagnk's\"><input type=hidden" );
			}
		}

		// The third of these is the outfit managing page,
		// which requires that the form for the table be
		// on the outside of the table.

		if ( displayHTML.indexOf( "action=account_manageoutfits.php" ) != -1 )
		{
			// turn:  <center><table><form>...</center></td></tr></form></table>
			// into:  <form><center><table>...</td></tr></table></center></form>

			displayHTML = displayHTML.replaceAll( "<center>(<table[^>]*>)(<form[^>]*>)", "$2<center>$1" );
			displayHTML =
				displayHTML.replaceAll( "</center></td></tr></form></table>", "</td></tr></table></center></form>" );
		}

		// The fourth of these is the fight page, which is
		// totally mixed up -- in addition to basic modifications,
		// also resort the combat item list.

		if ( displayHTML.indexOf( "action=fight.php" ) != -1 )
		{
			displayHTML = displayHTML.replaceAll( "<form(.*?)<tr><td([^>]*)>", "<tr><td$2><form$1" );
			displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );

			// The following all appear when the WOWbar is active
			// and are useless without Javascript.
			displayHTML = displayHTML.replaceAll(  "<img.*?id='dragged'>", "" );
			displayHTML = displayHTML.replaceAll( "<div class=contextmenu.*?</div>", "");
			displayHTML = displayHTML.replaceAll( "<div id=topbar>?.*?</div>", "");
			displayHTML = displayHTML.replaceAll( "<div id='fightform' class='hideform'>.*?</div>(<p><center>You win the fight!)", "$1" );
		}

		// Doc Galaktik's page is going to get completely
		// killed, except for the main purchases.

		if ( displayHTML.indexOf( "action=galaktik.php" ) != -1 )
		{
			displayHTML =
				StringUtilities.globalStringReplace( displayHTML, "</tr><td valign=center>", "</tr><tr><td valign=center>" );
			displayHTML = StringUtilities.globalStringReplace( displayHTML, "<td>", "</td><td>" );
			displayHTML = StringUtilities.globalStringReplace( displayHTML, "</td></td>", "</td>" );

			displayHTML =
				displayHTML.replaceAll(
					"<table><table>(.*?)(<form action=galaktik\\.php method=post><input[^>]+><input[^>]+>)",
					"<table><tr><td>$2<table>$1<tr>" );
		}

		// The library bookshelf has some secretive Javascript
		// which needs to be removed.

		displayHTML = RequestFrame.BOOKSHELF_PATTERN.matcher( displayHTML ).replaceAll( "href=\"$1\"" );

		if ( logIt )
		{
			// Print it to the debug log for reference purposes.
			RequestLogger.updateDebugLog( displayHTML );
		}

		// All HTML is now properly rendered!  Return compiled string.

		return displayHTML;
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
			this.showHTML( request.responseText );
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
	 
			RequestFrame.this.showHTML( this.request.responseText );
		}
	}

	public void showHTML( String responseText )
	{
		// Function exactly like a history in a normal browser -
		// if you open a new frame after going back, all the ones
		// in the future get removed.

		responseText = this.getDisplayHTML( responseText );

		String location = this.currentLocation;
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
		String displayHTML = RequestFrame.getDisplayHTML( "charpane.php", CharPaneRequest.getLastResponse(), false );

		for ( int i = 0; i < RequestFrame.sideBarFrames.size(); ++i )
		{
			RequestFrame current = (RequestFrame) RequestFrame.sideBarFrames.get( i );

			if ( current.sideDisplay == null )
			{
				continue;
			}

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
