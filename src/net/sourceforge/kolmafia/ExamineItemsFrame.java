/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.DefaultListCellRenderer;

// events
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTabbedPane;

// utilities
import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to examine item descriptions
 */

public class ExamineItemsFrame extends KoLFrame
{
	private JTabbedPane tabs;
	private ExamineItemsPanel items;
	private ExamineEffectsPanel effects;
	private ExamineSkillsPanel skills;
	private ExamineFamiliarsPanel familiars;

	private static LockableListModel allItems;
	static
	{
		allItems = new LockableListModel();

		Iterator items = TradeableItemDatabase.iterator();
		while ( items.hasNext() )
			allItems.add( items.next() );
	}

	private static LockableListModel allEffects;
	static
	{
		allEffects = new LockableListModel();

		Iterator effects = StatusEffectDatabase.iterator();
		while ( effects.hasNext() )
			allEffects.add( effects.next() );
	}

	private static LockableListModel allSkills;
	static
	{
		allSkills = new LockableListModel();

		Iterator skills = ClassSkillsDatabase.iterator();
		while ( skills.hasNext() )
			allSkills.add( skills.next() );
	}

	private static LockableListModel allFamiliars;
	static
	{
		allFamiliars = new LockableListModel();

		Iterator familiars = FamiliarsDatabase.iterator();
		while ( familiars.hasNext() )
			allFamiliars.add( familiars.next() );
	}

	public ExamineItemsFrame( KoLmafia client )
	{
		super( client, "Kingdom of Loathing Encyclopedia" );

		tabs = new JTabbedPane();

		items = new ExamineItemsPanel();
		JPanel itemsContainer = new JPanel();
		itemsContainer.setLayout( new BorderLayout() );
		itemsContainer.add( items, BorderLayout.CENTER );
		tabs.addTab( "Items", itemsContainer );

		familiars = new ExamineFamiliarsPanel();
		JPanel familiarsContainer = new JPanel();
		familiarsContainer.setLayout( new BorderLayout() );
		familiarsContainer.add( familiars, BorderLayout.CENTER );
		tabs.addTab( "Familiars", familiarsContainer );

		skills = new ExamineSkillsPanel();
		JPanel skillsContainer = new JPanel();
		skillsContainer.setLayout( new BorderLayout() );
		skillsContainer.add( skills, BorderLayout.CENTER );
		tabs.addTab( "Skills", skillsContainer );

		effects = new ExamineEffectsPanel();
		JPanel effectsContainer = new JPanel();
		effectsContainer.setLayout( new BorderLayout() );
		effectsContainer.add( effects, BorderLayout.CENTER );
		tabs.addTab( "Effects", effectsContainer );

		getContentPane().setLayout( new CardLayout( 10, 10 ) );
		getContentPane().add( tabs, "" );
	}

	public boolean isEnabled()
	{	return true;
	}

	private class ExamineItemsPanel extends ItemManagePanel
	{
		public ExamineItemsPanel()
		{
			super( "All KoL Items", "Sort by name", "Sort by item #", allItems );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowItemAdapter() );
			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			elementList.clearSelection();
			java.util.Collections.sort( allItems, new EntryNameComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		public void actionCancelled()
		{
			// Sort elements by item number
			elementList.clearSelection();
			java.util.Collections.sort( allItems, new EntryIDComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		private class ShowItemAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = elementList.locationToIndex( e.getPoint() );
					Object item = elementList.getModel().getElementAt( index );

					if ( !(item instanceof Map.Entry ) )
						return;

					int id = ((Integer)((Map.Entry)item).getValue()).intValue();
					String desc = TradeableItemDatabase.getDescriptionID( id );
					elementList.ensureIndexIsVisible( index );
					openRequestFrame( "desc_item.php?whichitem=" + desc );
				}
			}
		}
	}

	private class ExamineFamiliarsPanel extends ItemManagePanel
	{
		public ExamineFamiliarsPanel()
		{
			super( "All KoL familiars", "Sort by name", "Sort by familiar #", allFamiliars );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowFamiliarAdapter() );
			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			elementList.clearSelection();
			java.util.Collections.sort( allFamiliars, new EntryNameComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		public void actionCancelled()
		{
			// Sort elements by familiar number
			elementList.clearSelection();
			java.util.Collections.sort( allFamiliars, new EntryIDComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		private class ShowFamiliarAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = elementList.locationToIndex( e.getPoint() );
					Object familiar = elementList.getModel().getElementAt( index );

					if ( !(familiar instanceof Map.Entry ) )
						return;

					int id = ((Integer)((Map.Entry)familiar).getValue()).intValue();
					elementList.ensureIndexIsVisible( index );
					openRequestFrame( "desc_familiar.php?which=" + id );
				}
			}
		}
	}

	private class ExamineSkillsPanel extends ItemManagePanel
	{
		public ExamineSkillsPanel()
		{
			super( "All KoL skills", "Sort by name", "Sort by skill #", allSkills );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowSkillAdapter() );
			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			elementList.clearSelection();
			java.util.Collections.sort( allSkills, new EntryNameComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		public void actionCancelled()
		{
			// Sort elements by skill number
			elementList.clearSelection();
			java.util.Collections.sort( allSkills, new EntryIDComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		private class ShowSkillAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = elementList.locationToIndex( e.getPoint() );
					Object skill = elementList.getModel().getElementAt( index );

					if ( !(skill instanceof Map.Entry ) )
						return;

					int id = ((Integer)((Map.Entry)skill).getValue()).intValue();
					elementList.ensureIndexIsVisible( index );
					openRequestFrame( "desc_skill.php?whichskill=" + id );
				}
			}
		}
	}

	private class ExamineEffectsPanel extends ItemManagePanel
	{
		public ExamineEffectsPanel()
		{
			super( "All KoL Effects", "Sort by name", "Sort by effect #", allEffects );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			elementList.addMouseListener( new ShowEffectAdapter() );
			actionConfirmed();
		}

		protected void actionConfirmed()
		{
			// Sort elements by name
			elementList.clearSelection();
			java.util.Collections.sort( allEffects, new EntryNameComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		public void actionCancelled()
		{
			// Sort elements by item number
			elementList.clearSelection();
			java.util.Collections.sort( allEffects, new EntryIDComparator() );
			elementList.setCellRenderer( new EntryCellRenderer() );
		}

		private class ShowEffectAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = elementList.locationToIndex( e.getPoint() );
					Object item = elementList.getModel().getElementAt( index );

					if ( !(item instanceof Map.Entry ) )
						return;

					int id = ((Integer)((Map.Entry)item).getValue()).intValue();
					elementList.ensureIndexIsVisible( index );
					openRequestFrame( "desc_effect.php?whicheffect=" + id );
				}
			}
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
			stringForm.append( (String)entry.getKey() );
			stringForm.append( " (" );
			stringForm.append( (Integer)entry.getValue() );
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

			int i1 = ((Integer)((Map.Entry)o1).getValue()).intValue();
			int i2 = ((Integer)((Map.Entry)o2).getValue()).intValue();
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

			String s1 = (String)((Map.Entry)o1).getKey();
			String s2 = (String)((Map.Entry)o2).getKey();
			return s1.compareTo( s2 );
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( ExamineItemsFrame.class, parameters )).run();
	}
}
