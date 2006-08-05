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
import java.awt.Dimension;

// containers
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// utilities
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	private JList buffRequestList;
	private BuffOptionsComboBox buffOptions;
	private SortedListModel buffRequests = new SortedListModel();

	private JPanel philanthropyContainer = new JPanel();
	private CardLayout philanthropyCards = new CardLayout( 10, 10 );

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );

		tabs = new JTabbedPane();

		tabs.addTab( "Standard Buffs", new BuffRequestPanel() );
		Object [] list = BuffBotDatabase.getPhilanthropicBotList();

		JPanel philanthropyPanel = new JPanel( new BorderLayout() );
		philanthropyPanel.add( new PhilanthropyComboBox( list ), BorderLayout.NORTH );
		philanthropyContainer.setLayout( philanthropyCards );

		philanthropyContainer.add( new PhilanthropyPanel( null ), "" );
		for ( int i = 0; i < list.length; ++i )
			philanthropyContainer.add( new PhilanthropyPanel( (String) list[i] ), (String) list[i] );

		philanthropyCards.show( philanthropyContainer, "" );
		philanthropyPanel.add( philanthropyContainer, BorderLayout.CENTER );
		tabs.addTab( "Philanthropic Buffs", philanthropyPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
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

	private class BuffRequestPanel extends LabeledScrollPanel
	{
		public BuffRequestPanel()
		{
			super( "", "request", "online?", new JList( buffRequests ) );

			buffOptions = new BuffOptionsComboBox( BuffBotDatabase.getOfferingList() );

			actualPanel.add( buffOptions, BorderLayout.NORTH );
			buffRequestList = (JList) scrollComponent;
		}

		public void actionConfirmed()
		{
			Object [] values = buffRequestList.getSelectedValues();
			Runnable [] runnables = new Runnable[ values.length ];

			for ( int i = 0; i < values.length; ++i )
				runnables[i] = ((BuffBotDatabase.Offering)values[i]).toRequest();

			(new RequestThread( runnables )).start();
		}

		public void actionCancelled()
		{
			BuffBotDatabase.Offering selected = (BuffBotDatabase.Offering) buffRequestList.getSelectedValue();
			if ( selected != null )
				isBotOnline( selected.getBotName() );
		}
	}

	private class BuffOptionsComboBox extends JComboBox implements ActionListener
	{
		public BuffOptionsComboBox( Object [] data )
		{
			super( data );
			setSelectedItem( null );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			buffRequests.clear();
			buffRequests.addAll( BuffBotDatabase.getOfferings( (String) getSelectedItem() ) );
		}
	}

	private class PhilanthropyComboBox extends JComboBox implements ActionListener
	{
		public PhilanthropyComboBox( Object [] data )
		{
			super( data );
			setSelectedItem( null );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	philanthropyCards.show( philanthropyContainer, (String) getSelectedItem() );
		}
	}

	private class PhilanthropyPanel extends KoLPanel
	{
		private String botName;
		private JCheckBox [] checkboxes;
		private BuffBotDatabase.Offering [] offerings;

		public PhilanthropyPanel( String botName )
		{
			super( "request", "online?" );

			this.botName = botName;
			SortedListModel list = BuffBotDatabase.getPhilanthropicOfferings( botName );

			offerings = new BuffBotDatabase.Offering[ list.size() ];
			list.toArray( offerings );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

			checkboxes = new JCheckBox[ offerings.length ];
			for ( int i = 0; i < checkboxes.length; ++i )
			{
				checkboxes[i] = new JCheckBox( offerings[i].toString() );
				centerPanel.add( checkboxes[i] );
			}

			setContent( new VerifiableElement[0] );

			JScrollPane scroller = new JScrollPane( centerPanel,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			JComponentUtilities.setComponentSize( scroller, 400, 400 );
			container.add( scroller, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
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
	}
}
