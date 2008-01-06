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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class MutableComboBox
	extends JComboBox
	implements ListElementFilter
{
	private int currentIndex = -1;
	private boolean isRecentFocus = false;

	private String currentName;
	private String matchString;
	public Object currentMatch;
	private LockableListModel model;
	private boolean allowAdditions;

	private boolean active, strict;
	private final JTextComponent editor;

	public MutableComboBox( final LockableListModel model, final boolean allowAdditions )
	{
		this.setModel( model );

		this.setEditable( true );

		this.allowAdditions = allowAdditions;
		NameInputListener listener = new NameInputListener();

		this.addItemListener( listener );
		this.editor = (JTextComponent) this.getEditor().getEditorComponent();

		this.editor.addFocusListener( listener );
		this.editor.addKeyListener( listener );
	}

	public void setModel( final LockableListModel model )
	{
		this.model = model;
		super.setModel( this.model );
		this.model.setFilter( this );
		this.model.setSelectedItem( null );
	}

	public void forceAddition()
	{
		if ( this.currentName == null || this.currentName.length() == 0 )
		{
			return;
		}

		if ( this.currentMatch == null && this.allowAdditions && !this.model.contains( this.currentName ) )
		{
			this.model.add( this.currentName );
		}

		this.setSelectedItem( this.currentName );
	}

	private void update()
	{
		if ( this.currentName == null )
		{
			return;
		}

		this.isRecentFocus = false;
		this.currentIndex = -1;
		this.model.setSelectedItem( null );

		this.active = true;
		this.matchString = this.currentName.toLowerCase();

		this.strict = true;
		this.model.updateFilter( false );

		if ( this.model.getSize() > 0 )
		{
			return;
		}

		this.strict = false;
		this.model.updateFilter( false );
	}

	public synchronized void findMatch( final int keyCode )
	{
		this.currentName = this.getEditor().getItem().toString();

		if ( !this.allowAdditions && this.model.contains( this.currentName ) )
		{
			this.setSelectedItem( this.currentName );
			return;
		}

		this.currentMatch = null;
		this.update();

		if ( this.allowAdditions )
		{
			if ( this.model.getSize() != 1 || keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE )
			{
				this.editor.setText( this.currentName );
				return;
			}

			this.currentMatch = this.model.getElementAt( 0 );
			this.matchString = this.currentMatch.toString().toLowerCase();

			this.editor.setText( this.currentMatch.toString() );
			this.editor.setSelectionStart( this.currentName.length() );
			this.editor.setSelectionEnd( this.matchString.length() );

			return;
		}

		this.editor.setText( this.currentName );
		if ( !this.isPopupVisible() )
		{
			this.showPopup();
		}
	}

	public boolean isVisible( final Object element )
	{
		if ( !this.active )
		{
			return true;
		}

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		if ( this.matchString == null || this.matchString.length() == 0 )
		{
			return true;
		}

		String elementName = element.toString().toLowerCase();
		return this.allowAdditions ? elementName.startsWith( this.matchString ) : this.strict ? elementName.indexOf( this.matchString ) != -1 : KoLDatabase.fuzzyMatches(
			elementName, this.matchString );
	}

	private class NameInputListener
		extends KeyAdapter
		implements FocusListener, ItemListener
	{
		public void keyReleased( final KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				if ( !MutableComboBox.this.isRecentFocus && MutableComboBox.this.currentIndex + 1 < MutableComboBox.this.model.getSize() )
				{
					MutableComboBox.this.currentMatch =
						MutableComboBox.this.model.getElementAt( ++MutableComboBox.this.currentIndex );
				}

				MutableComboBox.this.isRecentFocus = false;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				if ( !MutableComboBox.this.isRecentFocus && MutableComboBox.this.model.getSize() > 0 && MutableComboBox.this.currentIndex > 0 )
				{
					MutableComboBox.this.currentMatch =
						MutableComboBox.this.model.getElementAt( --MutableComboBox.this.currentIndex );
				}

				MutableComboBox.this.isRecentFocus = false;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
			{
				this.focusLost( null );
			}
			else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
			{
				MutableComboBox.this.findMatch( e.getKeyCode() );
			}
		}

		public final void itemStateChanged( final ItemEvent e )
		{
			MutableComboBox.this.currentMatch = MutableComboBox.this.getSelectedItem();

			if ( MutableComboBox.this.currentMatch == null )
			{
				return;
			}

			MutableComboBox.this.currentName = MutableComboBox.this.currentMatch.toString();

			if ( !MutableComboBox.this.isPopupVisible() )
			{
				MutableComboBox.this.active = false;
				MutableComboBox.this.model.updateFilter( false );
			}
		}

		public final void focusGained( final FocusEvent e )
		{
			MutableComboBox.this.getEditor().selectAll();

			MutableComboBox.this.isRecentFocus = true;
			MutableComboBox.this.currentIndex = MutableComboBox.this.model.getSelectedIndex();
		}

		public final void focusLost( final FocusEvent e )
		{
			if ( MutableComboBox.this.currentMatch != null )
			{
				MutableComboBox.this.setSelectedItem( MutableComboBox.this.currentMatch );
			}
			else if ( MutableComboBox.this.currentName != null && MutableComboBox.this.currentName.trim().length() != 0 )
			{
				MutableComboBox.this.forceAddition();
			}
		}
	}
}
