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

package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JEditorPane;
import javax.swing.JScrollBar;

import net.java.dev.spellcast.utilities.ChatBuffer;

public class StickyListener
	implements AdjustmentListener
{
	private ChatBuffer buffer;
	private JEditorPane editor;
	private int tolerance;
	private boolean currentlySticky;

	public StickyListener( ChatBuffer buffer, JEditorPane editor, int tolerance )
	{
		this.buffer = buffer;
		this.editor = editor;
		this.tolerance = tolerance;
		this.currentlySticky = true;
	}

	public void adjustmentValueChanged( AdjustmentEvent event )
	{
		JScrollBar bar = (JScrollBar) event.getSource();

		int value = event.getValue();
		int knob = bar.getVisibleAmount();
		int max = bar.getMaximum();

		boolean shouldBeSticky = value + knob > max - tolerance;

		if ( this.currentlySticky != shouldBeSticky )
		{
			this.currentlySticky = shouldBeSticky;
			buffer.setSticky( this.editor, shouldBeSticky );
		}
	}
}