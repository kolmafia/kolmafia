package net.sourceforge.kolmafia.textui;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamespaceInterpreterTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("NamespaceInterpreterTest");
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "b", "c", "z"})
  void reloadsWhenAnyDirectOrTransitiveImportChanges(String changed) throws IOException {
    Path directory = Files.createTempDirectory(KoLConstants.SCRIPT_LOCATION.toPath(), "namespace-");
    Path first = directory.resolve("a.ash");
    Path dependency = directory.resolve("b.ash");
    Path parent = directory.resolve("c.ash");
    Path last = directory.resolve("z.ash");
    String dependencyImport = "import <" + dependency.toAbsolutePath() + ">;\n";
    try {
      Files.writeString(first, "int value_a() { return 1; }\n");
      Files.writeString(dependency, "int value_b() { return 1; }\n");
      Files.writeString(parent, dependencyImport + "int value_c() { return 1; }\n");
      Files.writeString(last, "int value_z() { return 1; }\n");
      String namespace =
          first.toAbsolutePath() + "," + parent.toAbsolutePath() + "," + last.toAbsolutePath();
      try (var cleanups = new Cleanups(withProperty("commandLineNamespace", namespace))) {
        var interpreter = new NamespaceInterpreter();
        String function = "value_" + changed;
        assertThat(interpreter.execute(function, null).intValue(), is(1L));

        Path edited = directory.resolve(changed + ".ash");
        long timestamp = Files.getLastModifiedTime(edited).toMillis();
        String imports = changed.equals("c") ? dependencyImport : "";
        Files.writeString(edited, imports + "int " + function + "() { return 2; }\n");
        // Ensure a distinct timestamp even on filesystems with coarse time resolution.
        Files.setLastModifiedTime(edited, FileTime.fromMillis(timestamp + 2000));

        assertThat(interpreter.execute(function, null).intValue(), is(2L));
      }
    } finally {
      Files.deleteIfExists(first);
      Files.deleteIfExists(dependency);
      Files.deleteIfExists(parent);
      Files.deleteIfExists(last);
      Files.deleteIfExists(directory);
    }
  }
}
