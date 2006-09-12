/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRootPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ActionPanel extends JRootPane
{
	protected JButton confirmedButton, cancelledButton;

	public abstract void actionConfirmed();
	public abstract void actionCancelled();

	protected class VerifyButtonPanel extends JPanel
	{
		private boolean bothDisabledOnClick;
		private String cancelledText1, cancelledText2;

		public VerifyButtonPanel( String confirmedText )
		{	this( confirmedText, null );
		}

		public VerifyButtonPanel( String confirmedText, String cancelledText )
		{	this( confirmedText, cancelledText, cancelledText );
		}

		public VerifyButtonPanel( String confirmedText, String cancelledText1, String cancelledText2 )
		{
 			setLayout( new BorderLayout() );
 			this.cancelledText1 = cancelledText1;
 			this.cancelledText2 = cancelledText2;

			JPanel containerPanel = new JPanel( new GridLayout( cancelledText1 == null ? 1 : 2, 1, 5, 5 ) );
			add( containerPanel, BorderLayout.NORTH );

			// add the "confirmed" button
			confirmedButton = new JButton( confirmedText );
			confirmedButton.addActionListener( new ConfirmedListener() );
			containerPanel.add( confirmedButton );

			// add the "cancelled" button
			if ( cancelledText1 != null )
			{
				cancelledButton = new JButton( cancelledText1 );
				cancelledButton.addActionListener( new CancelledListener() );
				containerPanel.add( cancelledButton );
			}
			else
				cancelledButton = null;
		}

		public void setEnabled( boolean isEnabled )
		{
			confirmedButton.setEnabled( isEnabled );

			if ( cancelledButton != null )
			{
				if ( bothDisabledOnClick )
					cancelledButton.setEnabled( isEnabled );
				cancelledButton.setText( isEnabled ? cancelledText1 : cancelledText2 );
			}
		}

		public void setBothDisabledOnClick( boolean bothDisabledOnClick )
		{	this.bothDisabledOnClick = bothDisabledOnClick;
		}
	}

	private class ConfirmedListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ConfirmThread()).start();
		}

		private class ConfirmThread extends Thread
		{
			public ConfirmThread()
			{	setDaemon( true );
			}

			public void run()
			{	actionConfirmed();
			}
		}
	}

	private class CancelledListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new CancelThread()).start();
		}

		private class CancelThread extends Thread
		{
			public CancelThread()
			{	setDaemon( true );
			}

			public void run()
			{	actionCancelled();
			}
		}
	}
}