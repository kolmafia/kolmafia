package org.tmatesoft.svn.core.wc2;

/**
 * Describe the kind of item. This can be:
 * <ul>
 * <li>FILE - file
 * <li>DIRECTORY - directory
 * <li>SYMLINK - symlink
 * <li>UNKNOWN - not known kind
 * </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public enum SvnFileKind {
    FILE, DIRECTORY, SYMLINK, UNKNOWN;
}
