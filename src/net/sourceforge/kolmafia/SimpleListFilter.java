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

import java.util.Map.Entry;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class SimpleListFilter extends ListElementFilter
{
	private JTextField field;
	private JTextComponent model;

	private boolean active = true;
	private boolean strict = true;

	public SimpleListFilter( JTextField field )
	{	this.field = field;
	}

	public SimpleListFilter( MutableComboBox model )
	{	this.model = (JTextComponent) model.getEditor().getEditorComponent();;
	}

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

	private String getCurrentName()
	{
		if ( model != null )
			return model.getText();

		if ( field != null )
			return field.getText();

		return "";
	}

	public boolean isVisible( Object element )
	{
		if ( !active )
			return true;

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		if ( isNonResult( element ) )
		{
			if ( getCurrentName() == null || getCurrentName().length() == 0 )
				return true;

			if ( element instanceof Entry )
			{
				Entry entry = (Entry) element;
				return strict ? entry.getValue().toString().toLowerCase().indexOf( getCurrentName().toLowerCase() ) != -1 :
					KoLDatabase.fuzzyMatches( entry.getValue().toString(), getCurrentName() );
			}

			return strict ? element.toString().toLowerCase().indexOf( getCurrentName().toLowerCase() ) != -1 :
				KoLDatabase.fuzzyMatches( element.toString(), getCurrentName() );
		}

		// In all other cases, compare the item against the
		// item name, so counts don't interfere.

		String name = element instanceof AdventureResult ? ((AdventureResult)element).getName() : ((ItemCreationRequest)element).getName();
		return getCurrentName() == null || getCurrentName().length() == 0 ||
			(strict ? name.toLowerCase().indexOf( getCurrentName().toLowerCase() ) != -1 : KoLDatabase.fuzzyMatches( name, getCurrentName() ));
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