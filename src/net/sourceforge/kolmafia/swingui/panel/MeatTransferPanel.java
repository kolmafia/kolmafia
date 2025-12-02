package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;
import javax.swing.JLabel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ClosetRequest.ClosetRequestType;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MeatTransferPanel extends LabeledPanel {
  private final TransferType transferType;
  private final AutoHighlightTextField amountField;
  private final JLabel closetField;

  public enum TransferType {
    MEAT_TO_CLOSET,
    MEAT_TO_INVENTORY,
    PULL_MEAT_FROM_STORAGE
  }

  public MeatTransferPanel(final TransferType transferType) {
    super(
        MeatTransferPanel.getTitle(transferType),
        "transfer",
        "transfer all",
        new Dimension(80, 20),
        new Dimension(240, 20));

    this.amountField = new AutoHighlightTextField();
    this.closetField = new JLabel(" ");

    VerifiableElement[] elements = new VerifiableElement[2];
    elements[0] = new VerifiableElement("Amount: ", this.amountField);
    elements[1] = new VerifiableElement("Available: ", this.closetField);

    this.setContent(elements);

    this.transferType = transferType;
    this.refreshCurrentAmount();

    CharacterListenerRegistry.addCharacterListener(new CharacterListener(new AmountRefresher()));
  }

  private static String getTitle(final TransferType transferType) {
    return switch (transferType) {
      case MEAT_TO_CLOSET -> "Put Meat in Your Closet";
      case MEAT_TO_INVENTORY -> "Take Meat from Your Closet";
      case PULL_MEAT_FROM_STORAGE -> "Pull Meat from Hagnk's";
      default -> "Unknown Transfer Type";
    };
  }

  private GenericRequest getRequest(final long amount) {
    return switch (transferType) {
      case MEAT_TO_CLOSET -> new ClosetRequest(ClosetRequestType.MEAT_TO_CLOSET, amount);
      case MEAT_TO_INVENTORY -> new ClosetRequest(ClosetRequestType.MEAT_TO_INVENTORY, amount);
      case PULL_MEAT_FROM_STORAGE ->
          new StorageRequest(StorageRequestType.PULL_MEAT_FROM_STORAGE, amount);
      default -> null;
    };
  }

  private long currentAvailable() {
    return switch (this.transferType) {
      case MEAT_TO_CLOSET -> KoLCharacter.getAvailableMeat();
      case MEAT_TO_INVENTORY -> KoLCharacter.getClosetMeat();
      case PULL_MEAT_FROM_STORAGE -> KoLCharacter.getStorageMeat();
      default -> 0;
    };
  }

  private void refreshCurrentAmount() {
    switch (this.transferType) {
      case MEAT_TO_CLOSET, MEAT_TO_INVENTORY, PULL_MEAT_FROM_STORAGE -> {
        long amount = this.currentAvailable();
        this.closetField.setText(KoLConstants.COMMA_FORMAT.format(amount) + " meat");
      }
      default -> this.closetField.setText("Information not available");
    }
  }

  @Override
  public void actionConfirmed() {
    int amountToTransfer = InputFieldUtilities.getValue(this.amountField);

    RequestThread.postRequest(this.getRequest(amountToTransfer));
  }

  @Override
  public void actionCancelled() {
    RequestThread.postRequest(this.getRequest(this.currentAvailable()));
  }

  public boolean shouldAddStatusLabel(final VerifiableElement[] elements) {
    return false;
  }

  private class AmountRefresher implements Runnable {
    @Override
    public void run() {
      MeatTransferPanel.this.refreshCurrentAmount();
    }
  }
}
