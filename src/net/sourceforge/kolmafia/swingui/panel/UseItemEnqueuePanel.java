/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.BuffUpListener;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.ConsumableFilterField;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.EnqueueListener;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.ExecuteListener;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.ExperimentalCheckbox;
import net.sourceforge.kolmafia.swingui.ItemManageFrame.ConsumeEnqueuePanel.ReSortListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class UseItemEnqueuePanel
	extends ItemManagePanel
{
	private boolean food, booze, spleen;
	private final JCheckBox[] filters;
	private final JTabbedPane queueTabs;

	public UseItemEnqueuePanel( final boolean food, final boolean booze, final boolean spleen, final JTabbedPane queueTabs )
	{
		super( ConcoctionDatabase.getUsables(), true, true );

		this.food = food;
		this.booze = booze;
		this.spleen = spleen;

		this.queueTabs = queueTabs;

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			this.setButtons(
				false, new ActionListener[] { new EnqueueListener(), new ExecuteListener(), new BuffUpListener() } );
		}
		else
		{
			this.setButtons( false, new ActionListener[] { new ExecuteListener(), new BuffUpListener() } );
		}

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

		this.elementList.setFixedCellHeight( (int) ( test.getPreferredSize().getHeight() * 2.5f ) );

		this.elementList.setVisibleRowCount( 6 );
		this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		this.filters = new JCheckBox[ food || booze || spleen ? 5 : 4 ];

		this.filters[ 0 ] = new JCheckBox( "no create" );
		this.filters[ 1 ] = new JCheckBox( "+mus only" );
		this.filters[ 2 ] = new JCheckBox( "+mys only" );
		this.filters[ 3 ] = new JCheckBox( "+mox only" );

		for ( int i = 0; i < 4; ++i )
		{
			this.listenToCheckBox( this.filters[ i ] );
		}

		if ( food || booze || spleen )
		{
			this.filters[ 4 ] = new ExperimentalCheckbox( food, booze );
		}

		JPanel filterPanel = new JPanel();
		for ( int i = 0; i < this.filters.length; ++i )
		{
			filterPanel.add( this.filters[ i ] );
		}

		this.setEnabled( true );
		this.northPanel.add( filterPanel, BorderLayout.NORTH );

		this.filterItems();
	}

	public AutoFilterTextField getWordFilter()
	{
		return new ConsumableFilterField();
	}

	protected void listenToCheckBox( final JCheckBox box )
	{
		super.listenToCheckBox( box );
		box.addActionListener( new ReSortListener() );
	}

	public void actionConfirmed()
	{
	}

	public void actionCancelled()
	{
	}

	private class ReSortListener
		extends ThreadedListener
	{
		public void run()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private class EnqueueListener
		extends ThreadedListener
	{
		public void run()
		{
			itemManageFrame.getDesiredItems( "Queue" );
			ConcoctionDatabase.refreshConcoctions();

			if ( itemManageFrame.food )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( itemManageFrame.booze )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( itemManageFrame.spleen )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
		}

		public String toString()
		{
			return "enqueue";
		}
	}

	private class ExecuteListener
		extends ThreadedListener
	{
		public void run()
		{
			itemManageFrame.getDesiredItems( "Consume" );
			ConcoctionDatabase.refreshConcoctions();
			ConcoctionDatabase.handleQueue( true );

			if ( itemManageFrame.food )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( itemManageFrame.booze )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( itemManageFrame.spleen )
			{
				itemManageFrame.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
		}

		public String toString()
		{
			return "consume";
		}
	}

	private class BuffUpListener
		extends ThreadedListener
	{
		public void run()
		{
			if ( itemManageFrame.food )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.MILK_OF_MAGNESIUM, 1 ) ) );
			}
			else if ( itemManageFrame.booze )
			{
				if ( !KoLConstants.activeEffects.contains( new AdventureResult( "Ode to Booze", 1, true ) ) )
				{
					RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
				}
			}
			else if ( itemManageFrame.spleen )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.MOJO_FILTER, 1 ) ) );
			}
		}

		public String toString()
		{
			return itemManageFrame.food ? "use milk" : itemManageFrame.booze ? "cast ode" : "flush mojo";
		}
	}

	private class ConsumableFilterField
		extends FilterItemField
	{
		public boolean isVisible( final Object element )
		{
			Concoction creation = (Concoction) element;

			if ( ItemDatabase.getFullness( creation.getName() ) > 0 )
			{
				if ( !itemManageFrame.food )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getInebriety( creation.getName() ) > 0 )
			{
				if ( !itemManageFrame.booze )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getSpleenHit( creation.getName() ) > 0 )
			{
				if ( !itemManageFrame.spleen )
				{
					return false;
				}
			}
			else
			{
				return false;
			}

			if ( creation.getTotal() == 0 )
			{
				return false;
			}

			if ( itemManageFrame.filters[ 0 ].isSelected() )
			{
				AdventureResult item = creation.getItem();
				if ( item != null && item.getCount( KoLConstants.inventory ) == 0 )
				{
					return false;
				}
			}

			if ( itemManageFrame.filters[ 1 ].isSelected() )
			{
				String range = ItemDatabase.getMuscleRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( itemManageFrame.filters[ 2 ].isSelected() )
			{
				String range = ItemDatabase.getMysticalityRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( itemManageFrame.filters[ 3 ].isSelected() )
			{
				String range = ItemDatabase.getMoxieRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			return super.isVisible( element );
		}
	}

	private class ExperimentalCheckbox
		extends JCheckBox
		implements ActionListener
	{
		public ExperimentalCheckbox( final boolean food, final boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : food ? "per full" : "per spleen" );

			this.setToolTipText( "Sort gains per adventure" );
			this.setSelected( Preferences.getBoolean( "showGainsPerUnit" ) );

			this.addActionListener( this );
			Preferences.registerCheckbox( "showGainsPerUnit", this );
		}

		public void actionPerformed( final ActionEvent e )
		{
			if ( Preferences.getBoolean( "showGainsPerUnit" ) == this.isSelected() )
			{
				return;
			}

			Preferences.setBoolean( "showGainsPerUnit", this.isSelected() );
			ConcoctionDatabase.getUsables().sort();
		}
	}
}