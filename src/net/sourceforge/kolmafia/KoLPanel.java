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

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;

/**
 * An internal class used as the basis for content panels.  This
 * class builds upon the <code>ActionVerifyPanel</code> by adding
 * a <code>setStatusMessage()</code> method.
 */

public abstract class KoLPanel extends ActionVerifyPanel
{
	private JPanel actionStatusPanel;
	private JLabel actionStatusLabel;

	protected KoLPanel( Dimension labelSize, Dimension fieldSize )
	{	super( labelSize, fieldSize );
	}

	protected KoLPanel( Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{	super( labelSize, fieldSize, isCenterPanel );
	}

	protected KoLPanel( String confirmedText )
	{	super( confirmedText );
	}

	protected KoLPanel( String confirmedText, String cancelledText )
	{	super( confirmedText, cancelledText );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
	{
		super( confirmedText, cancelledText, labelSize, fieldSize );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize )
	{
		super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, labelSize, fieldSize, isCenterPanel );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize, isCenterPanel );
	}

	protected void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel, boolean isLabelPreceeding, boolean bothDisabledOnClick )
	{
		super.setContent( elements, mainPanel, eastPanel, isLabelPreceeding, bothDisabledOnClick );

		if ( elements.length != 0 )
		{
			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			add( actionStatusPanel, BorderLayout.SOUTH );
		}
	}

	public void setStatusMessage( int displayState, String s )
	{
		if ( actionStatusLabel != null && !s.equals( "" ) )
			actionStatusLabel.setText( s );
	}
}
