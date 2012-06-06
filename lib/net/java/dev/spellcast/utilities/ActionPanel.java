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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRootPane;

public abstract class ActionPanel
	extends JRootPane
{
	protected boolean contentSet = false;
	protected JButton confirmedButton, cancelledButton;
	protected static boolean actionsEnabled = true;

	public abstract void actionConfirmed();

	public abstract void actionCancelled();

	public void dispose()
	{
	}

	protected class VerifyButtonPanel
		extends JPanel
	{
		private boolean bothDisabledOnClick;
		private ActionListener confirmListener, cancelListener;
		private final String cancelledText1, cancelledText2;

		public VerifyButtonPanel( final String confirmedText, ActionListener confirmListener, ActionListener cancelListener )
		{
			this( confirmedText, null, null, confirmListener, cancelListener );
		}

		public VerifyButtonPanel( final String confirmedText, final String cancelledText, ActionListener confirmListener, ActionListener cancelListener )
		{
			this( confirmedText, cancelledText, cancelledText, confirmListener, cancelListener );
		}

		public VerifyButtonPanel( final String confirmedText, final String cancelledText1, final String cancelledText2, ActionListener confirmListener, ActionListener cancelListener )
		{
			this.confirmListener = confirmListener;
			this.cancelListener = cancelListener;

			this.setLayout( new BorderLayout() );

			this.setOpaque( true );
			if ( ActionPanel.this.getContentPane() instanceof JPanel )
			{
				( (JPanel) ActionPanel.this.getContentPane() ).setOpaque( true );
			}

			this.cancelledText1 = cancelledText1;
			this.cancelledText2 = cancelledText2;

			JPanel containerPanel = new JPanel( new GridLayout( cancelledText1 == null ? 1 : 2, 1, 5, 5 ) );

			containerPanel.setOpaque( true );
			this.add( containerPanel, BorderLayout.NORTH );

			// add the "confirmed" button
			ActionPanel.this.confirmedButton = new JButton( confirmedText );
			ActionPanel.this.confirmedButton.addActionListener( confirmListener );
			containerPanel.add( ActionPanel.this.confirmedButton );

			// add the "cancelled" button
			if ( cancelledText1 != null )
			{
				ActionPanel.this.cancelledButton = new JButton( cancelledText1 );
				ActionPanel.this.cancelledButton.addActionListener( cancelListener );
				containerPanel.add( ActionPanel.this.cancelledButton );
			}
			else
			{
				ActionPanel.this.cancelledButton = null;
			}
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			ActionPanel.this.confirmedButton.setEnabled( isEnabled );

			if ( ActionPanel.this.cancelledButton != null )
			{
				if ( this.bothDisabledOnClick )
				{
					ActionPanel.this.cancelledButton.setEnabled( isEnabled );
				}
				ActionPanel.this.cancelledButton.setText( isEnabled ? this.cancelledText1 : this.cancelledText2 );
			}
		}

		public void setBothDisabledOnClick( final boolean bothDisabledOnClick )
		{
			this.bothDisabledOnClick = bothDisabledOnClick;
		}

		public void dispose()
		{
			ActionPanel.this.confirmedButton.removeActionListener( this.confirmListener );

			if ( ActionPanel.this.cancelledButton != null )
			{
				ActionPanel.this.cancelledButton.removeActionListener( this.cancelListener );
			}
		}
	}

	public static void enableActions( boolean enable )
	{
		ActionPanel.actionsEnabled = enable;
	}
}
