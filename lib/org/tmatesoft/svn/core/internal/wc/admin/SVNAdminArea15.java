/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNAdminArea15 extends SVNAdminArea14 {

    public static final int WC_FORMAT = SVNAdminArea15Factory.WC_FORMAT;

    protected static final String ATTRIBUTE_KEEP_LOCAL = "keep-local";

    private static final Set INAPPLICABLE_PROPERTIES = new SVNHashSet();

    static {
        INAPPLICABLE_PROPERTIES.add(SVNProperty.FILE_EXTERNAL_PATH);
        INAPPLICABLE_PROPERTIES.add(SVNProperty.FILE_EXTERNAL_REVISION);
        INAPPLICABLE_PROPERTIES.add(SVNProperty.FILE_EXTERNAL_PEG_REVISION);
        INAPPLICABLE_PROPERTIES.add(SVNProperty.TREE_CONFLICT_DATA);
    }

    public SVNAdminArea15(File dir) {
        super(dir);
    }

    public int getFormatVersion() {
        return WC_FORMAT;
    }

    protected boolean readExtraOptions(BufferedReader reader, SVNEntry entry) throws SVNException, IOException {
        String line = reader.readLine();
        if (isEntryFinished(line)) {
            return true;
        }
        String changelist = parseString(line);
        if (changelist != null) {
            entry.setChangelistName(changelist);
        }

        line = reader.readLine();
        if (isEntryFinished(line)) {
            return true;
        }
        boolean keepLocal = parseBoolean(line, ATTRIBUTE_KEEP_LOCAL);
        if (keepLocal) {
            entry.setKeepLocal(keepLocal);
        }
        
        line = reader.readLine();
        if (isEntryFinished(line)) {
            return true;
        }
        String workingSize = parseString(line);
        if (workingSize != null) {
            try {
                long size = Long.parseLong(workingSize);
                entry.setWorkingSize(size);
            } catch (NumberFormatException nfe) {
                entry.setWorkingSize(SVNProperty.WORKING_SIZE_UNKNOWN);
            }
        }
        
        line = reader.readLine();
        if (isEntryFinished(line)) {
            return true;
        }
        String depthStr = parseValue(line);
        if (depthStr == null) {
            entry.setDepth(SVNDepth.INFINITY);
        } else {
            SVNDepth depth = SVNDepth.fromString(depthStr);            
            entry.setDepth(depth);
        }
        return false;
    }

    protected int writeExtraOptions(Writer writer, String entryName, SVNEntry entry, int emptyFields) throws SVNException, IOException {
        emptyFields = super.writeExtraOptions(writer, entryName, entry, emptyFields);
        
        String changelist = entry.getChangelistName(); 
        if (writeString(writer, changelist, emptyFields)) {
            emptyFields = 0;
        } else {
            ++emptyFields;
        }
        
        boolean keepLocalAttr = entry.isKeepLocal();
        if (keepLocalAttr) {
            writeValue(writer, ATTRIBUTE_KEEP_LOCAL, emptyFields);
            emptyFields = 0;
        } else {
            ++emptyFields;
        }

        long size = entry.getWorkingSize();
        String workingSize = Long.toString(size);
        workingSize = "-1".equals(workingSize) ? null : workingSize;
        if (writeString(writer, workingSize, emptyFields)) {
            emptyFields = 0;
        } else {
            ++emptyFields;
        }
        
        boolean isThisDir = getThisDirName().equals(entryName);
        boolean isSubDir = !isThisDir && entry.isDirectory(); 
        SVNDepth depth = entry.getDepth();
        if ((isSubDir && depth != SVNDepth.EXCLUDE) || depth == SVNDepth.INFINITY) {
            emptyFields++;
        } else {
            if (writeValue(writer, depth.toString(), emptyFields)) {
                emptyFields = 0;    
            } else {
                ++emptyFields;
            }
        }

        return emptyFields;
    }

    protected SVNAdminArea createAdminAreaForDir(File dir) {
        return new SVNAdminArea15(dir);
    }

    protected boolean isEntryPropertyApplicable(String propName) {
        return propName != null && !INAPPLICABLE_PROPERTIES.contains(propName);
    }
}
