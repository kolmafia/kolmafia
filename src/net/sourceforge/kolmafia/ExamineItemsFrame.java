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

// layout
import java.awt.CardLayout;
import javax.swing.DefaultListCellRenderer;

// events
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.ListSelectionModel;

// containers
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTabbedPane;

// utilities
import java.util.Map;
import java.util.Comparator;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A Frame to examine item descriptions
 */

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
		private String type;
		private String which;

		public ItemLookupPanel( LockableListModel list, String title, String type, String which )
		{
			super( "All KoL " + title, "Sort by name", "Sort by " + type + " #", list );

			this.list = list;
			this.type = type;
			this.which = which;

			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowEntryAdapter() );
			elementList.setCellRenderer( new EntryCellRenderer() );

			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			list.sort( new EntryNameComparator() );
		}

		public void actionCancelled()
		{
			// Sort elements by ID number
			list.sort( new EntryIDComparator() );
		}

		private class ShowEntryAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = elementList.locationToIndex( e.getPoint() );
					Object entry = elementList.getModel().getElementAt( index );

					if ( !(entry instanceof Map.Entry ) )
						return;

					elementList.ensureIndexIsVisible( index );
					String id = IDNumberMapper( ((Integer)((Map.Entry)entry).getKey()).intValue() );
					StaticEntity.openRequestFrame( "desc_" + type + ".php?" + which + "=" + id );
				}
			}
		}

		public String IDNumberMapper( int id )
		{	return String.valueOf( id );
		}
	}

	private class ExamineItemsPanel extends ItemLookupPanel
	{
		public ExamineItemsPanel( LockableListModel list )
		{	super( list, "Items", "item", "whichitem" );
		}

		public String IDNumberMapper( int id )
		{	return TradeableItemDatabase.getDescriptionID( id );
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

			if ( value == null || !(value instanceof Map.Entry ) )
				return defaultComponent;

			Map.Entry entry = (Map.Entry) value;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( (String)entry.getValue() );
			stringForm.append( " (" );
			stringForm.append( (Integer)entry.getKey() );
			stringForm.append( ")" );

			((JLabel) defaultComponent).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	private class EntryIDComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if ( !(o1 instanceof Map.Entry ) ||
			     !(o2 instanceof Map.Entry ) )
				throw new ClassCastException();

			int i1 = ((Integer)((Map.Entry)o1).getKey()).intValue();
			int i2 = ((Integer)((Map.Entry)o2).getKey()).intValue();
			return i1 - i2;
		}
	}

	private class EntryNameComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if ( !(o1 instanceof Map.Entry ) ||
			     !(o2 instanceof Map.Entry ) )
				throw new ClassCastException();

			String s1 = (String)((Map.Entry)o1).getValue();
			String s2 = (String)((Map.Entry)o2).getValue();
			return s1.compareToIgnoreCase( s2 );
		}
	}
}
