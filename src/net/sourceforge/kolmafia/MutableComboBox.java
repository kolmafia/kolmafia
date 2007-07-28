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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import net.java.dev.spellcast.utilities.LockableListModel;

public class MutableComboBox extends JComboBox implements KoLConstants
{
	public String currentName;
	public Object currentMatch;
	public LockableListModel model;
	public boolean allowAdditions;
	public SimpleListFilter filter;

	public MutableComboBox( LockableListModel model, boolean allowAdditions )
	{
		super( model );

		this.model = model;
		this.filter = new SimpleListFilter( this );
		this.setEditable( true );

		this.allowAdditions = allowAdditions;
		NameInputListener listener = new NameInputListener();

		this.getEditor().getEditorComponent().addFocusListener( listener );
		this.getEditor().getEditorComponent().addKeyListener( listener );
	}

	public String getCurrentName()
	{	return this.currentName;
	}

	public void forceAddition()
	{
		if ( this.currentName == null || this.currentName.length() == 0 )
			return;

		if ( this.currentMatch == null && this.allowAdditions && !this.model.contains( this.currentName ) )
			this.model.add( this.currentName );

		this.setSelectedItem( this.currentName );
	}

	public void setSelectedItem( Object anObject )
	{
		super.setSelectedItem( anObject );
		this.currentMatch = anObject;

		if ( anObject == null )
			return;

		this.currentName = anObject.toString();

		if ( !this.isPopupVisible() )
		{
			this.filter.deactivate();
			this.model.applyListFilter( this.filter );
		}
	}

	private void updateFilter()
	{
		this.filter.activate();
		this.filter.makeStrict();
		this.model.applyListFilter( this.filter );

		if ( this.model.getSize() != 0 )
			return;

		this.filter.makeFuzzy();
		this.model.applyListFilter( this.filter );
	}

	public synchronized void findMatch( int keyCode )
	{
		// If it wasn't the enter key that was being released,
		// then make sure that the current name is stored
		// before the key typed event is fired

		this.currentName = this.getEditor().getItem().toString();
		JTextComponent editor = (JTextComponent) this.getEditor().getEditorComponent();

		if ( this.model.contains( this.currentName ) )
		{
			this.setSelectedItem( this.currentName );
			return;
		}

		this.currentMatch = null;

		if ( !this.allowAdditions )
		{
			if ( !this.isPopupVisible() )
				this.showPopup();

			this.updateFilter();
			this.getEditor().setItem( this.currentName );

			editor.setSelectionStart( this.currentName.length() );
			editor.setSelectionEnd( this.currentName.length() );
			return;
		}

		if ( this.currentName.length() == 0 || keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE )
			return;

		// Autohighlight and popup - note that this should only happen
		// for standard typing keys, or the delete and backspace keys.

		Object matchTest;
		int matchCount = 0;
		String lowercase = this.currentName.toLowerCase();

		for ( int i = 0; i < this.model.getSize(); ++i )
		{
			matchTest = this.model.getElementAt(i);
			if ( matchTest.toString().toLowerCase().startsWith( lowercase ) )
			{
				++matchCount;
				this.currentMatch = matchTest;
			}
		}

		if ( matchCount != 1 )
		{
			this.currentMatch = null;
			return;
		}

		// If this wasn't an undefined character, then the user wants auto-completion!
		// Highlight the rest of the possible name.

		this.getEditor().setItem( this.currentMatch );
		editor.setSelectionStart( this.currentMatch.toString().toLowerCase().indexOf( this.currentName.toLowerCase() ) + this.currentName.toString().length() );
		editor.setSelectionEnd( this.currentMatch.toString().length() );
	}

	private class NameInputListener extends KeyAdapter implements FocusListener
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
				this.focusLost( null );
			else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
				MutableComboBox.this.findMatch( e.getKeyCode() );
		}

		public final void focusGained( FocusEvent e )
		{
			MutableComboBox.this.getEditor().selectAll();
		}

		public final void focusLost( FocusEvent e )
		{
			if ( MutableComboBox.this.isPopupVisible() )
				return;

			if ( MutableComboBox.this.currentName == null || MutableComboBox.this.currentName.trim().length() == 0 )
				return;

			if ( MutableComboBox.this.currentMatch == null )
				MutableComboBox.this.forceAddition();
			else
				MutableComboBox.this.setSelectedItem( MutableComboBox.this.currentMatch );
		}
	}
}
