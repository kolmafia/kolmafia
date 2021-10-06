package net.sourceforge.kolmafia.textui.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AnsiSerializerTest {
  @Test
  public void itShouldSerializeSimpleColoredTexts() {
    String html = "<font color=\"red\">Some red text</font>";
    String expected = "\u001B[38;2;255;0;0mSome red text\u001B[39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeUnquotedColorNames() {
    String html = "<font color=red>Some red text</font>";
    String expected = "\u001B[38;2;255;0;0mSome red text\u001B[39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeMultipleFontTags() {
    String html =
        "<font color=\"red\">Some red text</font> and <font color=\"blue\">some blue text</font>";
    String expected =
        "\u001B[38;2;255;0;0mSome red text\u001B[39m and \u001B[38;2;0;0;255msome blue text\u001B[39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeMultipleFontTagsWithTrailingText() {
    String html =
        "<font color=\"red\">Some red text</font> and <font color=\"blue\">some blue text</font> plus some trailing text";
    String expected =
        "\u001B[38;2;255;0;0mSome red text\u001B[39m and \u001B[38;2;0;0;255msome blue text\u001B[39m plus some trailing text\u001B[m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeNestedFontTags() {
    String html =
        "<font color=\"red\">Some red text that contains <font color=\"blue\">some blue text</font></font>";
    String expected =
        "\u001B[38;2;255;0;0mSome red text that contains \u001B[38;2;0;0;255msome blue text\u001B[38;2;255;0;0;39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeNestedFontTagsWithTrailingText() {
    String html =
        "<font color=\"red\">Some red text that contains <font color=\"blue\">some blue text</font> plus</font> some trailing text";
    String expected =
        "\u001B[38;2;255;0;0mSome red text that contains \u001B[38;2;0;0;255msome blue text\u001B[38;2;255;0;0m plus\u001B[39m some trailing text\u001B[m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeSimpleAttributes() {
    String html =
        "<font color=\"seagreen\">some <b>bold</b>, <i>italic</i>, <u>underlined</u> and <s>striked out</s> text</font>";
    String expected =
        "\u001B[38;2;46;139;87msome \u001B[1mbold\u001B[22m, \u001B[3mitalic\u001B[23m, \u001B[4munderlined\u001B[24m and \u001B[9mstriked out\u001B[29m text\u001B[39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldSerializeNestedSimpleAttributes() {
    String html =
        "some <b>bold and <i>italic and <u>underlined and <s>striked out</s></u></i></b> text";
    String expected =
        "some \u001B[1mbold and \u001B[3mitalic and \u001B[4munderlined and \u001B[9mstriked out\u001B[29;24;23;22m text\u001B[m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldRenderAllOtherTagsInGray1() {
    String html = "<font color=\"red\">Some <span>red<span> text</font>";
    String expected =
        "\u001B[38;2;255;0;0mSome \u001B[38;2;50;50;50m<span>\u001B[38;2;255;0;0mred\u001B[38;2;50;50;50m<span>\u001B[38;2;255;0;0m text\u001B[38;2;50;50;50m</span>\u001B[38;2;255;0;0;38;2;50;50;50m</span>\u001B[38;2;255;0;0;39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }

  @Test
  public void itShouldRenderAllOtherTagsInGray2() {
    String html = "<span>Some <span style=\"color:red\">red<span> text";
    String expected =
        "\u001B[38;2;50;50;50m<span>\u001B[39mSome \u001B[38;2;50;50;50m<span style=\"color:red\">\u001B[39mred\u001B[38;2;50;50;50m<span>\u001B[39m text\u001B[38;2;50;50;50m</span>\u001B[39;38;2;50;50;50m</span>\u001B[39;38;2;50;50;50m</span>\u001B[39;0m";
    String actual = AnsiSerializer.serializeHtml(html);
    assertEquals(expected, actual);
  }
}
