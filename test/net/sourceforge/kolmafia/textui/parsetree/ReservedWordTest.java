package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ReservedWordTest {
  @BeforeAll
  public static void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "path",
            "int[string] path;",
            "Reserved word 'path' cannot be a variable name",
            "char 13 to char 17"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
