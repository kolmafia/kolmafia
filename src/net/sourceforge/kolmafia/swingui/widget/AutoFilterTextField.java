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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.CreateItemRequest;

import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class AutoFilterTextField
	extends AutoHighlightTextField
	implements ActionListener, ListElementFilter, Runnable
{
	protected JList list;
	protected String text;
	protected LockableListModel model;
	protected boolean strict;
	protected int quantity;
	protected int price;
	protected boolean qtyChecked;
	protected boolean qtyEQ, qtyLT, qtyGT;
	protected boolean asChecked;
	protected boolean asEQ, asLT, asGT;
	protected boolean notChecked;

	private static final Pattern QTYSEARCH_PATTERN = Pattern.compile(
		"\\s*#\\s*([<=>]+)\\s*([\\d,]+)\\s*" );

	private static final Pattern ASSEARCH_PATTERN = Pattern.compile(
		"\\s*\\p{Sc}\\s*([<=>]+)\\s*([\\d,]+)\\s*" );

	private static final Pattern NOTSEARCH_PATTERN = Pattern.compile(
		"\\s*!\\s*=\\s*(.+)\\s*" );

	public AutoFilterTextField( final JList list )
	{
		this.setList( list );
		this.addKeyListener( new FilterListener() );
		
		// Make this look like a normal search field on OS X.
		// Note that the field MUST NOT be forced to a height other than its
		// preferred height; that produces some ugly visual glitches.
		this.putClientProperty( "JTextField.variant", "search" );
	}

	public AutoFilterTextField( final JList list, Object initial )
	{
		this( list );

		if ( initial != null )
		{
			this.setText( initial.toString() );
		}
	}

	public AutoFilterTextField( LockableListModel displayModel )
	{
		this( new JList( displayModel ) );
	}

	public void setList( final JList list )
	{
		this.list = list;
		this.model = (LockableListModel) list.getModel();
		this.model.setFilter( this );
		this.list.clearSelection();
	}

	public void actionPerformed( final ActionEvent e )
	{
		this.update();
	}

	private String tempText = null;
	
	@Override
	public void setText( final String text )
	{
		this.tempText = text;
		if ( !SwingUtilities.isEventDispatchThread() )
		{
			try
			{
				SwingUtilities.invokeAndWait( this );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}
		else {
			this.run();
		}
	}
	
	public void run()
	{
		super.setText( this.tempText );
		this.update();
	}

	public void update()
	{
		try
		{
			this.model.setFiltering( true );
			
			this.qtyChecked = false;
			this.asChecked = false;
			this.notChecked = false;
			this.text = this.getText().toLowerCase();

			Matcher mqty = AutoFilterTextField.QTYSEARCH_PATTERN.matcher( this.text );
			if ( mqty.find() )
			{
				this.qtyChecked = true;
				this.quantity = StringUtilities.parseInt( mqty.group( 2 ) );
				String op = mqty.group( 1 );
				this.qtyEQ = op.indexOf( "=" ) != -1;
				this.qtyLT = op.indexOf( "<" ) != -1;
				this.qtyGT = op.indexOf( ">" ) != -1;
				this.text = mqty.replaceFirst( "" );
			}

			Matcher mas = AutoFilterTextField.ASSEARCH_PATTERN.matcher( this.text );
			if ( mas.find() )
			{
				this.asChecked = true;
				this.price = StringUtilities.parseInt( mas.group( 2 ) );
				String op = mas.group( 1 );
				this.asEQ = op.indexOf( "=" ) != -1;
				this.asLT = op.indexOf( "<" ) != -1;
				this.asGT = op.indexOf( ">" ) != -1;
				this.text = mas.replaceFirst( "" );
			}

			Matcher mnot = AutoFilterTextField.NOTSEARCH_PATTERN.matcher( this.text );
			if ( mnot.find() )
			{
				this.notChecked = true;
				this.text = mnot.group( 1 );
			}

			this.strict = true;
			this.model.updateFilter( false );

			if ( this.model.getSize() == 0 )
			{
				this.strict = false;
				this.model.updateFilter( false );
			}

			if ( this.model.getSize() == 1 )
			{
				this.list.setSelectedIndex( 0 );
			}
			else
			{
				this.list.clearSelection();
			}
		}
		finally
		{
			this.model.setFiltering( false );
			if ( this.model.size() > 0 )
			{
				this.model.fireContentsChanged( this.model, 0, this.model.size() - 1 );
			}
		}
	}

	public boolean isVisible( final Object element )
	{
		if ( this.qtyChecked )
		{
			int qty = AutoFilterTextField.getResultQuantity( element );
			if ( ( qty == this.quantity && !this.qtyEQ ) ||
			     ( qty < this.quantity && !this.qtyLT ) ||
			     ( qty > this.quantity && !this.qtyGT ) )
			{
				return false;
			}
		}

		if ( this.asChecked )
		{
			int as = AutoFilterTextField.getResultPrice( element );
			if ( ( as == this.price && !this.asEQ ) ||
			     ( as < this.price && !this.asLT ) ||
			     ( as > this.price && !this.asGT ) )
			{
				return false;
			}
		}

		if ( this.text == null || this.text.length() == 0 )
		{
			return true;
		}

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		String elementName = AutoFilterTextField.getResultName( element );

		if ( this.notChecked )
		{
			return elementName.indexOf( this.text ) == -1;
		}

		return this.strict ? elementName.indexOf( this.text ) != -1 :
			StringUtilities.fuzzyMatches( elementName, this.text );
	}

	public static final String getResultName( final Object element )
	{
		if ( element == null )
		{
			return "";
		}

		if ( element instanceof AdventureResult )
		{
			return ( (AdventureResult) element ).getName().toLowerCase();
		}
		if ( element instanceof CreateItemRequest )
		{
			return ( (CreateItemRequest) element ).getName().toLowerCase();
		}
		if ( element instanceof Concoction )
		{
			return ( (Concoction) element ).getName().toLowerCase();
		}
		if ( element instanceof SoldItem )
		{
			return ( (SoldItem) element ).getItemName().toLowerCase();
		}
		if ( element instanceof LowerCaseEntry )
		{
			return ( (LowerCaseEntry) element ).getLowerCase();
		}
		if ( element instanceof KoLAdventure )
		{
			return ( (KoLAdventure) element ).toLowerCaseString();
		}
		if ( element instanceof Monster )
		{
			return ( (Monster) element ).toLowerCaseString();
		}

		return element.toString();
	}

	public static final int getResultPrice( final Object element )
	{
		if ( element == null )
		{
			return -1;
		}

		if ( element instanceof AdventureResult )
		{
			return ItemDatabase.getPriceById( ( (AdventureResult) element ).getItemId() );
		}

		return -1;
	}


	public static final int getResultQuantity( final Object element )
	{
		if ( element == null )
		{
			return -1;
		}

		if ( element instanceof AdventureResult )
		{
			return ( (AdventureResult) element ).getCount();
		}
		if ( element instanceof CreateItemRequest )
		{
			return ( (CreateItemRequest) element ).getQuantityPossible();
		}
		if ( element instanceof Concoction )
		{
			return ( (Concoction) element ).getAvailable();
		}
		if ( element instanceof SoldItem )
		{
			return ( (SoldItem) element ).getQuantity();
		}
		if ( element instanceof LowerCaseEntry )
		{	// no meaningful integer fields
			return -1;
		}
		if ( element instanceof KoLAdventure )
		{
			return StringUtilities.parseInt( ( (KoLAdventure) element ).getAdventureId() );
		}

		return -1;
	}

	private class FilterListener
		extends KeyAdapter
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
			AutoFilterTextField.this.update();
		}
	}
}
