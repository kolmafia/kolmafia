package net.sourceforge.kolmafia.textui;

public record Location(String uri, Range range) {
  public String getUri() {
    return this.uri;
  }

  public Range getRange() {
    return this.range;
  }
}
