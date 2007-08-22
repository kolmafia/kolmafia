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

public class MutableComboBox extends JComboBox implements ListElementFilter
{
	private int currentIndex = -1;

	private String currentName;
	private String matchString;
	private Object currentMatch;
	private LockableListModel model;
	private boolean allowAdditions;

	private boolean active, strict;

	public MutableComboBox( LockableListModel model, boolean allowAdditions )
	{
		this.model = model;
		this.setModel( this.model );

		this.model.setFilter( this );
		this.setEditable( true );

		this.allowAdditions = allowAdditions;
		NameInputListener listener = new NameInputListener();

		this.addItemListener( listener );
		this.getEditor().getEditorComponent().addFocusListener( listener );
		this.getEditor().getEditorComponent().addKeyListener( listener );
	}

	public void forceAddition()
	{
		if ( this.currentName == null || this.currentName.length() == 0 )
			return;

		if ( this.currentMatch == null && this.allowAdditions && !this.model.contains( this.currentName ) )
			this.model.add( this.currentName );

		this.setSelectedItem( this.currentName );
	}

	private void update()
	{
		if ( this.currentName == null )
			return;

		this.active = true;
		this.matchString = this.currentName.toLowerCase();

		this.strict = true;
		this.model.updateFilter( false );

		if ( this.model.getSize() > 0 )
			return;

		this.strict = false;
		this.model.updateFilter( false );
	}

	public synchronized void findMatch( int keyCode )
	{
		this.currentIndex = -1;
		this.currentName = this.getEditor().getItem().toString();

		if ( !allowAdditions && this.model.contains( this.currentName ) )
		{
			this.setSelectedItem( this.currentName );
			return;
		}

		this.currentMatch = null;
		this.update();

		if ( allowAdditions )
			return;

		((JTextComponent)this.getEditor().getEditorComponent()).setText( this.currentName );

		if ( !this.isPopupVisible() )
			this.showPopup();
	}

	public boolean isVisible( Object element )
	{
		if ( !this.active )
			return true;

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		if ( this.matchString == null || this.matchString.length() == 0 )
			return true;

		String elementName = element.toString().toLowerCase();
		return this.allowAdditions ? elementName.startsWith( this.matchString ) :
			this.strict ? elementName.indexOf( this.matchString ) != -1 :
			KoLDatabase.fuzzyMatches( elementName, this.matchString );
	}

	private class NameInputListener extends KeyAdapter implements FocusListener, ItemListener
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				if ( currentIndex + 1 < model.getSize() )
					currentMatch = model.getElementAt( ++currentIndex );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				if ( model.getSize() > 0 && currentIndex > 0 )
					currentMatch = model.getElementAt( --currentIndex );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
				this.focusLost( null );
			else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
				findMatch( e.getKeyCode() );
		}

		public final void itemStateChanged( ItemEvent e )
		{
			currentIndex = -1;
			currentMatch = getSelectedItem();

			if ( currentMatch == null )
				return;

			currentName = currentMatch.toString();

			if ( !isPopupVisible() )
			{
				active = false;
				model.updateFilter( false );
			}
		}

		public final void focusGained( FocusEvent e )
		{
			currentIndex = -1;
			getEditor().selectAll();
		}

		public final void focusLost( FocusEvent e )
		{
			currentIndex = -1;

			if ( currentMatch != null )
				setSelectedItem( currentMatch );
			else if ( currentName != null && currentName.trim().length() != 0 )
				forceAddition();
		}
	}
}
