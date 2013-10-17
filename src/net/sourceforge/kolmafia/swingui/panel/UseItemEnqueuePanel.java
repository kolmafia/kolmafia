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
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListenerCheckBox;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class UseItemEnqueuePanel
	extends ItemListManagePanel
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

		ArrayList<ThreadedListener> listeners = new ArrayList<ThreadedListener>();

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
		else if ( this.spleen )
		{
			listeners.add( new MojoListener() );
		}

		ActionListener [] listenerArray = new ActionListener[ listeners.size() ];
		listeners.toArray( listenerArray );

		this.setButtons( false, listenerArray );

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

		this.getElementList().setFixedCellHeight( (int) ( test.getPreferredSize().getHeight() * 2.5f ) );

		this.getElementList().setVisibleRowCount( 6 );
		this.getElementList().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		this.filters = new JCheckBox[ food || booze || spleen ? 8 : 7 ];

		this.filters[ 0 ] = new JCheckBox( "no create" );
		this.filters[ 1 ] = new TurnFreeCheckbox();
		this.filters[ 2 ] = new NoSummonCheckbox();
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

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		// The "binge" listener is the second or third button
		int bingeIndex = Preferences.getBoolean( "addCreationQueue" ) ? 2 : 1;

		// Disable all buttons if false, otherwise allow buttons to only be lit when they are valid to stop flashing buttons
		if ( !isEnabled )
		{
			super.setEnabled( false );
		}
		else
		{
			this.getElementList().setEnabled( true );
			this.buttons[ 0 ].setEnabled( true );
			if ( bingeIndex == 2 )
			{
				this.buttons[ 1 ].setEnabled( true );
			}
		}

		if ( isEnabled && this.food )
		{
			boolean haveGhost = KoLCharacter.findFamiliar( FamiliarPool.GHOST ) != null;
			this.buttons[ bingeIndex ].setEnabled( haveGhost );

			// The milk listener is just after the ghost listener
			boolean milkAvailable = InventoryManager.getAccessibleCount( ItemPool.MILK_OF_MAGNESIUM ) > 0
						|| ( KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithMall" ) )
						|| KoLCharacter.hasSkill( "Song of the Glorious Lunch" )
						|| CreateItemRequest.getInstance( ItemPool.get( ItemPool.MILK_OF_MAGNESIUM, 1 ), false ).getQuantityPossible() > 0;

			this.buttons[ bingeIndex + 1 ].setEnabled( milkAvailable );

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
			boolean haveHobo = KoLCharacter.findFamiliar( FamiliarPool.HOBO ) != null;
			this.buttons[ bingeIndex ].setEnabled( haveHobo );

			// The ode listener is just after the hobo listener
			boolean haveOde = KoLCharacter.hasSkill( "The Ode to Booze" );
			this.buttons[ bingeIndex + 1 ].setEnabled( haveOde );

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

		if ( isEnabled && this.spleen )
		{
			int flushIndex = this.buttons.length - 1;
			boolean filterAvailable = InventoryManager.getAccessibleCount( ItemPool.MOJO_FILTER ) > 0 ||
					( KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithMall" ) );
			boolean haveSpleen = KoLCharacter.getSpleenUse() > 0;
			boolean canUseFilter = Preferences.getInteger( "currentMojoFilters" ) < 3;
			boolean canFlush = filterAvailable && haveSpleen && canUseFilter;
			this.buttons[ flushIndex ].setEnabled( canFlush );
		}
	}

	@Override
	public AutoFilterTextField getWordFilter()
	{
		return new ConsumableFilterField();
	}

	@Override
	protected void listenToCheckBox( final JCheckBox box )
	{
		super.listenToCheckBox( box );
		box.addActionListener( new ReSortListener() );
	}

	@Override
	public void actionConfirmed()
	{
	}

	@Override
	public void actionCancelled()
	{
	}

	private static class ReSortListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private class EnqueueListener
		extends ThreadedListener
	{
		@Override
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

		@Override
		public String toString()
		{
			return "enqueue";
		}
	}

	private class ExecuteListener
		extends ThreadedListener
	{
		@Override
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

			UseItemEnqueuePanel.this.setEnabled( false );

			AdventureResult [] items = UseItemEnqueuePanel.this.getDesiredItems( "Consume" );

			if ( items == null )
			{
				return;
			}

			if ( UseItemEnqueuePanel.this.food )
			{
				ConcoctionDatabase.handleQueue( true, false, false, KoLConstants.CONSUME_EAT );
				UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemEnqueuePanel.this.booze )
			{
				ConcoctionDatabase.handleQueue( false, true, false, KoLConstants.CONSUME_DRINK );
				UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemEnqueuePanel.this.spleen )
			{
				ConcoctionDatabase.handleQueue( false, false, true, KoLConstants.CONSUME_USE );
				UseItemEnqueuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		@Override
		public String toString()
		{
			return "consume";
		}
	}

	private class BingeGhostListener
		extends FamiliarFeedListener
	{
		@Override
		public boolean warnBeforeConsume()
		{
			return ConcoctionDatabase.getQueuedFullness() != 0;
		}

		@Override
		public void handleQueue()
		{
			ConcoctionDatabase.handleQueue( true, false, false, KoLConstants.CONSUME_GHOST );
		}

		@Override
		public String getTitle()
		{
			return ConcoctionDatabase.getQueuedFullness() + " Full Queued";
		}

		@Override
		public String toString()
		{
			return "feed ghost";
		}
	}

	private class BingeHoboListener
		extends FamiliarFeedListener
	{
		@Override
		public boolean warnBeforeConsume()
		{
			return ConcoctionDatabase.getQueuedInebriety() != 0;
		}

		@Override
		public void handleQueue()
		{
			ConcoctionDatabase.handleQueue( false, true, false, KoLConstants.CONSUME_HOBO );
		}

		@Override
		public String getTitle()
		{
			return ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued";
		}

		@Override
		public String toString()
		{
			return "feed hobo";
		}
	}

	private abstract class FamiliarFeedListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( this.warnBeforeConsume() && !InputFieldUtilities.confirm( "This action will also feed any queued items to your familiar. Are you sure you wish to continue?" ) )
			{
				return;
			}

			AdventureResult [] items = UseItemEnqueuePanel.this.getDesiredItems( "Feed" );

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
		@Override
		public abstract String toString();
	}

	private class MilkListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( KoLCharacter.hasSkill( "Song of the Glorious Lunch" ) )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( "Song of the Glorious Lunch", 1 ) );
			}
			else
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.MILK_OF_MAGNESIUM, 1 ) ) );
			}
		}

		@Override
		public String toString()
		{
			return KoLCharacter.hasSkill( "Song of the Glorious Lunch" ) ?
				"glorious lunch" : "use milk" ;
		}
	}

	private class OdeListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
			if ( !KoLConstants.activeEffects.contains( EffectPool.get( Effect.ODE ) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Failed to cast Ode." );
			}
		}

		@Override
		public String toString()
		{
			return "cast ode" ;
		}
	}

	private class DistendListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.DISTENTION_PILL, 1 ) ) );
		}

		@Override
		public String toString()
		{
			return "distend";
		}
	}

	private class DogHairListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.SYNTHETIC_DOG_HAIR_PILL, 1 ) ) );
		}

		@Override
		public String toString()
		{
			return "dog hair";
		}
	}

	private class MojoListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.MOJO_FILTER, 1 ) ) );
		}

		@Override
		public String toString()
		{
			return "flush mojo";
		}
	}

	private class ConsumableFilterField
		extends FilterItemField
	{
		@Override
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

			if ( ItemDatabase.getRawFullness( creation.getName() ) != null )
			{
				if ( !UseItemEnqueuePanel.this.food )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getRawInebriety( creation.getName() ) != null )
			{
				if ( !UseItemEnqueuePanel.this.booze )
				{
					return false;
				}
			}
			else if ( ItemDatabase.getRawSpleenHit( creation.getName() ) != null )
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

			if ( UseItemEnqueuePanel.this.food && KoLCharacter.inZombiecore() )
			{
				// If you don't have a GGG equipped, show only brains or a steel lasagna
				int fam = KoLCharacter.getFamiliar().getId();
				if ( fam != FamiliarPool.GHOST )
				{
					AdventureResult item = creation.getItem();
					String name = item != null ? item.getName() : null;
					if ( name != null && !name.equals( "steel lasagna" ) &&
					     ( ItemDatabase.getNotes( name ) == null || !ItemDatabase.getNotes( name ).startsWith( "Zombie Slayer" ) ) )
					{
						return false;
					}
				}
			}

			if ( KoLCharacter.isJarlsberg() && ( UseItemEnqueuePanel.this.food || UseItemEnqueuePanel.this.booze ) )
			{
				AdventureResult item = creation.getItem();
				String name = item != null ? item.getName() : null;
				if ( creation.getMixingMethod() != CraftingType.JARLS && name != null &&
				     !name.equals( "steel margarita" ) &&
				     !name.equals( "mediocre lager" ) )
				{
					return false;
				}
			}

			if ( KoLCharacter.inHighschool() && UseItemEnqueuePanel.this.booze )
			{
				AdventureResult item = creation.getItem();
				if ( item == null )
				{
					return false;
				}
				String name = item.getName();
				if ( name == null )
				{
					return false;
				}
				String notes = ItemDatabase.getNotes( name );
				if ( !name.equals( "steel margarita" ) &&
				     ( notes == null || !notes.startsWith( "KOLHS" ) ) )
				{
					return false;
				}
			}

			if ( creation.fancydog && ConcoctionDatabase.queuedFancyDog )
			{
				return false;
			}

			// turn-free
			if ( UseItemEnqueuePanel.this.filters[ 1 ].isSelected() )
			{
				AdventureResult item = creation.getItem();
				if ( (item != null && item.getItemId() > 0 ) &&
				     creation.getTurnFreeAvailable() == 0 &&
				     !KoLConstants.restaurantItems.contains( creation.getName() ) &&
				     !KoLConstants.microbreweryItems.contains( creation.getName() ) )
				{
					return false;
				}
			}
			// no summon
			if ( UseItemEnqueuePanel.this.filters[ 2 ].isSelected() )
			{
				AdventureResult item = creation.getItem();
				if ( item != null && creation.getMixingMethod() == CraftingType.CLIPART && item.getCount( KoLConstants.inventory ) == 0 )
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

			// Don't display memento items if memento items are protected from destruction
			if ( Preferences.getBoolean( "mementoListActive" ) )
			{
				for ( Object it : KoLConstants.mementoList )
				{
					if ( ( (AdventureResult) it ).getName().equals( creation.getName() ) )
					{
						return false;
					}
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

		@Override
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

		@Override
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

		@Override
		protected void handleClick()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private static class NoSummonCheckbox
		extends PreferenceListenerCheckBox
	{
		public NoSummonCheckbox()
		{
			super( "no-summon", "showNoSummonOnly" );

			this.setToolTipText( "Do not show creations that use up summoning charges" );
		}

		@Override
		protected void handleClick()
		{
			ConcoctionDatabase.getUsables().sort();
		}
	}
}
