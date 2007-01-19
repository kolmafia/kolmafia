/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ExamineItemsFrame extends KoLFrame
{
	private ExamineItemsPanel items;
	private ItemLookupPanel familiars, skills, effects;

	private static LockableListModel allItems = new LockableListModel( TradeableItemDatabase.entrySet() );
	private static LockableListModel allEffects = new LockableListModel( StatusEffectDatabase.entrySet() );
	private static LockableListModel allSkills = new LockableListModel( ClassSkillsDatabase.entrySet() );
	private static LockableListModel allFamiliars = new LockableListModel( FamiliarsDatabase.entrySet() );

	public ExamineItemsFrame()
	{
		super( "Internal Database" );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		items = new ExamineItemsPanel( allItems );
		tabs.addTab( "Items", items );

		familiars = new ItemLookupPanel( allFamiliars, "Familiars", "familiar", "which" );
		tabs.addTab( "Familiars", familiars );

		skills = new ItemLookupPanel( allSkills, "Skills", "skill", "whichskill" );
		tabs.addTab( "Skills", skills );

		effects = new ItemLookupPanel( allEffects, "Effects", "effect", "whicheffect" );
		tabs.addTab( "Effects", effects );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private class ItemLookupPanel extends ItemManagePanel
	{
		private LockableListModel list;
		public String type;
		public String which;

		public ItemLookupPanel( LockableListModel list, String title, String type, String which )
		{
			super( "All KoL " + title, "Sort by name", "Sort by " + type + " #", list );

			this.list = list;
			this.type = type;
			this.which = which;

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowEntryAdapter() );
			elementList.setCellRenderer( new EntryCellRenderer() );

			elementList.contextMenu.add( new DescriptionMenuItem(), 0 );

			actionConfirmed();
		}

		public void actionConfirmed()
		{
			// Sort elements by name
			list.sort( new EntryNameComparator() );
		}

		public void actionCancelled()
		{
			// Sort elements by Id number
			list.sort( new EntryIdComparator() );
		}

		/**
		 * Utility class which shows the description of the item
		 * which is currently selected.
		 */

		private class DescriptionMenuItem extends ThreadedMenuItem
		{
			public DescriptionMenuItem()
			{	super( "Game description" );
			}

			public void run()
			{	showDescription( (Entry) elementModel.get( elementList.lastSelectIndex ) );
			}
		}

		private class ShowEntryAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() != 2 )
					return;

				int index = elementList.locationToIndex( e.getPoint() );
				Object entry = elementList.getModel().getElementAt( index );

				if ( !(entry instanceof Entry ) )
					return;

				elementList.ensureIndexIsVisible( index );
				showDescription( (Entry) entry );
			}
		}

		public void showDescription( Entry entry )
		{
			String id = String.valueOf( ((Integer)entry.getKey()).intValue() );
			StaticEntity.openRequestFrame( "desc_" + type + ".php?" + which + "=" + id );
		}
	}

	private class ExamineItemsPanel extends ItemLookupPanel
	{
		public ExamineItemsPanel( LockableListModel list )
		{	super( list, "Items", "item", "whichitem" );
		}

		public void showDescription( Entry entry )
		{
			String id = TradeableItemDatabase.getDescriptionId( ((Integer)entry.getKey()).intValue() );
			StaticEntity.openRequestFrame( "desc_" + type + ".php?" + which + "=" + id );
		}
	}

	private class EntryCellRenderer extends DefaultListCellRenderer
	{
		public EntryCellRenderer()
		{	setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof Entry ) )
				return defaultComponent;

			Entry entry = (Entry) value;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( (String)entry.getValue() );
			stringForm.append( " (" );
			stringForm.append( (Integer)entry.getKey() );
			stringForm.append( ")" );

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	private class EntryIdComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if ( !(o1 instanceof Entry ) ||
			     !(o2 instanceof Entry ) )
				throw new ClassCastException();

			int i1 = ((Integer)((Entry)o1).getKey()).intValue();
			int i2 = ((Integer)((Entry)o2).getKey()).intValue();
			return i1 - i2;
		}
	}

	private class EntryNameComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if ( !(o1 instanceof Entry ) ||
			     !(o2 instanceof Entry ) )
				throw new ClassCastException();

			String s1 = (String)((Entry)o1).getValue();
			String s2 = (String)((Entry)o2).getValue();
			return s1.compareToIgnoreCase( s2 );
		}
	}
}
