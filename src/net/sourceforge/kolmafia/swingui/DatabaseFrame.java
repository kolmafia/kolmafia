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

package net.sourceforge.kolmafia.swingui;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import java.util.Map.Entry;

import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;

import net.sourceforge.kolmafia.swingui.panel.ItemTableManagePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.LowerCaseEntry;

public class DatabaseFrame
	extends GenericFrame
{
	public static final LockableListModel allItems = LowerCaseEntry.createListModel( ItemDatabase.entrySet() );
	public static final LockableListModel allEffects = LowerCaseEntry.createListModel( EffectDatabase.entrySet() );
	public static final LockableListModel allSkills = LowerCaseEntry.createListModel( SkillDatabase.entrySet() );
	public static final LockableListModel allFamiliars = LowerCaseEntry.createListModel( FamiliarDatabase.entrySet() );
	public static final LockableListModel allOutfits = LowerCaseEntry.createListModel( EquipmentDatabase.outfitEntrySet() );

	public DatabaseFrame()
	{
		super( "Internal Database" );

		this.tabs.addTab( "Items", new ExamineItemsPanel() );
		this.tabs.addTab( "Familiars", new ItemLookupPanel( DatabaseFrame.allFamiliars, "familiar", "which" ) );
		this.tabs.addTab( "Skills", new ItemLookupPanel( DatabaseFrame.allSkills, "skill", "whichskill" ) );
		this.tabs.addTab( "Effects", new ExamineEffectsPanel() );
		this.tabs.addTab( "Outfits", new ItemLookupPanel( DatabaseFrame.allOutfits, "outfit", "whichoutfit" ) );

		this.setCenterComponent( this.tabs );
	}

	private class ItemLookupPanel
		extends ItemTableManagePanel
	{
		public String type;
		public String which;

		public ItemLookupPanel( final LockableListModel list, final String type, final String which )
		{
			super( list );

			this.type = type;
			this.which = which;

			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.elementList.addMouseListener( new ShowEntryListener() );
			this.elementList.contextMenu.add( new ThreadedMenuItem( "Game description", new DescriptionListener() ), 0 );
			
			this.setPreferredSize( new Dimension(400, 400) );

			this.actionConfirmed();
		}

		@Override
		public AutoFilterTextField getWordFilter()
		{
			return new AutoFilterTextField( this.elementModel );
		}

		/**
		 * Utility class which shows the description of the item which is currently selected.
		 */

		private class DescriptionListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				int index = ItemLookupPanel.this.elementList.getSelectedIndex();
				if ( index != -1 )
				{
					Entry entry = (Entry) ItemLookupPanel.this.elementList.getDisplayModel().getElementAt( index );
					ItemLookupPanel.this.showDescription( entry );
				}
			}
		}

		private class ShowEntryListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				MouseEvent e = getMouseEvent();

				if ( e.getClickCount() != 2 )
				{
					return;
				}

				int index = ItemLookupPanel.this.elementList.locationToIndex( e.getPoint() );
				Object entry = ItemLookupPanel.this.elementList.getDisplayModel().getElementAt( index );

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

		@Override
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

		@Override
		public String getId( final Entry e )
		{
			return EffectDatabase.getDescriptionId( ( (Integer) e.getKey() ).intValue() );
		}
	}
}
