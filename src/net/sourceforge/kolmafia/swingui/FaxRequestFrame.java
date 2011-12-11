/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.ListSelectionModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class FaxRequestFrame
	extends GenericFrame
{
	private MonsterCategoryComboBox categorySelect;
	private MonsterSelectPanel monsterSelect;
	private int monsterIndex;

	private static ShowDescriptionList [] monsterLists;
	private static final int ROWS = 15;
	private static final int LIMIT = 60;
	private static final int DELAY = 200;

	static
	{
		FaxBotDatabase.configure();
		FaxRequestFrame.monsterLists = new ShowDescriptionList[ FaxBotDatabase.monstersByCategory.length ];
		for ( int i = 0; i < FaxRequestFrame.monsterLists.length; ++i )
		{
			FaxRequestFrame.monsterLists[ i ] = new ShowDescriptionList( FaxBotDatabase.monstersByCategory[ i ], ROWS );
		}
	}

	public FaxRequestFrame()
	{
		super( "Request a Fax" );
		this.setCenterComponent( new FaxRequestPanel() );
	}

	private class FaxRequestPanel
		extends GenericPanel
	{
		String botName = FaxBotDatabase.botName( 0 );
		private String statusMessage;

		public FaxRequestPanel()
		{
			super( "request", "online?", new Dimension( 75, 24 ), new Dimension( 200, 24) );

			FaxRequestFrame.this.categorySelect = new MonsterCategoryComboBox();
			FaxRequestFrame.this.monsterIndex = 0;
			FaxRequestFrame.this.monsterSelect = new MonsterSelectPanel( FaxRequestFrame.monsterLists[0] );

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Category: ", FaxRequestFrame.this.categorySelect );


			this.setContent( elements );
			this.add( FaxRequestFrame.this.monsterSelect, BorderLayout.CENTER );
			this.add( new StatusPanel(), BorderLayout.SOUTH );
		}

		public boolean shouldAddStatusLabel()
		{
			return false;
		}

		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			if ( FaxRequestFrame.this.categorySelect != null )
			{
				FaxRequestFrame.this.categorySelect.setEnabled( isEnabled );
			}
			if ( FaxRequestFrame.this.monsterSelect != null )
			{
				FaxRequestFrame.this.monsterSelect.setEnabled( isEnabled );
			}
		}

		public void actionConfirmed()
		{
			int list = FaxRequestFrame.this.monsterIndex;
			Object value = FaxRequestFrame.monsterLists[ list ].getSelectedValue();
			if ( value == null )
			{
				return;
			}

			// Validate ability to receive a fax
			if ( !canReceiveFax() )
			{
				KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
				return;
			}

			// Make sure FaxBot is online
			if ( !isBotOnline( botName ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
				return;
			}

			// Make sure we can receive chat messages, either via
			// KoLmafia chat or in the Relay Browser.
			if ( !( ChatManager.isRunning() || true ) )
			{
				statusMessage = "You must be in chat so we can receive messages from " + botName;
				KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
				return;
			}

			// Do you already have a photocopied monster?
			if ( InventoryManager.hasItem( ItemPool.PHOTOCOPIED_MONSTER ) )
			{
				String monster = Preferences.getString( "photocopyMonster" );
				if ( monster.equals( "" ) )
				{
					monster = "monster";
				}

				// Yes. Offer a chance to discard it right now
				if ( !InputFieldUtilities.confirm( "You have a photocopied " + monster + " in your inventory. Dump it?" ) )
				{
					statusMessage = "You need to dispose of your photocopied " + monster + " before you can receive a fax.";
					KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
					return;
				}

				ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.SEND_FAX );
				RequestThread.postRequest( request );
			}

			Monster monster = (Monster)value;
			String name = monster.getName();
			String command = monster.getCommand();

			// We can try several times...
			PauseObject pauser = new PauseObject();

			while ( true )
			{
				KoLmafia.updateDisplay( "Asking " + botName + " to send a fax of " + name + ": " + command );

				// Clear last message, just in case.
				ChatManager.getLastFaxBotMessage();

				ChatSender.sendMessage( botName, command, false );

				String response = null;
				// Response is sent blue message. Can it fail?

				int polls = LIMIT * 1000 / DELAY;
				for ( int i = 0; i < polls; ++i )
				{
					response = ChatManager.getLastFaxBotMessage();
					if ( response != null )
					{
						break;
					}
					pauser.pause( DELAY );
				}

				if ( response == null )
				{
					statusMessage = "No response from " + botName + " after " + LIMIT + " seconds.";
					KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
					return;
				}

				// FaxBot just delivered a fax to your clan,
				// please try again in 1 minute.
				if ( response.indexOf( "just delivered a fax" ) != -1 )
				{
					statusMessage = botName + " recently delivered another fax. Retrying in one minute.";
					KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
					KoLmafia.forceContinue();
					StaticEntity.executeCountdown( "Countdown: ", 60 );

					continue;
				}

				// parse FaxBot's response
				if ( !faxAvailable( response ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
					return;
				}

				// Success! No need to retry
				break;
			}

			// The monster is there! retrieve it.
			ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX );
			RequestThread.postRequest( request );
			KoLmafia.enableDisplay();
		}

		private boolean canReceiveFax()
		{
			// Do you have a VIP key?
			if ( !InventoryManager.hasItem( ClanLoungeRequest.VIP_KEY ) )
			{
				statusMessage = "You don't have a VIP key.";
				return false;
			}

			// Try to visit the fax machine
			ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE );
			RequestThread.postRequest( request );

			// Are you in a clan?
			String redirect = request.redirectLocation;
			if ( redirect != null && redirect.equals( "clan_signup.php" ) )
			{
				statusMessage = "You are not in a clan.";
				return false;
			}

			// Does your clan have a fax machine?
			if ( request.responseText.indexOf( "You approach the fax machine" ) == -1 )
			{
				statusMessage = "Your clan does not have a fax machine.";
				return false;
			}
			return true;
		}

		private boolean faxAvailable( final String response )
		{
			// FaxBot has copied a Rockfish into your clan's Fax
			// Machine.
			if ( response.indexOf( "into your clan's Fax Machine" ) != -1 )
			{
				return true;
			}

			if ( response.indexOf( "I do not understand your request" ) != -1 )
			{
				statusMessage = "Configuration error: unknown command sent to " + botName;
				return false;
			}

			if ( response.indexOf( "could not whitelist" ) != -1 )
			{
				statusMessage = botName + " is not on your clan's whitelist";
				return false;
			}

			// You are only allowed 20 fax requests per day. Please
			// try again tomorrow.

			statusMessage = response;
			return false;
		}

		public void actionCancelled()
		{
			if ( isBotOnline( botName ) )
			{
				statusMessage = botName + " is online.";
			}
			KoLmafia.updateDisplay( KoLConstants.ENABLE_STATE, statusMessage );
		}

		private boolean isBotOnline( final String botName )
		{
			// Return false and set statusMessage to an appropriate
			// message if the bot is NOT online.

			if ( botName == null )
			{
				statusMessage = "No faxbots configured.";
				return false;
			}

			if ( !KoLmafia.isPlayerOnline( botName ) )
			{
				statusMessage = botName + " is probably not online.";
				return false;
			}

			// Do not bother allocating a message if bot is online
			return true;
		}
	}

	private class MonsterCategoryComboBox
		extends JComboBox
	{
		public MonsterCategoryComboBox()
		{
			super();
			int count = FaxBotDatabase.categories.size();
			for ( int i = 0; i < count; ++i )
			{
				addItem( FaxBotDatabase.categories.get( i ) );
			}
			addActionListener( new MonsterCategoryListener() );
		}

		private class MonsterCategoryListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				int index = MonsterCategoryComboBox.this.getSelectedIndex();
				FaxRequestFrame.this.monsterIndex = index;
				FaxRequestFrame.this.monsterSelect.setElementList( FaxRequestFrame.monsterLists[ index ] );
			}
		}
	}

	private class MonsterSelectPanel
		extends ScrollablePanel
	{
		private ShowDescriptionList elementList;
		private AutoFilterTextField filterfield;

		public MonsterSelectPanel( final ShowDescriptionList list )
		{
			super( "", null, null, list, false );

			this.elementList = list;
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.elementList.setVisibleRowCount( 8 );

			this.filterfield = new AutoFilterTextField( this.elementList );
			this.centerPanel.add( this.filterfield, BorderLayout.NORTH );
		}

		public void setElementList( final ShowDescriptionList list )
		{
			this.elementList = list;
			this.scrollPane.getViewport().setView( list );
			this.filterfield.setList( list );
		}
	}
}
