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

// layout
import java.awt.BorderLayout;
import java.awt.CardLayout;

// containers
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// utilities
import java.util.TreeMap;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );

		tabs = new JTabbedPane();

		addTab( "Standard Buffs", false );
		addTab( "Philanthropic Buffs", true );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	public void addTab( String tabName, boolean isFree )
	{
		tabs.addTab( tabName, new BuffRequestPanel( isFree ) );

	}

	private void isBotOnline( String botName )
	{
		KoLRequest request = new KoLRequest( StaticEntity.getClient(), "submitnewchat.php" );

		request.addFormField( "playerid", String.valueOf( KoLCharacter.getUserID() ) );
		request.addFormField( "pwd" );
		request.addFormField( "graf", "/whois " + botName );
		request.run();

		if ( request.responseText != null && request.responseText.indexOf( "online" ) != -1 )
			JOptionPane.showMessageDialog( null, botName + " is online." );
		else
			JOptionPane.showMessageDialog( null, botName + " is probably not online." );
	}

	private class BuffRequestPanel extends KoLPanel
	{
		private String botName;
		private TreeMap panelMap;
		private JPanel requestContainer;
		private CardLayout requestCards;

		public BuffRequestPanel( boolean isFree )
		{
			super( "request", "online?" );

			Object [] list = isFree ? BuffBotDatabase.getPhilanthropicBotList() :
				BuffBotDatabase.getStandardBotList();

			panelMap = new TreeMap();
			requestCards = new CardLayout( 10, 10 );
			requestContainer = new JPanel( requestCards );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Bot Name: ", new NameComboBox( list ) );

			setContent( elements );

			requestContainer.add( new RequestPanel( isFree, null ), "" );
			for ( int i = 0; i < list.length; ++i )
			{
				RequestPanel panel = new RequestPanel( isFree, (String) list[i] );

				panelMap.put( (String) list[i], panel );
				requestContainer.add( panel, (String) list[i] );
			}

			requestCards.show( requestContainer, "" );
			southContainer.add( requestContainer, BorderLayout.CENTER );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return true;
		}

		public void actionConfirmed()
		{
			RequestPanel panel = (RequestPanel) panelMap.get( botName );

			JCheckBox [] checkboxes = panel.checkboxes;
			BuffBotDatabase.Offering [] offerings = panel.offerings;

			ArrayList requests = new ArrayList();
			for ( int i = 0; i < checkboxes.length; ++i )
				if ( checkboxes[i].isSelected() )
					requests.add( offerings[i].toRequest() );

			if ( requests.isEmpty() )
				return;

			Runnable [] runnables = new Runnable[ requests.size() ];
			requests.toArray( runnables );

			(new RequestThread( runnables )).start();
		}

		public void actionCancelled()
		{	isBotOnline( botName );
		}

		private class NameComboBox extends JComboBox implements ActionListener
		{
			public NameComboBox( Object [] data )
			{
				super( data );
				setSelectedItem( null );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				botName = (String) getSelectedItem();
				requestCards.show( requestContainer, botName );
			}
		}

		private class RequestPanel extends JPanel
		{
			private String botName;
			private JCheckBox [] checkboxes;
			private BuffBotDatabase.Offering [] offerings;

			public RequestPanel( boolean isFree, String botName )
			{
				this.botName = botName;
				SortedListModel list = isFree ? BuffBotDatabase.getPhilanthropicOfferings( botName ) :
					BuffBotDatabase.getStandardOfferings( botName );

				offerings = new BuffBotDatabase.Offering[ list.size() ];
				list.toArray( offerings );

				JPanel centerPanel = new JPanel();
				centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

				checkboxes = new JCheckBox[ offerings.length ];
				TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

				for ( int i = 0; i < checkboxes.length; ++i )
				{
					checkboxes[i] = new JCheckBox( offerings[i].toString() );
					checkboxes[i].setVerticalTextPosition( JCheckBox.TOP );
					checkboxes[i].addActionListener( priceUpdater );
					centerPanel.add( checkboxes[i] );
				}

				JScrollPane scroller = new JScrollPane( centerPanel,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

				JComponentUtilities.setComponentSize( scroller, 300, 400 );

				setLayout( new BorderLayout() );
				add( scroller, BorderLayout.CENTER );
			}
		}

		private class TotalPriceUpdater implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				int price = 0;
				RequestPanel panel = (RequestPanel) panelMap.get( botName );

				JCheckBox [] checkboxes = panel.checkboxes;
				BuffBotDatabase.Offering [] offerings = panel.offerings;

				for ( int i = 0; i < checkboxes.length; ++i )
					if ( checkboxes[i].isSelected() )
						price += offerings[i].getPrice();

				setStatusMessage( ENABLE_STATE, COMMA_FORMAT.format( price ) + " meat will be sent to " + botName );
			}
		}
	}
}
