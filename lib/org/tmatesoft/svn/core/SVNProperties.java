/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.tmatesoft.svn.core.internal.util.SVNHashMap;

/**
 * The <b>SVNProperties</b> class represents an object wrapper for
 * <code>String</code> to {@link SVNPropertyValue} mappings where
 * <code>String</code> keys represent property names and values - property
 * values wrapped in {@link SVNPropertyValue} objects.
 *
 * <p>
 * This class is backed by a <code>Map</code> object and brings specific methods
 * useful for working with version controlled properties.
 *
 * <p>
 * Objects of this type are modifiable.
 *
 * @author TMate Software Ltd.
 * @version 1.3
 * @since 1.2
 */
public class SVNProperties implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    private Map myProperties;

    /**
     * Creates a new <code>SVNProperties</code> object wrapping a given map with
     * properties.
     *
     * <p/>
     * <code>map</code> is not stored by this object, instead its contents are
     * copied into a new <code>Map</code> object (which will be backed by a new
     * <code>SVNProperties</code> object) according to the following rules:
     *
     * <ul>
     * <li/>if the value is of type <code>String</code>, then it's wrapped into
     * {@link SVNPropertyValue} using the
     * {@link SVNPropertyValue#create(String)} method;
     * <li/>if the value is of type <code>byte[]</code>, then it's wrapped into
     * {@link SVNPropertyValue} using the
     * {@link SVNPropertyValue#create(String, byte[])} method;
     * <li/>if the value is of type {@link SVNPropertyValue}, then it's not
     * copied but is put into a new map as is;
     * </ul>
     *
     * @param map
     *            initial map holding properties
     * @return <code>SVNProperties</code> object; if <code>map</code> is <span
     *         class="javakeyword">null</span>, returns an empty
     *         <code>SVNProperties</code> object created as
     *         <code>new SVNProperties()</code>
     * @see #SVNProperties()
     */
    public static SVNProperties wrap(Map map) {
        if (map == null) {
            return new SVNProperties();
        }
        Map propertiesMap = new SVNHashMap();
        for (Iterator names = map.keySet().iterator(); names.hasNext();) {
            Object n = names.next();
            if (!(n instanceof String)) {
                continue;
            }
            Object value = map.get(n);
            SVNPropertyValue v = null;
            if (value instanceof String) {
                v = SVNPropertyValue.create((String) value);
            } else if (value instanceof byte[]) {
                v = SVNPropertyValue.create(n.toString(), (byte[]) value);
            } else if (value instanceof SVNPropertyValue) {
                v = (SVNPropertyValue) value;
            }
            if (v != null) {
                propertiesMap.put(n, v);
            }
        }
        return new SVNProperties(propertiesMap);
    }

    /**
     * Returns an unmodifiable view of the specified <code>properties</code>.
     * Any attempt to modify the returned <code>SVNProperties</code> object
     * result in an <code>UnsupportedOperationException</code>.
     *
     * @param properties
     *            <code>SVNProperties</code> object for which an unmodifiable
     *            view is to be returned.
     * @return an unmodifiable view of the specified properties.
     */
    public static SVNProperties unmodifiableProperties(SVNProperties properties) {
        Map propertiesMap = properties.myProperties;
        propertiesMap = Collections.unmodifiableMap(propertiesMap);
        return new SVNProperties(propertiesMap);
    }

    /**
     * Creates an empty <code>SVNProperties</code> object.
     */
    public SVNProperties() {
        myProperties = new SVNHashMap();
    }

    /**
     * Creates a new <code>SVNProperties</code> object copying the given one.
     *
     * @param properties
     *            an initializer
     */
    public SVNProperties(SVNProperties properties) {
        myProperties = new SVNHashMap(properties.myProperties);
    }

    private SVNProperties(Map properties) {
        myProperties = properties;
    }

    /**
     * Returns SVNProperties as Map of String, SVNPropertyValue pairs.
     *
     * @return copy of SVNProperties as Map object
     */
    public Map asMap() {
        if (myProperties == null) {
            return Collections.unmodifiableMap(Collections.EMPTY_MAP);
        }
        return Collections.unmodifiableMap(myProperties);
    }

    /**
     * Stores a new mapping <code>propertyName</code> to
     * <code>propertyValue</code> in this object.
     *
     * @param propertyName
     *            property name
     * @param propertyValue
     *            property value object
     */
    public void put(String propertyName, SVNPropertyValue propertyValue) {
        myProperties.put(propertyName, propertyValue);
    }

    /**
     * Stores a new property name-to-value mapping in this object.
     *
     * <p>
     * <code>propertyValue</code> is converted to an {@link SVNPropertyValue}
     * object through a call to {@link SVNPropertyValue#create(String)}.
     *
     * @param propertyName
     *            property name
     * @param propertyValue
     *            property value string
     */
    public void put(String propertyName, String propertyValue) {
        myProperties.put(propertyName, SVNPropertyValue.create(propertyValue));
    }

    /**
     * Stores a new property name-to-value mapping in this object.
     *
     * <p>
     * <code>propertyValue</code> is converted to an {@link SVNPropertyValue}
     * object through a call to {@link SVNPropertyValue#create(String, byte[])}.
     *
     * @param propertyName
     *            property name
     * @param propertyValue
     *            property value bytes
     */
    public void put(String propertyName, byte[] propertyValue) {
        myProperties.put(propertyName, SVNPropertyValue.create(propertyName, propertyValue));
    }

    /**
     * Returns a <code>String</code> property value.
     *
     * @param propertyName
     *            property name
     * @return property value string; <span class="javakeyword">null</span> if
     *         there's no such property or if it's not a <code>String</code>
     *         property value
     */
    public String getStringValue(String propertyName) {
        SVNPropertyValue value = (SVNPropertyValue) myProperties.get(propertyName);
        return value == null ? null : value.getString();
    }

    /**
     * Returns a binary property value.
     *
     * @param propertyName
     *            property name
     * @return byte array containing property value bytes; <span
     *         class="javakeyword">null</span> if there's no such property or if
     *         it's not a binary property value
     */
    public byte[] getBinaryValue(String propertyName) {
        SVNPropertyValue value = (SVNPropertyValue) myProperties.get(propertyName);
        return value == null ? null : value.getBytes();
    }

    /**
     * Returns a property value as an {@link SVNPropertyValue}.
     *
     * @param propertyName
     *            property name
     * @return property value object; <span class="javakeyword">null</span> if
     *         there's no such property
     */
    public SVNPropertyValue getSVNPropertyValue(String propertyName) {
        return (SVNPropertyValue) myProperties.get(propertyName);
    }

    /**
     * Removes the specified property from this properties object.
     *
     * @param propertyName
     *            name of the property to remove from this object
     * @return the value of the removed object
     */
    public SVNPropertyValue remove(String propertyName) {
        return (SVNPropertyValue) myProperties.remove(propertyName);
    }

    /**
     * Puts all properties from the specified properties object to this object.
     *
     * @param properties
     *            properties object
     */
    public void putAll(SVNProperties properties) {
        myProperties.putAll(properties.myProperties);
    }

    /**
     * Tells if this properties object holds no properties (empty).
     *
     * @return <span class="javakeyword">true</span> if this object holds no
     *         properties; otherwise <span class="javakeyword">false</span>
     */
    public boolean isEmpty() {
        return myProperties.isEmpty();
    }

    /**
     * Removes all properties from this object.
     *
     */
    public void clear() {
        myProperties.clear();
    }

    /**
     * Removes all mappings which values are <span
     * class="javakeyword">null</span>s from this object.
     */
    public void removeNullValues() {
        for (Iterator iterator = myProperties.keySet().iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if (myProperties.get(name) == null) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns the number of properties held by this object.
     *
     * @return number of properties
     */
    public int size() {
        return myProperties.size();
    }

    /**
     * Tells whether this properties object contains the specified property
     * name.
     *
     * @param propertyName
     *            property name
     * @return <span class="javakeyword">true</span> if this object contains a
     *         mapping with the specified key (<code>propertyName</code>)
     */
    public boolean containsName(String propertyName) {
        return myProperties.containsKey(propertyName);
    }

    /**
     * Returns a set of property names contained by this object.
     *
     * @return property names set
     */
    public Set<String> nameSet() {
        return myProperties.keySet();
    }

    /**
     * Tells whether this properties object contains the specified property
     * value.
     *
     * @param value
     *            property value
     * @return <span class="javakeyword">true</span> if this object contains
     *         <code>value</code>
     *
     */
    public boolean containsValue(SVNPropertyValue value) {
        return myProperties.containsValue(value);
    }

    /**
     * Returns a collection of property values contained in this properties
     * object.
     *
     * @return property values collection
     */
    public Collection values() {
        return myProperties.values();
    }

    /**
     * Returns a subset of properties contained in this properties object which
     * suffice for {@link SVNProperty#isRegularProperty(String)} clause.
     *
     * @return regular properties; if there are no properties which would
     *         suffice the aforementioned clause, an empty
     *         <code>SVNProperties</code> object is returned
     */
    public SVNProperties getRegularProperties() {
        SVNProperties result = new SVNProperties();
        for (Iterator propNamesIter = nameSet().iterator(); propNamesIter.hasNext();) {
            String propName = (String) propNamesIter.next();
            if (SVNProperty.isRegularProperty(propName)) {
                result.put(propName, getSVNPropertyValue(propName));
            }
        }
        return result;
    }

    /**
     * Compares this object against another one returning a difference between
     * them.
     *
     * <p/>
     * Properties which are present in this object but are not in
     * <code>properties</code>, are put to the result as property name to <span
     * class="javakeyword">null</span> mappings. Properties which are present
     * only in <code>properties</code> but not in this object, are added to the
     * result. Also result will include those properties which are present in
     * both objects but have different values; in this case result will include
     * such properties with values from <code>properties</code>.
     *
     * @param properties
     *            another properties object
     * @return properties object holding the properties difference
     */
    public SVNProperties compareTo(SVNProperties properties) {
        SVNProperties result = new SVNProperties();
        if (isEmpty()) {
            result.putAll(properties);
            return result;
        }

        Collection props1 = nameSet();
        Collection props2 = properties.nameSet();

        // missed in props2.
        Collection tmp = new TreeSet(props1);
        tmp.removeAll(props2);
        for (Iterator props = tmp.iterator(); props.hasNext();) {
            String missing = (String) props.next();
            result.put(missing, (byte[]) null);
        }

        // added in props2.
        tmp = new TreeSet(props2);
        tmp.removeAll(props1);

        for (Iterator props = tmp.iterator(); props.hasNext();) {
            String added = (String) props.next();
            result.put(added, properties.getSVNPropertyValue(added));
        }

        // changed in props2
        tmp = new TreeSet(props2);
        tmp.retainAll(props1);

        for (Iterator props = tmp.iterator(); props.hasNext();) {
            String changed = (String) props.next();
            SVNPropertyValue value1 = getSVNPropertyValue(changed);
            SVNPropertyValue value2 = properties.getSVNPropertyValue(changed);
            if (!value1.equals(value2)) {
                result.put(changed, value2);
            }
        }
        return result;
    }

    /**
     * Returns a hash code of this object.
     *
     * <p/>
     * A hash code is evaluated as follows: <code>31 + </code>
     * {@link java.util.Map#hashCode() hash code} of the underlying
     * <code>Map</code> holding the property key to property value mappings.
     *
     * @return hash code of this object
     */
    public int hashCode() {
        return 31 + ((myProperties == null) ? 0 : myProperties.hashCode());
    }

    /**
     * Tells whether this object and <code>obj</code> are equal.
     *
     * @param obj
     *            object to compare with
     * @return <span class="javakeyword">true</span> if <code>obj</code> is
     *         either this very object, or is an instance of
     *         <code>SVNProperties</code> with the same contents of properties
     *
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SVNProperties other = (SVNProperties) obj;
        if (myProperties == null) {
            if (other.myProperties != null) {
                return false;
            }
        } else if (!myProperties.equals(other.myProperties)) {
            return false;
        }
        return true;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance
     *
     */
    public Object clone() throws CloneNotSupportedException {
        try {
            super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }

        SVNProperties result = new SVNProperties();
        result.putAll(this);
        return result;
    }

    public String toString() {
        if (myProperties != null) {
            return myProperties.toString();
        }
        return "";
    }

}
