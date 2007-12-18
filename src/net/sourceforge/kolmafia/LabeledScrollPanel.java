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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class LabeledScrollPanel
	extends ActionPanel
	implements KoLConstants
{
	public JPanel actualPanel;
	public JPanel centerPanel;

	public JPanel eastPanel;
	public VerifyButtonPanel buttonPanel;
	public JComponent scrollComponent;

	public LabeledScrollPanel( final String title, final JComponent scrollComponent )
	{
		this( title, null, null, scrollComponent );
	}

	public LabeledScrollPanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent )
	{
		this( title, confirmedText, cancelledText, scrollComponent, true );
	}

	public LabeledScrollPanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent, final boolean isRootPane )
	{
		this.scrollComponent = scrollComponent;

		this.centerPanel = new JPanel( new BorderLayout() );

		if ( !title.equals( "" ) )
		{
			this.centerPanel.add( JComponentUtilities.createLabel(
				title, SwingConstants.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
		}

		this.centerPanel.add( new SimpleScrollPane( scrollComponent ), BorderLayout.CENTER );
		this.actualPanel = new JPanel( new BorderLayout( 20, 10 ) );
		this.actualPanel.add( this.centerPanel, BorderLayout.CENTER );

		this.eastPanel = new JPanel( new BorderLayout() );

		if ( confirmedText != null )
		{
			this.buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText, cancelledText );
			this.buttonPanel.setBothDisabledOnClick( true );

			this.eastPanel.add( this.buttonPanel, BorderLayout.NORTH );
			this.actualPanel.add( this.eastPanel, BorderLayout.EAST );
		}

		JPanel containerPanel = new JPanel( new CardLayout( 10, 10 ) );
		containerPanel.add( this.actualPanel, "" );

		if ( isRootPane )
		{
			this.getContentPane().setLayout( new BorderLayout() );
			this.getContentPane().add( containerPanel, BorderLayout.CENTER );
		}
		else
		{
			this.setLayout( new BorderLayout() );
			this.add( containerPanel, BorderLayout.CENTER );
		}

		( (JPanel) this.getContentPane() ).setOpaque( true );
		StaticEntity.registerPanel( this );

		this.contentSet = true;
	}

	public void setEnabled( final boolean isEnabled )
	{
		if ( this.scrollComponent == null || this.buttonPanel == null )
		{
			return;
		}

		this.scrollComponent.setEnabled( isEnabled );
		this.buttonPanel.setEnabled( isEnabled );
	}

	public void actionConfirmed()
	{
	}

	public void actionCancelled()
	{
	}

	public void dispose()
	{
		StaticEntity.unregisterPanel( this );
		super.dispose();
	}
}
