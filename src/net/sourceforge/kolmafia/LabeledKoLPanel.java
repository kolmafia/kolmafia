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
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class LabeledKoLPanel extends KoLPanel
{
	private String panelTitle;
	public LabeledKoLPanel( String panelTitle, Dimension left, Dimension right )
	{
		super( left, right, panelTitle != null && !panelTitle.equals( "" ) );
		this.panelTitle = panelTitle;
	}

	public LabeledKoLPanel( String panelTitle, String confirmButton, Dimension left, Dimension right )
	{
		super( confirmButton, left, right, panelTitle != null && !panelTitle.equals( "" ) );
		this.panelTitle = panelTitle;
	}

	public LabeledKoLPanel( String panelTitle, String confirmButton, String cancelButton, Dimension left, Dimension right )
	{
		super( confirmButton, cancelButton, left, right, panelTitle != null && !panelTitle.equals( "" ) );
		this.panelTitle = panelTitle;
	}

	public void setContent( VerifiableElement [] elements, boolean bothDisabledOnClick )
	{
		super.setContent( elements, bothDisabledOnClick );

		if ( this.panelTitle != null && !this.panelTitle.equals( "" ) )
			this.add( JComponentUtilities.createLabel( this.panelTitle, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
	}

	public void actionCancelled()
	{
	}

	public void requestFocus()
	{
	}

	public boolean shouldAddStatusLabel()
	{	return false;
	}
}
