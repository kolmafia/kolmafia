package net.sourceforge.kolmafia.combat;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

public class CombatEncounterKey {
  private static final Pattern ELEMENT_PATTERN = Pattern.compile("\\s*\\$element\\[([^\\]]+)\\]");
  private static final Pattern PHYLUM_PATTERN = Pattern.compile("\\s*\\$phylum\\[([^\\]]+)\\]");
  private static final Pattern ITEM_PATTERN = Pattern.compile("\\s*\\$item\\[([^\\]]+)\\]");

  private final String encounterKey;
  private String monsterName;

  private Element element;
  private Phylum phylum;
  private int itemId;

  public CombatEncounterKey(String encounterKey) {
    this.encounterKey = encounterKey.trim();
    this.monsterName = this.encounterKey;

    this.element = Element.NONE;
    this.phylum = Phylum.NONE;
    this.itemId = -1;

    Matcher elementMatcher = ELEMENT_PATTERN.matcher(this.monsterName);

    if (elementMatcher.find()) {
      String elementName = elementMatcher.group(1);

      this.element = MonsterDatabase.stringToElement(elementName);
    }

    this.monsterName = elementMatcher.replaceAll("").trim();

    Matcher phylumMatcher = PHYLUM_PATTERN.matcher(this.monsterName);

    if (phylumMatcher.find()) {
      String phylumName = phylumMatcher.group(1);

      this.phylum = Phylum.find(phylumName);
    }

    this.monsterName = phylumMatcher.replaceAll("").trim();

    Matcher itemMatcher = ITEM_PATTERN.matcher(this.monsterName);

    if (itemMatcher.find()) {
      String itemName = itemMatcher.group(1);

      this.itemId = ItemDatabase.getItemId(itemName);
    }

    this.monsterName = itemMatcher.replaceAll("").trim();
  }

  public boolean matches(String monsterName, MonsterData monsterData) {
    if (monsterData != null) {
      if (this.element != Element.NONE && monsterData.getDefenseElement() != this.element) {
        return false;
      }

      if (this.phylum != Phylum.NONE && monsterData.getPhylum() != this.phylum) {
        return false;
      }

      if (this.itemId != -1) {
        boolean foundItem = false;

        Iterator itemIterator = monsterData.getItems().iterator();

        while (!foundItem && itemIterator.hasNext()) {
          AdventureResult item = (AdventureResult) itemIterator.next();

          if (item.getItemId() == this.itemId) {
            foundItem = true;
          }
        }

        if (!foundItem) {
          return false;
        }
      }
    }

    if (this.monsterName.equals("")) {
      return true;
    }

    return monsterName.indexOf(this.monsterName) != -1;
  }

  @Override
  public String toString() {
    return encounterKey;
  }
}
