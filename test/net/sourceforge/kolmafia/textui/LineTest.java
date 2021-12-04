package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import net.sourceforge.kolmafia.textui.Line.Token;
import org.junit.jupiter.api.Test;

public class LineTest {
  private static final String scriptData =
      "\ufeffLorem ipsum\n      \r\n  \t\t\tMary had a little lamb...\t \t  ";

  private final Line line1BOM;
  private final Line line2Empty;
  private final Line line3SurroundingWhitespace;
  private final Line endOfFile;

  private final Token line1Token1;
  private final Token line1Token2;
  private final Token line1Token3;
  private final Token line3Token1;
  private final Token line3Token2;
  private final Token line3Token3;
  private final Token line3Token4;
  private final Token endOfFileToken;

  private final List<Token> line1Tokens;
  private final List<Token> line2Tokens;
  private final List<Token> line3Tokens;
  private final List<Token> endOfFileTokens;

  private final List<List<Token>> allTokens;

  public LineTest() {
    LineNumberReader commandStream =
        new LineNumberReader(
            new InputStreamReader(
                new ByteArrayInputStream(scriptData.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8));

    line1BOM = new Line(commandStream);
    line2Empty = new Line(commandStream, line1BOM);
    line3SurroundingWhitespace = new Line(commandStream, line2Empty);
    endOfFile = new Line(commandStream, line3SurroundingWhitespace);

    line1Token1 = line1BOM.makeToken(4);
    line1Token2 = line1BOM.makeComment(3);
    line1Token3 = line1BOM.makeToken(4);
    line1Tokens = Arrays.asList(line1Token1, line1Token2, line1Token3);

    line2Tokens = Arrays.asList();

    line3Token1 = line3SurroundingWhitespace.makeToken(4);
    line3Token2 = line3SurroundingWhitespace.makeToken(3);
    line3Token3 = line3SurroundingWhitespace.makeToken(8);
    line3Token4 = line3SurroundingWhitespace.makeComment(7);
    line3Tokens = Arrays.asList(line3Token1, line3Token2, line3Token3, line3Token4);

    // Submitted length doesn't matter for EOF
    endOfFileToken = endOfFile.makeToken(1234);
    endOfFileTokens = Arrays.asList(endOfFileToken);

    allTokens = Arrays.asList(line1Tokens, line2Tokens, line3Tokens, endOfFileTokens);
  }

  /** Cannot make a token of length 0 or negative */
  @Test
  public void testInvalidLengthToken() {
    assertThrows(IllegalArgumentException.class, () -> line1BOM.makeToken(0));
    assertThrows(IllegalArgumentException.class, () -> line1BOM.makeComment(-1));
    assertThrows(IllegalArgumentException.class, () -> line1BOM.makeToken(-5));
  }

  /** Can only have a single "end of file" token */
  @Test
  public void testDuplicateEndOfFileToken() {
    assertThrows(IllegalStateException.class, () -> endOfFile.makeToken(0));
  }

  /** Incremental, 1-indexed */
  @Test
  public void testLineNumbers() {
    assertEquals(1, line1BOM.lineNumber);
    assertEquals(2, line2Empty.lineNumber);
    assertEquals(3, line3SurroundingWhitespace.lineNumber);
    assertEquals(line3SurroundingWhitespace.lineNumber, endOfFile.lineNumber);
  }

  /** Count of leading whitespace characters, if the line is not all but whitespace */
  @Test
  public void testLineOffsets() {
    assertEquals(1, line1BOM.offset);
    assertEquals(0, line2Empty.offset);
    assertEquals(5, line3SurroundingWhitespace.offset);
    assertEquals(
        line3SurroundingWhitespace.offset + line3SurroundingWhitespace.content.length(),
        endOfFile.offset);
  }

  /** Trimmed line content */
  @Test
  public void testLineContents() {
    assertEquals("Lorem ipsum", line1BOM.content);
    assertEquals("", line2Empty.content);
    assertEquals("Mary had a little lamb...", line3SurroundingWhitespace.content);
    assertNull(endOfFile.content);
  }

  @Test
  public void testLineToString() {
    assertEquals(line1BOM.content, line1BOM.toString());
    assertEquals(line2Empty.content, line2Empty.toString());
    assertEquals(line3SurroundingWhitespace.content, line3SurroundingWhitespace.toString());
    assertEquals(endOfFile.content, endOfFile.toString());
  }

  @Test
  public void testLineSubstring() {
    // The value of Line.offset is subtracted from beginIndex before being applied
    assertEquals(line1BOM.content.substring(3), line1BOM.substring(line1BOM.offset + 3));
    assertEquals(line2Empty.content, line2Empty.substring(line2Empty.offset));
    assertEquals(
        line3SurroundingWhitespace.content.substring(5),
        line3SurroundingWhitespace.substring(line3SurroundingWhitespace.offset + 5));
    assertEquals("", endOfFile.substring(endOfFile.offset)); // Always an empty string
  }

  @Test
  public void testLineInvalidSubstring() {
    // Line.substring cannot take a value lesser than its offset
    assertThrows(IndexOutOfBoundsException.class, () -> line1BOM.substring(line1BOM.offset - 1));
    // End of File being the exception
    assertDoesNotThrow(() -> endOfFile.substring(-1000));
  }

  @Test
  public void testLineRelations() {
    assertNull(line1BOM.previousLine);
    assertSame(line1BOM.nextLine, line2Empty);

    assertSame(line2Empty.previousLine, line1BOM);
    assertSame(line2Empty.nextLine, line3SurroundingWhitespace);

    assertSame(line3SurroundingWhitespace.previousLine, line2Empty);
    assertSame(line3SurroundingWhitespace.nextLine, endOfFile);

    assertSame(endOfFile.previousLine, line3SurroundingWhitespace);
    assertNull(endOfFile.nextLine);
  }

  @Test
  public void testHasTokens() {
    assertTrue(line1BOM.hasTokens());
    assertFalse(line2Empty.hasTokens());
    assertTrue(line3SurroundingWhitespace.hasTokens());
    assertTrue(endOfFile.hasTokens());
  }

  @Test
  public void testGetLastToken() {
    assertSame(line1Token3, line1BOM.getLastToken());
    assertThrowsExactly(NoSuchElementException.class, line2Empty::getLastToken);
    assertSame(line3Token4, line3SurroundingWhitespace.getLastToken());
    assertSame(endOfFileToken, endOfFile.getLastToken());
  }

  /** Diverts from getLastToken() in that it skips comments, and has access to previous lines */
  @Test
  public void testPeekLastToken() {
    assertSame(line1Token3, line1BOM.peekLastToken());
    assertSame(line1Token3, line2Empty.peekLastToken());
    assertSame(line3Token3, line3SurroundingWhitespace.peekLastToken());
    assertSame(endOfFileToken, endOfFile.peekLastToken());
  }

  /** peekPreviousToken(null) is the same as peekLastToken() */
  @Test
  public void testPeekPreviousToken1() {
    assertSame(line1BOM.peekLastToken(), line1BOM.peekPreviousToken(null));
    assertSame(line2Empty.peekLastToken(), line2Empty.peekPreviousToken(null));
    assertSame(
        line3SurroundingWhitespace.peekLastToken(),
        line3SurroundingWhitespace.peekPreviousToken(null));
    assertSame(endOfFile.peekLastToken(), endOfFile.peekPreviousToken(null));
  }

  /** returns null if submitted the very first token */
  @Test
  public void testPeekPreviousToken2() {
    assertEquals(null, line1BOM.peekPreviousToken(line1Token1));
  }

  @Test
  public void testPeekPreviousToken3() {
    assertSame(line1Token1, line1BOM.peekPreviousToken(line1Token2));
    assertSame(line1Token3, line3SurroundingWhitespace.peekPreviousToken(line3Token1));
    assertSame(line3Token1, line3SurroundingWhitespace.peekPreviousToken(line3Token2));
    assertSame(line3Token2, line3SurroundingWhitespace.peekPreviousToken(line3Token3));
    assertSame(line3Token3, line3SurroundingWhitespace.peekPreviousToken(line3Token4));
  }

  /** Comments are skipped */
  @Test
  public void testPeekPreviousToken4() {
    assertSame(line1Token1, line1BOM.peekPreviousToken(line1Token3));
    assertSame(line3Token3, endOfFile.peekPreviousToken(endOfFileToken));
  }

  /** Submitted tokens must come from the corresponding line */
  @Test
  public void testPeekPreviousToken5() {
    assertThrows(IllegalArgumentException.class, () -> endOfFile.peekPreviousToken(line3Token4));
  }

  @Test
  public void testGetTokensIterator() {
    assertIterableEquals(line1Tokens, line1BOM.getTokensIterator());
    assertIterableEquals(line2Tokens, line2Empty.getTokensIterator());
    assertIterableEquals(line3Tokens, line3SurroundingWhitespace.getTokensIterator());
    assertIterableEquals(endOfFileTokens, endOfFile.getTokensIterator());
  }

  @Test
  public void testLineTokenRelation() {
    for (Token token : line1Tokens) {
      assertSame(line1BOM, token.getLine());
    }
    for (Token token : line2Tokens) {
      assertSame(line2Empty, token.getLine());
    }
    for (Token token : line3Tokens) {
      assertSame(line3SurroundingWhitespace, token.getLine());
    }
    for (Token token : endOfFileTokens) {
      assertSame(endOfFile, token.getLine());
    }
  }

  @Test
  public void testTokenContent() {
    assertEquals("Lore", line1Token1.content);
    assertEquals("m i", line1Token2.content);
    assertEquals("psum", line1Token3.content);

    assertEquals("Mary", line3Token1.content);
    assertEquals("had", line3Token2.content);
    assertEquals("a little", line3Token3.content);
    assertEquals("lamb...", line3Token4.content);

    assertEquals(";", endOfFileToken.content);
  }

  @Test
  public void testFollowingWhitespace() {
    assertEquals("", line1Token1.followingWhitespace);
    assertEquals("", line1Token2.followingWhitespace);
    assertEquals("", line1Token3.followingWhitespace);

    assertEquals(" ", line3Token1.followingWhitespace);
    assertEquals(" ", line3Token2.followingWhitespace);
    assertEquals(" ", line3Token3.followingWhitespace);
    assertEquals("", line3Token4.followingWhitespace);

    assertEquals("", endOfFileToken.followingWhitespace);
  }

  @Test
  public void testTokenStartCharacter() {
    assertEquals(line1BOM.offset, line1Token1.getStart().getCharacter());
    assertEquals(line1Token1.restOfLineStart, line1Token2.getStart().getCharacter());
    assertEquals(line1Token2.restOfLineStart, line1Token3.getStart().getCharacter());

    assertEquals(line3SurroundingWhitespace.offset, line3Token1.getStart().getCharacter());
    assertEquals(line3Token1.restOfLineStart, line3Token2.getStart().getCharacter());
    assertEquals(line3Token2.restOfLineStart, line3Token3.getStart().getCharacter());
    assertEquals(line3Token3.restOfLineStart, line3Token4.getStart().getCharacter());

    assertEquals(
        line3SurroundingWhitespace.offset + line3SurroundingWhitespace.content.length(),
        endOfFileToken.getStart().getCharacter());
  }

  @Test
  public void testTokenEndCharacter() {
    assertEquals(
        line1Token1.getStart().getCharacter() + line1Token1.content.length(),
        line1Token1.getEnd().getCharacter());
    assertEquals(
        line1Token2.getStart().getCharacter() + line1Token2.content.length(),
        line1Token2.getEnd().getCharacter());
    assertEquals(
        line1Token3.getStart().getCharacter() + line1Token3.content.length(),
        line1Token3.getEnd().getCharacter());

    assertEquals(
        line3Token1.getStart().getCharacter() + line3Token1.content.length(),
        line3Token1.getEnd().getCharacter());
    assertEquals(
        line3Token2.getStart().getCharacter() + line3Token2.content.length(),
        line3Token2.getEnd().getCharacter());
    assertEquals(
        line3Token3.getStart().getCharacter() + line3Token3.content.length(),
        line3Token3.getEnd().getCharacter());
    assertEquals(
        line3Token4.getStart().getCharacter() + line3Token4.content.length(),
        line3Token4.getEnd().getCharacter());

    assertEquals(endOfFileToken.getStart().getCharacter(), endOfFileToken.getEnd().getCharacter());
  }

  /** Tokens' positions' line are 0-indexed version of the line they are on */
  @Test
  public void testTokenStartLine() {
    assertEquals(line1Token1.getLine().lineNumber - 1, line1Token1.getStart().getLine());
    assertEquals(line1Token2.getLine().lineNumber - 1, line1Token2.getStart().getLine());
    assertEquals(line1Token3.getLine().lineNumber - 1, line1Token3.getStart().getLine());

    assertEquals(line3Token1.getLine().lineNumber - 1, line3Token1.getStart().getLine());
    assertEquals(line3Token2.getLine().lineNumber - 1, line3Token2.getStart().getLine());
    assertEquals(line3Token3.getLine().lineNumber - 1, line3Token3.getStart().getLine());
    assertEquals(line3Token4.getLine().lineNumber - 1, line3Token4.getStart().getLine());

    assertEquals(endOfFileToken.getLine().lineNumber - 1, endOfFileToken.getStart().getLine());
  }

  /** Tokens are not allowed to span multiple lines */
  @Test
  public void testTokenSingleLine() {
    assertEquals(line1Token1.getStart().getLine(), line1Token1.getEnd().getLine());
    assertEquals(line1Token2.getStart().getLine(), line1Token2.getEnd().getLine());
    assertEquals(line1Token3.getStart().getLine(), line1Token3.getEnd().getLine());

    assertEquals(line3Token1.getStart().getLine(), line3Token1.getEnd().getLine());
    assertEquals(line3Token2.getStart().getLine(), line3Token2.getEnd().getLine());
    assertEquals(line3Token3.getStart().getLine(), line3Token3.getEnd().getLine());
    assertEquals(line3Token4.getStart().getLine(), line3Token4.getEnd().getLine());

    assertEquals(endOfFileToken.getStart().getLine(), endOfFileToken.getEnd().getLine());
  }

  @Test
  public void testRestOfLineStart() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        // restOfLineStart is undefined for the EOF token
        if (token != endOfFileToken) {
          assertEquals(
              token.getEnd().getCharacter() + token.followingWhitespace.length(),
              token.restOfLineStart,
              "Token "
                  + token
                  + "'s restOfLineStart doesn't equal its end character's position + the length of its following whitespace");
        }
      }
    }
  }

  @Test
  public void testTokenToString() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.content,
            token.toString(),
            "Token " + token + "'s .toString() didn't match its content");
      }
    }
  }

  @Test
  public void testTokenLength() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.content.length(),
            token.length(),
            "The behavior of Token " + token + "'s .length() didn't match its content's .length()");
      }
    }
  }

  @Test
  public void testTokenEquals() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.content.equals("Lore"),
            token.equals("Lore"),
            "The behavior of Token "
                + token
                + "'s .equals(String) didn't match its content's .equals(String)");
      }
    }
  }

  @Test
  public void testTokenEqualsIgnoreCase() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.content.equalsIgnoreCase("mary"),
            token.equalsIgnoreCase("mary"),
            "The behavior of Token "
                + token
                + "'s .equalsIgnoreCase(String) didn't match its content's .equalsIgnoreCase(String)");
      }
    }
  }

  @Test
  public void testTokenEndsWith() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.content.endsWith("little"),
            token.endsWith("little"),
            "The behavior of Token "
                + token
                + "'s .endsWith(String) didn't match its content's .endsWith(String)");
      }
    }
  }

  @Test
  public void testTokenSubstring1() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        if (token.content.length() < 2) {
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> token.substring(2),
              "The behavior of Token "
                  + token
                  + "'s .substring(int) didn't match its content's .substring(int)");
        } else {
          assertEquals(
              token.content.substring(2),
              token.substring(2),
              "The behavior of Token "
                  + token
                  + "'s .substring(int) didn't match its content's .substring(int)");
        }
      }
    }
  }

  @Test
  public void testTokenSubstring2() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        if (token.content.length() < 3) {
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> token.substring(1, 3),
              "The behavior of Token "
                  + token
                  + "'s .substring(int, int) didn't match its content's .substring(int, int)");
        } else {
          assertEquals(
              token.content.substring(1, 3),
              token.substring(1, 3),
              "The behavior of Token "
                  + token
                  + "'s .substring(int, int) didn't match its content's .substring(int, int)");
        }
      }
    }
  }

  @Test
  public void testRemoveLastToken() {
    assertSame(line3Token4, line3SurroundingWhitespace.removeLastToken());
    assertSame(line3Token3, line3SurroundingWhitespace.removeLastToken());
    assertSame(line3Token2, line3SurroundingWhitespace.removeLastToken());
    assertSame(line3Token1, line3SurroundingWhitespace.removeLastToken());
    assertThrowsExactly(NoSuchElementException.class, line3SurroundingWhitespace::removeLastToken);
  }

  @Test
  public void testTokenFromEmptyLine() {
    assertThrows(IndexOutOfBoundsException.class, () -> line2Empty.makeToken(1));
  }

  /** Remove line 3's last token, then try to make a new one, way bigger */
  @Test
  public void testTokenSizeOverflow() {
    assertSame(line3Token4, line3SurroundingWhitespace.removeLastToken());
    assertThrows(IndexOutOfBoundsException.class, () -> line3SurroundingWhitespace.makeToken(50));
  }
}
