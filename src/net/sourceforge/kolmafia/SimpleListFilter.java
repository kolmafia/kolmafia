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

import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public class SimpleListFilter implements ListElementFilter
{
	private JTextField field;
	private MutableComboBox model;

	private boolean active = true;
	private boolean strict = true;

	public SimpleListFilter( JTextField field )
	{	this.field = field;
	}

	public SimpleListFilter( MutableComboBox model )
	{	this.model = model;
	}

	public void activate()
	{	this.active = true;
	}

	public void deactivate()
	{	this.active = false;
	}

	public void makeStrict()
	{	this.strict = true;
	}

	public void makeFuzzy()
	{	this.strict = false;
	}

	private String getCurrentName()
	{
		if ( this.model != null )
			return this.model.getCurrentName();

		if ( this.field != null )
			return this.field.getText();

		return "";
	}

	public boolean isVisible( Object element )
	{
		if ( !this.active )
			return true;

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		String elementName = getResultName( element );
		String currentName = this.getCurrentName();

		if ( currentName == null || currentName.length() == 0 )
			return true;

		if ( this.strict )
			return elementName.toLowerCase().indexOf( currentName.toLowerCase() ) != -1;

		return KoLDatabase.fuzzyMatches( elementName, currentName );
	}

	public static final String getResultName( Object element )
	{
		if ( element == null )
			return "";

		if ( element instanceof AdventureResult )
			return ((AdventureResult)element).getName();
		if ( element instanceof ItemCreationRequest )
			return ((ItemCreationRequest)element).getName();
		if ( element instanceof Concoction )
			return ((Concoction)element).getName();
		if ( element instanceof SoldItem )
			return ((SoldItem)element).getItemName();

		if ( element instanceof Entry )
			return ((Entry)element).getValue().toString();

		return element.toString();
	}
}