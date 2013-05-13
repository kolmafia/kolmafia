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

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;

/**
 * Like an MRUList, but maintains a list of "default" settings at the bottom under a JSeparator.
 */
public class PartialMRUList
	extends ScriptMRUList
{
	private final String[] defaultList;
	private final ComboSeparatorsRenderer renderer = new ComboSeparatorsRenderer( new DefaultListCellRenderer() )
	{
		@Override
		protected boolean addSeparatorAfter( JList list, Object value, int index )
		{
			if ( PartialMRUList.this.maxMRU < 0 )
				return false;
			return index == PartialMRUList.this.maxMRU - 1;
		}
	};

	/**
	 * @param pList
	 * @param pLen
	 */
	public PartialMRUList( String pList, String pLen, String[] defaultList )
	{
		super( pList, pLen );
		this.defaultList = defaultList;
	}

	/*
	 * Override the update method in order to create a "hybrid" MRU-plus-default list. Note to implementers: calling
	 * this method alters the combobox's default renderer. (non-Javadoc)
	 * @see net.sourceforge.kolmafia.swingui.menu.ScriptMRUList#updateJComboData(javax.swing.JComboBox)
	 */
	@Override
	public void updateJComboData( JComboBox jcb )
	{
		if ( !isInit )
		{
			init();
		}
		if ( jcb.getRenderer() != this.renderer )
		{
			jcb.setRenderer( this.renderer );
		}
		jcb.removeAllItems();

		int count = mruList.size();
		if ( count >= 1 )
		{
			for ( Object ob : mruList )
			{
				jcb.addItem( ob );
			}
		}

		for ( String str : defaultList )
		{
			jcb.addItem( str );
		}
		jcb.setSelectedIndex( 0 );
	}

	/**
	 * Adapted from http://www.jroller.com/santhosh/entry/jcombobox_items_with_separators 
	 * Last Access: 5/11/13
	 * Distributed under GNU Lesser GPL. 
	 * Copyright (C) 2005 Santhosh Kumar T
	 * 
	 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
	 * License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option) any
	 * later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
	 * License for more details. You should have received a copy of the GNU General Public License along with this
	 * program. If not, see <http://www.gnu.org/licenses/>.
	 * 
	 * @author Santhosh Kumar T
	 * @email santhosh.tekuri@gmail.com
	 */
	abstract class ComboSeparatorsRenderer
		implements ListCellRenderer
	{
		private ListCellRenderer delegate;
		private JPanel separatorPanel = new JPanel( new BorderLayout() );
		private JSeparator separator = new JSeparator();

		public ComboSeparatorsRenderer( ListCellRenderer delegate )
		{
			this.delegate = delegate;
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus )
		{
			Component comp = delegate.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			if ( index != -1 && addSeparatorAfter( list, value, index ) )
			{ // index==1 if renderer is used to paint current value in combo
				separatorPanel.removeAll();
				separatorPanel.add( comp, BorderLayout.CENTER );
				separatorPanel.add( separator, BorderLayout.SOUTH );
				return separatorPanel;
			}
			else
				return comp;
		}

		protected abstract boolean addSeparatorAfter( JList list, Object value, int index );
	}
}
