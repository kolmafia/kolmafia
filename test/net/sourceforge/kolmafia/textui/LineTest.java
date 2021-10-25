package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.textui.Line.Token;
import org.junit.jupiter.api.Test;

public class LineTest {
  private static final String scriptData =
      "\ufeffLorem ipsum\n      \r\n  			Mary had a little lamb...	 	  ";

  @Test
  public void testLines() {
    LineNumberReader commandStream =
        new LineNumberReader(
            new InputStreamReader(
                new ByteArrayInputStream(scriptData.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8));

    Line line1 = new Line(commandStream);
    assertNotNull(line1.content);
    Line line2 = new Line(commandStream, line1);
    assertNotNull(line2.content);
    Line line3 = new Line(commandStream, line2);
    assertNotNull(line3.content);
    Line endOfFile = new Line(commandStream, line3);
    assertNull(endOfFile.content);

    // Basic information
    assertEquals(1, line1.lineNumber);
    assertEquals(2, line2.lineNumber);
    assertEquals(3, line3.lineNumber);
    assertEquals(line3.lineNumber, endOfFile.lineNumber);

    assertEquals(1, line1.offset);
    assertEquals(0, line2.offset);
    assertEquals(5, line3.offset);
    assertEquals(line3.offset, endOfFile.offset);

    assertEquals("Lorem ipsum", line1.content);
    assertEquals("", line2.content);
    assertEquals("Mary had a little lamb...", line3.content);

    // Relation between the lines
    Line[] lines = {line1, line2, line3, endOfFile};
    for (int i = 0; i < lines.length; ++i) {
      Line line = lines[i];

      if (i == 0) {
        assertNull(line.previousLine);
      } else {
        assertSame(lines[i - 1], line.previousLine);
      }

      if (i == lines.length - 1) {
        assertNull(line.nextLine);
      } else {
        assertSame(lines[i + 1], line.nextLine);
      }
    }

    // Tokens
    Token token1 = line1.makeToken(4);
    Token token2 = line1.makeComment(3);
    Token token3 = line1.makeToken(4);
    List<Token> line1Tokens = Arrays.asList(token1, token2, token3);

    Token token4 = line3.makeToken(4);
    Token token5 = line3.makeToken(3);
    Token token6 = line3.makeToken(8);
    Token token7 = line3.makeToken(7);
    Token token8;
    List<Token> line3Tokens = Arrays.asList(token4, token5, token6, token7);

    assertTrue(line1.hasTokens());
    assertFalse(line2.hasTokens());
    assertTrue(line3.hasTokens());
    assertFalse(endOfFile.hasTokens());

    assertSame(token3, line1.getLastToken());
    assertSame(token7, line3.getLastToken());

    assertIterableEquals(line1Tokens, line1.getTokensIterator());
    assertIterableEquals(line3Tokens, line3.getTokensIterator());

    for (Token token : line1Tokens) {
      assertSame(line1, token.getLine());
    }
    for (Token token : line3Tokens) {
      assertSame(line3, token.getLine());
    }

    assertTrue(token1.equals("Lore"));
    assertEquals("", token1.followingWhitespace);
    assertEquals(1, token1.offset);
    assertEquals(5, token1.restOfLineStart);
    assertTrue(token2.equals("m i"));
    assertEquals("", token2.followingWhitespace);
    assertEquals(5, token2.offset);
    assertEquals(8, token2.restOfLineStart);
    assertTrue(token3.equals("psum"));
    assertEquals("", token3.followingWhitespace);
    assertEquals(8, token3.offset);
    assertEquals(12, token3.restOfLineStart);

    assertTrue(token4.equals("Mary"));
    assertEquals(" ", token4.followingWhitespace);
    assertEquals(5, token4.offset);
    assertEquals(10, token4.restOfLineStart);
    assertTrue(token5.equals("had"));
    assertEquals(" ", token5.followingWhitespace);
    assertEquals(10, token5.offset);
    assertEquals(14, token5.restOfLineStart);
    assertTrue(token6.equals("a little"));
    assertEquals(" ", token6.followingWhitespace);
    assertEquals(14, token6.offset);
    assertEquals(23, token6.restOfLineStart);
    assertTrue(token7.equals("lamb..."));
    assertEquals("", token7.followingWhitespace);
    assertEquals(23, token7.offset);
    assertEquals(30, token7.restOfLineStart);

    // removeLastToken
    // Split "a little" "lamb..." into "a little" "lamb" "..."
    assertSame(token7, line3.removeLastToken());
    assertSame(token6, line3.removeLastToken());

    token6 = line3.makeToken(8);
    token7 = line3.makeToken(4);
    token8 = line3.makeToken(3);
    line3Tokens = Arrays.asList(token4, token5, token6, token7, token8);

    assertSame(token8, line3.getLastToken());

    assertIterableEquals(line3Tokens, line3.getTokensIterator());

    for (Token token : line3Tokens) {
      assertSame(line3, token.getLine());
    }

    assertTrue(token4.equals("Mary"));
    assertEquals(" ", token4.followingWhitespace);
    assertEquals(5, token4.offset);
    assertEquals(10, token4.restOfLineStart);
    assertTrue(token5.equals("had"));
    assertEquals(" ", token5.followingWhitespace);
    assertEquals(10, token5.offset);
    assertEquals(14, token5.restOfLineStart);
    assertTrue(token6.equals("a little"));
    assertEquals(" ", token6.followingWhitespace);
    assertEquals(14, token6.offset);
    assertEquals(23, token6.restOfLineStart);
    assertTrue(token7.equals("lamb"));
    assertEquals("", token7.followingWhitespace);
    assertEquals(23, token7.offset);
    assertEquals(27, token7.restOfLineStart);
    assertTrue(token8.equals("..."));
    assertEquals("", token8.followingWhitespace);
    assertEquals(27, token8.offset);
    assertEquals(30, token8.restOfLineStart);
  }
}
