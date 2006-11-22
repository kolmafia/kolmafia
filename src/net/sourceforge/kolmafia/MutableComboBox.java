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

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.LockableListModel;

public class MutableComboBox extends JComboBox implements KoLConstants
{
	protected String currentName;
	protected String currentMatch;
	protected LockableListModel model;
	protected boolean allowAdditions;
	protected boolean prefixMatchOnly;
	protected WordBasedFilter filter;

	public MutableComboBox( LockableListModel model, boolean allowAdditions, boolean prefixMatchOnly )
	{
		super( model );

		this.model = model;
		this.filter = new WordBasedFilter( false );

		model.applyListFilter( filter );
		this.setEditable( true );

		this.allowAdditions = allowAdditions;
		this.prefixMatchOnly = prefixMatchOnly;

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
		currentMatch = (String) anObject;

		if ( currentMatch != null && currentName != null && !currentMatch.startsWith( currentName ) )
			currentName = (String) anObject;
	}

	protected synchronized void findMatch( int keyCode )
	{
		// If it wasn't the enter key that was being released,
		// then make sure that the current name is stored
		// before the key typed event is fired

		currentName = (String) getEditor().getItem();

		if ( model.contains( currentName ) )
		{
			currentMatch = currentName;
			return;
		}

		currentMatch = null;
		if ( currentName.length() == 0 || keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE )
			return;

		// Autohighlight and popup - note that this should only happen
		// for standard typing keys, or the delete and backspace keys.

		int matchCount = 0;
		Object [] currentNames = model.toArray();

		if ( prefixMatchOnly )
		{
			String lowercase = currentName.toLowerCase();
			for ( int i = 0; i < currentNames.length; ++i )
			{
				if ( ((String) currentNames[i]).toLowerCase().startsWith( lowercase ) )
				{
					++matchCount;
					currentMatch = (String) currentNames[i];
				}
			}
		}
		else
		{
			for ( int i = 0; i < currentNames.length; ++i )
			{
				if ( KoLDatabase.fuzzyMatches( (String) currentNames[i], currentName ) )
				{
					++matchCount;
					currentMatch = (String) currentNames[i];
				}
			}
		}

		if ( matchCount != 1 )
		{
			currentMatch = null;
			return;
		}

		// If this wasn't an undefined character, then the user wants auto-completion!
		// Highlight the rest of the possible name.

		setSelectedItem( currentMatch );
		getEditor().setItem( currentMatch );

		JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();
		editor.setSelectionStart( currentMatch.toLowerCase().indexOf( currentName.toLowerCase() ) + currentName.length() );
		editor.setSelectionEnd( currentMatch.length() );
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
			findMatch( KeyEvent.VK_DELETE );
		}

		public final void focusLost( FocusEvent e )
		{
			if ( currentName == null || currentName.trim().length() == 0 )
				return;

			if ( currentMatch == null )
				forceAddition();
			else
				setSelectedItem( currentMatch );
		}
	}

	protected class WordBasedFilter extends LockableListModel.ListElementFilter
	{
		protected boolean shouldFilterJunkItems;

		public WordBasedFilter( boolean shouldFilterJunkItems )
		{	this.shouldFilterJunkItems = shouldFilterJunkItems;
		}

		public boolean isVisible( Object element )
		{
			if ( shouldFilterJunkItems )
			{
				if ( element instanceof AdventureResult )
				{
					if ( junkItemList.contains( element ) )
						return false;
				}
				else if ( element instanceof ItemCreationRequest )
				{
					if ( junkItemList.contains( ((ItemCreationRequest) element).createdItem ) )
						return false;
				}
			}

			// If it's not a result, then check to see if you need to
			// filter based on its string form.

			if ( isNonResult( element ) )
			{
				if ( currentName == null || currentName.length() == 0 )
					return true;

				if ( element instanceof Map.Entry )
				{
					Map.Entry entry = (Map.Entry) element;
					return KoLDatabase.fuzzyMatches( entry.getValue().toString(), currentName );
				}

				return KoLDatabase.fuzzyMatches( element.toString(), currentName );
			}

			// In all other cases, compare the item against the
			// item name, so counts don't interfere.

			String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
			return currentName == null || currentName.length() == 0 || KoLDatabase.fuzzyMatches( name, currentName );
		}

		protected final boolean isNonResult( Object element )
		{
			if ( !(element instanceof AdventureResult) && !(element instanceof ItemCreationRequest) )
				return true;

			if ( element instanceof ItemCreationRequest )
				return false;

			if ( ((AdventureResult)element).isItem() || ((AdventureResult)element).isStatusEffect() )
				return false;

			return true;
		}
	}
}
