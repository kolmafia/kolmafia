/*
 * $Id: DefaultUserNameStore.java,v 1.10 2009/02/09 14:54:00 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.auth;

import java.util.prefs.Preferences;

/**
 * Saves the user names in Preferences. Because any string could be part
 * of the user name, for every user name that must be saved a new Preferences
 * key/value pair must be stored.
 *
 * @author Bino George
 * @author rbair
 */
public class DefaultUserNameStore extends UserNameStore {
    /**
     * The key for one of the preferences
     */
    private static final String USER_KEY = "usernames";
    /**
     */
    private static final String NUM_KEY = "usernames.length";
    /**
     * The preferences node
     */
    private Preferences prefs;
    /**
     * Contains the user names. Since the list of user names is not
     * frequently updated, there is no penalty in storing the values
     * in an array.
     */
    private String[] userNames;
    
    /**
     * Creates a new instance of DefaultUserNameStore
     */
    public DefaultUserNameStore() {
        userNames = new String[0];
    }
    
    /**
     * Loads the user names from Preferences
     */
    @Override
public void loadUserNames() {
        initPrefs();
        if (prefs != null) {
            int n = prefs.getInt(NUM_KEY, 0);
            String[] names = new String[n];
            for (int i = 0; i < n; i++) {
                names[i] = prefs.get(USER_KEY + "." + i, null);
            }
            setUserNames(names);
        }
    }
    
    /**
     * Saves the user names to Preferences
     */
    @Override
public void saveUserNames() {
        initPrefs();
        if (prefs != null) {
            prefs.putInt(NUM_KEY, userNames.length);
            for (int i = 0; i < userNames.length; i++) {
                prefs.put(USER_KEY + "." + i, userNames[i]);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
public String[] getUserNames() {
        String[] copy = new String[userNames.length];
        System.arraycopy(userNames, 0, copy, 0, userNames.length);
        
        return copy;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
public void setUserNames(String[] userNames) {
        userNames = userNames == null ? new String[0] : userNames;
        String[] old = getUserNames();
        this.userNames = userNames;
        firePropertyChange("userNames", old, getUserNames());
    }
    
    /**
     * Add a username to the store.
     * @param name
     */
    @Override
public void addUserName(String name) {
        if (!containsUserName(name)) {
            String[] newNames = new String[userNames.length + 1];
            for (int i=0; i<userNames.length; i++) {
                newNames[i] = userNames[i];
            }
            newNames[newNames.length - 1] = name;
            setUserNames(newNames);
        }
    }
    
    /**
     * Removes a username from the list.
     *
     * @param name
     */
    @Override
public void removeUserName(String name) {
        if (containsUserName(name)) {
            String[] newNames = new String[userNames.length - 1];
            int index = 0;
            for (String s : userNames) {
                if (!s.equals(name)) {
                    newNames[index++] = s;
                }
            }
            setUserNames(newNames);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
public boolean containsUserName(String name) {
        for (String s : userNames) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return Returns Preferences node in which the user names will be stored
     */
    public Preferences getPreferences() {
        return prefs;
    }
    
    /**
     * @param prefs the Preferences node to store the user names in. If null,
     * or undefined, then they are stored in /org/jdesktop/swingx/auth/DefaultUserNameStore.
     */
    public void setPreferences(Preferences prefs) {
        Preferences old = getPreferences();
        initPrefs();
        this.prefs = prefs;
        firePropertyChange("preferences", old, getPreferences());
        if (this.prefs != old) {
            //if prefs is null, this next method will create the default prefs node
            loadUserNames();
        }
    }

    /**
     * Creates the default prefs node
     */
    private void initPrefs() {
        if (prefs == null) {
            prefs = Preferences.userNodeForPackage(DefaultUserNameStore.class);
            prefs = prefs.node("DefaultUserNameStore");
        }
    }
}