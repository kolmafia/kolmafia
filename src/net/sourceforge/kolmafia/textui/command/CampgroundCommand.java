package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundCommand extends AbstractCommand {
  public CampgroundCommand() {
    this.usage =
        " rest [free] [chateau|campaway|vault|campground] [<times>] | <other action> [<times>] - perform campground actions.";
  }

  protected enum RestType {
    CHATEAU(
        "Chateau Montange",
        "chateau",
        new ChateauRequest(ChateauRequest.BED),
        ChateauRequest::chateauRestUsable),
    CAMPAWAY(
        "Campsite Away From Your Campsite",
        "campaway",
        new CampAwayRequest(CampAwayRequest.TENT),
        CampAwayRequest::campAwayTentRestUsable),
    VAULT(
        "Cryo-Sleep Chamber",
        "vault",
        new FalloutShelterRequest("vault1"),
        KoLCharacter::inNuclearAutumn),
    CAMPGROUND(
        "campground",
        "campground",
        new CampgroundRequest("rest"),
        () -> !KoLCharacter.getLimitMode().limitCampground() && !KoLCharacter.isEd());

    private final String name;
    private final String shortname;
    private final BooleanSupplier predicate;
    private final GenericRequest request;

    RestType(
        final String name,
        final String shortname,
        GenericRequest request,
        BooleanSupplier predicate) {
      this.name = name;
      this.shortname = shortname;
      this.request = request;
      this.predicate = predicate;
    }

    public String getName() {
      return name;
    }

    public String getShortname() {
      return shortname;
    }

    public BooleanSupplier getPredicate() {
      return predicate;
    }

    public GenericRequest getRequest() {
      return request;
    }

    /** Get the list of RestTypes in KoLmafia's semi-subjective order of optimality */
    static List<RestType> valuesInOrder() {
      return List.of(CHATEAU, CAMPAWAY, VAULT, CAMPGROUND);
    }
  }

  @Override
  public void run(final String cmd, final String parameters) {
    var parameterList = new ArrayList<>(List.of(parameters.split("\\s+")));

    String action = parameterList.removeFirst();

    if (action.equals("rest")) {
      rest(parameterList);
      return;
    }

    if (KoLCharacter.getLimitMode().limitCampground() || KoLCharacter.isEd()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a campground right now.");
      return;
    }

    var request =
        KoLCharacter.inNuclearAutumn()
            ? new FalloutShelterRequest(action.equals("terminal") ? "vault_term" : action)
            : new CampgroundRequest(action);

    var count = parameterList.isEmpty() ? 1 : StringUtilities.parseInt(parameterList.getLast());

    KoLmafia.makeRequest(request, count);
  }

  private void rest(final List<String> params) {
    var max = Integer.MAX_VALUE;
    // If user specified free rests only, check if they have any left
    if (params.contains("free")) {
      max = KoLCharacter.freeRestsRemaining();
      params.remove("free");
    }

    GenericRequest request = null;

    // Check if user specified a particular resting spot
    for (var restType : RestType.valuesInOrder()) {
      if (!params.contains(restType.getShortname())) continue;

      if (!restType.getPredicate().getAsBoolean()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You cannot rest at your " + restType.getName() + " right now.");
        return;
      }

      params.remove(restType.getShortname());
      request = restType.getRequest();
      break;
    }

    // Find our best waiting spot if none was specified
    if (request == null) {
      request =
          RestType.valuesInOrder().stream()
              .filter(restType -> restType.getPredicate().getAsBoolean())
              .map(RestType::getRequest)
              .findFirst()
              .orElse(null);
    }

    if (request == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have no available resting spots right now.");
      return;
    }

    var count = Math.min(max, params.isEmpty() ? 1 : StringUtilities.parseInt(params.getFirst()));
    KoLmafia.makeRequest(request, count);
  }
}
