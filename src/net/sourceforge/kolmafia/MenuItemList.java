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

import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;


/**
 * A special class which renders the menu holding the list of menu items
 * listening to a lockable list model.
 */

public abstract class MenuItemList extends JMenu implements ListDataListener
{
	private int headerCount;
	private ArrayList dataValues;

	public MenuItemList( String title, LockableListModel model )
	{
		super( title );
		dataValues = new ArrayList();

		// Add the headers to the list of items which
		// need to be added.

		JComponent [] headers = getHeaders();

		for ( int i = 0; i < headers.length; ++i )
			this.add( headers[i] );

		// Add a separator between the headers and the
		// elements displayed in the list.  Also go
		// ahead and initialize the header count.

		this.add( new JSeparator() );
		this.headerCount = headers.length + 1;

		// Now, add everything that's contained inside of
		// the current list.

		for ( int i = 0; i < model.size(); ++i )
		{
			dataValues.add( model.get(i) );
			this.add( constructMenuItem( model.get(i) ) );
		}

		// Add this as a listener to the list so that the menu gets
		// updated whenever the list updates.

		model.addListDataListener( this );
	}

	public abstract JComponent [] getHeaders();
	public abstract JComponent constructMenuItem( Object o );

	/**
	 * Called whenever contents have been added to the original list; a
	 * function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalAdded( ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();  int index1 = e.getIndex1();

		if ( index1 >= source.size() )
			return;

		for ( int i = index0; i <= index1; ++i )
		{
			Object item = source.get(i);
			if ( !dataValues.contains( item ) )
			{
				dataValues.add( i, item );
				add( constructMenuItem( item ), i + headerCount );
			}
		}

		validate();
	}

	/**
	 * Called whenever contents have been removed from the original list;
	 * a function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalRemoved( ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();  int index1 = e.getIndex1();

		if ( index0 + headerCount >= getMenuComponentCount() || source.size() + headerCount == getMenuComponentCount() )
			return;

		for ( int i = index1; i >= index0; --i )
		{
			Object item = source.get(i);
			int itemIndex = dataValues.indexOf( item );
			if ( itemIndex != -1 )
			{
				dataValues.remove( itemIndex );
				remove( itemIndex + headerCount );
			}
		}

		validate();
	}

	/**
	 * Called whenever contents in the original list have changed; a
	 * function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public void contentsChanged( ListDataEvent e )
	{
	}
}
