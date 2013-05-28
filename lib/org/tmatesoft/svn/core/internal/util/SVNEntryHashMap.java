package org.tmatesoft.svn.core.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.wc.SVNObjectsPool;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SVNEntryHashMap extends SVNHashMap {
    
    private static final long serialVersionUID = 1L;    
    private static final Set<String> ourNonPoolableKeys = new HashSet<String>();
    private static final Set<String> ourURLKeys = new HashSet<String>();
    
    static {
        ourNonPoolableKeys.add(SVNProperty.CHECKSUM);
        ourNonPoolableKeys.add(SVNProperty.COMMITTED_DATE); 
        ourNonPoolableKeys.add(SVNProperty.TEXT_TIME);
        ourNonPoolableKeys.add(SVNProperty.WORKING_SIZE); 
        ourNonPoolableKeys.add(SVNProperty.LOCK_CREATION_DATE); 
        ourNonPoolableKeys.add(SVNProperty.PROP_TIME); 
        
        ourURLKeys.add(SVNProperty.URL); 
        ourURLKeys.add(SVNProperty.COPYFROM_URL); 
        ourURLKeys.add(SVNProperty.FILE_EXTERNAL_PATH);
        ourURLKeys.add(SVNProperty.REPOS); 
    }
    
    private SVNObjectsPool myObjectsPool;
    
    public SVNEntryHashMap(SVNObjectsPool pool) {
        this(null, pool);
    }
    
    public SVNEntryHashMap(Map<?, ?> map, SVNObjectsPool pool) {
        myObjectsPool = pool;
        
        init();
        putAll(map);
    }
    
    @Override
    public Object put(Object key, Object value) {
        key = getObjectFromPool(key);
        return super.put(key, value);
    }

    @Override
    protected TableEntry createTableEntry(Object key, Object value, int hash) {
        return new PooledTableEntry(myObjectsPool, key, value, hash);
    }
    
    private Object getObjectFromPool(Object value) {
        if (myObjectsPool != null) {
            return myObjectsPool.getObject(value);
        }
        return value;
    }
    
    private static boolean isNonPoolableKey(Object key) {            
        return ourNonPoolableKeys.contains(key); 
    }
    
    private static boolean isURLKey(Object key) {            
        return ourURLKeys.contains(key); 
    }
    
    protected static class PooledTableEntry extends SVNHashMap.TableEntry {
        
        private SVNObjectsPool myObjectsPool;
        
        public PooledTableEntry(SVNObjectsPool pool, Object key, Object value, int hash) {
            myObjectsPool = pool;
            init(key, value, hash);
        }
        
        public Object setValue(Object value) {
            Object valueForPool = getPoolValue(super.getKey(), value);
            return super.setValue(valueForPool);
        }

        public Object getValue() {
            return getRealValue(super.getValue());
        }

        private Object getRealValue(Object value) {
            if (value instanceof StringAsArray) {
                return ((StringAsArray) value).toString();
            }
            return value;
        }
        
        private Object getPoolValue(Object key, Object value) {
            if (myObjectsPool != null) {
                if (value instanceof String) {
                    if (isURLKey(key)) {
                        return new StringAsArray((String) value, myObjectsPool);
                    } else if (!isNonPoolableKey(key)) {
                        return myObjectsPool.getObject(value);
                    }
                    return value;
                } else if (value instanceof String[]) {
                    String[] array = (String[]) value;
                    for (int i = 0; i < array.length; i++) {
                        array[i] = (String) myObjectsPool.getObject(array[i]);
                    }
                } else if (value instanceof SVNRevision) {
                    return myObjectsPool.getObject(value);
                }
            }
            return value;
        }
    }
    
    private static Object[] split(String url) {
        ArrayList<String> segments = new ArrayList<String>();
        int startIndex = 0;
        int count = 0;
        for(int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (ch != '/' && i > 0 && url.charAt(i - 1) == '/') {
                count++;
                if (count > 3) {
                    segments.add(url.substring(startIndex, i));
                    startIndex = i;
                }
            }
        }
        if (startIndex < url.length()) {
            segments.add(url.substring(startIndex));
        }
        return segments.toArray();
    }
    
    
    private static class StringAsArray {
        
        private Object[] segments;
        private int hashCode;
        
        public StringAsArray(String str, SVNObjectsPool pool) {
            hashCode = str.hashCode();
            segments = split(str);
            for (int i = 0; i < segments.length; i++) {
                segments[i] = pool.getObject(segments[i]);
            }
        }
        
        public boolean equals(Object other) {
            if (other instanceof StringAsArray) {
                return Arrays.equals(segments, ((StringAsArray) other).segments);
            }
            return false;
        }
        
        public int hashCode() {
            return hashCode;
        }
        
        public String toString() {
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < segments.length; i++) {
                str.append((String) segments[i]);
            }
            return str.toString();
        }
    }

}
