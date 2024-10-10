package net.sourceforge.kolmafia.textui.javascript;

import java.util.Calendar;
import java.util.GregorianCalendar;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ScriptableValueConverterTest {
  private static Context cx;
  private static Scriptable scope =
      new Scriptable() {
        @Override
        public String getClassName() {
          return null;
        }

        @Override
        public Object get(String name, Scriptable start) {
          return null;
        }

        @Override
        public Object get(int index, Scriptable start) {
          return null;
        }

        @Override
        public boolean has(String name, Scriptable start) {
          return false;
        }

        @Override
        public boolean has(int index, Scriptable start) {
          return false;
        }

        @Override
        public void put(String name, Scriptable start, Object value) {}

        @Override
        public void put(int index, Scriptable start, Object value) {}

        @Override
        public void delete(String name) {}

        @Override
        public void delete(int index) {}

        @Override
        public Scriptable getPrototype() {
          return null;
        }

        @Override
        public void setPrototype(Scriptable prototype) {}

        @Override
        public Scriptable getParentScope() {
          return null;
        }

        @Override
        public void setParentScope(Scriptable parent) {}

        @Override
        public Object[] getIds() {
          return new Object[0];
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
          return null;
        }

        @Override
        public boolean hasInstance(Scriptable instance) {
          return false;
        }
      };

  @BeforeAll
  static void beforeAll() {
    cx = Context.enter();
  }

  @AfterAll
  static void afterAll() {
    Context.exit();
  }

  @Test
  void asJavaFromJavaConvertWithoutDataLoss() {
    Value longMaxInt = new Value(Integer.MAX_VALUE - 1);
    Calendar timestamp = new GregorianCalendar();
    Value nowTime = new Value(timestamp.getTimeInMillis());
    Value longMaxLong = new Value(Long.MAX_VALUE);
    Value boolTrue = new Value(true);
    Value floatValue = new Value(Float.MAX_VALUE);

    ScriptableValueConverter vc = new ScriptableValueConverter(cx, scope);
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(longMaxInt)),
        longMaxInt,
        longMaxInt + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(longMaxLong)),
        longMaxLong,
        longMaxLong + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(nowTime)), nowTime, nowTime + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(boolTrue)), boolTrue, boolTrue + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(floatValue)),
        floatValue,
        floatValue + " did not convert to/from Java");
  }
}
