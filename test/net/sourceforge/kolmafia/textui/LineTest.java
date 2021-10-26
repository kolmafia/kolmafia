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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LineTest {
  private static final String scriptData =
      "\ufeffLorem ipsum\n      \r\n  \t\t\tMary had a little lamb...\t \t  ";

  private Line line1BOM;
  private Line line2Empty;
  private Line line3SurroundingWhitespace;
  private Line endOfFile;

  private Token line1Token1;
  private Token line1Token2;
  private Token line1Token3;
  private Token line3Token1;
  private Token line3Token2;
  private Token line3Token3;
  private Token line3Token4;

  private List<Token> line1Tokens;
  private List<Token> line2Tokens;
  private List<Token> line3Tokens;
  private List<Token> endOfFileTokens;

  private List<List<Token>> allTokens;

  @BeforeEach
  public void prepare() {
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
    line3Token4 = line3SurroundingWhitespace.makeToken(7);
    line3Tokens = Arrays.asList(line3Token1, line3Token2, line3Token3, line3Token4);

    endOfFileTokens = Arrays.asList();

    allTokens = Arrays.asList(line1Tokens, line2Tokens, line3Tokens, endOfFileTokens);
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
    assertEquals(line3SurroundingWhitespace.offset, endOfFile.offset);
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
    assertEquals(line1BOM.content, line1BOM.substring(line1BOM.offset));
    assertEquals(line2Empty.content, line2Empty.substring(line2Empty.offset));
    assertEquals(
        line3SurroundingWhitespace.content,
        line3SurroundingWhitespace.substring(line3SurroundingWhitespace.offset));
    assertEquals("", endOfFile.substring(endOfFile.offset));
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
    assertFalse(endOfFile.hasTokens());
  }

  @Test
  public void testGetLastToken() {
    assertSame(line1Token3, line1BOM.getLastToken());
    assertThrowsExactly(NoSuchElementException.class, line2Empty::getLastToken);
    assertSame(line3Token4, line3SurroundingWhitespace.getLastToken());
    assertThrowsExactly(NoSuchElementException.class, endOfFile::getLastToken);
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
  }

  @Test
  public void testTokenOffset() {
    assertEquals(line1BOM.offset, line1Token1.offset);
    assertEquals(line1Token1.restOfLineStart, line1Token2.offset);
    assertEquals(line1Token2.restOfLineStart, line1Token3.offset);

    assertEquals(line3SurroundingWhitespace.offset, line3Token1.offset);
    assertEquals(line3Token1.restOfLineStart, line3Token2.offset);
    assertEquals(line3Token2.restOfLineStart, line3Token3.offset);
    assertEquals(line3Token3.restOfLineStart, line3Token4.offset);
  }

  @Test
  public void testRestOfLineStart() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(
            token.offset + token.content.length() + token.followingWhitespace.length(),
            token.restOfLineStart);
      }
    }
  }

  @Test
  public void testTokenToString() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(token.content, token.toString());
      }
    }
  }

  @Test
  public void testTokenLength() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(token.content.length(), token.length());
      }
    }
  }

  @Test
  public void testTokenEquals() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(token.content.equals("Lore"), token.equals("Lore"));
      }
    }
  }

  @Test
  public void testTokenEqualsIgnoreCase() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(token.content.equalsIgnoreCase("mary"), token.equalsIgnoreCase("mary"));
      }
    }
  }

  @Test
  public void testTokenEndsWith() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        assertEquals(token.content.endsWith("little"), token.endsWith("little"));
      }
    }
  }

  @Test
  public void testTokenSubstring1() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        if (token.content.length() < 2) {
          assertThrowsExactly(IndexOutOfBoundsException.class, () -> token.substring(2));
        } else {
          assertEquals(token.content.substring(2), token.substring(2));
        }
      }
    }
  }

  @Test
  public void testTokenSubstring2() {
    for (List<Token> tokenList : allTokens) {
      for (Token token : tokenList) {
        if (token.content.length() < 3) {
          assertThrowsExactly(IndexOutOfBoundsException.class, () -> token.substring(1, 3));
        } else {
          assertEquals(token.content.substring(1, 3), token.substring(1, 3));
        }
      }
    }
  }

  /** Split "a little" "lamb..." into "a little" "lamb" "..." */
  @Test
  public void testRemoveLastToken() {
    assertSame(line3Token4, line3SurroundingWhitespace.removeLastToken());
    assertSame(line3Token3, line3SurroundingWhitespace.removeLastToken());

    line3Token3 = line3SurroundingWhitespace.makeToken(8);
    line3Token4 = line3SurroundingWhitespace.makeToken(4);
    Token line3Token5 = line3SurroundingWhitespace.makeToken(3);
    line3Tokens = Arrays.asList(line3Token1, line3Token2, line3Token3, line3Token4, line3Token5);

    // Re-do the tests
    assertSame(line3Token5, line3SurroundingWhitespace.getLastToken());

    testGetTokensIterator();
    testLineRelations();

    assertEquals("Mary", line3Token1.content);
    assertEquals("had", line3Token2.content);
    assertEquals("a little", line3Token3.content);
    assertEquals("lamb", line3Token4.content);
    assertEquals("...", line3Token5.content);

    assertEquals(" ", line3Token1.followingWhitespace);
    assertEquals(" ", line3Token2.followingWhitespace);
    assertEquals(" ", line3Token3.followingWhitespace);
    assertEquals("", line3Token4.followingWhitespace);
    assertEquals("", line3Token5.followingWhitespace);

    assertEquals(line3SurroundingWhitespace.offset, line3Token1.offset);
    assertEquals(line3Token1.restOfLineStart, line3Token2.offset);
    assertEquals(line3Token2.restOfLineStart, line3Token3.offset);
    assertEquals(line3Token3.restOfLineStart, line3Token4.offset);
    assertEquals(line3Token4.restOfLineStart, line3Token5.offset);

    testRestOfLineStart();
    testTokenToString();
    testTokenLength();
    testTokenEquals();
    testTokenEqualsIgnoreCase();
    testTokenEndsWith();
    testTokenSubstring1();
    testTokenSubstring2();
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
