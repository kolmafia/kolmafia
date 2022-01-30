package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class OysterEggManager {
  static String[] messages = {
    "You find an Oyster egg Array.",
    "You find an Oyster egg atop a huge X that has been painted on the ground, for some reason.",
    "You find an Oyster egg behind a furnace.",
    "You find an Oyster egg behind a stack of questionable men's magazines.",
    "You find an Oyster egg behind a toilet. Eeeeew.",
    "You find an Oyster egg behind Curtain #3.",
    "You find an Oyster egg behind the ear of a startled-looking Knob Goblin teenager.",
    "You find an Oyster egg carefully hidden in your own pants.",
    "You find an Oyster egg down by a crick. Er, a creek.",
    "You find an Oyster egg down by the schoolyard, in some kid named Julio's backpack.",
    "You find an Oyster egg hidden behind a stack of old newspapers.",
    "You find an Oyster egg hidden in a pile of dryer lint.",
    "You find an Oyster egg hidden in a pile of other, less interesting eggs.",
    "You find an Oyster egg in a huge box labeled 'Oyster eggs. Do not touch.'",
    "You find an Oyster egg in a plain brown wrapper, in somebody else's mailbox.",
    "You find an Oyster egg in an old shoebox, next to the skeleton of a gerbil.",
    "You find an Oyster egg in between the spokes of a baby carriage wheel.",
    "You find an Oyster egg in plain sight.",
    "You find an Oyster egg in the freezer section of a nearby grocery store.",
    "You find an Oyster egg in the pocket of somebody's bathrobe.",
    "You find an Oyster egg in your imaginary friend's back pocket.",
    "You find an Oyster egg inside a loaf of bread.",
    "You find an Oyster egg inside a pumpkin. What was that pumpkin doing there?",
    "You find an Oyster egg inside a stuffed animal, which did not survive the egg-removal operation.",
    "You find an Oyster egg inside an empty cereal box.",
    "You find an Oyster egg next to something that's either a coyote or a goose. You can't tell which.",
    "You find an Oyster egg taped under the lid of a nearby wastebasket.",
    "You find an Oyster egg to the left. Of everything.",
    "You find an Oyster egg under a bed. Of flowers.",
    "You find an Oyster egg under a boardwalk. Down by the sea.",
    "You find an Oyster egg under the cushions of an inexplicable couch.",
    "You find an Oyster egg underneath a fireman's hat. That was lying on the ground.",
    "You find an Oyster egg underneath a sleeping Frat Boy.",
    "You find an Oyster egg underneath an annoyed-looking Lavatory Troll.",
  };

  public static void trackEgg(String responseText) {
    if (!HolidayDatabase.getHoliday().contains("Oyster Egg Day")) return;
    for (String msg : messages) {
      if (responseText.contains(msg)) {
        Preferences.increment("_oysterEggsFound");
        return;
      }
    }
  }
}
