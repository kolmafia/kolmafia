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

import java.awt.Component;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class MutableComboBox extends JComboBox implements KoLConstants
{
	public String currentName;
	public Object currentMatch;
	public LockableListModel model;
	public boolean allowAdditions;
	public WordBasedFilter filter;

	public MutableComboBox( LockableListModel model, boolean allowAdditions )
	{
		super( model );

		this.model = model;
		this.filter = new WordBasedFilter();
		this.setEditable( true );

		this.allowAdditions = allowAdditions;
		NameInputListener listener = new NameInputListener();

		this.getEditor().getEditorComponent().addFocusListener( listener );
		this.getEditor().getEditorComponent().addKeyListener( listener );
	}

	public void forceAddition()
	{
		if ( currentName == null || currentName.length() == 0 )
			return;

		if ( currentMatch == null && allowAdditions && !model.contains( currentName ) )
			model.add( currentName );

		setSelectedItem( currentName );
	}

	public void setSelectedItem( Object anObject )
	{
		super.setSelectedItem( anObject );
		currentMatch = anObject;

		if ( anObject == null )
			return;

		currentName = anObject.toString();
		filter.deactivate();
		model.applyListFilter( filter );
	}

	private void updateFilter()
	{
		filter.activate();
		filter.makeStrict();
		model.applyListFilter( filter );

		if ( model.getSize() != 0 )
			return;

		filter.makeFuzzy();
		model.applyListFilter( filter );
	}

	public synchronized void findMatch( int keyCode )
	{
		// If it wasn't the enter key that was being released,
		// then make sure that the current name is stored
		// before the key typed event is fired

		currentName = getEditor().getItem().toString();
		JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();

		if ( model.contains( currentName ) )
		{
			setSelectedItem( currentName );
			return;
		}

		currentMatch = null;

		if ( !allowAdditions )
		{
			if ( !isPopupVisible() )
				showPopup();

			updateFilter();
			getEditor().setItem( currentName );

			editor.setSelectionStart( currentName.length() );
			editor.setSelectionEnd( currentName.length() );
		}

		if ( !allowAdditions || currentName.length() == 0 || keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE )
			return;

		// Autohighlight and popup - note that this should only happen
		// for standard typing keys, or the delete and backspace keys.

		int matchCount = 0;
		Object [] currentNames = model.toArray();

		String lowercase = currentName.toLowerCase();
		for ( int i = 0; i < currentNames.length; ++i )
		{
			if ( currentNames[i].toString().toLowerCase().startsWith( lowercase ) )
			{
				++matchCount;
				currentMatch = currentNames[i];
			}
		}

		if ( matchCount != 1 )
		{
			currentMatch = null;
			return;
		}

		// If this wasn't an undefined character, then the user wants auto-completion!
		// Highlight the rest of the possible name.

		getEditor().setItem( currentMatch );
		editor.setSelectionStart( currentMatch.toString().toLowerCase().indexOf( currentName.toLowerCase() ) + currentName.toString().length() );
		editor.setSelectionEnd( currentMatch.toString().length() );
	}

	private class NameInputListener extends KeyAdapter implements FocusListener
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
				focusLost( null );
			else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
				findMatch( e.getKeyCode() );
		}

		public final void focusGained( FocusEvent e )
		{
			getEditor().selectAll();
		}

		public final void focusLost( FocusEvent e )
		{
			if ( isPopupVisible() )
				return;

			if ( currentName == null || currentName.trim().length() == 0 )
				return;

			if ( currentMatch == null )
				forceAddition();
			else
				setSelectedItem( currentMatch );
		}
	}

	public class WordBasedFilter extends ListElementFilter
	{
		private boolean active = true;
		private boolean strict = true;

		public void activate()
		{	active = true;
		}

		public void deactivate()
		{	active = false;
		}

		public void makeStrict()
		{	strict = true;
		}

		public void makeFuzzy()
		{	strict = false;
		}

		public boolean isVisible( Object element )
		{
			if ( !active )
				return true;

			// If it's not a result, then check to see if you need to
			// filter based on its string form.

			if ( isNonResult( element ) )
			{
				if ( currentName == null || currentName.length() == 0 )
					return true;

				if ( element instanceof Entry )
				{
					Entry entry = (Entry) element;
					return strict ? entry.getValue().toString().toLowerCase().indexOf( currentName.toLowerCase() ) != -1 :
						KoLDatabase.fuzzyMatches( entry.getValue().toString(), currentName );
				}

				return strict ? element.toString().toLowerCase().indexOf( currentName.toLowerCase() ) != -1 :
					KoLDatabase.fuzzyMatches( element.toString(), currentName );
			}

			// In all other cases, compare the item against the
			// item name, so counts don't interfere.

			String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
			return currentName == null || currentName.length() == 0 ||
				(strict ? name.toLowerCase().indexOf( currentName.toLowerCase() ) != -1 : KoLDatabase.fuzzyMatches( name, currentName ));
		}

		public final boolean isNonResult( Object element )
		{
			if ( element instanceof ItemCreationRequest )
				return false;

			if ( !(element instanceof AdventureResult) )
				return true;

			if ( ((AdventureResult)element).isItem() )
				return ((AdventureResult)element).getCount() <= 0;

			if ( ((AdventureResult)element).isStatusEffect() )
				return false;

			return true;
		}
	}
}
