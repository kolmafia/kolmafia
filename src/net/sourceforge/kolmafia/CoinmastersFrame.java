/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class CoinmastersFrame
	extends KoLFrame
{
	public static final AdventureResult LUCRE = new AdventureResult( 2098, -1 );

	public static final int WAR_HIPPY_OUTFIT = 32;
	public static final int WAR_FRAT_OUTFIT = 33;

	private static CoinmastersFrame INSTANCE = null;
	private static boolean atWar = false;
	private static int dimes = 0;
	private static int quarters = 0;
	private static int lucre = 0;

	private CoinmasterPanel dimePanel = null;
	private CoinmasterPanel quarterPanel = null;
	private CoinmasterPanel lucrePanel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );
		CoinmastersFrame.INSTANCE = this;

		JPanel panel = new JPanel( new BorderLayout() );
		dimePanel = new DimemasterPanel();
		panel.add( dimePanel );
		this.tabs.add( "Dimemaster", panel );

		panel = new JPanel( new BorderLayout() );
		quarterPanel = new QuartersmasterPanel();
		panel.add( quarterPanel );
		this.tabs.add( "Quartersmaster", panel );

		panel = new JPanel( new BorderLayout() );
		lucrePanel = new BountyHunterHunterPanel();
		panel.add( lucrePanel );
		this.tabs.add( "Bounty Hunter Hunter", panel );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
		CoinmastersFrame.externalUpdate();
	}

	public void dispose()
	{
		CoinmastersFrame.INSTANCE = null;
		super.dispose();
	}

	public static void externalUpdate()
	{
		if ( INSTANCE == null )
			return;

		BigIsland.ensureUpdatedBigIsland();
		atWar = Preferences.getString( "warProgress" ).equals( "started" );
		dimes = Preferences.getInteger( "availableDimes" );
		quarters = Preferences.getInteger( "availableQuarters" );
		lucre =	 LUCRE.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableLucre", lucre );

		INSTANCE.setTitle( "Coin Masters (" + dimes + " dimes/" + quarters + " quarters/" + lucre + " lucre)" );
		INSTANCE.update();
	}

	private void update()
	{
		dimePanel.update();
		quarterPanel.update();
		lucrePanel.update();
	}

	private class DimemasterPanel
		extends CoinmasterPanel
	{
		public DimemasterPanel()
		{
			super( CoinmastersDatabase.getDimeItems(),
			       CoinmastersDatabase.dimeSellPrices(),
			       CoinmastersDatabase.dimeBuyPrices(),
			       WAR_HIPPY_OUTFIT,
			       "availableDimes",
			       "dime",
			       "dimemaster",
			       "hippy");
			buyAction = "getgear";
			sellAction = "turnin";
		}
	}

	private class QuartersmasterPanel
		extends CoinmasterPanel
	{
		public QuartersmasterPanel()
		{
			super( CoinmastersDatabase.getQuarterItems(),
			       CoinmastersDatabase.quarterSellPrices(),
			       CoinmastersDatabase.quarterBuyPrices(),
			       WAR_FRAT_OUTFIT,
			       "availableQuarters",
			       "quarter",
			       "quartersmaster",
			       "fratboy" );
			buyAction = "getgear";
			sellAction = "turnin";
		}
	}

	private class BountyHunterHunterPanel
		extends CoinmasterPanel
	{
		public BountyHunterHunterPanel()
		{
			super( CoinmastersDatabase.getLucreItems(),
			       null,
			       CoinmastersDatabase.lucreBuyPrices(),
			       0,
			       "availableLucre",
			       "lucre",
			       "bounty hunter hunter",
			       null);
			buyAction = "buy";
		}
	}

	private class CoinmasterPanel
		extends JPanel
	{
		private final LockableListModel purchases;
		private final Map sellPrices;
		private final Map buyPrices;
		private final int outfit;
		private final String property;
		private final String token;
		private final String master;
		private final String side;
		protected String buyAction;
		protected String sellAction;

		private SellPanel sellPanel = null;
		private BuyPanel buyPanel = null;

		private boolean hasOutfit = false;

		public CoinmasterPanel( LockableListModel purchases, Map sellPrices, Map buyPrices, int outfit, String property, String token, String master, String side )
		{
			super( new BorderLayout() );

			this.purchases = purchases;
			this.sellPrices = sellPrices;
			this.buyPrices = buyPrices;
			this.outfit = outfit;
			this.property = property;
			this.token = token;
			this.master = master;
			this.side = side;

			if ( sellPrices != null )
			{
				sellPanel = new SellPanel();
				this.add( sellPanel, BorderLayout.NORTH );
			}

			if ( buyPrices != null )
			{
				buyPanel = new BuyPanel();
				this.add( buyPanel, BorderLayout.CENTER );
			}
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public boolean showLighthouse()
		{
			return Preferences.getString( "sidequestLighthouseCompleted" ).equals( side );
		}

		public void update()
		{
			this.hasOutfit = this.outfit == 0 || EquipmentDatabase.hasOutfit( this.outfit );
		}

		private GenericRequest outfitRequest()
		{
			return new EquipmentRequest( EquipmentDatabase.getOutfit( this.outfit ) );
		}

		public void visit()
		{
			if ( this.outfit == 0 )
			{
				return;
			}

			if ( !this.hasOutfit )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right outfit" );
				return;
			}

			if ( !CoinmastersFrame.atWar )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're not at war." );
				return;
			}

			RequestThread.openRequestSequence();
			if ( !EquipmentDatabase.isWearingOutfit( this.outfit ) )
			{
				EquipmentDatabase.retrieveOutfit( this.outfit );
				RequestThread.postRequest( outfitRequest() );
			}
			RequestThread.postRequest( new CoinMasterRequest( this.token ) );
			RequestThread.closeRequestSequence();
		}

		private void execute( final String action, final Object [] items )
		{
			if ( items.length == 0 )
			{
				return;
			}

			if ( this.outfit != 0 && !this.hasOutfit )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right outfit" );
				return;
			}

			RequestThread.openRequestSequence();

			if ( this.outfit != 0 && !EquipmentDatabase.isWearingOutfit( this.outfit ) )
			{
				RequestThread.postRequest( outfitRequest() );
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult it = (AdventureResult)items[i];
				GenericRequest request = new CoinMasterRequest( token, action, it );
				RequestThread.postRequest( request );
			}

			RequestThread.closeRequestSequence();
		}

		private class SellPanel
			extends ItemManagePanel
		{
			public SellPanel()
			{
				super( KoLConstants.inventory );
				this.setButtons( true, new ActionListener[] {
						new SellListener(),
					} );

				this.elementList.setCellRenderer( getCoinmasterRenderer( sellPrices, token ) );
				this.setEnabled( true );
				this.filterItems();
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( hasOutfit && atWar );
			}

			public void addFilters()
			{
			}

			public AutoFilterTextField getWordFilter()
			{
				return new SellableFilterField();
			}

			public void actionConfirmed()
			{
			}

			public void actionCancelled()
			{
			}

			public class SellListener
				extends ThreadedListener
			{
				public void run()
				{
					if ( !KoLFrame.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					Object[] items = SellPanel.this.getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					execute( sellAction, items );
				}

				public String toString()
				{
					return "sell";
				}
			}

			private class SellableFilterField
				extends FilterItemField
			{
				public boolean isVisible( final Object element )
				{
					if ( !( element instanceof AdventureResult ) )
					{
						return false;
					}
					AdventureResult ar = (AdventureResult)element;
					int price = CoinmastersDatabase.getPrice( ar.getName(), CoinmasterPanel.this.sellPrices );
					return ( price > 0 ) && super.isVisible( element );
				}
			}
		}

		private class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel()
			{
				super( purchases );

				this.setButtons( true, new ActionListener[] {
						new BuyListener(),
					} );

				this.eastPanel.add( new InvocationButton( "visit", CoinmasterPanel.this, "visit" ), BorderLayout.SOUTH );

				this.elementList.setCellRenderer( getCoinmasterRenderer( buyPrices, token, property, showLighthouse() ) );
				this.elementList.setVisibleRowCount( 6 );
				this.setEnabled( true );
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( outfit == 0 || hasOutfit && atWar );
			}

			public void addFilters()
			{
			}

			public void addMovers()
			{
			}

			public Object[] getDesiredItems()
			{
				Object[] items = this.elementList.getSelectedValues();
				if ( items.length == 0 )
				{
					return null;
				}

				int neededSize = items.length;
				int originalBalance = Preferences.getInteger( CoinmasterPanel.this.property );
				int balance = originalBalance;

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult item = (AdventureResult) items[ i ];
					String itemName = item.getName();
					int price = CoinmastersDatabase.getPrice( itemName, CoinmasterPanel.this.buyPrices );

					if ( price > originalBalance )
					{
						// This was grayed out.
						items[ i ] = null;
						--neededSize;
						continue;
					}

					int max = balance / price;

					String value = KoLFrame.input( "Buying " + itemName + "...", KoLConstants.COMMA_FORMAT.format( max ) );
					if ( value == null )
					{
						// He hit cancel
						return null;
					}

					int quantity = StaticEntity.parseInt( value );
					if ( quantity > max )
					{
						quantity = max;
					}

					if ( quantity <= 0 )
					{
						items[ i ] = null;
						--neededSize;
						continue;
					}

					items[ i ] = item.getInstance( quantity );
					balance -= quantity * price;
				}

				// Shrink the array which will be returned so
				// that it removes any nulled values.

				if ( neededSize == 0 )
				{
					return null;
				}

				Object[] desiredItems = new Object[ neededSize ];
				neededSize = 0;

				for ( int i = 0; i < items.length; ++i )
				{
					if ( items[ i ] != null )
					{
						desiredItems[ neededSize++ ] = items[ i ];
					}
				}

				return desiredItems;
			}

			public class BuyListener
				extends ThreadedListener
			{
				public void run()
				{
					Object[] items = BuyPanel.this.getDesiredItems();
					if ( items == null )
					{
						return;
					}

					execute( buyAction, items );
				}

				public String toString()
				{
					return "buy";
				}
			}
		}
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token )
	{
		return new CoinmasterRenderer( prices, token );
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token, String property, boolean lighthouse )
	{
		return new CoinmasterRenderer( prices, token, property, lighthouse );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private Map prices;
		private String token;
		private String property;
		private boolean lighthouse;

		public CoinmasterRenderer( final Map prices, final String token )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.property = null;
			this.lighthouse = true;
		}

		public CoinmasterRenderer( final Map prices, final String token, String property, boolean lighthouse )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.property = property;
			this.lighthouse = lighthouse;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
			{
				return defaultComponent;
			}

			if ( value instanceof AdventureResult )
			{
				return this.getRenderer( defaultComponent, (AdventureResult) value );
			}

			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			String name = ar.getName();
			String canonicalName = KoLDatabase.getCanonicalName( name );

			if ( !lighthouse && CoinmastersDatabase.lighthouseItems().get( canonicalName ) != null )
			{
				return null;
			}

			Integer iprice = (Integer)prices.get( canonicalName );

			if ( iprice == null )
			{
				return defaultComponent;
			}

			int price = iprice.intValue();
			boolean show = true;
			if ( property != null )
			{
				int balance = Preferences.getInteger( property );
				if ( price > balance )
				{
					show = false;
				}
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "<html>" );
			if ( !show )
			{
				stringForm.append( "<font color=gray>" );
			}
			stringForm.append( name );
			stringForm.append( " (" );
			stringForm.append( price );
			stringForm.append( " " );
			stringForm.append( token );
			if ( price > 1 )
				stringForm.append( "s" );
			stringForm.append( ")" );
			int count = ar.getCount();
			if ( count > 0 )
			{
				stringForm.append( " (" );
				stringForm.append( KoLConstants.COMMA_FORMAT.format( count ) );
				stringForm.append( ")" );
			}
			if ( !show )
			{
				stringForm.append( "</font>" );
			}
			stringForm.append( "</html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
