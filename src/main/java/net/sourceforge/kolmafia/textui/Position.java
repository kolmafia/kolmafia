package net.sourceforge.kolmafia.textui;

public record Position(int line, int character) {
  public int getLine() {
    return this.line;
  }

  public int getCharacter() {
    return this.character;
  }

  public boolean isBefore(final Position other) {
    if (this.line != other.line) {
      return this.line < other.line;
    }

    return this.character < other.character;
  }
}
