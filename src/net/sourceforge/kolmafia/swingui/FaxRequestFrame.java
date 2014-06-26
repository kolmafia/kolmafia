/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanLoungeRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
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
	private static final int ROWS = 15;
	private static final int LIMIT = 60;
	private static final int DELAY = 200;

	private CardLayoutSelectorPanel selectorPanel = null;
	private static String statusMessage;

	static
	{
		FaxBotDatabase.configure();
	}

	public FaxRequestFrame()
	{
		super( "Request a Fax" );
		this.selectorPanel = new CardLayoutSelectorPanel( "faxbots", "MMMMMMMMMMMM" );
		for ( FaxBot bot : FaxBotDatabase.faxbots )
		{
			JPanel panel = new JPanel( new BorderLayout() );
			FaxRequestPanel botPanel = new FaxRequestPanel( bot );
			panel.add( botPanel );
			this.selectorPanel.addPanel( bot.getName(), panel );
		}
		this.selectorPanel.setSelectedIndex( 0 );
		this.setCenterComponent( this.selectorPanel );
		this.add( new StatusPanel(), BorderLayout.SOUTH );
	}

	private class FaxRequestPanel
		extends GenericPanel
	{
		private FaxBot bot;

		public ShowDescriptionList [] monsterLists;
		public int monsterIndex;
		private MonsterCategoryComboBox categorySelect;
		private MonsterSelectPanel monsterSelect;

		public FaxRequestPanel( FaxBot bot )
		{
			super( "request", "online?", new Dimension( 75, 24 ), new Dimension( 200, 24) );

			this.bot = bot;

			LockableListModel [] monstersByCategory = bot.getMonstersByCategory();
			int categories = monstersByCategory.length;
			this.monsterLists = new ShowDescriptionList[ categories ];
			for ( int i = 0; i < categories; ++i )
			{
				this.monsterLists[ i ] = new ShowDescriptionList( monstersByCategory[ i ], ROWS );
			}


			this.categorySelect = new MonsterCategoryComboBox( this, bot );
			this.monsterSelect = new MonsterSelectPanel( this.monsterLists[0] );
			this.monsterIndex = 0;

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Category: ", this.categorySelect );


			this.setContent( elements );
			this.add( this.monsterSelect, BorderLayout.CENTER );
		}

		@Override
		public boolean shouldAddStatusLabel()
		{
			return false;
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			if ( this.categorySelect != null )
			{
				this.categorySelect.setEnabled( isEnabled );
			}
			if ( this.monsterSelect != null )
			{
				this.monsterSelect.setEnabled( isEnabled );
			}
		}

		@Override
		public void actionConfirmed()
		{
			int list = this.monsterIndex;
			Object value = monsterLists[ list ].getSelectedValue();
			if ( value == null )
			{
				return;
			}

			String botName = this.bot.getName();
			Monster monster = (Monster)value;
			String name = monster.getName();
			String command = monster.getCommand();

			FaxRequestFrame.requestFax( botName, name, command );
		}

		@Override
		public void actionCancelled()
		{
			String botName = this.bot.getName();
			if ( FaxRequestFrame.isBotOnline( botName ) )
			{
				FaxRequestFrame.statusMessage = botName + " is online.";
			}
			KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
		}

	}

	public static boolean requestFax( final String botName, final String monster, final String command )
	{
		// Validate ability to receive a fax
		if ( !FaxRequestFrame.canReceiveFax() )
		{
			KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
			return false;
		}

		// Make sure FaxBot is online
		if ( !FaxRequestFrame.isBotOnline( botName ) )
		{
			KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
			return false;
		}

		// Make sure we can receive chat messages, either via KoLmafia chat or in the Relay Browser.
		if ( !( ChatManager.isRunning() || true ) )
		{
			FaxRequestFrame.statusMessage = "You must be in chat so we can receive messages from " + botName;
			KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
			return false;
		}

		// Do you already have a photocopied monster?
		if ( InventoryManager.hasItem( ItemPool.PHOTOCOPIED_MONSTER ) )
		{
			String current = Preferences.getString( "photocopyMonster" );
			if ( current.equals( "" ) )
			{
				current = "monster";
			}

			// Yes. Offer a chance to discard it right now
			if ( !InputFieldUtilities.confirm( "You have a photocopied " + current + " in your inventory. Dump it?" ) )
			{
				FaxRequestFrame.statusMessage = "You need to dispose of your photocopied " + current + " before you can receive a fax.";
				KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
				return false;
			}

			ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.SEND_FAX );
			RequestThread.postRequest( request );
		}

		// We can try several times...
		PauseObject pauser = new PauseObject();

		StringBuilder buf = new StringBuilder();
		buf.append( "Asking " );
		buf.append( botName );
		buf.append( " to send a fax" );
		if ( monster != null )
		{
			buf.append( " of " );
			buf.append( monster );
		}
		buf.append( ": " );
		buf.append( command );

		String message = buf.toString();

		try
		{
			ChatManager.setFaxBot( botName );

			while ( true )
			{
				KoLmafia.updateDisplay( message );

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
					FaxRequestFrame.statusMessage = "No response from " + botName + " after " + LIMIT + " seconds.";
					KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
					return false;
				}

				// FaxBot just delivered a fax to your clan, please try again in 1 minute.
				if ( response.contains( "just delivered a fax" ) )
				{
					FaxRequestFrame.statusMessage = botName + " recently delivered another fax. Retrying in one minute.";
					KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
					KoLmafia.forceContinue();
					StaticEntity.executeCountdown( "Countdown: ", 60 );
					continue;
				}

				// parse FaxBot's response
				if ( !FaxRequestFrame.faxAvailable( botName, response ) )
				{
					KoLmafia.updateDisplay( FaxRequestFrame.statusMessage );
					return false;
				}

				// Success! No need to retry
				break;
			}
		}
		finally
		{
			ChatManager.setFaxBot( null );
		}

		// The monster is there! retrieve it.
		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX );
		RequestThread.postRequest( request );
		KoLmafia.enableDisplay();
		return true;
	}

	private static boolean canReceiveFax()
	{
		// Are you allowed to use chat?
		if ( !ChatManager.chatLiterate() )
		{
			FaxRequestFrame.statusMessage = "You are not allowed to use chat.";
			return false;
		}

		// Do you have a VIP key?
		if ( !InventoryManager.hasItem( ClanLoungeRequest.VIP_KEY ) )
		{
			FaxRequestFrame.statusMessage = "You don't have a VIP key.";
			return false;
		}

		// Are you Trendy?
		if ( KoLCharacter.isTrendy() || KoLCharacter.getRestricted() )
		{
			FaxRequestFrame.statusMessage = "Fax machines are out of style.";
			return false;
		}

		// Are you an Avatar of Boris?
		if ( KoLCharacter.inAxecore() )
		{
			FaxRequestFrame.statusMessage = "Boris sneered at technology.";
			return false;
		}

		// Are you an Avatar of Jarlsberg?
		if ( KoLCharacter.isJarlsberg() )
		{
			FaxRequestFrame.statusMessage = "Jarlsberg was more into magic than technology.";
			return false;
		}

		// Are you an Avatar of Sneaky Pete?
		if ( KoLCharacter.isSneakyPete() )
		{
			FaxRequestFrame.statusMessage = "Have you ever seen a cool person use a fax machine? I didn't think so.";
			return false;
		}

		// Try to visit the fax machine
		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE );
		RequestThread.postRequest( request );

		// Are you in a clan?
		String redirect = request.redirectLocation;
		if ( redirect != null && redirect.equals( "clan_signup.php" ) )
		{
			FaxRequestFrame.statusMessage = "You are not in a clan.";
			return false;
		}

		// Does your clan have a fax machine?
		if ( !request.responseText.contains( "You approach the fax machine" ) )
		{
			FaxRequestFrame.statusMessage = "Your clan does not have a fax machine.";
			return false;
		}
		return true;
	}

	private static boolean faxAvailable( final String botName, final String response )
	{
		// FaxBot has copied a Rockfish into your clan's Fax Machine.
		// Your monster has been delivered to your clan Fax Machine. Thank you for using FaustBot.
		// Your fax is ready.
		if ( response.contains( "into your clan's Fax Machine" ) ||
		     response.contains( "delivered to your clan Fax Machine" ) ||
		     response.contains( "Your fax is ready" ) )
		{
			return true;
		}

		// Sorry, it appears you requested an invalid monster.
		// I couldn't find that monster. Try sending "list" for a list of monster names.
		if ( response.contains( "I do not understand your request" ) ||
		     response.contains( "you requested an invalid monster" ) ||
		     response.contains( "I couldn't find that monster" ) )
		{
			FaxRequestFrame.statusMessage = "Configuration error: unknown command sent to " + botName;
			return false;
		}

		// I am unable to whitelist to clan 'xxx', please verify faustbot (#2504770) is whitelisted. Thank you.
		// I couldn't get into your clan
		if ( response.contains( "could not whitelist" ) ||
		     response.contains( "unable to whitelist" ) ||
		     response.contains( "I couldn't get into your clan" ) )
		{
			FaxRequestFrame.statusMessage = botName + " is not on your clan's whitelist";
			return false;
		}

		// You are only allowed 20 fax requests per day. Please
		// try again tomorrow.

		FaxRequestFrame.statusMessage = response;
		return false;
	}

	private static boolean isBotOnline( final String botName )
	{
		// Return false and set FaxRequestFrame.statusMessage to an appropriate
		// message if the bot is NOT online.

		if ( botName == null )
		{
			FaxRequestFrame.statusMessage = "No faxbots configured.";
			return false;
		}

		if ( !KoLmafia.isPlayerOnline( botName ) )
		{
			FaxRequestFrame.statusMessage = botName + " is probably not online.";
			return false;
		}

		// Do not bother allocating a message if bot is online
		return true;
	}

	private class MonsterCategoryComboBox
		extends JComboBox
	{
		FaxRequestPanel panel;

		public MonsterCategoryComboBox( FaxRequestPanel panel, FaxBot bot )
		{
			super();
			this.panel = panel;
			LockableListModel categories = bot.getCategories();
			int count = categories.size();
			for ( int i = 0; i < count; ++i )
			{
				addItem( categories.get( i ) );
			}
			addActionListener( new MonsterCategoryListener() );
		}

		private class MonsterCategoryListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				int index = MonsterCategoryComboBox.this.getSelectedIndex();
				MonsterCategoryComboBox.this.panel.monsterIndex = index;
				MonsterCategoryComboBox.this.panel.monsterSelect.setElementList( MonsterCategoryComboBox.this.panel.monsterLists[ index ] );
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
