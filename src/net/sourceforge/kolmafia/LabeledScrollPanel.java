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

import java.awt.Color;
import java.awt.CardLayout;
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An internal class which creates a panel which displays
 * a generic scroll pane.  Note that the code for this
 * frame was lifted from the ActionVerifyPanel found in
 * the Spellcast package.
 */

public abstract class LabeledScrollPanel extends ActionPanel
{
	protected JPanel actualPanel;
	protected VerifyButtonPanel buttonPanel;
	protected JComponent scrollComponent;

	public LabeledScrollPanel( String title, JComponent scrollComponent )
	{	this( title, null, null, scrollComponent );
	}

	public LabeledScrollPanel( String title, String confirmedText, String cancelledText, JComponent scrollComponent )
	{
		this.scrollComponent = scrollComponent;

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );

		centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
		centerPanel.add( new JScrollPane( scrollComponent, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

		if ( confirmedText != null )
		{
			buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText, cancelledText );
			buttonPanel.setBothDisabledOnClick( true );
		}

		actualPanel = new JPanel();
		actualPanel.setLayout( new BorderLayout( 20, 10 ) );
		actualPanel.add( centerPanel, BorderLayout.CENTER );

		if ( buttonPanel != null )
			actualPanel.add( buttonPanel, BorderLayout.EAST );

		setLayout( new CardLayout( 10, 10 ) );
		add( actualPanel, "" );

		if ( buttonPanel != null )
			buttonPanel.setBothDisabledOnClick( true );
	}

	protected abstract void actionConfirmed();
	protected abstract void actionCancelled();

	public void setEnabled( boolean isEnabled )
	{
		scrollComponent.setEnabled( isEnabled );
		buttonPanel.setEnabled( isEnabled );
	}
}
