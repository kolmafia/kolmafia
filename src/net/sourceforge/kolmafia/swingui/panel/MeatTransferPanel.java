/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;

import javax.swing.JLabel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MeatTransferPanel
	extends LabeledPanel
{
	private final int transferType;
	private final AutoHighlightTextField amountField;
	private final JLabel closetField;

	public static final int MEAT_TO_CLOSET = 1;
	public static final int MEAT_TO_INVENTORY = 2;
	public static final int PULL_MEAT_FROM_STORAGE = 3;

	public MeatTransferPanel( final int transferType )
	{
		super( MeatTransferPanel.getTitle( transferType ),
			"transfer", "transfer all", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

		this.amountField = new AutoHighlightTextField();
		this.closetField = new JLabel( " " );

		VerifiableElement[] elements = new VerifiableElement[ 2 ];
		elements[ 0 ] = new VerifiableElement( "Amount: ", this.amountField );
		elements[ 1 ] = new VerifiableElement( "Available: ", this.closetField );

		this.setContent( elements );

		this.transferType = transferType;
		this.refreshCurrentAmount();

		KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new AmountRefresher() ) );
	}

	private static String getTitle( final int transferType )
	{
		switch ( transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return "Put Meat in Your Closet";
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return "Take Meat from Your Closet";
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return "Pull Meat from Hagnk's";
		}
		return "Unknown Transfer Type";
	}

	private GenericRequest getRequest( final int amount )
	{
		switch ( transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return new ClosetRequest( ClosetRequest.MEAT_TO_CLOSET, amount );
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return new ClosetRequest( ClosetRequest.MEAT_TO_INVENTORY, amount );
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return new StorageRequest( StorageRequest.PULL_MEAT_FROM_STORAGE, amount );
		}
		return null;
	}

	private int currentAvailable()
	{
		switch ( this.transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return KoLCharacter.getAvailableMeat();
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return KoLCharacter.getClosetMeat();
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return KoLCharacter.getStorageMeat();
		}

		return 0;
	}

	private void refreshCurrentAmount()
	{
		switch ( this.transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
		case MeatTransferPanel.MEAT_TO_INVENTORY:
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			int amount = this.currentAvailable();
			this.closetField.setText( KoLConstants.COMMA_FORMAT.format( amount ) + " meat" );
			break;

		default:
			this.closetField.setText( "Information not available" );
			break;
		}
	}

	@Override
	public void actionConfirmed()
	{
		int amountToTransfer = InputFieldUtilities.getValue( this.amountField );

		RequestThread.postRequest( this.getRequest( amountToTransfer ) );
	}

	@Override
	public void actionCancelled()
	{
		RequestThread.postRequest( this.getRequest( this.currentAvailable() ) );
	}

	public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
	{
		return false;
	}

	private class AmountRefresher
		implements Runnable
	{
		public void run()
		{
			MeatTransferPanel.this.refreshCurrentAmount();
		}
	}
}
