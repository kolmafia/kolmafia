package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MonsterDataTest {

  @Nested
  class Attributes {
    @Test
    public void canNormalizeAttribiuteString() {
      String name = "scary monster";
      String attributes = "Def: 10 HP: 10 Atk: 10";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals("Atk: 10 Def: 10 HP: 10", normalized);
    }
  }
}
