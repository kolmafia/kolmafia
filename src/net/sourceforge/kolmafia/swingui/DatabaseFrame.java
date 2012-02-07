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

package net.sourceforge.kolmafia.swingui;

import java.awt.event.MouseEvent;

import java.util.Comparator;
import java.util.Map.Entry;

import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;

import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.utilities.LowerCaseEntry;

public class DatabaseFrame
	extends GenericFrame
{
	private static final LockableListModel allItems = LowerCaseEntry.createListModel( ItemDatabase.entrySet() );
	private static final LockableListModel allEffects = LowerCaseEntry.createListModel( EffectDatabase.entrySet() );
	private static final LockableListModel allSkills = LowerCaseEntry.createListModel( SkillDatabase.entrySet() );
	private static final LockableListModel allFamiliars = LowerCaseEntry.createListModel( FamiliarDatabase.entrySet() );

	public DatabaseFrame()
	{
		super( "Internal Database" );

		this.tabs.addTab( "Items", new ExamineItemsPanel() );
		this.tabs.addTab( "Familiars", new ItemLookupPanel( DatabaseFrame.allFamiliars, "familiar", "which" ) );
		this.tabs.addTab( "Skills", new ItemLookupPanel( DatabaseFrame.allSkills, "skill", "whichskill" ) );
		this.tabs.addTab( "Effects", new ExamineEffectsPanel() );

		this.setCenterComponent( this.tabs );
	}

	private class ItemLookupPanel
		extends ItemManagePanel
	{
		public String type;
		public String which;

		public ItemLookupPanel( final LockableListModel list, final String type, final String which )
		{
			super( "Sort by name", "Sort by " + type + " #", list );

			this.type = type;
			this.which = which;

			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.elementList.addMouseListener( new ShowEntryListener() );
			this.elementList.contextMenu.add( new ThreadedMenuItem( "Game description", new DescriptionListener() ), 0 );

			this.actionConfirmed();
		}

		public AutoFilterTextField getWordFilter()
		{
			return new AutoFilterTextField( this.elementList );
		}

		public void actionConfirmed()
		{
			// Sort elements by name
			this.elementModel.sort( new EntryNameComparator() );
		}

		public void actionCancelled()
		{
			// Sort elements by Id number
			this.elementModel.sort( new EntryIdComparator() );
		}

		/**
		 * Utility class which shows the description of the item which is currently selected.
		 */

		private class DescriptionListener
			extends ThreadedListener
		{
			protected void execute()
			{
				int index = ItemLookupPanel.this.elementList.lastSelectIndex;
				if ( index != -1 )
				{
					Entry entry = (Entry) ItemLookupPanel.this.elementModel.getElementAt( index );
					ItemLookupPanel.this.showDescription( entry );
				}
			}
		}

		private class ShowEntryListener
			extends ThreadedListener
		{
			protected void execute()
			{
				MouseEvent e = getMouseEvent();

				if ( e.getClickCount() != 2 )
				{
					return;
				}

				int index = ItemLookupPanel.this.elementList.locationToIndex( e.getPoint() );
				Object entry = ItemLookupPanel.this.elementList.getModel().getElementAt( index );

				if ( !( entry instanceof Entry ) )
				{
					return;
				}

				ItemLookupPanel.this.elementList.ensureIndexIsVisible( index );
				ItemLookupPanel.this.showDescription( (Entry) entry );
			}
		}

		public void showDescription( final Entry entry )
		{
			StaticEntity.openRequestFrame( "desc_" + this.type + ".php?" + this.which + "=" + this.getId( entry ) );
		}

		public String getId( final Entry e )
		{
			return String.valueOf( ( (Integer) e.getKey() ).intValue() );
		}
	}

	private class ExamineItemsPanel
		extends ItemLookupPanel
	{
		public ExamineItemsPanel()
		{
			super( DatabaseFrame.allItems, "item", "whichitem" );
		}

		public String getId( final Entry e )
		{
			return ItemDatabase.getDescriptionId( ( (Integer) e.getKey() ).intValue() );
		}
	}

	private class ExamineEffectsPanel
		extends ItemLookupPanel
	{
		public ExamineEffectsPanel()
		{
			super( DatabaseFrame.allEffects, "effect", "whicheffect" );
		}

		public String getId( final Entry e )
		{
			return EffectDatabase.getDescriptionId( ( (Integer) e.getKey() ).intValue() );
		}
	}

	private class EntryIdComparator
		implements Comparator
	{
		public int compare( final Object o1, final Object o2 )
		{
			if ( !( o1 instanceof Entry ) || !( o2 instanceof Entry ) )
			{
				throw new ClassCastException();
			}

			int i1 = ( (Integer) ( (Entry) o1 ).getKey() ).intValue();
			int i2 = ( (Integer) ( (Entry) o2 ).getKey() ).intValue();
			return i1 - i2;
		}
	}

	private class EntryNameComparator
		implements Comparator
	{
		public int compare( final Object o1, final Object o2 )
		{
			if ( !( o1 instanceof Entry ) || !( o2 instanceof Entry ) )
			{
				throw new ClassCastException();
			}

			String s1 = (String) ( (Entry) o1 ).getValue();
			String s2 = (String) ( (Entry) o2 ).getValue();
			return s1.compareToIgnoreCase( s2 );
		}
	}
}
