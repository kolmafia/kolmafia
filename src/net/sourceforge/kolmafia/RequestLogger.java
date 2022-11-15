package net.sourceforge.kolmafia;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.*;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.ElVibratoManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.NullStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;

public class RequestLogger extends NullStream {
  public static final RequestLogger INSTANCE = new RequestLogger();

  private static PrintStream outputStream = KoLmafiaTUI.outputStream;
  private static PrintStream mirrorStream = NullStream.INSTANCE;

  private static PrintStream sessionStream = NullStream.INSTANCE;
  private static PrintStream debugStream = NullStream.INSTANCE;
  private static PrintStream traceStream = NullStream.INSTANCE;

  private static String lastURLString = "";
  public static String previousUpdateString = "";
  private static boolean wasLastRequestSimple = false;

  private RequestLogger() {}

  public static String getLastURLString() {
    return lastURLString;
  }

  @Override
  public void println() {
    RequestLogger.printLine();
  }

  @Override
  public void println(final String line) {
    RequestLogger.printLine(line);
  }

  public static void printList(final List<?> printing, final PrintStream ostream) {
    if (printing == null || ostream == null) {
      return;
    }

    if (printing != KoLConstants.availableSkills) {
      ostream.println(
          printing.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .collect(Collectors.joining(KoLConstants.LINE_BREAK)));
      return;
    }

    StringBuffer buffer = new StringBuffer();
    SkillDatabase.generateSkillList(buffer, false);

    if (ostream != INSTANCE) {
      ostream.println(buffer);
      return;
    }

    printLine(buffer.toString(), false);

    buffer.setLength(0);
    SkillDatabase.generateSkillList(buffer, true);
    KoLConstants.commandBuffer.append(buffer.toString());
  }

  public static void printList(final List<?> printing) {
    RequestLogger.printList(printing, INSTANCE);
  }

  public static void printLine() {
    RequestLogger.printLine(MafiaState.CONTINUE, "", true);
  }

  public static void printLine(final String message) {
    RequestLogger.printLine(MafiaState.CONTINUE, message, true);
  }

  public static void printLine(final String message, final boolean addToBuffer) {
    RequestLogger.printLine(MafiaState.CONTINUE, message, addToBuffer);
  }

  public static void printLine(final MafiaState state, final String message) {
    RequestLogger.printLine(state, message, true);
  }

  public static void printLine(final MafiaState state, String message, boolean addToBuffer) {
    if (message == null) {
      return;
    }

    message = message.trim();

    if (message.length() == 0 && RequestLogger.previousUpdateString.length() == 0) {
      return;
    }

    RequestLogger.previousUpdateString = message;

    RequestLogger.outputStream.println(message);
    RequestLogger.debugStream.println(message);

    if (StaticEntity.backtraceTrigger != null && message.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by message");
    }

    if (!addToBuffer) {
      RequestLogger.mirrorStream.println(message);
      return;
    }

    StringBuffer colorBuffer = new StringBuffer();

    if (message.isEmpty()) {
      colorBuffer.append("<br>");
    } else {
      boolean addedColor = false;

      // Temporary workaround for Java bug
      if (message.startsWith("/")) {
        colorBuffer.append("<span>");
      }

      if (state == MafiaState.ERROR || state == MafiaState.ABORT) {
        addedColor = true;
        colorBuffer.append("<font color=red>");
      } else if (message.startsWith("> QUEUED")) {
        addedColor = true;
        colorBuffer.append(" <font color=olive><b>");
      } else if (message.startsWith("> ")) {
        addedColor = true;
        colorBuffer.append(" <font color=olive>");
      }

      colorBuffer.append(StringUtilities.globalStringReplace(message, "\n", "<br>"));

      if (message.startsWith("> QUEUED")) {
        colorBuffer.append("</b>");
      }

      if (addedColor) {
        colorBuffer.append("</font><br>");
      } else {
        colorBuffer.append("<br>");
      }

      if (!message.contains("<") && message.contains(KoLConstants.LINE_BREAK)) {
        colorBuffer.append("</pre>");
      }

      // Temporary workaround for Java bug
      if (message.startsWith("/")) {
        colorBuffer.append("</span>");
      }

      StringUtilities.globalStringDelete(colorBuffer, "<html>");
      StringUtilities.globalStringDelete(colorBuffer, "</html>");
    }

    colorBuffer.append(KoLConstants.LINE_BREAK);
    KoLConstants.commandBuffer.append(colorBuffer.toString());
    RequestLogger.mirrorStream.println(colorBuffer);
    RelayServer.addStatusMessage(colorBuffer.toString());
  }

  public static PrintStream openStream(
      final String filename, final PrintStream originalStream, boolean hasLocation) {
    if (!hasLocation && KoLCharacter.getUserName().isEmpty()) {
      return NullStream.INSTANCE;
    }

    // Before doing anything, be sure to close the
    // original stream.

    if (!(originalStream instanceof NullStream)) {
      if (hasLocation) {
        return originalStream;
      }

      RequestLogger.closeStream(originalStream);
    }

    return LogStream.openStream(filename, false);
  }

  public static void closeStream(final PrintStream stream) {
    try {
      stream.close();
    } catch (Exception e) {
    }
  }

  public static void openCustom(PrintStream out) {
    RequestLogger.outputStream = out;
  }

  public static void closeCustom() {
    RequestLogger.closeStream(RequestLogger.outputStream);
    RequestLogger.outputStream = KoLmafiaTUI.outputStream;
  }

  public static void openMirror(final String location) {
    RequestLogger.mirrorStream =
        RequestLogger.openStream(location, RequestLogger.mirrorStream, true);
  }

  public static void closeMirror() {
    RequestLogger.closeStream(RequestLogger.mirrorStream);
    RequestLogger.mirrorStream = NullStream.INSTANCE;
  }

  public static void setSessionStream(PrintStream stream) {
    RequestLogger.sessionStream = stream;
  }

  public static PrintStream getSessionStream() {
    return RequestLogger.sessionStream;
  }

  public static void openSessionLog() {
    RequestLogger.sessionStream =
        RequestLogger.openStream(
            KoLConstants.SESSIONS_DIRECTORY
                + StringUtilities.globalStringReplace(KoLCharacter.getUserName(), " ", "_")
                + "_"
                + KoLConstants.DAILY_FORMAT.format(new Date())
                + ".txt",
            RequestLogger.sessionStream,
            false);
  }

  public static void closeSessionLog() {
    RequestLogger.closeStream(RequestLogger.sessionStream);
    RequestLogger.sessionStream = NullStream.INSTANCE;
  }

  public static void updateSessionLog() {
    RequestLogger.sessionStream.println();
  }

  public static void updateSessionLog(final String line) {
    if (StaticEntity.backtraceTrigger != null && line.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by session log message");
    }

    RequestLogger.sessionStream.println(line);
  }

  public static boolean isDebugging() {
    return RequestLogger.debugStream != NullStream.INSTANCE;
  }

  public static PrintStream getDebugStream() {
    return RequestLogger.debugStream;
  }

  public static void openDebugLog() {
    RequestLogger.debugStream =
        RequestLogger.openStream(
            "DEBUG_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt",
            RequestLogger.debugStream,
            true);
    NamedListenerRegistry.fireChange("(debug)");
  }

  public static void closeDebugLog() {
    RequestLogger.closeStream(RequestLogger.debugStream);
    RequestLogger.debugStream = NullStream.INSTANCE;
    NamedListenerRegistry.fireChange("(debug)");
  }

  public static void updateDebugLog() {
    RequestLogger.debugStream.println();
  }

  public static void updateDebugLog(final String line) {
    if (StaticEntity.backtraceTrigger != null && line.contains(StaticEntity.backtraceTrigger)) {
      StaticEntity.printStackTrace("Backtrace triggered by debug log message");
    }

    RequestLogger.debugStream.println(line);
  }

  public static void updateDebugLog(final Throwable t) {
    t.printStackTrace(RequestLogger.debugStream);
  }

  public static void updateDebugLog(final Object o) {
    RequestLogger.debugStream.println(o.toString());
  }

  public static boolean isTracing() {
    return RequestLogger.traceStream != NullStream.INSTANCE;
  }

  public static PrintStream getTraceStream() {
    return RequestLogger.traceStream;
  }

  public static void openTraceStream() {
    RequestLogger.traceStream =
        RequestLogger.openStream(
            "TRACE_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt",
            RequestLogger.traceStream,
            true);
  }

  public static void closeTraceStream() {
    RequestLogger.closeStream(RequestLogger.traceStream);
    RequestLogger.traceStream = NullStream.INSTANCE;
  }

  private static final StringBuilder traceBuffer = new StringBuilder();

  public static synchronized void trace(String message) {
    if (RequestLogger.isTracing()) {
      traceBuffer.setLength(0);
      traceBuffer.append((new Date()).getTime());
      traceBuffer.append(": ");
      traceBuffer.append(message);
      RequestLogger.traceStream.println(traceBuffer);
    }
  }

  public static void registerRequest(final GenericRequest request, final String urlString) {
    try {
      RequestLogger.doRegister(request, urlString);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void doRegister(final GenericRequest request, final String urlString) {
    // If we are in a fight, don't even look at things which are
    // not fight.php, since they will immediately redirect to
    // continue the fight.
    if (FightRequest.currentRound != 0 && !urlString.startsWith("fight.php")) {
      return;
    }

    RequestLogger.lastURLString = urlString;

    if (urlString.startsWith("api")
        || urlString.startsWith("charpane")
        || urlString.startsWith("account")
        || urlString.startsWith("login")
        || urlString.startsWith("logout")) {
      return;
    }

    // We want to do special things when we visit locations within
    // the Sorceress' Lair. Those locations which are "adventures"
    // but are not claimed here will be picked up by KoLAdventure

    if (SorceressLairManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if (KoLAdventure.recordToSession(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Anything past this is not an "adventure" per se

    boolean isExternal =
        request.getClass() == GenericRequest.class
            || request instanceof RelayRequest
            || request instanceof PlaceRequest;

    if ((isExternal || request instanceof FightRequest)
        && FightRequest.registerRequest(isExternal, urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Some adventures do not post any form fields,
    // so handle them first.

    // We want to register simple visits to the Altar of Literacy
    if ((isExternal || request instanceof AltarOfLiteracyRequest)
        && AltarOfLiteracyRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register simple visits to the Bounty Hunter Hunter
    if ((isExternal || request instanceof BountyHunterHunterRequest)
        && BountyHunterHunterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register visits to the You're the Fudge Wizard Now, Dog choice adventure before
    // ChoiceManager.
    if ((isExternal || request instanceof FudgeWandRequest)
        && FudgeWandRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register visits to the Summoning Chamber choice adventure before ChoiceManager.
    if ((isExternal || request instanceof SummoningChamberRequest)
        && SummoningChamberRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register simple visits to HeyDeze
    if ((isExternal || request instanceof HeyDezeRequest)
        && HeyDezeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register simple visits to Mr. Store
    if ((isExternal || request instanceof MrStoreRequest)
        && MrStoreRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register simple visits to Spaaace
    if ((isExternal || request instanceof SpaaaceRequest)
        && SpaaaceRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register simple visits to the Volcano Maze
    if ((isExternal || request instanceof VolcanoMazeRequest)
        && VolcanoMazeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Burning Newspaper creation is an instance of choice.php
    if ((isExternal || request instanceof BurningNewspaperRequest)
        && BurningNewspaperRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // metal meteoroid creation is an instance of choice.php
    if ((isExternal || request instanceof MeteoroidRequest)
        && MeteoroidRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Clan Lounge Swimming Pool is an instance of choice.php
    if ((isExternal || request instanceof ClanLoungeSwimmingPoolRequest)
        && ClanLoungeSwimmingPoolRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Cargo Cultist Shorts is an instance of choice.php
    if ((isExternal || request instanceof CargoCultistShortsRequest)
        && CargoCultistShortsRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Deck of Every Card is an instance of choice.php
    if ((isExternal || request instanceof DeckOfEveryCardRequest)
        && DeckOfEveryCardRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Sweet Synthesis is an instance of choice.php
    if ((isExternal || request instanceof SweetSynthesisRequest)
        && SweetSynthesisRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Florist is an instance of choice.php
    if ((isExternal || request instanceof FloristRequest)
        && FloristRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Numberology is an instance of choice.php
    if ((isExternal || request instanceof NumberologyRequest)
        && NumberologyRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Potted Tea Tree is an instance of choice.php
    if ((isExternal || request instanceof PottedTeaTreeRequest)
        && PottedTeaTreeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Sausage Grinder creation is an instance of choice.php
    if ((isExternal || request instanceof SausageOMaticRequest)
        && SausageOMaticRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The Source Terminal is an instance of choice.php
    if ((isExternal || request instanceof TerminalRequest)
        && TerminalRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Wax Glob creation is an instance of choice.php
    if ((isExternal || request instanceof WaxGlobRequest)
        && WaxGlobRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Some general URLs which never need to be registered
    // because they don't do anything.

    if (urlString.startsWith("choice")) {
      ChoiceManager.registerRequest(urlString);
      return;
    }

    // We want to register a simple visit to tiles.php
    if (DvorakManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register a simple visit to ocean.php
    if (OceanManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // We want to register a simple visit to elvmachine.php
    if (ElVibratoManager.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Anything else that doesn't submit an actual form
    // should not be registered.

    if (!urlString.contains("?")) {
      return;
    }

    // We want to register some visits to the Campground
    if ((isExternal || request instanceof CampgroundRequest)
        && CampgroundRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // This is a campground request and so must go here.
    if ((isExternal || request instanceof PizzaCubeRequest)
        && PizzaCubeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // This is a campground request and so must go here.
    if ((isExternal || request instanceof PortalRequest)
        && PortalRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // This is a campground request and so must go here.
    if ((isExternal || request instanceof TelescopeRequest)
        && TelescopeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // This might be a campground request and so must go here.
    if ((isExternal || request instanceof UseSkillRequest)
        && UseSkillRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // This might be on inventory.php

    if ((isExternal || request instanceof EquipmentRequest)
        && EquipmentRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if (urlString.startsWith("campground")
        || urlString.startsWith("doc.php")
        || urlString.startsWith("inventory.php?ajax")
        || urlString.startsWith("inventory.php?which=")
        || urlString.startsWith("inventory.php?action=message")
        || urlString.startsWith("mining")) {
      return;
    }

    // Check individual cafes
    if ((isExternal || request instanceof MicroBreweryRequest)
        && MicroBreweryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ChezSnooteeRequest)
        && ChezSnooteeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CrimboCafeRequest)
        && CrimboCafeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Check UseItemRequest early, so that lastItemUsed gets
    // cleared when processing anything else.  Otherwise, any
    // non-item-use that redirects to inventory.php?action=message
    // (such as outfit changes) will cause the last item to be
    // processed again.

    // However, we have to check CreateItemRequest earlier, so that
    // it can handle single-/multi-use concoctions.
    if (CreateItemRequest.registerRequest(isExternal, urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Are we finally ready to call UseItemRequest?
    if ((isExternal || request instanceof UseItemRequest)
        && UseItemRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The following is in place.php
    if ((isExternal || request instanceof FalloutShelterRequest)
        && FalloutShelterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Let the "placeholder" for place.php take every otherwise
    // unclaimed call to that URL.

    if (PlaceRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // The following lists all the remaining requests in
    // alphabetical order.

    if ((isExternal || request instanceof AfterLifeRequest)
        && AfterLifeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AirportRequest)
        && AirportRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AltarOfBonesRequest)
        && AltarOfBonesRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ArmoryRequest)
        && ArmoryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ArmoryAndLeggeryRequest)
        && ArmoryAndLeggeryRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AppleStoreRequest)
        && AppleStoreRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ArcadeRequest || request instanceof TicketCounterRequest)
        && ArcadeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ArtistRequest)
        && ArtistRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AutoMallRequest)
        && AutoMallRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AutoSellRequest)
        && AutoSellRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof AWOLQuartermasterRequest)
        && AWOLQuartermasterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BatFabricatorRequest)
        && BatFabricatorRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BeerGardenRequest)
        && BeerGardenRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BeerPongRequest)
        && BeerPongRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BigBrotherRequest)
        && BigBrotherRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BlackMarketRequest)
        && BlackMarketRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BoutiqueRequest)
        && BoutiqueRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BrogurtRequest)
        && BrogurtRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BuffJimmyRequest)
        && BuffJimmyRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof BURTRequest) && BURTRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CafeRequest) && CafeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CakeArenaRequest)
        && CakeArenaRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CampAwayRequest)
        && CampAwayRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CanteenRequest)
        && CanteenRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ChateauRequest)
        && ChateauRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ChemiCorpRequest)
        && ChemiCorpRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ClanLoungeRequest)
        && ClanLoungeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ClanRumpusRequest)
        && ClanRumpusRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ClanStashRequest)
        && ClanStashRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ClosetRequest)
        && ClosetRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CosmicRaysBazaarRequest)
        && CosmicRaysBazaarRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CRIMBCOGiftShopRequest)
        && CRIMBCOGiftShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo07Request)
        && Crimbo07Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo09Request)
        && Crimbo09Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo10Request)
        && Crimbo10Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo11Request)
        && Crimbo11Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo12Request)
        && Crimbo12Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo14Request)
        && Crimbo14Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo16Request)
        && Crimbo16Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo17Request)
        && Crimbo17Request.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo20BoozeRequest)
        && Crimbo20BoozeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo20CandyRequest)
        && Crimbo20CandyRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo20FoodRequest)
        && Crimbo20FoodRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof Crimbo21TreeRequest)
        && Crimbo21TreeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CrimboCartelRequest)
        && CrimboCartelRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof CurseRequest)
        && CurseRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DigRequest) && DigRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DimemasterRequest)
        && DimemasterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DinostaurRequest)
        && DinostaurRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DinseyCompanyStoreRequest)
        && DinseyCompanyStoreRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DiscoGiftCoRequest)
        && DiscoGiftCoRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DisplayCaseRequest)
        && DisplayCaseRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DollHawkerRequest)
        && DollHawkerRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DreadsylvaniaRequest)
        && DreadsylvaniaRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DwarfContraptionRequest)
        && DwarfContraptionRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof DwarfFactoryRequest)
        && DwarfFactoryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof EdBaseRequest)
        && EdBaseRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof EdShopRequest)
        && EdShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FamiliarRequest)
        && FamiliarRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FamTeamRequest)
        && FamTeamRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FancyDanRequest)
        && FancyDanRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FantasyRealmRequest)
        && FantasyRealmRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FDKOLRequest)
        && FDKOLRequest.registerRequest(urlString, false)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FishboneryRequest)
        && FishboneryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FiveDPrinterRequest)
        && FiveDPrinterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FreeSnackRequest)
        && FreeSnackRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FriarRequest)
        && FriarRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof FunALogRequest)
        && FunALogRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GameShoppeRequest)
        && GameShoppeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GMartRequest)
        && GMartRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GourdRequest)
        && GourdRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GotporkOrphanageRequest)
        && GotporkOrphanageRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GotporkPDRequest)
        && GotporkPDRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GrandmaRequest)
        && GrandmaRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GrandpaRequest)
        && GrandpaRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof GuildRequest)
        && GuildRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof HermitRequest)
        && HermitRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof IslandRequest)
        && IslandRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof IsotopeSmitheryRequest)
        && IsotopeSmitheryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof JarlsbergRequest)
        && JarlsbergRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof JunkMagazineRequest)
        && JunkMagazineRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof KnollRequest)
        && KnollRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof KOLHSRequest)
        && KOLHSRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof KringleRequest)
        && KringleRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof LeafletRequest)
        && LeafletRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof LTTRequest) && LTTRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof LunarLunchRequest)
        && LunarLunchRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MallSearchRequest)
        && MallSearchRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ManageStoreRequest)
        && ManageStoreRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MemeShopRequest)
        && MemeShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MerchTableRequest)
        && MerchTableRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MindControlRequest)
        && MindControlRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MomRequest) && MomRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MonsterManuelRequest)
        && MonsterManuelRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof MushroomRequest)
        && MushroomRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof NeandermallRequest)
        && NeandermallRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof NemesisRequest)
        && NemesisRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof NinjaStoreRequest)
        && NinjaStoreRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof NuggletCraftingRequest)
        && NuggletCraftingRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PandamoniumRequest)
        && PandamoniumRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PeeVPeeRequest)
        && PeeVPeeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PixelRequest)
        && PixelRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PokemporiumRequest)
        && PokemporiumRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PrecinctRequest)
        && PrecinctRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ProfileRequest)
        && ProfileRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof PulverizeRequest)
        && PulverizeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof QuartersmasterRequest)
        && QuartersmasterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof RaffleRequest)
        && RaffleRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof RichardRequest)
        && RichardRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof RubeeRequest)
        && RubeeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof RumpleRequest)
        && RumpleRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ScrapheapRequest)
        && ScrapheapRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SeaMerkinRequest)
        && SeaMerkinRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SendGiftRequest)
        && SendGiftRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SendMailRequest)
        && SendMailRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ShoeRepairRequest)
        && ShoeRepairRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SHAWARMARequest)
        && SHAWARMARequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ShoreGiftShopRequest)
        && ShoreGiftShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ShrineRequest)
        && ShrineRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SkateParkRequest)
        && SkateParkRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SliemceRequest)
        && SliemceRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SpacegateFabricationRequest)
        && SpacegateFabricationRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SpantRequest)
        && SpantRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SpinMasterLatheRequest)
        && SpinMasterLatheRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof StarChartRequest)
        && StarChartRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof StandardRequest)
        && StandardRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof StillRequest)
        && StillRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof StorageRequest)
        && StorageRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SuburbanDisRequest)
        && SuburbanDisRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SugarSheetRequest)
        && SugarSheetRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof SwaggerShopRequest)
        && SwaggerShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TacoDanRequest)
        && TacoDanRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TavernRequest)
        && TavernRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TerrifiedEagleInnRequest)
        && TerrifiedEagleInnRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ThankShopRequest)
        && ThankShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TicketCounterRequest)
        && TicketCounterRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ToxicChemistryRequest)
        && ToxicChemistryRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TrapperRequest)
        && TrapperRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TravelingTraderRequest)
        && TravelingTraderRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TrophyHutRequest)
        && TrophyHutRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof TutorialRequest)
        && TutorialRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof UneffectRequest)
        && UneffectRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof UntinkerRequest)
        && UntinkerRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof VendingMachineRequest)
        && VendingMachineRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof VolcanoIslandRequest)
        && VolcanoIslandRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof WalMartRequest)
        && WalMartRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof WarbearBoxRequest)
        && WarbearBoxRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof WildfireCampRequest)
        && WildfireCampRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof WinterGardenRequest)
        && WinterGardenRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof XOShopRequest)
        && XOShopRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof YeNeweSouvenirShoppeRequest)
        && YeNeweSouvenirShoppeRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof YourCampfireRequest)
        && YourCampfireRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    if ((isExternal || request instanceof ZapRequest) && ZapRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Do PurchaseRequest after all Coinmaster shops so they can
    // register simple visits, if they so choose.

    if ((isExternal || request instanceof PurchaseRequest)
        && PurchaseRequest.registerRequest(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Let PlaceRequest step in and suppress logging for any
    // unclaimed simple visits to a place.php container

    if (PlaceRequest.unclaimedPlace(urlString)) {
      RequestLogger.wasLastRequestSimple = false;
      return;
    }

    // Otherwise, print the raw URL so that it's at least mentioned
    // in the session log.

    if (!RequestLogger.wasLastRequestSimple) {
      RequestLogger.updateSessionLog();
    }

    RequestLogger.wasLastRequestSimple = true;
    RequestLogger.updateSessionLog(urlString);
  }

  public static void registerLocation(final String location) {
    String message = "[" + KoLAdventure.getAdventureCount() + "] " + location;

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);
  }

  public static void registerLastLocation() {
    String location = KoLAdventure.lastLocationName;

    if (location == null) {
      location =
          (GenericRequest.itemMonster != null) ? GenericRequest.itemMonster : "Unknown Location";
    }

    RequestLogger.registerLocation(location);
  }
}
