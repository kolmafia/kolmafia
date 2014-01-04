/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.table;

import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import javax.swing.table.DefaultTableModel;

import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class ListWrapperTableModel
	extends DefaultTableModel
	implements ListDataListener
{
	private final String[] headers;
	private final Class[] types;
	private final boolean[] editable;

	protected LockableListModel listModel;

	public ListWrapperTableModel( final String[] headers, final Class[] types, final boolean[] editable,
		final LockableListModel listModel )
	{
		super( 0, headers.length );

		this.listModel = listModel;
		this.headers = headers;
		this.types = types;
		this.editable = editable;

		SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				for ( int i = 0; i < listModel.size(); ++i )
				{
					ListWrapperTableModel.this.insertRow(
						i, ListWrapperTableModel.this.constructVector( listModel.get( i ) ) );
				}
			}
		} );

		listModel.addListDataListener( this );
	}

	@Override
	public String getColumnName( final int index )
	{
		return index < 0 || index >= this.headers.length ? "" : this.headers[ index ];
	}

	@Override
	public Class getColumnClass( final int column )
	{
		return column < 0 || column >= this.types.length ? Object.class : this.types[ column ];
	}

	public abstract Vector constructVector( Object o );

	@Override
	public boolean isCellEditable( final int row, final int column )
	{
		return column < 0 || column >= this.editable.length ? false : this.editable[ column ];
	}

	/**
	 * Called whenever contents have been added to the original list; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalAdded( final ListDataEvent e )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				LockableListModel source = (LockableListModel) e.getSource();
				int index0 = e.getIndex0();
				int index1 = e.getIndex1();

				for ( int i = index0; i <= index1; ++i )
				{
					ListWrapperTableModel.this.insertRow(
						i, ListWrapperTableModel.this.constructVector( source.get( i ) ) );
				}
			}
		} );
	}

	/**
	 * Called whenever contents have been removed from the original list; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void intervalRemoved( final ListDataEvent e )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				int index0 = e.getIndex0();
				int index1 = e.getIndex1();

				for ( int i = index1; i >= index0; --i )
				{
					ListWrapperTableModel.this.removeRow( i );
				}
			}
		} );
	}

	/**
	 * Called whenever contents in the original list have changed; a function required by every
	 * <code>ListDataListener</code>.
	 *
	 * @param e the <code>ListDataEvent</code> that triggered this function call
	 */

	public void contentsChanged( final ListDataEvent e )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			public void run()
			{
				LockableListModel source = (LockableListModel) e.getSource();
				int index0 = e.getIndex0();
				int index1 = e.getIndex1();

				if ( index0 < 0 || index1 < 0 )
				{
					return;
				}

				int rowCount = ListWrapperTableModel.this.getRowCount();

				for ( int i = index1; i >= index0; --i )
				{
					if ( source.size() < i )
					{
						ListWrapperTableModel.this.removeRow( i );
					}
					else if ( i > rowCount )
					{
						ListWrapperTableModel.this.insertRow(
							rowCount, ListWrapperTableModel.this.constructVector( source.get( i ) ) );
					}
					else
					{
						ListWrapperTableModel.this.removeRow( i );
						ListWrapperTableModel.this.insertRow(
							i, ListWrapperTableModel.this.constructVector( source.get( i ) ) );
					}
				}
			}
		} );
	}
}
