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

import java.util.HashSet;
import java.util.Set;

/**
 * The <b>SVNProperty</b> class is a representation class for both versioned
 * properties (user-managed svn specials) and for metaproperties (untweakable)
 * supported by Subversion. This class holds string constants that are property
 * names, and gives some useful methods to operate with properties (in particular).
 *
 * @author TMate Software Ltd.
 * @version 1.3
 * @since   1.2
 */
public class SVNProperty {
    /**
     * An <span class="javastring">"svn:"</span> prefix.
     */
    public static final String SVN_PREFIX = "svn:";
    
    /**
     * SVNKit's own property namespace.
     */
    public static final String SVNKIT_PREFIX = "svnkit:";
    /**
     * An <span class="javastring">"svn:wc:"</span> prefix.
     */
    public static final String SVN_WC_PREFIX = "svn:wc:";

    /**
     * The namespace for revision properties which are used in repository synching operations.
     */
    public static final String SVN_SYNC_PREFIX = "svn:sync-";

    /**
     * A special property used in a commit transaction.
     */
    public static final String TXN_CHECK_LOCKS = SVN_PREFIX + "check-locks";

    /**
     * A special property used in a commit transaction.
     */
    public static final String TXN_CHECK_OUT_OF_DATENESS = SVN_PREFIX + "check-ood";

    /**
     * An <span class="javastring">"svn:entry:"</span> prefix.
     */
    public static final String SVN_ENTRY_PREFIX = "svn:entry:";
    /**
     * An <span class="javastring">"svn:eol-style"</span> SVN special property.
     */
    public static final String EOL_STYLE = SVN_PREFIX + "eol-style";
    /**
     * An <span class="javastring">"svn:ignore"</span> SVN special property.
     */
    public static final String IGNORE = SVN_PREFIX + "ignore";
    /**
     * An <span class="javastring">"svn:mime-type"</span> SVN special property.
     */
    public static final String MIME_TYPE = SVN_PREFIX + "mime-type";
    /**
     * An <span class="javastring">"svn:keywords"</span> SVN special property.
     */
    public static final String KEYWORDS = SVN_PREFIX + "keywords";
    /**
     * An <span class="javastring">"svn:executable"</span> SVN special property.
     */
    public static final String EXECUTABLE = SVN_PREFIX + "executable";
    /**
     * An <span class="javastring">"svn:externals"</span> SVN special property.
     */
    public static final String EXTERNALS = SVN_PREFIX + "externals";
    /**
     * An <span class="javastring">"svn:special"</span> SVN special property.
     */
    public static final String SPECIAL = SVN_PREFIX + "special";

    /**
     * @since SVN 1.5
     */
    public static final String MERGE_INFO = SVN_PREFIX + "mergeinfo";
    
    /**
     * An <span class="javastring">"svn:entry:revision"</span> SVN untweakable metaproperty.
     */
    public static final String REVISION = SVN_ENTRY_PREFIX + "revision";
    /**
     * An <span class="javastring">"svn:entry:committed-rev"</span> SVN untweakable metaproperty.
     */
    public static final String COMMITTED_REVISION = SVN_ENTRY_PREFIX
            + "committed-rev";
    /**
     * An <span class="javastring">"svn:entry:committed-date"</span> SVN untweakable metaproperty.
     */
    public static final String COMMITTED_DATE = SVN_ENTRY_PREFIX
            + "committed-date";

    /**
     * <span class="javastring">"has-props"</span> SVN untweakable metaproperty.
     *
     * @since 1.1, new in Subversion 1.4
     */
    public static final String HAS_PROPS = SVN_ENTRY_PREFIX + "has-props";

    /**
     * <span class="javastring">"has-prop-mods"</span> SVN untweakable metaproperty.
     *
     * @since 1.1, new in Subversion 1.4
     */
    public static final String HAS_PROP_MODS = SVN_ENTRY_PREFIX + "has-prop-mods";

    /**
     * <span class="javastring">"cachable-props"</span> SVN untweakable metaproperty.
     *
     * @since 1.1, new in Subversion 1.4
     */
    public static final String CACHABLE_PROPS = SVN_ENTRY_PREFIX + "cachable-props";

    /**
     * <span class="javastring">"present-props"</span> SVN untweakable metaproperty.
     *
     * @since 1.1, new in Subversion 1.4
     */
    public static final String PRESENT_PROPS = SVN_ENTRY_PREFIX + "present-props";

    /**
     * An <span class="javastring">"svn:entry:keep-local"</span> SVN untweakable metaproperty.
     * @since  1.2.0, new in Subversion 1.5.0
     */
    public static final String KEEP_LOCAL = SVN_ENTRY_PREFIX + "keep-local";

    /**
     * An <span class="javastring">"svn:entry:changelist"</span> SVN untweakable metaproperty.
     * @since  1.2.0, new in Subversion 1.5.0
     */
    public static final String CHANGELIST = SVN_ENTRY_PREFIX + "changelist";

    /**
     * An <span class="javastring">"svn:entry:working-size"</span> SVN untweakable metaproperty.
     * @since  1.2.0, new in Subversion 1.5.0
     */
    public static final String WORKING_SIZE = SVN_ENTRY_PREFIX + "working-size";

    /**
     * An <span class="javastring">"svn:entry:depth"</span> SVN untweakable metaproperty.
     * @since  1.2.0, new in Subversion 1.5.0
     */
    public static final String DEPTH = SVN_ENTRY_PREFIX + "depth";

    /**
     * @since 1.3, new in Subversion 1.6
     */
    public static final String FILE_EXTERNAL_PATH = SVN_ENTRY_PREFIX + "file-external-path";

    /**
     * @since 1.3, new in Subversion 1.6
     */
    public static final String FILE_EXTERNAL_REVISION = SVN_ENTRY_PREFIX + "file-external-revision";

    /**
     * @since 1.3, new in Subversion 1.6
     */
    public static final String FILE_EXTERNAL_PEG_REVISION = SVN_ENTRY_PREFIX + "file-external-peg-revision";

    /**
     * @since 1.3, new in Subversion 1.6
     */
    public static final String TREE_CONFLICT_DATA = SVN_ENTRY_PREFIX + "tree-conflicts";
    
    /**
     * An <span class="javastring">"svn:entry:checksum"</span> SVN untweakable metaproperty.
     */
    public static final String CHECKSUM = SVN_ENTRY_PREFIX + "checksum";
    /**
     * An <span class="javastring">"svn:entry:url"</span> SVN untweakable metaproperty.
     */
    public static final String URL = SVN_ENTRY_PREFIX + "url";
    /**
     * An <span class="javastring">"svn:entry:copyfrom-url"</span> SVN untweakable metaproperty.
     */
    public static final String COPYFROM_URL = SVN_ENTRY_PREFIX + "copyfrom-url";
    /**
     * An <span class="javastring">"svn:entry:copyfrom-rev"</span> SVN untweakable metaproperty.
     */
    public static final String COPYFROM_REVISION = SVN_ENTRY_PREFIX
            + "copyfrom-rev";
    /**
     * An <span class="javastring">"svn:entry:schedule"</span> SVN untweakable metaproperty.
     */
    public static final String SCHEDULE = SVN_ENTRY_PREFIX + "schedule";
    /**
     * An <span class="javastring">"svn:entry:copied"</span> SVN untweakable metaproperty.
     */
    public static final String COPIED = SVN_ENTRY_PREFIX + "copied";
    /**
     * An <span class="javastring">"svn:entry:last-author"</span> SVN untweakable metaproperty.
     */
    public static final String LAST_AUTHOR = SVN_ENTRY_PREFIX + "last-author";
    /**
     * An <span class="javastring">"svn:entry:uuid"</span> SVN untweakable metaproperty.
     */
    public static final String UUID = SVN_ENTRY_PREFIX + "uuid";
    /**
     * An <span class="javastring">"svn:entry:repos"</span> SVN untweakable metaproperty.
     */
    public static final String REPOS = SVN_ENTRY_PREFIX + "repos";
    /**
     * An <span class="javastring">"svn:entry:prop-time"</span> SVN untweakable metaproperty.
     */
    public static final String PROP_TIME = SVN_ENTRY_PREFIX + "prop-time";
    /**
     * An <span class="javastring">"svn:entry:text-time"</span> SVN untweakable metaproperty.
     */
    public static final String TEXT_TIME = SVN_ENTRY_PREFIX + "text-time";
    /**
     * An <span class="javastring">"svn:entry:name"</span> SVN untweakable metaproperty.
     */
    public static final String NAME = SVN_ENTRY_PREFIX + "name";
    /**
     * An <span class="javastring">"svn:entry:kind"</span> SVN untweakable metaproperty.
     */
    public static final String KIND = SVN_ENTRY_PREFIX + "kind";
    /**
     * An <span class="javastring">"svn:entry:conflict-old"</span> SVN untweakable metaproperty.
     */
    public static final String CONFLICT_OLD = SVN_ENTRY_PREFIX + "conflict-old";
    /**
     * An <span class="javastring">"svn:entry:conflict-new"</span> SVN untweakable metaproperty.
     */
    public static final String CONFLICT_NEW = SVN_ENTRY_PREFIX + "conflict-new";
    /**
     * An <span class="javastring">"svn:entry:conflict-wrk"</span> SVN untweakable metaproperty.
     */
    public static final String CONFLICT_WRK = SVN_ENTRY_PREFIX + "conflict-wrk";
    /**
     * An <span class="javastring">"svn:entry:prop-reject-file"</span> SVN untweakable metaproperty.
     */
    public static final String PROP_REJECT_FILE = SVN_ENTRY_PREFIX
            + "prop-reject-file";
    /**
     * An <span class="javastring">"svn:entry:deleted"</span> SVN untweakable metaproperty.
     */
    public static final String DELETED = SVN_ENTRY_PREFIX + "deleted";
    /**
     * An <span class="javastring">"svn:entry:absent"</span> SVN untweakable metaproperty.
     */
    public static final String ABSENT = SVN_ENTRY_PREFIX + "absent";
    /**
     * An <span class="javastring">"svn:entry:incomplete"</span> SVN untweakable metaproperty.
     */
    public static final String INCOMPLETE = SVN_ENTRY_PREFIX + "incomplete";
    /**
     * An <span class="javastring">"svn:entry:corrupted"</span> SVN untweakable metaproperty.
     */
    public static final String CORRUPTED = SVN_ENTRY_PREFIX + "corrupted";
    /**
     * An <span class="javastring">"svn:wc:ra_dav:version-url"</span> SVN untweakable metaproperty.
     */
    public static final String WC_URL = SVN_WC_PREFIX + "ra_dav:version-url";
    /**
     * An <span class="javastring">"svn:wc:ra_dav:activity-url"</span> SVN untweakable metaproperty.
     */
    public static final String ACTIVITY_URL = SVN_WC_PREFIX + "ra_dav:activity-url";
    /**
     * An <span class="javastring">"svn:entry:lock-token"</span> SVN untweakable metaproperty.
     */
    public static final String LOCK_TOKEN = SVN_ENTRY_PREFIX + "lock-token";
    /**
     * An <span class="javastring">"svn:entry:lock-comment"</span> SVN untweakable metaproperty.
     */
    public static final String LOCK_COMMENT = SVN_ENTRY_PREFIX + "lock-comment";
    /**
     * An <span class="javastring">"svn:entry:lock-owner"</span> SVN untweakable metaproperty.
     */
    public static final String LOCK_OWNER = SVN_ENTRY_PREFIX + "lock-owner";
    /**
     * An <span class="javastring">"svn:entry:lock-creation-date"</span> SVN untweakable metaproperty.
     */
    public static final String LOCK_CREATION_DATE = SVN_ENTRY_PREFIX
            + "lock-creation-date";
    /**
     * An <span class="javastring">"svn:needs-lock"</span> SVN special property.
     */
    public static final String NEEDS_LOCK = SVN_PREFIX + "needs-lock";
    /**
     * One of the two possible values of the {@link #KIND} property -
     * <span class="javastring">"dir"</span>
     */
    public static final String KIND_DIR = "dir";
    /**
     * One of the two possible values of the {@link #KIND} property -
     * <span class="javastring">"file"</span>
     */
    public static final String KIND_FILE = "file";
    /**
     * One of the four possible values of the {@link #EOL_STYLE} property -
     * <span class="javastring">"LF"</span> (line feed)
     */
    public static final String EOL_STYLE_LF = "LF";
    /**
     * One of the four possible values of the {@link #EOL_STYLE} property -
     * <span class="javastring">"CR"</span> (linefeed)
     */
    public static final String EOL_STYLE_CR = "CR";
    /**
     * One of the four possible values of the {@link #EOL_STYLE} property -
     * <span class="javastring">"CRLF"</span>
     */
    public static final String EOL_STYLE_CRLF = "CRLF";
    /**
     * One of the four possible values of the {@link #EOL_STYLE} property -
     * <span class="javastring">"native"</span>
     */
    public static final String EOL_STYLE_NATIVE = "native";

    /**
     * LF (line feed) EOL (end of line) byte array.
     */
    public static final byte[] EOL_LF_BYTES = {'\n'};

    /**
     * CR (carriage return) and LF (line feed) EOL (end of line) bytes array.
     */
    public static final byte[] EOL_CRLF_BYTES = {'\r', '\n'};

    /**
     * CR (carriage return) EOL (end of line) byte array.
     */
    public static final byte[] EOL_CR_BYTES = {'\r'};

    /**
     * <code>SVNKit</code> specific property denoting a charset. A user may set this property on files
     * if he would like to fix the charset of the file. Then when checking out, exporting, updating, etc. 
     * files with such properties set on them will be translated (encoded) using the charset value of this
     * property. Note that to take advantage of this property a user must utilize a corresponging version 
     * of the <code>SVNKit</code> library supporting this property.
     */
    public static final String CHARSET = SVNKIT_PREFIX + "charset";

    /**
     * Default value for the {@link #CHARSET} property denoting that the native charset should be used 
     * to encode a file during translation. The native charset name will be fetched via a call to 
     * {@link org.tmatesoft.svn.core.wc.ISVNOptions#getNativeCharset()}.
     */
    public static final String NATIVE = "native";
    
    /**
     * One of the three possible values of the {@link #SCHEDULE} property -
     * <span class="javastring">"add"</span>
     */
    public static final String SCHEDULE_ADD = "add";
    /**
     * One of the three possible values of the {@link #SCHEDULE} property -
     * <span class="javastring">"delete"</span>
     */
    public static final String SCHEDULE_DELETE = "delete";
    /**
     * One of the three possible values of the {@link #SCHEDULE} property -
     * <span class="javastring">"replace"</span>
     */
    public static final String SCHEDULE_REPLACE = "replace";

    /**
     * Default value of the {@link #WORKING_SIZE} property.
     * @since  1.2.0, new in Subversion 1.5.0
     */
    public static final long WORKING_SIZE_UNKNOWN = -1;

    /**
     * Default value for such properties as {@link #EXECUTABLE}, {@link #NEEDS_LOCK}, {@link #SPECIAL}.
     * Used only by <code>SVNKit</code> internals, never stored in a working copy.
     * 
     * @since  1.2.0
     */
    public static final SVNPropertyValue BOOLEAN_PROPERTY_VALUE = SVNPropertyValue.create("*");

    /**
     * Says if the given property name starts with the {@link #SVN_WC_PREFIX}
     * prefix.
     *
     * @param name a property name to check
     * @return <span class="javakeyword">true</span> if <code>name</code> is
     *         not <span class="javakeyword">null</span> and starts with
     *         the {@link #SVN_WC_PREFIX} prefix, otherwise <span class="javakeyword">false</span>
     */
    public static boolean isWorkingCopyProperty(String name) {
        return name != null && name.startsWith(SVN_WC_PREFIX);
    }

    /**
     * Says if the given property name starts with the {@link #SVN_ENTRY_PREFIX}
     * prefix.
     *
     * @param name a property name to check
     * @return <span class="javakeyword">true</span> if <code>name</code> is
     *         not <span class="javakeyword">null</span> and starts with
     *         the {@link #SVN_ENTRY_PREFIX} prefix, otherwise <span class="javakeyword">false</span>
     */
    public static boolean isEntryProperty(String name) {
        return name != null && name.startsWith(SVN_ENTRY_PREFIX);
    }

    /**
     * Says if the given property name starts with the {@link #SVN_PREFIX}
     * prefix or with the {@link #SVNKIT_PREFIX}.
     *
     * @param name a property name to check
     * @return <span class="javakeyword">true</span> if <code>name</code> is
     *         not <span class="javakeyword">null</span> and starts with
     *         the {@link #SVN_PREFIX} prefix or with the {@link #SVNKIT_PREFIX} prefix,
     *         otherwise <span class="javakeyword">false</span>
     */
    public static boolean isSVNProperty(String name) {
        return name != null && (name.startsWith(SVN_PREFIX) || name.startsWith(SVNKIT_PREFIX));
    }

    /**
     * Says if the given property name starts with the {@link #SVNKIT_PREFIX}.
     *
     * @param name a property name to check
     * @return <span class="javakeyword">true</span> if <code>name</code> is
     *         not <span class="javakeyword">null</span> and starts with the
     * {@link #SVNKIT_PREFIX} prefix, otherwise <span class="javakeyword">false</span>
     */
    public static boolean isSVNKitProperty(String name) {
        return name != null && name.startsWith(SVNKIT_PREFIX);
    }

    /**
     * Checks if a property is regular. 
     * 
     * <p/>
     * A property is considered to be regular if it is not <span class="javakeyword">null</span> and 
     * does not start neither with {@link #SVN_WC_PREFIX} nor with {@link #SVN_ENTRY_PREFIX}.
     * 
     * @param name a property name
     * @return <span class="javakeyword">true</span> if regular, otherwise
     *         <span class="javakeyword">false</span>
     */
    public static boolean isRegularProperty(String name) {
        if (name == null) {
            return false;
        } else if (name.startsWith(SVN_WC_PREFIX) || name.startsWith(SVN_ENTRY_PREFIX)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Says if the given MIME-type corresponds to a text type.
     *
     * @param mimeType a value of a file {@link #MIME_TYPE} property
     * @return <span class="javakeyword">true</span> if <code>mimeType</code>
     *         is either <span class="javakeyword">null</span> or is a text
     *         type (starts with <span class="javastring">"text/"</span>)
     * @see #isBinaryMimeType(String)
     */
    public static boolean isTextMimeType(String mimeType) {
        if (mimeType == null || mimeType.startsWith("text/")) {
            return true;
        }
        synchronized (ourTextMimeTypes) {
            return ourTextMimeTypes.contains(mimeType);
        }
    }

    /**
     * Says if the given MIME-type corresponds to a binary (non-textual) type.
     *
     * @param mimeType a value of a file {@link #MIME_TYPE} property
     * @return <span class="javakeyword">true</span> if <code>mimeType</code>
     *         is not a text type
     * @see #isTextMimeType(String)
     */
    public static boolean isBinaryMimeType(String mimeType) {
        return !isTextMimeType(mimeType);
    }

    /**
     * Says if the given charset is the name of UTF-8 encoding.
     *
     * @param charset a value of a file {@link #CHARSET} property
     * @return <span class="javakeyword">true</span> if <code>charset</code>
     *         is the name of UTF-8 encoding
     */
    public static boolean isUTF8(String charset) {
        return charset != null && charset.equalsIgnoreCase("UTF-8");
    }

    /**
     * Converts a string representation of a boolean value to boolean.
     * Useful to convert values of the {@link #COPIED} property.
     *
     * @param text a string to convert to a boolean value
     * @return <span class="javakeyword">true</span> if and only if
     *         <code>text</code> is not <span class="javakeyword">null</span>
     *         and is equal, ignoring case, to the string
     *         <span class="javastring">"true"</span>
     */
    public static boolean booleanValue(String text) {
        return text == null ? false : Boolean.valueOf(text.trim())
                .booleanValue();
    }

    /**
     * Converts a string representation of a numeric value to a long value.
     * Useful to convert revision numbers.
     *
     * @param text a string to convert to a long value
     * @return a long representation of the given string;
     *         -1 is returned if the string can not be parsed
     */
    public static long longValue(String text) {
        if (text != null) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    /**
     * Converts a boolean value to a string representation.
     * Useful to convert values of the {@link #COPIED} property.
     *
     * @param b a boolean value
     * @return a string representation of <code>b</code>
     */
    public static String toString(boolean b) {
        return Boolean.toString(b);
    }

    /**
     * Converts a long value to a string representation.
     * Useful to convert revision numbers.
     *
     * @param i a long value
     * @return a string representation of <code>i</code>
     */
    public static String toString(long i) {
        return Long.toString(i);
    }

    /**
     * Returns a short name for the given property name - that is
     * a name without any prefixes.
     *
     * @param longName a property name
     * @return a property short name
     */
    public static String shortPropertyName(String longName) {
        if (longName == null) {
            return null;
        }
        if (longName.startsWith(SVNProperty.SVN_ENTRY_PREFIX)) {
            return longName.substring(SVNProperty.SVN_ENTRY_PREFIX.length());
        } else if (longName.startsWith(SVNProperty.SVN_WC_PREFIX)) {
            return longName.substring(SVNProperty.SVN_WC_PREFIX.length());
        } else if (longName.startsWith(SVNProperty.SVN_PREFIX)) {
            return longName.substring(SVNProperty.SVN_PREFIX.length());
        }
        return longName;
    }

    /**
     * Returns the value for such boolean properties as
     * <span class="javastring">"svn:executable"</span>, <span class="javastring">"svn:needs-lock"</span>
     * and <span class="javastring">"svn:special"</span>.
     * Used by internals.
     *
     * @param propName a property name
     * @return the property value <span class="javastring">"*"</span>, or
     *         <span class="javakeyword">null</span> if the property is not boolean
     * @see #isBooleanProperty(String)
     * @since 1.1
     */
    public static SVNPropertyValue getValueOfBooleanProperty(String propName) {
        if (SVNProperty.EXECUTABLE.equals(propName) || SVNProperty.NEEDS_LOCK.equals(propName) || 
                SVNProperty.SPECIAL.equals(propName)) {
            return BOOLEAN_PROPERTY_VALUE;
        }
        return null;
    }

    /**
     * Checks whether the property is boolean.
     *
     * @param propName a property name
     * @return <span class="javakeyword">true</span> if boolean,
     *         otherwise <span class="javakeyword">false</span>
     * @since 1.1
     */
    public static boolean isBooleanProperty(String propName) {
        return SVNProperty.EXECUTABLE.equals(propName) || SVNProperty.SPECIAL.equals(propName) || SVNProperty.NEEDS_LOCK.equals(propName);
    }
    

    private static final Set ourTextMimeTypes = new HashSet();

    /**
     * Adds custom mime-type value that should be considered as text.
     * Otherwise only 'null' mime-types and those starting with 'text/' are considered as text.
     */
    public static void addTextMimeType(String textMimeType) {
        if (textMimeType != null) {
            synchronized (ourTextMimeTypes) {
                ourTextMimeTypes.add(textMimeType);
            }
        }
    }
    
    /**
     * Returns custom mime-types previously added.
     */
    public Set getTextMimeTypes() {
        synchronized (ourTextMimeTypes) {
            return new HashSet(ourTextMimeTypes);
        }
    }

    /**
     * Clears custom mime-types previously added.
     */
    public static void clearTextMimeTypes() {
        synchronized (ourTextMimeTypes) {
            ourTextMimeTypes.clear();
        }
    }
}
