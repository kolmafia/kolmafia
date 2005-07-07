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
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JSplitPane;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private RequestFrame parent;
	private KoLRequest currentRequest;
	private LimitedSizeChatBuffer mainBuffer, sideBuffer;
	private KoLRequest sidePaneRequest;

	protected JEditorPane mainDisplay;

	public RequestFrame( KoLmafia client, String title, KoLRequest request )
	{	this( client, null, title, request );
	}

	public RequestFrame( KoLmafia client, RequestFrame parent, String title, KoLRequest request )
	{
		super( title, client );

		this.parent = parent;
		this.currentRequest = request;

		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );

		this.mainDisplay = new JEditorPane();
		this.mainDisplay.setEditable( false );

		if ( !(this instanceof PendingTradesFrame) )
			this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( title );
		this.mainBuffer.setChatDisplay( this.mainDisplay );

		JScrollPane mainScroller = new JScrollPane( this.mainDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );

		// Profile requests, trade requests, and game text descriptions,
		// as well as player searches, should not add extra requests to
		// the server by having a side panel - however, everything else
		// should need one.  Therefore, only add the side-bar when the
		// request frame is initialized as a mini-browser.

		if ( request instanceof ProfileRequest || request instanceof ProposeTradeRequest ||
			getCurrentLocation().startsWith( "desc" ) || getCurrentLocation().startsWith( "doc" ) || getCurrentLocation().startsWith( "search" ) )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			getContentPane().setLayout( new GridLayout( 1, 1 ) );
			getContentPane().add( mainScroller );
		}
		else
		{
			this.sidePaneRequest = new KoLRequest( client, "charpane.php" );

			JEditorPane sideDisplay = new JEditorPane();
			sideDisplay.setEditable( false );
			sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

			this.sideBuffer = new LimitedSizeChatBuffer( "" );
			this.sideBuffer.setChatDisplay( sideDisplay );

			JScrollPane sideScroller = new JScrollPane( sideDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
			JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

			JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
			splitPane.setOneTouchExpandable( true );
			JComponentUtilities.setComponentSize( splitPane, 600, 450 );

			getContentPane().setLayout( new GridLayout( 1, 1 ) );
			getContentPane().add( splitPane );
			addMenuBar();
		}

		refreshSidePane();
		(new DisplayRequestThread()).start();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu functionMenu = new JMenu( "Function" );
		functionMenu.setMnemonic( KeyEvent.VK_F );

		functionMenu.add( createMenuItem( "Inventory", "inventory.php" ) );
		functionMenu.add( createMenuItem( "Character", "charsheet.php" ) );
		functionMenu.add( createMenuItem( "Class Skills", "skills.php" ) );
		functionMenu.add( createMenuItem( "Read Messages", "messages.php" ) );
		functionMenu.add( createMenuItem( "Account Menu", "account.php" ) );
		functionMenu.add( createMenuItem( "Documentation", "doc.php?topic=home" ) );
		functionMenu.add( createMenuItem( "KoL Forums", "http://forums.kingdomofloathing.com/" ) );
		functionMenu.add( createMenuItem( "Radio KoL", "http://grace.fast-serv.com:9140/listen.pls" ) );
		functionMenu.add( createMenuItem( "Report Bug", "sendmessage.php?toid=Jick" ) );
		functionMenu.add( createMenuItem( "Donate to KoL", "donatepopup.php?pid=" + (client == null ? 0 : client.getUserID()) ) );
		functionMenu.add( createMenuItem( "Log Out", "logout.php" ) );

		menuBar.add( functionMenu );

		JMenu gotoMenu = new JMenu( "Goto (Maki)" );
		gotoMenu.setMnemonic( KeyEvent.VK_G );

		gotoMenu.add( createMenuItem( "Main Map", "main.php" ) );
		gotoMenu.add( createMenuItem( "Seaside Town", "town.php" ) );
		gotoMenu.add( createMenuItem( "The Mall", "mall.php" ) );
		gotoMenu.add( createMenuItem( "Clan Hall", "clan_hall.php" ) );
		gotoMenu.add( createMenuItem( "Campground", "campground.php" ) );
		gotoMenu.add( createMenuItem( "Big Mountains", "mountains.php" ) );
		gotoMenu.add( createMenuItem( "Nearby Plains", "plains.php" ) );
		gotoMenu.add( createMenuItem( "Desert Beach", "beach.php" ) );
		gotoMenu.add( createMenuItem( "Distant Woods", "woods.php" ) );
		gotoMenu.add( createMenuItem( "Mysterious Island", "island.php" ) );

		menuBar.add( gotoMenu );
	}

	private JMenuItem createMenuItem( String label, String location )
	{
		JMenuItem menuItem = new JMenuItem( label );
		menuItem.addActionListener( new DisplayRequestListener( location ) );
		return menuItem;
	}

	public String getCurrentLocation()
	{	return currentRequest.getURLString();
	}

	public void refresh( KoLRequest request )
	{
		String location = request.getURLString();

		if ( parent == null || location.startsWith( "search" ) )
		{
			setTitle( "Mini-Browser Window" );
			currentRequest = request;
			(new DisplayRequestThread()).start();
		}
		else
			parent.refresh( request );
	}

	public void refreshSidePane()
	{
		if ( sideBuffer != null )
			(new RefreshSidePaneThread()).start();
	}

	private class RefreshSidePaneThread extends DaemonThread
	{
		public void run()
		{
			sideBuffer.clearBuffer();
			sideBuffer.append( "Retrieving..." );

			sidePaneRequest.run();

			sideBuffer.clearBuffer();
			sideBuffer.append( getDisplayHTML( sidePaneRequest.responseText ) );
		}
	}

	private class DisplayRequestListener implements ActionListener
	{
		private String location;

		public DisplayRequestListener( String location )
		{	this.location = location;
		}

		public void actionPerformed( ActionEvent e )
		{
			currentRequest = new KoLRequest( client, location );
			(new DisplayRequestThread()).start();
		}
	}

	private class DisplayRequestThread extends DaemonThread
	{
		public void run()
		{
			if ( currentRequest == null )
				return;

			mainBuffer.clearBuffer();
			mainBuffer.append( "Retrieving..." );

			if ( currentRequest.responseText == null )
			{
				currentRequest.run();
				client.processResults( currentRequest.responseText );

				// In the event that something resembling a gain event
				// is seen in the response text, or in the event that you
				// switch between compact and full mode, refresh the sidebar.

				if ( currentRequest.responseText.indexOf( ">You " ) != -1 || getCurrentLocation().indexOf( "togglecompact" ) != -1 )
					refreshSidePane();
			}

			mainBuffer.clearBuffer();
			mainBuffer.append( getDisplayHTML( currentRequest.responseText ) );
		}
	}

	private String getDisplayHTML( String responseText )
	{
		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		String displayHTML = responseText.replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll( "<[Hh][Rr].*?>", "<br>" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML = displayHTML.replaceAll( "font-size: .8em;", "" ).replaceAll( "<font size=[12]>", "" ).replaceAll(
			" class=small", "" ).replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML = displayHTML.replaceAll( "<tr><td([^>]*?) bgcolor=black([^>]*?)>((</td>)?)</tr>", "<tr><td$1$2></td></tr>" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// turn:  <form...><tr...><td...>...</td></tr></form>
		// into:  <tr...><td...><form...>...</form></td></tr>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)<tr>(<td[^>]*>)", "<tr>$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\([\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML = displayHTML.replaceAll( "<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>" , "</style>" );

		return displayHTML;
	}
}
