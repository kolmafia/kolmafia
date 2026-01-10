package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.request.coinmaster.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.coinmaster.BURTRequest;
import net.sourceforge.kolmafia.request.coinmaster.BigBrotherRequest;
import net.sourceforge.kolmafia.request.coinmaster.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.coinmaster.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.Crimbo11Request;
import net.sourceforge.kolmafia.request.coinmaster.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.coinmaster.DimemasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.FreeSnackRequest;
import net.sourceforge.kolmafia.request.coinmaster.FudgeWandRequest;
import net.sourceforge.kolmafia.request.coinmaster.GameShoppeRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.request.coinmaster.MrStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.SkeletonOfCrimboPastRequest;
import net.sourceforge.kolmafia.request.coinmaster.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.TravelingTraderRequest;
// CHECKSTYLE.SUPPRESS: AvoidStarImport
import net.sourceforge.kolmafia.request.coinmaster.shop.*;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemListManagePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersFrame extends GenericFrame implements ChangeListener {
  private CardLayoutSelectorPanel selectorPanel = null;

  public CoinmastersFrame() {
    super("Coin Masters");

    this.selectorPanel =
        new CardLayoutSelectorPanel("coinMasterIndex", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", true);

    // Always available coinmasters
    this.selectorPanel.addCategory("Always Available");

    addPanel(new PixelPanel());
    addPanel(new StarChartPanel());
    addPanel(new BountyHunterHunterPanel());
    addPanel(new MrStorePanel());
    addPanel(new ArmoryAndLeggeryPanel());
    addPanel(new BlackMarketPanel());
    addPanel(new HermitPanel());
    addPanel(new ShoreGiftShopPanel());
    addPanel(new JunkMagazinePanel());
    addPanel(new TrapperPanel());
    addPanel(new VendingMachinePanel());
    addPanel(new SwaggerShopPanel());
    addPanel(new NuggletCraftingPanel());
    addPanel(new DripArmoryPanel());

    // Ascension coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Ascension");

    addPanel(new DimemasterPanel());
    addPanel(new QuartersmasterPanel());
    addPanel(new BURTPanel());
    addPanel(new JarlsbergPanel());
    addPanel(new KOLHSArtPanel());
    addPanel(new KOLHSChemPanel());
    addPanel(new KOLHSShopPanel());
    addPanel(new FishboneryPanel());
    addPanel(new EdShopPanel());
    addPanel(new GeneticFiddlingPanel());
    addPanel(new PokemporiumPanel());
    addPanel(new GMartPanel());
    addPanel(new CosmicRaysBazaarPanel());
    addPanel(new PlumberGearPanel());
    addPanel(new PlumberItemPanel());
    addPanel(new DinostaurPanel());
    addPanel(new ReplicaMrStorePanel());
    addPanel(new TinkeringBenchPanel());
    addPanel(new WetCrapForSalePanel());

    // Aftercore coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Aftercore");

    addPanel(new BigBrotherPanel());
    addPanel(new GrandmaPanel());
    addPanel(new TerrifiedEagleInnPanel());

    // IOTM coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Item of the Month");

    addPanel(new SugarSheetPanel());
    addPanel(new TicketCounterPanel());
    addPanel(new GameShoppePanel());
    addPanel(new SnackVoucherPanel());
    addPanel(new IsotopeSmitheryPanel());
    addPanel(new DollHawkerPanel());
    addPanel(new LunarLunchPanel());
    addPanel(new BeerGardenPanel());
    addPanel(new WinterGardenPanel());
    addPanel(new BoutiquePanel());
    addPanel(new RumplePanel());
    addPanel(new BrogurtPanel());
    addPanel(new BuffJimmyPanel());
    addPanel(new TacoDanPanel());
    addPanel(new FiveDPanel());
    addPanel(new SHAWARMAPanel());
    addPanel(new CanteenPanel());
    addPanel(new ArmoryPanel());
    addPanel(new DinseyCompanyStorePanel());
    addPanel(new ToxicChemistryPanel());
    addPanel(new DiscoGiftCoPanel());
    addPanel(new WalmartPanel());
    addPanel(new AirportPanel());
    addPanel(new BatFabricatorPanel());
    addPanel(new ChemiCorpPanel());
    addPanel(new GotporkOrphanagePanel());
    addPanel(new GotporkPDPanel());
    addPanel(new LTTPanel());
    addPanel(new BaconPanel());
    addPanel(new PrecinctPanel());
    addPanel(new CashewPanel());
    addPanel(new SpacegateFabricationPanel());
    addPanel(new SpantPanel());
    addPanel(new XOPanel());
    addPanel(new RubeePanel());
    addPanel(new SliemcePanel());
    addPanel(new FunALogPanel());
    addPanel(new YourCampfirePanel());
    addPanel(new GuzzlrPanel());
    addPanel(new SpinMasterLathePanel());
    addPanel(new FancyDanPanel());
    addPanel(new ShadowForgePanel());
    addPanel(new MrStore2002Panel());
    addPanel(new FixodentPanel());
    addPanel(new KiwiKwikiMartPanel());
    addPanel(new SeptEmberPanel());
    addPanel(new DedigitizerPanel());
    addPanel(new ShowerThoughtsPanel());
    addPanel(new SkeletonOfCrimboPastPanel());

    // Twitch coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Twitch");

    addPanel(new NeandermallPanel());
    addPanel(new ShoeRepairPanel());
    addPanel(new ApplePanel());
    addPanel(new NinjaPanel());
    addPanel(new ShakeShopPanel());
    addPanel(new FlowerTradeinPanel());
    addPanel(new MerchTablePanel());
    addPanel(new TwitchSoupPanel());
    addPanel(new AlliedHqPanel());

    // Events coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Special Events");

    addPanel(new CommendationPanel());
    addPanel(new FudgeWandPanel());
    addPanel(new TravelingTraderPanel());
    addPanel(new fdkolPanel());
    addPanel(new WarbearBoxPanel());
    addPanel(new Crimbo25SammyPanel());

    // Removed coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Removed");

    addPanel(new AltarOfBonesPanel());
    addPanel(new CrimboCartelPanel());
    addPanel(new CRIMBCOGiftShopPanel());
    addPanel(new Crimbo11Panel());
    addPanel(new Crimbo14Panel());
    addPanel(new Crimbo16Panel());
    addPanel(new Crimbo17Panel());
    addPanel(new Crimbo19Panel());
    addPanel(new Crimbo20BoozePanel());
    addPanel(new Crimbo20CandyPanel());
    addPanel(new Crimbo20FoodPanel());
    addPanel(new Crimbo23ElfBarPanel());
    addPanel(new Crimbo23ElfCafePanel());
    addPanel(new Crimbo23ElfArmoryPanel());
    addPanel(new Crimbo23ElfFactoryPanel());
    addPanel(new Crimbo23PirateBarPanel());
    addPanel(new Crimbo23PirateCafePanel());
    addPanel(new Crimbo23PirateArmoryPanel());
    addPanel(new Crimbo23PirateFactoryPanel());
    addPanel(new Crimbo24BarPanel());
    addPanel(new Crimbo24CafePanel());
    addPanel(new Crimbo24FactoryPanel());

    this.selectorPanel.addChangeListener(this);
    this.selectorPanel.setSelectedIndex(Preferences.getInteger("coinMasterIndex"));

    JPanel wrapperPanel = new JPanel(new BorderLayout());
    wrapperPanel.add(this.selectorPanel, BorderLayout.CENTER);
    wrapperPanel.add(new StatusPanel(), BorderLayout.SOUTH);

    this.setCenterComponent(wrapperPanel);
  }

  private CoinmasterPanel currentPanel() {
    JComponent panel = this.selectorPanel.currentPanel();
    Component cm = (panel instanceof JPanel) ? panel.getComponent(0) : null;
    return (cm instanceof CoinmasterPanel cp) ? cp : null;
  }

  private void addPanel(CoinmasterPanel newPanel) {
    var panel = new JPanel(new BorderLayout());
    panel.add(newPanel);
    this.selectorPanel.addPanel(newPanel.getPanelSelector(), panel);
  }

  /**
   * Whenever the tab changes, this method is used to change the title to count the coins of the new
   * tab
   */
  @Override
  public void stateChanged(final ChangeEvent e) {
    CoinmasterPanel current = this.currentPanel();
    if (current != null) {
      current.setTitle();
    }
  }

  private class PixelPanel extends CoinmasterPanel {
    public PixelPanel() {
      super(PixelRequest.DATA);
    }
  }

  private class StarChartPanel extends CoinmasterPanel {
    public StarChartPanel() {
      super(StarChartRequest.DATA);
    }
  }

  private class DimemasterPanel extends WarMasterPanel {
    public DimemasterPanel() {
      super(DimemasterRequest.HIPPY);
    }
  }

  private class QuartersmasterPanel extends WarMasterPanel {
    public QuartersmasterPanel() {
      super(QuartersmasterRequest.FRATBOY);
    }
  }

  private class BountyHunterHunterPanel extends CoinmasterPanel {
    public BountyHunterHunterPanel() {
      super(BountyHunterHunterRequest.BHH);
    }
  }

  public class MrStorePanel extends CoinmasterPanel {
    private static final StorageRequest PULL_MR_A_REQUEST =
        new StorageRequest(
            StorageRequestType.STORAGE_TO_INVENTORY, new AdventureResult[] {MrStoreRequest.MR_A});
    private static final StorageRequest PULL_UNCLE_B_REQUEST =
        new StorageRequest(
            StorageRequestType.STORAGE_TO_INVENTORY,
            new AdventureResult[] {MrStoreRequest.UNCLE_B});

    private final JButton pullA = new InvocationButton("pull Mr. A", this, "pullA");
    private final JButton pullB = new InvocationButton("pull Uncle B", this, "pullB");
    private final JButton AToB = new InvocationButton("1 A -> 10 B", this, "AToB");
    private final JButton BToA = new InvocationButton("10 B -> 1 A", this, "BToA");
    private int ACountStorage = 0;
    private int BCountStorage = 0;
    private int ACount = 0;
    private int BCount = 0;

    public MrStorePanel() {
      super(MrStoreRequest.MR_STORE);
      this.buyPanel.addButton(pullA, false);
      this.buyPanel.addButton(pullB, false);
      this.buyPanel.addButton(AToB, false);
      this.buyPanel.addButton(BToA, false);
      this.storageInTitle = true;
      this.setPullsInTitle();
      this.update();
    }

    @Override
    public final void update() {
      this.ACount = MrStoreRequest.MR_A.getCount(KoLConstants.inventory);
      this.BCount = MrStoreRequest.UNCLE_B.getCount(KoLConstants.inventory);
      this.ACountStorage = MrStoreRequest.MR_A.getCount(KoLConstants.storage);
      this.BCountStorage = MrStoreRequest.UNCLE_B.getCount(KoLConstants.storage);
      boolean canPull = KoLCharacter.isHardcore() || ConcoctionDatabase.getPullsRemaining() != 0;
      this.pullA.setEnabled(canPull && this.ACountStorage > 0);
      this.pullB.setEnabled(canPull && this.BCountStorage > 0);
      this.AToB.setEnabled(this.ACount > 0);
      this.BToA.setEnabled(this.BCount >= 10);
      super.update();
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(this.BCount);
      buffer.append(" ");
      buffer.append("Uncle B");
      if (this.BCount != 1) {
        buffer.append("s");
      }
      buffer.append(", ");
      buffer.append(this.BCountStorage);
      buffer.append(" in storage");
      buffer.append(")");
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      this.pullA.setEnabled(isEnabled && this.ACountStorage > 0);
      this.pullB.setEnabled(isEnabled && this.BCountStorage > 0);
      this.AToB.setEnabled(isEnabled && this.ACount > 0);
      this.BToA.setEnabled(isEnabled && this.BCount >= 10);
    }

    public void pullA() {
      GenericRequest request =
          KoLCharacter.isHardcore() ? new MrStoreRequest("pullmras") : PULL_MR_A_REQUEST;
      RequestThread.postRequest(request);
    }

    public void pullB() {
      GenericRequest request =
          KoLCharacter.isHardcore() ? new MrStoreRequest("pullunclebs") : PULL_UNCLE_B_REQUEST;
      RequestThread.postRequest(request);
    }

    public void AToB() {
      RequestThread.postRequest(new MrStoreRequest("a_to_b"));
    }

    public void BToA() {
      RequestThread.postRequest(new MrStoreRequest("b_to_a"));
    }
  }

  public class ArmoryAndLeggeryPanel extends CoinmasterPanel {
    public ArmoryAndLeggeryPanel() {
      super(ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY);
      NamedListenerRegistry.registerNamedListener("(armoryandleggery)", this);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        // There are two currencies for every year of Standard equipment.
        // That is far too many to show all of them in the title.
        // Show only the ones you have in inventory right now.
        if (InventoryManager.getCount(currency.getItemId()) > 0) {
          buffer.append(" (");
          buffer.append(InventoryManager.getCount(currency));
          buffer.append(" ");
          buffer.append(currency.getName());
          buffer.append(")");
        }
      }
    }
  }

  public class BlackMarketPanel extends CoinmasterPanel {
    public BlackMarketPanel() {
      super(BlackMarketRequest.BLACK_MARKET);
    }
  }

  public class HermitPanel extends CoinmasterPanel {
    private final JButton fish = new InvocationButton("go fish", this, "fish");

    public HermitPanel() {
      super(HermitRequest.HERMIT);
      this.buyPanel.addButton(fish, true);
    }

    public void fish() {
      int available = HermitRequest.getWorthlessItemCount();
      AdventureResult item = HermitRequest.WORTHLESS_ITEM.getInstance(available + 1);
      InventoryManager.retrieveItem(item, false);
    }
  }

  public class TrapperPanel extends CoinmasterPanel {
    public TrapperPanel() {
      super(TrapperRequest.TRAPPER);
    }
  }

  public class SwaggerShopPanel extends CoinmasterPanel {
    public SwaggerShopPanel() {
      super(SwaggerShopRequest.SWAGGER_SHOP);
      PreferenceListenerRegistry.registerPreferenceListener("blackBartsBootyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("holidayHalsBookAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "antagonisticSnowmanKitAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("mapToKokomoAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("essenceOfBearAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("manualOfNumberologyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("ROMOfOptimalityAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "schoolOfHardKnocksDiplomaAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("guideToSafariAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("glitchItemAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("lawOfAveragesAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("universalSeasoningAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("bookOfIronyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("essenceOfAnnoyanceAvailable", this);
    }
  }

  public class BURTPanel extends CoinmasterPanel {
    public BURTPanel() {
      super(BURTRequest.BURT);
    }
  }

  public class JarlsbergPanel extends CoinmasterPanel {
    public JarlsbergPanel() {
      super(JarlsbergRequest.DATA);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.COSMIC_SIX_PACK -> 1;
        default -> max;
      };
    }
  }

  public class KOLHSArtPanel extends CoinmasterPanel {
    public KOLHSArtPanel() {
      super(KOLHSArtRequest.DATA);
      PreferenceListenerRegistry.registerPreferenceListener(
          "lastKOLHSArtClassUnlockAdventure", this);
    }
  }

  public class KOLHSChemPanel extends CoinmasterPanel {
    public KOLHSChemPanel() {
      super(KOLHSChemRequest.DATA);
      PreferenceListenerRegistry.registerPreferenceListener(
          "lastKOLHSChemClassUnlockAdventure", this);
    }
  }

  public class KOLHSShopPanel extends CoinmasterPanel {
    public KOLHSShopPanel() {
      super(KOLHSShopRequest.DATA);
      PreferenceListenerRegistry.registerPreferenceListener(
          "lastKOLHSShopClassUnlockAdventure", this);
    }
  }

  public class FishboneryPanel extends CoinmasterPanel {
    public FishboneryPanel() {
      super(FishboneryRequest.FISHBONERY);
    }
  }

  public class EdShopPanel extends CoinmasterPanel {
    public EdShopPanel() {
      super(EdShopRequest.EDSHOP);
    }
  }

  public class NuggletCraftingPanel extends CoinmasterPanel {
    public NuggletCraftingPanel() {
      super(NuggletCraftingRequest.NUGGLETCRAFTING);
    }
  }

  private abstract class TwitchPanel extends CoinmasterPanel {
    public TwitchPanel(CoinmasterData data) {
      super(data);
      PreferenceListenerRegistry.registerPreferenceListener("timeTowerAvailable", this);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(Preferences.getBoolean("timeTowerAvailable"));
    }
  }

  public class NeandermallPanel extends TwitchPanel {
    public NeandermallPanel() {
      super(NeandermallRequest.NEANDERMALL);
    }
  }

  public class ShoeRepairPanel extends TwitchPanel {
    public ShoeRepairPanel() {
      super(ShoeRepairRequest.SHOE_REPAIR);
    }
  }

  public class ApplePanel extends TwitchPanel {
    public ApplePanel() {
      super(AppleStoreRequest.APPLE_STORE);
    }
  }

  public class NinjaPanel extends TwitchPanel {
    public NinjaPanel() {
      super(NinjaStoreRequest.NINJA_STORE);
    }
  }

  public class ShakeShopPanel extends TwitchPanel {
    public ShakeShopPanel() {
      super(YeNeweSouvenirShoppeRequest.SHAKE_SHOP);
    }
  }

  public class FlowerTradeinPanel extends CoinmasterPanel {
    public FlowerTradeinPanel() {
      super(FlowerTradeinRequest.DATA);
    }

    static List<Map.Entry<String, String>> currencies = new ArrayList<>();

    static {
      currencies.add(Map.entry("rose", "rose"));
      currencies.add(Map.entry("white tulip", "white tulip"));
      currencies.add(Map.entry("red tulip", "red tulip"));
      currencies.add(Map.entry("blue tulip", "blue tulip"));
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (var entry : currencies) {
        int itemId = ItemDatabase.getItemId(entry.getKey());
        int count = InventoryManager.getCount(itemId);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(entry.getValue());
        buffer.append(")");
      }
    }
  }

  public class MerchTablePanel extends TwitchPanel {
    public MerchTablePanel() {
      super(MerchTableRequest.MERCH_TABLE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(InventoryManager.getCount(MerchTableRequest.CHRONER));
      buffer.append(" Chroner)");
    }
  }

  public class TwitchSoupPanel extends TwitchPanel {
    public TwitchSoupPanel() {
      super(PrimordialSoupKitchenRequest.DATA);
    }

    static List<Map.Entry<String, String>> currencies = new ArrayList<>();

    static {
      currencies.add(Map.entry("Chroner", "Chroner"));
      currencies.add(Map.entry("bacteria bisque", "bisque"));
      currencies.add(Map.entry("ciliophora chowder", "chowder"));
      currencies.add(Map.entry("cream of chloroplasts", "cream"));
      currencies.add(Map.entry("protogenetic chunklet (elbow)", "elbow"));
      currencies.add(Map.entry("protogenetic chunklet (flagellum)", "flagellum"));
      currencies.add(Map.entry("protogenetic chunklet (lips)", "lips"));
      currencies.add(Map.entry("protogenetic chunklet (muscle)", "muscle"));
      currencies.add(Map.entry("protogenetic chunklet (synapse)", "synapse"));
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (var entry : currencies) {
        int itemId = ItemDatabase.getItemId(entry.getKey());
        int count = InventoryManager.getCount(itemId);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(entry.getValue());
        buffer.append(")");
      }
    }
  }

  public class AlliedHqPanel extends TwitchPanel {
    public AlliedHqPanel() {
      super(AlliedHqRequest.DATA);
    }
  }

  public class ShoreGiftShopPanel extends CoinmasterPanel {
    public ShoreGiftShopPanel() {
      super(ShoreGiftShopRequest.SHORE_GIFT_SHOP);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerAscension637", this);
    }
  }

  public class JunkMagazinePanel extends CoinmasterPanel {
    public JunkMagazinePanel() {
      super(JunkMagazineRequest.DATA);
    }
  }

  public class SpacegateFabricationPanel extends CoinmasterPanel {
    public SpacegateFabricationPanel() {
      super(SpacegateFabricationRequest.SPACEGATE_STORE);
    }
  }

  public class SpantPanel extends CoinmasterPanel {
    public SpantPanel() {
      super(SpantRequest.DATA);
    }
  }

  public class XOPanel extends CoinmasterPanel {
    public XOPanel() {
      super(XOShopRequest.DATA);
    }
  }

  public class VendingMachinePanel extends CoinmasterPanel {
    public VendingMachinePanel() {
      super(VendingMachineRequest.VENDING_MACHINE);
    }
  }

  private class BigBrotherPanel extends CoinmasterPanel {
    public BigBrotherPanel() {
      super(BigBrotherRequest.BIG_BROTHER);
    }
  }

  private class GrandmaPanel extends CoinmasterPanel {
    public GrandmaPanel() {
      super(GrandmaRequest.DATA);
    }
  }

  private class DedigitizerPanel extends CoinmasterPanel {
    private static AdventureResult ONE = ItemPool.get(ItemPool.ONE);
    private static AdventureResult ZERO = ItemPool.get(ItemPool.ZERO);

    public DedigitizerPanel() {
      super(DedigitizerRequest.DATA);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);

      // Only show 0's and 1's. All but 5 also require a schematic,
      // but there are 22 of them and the title will be cluttered. Rows
      // will be greyed out if you don't have the required schematic

      int count1 = InventoryManager.getCount(ONE);
      buffer.append(" (");
      buffer.append(count1);
      buffer.append(" ");
      buffer.append(ONE.getPluralName(count1));
      buffer.append(")");
      int count0 = InventoryManager.getCount(ZERO);
      buffer.append(" (");
      buffer.append(count0);
      buffer.append(" ");
      buffer.append(ZERO.getPluralName(count0));
      buffer.append(")");
    }
  }

  private class Crimbo11Panel extends CoinmasterPanel {
    public Crimbo11Panel() {
      super();

      this.setData(Crimbo11Request.CRIMBO11);

      this.sellPanel = new SellPanel();
      this.add(this.sellPanel, BorderLayout.NORTH);

      ActionListener[] listeners = new ActionListener[2];
      listeners[0] = new GiftListener();
      listeners[1] = new DonateListener();

      this.buyPanel = new BuyPanel(listeners);
      this.add(this.buyPanel, BorderLayout.CENTER);
    }

    public AdventureResult[] getDesiredItems() {
      AdventureResult[] items = this.buyPanel.getSelectedValues().toArray(new AdventureResult[0]);
      return this.getDesiredBuyItems(items, false);
    }

    public class GiftListener extends ThreadedListener {
      @Override
      protected void execute() {
        CoinmasterData data = Crimbo11Panel.this.data;
        String reason = data.canBuy();
        if (reason != null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, reason);
          return;
        }

        AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
        if (items == null) {
          return;
        }

        String victim = InputFieldUtilities.input("Send a gift to whom?");
        if (victim == null) {
          return;
        }

        Crimbo11Panel.this.execute(true, items, "towho=" + victim);
      }

      @Override
      public String toString() {
        return "gift";
      }
    }

    public class DonateListener extends ThreadedListener {
      @Override
      protected void execute() {
        CoinmasterData data = Crimbo11Panel.this.data;
        String reason = data.canBuy();
        if (reason != null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, reason);
          return;
        }

        AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
        if (items == null) {
          return;
        }

        Crimbo11Panel.this.execute(true, items, "towho=0");
      }

      @Override
      public String toString() {
        return "donate";
      }
    }
  }

  private class CrimboCartelPanel extends CoinmasterPanel {
    public CrimboCartelPanel() {
      super(CrimboCartelRequest.CRIMBO_CARTEL);
    }
  }

  private class Crimbo14Panel extends CoinmasterPanel {
    public Crimbo14Panel() {
      super();

      this.setData(Crimbo14Request.CRIMBO14);

      this.sellPanel = new SellPanel();
      this.add(this.sellPanel, BorderLayout.NORTH);
      this.buyPanel = new BuyPanel();
      this.add(this.buyPanel, BorderLayout.CENTER);
    }
  }

  private class Crimbo16Panel extends CoinmasterPanel {
    public Crimbo16Panel() {
      super(Crimbo16Request.DATA);
    }
  }

  private class Crimbo17Panel extends CoinmasterPanel {
    public Crimbo17Panel() {
      super(Crimbo17Request.CRIMBO17);
    }
  }

  private class Crimbo19Panel extends CoinmasterPanel {
    public Crimbo19Panel() {
      super(KringleRequest.DATA);
    }
  }

  private class Crimbo20BoozePanel extends CoinmasterPanel {
    public Crimbo20BoozePanel() {
      super(Crimbo20BoozeRequest.CRIMBO20BOOZE);
    }
  }

  private class Crimbo20CandyPanel extends CoinmasterPanel {
    public Crimbo20CandyPanel() {
      super(Crimbo20CandyRequest.CRIMBO20CANDY);
    }
  }

  private class Crimbo20FoodPanel extends CoinmasterPanel {
    public Crimbo20FoodPanel() {
      super(Crimbo20FoodRequest.CRIMBO20FOOD);
    }
  }

  private class PokemporiumPanel extends CoinmasterPanel {
    public PokemporiumPanel() {
      super(PokemporiumRequest.POKEMPORIUM);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  private class SugarSheetPanel extends CoinmasterPanel {
    public SugarSheetPanel() {
      super(SugarSheetRequest.DATA);
    }
  }

  public class TicketCounterPanel extends CoinmasterPanel {
    private final JButton skeeball = new InvocationButton("skeeball", this, "skeeball");
    private int gameGridTokens = 0;

    public TicketCounterPanel() {
      super(TicketCounterRequest.TICKET_COUNTER);
      this.buyPanel.addButton(skeeball, false);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.gameGridTokens = ArcadeRequest.TOKEN.getCount(KoLConstants.inventory);
      this.skeeball.setEnabled(this.gameGridTokens > 0);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      this.skeeball.setEnabled(isEnabled && this.gameGridTokens > 0);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(this.gameGridTokens);
      buffer.append(" Game Grid tokens)");
    }

    public void skeeball() {
      RequestThread.postRequest(new ArcadeRequest("arcade_skeeball"));
    }
  }

  private class GameShoppePanel extends CoinmasterPanel {
    public GameShoppePanel() {
      super(GameShoppeRequest.GAMESHOPPE);
    }
  }

  private class SnackVoucherPanel extends CoinmasterPanel {
    public SnackVoucherPanel() {
      super(FreeSnackRequest.FREESNACKS);
    }
  }

  private class AltarOfBonesPanel extends CoinmasterPanel {
    public AltarOfBonesPanel() {
      super(AltarOfBonesRequest.ALTAR_OF_BONES);
    }
  }

  private class CRIMBCOGiftShopPanel extends CoinmasterPanel {
    public CRIMBCOGiftShopPanel() {
      super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP);
    }
  }

  public class DripArmoryPanel extends CoinmasterPanel {
    public DripArmoryPanel() {
      super(DripArmoryRequest.DRIP_ARMORY);
    }
  }

  private class CommendationPanel extends CoinmasterPanel {
    public CommendationPanel() {
      super(AWOLQuartermasterRequest.AWOL);
    }
  }

  private class FudgeWandPanel extends CoinmasterPanel {
    public FudgeWandPanel() {
      super(FudgeWandRequest.FUDGEWAND);
    }
  }

  private class TravelingTraderPanel extends CoinmasterPanel {
    public TravelingTraderPanel() {
      super(TravelingTraderRequest.TRAVELER);
    }
  }

  private class fdkolPanel extends CoinmasterPanel {
    public fdkolPanel() {
      super(FDKOLRequest.FDKOL);
    }
  }

  private class IsotopeSmitheryPanel extends CoinmasterPanel {
    public IsotopeSmitheryPanel() {
      super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class DollHawkerPanel extends CoinmasterPanel {
    public DollHawkerPanel() {
      super(DollHawkerRequest.DOLLHAWKER);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class LunarLunchPanel extends CoinmasterPanel {
    public LunarLunchPanel() {
      super(LunarLunchRequest.LUNAR_LUNCH);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class BrogurtPanel extends CoinmasterPanel {
    public BrogurtPanel() {
      super(BrogurtRequest.BROGURT);
    }
  }

  private class BuffJimmyPanel extends CoinmasterPanel {
    public BuffJimmyPanel() {
      super(BuffJimmyRequest.BUFF_JIMMY);
    }
  }

  private class TacoDanPanel extends CoinmasterPanel {
    public TacoDanPanel() {
      super(TacoDanRequest.TACO_DAN);
    }
  }

  private class FiveDPanel extends CoinmasterPanel {
    public FiveDPanel() {
      super(FiveDPrinterRequest.DATA);
    }
  }

  private class SHAWARMAPanel extends CoinmasterPanel {
    public SHAWARMAPanel() {
      super(SHAWARMARequest.SHAWARMA);
    }
  }

  private class CanteenPanel extends CoinmasterPanel {
    public CanteenPanel() {
      super(CanteenRequest.CANTEEN);
    }
  }

  private class ArmoryPanel extends CoinmasterPanel {
    public ArmoryPanel() {
      super(ArmoryRequest.ARMORY);
    }
  }

  private class DinseyCompanyStorePanel extends CoinmasterPanel {
    public DinseyCompanyStorePanel() {
      super(DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  private class DiscoGiftCoPanel extends CoinmasterPanel {
    public DiscoGiftCoPanel() {
      super(DiscoGiftCoRequest.DISCO_GIFTCO);
    }
  }

  private class WalmartPanel extends CoinmasterPanel {
    public WalmartPanel() {
      super(WalMartRequest.WALMART);
    }
  }

  private class AirportPanel extends CoinmasterPanel {
    public AirportPanel() {
      super(AirportRequest.DATA);
    }
  }

  private class BatFabricatorPanel extends BatFellowPanel {
    public BatFabricatorPanel() {
      super(BatFabricatorRequest.BAT_FABRICATOR);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        buffer.append(" (");
        buffer.append(InventoryManager.getCount(currency));
        buffer.append(" ");
        buffer.append(currency.getName());
        buffer.append(")");
      }
    }
  }

  private class ChemiCorpPanel extends BatFellowPanel {
    public ChemiCorpPanel() {
      super(ChemiCorpRequest.CHEMICORP);
    }
  }

  private class GotporkOrphanagePanel extends BatFellowPanel {
    public GotporkOrphanagePanel() {
      super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE);
    }
  }

  private class GotporkPDPanel extends BatFellowPanel {
    public GotporkPDPanel() {
      super(GotporkPDRequest.GOTPORK_PD);
    }
  }

  private class LTTPanel extends CoinmasterPanel {
    public LTTPanel() {
      super(LTTRequest.LTT);
    }
  }

  private class PrecinctPanel extends CoinmasterPanel {
    public PrecinctPanel() {
      super(PrecinctRequest.PRECINCT);
    }
  }

  private class RubeePanel extends CoinmasterPanel {
    public RubeePanel() {
      super(RubeeRequest.RUBEE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  private class SliemcePanel extends CoinmasterPanel {
    public SliemcePanel() {
      super(SliemceRequest.DATA);
    }
  }

  private class FunALogPanel extends CoinmasterPanel {
    public FunALogPanel() {
      super(FunALogRequest.FUN_A_LOG);
    }
  }

  private class YourCampfirePanel extends CoinmasterPanel {
    public YourCampfirePanel() {
      super(YourCampfireRequest.YOUR_CAMPFIRE);
    }
  }

  private class GuzzlrPanel extends CoinmasterPanel {
    public GuzzlrPanel() {
      super(GuzzlrRequest.GUZZLR);
    }
  }

  private class SpinMasterLathePanel extends CoinmasterPanel {
    public SpinMasterLathePanel() {
      super(SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        buffer.append(" (");
        buffer.append(InventoryManager.getCount(currency));
        buffer.append(" ");
        buffer.append(currency.getName());
        buffer.append(")");
      }
    }
  }

  public class ToxicChemistryPanel extends CoinmasterPanel {
    public ToxicChemistryPanel() {
      super(ToxicChemistryRequest.TOXIC_CHEMISTRY);
    }
  }

  private class TerrifiedEagleInnPanel extends CoinmasterPanel {
    public TerrifiedEagleInnPanel() {
      super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6423", this);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6428", this);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6429", this);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.TALES_OF_DREAD, ItemPool.BRASS_DREAD_FLASK, ItemPool.SILVER_DREAD_FLASK -> 1;
        default -> max;
      };
    }
  }

  private class BeerGardenPanel extends CoinmasterPanel {
    public BeerGardenPanel() {
      super(BeerGardenRequest.DATA);
    }
  }

  private class WarbearBoxPanel extends CoinmasterPanel {
    public WarbearBoxPanel() {
      super(WarbearBoxRequest.WARBEARBOX);
    }
  }

  private class WinterGardenPanel extends CoinmasterPanel {
    public WinterGardenPanel() {
      super(WinterGardenRequest.DATA);
    }
  }

  private class BoutiquePanel extends CoinmasterPanel {
    public BoutiquePanel() {
      super(BoutiqueRequest.BOUTIQUE);
    }
  }

  private class RumplePanel extends CoinmasterPanel {
    public RumplePanel() {
      super(RumpleRequest.DATA);
    }
  }

  private class BaconPanel extends CoinmasterPanel {
    public BaconPanel() {
      super(MemeShopRequest.BACON_STORE);
      PreferenceListenerRegistry.registerPreferenceListener("_internetViralVideoBought", this);
      PreferenceListenerRegistry.registerPreferenceListener("_internetPlusOneBought", this);
      PreferenceListenerRegistry.registerPreferenceListener("_internetGallonOfMilkBought", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "_internetPrintScreenButtonBought", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "_internetDailyDungeonMalwareBought", this);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.VIRAL_VIDEO,
            ItemPool.PLUS_ONE,
            ItemPool.GALLON_OF_MILK,
            ItemPool.PRINT_SCREEN,
            ItemPool.DAILY_DUNGEON_MALWARE ->
            1;
        default -> max;
      };
    }
  }

  private class CashewPanel extends CoinmasterPanel {
    public CashewPanel() {
      super(ThankShopRequest.CASHEW_STORE);
    }
  }

  private abstract class BatFellowPanel extends CoinmasterPanel {
    public BatFellowPanel(CoinmasterData data) {
      super(data);
      NamedListenerRegistry.registerNamedListener("(batfellow)", this);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public int buyDefault(final int max) {
      return max;
    }
  }

  private abstract class WarMasterPanel extends CoinmasterPanel {
    public WarMasterPanel(CoinmasterData data) {
      super(data);
      this.buyPanel.filterItems();
      NamedListenerRegistry.registerNamedListener("(outfit)", this);
      PreferenceListenerRegistry.registerPreferenceListener("warProgress", this);
      PreferenceListenerRegistry.registerPreferenceListener("sidequestLighthouseCompleted", this);
    }

    @Override
    public int buyDefault(final int max) {
      return max;
    }
  }

  private class GeneticFiddlingPanel extends CoinmasterPanel {
    public GeneticFiddlingPanel() {
      super(GeneticFiddlingRequest.DATA);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return 1;
    }
  }

  private class GMartPanel extends CoinmasterPanel {
    public GMartPanel() {
      super(GMartRequest.GMART);
    }
  }

  private class PlumberGearPanel extends CoinmasterPanel {
    public PlumberGearPanel() {
      super(PlumberGearRequest.PLUMBER_GEAR);
    }
  }

  private class PlumberItemPanel extends CoinmasterPanel {
    public PlumberItemPanel() {
      super(PlumberItemRequest.PLUMBER_ITEMS);
    }
  }

  private class CosmicRaysBazaarPanel extends CoinmasterPanel {
    public CosmicRaysBazaarPanel() {
      super(CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count =
            currency.isMeat() ? Concoction.getAvailableMeat() : InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class FancyDanPanel extends CoinmasterPanel {
    public FancyDanPanel() {
      super(FancyDanRequest.FANCY_DAN);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count = InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class ShadowForgePanel extends CoinmasterPanel {
    public ShadowForgePanel() {
      super(ShadowForgeRequest.DATA);
      PreferenceListenerRegistry.registerPreferenceListener("lastShadowForgeUnlockAdventure", this);
    }
  }

  private class DinostaurPanel extends CoinmasterPanel {
    public DinostaurPanel() {
      super(DinostaurRequest.DINOSTAUR);
    }
  }

  private class ReplicaMrStorePanel extends CoinmasterPanel {
    public ReplicaMrStorePanel() {
      super(ReplicaMrStoreRequest.REPLICA_MR_STORE);
    }
  }

  private class TinkeringBenchPanel extends CoinmasterPanel {
    public TinkeringBenchPanel() {
      super(TinkeringBenchRequest.DATA);
    }
  }

  private class MrStore2002Panel extends CoinmasterPanel {
    public MrStore2002Panel() {
      super(MrStore2002Request.MR_STORE_2002);
    }
  }

  private class FixodentPanel extends CoinmasterPanel {
    public FixodentPanel() {
      super(FixodentRequest.DATA);
    }
  }

  private class Crimbo23ElfBarPanel extends CoinmasterPanel {
    public Crimbo23ElfBarPanel() {
      super(Crimbo23ElfBarRequest.DATA);
    }
  }

  private class Crimbo23ElfCafePanel extends CoinmasterPanel {
    public Crimbo23ElfCafePanel() {
      super(Crimbo23ElfCafeRequest.DATA);
    }
  }

  private class Crimbo23ElfArmoryPanel extends CoinmasterPanel {
    public Crimbo23ElfArmoryPanel() {
      super(Crimbo23ElfArmoryRequest.DATA);
    }
  }

  private class Crimbo23ElfFactoryPanel extends CoinmasterPanel {
    public Crimbo23ElfFactoryPanel() {
      super(Crimbo23ElfFactoryRequest.DATA);
    }
  }

  private class Crimbo23PirateBarPanel extends CoinmasterPanel {
    public Crimbo23PirateBarPanel() {
      super(Crimbo23PirateBarRequest.DATA);
    }
  }

  private class Crimbo23PirateCafePanel extends CoinmasterPanel {
    public Crimbo23PirateCafePanel() {
      super(Crimbo23PirateCafeRequest.DATA);
    }
  }

  private class Crimbo23PirateArmoryPanel extends CoinmasterPanel {
    public Crimbo23PirateArmoryPanel() {
      super(Crimbo23PirateArmoryRequest.DATA);
    }
  }

  private class Crimbo23PirateFactoryPanel extends CoinmasterPanel {
    public Crimbo23PirateFactoryPanel() {
      super(Crimbo23PirateFactoryRequest.DATA);
    }
  }

  private class Crimbo24Panel extends CoinmasterPanel {
    public Crimbo24Panel(CoinmasterData data) {
      super(data);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count = InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class Crimbo24BarPanel extends Crimbo24Panel {
    public Crimbo24BarPanel() {
      super(Crimbo24BarRequest.DATA);
    }
  }

  private class Crimbo24CafePanel extends Crimbo24Panel {
    public Crimbo24CafePanel() {
      super(Crimbo24CafeRequest.DATA);
    }
  }

  private class Crimbo24FactoryPanel extends Crimbo24Panel {
    public Crimbo24FactoryPanel() {
      super(Crimbo24FactoryRequest.DATA);
    }
  }

  private class Crimbo25SammyPanel extends CoinmasterPanel {
    public Crimbo25SammyPanel() {
      super(Crimbo25SammyRequest.DATA);
    }
  }

  private class KiwiKwikiMartPanel extends CoinmasterPanel {
    public KiwiKwikiMartPanel() {
      super(KiwiKwikiMartRequest.DATA);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.MINI_KIWI_INTOXICATING_SPIRITS -> 1;
        default -> max;
      };
    }
  }

  private class SeptEmberPanel extends CoinmasterPanel {
    public SeptEmberPanel() {
      super(SeptEmberCenserRequest.SEPTEMBER_CENSER);
    }
  }

  private class ShowerThoughtsPanel extends CoinmasterPanel {
    public ShowerThoughtsPanel() {
      super(UsingYourShowerThoughtsRequest.DATA);
    }
  }

  private class WetCrapForSalePanel extends CoinmasterPanel {
    public WetCrapForSalePanel() {
      super(WetCrapForSaleRequest.DATA);
    }
  }

  private class SkeletonOfCrimboPastPanel extends CoinmasterPanel {
    public SkeletonOfCrimboPastPanel() {
      super(SkeletonOfCrimboPastRequest.SKELETON_OF_CRIMBO_PAST);
    }
  }

  public abstract class CoinmasterPanel extends JPanel implements Listener {
    protected CoinmasterData data;
    protected boolean storageInTitle = false;
    protected boolean pullsInTitle = false;

    protected ShopRowPanel shopRowPanel = null;
    protected SellPanel sellPanel = null;
    protected BuyPanel buyPanel = null;

    public CoinmasterPanel() {
      super(new BorderLayout());
      NamedListenerRegistry.registerNamedListener("(coinmaster)", this);
    }

    protected void setData(final CoinmasterData data) {
      this.data = data;

      String property = data.getProperty();
      if (property != null) {
        PreferenceListenerRegistry.registerPreferenceListener(property, this);
      }
    }

    protected void setPullsInTitle() {
      this.pullsInTitle = true;
      NamedListenerRegistry.registerNamedListener("(pullsremaining)", this);
    }

    public CoinmasterPanel(final CoinmasterData data) {
      this();

      this.setData(data);

      if (data.getShopRows() != null) {
        this.shopRowPanel = new ShopRowPanel();
        this.add(shopRowPanel, BorderLayout.CENTER);
      } else {
        if (data.getSellPrices() != null) {
          this.sellPanel = new SellPanel();
          this.add(sellPanel, BorderLayout.NORTH);
        }

        if (data.getBuyPrices() != null) {
          this.buyPanel = new BuyPanel();
          this.add(buyPanel, BorderLayout.CENTER);
        }
      }

      this.storageInTitle = this.data.getStorageAction() != null;
    }

    @Override
    public void update() {
      // (coinmaster) is fired when tokens change
      this.setTitle();
      if (this.shopRowPanel != null) {
        this.shopRowPanel.filterItems();
      }
      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    public CoinMasterRequest getRequest() {
      return this.data.getRequest();
    }

    public CoinMasterRequest getRequest(final boolean buying, final AdventureResult[] items) {
      return this.data.getRequest(buying, items);
    }

    public final void setTitle() {
      if (this == CoinmastersFrame.this.currentPanel()) {
        StringBuffer buffer = new StringBuffer();
        this.setTitle(buffer);
        CoinmastersFrame.this.setTitle(buffer.toString());
      }
    }

    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
    }

    public final void standardTitle(final StringBuffer buffer) {
      buffer.append("Coin Masters");
      String token = this.data.getToken();
      if (token != null) {
        AdventureResult item = this.data.getItem();
        int count = this.data.availableTokens();
        String name = (count != 1) ? this.data.getPluralToken() : token;
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(name);

        // Makes no sense to show storage except for real items
        if (storageInTitle && item != null) {
          int count1 = item.getCount(KoLConstants.storage);
          buffer.append(", ");
          buffer.append(count1);
          buffer.append(" in storage");

          // Only show pulls if we actually have the item in storage
          if (pullsInTitle && count1 > 0 && !KoLCharacter.isHardcore()) {
            int pulls = ConcoctionDatabase.getPullsRemaining();
            buffer.append(", ");
            buffer.append(KoLCharacter.inRonin() ? String.valueOf(pulls) : "unlimited");
            buffer.append(" pull");
            buffer.append(pulls != 1 ? "s" : "");
            buffer.append(" available");
          }
        }

        buffer.append(")");
      }
    }

    public void actionConfirmed() {}

    public void actionCancelled() {}

    public boolean addSellMovers() {
      return true;
    }

    public String getPanelSelector() {
      return "<html>- " + this.data.getMaster() + "</html>";
    }

    public boolean enabled() {
      return this.data.isAccessible();
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      if (this.shopRowPanel != null) {
        this.shopRowPanel.setEnabled(isEnabled);
      }
      if (this.buyPanel != null) {
        this.buyPanel.setEnabled(isEnabled);
      }
      if (this.sellPanel != null) {
        this.sellPanel.setEnabled(isEnabled);
      }
    }

    public int buyMax(final AdventureResult item, final int max) {
      return max;
    }

    public int buyDefault(final int max) {
      return 1;
    }

    public void check() {
      RequestThread.postRequest(this.getRequest());
      if (this.shopRowPanel != null) {
        this.shopRowPanel.filterItems();
      }
      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    protected void execute(final boolean buying, final AdventureResult[] items) {
      this.execute(buying, items, null);
    }

    protected void execute(
        final boolean buying, final AdventureResult[] items, final String extraAction) {
      if (items.length == 0) {
        return;
      }

      CoinMasterRequest request = this.getRequest(buying, items);
      if (extraAction != null) {
        request.addFormField(extraAction);
      }

      RequestThread.postRequest(request);

      if (this.shopRowPanel != null) {
        this.shopRowPanel.filterItems();
      }
      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    public AdventureResult[] getDesiredBuyItems(
        final AdventureResult[] items, final boolean fromStorage) {
      if (items.length == 0) {
        return null;
      }

      CoinmasterData data = this.data;
      Map<Integer, Integer> originalBalances = new TreeMap<>();
      Map<Integer, Integer> balances = new TreeMap<>();
      int neededSize = items.length;

      for (int i = 0; i < items.length; ++i) {
        AdventureResult item = items[i];
        int itemId = item.getItemId();

        if (!data.availableItem(itemId)) {
          // This was shown but was grayed out.
          items[i] = null;
          --neededSize;
          continue;
        }

        AdventureResult cost = data.itemBuyPrice(itemId);
        Integer currency = cost.getItemId();
        int price = cost.getCount();

        Integer value = originalBalances.get(currency);
        if (value == null) {
          int newValue =
              fromStorage ? data.availableStorageTokens(cost) : data.availableTokens(cost);
          value = newValue;
          originalBalances.put(currency, value);
          balances.put(currency, value);
        }

        int originalBalance = value.intValue();
        int balance = balances.get(currency).intValue();

        if (price > originalBalance) {
          // This was grayed out.
          items[i] = null;
          --neededSize;
          continue;
        }

        int max = CoinmasterPanel.this.buyMax(item, balance / price);
        int quantity = max;

        if (max > 1) {
          int def = CoinmasterPanel.this.buyDefault(max);
          String val =
              InputFieldUtilities.input(
                  "Buying " + item.getName() + "...", KoLConstants.COMMA_FORMAT.format(def));
          if (val == null) {
            // He hit cancel
            return null;
          }

          quantity = StringUtilities.parseInt(val);
        }

        if (quantity > max) {
          quantity = max;
        }

        if (quantity <= 0) {
          items[i] = null;
          --neededSize;
          continue;
        }

        items[i] = item.getInstance(quantity);
        balance -= quantity * price;
        balances.put(currency, balance);
      }

      // Shrink the array which will be returned so
      // that it removes any nulled values.

      if (neededSize == 0) {
        return null;
      }

      AdventureResult[] desiredItems = new AdventureResult[neededSize];
      neededSize = 0;

      for (int i = 0; i < items.length; ++i) {
        if (items[i] != null) {
          desiredItems[neededSize++] = items[i];
        }
      }

      return desiredItems;
    }

    public boolean canBuyItem(AdventureResult item) {
      return this.data.canBuyItem(item.getItemId());
    }

    public class SellPanel extends ItemListManagePanel<AdventureResult> {
      public SellPanel() {
        super((SortedListModel<AdventureResult>) KoLConstants.inventory);
        this.setButtons(
            true,
            new ActionListener[] {
              new SellListener(),
            });

        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, false));
        this.setEnabled(true);
        this.filterItems();
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        this.buttons[0].setEnabled(CoinmasterPanel.this.enabled());
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {
        if (CoinmasterPanel.this.addSellMovers()) {
          super.addMovers();
        }
      }

      @Override
      public AutoFilterTextField<AdventureResult> getWordFilter() {
        return new SellableFilterField();
      }

      @Override
      public void actionConfirmed() {}

      @Override
      public void actionCancelled() {}

      public class SellListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canSell();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          if (!InputFieldUtilities.confirm(
              "Are you sure you would like to trade in the selected items?")) {
            return;
          }

          AdventureResult[] items = SellPanel.this.getDesiredItems("Selling");
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(false, items);
        }

        @Override
        public String toString() {
          return "sell";
        }
      }

      private class SellableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof AdventureResult ar)) {
            return false;
          }
          int price =
              CoinmastersDatabase.getPrice(
                  ar.getItemId(), CoinmasterPanel.this.data.getSellPrices());
          return (price > 0) && super.isVisible(element);
        }
      }
    }

    public class BuyPanel extends ItemListManagePanel<AdventureResult> {
      public BuyPanel(ActionListener[] listeners) {
        super((LockableListModel<AdventureResult>) CoinmasterPanel.this.data.getBuyItems());

        this.eastPanel.add(
            new InvocationButton("visit", CoinmasterPanel.this, "check"), BorderLayout.SOUTH);
        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, true));
        this.getElementList().setVisibleRowCount(6);

        if (listeners != null) {
          this.setButtons(true, listeners);
          this.setEnabled(true);
          this.filterItems();
        }
      }

      public BuyPanel() {
        this(null);

        boolean storage = CoinmasterPanel.this.data.getStorageAction() != null;
        int count = storage ? 2 : 1;
        ActionListener[] listeners = new ActionListener[count];
        listeners[0] = new BuyListener();
        if (count > 1) {
          listeners[1] = new BuyUsingStorageListener();
        }

        this.setButtons(true, listeners);
        this.setEnabled(true);
        this.filterItems();
      }

      public void addButton(final JButton button, final boolean save) {
        JButton[] buttons = new JButton[1];
        buttons[0] = button;
        this.addButtons(buttons, save);
      }

      @Override
      public void addButtons(final JButton[] buttons, final boolean save) {
        super.addButtons(buttons, save);
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        for (int i = 0; this.buttons != null && i < this.buttons.length; ++i) {
          this.buttons[i].setEnabled(CoinmasterPanel.this.enabled());
        }
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {}

      @Override
      public AutoFilterTextField<AdventureResult> getWordFilter() {
        return new BuyableFilterField();
      }

      public AdventureResult[] getDesiredItems(final boolean fromStorage) {
        AdventureResult[] items = this.getSelectedValues().toArray(new AdventureResult[0]);
        return CoinmasterPanel.this.getDesiredBuyItems(items, fromStorage);
      }

      public class BuyListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canBuy();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          AdventureResult[] items = BuyPanel.this.getDesiredItems(false);
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(true, items);
        }

        @Override
        public String toString() {
          return "buy";
        }
      }

      public class BuyUsingStorageListener extends ThreadedListener {
        @Override
        protected void execute() {
          AdventureResult[] items = BuyPanel.this.getDesiredItems(true);
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(true, items, CoinmasterPanel.this.data.getStorageAction());
        }

        @Override
        public String toString() {
          return "from storage";
        }
      }

      private class BuyableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof AdventureResult ar)) {
            return false;
          }
          return CoinmasterPanel.this.canBuyItem(ar) && super.isVisible(element);
        }
      }
    }

    public class ShopRowPanel extends ItemListManagePanel<ShopRow> {
      // Unlike an AdventureResult, a ShopRow doesn't come with a count field.
      private record ShopRowSelection(ShopRow row, int count) {}

      public ShopRowPanel(ActionListener[] listeners) {
        super((LockableListModel<ShopRow>) CoinmasterPanel.this.data.getShopRows());
        this.eastPanel.add(
            new InvocationButton("visit", CoinmasterPanel.this, "check"), BorderLayout.SOUTH);
        this.getElementList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, true));
        this.getElementList().setVisibleRowCount(6);

        if (listeners != null) {
          this.setButtons(true, listeners);
          this.setEnabled(true);
          this.filterItems();
        }
      }

      public ShopRowPanel() {
        this(null);

        ActionListener[] listeners = new ActionListener[1];
        listeners[0] = new BuyListener();

        this.setButtons(true, listeners);
        this.setEnabled(true);
        this.filterItems();
      }

      public void addButton(final JButton button, final boolean save) {
        JButton[] buttons = new JButton[1];
        buttons[0] = button;
        this.addButtons(buttons, save);
      }

      @Override
      public void addButtons(final JButton[] buttons, final boolean save) {
        super.addButtons(buttons, save);
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        for (int i = 0; this.buttons != null && i < this.buttons.length; ++i) {
          this.buttons[i].setEnabled(CoinmasterPanel.this.enabled());
        }
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {}

      @Override
      public AutoFilterTextField<ShopRow> getWordFilter() {
        return new BuyableFilterField();
      }

      public ShopRowSelection getDesiredRow() {
        ShopRow row = this.getSelectedValue();
        return getDesiredRow(row);
      }

      public ShopRowSelection getDesiredRow(final ShopRow row) {
        int max = row.getAffordableCount();

        if (max <= 0) {
          return null;
        }

        AdventureResult item = row.getItem();
        int quantity = 1;

        if (max > 1) {
          int def = CoinmasterPanel.this.buyDefault(max);
          String val =
              InputFieldUtilities.input(
                  "Buying " + item.getName() + "...", KoLConstants.COMMA_FORMAT.format(def));
          if (val == null) {
            // He hit cancel
            return null;
          }

          quantity = StringUtilities.parseInt(val);
        }

        if (quantity > max) {
          quantity = max;
        }

        if (quantity <= 0) {
          return null;
        }

        return new ShopRowSelection(row, quantity);
      }

      public CoinMasterRequest getRequest(final ShopRow row, int quantity) {
        return CoinmasterPanel.this.data.getRequest(row, quantity);
      }

      protected void execute(final ShopRow row, int quantity) {
        CoinMasterRequest request = getRequest(row, quantity);
        RequestThread.postRequest(request);
        filterItems();
      }

      public class BuyListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canBuy();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          ShopRowSelection selection = ShopRowPanel.this.getDesiredRow();
          if (selection == null) {
            return;
          }

          ShopRowPanel.this.execute(selection.row(), selection.count());
        }

        @Override
        public String toString() {
          return "buy";
        }
      }

      private class BuyableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof ShopRow)) {
            return false;
          }
          return super.isVisible(element);
        }
      }
    }
  }

  public static final DefaultListCellRenderer getCoinmasterRenderer(
      CoinmasterData data, final boolean buying) {
    return new CoinmasterRenderer(data, buying);
  }

  private static class CoinmasterRenderer extends DefaultListCellRenderer {
    private final CoinmasterData data;
    private final boolean buying;

    public CoinmasterRenderer(CoinmasterData data, final boolean buying) {
      this.setOpaque(true);
      this.data = data;
      this.buying = buying;
    }

    public boolean allowHighlight() {
      return true;
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      Component defaultComponent =
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value == null) {
        return defaultComponent;
      }

      if (value instanceof AdventureResult) {
        return this.getRenderer(defaultComponent, (AdventureResult) value);
      }

      if (value instanceof ShopRow) {
        return this.getRenderer(defaultComponent, (ShopRow) value);
      }

      return defaultComponent;
    }

    public Component getRenderer(final Component defaultComponent, final AdventureResult ar) {
      boolean show = true;
      AdventureResult cost = null;

      if (ar.isSkill()) {
        int skillId = ar.getSkillId();
        cost = this.data.skillBuyPrice(skillId);
        show = data.availableSkill(skillId);
      } else if (ar.isItem()) {
        int itemId = ar.getItemId();
        cost = this.buying ? this.data.itemBuyPrice(itemId) : this.data.itemSellPrice(itemId);
        show = !this.buying || data.availableItem(itemId);
      } else {
        return defaultComponent;
      }

      if (cost == null) {
        return defaultComponent;
      }

      int price = cost.getCount();

      if (cost.isMeat()) {
        price = NPCPurchaseRequest.currentDiscountedPrice(price);
      }

      if (show && this.buying) {
        int balance1 = this.data.availableTokens(cost);
        int balance2 = this.data.availableStorageTokens(cost);
        if (price > balance1 && price > balance2) {
          show = false;
        }
      }

      StringBuilder stringForm = new StringBuilder();
      stringForm.append("<html>");
      if (!show) {
        stringForm.append("<font color=gray>");
      }
      stringForm.append(ar.getName());
      stringForm.append(" (");
      stringForm.append(price);
      stringForm.append(" ");
      stringForm.append(cost.getPluralName(price));
      stringForm.append(")");
      int count = ar.getCount();
      if (count == -1) {
        stringForm.append(" (unknown)");
      } else if (count != PurchaseRequest.MAX_QUANTITY) {
        stringForm.append(" (");
        stringForm.append(KoLConstants.COMMA_FORMAT.format(count));
        stringForm.append(")");
      }
      if (!show) {
        stringForm.append("</font>");
      }
      stringForm.append("</html>");

      ((JLabel) defaultComponent).setText(stringForm.toString());
      return defaultComponent;
    }

    public Component getRenderer(final Component defaultComponent, final ShopRow sr) {
      AdventureResult[] costs = sr.getCosts();

      if (costs == null) {
        return defaultComponent;
      }

      AdventureResult ar = sr.getItem();
      boolean show = true;

      if (ar.isSkill()) {
        int skillId = ar.getSkillId();
        show = data.availableSkill(skillId);
      } else if (ar.isItem()) {
        show = sr.getAffordableCount() > 0;
      } else {
        return defaultComponent;
      }

      String costString = sr.costString();

      StringBuilder stringForm = new StringBuilder();
      stringForm.append("<html>");
      if (!show) {
        stringForm.append("<font color=gray>");
      }
      stringForm.append(ar.getName());
      int count = ar.getCount();
      if (count == -1) {
        stringForm.append(" (unknown)");
      } else if (count > 1 && count != PurchaseRequest.MAX_QUANTITY) {
        stringForm.append(" (");
        stringForm.append(KoLConstants.COMMA_FORMAT.format(count));
        stringForm.append(")");
      }
      stringForm.append(" ");
      stringForm.append(costString);
      if (!show) {
        stringForm.append("</font>");
      }
      stringForm.append("</html>");

      ((JLabel) defaultComponent).setText(stringForm.toString());
      return defaultComponent;
    }
  }
}
