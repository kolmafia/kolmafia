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

	private JPanel requestContainer;
	private CardLayout requestCards;

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );

		this.requestCards = new CardLayout( 10, 10 );
		this.requestContainer = new JPanel( this.requestCards );

		JPanel centerPanel = new JPanel( new BorderLayout() );
		centerPanel.add( new BuffRequestPanel(), BorderLayout.NORTH );
		centerPanel.add( this.requestContainer, BorderLayout.CENTER );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( centerPanel, "" );
	}

	public JTabbedPane getTabbedPane()
	{	return null;
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
		private String botName;
		private TreeMap panelMap;

		private SetComboBox sets;
		private NameComboBox names;

		public BuffRequestPanel()
		{
			super( "request", "online?" );

			Object [] list = BuffBotDatabase.getCompleteBotList();

			this.panelMap = new TreeMap();

			for ( int i = 0; i < list.length; ++i )
			{
				RequestPanel panel = new RequestPanel( true, (String) list[i] );
				this.panelMap.put( list[i] + " : 1", panel );
				BuffRequestFrame.this.requestContainer.add( panel, list[i] + " : 1" );

				panel = new RequestPanel( false, (String) list[i] );
				this.panelMap.put( list[i] + " : 2", panel );
				BuffRequestFrame.this.requestContainer.add( panel, list[i] + " : 2" );
			}

			this.names = new NameComboBox( list );
			this.sets = new SetComboBox();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Bot Name: ", this.names );
			elements[1] = new VerifiableElement( "Buff Set: ", this.sets );

			this.setContent( elements );
			this.resetCard();
		}

		public void actionConfirmed()
		{
			RequestPanel panel = (RequestPanel) this.panelMap.get( this.getCardId() );

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
				KoLmafia.updateDisplay( "Submitting buff request " + (i+1) + " of " + runnables.length + " to " + this.botName + "..." );
				RequestThread.postRequest( runnables[i] );
			}

			KoLmafia.updateDisplay( "Buff requests complete." );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{	BuffRequestFrame.this.isBotOnline( this.botName );
		}

		private String getCardId()
		{
			this.botName = (String) this.names.getSelectedItem();
			return this.botName + " : " + (this.sets.getSelectedIndex() + 1);
		}

		private void resetCard()
		{	BuffRequestFrame.this.requestCards.show( BuffRequestFrame.this.requestContainer, this.getCardId() );
		}

		private class NameComboBox extends JComboBox
		{
			public NameComboBox( Object [] data )
			{
				super( data );
				this.addActionListener( new NameComboBoxListener() );
			}
		}

		private class NameComboBoxListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	BuffRequestPanel.this.resetCard();
			}
		}

		private class SetComboBox extends JComboBox
		{
			public SetComboBox()
			{
				super( new String [] { "Once-per-day offerings", "Standard offerings" } );
				this.addActionListener( new SetComboBoxListener() );
			}
		}

		private class SetComboBoxListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	BuffRequestPanel.this.resetCard();
			}
		}

		private class RequestPanel extends JPanel
		{
			private JCheckBox [] checkboxes;
			private Offering [] offerings;

			public RequestPanel( boolean isFree, String botName )
			{
				if ( !botName.equals( "" ) && BuffBotDatabase.getPhilanthropicOfferings( botName ).isEmpty() &&
					BuffBotDatabase.getStandardOfferings( botName ).isEmpty() )
				{
					this.setLayout( new BorderLayout() );

					JTextArea message = new JTextArea(
						"\nTo whom it may concern:\n\n" +
						"At the frequent request of individuals wanting to see the name '" + botName + "' listed in KoLmafia's buff purchase interface, " + botName + " has been added to our internal buffbot database.\n\n" +
						"However, at the request of the individuals responsible for maintaining " + botName + ", " + botName + "'s formal price list and buff offerings are not available directly through KoLmafia.\n\n" +
						"You are welcome to use this interface to check whether or not " + botName + " is currently logged in to KoL.  However, we hope this message helps you understand why additional support was not added.\n\n\n" +
						"Respectfully yours,\nThe KoLmafia development team" );

					message.setColumns( 40 );
					message.setLineWrap( true );
					message.setWrapStyleWord( true );
					message.setEditable( false );
					message.setOpaque( false );
					message.setFont( DEFAULT_FONT );

					this.add( new SimpleScrollPane( message ), BorderLayout.CENTER );

					return;
				}

				LockableListModel list = isFree ? BuffBotDatabase.getPhilanthropicOfferings( botName ) :
					BuffBotDatabase.getStandardOfferings( botName );

				this.offerings = new Offering[ list.size() ];
				list.toArray( this.offerings );

				JPanel centerPanel = new JPanel();
				centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

				this.checkboxes = new JCheckBox[ this.offerings.length ];
				TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

				boolean addedAnyLabel = false;
				boolean addedPackLabel = false;
				boolean addedSingleLabel = false;

				boolean addedTurtleLabel = false;
				boolean addedSaucerorLabel = false;
				boolean addedThiefLabel = false;

				for ( int i = 0; i < this.checkboxes.length; ++i )
				{
					this.checkboxes[i] = new JCheckBox( this.offerings[i].toString() );
					this.checkboxes[i].setVerticalTextPosition( JCheckBox.TOP );
					this.checkboxes[i].addActionListener( priceUpdater );

					int price = this.offerings[i].getPrice();
					int [] turns = this.offerings[i].getTurns();
					String tooltip = price + " meat (" + FLOAT_FORMAT.format( (float)turns[0] / (float)price ) + " turns/meat)";
					this.checkboxes[i].setToolTipText( tooltip );

					if ( turns.length > 1 )
					{
						if ( !addedPackLabel )
						{
							centerPanel.add( new JLabel( "<html><h2>Buff Packs</h2></html>" ) );
							centerPanel.add( Box.createVerticalStrut( 10 ) );

							addedAnyLabel = true;
							addedPackLabel = true;
						}
					}
					else if ( isFree )
					{
						if ( !addedSingleLabel )
						{
							if ( addedAnyLabel )
								centerPanel.add( Box.createVerticalStrut( 20 ) );

							centerPanel.add( new JLabel( "<html><h2>Individual Buffs</h2></html>" ) );
							centerPanel.add( Box.createVerticalStrut( 10 ) );

							addedAnyLabel = true;
							addedSingleLabel = true;
						}
					}
					else
					{
						int buffId = this.offerings[i].getLowestBuffId();
						if ( buffId > 2000 && buffId < 3000 )
						{
							if ( !addedTurtleLabel )
							{
								if ( addedAnyLabel )
									centerPanel.add( Box.createVerticalStrut( 20 ) );

								centerPanel.add( new JLabel( "<html><h2>Turtle Tamer Buffs</h2></html>" ) );
								centerPanel.add( Box.createVerticalStrut( 10 ) );

								addedAnyLabel = true;
								addedTurtleLabel = true;
							}
						}
						if ( buffId > 4000 && buffId < 5000 )
						{
							if ( !addedSaucerorLabel )
							{
								if ( addedAnyLabel )
									centerPanel.add( Box.createVerticalStrut( 20 ) );

								centerPanel.add( new JLabel( "<html><h2>Sauceror Buffs</h2></html>" ) );
								centerPanel.add( Box.createVerticalStrut( 10 ) );

								addedAnyLabel = true;
								addedSaucerorLabel = true;
							}
						}
						if ( buffId > 6000 && buffId < 7000 )
						{
							if ( !addedThiefLabel )
							{
								if ( addedAnyLabel )
									centerPanel.add( Box.createVerticalStrut( 20 ) );

								centerPanel.add( new JLabel( "<html><h2>Accordion Thief Buffs</h2></html>" ) );
								centerPanel.add( Box.createVerticalStrut( 10 ) );

								addedAnyLabel = true;
								addedThiefLabel = true;
							}
						}
					}

					centerPanel.add( this.checkboxes[i] );
				}

				SimpleScrollPane scroller = new SimpleScrollPane( centerPanel );
				JComponentUtilities.setComponentSize( scroller, 500, 400 );

				this.setLayout( new BorderLayout() );
				this.add( scroller, BorderLayout.CENTER );
			}
		}

		private class TotalPriceUpdater implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				int price = 0;
				int requestCount = 0;

				RequestPanel panel = (RequestPanel) BuffRequestPanel.this.panelMap.get( BuffRequestPanel.this.getCardId() );

				JCheckBox [] checkboxes = panel.checkboxes;
				Offering [] offerings = panel.offerings;

				for ( int i = 0; i < checkboxes.length; ++i )
				{
					if ( checkboxes[i].isSelected() )
					{
						++requestCount;
						price += offerings[i].getPrice();
					}
				}

				if ( BuffRequestPanel.this.sets.getSelectedIndex() == 0 && requestCount > BuffBotManager.REFUND_THRESHOLD )
					BuffRequestPanel.this.setStatusMessage( "That's too many philanthropic buff requests." );
				else
					BuffRequestPanel.this.setStatusMessage( COMMA_FORMAT.format( price ) + " meat will be sent to " + BuffRequestPanel.this.botName );
			}
		}
	}
}
