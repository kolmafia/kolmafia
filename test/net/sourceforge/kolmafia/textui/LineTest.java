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

  private Line line1;
  private Line line2;
  private Line line3;
  private Line endOfFile;

  private Token token1;
  private Token token2;
  private Token token3;
  private Token token4;
  private Token token5;
  private Token token6;
  private Token token7;

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

    line1 = new Line(commandStream);
    line2 = new Line(commandStream, line1);
    line3 = new Line(commandStream, line2);
    endOfFile = new Line(commandStream, line3);

    token1 = line1.makeToken(4);
    token2 = line1.makeComment(3);
    token3 = line1.makeToken(4);
    line1Tokens = Arrays.asList(token1, token2, token3);

    line2Tokens = Arrays.asList();

    token4 = line3.makeToken(4);
    token5 = line3.makeToken(3);
    token6 = line3.makeToken(8);
    token7 = line3.makeToken(7);
    line3Tokens = Arrays.asList(token4, token5, token6, token7);

    endOfFileTokens = Arrays.asList();

    allTokens = Arrays.asList(line1Tokens, line2Tokens, line3Tokens, endOfFileTokens);
  }

  @Test
  public void testLineNumbers() {
    // Incremental, 1-indexed
    assertEquals(1, line1.lineNumber);
    assertEquals(2, line2.lineNumber);
    assertEquals(3, line3.lineNumber);
    assertEquals(line3.lineNumber, endOfFile.lineNumber);
  }

  @Test
  public void testLineOffsets() {
    // Nmb of whitespace before the content, if the line is not all but whitespace
    assertEquals(1, line1.offset);
    assertEquals(0, line2.offset);
    assertEquals(5, line3.offset);
    assertEquals(line3.offset, endOfFile.offset);
  }

  @Test
  public void testLineContents() {
    // Trimmed line content
    assertEquals("Lorem ipsum", line1.content);
    assertEquals("", line2.content);
    assertEquals("Mary had a little lamb...", line3.content);
    assertNull(endOfFile.content);
  }

  @Test
  public void testLineToString() {
    assertEquals(line1.content, line1.toString());
    assertEquals(line2.content, line2.toString());
    assertEquals(line3.content, line3.toString());
    assertEquals(endOfFile.content, endOfFile.toString());
  }

  @Test
  public void testLineSubstring() {
    // The value of Line.offset is subtracted from beginIndex before being applied
    assertEquals(line1.content, line1.substring(line1.offset));
    assertEquals(line2.content, line2.substring(line2.offset));
    assertEquals(line3.content, line3.substring(line3.offset));
    assertEquals("", endOfFile.substring(endOfFile.offset));
  }

  @Test
  public void testLineRelations() {
    assertNull(line1.previousLine);
    assertSame(line1.nextLine, line2);

    assertSame(line2.previousLine, line1);
    assertSame(line2.nextLine, line3);

    assertSame(line3.previousLine, line2);
    assertSame(line3.nextLine, endOfFile);

    assertSame(endOfFile.previousLine, line3);
    assertNull(endOfFile.nextLine);
  }

  @Test
  public void testHasTokens() {
    assertTrue(line1.hasTokens());
    assertFalse(line2.hasTokens());
    assertTrue(line3.hasTokens());
    assertFalse(endOfFile.hasTokens());
  }

  @Test
  public void testGetLastToken() {
    assertSame(token3, line1.getLastToken());
    assertThrowsExactly(NoSuchElementException.class, line2::getLastToken);
    assertSame(token7, line3.getLastToken());
    assertThrowsExactly(NoSuchElementException.class, endOfFile::getLastToken);
  }

  @Test
  public void testGetTokensIterator() {
    assertIterableEquals(line1Tokens, line1.getTokensIterator());
    assertIterableEquals(line2Tokens, line2.getTokensIterator());
    assertIterableEquals(line3Tokens, line3.getTokensIterator());
    assertIterableEquals(endOfFileTokens, endOfFile.getTokensIterator());
  }

  @Test
  public void testLineTokenRelation() {
    for (Token token : line1Tokens) {
      assertSame(line1, token.getLine());
    }
    for (Token token : line2Tokens) {
      assertSame(line2, token.getLine());
    }
    for (Token token : line3Tokens) {
      assertSame(line3, token.getLine());
    }
    for (Token token : endOfFileTokens) {
      assertSame(endOfFile, token.getLine());
    }
  }

  @Test
  public void testTokenContent() {
    assertEquals("Lore", token1.content);
    assertEquals("m i", token2.content);
    assertEquals("psum", token3.content);

    assertEquals("Mary", token4.content);
    assertEquals("had", token5.content);
    assertEquals("a little", token6.content);
    assertEquals("lamb...", token7.content);
  }

  @Test
  public void testFollowingWhitespace() {
    assertEquals("", token1.followingWhitespace);
    assertEquals("", token2.followingWhitespace);
    assertEquals("", token3.followingWhitespace);

    assertEquals(" ", token4.followingWhitespace);
    assertEquals(" ", token5.followingWhitespace);
    assertEquals(" ", token6.followingWhitespace);
    assertEquals("", token7.followingWhitespace);
  }

  @Test
  public void testTokenOffset() {
    assertEquals(line1.offset, token1.offset);
    assertEquals(token1.restOfLineStart, token2.offset);
    assertEquals(token2.restOfLineStart, token3.offset);

    assertEquals(line3.offset, token4.offset);
    assertEquals(token4.restOfLineStart, token5.offset);
    assertEquals(token5.restOfLineStart, token6.offset);
    assertEquals(token6.restOfLineStart, token7.offset);
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

  @Test
  public void testRemoveLastToken() {
    // Split "a little" "lamb..." into "a little" "lamb" "..."
    assertSame(token7, line3.removeLastToken());
    assertSame(token6, line3.removeLastToken());

    token6 = line3.makeToken(8);
    token7 = line3.makeToken(4);
    Token token8 = line3.makeToken(3);
    line3Tokens = Arrays.asList(token4, token5, token6, token7, token8);

    // Re-do the tests
    assertSame(token8, line3.getLastToken());

    testGetTokensIterator();
    testLineRelations();

    assertEquals("Mary", token4.content);
    assertEquals("had", token5.content);
    assertEquals("a little", token6.content);
    assertEquals("lamb", token7.content);
    assertEquals("...", token8.content);

    assertEquals(" ", token4.followingWhitespace);
    assertEquals(" ", token5.followingWhitespace);
    assertEquals(" ", token6.followingWhitespace);
    assertEquals("", token7.followingWhitespace);
    assertEquals("", token8.followingWhitespace);

    assertEquals(line3.offset, token4.offset);
    assertEquals(token4.restOfLineStart, token5.offset);
    assertEquals(token5.restOfLineStart, token6.offset);
    assertEquals(token6.restOfLineStart, token7.offset);
    assertEquals(token7.restOfLineStart, token8.offset);

    testRestOfLineStart();
    testTokenToString();
    testTokenLength();
    testTokenEquals();
    testTokenEqualsIgnoreCase();
    testTokenEndsWith();
    testTokenSubstring1();
    testTokenSubstring2();
  }
}
