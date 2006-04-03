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

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

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
	private MeatStoragePanel meatStorage;
	private HeroDonationPanel heroDonation;

	public MeatManageFrame()
	{
		super( "Meat Manager" );

		JPanel container = new JPanel();
		container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );
		container.add( meatStorage );
		container.add( heroDonation );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( container );
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
			super( "Donations to the Greater Good", "lump sum", "increments", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

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

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			heroField.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			if ( heroField.getSelectedIndex() != -1 )
				(new RequestThread( new HeroDonationRequest( StaticEntity.getClient(), heroField.getSelectedIndex() + 1, getValue( amountField ) ) )).start();

		}

		protected void actionCancelled()
		{
			try
			{
				int increments = df.parse( JOptionPane.showInputDialog( "How many increments?" ) ).intValue();

				if ( increments == 0 )
				{
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Donation cancelled." );
					return;
				}

				if ( heroField.getSelectedIndex() != -1 )
				{
					int eachAmount = getValue( amountField ) / increments;
					(new RequestThread( new HeroDonationRequest( StaticEntity.getClient(), heroField.getSelectedIndex() + 1, eachAmount ), increments )).start();
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}
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
			closetField = new JTextField( df.format( KoLCharacter.getClosetMeat() ) );
			closetField.setEnabled( false );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Transfer: ", fundSource );
			elements[1] = new VerifiableElement( "Amount: ", amountField );
			elements[2] = new VerifiableElement( "In Closet: ", closetField );
			setContent( elements, true, true );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new ClosetUpdater() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			fundSource.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			int transferType = -1;
			int fundTransferType = fundSource.getSelectedIndex();
			int amountToTransfer = getValue( amountField );

			if ( fundTransferType > 1 && KoLCharacter.isHardcore() )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You cannot pull meat from Hagnk's while in Hardcore." );
				StaticEntity.getClient().enableDisplay();
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
						DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You must specify an amount to pull from Hagnk's." );
						StaticEntity.getClient().enableDisplay();
						return;
					}

					break;
			}
			
			requests[0] = new ItemStorageRequest( StaticEntity.getClient(), amountToTransfer, transferType );
			if ( fundTransferType == 3 )
				requests[1] = new ItemStorageRequest( StaticEntity.getClient(), amountToTransfer, ItemStorageRequest.MEAT_TO_CLOSET );

			(new RequestThread( requests )).start();
		}

		protected void actionCancelled()
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "<html><font color=green>You <font color=red>lost</font>. Better luck next time.</font></html>" );
			StaticEntity.getClient().enableDisplay();
		}

		private class ClosetUpdater implements Runnable
		{
			public void run()
			{	closetField.setText( df.format( KoLCharacter.getClosetMeat() ) );
			}
		}
	}
}
