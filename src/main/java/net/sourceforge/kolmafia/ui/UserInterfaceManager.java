package net.sourceforge.kolmafia.ui;

public class UserInterfaceManager {
  private static final UserInterface DEFAULT = new UserInterface() {};

  private static volatile UserInterface userInterface = DEFAULT;

  private UserInterfaceManager() {}

  public static UserInterface get() {
    return UserInterfaceManager.userInterface;
  }

  public static void set(final UserInterface userInterface) {
    UserInterfaceManager.userInterface = userInterface == null ? DEFAULT : userInterface;
  }
}
