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

import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * adventure in the Kingdom of Loathing.  As the class is developed, it will also
 * provide other adventure-related functionality, such as inventoryManage management
 * and mall purchases.  Its content panel will also change, pending the activity
 * executed at that moment.
 */

public class MeatManageFrame extends KoLFrame
{
	private HeroDonationPanel heroDonation;
	private MeatStoragePanel meatStorage;

	public MeatManageFrame()
	{
		super( "Meat Manager" );

		heroDonation = new HeroDonationPanel();
		meatStorage = new MeatStoragePanel();

		JPanel container = new JPanel( new GridLayout( 2, 1 ) );
		container.add( heroDonation );
		container.add( meatStorage );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( container, "" );
	}

	public boolean useSidePane()
	{	return true;
	}

	/**
	 * An internal class which represents the panel used for donations to
	 * the statues in the shrine.
	 */

	private class HeroDonationPanel extends LabeledKoLPanel
	{
		private JComboBox heroField;
		private JTextField amountField;

		public HeroDonationPanel()
		{
			super( "Donations to the Greater Good", "donate", "explode", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );

			heroField = new JComboBox( heroes );
			amountField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Donate To: ", heroField );
			elements[1] = new VerifiableElement( "Amount: ", amountField );

			setContent( elements, true, true );
		}

		public void actionConfirmed()
		{
			if ( heroField.getSelectedIndex() != -1 )
				(new RequestThread( new HeroDonationRequest( heroField.getSelectedIndex() + 1, getValue( amountField ) ) )).start();
		}

		public void actionCancelled()
		{	setStatusMessage( "The Frost poem you dialed is unavailable at this time." );
		}
	}

	/**
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	private class MeatStoragePanel extends LabeledKoLPanel
	{
		private JComboBox fundSource;
		private JTextField amountField, closetField;

		public MeatStoragePanel()
		{
			super( "Meat Management", "transfer", "bedidall", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

 			fundSource = new JComboBox();
			fundSource.addItem( "From Inventory to Closet" );
			fundSource.addItem( "From Closet to Inventory" );
			fundSource.addItem( "From Hagnk's to Inventory" );
			fundSource.addItem( "From Hagnk's to Closet" );

			amountField = new JTextField();
			closetField = new JTextField( COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Transfer: ", fundSource );
			elements[1] = new VerifiableElement( "Amount: ", amountField );
			elements[2] = new VerifiableElement( "In Closet: ", closetField );
			setContent( elements, true, true );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new ClosetUpdater() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( closetField == null )
				return;

			super.setEnabled( isEnabled );
			closetField.setEnabled( false );
		}

		public void actionConfirmed()
		{
			int transferType = -1;
			int fundTransferType = fundSource.getSelectedIndex();
			int amountToTransfer = getValue( amountField );

			if ( fundTransferType > 1 && KoLCharacter.isHardcore() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You cannot pull meat from Hagnk's while in Hardcore." );
				return;
			}

			ItemStorageRequest [] requests = new ItemStorageRequest[ fundTransferType == 3 ? 2 : 1 ];

			switch ( fundTransferType )
			{
				case 0:
					transferType = ItemStorageRequest.MEAT_TO_CLOSET;

					if ( amountToTransfer <= 0 )
						amountToTransfer = KoLCharacter.getAvailableMeat();

					break;

				case 1:
					transferType = ItemStorageRequest.MEAT_TO_INVENTORY;

					if ( amountToTransfer <= 0 )
						amountToTransfer = KoLCharacter.getClosetMeat();

					break;

				case 2:
				case 3:

					transferType = ItemStorageRequest.PULL_MEAT_FROM_STORAGE;

					if ( amountToTransfer <= 0 )
					{
						KoLmafia.updateDisplay( ERROR_STATE, "You must specify an amount to pull from Hagnk's." );
						return;
					}

					break;
			}

			requests[0] = new ItemStorageRequest( amountToTransfer, transferType );
			if ( fundTransferType == 3 )
				requests[1] = new ItemStorageRequest( amountToTransfer, ItemStorageRequest.MEAT_TO_CLOSET );

			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{	KoLmafiaGUI.constructFrame( "MoneyMakingGameFrame" );
		}

		private class ClosetUpdater implements Runnable
		{
			public void run()
			{	closetField.setText( COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) );
			}
		}
	}
}
