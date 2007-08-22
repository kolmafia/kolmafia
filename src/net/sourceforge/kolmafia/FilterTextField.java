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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public class FilterTextField extends JTextField implements ActionListener, FocusListener, ListElementFilter
{
	protected String text;

	private LockableListModel model;
	private boolean strict;

	public FilterTextField( LockableListModel model )
	{
		this.model = model;
		this.model.setFilter( this );

		this.addFocusListener( this );
		this.addKeyListener( new FilterListener() );
	}

	public void actionPerformed( ActionEvent e )
	{	update();
	}

	public void focusGained( FocusEvent e )
	{	this.selectAll();
	}

	public void focusLost( FocusEvent e )
	{
	}

	public void update()
	{
		FilterTextField.this.text = FilterTextField.this.getText().toLowerCase();

		FilterTextField.this.strict = true;
		FilterTextField.this.model.updateFilter( false );
		if ( FilterTextField.this.model.getSize() > 0 )
			return;

		FilterTextField.this.strict = false;
		FilterTextField.this.model.updateFilter( false );
	}

	public boolean isVisible( Object element )
	{
		if ( this.text == null || this.text.length() == 0 )
			return true;

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		String elementName = getResultName( element );

		if ( this.text == null || this.text.length() == 0 )
			return true;

		return this.strict ? elementName.toLowerCase().indexOf( this.text ) != -1 :
			KoLDatabase.fuzzyMatches( elementName, this.text );
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

	private class FilterListener extends KeyAdapter
	{
		public void keyReleased( KeyEvent e )
		{	update();
		}
	}
}
