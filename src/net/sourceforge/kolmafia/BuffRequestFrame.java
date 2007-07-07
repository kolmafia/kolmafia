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

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.BuffBotDatabase.Offering;

public class BuffRequestFrame extends KoLFrame
{
	private static final KoLRequest ONLINE_VALIDATOR = new KoLRequest( "submitnewchat.php", true );

	private static final String NO_REQUEST_TEXT = "\nTo whom it may concern:\n\n" +
		"At the frequent request of individuals wanting to see the name 'BOT_NAME' listed in KoLmafia's buff purchase interface, " +
		"BOT_NAME has been added to our internal buffbot database.\n\n" +
		"However, at the request of the individuals responsible for maintaining BOT_NAME, " +
		"BOT_NAME's formal price list and buff offerings are not available directly through KoLmafia.\n\n" +
		"You are welcome to use this interface to check whether or not BOT_NAME is currently logged in to KoL.  " +
		"However, we hope this message helps you understand why additional support was not added.\n\n\n" +
		"Respectfully yours,\nThe KoLmafia development team";

	private String botName;
	private JComboBox names;
	private TreeMap panelMap;

	private JPanel requestContainer;
	private CardLayout requestCards;

	private BuffRequestPanel mainPanel;
	private TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );
		this.setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		this.panelMap = new TreeMap();
		this.names = new JComboBox();

		addActionListener( this.names, new NameComboBoxListener() );

		this.requestCards = new CardLayout();
		this.requestContainer = new JPanel( requestCards );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.mainPanel = new BuffRequestPanel(), "" );
	}

	public JTabbedPane getTabbedPane()
	{	return null;
	}

	public void dispose()
	{
		removePanel( this.mainPanel );

		this.names = null;
		this.panelMap.clear();

		this.requestContainer = null;
		this.requestCards = null;
		this.mainPanel = null;

		this.priceUpdater = null;
		super.dispose();
	}

	private void isBotOnline( String botName )
	{
		ONLINE_VALIDATOR.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );
		ONLINE_VALIDATOR.addFormField( "pwd" );
		ONLINE_VALIDATOR.addFormField( "graf", "/whois " + botName );

		RequestThread.postRequest( ONLINE_VALIDATOR );

		if ( ONLINE_VALIDATOR.responseText != null && ONLINE_VALIDATOR.responseText.indexOf( "online" ) != -1 )
			JOptionPane.showMessageDialog( null, botName + " is online." );
		else
			JOptionPane.showMessageDialog( null, botName + " is probably not online." );
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

				if ( BuffBotDatabase.getStandardOfferings( (String) list[i] ).isEmpty() )
					if ( !BuffBotDatabase.getPhilanthropicOfferings( (String) list[i] ).isEmpty() )
						continue;

				BuffRequestFrame.this.names.addItem( list[i] );
				RequestPanel panel = new RequestPanel( (String) list[i] );
				BuffRequestFrame.this.panelMap.put( list[i], new WeakReference( panel ) );
				BuffRequestFrame.this.requestContainer.add( panel, list[i] );
			}

			JPanel selectContainer = new JPanel( new BorderLayout() );
			selectContainer.add( BuffRequestFrame.this.names, BorderLayout.NORTH );
			selectContainer.add( BuffRequestFrame.this.requestContainer, BorderLayout.CENTER );

			this.setContent( new VerifiableElement[0] );

			container.add( southContainer, BorderLayout.NORTH );
			container.add( selectContainer, BorderLayout.CENTER );

			BuffRequestFrame.this.names.setSelectedIndex( RNG.nextInt( BuffRequestFrame.this.names.getItemCount() ) );
			BuffRequestFrame.this.resetCard();
		}

		public void actionConfirmed()
		{
			RequestPanel panel = getPanel();

			JCheckBox [] checkboxes = panel.checkboxes;
			Offering [] offerings = panel.offerings;

			ArrayList requests = new ArrayList();
			for ( int i = 0; i < checkboxes.length; ++i )
				if ( checkboxes[i].isSelected() )
					requests.add( offerings[i].toRequest() );

			if ( requests.isEmpty() )
				return;

			GreenMessageRequest [] runnables = new GreenMessageRequest[ requests.size() ];
			requests.toArray( runnables );

			RequestThread.openRequestSequence();

			for ( int i = 0; i < runnables.length; ++i )
			{
				KoLmafia.updateDisplay( "Submitting buff request " + (i+1) + " of " + runnables.length + " to " + BuffRequestFrame.this.botName + "..." );
				RequestThread.postRequest( runnables[i] );
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
		private JPanel centerPanel = new JPanel();

		private boolean addedAnyLabel = false;
		private boolean addedPackLabel = false;
		private boolean addedSingleLabel = false;

		private boolean addedTurtleLabel = false;
		private boolean addedSaucerorLabel = false;
		private boolean addedThiefLabel = false;

		private JCheckBox [] checkboxes;
		private Offering [] offerings;

		public RequestPanel( String botName )
		{
			if ( BuffBotDatabase.getStandardOfferings( botName ).isEmpty() )
			{
				addNoRequestMessage( botName );
				return;
			}

			LockableListModel list = BuffBotDatabase.getStandardOfferings( botName );

			this.offerings = new Offering[ list.size() ];
			list.toArray( this.offerings );

			centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

			checkboxes = new JCheckBox[ this.offerings.length ];

			for ( int i = 0; i < checkboxes.length; ++i )
			{
				if ( this.offerings[i].getLowestBuffId() < 1000 )
					continue;

				checkboxes[i] = new JCheckBox( this.offerings[i].toString() );
				checkboxes[i].setVerticalTextPosition( JCheckBox.TOP );
				addActionListener( checkboxes[i], priceUpdater );

				int price = this.offerings[i].getPrice();
				int [] turns = this.offerings[i].getTurns();
				String tooltip = price + " meat (" + FLOAT_FORMAT.format( (float)turns[0] / (float)price ) + " turns/meat)";
				checkboxes[i].setToolTipText( tooltip );

				if ( turns.length > 1 )
					addBuffPackLabel();
				else
					addSingleLabel( this.offerings[i].getLowestBuffId() );

				centerPanel.add( checkboxes[i] );
			}

			SimpleScrollPane scroller = new SimpleScrollPane( centerPanel );
			JComponentUtilities.setComponentSize( scroller, 500, 400 );

			this.setLayout( new BorderLayout() );
			this.add( scroller, BorderLayout.CENTER );
		}

		private void addNoRequestMessage( String botName )
		{
			this.setLayout( new BorderLayout() );
			JTextArea message = new JTextArea( StaticEntity.globalStringReplace( NO_REQUEST_TEXT, "BOT_NAME", botName ) );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( DEFAULT_FONT );

			this.add( new SimpleScrollPane( message ), BorderLayout.CENTER );
		}

		private void addBuffPackLabel()
		{
			if ( addedPackLabel )
				return;

			centerPanel.add( new JLabel( "<html><h2>Buff Packs</h2></html>" ) );
			centerPanel.add( Box.createVerticalStrut( 10 ) );

			addedAnyLabel = true;
			addedPackLabel = true;
		}

		private void addSingleLabel( int buffId )
		{
			if ( buffId > 2000 && buffId < 3000 )
			{
				if ( addedTurtleLabel )
					return;

				if ( addedAnyLabel )
					centerPanel.add( Box.createVerticalStrut( 20 ) );

				centerPanel.add( new JLabel( "<html><h2>Turtle Tamer Buffs</h2></html>" ) );
				centerPanel.add( Box.createVerticalStrut( 10 ) );

				addedAnyLabel = true;
				addedTurtleLabel = true;
			}
			if ( buffId > 4000 && buffId < 5000 )
			{
				if ( addedSaucerorLabel )
					return;

				if ( addedAnyLabel )
					centerPanel.add( Box.createVerticalStrut( 20 ) );

				centerPanel.add( new JLabel( "<html><h2>Sauceror Buffs</h2></html>" ) );
				centerPanel.add( Box.createVerticalStrut( 10 ) );

				addedAnyLabel = true;
				addedSaucerorLabel = true;
			}
			if ( buffId > 6000 && buffId < 7000 )
			{
				if ( addedThiefLabel )
					return;

				if ( addedAnyLabel )
					centerPanel.add( Box.createVerticalStrut( 20 ) );

				centerPanel.add( new JLabel( "<html><h2>Accordion Thief Buffs</h2></html>" ) );
				centerPanel.add( Box.createVerticalStrut( 10 ) );

				addedAnyLabel = true;
				addedThiefLabel = true;
			}
		}
	}

	private void updateSendPrice()
	{
		if ( this.mainPanel == null )
			return;

		RequestPanel panel = getPanel();
		if ( panel == null )
			return;

		int price = 0;
		JCheckBox [] checkboxes = panel.checkboxes;
		Offering [] offerings = panel.offerings;

		for ( int i = 0; i < checkboxes.length; ++i )
			if ( checkboxes[i].isSelected() )
				price += offerings[i].getPrice();

		this.mainPanel.setStatusMessage( COMMA_FORMAT.format( price ) + " meat will be sent to " + this.botName );
	}

	private String getCardId()
	{
		this.botName = (String) this.names.getSelectedItem();
		return this.botName;
	}

	private void resetCard()
	{
		this.requestCards.show( BuffRequestFrame.this.requestContainer, BuffRequestFrame.this.getCardId() );
		updateSendPrice();
	}

	private RequestPanel getPanel()
	{
		WeakReference ref = (WeakReference) this.panelMap.get( this.getCardId() );
		return ref == null || ref.get() == null ? null : (RequestPanel) ref.get();
	}

	private class NameComboBoxListener extends ThreadedListener
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
