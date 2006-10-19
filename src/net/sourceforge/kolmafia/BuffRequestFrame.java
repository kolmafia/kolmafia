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
import java.awt.GridLayout;
import java.awt.Color;

// containers
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
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
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	private static final KoLRequest ONLINE_VALIDATOR = new KoLRequest( "submitnewchat.php", true );

	private JPanel requestContainer;
	private CardLayout requestCards;

	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );

		requestCards = new CardLayout( 10, 10 );
		requestContainer = new JPanel( requestCards );

		JPanel centerPanel = new JPanel( new BorderLayout() );
		centerPanel.add( new BuffRequestPanel(), BorderLayout.NORTH );
		centerPanel.add( requestContainer, BorderLayout.CENTER );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( centerPanel, "" );
	}

	private void isBotOnline( String botName )
	{
		ONLINE_VALIDATOR.addFormField( "playerid", String.valueOf( KoLCharacter.getUserID() ) );
		ONLINE_VALIDATOR.addFormField( "pwd" );
		ONLINE_VALIDATOR.addFormField( "graf", "/whois " + botName );
		ONLINE_VALIDATOR.run();

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

			panelMap = new TreeMap();

			for ( int i = 0; i < list.length; ++i )
			{
				RequestPanel panel = new RequestPanel( true, (String) list[i] );
				panelMap.put( list[i] + " : 1", panel );
				requestContainer.add( panel, list[i] + " : 1" );

				panel = new RequestPanel( false, (String) list[i] );
				panelMap.put( list[i] + " : 2", panel );
				requestContainer.add( panel, list[i] + " : 2" );
			}

			names = new NameComboBox( list );
			sets = new SetComboBox();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Bot Name: ", names );
			elements[1] = new VerifiableElement( "Buff Set: ", sets );

			setContent( elements );
			resetCard();
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return true;
		}

		public void actionConfirmed()
		{	(new RequestThread( new BuffPurchaseRunnable() )).start();
		}

		private class BuffPurchaseRunnable implements Runnable
		{
			public void run()
			{
				RequestPanel panel = (RequestPanel) panelMap.get( getCardID() );

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

				for ( int i = 0; i < runnables.length; ++i )
				{
					KoLmafia.updateDisplay( "Submitting buff request " + (i+1) + " of " + runnables.length + " to " + botName + "..." );
					runnables[i].run();
				}

				KoLmafia.updateDisplay( "Buff requests complete." );
				KoLmafia.enableDisplay();
			}
		}

		public void actionCancelled()
		{	isBotOnline( botName );
		}

		private String getCardID()
		{
			botName = (String) names.getSelectedItem();
			return botName + " : " + (sets.getSelectedIndex() + 1);
		}

		private void resetCard()
		{	requestCards.show( requestContainer, getCardID() );
		}

		private class NameComboBox extends JComboBox implements ActionListener
		{
			public NameComboBox( Object [] data )
			{
				super( data );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{	resetCard();
			}
		}

		private class SetComboBox extends JComboBox implements ActionListener
		{
			public SetComboBox()
			{
				super( new String [] { "Once-per-day offerings", "Standard offerings" } );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{	resetCard();
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

				if ( !botName.equals( "" ) && BuffBotDatabase.getPhilanthropicOfferings( botName ).isEmpty() &&
					BuffBotDatabase.getStandardOfferings( botName ).isEmpty() )
				{
					add( new JLabel( "<html><center><b><font size=7>This buffbot has opted<br>not to be included<br>in this price listing.<br><br>Please stop asking.<br>Thank you.</font><b></center></html>" ) );
					return;
				}

				LockableListModel list = isFree ? BuffBotDatabase.getPhilanthropicOfferings( botName ) :
					BuffBotDatabase.getStandardOfferings( botName );

				offerings = new BuffBotDatabase.Offering[ list.size() ];
				list.toArray( offerings );

				JPanel centerPanel = new JPanel();
				centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );

				checkboxes = new JCheckBox[ offerings.length ];
				TotalPriceUpdater priceUpdater = new TotalPriceUpdater();

				boolean addedAnyLabel = false;
				boolean addedPackLabel = false;
				boolean addedSingleLabel = false;

				boolean addedTurtleLabel = false;
				boolean addedSaucerorLabel = false;
				boolean addedThiefLabel = false;

				for ( int i = 0; i < checkboxes.length; ++i )
				{
					checkboxes[i] = new JCheckBox( offerings[i].toString() );
					checkboxes[i].setVerticalTextPosition( JCheckBox.TOP );
					checkboxes[i].addActionListener( priceUpdater );

					int price = offerings[i].getPrice();
					int [] turns = offerings[i].getTurns();
					String tooltip = price + " meat (" + FLOAT_FORMAT.format( (float)turns[0] / (float)price ) + " turns/meat)";
					checkboxes[i].setToolTipText( tooltip );

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
						int buffID = offerings[i].getLowestBuffID();
						if ( buffID > 2000 && buffID < 3000 )
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
						if ( buffID > 4000 && buffID < 5000 )
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
						if ( buffID > 6000 && buffID < 7000 )
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

					centerPanel.add( checkboxes[i] );
				}

				SimpleScrollPane scroller = new SimpleScrollPane( centerPanel );
				JComponentUtilities.setComponentSize( scroller, 500, 400 );

				setLayout( new BorderLayout() );
				add( scroller, BorderLayout.CENTER );
			}
		}

		private class TotalPriceUpdater implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				int price = 0;
				RequestPanel panel = (RequestPanel) panelMap.get( getCardID() );

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
