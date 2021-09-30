package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.CoinMasterRequest;

public class CoinmasterCommand extends AbstractCommand {
  public CoinmasterCommand() {
    this.usage = " (buy|sell) <nickname> <item>... - buy or sell items to specified coinmaster.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    boolean isBuy;
    if (parameters.startsWith("buy")) {
      parameters = parameters.substring(3).trim();
      isBuy = true;
    } else if (parameters.startsWith("sell")) {
      parameters = parameters.substring(4).trim();
      isBuy = false;
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid coinmaster command.");
      return;
    }

    // Identify the coinmaster

    int spaceIndex = parameters.indexOf(" ");
    if (spaceIndex == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid coinmaster command.");
      return;
    }

    String nickname = parameters.substring(0, spaceIndex);
    parameters = parameters.substring(spaceIndex + 1).trim();

    CoinmasterData data = CoinmasterRegistry.findCoinmasterByNickname(nickname);
    if (data == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which coinmaster is " + nickname + "?");
      return;
    }

    List<AdventureResult> source = isBuy ? null : KoLConstants.inventory;
    AdventureResult[] itemList = ItemFinder.getMatchingItemList(parameters, source);

    if (itemList.length == 0) {
      return;
    }

    String URL;
    String action;

    if (isBuy) {
      action = data.getBuyAction();
      if (action == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't buy from " + data.getMaster());
        return;
      }

      for (int i = 0; i < itemList.length; ++i) {
        AdventureResult item = itemList[i];
        if (!data.canBuyItem(item.getItemId())) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You can't buy " + item.getName() + " from " + data.getMaster());
          return;
        }
      }

      String reason = data.canBuy();
      if (reason != null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, reason);
        return;
      }
    } else {
      action = data.getSellAction();
      if (action == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't sell to " + data.getMaster());
        return;
      }

      for (int i = 0; i < itemList.length; ++i) {
        AdventureResult item = itemList[i];
        if (!data.canSellItem(item.getItemId())) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You can't sell " + item.getName() + " to " + data.getMaster());
          return;
        }
      }

      String reason = data.canSell();
      if (reason != null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, reason);
        return;
      }
    }

    CoinMasterRequest request = data.getRequest(isBuy, itemList);

    RequestThread.postRequest(request);
  }
}
