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

import java.awt.GridLayout;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private String title;
	protected JEditorPane display;
	private LimitedSizeChatBuffer buffer;

	public RequestFrame( KoLmafia client, String title, KoLRequest request )
	{
		super( title, client );
		this.title = title;

		this.display = new JEditorPane();
		this.display.setEditable( false );
		this.display.addHyperlinkListener( new KoLHyperlinkAdapter() );

		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );
		RequestEditorKit.setClient( client );
		RequestEditorKit.setRequestFrame( this );

		this.buffer = new LimitedSizeChatBuffer( title );
		this.buffer.setChatDisplay( display );

		JScrollPane scrollPane = new JScrollPane( display, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( scrollPane, 400, 300 );
		getContentPane().setLayout( new GridLayout( 1, 1 ) );
		getContentPane().add( scrollPane );
		addMenuBar();

		(new DisplayRequestThread( request )).start();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenu functionMenu = new JMenu( "Function" );
		functionMenu.setMnemonic( KeyEvent.VK_F );

		functionMenu.add( createMenuItem( "Inventory", "inventory.php" ) );
		functionMenu.add( createMenuItem( "Character", "charsheet.php" ) );
		functionMenu.add( createMenuItem( "Skills", "skills.php" ) );
		functionMenu.add( createMenuItem( "Read Messages", "messages.php" ) );
		functionMenu.add( createMenuItem( "Account Menu", "account.php" ) );
		functionMenu.add( createMenuItem( "Documentation", "doc.php?topic=home" ) );
		functionMenu.add( createMenuItem( "Forums", "http://forums.kingdomofloathing.com/" ) );
		functionMenu.add( createMenuItem( "Radio", "http://grace.fast-serv.com:9140/listen.pls" ) );
		functionMenu.add( createMenuItem( "Report Bug", "sendmessage.php?toid=Jick" ) );
		functionMenu.add( createMenuItem( "Donate", "donatepopup.php?pid=" + (client == null ? 0 : client.getUserID()) ) );
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

	public void refresh( KoLRequest request )
	{
		setTitle( "Mini-Browser Window" );
		(new DisplayRequestThread( request )).start();
	}

	private class DisplayRequestListener implements ActionListener
	{
		private String location;

		public DisplayRequestListener( String location )
		{	this.location = location;
		}

		public void actionPerformed( ActionEvent e )
		{	(new DisplayRequestThread( new KoLRequest( client, location ) )).start();
		}
	}

	private class DisplayRequestThread extends DaemonThread
	{
		private KoLRequest request;

		public DisplayRequestThread( KoLRequest request )
		{	this.request = request;
		}

		public void run()
		{
			if ( request == null )
				return;

			buffer.clearBuffer();
			buffer.append( "Retrieving..." );

			if ( request.responseText == null )
			{
				request.run();
				client.processResults( request.responseText );
			}

			// Remove all the <BR> tags that are not understood
			// by the default Java browser.

			String displayHTML = request.responseText.replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll( " class=small", "" );

			// This is to replace all the rows with a height of 1
			// with nothing to avoid weird rendering.

			displayHTML = displayHTML.replaceAll( "<tr><td height=1 bgcolor=black></td></tr>", "" );
			displayHTML = Pattern.compile( "<tr><td colspan=(\\d+) height=1 bgcolor=black></td></tr>" ).matcher( displayHTML ).replaceAll( "" );

                        // Kingdom of Loathing uses HTML in some of its maps
                        // that confuses the default browser. We can transform
                        // it to make it render correctly.
                        //
                        // Transform:
                        //     <form...><td...>...</td></form>
                        // into:
                        //     <td..><form...>...</form></td>

			displayHTML = Pattern.compile( "(<form[^>]*>)((<input[^>]*>)*)(<td[^>]*>)" ).matcher( displayHTML ).replaceAll( "$4$1$2" );
			displayHTML = Pattern.compile( "</td></form>" ).matcher( displayHTML ).replaceAll( "</form></td>" );

			buffer.clearBuffer();
			buffer.append( displayHTML );
		}
	}
}
