package org.tmatesoft.svn.core.wc2.hooks;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

/**
 * Implementing this interface allows to define properties to change.
 *
 * @author TMate Software Ltd.
 * @version 1.7
 */
public interface ISvnPropertyValueProvider {

    /**
     * Defines local item's properties to be installed.
     *
     * @param path          an working copy item's path
     * @param properties    an item's versioned properties
     * @return              <b>SVNProperties</b> object which stores properties to be installed on an item
     * @throws SVNException
     */
    public SVNProperties providePropertyValues(File path, SVNProperties properties) throws SVNException;

}
