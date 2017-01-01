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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

public class SynthesizePanel
	extends JPanel
{
	private JPanel centerPanel;
	private JPanel eastPanel;

	private EffectPanel effectPanel;
	private CandyPanel candyPanel;
	private JButton synthesizeButton;
	private SelectedCandyPanel selectedCandyPanel;

	public static final LockableListModel<Candy> candy1List = new LockableListModel<Candy>();
	public static final LockableListModel<Candy> candy2List = new LockableListModel<Candy>();

	private int effectId = -1;
	private Candy candy1 = null;
	private Candy candy2 = null;

	public SynthesizePanel()
	{
		super();
		
		this.centerPanel = new JPanel( new BorderLayout() );

		this.effectPanel = new EffectPanel();
		this.centerPanel.add( this.effectPanel, BorderLayout.NORTH );

		this.candyPanel = new CandyPanel();
		this.centerPanel.add( this.candyPanel, BorderLayout.CENTER );

		this.eastPanel = new JPanel( new BorderLayout() );

		this.synthesizeButton = new JButton( "Synthesize!" );
		this.synthesizeButton.addActionListener( new SynthesizeListener() );
		this.eastPanel.add( this.synthesizeButton, BorderLayout.NORTH );

		this.selectedCandyPanel = new SelectedCandyPanel();
		this.eastPanel.add( this.selectedCandyPanel, BorderLayout.SOUTH );

		this.setLayout( new BorderLayout( 20, 10 ) );
		this.add( this.centerPanel, BorderLayout.CENTER );
		this.add( this.eastPanel, BorderLayout.EAST );

		this.setEnabled( true );
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		this.synthesizeButton.setEnabled( isEnabled && this.effectId != -1 && this.candy1 != null && this.candy2 != null );
	}

	public void synthesize()
	{
		// Synthesize
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

	static final Comparator<Candy> MALL_PRICE_COMPARATOR = new MallPriceComparator();

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

	static final Comparator<Candy> INVERSE_COUNT_COMPARATOR = new MallPriceComparator();

	public static void loadCandy( LockableListModel<Candy> list, Set<Integer> itemIds )
	{
		ArrayList<Candy> array = new ArrayList<Candy>();

		for ( int itemId : itemIds )
		{
			array.add( new Candy( itemId ) );
		}

		Comparator comparator =
			KoLCharacter.canInteract() ?
			SynthesizePanel.MALL_PRICE_COMPARATOR :
			SynthesizePanel.INVERSE_COUNT_COMPARATOR;

		Collections.sort( array, comparator );

		list.clear();
		list.addAll( array );
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

		public int currentEffect()
		{
			return this.selected == null ? -1 : this.selected.effectId;
		}

		private class EffectButton
			extends JButton
			implements ActionListener
		{
			final public int effectId;
			
			public EffectButton( final String name, final int effectId )
			{
				super( name );
				this.setOpaque( true );
				this.effectId = effectId;
				this.addActionListener( this );
			}

			public void actionPerformed( final ActionEvent e )
			{
				EffectButton current = EffectPanel.this.selected;
				if ( current != null )
				{
					current.setBackground( Color.WHITE );
					SynthesizePanel.candy2List.clear();
				}
				if ( current == this )
				{
					EffectPanel.this.selected = null;
					SynthesizePanel.this.effectId = -1;
					SynthesizePanel.candy1List.clear();
				}
				else
				{
					EffectPanel.this.selected = this;
					SynthesizePanel.this.effectId = this.effectId;
					this.setBackground( Color.LIGHT_GRAY );
					Set<Integer> candy = CandyDatabase.candyForTier( CandyDatabase.getEffectTier( this.effectId ) );
					SynthesizePanel.loadCandy( SynthesizePanel.candy1List, candy );
				}
			}
		}
	}

	public static class Candy
	{
		private final int itemId;
		private final String name;
		private final int count;
		private final int mallprice;
		private final int autosell;

		public Candy( final int itemId )
		{
			this( itemId,
			      ItemDatabase.getDataName( itemId ),
			      InventoryManager.getAccessibleCount( itemId ),
			      MallPriceDatabase.getPrice( itemId ),
			      ItemDatabase.getPriceById( itemId ) );
		}

		public Candy( final int itemId, final String name, final int count, final int mallprice, final int autosell )
		{
			this.itemId = itemId;
			this.name = name;
			this.count = count;
			this.mallprice = mallprice;
			this.autosell = autosell;
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

		public int getAutosell()
		{
			return this.autosell;
		}

		public String to_string()
		{
			return this.name;
		}
	}

	private class CandyPanel
		extends JPanel
		implements ListSelectionListener
	{
		private final CandyList candyList1;
		private final Object source1;
		private final CandyList candyList2;
		private final Object source2;

		public CandyPanel()
		{
			super( new GridLayout( 1, 2 ) );
			this.candyList1 = new CandyList( SynthesizePanel.candy1List );
			this.source1 = this.candyList1.getElementList().getSelectionModel();
			this.add( this.candyList1 );
			this.candyList2 = new CandyList( SynthesizePanel.candy2List );
			this.source2 = this.candyList2.getElementList().getSelectionModel();
			this.add( this.candyList2 );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			Object source = e.getSource();
			if ( source == source1 )
			{
				Object[] items = candyList1.getSelectedValues();
				Candy current = SynthesizePanel.this.candy1;
				Candy replace = items.length == 0 ? null : (Candy)items[0];
				if ( replace == null )
				{
					SynthesizePanel.this.candy1 = null;
					SynthesizePanel.this.selectedCandyPanel.candyData1.updateCandy( null );
					SynthesizePanel.this.candy2 = null;
					SynthesizePanel.this.selectedCandyPanel.candyData2.updateCandy( null );
					SynthesizePanel.candy1List.clear();
					SynthesizePanel.candy2List.clear();
					SynthesizePanel.this.synthesizeButton.setEnabled( false );
				}
				else if ( current != replace )
				{
					SynthesizePanel.this.candy1 = replace;
					SynthesizePanel.this.selectedCandyPanel.candyData1.updateCandy( replace );
					Set<Integer> candy = CandyDatabase.sweetSynthesisPairing( SynthesizePanel.this.effectId, replace.getItemId() );
					SynthesizePanel.loadCandy( SynthesizePanel.candy2List, candy );
					SynthesizePanel.this.synthesizeButton.setEnabled( false );
				}
			}
			else if ( source == source2 )
			{
				Object[] items = candyList2.getSelectedValues();
				Candy current = SynthesizePanel.this.candy2;
				Candy replace = items.length == 0 ? null : (Candy)items[0];
				if ( current != replace )
				{
					SynthesizePanel.this.candy2 = replace;
					SynthesizePanel.this.selectedCandyPanel.candyData2.updateCandy( replace );
					SynthesizePanel.this.synthesizeButton.setEnabled( true );
				}
			}
		}

		private class CandyList
			extends ItemTableManagePanel
		{
			public CandyList( final LockableListModel candyList )
			{
				super( candyList, false, false );
				this.getElementList().setVisibleRowCount( 20 );
				this.getElementList().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				this.getElementList().getSelectionModel().addListSelectionListener( CandyPanel.this );
				this.setPreferredSize( new Dimension( 200, 400 ) );
			}
		}
	}

	private class SelectedCandyPanel
		extends JPanel
	{
		public final CandyData candyData1;
		public final CandyData candyData2;

		public SelectedCandyPanel()
		{
			super();
			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			this.candyData1 = new CandyData( "Candy A" );
			this.add( this.candyData1 );
			this.add( Box.createVerticalStrut( 10 ) );
			this.candyData2 = new CandyData( "Candy B" );
			this.add( this.candyData2 );
		}

		private class CandyData
			extends JPanel
		{
			private final JLabel haveValue;
			private final JLabel costValue;

			public CandyData( final String title )
			{
				super( new BorderLayout() );
				this.add( new JLabel( title ), BorderLayout.NORTH );

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
	}

	private class SynthesizeListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			SynthesizePanel.this.synthesize();
		}
	}
}
