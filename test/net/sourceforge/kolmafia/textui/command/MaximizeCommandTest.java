package net.sourceforge.kolmafia.textui.command;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaximizeCommandTest extends AbstractCommandTestBase {
    public MaximizeCommandTest() {
        this.command = "maximize";
    }
    @BeforeAll
    public static void beforeAll() {
        KoLCharacter.reset("MaximizeCommandTest");
    }
    @Test
    public void placeholder() {
        String maxStr =
                "5item,meat,0.5initiative,0.1da 1000max,dr,0.5all res,1.5mainstat,-fumble,mox,0.4hp,0.2mp 1000max,3mp regen,0.25spell damage,1.75spell damage percent,2familiar weight,5familiar exp,10exp,5Mysticality experience percent,200combat 20max,+200bonus mafia thumb ring";
        var cleanups =
                new Cleanups(
                        withEquippableItem("candy cane sword cane"),
                        withEquippableItem("pasta spoon"),
                        withEquippableItem("Rain-Doh violet bo"),
                        withEquippableItem("Rain-Doh yellow laser gun"),
                        withEquippableItem("saucepan"),
                        withEquippableItem("toy accordion"),
                        withEquippableItem("turtle totem"),
                        withEquippableItem("psychic's crystal ball"),
                        withEquippableItem("Rain-Doh green lantern"),
                        withEquippableItem("stuffed baby gravy fairy"),
                        withEquippableItem("stuffed key"),
                        withEquippableItem("unbreakable umbrella (broken)"),
                        withStats(2, 27, 1),
                        withSkill(SkillPool.MASTER_OF_THE_SURPRISING_FIST));
        String out;
        try (cleanups) {
            out = execute(maxStr);
        }
        assertFalse(out.isEmpty());
        assertTrue(out.contains("Wielding candy cane sword cane..."));
        assertTrue(out.contains("Folding umbrella"));
        assertTrue(out.contains("Holding unbreakable umbrella..."));
    }
}
