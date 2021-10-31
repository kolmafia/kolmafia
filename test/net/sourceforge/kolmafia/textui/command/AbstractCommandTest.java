package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractCommandTest {
  @BeforeEach
  public void initEach() {
    AbstractCommand.clear();
  }

  @Test
  void unregisteredCommandIsNotFound() {
    assertNull(AbstractCommand.lookup.get("fake"));
    assertNull(AbstractCommand.getSubstringMatch("fake"));
  }

  @Test
  void registeredCommandIsFound() {
    AbstractCommand command = new FakeCommand().register("fake");
    assertEquals(command, AbstractCommand.lookup.get("fake"));
    assertNull(AbstractCommand.getSubstringMatch("fake"));
  }

  @Test
  void registerSubstringFindsCommand() {
    AbstractCommand command = new FakeCommand().registerSubstring("fake");
    assertNull(AbstractCommand.lookup.get("fake"));
    assertEquals(command, AbstractCommand.getSubstringMatch("fake"));
    assertEquals(command, AbstractCommand.getSubstringMatch("morefake"));
  }
}

class FakeCommand extends AbstractCommand {

  @Override
  public void run(String cmd, String parameters) {
    KoLmafia.updateDisplay(MafiaState.CONTINUE, "Fake command was run");
  }
}
