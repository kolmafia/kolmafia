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
import java.util.Map;

import org.tmatesoft.svn.core.internal.util.SVNHashMap;


/**
 * The <b>SVNErrorCode</b> class represents possible predefined kinds 
 * of errors with their own identifying information. Each <b>SVNErrorCode</b> 
 * has its common description, belongs to a definite category of errors and 
 * also has its unique error code int value based upon the category.
 * 
 * <p>
 * Error codes (values, common descriptions and categories) are similar 
 * to ones in the native SVN. 
 * 
 * <p>
 * Error codes are divided into categories of up to 5000 errors each. 
 * Categories are fixed-size; if a category has fewer than 5000 errors, then it just ends with a range of 
 * unused numbers.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNErrorCode implements Serializable {
    
	private static final long serialVersionUID = 1L;
	
	private String myDescription;
    private int myCategory;
    private int myCode;
    
    private static final Map ourErrorCodes = new SVNHashMap();
    
    /**
     * Gets an error code object given its unique error code number. 
     * If no definite error code objects corresponds to the provided 
     * value, returns {@link #UNKNOWN}.
     * 
     * @param  code an error code number
     * @return a corresponding <b>SVNErrorCode</b>.
     */
    public static SVNErrorCode getErrorCode(int code) {
        SVNErrorCode errorCode = (SVNErrorCode) ourErrorCodes.get(new Integer(code));
        if (errorCode == null) {
            errorCode = UNKNOWN;
        }
        return errorCode;
    }
    
    protected SVNErrorCode(int category, int index, String description) {
        myCategory = category;
        myCode = category + index;
        myDescription = description;
        ourErrorCodes.put(new Integer(myCode), this);
    }
    
    /**
     * Returns a unique error code value. 
     * 
     * @return an error code number
     */
    public int getCode() {
        return myCode;
    }
    
    /**
     * Returns the category this error code object belongs to. 
     * 
     * @return an error code category
     */
    public int getCategory() {
        return myCategory;
    }
    
    /**
     * Returns a description of this error. 
     * 
     * @return an error description common for all errors of the same
     *         error code 
     */
    public String getDescription() {
        return myDescription;
    }
    
    /**
     * Returns a hash code for this object.
     * 
     * @return hash code value
     */
    public int hashCode() {
        return myCode;
    }
    
    /**
     * Says if the given object and this one are equal.
     * 
     * @param  o an object to compare with
     * @return   <span class="javakeyword">true</span> if equals,
     *           <span class="javakeyword">false</span> otherwise 
     */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != SVNErrorCode.class) {
            return false;
        }
        return myCode == ((SVNErrorCode) o).myCode;
    }
    
    /**
     * Says if this error is an authentication error.
     *  
     * @return  <span class="javakeyword">true</span> if it is,
     *          <span class="javakeyword">false</span> otherwise 
     */
    public boolean isAuthentication() {
        return this == RA_NOT_AUTHORIZED || this == RA_UNKNOWN_AUTH || getCategory() == AUTHZ_CATEGORY || getCategory() == AUTHN_CATEGORY;
    }
    
    private Object readResolve() {
        return ourErrorCodes.get(new Integer(myCode));
    }
    
    /**
     * Gives a string representation of this object.
     * 
     * @return a string representing this object
     */
    public String toString() {
        return myCode + ": " + myDescription;
    }
    
    private static final int ERR_BASE = 120000;
    private static final int ERR_CATEGORY_SIZE = 5000;

    public static final int BAD_CATEGORY = ERR_BASE + 1*ERR_CATEGORY_SIZE;
    public static final int XML_CATEGORY = ERR_BASE + 2*ERR_CATEGORY_SIZE;
    public static final int IO_CATEGORY = ERR_BASE + 3*ERR_CATEGORY_SIZE;
    public static final int STREAM_CATEGORY = ERR_BASE + 4*ERR_CATEGORY_SIZE;
    public static final int NODE_CATEGORY = ERR_BASE + 5*ERR_CATEGORY_SIZE;
    public static final int ENTRY_CATEGORY = ERR_BASE + 6*ERR_CATEGORY_SIZE;
    public static final int WC_CATEGORY = ERR_BASE + 7*ERR_CATEGORY_SIZE;
    public static final int FS_CATEGORY = ERR_BASE + 8*ERR_CATEGORY_SIZE;
    public static final int REPOS_CATEGORY = ERR_BASE + 9*ERR_CATEGORY_SIZE;
    public static final int RA_CATEGORY = ERR_BASE + 10*ERR_CATEGORY_SIZE;
    public static final int RA_DAV_CATEGORY = ERR_BASE + 11*ERR_CATEGORY_SIZE;
    public static final int RA_LOCAL_CATEGORY = ERR_BASE + 12*ERR_CATEGORY_SIZE;
    public static final int SVNDIFF_CATEGORY = ERR_BASE + 13*ERR_CATEGORY_SIZE;
    public static final int APMOD_CATEGORY = ERR_BASE + 14*ERR_CATEGORY_SIZE;
    public static final int CLIENT_CATEGORY = ERR_BASE + 15*ERR_CATEGORY_SIZE;
    public static final int MISC_CATEGORY = ERR_BASE + 16*ERR_CATEGORY_SIZE;
    public static final int CL_CATEGORY = ERR_BASE + 17*ERR_CATEGORY_SIZE;
    public static final int RA_SVN_CATEGORY = ERR_BASE + 18*ERR_CATEGORY_SIZE;
    public static final int AUTHN_CATEGORY = ERR_BASE + 19*ERR_CATEGORY_SIZE;
    public static final int AUTHZ_CATEGORY = ERR_BASE + 20*ERR_CATEGORY_SIZE;
    public static final int DIFF_CATEGORY = ERR_BASE + 21*ERR_CATEGORY_SIZE;
    public static final int RA_SERF_CATEGORY = ERR_BASE + 22*ERR_CATEGORY_SIZE;
    public static final int MALFUNC_CATEGORY = ERR_BASE + 23*ERR_CATEGORY_SIZE;
    

    
    
    public static final SVNErrorCode UNKNOWN = new SVNErrorCode(MISC_CATEGORY, ERR_CATEGORY_SIZE - 100, "Unknown error");
    public static final SVNErrorCode IO_ERROR = new SVNErrorCode(MISC_CATEGORY, ERR_CATEGORY_SIZE - 101, "Generic IO error");

    public static final SVNErrorCode BAD_CONTAINING_POOL = new SVNErrorCode(BAD_CATEGORY, 0, "Bad parent pool passed to svn_make_pool()");
    public static final SVNErrorCode BAD_FILENAME = new SVNErrorCode(BAD_CATEGORY, 1, "Bogus filename");
    public static final SVNErrorCode BAD_URL = new SVNErrorCode(BAD_CATEGORY, 2, "Bogus URL");
    public static final SVNErrorCode BAD_DATE = new SVNErrorCode(BAD_CATEGORY, 3, "Bogus date");
    public static final SVNErrorCode BAD_MIME_TYPE = new SVNErrorCode(BAD_CATEGORY, 4, "Bogus mime-type");
    public static final SVNErrorCode BAD_PROPERTY_VALUE = new SVNErrorCode(BAD_CATEGORY, 5, "Wrong or unexpected property value");
    public static final SVNErrorCode BAD_VERSION_FILE_FORMAT = new SVNErrorCode(BAD_CATEGORY, 6, "Version file format not correct");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode BAD_RELATIVE_PATH = new SVNErrorCode(BAD_CATEGORY, 7, "Path is not an immediate child of the specified directory");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode BAD_UUID = new SVNErrorCode(BAD_CATEGORY, 8, "Bogus UUID");
    
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode BAD_CONFIG_VALUE = new SVNErrorCode(BAD_CATEGORY, 9, "Invalid configuration value");
    
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode BAD_SERVER_SPECIFICATION = new SVNErrorCode(BAD_CATEGORY, 10, "Bogus server specification");

    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode BAD_CHECKSUM_KIND = new SVNErrorCode(BAD_CATEGORY, 11, "Unsupported checksum type");

    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode BAD_CHECKSUM_PARSE = new SVNErrorCode(BAD_CATEGORY, 12, "Invalid character in hex checksum");

    public static final SVNErrorCode XML_ATTRIB_NOT_FOUND = new SVNErrorCode(XML_CATEGORY, 0, "No such XML tag attribute");
    public static final SVNErrorCode XML_MISSING_ANCESTRY = new SVNErrorCode(XML_CATEGORY, 1, "<delta-pkg> is missing ancestry");
    public static final SVNErrorCode XML_UNKNOWN_ENCODING = new SVNErrorCode(XML_CATEGORY, 2, "Unrecognized binary data encoding; can't decode");
    public static final SVNErrorCode XML_MALFORMED = new SVNErrorCode(XML_CATEGORY, 3, "XML data was not well-formed");
    public static final SVNErrorCode XML_UNESCAPABLE_DATA = new SVNErrorCode(XML_CATEGORY, 4, "Data cannot be safely XML-escaped");
    
    public static final SVNErrorCode IO_INCONSISTENT_EOL = new SVNErrorCode(IO_CATEGORY, 0, "Inconsistent line ending style");
    public static final SVNErrorCode IO_UNKNOWN_EOL = new SVNErrorCode(IO_CATEGORY, 1, "Unrecognized line ending style");
    public static final SVNErrorCode IO_CORRUPT_EOL = new SVNErrorCode(IO_CATEGORY, 2, "Line endings other than expected");
    public static final SVNErrorCode IO_UNIQUE_NAMES_EXHAUSTED = new SVNErrorCode(IO_CATEGORY, 3, "Ran out of unique names");
    public static final SVNErrorCode IO_PIPE_FRAME_ERROR = new SVNErrorCode(IO_CATEGORY, 4, "Framing error in pipe protocol");
    public static final SVNErrorCode IO_PIPE_READ_ERROR = new SVNErrorCode(IO_CATEGORY, 5, "Read error in pipe");
    public static final SVNErrorCode IO_WRITE_ERROR = new SVNErrorCode(IO_CATEGORY, 6, "Write error");
    
    public static final SVNErrorCode STREAM_UNEXPECTED_EOF = new SVNErrorCode(STREAM_CATEGORY, 0, "Unexpected EOF on stream");
    public static final SVNErrorCode STREAM_MALFORMED_DATA = new SVNErrorCode(STREAM_CATEGORY, 1, "Malformed stream data");
    public static final SVNErrorCode STREAM_UNRECOGNIZED_DATA = new SVNErrorCode(STREAM_CATEGORY, 2, "Unrecognized stream data");
    
    public static final SVNErrorCode NODE_UNKNOWN_KIND = new SVNErrorCode(NODE_CATEGORY, 0, "Unknown svn_node_kind");
    public static final SVNErrorCode NODE_UNEXPECTED_KIND = new SVNErrorCode(NODE_CATEGORY, 1, "Unexpected node kind found");
    
    public static final SVNErrorCode ENTRY_NOT_FOUND = new SVNErrorCode(ENTRY_CATEGORY, 0, "Can't find an entry");
    public static final SVNErrorCode ENTRY_EXISTS = new SVNErrorCode(ENTRY_CATEGORY, 2, "Entry already exists");
    public static final SVNErrorCode ENTRY_MISSING_REVISION = new SVNErrorCode(ENTRY_CATEGORY, 3, "Entry has no revision");
    public static final SVNErrorCode ENTRY_MISSING_URL = new SVNErrorCode(ENTRY_CATEGORY, 4, "Entry has no URL");
    public static final SVNErrorCode ENTRY_ATTRIBUTE_INVALID = new SVNErrorCode(ENTRY_CATEGORY, 5, "Entry has an invalid attribute");
    public static final SVNErrorCode ENTRY_FORBIDDEN = new SVNErrorCode(ENTRY_CATEGORY, 6, "Can't create an entry for a forbidden name");
    
    public static final SVNErrorCode WC_OBSTRUCTED_UPDATE = new SVNErrorCode(WC_CATEGORY, 0, "Obstructed update");
    public static final SVNErrorCode WC_UNWIND_MISMATCH = new SVNErrorCode(WC_CATEGORY, 1, "Mismatch popping the WC unwind stack");
    public static final SVNErrorCode WC_UNWIND_EMPTY = new SVNErrorCode(WC_CATEGORY, 2, "Attempt to pop empty WC unwind stack");
    public static final SVNErrorCode WC_UNWIND_NOT_EMPTY = new SVNErrorCode(WC_CATEGORY, 3, "Attempt to unlock with non-empty unwind stack");
    public static final SVNErrorCode WC_LOCKED = new SVNErrorCode(WC_CATEGORY, 4, "Attempted to lock an already-locked dir");
    public static final SVNErrorCode WC_NOT_LOCKED = new SVNErrorCode(WC_CATEGORY, 5, "Working copy not locked; this is probably a bug, please report");
    public static final SVNErrorCode WC_INVALID_LOCK = new SVNErrorCode(WC_CATEGORY, 6, "Invalid lock");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_NOT_WORKING_COPY = new SVNErrorCode(WC_CATEGORY, 7, "Path is not a working copy directory");

    /**
     * This code is deprecated. Use WC_NOT_WORKING_COPY. Provided for backward compatibility with pre-1.4 API
     */
    public static final SVNErrorCode WC_NOT_DIRECTORY = WC_NOT_WORKING_COPY;
    public static final SVNErrorCode WC_NOT_FILE = new SVNErrorCode(WC_CATEGORY, 8, "Path is not a working copy file");
    public static final SVNErrorCode WC_BAD_ADM_LOG = new SVNErrorCode(WC_CATEGORY, 9, "Problem running log");
    public static final SVNErrorCode WC_PATH_NOT_FOUND = new SVNErrorCode(WC_CATEGORY, 10, "Can't find a working copy path");
    public static final SVNErrorCode WC_NOT_UP_TO_DATE = new SVNErrorCode(WC_CATEGORY, 11, "Working copy is not up-to-date");
    public static final SVNErrorCode WC_LEFT_LOCAL_MOD = new SVNErrorCode(WC_CATEGORY, 12, "Left locally modified or unversioned files");
    public static final SVNErrorCode WC_SCHEDULE_CONFLICT = new SVNErrorCode(WC_CATEGORY, 13, "Unmergeable scheduling requested on an entry");
    public static final SVNErrorCode WC_PATH_FOUND = new SVNErrorCode(WC_CATEGORY, 14, "Found a working copy path");
    public static final SVNErrorCode WC_FOUND_CONFLICT = new SVNErrorCode(WC_CATEGORY, 15, "A conflict in the working copy obstructs the current operation");
    public static final SVNErrorCode WC_CORRUPT = new SVNErrorCode(WC_CATEGORY, 16, "Working copy is corrupt");
    public static final SVNErrorCode WC_CORRUPT_TEXT_BASE = new SVNErrorCode(WC_CATEGORY, 17, "Working copy text base is corrupt");
    public static final SVNErrorCode WC_NODE_KIND_CHANGE = new SVNErrorCode(WC_CATEGORY, 18, "Cannot change node kind");
    public static final SVNErrorCode WC_INVALID_OP_ON_CWD = new SVNErrorCode(WC_CATEGORY, 19, "Invalid operation on the current working directory");
    public static final SVNErrorCode WC_BAD_ADM_LOG_START = new SVNErrorCode(WC_CATEGORY, 20, "Problem on first log entry in a working copy");
    public static final SVNErrorCode WC_UNSUPPORTED_FORMAT = new SVNErrorCode(WC_CATEGORY, 21, "Unsupported working copy format");
    public static final SVNErrorCode WC_BAD_PATH = new SVNErrorCode(WC_CATEGORY, 22, "Path syntax not supported in this context");
    public static final SVNErrorCode WC_INVALID_SCHEDULE = new SVNErrorCode(WC_CATEGORY, 23, "Invalid schedule");
    public static final SVNErrorCode WC_INVALID_RELOCATION = new SVNErrorCode(WC_CATEGORY, 24, "Invalid relocation");
    public static final SVNErrorCode WC_INVALID_SWITCH = new SVNErrorCode(WC_CATEGORY, 25, "Invalid switch");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode WC_MISMATCHED_CHANGELIST = new SVNErrorCode(WC_CATEGORY, 26, "Changelist doesn't match");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode WC_CONFLICT_RESOLVER_FAILURE = new SVNErrorCode(WC_CATEGORY, 27, "Conflict resolution failed");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode WC_COPYFROM_PATH_NOT_FOUND = new SVNErrorCode(WC_CATEGORY, 28, "Failed to locate 'copyfrom' path in working copy");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode WC_CHANGELIST_MOVE = new SVNErrorCode(WC_CATEGORY, 29, "Moving a path from one changelist to another");
    
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode WC_CANNOT_DELETE_FILE_EXTERNAL =  new SVNErrorCode(WC_CATEGORY, 30, "Cannot delete a file external");

    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNErrorCode WC_CANNOT_MOVE_FILE_EXTERNAL =  new SVNErrorCode(WC_CATEGORY, 31, "Cannot move a file external");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_DB_ERROR = new SVNErrorCode(WC_CATEGORY, 32, "Something's amiss with the wc sqlite database");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_MISSING = new SVNErrorCode(WC_CATEGORY, 33, "The working copy is missing");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_NOT_SYMLINK = new SVNErrorCode(WC_CATEGORY, 34, "The specified node is not a symlink");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_PATH_UNEXPECTED_STATUS = new SVNErrorCode(WC_CATEGORY, 35, "The specified path has an unexpected status");

    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_UPGRADE_REQUIRED = new SVNErrorCode(WC_CATEGORY, 36, "The working copy needs to be upgraded");
    
    /**
     * @since 1.4, SVN 1.7
     */
    public static final SVNErrorCode WC_CLEANUP_REQUIRED = new SVNErrorCode(WC_CATEGORY, 37, "Previous operation was interrupted; run 'svn cleanup'");
    public static final SVNErrorCode WC_INVALID_OPERATION_DEPTH = new SVNErrorCode(WC_CATEGORY, 38, "The operation can not be performed with the specified depth");
    
    public static final SVNErrorCode FS_GENERAL = new SVNErrorCode(FS_CATEGORY, 0, "General filesystem error");
    public static final SVNErrorCode FS_CLEANUP = new SVNErrorCode(FS_CATEGORY, 1, "Error closing filesystem");
    public static final SVNErrorCode FS_ALREADY_OPEN = new SVNErrorCode(FS_CATEGORY, 2, "Filesystem is already open");
    public static final SVNErrorCode FS_NOT_OPEN = new SVNErrorCode(FS_CATEGORY, 3, "Filesystem is not open");
    public static final SVNErrorCode FS_CORRUPT = new SVNErrorCode(FS_CATEGORY, 4, "Filesystem is corrupt");
    public static final SVNErrorCode FS_PATH_SYNTAX = new SVNErrorCode(FS_CATEGORY, 5, "Invalid filesystem path syntax");
    public static final SVNErrorCode FS_NO_SUCH_REVISION = new SVNErrorCode(FS_CATEGORY, 6, "Invalid filesystem revision number");
    public static final SVNErrorCode FS_NO_SUCH_TRANSACTION = new SVNErrorCode(FS_CATEGORY, 7, "Invalid filesystem transaction name");
    public static final SVNErrorCode FS_NO_SUCH_ENTRY = new SVNErrorCode(FS_CATEGORY, 8, "Filesystem directory has no such entry");
    public static final SVNErrorCode FS_NO_SUCH_REPRESENTATION = new SVNErrorCode(FS_CATEGORY, 9, "Filesystem has no such representation");
    public static final SVNErrorCode FS_NO_SUCH_STRING = new SVNErrorCode(FS_CATEGORY, 10, "Filesystem has no such string");
    public static final SVNErrorCode FS_NO_SUCH_COPY = new SVNErrorCode(FS_CATEGORY, 11, "Filesystem has no such copy");
    public static final SVNErrorCode FS_TRANSACTION_NOT_MUTABLE = new SVNErrorCode(FS_CATEGORY, 12, "The specified transaction is not mutable");
    public static final SVNErrorCode FS_NOT_FOUND = new SVNErrorCode(FS_CATEGORY, 13, "Filesystem has no item");
    public static final SVNErrorCode FS_ID_NOT_FOUND = new SVNErrorCode(FS_CATEGORY, 14, "Filesystem has no such node-rev-id");
    public static final SVNErrorCode FS_NOT_ID = new SVNErrorCode(FS_CATEGORY, 15, "String does not represent a node or node-rev-id");
    public static final SVNErrorCode FS_NOT_DIRECTORY = new SVNErrorCode(FS_CATEGORY, 16, "Name does not refer to a filesystem directory");
    public static final SVNErrorCode FS_NOT_FILE = new SVNErrorCode(FS_CATEGORY, 17, "Name does not refer to a filesystem file");
    public static final SVNErrorCode FS_NOT_SINGLE_PATH_COMPONENT = new SVNErrorCode(FS_CATEGORY, 18, "Name is not a single path component");
    public static final SVNErrorCode FS_NOT_MUTABLE = new SVNErrorCode(FS_CATEGORY, 19, "Attempt to change immutable filesystem node");
    public static final SVNErrorCode FS_ALREADY_EXISTS = new SVNErrorCode(FS_CATEGORY, 20, "Item already exists in filesystem");
    public static final SVNErrorCode FS_ROOT_DIR = new SVNErrorCode(FS_CATEGORY, 21, "Attempt to remove or recreate fs root dir");
    public static final SVNErrorCode FS_NOT_TXN_ROOT = new SVNErrorCode(FS_CATEGORY, 22, "Object is not a transaction root");
    public static final SVNErrorCode FS_NOT_REVISION_ROOT = new SVNErrorCode(FS_CATEGORY, 23, "Object is not a revision root");
    public static final SVNErrorCode FS_CONFLICT = new SVNErrorCode(FS_CATEGORY, 24, "Merge conflict during commit");
    public static final SVNErrorCode FS_REP_CHANGED = new SVNErrorCode(FS_CATEGORY, 25, "A representation vanished or changed between reads");
    public static final SVNErrorCode FS_REP_NOT_MUTABLE = new SVNErrorCode(FS_CATEGORY, 26, "Tried to change an immutable representation");
    public static final SVNErrorCode FS_MALFORMED_SKEL = new SVNErrorCode(FS_CATEGORY, 27, "Malformed skeleton data");
    public static final SVNErrorCode FS_TXN_OUT_OF_DATE = new SVNErrorCode(FS_CATEGORY, 28, "Transaction is out of date");
    public static final SVNErrorCode FS_BERKELEY_DB = new SVNErrorCode(FS_CATEGORY, 29, "Berkeley DB error");
    public static final SVNErrorCode FS_BERKELEY_DB_DEADLOCK = new SVNErrorCode(FS_CATEGORY, 30, "Berkeley DB deadlock error");
    public static final SVNErrorCode FS_TRANSACTION_DEAD = new SVNErrorCode(FS_CATEGORY, 31, "Transaction is dead");
    public static final SVNErrorCode FS_TRANSACTION_NOT_DEAD = new SVNErrorCode(FS_CATEGORY, 32, "Transaction is not dead");
    public static final SVNErrorCode FS_UNKNOWN_FS_TYPE = new SVNErrorCode(FS_CATEGORY, 33, "Unknown FS type");
    public static final SVNErrorCode FS_NO_USER = new SVNErrorCode(FS_CATEGORY, 34, "No user associated with filesystem");
    public static final SVNErrorCode FS_PATH_ALREADY_LOCKED = new SVNErrorCode(FS_CATEGORY, 35, "Path is already locked");
    public static final SVNErrorCode FS_PATH_NOT_LOCKED = new SVNErrorCode(FS_CATEGORY, 36, "Path is not locked");
    public static final SVNErrorCode FS_BAD_LOCK_TOKEN = new SVNErrorCode(FS_CATEGORY, 37, "Lock token is incorrect");
    public static final SVNErrorCode FS_NO_LOCK_TOKEN = new SVNErrorCode(FS_CATEGORY, 38, "No lock token provided");
    public static final SVNErrorCode FS_LOCK_OWNER_MISMATCH = new SVNErrorCode(FS_CATEGORY, 39, "Username does not match lock owner");
    public static final SVNErrorCode FS_NO_SUCH_LOCK = new SVNErrorCode(FS_CATEGORY, 40, "Filesystem has no such lock");
    public static final SVNErrorCode FS_LOCK_EXPIRED = new SVNErrorCode(FS_CATEGORY, 41, "Lock has expired");
    public static final SVNErrorCode FS_OUT_OF_DATE = new SVNErrorCode(FS_CATEGORY, 42, "Item is out of date");
    public static final SVNErrorCode FS_UNSUPPORTED_FORMAT = new SVNErrorCode(FS_CATEGORY, 43, "Unsupported FS format");
    public static final SVNErrorCode FS_REP_BEING_WRITTEN = new SVNErrorCode(FS_CATEGORY, 44, "Representation is being written");
    public static final SVNErrorCode FS_TXN_NAME_TOO_LONG = new SVNErrorCode(FS_CATEGORY, 45, "The generated transaction name is too long");
    public static final SVNErrorCode FS_NO_SUCH_NODE_ORIGIN = new SVNErrorCode(FS_CATEGORY, 46, "Filesystem has no such node origin record");
    
    public static final SVNErrorCode REPOS_LOCKED = new SVNErrorCode(REPOS_CATEGORY, 0, "The repository is locked, perhaps for db recovery");
    public static final SVNErrorCode REPOS_HOOK_FAILURE = new SVNErrorCode(REPOS_CATEGORY, 1, "A repository hook failed");
    public static final SVNErrorCode REPOS_BAD_ARGS = new SVNErrorCode(REPOS_CATEGORY, 2, "Incorrect arguments supplied");
    public static final SVNErrorCode REPOS_NO_DATA_FOR_REPORT = new SVNErrorCode(REPOS_CATEGORY, 3, "A report cannot be generated because no data was supplied");
    public static final SVNErrorCode REPOS_BAD_REVISION_REPORT = new SVNErrorCode(REPOS_CATEGORY, 4, "Bogus revision report");
    public static final SVNErrorCode REPOS_UNSUPPORTED_VERSION = new SVNErrorCode(REPOS_CATEGORY, 5, "Unsupported repository version");
    public static final SVNErrorCode REPOS_DISABLED_FEATURE = new SVNErrorCode(REPOS_CATEGORY, 6, "Disabled repository feature");
    public static final SVNErrorCode REPOS_POST_COMMIT_HOOK_FAILED = new SVNErrorCode(REPOS_CATEGORY, 7, "Error running post-commit hook");
    public static final SVNErrorCode REPOS_POST_LOCK_HOOK_FAILED = new SVNErrorCode(REPOS_CATEGORY, 8, "Error running post-lock hook");
    public static final SVNErrorCode REPOS_POST_UNLOCK_HOOK_FAILED = new SVNErrorCode(REPOS_CATEGORY, 9, "Error running post-unlock hook");
    
    public static final SVNErrorCode RA_ILLEGAL_URL = new SVNErrorCode(RA_CATEGORY, 0, "Bad URL passed to RA layer");
    public static final SVNErrorCode RA_NOT_AUTHORIZED = new SVNErrorCode(RA_CATEGORY, 1, "Authorization failed");
    public static final SVNErrorCode RA_UNKNOWN_AUTH = new SVNErrorCode(RA_CATEGORY, 2, "Unknown authorization method");
    public static final SVNErrorCode RA_NOT_IMPLEMENTED = new SVNErrorCode(RA_CATEGORY, 3, "Repository access method not implemented");
    public static final SVNErrorCode RA_OUT_OF_DATE = new SVNErrorCode(RA_CATEGORY, 4, "Item is out-of-date");
    public static final SVNErrorCode RA_NO_REPOS_UUID = new SVNErrorCode(RA_CATEGORY, 5, "Repository has no UUID");
    public static final SVNErrorCode RA_UNSUPPORTED_ABI_VERSION = new SVNErrorCode(RA_CATEGORY, 6, "Unsupported RA plugin ABI version");
    public static final SVNErrorCode RA_NOT_LOCKED = new SVNErrorCode(RA_CATEGORY, 7, "Path is not locked");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode RA_PARTIAL_REPLAY_NOT_SUPPORTED = new SVNErrorCode(RA_CATEGORY, 8, "Server can only replay from the root of a repository");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode RA_UUID_MISMATCH = new SVNErrorCode(RA_CATEGORY, 9, "Repository UUID does not match expected UUID");

    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode RA_REPOS_ROOT_URL_MISMATCH = new SVNErrorCode(RA_CATEGORY, 10, "Repository root URL does not match expected root URL");
    
    public static final SVNErrorCode RA_DAV_SOCK_INIT = new SVNErrorCode(RA_DAV_CATEGORY, 0, "RA layer failed to init socket layer");
    public static final SVNErrorCode RA_DAV_CREATING_REQUEST = new SVNErrorCode(RA_DAV_CATEGORY, 1, "RA layer failed to create HTTP request");
    public static final SVNErrorCode RA_DAV_REQUEST_FAILED = new SVNErrorCode(RA_DAV_CATEGORY, 2, "RA layer request failed");
    public static final SVNErrorCode RA_DAV_OPTIONS_REQ_FAILED = new SVNErrorCode(RA_DAV_CATEGORY, 3, "RA layer didn't receive requested OPTIONS info");
    public static final SVNErrorCode RA_DAV_PROPS_NOT_FOUND = new SVNErrorCode(RA_DAV_CATEGORY, 4, "RA layer failed to fetch properties");
    public static final SVNErrorCode RA_DAV_ALREADY_EXISTS = new SVNErrorCode(RA_DAV_CATEGORY, 5, "RA layer file already exists");
    public static final SVNErrorCode RA_DAV_INVALID_CONFIG_VALUE = new SVNErrorCode(RA_DAV_CATEGORY, 6, "Invalid configuration value");
    public static final SVNErrorCode RA_DAV_PATH_NOT_FOUND = new SVNErrorCode(RA_DAV_CATEGORY, 7, "HTTP Path Not Found");
    public static final SVNErrorCode RA_DAV_PROPPATCH_FAILED = new SVNErrorCode(RA_DAV_CATEGORY, 8, "Failed to execute WebDAV PROPPATCH");
    public static final SVNErrorCode RA_DAV_MALFORMED_DATA = new SVNErrorCode(RA_DAV_CATEGORY, 9, "Malformed network data");
    public static final SVNErrorCode RA_DAV_RESPONSE_HEADER_BADNESS = new SVNErrorCode(RA_DAV_CATEGORY, 10, "Unable to extract data from response header");
    
    /**
     * @since 1.2.0, SVN 1.5
     */
    public static final SVNErrorCode RA_DAV_RELOCATED = new SVNErrorCode(RA_DAV_CATEGORY, 11, "Repository has been moved");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode RA_DAV_CONN_TIMEOUT = new SVNErrorCode(RA_DAV_CATEGORY, 12, "Connection timed out");

    /**
     * @since 1.7, SVN 1.6
     */
    public static final SVNErrorCode RA_DAV_FORBIDDEN = new SVNErrorCode(RA_DAV_CATEGORY, 13, "Connection timed out");

    public static final SVNErrorCode RA_LOCAL_REPOS_NOT_FOUND = new SVNErrorCode(RA_LOCAL_CATEGORY, 0, "Couldn't find a repository");
    public static final SVNErrorCode RA_LOCAL_REPOS_OPEN_FAILED = new SVNErrorCode(RA_LOCAL_CATEGORY, 1, "Couldn't open a repository");
    
    public static final SVNErrorCode RA_SVN_CMD_ERR = new SVNErrorCode(RA_SVN_CATEGORY, 0, "Special code for wrapping server errors to report to client");
    public static final SVNErrorCode RA_SVN_UNKNOWN_CMD = new SVNErrorCode(RA_SVN_CATEGORY, 1, "Unknown svn protocol command");
    public static final SVNErrorCode RA_SVN_CONNECTION_CLOSED = new SVNErrorCode(RA_SVN_CATEGORY, 2, "Network connection closed unexpectedly");
    public static final SVNErrorCode RA_SVN_IO_ERROR = new SVNErrorCode(RA_SVN_CATEGORY, 3, "Network read/write error");
    public static final SVNErrorCode RA_SVN_MALFORMED_DATA = new SVNErrorCode(RA_SVN_CATEGORY, 4, "Malformed network data");
    public static final SVNErrorCode RA_SVN_REPOS_NOT_FOUND = new SVNErrorCode(RA_SVN_CATEGORY, 5, "Couldn't find a repository");
    public static final SVNErrorCode RA_SVN_BAD_VERSION = new SVNErrorCode(RA_SVN_CATEGORY, 6, "Client/server version mismatch");
    
    public static final SVNErrorCode AUTHN_CREDS_UNAVAILABLE = new SVNErrorCode(AUTHN_CATEGORY, 0, "Credential data unavailable");
    public static final SVNErrorCode AUTHN_NO_PROVIDER = new SVNErrorCode(AUTHN_CATEGORY, 1, "No authentication provider available");
    public static final SVNErrorCode AUTHN_PROVIDERS_EXHAUSTED = new SVNErrorCode(AUTHN_CATEGORY, 2, "All authentication providers exhausted");
    public static final SVNErrorCode AUTHN_CREDS_NOT_SAVED = new SVNErrorCode(AUTHN_CATEGORY, 3, "Credentials not saved");
    
    public static final SVNErrorCode AUTHZ_ROOT_UNREADABLE = new SVNErrorCode(AUTHZ_CATEGORY, 0, "Read access denied for root of edit");
    public static final SVNErrorCode AUTHZ_UNREADABLE = new SVNErrorCode(AUTHZ_CATEGORY, 1, "Item is not readable");
    public static final SVNErrorCode AUTHZ_PARTIALLY_READABLE = new SVNErrorCode(AUTHZ_CATEGORY, 2, "Item is partially readable");
    public static final SVNErrorCode AUTHZ_INVALID_CONFIG = new SVNErrorCode(AUTHZ_CATEGORY, 3, "Invalid authz configuration");
    public static final SVNErrorCode AUTHZ_UNWRITABLE = new SVNErrorCode(AUTHZ_CATEGORY, 4, "Item is not writable");
    
    public static final SVNErrorCode SVNDIFF_INVALID_HEADER = new SVNErrorCode(SVNDIFF_CATEGORY, 0, "Svndiff data has invalid header");
    public static final SVNErrorCode SVNDIFF_CORRUPT_WINDOW = new SVNErrorCode(SVNDIFF_CATEGORY, 1, "Svndiff data contains corrupt window");
    public static final SVNErrorCode SVNDIFF_BACKWARD_VIEW = new SVNErrorCode(SVNDIFF_CATEGORY, 2, "Svndiff data contains backward-sliding source view");
    public static final SVNErrorCode SVNDIFF_INVALID_OPS = new SVNErrorCode(SVNDIFF_CATEGORY, 3, "Svndiff data contains invalid instruction");
    public static final SVNErrorCode SVNDIFF_UNEXPECTED_END = new SVNErrorCode(SVNDIFF_CATEGORY, 4, "Svndiff data ends unexpectedly");
    
    public static final SVNErrorCode APMOD_MISSING_PATH_TO_FS = new SVNErrorCode(APMOD_CATEGORY, 0, "Apache has no path to an SVN filesystem");
    public static final SVNErrorCode APMOD_MALFORMED_URI = new SVNErrorCode(APMOD_CATEGORY, 1, "Apache got a malformed URI");
    public static final SVNErrorCode APMOD_ACTIVITY_NOT_FOUND = new SVNErrorCode(APMOD_CATEGORY, 2, "Activity not found");
    public static final SVNErrorCode APMOD_BAD_BASELINE = new SVNErrorCode(APMOD_CATEGORY, 3, "Baseline incorrect");
    public static final SVNErrorCode APMOD_CONNECTION_ABORTED = new SVNErrorCode(APMOD_CATEGORY, 4, "Input/output error");
    
    public static final SVNErrorCode CLIENT_VERSIONED_PATH_REQUIRED = new SVNErrorCode(CLIENT_CATEGORY, 0, "A path under version control is needed for this operation");
    public static final SVNErrorCode CLIENT_RA_ACCESS_REQUIRED = new SVNErrorCode(CLIENT_CATEGORY, 1, "Repository access is needed for this operation");
    public static final SVNErrorCode CLIENT_BAD_REVISION = new SVNErrorCode(CLIENT_CATEGORY, 2, "Bogus revision information given");
    public static final SVNErrorCode CLIENT_DUPLICATE_COMMIT_URL = new SVNErrorCode(CLIENT_CATEGORY, 3, "Attempting to commit to a URL more than once");
    public static final SVNErrorCode CLIENT_IS_BINARY_FILE = new SVNErrorCode(CLIENT_CATEGORY, 4, "Operation does not apply to binary file");
    public static final SVNErrorCode CLIENT_INVALID_EXTERNALS_DESCRIPTION = new SVNErrorCode(CLIENT_CATEGORY, 5, "Format of an svn:externals property was invalid");
    public static final SVNErrorCode CLIENT_MODIFIED = new SVNErrorCode(CLIENT_CATEGORY, 6, "Attempting restricted operation for modified resource");
    public static final SVNErrorCode CLIENT_IS_DIRECTORY = new SVNErrorCode(CLIENT_CATEGORY, 7, "Operation does not apply to directory");
    public static final SVNErrorCode CLIENT_REVISION_RANGE = new SVNErrorCode(CLIENT_CATEGORY, 8, "Revision range is not allowed");
    public static final SVNErrorCode CLIENT_INVALID_RELOCATION = new SVNErrorCode(CLIENT_CATEGORY, 9, "Inter-repository relocation not allowed");
    public static final SVNErrorCode CLIENT_REVISION_AUTHOR_CONTAINS_NEWLINE = new SVNErrorCode(CLIENT_CATEGORY, 10, "Author name cannot contain a newline");
    public static final SVNErrorCode CLIENT_PROPERTY_NAME = new SVNErrorCode(CLIENT_CATEGORY, 11, "Bad property name");
    public static final SVNErrorCode CLIENT_UNRELATED_RESOURCES = new SVNErrorCode(CLIENT_CATEGORY, 12, "Two versioned resources are unrelated");
    public static final SVNErrorCode CLIENT_MISSING_LOCK_TOKEN = new SVNErrorCode(CLIENT_CATEGORY, 13, "Path has no lock token");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode CLIENT_MULTIPLE_SOURCES_DISALLOWED = new SVNErrorCode(CLIENT_CATEGORY, 14, 
            "Operation does not support multiple sources");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode CLIENT_NO_VERSIONED_PARENT = new SVNErrorCode(CLIENT_CATEGORY, 15, 
            "No versioned parent directories");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode CLIENT_NOT_READY_TO_MERGE = new SVNErrorCode(CLIENT_CATEGORY, 16, 
            "Working copy and merge source not ready for reintegration");

    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode CLIENT_FILE_EXTERNAL_OVERWRITE_VERSIONED = new SVNErrorCode(CLIENT_CATEGORY, 17, 
            "A file external cannot overwrite an existing versioned item");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_BAD_STRIP_COUNT = new SVNErrorCode(CLIENT_CATEGORY, 18,
            "Invalid path component strip count specified");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_CYCLE_DETECTED = new SVNErrorCode(CLIENT_CATEGORY, 19,
            "Detected a cycle while processing the operation");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_MERGE_UPDATE_REQUIRED = new SVNErrorCode(CLIENT_CATEGORY, 20,
            "Working copy and merge source not ready for reintegration");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_INVALID_MERGEINFO_NO_MERGETRACKING = new SVNErrorCode(CLIENT_CATEGORY, 21,
            "Invalid mergeinfo detected in merge target");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_NO_LOCK_TOKEN = new SVNErrorCode(CLIENT_CATEGORY, 22,
            "Can't perform this operation without a valid lock token");

    /**
     * @since 1.7, SVN 1.7
     */
    public static final SVNErrorCode CLIENT_FORBIDDEN_BY_SERVER = new SVNErrorCode(CLIENT_CATEGORY, 23,
            "The operation is forbidden by the server");

    
    public static final SVNErrorCode BASE = new SVNErrorCode(MISC_CATEGORY, 0, "A problem occurred; see later errors for details");
    public static final SVNErrorCode PLUGIN_LOAD_FAILURE = new SVNErrorCode(MISC_CATEGORY, 1, "Failure loading plugin");    
    public static final SVNErrorCode MALFORMED_FILE = new SVNErrorCode(MISC_CATEGORY, 2, "Malformed file");
    public static final SVNErrorCode INCOMPLETE_DATA = new SVNErrorCode(MISC_CATEGORY, 3, "Incomplete data");
    public static final SVNErrorCode INCORRECT_PARAMS = new SVNErrorCode(MISC_CATEGORY, 4, "Incorrect parameters given");
    public static final SVNErrorCode UNVERSIONED_RESOURCE = new SVNErrorCode(MISC_CATEGORY, 5, "Tried a versioning operation on an unversioned resource");
    public static final SVNErrorCode TEST_FAILED = new SVNErrorCode(MISC_CATEGORY, 6, "Test failed");
    public static final SVNErrorCode UNSUPPORTED_FEATURE = new SVNErrorCode(MISC_CATEGORY, 7, "Trying to use an unsupported feature");
    public static final SVNErrorCode BAD_PROP_KIND = new SVNErrorCode(MISC_CATEGORY, 8, "Unexpected or unknown property kind");
    public static final SVNErrorCode ILLEGAL_TARGET = new SVNErrorCode(MISC_CATEGORY, 9, "Illegal target for the requested operation");
    public static final SVNErrorCode DELTA_MD5_CHECKSUM_ABSENT = new SVNErrorCode(MISC_CATEGORY, 10, "MD5 checksum is missing");
    public static final SVNErrorCode DIR_NOT_EMPTY = new SVNErrorCode(MISC_CATEGORY, 11, "Directory needs to be empty but is not");
    public static final SVNErrorCode EXTERNAL_PROGRAM = new SVNErrorCode(MISC_CATEGORY, 12, "Error calling external program");
    public static final SVNErrorCode SWIG_PY_EXCEPTION_SET = new SVNErrorCode(MISC_CATEGORY, 13, "Python exception has been set with the error");
    public static final SVNErrorCode CHECKSUM_MISMATCH = new SVNErrorCode(MISC_CATEGORY, 14, "A checksum mismatch occurred");
    public static final SVNErrorCode CANCELLED = new SVNErrorCode(MISC_CATEGORY, 15, "The operation was interrupted");
    public static final SVNErrorCode INVALID_DIFF_OPTION = new SVNErrorCode(MISC_CATEGORY, 16, "The specified diff option is not supported");
    public static final SVNErrorCode PROPERTY_NOT_FOUND = new SVNErrorCode(MISC_CATEGORY, 17, "Property not found");
    public static final SVNErrorCode NO_AUTH_FILE_PATH = new SVNErrorCode(MISC_CATEGORY, 18, "No auth file path available");
    public static final SVNErrorCode VERSION_MISMATCH = new SVNErrorCode(MISC_CATEGORY, 19, "Incompatible library version");
    
    
    

        /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode MERGE_INFO_PARSE_ERROR = new SVNErrorCode(MISC_CATEGORY, 20, "Merge info parse error");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode CEASE_INVOCATION = new SVNErrorCode(MISC_CATEGORY, 21, "Cease invocation of this API");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode REVISION_NUMBER_PARSE_ERROR = new SVNErrorCode(MISC_CATEGORY, 22, "Revision number parse error");
    
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode ITER_BREAK = new SVNErrorCode(MISC_CATEGORY, 23, "Iteration terminated before completion");
    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode UNKNOWN_CHANGELIST = new SVNErrorCode(MISC_CATEGORY, 24, "Unknown changelist");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode RESERVED_FILENAME_SPECIFIED = new SVNErrorCode(MISC_CATEGORY, 25, "Reserved directory name in command line arguments");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode UNKNOWN_CAPABILITY = new SVNErrorCode(MISC_CATEGORY, 26, "Inquiry about unknown capability");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode TEST_SKIPPED = new SVNErrorCode(MISC_CATEGORY, 27, "Test skipped");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode ATOMIC_INIT_FAILURE = new SVNErrorCode(MISC_CATEGORY, 29, "Couldn't perform atomic initialization");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode SQLITE_ERROR = new SVNErrorCode(MISC_CATEGORY, 30, "SQLite error");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode SQLITE_READONLY = new SVNErrorCode(MISC_CATEGORY, 31, "Attempted to write to readonly SQLite db");
    /**
     * @since  1.3, SVN 1.6
     */
    public static final SVNErrorCode UNSUPPORTED_SCHEMA = new SVNErrorCode(MISC_CATEGORY, 32, "Unsupported schema found in SQLite db");
    
    public static final SVNErrorCode CL_ARG_PARSING_ERROR = new SVNErrorCode(CL_CATEGORY, 0, "Client error in parsing arguments");
    public static final SVNErrorCode CL_INSUFFICIENT_ARGS = new SVNErrorCode(CL_CATEGORY, 1, "Not enough args provided");
    public static final SVNErrorCode CL_MUTUALLY_EXCLUSIVE_ARGS = new SVNErrorCode(CL_CATEGORY, 2, "Mutually exclusive arguments specified");
    public static final SVNErrorCode CL_ADM_DIR_RESERVED = new SVNErrorCode(CL_CATEGORY, 3, "Attempted command in administrative dir");
    public static final SVNErrorCode CL_LOG_MESSAGE_IS_VERSIONED_FILE = new SVNErrorCode(CL_CATEGORY, 4, "The log message file is under version control");
    public static final SVNErrorCode CL_LOG_MESSAGE_IS_PATHNAME = new SVNErrorCode(CL_CATEGORY, 5, "The log message is a pathname");
    public static final SVNErrorCode CL_COMMIT_IN_ADDED_DIR = new SVNErrorCode(CL_CATEGORY, 6, "Committing in directory scheduled for addition");
    public static final SVNErrorCode CL_NO_EXTERNAL_EDITOR = new SVNErrorCode(CL_CATEGORY, 7, "No external editor available");
    public static final SVNErrorCode CL_BAD_LOG_MESSAGE = new SVNErrorCode(CL_CATEGORY, 8, "Something is wrong with the log message's contents");
    public static final SVNErrorCode CL_UNNECESSARY_LOG_MESSAGE = new SVNErrorCode(CL_CATEGORY, 9, "A log message was given where none was necessary");

    /**
     * @since  1.2.0, SVN 1.5
     */
    public static final SVNErrorCode CL_NO_EXTERNAL_MERGE_TOOL = new SVNErrorCode(CL_CATEGORY, 10, "No external merge tool available");
    public static final SVNErrorCode CL_ERROR_PROCESSING_EXTERNALS = new SVNErrorCode(CL_CATEGORY, 11, "Failed processing one or more externals definitions");
    /**
     * @since 1.3.3
     */
    public static final SVNErrorCode ASSERTION_FAIL = new SVNErrorCode(MALFUNC_CATEGORY, 0, "Assertion failure");
    
    /** 
     * @since New in 1.7. 
     */
    public static final SVNErrorCode BAD_CHANGELIST_NAME = new SVNErrorCode(BAD_CATEGORY, 14, "Invalid changelist name");


}

