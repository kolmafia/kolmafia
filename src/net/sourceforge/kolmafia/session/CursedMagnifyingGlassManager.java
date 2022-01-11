package net.sourceforge.kolmafia.session;

import java.util.Map;
import net.sourceforge.kolmafia.preferences.Preferences;

public class CursedMagnifyingGlassManager {
  private static Map<String, Integer> MAGNIFYING_GLASS_MESSAGES =
      Map.ofEntries(
          Map.entry("In the distance, an owl hoots 13 times.  Give it a rest, Mr. Owl.", 1),
          Map.entry(
              "You are startled by the cacophanous cawing of a bunch of crows.  Probably exactly twelve crows, if you had to guess.",
              2),
          Map.entry(
              "A distant clock chimes 11, even though it is (probably) not 11 o'clock right now.",
              3),
          Map.entry("A madman in the distance shrieks: \"Ten! Only ten now! Hee hee!\"", 4),
          Map.entry("Nine ravens burst from a nearby tree and take to the sky.", 5),
          Map.entry("Eight rats scurry out from behind a nearby bush, startling you.", 6),
          Map.entry("To your left, seven stray dogs fight over a scrap of carrion.", 7),
          Map.entry("A creepy-looking little girl walks up and whispers in your ear.  \"Six.\"", 8),
          Map.entry(
              "You look at your left hand and notice, to your horror, that you have five fingers.  Oh, wait, that's the normal number.  Never mind.",
              9),
          Map.entry("The bells of a distant cathedral ring four times.", 10),
          Map.entry(
              "Three wolves howl in the distance.  You wonder what they're howling about.  Probably just ordinary wolf stuff.",
              11),
          Map.entry(
              "You hear two black cats fighting somewhere nearby.  At least you hope they're fighting.",
              12),
          Map.entry(
              "The hair on the back of your neck stands up.  A feeling of impending dread overwhelms your senses.",
              13));

  public static final void updatePreference(String resultText) {
    for (Map.Entry<String, Integer> entry : MAGNIFYING_GLASS_MESSAGES.entrySet()) {
      if (resultText.contains(entry.getKey())) {
        Preferences.setInteger("cursedMagnifyingGlassCount", entry.getValue());
        return;
      }
    }
  }
}
