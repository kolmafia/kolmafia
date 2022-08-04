package net.sourceforge.kolmafia.textui.command;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static internal.helpers.Player.withSkill;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MirrorCommandTest extends AbstractCommandTestBase {

    public MirrorCommandTest() {
        this.command = "mirror";
    }

    @BeforeEach
    public void stopMirrorLogging() {
        execute("");
    }

    private String getMirrorLog(String fileName) {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = FileUtilities.getReader(new File(KoLConstants.ROOT_LOCATION, "chats/" + fileName))) {
            String line;

            // Read the full file to a string
            while ((line = FileUtilities.readLine(reader)) != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }

                builder.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    @Test
    public void testMirrorWritesHTML() {
        // Open the mirror
        execute("chats/test_writes_html.html");

        // When `> ` is used as a prefix, the RequestLogger will colorize it assuming its a command
        // input.
        RequestLogger.printLine("> Fake command input");

        // Log another line, this should be raw.
        RequestLogger.printLine("Raw Line");

        // Close the mirror
        execute("");

        String mirrorOutput = getMirrorLog("test_writes_html.html");

        assertTrue(mirrorOutput.contains("<font color=olive>> Fake command input</font><br>"));
        assertTrue(mirrorOutput.contains("Raw Line<br>"));
    }

    @Test
    public void testMirrorClose() {
        execute("chats/test_mirror_closes");

        // Write to the mirror log
        RequestLogger.printLine("Some input");

        // Close the mirror
        execute("");

        // Write to the command buffer again
        RequestLogger.printLine("Should not be seen");

        // Now read the log and verify that it was not written
        String mirrorOutput = getMirrorLog("test_mirror_closes.txt");

        // Test that the second line is not in the output
        assertFalse(mirrorOutput.contains("Should not be seen"));
    }

    @Test
    public void testMirrorListStrings() {
        execute("chats/test_mirror_list_strings");

        List<String> list = new ArrayList<>();
        list.add("Line 1");
        list.add("Line 2");

        RequestLogger.printList(list);

        // Close the mirror
        execute("");

        // Read the log
        String mirrorOutput = getMirrorLog("test_mirror_list_strings.txt");

        // Test that the output matches as expected
        assertEquals("Line 1<br>Line 2<br></pre>", mirrorOutput);
    }

    @Test
    public void testMirrorListSkills() {
        try (var cleanups = new Cleanups(withSkill(1), withSkill(2))) {
            execute("chats/test_mirror_list_skills");

            // Available skills is special in that it will print html
            RequestLogger.printList(KoLConstants.availableSkills);

            // Close the mirror
            execute("");

            // Now read the log and verify that it contains html
            String mirrorOutput = getMirrorLog("test_mirror_list_skills.txt");

            // Test that the output matches the expected html
            assertEquals("<u><b>Uncategorized</b></u><br><a onClick=\"javascript:skill(2);\">Chronic Indigestion</a><br><a onClick=\"javascript:skill(1);" +
                "\">Liver of Steel</a><br>", mirrorOutput);
        }
    }

    @Test
    public void testMultipleMirrorsOpened() {
        execute("chats/mirror_1");

        // Write to the mirror log
        RequestLogger.printLine("Mirror 1 Log");

        // Switch mirrors to mirror_2
        execute("chats/mirror_2");

        // Write to the mirror log
        RequestLogger.printLine("Mirror 2 Log");

        // Close the mirror
        execute("");

        // Behavior is that if the mirror has already been opened, that it will stay opened and the new
        // mirror requests are effectively ignored.

        // Assert that mirror_1 only contains both messages
        assertTrue(getMirrorLog("mirror_1.txt").contains("Mirror 1 Log"));
        assertTrue(getMirrorLog("mirror_1.txt").contains("Mirror 2 Log"));

        // Assert that mirror_2 contains neither messages
        assertFalse(getMirrorLog("mirror_2.txt").contains("Mirror 1 Log"));
        assertFalse(getMirrorLog("mirror_2.txt").contains("Mirror 2 Log"));
    }

    @ParameterizedTest
    @CsvSource({"file,file.txt", "file.html,file.html", "file.htm,file.htm", "file.txt,file.txt", "file.csv,file.csv.txt"})
    public void testMirrorFileNames(String mirrorName, String fileName) {
        execute("chats/" + mirrorName);

        RequestLogger.printLine("Filler Line");

        execute("");

        assertTrue(getMirrorLog(fileName).contains("Filler Line"));
    }
}
