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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.Box;
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

import net.sourceforge.kolmafia.preferences.PreferenceListenerCheckBox;
import net.sourceforge.kolmafia.preferences.Preferences;

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

	public UseItemEnqueuePanel( final boolean food, final boolean booze, final boolean spleen, JTabbedPane queueTabs )
	{
		super( ConcoctionDatabase.getUsables(), true, true );
		// Remove the default borders inherited from ScrollablePanel.
		BorderLayout a = (BorderLayout) this.actualPanel.getLayout();
		a.setVgap( 0 );
		CardLayout b = (CardLayout) this.actualPanel.getParent().getLayout();
		b.setVgap( 0 );

		this.food = food;
		this.booze = booze;
		this.spleen = spleen;

		if ( queueTabs == null )
		{	// Make a dummy tabbed pane, so that we don't have to do null
			// checks in the 8 places where setTitleAt(0, ...) is called.
			queueTabs = new JTabbedPane();
			queueTabs.addTab( "dummy", new JLabel() );
		}
		this.queueTabs = queueTabs;

		ArrayList listeners = new ArrayList();

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			listeners.add( new EnqueueListener() );
		}

		listeners.add( new ExecuteListener() );

		if ( this.food )
		{
			listeners.add( new BingeGhostListener() );
			listeners.add( new MilkListener() );
			listeners.add( new DistendListener() );
		}
		else if ( this.booze )
		{
			listeners.add( new BingeHoboListener() );
			listeners.add( new OdeListener() );
			listeners.add( new DogHairListener() );
		}
		else if (this.spleen )
		{
			listeners.add( new MojoListener() );
		}

		ActionListener [] listenerArray = new ActionListener[ listeners.size() ];
		listeners.toArray( listenerArray );

		this.setButtons( false, listenerArray );

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

		this.elementList.setFixedCellHeight( (int) ( test.getPreferredSize().getHeight() * 2.5f ) );

		this.elementList.setVisibleRowCount( 6 );
		this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		this.filters = new JCheckBox[ food || booze || spleen ? 8 : 7 ];

		this.filters[ 0 ] = new JCheckBox( "no create" );
		this.filters[ 1 ] = new TurnFreeCheckbox();
		this.filters[ 2 ] = new JCheckBox( "no summon" );
		this.filters[ 3 ] = new JCheckBox( "+mus only" );
		this.filters[ 4 ] = new JCheckBox( "+mys only" );
		this.filters[ 5 ] = new JCheckBox( "+mox only" );

		for ( int i = 0; i < 6; ++i )
		{
			this.listenToCheckBox( this.filters[ i ] );
		}

		JPanel filterPanel = new JPanel( new GridLayout() );
		JPanel column1 = new JPanel( new BorderLayout() );
		JPanel column2 = new JPanel( new BorderLayout() );
		JPanel column3 = new JPanel( new BorderLayout() );
		JPanel column4 = new JPanel( new BorderLayout() );

		column1.add( this.filters[ 0 ], BorderLayout.NORTH );
		column2.add( this.filters[ 1 ], BorderLayout.NORTH );
		column3.add( this.filters[ 2 ], BorderLayout.NORTH );
		column1.add( this.filters[ 3 ], BorderLayout.CENTER );
		column2.add( this.filters[ 4 ], BorderLayout.CENTER );
		column3.add( this.filters[ 5 ], BorderLayout.CENTER );

		if ( food || booze || spleen )
		{
			this.filters[ 6 ] = new ExperimentalCheckBox( food, booze );
			this.filters[ 7 ] = new ByRoomCheckbox();
			column4.add( this.filters[ 6 ], BorderLayout.NORTH );
			column4.add( this.filters[ 7 ], BorderLayout.CENTER );
		}
		else
		{
			this.filters[ 6 ] = new ByRoomCheckbox();
			column4.add( this.filters[ 6 ], BorderLayout.CENTER );
		}

		filterPanel.add( column1 );
		filterPanel.add( column2 );
		filterPanel.add( column3 );
		filterPanel.add( column4 );

		// Set the height of the filter panel to be just a wee bit taller than two checkboxes need
		filterPanel.setPreferredSize( new Dimension( 10,
			(int) ( this.filters[ 0 ].getPreferredSize().height * 2.1f ) ) );

		this.setEnabled( true );

		this.northPanel.add( filterPanel, BorderLayout.NORTH );
		// Restore the 10px border that we removed from the bottom.
		this.actualPanel.add( Box.createVerticalStrut( 10 ), BorderLayout.SOUTH );

		this.filterItems();
	}

	public void setEnabled( final boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( isEnabled && this.food )
		{
			// The "binge" listener is the third button
			int bingeIndex = 2;
			boolean haveGhost = KoLCharacter.findFamiliar( FamiliarPool.GHOST ) != null;
			this.buttons[ bingeIndex ].setEnabled( haveGhost );

			// We gray out the distend button unless we have a
			// pill, and haven't used one today.
			//
			// The "flush" listener is the last button
			int flushIndex = this.buttons.length - 1;
			boolean havepill = InventoryManager.getCount( ItemPool.DISTENTION_PILL ) > 0;
			boolean activepill = Preferences.getBoolean( "distentionPillActive" );
			boolean usedpill = Preferences.getBoolean( "_distentionPillUsed" );
			boolean canFlush = ( havepill && !activepill && !usedpill );
			this.buttons[ flushIndex ].setEnabled( canFlush );
		}

		if ( isEnabled && this.booze )
		{
			// The "binge" listener is the third button
			int bingeIndex = 2;
			boolean haveHobo = KoLCharacter.findFamiliar( FamiliarPool.HOBO ) != null;
			this.buttons[ bingeIndex ].setEnabled( haveHobo );

			// We gray out the dog hair button unless we have
			// inebriety, have a pill, and haven't used one today.
			//
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

	private class BingeGhostListener
		extends FamiliarFeedListener
	{
		public boolean warnBeforeConsume()
		{
			return ConcoctionDatabase.getQueuedFullness() != 0;
		}

		public void handleQueue()
		{
			ConcoctionDatabase.handleQueue( true, false, false, KoLConstants.CONSUME_GHOST );
		}

		public String getTitle()
		{
			return ConcoctionDatabase.getQueuedFullness() + " Full Queued";
		}

		public String toString()
		{
			return "feed ghost";
		}
	}

	private class BingeHoboListener
		extends FamiliarFeedListener
	{
		public boolean warnBeforeConsume()
		{
			return ConcoctionDatabase.getQueuedInebriety() != 0;
		}

		public void handleQueue()
		{
			ConcoctionDatabase.handleQueue( false, true, false, KoLConstants.CONSUME_HOBO );
		}

		public String getTitle()
		{
			return ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued";
		}

		public String toString()
		{
			return "feed hobo";
		}
	}

	private abstract class FamiliarFeedListener
		extends ThreadedListener
	{
		protected void execute()
		{
			if ( this.warnBeforeConsume() && !InputFieldUtilities.confirm( "This action will also feed any queued items to your familiar. Are you sure you wish to continue?" ) )
			{
				return;
			}

			Object [] items = UseItemEnqueuePanel.this.getDesiredItems( "Feed" );

			if ( items == null )
			{
				return;
			}

			this.handleQueue();

			UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, this.getTitle() );
		}

		public abstract boolean warnBeforeConsume();
		public abstract void handleQueue();
		public abstract String getTitle();
		public abstract String toString();
	}

	private class MilkListener
		extends ThreadedListener
	{
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.MILK_OF_MAGNESIUM, 1 ) ) );
		}

		public String toString()
		{
			return "use milk" ;
		}
	}

	private class OdeListener
		extends ThreadedListener
	{
		protected void execute()
		{
			RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
		}

		public String toString()
		{
			return "cast ode" ;
		}
	}

	private class DistendListener
		extends ThreadedListener
	{
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.DISTENTION_PILL, 1 ) ) );
		}

		public String toString()
		{
			return "distend";
		}
	}

	private class DogHairListener
		extends ThreadedListener
	{
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.SYNTHETIC_DOG_HAIR_PILL, 1 ) ) );
		}

		public String toString()
		{
			return "dog hair";
		}
	}

	private class MojoListener
		extends ThreadedListener
	{
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.MOJO_FILTER, 1 ) ) );
		}

		public String toString()
		{
			return "flush mojo";
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

			// no create
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

			// turn-free
			if ( UseItemEnqueuePanel.this.filters[ 1 ].isSelected() )
			{
				if ( creation.getTurnFreeAvailable() == 0 )
				{
					return false;
				}
			}
			// no summon
			if ( UseItemEnqueuePanel.this.filters[ 2 ].isSelected() )
			{
				AdventureResult item = creation.getItem();
				if ( item != null && 
					( creation.getMixingMethod() & KoLConstants.CT_MASK ) == KoLConstants.CLIPART )
				{
					return false;
				}
			}
			if ( UseItemEnqueuePanel.this.filters[ 3 ].isSelected() )
			{
				String range = ItemDatabase.getMuscleRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( UseItemEnqueuePanel.this.filters[ 4 ].isSelected() )
			{
				String range = ItemDatabase.getMysticalityRange( creation.getName() );
				if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
				{
					return false;
				}
			}

			if ( UseItemEnqueuePanel.this.filters[ 5 ].isSelected() )
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

	private static class ExperimentalCheckBox
		extends PreferenceListenerCheckBox
	{
		public ExperimentalCheckBox( final boolean food, final boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : food ? "per full" : "per spleen", "showGainsPerUnit" );

			this.setToolTipText( "Sort gains per adventure" );
		}

		protected void handleClick()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private static class ByRoomCheckbox
		extends PreferenceListenerCheckBox
	{
		public ByRoomCheckbox()
		{
			super( "by room", "sortByRoom" );

			this.setToolTipText( "Sort items you have no room for to the bottom" );
		}

		protected void handleClick()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}
	
	private static class TurnFreeCheckbox
		extends PreferenceListenerCheckBox
	{
		public TurnFreeCheckbox()
		{
			super( "turn-free", "showTurnFreeOnly" );

			this.setToolTipText( "Only show creations that will not take a turn" );
		}

		protected void handleClick()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}
}
