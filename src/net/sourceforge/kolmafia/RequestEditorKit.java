package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.NemesisManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.VolcanoMazeManager;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.BasementDecorator;
import net.sourceforge.kolmafia.webui.BeerPongDecorator;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;
import net.sourceforge.kolmafia.webui.ClanFortuneDecorator;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.FightDecorator;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;
import net.sourceforge.kolmafia.webui.MineDecorator;
import net.sourceforge.kolmafia.webui.NemesisDecorator;
import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;
import net.sourceforge.kolmafia.webui.TopMenuDecorator;
import net.sourceforge.kolmafia.webui.UseItemDecorator;
import net.sourceforge.kolmafia.webui.UseLinkDecorator;
import net.sourceforge.kolmafia.webui.UseLinkDecorator.UseLink;
import net.sourceforge.kolmafia.webui.ValhallaDecorator;

public class RequestEditorKit extends HTMLEditorKit {
  private static final Pattern FORM_PATTERN = Pattern.compile("name=choiceform(\\d+)");
  public static final Pattern OPTION_PATTERN = Pattern.compile("name=option value=(\\d+)");
  public static final Pattern BUTTON_TEXT_PATTERN =
      Pattern.compile("type=['\"]?submit['\"]? value=['\"](.*?)['\"]", Pattern.DOTALL);
  private static final Pattern OUTFIT_FORM_PATTERN =
      Pattern.compile("<form name=outfit.*?</form>", Pattern.DOTALL);
  private static final Pattern OPTGROUP_PATTERN =
      Pattern.compile("<optgroup label=['\"]([^']*)['\"]>(.*?)</optgroup>", Pattern.DOTALL);
  private static final Pattern NOLABEL_CUSTOM_OUTFITS_PATTERN =
      Pattern.compile("\\(select an outfit\\)</option>(<option.*?)<optgroup", Pattern.DOTALL);

  private static final Pattern ROUND_SEP_PATTERN = Pattern.compile("<(?:b>Combat!</b>|hr.*?>)");
  private static final Pattern RCM_JS_PATTERN = Pattern.compile("rcm\\.(\\d+\\.)?js");

  private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

  /**
   * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts some of the form
   * handling to ensure that <code>GenericRequest</code> objects are instantiated on form submission
   * rather than the <code>HttpRequest</code> objects created by the default HTML editor kit.
   */
  @Override
  public ViewFactory getViewFactory() {
    return RequestEditorKit.DEFAULT_FACTORY;
  }

  /**
   * Registers thethat is supposed to be used for handling data submission to the Kingdom of
   * Loathing server.
   */
  private static class RequestViewFactory extends HTMLFactory {
    @Override
    public View create(final Element elem) {
      if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.INPUT) {
        return new KoLSubmitView(elem);
      }

      if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG) {
        return new KoLImageView(elem);
      }

      return super.create(elem);
    }
  }

  private static class KoLImageView extends ImageView {
    public KoLImageView(final Element elem) {
      super(elem);
    }

    @Override
    public URL getImageURL() {
      String src = (String) this.getElement().getAttributes().getAttribute(HTML.Attribute.SRC);

      if (src == null) {
        return null;
      }

      File imageFile = FileUtilities.downloadImage(src);

      try {
        return imageFile.toURI().toURL();
      } catch (IOException e) {
        return null;
      }
    }
  }

  public static final String getFeatureRichHTML(final String location, final String text) {
    return RequestEditorKit.getFeatureRichHTML(location, text, true);
  }

  public static final void getFeatureRichHTML(final String location, final StringBuffer buffer) {
    RequestEditorKit.getFeatureRichHTML(location, buffer, true);
  }

  // Stupid bureacrats, always ruining everybody's fun with their permits
  // and forms.
  private static final String NO_PERMIT_TEXT =
      "always ruining everybody's fun with their permits and forms.";
  private static final String BUY_PERMIT_TEXT =
      RequestEditorKit.NO_PERMIT_TEXT
          + " [<a href=\"hermit.php?autopermit=on\">buy a hermit permit</a>]";

  // He looks at you with a disappointed sigh -- looks like you don't have
  // anything worthless enough for him to want to trade for it.
  private static final String NO_WORTHLESS_ITEM_TEXT =
      "worthless enough for him to want to trade for it.";
  private static final String BUY_WORTHLESS_ITEM_TEXT =
      RequestEditorKit.NO_WORTHLESS_ITEM_TEXT
          + " [<a href=\"hermit.php?autoworthless=on\">fish for a worthless item</a>]";

  private static final ArrayList<String> maps = new ArrayList<String>();

  static {
    RequestEditorKit.maps.add("place.php?whichplace=plains");
    RequestEditorKit.maps.add("place.php?whichplace=bathole");
    RequestEditorKit.maps.add("fernruin.php");
    RequestEditorKit.maps.add("cobbsknob.php");
    RequestEditorKit.maps.add("cobbsknob.php?action=tolabs");
    RequestEditorKit.maps.add("cobbsknob.php?action=tomenagerie");
    RequestEditorKit.maps.add("cyrpt.php");
    RequestEditorKit.maps.add("place.php?whichplace=beanstalk");
    RequestEditorKit.maps.add("woods.php");
    RequestEditorKit.maps.add("friars.php");
    RequestEditorKit.maps.add("pandamonium.php");
    RequestEditorKit.maps.add("place.php?whichplace=mountains");
    RequestEditorKit.maps.add("tutorial.php");
    RequestEditorKit.maps.add("place.php?whichplace=mclargehuge");
    RequestEditorKit.maps.add("island.php");
    RequestEditorKit.maps.add("place.php?whichplace=cove");
    RequestEditorKit.maps.add("bigisland.php");
    RequestEditorKit.maps.add("postwarisland.php");
    RequestEditorKit.maps.add("place.php?whichplace=desertbeach");
    RequestEditorKit.maps.add("pyramid.php");
    RequestEditorKit.maps.add("place.php?whichplace=town_wrong");
    RequestEditorKit.maps.add("place.php?whichplace=town_right");
    RequestEditorKit.maps.add("place.php?whichplace=spookyraven1");
    RequestEditorKit.maps.add("place.php?whichplace=spookyraven2");
    RequestEditorKit.maps.add("place.php?whichplace=wormwood");
    RequestEditorKit.maps.add("manor3.php");
    RequestEditorKit.maps.add("da.php");
    RequestEditorKit.maps.add("canadia.php");
    RequestEditorKit.maps.add("gnomes.php");
    RequestEditorKit.maps.add("heydeze.php");
    RequestEditorKit.maps.add("dwarffactory.php");
  }

  public static final String getFeatureRichHTML(
      final String location, final String text, final boolean addComplexFeatures) {
    if (text == null || text.length() == 0) {
      return "";
    }

    StringBuffer buffer = new StringBuffer(text);
    RequestEditorKit.getFeatureRichHTML(location, buffer, addComplexFeatures);
    return buffer.toString();
  }

  public static final void getFeatureRichHTML(
      final String location, final StringBuffer buffer, final boolean addComplexFeatures) {
    if (buffer.length() == 0) {
      return;
    }

    // Skip all decorations on the raw KoL api.

    if (location.startsWith("api.php")) {
      return;
    }

    // Remove bogus <body> tag preceding <head> tag.  topmenu has
    // this, but don't assume other pages are flawless

    StringUtilities.singleStringReplace(buffer, "<body><head>", "<head>");

    // Apply individual page adjustments

    RequestEditorKit.applyPageAdjustments(location, buffer, addComplexFeatures);

    // Apply adjustments that should be on all pages

    RequestEditorKit.applyGlobalAdjustments(location, buffer, addComplexFeatures);
  }

  protected static final void applyPageAdjustments(
      final String location, final StringBuffer buffer, final boolean addComplexFeatures) {
    // Check for charpane first, since it occurs frequently.

    if (location.startsWith("charpane.php")) {
      if (addComplexFeatures) {
        CharPaneDecorator.decorate(buffer);
      }

      return;
    }

    // Handle topmenu

    if (location.contains("menu.php")) {
      TopMenuDecorator.decorate(buffer, location);
      return;
    }

    // Override images, if requested
    RelayRequest.overrideImages(buffer);

    // Make changes which only apply to a single page.

    if (location.startsWith("account.php")) {
      StringUtilities.singleStringReplace(
          buffer, "Manage Subscriptions", "Manage Subscriptions (this will not work in KoLmafia)");
      StringUtilities.singleStringReplace(
          buffer, "account_subscription.php\"", "#\" title='This will not work in KoLmafia'");
    } else if (location.startsWith("account_combatmacros.php")) {
      StringUtilities.insertAfter(
          buffer,
          "</textarea>",
          "<script language=JavaScript src=\"/" + KoLConstants.MACROHELPER_JS + "\"></script>");
    } else if (location.startsWith("adminmail.php")) {
      // Per KoL dev team request, add extra warning to the
      // bug report form.
      RequestEditorKit.addBugReportWarning(buffer);
    } else if (location.startsWith("adventure.php")) {
      RequestEditorKit.fixTavernCellar(buffer);
      // RequestEditorKit.fixBallroom1( buffer );
      RequestEditorKit.fixDucks(buffer);
      StationaryButtonDecorator.decorate(location, buffer);
      RequestEditorKit.fixBallroom2(buffer);
      RequestEditorKit.fixGovernmentLab(buffer);
    } else if (location.startsWith("ascend.php")) {
      ValhallaDecorator.decorateGashJump(location, buffer);
    } else if (location.startsWith("ascensionhistory.php")) {
      // No Javascript in Java's HTML renderer
      if (addComplexFeatures) {
        StringUtilities.insertBefore(
            buffer,
            "</head>",
            "<script language=\"Javascript\" src=\"/" + KoLConstants.SORTTABLE_JS + "\"></script>");
        StringUtilities.singleStringReplace(
            buffer,
            "<table><tr><td class=small>",
            "<table class=\"sortable\" id=\"history\"><tr><td class=small>");
        StringUtilities.globalStringReplace(
            buffer,
            "<tr><td colspan=9",
            "<tr class=\"sortbottom\" style=\"display:none\"><td colspan=9");
      }
    } else if (location.startsWith("barrel.php")) {
      BarrelDecorator.decorate(buffer);
    } else if (location.startsWith("basement.php")) {
      BasementDecorator.decorate(buffer);
    } else if (location.startsWith("bathole.php")) {
      StringUtilities.globalStringReplace(buffer, "action=bathole.php", "action=adventure.php");
    } else if (location.startsWith("beerpong.php")) {
      BeerPongDecorator.decorate(buffer);
    } else if (location.startsWith("bigisland.php")) {
      IslandDecorator.decorateBigIsland(location, buffer);
    } else if (location.startsWith("casino.php")) {
      if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.LUCKY))) {
        StringUtilities.insertAfter(
            buffer,
            "<a href=\"casino.php?action=slot&whichslot=11\"",
            " onclick=\"return confirm('Are you sure you want to adventure here WITHOUT Lucky!?');\"");
      }
    } else if (location.startsWith("cave.php")) {
      NemesisManager.decorate(location, buffer);
    } else if (location.startsWith("choice.php")) {
      RequestEditorKit.fixTavernCellar(buffer);
      StationaryButtonDecorator.decorate(location, buffer);
      RequestEditorKit.addChoiceSpoilers(location, buffer);
      RequestEditorKit.addBarrelSounds(buffer);
    } else if (location.startsWith("clan_hobopolis.php")) {
      HobopolisDecorator.decorate(location, buffer);
    } else if (location.startsWith("clan_viplounge.php?preaction=testlove")) {
      ClanFortuneDecorator.decorateAnswer(buffer);
    } else if (location.startsWith("council.php")) {
      RequestEditorKit.decorateCouncil(buffer);
    } else if (location.startsWith("crypt.php")) {
      RequestEditorKit.decorateCrypt(buffer);
    } else if (location.startsWith("dwarffactory.php")) {
      DwarfFactoryRequest.decorate(location, buffer);
    } else if (location.startsWith("fight.php")) {
      // Remove bogus directive in monster images
      StringUtilities.globalStringDelete(buffer, "crossorigin=\"Anonymous\"");

      RequestEditorKit.suppressInappropriateNags(buffer);
      RequestEditorKit.suppressPowerPixellation(buffer);
      RequestEditorKit.fixTavernCellar(buffer);

      // Decorate end of fight before stationary buttons
      FightDecorator.decorateEndOfFight(buffer);

      StationaryButtonDecorator.decorate(location, buffer);

      DiscoCombatHelper.decorate(buffer);
      RequestEditorKit.addFightModifiers(buffer);
      RequestEditorKit.addTaleOfDread(buffer);
      RequestEditorKit.addDesertProgress(buffer);
      RequestEditorKit.addBlackForestProgress(buffer);
      RequestEditorKit.addPartyFairProgress(buffer);

      // Do any monster-specific decoration
      FightDecorator.decorateMonster(buffer);

      // Do any location-specific decoration
      FightDecorator.decorateLocation(buffer);
    } else if (location.startsWith("fambattle.php")) {
      // Do a subset of the above for a Pokefam battle
      RequestEditorKit.fixTavernCellar(buffer);
      FightDecorator.decorateEndOfFight(buffer);
      // Not yet.
      // StationaryButtonDecorator.decorate( location, buffer );
      RequestEditorKit.addFambattleModifiers(buffer);
      RequestEditorKit.addDesertProgress(buffer);
      RequestEditorKit.addBlackForestProgress(buffer);
      FightDecorator.decorateMonster(buffer);
      FightDecorator.decorateLocation(buffer);
    } else if (location.startsWith("hermit.php")) {
      StringUtilities.singleStringReplace(
          buffer, RequestEditorKit.NO_PERMIT_TEXT, RequestEditorKit.BUY_PERMIT_TEXT);
      StringUtilities.singleStringReplace(
          buffer,
          RequestEditorKit.NO_WORTHLESS_ITEM_TEXT,
          RequestEditorKit.BUY_WORTHLESS_ITEM_TEXT);
    } else if (location.startsWith("inventory.php")) {
      RequestEditorKit.decorateInventory(buffer, addComplexFeatures);
      UseItemDecorator.decorate(location, buffer);
    } else if (location.startsWith("inv_use.php")) {
      UseItemDecorator.decorate(location, buffer);
    } else if (location.contains("lchat.php")) {
      StringUtilities.globalStringDelete(buffer, "spacing: 0px;");
      StringUtilities.insertBefore(
          buffer,
          "if (postedgraf",
          "if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } ");
    } else if (location.startsWith("mall.php")) {
      MallSearchRequest.decorateMallSearch(buffer);
    } else if (location.startsWith("mining.php")) {
      MineDecorator.decorate(location, buffer);
    } else if (location.startsWith("mrstore.php")) {
      StringUtilities.singleStringReplace(
          buffer, "account_subscription.php", "# title='This will not work in KoLmafia'");
      StringUtilities.singleStringReplace(
          buffer, "subscribing</a>", "subscribing (does not work in KoLmafia)</a>");
    } else if (location.startsWith("multiuse.php")) {
      RequestEditorKit.addMultiuseModifiers(buffer);
    } else if (location.startsWith("ocean.php")) {
      OceanManager.decorate(buffer);
    } else if (location.startsWith("pandamonium.php")) {
      PandamoniumRequest.decoratePandamonium(location, buffer);
    } else if (location.startsWith("place.php?whichplace=arcade")) {
      StringBuilder note = new StringBuilder("Arcade (");
      int count = InventoryManager.getCount(ItemPool.GG_TOKEN);
      note.append(count);
      note.append(" token");
      if (count != 1) {
        note.append('s');
      }
      note.append(", ");
      count = InventoryManager.getCount(ItemPool.GG_TICKET);
      note.append(count);
      note.append(" ticket");
      if (count != 1) {
        note.append('s');
      }
      note.append(")</b>");

      StringUtilities.singleStringReplace(buffer, "Arcade</b>", note.toString());
    } else if (location.startsWith("place.php")) {
      PlaceRequest.decorate(location, buffer);
    } else if (location.startsWith("postwarisland.php")) {
      IslandDecorator.decoratePostwarIsland(location, buffer);
    } else if (location.startsWith("searchplayer.php")) {
      StringUtilities.insertAfter(buffer, "name=pvponly", " checked");
      StringUtilities.singleStringReplace(buffer, "value=0 checked", "value=0");

      if (KoLCharacter.isHardcore()) {
        StringUtilities.insertAfter(buffer, "value=1", " checked");
      } else {
        StringUtilities.insertAfter(buffer, "value=2", " checked");
      }
    } else if (location.startsWith("tiles.php")) {
      DvorakManager.decorate(buffer);
    } else if (location.startsWith("volcanomaze.php")) {
      VolcanoMazeManager.decorate(location, buffer);
    } else if (location.startsWith("wand.php") && !location.contains("notrim=1")) {
      ZapRequest.decorate(buffer);
    }
  }

  protected static final void applyGlobalAdjustments(
      final String location, final StringBuffer buffer, final boolean addComplexFeatures) {
    // Make basics.js and basics.css available to all pages

    if (addComplexFeatures) {
      StringUtilities.insertBefore(
          buffer,
          "</head>",
          "<script language=\"Javascript\" src=\"/" + KoLConstants.BASICS_JS + "\"></script>");

      StringUtilities.insertBefore(
          buffer,
          "</head>",
          "<link rel=\"stylesheet\" href=\"/" + KoLConstants.BASICS_CSS + "\" />");
    }

    // Skip additional decorations for the character pane and the top menu

    if (location.startsWith("charpane.php") || location.contains("menu.php")) {
      return;
    }

    // Handle changes which happen on a lot of different pages
    // rather than just one or two.

    RequestEditorKit.changePunchcardNames(buffer);
    RequestEditorKit.changePotionImages(buffer);
    RequestEditorKit.decorateLevelGain(buffer);
    RequestEditorKit.addAbsintheLink(buffer);
    RequestEditorKit.addTransponderLink(buffer);
    RequestEditorKit.addBatteryLink(buffer);
    RequestEditorKit.addFolioLink(buffer);
    RequestEditorKit.addNewLocationLinks(buffer);
    RequestEditorKit.suppressPotentialMalware(buffer);
    RequestEditorKit.extendRightClickMenu(buffer);

    // Now do anything which doesn't work in Java's internal HTML renderer

    if (addComplexFeatures) {
      if (RequestEditorKit.maps.contains(location)) {
        buffer.insert(
            buffer.indexOf("</tr>"),
            "<td width=15 valign=bottom align=left bgcolor=blue><a style=\"color: white; font-weight: normal; font-size: small; text-decoration: underline\" href=\"javascript: attachSafetyText(); void(0);\">?</a>");
        buffer.insert(buffer.indexOf("<td", buffer.indexOf("</tr>")) + 3, " colspan=2");
      }

      if (Preferences.getBoolean("relayAddsUseLinks")) {
        UseLinkDecorator.decorate(location, buffer);
      }

      if (buffer.indexOf("showplayer.php") != -1 && !RCM_JS_PATTERN.matcher(buffer).find()) {
        RequestEditorKit.addChatFeatures(buffer);
      }

      // Always select the contents of text fields when you
      // click on them to make for easy editing.

      if (Preferences.getBoolean("autoHighlightOnFocus") && buffer.indexOf("</html>") != -1) {
        StringUtilities.insertBefore(
            buffer, "</html>", "<script src=\"/" + KoLConstants.ONFOCUS_JS + "\"></script>");
      }

      if (location.contains("fight.php")) {
        StringUtilities.insertBefore(
            buffer, "</html>", "<script src=\"/" + KoLConstants.COMBATFILTER_JS + "\"></script>");
      }
    }

    Matcher eventMatcher = EventManager.eventMatcher(buffer.toString());

    if (EventManager.hasEvents() && (eventMatcher != null || location.equals("main.php"))) {
      int eventTableInsertIndex = 0;

      if (eventMatcher != null) {
        eventTableInsertIndex = eventMatcher.start();

        buffer.setLength(0);
        buffer.append(eventMatcher.replaceFirst(""));
      } else {
        eventTableInsertIndex = buffer.indexOf("</div>") + 6;
      }

      StringBuilder eventsTable = new StringBuilder();

      eventsTable.append("<center><table width=95% cellspacing=0 cellpadding=0>");
      eventsTable.append("<tr><td style=\"color: white;\" align=center bgcolor=orange>");
      eventsTable.append("<b>New Events:</b>");
      eventsTable.append("</td></tr>");
      eventsTable.append("<tr><td style=\"padding: 5px; border: 1px solid orange;\" align=center>");

      Iterator<String> eventHyperTextIterator = EventManager.getEventHyperTexts().iterator();

      while (eventHyperTextIterator.hasNext()) {
        eventsTable.append(eventHyperTextIterator.next());

        if (eventHyperTextIterator.hasNext()) {
          eventsTable.append("<br />");
        }
      }

      eventsTable.append("</td></tr>");
      eventsTable.append("<tr><td height=4></td></tr>");
      eventsTable.append("</table></center>");

      buffer.insert(eventTableInsertIndex, eventsTable.toString());

      EventManager.clearEventHistory();
    }

    // Having done all the decoration on the page, do things that
    // might modify or depend on those decorations

    // Change border colors if the user wants something other than blue

    String defaultColor = Preferences.getString("defaultBorderColor");
    if (!defaultColor.equals("blue")) {
      StringUtilities.globalStringReplace(
          buffer, "bgcolor=blue", "bgcolor=\"" + defaultColor + "\"");
      StringUtilities.globalStringReplace(
          buffer, "border: 1px solid blue", "border: 1px solid " + defaultColor);
    }
  }

  private static void extendRightClickMenu(StringBuffer buffer) {
    if (buffer.indexOf("pop_ircm_contents") != -1) {
      StringUtilities.insertBefore(
          buffer, "</html>", "<script src=\"/" + KoLConstants.IRCM_JS + "\"></script>");
    }
  }

  private static final String TOPMENU_REFRESH =
      "<script>top.menupane.location.href=\"topmenu.php\";</script>";

  public static final void addTopmenuRefresh(final StringBuffer buffer) {
    int index = buffer.indexOf("</body>");
    if (index != -1) {
      buffer.insert(index, RequestEditorKit.TOPMENU_REFRESH);
    }
  }

  private static void decorateLevelGain(final StringBuffer buffer) {
    String test = "<b>You gain a Level!</b>";
    int index = buffer.indexOf(test);

    if (index == -1) {
      String test2 = "<b>You gain some Levels!</b>";
      int index2 = buffer.indexOf(test2);
      if (index2 == -1) {
        return;
      }
      index = index2;
      test = test2;
    }

    StringBuilder links = new StringBuilder();
    boolean haveLinks = false;
    int newLevel = KoLCharacter.getLevel();

    links.append("<font size=1>");

    // If we are Level 13 or less, the Council might have quests for us
    if (newLevel <= 13) {
      // If we're Ed, and have already found we're talking to Amun instead, link to Amun
      if (KoLCharacter.isEd()
          && QuestDatabase.isQuestLaterThan(Quest.LARVA, QuestDatabase.UNSTARTED)) {
        links.append(" [<a href=\"council.php\">Amun</a>]");
      } else {
        links.append(" [<a href=\"council.php\">council</a>]");
      }
      haveLinks = true;
    }

    // If we are an Avatar of Boris, we can learn a new skill
    if (KoLCharacter.inAxecore() && newLevel <= 15) {
      links.append(" [<a href=\"da.php?place=gate1\">boris</a>]");
      haveLinks = true;
    } else if (KoLCharacter.isJarlsberg() && newLevel <= 15) {
      links.append(" [<a href=\"da.php?place=gate2\">jarlsberg</a>]");
      haveLinks = true;
    } else if (KoLCharacter.isSneakyPete() && newLevel <= 15) {
      links.append(" [<a href=\"da.php?place=gate3\">sneaky pete</a>]");
      haveLinks = true;
    } else if (KoLCharacter.isEd() && newLevel <= 15) {
      if (newLevel % 3 == 0) {
        links.append(" [<a href=\"/place.php?whichplace=edbase&action=edbase_door\">servant</a>]");
      } else {
        if (KoLCharacter.hasSkill("Bounty of Renenutet")
            && KoLCharacter.hasSkill("Wrath of Ra")
            && KoLCharacter.hasSkill("Curse of Stench")) {
          links.append(
              " [<a href=\"/place.php?whichplace=edbase&action=edbase_door\">servant xp</a>]");
        } else {
          links.append(
              " [<a href=\"/place.php?whichplace=edbase&action=edbase_book\">skill book</a>]");
        }
      }
      haveLinks = true;
    }

    // Otherwise, if we are level 15 or less, the guild might have a skill for us
    // Only give a link if we have opened the guild
    else if (newLevel <= 15 && KoLCharacter.getGuildStoreOpen()) {
      links.append(" [<a href=\"guild.php\">guild</a>]");
      haveLinks = true;
    }

    links.append("</font>");

    if (haveLinks) {
      buffer.insert(index + test.length(), links.toString());
    }
  }

  private static void addTransponderLink(final StringBuffer buffer) {
    // You can't get there anymore, because you don't know the
    // transporter frequency. You consider beating up Kenneth to
    // see if <i>he</i> remembers it, but you think better of it.

    String test =
        "You consider beating up Kenneth to see if <i>he</i> remembers it, but you think better of it.";
    int index = buffer.indexOf(test);

    if (index == -1) {
      test = "You can't get here without the proper transporter frequency.";
      index = buffer.indexOf(test);
    }

    if (index == -1) {
      return;
    }

    if (SpaaaceRequest.TRANSPONDER.getCount(KoLConstants.inventory) == 0) {
      return;
    }

    UseLink link =
        new UseLink(
            ItemPool.TRANSPORTER_TRANSPONDER,
            1,
            "use transponder",
            "inv_use.php?which=3&whichitem=");
    buffer.insert(index + test.length(), link.getItemHTML());
  }

  private static final AdventureResult WARBEAR_BATTERY = ItemPool.get(ItemPool.WARBEAR_BATTERY, 1);

  private static void addBatteryLink(final StringBuffer buffer) {
    // Your hoverbelt would totally do the trick to get you up
    // there, only it's out of juice.

    String test =
        "Your hoverbelt would totally do the trick to get you up there, only it's out of juice.";
    int index = buffer.indexOf(test);
    if (index == -1) {
      return;
    }

    if (RequestEditorKit.WARBEAR_BATTERY.getCount(KoLConstants.inventory) == 0) {
      return;
    }

    UseLink link =
        new UseLink(
            ItemPool.WARBEAR_BATTERY,
            1,
            "install warbear battery",
            "inv_use.php?which=3&whichitem=");
    buffer.insert(index + test.length(), link.getItemHTML());
  }

  private static void addFolioLink(final StringBuffer buffer) {
    // Remember that devilish folio you read?
    // No, you don't! You don't have it all still in your head!
    // Better find a new one you can read! I swear this:
    // 'Til you do, you can't visit the Suburbs of Dis!

    String test = "'Til you do, you can't visit the Suburbs of Dis!";
    int index = buffer.indexOf(test);

    if (index == -1) {
      return;
    }

    if (SuburbanDisRequest.FOLIO.getCount(KoLConstants.inventory) == 0) {
      return;
    }

    UseLink link =
        new UseLink(
            ItemPool.DEVILISH_FOLIO, 1, "use devilish folio", "inv_use.php?which=3&whichitem=");
    buffer.insert(index + test.length(), link.getItemHTML());
  }

  private static void addAbsintheLink(final StringBuffer buffer) {
    // For some reason, you can't find your way back there.

    String test = "For some reason, you can't find your way back there.";
    int index = buffer.indexOf(test);

    if (index == -1) {
      return;
    }

    if (ItemPool.get(ItemPool.ABSINTHE, 1).getCount(KoLConstants.inventory) == 0) {
      return;
    }

    UseLink link =
        new UseLink(ItemPool.ABSINTHE, 1, "use absinthe", "inv_use.php?which=3&whichitem=");
    buffer.insert(index + test.length(), link.getItemHTML());
  }

  // <table  width=400  cellspacing=0 cellpadding=0><tr><td style="color: white;" align=center
  // bgcolor=blue>f<b>New Area Unlocked</b></td></tr><tr><td style="padding: 5px; border: 1px solid
  // blue;"><center><table><tr><td><center><table><tr><td valign=center><img
  // src="http://images.kingdomofloathing.com/adventureimages/../otherimages/ocean/corrala.gif"></td><td valign=center class=small><b>The Coral Corral</b>, on <a class=nounder href=seafloor.php><b>The Sea Floor</b></a>.</td></tr></table></center></td></tr></table></center></td></tr><tr><td height=4></td></tr></table>

  private static final Pattern NEW_LOCATION_PATTERN =
      Pattern.compile(
          "<table.*?<b>New Area Unlocked</b>.*?(<img[^>]*>).*?(<b>(.*?)</b>)", Pattern.DOTALL);

  public static final void addNewLocationLinks(final StringBuffer buffer) {
    if (buffer.indexOf("New Area Unlocked") == -1) {
      return;
    }

    Matcher matcher = NEW_LOCATION_PATTERN.matcher(buffer);

    // The Trapper can unlock multiple new locations for you at once
    while (matcher.find()) {
      String image = matcher.group(1);
      String boldloc = matcher.group(2);
      String locname = matcher.group(3);
      String url;

      if (locname.contains("Degrassi Knoll")) {
        url =
            KoLCharacter.knollAvailable()
                ? "place.php?whichplace=knoll_friendly"
                : "place.php?whichplace=knoll_hostile";
      } else if (locname.contains("A Small Pyramid")) {
        url =
            KoLCharacter.isKingdomOfExploathing()
                ? "place.php?whichplace=exploathing_beach&action=expl_pyramidpre"
                : "place.php?whichplace=desertbeach&action=db_pyramid1";
      } else if (locname.contains("An Ancient Altar")) {
        url = "place.php?whichplace=spelunky&action=spelunky_side6";
      } else {
        KoLAdventure adventure = AdventureDatabase.getAdventure(locname);
        if (adventure == null) {
          if (locname.startsWith("The ")) {
            adventure = AdventureDatabase.getAdventure(locname.substring(4));
          } else {
            adventure = AdventureDatabase.getAdventure("The " + locname);
          }
        }

        if (adventure == null) {
          continue;
        }

        url = adventure.getRequest().getURLString();
      }

      String search = matcher.group(0);
      String replace;

      // Make the image clickable to go to the url
      StringBuilder rep = new StringBuilder();
      rep.append("<a href=\"");
      rep.append(url);
      rep.append("\">");
      rep.append(image);
      rep.append("</a>");
      replace = StringUtilities.singleStringReplace(search, image, rep.toString());

      // Make the location name clickable to go to the url
      rep.setLength(0);
      rep.append("<a class=nounder href=\"");
      rep.append(url);
      rep.append("\">");
      rep.append(boldloc);
      rep.append("</a>");
      replace = StringUtilities.singleStringReplace(replace, boldloc, rep.toString());

      // Insert the replacements into the buffer
      StringUtilities.singleStringReplace(buffer, search, replace);

      if (locname.equals("The Spooky Forest")) {
        // The Distant Woods must be accessible before The Florist Friar
        // can be used.  This is the most reliable place to detect that.
        FloristRequest.reset();
        RequestThread.postRequest(new FloristRequest());
      }
    }
  }

  // <script>
  //  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  //  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  //  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  //  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
  //
  //  ga('create', 'UA-47556088-1', 'kingdomofloathing.com');
  //  ga('send', 'pageview');
  //
  // </script>

  private static final Pattern MALWARE1_PATTERN =
      Pattern.compile(
          "<script>[\\s]*\\(function\\(i,s,o,g,r,a,m\\).*?GoogleAnalyticsObject.*?</script>",
          Pattern.DOTALL);

  // <script async src="//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>
  // <!-- ROS_728x90 -->
  // <ins class="adsbygoogle"
  //      style="display:inline-block;width:728px;height:90px"
  //      data-ad-client="ca-pub-5904875379193204"
  //      data-ad-slot="3053908571"></ins>
  // <script>
  // (adsbygoogle = window.adsbygoogle || []).push({});
  // </script>
  // <br><img src=/images/otherimages/1x1trans.gif height=4><br>

  private static final Pattern MALWARE2_PATTERN =
      Pattern.compile(
          "<script async src=\"//.*?adsbygoogle.js\".*?1x1trans.gif.*?<br>", Pattern.DOTALL);

  private static void suppressPotentialMalware(final StringBuffer buffer) {
    // Always remove lag-inducing Javascript
    if (false && !Preferences.getBoolean("suppressPotentialMalware")) {
      return;
    }

    if (buffer.indexOf("GoogleAnalyticsObject") != -1) {
      Matcher matcher = RequestEditorKit.MALWARE1_PATTERN.matcher(buffer);
      if (matcher.find()) {
        StringUtilities.globalStringDelete(buffer, matcher.group(0));
      }
    }

    if (buffer.indexOf("adsbygoogle") != -1) {
      Matcher matcher = RequestEditorKit.MALWARE2_PATTERN.matcher(buffer);
      if (matcher.find()) {
        StringUtilities.globalStringDelete(buffer, matcher.group(0));
      }
    }
  }

  // *********************************************************************
  //
  // If you have donated to KoL within the last 90 days, periodically you
  // get a nice "thank you" message on the fight page. I always smile when
  // I get such a thank you.
  //
  // If you have not donated within the last 90 days, you get a "nag",
  // suggesting that you donate and buy the current IOTM.
  //
  // I donate every month to buy the IOTM. A while ago, I decided to
  // donate $50 and get five Mr. A's, to be spent one per month. More
  // convenient for me, and better for KoL, since paying in advance always
  // favors the vendor, who gets the interest on your money.
  //
  // It turns out that this means I get 3 months of "thank you" and 2
  // months of "you haven't donated recently enough. Why don't you donate
  // for a Mr. A so you can buy the IOTM you just bought with a Mr. A. you
  // previously donated for?"
  //
  // Before I understood why this was happening, I sent a polite and
  // friendly bug report asking why I was getting nags, when I was a
  // regular donator. The response was that the advertising was behaving
  // as coded, and therefore was "correct"
  //
  // I do not expect a coding change on KoL's end to stop inappropriate
  // nags, so here is a simple self-service remedy.

  private static final Pattern NAG_PATTERN =
      Pattern.compile(
          "<table.*?Please consider supporting the Kingdom.*?<td height=4>.*?<td height=4>.*?</table>",
          Pattern.DOTALL);

  private static void suppressInappropriateNags(final StringBuffer buffer) {
    if (!Preferences.getBoolean("suppressInappropriateNags")) {
      return;
    }

    if (buffer.indexOf("Please consider supporting the Kingdom!") != -1) {
      Matcher matcher = RequestEditorKit.NAG_PATTERN.matcher(buffer);
      if (matcher.find()) {
        StringUtilities.globalStringDelete(buffer, matcher.group(0));
      }
    }
  }

  // If you are wearing a Powerful Glove, the "powerPixel" cosmetic
  // effect overlays every monster image. I found that amusing for about
  // five minutes and then humor-degrading forever more.

  private static final String OCRS_JS =
      "<script language=\"javascript\" src=\"/iii/scripts/ocrs.20200128.js\"></script>";
  private static final String OCRS_CSS =
      " <link rel=\"stylesheet\" href=\"/iii/scripts/ocrs.css\" />";

  // <img  crossorigin="anonymous"
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/powerpixels/eyes1.png"
  // style="position: absolute; top: 0px; left: 30px" id='peyes' />
  private static final Pattern EYES_PATTERN = Pattern.compile("<img[^>]*id='peyes' />");

  // <script>var ocrs =
  // ["flip","floating","broke","drunk","appendimg:adventureimages\/ol_drunk.gif:0:0","turgid","narcissistic"];</script>
  private static final Pattern OCRS_PATTERN =
      Pattern.compile("<script>var ocrs = \\[(.*?)\\];</script>", Pattern.DOTALL);

  private static void suppressPowerPixellation(final StringBuffer buffer) {
    boolean suppressPowerPixellation = Preferences.getBoolean("suppressPowerPixellation");
    String extraCosmeticModifiers = Preferences.getString("extraCosmeticModifiers").trim();
    boolean haveExtraCosmeticModifiers = !extraCosmeticModifiers.equals("");

    if (!suppressPowerPixellation && !haveExtraCosmeticModifiers) {
      return;
    }

    if (suppressPowerPixellation) {
      Matcher eyes = RequestEditorKit.EYES_PATTERN.matcher(buffer);
      if (eyes.find()) {
        StringUtilities.singleStringReplace(buffer, eyes.group(0), "");
      }
    }

    // Iterate over all ocrs modifiers. We should only manipulate
    // the cosmetic ones, but we can add or delete those at will.

    StringBuilder ocrs = new StringBuilder();
    String delimiter = "";

    Matcher matcher = RequestEditorKit.OCRS_PATTERN.matcher(buffer);
    boolean found = matcher.find();
    String find = found ? matcher.group(1) : "";

    if (found) {
      String[] modifiers = find.split(",");
      String powerPixel = "\"powerPixel\"";

      for (String modifier : modifiers) {
        if (suppressPowerPixellation && modifier.equals(powerPixel)) {
          continue;
        }
        ocrs.append(delimiter);
        ocrs.append(modifier);
        delimiter = ",";
      }
    }

    if (haveExtraCosmeticModifiers) {
      String[] extraModifiers = extraCosmeticModifiers.split(" *, *");

      for (String modifier : extraModifiers) {
        String image = MonsterData.cosmeticModifierImages.get(modifier);
        if (image != null) {
          ocrs.append(delimiter);
          ocrs.append("\"");
          ocrs.append("appendimg:adventureimages\\/");
          ocrs.append(image);
          ocrs.append("\"");
          delimiter = ",";
        } else {
          ocrs.append(delimiter);
          ocrs.append("\"");
          ocrs.append(modifier);
          ocrs.append("\"");
          delimiter = ",";
        }
      }
    }

    String replace = ocrs.toString();

    if (found) {
      // We had an "ocrs" var. Replace the value
      StringUtilities.singleStringReplace(buffer, find, replace);
    } else if (!replace.equals("")) {
      // We did not have an ocrs var but want to add some.
      // Include the appropriate scripts and insert a variable
      buffer.insert(buffer.indexOf("</head>"), OCRS_JS);
      buffer.insert(buffer.indexOf("</head>"), OCRS_CSS);
      String ocrs_var = "<script>var ocrs = [" + replace + "];</script>";
      buffer.insert(buffer.indexOf("</head>"), ocrs_var);
    }
  }

  // *******************************************************************

  private static void decorateInventory(
      final StringBuffer buffer, final boolean addComplexFeatures) {
    // <table width=100%><tr><td colspan=2 width="210"></td><td width=20 rowspan=2></td><td
    // class=small align=center valign=top rowspan=2><font size=2>[<a
    // href="craft.php">craft&nbsp;stuff</a>]&nbsp;  [<a
    // href="sellstuff.php">sell&nbsp;stuff</a>]<br /></font></td><td width=20 rowspan=2></td><td
    // colspan=2 width="210"></td></tr></table>

    StringBuilder links = new StringBuilder();
    boolean sushi = KoLCharacter.hasSushiMat();
    if (sushi) {
      links.append("[<a href=\"sushi.php\">roll sushi</a>]");
    }

    AdventureResult wand = KoLCharacter.getZapper();
    if (wand != null) {
      if (links.length() > 0) {
        links.append("&nbsp;&nbsp;");
      }

      links.append("[<a href=\"wand.php?whichwand=");
      links.append(wand.getItemId());
      links.append("\">zap items</a>]");
    }

    if (links.length() > 0) {
      StringUtilities.globalStringDelete(buffer, "<td width=20 rowspan=2></td>");
      StringUtilities.singleStringReplace(
          buffer, "<br /></font></td>", "<br />" + links.toString() + "<br /></font>");
    }

    // Automatically name the outfit "backup" for simple save
    // purposes while adventuring in browser.

    StringUtilities.insertAfter(buffer, "<input type=text name=outfitname", " value=\"Backup\"");

    if (!addComplexFeatures) {
      return;
    }

    // Split out normal outfits, custom outfits, automatic outfits
    Matcher fmatcher = OUTFIT_FORM_PATTERN.matcher(buffer);
    if (!fmatcher.find()) {
      return;
    }

    StringBuffer obuffer = new StringBuffer();
    obuffer.append("<table>");

    // If there aren't any normal outfits, the Custom Outfits label is absent
    Matcher cmatcher = NOLABEL_CUSTOM_OUTFITS_PATTERN.matcher(fmatcher.group());
    if (cmatcher.find()) {
      String options = cmatcher.group(1);
      addOutfitGroup(obuffer, "outfit2", "Custom", "a custom", options);
    }

    // Find option groups in the whichoutfit drop down
    Matcher omatcher = OPTGROUP_PATTERN.matcher(fmatcher.group());
    while (omatcher.find()) {
      String group = omatcher.group(1);
      String options = omatcher.group(2);
      if (group.equals("Normal Outfits")) {
        addOutfitGroup(obuffer, "outfit", "Outfits", "an", options);
      } else if (group.equals("Custom Outfits")) {
        addOutfitGroup(obuffer, "outfit2", "Custom", "a custom", options);
      } else if (group.equals("Automatic Outfits")) {
        addOutfitGroup(obuffer, "outfit3", "Automatic", "an automatic", options);
      }
    }

    obuffer.append("</table>");

    // Replace the original form with a table of forms
    buffer.replace(fmatcher.start(), fmatcher.end(), obuffer.toString());
  }

  private static void addOutfitGroup(
      final StringBuffer buffer,
      final String formName,
      final String label,
      final String type,
      final String options) {
    if (options.length() == 0) {
      return;
    }

    buffer.append("<tr><td align=right><form name=");
    buffer.append(formName);
    buffer.append(
        " action=inv_equip.php><input type=hidden name=action value=\"outfit\"><input type=hidden name=which value=2><b>");
    buffer.append(label);
    buffer.append(
        ":</b> </td><td><select style=\"width: 250px\" name=whichoutfit><option value=0>(select ");
    buffer.append(type);
    buffer.append(" outfit)</option>");
    buffer.append(options);
    buffer.append(
        "</select></td><td> <input class=button type=submit value=\"Dress Up!\"><br></form></td></tr>");
  }

  public static final void addChatFeatures(final StringBuffer buffer) {
    StringUtilities.insertBefore(
        buffer,
        "</html>",
        "<script language=\"Javascript\"> var notchat = true; var "
            + ChatPoller.getRightClickMenu()
            + " </script>"
            + "<script language=\"Javascript\" src=\"/images/scripts/rcm.20101215.js\"></script>");

    StringUtilities.insertBefore(buffer, "</body>", "<div id='menu' class='rcm'></div>");
  }

  private static void addFightModifiers(final StringBuffer buffer) {
    // Change bang potion names in item dropdown
    RequestEditorKit.changePotionNames(buffer);

    // Hilight He-Boulder eye color messages
    if (KoLCharacter.getFamiliar().getId() == FamiliarPool.HE_BOULDER) {
      StringUtilities.globalStringReplace(buffer, "s red eye", "s <font color=red>red eye</font>");
      StringUtilities.globalStringReplace(buffer, " blue eye", " <font color=blue>blue eye</font>");
      StringUtilities.globalStringReplace(
          buffer, " yellow eye", " <font color=olive>yellow eye</font>");
    }

    RequestEditorKit.insertRoundNumbers(buffer);

    if (Preferences.getBoolean("macroLens")) {
      String test = "<input type=\"hidden\" name=\"macrotext\" value=\"\">";
      if (buffer.indexOf(test) == -1) {
        String test2 = "<form name=runaway action=fight.php method=post>";
        int index = buffer.indexOf(test2);
        if (index != -1) {
          buffer.insert(
              index,
              "<form name=macro action=fight.php method=post><input type=hidden name=action value=\"macro\"><input type=\"hidden\" name=\"macrotext\" value=\"\"><tr><td align=center><select name=whichmacro><option value='0'>(select a macro)</option></select> <input class=button type=submit onclick=\"return killforms(this);\" value=\"Execute Macro\"></td></tr></form>");
        }
      }
      StringUtilities.singleStringReplace(
          buffer,
          test,
          "<tr><td><textarea name=\"macrotext\" cols=25 rows=10 placeholder=\"type macro here\"></textarea><script language=JavaScript src=\"/"
              + KoLConstants.MACROHELPER_JS
              + "\"></script></td></tr>");
    }

    if (buffer.indexOf("but not before you grab one of its teeth") != -1) {
      StringUtilities.singleStringReplace(
          buffer, "necklace", "<a href=\"javascript:void(item('222160625'))\">necklace</a>");
    }

    int runaway = FightRequest.freeRunawayChance();
    if (runaway > 0) {
      int pos = buffer.indexOf("type=submit value=\"Run Away\"");
      if (pos != -1) {
        buffer.insert(pos + 27, " (" + runaway + "% chance of being free)");
      }
    }

    // Add monster data HP/Atk/Def and item drop data
    RequestEditorKit.annotateMonster(buffer);

    // You slap a flyer up on your opponent.  It enrages
    // it.</td></tr>

    int flyerIndex = buffer.indexOf("You slap a flyer up on your opponent");
    if (flyerIndex != -1) {
      String message = "<tr><td colspan=2>" + RequestEditorKit.advertisingMessage() + "</td></tr>";
      flyerIndex = buffer.indexOf("</tr>", flyerIndex);
      buffer.insert(flyerIndex + 5, message);
    }

    // You are slowed too much by the water, and a stupid dolphin
    // swims up and snags <b>a seaweed</b> before you can grab
    // it.<p>

    int dolphinIndex = buffer.indexOf("a stupid dolphin swims up and snags");
    if (dolphinIndex != -1) {
      // If we have a dolphin whistle in inventory, offer a link to use it.
      if (InventoryManager.hasItem(ItemPool.DOLPHIN_WHISTLE)) {
        String message =
            "<br><font size=1>[<a href=\"inv_use.php?pwd="
                + GenericRequest.passwordHash
                + "&which=3&whichitem=3997\">use dolphin whistle</a>]</font><br>";
        dolphinIndex = buffer.indexOf("<p>", dolphinIndex);
        buffer.replace(dolphinIndex, dolphinIndex + 3, message);
      }
    }

    Matcher matcher = DwarfFactoryRequest.attackMessage(buffer);
    if (matcher != null) {
      int attack = DwarfFactoryRequest.deduceAttack(matcher);
      buffer.insert(matcher.end(), "<p>(Attack rating = " + attack + ")</p>");
    }

    matcher = DwarfFactoryRequest.defenseMessage(buffer);
    if (matcher != null) {
      int defense = DwarfFactoryRequest.deduceDefense(matcher);
      buffer.insert(matcher.end(), "<p>(Defense rating = " + defense + ")</p>");
    }

    matcher = DwarfFactoryRequest.hpMessage(buffer);
    if (matcher != null) {
      // Must iterate over a copy of the buffer, since we'll be modifying it
      matcher = DwarfFactoryRequest.hpMessage(buffer.toString());
      buffer.setLength(0);
      do {
        int hp = DwarfFactoryRequest.deduceHP(matcher);
        matcher.appendReplacement(buffer, "$0<p>(Hit Points = " + hp + ")</p>");
      } while (matcher.find());
      matcher.appendTail(buffer);
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    // We want to decorate battlefield monsters, whether or not you
    // actually find them on the battlefield.
    if (IslandManager.isBattlefieldMonster(monsterName)) {
      IslandDecorator.decorateBattlefieldFight(buffer);
    }

    // Copied ravers still need to be decorated
    if (NemesisDecorator.isRaver(monsterName)) {
      NemesisDecorator.decorateRaverFight(buffer);
    }

    if (monsterName.contains("gremlin")) {
      IslandDecorator.decorateGremlinFight(monster, buffer);
    }

    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.THEMTHAR_HILLS:
        IslandDecorator.decorateThemtharFight(buffer);
        break;

      case AdventurePool.SEASIDE_MEGALOPOLIS:
        MemoriesDecorator.decorateMegalopolisFight(buffer);
        break;
    }
  }

  private static void addFambattleModifiers(final StringBuffer buffer) {
    RequestEditorKit.insertRoundNumbers(buffer);

    // You are slowed too much by the water, and a stupid dolphin
    // swims up and snags <b>a seaweed</b> before you can grab
    // it.<p>

    int dolphinIndex = buffer.indexOf("a stupid dolphin swims up and snags");
    if (dolphinIndex != -1) {
      // If we have a dolphin whistle in inventory, offer a link to use it.
      if (InventoryManager.hasItem(ItemPool.DOLPHIN_WHISTLE)) {
        String message =
            "<br><font size=1>[<a href=\"inv_use.php?pwd="
                + GenericRequest.passwordHash
                + "&which=3&whichitem=3997\">use dolphin whistle</a>]</font><br>";
        dolphinIndex = buffer.indexOf("<p>", dolphinIndex);
        buffer.replace(dolphinIndex, dolphinIndex + 3, message);
      }
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    // We want to decorate battlefield monsters, whether or not you
    // actually find them on the battlefield.
    if (IslandManager.isBattlefieldMonster(monsterName)) {
      IslandDecorator.decorateBattlefieldFight(buffer);
    }

    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.THEMTHAR_HILLS:
        IslandDecorator.decorateThemtharFight(buffer);
        break;
    }
  }

  public static final String advertisingMessage() {
    int ML = Preferences.getInteger("flyeredML");
    float percent = Math.min(100.0f * (float) ML / 10000.0f, 100.0f);
    return "You have completed "
        + KoLConstants.FLOAT_FORMAT.format(percent)
        + "% of the necessary advertising.";
  }

  private static void annotateMonster(final StringBuffer buffer) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();

    if (monster == null) {
      return;
    }

    // Don't show monster unless we know combat stats or items
    // or monster element
    if (monster.getHP() == 0
        && monster.getItems().isEmpty()
        && monster.getDefenseElement() == MonsterDatabase.Element.NONE) {
      return;
    }

    // KoL has some buggy situations (an army of toddlers killing
    // the monster, for example) where there is no monster image.
    // Don't bother annotating in such cases
    int nameIndex = buffer.indexOf("<span id='monname");
    if (nameIndex == -1) {
      return;
    }

    int combatIndex = buffer.indexOf("</span>", nameIndex);
    if (combatIndex == -1) {
      return;
    }
    int insertionPointForData = combatIndex + 7;

    StringBuffer monsterData = new StringBuffer("<font size=2 color=gray>");
    monsterData.append("<br />HP: ");
    monsterData.append(MonsterStatusTracker.getMonsterHealth());
    monsterData.append(", Atk: ");
    monsterData.append(MonsterStatusTracker.getMonsterAttack());
    monsterData.append(", Def: ");
    monsterData.append(MonsterStatusTracker.getMonsterDefense());
    monsterData.append(", Type: ");
    monsterData.append(MonsterStatusTracker.getMonsterPhylum().toString());

    String monsterName = monster.getName();

    if (FightRequest.isPirate(monster)) {
      int count = BeerPongRequest.countPirateInsults();
      monsterData.append(", Insults: ");
      monsterData.append(count);
      monsterData.append(" (");
      float odds = BeerPongRequest.pirateInsultOdds(count) * 100.0f;
      monsterData.append(KoLConstants.FLOAT_FORMAT.format(odds));
      monsterData.append("%)");
    } else if (monsterName.equals("black pudding")) {
      int count = Preferences.getInteger("blackPuddingsDefeated");
      monsterData.append(", Defeated: ");
      monsterData.append(count);
    } else if (monsterName.equals("wall of skin")) {
      RequestEditorKit.selectOption(buffer, "whichitem", String.valueOf(ItemPool.BEEHIVE));
    } else if (monsterName.equals("wall of bones")) {
      RequestEditorKit.selectOption(
          buffer, "whichitem", String.valueOf(ItemPool.ELECTRIC_BONING_KNIFE));
    }

    String danceMoveStatus = NemesisDecorator.danceMoveStatus(monsterName);
    if (danceMoveStatus != null) {
      monsterData.append("<br />");
      monsterData.append(danceMoveStatus);
    }

    List<AdventureResult> items = monster.getItems();
    if (!items.isEmpty()) {
      monsterData.append("<br />Drops: ");
      for (int i = 0; i < items.size(); ++i) {
        if (i != 0) {
          monsterData.append(", ");
        }
        AdventureResult item = items.get(i);
        int rate = item.getCount() >> 16;
        monsterData.append(item.getName());
        switch ((char) item.getCount() & 0xFFFF) {
          case 'p':
            monsterData.append(" (");
            monsterData.append(rate);
            monsterData.append(" pp only)");
            break;
          case 'n':
            monsterData.append(" (");
            monsterData.append(rate);
            monsterData.append(" no pp)");
            break;
          case 'c':
            monsterData.append(" (");
            monsterData.append(rate);
            monsterData.append(" cond)");
            break;
          case 'f':
            monsterData.append(" (");
            monsterData.append(rate);
            monsterData.append(" no mod)");
            break;
          case 'a':
            monsterData.append(" (stealable accordion)");
            break;
          default:
            monsterData.append(" (");
            monsterData.append(rate);
            monsterData.append(")");
        }
      }
    }

    String bounty = BountyDatabase.getNameByMonster(monsterName);
    if (bounty != null) {
      monsterData.append(items.isEmpty() ? "<br />Drops: " : ", ");
      monsterData.append(bounty);
      monsterData.append(" (bounty)");
    }

    int minMeat = monster.getMinMeat();
    int maxMeat = monster.getMaxMeat();
    if (maxMeat > 0) {
      double modifier =
          Math.max(0.0, (KoLCharacter.getMeatDropPercentAdjustment() + 100.0) / 100.0);
      monsterData.append("<br />Meat: ");
      monsterData.append((int) Math.floor(minMeat * modifier));
      monsterData.append("-");
      monsterData.append((int) Math.floor(maxMeat * modifier));
    }

    int minSprinkles = monster.getMinSprinkles();
    int maxSprinkles = monster.getMaxSprinkles();
    if (maxSprinkles > 0) {
      double modifier =
          Math.max(0.0, (KoLCharacter.getSprinkleDropPercentAdjustment() + 100.0) / 100.0);
      monsterData.append("<br />Sprinkles: ");
      monsterData.append((int) Math.floor(minSprinkles * modifier));
      if (maxSprinkles != minSprinkles) {
        monsterData.append("-");
        monsterData.append((int) Math.ceil(maxSprinkles * modifier));
      }
    }

    IslandDecorator.appendMissingGremlinTool(monster, monsterData);

    if (KoLCharacter.getLimitmode() == Limitmode.SPELUNKY) {
      SpelunkyRequest.decorateSpelunkyMonster(monsterData);
    }

    monsterData.append("</font>");
    buffer.insert(insertionPointForData, monsterData.toString());

    // Insert color for monster element
    MonsterDatabase.Element monsterElement = monster.getDefenseElement();
    if (monsterElement != MonsterDatabase.Element.NONE) {
      int insertionPointForElement = nameIndex + 6;
      buffer.insert(insertionPointForElement, "class=\"element" + monsterElement + "\" ");
    }
  }

  private static void addMultiuseModifiers(final StringBuffer buffer) {
    // Change bang potion names in item dropdown
    RequestEditorKit.changePotionNames(buffer);
  }

  private static void changePotionImages(final StringBuffer buffer) {
    if (buffer.indexOf("exclam.gif") == -1 && buffer.indexOf("vial.gif") == -1) {
      return;
    }
    if (!Preferences.getBoolean("relayShowSpoilers")) {
      return;
    }

    ArrayList<String> potionNames = new ArrayList<String>();
    ArrayList<String> pluralNames = new ArrayList<String>();
    ArrayList<String> potionEffects = new ArrayList<String>();

    for (int i = 819; i <= 827; ++i) {
      String name = ItemDatabase.getItemName(i);
      String plural = ItemDatabase.getPluralName(i);
      if (buffer.indexOf(name) != -1 || buffer.indexOf(plural) != -1) {
        String effect = Preferences.getString("lastBangPotion" + i);
        if (!effect.equals("")) {
          potionNames.add(name);
          pluralNames.add(plural);
          potionEffects.add(" of " + effect);
        }
      }
    }
    for (int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i) {
      String name = ItemDatabase.getItemName(i);
      String plural = ItemDatabase.getPluralName(i);
      if (buffer.indexOf(name) != -1 || buffer.indexOf(plural) != -1) {
        String effect = Preferences.getString("lastSlimeVial" + i);
        if (!effect.equals("")) {
          potionNames.add(name);
          pluralNames.add(plural);
          potionEffects.add(": " + effect);
        }
      }
    }

    if (potionNames.isEmpty()) {
      return;
    }

    for (int i = 0; i < potionNames.size(); ++i) {
      String name = potionNames.get(i);
      String plural = pluralNames.get(i);
      String effect = potionEffects.get(i);

      StringUtilities.globalStringReplace(buffer, name + "</b>", name + effect + "</b>");
      StringUtilities.globalStringReplace(buffer, plural + "</b>", plural + effect + "</b>");
    }
  }

  private static void changePotionNames(final StringBuffer buffer) {
    for (int i = 819; i <= 827; ++i) {
      String name = ItemDatabase.getItemName(i);
      String plural = ItemDatabase.getPluralName(i);
      if (buffer.indexOf(name) != -1 || buffer.indexOf(plural) != -1) {
        String effect = Preferences.getString("lastBangPotion" + i);
        if (effect.equals("")) {
          continue;
        }

        StringUtilities.globalStringReplace(buffer, name, name + " of " + effect);
        StringUtilities.globalStringReplace(buffer, plural, plural + " of " + effect);
      }
    }
    for (int i = ItemPool.VIAL_OF_RED_SLIME; i <= ItemPool.VIAL_OF_PURPLE_SLIME; ++i) {
      String name = ItemDatabase.getItemName(i);
      String plural = ItemDatabase.getPluralName(i);
      if (buffer.indexOf(name) != -1 || buffer.indexOf(plural) != -1) {
        String effect = Preferences.getString("lastSlimeVial" + i);
        if (effect.equals("")) {
          continue;
        }

        StringUtilities.globalStringReplace(buffer, name, name + ": " + effect);
        StringUtilities.globalStringReplace(buffer, plural, plural + ": " + effect);
      }
    }
  }

  private static void changePunchcardNames(final StringBuffer buffer) {
    if (buffer.indexOf("El Vibrato punchcard") == -1) {
      return;
    }

    for (Object[] punchcard : ItemDatabase.PUNCHCARDS) {
      String name = (String) punchcard[1];
      if (buffer.indexOf(name) != -1) {
        StringUtilities.globalStringReplace(buffer, name, (String) punchcard[2]);
      }
    }
  }

  private static void fixTavernCellar(final StringBuffer buffer) {
    // When you adventure in the Typical Tavern Cellar, the
    // Adventure Again link takes you to the map. Fix that link
    // as follows:
    //
    // (new) Explore Next Unexplored Square
    // Go back to the Typical Tavern Cellar

    int index = buffer.indexOf("<a href=\"cellar.php\">");
    if (index == -1) {
      return;
    }

    int unexplored = TavernManager.nextUnexploredSquare();
    if (unexplored <= 0) {
      return;
    }

    String link =
        "<a href=\"cellar.php?action=explore&whichspot="
            + unexplored
            + "\">Explore Next Unexplored Square</a><p>";
    buffer.insert(index, link);
  }

  private static void addTaleOfDread(final StringBuffer buffer) {
    // You hear the scratching sounds of a quill pen from inside
    // your sack. A new Tale of Dread has written itself in your
    // scary storybook!

    int index = buffer.indexOf("A new Tale of Dread");
    if (index == -1) {
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName =
        (monster != null)
            ? StringUtilities.singleStringReplace(monster.getName(), " (Dreadsylvanian)", "")
            : "";
    String find = "your scary storybook!";

    String replace =
        find
            + " <font size=1>[<a href=\""
            + "/KoLmafia/redirectedCommand?cmd=taleofdread "
            + monsterName
            + " redirect"
            + "&pwd="
            + GenericRequest.passwordHash
            + "\">read it</a>]</font>";
    StringUtilities.singleStringReplace(buffer, find, replace);
  }

  private static final Pattern DESERT_EXPLORATION_PATTERN =
      Pattern.compile("Desert exploration <b>\\+\\d+%</b>");

  private static void addDesertProgress(final StringBuffer buffer) {
    String lastAdventure = Preferences.getString("lastAdventure");
    if (!lastAdventure.equals("The Arid, Extra-Dry Desert") || buffer.indexOf("WINWINWIN") == -1) {
      return;
    }

    Matcher m = RequestEditorKit.DESERT_EXPLORATION_PATTERN.matcher(buffer);
    if (!m.find()) {
      return;
    }

    String progress = " (" + Preferences.getInteger("desertExploration") + "% explored)";
    buffer.insert(m.end(), progress);
  }

  private static final Pattern FOREST_EXPLORATION_PATTERN =
      Pattern.compile(
          "(location on your black map\\.|Halloween falls on a Sunday, maybe\\.|realize why that would have been a bad idea\\.)");

  private static void addBlackForestProgress(final StringBuffer buffer) {
    String lastAdventure = Preferences.getString("lastAdventure");
    if (!lastAdventure.equals("The Black Forest") || buffer.indexOf("WINWINWIN") == -1) {
      return;
    }

    Matcher m = RequestEditorKit.FOREST_EXPLORATION_PATTERN.matcher(buffer);
    if (!m.find()) {
      return;
    }

    String progress =
        " (" + Preferences.getInteger("blackForestProgress") + "/5 Landmarks marked on your map)";
    buffer.insert(m.end(), progress);
  }

  private static final Pattern WOOTS_PATTERN =
      Pattern.compile("(entire room erupts into cheers\\.|It works\\.  Sort of\\.)");
  private static final Pattern TRASH_PATTERN =
      Pattern.compile(
          "(greasy paper plates|wadded up napkins|discarded bottle caps|empty beer cans|dirty plastic cups|empty beer bottles)");
  private static final Pattern MEAT_PATTERN = Pattern.compile("Meat for the DJ.");
  private static final Pattern PARTIERS_PATTERN = Pattern.compile("You win the fight!");

  private static void addPartyFairProgress(final StringBuffer buffer) {
    String lastAdventure = Preferences.getString("lastAdventure");
    if (!lastAdventure.equals("The Neverending Party") || buffer.indexOf("WINWINWIN") == -1) {
      return;
    }

    String partyQuest = Preferences.getString("_questPartyFairQuest");

    if (partyQuest.equals("woots")) {
      Matcher m = RequestEditorKit.WOOTS_PATTERN.matcher(buffer);
      if (m.find()) {
        String progress =
            " (" + Preferences.getString("_questPartyFairProgress") + "/100 megawoots)";
        buffer.insert(m.end(), progress);
        return;
      }
    } else if (partyQuest.equals("trash")) {
      Matcher m = RequestEditorKit.TRASH_PATTERN.matcher(buffer);
      if (m.find()) {
        String progress =
            " (~"
                + Preferences.getString("_questPartyFairProgress")
                + " pieces of trash remaining)";
        buffer.insert(m.end(), progress);
        return;
      }
    } else if (partyQuest.equals("dj")) {
      Matcher m = RequestEditorKit.MEAT_PATTERN.matcher(buffer);
      if (m.find()) {
        String progress =
            " (" + Preferences.getString("_questPartyFairProgress") + " Meat remaining)";
        buffer.insert(m.end(), progress);
        return;
      }
    }
    // No special text, just append to You win the fight if on clear the party quest
    else if (partyQuest.equals("partiers")) {
      Matcher m = RequestEditorKit.PARTIERS_PATTERN.matcher(buffer);
      if (m.find()) {
        String progress =
            " (" + Preferences.getString("_questPartyFairProgress") + " Partiers remaining)";
        buffer.insert(m.end(), progress);
        return;
      }
    }
  }

  private static void insertRoundNumbers(final StringBuffer buffer) {
    Matcher m = FightRequest.ONTURN_PATTERN.matcher(buffer);
    if (!m.find()) {
      return;
    }
    int round = StringUtilities.parseInt(m.group(1));
    m = RequestEditorKit.ROUND_SEP_PATTERN.matcher(buffer.toString());
    buffer.setLength(0);
    while (m.find()) {
      if (m.group().startsWith("<b")) { // Initial round - add # after "Combat"
        m.appendReplacement(buffer, "<b>Combat: Round ");
        buffer.append(round++);
        if (KoLCharacter.isEd()) {
          int edfight = Preferences.getInteger("_edDefeats");
          if (FightRequest.currentRound != 0) {
            edfight++;
          }
          buffer.append(", Fight ").append(edfight);
        }
        buffer.append("!</b>");
      } else { // Subsequent rounds - replace <hr> with bar like title
        m.appendReplacement(
            buffer,
            "<table width=100%><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Round ");
        buffer.append(round++);
        buffer.append("!</b></td></tr></table>");
      }
    }
    m.appendTail(buffer);
  }

  private static void addChoiceSpoilers(final String location, final StringBuffer buffer) {
    if (!Preferences.getBoolean("relayShowSpoilers")) {
      return;
    }

    // Make sure that it's an actual choice adventure
    int choice = ChoiceManager.extractChoice(buffer.toString());

    if (choice == 0) {
      // It's a response to taking a choice.
      RequestEditorKit.decorateChoiceResponse(location, buffer);
      return;
    }

    // Do any choice-specific decorations
    ChoiceManager.decorateChoice(choice, buffer);

    String text = buffer.toString();
    Matcher matcher = FORM_PATTERN.matcher(text);
    if (!matcher.find()) {
      return;
    }

    // Find the options for the choice we've encountered
    Object[][] spoilers = ChoiceManager.choiceSpoilers(choice, buffer);

    // Some choices we don't mark up with spoilers
    if (ChoiceManager.noRelayChoice(choice)) {
      spoilers = null;
    }

    if (spoilers == null) { // Don't give up - there may be a specified choice even if there
      // are no spoilers.
      spoilers = new Object[][] {null, null, {}};
    }

    int index1 = matcher.start();
    int decision = ChoiceManager.getDecision(choice, text);

    buffer.setLength(0);
    buffer.append(text, 0, index1);

    while (true) {
      int index2 = text.indexOf("</form>", index1);

      // If KoL says we've run out of choices, quit now
      if (index2 == -1) {
        break;
      }

      String currentSection = text.substring(index1, index2);
      Matcher optionMatcher = RequestEditorKit.OPTION_PATTERN.matcher(currentSection);
      if (!optionMatcher.find()) { // this wasn't actually a choice option - strange!
        buffer.append(currentSection);
        buffer.append("</form>");
        index1 = index2 + 7;
        continue;
      }

      int i = StringUtilities.parseInt(optionMatcher.group(1));
      if (i != decision) {
        buffer.append(currentSection);
      } else {
        int pos = currentSection.lastIndexOf("value=\"");
        buffer.append(currentSection, 0, pos + 7);
        buffer.append("&rarr; ");
        buffer.append(currentSection.substring(pos + 7));
      }

      // Build spoiler text
      while (i > 0) {
        // Say what the choice will give you
        Object spoiler = ChoiceManager.choiceSpoiler(choice, i, spoilers[2]);

        // If we have nothing to say about this option, don't say anything
        if (spoiler == null) {
          break;
        }

        StringBuilder spoilerBuffer = new StringBuilder(spoiler.toString());

        // If this decision has an item associated with it, annotate it
        if (spoiler instanceof ChoiceManager.Option) {
          ChoiceManager.Option option = ((ChoiceManager.Option) spoiler);
          AdventureResult[] items = option.getItems();

          // If this decision leads to one or more item...
          for (int it = 0; it < items.length; it++) {
            AdventureResult item = items[it];
            if (item != null) {
              if (it > 0) {
                spoilerBuffer.append(" or ");
                spoilerBuffer.append(item.getName());
              }

              // List # in inventory
              spoilerBuffer.append(
                  "<img src=\"/images/itemimages/magnify.gif\" valign=middle onclick=\"descitem('");
              spoilerBuffer.append(ItemDatabase.getDescriptionId(item.getItemId()));
              spoilerBuffer.append("');\">");

              int available = KoLCharacter.hasEquipped(item) ? 1 : 0;
              available += item.getCount(KoLConstants.inventory);

              spoilerBuffer.append(available);
              spoilerBuffer.append(" in inventory");
            }
          }
        }

        if (spoilerBuffer.length() != 0) {
          // Add spoiler text
          buffer.append("<br><font size=-1>(");
          buffer.append(spoilerBuffer);
          buffer.append(")</font>");
        }

        break;
      }
      buffer.append("</form>");
      index1 = index2 + 7;
    }

    buffer.append(text.substring(index1));
  }

  private static void addBarrelSounds(final StringBuffer buffer) {
    if (!Preferences.getBoolean("relayAddSounds")) {
      return;
    }

    if (buffer.indexOf("barrelpart") != -1) {
      StringUtilities.insertBefore(
          buffer, "</html>", "<script src=\"/" + KoLConstants.BARREL_SOUNDS_JS + "\"></script>");
    }
  }

  private static void decorateChoiceResponse(final String location, final StringBuffer buffer) {
    int choice = ChoiceManager.extractChoiceFromURL(location);
    if (choice == 0) {
      return;
    }
    int option = ChoiceManager.extractOptionFromURL(location);

    switch (choice) {
        // The Oracle Will See You Now
      case 3:
        StringUtilities.singleStringReplace(
            buffer,
            "It's actually a book.  Read it.",
            "It's actually a book. <font size=1>[<a href=\"inv_use.php?pwd="
                + GenericRequest.passwordHash
                + "&which=3&whichitem=818\">read it</a>]</font>");
        break;

      case 392:
        MemoriesDecorator.decorateElementsResponse(buffer);
        break;
      case 443:
        // Chess Puzzle
        RabbitHoleManager.decorateChessPuzzleResponse(buffer);
        break;

      case 509: // Of Course!
      case 1000: // Everything in Moderation
        // You should probably go tell Bart you've fixed his rat problem.
        // You should probably go tell Bart you've fixed his lack-of-rat problem.
        StringUtilities.singleStringReplace(
            buffer,
            "rat problem.",
            "rat problem. <font size=1>[<a href=\"tavern.php?place=barkeep\">Visit Bart</a>]</font>");
        break;
      case 537:
        // Play Porko!
      case 540:
        // Big-Time Generator
        SpaaaceRequest.decoratePorko(buffer);
        break;

      case 571:
        // Your Minstrel Vamps
        RequestEditorKit.addMinstrelNavigationLink(
            buffer, "Go to the Typical Tavern", "tavern.php");
        break;

      case 572:
        // Your Minstrel Clamps
        RequestEditorKit.addMinstrelNavigationLink(
            buffer, "Go to the Knob Shaft", "adventure.php?snarfblat=101");
        break;

      case 573:
        // Your Minstrel Stamps
        // Add a link to the Luter's Grave
        RequestEditorKit.addMinstrelNavigationLink(
            buffer, "Go to the Luter's Grave", "place.php?whichplace=plains&action=lutersgrave");
        break;

      case 576:
        // Your Minstrel Camps
        RequestEditorKit.addMinstrelNavigationLink(
            buffer, "Go to the Icy Peak", "adventure.php?snarfblat=110");
        break;

      case 577:
        // Your Minstrel Scamp
        RequestEditorKit.addMinstrelNavigationLink(
            buffer, "Go to the Ancient Buried Pyramid", "pyramid.php");
        break;

      case 579:
        // Such Great Heights
        if (option == 3) {
          // xyzzy
          int index =
              buffer.indexOf(
                  "<p><a href=\"adventure.php?snarfblat=280\">Adventure Again (The Hidden Temple)</a>");
          if (index == -1) {
            break;
          }

          int itemId = ItemPool.STONE_WOOL;
          int count = ItemPool.get(itemId, 1).getCount(KoLConstants.inventory);
          if (count == 0) {
            break;
          }

          String name = "stone wool";
          String link =
              "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem="
                  + itemId
                  + "&pwd="
                  + GenericRequest.passwordHash
                  + "&ajax=1');void(0);\">Use another "
                  + name
                  + "</a>";
          buffer.insert(index, link);
        }
        break;

      case 611:
        {
          // The Horror...
          int index =
              buffer.indexOf(
                  "<p><a href=\"adventure.php?snarfblat=296\">Adventure Again (A-Boo Peak)</a>");
          if (index == -1) {
            break;
          }

          boolean glover = KoLCharacter.inGLover();
          int itemId = glover ? ItemPool.GLUED_BOO_CLUE : ItemPool.BOO_CLUE;
          int count = ItemPool.get(itemId, 1).getCount(KoLConstants.inventory);
          if (count == 0) {
            break;
          }

          String name = glover ? "glued A-Boo Clue" : "A-Boo Clue";
          String link =
              "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem="
                  + itemId
                  + "&pwd="
                  + GenericRequest.passwordHash
                  + "&ajax=1');void(0);\">Use another "
                  + name
                  + "</a>";
          buffer.insert(index, link);
          break;
        }

      case 1027: // The End of the Tale of Spelunking
      case 1042: // Pick a Perk
        SpelunkyRequest.decorateSpelunkyExit(buffer);
        break;

      case 1325: // A Room With a View...  Of a Bed
        StringUtilities.singleStringReplace(
            buffer,
            "hurry through the door to take your place.",
            "hurry through the door to take your place. ("
                + Preferences.getString("_questPartyFairProgress")
                + "/100 megawoots)");
        StringUtilities.singleStringReplace(
            buffer,
            "start complaining and then leave.",
            "start complaining and then leave. ("
                + Preferences.getString("_questPartyFairProgress")
                + " Partiers remaining)");
        StringUtilities.singleStringReplace(
            buffer,
            "contribute to the DJ's bill.",
            "contribute to the DJ's bill. ("
                + Preferences.getString("_questPartyFairProgress")
                + " Meat remaining)");
        break;

      case 1326: // Gone Kitchin'
        StringUtilities.singleStringReplace(
            buffer,
            "pieces of trash in that can!",
            "pieces of trash in that can! (~"
                + Preferences.getString("_questPartyFairProgress")
                + " pieces of trash remaining)");
        break;

      case 1327: // Forward to the Back
        StringUtilities.singleStringReplace(
            buffer,
            "flees over the back fence.",
            "flees over the back fence. ("
                + Preferences.getString("_questPartyFairProgress")
                + " Partiers remaining)");
        break;

      case 1328: // Basement Urges
        StringUtilities.singleStringReplace(
            buffer,
            "burns the house down.",
            "burns the house down. ("
                + Preferences.getString("_questPartyFairProgress")
                + "/100 megawoots)");
        break;
    }
  }

  private static void addMinstrelNavigationLink(
      final StringBuffer buffer, final String tag, final String url) {
    int index = buffer.lastIndexOf("<table>");
    if (index == -1) {
      return;
    }

    index = buffer.indexOf("<p>", index);
    if (index == -1) {
      return;
    }

    String link = "<p><a href=\"" + url + "\">" + tag + "</a>";
    buffer.insert(index, link);
  }

  private static void decorateCouncil(final StringBuffer buffer) {
    if (!KoLCharacter.inAxecore()) {
      return;
    }

    int index = buffer.lastIndexOf("<p>");
    if (index != -1) {
      buffer.insert(
          index + 3,
          "<center><a href=\"da.php?place=gate1\">Bask in the Glory of Boris</a></center><br>");
    }
  }

  private static void decorateCrypt(final StringBuffer buffer) {
    if (Preferences.getInteger("cyrptTotalEvilness") == 0) {
      return;
    }

    int nookEvil = Preferences.getInteger("cyrptNookEvilness");
    int nicheEvil = Preferences.getInteger("cyrptNicheEvilness");
    int crannyEvil = Preferences.getInteger("cyrptCrannyEvilness");
    int alcoveEvil = Preferences.getInteger("cyrptAlcoveEvilness");

    String nookColor = nookEvil > 25 ? "000000" : "FF0000";
    String nookHint = nookEvil > 25 ? "Item Drop" : "<b>BOSS</b>";
    String nicheColor = nicheEvil > 25 ? "000000" : "FF0000";
    String nicheHint = nicheEvil > 25 ? "Sniff Dirty Lihc" : "<b>BOSS</b>";
    String crannyColor = crannyEvil > 25 ? "000000" : "FF0000";
    String crannyHint = crannyEvil > 25 ? "ML & Noncombat" : "<b>BOSS</b>";
    String alcoveColor = alcoveEvil > 25 ? "000000" : "FF0000";
    String alcoveHint = alcoveEvil > 25 ? "Initiative" : "<b>BOSS</b>";

    StringBuilder evilometer = new StringBuilder();

    evilometer.append("<table cellpadding=0 cellspacing=0><tr><td colspan=3>");
    evilometer.append("<img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_top.gif\">");
    evilometer.append("<tr><td><img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_left.gif\">");
    evilometer.append("<td width=150><center>");

    if (nookEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(nookColor);
      evilometer.append("\"><b>Nook</b> - ");
      evilometer.append(nookEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(nookHint);
      evilometer.append("<br></font></font>");
    }

    if (nicheEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(nicheColor);
      evilometer.append("\"><b>Niche</b> - ");
      evilometer.append(nicheEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(nicheHint);
      evilometer.append("<br></font></font>");
    }

    if (crannyEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(crannyColor);
      evilometer.append("\"><b>Cranny</b> - ");
      evilometer.append(crannyEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(crannyHint);
      evilometer.append("<br></font></font>");
    }

    if (alcoveEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(alcoveColor);
      evilometer.append("\"><b>Alcove</b> - ");
      evilometer.append(alcoveEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(alcoveHint);
      evilometer.append("<br></font></font>");
    }

    evilometer.append("<td><img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_right.gif\"><tr><td colspan=3>");
    evilometer.append("<img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_bottom.gif\"></table>");

    String selector = "</map><table";
    int index = buffer.indexOf(selector);
    buffer.insert(index + selector.length(), "><tr><td><table");

    // <A href=place.php?whichplace=plains>Back to the Misspelled Cemetary</a>
    // I expect that will change to "whichplace=cemetery" eventually.
    index = buffer.indexOf("</tr></table><p><center><A href=place.php");
    evilometer.insert(0, "</table><td>");
    buffer.insert(index + 5, evilometer.toString());
  }

  private static void addBugReportWarning(final StringBuffer buffer) {
    // <div id="type_1">
    // <table><tr><td><b>IMPORTANT:</b> If you can see this notice,
    // the information you're about to submit may be seen by dev
    // team volunteers in addition to the staff of Asymmetric
    // Publications.<p>For the protection of your privacy, please
    // do not submit any passwords, personal data, or donation
    // information as a bug report! If you're having a donation or
    // store issue, please select the appropriate category
    // above.</td></tr></table>
    // <p><b>Please describe the bug:</b></p>
    // <textarea class="req" name=message cols=60 rows=10></textarea><br>
    // </div>

    int index = buffer.indexOf("<p><b>Please describe the bug:</b></p>");
    if (index == -1) {
      return;
    }

    String disclaimer =
        "<p><span style=\"width: 95%; border: 3px solid red; color: red; padding: 5px; text-align: left; margin-bottom: 10px; background-color: rgb(254, 226, 226); display:block;\">"
            + "You are currently running in the KoLmafia Relay Browser. It is possible that the bug you are experiencing is not in KoL itself, but is a result of KoLmafia, a Greasemonkey script, or another client-side modification. The KoL team requests that you verify that you can reproduce the bug in a vanilla browser with all add-ons and extensions disabled before submitting a bug report."
            + "</span>";
    buffer.insert(index, disclaimer);
  }

  private static void fixDucks(final StringBuffer buffer) {
    // KoL does not currently provide a link back to the farm after
    // you defeat the last duck.

    if (buffer.indexOf("ducks") == -1) {
      return;
    }

    // But if they fix it and it now adds one, cool.

    if (buffer.indexOf("island.php") != -1) {
      return;
    }

    String war = IslandManager.warProgress();
    String test;
    String url;

    if (war.equals("finished")) {
      // You wander around the farm for a while, but can't
      // find any additional ducks to fight. Maybe some more
      // will come out of hiding by tomorrow.
      test = "any additional ducks";
      url = "postwarisland.php";
    } else {
      // There are no more ducks here.
      test = "There are no more ducks here.";
      url = "bigisland.php?place=farm";
    }

    if (buffer.indexOf(test) == -1) {
      return;
    }

    RequestEditorKit.addAdventureAgainSection(
        buffer, url, "Go back to The Mysterious Island of Mystery");
  }

  public static final void addAdventureAgainSection(
      final StringBuffer buffer, final String link, final String tag) {
    int index = buffer.indexOf("</center></td></tr><tr><td height=4></td></tr></table>");
    if (index == -1) {
      return;
    }

    String section = "<center><p><a href=\"" + link + "\">" + tag + "</a></center>";
    buffer.insert(index, section);
  }

  private static void fixBallroom1(final StringBuffer buffer) {
    // Things that go BEFORE Stationary Buttons have been generated

    String link = null;

    if (buffer.indexOf("Having a Ball in the Ballroom") != -1) {
      // Give the player a link to talk to Lady Spookyraven again (on the third floor)
      //
      // Unfortunately, place.php?whichplace=manor3&action=manor3_ladys does not work until you
      // visit the third floor map
      // link = "<p><a href=\"place.php?whichplace=manor3&action=manor3_ladys\">Talk to Lady
      // Spookyraven on the Third Floor</a>";
      //
      // Unfortunately, place.php?whichplace=manor3 doesn't work until you visit the second floor
      // map again.
      // link = "<p><a href=\"place.php?whichplace=manor3\">Go to the Third Floor</a>";
      //
      // Which makes this whole function useless, unless we
      // can figure out a way - via a sidepane command, say -
      // to unlock the third floor and then talk to Lady S on
      // the third floor
    }

    if (link == null) {
      return;
    }

    int index = buffer.indexOf("<p><a href=\"adventure.php?snarfblat=395\">");
    if (index != -1) {
      buffer.insert(index, link);
    }
  }

  private static final AdventureResult DANCE_CARD = ItemPool.get(ItemPool.DANCE_CARD, 1);

  private static void fixBallroom2(final StringBuffer buffer) {
    // Things that go AFTER Stationary Buttons have been generated

    String link = null;

    if (buffer.indexOf("Rotting Matilda") != -1) {
      // Give player a link to use another dance card
      if (DANCE_CARD.getCount(KoLConstants.inventory) <= 0) {
        return;
      }
      link =
          "<p><a href=\"javascript:singleUse('inv_use.php','which=3&whichitem=1963&pwd="
              + GenericRequest.passwordHash
              + "&ajax=1');void(0);\">Use another dance card</a>";
    }

    if (link == null) {
      return;
    }

    int index = buffer.indexOf("<p><a href=\"adventure.php?snarfblat=395\">");
    if (index != -1) {
      buffer.insert(index, link);
    }
  }

  private static void fixGovernmentLab(final StringBuffer buffer) {
    // Things that go AFTER Stationary Buttons have been generated

    String link = null;

    String test = "without wearing a Personal Ventilation Unit.";
    int index = buffer.indexOf(test);

    if (index == -1) {
      return;
    }

    // Give player a link to equip the PVU
    link =
        " <a href=\"javascript:singleUse('inv_equip.php',which=2&action=equip&slot=1&whichitem=7770&pwd="
            + GenericRequest.passwordHash
            + "');void();\">[acc1]</a>"
            + "<a href=\"javascript:singleUse('inv_equip.php',which=2&action=equip&slot=2&whichitem=7770&pwd="
            + GenericRequest.passwordHash
            + "');void();\">[acc2]</a>"
            + "<a href=\"javascript:singleUse('inv_equip.php',which=2&action=equip&slot=3&whichitem=7770&pwd="
            + GenericRequest.passwordHash
            + "');void();\">[acc3]</a>";
    UseLink link1 =
        new UseLink(
            ItemPool.VENTILATION_UNIT,
            1,
            UseLinkDecorator.getEquipmentSpeculation(
                "acc1", ItemPool.VENTILATION_UNIT, EquipmentManager.ACCESSORY1),
            "inv_equip.php?which=2&action=equip&slot=1&whichitem=");
    UseLink link2 =
        new UseLink(
            ItemPool.VENTILATION_UNIT,
            1,
            UseLinkDecorator.getEquipmentSpeculation(
                "acc2", ItemPool.VENTILATION_UNIT, EquipmentManager.ACCESSORY2),
            "inv_equip.php?which=2&action=equip&slot=2&whichitem=");
    UseLink link3 =
        new UseLink(
            ItemPool.VENTILATION_UNIT,
            1,
            UseLinkDecorator.getEquipmentSpeculation(
                "acc3", ItemPool.VENTILATION_UNIT, EquipmentManager.ACCESSORY3),
            "inv_equip.php?which=2&action=equip&slot=3&whichitem=");
    buffer.insert(
        index + test.length(), link1.getItemHTML() + link2.getItemHTML() + link3.getItemHTML());
  }

  private static class KoLSubmitView extends FormView {
    public KoLSubmitView(final Element elem) {
      super(elem);
    }

    @Override
    public Component createComponent() {
      Component c = super.createComponent();

      if ((c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox)) {
        c.setBackground(Color.white);
      }

      return c;
    }

    @Override
    public void submitData(final String data) {
      // Get the element

      Element inputElement = this.getElement();

      if (inputElement == null) {
        return;
      }

      // Retrieve the frame which is being used by this form
      // viewer.

      RequestFrame frame = this.findFrame();

      // If there is no frame, then there's nothing to
      // refresh, so return.

      if (frame == null) {
        return;
      }

      // Retrieve the form element so that you know where you
      // need to submit the data.

      Element formElement = inputElement;

      while (formElement != null
          && formElement.getAttributes().getAttribute(StyleConstants.NameAttribute)
              != HTML.Tag.FORM) {
        formElement = formElement.getParentElement();
      }

      // If the form element is null, then there was no
      // enclosing form for the <INPUT> tag, so you can
      // return, doing nothing.

      if (formElement == null) {
        return;
      }

      // Now that you know you have a form element,
      // get the action field, attach the data, and
      // refresh the appropriate request frame.

      String action = (String) formElement.getAttributes().getAttribute(HTML.Attribute.ACTION);

      // If there is no action, how do we know which page to
      // connect to?  We assume it's the originating page.

      if (action == null) {
        action = frame.getCurrentLocation();
      }

      // Now get the data fields we will submit to this form

      String[] elements = data.split("&");
      String[] fields = new String[elements.length];

      if (elements[0].length() > 0) {
        for (int i = 0; i < elements.length; ++i) {
          fields[i] = elements[i].substring(0, elements[i].indexOf("="));
        }
      } else {
        fields[0] = "";
      }

      // Prepare the element string -- make sure that
      // you don't have duplicate fields.

      for (int i = 0; i < elements.length; ++i) {
        for (int j = i + 1; j < elements.length; ++j) {
          if (elements[i] != null && elements[j] != null && fields[i].equals(fields[j])) {
            elements[j] = null;
          }
        }
      }

      GenericRequest formSubmitter = new GenericRequest("");

      if (action.contains("?")) {
        // For quirky URLs where there's a question mark
        // in the middle of the URL, just string the data
        // onto the URL.  This is the way browsers work,
        // so it's the way KoL expects the data.

        StringBuilder actionString = new StringBuilder();
        actionString.append(action);

        for (String element : elements) {
          if (element != null) {
            actionString.append('&');
            actionString.append(element);
          }
        }

        formSubmitter.constructURLString(
            GenericRequest.decodeField(actionString.toString(), "ISO-8859-1"));
      } else {
        // For normal URLs, the form data can be submitted
        // just like in every other request.

        formSubmitter.constructURLString(action);
        if (elements[0].length() > 0) {
          for (String element : elements) {
            if (element != null) {
              formSubmitter.addEncodedFormField(element);
            }
          }
        }
      }

      frame.refresh(formSubmitter);
    }

    private RequestFrame findFrame() {
      // Goal: find the RequestFrame that contains the RequestPane that
      // contains the HTML field containing this form submit button.
      // Original solution: enumerate all Frames, choose the one containing
      // text that matches the button name.  This broke in the presence
      // of HTML entities, and wasn't guaranteed to be unique anyway.
      // Try 1: enumerate enclosing containers until an instance of
      // RequestFrame is found.  This works for standalone windows, but
      // not for frames that open in a tab because they aren't actually
      // part of the containment hierarchy in that case - the contentPane
      // of the frame gets reparented into the main tabs.
      // Try 2: enumerate containers to find the RequestPane, enumerate
      // Frames to find the RequestFrame that owns it.

      Container c = this.getContainer();
      while (c != null && !(c instanceof RequestPane)) {
        c = c.getParent();
      }

      Frame[] frames = Frame.getFrames();
      for (Frame frame : frames) {
        if (frame instanceof RequestFrame && ((RequestFrame) frame).mainDisplay == c) {
          return (RequestFrame) frame;
        }
      }
      return null;
    }
  }

  /**
   * Utility method used to determine the GenericRequest that should be sent, given the appropriate
   * location.
   */
  public static final GenericRequest extractRequest(String location) {
    if (location.contains("pics.communityofloathing.com")) {
      FileUtilities.downloadImage(location);
      location = location.substring(location.indexOf("/"));

      GenericRequest extractedRequest = new GenericRequest(location);
      extractedRequest.responseCode = 200;
      extractedRequest.responseText = "<html><img src=\"" + location + "\"></html>";
      return extractedRequest;
    }

    return new GenericRequest(location);
  }

  /** Utility method used to deselect current selection and select a new one */
  public static final void selectOption(
      final StringBuffer buffer, final String select, final String option) {
    // Find the correct select within the html
    int start = buffer.indexOf("<select name=" + select + ">");
    if (start < 0) {
      return;
    }
    int end = buffer.indexOf("</select>", start);
    if (end < 0) {
      return;
    }

    // Delete currently selected items
    int index = start;
    while (true) {
      index = buffer.indexOf(" selected", index);
      if (index == -1 || index >= end) {
        break;
      }
      buffer.delete(index, index + 9);
    }

    // Select desired item
    String selector = "value=" + option;
    end = buffer.indexOf("</select>", start);
    index = buffer.indexOf(selector + ">", start);
    if (index == -1 || index >= end) {
      return;
    }

    buffer.insert(index + selector.length(), " selected");
  }
}
