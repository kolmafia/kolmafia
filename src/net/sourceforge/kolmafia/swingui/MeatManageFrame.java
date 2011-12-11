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

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.MeatTransferPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MeatManageFrame
	extends GenericFrame
{
	public MeatManageFrame()
	{
		super( "Meat Manager" );

		JPanel container = new JPanel( new GridLayout( 4, 1 ) );
		container.add( new HeroDonationPanel() );
		container.add( new MeatTransferPanel( MeatTransferPanel.MEAT_TO_CLOSET ) );
		container.add( new MeatTransferPanel( MeatTransferPanel.MEAT_TO_INVENTORY ) );
		container.add( new MeatTransferPanel( MeatTransferPanel.PULL_MEAT_FROM_STORAGE ) );

		this.setCenterComponent( container );
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	/**
	 * An internal class which represents the panel used for donations to the statues in the shrine.
	 */

	private class HeroDonationPanel
		extends LabeledPanel
	{
		private final JComboBox heroField;
		private final AutoHighlightTextField amountField;

		public HeroDonationPanel()
		{
			super( "Donations to the Greater Good", "donate", "explode", new Dimension( 80, 20 ), new Dimension(
				240, 20 ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );

			this.heroField = new JComboBox( heroes );
			this.amountField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Donate To: ", this.heroField );
			elements[ 1 ] = new VerifiableElement( "Amount: ", this.amountField );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			if ( this.heroField.getSelectedIndex() != -1 )
			{
				RequestThread.postRequest( new ShrineRequest(
					this.heroField.getSelectedIndex() + 1, InputFieldUtilities.getValue( this.amountField ) ) );
			}
		}

		public void actionCancelled()
		{
			this.setStatusMessage( "The Frost poem you dialed is unavailable at this time." );
		}
	}
}
