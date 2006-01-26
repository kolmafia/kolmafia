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

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.lang.ref.WeakReference;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;

/**
 * An internal class used as the basis for content panels.  This
 * class builds upon the <code>ActionVerifyPanel</code> by adding
 * a <code>setStatusMessage()</code> method.
 */

public abstract class KoLPanel extends ActionVerifyPanel implements KoLConstants
{
	protected JPanel actionStatusPanel;
	protected StatusLabel actionStatusLabel;

	protected KoLPanel( Dimension labelSize, Dimension fieldSize )
	{
		super( labelSize, fieldSize );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		super( labelSize, fieldSize, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText )
	{
		super( confirmedText );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, boolean isCenterPanel )
	{
		super( confirmedText, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText )
	{
		super( confirmedText, cancelledText );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
	{
		super( confirmedText, cancelledText, labelSize, fieldSize );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize )
	{
		super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, labelSize, fieldSize, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel, boolean isLabelPreceeding, boolean bothDisabledOnClick )
	{
		super.setContent( elements, mainPanel, eastPanel, isLabelPreceeding, bothDisabledOnClick );

		boolean shouldAddStatusLabel = elements.length != 0;
		for ( int i = 0; i < elements.length; ++i )
			shouldAddStatusLabel &= !(elements[i].getInputField() instanceof JScrollPane);

		if ( shouldAddStatusLabel )
		{
			JPanel statusContainer = new JPanel();
			statusContainer.setLayout( new BoxLayout( statusContainer, BoxLayout.Y_AXIS ) );

			actionStatusPanel = new JPanel( new BorderLayout() );
			actionStatusLabel = new StatusLabel();
			actionStatusPanel.add( actionStatusLabel, BorderLayout.SOUTH );

			statusContainer.add( actionStatusPanel );
			statusContainer.add( Box.createVerticalStrut( 20 ) );

			add( statusContainer, BorderLayout.SOUTH );
		}
	}

	private class StatusLabel extends JLabel
	{
		public StatusLabel()
		{	super( " ", JLabel.CENTER );
		}

		public void setStatusMessage( int displayState, String s )
		{
			String label = getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( s == null || label == null )
				return;

			// If you're not attempting to time-in the session, but
			// the session has timed out, then ignore all changes
			// to the attempt to time-in the session.

			if ( label.equals( "Session timed out." ) || label.equals( "Nightly maintenance." ) )
				if ( displayState == NORMAL_STATE || displayState == DISABLE_STATE )
					return;

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			if ( !s.equals( "" ) )
				setText( s );
		}
	}

	public void setStatusMessage( int displayState, String s )
	{
		if ( actionStatusLabel != null && !s.equals( "" ) )
			actionStatusLabel.setStatusMessage( displayState, s );
	}
}
