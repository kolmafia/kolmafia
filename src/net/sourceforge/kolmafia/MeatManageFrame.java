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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.LockableListModel;

public class MeatManageFrame
	extends KoLFrame
{
	public MeatManageFrame()
	{
		super( "Meat Manager" );

		JPanel container = new JPanel( new GridLayout( 4, 1 ) );
		container.add( new HeroDonationPanel() );
		container.add( new MeatTransferPanel( ItemStorageRequest.MEAT_TO_CLOSET ) );
		container.add( new MeatTransferPanel( ItemStorageRequest.MEAT_TO_INVENTORY ) );
		container.add( new MeatTransferPanel( ItemStorageRequest.PULL_MEAT_FROM_STORAGE ) );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( container, "" );
	}

	public UnfocusedTabbedPane getTabbedPane()
	{
		return null;
	}

	public boolean useSidePane()
	{
		return true;
	}

	/**
	 * An internal class which represents the panel used for donations to the statues in the shrine.
	 */

	private class HeroDonationPanel
		extends LabeledKoLPanel
	{
		private final JComboBox heroField;
		private final AutoHighlightField amountField;

		public HeroDonationPanel()
		{
			super( "Donations to the Greater Good", "donate", "explode", new Dimension( 80, 20 ), new Dimension(
				240, 20 ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );

			this.heroField = new JComboBox( heroes );
			this.amountField = new AutoHighlightField();

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Donate To: ", this.heroField );
			elements[ 1 ] = new VerifiableElement( "Amount: ", this.amountField );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			if ( this.heroField.getSelectedIndex() != -1 )
			{
				RequestThread.postRequest( new HeroDonationRequest(
					this.heroField.getSelectedIndex() + 1, KoLFrame.getValue( this.amountField ) ) );
			}
		}

		public void actionCancelled()
		{
			this.setStatusMessage( "The Frost poem you dialed is unavailable at this time." );
		}
	}
}
