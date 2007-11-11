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
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.BuffBotManager.Offering;

public class BuffRequestFrame extends KoLFrame
{
	public static final KoLRequest ONLINE_VALIDATOR = new KoLRequest( "submitnewchat.php" );

	private static final String NO_REQUEST_TEXT = "\nTo whom it may concern:\n\n" +
		"At the frequent request of individuals wanting to see the name 'BOT_NAME' listed in KoLmafia's buff purchase interface, " +
		"BOT_NAME has been added to our internal buffbot database.\n\n" +
		"However, at the request of the individuals responsible for maintaining BOT_NAME, " +
		"BOT_NAME's formal price list and buff offerings are not available directly through KoLmafia.\n\n" +
		"You are welcome to use this interface to check whether or not BOT_NAME is currently logged in to KoL.  " +
		"However, we hope this message helps you understand why additional support was not added.\n\n\n" +
		"Respectfully yours,\nThe KoLmafia development team";

	private String botName;
	private JComboBox names, types;
	private SortedListModel [] nameList;

	private TreeMap panelMap;

	private JPanel nameContainer;
	private CardLayout nameCards;

	private RequestPanel lastPanel;
	private BuffRequestPanel mainPanel;
	private TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );
		this.setDefaultCloseOperation( HIDE_ON_CLOSE );

		this.panelMap = new TreeMap();
		this.nameList = new SortedListModel[4];
		for ( int i = 0; i < 4; ++i )
			nameList[i] = new SortedListModel();

		this.names = new JComboBox( nameList[0] );

		this.types = new JComboBox();
		this.types.addItem( "buff packs" );
		this.types.addItem( "sauceror buffs" );
		this.types.addItem( "turtle tamer buffs" );
		this.types.addItem( "accordion thief buffs" );

		CardSwitchListener listener = new CardSwitchListener();

		addActionListener( this.names, listener );
		addActionListener( this.types, listener );

		this.nameCards = new CardLayout();
		this.nameContainer = new JPanel( nameCards );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.mainPanel = new BuffRequestPanel(), "" );

		this.nameContainer.add( new SimpleScrollPane( new JPanel() ), "" );

		int lastSelectedIndex = KoLSettings.getIntegerProperty( "lastBuffRequestType" );
		if ( lastSelectedIndex >= 0 && lastSelectedIndex < 4 )
			this.types.setSelectedIndex( lastSelectedIndex );

		this.resetCard();
	}

	public UnfocusedTabbedPane getTabbedPane()
	{	return null;
	}

	public void dispose()
	{
		KoLSettings.setUserProperty( "lastBuffRequestType", String.valueOf( types.getSelectedIndex() ) );

		this.panelMap.clear();
		this.mainPanel.dispose();

		super.dispose();
	}

	private void isBotOnline( String botName )
	{
		ONLINE_VALIDATOR.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );
		ONLINE_VALIDATOR.addFormField( "pwd" );
		ONLINE_VALIDATOR.addFormField( "graf", "/whois " + botName );

		RequestThread.postRequest( ONLINE_VALIDATOR );

		if ( ONLINE_VALIDATOR.responseText != null && ONLINE_VALIDATOR.responseText.indexOf( "online" ) != -1 )
			alert( botName + " is online." );
		else
			alert( botName + " is probably not online." );
	}

	private class BuffRequestPanel extends KoLPanel
	{
		public BuffRequestPanel()
		{
			super( "request", "online?" );

			Object [] list = BuffBotDatabase.getCompleteBotList();

			for ( int i = 0; i < list.length; ++i )
			{
				if ( list[i] == null || list[i].equals( "" ) )
					continue;

				RequestPanel panel = new RequestPanel( (String) list[i] );
				BuffRequestFrame.this.panelMap.put( list[i], panel );
				BuffRequestFrame.this.nameContainer.add( panel, list[i] );
			}

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Category:  ", BuffRequestFrame.this.types );
			elements[1] = new VerifiableElement( "Bot Name:  ", BuffRequestFrame.this.names );

			this.setContent( elements );
			this.add( BuffRequestFrame.this.nameContainer, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			RequestPanel panel = getPanel();

			if ( panel == null )
				return;

			JCheckBox [] checkboxes = panel.checkboxes;
			Offering [] offerings = panel.offerings;

			ArrayList requests = new ArrayList();
			for ( int i = 0; i < checkboxes.length; ++i )
			{
				if ( checkboxes[i] != null && checkboxes[i].isSelected() )
				{
					checkboxes[i].setSelected( false );
					requests.add( offerings[i].toRequest() );
				}
			}

			if ( requests.isEmpty() )
				return;

			RequestThread.openRequestSequence();

			for ( int i = 0; i < requests.size(); ++i )
			{
				KoLmafia.updateDisplay( "Submitting buff request " + (i+1) + " of " + requests.size() + " to " + BuffRequestFrame.this.botName + "..." );
				RequestThread.postRequest( (GreenMessageRequest) requests.get(i) );
			}

			KoLmafia.updateDisplay( "Buff requests complete." );
			RequestThread.closeRequestSequence();
		}

		public boolean shouldAddStatusLabel()
		{	return true;
		}

		public void actionCancelled()
		{	BuffRequestFrame.this.isBotOnline( BuffRequestFrame.this.botName );
		}
	}

	private class RequestPanel extends JPanel
	{
		private int lastBuffId = 0;
		private boolean addedBuffPackLabel = false;
		private CardLayout categoryCards = new CardLayout();
		private JPanel [] categoryPanels = new JPanel[4];

		private JCheckBox [] checkboxes;
		private Offering [] offerings;

		public RequestPanel( String botName )
		{
			this.setLayout( this.categoryCards );

			if ( BuffBotDatabase.getStandardOfferings( botName ).isEmpty() )
			{
				addNoRequestMessage( botName );
				return;
			}

			for ( int i = 0; i < 4; ++i )
			{
				categoryPanels[i] = new JPanel();
				categoryPanels[i].setLayout( new BoxLayout( categoryPanels[i], BoxLayout.Y_AXIS ) );

				SimpleScrollPane scroller = new SimpleScrollPane( categoryPanels[i] );
				JComponentUtilities.setComponentSize( scroller, 500, 400 );

				this.add( scroller, String.valueOf(i) );
			}

			ArrayList list = new ArrayList();
			list.addAll( BuffBotDatabase.getStandardOfferings( botName ) );
			list.addAll( BuffBotDatabase.getPhilanthropicOfferings( botName ) );

			Collections.sort( list );

			this.offerings = new Offering[ list.size() ];
			list.toArray( this.offerings );

			this.checkboxes = new JCheckBox[ this.offerings.length ];

			for ( int i = 0; i < this.checkboxes.length; ++i )
			{
				if ( this.offerings[i].getLowestBuffId() < 1000 )
					continue;

				this.checkboxes[i] = new JCheckBox( this.offerings[i].toString() );
				this.checkboxes[i].setVerticalTextPosition( JCheckBox.TOP );
				addActionListener( checkboxes[i], priceUpdater );

				int price = this.offerings[i].getPrice();
				int [] turns = this.offerings[i].getTurns();
				String tooltip = price + " meat (" + FLOAT_FORMAT.format( (float)turns[0] / (float)price ) + " turns/meat)";
				this.checkboxes[i].setToolTipText( tooltip );

				int buffId = this.offerings[i].getLowestBuffId();
				int categoryId = getCategory( turns.length, buffId );

				addBuffLabel( turns.length, buffId, categoryId );

				if ( !nameList[categoryId].contains( botName ) )
					nameList[categoryId].add( botName );

				categoryPanels[categoryId].add( checkboxes[i] );
			}
		}

		private int getCategory( int count, int buffId )
		{
			if ( count > 1 )
				return 0;
			else if ( buffId > 4000 && buffId < 5000 )
				return 1;
			else if ( buffId > 2000 && buffId < 3000 )
				return 2;
			else if ( buffId > 6000 && buffId < 7000 )
				return 3;
			else
				return 0;
		}

		private void addNoRequestMessage( String botName )
		{
			for ( int i = 0; i < 4; ++i )
			{
				JTextArea message = new JTextArea( StaticEntity.globalStringReplace( NO_REQUEST_TEXT, "BOT_NAME", botName ) );

				message.setColumns( 40 );
				message.setLineWrap( true );
				message.setWrapStyleWord( true );
				message.setEditable( false );
				message.setOpaque( false );
				message.setFont( DEFAULT_FONT );

				this.add( new SimpleScrollPane( message ), String.valueOf(i) );
			}
		}

		private void addBuffLabel( int count, int buffId, int categoryId )
		{
			if ( count > 1 )
			{
				if ( addedBuffPackLabel )
					return;

				addedBuffPackLabel = true;
				categoryPanels[categoryId].add( new JLabel( "<html><h3>Buff Packs</h3></html>" ) );
				categoryPanels[categoryId].add( Box.createVerticalStrut( 5 ) );
				return;
			}

			if ( buffId == lastBuffId )
				return;

			lastBuffId = buffId;
			categoryPanels[categoryId].add( new JLabel( "<html><h3>" + ClassSkillsDatabase.getSkillName( buffId ) + "</h3></html>" ) );
			categoryPanels[categoryId].add( Box.createVerticalStrut( 5 ) );
		}
	}

	private void updateSendPrice()
	{
		if ( this.mainPanel == null )
			return;

		RequestPanel panel = getPanel();
		if ( panel == null || panel.checkboxes == null || panel.offerings == null )
			return;

		if ( lastPanel != null && lastPanel != panel )
		{
			JCheckBox [] checkboxes = lastPanel.checkboxes;

			for ( int i = 0; i < checkboxes.length; ++i )
				if ( checkboxes[i] != null )
					checkboxes[i].setSelected( false );
		}

		this.lastPanel = panel;

		int price = 0;
		JCheckBox [] checkboxes = panel.checkboxes;
		Offering [] offerings = panel.offerings;

		for ( int i = 0; i < checkboxes.length; ++i )
		{
			if ( checkboxes[i] == null || offerings[i] == null )
				continue;

			if ( checkboxes[i].isSelected() )
				price += offerings[i].getPrice();
		}

		this.mainPanel.setStatusMessage( COMMA_FORMAT.format( price ) + " meat will be sent to " + this.botName );
	}

	private String getCardId()
	{
		this.botName = (String) this.names.getSelectedItem();
		return this.botName;
	}

	private void resetCard()
	{
		int typeId = this.types.getSelectedIndex();
		if ( typeId != -1 && this.names.getModel() != nameList[typeId] )
			this.names.setModel( nameList[typeId] );

		RequestPanel panel = getPanel();
		if ( typeId == -1 || panel == null )
		{
			this.nameCards.show( this.nameContainer, "" );
			this.mainPanel.setStatusMessage( " " );
			return;
		}

		panel.categoryCards.show( panel, String.valueOf( typeId ) );
		this.nameCards.show( this.nameContainer, this.getCardId() );

		updateSendPrice();
	}

	private RequestPanel getPanel()
	{
		String cardId = this.getCardId();
		if ( cardId == null )
			return null;

		return (RequestPanel) this.panelMap.get( cardId );
	}

	private class CardSwitchListener extends ThreadedListener
	{
		public void run()
		{	resetCard();
		}
	}

	private class TotalPriceUpdater implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	updateSendPrice();
		}
	}
}
