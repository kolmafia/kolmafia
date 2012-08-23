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

package net.sourceforge.kolmafia.swingui.widget;

import javax.swing.JSpinner;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoHighlightSpinner
	extends JSpinner
{
	private AutoHighlightNumberEditor editor;

	public AutoHighlightSpinner()
	{
		super();
		this.editor = new AutoHighlightNumberEditor( this );
		this.setEditor( editor );
	}

	public void setValue( int value )
	{
		this.setValue( IntegerPool.get( value ) );
	}

	public void setHorizontalAlignment( int alignment )
	{
		this.editor.setHorizontalAlignment( alignment );
	}

	private class AutoHighlightNumberEditor
		extends AutoHighlightTextField
		implements ChangeListener
	{
		private boolean changing = true;

		public AutoHighlightNumberEditor( JSpinner spinner)
		{
			super();
			AutoHighlightSpinner.this.addChangeListener( this );
			this.getDocument().addDocumentListener( new AutoHighlightNumberEditorDocumentListener() );
			this.setText( "0" );
			this.changing = false;
		}

		public void stateChanged(ChangeEvent evt)
		{
			if ( this.changing )
			{
				return;
			}

			Integer value = (Integer)AutoHighlightSpinner.this.getValue();
			this.setText( String.valueOf( value ) );
		}

		private class AutoHighlightNumberEditorDocumentListener
			implements DocumentListener
		{
			public void changedUpdate( DocumentEvent e )
			{
			}

			public void insertUpdate( DocumentEvent e )
			{
				this.updateSpinnerModel();
			}

			public void removeUpdate( DocumentEvent e )
			{
				this.updateSpinnerModel();
			}

			private void updateSpinnerModel()
			{
				try
				{
					String text = AutoHighlightNumberEditor.this.getText();
					int value = StringUtilities.parseInt( text );
					AutoHighlightNumberEditor.this.changing = true;
					AutoHighlightSpinner.this.setValue( IntegerPool.get( value ) );
					AutoHighlightNumberEditor.this.changing = false;
				}
				catch ( NumberFormatException e )
				{
				}
			}
		}
	}
}
