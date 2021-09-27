package net.sourceforge.kolmafia.swingui.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;
import java.util.LinkedList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.SwinglessUIUtils;

/**
 * Like an MRUList, but maintains a list of "default" settings at the bottom under a JSeparator.
 */
public class PartialMRUList
	extends ScriptMRUList
	implements Listener
{
	private final LinkedList<String> defaultList = new LinkedList<>();
	private final String pDefaultList;

	/**
	 * This is a ComboSeparatorsRenderer, but in order to have this class compile in
	 * Swingless environments, it is typed as an Object
	 */
	private final Object renderer;

	/**
	 * @param pList
	 * @param pLen
	 */
	public PartialMRUList( String pList, String pLen, String pDefaultList )
	{
		super( pList, pLen );
		this.pDefaultList = pDefaultList;
		PreferenceListenerRegistry.registerPreferenceListener( pDefaultList, this );
		update();
		if ( SwinglessUIUtils.isSwingAvailable() )
		{
			renderer = new ComboSeparatorsRenderer( new DefaultListCellRenderer() )
			{
				@Override
				protected boolean addSeparatorAfter( JList<?> list, Object value, int index )
				{
					if ( PartialMRUList.this.maxMRU < 0 )
						return false;
					return index == PartialMRUList.this.maxMRU - 1;
				}
			};
		}
		else
		{
			renderer = null;
		}
	}

	/*
	 * Override the update method in order to create a "hybrid" MRU-plus-default list. Note to implementers: calling
	 * this method alters the combobox's default renderer. (non-Javadoc)
	 * @see net.sourceforge.kolmafia.swingui.menu.ScriptMRUList#updateJComboData(javax.swing.JComboBox)
	 */
	@Override
	public void updateJComboData( JComboBox<Object> jcb )
	{
		if ( !isInit )
		{
			init();
		}
		if ( jcb.getRenderer() != this.renderer )
		{
			jcb.setRenderer( (ComboSeparatorsRenderer) this.renderer );
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
	abstract static class ComboSeparatorsRenderer
		implements ListCellRenderer<Object>
	{
		private final ListCellRenderer<Object> delegate;
		private final JPanel separatorPanel = new JPanel( new BorderLayout() );
		private final JSeparator separator = new JSeparator();

		public ComboSeparatorsRenderer( ListCellRenderer<Object> delegate )
		{
			this.delegate = delegate;
		}

		public Component getListCellRendererComponent( final JList<?> list, Object value, int index, boolean isSelected,
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

		protected abstract boolean addSeparatorAfter( JList<?> list, Object value, int index );
	}

	public void update()
	{
		String[] newlist = Preferences.getString( this.pDefaultList ).split( " \\| " );
		this.defaultList.clear();
		Collections.addAll(this.defaultList, newlist);
	}
}
