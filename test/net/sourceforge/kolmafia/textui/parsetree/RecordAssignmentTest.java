package net.sourceforge.kolmafia.textui.parsetree;

import static internal.helpers.RequestLoggerOutput.startStream;
import static internal.helpers.RequestLoggerOutput.stopStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.AshRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordAssignmentTest {
  @BeforeEach
  void reset() {
    KoLCharacter.reset("RecordAssignmentTest");
    Preferences.reset("RecordAssignmentTest");
  }

  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  private static String runAsh(String source) {
    startStream();
    try {
      var istream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
      AshRuntime interpreter = new AshRuntime();
      interpreter.validate(null, istream);
      interpreter.execute("main", null);
      return stopStream();
    } catch (Throwable t) {
      stopStream();
      throw t;
    }
  }

  // Same fields, matching name and order.

  @Test
  void storeRecordSameFieldsSameName() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record {string file; string name; int line;} dest = new f('a','b',3);\n"
                + "print(dest.name + dest.line + ';done');\n");
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordSameFieldsDifferentName() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record g {string file; string name; int line;};\n"
                + "f source = new f('a','b',3);\n"
                + "g dest = source;\n"
                + "print(dest.name + dest.line + ';done');\n");
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordSameFieldsFromFunction() {
    String output =
        runAsh(
            "record {string file; string name; int line;} dest = get_stack_trace()[0];\n"
                + "print('done');\n");
    assertThat(output, containsString("done"));
  }

  // Fewer fields: the destination record keeps a subset of the source record's fields.

  @Test
  void storeRecordFewerFields() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record g {string name; int line;};\n"
                + "f source = new f('a','b',3);\n"
                + "g dest = source;\n"
                + "print(dest.name + dest.line + ';done');\n");
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordFewerFieldsRearranged() {
    String output =
        runAsh(
            "record f {string file; string name; int line; boolean george;};\n"
                + "record g {string name; boolean george; int line;};\n"
                + "f source = new f('a','b',3,true);\n"
                + "g dest = source;\n"
                + "print(dest.name + dest.line + dest.george + ';done');\n");
    assertThat(output, containsString("b3true;done"));
  }

  @Test
  void storeRecordFewerFieldsFromFunction() {
    String output =
        runAsh(
            "record {string name; int line;} dest = get_stack_trace()[0];\n" + "print('done');\n");
    assertThat(output, containsString("done"));
  }

  @Test
  void storeRecordSingleField() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record g {int line;};\n"
                + "f source = new f('a','b',77);\n"
                + "g dest = source;\n"
                + "print(dest.line + ';done');\n");
    assertThat(output, containsString("77;done"));
  }

  @Test
  void storeRecordFewerFieldsPlainAssignment() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record g {string name; int line;};\n"
                + "f source = new f('a','b',3);\n"
                + "g dest;\n"
                + "dest = source;\n"
                + "print(dest.name + dest.line + ';done');\n");
    assertThat(output, containsString("b3;done"));
  }

  // Fields are matched by name, ignoring their order in the record definitions.

  @Test
  void storeRecordSameFieldsDifferentOrder() {
    String output =
        runAsh(
            "record f {string file; string name; int line;};\n"
                + "record g {string name; int line; string file;};\n"
                + "f source = new f('a','b',3);\n"
                + "g dest = source;\n"
                + "print(dest.name + dest.line + dest.file + ';done');\n");
    assertThat(output, containsString("b3a;done"));
  }

  // Destination records that are not a subset of the source still fail to parse.

  @Test
  void storeRecordExtraFieldsDestinationFails() {
    String output =
        runAsh(
            "record f {string name;};\n"
                + "record g {string name; int line;};\n"
                + "f source = new f('a');\n"
                + "g dest = source;\n"
                + "print('FAIL');\n");
    assertThat(output, containsString("Cannot store"));
  }

  @Test
  void storeRecordIncompatibleFieldTypeFails() {
    String output =
        runAsh(
            "record f {string name;};\n"
                + "record g {int name;};\n"
                + "f source = new f('a');\n"
                + "g dest = source;\n"
                + "print('FAIL');\n");
    assertThat(output, containsString("Cannot store"));
  }

  // Fields can be coerced using the same rules as straight assignment

  @Test
  void storeRecordCoerceFloatToInt() {
    String output =
        runAsh(
            "record f {float item;};\n"
                + "record {int item;} dest = new f(3.0);\n"
                + "print(dest.item + ';done');\n");
    assertThat(output, containsString("3;done"));
  }

  @Test
  void storeRecordCoerceIntToFloat() {
    String output =
        runAsh(
            "record f {int item;};\n"
                + "record {float item;} dest = new f(3);\n"
                + "print(dest.item + ';done');\n");
    assertThat(output, containsString("3.0;done"));
  }

  @Test
  void storeRecordCoerceIntToString() {
    String output =
        runAsh(
            "record f {int item;};\n"
                + "record {string item;} dest = new f(3);\n"
                + "print(dest.item + ';done');\n");
    assertThat(output, containsString("3;done"));
  }
}
