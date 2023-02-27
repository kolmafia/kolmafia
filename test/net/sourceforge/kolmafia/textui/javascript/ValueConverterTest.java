package net.sourceforge.kolmafia.textui.javascript;

import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class ValueConverterTest {
     Context cx = new Context();
     Scriptable scope = new Scriptable() {
          @Override
          public String getClassName() {
               return null;
          }

          @Override
          public Object get( String name, Scriptable start ) {
               return null;
          }

          @Override
          public Object get( int index, Scriptable start ) {
               return null;
          }

          @Override
          public boolean has( String name, Scriptable start ) {
               return false;
          }

          @Override
          public boolean has( int index, Scriptable start ) {
               return false;
          }

          @Override
          public void put( String name, Scriptable start, Object value ) {

          }

          @Override
          public void put( int index, Scriptable start, Object value ) {

          }

          @Override
          public void delete( String name ) {

          }

          @Override
          public void delete( int index ) {

          }

          @Override
          public Scriptable getPrototype() {
               return null;
          }

          @Override
          public void setPrototype( Scriptable prototype ) {

          }

          @Override
          public Scriptable getParentScope() {
               return null;
          }

          @Override
          public void setParentScope( Scriptable parent ) {

          }

          @Override
          public Object[] getIds() {
               return new Object[0];
          }

          @Override
          public Object getDefaultValue( Class<?> hint ) {
               return null;
          }

          @Override
          public boolean hasInstance( Scriptable instance ) {
               return false;
          }
     };
     @Test
     void itHandlesBigIntegersCorrectly() {
          Value longVal = new Value(Integer.MAX_VALUE -1);
          Calendar timestamp = new GregorianCalendar();
          Value bigVal = new Value(timestamp.getTimeInMillis());

          ValueConverter vc = new ValueConverter( cx, scope );
          Assertions.assertInstanceOf(Long.class, vc.asJava( longVal ), longVal + " should be a Long");
          Assertions.assertInstanceOf( BigInteger.class, vc.asJava( bigVal ), longVal + " should be a BigInteger");

     }
}
