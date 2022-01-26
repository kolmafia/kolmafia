import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import org.junit.jupiter.api.Test;

public class FamiliarDataTest {
  @Test
  public void canTellIfFamiliarIsTrainable() {
    var fam = new FamiliarData(FamiliarPool.MOSQUITO);
    assertTrue(fam.trainable());
  }

  @Test
  public void canTellIfFamiliarIsNotTrainable() {
    var fam = new FamiliarData(FamiliarPool.PET_ROCK);
    assertFalse(fam.trainable());
  }
}
