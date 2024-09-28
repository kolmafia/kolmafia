package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public class DadManager {
  // You shake your head and look above the tank, at the window into
  // space. <Clue 1> forms <Clue 2> in the darkness, each more <Clue 3>
  // than the last. <Clue 4> <Clue 5>, <Clue 6> revealing <Clue 7>-
  // dimensional monstrosities.

  // No. Look again. There is nothing. <Is/Are> your <Clue 8> betraying
  // you? As if on cue, <Clue 9>-sided triangles materialize and then
  // disappear. So impossible that your <Clue 10> throbs.

  private static final Pattern CLUE_PATTERN =
      Pattern.compile(
          "You shake your head and look above the tank, at the window into space. *([^ ]+) forms ([^ ]+) in the darkness, each more ([^ ]+) than the last. *(?:The )?([^ ]+) ([^,]+), ([^ ]+) revealing (\\d+)-dimensional monstrosities..*?No. *Look again. *There is nothing. *(?:Is|Are) your (.+?) betraying you\\? *As if on cue, (\\d+)-sided triangles materialize and then disappear. *So impossible that your ([^ ]+) throbs.",
          Pattern.DOTALL);

  private DadManager() {}

  public enum Element {
    NONE,
    HOT,
    COLD,
    STENCH,
    SPOOKY,
    SLEAZE,
    PHYSICAL
  }

  private record ElementSpell(Element element, String name, String... spells) {}

  private static final ElementSpell[] ELEMENTS = {
    // Starting with the third element, any number of skills can be listed, one per element
    new ElementSpell(Element.NONE, "none", ""),
    new ElementSpell(Element.HOT, "hot", "Awesome Balls of Fire", "Volcanometeor Showeruption"),
    new ElementSpell(Element.COLD, "cold", "Snowclone"),
    new ElementSpell(Element.STENCH, "stench", "Eggsplosion"),
    new ElementSpell(Element.SPOOKY, "spooky", "Raise Backup Dancer"),
    new ElementSpell(Element.SLEAZE, "sleaze", "Grease Lightning"),
    new ElementSpell(Element.PHYSICAL, "physical", "Toynado", "Shrap"),
  };

  private static ElementSpell search(Element element) {
    for (ElementSpell row : ELEMENTS) {
      if (row.element == element) {
        return row;
      }
    }
    return null;
  }

  public static String elementToName(Element element) {
    ElementSpell row = DadManager.search(element);
    return row == null ? "Unknown" : row.name;
  }

  public static String elementToSpell(Element element) {
    ElementSpell row = DadManager.search(element);
    if (row == null) {
      return "Unknown";
    }
    for (String spell : row.spells) {
      if (KoLCharacter.hasSkill(spell)) {
        return spell;
      }
    }
    return "Unknown";
  }

  public static Element intToElement(int index) {
    return (index < 0 || index > ELEMENTS.length) ? Element.NONE : ELEMENTS[index].element;
  }

  private record WordElement(String word, Element element) {}

  private static final WordElement[] WORD1 = {
    new WordElement("chaotic", Element.HOT),
    new WordElement("rigid", Element.COLD),
    new WordElement("rotting", Element.STENCH),
    new WordElement("horrifying", Element.SPOOKY),
    new WordElement("slimy", Element.SLEAZE),
    new WordElement("pulpy", Element.PHYSICAL),
  };

  private static final WordElement[] WORD2 = {
    new WordElement("skitter", Element.HOT),
    new WordElement("shamble", Element.COLD),
    new WordElement("ooze", Element.STENCH),
    new WordElement("float", Element.SPOOKY),
    new WordElement("slither", Element.SLEAZE),
    new WordElement("swim", Element.PHYSICAL),
  };

  private static final WordElement[] WORD3 = {
    new WordElement("terrible", Element.HOT),
    new WordElement("awful", Element.COLD),
    new WordElement("putrescent", Element.STENCH),
    new WordElement("frightening", Element.SPOOKY),
    new WordElement("bloated", Element.SLEAZE),
    new WordElement("curious", Element.PHYSICAL),
  };

  private static final WordElement[] WORD4 = {
    new WordElement("blackness", Element.HOT),
    new WordElement("space", Element.COLD),
    new WordElement("void", Element.STENCH),
    new WordElement("darkness", Element.SPOOKY),
    new WordElement("emptiness", Element.SLEAZE),
    new WordElement("portal", Element.PHYSICAL),
  };

  private static final WordElement[] WORD5 = {
    new WordElement("warps", Element.HOT),
    new WordElement("shifts", Element.COLD),
    new WordElement("shimmers", Element.STENCH),
    new WordElement("shakes", Element.SPOOKY),
    new WordElement("wobbles", Element.SLEAZE),
    new WordElement("cracks open", Element.PHYSICAL),
  };

  private static final String[] WORD8 = {
    "brain",
    "mind",
    "reason",
    "sanity",
    "grasp on reality",
    "sixth sense",
    "eyes",
    "thoughts",
    "senses",
    "memories",
    "fears",
  };

  private static final String[] WORD10 = {
    "spleen", "stomach", "skull", "forehead", "brain", "mind", "heart", "throat", "chest", "head",
  };

  private static Element search(String key, WordElement[] table) {
    for (WordElement row : table) {
      if (key.equals(row.word)) {
        return row.element;
      }
    }
    return null;
  }

  private static Element wordToElement(String key, WordElement[] table) {
    Element element = DadManager.search(key.toLowerCase(), table);
    return element == null ? Element.NONE : element;
  }

  private static int wordToInteger(String key, String[] table) {
    for (int i = 0; i < table.length; ++i) {
      if (key.equals(table[i])) {
        return i;
      }
    }
    return -1;
  }

  // cannonfire40 wrote this forum post:
  // http://forums.kingdomofloathing.com/vb/showpost.php?p=4439411&postcount=369
  //
  // Rounds 1-3 use simple keyword to element matching.
  //
  // Rounds 4-5 use keyword to element matching as well, but their clues
  // are in reverse order.
  //
  // Rounds 6-7 consist of either "Suddenly or slowly", and a number.
  // That number is of the form 2^x+2^y, where x and y are integers
  // between 0 and 5 inclusive. X and y are the elements in the order
  // 0= Hot, 1=Cold, 2=Stench, 3=Spooky, 4=Sleaze, and 5=Physical.
  // If the word is slowly, the element for the bigger digit is in
  // round 6, and the smaller number in round 7. Order is reversed
  // for the two rounds if the word is suddenly. If the digits are
  // equal, order does not matter because the same element appears in
  // both rounds. (There is an easier way to explain this using binary,
  // but I won't get into that here.)
  //
  // Round 8 uses a list of 11 terms, with correlate to numeric values
  // ranging from 2-12. You subtract the value of the element in round 1
  // (using the same ordering as before but with 1-6 representing the
  // elements instead of 0-5) and the remaining value represents the
  // elemental weakness.
  //
  // Round 9 sums the values of the elemental weaknesses in rounds 2
  // through 5, adds four, subtracts the number given as a clue, and then
  // the remaining value represents the elemental weakness for the round.
  //
  // Round 10 uses a list of 10 words, each with a value of 1-10. If the
  // value of the word is less than 10, the elemental weakness is the
  // same as the round number of the value of the word. If the value is
  // 10, the weakness corresponds to an element that has yet to appear in
  // the fight.
  //
  // Round 11 does not have a clue, but that doesn't matter, because
  // using the appropriate hobopolis 120 mp spell wins the fight on round
  // 10 every time. (Confirmed).

  public static Element[] ElementalWeakness = new Element[11];

  public static Element weakness(final int round) {
    return (round < 1 || round > 10) ? Element.NONE : ElementalWeakness[round];
  }

  private static Element unusedElement() {
    boolean hot = false;
    boolean cold = false;
    boolean stench = false;
    boolean spooky = false;
    boolean sleaze = false;
    boolean physical = false;
    for (int i = 1; i < 10; ++i) {
      switch (ElementalWeakness[i]) {
        case HOT -> hot = true;
        case COLD -> cold = true;
        case STENCH -> stench = true;
        case SPOOKY -> spooky = true;
        case SLEAZE -> sleaze = true;
        case PHYSICAL -> physical = true;
      }
    }

    return !hot
        ? Element.HOT
        : !cold
            ? Element.COLD
            : !stench
                ? Element.STENCH
                : !spooky
                    ? Element.SPOOKY
                    : !sleaze ? Element.SLEAZE : !physical ? Element.PHYSICAL : Element.NONE;
  }

  public static boolean solve(final String responseText) {
    // Give us a response text, at least!
    if (responseText == null) {
      return false;
    }

    // Extract the clues from the text
    Matcher matcher = CLUE_PATTERN.matcher(responseText);
    if (!matcher.find()) {
      return false;
    }

    // Initialize the array of elemental weaknesses
    Arrays.fill(ElementalWeakness, Element.NONE);

    // Now parse the clues and fill in the weaknesses
    ElementalWeakness[1] = DadManager.wordToElement(matcher.group(1), DadManager.WORD1);
    ElementalWeakness[2] = DadManager.wordToElement(matcher.group(2), DadManager.WORD2);
    ElementalWeakness[3] = DadManager.wordToElement(matcher.group(3), DadManager.WORD3);
    ElementalWeakness[4] = DadManager.wordToElement(matcher.group(5), DadManager.WORD5);
    ElementalWeakness[5] = DadManager.wordToElement(matcher.group(4), DadManager.WORD4);

    int word7 = StringUtilities.parseInt(matcher.group(7));
    Element[] elements = new Element[6];
    int index = 0;
    if ((word7 & 32) != 0) {
      elements[index++] = Element.PHYSICAL;
    }
    if ((word7 & 16) != 0) {
      elements[index++] = Element.SLEAZE;
    }
    if ((word7 & 8) != 0) {
      elements[index++] = Element.SPOOKY;
    }
    if ((word7 & 4) != 0) {
      elements[index++] = Element.STENCH;
    }
    if ((word7 & 2) != 0) {
      elements[index++] = Element.COLD;
    }
    if ((word7 & 1) != 0) {
      elements[index++] = Element.HOT;
    }

    // If there was only one bit set, the same element was added in twice
    // If that element is PHYSICAL, none are set since we didn't check for bit 64
    if (index == 0) {
      elements[0] = elements[1] = Element.PHYSICAL;
    }
    if (index == 1) {
      elements[0] = elements[1] = DadManager.intToElement(elements[0].ordinal() - 1);
    }

    boolean reverse = matcher.group(6).equalsIgnoreCase("suddenly");
    ElementalWeakness[6] = reverse ? elements[1] : elements[0];
    ElementalWeakness[7] = reverse ? elements[0] : elements[1];

    int word8 = DadManager.wordToInteger(matcher.group(8), DadManager.WORD8) + 2;
    int val8 = word8 - ElementalWeakness[1].ordinal();
    ElementalWeakness[8] = DadManager.intToElement(val8);

    int word9 = StringUtilities.parseInt(matcher.group(9));
    int val9 =
        ElementalWeakness[2].ordinal()
            + ElementalWeakness[3].ordinal()
            + ElementalWeakness[4].ordinal()
            + ElementalWeakness[5].ordinal()
            + 4
            - word9;
    ElementalWeakness[9] = DadManager.intToElement(val9);

    int word10 = DadManager.wordToInteger(matcher.group(10), DadManager.WORD10) + 1;
    ElementalWeakness[10] =
        word10 < 1
            ? Element.NONE
            : word10 < 10 ? ElementalWeakness[word10] : DadManager.unusedElement();

    return true;
  }
}
