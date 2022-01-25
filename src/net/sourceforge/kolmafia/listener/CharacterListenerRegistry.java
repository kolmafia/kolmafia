package net.sourceforge.kolmafia.listener;

import java.util.ArrayList;
import java.util.List;

public class CharacterListenerRegistry {
  // Listener-driven container items

  private static final List<CharacterListener> listenerList = new ArrayList<CharacterListener>();

  private CharacterListenerRegistry() {}

  /**
   * Adds a new <code>CharacterListener</code> to the list of listeners listening to this <code>
   * KoLCharacter</code>.
   *
   * @param listener The listener to be added to the listener list
   */
  public static final void addCharacterListener(final CharacterListener listener) {
    if (listener != null && !CharacterListenerRegistry.listenerList.contains(listener)) {
      CharacterListenerRegistry.listenerList.add(listener);
    }
  }

  /**
   * Removes an existing <code>KoLCharacterListener</code> from the list of listeners listening to
   * this <code>KoLCharacter</code>.
   *
   * @param listener The listener to be removed from the listener list
   */
  public static final void removeCharacterListener(final CharacterListener listener) {
    if (listener != null) {
      CharacterListenerRegistry.listenerList.remove(listener);
    }
  }

  public static final void updateStatus() {
    CharacterListener[] listenerArray =
        new CharacterListener[CharacterListenerRegistry.listenerList.size()];
    CharacterListenerRegistry.listenerList.toArray(listenerArray);

    for (int i = 0; i < listenerArray.length; ++i) {
      listenerArray[i].update();
    }
  }
}
