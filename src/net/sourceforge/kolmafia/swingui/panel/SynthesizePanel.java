/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import javax.swing.border.TitledBorder;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.request.SweetSynthesisRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

public class SynthesizePanel
	extends JPanel
	implements ActionListener, Listener
{
	// The panel with the effect buttons
	private EffectPanel effectPanel;

	// The filter checkboxes
	private JCheckBox[] filters;
	private boolean availableChecked;
	private boolean unrestrictedChecked;

	// The panel with Candy A and Candy B columns
	private CandyPanel candyPanel;
	private CandyPanel.CandyList candyList1;
	private CandyPanel.CandyList candyList2;

	// The Synthesize! button
	private JButton synthesizeButton;

	// The panel with data about Candy A and Candy B and total cost/turn
	private CandyDataPanel candyData;

	// The list models for Candy A and Candy B.  They are static because
	// TableCellFactory.getColumnNames chooses column names based on the
	// specific list, not what goes in the list. Sloppy API.
	public static final LockableListModel<Candy> candy1List = new LockableListModel<Candy>();
	public static final LockableListModel<Candy> candy2List = new LockableListModel<Candy>();

	public SynthesizePanel()
	{
		super();
		
		JPanel centerPanel = new JPanel( new BorderLayout( 0, 0 ) );

		JPanel northPanel = new JPanel();
		northPanel.setLayout( new BoxLayout( northPanel, BoxLayout.Y_AXIS ) );

		this.effectPanel = new EffectPanel();

		northPanel.add( this.effectPanel );
		northPanel.add( this.addFilters() );

		centerPanel.add( northPanel, BorderLayout.NORTH );

		candyPanel = new CandyPanel();
		centerPanel.add( this.candyPanel, BorderLayout.CENTER );

		JPanel eastPanel = new JPanel( new BorderLayout() );

		this.synthesizeButton = new JButton( "Synthesize!" );
		this.synthesizeButton.addActionListener( new SynthesizeListener() );
		eastPanel.add( this.synthesizeButton, BorderLayout.NORTH );

		this.candyData = new CandyDataPanel();
		eastPanel.add( this.candyData, BorderLayout.SOUTH );

		this.setLayout( new BorderLayout( 10, 10 ) );
		this.add( centerPanel, BorderLayout.CENTER );
		this.add( eastPanel, BorderLayout.EAST );

		NamedListenerRegistry.registerNamedListener( "(candy)", this );

		this.setEnabled( true );

		SynthesizePanel.candy1List.clear();
		SynthesizePanel.candy2List.clear();
	}

	private int effectId()
	{
		return this.effectPanel == null ? -1 : this.effectPanel.currentEffectId();
	}

	private Candy candy1()
	{
		return this.candyList1 == null ? null : this.candyList1.currentCandy();
	}

	private Candy candy2()
	{
		return this.candyList2 == null ? null : this.candyList2.currentCandy();
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		this.synthesizeButton.setEnabled( isEnabled && this.effectId() != -1 && this.candy1() != null && this.candy2() != null );
	}

	private JPanel addFilters()
	{
		JPanel filterPanel = new JPanel();

		this.availableChecked = !KoLCharacter.canInteract();
		this.unrestrictedChecked = KoLCharacter.getRestricted();

		this.filters = new JCheckBox[ 2 ];
		this.filters[ 0 ] = new JCheckBox( "available", this.availableChecked );
		this.filters[ 1 ] = new JCheckBox( "unrestricted", this.unrestrictedChecked );

		for ( JCheckBox checkbox : this.filters )
		{
			filterPanel.add( checkbox );
			checkbox.addActionListener( this );
		}

		return filterPanel;
	}

	// Called when checkbox changes
	public void actionPerformed( final ActionEvent e )
	{
		this.availableChecked = this.filters[0].isSelected();
		this.unrestrictedChecked = this.filters[1].isSelected();
		this.filterItems();
	}

	private void filterItems()
	{
		Candy candy1 = this.candy1();
		Candy candy2 = this.candy2();

		this.candyList1.filterItems( candy1 );
		this.candyList2.filterItems( candy2 );
	}

	// called when (candy) fires
	public void update()
	{
		for ( Candy candy : SynthesizePanel.candy1List )
		{
			candy.update();
		}

		for ( Candy candy : SynthesizePanel.candy2List )
		{
			candy.update();
		}

		this.filterItems();
	}

	private class EffectPanel
		extends JPanel
	{
		public EffectButton selected = null;

		public EffectPanel()
		{
			super( new GridLayout( 3, 5 ) );
			// Tier 1 effects
			this.add( new EffectButton( "Hot Res +9", EffectPool.SYNTHESIS_HOT ) );
			this.add( new EffectButton( "Cold Res +9", EffectPool.SYNTHESIS_COLD ) );
			this.add( new EffectButton( "Stench Res +9", EffectPool.SYNTHESIS_PUNGENT ) );
			this.add( new EffectButton( "Spooky Res +9", EffectPool.SYNTHESIS_SCARY ) );
			this.add( new EffectButton( "Sleaze Res +9", EffectPool.SYNTHESIS_GREASY ) );
			// Tier 2 effects
			this.add( new EffectButton( "Mus +300%", EffectPool.SYNTHESIS_STRONG ) );
			this.add( new EffectButton( "Mys +300%", EffectPool.SYNTHESIS_SMART ) );
			this.add( new EffectButton( "Mox +300%", EffectPool.SYNTHESIS_COOL ) );
			this.add( new EffectButton( "Max HP +300%", EffectPool.SYNTHESIS_HARDY ) );
			this.add( new EffectButton( "Max MP +300%", EffectPool.SYNTHESIS_ENERGY ) );
			// Tier 3 effects
			this.add( new EffectButton( "Meat +300%", EffectPool.SYNTHESIS_GREED ) );
			this.add( new EffectButton( "Item +150%", EffectPool.SYNTHESIS_COLLECTION ) );
			this.add( new EffectButton( "Mus Exp +50%", EffectPool.SYNTHESIS_MOVEMENT ) );
			this.add( new EffectButton( "Mys Exp +50%", EffectPool.SYNTHESIS_LEARNING ) );
			this.add( new EffectButton( "Mox Exp +50%", EffectPool.SYNTHESIS_STYLE ) );
		}

		public int currentEffectId()
		{
			return this.selected == null ? -1 : this.selected.effectId;
		}

		private class EffectButton
			extends JButton
			implements ActionListener
		{
			final public int effectId;
			final Color foreground;
			final Color background;
			
			public EffectButton( final String name, final int effectId )
			{
				super( name );

				// The following makes the button a solid
				// rectangle on OS X.
				this.setContentAreaFilled( false );
				this.setBorderPainted( false );
				this.setOpaque( true );

				this.effectId = effectId;
				this.foreground = this.getForeground();
				this.background = this.getBackground();

				this.addActionListener( this );
			}

			private void originalColors()
			{
				this.setBackground( background );
				this.setForeground( foreground );
			}

			private void reverseColors()
			{
				this.setBackground( Color.BLACK );
				this.setForeground( Color.WHITE );
			}

			public void actionPerformed( final ActionEvent e )
			{
				EffectButton current = EffectPanel.this.selected;
				if ( current != null )
				{
					current.originalColors();
					SynthesizePanel.candy2List.clear();
				}
				if ( current == this )
				{
					EffectPanel.this.selected = null;
					SynthesizePanel.candy1List.clear();
				}
				else
				{
					EffectPanel.this.selected = this;
					this.reverseColors();
					Set<Integer> candy = CandyDatabase.candyForTier( CandyDatabase.getEffectTier( this.effectId ) );
					SynthesizePanel.this.candyList1.loadCandy( candy );
				}
			}
		}
	}

	public static class Candy
	{
		private final int itemId;
		private final String name;
		private int count;
		private int mallprice;
		private boolean restricted;
		private final int autosell;

		public Candy( final int itemId )
		{
			this( itemId,
			      ItemDatabase.getDataName( itemId ),
			      InventoryManager.getAccessibleCount( itemId ),
			      MallPriceDatabase.getPrice( itemId ),
			      !ItemDatabase.isAllowedInStandard( itemId ),
			      ItemDatabase.getPriceById( itemId ) );
		}

		public Candy( final int itemId, final String name, final int count, final int mallprice, final boolean restricted, final int autosell )
		{
			this.itemId = itemId;
			this.name = name;
			this.count = count;
			this.mallprice = mallprice;
			this.restricted = restricted;
			this.autosell = autosell;
		}


		@Override
		public boolean equals( final Object o )
		{
			return ( o instanceof Candy ) && ( this.itemId == ((Candy)o).itemId );
		}

		public int getItemId()
		{
			return this.itemId;
		}

		public String getName()
		{
			return this.name;
		}

		public int getCount()
		{
			return this.count;
		}

		public int getCost()
		{
			return this.mallprice == 0 ? Integer.MAX_VALUE : this.mallprice;
		}

		public int getMallPrice()
		{
			return this.mallprice;
		}

		public boolean getRestricted()
		{
			return this.restricted;
		}

		public int getAutosell()
		{
			return this.autosell;
		}

		public Candy update()
		{
			this.count = InventoryManager.getAccessibleCount( this.itemId );
			this.mallprice = MallPriceDatabase.getPrice( this.itemId );
			return this;
		}

		public String toString()
		{
			return this.name;
		}
	}

	// Compare by lowest mall price, then largest quantity, then alphabetically
	private static class MallPriceComparator
		implements Comparator<Candy>
	{
		public int compare( Candy o1, Candy o2 )
		{
			int cost1 = o1.getCost();
			int cost2 = o2.getCost();
			if ( cost1 != cost2 )
			{
				return cost1 - cost2;
			}
			int count1 = o1.getCount();
			int count2 = o2.getCount();
			if ( count1 != count2 )
			{
				return count2 - count1;
			}
			return o1.getName().compareToIgnoreCase( o2.getName() );
		}
	}

	public static final Comparator<Candy> MALL_PRICE_COMPARATOR = new MallPriceComparator();

	// Compare by largest quantity, then alphabetically
	private static class InverseCountComparator
		implements Comparator<Candy>
	{
		public int compare( Candy o1, Candy o2 )
		{
			int count1 = o1.getCount();
			int count2 = o2.getCount();
			if ( count1 != count2 )
			{
				return count2 - count1;
			}
			return o1.getName().compareToIgnoreCase( o2.getName() );
		}
	}

	public static final Comparator<Candy> INVERSE_COUNT_COMPARATOR = new InverseCountComparator();

	private class CandyPanel
		extends JPanel
	{
		public CandyPanel()
		{
			super( new GridLayout( 1, 2 ) );

			SynthesizePanel.this.candyList1 = new CandyListA();
			this.add( SynthesizePanel.this.candyList1 );

			SynthesizePanel.this.candyList2 = new CandyListB();
			this.add( SynthesizePanel.this.candyList2 );
		}

		public abstract class CandyList
			extends ItemTableManagePanel
			implements ListElementFilter, ListSelectionListener
		{
			private final LockableListModel<Candy> model;
			private final ShowDescriptionTable table;
			private final ListSelectionModel selectionModel;
			private final LockableListModel displayModel; 
			protected Candy candy = null;

			public CandyList( final String title, final LockableListModel<Candy> candyList )
			{
				super( candyList, false, false );

				this.model = candyList;
				this.table = this.getElementList();
				this.selectionModel = this.table.getSelectionModel();
				this.displayModel = this.table.getDisplayModel();

				this.scrollPane.setBorder( BorderFactory.createTitledBorder( null, title, TitledBorder.CENTER, TitledBorder.TOP ) );
				this.setPreferredSize( new Dimension( 200, 400 ) );

				this.table.setVisibleRowCount( 20 );
				this.table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				this.selectionModel.addListSelectionListener( this );
				this.displayModel.setFilter( this );
			}

			public Candy currentCandy()
			{
				return this.candy;
			}

			public abstract void valueChanged( ListSelectionEvent e );

			public void loadCandy( Set<Integer> itemIds )
			{
				ArrayList<Candy> array = new ArrayList<Candy>();

				for ( int itemId : itemIds )
				{
					array.add( new Candy( itemId ) );
				}

				Comparator<Candy> comparator =
					KoLCharacter.canInteract() ?
					SynthesizePanel.MALL_PRICE_COMPARATOR :
					SynthesizePanel.INVERSE_COUNT_COMPARATOR;

				Collections.sort( array, comparator );

				this.model.clear();
				this.model.addAll( array );
				this.filterItems( null );
			}

			public void filterItems( final Candy selected )
			{
				int index = -1;

				try
				{
					this.displayModel.updateFilter( false );
					int size = this.displayModel.size();
					if ( size > 0 )
					{
						this.displayModel.fireContentsChanged( this.displayModel, 0, size - 1 );
						index = this.displayModel.getIndexOf( selected );
					}
				}
				finally
				{
				}

				if ( index == -1 )
				{
					this.table.clearSelection();
				}
				else
				{
					this.table.setSelectedIndex( index );
				}
			}

			public boolean isVisible( final Object o )
			{
				if ( o instanceof Candy )
				{
					if ( SynthesizePanel.this.availableChecked && ((Candy)o).count == 0 )
					{
						return false;
					}
					if ( SynthesizePanel.this.unrestrictedChecked && ((Candy)o).restricted )
					{
						return false;
					}
				}
				return true;
			}
		}

		public class CandyListA
			extends CandyList
		{
			public CandyListA()
			{
				super( "Candy A", SynthesizePanel.candy1List );
			}

			public void valueChanged( ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
				{
					// Mouse down, for example.
					// Wait until final event comes in
					return;
				}

				Object item = SynthesizePanel.this.candyList1.getSelectedValue();
				Candy current = SynthesizePanel.this.candy1();
				Candy replace = (Candy)item;
				if ( current != replace )
				{
					this.candy = replace;
					SynthesizePanel.this.candyData.update();
					if ( replace == null )
					{
						SynthesizePanel.candy2List.clear();
					}
					else
					{
						Set<Integer> candy = CandyDatabase.sweetSynthesisPairing( SynthesizePanel.this.effectId(), replace.getItemId() );
						SynthesizePanel.this.candyList2.loadCandy( candy );
					}
					SynthesizePanel.this.synthesizeButton.setEnabled( false );
				}
				return;
			}
		}

		public class CandyListB
			extends CandyList
		{
			public CandyListB()
			{
				super( "Candy B", SynthesizePanel.candy2List );
			}

			public void valueChanged( ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
				{
					// Mouse down, for example.
					// Wait until final event comes in
					return;
				}

				Object item = SynthesizePanel.this.candyList2.getSelectedValue();
				Candy current = SynthesizePanel.this.candy2();
				Candy replace = (Candy)item;
				if ( current != replace )
				{
					this.candy = replace;
					SynthesizePanel.this.candyData.update();
					SynthesizePanel.this.synthesizeButton.setEnabled( replace != null );
				}
			}
		}
	}

	private class CandyDataPanel
		extends JPanel
	{
		private final CandyData candyData1;
		private final CandyData candyData2;
		private final CandyTotal candyTotal;

		public CandyDataPanel()
		{
			super();
			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			this.candyData1 = new CandyData( "Candy A" );
			this.add( this.candyData1 );
			this.candyData2 = new CandyData( "Candy B" );
			this.add( this.candyData2 );
			this.candyTotal = new CandyTotal();
			this.add( this.candyTotal );
		}

		public void update()
		{
			Candy candy1 = SynthesizePanel.this.candy1();
			Candy candy2 = SynthesizePanel.this.candy2();
			this.candyData1.updateCandy( candy1 );
			this.candyData2.updateCandy( candy2 );
			this.candyTotal.update( candy1, candy2 );
		}

		private class CandyData
			extends JPanel
		{
			private final JLabel haveValue;
			private final JLabel costValue;

			public CandyData( final String title )
			{
				super( new BorderLayout() );
				this.setBorder( BorderFactory.createTitledBorder( null, title, TitledBorder.CENTER, TitledBorder.TOP ) );

				JPanel labelPanel = new JPanel( new GridLayout( 2, 1 ) );
				labelPanel.add( new JLabel( "Have: " ) );
				labelPanel.add( new JLabel( "Cost:" ) );

				JPanel valuePanel = new JPanel( new GridLayout( 2, 1 ) );
				this.haveValue = new JLabel( "" );
				valuePanel.add( this.haveValue );
				this.costValue = new JLabel( "" );
				valuePanel.add( this.costValue );

				this.add( labelPanel, BorderLayout.WEST );
				this.add( valuePanel, BorderLayout.CENTER );
			}

			public void updateCandy( Candy candy )
			{
				if ( candy == null )
				{
					this.haveValue.setText( "" );
					this.costValue.setText( "" );
				}
				else
				{
					this.haveValue.setText( String.valueOf( candy.getCount() ) );
					this.costValue.setText( String.valueOf( candy.getCost() ) );
				}
			}
		}

		private class CandyTotal
			extends JPanel
		{
			private final JLabel totalValue;
			private final JLabel perTurnValue;

			public CandyTotal()
			{
				super( new BorderLayout() );
				this.setBorder( BorderFactory.createTitledBorder( null, "Total", TitledBorder.CENTER, TitledBorder.TOP ) );

				JPanel labelPanel = new JPanel( new GridLayout( 2, 1 ) );
				labelPanel.add( new JLabel( "Cost:" ) );
				labelPanel.add( new JLabel( "/Adv: " ) );

				JPanel valuePanel = new JPanel( new GridLayout( 2, 1 ) );
				this.totalValue = new JLabel( "" );
				valuePanel.add( this.totalValue );
				this.perTurnValue = new JLabel( "" );
				valuePanel.add( this.perTurnValue );

				this.add( labelPanel, BorderLayout.WEST );
				this.add( valuePanel, BorderLayout.CENTER );
			}

			public void update( Candy candy1, Candy candy2 )
			{
				if ( candy1 == null || candy2 == null )
				{
					this.totalValue.setText( "" );
					this.perTurnValue.setText( "" );
				}
				else
				{
					int total = candy1.getCost() + candy2.getCost();
					int perTurn = Math.round( total / 30.0f );
					this.totalValue.setText( String.valueOf( total ) );
					this.perTurnValue.setText( String.valueOf( perTurn ) );
				}
			}
		}
	}

	private class SynthesizeListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( SynthesizePanel.this.candy1() == null || SynthesizePanel.this.candy2() == null )
			{
				return;
			}

			Candy candy1 = SynthesizePanel.this.candy1();
			int itemId1 = candy1.getItemId();
			Candy candy2 = SynthesizePanel.this.candy2();
			int itemId2 = candy2.getItemId();

			KoLmafia.updateDisplay( "Synthesizing " + candy1 + " with " + candy2 + "..." );

			SweetSynthesisRequest request = new SweetSynthesisRequest( itemId1, itemId2 );
			RequestThread.postRequest( request );

			if ( KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( "Done!" );;
			}
		}

		@Override
		public String toString()
		{
			return "synthesize";
		}
	}

	public void dispose()
	{
		SynthesizePanel.candy1List.clear();
		SynthesizePanel.candy1List.setFilter( null );

		SynthesizePanel.candy2List.clear();
		SynthesizePanel.candy2List.setFilter( null );
	}
}
