package net.sourceforge.kolmafia.textui;

import java.util.Objects;

public class Range {
  private Position start;
  private Position end;

  protected Range() {}

  public Range(final Position start, final Position end) {
    this.start = start;
    this.end = end;
  }

  public Position getStart() {
    return this.start;
  }

  protected void setStart(final Position start) {
    this.start = start;
  }

  public Position getEnd() {
    return this.end;
  }

  protected void setEnd(final Position end) {
    this.end = end;
  }

  public boolean contains(final Range other) {
    return this.contains(other.start) && this.contains(other.end);
  }

  public boolean contains(final Position position) {
    return this.start.equals(position)
        || this.start.isBefore(position)
            && (this.end.equals(position) || position.isBefore(this.end));
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }

    final Range other = (Range) obj;
    return Objects.equals(this.start, other.start) && Objects.equals(this.end, other.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.start, this.end);
  }

  @Override
  public String toString() {
    return "Range[start=" + this.start + ", end=" + this.end + "]";
  }
}
