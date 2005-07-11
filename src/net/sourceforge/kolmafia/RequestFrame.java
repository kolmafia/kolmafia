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
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private static LockableListModel bookmarks = new LockableListModel();

	private JMenu bookmarkMenu;
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
		super( client, title );

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

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		if ( getCurrentLocation().startsWith( "desc" ) || getCurrentLocation().startsWith( "doc" ) || getCurrentLocation().startsWith( "search" ) )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			getContentPane().setLayout( new GridLayout( 1, 1 ) );
			getContentPane().add( mainScroller );
		}
		else
		{
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
		}

		addMenuBar();
		(new DisplayRequestThread()).start();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		// The function and goto menus are only available if there is a sidebar;
		// otherwise, adding them clutters the user interface.

		if ( this.sideBuffer != null )
		{
			JMenu functionMenu = new JMenu( "Function" );
			functionMenu.setMnemonic( KeyEvent.VK_F );

			functionMenu.add( new DisplayRequestMenuItem( "Inventory", "inventory.php" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Character", "charsheet.php" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Class Skills", "skills.php" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Read Messages", "messages.php" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Account Menu", "account.php" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Documentation", "doc.php?topic=home" ) );
			functionMenu.add( new DisplayPageMenuItem( "KoL Forums", KeyEvent.KEY_LOCATION_UNKNOWN, "http://forums.kingdomofloathing.com/" ) );
			functionMenu.add( new DisplayPageMenuItem( "Radio KoL", KeyEvent.KEY_LOCATION_UNKNOWN, "http://grace.fast-serv.com:9140/listen.pls" ) );
			functionMenu.add( new DisplayRequestMenuItem( "Report Bug", "sendmessage.php?toid=Jick" ) );
			functionMenu.add( new DisplayPageMenuItem( "Donate to KoL", KeyEvent.KEY_LOCATION_UNKNOWN, "http://www.kingdomofloathing.com/donatepopup.php?pid=" + (client == null ? 0 : client.getUserID()) ) );
			functionMenu.add( new DisplayRequestMenuItem( "Log Out", "logout.php" ) );

			menuBar.add( functionMenu );

			JMenu gotoMenu = new JMenu( "Goto (Maki)" );
			gotoMenu.setMnemonic( KeyEvent.VK_G );

			gotoMenu.add( new DisplayRequestMenuItem( "Main Map", "main.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Seaside Town", "town.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "The Mall", "mall.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Clan Hall", "clan_hall.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Campground", "campground.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Big Mountains", "mountains.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Nearby Plains", "plains.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Sorceress' Lair", "lair.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Desert Beach", "beach.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Distant Woods", "woods.php" ) );
			gotoMenu.add( new DisplayRequestMenuItem( "Mysterious Island", "island.php" ) );

			menuBar.add( gotoMenu );
		}

		// All frames get the benefit of the bookmarks menu bar, eventhough it
		// might be a little counterintuitive when viewing player profiles.

		compileBookmarks();
		menuBar.add( new BookmarkMenu() );
	}

	/**
	 * Utility method which returns the current URL being pointed
	 * to by this <code>RequestFrame</code>.
	 */

	public String getCurrentLocation()
	{	return currentRequest.getURLString();
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

		if ( parent == null || location.startsWith( "search" ) )
		{
			setTitle( "Mini-Browser Window" );
			currentRequest = request;
			(new DisplayRequestThread()).start();
		}
		else
			parent.refresh( request );
	}

	/**
	 * Utility method which refreshes the side pane.  This
	 * is used whenever something occurs in the main pane
	 * which is suspected to cause some display change here.
	 */

	private void refreshSidePane()
	{
		if ( sideBuffer != null )
		{
			sidePaneRequest.run();
			sideBuffer.clearBuffer();
			sideBuffer.append( getDisplayHTML( sidePaneRequest.responseText ) );
		}
	}

	/**
	 * Internal class which displays the given request inside
	 * of the current frame.
	 */

	private class DisplayRequestMenuItem extends JMenuItem implements ActionListener
	{
		private String location;

		public DisplayRequestMenuItem( String label, String location )
		{
			super( label );
			addActionListener( this );
			this.location = location;
		}

		public void actionPerformed( ActionEvent e )
		{
			currentRequest = new KoLRequest( client, location );
			(new DisplayRequestThread()).start();
		}
	}

	/**
	 * A special thread class which ensures that attempts to
	 * refresh the frame with data do not long the Swing thread.
	 */

	private class DisplayRequestThread extends DaemonThread
	{
		public void run()
		{
			if ( sidePaneRequest == null && sideBuffer != null )
			{
				sidePaneRequest = new KoLRequest( client, "charpane.php" );
				refreshSidePane();
			}

			if ( currentRequest == null )
				return;

			mainBuffer.clearBuffer();
			mainBuffer.append( "Retrieving..." );

			if ( currentRequest.responseText == null )
			{
				currentRequest.run();
				client.processResults( currentRequest.responseText );
			}

			// In the event that something resembling a gain event
			// is seen in the response text, or in the event that you
			// switch between compact and full mode, refresh the sidebar.

			if ( sidePaneRequest == null && sideBuffer != null )
			{
				sidePaneRequest = new KoLRequest( client, "charpane.php" );
				refreshSidePane();
			}
			else if ( currentRequest.responseText.indexOf( ">You " ) != -1 || getCurrentLocation().indexOf( "togglecompact" ) != -1 )
				refreshSidePane();

			mainBuffer.clearBuffer();
			mainBuffer.append( getDisplayHTML( currentRequest.responseText ) );
		}
	}

	/**
	 * Utility method which converts the given text into a form which
	 * can be displayed properly in a <code>JEditorPane</code>.  This
	 * method is necessary primarily due to the bad HTML which is used
	 * but can still be properly rendered by post-3.2 browsers.
	 */

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

		// For some reason, character entitites are not properly
		// handled by the mini browser.

		displayHTML = displayHTML.replaceAll( "&ntilde;", "ñ" ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&infin;", "**" );

		return displayHTML;
	}

	/**
	 * Utility method to compile the list of bookmarks based on the
	 * current server settings.
	 */

	private void compileBookmarks()
	{
		bookmarks.clear();

		String [] bookmarkData = client.getSettings().getProperty( "browserBookmarks" ).split( "\\|" );
		String name, location, pwdhash;

		if ( bookmarkData.length > 1 )
		{
			for ( int i = 0; i < bookmarkData.length; ++i )
			{
				name = bookmarkData[i];
				location = bookmarkData[++i];
				pwdhash = bookmarkData[++i];

				if ( pwdhash.equals( "true" ) )
					location += "&pwd=" + client.getPasswordHash();

				bookmarks.add( new DisplayRequestMenuItem( name, location ) );
			}
		}
	}

	/**
	 * A special class which renders the menu holding the list of bookmarks.
	 * This class also synchronizes with the list of available bookmarks.
	 */

	private class BookmarkMenu extends JMenu implements ListDataListener
	{
		private int HEADER_COUNT = 3;

		public BookmarkMenu()
		{
			super( "Bookmarks" );
			setMnemonic( KeyEvent.VK_B );

			// The bookmark menu is headed off with two items
			// which allow you to add to the menu and a separator
			// to separate the menu from the bookmarks.

			this.add( new AddBookmarkMenuItem() );
			this.add( new RemoveBookmarkMenuItem() );
			this.add( new JSeparator() );

			// Now, add everything that's contained inside of
			// the current list of bookmarks.

			for ( int i = 0; i < bookmarks.size(); ++i )
				this.add( (DisplayRequestMenuItem) bookmarks.get(i) );

			// Add this as a listener to the list of bookmarks
			// so that it gets updated whenever bookmarks are
			// updated in any way.

			bookmarks.addListDataListener( this );
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalAdded( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= source.size() || source.size() + HEADER_COUNT == getMenuComponentCount() )
				return;

			for ( int i = index0; i <= index1; ++i )
				add( (DisplayRequestMenuItem) source.get(i), i + HEADER_COUNT );

			validate();
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalRemoved( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 + HEADER_COUNT >= getMenuComponentCount() || source.size() + HEADER_COUNT == getMenuComponentCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				remove( i + HEADER_COUNT );

			validate();
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void contentsChanged( ListDataEvent e )
		{
		}

		/**
		 * An internal class which handles the addition of new
		 * bookmarks to the bookmark menu.
		 */

		private class AddBookmarkMenuItem extends JMenuItem implements ActionListener
		{
			public AddBookmarkMenuItem()
			{
				super( "Bookmark Page", KeyEvent.VK_A );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( client == null )
					return;

				String name = JOptionPane.showInputDialog( "Name your bookmark?" );

				if ( name == null )
					return;

				String location = getCurrentLocation();
				boolean pwdhash = location.indexOf( "pwd=" ) != -1;

				StringBuffer bookmarkData = new StringBuffer();
				bookmarkData.append( client.getSettings().getProperty( "browserBookmarks" ) );

				if ( bookmarkData.length() > 0 )
					bookmarkData.append( "|" );

				bookmarkData.append( name.replaceAll( "\\|", "" ) );
				bookmarkData.append( "|" );
				bookmarkData.append( location.replaceFirst( "pwd=" + client.getPasswordHash(), "" ).replaceFirst( "\\?&", "?" ).replaceFirst( "&&", "&" ) );
				bookmarkData.append( "|" );
				bookmarkData.append( pwdhash );

				client.getSettings().setProperty( "browserBookmarks", bookmarkData.toString() );
				client.getSettings().saveSettings();

				compileBookmarks();
			}
		}

		/**
		 * An internal class which handles the removal of old
		 * bookmarks from the bookmark menu.
		 */

		private class RemoveBookmarkMenuItem extends JMenuItem implements ActionListener
		{
			public RemoveBookmarkMenuItem()
			{
				super( "Unbookmark Page", KeyEvent.VK_U );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( client == null )
					return;

				String location = getCurrentLocation().replaceFirst( "pwd=" + client.getPasswordHash(), "" ).replaceFirst( "\\?&", "?" ).replaceFirst( "&&", "&" );
				String bookmarkData = client.getSettings().getProperty( "browserBookmarks" );

				client.getSettings().setProperty( "browserBookmarks", bookmarkData.replaceAll( "[^\\|]*?\\|" + location + "\\|(true|false)\\|?", "" ) );
				client.getSettings().saveSettings();

				compileBookmarks();
			}
		}
	}
}
