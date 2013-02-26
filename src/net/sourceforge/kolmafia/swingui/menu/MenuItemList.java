/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.menu;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JSeparator;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class MenuItemList
	extends JMenu
	implements ListDataListener
{
	private int headerCount;
	private ArrayList dataValues;
	private LockableListModel model;

	public MenuItemList( final String title, final LockableListModel model )
	{
		super( title );

		this.model = model;
		this.dataValues = new ArrayList();

		// Add the headers to the list of items which
		// need to be added.

		JComponent[] headers = this.getHeaders();

		for ( int i = 0; i < headers.length; ++i )
		{
			this.add( headers[ i ] );
		}

		// Add a separator between the headers and the
		// elements displayed in the list.  Also go
		// ahead and initialize the header count.

		if ( headers.length == 0 )
		{
			this.headerCount = 0;
		}
		else
		{
			this.add( new JSeparator() );
			this.headerCount = headers.length + 1;
		}

		// Now, add everything that's contained inside of
		// the current list.

		for ( int i = 0; i < model.size(); ++i )
		{
			this.dataValues.add( model.get( i ) );
			this.add( this.constructMenuItem( model.get( i ) ) );
		}

		// Add this as a listener to the list so that the menu gets
		// updated whenever the list updates.

		model.addListDataListener( this );
	}

	public void dispose()
	{
		if ( this.dataValues != null )
		{
			this.dataValues.clear();
			this.dataValues = null;
		}

		if ( this.model != null )
		{
			this.model.removeListDataListener( this );
			this.model = null;
		}
	}

	public abstract JComponent[] getHeaders();

	public abstract JComponent constructMenuItem( Object o );

	/**
	 * Called whenever contents have been added to the original list; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalAdded( final ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();
		int index1 = e.getIndex1();

		for ( int i = index0; i <= index1; ++i )
		{
			Object item = source.get( i );

			this.dataValues.add( i, item );
			this.add( this.constructMenuItem( item ), i + this.headerCount );
		}

		this.validate();
	}

	/**
	 * Called whenever contents have been removed from the original list; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalRemoved( final ListDataEvent e )
	{
		int index0 = e.getIndex0();
		int index1 = e.getIndex1();

		for ( int i = index1; i >= index0; --i )
		{
			this.dataValues.remove( i );
			this.remove( i + this.headerCount );
		}

		this.validate();
	}

	/**
	 * Called whenever contents in the original list have changed; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void contentsChanged( final ListDataEvent e )
	{
		for ( int i = 0; i < this.dataValues.size(); ++i )
		{
			this.remove( this.headerCount );
		}

		this.dataValues.clear();
		LockableListModel source = (LockableListModel) e.getSource();

		for ( int i = 0; i < source.size(); ++i )
		{
			this.dataValues.add( i, source.get( i ) );
			this.add( this.constructMenuItem( source.get( i ) ), i + this.headerCount );
		}
	}
}
