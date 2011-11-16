/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

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

		ArrayList listeners = new ArrayList();

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			listeners.add( new EnqueueListener() );
		}

		listeners.add( new ExecuteListener() );

		if ( this.food || this.booze )
		{
			listeners.add( new FamiliarFeedListener() );
			listeners.add( new BuffUpListener() );
		}

		if ( this.booze || this.spleen )
		{
			listeners.add( new FlushListener() );
		}

		ActionListener [] listenerArray = new ActionListener[ listeners.size() ];
		listeners.toArray( listenerArray );

		this.setButtons( false, listenerArray );

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

		this.elementList.setFixedCellHeight( (int) ( test.getPreferredSize().getHeight() * 2.5f ) );

		this.elementList.setVisibleRowCount( 6 );
		this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		this.filters = new JCheckBox[ food || booze || spleen ? 6 : 5 ];

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
			this.filters[ 5 ] = new ByRoomCheckbox();
		}
		else
		{
			this.filters[ 4 ] = new ByRoomCheckbox();
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

	public void setEnabled( final boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		// We gray out the dog hair button unless we have drunkenness,
		// have a pill, and haven't used one today.
		if ( isEnabled && this.booze )
		{
			// The "flush" listener is the last button
			int flushIndex = this.buttons.length - 1;
			boolean havedrunk = KoLCharacter.getInebriety() > 0;
			boolean havepill = InventoryManager.getCount( ItemPool.SYNTHETIC_DOG_HAIR_PILL ) > 0;
			boolean usedpill = Preferences.getBoolean( "_syntheticDogHairPillUsed" );
			boolean canFlush = havedrunk && ( havepill && !usedpill );
			this.buttons[ flushIndex ].setEnabled( canFlush );
		}
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

	private static class ReSortListener
		extends ThreadedListener
	{
		protected void execute()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private class EnqueueListener
		extends ThreadedListener
	{
		protected void execute()
		{
			UseItemEnqueuePanel.this.getDesiredItems( "Queue" );
			ConcoctionDatabase.refreshConcoctions( true );

			if ( UseItemEnqueuePanel.this.food )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemEnqueuePanel.this.booze )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemEnqueuePanel.this.spleen )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		public String toString()
		{
			return "enqueue";
		}
	}

	private class ExecuteListener
		extends ThreadedListener
	{
		protected void execute()
		{
			boolean warnFirst =
				( UseItemEnqueuePanel.this.food && ConcoctionDatabase.getQueuedFullness() != 0 ) ||
				( UseItemEnqueuePanel.this.booze && ConcoctionDatabase.getQueuedInebriety() != 0 ) ||
				( UseItemEnqueuePanel.this.spleen && ConcoctionDatabase.getQueuedSpleenHit() != 0 );

			if ( warnFirst && !InputFieldUtilities.confirm( "This action will also consume any queued items.  Are you sure you wish to continue?" ) )
			{
				return;
			}

			Object [] items = UseItemEnqueuePanel.this.getDesiredItems( "Consume" );

			if ( items == null )
			{
				return;
			}

			ConcoctionDatabase.handleQueue( UseItemEnqueuePanel.this.food, UseItemEnqueuePanel.this.booze, UseItemEnqueuePanel.this.spleen, KoLConstants.CONSUME_USE );

			if ( UseItemEnqueuePanel.this.food )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemEnqueuePanel.this.booze )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemEnqueuePanel.this.spleen )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		public String toString()
		{
			return "consume";
		}
	}

	private class FamiliarFeedListener
		extends ThreadedListener
	{
		private int consumptionType;

		public FamiliarFeedListener()
		{
			if ( UseItemEnqueuePanel.this.food )
			{
				this.consumptionType = KoLConstants.CONSUME_GHOST;
			}
			else if ( UseItemEnqueuePanel.this.booze )
			{
				this.consumptionType = KoLConstants.CONSUME_HOBO;
			}
			else
			{
				this.consumptionType = KoLConstants.NO_CONSUME;
			}
		}

		protected void execute()
		{
			boolean warnFirst =
				( UseItemEnqueuePanel.this.food && ConcoctionDatabase.getQueuedFullness() != 0 ) ||
				( UseItemEnqueuePanel.this.booze && ConcoctionDatabase.getQueuedInebriety() != 0 );

			if ( warnFirst && !InputFieldUtilities.confirm( "This action will also feed any queued items to your familiar.  Are you sure you wish to continue?" ) )
			{
				return;
			}

			Object [] items = UseItemEnqueuePanel.this.getDesiredItems( "Feed" );

			if ( items == null )
			{
				return;
			}

			ConcoctionDatabase.handleQueue( UseItemEnqueuePanel.this.food, UseItemEnqueuePanel.this.booze, UseItemEnqueuePanel.this.spleen, consumptionType );

			if ( UseItemEnqueuePanel.this.food )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemEnqueuePanel.this.booze )
			{
				UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
		}

		public String toString()
		{
			switch ( this.consumptionType )
			{
			case KoLConstants.CONSUME_GHOST:
				return "feed ghost";
			case KoLConstants.CONSUME_HOBO:
				return "feed hobo";
			default:
				return "";
			}
		}
	}


	private class BuffUpListener
		extends ThreadedListener
	{
		protected void execute()
		{
			if ( UseItemEnqueuePanel.this.food )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.MILK_OF_MAGNESIUM, 1 ) ) );
			}
			else if ( UseItemEnqueuePanel.this.booze )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
			}
		}

		public String toString()
		{
			return UseItemEnqueuePanel.this.food ? "use milk" : UseItemEnqueuePanel.this.booze ? "cast ode" : "" ;
		}
	}

	private class FlushListener
		extends ThreadedListener
	{
		protected void execute()
		{
			if ( UseItemEnqueuePanel.this.booze )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.SYNTHETIC_DOG_HAIR_PILL, 1 ) ) );
			}
			else if ( UseItemEnqueuePanel.this.spleen )
			{
				RequestThread.postRequest( new UseItemRequest( ItemPool.get( ItemPool.MOJO_FILTER, 1 ) ) );
			}
		}

		public String toString()
		{
			return UseItemEnqueuePanel.this.food ? "" : UseItemEnqueuePanel.this.booze ? "dog hair" : "flush mojo";
		}
	}

	private class ConsumableFilterField
		extends FilterItemField
	{
		public boolean isVisible( final Object element )
		{
			Concoction creation = (Concoction) element;

			if ( creation.getAvailable() == 0 )
			{
				return false;
			}

			if ( UseItemEnqueuePanel.this.filters[ 0 ].isSelected() )
			{
				AdventureResult item = creation.getItem();
				if ( item != null && item.getCount( KoLConstants.inventory ) == 0 )
				{
					return false;
				}
			}

			if ( ItemDatabase.getFullness( creation.getName() ) > 0 )
			{
				if ( !UseItemEnqueuePanel.this.food )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getInebriety( creation.getName() ) > 0 )
			{
				if ( !UseItemEnqueuePanel.this.booze )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getSpleenHit( creation.getName() ) > 0 )
			{
				if ( !UseItemEnqueuePanel.this.spleen )
				{
					return false;
				}
			}
			else switch ( ItemDatabase.getConsumptionType( creation.getName() ) )
			{
			case KoLConstants.CONSUME_FOOD_HELPER:
				if ( !UseItemEnqueuePanel.this.food )
				{
					return false;
				}
				return super.isVisible( element );

			case KoLConstants.CONSUME_DRINK_HELPER:
				if ( !UseItemEnqueuePanel.this.booze )
				{
					return false;
				}
				return super.isVisible( element );

			case KoLConstants.CONSUME_MULTIPLE:
				if ( !UseItemEnqueuePanel.this.food ||
				     creation.getItemId() != ItemPool.MUNCHIES_PILL )
				{
					return false;
				}
				return super.isVisible( element );

			case KoLConstants.CONSUME_USE:
				if ( !UseItemEnqueuePanel.this.food ||
				     creation.getItemId() != ItemPool.DISTENTION_PILL )
				{
					return false;
				}
				return super.isVisible( element );

			default:
				return false;
			}

			if ( KoLCharacter.inBeecore() )
			{
				// If you have a GGG or Spirit Hobo equipped,
				// disable B filtering, since you may want to
				// binge your familiar with B consumables.
				int fam = KoLCharacter.getFamiliar().getId();
				boolean override =
					// You cannot equip a Spirit Hobo in Beecore.
					// ( UseItemEnqueuePanel.this.booze && fam == FamiliarPool.HOBO ) ||
					( UseItemEnqueuePanel.this.food && fam == FamiliarPool.GHOST );
				AdventureResult item = creation.getItem();
				if ( !override && item != null && KoLCharacter.hasBeeosity( item.getName() ) )
				{
					return false;
				}
			}

			if ( KoLCharacter.isTrendy() )
			{
				AdventureResult item = creation.getItem();
				if ( item != null && !TrendyRequest.isTrendy( "Items", item.getName() ) )
				{
					return false;
				}
			}

			if ( UseItemEnqueuePanel.this.filters[ 1 ].isSelected() )
			{
				String range = ItemDatabase.getMuscleRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( UseItemEnqueuePanel.this.filters[ 2 ].isSelected() )
			{
				String range = ItemDatabase.getMysticalityRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( UseItemEnqueuePanel.this.filters[ 3 ].isSelected() )
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

	private static class ExperimentalCheckbox
		extends JCheckBox
		implements ActionListener
	{
		public ExperimentalCheckbox( final boolean food, final boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : food ? "per full" : "per spleen" );

			this.setToolTipText( "Sort gains per adventure" );
			this.setSelected( Preferences.getBoolean( "showGainsPerUnit" ) );

			this.addActionListener( this );
			PreferenceListenerRegistry.registerCheckbox( "showGainsPerUnit", this );
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

	private static class ByRoomCheckbox
		extends JCheckBox
		implements ActionListener
	{
		public ByRoomCheckbox()
		{
			super( "by room" );

			this.setToolTipText( "Sort items you have no room for to the bottom" );
			this.setSelected( Preferences.getBoolean( "sortByRoom" ) );

			this.addActionListener( this );
			PreferenceListenerRegistry.registerCheckbox( "sortByRoom", this );
		}

		public void actionPerformed( final ActionEvent e )
		{
			if ( Preferences.getBoolean( "sortByRoom" ) == this.isSelected() )
			{
				return;
			}

			Preferences.setBoolean( "sortByRoom", this.isSelected() );
			ConcoctionDatabase.getUsables().sort();
		}
	}
}
