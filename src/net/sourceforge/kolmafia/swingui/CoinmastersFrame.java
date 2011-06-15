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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;

import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CoinmastersFrame
	extends GenericFrame
	implements ChangeListener
{
	public static final AdventureResult LUCRE = ItemPool.get( ItemPool.LUCRE, -1 );
	public static final AdventureResult SAND_DOLLAR = ItemPool.get( ItemPool.SAND_DOLLAR, -1 );
	public static final AdventureResult TICKET = ItemPool.get( ItemPool.GG_TICKET, -1 );
	public static final AdventureResult VOUCHER = ItemPool.get( ItemPool.SNACK_VOUCHER, -1 );

	public static final AdventureResult BONE_CHIPS = ItemPool.get( ItemPool.BONE_CHIPS, -1 );
	public static final AdventureResult CRIMBUCK = ItemPool.get( ItemPool.CRIMBUCK, -1 );
	public static final AdventureResult CRIMBCO_SCRIP = ItemPool.get( ItemPool.CRIMBCO_SCRIP, -1 );
	public static final AdventureResult AWOL = ItemPool.get( ItemPool.AWOL_COMMENDATION, -1 );
	public static final AdventureResult ISOTOPE = ItemPool.get( ItemPool.LUNAR_ISOTOPE, -1 );

	public static final AdventureResult AERATED_DIVING_HELMET = ItemPool.get( ItemPool.AERATED_DIVING_HELMET, 1 );
	public static final AdventureResult SCUBA_GEAR = ItemPool.get( ItemPool.SCUBA_GEAR, 1 );
	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult TRANSPONDER = ItemPool.get( ItemPool.TRANSPORTER_TRANSPONDER, 1 );
	public static final AdventureResult BUBBLIN_STONE = ItemPool.get( ItemPool.BUBBLIN_STONE, 1 );

	public static final AdventureResult TRANSPONDENT = new AdventureResult( EffectPool.TRANSPONDENT, 1, true );

	public static final int WAR_HIPPY_OUTFIT = 32;
	public static final int WAR_FRAT_OUTFIT = 33;

	private static CoinmastersFrame INSTANCE = null;
	private static boolean atWar = false;
	private static int dimes = 0;
	private static int quarters = 0;
	private static int lucre = 0;
	private static int sandDollars = 0;
	private static int tickets = 0;
	private static int storeCredits = 0;
	private static int snackVouchers = 0;
	private static int commendations = 0;
	private static int isotopes = 0;

	private static int boneChips = 0;
	private static int crimbux = 0;
	private static int scrip = 0;

	private CardLayoutSelectorPanel selectorPanel = null;

	private CoinmasterPanel dimemasterPanel = null;
	private CoinmasterPanel quartersmasterPanel = null;
	private CoinmasterPanel bhhPanel = null;
	private CoinmasterPanel bigBrotherPanel = null;
	private CoinmasterPanel arcadePanel = null;
	private CoinmasterPanel gameShoppePanel = null;
	private CoinmasterPanel freeSnackPanel = null;
	private CoinmasterPanel isotopeSmitheryPanel = null;
	private CoinmasterPanel dollhawkerPanel = null;
	private CoinmasterPanel lunarLunchPanel = null;
	private CoinmasterPanel awolPanel = null;

	private CoinmasterPanel altarOfBonesPanel = null;
	private CoinmasterPanel crimboCartelPanel = null;
	private CoinmasterPanel CRIMBCOGiftShopPanel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );
		CoinmastersFrame.INSTANCE = this;

		this.selectorPanel = new CardLayoutSelectorPanel( "coinMasterIndex", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
		JPanel panel;

		// Always available coinmasters
		this.selectorPanel.addCategory( "Always Available" );

		panel = new JPanel( new BorderLayout() );
		bhhPanel = new BountyHunterHunterPanel();
		panel.add( bhhPanel );
		this.selectorPanel.addPanel( bhhPanel.getPanelSelector(), panel );

		// *** Mr. Store goes here

		// Ascension coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Ascension" );

		panel = new JPanel( new BorderLayout() );
		dimemasterPanel = new DimemasterPanel();
		panel.add( dimemasterPanel );
		this.selectorPanel.addPanel( dimemasterPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		quartersmasterPanel = new QuartersmasterPanel();
		panel.add( quartersmasterPanel );
		this.selectorPanel.addPanel( quartersmasterPanel.getPanelSelector(), panel );

		// Aftercore coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Aftercore" );

		panel = new JPanel( new BorderLayout() );
		bigBrotherPanel = new BigBrotherPanel();
		panel.add( bigBrotherPanel );
		this.selectorPanel.addPanel( bigBrotherPanel.getPanelSelector(), panel );

		// IOTM coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Item of the Month" );

		panel = new JPanel( new BorderLayout() );
		arcadePanel = new TicketCounterPanel();
		panel.add( arcadePanel );
		this.selectorPanel.addPanel( arcadePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		gameShoppePanel = new GameShoppePanel();
		panel.add( gameShoppePanel );
		this.selectorPanel.addPanel( gameShoppePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		freeSnackPanel = new SnackVoucherPanel();
		panel.add( freeSnackPanel );
		this.selectorPanel.addPanel( freeSnackPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		isotopeSmitheryPanel = new IsotopeSmitheryPanel();
		panel.add( isotopeSmitheryPanel );
		this.selectorPanel.addPanel( isotopeSmitheryPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		dollhawkerPanel = new DollHawkerPanel();
		panel.add( dollhawkerPanel );
		this.selectorPanel.addPanel( dollhawkerPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		lunarLunchPanel = new LunarLunchPanel();
		panel.add( lunarLunchPanel );
		this.selectorPanel.addPanel( lunarLunchPanel.getPanelSelector(), panel );

		// Events coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Special Events" );

		panel = new JPanel( new BorderLayout() );
		awolPanel = new CommendationPanel();
		panel.add( awolPanel );
		this.selectorPanel.addPanel( awolPanel.getPanelSelector(), panel );

		// Removed coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Removed" );

		panel = new JPanel( new BorderLayout() );
		altarOfBonesPanel = new AltarOfBonesPanel();
		panel.add( altarOfBonesPanel );
		this.selectorPanel.addPanel( altarOfBonesPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		crimboCartelPanel = new CrimboCartelPanel();
		panel.add( crimboCartelPanel );
		this.selectorPanel.addPanel( crimboCartelPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		CRIMBCOGiftShopPanel = new CRIMBCOGiftShopPanel();
		panel.add( CRIMBCOGiftShopPanel );
		this.selectorPanel.addPanel( CRIMBCOGiftShopPanel.getPanelSelector(), panel );

		this.selectorPanel.addChangeListener( this );
		this.selectorPanel.setSelectedIndex( Preferences.getInteger( "coinMasterIndex" ) );

		this.framePanel.add( this.selectorPanel, BorderLayout.CENTER );

		CoinmastersFrame.externalUpdate();
	}

	/**
	 * Whenever the tab changes, this method is used to change the title to
	 * count the coins of the new tab
	 */

	public void stateChanged( final ChangeEvent e )
	{
		this.setTitle();
	}

	private void setTitle()
	{
		JPanel panel = (JPanel) this.selectorPanel.currentPanel();
		Component cm = ( panel instanceof JPanel ) ? panel.getComponent( 0 ) : null;
		if (cm instanceof CoinmasterPanel )
		{
			((CoinmasterPanel) cm).setTitle();
		}
	}

	public static void externalUpdate()
	{
		if ( INSTANCE == null )
		{
			return;
		}

		IslandDecorator.ensureUpdatedBigIsland();
		atWar = Preferences.getString( "warProgress" ).equals( "started" );
		dimes = Preferences.getInteger( "availableDimes" );
		quarters = Preferences.getInteger( "availableQuarters" );
		lucre = LUCRE.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableLucre", lucre );
		sandDollars = SAND_DOLLAR.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableSandDollars", sandDollars );
		tickets = TICKET.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableTickets", tickets );
		storeCredits = Preferences.getInteger( "availableStoreCredits" );
		snackVouchers = VOUCHER.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableSnackVouchers", snackVouchers );
		commendations = AWOL.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableCommendations", commendations );
		isotopes = ISOTOPE.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableIsotopes", isotopes );

		// Inaccessible
		crimbux = CRIMBUCK.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableCrimbux", crimbux );
		boneChips = BONE_CHIPS.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableBoneChips", boneChips );
		scrip = CRIMBCO_SCRIP.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableCRIMBCOScrip", scrip );

		INSTANCE.update();
	}

	private void update()
	{
		dimemasterPanel.update();
		quartersmasterPanel.update();
		bhhPanel.update();
		bigBrotherPanel.update();
		arcadePanel.update();
		gameShoppePanel.update();
		freeSnackPanel.update();
		awolPanel.update();
		altarOfBonesPanel.update();
		crimboCartelPanel.update();
		CRIMBCOGiftShopPanel.update();
		this.setTitle();
	}

	private class DimemasterPanel
		extends WarMasterPanel
	{
		public DimemasterPanel()
		{
			super( CoinMasterRequest.HIPPY, WAR_HIPPY_OUTFIT, "hippy");
		}
	}

	private class QuartersmasterPanel
		extends WarMasterPanel
	{
		public QuartersmasterPanel()
		{
			super( CoinMasterRequest.FRATBOY, WAR_FRAT_OUTFIT, "fratboy" );
		}
	}

	private class BountyHunterHunterPanel
		extends CoinmasterPanel
	{
		public BountyHunterHunterPanel()
		{
			super( CoinMasterRequest.BHH );
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class BigBrotherPanel
		extends CoinmasterPanel
	{
		private AdventureResult self = null;
		private AdventureResult familiar = null;
		private boolean rescuedBigBrother = false;

		public BigBrotherPanel()
		{
			super( CoinMasterRequest.BIGBROTHER );
		}

		public void update()
		{
			if ( InventoryManager.hasItem( CoinmastersFrame.AERATED_DIVING_HELMET ) )
			{
				this.self = CoinmastersFrame.AERATED_DIVING_HELMET;
				this.rescuedBigBrother = true;
			}
			else if ( InventoryManager.hasItem( CoinmastersFrame.SCUBA_GEAR ) )
			{
				this.self = CoinmastersFrame.SCUBA_GEAR;
				this.rescuedBigBrother = InventoryManager.hasItem( CoinmastersFrame.BUBBLIN_STONE );
			}
			else
			{
				this.rescuedBigBrother = false;
			}

			if ( InventoryManager.hasItem( CoinmastersFrame.DAS_BOOT ) )
			{
				this.familiar = CoinmastersFrame.DAS_BOOT;
			}
			else if ( InventoryManager.hasItem( CoinmastersFrame.BATHYSPHERE ) )
			{
				this.familiar = CoinmastersFrame.BATHYSPHERE;
			}
		}

		public boolean enabled()
		{
			return this.rescuedBigBrother;
		}

		public boolean accessible()
		{
			if ( !this.rescuedBigBrother )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You haven't rescued Big Brother yet." );
				return false;
			}

			if ( this.self == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right equipment to adventure underwater." );
				return false;
			}

			if ( !this.waterBreathingFamiliar() && this.familiar == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your familiar doesn't have the right equipment to adventure underwater." );
				return false;
			}

			return true;
		}

		public void equip()
		{
			if ( !KoLCharacter.hasEquipped( self ) )
			{
				EquipmentRequest request = new EquipmentRequest( self );
				RequestThread.postRequest( request );
			}

			if ( !this.waterBreathingFamiliar() && !KoLCharacter.hasEquipped( familiar ) )
			{
				EquipmentRequest request = new EquipmentRequest( familiar );
				RequestThread.postRequest( request );
			}
		}

		public boolean waterBreathingFamiliar()
		{
			return KoLCharacter.getFamiliar().waterBreathing();
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class CrimboCartelPanel
		extends CoinmasterPanel
	{
		public CrimboCartelPanel()
		{
			super( CoinMasterRequest.CRIMBOCARTEL );
		}

		public boolean accessible()
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The " + this.data.getMaster() + " is not available" );
			return false;
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class TicketCounterPanel
		extends CoinmasterPanel
	{
		public TicketCounterPanel()
		{
			super( CoinMasterRequest.TICKETCOUNTER );
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class GameShoppePanel
		extends CoinmasterPanel
	{
		public GameShoppePanel()
		{
			super( CoinMasterRequest.GAMESHOPPE );
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class SnackVoucherPanel
		extends CoinmasterPanel
	{
		public SnackVoucherPanel()
		{
			super( CoinMasterRequest.FREESNACKS );
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class AltarOfBonesPanel
		extends CoinmasterPanel
	{
		public AltarOfBonesPanel()
		{
			super( CoinMasterRequest.ALTAROFBONES );
		}

		public boolean accessible()
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The " + this.data.getMaster() + " is not available" );
			return false;
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class CRIMBCOGiftShopPanel
		extends CoinmasterPanel
	{
		public CRIMBCOGiftShopPanel()
		{
			super( CoinMasterRequest.CRIMBCOGIFTSHOP );
		}

		public boolean accessible()
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The " + this.data.getMaster() + " is not available" );
			return false;
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class CommendationPanel
		extends CoinmasterPanel
	{
		public CommendationPanel()
		{
			super( CoinMasterRequest.AWOL );
		}

		public boolean enabled()
		{
			// You access the Quartermaster by "using" an
			// A. W. O. L. commendation
			return commendations > 0;
		}

		public boolean accessible()
		{
			if ( commendations <= 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't any A. W. O. L. commendations" );
				return false;
			}

			return true;
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class WarMasterPanel
		extends CoinmasterPanel
	{
		private final int outfit;
		private final String side;

		private boolean hasOutfit = false;

		public WarMasterPanel( CoinmasterData data, int outfit, String side )
		{
			super( data );
			this.outfit = outfit;
			this.side = side;
		}

		public void update()
		{
			this.hasOutfit = EquipmentManager.hasOutfit( this.outfit );
		}

		public boolean enabled()
		{
			return CoinmastersFrame.atWar && this.hasOutfit;
		}

		public boolean accessible()
		{
			if ( !CoinmastersFrame.atWar )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're not at war." );
				return false;
			}

			if ( !this.hasOutfit )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right outfit" );
				return false;
			}

			return true;
		}

		public void equip()
		{
			if ( !EquipmentManager.isWearingOutfit( this.outfit ) )
			{

				EquipmentManager.retrieveOutfit( this.outfit );
				SpecialOutfit outfit = EquipmentDatabase.getOutfit( this.outfit );
				EquipmentRequest request = new EquipmentRequest( outfit );
				RequestThread.postRequest( request );
			}
		}

		public String lighthouseSide()
		{
			return this.side;
		}
	}

	private class IsotopeSmitheryPanel
		extends IsotopeMasterPanel
	{
		public IsotopeSmitheryPanel()
		{
			super( CoinMasterRequest.ISOTOPE_SMITHERY );
		}
	}

	private class DollHawkerPanel
		extends IsotopeMasterPanel
	{
		public DollHawkerPanel()
		{
			super( CoinMasterRequest.DOLLHAWKER );
		}
	}

	private class LunarLunchPanel
		extends IsotopeMasterPanel
	{
		public LunarLunchPanel()
		{
			super( CoinMasterRequest.LUNAR_LUNCH );
		}
	}

	private class IsotopeMasterPanel
		extends CoinmasterPanel
	{
		private boolean hasEffect = false;
		private boolean hasItem = false;

		public IsotopeMasterPanel( CoinmasterData data )
		{
			super( data );
		}

		public void update()
		{
			this.hasEffect = KoLConstants.activeEffects.contains( CoinmastersFrame.TRANSPONDENT );
			this.hasItem = TRANSPONDER.getCount( KoLConstants.inventory ) > 0;
		}

		public boolean enabled()
		{
			return this.hasEffect;
		}

		public boolean accessible()
		{
			if ( this.hasEffect )
			{
				return true;
			}

			if ( !this.hasItem )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need a transporter transponder to go there." );
				return false;
			}

			return true;
		}

		public void equip()
		{
			if ( !hasEffect && this.hasItem )
			{
				UseItemRequest request = new UseItemRequest( TRANSPONDER );
				RequestThread.postRequest( request );
			}
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	public class CoinmasterPanel
		extends JPanel
	{
		protected final CoinmasterData data;

		private SellPanel sellPanel = null;
		private BuyPanel buyPanel = null;

		public CoinmasterPanel( final CoinmasterData data )
		{
			super( new BorderLayout() );

			this.data = data;

			if ( data.getSellPrices() != null )
			{
				sellPanel = new SellPanel();
				this.add( sellPanel, BorderLayout.NORTH );
			}

			if ( data.getBuyPrices() != null )
			{
				buyPanel = new BuyPanel();
				this.add( buyPanel, BorderLayout.CENTER );
			}
		}

		public void setTitle()
		{
			String property = this.data.getProperty();
			int count =  Preferences.getInteger( property );
			String token = this.data.getToken();
			String name = count != 1 ? 
				ItemDatabase.getPluralName( token ) :
				token;
			INSTANCE.setTitle( "Coin Masters (" + count + " " + name + ")" );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public String getMaster()
		{
			return this.data.getMaster();
		}

		public String getPanelSelector()
		{
			return "- " + this.data.getMaster();
		}

		public boolean enabled()
		{
			return true;
		}

		public boolean accessible()
		{
			return true;
		}

		public void equip()
		{
		}

		public String lighthouseSide()
		{
			return null;
		}

		public void update()
		{
		}

		public int buyDefault( final int max )
		{
			return max;
		}

		public void check()
		{
			this.update();

			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();
			this.equip();
			RequestThread.postRequest( new CoinMasterRequest( this.data ) );
			RequestThread.closeRequestSequence();
		}

		private void execute( final String action, final Object [] items )
		{
			if ( items.length == 0 )
			{
				return;
			}

			this.update();

			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();

			this.equip();

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult it = (AdventureResult)items[i];
				GenericRequest request = new CoinMasterRequest( this.data, action, it );
				RequestThread.postRequest( request );
			}

			RequestThread.closeRequestSequence();

			// Update our token count in the title
			this.setTitle();
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

				Map sellPrices = CoinmasterPanel.this.data.getSellPrices();
				String token = CoinmasterPanel.this.data.getToken();
				this.elementList.setCellRenderer( getCoinmasterRenderer( sellPrices, token ) );
				this.setEnabled( true );
				this.filterItems();
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( CoinmasterPanel.this.enabled() );
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
					if ( !InputFieldUtilities.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					Object[] items = SellPanel.this.getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					execute( CoinmasterPanel.this.data.getSellAction(), items );
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
					int price = CoinmastersDatabase.getPrice( ar.getName(), CoinmasterPanel.this.data.getSellPrices() );
					return ( price > 0 ) && super.isVisible( element );
				}
			}
		}

		private class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel()
			{
				super( CoinmasterPanel.this.data.getBuyItems() );

				this.setButtons( true, new ActionListener[] {
						new BuyListener(),
					} );

				this.eastPanel.add( new InvocationButton( "check", CoinmasterPanel.this, "check" ), BorderLayout.SOUTH );

				Map buyPrices = CoinmasterPanel.this.data.getBuyPrices();
				String token = CoinmasterPanel.this.data.getToken();
				String property = CoinmasterPanel.this.data.getProperty();
				String side = CoinmasterPanel.this.lighthouseSide();
				this.elementList.setCellRenderer( getCoinmasterRenderer( buyPrices, token, property, side ) );
				this.elementList.setVisibleRowCount( 6 );
				this.setEnabled( true );
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( CoinmasterPanel.this.enabled() );
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
				String property = CoinmasterPanel.this.data.getProperty();
				int originalBalance = Preferences.getInteger( property );
				int balance = originalBalance;
				Map buyPrices = CoinmasterPanel.this.data.getBuyPrices();

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult item = (AdventureResult) items[ i ];
					String itemName = item.getName();
					int price = CoinmastersDatabase.getPrice( itemName, buyPrices );

					if ( price > originalBalance )
					{
						// This was grayed out.
						items[ i ] = null;
						--neededSize;
						continue;
					}

					int max = balance / price;
					int quantity = max;

					if ( max > 1 )
					{
						int def = CoinmasterPanel.this.buyDefault( max );
						String value = InputFieldUtilities.input( "Buying " + itemName + "...", KoLConstants.COMMA_FORMAT.format( def ) );
						if ( value == null )
						{
							// He hit cancel
							return null;
						}

						quantity = StringUtilities.parseInt( value );
					}

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

					execute( CoinmasterPanel.this.data.getBuyAction(), items );
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

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token, String property, String side )
	{
		return new CoinmasterRenderer( prices, token, property, side );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private Map prices;
		private String token;
		private String property;
		private String side;

		public CoinmasterRenderer( final Map prices, final String token )
		{
			this( prices, token, null, null );
		}

		public CoinmasterRenderer( final Map prices, final String token, String property, String side )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.property = property;
			this.side = side;
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
			String canonicalName = StringUtilities.getCanonicalName( name );

			if ( this.side != null &&
			     CoinmastersDatabase.lighthouseItems().get( canonicalName ) != null &&
			     !Preferences.getString( "sidequestLighthouseCompleted" ).equals( this.side ) )
			{
				return null;
			}

			Integer iprice = (Integer)prices.get( canonicalName );

			if ( iprice == null )
			{
				return defaultComponent;
			}

			int price = iprice.intValue();
			boolean show = CoinmastersDatabase.availableItem( canonicalName);

			if ( show && property != null )
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
			stringForm.append( price != 1 ?
					   ItemDatabase.getPluralName( token ) :
					   token );
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
