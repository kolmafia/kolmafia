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
import javax.swing.JTextField;

import net.java.dev.spellcast.utilities.LockableListModel;

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

			setContent( elements );
		}

		public void actionConfirmed()
		{
			if ( heroField.getSelectedIndex() != -1 )
				RequestThread.postRequest( new HeroDonationRequest( heroField.getSelectedIndex() + 1, getValue( amountField ) ) );
		}

		public void actionCancelled()
		{	setStatusMessage( "The Frost poem you dialed is unavailable at this time." );
		}
	}
}
