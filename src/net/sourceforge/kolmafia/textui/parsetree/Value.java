package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import org.eclipse.lsp4j.Location;
import org.json.JSONException;

/**
 * A concrete value, either computed as a result of executing a {@link Command} or created
 * artificially.
 *
 * <p>Is forbidden from interacting with {@link Parser} other than through {@link Constant}. See it
 * as some sort of... hazmat suit..?
 */
public class Value implements TypedNode, Comparable<Value> {
  public static final Value BAD_VALUE = new Value(new Type.BadType(null, null));

  public Type type;

  public long contentLong = 0;
  public String contentString = null;
  public Object content = null;

  public Value() {
    this.type = DataTypes.VOID_TYPE;
  }

  public Value(final long value) {
    this.type = DataTypes.INT_TYPE;
    this.contentLong = value;
  }

  public Value(final boolean value) {
    this.type = DataTypes.BOOLEAN_TYPE;
    this.contentLong = value ? 1 : 0;
  }

  public Value(final String value) {
    this.type = DataTypes.STRING_TYPE;
    this.contentString = value == null ? "" : value;
  }

  public Value(final double value) {
    this.type = DataTypes.FLOAT_TYPE;
    this.contentLong = Double.doubleToRawLongBits(value);
  }

  public Value(final Type type) {
    this.type = type;
  }

  public Value(final Type type, final String contentString) {
    this.type = type;
    this.contentString = contentString;
  }

  public Value(final Type type, final long contentLong, final String contentString) {
    this.type = type;
    this.contentLong = contentLong;
    this.contentString = contentString;
  }

  public Value(final Type type, final String contentString, final Object content) {
    this.type = type;
    this.contentString = contentString;
    this.content = content;
  }

  public Value(
      final Type type, final long contentLong, final String contentString, final Object content) {
    this.type = type;
    this.contentLong = contentLong;
    this.contentString = contentString;
    this.content = content;
  }

  public Value(final Value original) {
    this.type = original.type;
    this.contentLong = original.contentLong;
    this.contentString = original.contentString;
    this.content = original.content;
  }

  public Value toFloatValue() {
    if (this.getType().equals(DataTypes.TYPE_FLOAT)) {
      return this;
    }
    return DataTypes.makeFloatValue((double) this.contentLong);
  }

  public Value toIntValue() {
    if (this.getType().equals(DataTypes.TYPE_INT)) {
      return this;
    }
    if (this.getType().equals(DataTypes.TYPE_BOOLEAN)) {
      return DataTypes.makeIntValue(this.contentLong != 0);
    }
    return DataTypes.makeIntValue((long) this.floatValue());
  }

  public Value toBooleanValue() {
    if (this.getType().equals(DataTypes.TYPE_BOOLEAN)) {
      return this;
    }
    return DataTypes.makeBooleanValue(this.contentLong != 0);
  }

  @Override
  public Type getType() {
    return this.type.getBaseType();
  }

  @Override
  public Type getRawType() {
    return this.type;
  }

  @Override
  public String toString() {
    if (this.content instanceof StringBuffer) {
      return ((StringBuffer) this.content).toString();
    }

    if (this.getType().equals(DataTypes.TYPE_VOID)) {
      return "void";
    }

    if (this.contentString != null) {
      return this.contentString;
    }

    if (this.getType().equals(DataTypes.TYPE_BOOLEAN)) {
      return String.valueOf(this.contentLong != 0);
    }

    if (this.getType().equals(DataTypes.TYPE_FLOAT)) {
      return KoLConstants.NONSCIENTIFIC_FORMAT.format(this.floatValue());
    }

    return String.valueOf(this.contentLong);
  }

  public String toQuotedString() {
    if (this.contentString != null) {
      return "\"" + this.contentString + "\"";
    }
    return this.toString();
  }

  public Value toStringValue() {
    return new Value(this.toString());
  }

  public Object rawValue() {
    return this.content;
  }

  public long intValue() {
    if (this.getType().equals(DataTypes.TYPE_FLOAT)) {
      return (long) Double.longBitsToDouble(this.contentLong);
    }
    return this.contentLong;
  }

  public double floatValue() {
    if (!this.getType().equals(DataTypes.TYPE_FLOAT)) {
      return (double) this.contentLong;
    }
    return Double.longBitsToDouble(this.contentLong);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return this;
  }

  public Value asProxy() {
    if (this.getType().equals(DataTypes.CLASS_TYPE)) {
      return new ProxyRecordValue.ClassProxy(this);
    }
    if (this.getType().equals(DataTypes.ITEM_TYPE)) {
      return new ProxyRecordValue.ItemProxy(this);
    }
    if (this.getType().equals(DataTypes.FAMILIAR_TYPE)) {
      return new ProxyRecordValue.FamiliarProxy(this);
    }
    if (this.getType().equals(DataTypes.SKILL_TYPE)) {
      return new ProxyRecordValue.SkillProxy(this);
    }
    if (this.getType().equals(DataTypes.EFFECT_TYPE)) {
      return new ProxyRecordValue.EffectProxy(this);
    }
    if (this.getType().equals(DataTypes.LOCATION_TYPE)) {
      return new ProxyRecordValue.LocationProxy(this);
    }
    if (this.getType().equals(DataTypes.MONSTER_TYPE)) {
      return new ProxyRecordValue.MonsterProxy(this);
    }
    if (this.getType().equals(DataTypes.COINMASTER_TYPE)) {
      return new ProxyRecordValue.CoinmasterProxy(this);
    }
    if (this.getType().equals(DataTypes.BOUNTY_TYPE)) {
      return new ProxyRecordValue.BountyProxy(this);
    }
    if (this.getType().equals(DataTypes.THRALL_TYPE)) {
      return new ProxyRecordValue.ThrallProxy(this);
    }
    if (this.getType().equals(DataTypes.SERVANT_TYPE)) {
      return new ProxyRecordValue.ServantProxy(this);
    }
    if (this.getType().equals(DataTypes.VYKEA_TYPE)) {
      return new ProxyRecordValue.VykeaProxy(this);
    }
    if (this.getType().equals(DataTypes.ELEMENT_TYPE)) {
      return new ProxyRecordValue.ElementProxy(this);
    }
    if (this.getType().equals(DataTypes.PHYLUM_TYPE)) {
      return new ProxyRecordValue.PhylumProxy(this);
    }
    if (this.getType().equals(DataTypes.STAT_TYPE)) {
      return new ProxyRecordValue.StatProxy(this);
    }
    if (this.getType().equals(DataTypes.SLOT_TYPE)) {
      return new ProxyRecordValue.SlotProxy(this);
    }
    return this;
  }

  /* null-safe version of the above */
  public static Value asProxy(Value value) {
    if (value == null) {
      return null;
    }
    return value.asProxy();
  }

  public static final Comparator<Value> ignoreCaseComparator =
      new Comparator<Value>() {
        @Override
        public int compare(Value v1, Value v2) {
          return v1.compareToIgnoreCase(v2);
        }
      };

  @Override
  public int compareTo(final Value o) {
    return this.compareTo(o, false);
  }

  public int compareToIgnoreCase(final Value o) {
    return this.compareTo(o, true);
  }

  private int compareTo(final Value o, final boolean ignoreCase) {
    if (!(o instanceof Value)) {
      throw new ClassCastException();
    }

    if (this.getType().equals(DataTypes.BOOLEAN_TYPE)
        || this.getType().equals(DataTypes.INT_TYPE)
        || this.getType().equals(DataTypes.ITEM_TYPE)
        || this.getType().equals(DataTypes.EFFECT_TYPE)
        || this.getType().equals(DataTypes.CLASS_TYPE)
        || this.getType().equals(DataTypes.SKILL_TYPE)
        || this.getType().equals(DataTypes.FAMILIAR_TYPE)
        || this.getType().equals(DataTypes.SLOT_TYPE)
        || this.getType().equals(DataTypes.THRALL_TYPE)
        || this.getType().equals(DataTypes.SERVANT_TYPE)) {
      return this.contentLong < o.contentLong ? -1 : this.contentLong == o.contentLong ? 0 : 1;
    }

    if (this.getType().equals(DataTypes.VYKEA_TYPE)) {
      // Let the underlying data type itself decide
      VYKEACompanionData v1 = (VYKEACompanionData) (this.content);
      VYKEACompanionData v2 = (VYKEACompanionData) (o.content);
      return v1.compareTo(v2);
    }

    if (this.getType().equals(DataTypes.FLOAT_TYPE)) {
      return Double.compare(
          Double.longBitsToDouble(this.contentLong), Double.longBitsToDouble(o.contentLong));
    }

    if (this.getType().equals(DataTypes.MONSTER_TYPE)) {
      // If we know a monster ID, compare it
      if (this.contentLong != 0 || o.contentLong != 0) {
        return this.contentLong < o.contentLong ? -1 : this.contentLong == o.contentLong ? 0 : 1;
      }
      // Otherwise, must compare names
    }

    if (this.contentString != null && o.contentString != null) {
      return ignoreCase
          ? this.contentString.compareToIgnoreCase(o.contentString)
          : this.contentString.compareTo(o.contentString);
    }

    return -1;
  }

  public int count() {
    return 1;
  }

  public void clear() {}

  public boolean contains(final Value index) {
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    return !(o instanceof Value) ? false : this.compareTo((Value) o) == 0;
  }

  @Override
  public int hashCode() {
    int hash;
    hash = this.getType() != null ? this.getType().hashCode() : 0;
    hash = hash + 31 * (int) this.contentLong;
    hash = hash + 31 * (this.contentString != null ? this.contentString.hashCode() : 0);
    return hash;
  }

  public static String escapeString(String string) {
    // Since map_to_file has one record per line with fields
    // delimited by tabs, string values cannot have newline or tab
    // characters in them. Escape those characters. And, since we
    // escape using backslashes, backslash must also be escaped.
    //
    // Replace backslashes with \\, newlines with \n, and tabs with \t

    int length = string.length();
    StringBuilder buffer = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = string.charAt(i);
      switch (c) {
        case '\n':
          buffer.append("\\n");
          break;
        case '\t':
          buffer.append("\\t");
          break;
        case '\\':
          buffer.append("\\\\");
          break;
        default:
          buffer.append(c);
          break;
      }
    }
    return buffer.toString();
  }

  public static String unEscapeString(String string) {
    int length = string.length();
    StringBuilder buffer = new StringBuilder(length);
    boolean saw_backslash = false;

    for (int i = 0; i < length; i++) {
      char c = string.charAt(i);
      if (!saw_backslash) {
        if (c == '\\') {
          saw_backslash = true;
        } else {
          buffer.append(c);
        }
        continue;
      }

      switch (c) {
        case 'n':
          buffer.append('\n');
          break;
        case 't':
          buffer.append('\t');
          break;
        default:
          buffer.append(c);
          break;
      }

      saw_backslash = false;
    }

    if (saw_backslash) {
      buffer.append('\\');
    }

    return buffer.toString();
  }

  public static Value readValue(
      final Type type, final String string, final String filename, final int line) {
    int tnum = type.getType();
    if (tnum == DataTypes.TYPE_STRING) {
      return new Value(Value.unEscapeString(string));
    }

    Value value = type.parseValue(string, true);

    // Validate data and report errors
    List<String> names = type.getAmbiguousNames(string, value, false);
    if (names != null && names.size() > 1) {
      String message =
          "Multiple matches for "
              + string
              + "; using "
              + value.toString()
              + " in "
              + Parser.getLineAndFile(filename, line)
              + ". Clarify by using one of:";
      RequestLogger.printLine(message);
      for (String str : names) {
        RequestLogger.printLine(str);
      }
    }

    return value;
  }

  public String dumpValue() {
    int type = this.getType().getType();
    return type == DataTypes.TYPE_STRING
        ? Value.escapeString(this.contentString)
        : type == DataTypes.TYPE_ITEM
            ? "[" + this.contentLong + "]" + ItemDatabase.getDataName((int) this.contentLong)
            : type == DataTypes.TYPE_EFFECT
                ? "["
                    + this.contentLong
                    + "]"
                    + EffectDatabase.getEffectName((int) this.contentLong)
                : type == DataTypes.TYPE_MONSTER && this.contentLong != 0
                    ? "["
                        + this.contentLong
                        + "]"
                        + MonsterDatabase.getMonsterName((int) this.contentLong)
                    : type == DataTypes.TYPE_SKILL
                        ? "["
                            + this.contentLong
                            + "]"
                            + SkillDatabase.getSkillName((int) this.contentLong)
                        : this.toString();
  }

  public void dumpValue(final PrintStream writer) {
    writer.print(this.dumpValue());
  }

  public void dump(final PrintStream writer, final String prefix, final boolean compact) {
    writer.println(prefix + this.dumpValue());
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<VALUE " + this.getType() + " [" + this.toString() + "]>");
  }

  public Object toJSON() throws JSONException {
    if (this.getType().equals(DataTypes.TYPE_BOOLEAN)) {
      return Boolean.valueOf(this.contentLong > 0);
    } else if (this.getType().equals(DataTypes.TYPE_INT)) {
      return Long.valueOf(this.contentLong);
    } else if (this.getType().equals(DataTypes.TYPE_FLOAT)) {
      return Double.valueOf(Double.longBitsToDouble(this.contentLong));
    } else {
      return this.toString();
    }
  }

  /**
   * Returns a {@link Constant} holding {@code value} and {@code location}.
   *
   * <p>If {@code value} is {@code null}, returns {@code null}.
   */
  public static final Evaluable locate(final Location location, final Value value) {
    if (value == null) {
      return null;
    }

    return new Constant(location, value);
  }

  /**
   * A specific {@link Value} to which is assigned a {@link Location}, that can be carried across
   * {@link Parser}.
   *
   * <p>{@link #value} is never {@code null}.
   */
  public static final class Constant extends Evaluable {
    public final Value value;

    private Constant(final Location location, final Value value) {
      super(location);
      this.value = value;
    }

    @Override
    public Type getType() {
      return this.value.getType();
    }

    @Override
    public Type getRawType() {
      return this.value.getRawType();
    }

    @Override
    public String toString() {
      return this.value.toString();
    }

    @Override
    public String toQuotedString() {
      return this.value.toQuotedString();
    }

    @Override
    public Value execute(final AshRuntime interpreter) {
      return this.value.execute(interpreter);
    }

    @Override
    public void print(final PrintStream stream, final int indent) {
      this.value.print(stream, indent);
    }
  }
}
