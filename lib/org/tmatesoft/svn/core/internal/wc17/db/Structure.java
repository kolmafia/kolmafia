package org.tmatesoft.svn.core.internal.wc17.db;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Structure<T extends Enum<T>> {

    public interface TypeSafety {
        Class<?> getType();
    }
    
    private static final StructuresPool globalPool = new StructuresPool();
    
    private static final Object LONG_MARKER = Long.TYPE;
    private static final Object BOOLEAN_MARKER = Boolean.TYPE;

    @SuppressWarnings("unchecked")
    public static <X extends Structure<T>, T extends Enum<T>> X obtain(Class<T> e, Enum<T>... fields) {
        assert e != null && e.isEnum();
        return (X) globalPool.obtain(e, fields);
    }

    @SuppressWarnings("unchecked")
    public static <X extends Structure<T>, T extends Enum<T>> X obtain(Class<T> e) {
        assert e != null && e.isEnum();
        return (X) globalPool.obtain(e);
    }
    
    private static void release(Structure<?> e) {
        if (e != null) {
            globalPool.release(e);
        }
    }
    
    private Class<?> enumClass;
    private long requestedFields;
    private Object[] nonPrimitiveValues;
    private long[] longValues;
    private T[] copySource;
    
    private Structure(Class<T> enumClass, Enum<T>... fields) {
        init(enumClass, fields);
    }
    
    public long lng(T e) {
        assert e.getClass() == enumClass;
        assertSafeType(e, Long.TYPE);
        if (nonPrimitiveValues[e.ordinal()] == LONG_MARKER) {
            return longValues[e.ordinal()];
        }
        assert false;
        return 0;
    }
    
    public String text(T e) {
        assert e.getClass() == enumClass;        
        assertSafeType(e, String.class);
        return (String) nonPrimitiveValues[e.ordinal()];
    }
    
    public boolean is(T e) {
        assert e.getClass() == enumClass;
        assertSafeType(e, Boolean.TYPE);
        if (nonPrimitiveValues[e.ordinal()] == BOOLEAN_MARKER) {
            return longValues[e.ordinal()] != 0;
        }
        assert false;
        return false;
    }
    
    public boolean hasValue(Enum<?> e) {
        assert e.getClass() == enumClass;
        return nonPrimitiveValues[e.ordinal()] != null;
    }
    
    @SuppressWarnings("unchecked")
    public <X> X get(T e) {
        assert e.getClass() == enumClass;
        Class<?> expectedType = null;
        if (e instanceof TypeSafety) {
            expectedType = ((TypeSafety) e).getType(); 
        }
        Object value = nonPrimitiveValues[e.ordinal()];
        if (value == null) {
            return null;
        } 
        assert value != LONG_MARKER && value != BOOLEAN_MARKER;
        if (expectedType != null) {
            assert expectedType.isAssignableFrom(value.getClass());
        }
        return (X) value;
    }
    
    public void set(T x, Object v) {
        if (v == null) {
            unset(x);
            return;
        }
        if (x instanceof TypeSafety) {
            assert v == LONG_MARKER || v == BOOLEAN_MARKER || v == null || ((TypeSafety) x).getType().isAssignableFrom(v.getClass());
        }
        nonPrimitiveValues[x.ordinal()] = v;
    }
    
    public void unset(T x) {
        if ((requestedFields & (1 << x.ordinal())) == 0) {
            return;
        }
        nonPrimitiveValues[x.ordinal()] = null;
        longValues[x.ordinal()] = 0;
    }

    public void set(T x, long v) {        
        if (x instanceof TypeSafety) {
            assert ((TypeSafety) x).getType() == Long.TYPE;
        }
        longValues[x.ordinal()] = v;
        set(x, LONG_MARKER);
    }
    
    public void set(T x, boolean v) {        
        if (x instanceof TypeSafety) {
            assert ((TypeSafety) x).getType() == Boolean.TYPE;
        }
        longValues[x.ordinal()] = v ? 1 : 0;
        set(x, BOOLEAN_MARKER);
    }
    
    public boolean hasField(Enum<T> field) {
        return (requestedFields & (1 << field.ordinal())) != 0;
    }
    
    public void clear() {
        copySource = null;
        
        Arrays.fill(nonPrimitiveValues, null);
        Arrays.fill(longValues, 0);
    }
    
    public void release() {
        release(this);
    }
    
    public Structure<T> from(T... fields) {
        assert copySource == null;
        assert fields != null;
        copySource = fields;
        return this;
    }
    
    public <X extends Enum<X>> void into(Structure<X> target, X... fields) {
        assert copySource != null;
        assert fields != null;
        assert fields.length == copySource.length;
        assert target != null;
        
        try {
            for (int i = 0; i < copySource.length; i++) {
                int valueIndex = copySource[i].ordinal();
                Object v = nonPrimitiveValues[copySource[i].ordinal()];
                if (v == BOOLEAN_MARKER) {
                    target.set(fields[i], longValues[valueIndex] != 0);
                } else if (v == LONG_MARKER) {
                    target.set(fields[i], longValues[valueIndex]);
                } else if (v == null) {
                    target.unset(fields[i]);
                } else {
                    target.set(fields[i], v);
                }
            }
        } finally {
            copySource = null;
        }
    }
    
    public int hashCode() {
        int code = enumClass.hashCode();
        for (int i = 0; i < nonPrimitiveValues.length; i++) {
            if (nonPrimitiveValues != null && nonPrimitiveValues[i] != null) {
                code += 13*nonPrimitiveValues[i].hashCode();
            }
        }
        for (int i = 0; i < longValues.length; i++) {
            code += 17*longValues[i];            
        }
        return code;
    }
    
    public boolean equals(Object e) {
        if (e == null || e.getClass() != Structure.class) {
            return false;
        }
        Structure<?> other = (Structure<?>) e;
        if (other.enumClass == enumClass) {
            return Arrays.equals(other.nonPrimitiveValues, nonPrimitiveValues) &&
                Arrays.equals(longValues, other.longValues);
        }
        return false;
    }

    private void assertSafeType(T e, Class<?> c) {
        if (e instanceof TypeSafety) {
            assert ((TypeSafety) e).getType() == c;
        }
    }

    private void init(Class<?> enumClass, Enum<?>... fields) {
        this.enumClass = enumClass;
        
        if (fields != null && fields.length > 0) {
            requestedFields = 0;
            for (Enum<?> field : fields) {
                requestedFields |= (1 << field.ordinal());
            }
        } else {
            requestedFields = ~0;
        }
        
        Object[] enumConstants = enumClass.getEnumConstants();
        assert enumConstants != null;
        
        nonPrimitiveValues = adjustArraySize(nonPrimitiveValues, enumConstants.length);
        longValues = adjustArraySize(longValues, enumConstants.length);        
        
        clear();
    }
    
    private static Object[] adjustArraySize(Object[] array, int desiredSize) {
        if (array == null || array.length < desiredSize) {
            return new Object[desiredSize];
        }
        return array;
    }
    
    private static long[] adjustArraySize(long[] array, int desiredSize) {
        if (array == null || array.length < desiredSize) {
            return new long[desiredSize];
        }
        return array;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append(enumClass.getSimpleName());
        sb.append(">\n");
        for (Object field : enumClass.getEnumConstants()) {
            @SuppressWarnings("unchecked")
            T e = (T) field;
            Object o = nonPrimitiveValues[e.ordinal()];
            if (o != null) {
                sb.append(e.name());
                sb.append(" = ");
                if (o == LONG_MARKER) {
                    sb.append(Long.toString(lng(e)));
                } else if (o == BOOLEAN_MARKER) {
                    sb.append(is(e));
                    
                } else {
                    sb.append(o);
                    
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
    
    private static class StructuresPool {
        
        private BlockingQueue<Structure<?>> objectsQueues = new LinkedBlockingQueue<Structure<?>>(23);
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Structure<?> obtain(Class<?> enumClass, Enum<?>... fields) {
            Structure<?> t = objectsQueues.poll();
            if (t == null) {
                t = new Structure(enumClass, fields);
            } else {
                t.init(enumClass, fields);
            }
            return t;
        }
        
        public void release(Structure<?> t) {
            if (!objectsQueues.contains(t)) {
                objectsQueues.offer(t);
            } 
        }        
    }
}
