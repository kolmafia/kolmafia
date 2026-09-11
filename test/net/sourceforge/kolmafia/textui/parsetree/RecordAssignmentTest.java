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
  void storeRecordSameType() {
    String output =
        runAsh(
            """
        record f {string file; string name; int line;};
        f source = new f('a','b',3);
        f dest = source;
        print(dest.name + dest.line + ';done');
        """);
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordSameFieldsSameName() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record {string file; string name; int line;} dest = new f('a','b',3);
            print(dest.name + dest.line + ';done');
            """);
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordSameFieldsDifferentName() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record g {string file; string name; int line;};
            f source = new f('a','b',3);
            g dest = source;
            print(dest.name + dest.line + ';done');
            """);
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordSameFieldsFromFunction() {
    String output =
        runAsh(
            """
            record {string file; string name; int line;} dest = get_stack_trace()[0];
            print('done');
            """);
    assertThat(output, containsString("done"));
  }

  // Fewer fields: the destination record keeps a subset of the source record's fields.

  @Test
  void storeRecordFewerFields() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record g {string name; int line;};
            f source = new f('a','b',3);
            g dest = source;
            print(dest.name + dest.line + ';done');
            """);
    assertThat(output, containsString("b3;done"));
  }

  @Test
  void storeRecordFewerFieldsRearranged() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line; boolean george;};
            record g {string name; boolean george; int line;};
            f source = new f('a','b',3,true);
            g dest = source;
            print(dest.name + dest.line + dest.george + ';done');
            """);
    assertThat(output, containsString("b3true;done"));
  }

  @Test
  void storeRecordFewerFieldsFromFunction() {
    String output =
        runAsh(
            """
            record {string name; int line;} dest = get_stack_trace()[0];
            print('done');
            """);
    assertThat(output, containsString("done"));
  }

  @Test
  void storeRecordSingleField() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record g {int line;};
            f source = new f('a','b',77);
            g dest = source;
            print(dest.line + ';done');
            """);
    assertThat(output, containsString("77;done"));
  }

  @Test
  void storeRecordFewerFieldsPlainAssignment() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record g {string name; int line;};
            f source = new f('a','b',3);
            g dest;
            dest = source;
            print(dest.name + dest.line + ';done');
            """);
    assertThat(output, containsString("b3;done"));
  }

  // Fields are matched by name, ignoring their order in the record definitions.

  @Test
  void storeRecordSameFieldsDifferentOrder() {
    String output =
        runAsh(
            """
            record f {string file; string name; int line;};
            record g {string name; int line; string file;};
            f source = new f('a','b',3);
            g dest = source;
            print(dest.name + dest.line + dest.file + ';done');
            """);
    assertThat(output, containsString("b3a;done"));
  }

  // Destination records that are not a subset of the source still fail to parse.

  @Test
  void storeRecordExtraFieldsDestinationFails() {
    String output =
        runAsh(
            """
            record f {string name;};
            record g {string name; int line;};
            f source = new f('a');
            g dest = source;
            print('FAIL');
            """);
    assertThat(output, containsString("Cannot store"));
  }

  @Test
  void storeRecordIncompatibleFieldTypeFails() {
    String output =
        runAsh(
            """
            record f {string name;};
            record g {int name;};
            f source = new f('a');
            g dest = source;
            print('FAIL');
            """);
    assertThat(output, containsString("Cannot store"));
  }

  // Fields can be coerced using the same rules as straight assignment

  @Test
  void storeRecordCoerceFloatToInt() {
    String output =
        runAsh(
            """
            record f {float item;};
            record {int item;} dest = new f(3.0);
            print(dest.item + ';done');
            """);
    assertThat(output, containsString("3;done"));
  }

  @Test
  void storeRecordCoerceIntToFloat() {
    String output =
        runAsh(
            """
        record f {int item;};
        record {float item;} dest = new f(3);
        print(dest.item + ';done');
        """);
    assertThat(output, containsString("3.0;done"));
  }

  @Test
  void storeRecordCoerceIntToString() {
    String output =
        runAsh(
            """
        record f {int item;};
        record {string item;} dest = new f(3);
        print(dest.item + ';done');
        """);
    assertThat(output, containsString("3;done"));
  }

  // Records containing records work if the contained record is a subset

  @Test
  void storeRecordContainingRecord() {
    String output =
        runAsh(
            """
          record d {string name; string otherstuff;};
          record e {string name;};
          record f {d innerrec;};
          record g {e innerrec;};
          f source = new f(new d('a', 'b'));
          g dest = source;
          print(dest.innerrec.name + ';done');
          """);
    assertThat(output, containsString("a;done"));
  }

  // Arrays and maps can contain records as expected

  @Test
  void storeRecordInArray() {
    String output =
        runAsh(
            """
          record f {string name; string otherstuff;};
          record g {string name;};
          g[2] garray;
          f source = new f('a', 'b');
          g dest = new g('a');
          garray[0] = source;
          garray[1] = dest;
          print(garray[0].name + ';done');
          """);
    assertThat(output, containsString("a;done"));
  }

  @Test
  void storeRecordInMap() {
    String output =
        runAsh(
            """
          record f {string name; string otherstuff;};
          record g {string name;};
          g[int] gmap;
          f source = new f('a', 'b');
          g dest = new g('a');
          gmap[0] = source;
          gmap[1] = dest;
          print(gmap[0].name + ';done');
          """);
    assertThat(output, containsString("a;done"));
  }
}
