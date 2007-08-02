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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Map.Entry;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ExamineItemsFrame extends KoLFrame
{
	private static final LockableListModel allItems = new LockableListModel( TradeableItemDatabase.entrySet() );
	private static final LockableListModel allEffects = new LockableListModel( StatusEffectDatabase.entrySet() );
	private static final LockableListModel allSkills = new LockableListModel( ClassSkillsDatabase.entrySet() );
	private static final LockableListModel allFamiliars = new LockableListModel( FamiliarsDatabase.entrySet() );

	public ExamineItemsFrame()
	{
		super( "Internal Database" );
		this.setDefaultCloseOperation( HIDE_ON_CLOSE );

		this.tabs.addTab( "Items", new ExamineItemsPanel() );
		this.tabs.addTab( "Familiars", new ItemLookupPanel( allFamiliars, "familiar", "which" ) );
		this.tabs.addTab( "Skills", new ItemLookupPanel( allSkills, "skill", "whichskill" ) );
		this.tabs.addTab( "Effects", new ExamineEffectsPanel() );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	private class ItemLookupPanel extends ItemManagePanel
	{
		private LockableListModel list;
		public String type;
		public String which;

		public ItemLookupPanel( LockableListModel list, String type, String which )
		{
			super( "Sort by name", "Sort by " + type + " #", list );

			this.list = list;
			this.type = type;
			this.which = which;

			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.elementList.addMouseListener( new ShowEntryAdapter() );
			this.elementList.setCellRenderer( new EntryCellRenderer() );

			this.elementList.contextMenu.add( new DescriptionMenuItem(), 0 );

			this.actionConfirmed();
		}

		public void actionConfirmed()
		{
			// Sort elements by name
			this.list.sort( new EntryNameComparator() );
		}

		public void actionCancelled()
		{
			// Sort elements by Id number
			this.list.sort( new EntryIdComparator() );
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
			{	ItemLookupPanel.this.showDescription( (Entry) ItemLookupPanel.this.elementModel.get( ItemLookupPanel.this.elementList.lastSelectIndex ) );
			}
		}

		private class ShowEntryAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() != 2 )
					return;

				int index = ItemLookupPanel.this.elementList.locationToIndex( e.getPoint() );
				Object entry = ItemLookupPanel.this.elementList.getModel().getElementAt( index );

				if ( !(entry instanceof Entry ) )
					return;

				ItemLookupPanel.this.elementList.ensureIndexIsVisible( index );
				ItemLookupPanel.this.showDescription( (Entry) entry );
			}
		}

		public void showDescription( Entry entry )
		{
			StaticEntity.openRequestFrame( "desc_" + this.type + ".php?" + this.which + "=" + getId( entry ) );
		}

		public String getId( Entry e )
		{	return String.valueOf( ((Integer)e.getKey()).intValue() );
		}
	}

	private class ExamineItemsPanel extends ItemLookupPanel
	{
		public ExamineItemsPanel()
		{	super( allItems, "item", "whichitem" );
		}

		public String getId( Entry e )
		{	return TradeableItemDatabase.getDescriptionId( ((Integer)e.getKey()).intValue() );
		}
	}

	private class ExamineEffectsPanel extends ItemLookupPanel
	{
		public ExamineEffectsPanel()
		{	super( allEffects, "effect", "whicheffect" );
		}

		public String getId( Entry e )
		{	return StatusEffectDatabase.getDescriptionId( ((Integer)e.getKey()).intValue() );
		}
	}

	private class EntryCellRenderer extends DefaultListCellRenderer
	{
		public EntryCellRenderer()
		{	this.setOpaque( true );
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
			stringForm.append( entry.getKey() );
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
