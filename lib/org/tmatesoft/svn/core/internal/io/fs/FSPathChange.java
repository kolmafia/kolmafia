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
package org.tmatesoft.svn.core.internal.io.fs;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSPathChange extends SVNLogEntryPath {
    
    private static final long serialVersionUID = 4845L;
    
    private static final String FLAG_TRUE = "true";
    private static final String FLAG_FALSE = "false";

    private String myPath;
    private FSID myRevNodeId;
    private FSPathChangeKind myChangeKind;
    boolean isTextModified;
    boolean arePropertiesModified;
    
    public FSPathChange(String path, FSID id, FSPathChangeKind kind, boolean textModified, boolean propsModified, String copyfromPath, long copyfromRevision,
            SVNNodeKind pathKind) {
        super(path, FSPathChangeKind.getType(kind), copyfromPath, copyfromRevision, pathKind);
        myPath = path;
        myRevNodeId = id;
        myChangeKind = kind;
        isTextModified = textModified;
        arePropertiesModified = propsModified;
    }

    public String getPath(){
        return myPath;
    }

    public boolean arePropertiesModified() {
        return arePropertiesModified;
    }

    public void setPropertiesModified(boolean propertiesModified) {
        arePropertiesModified = propertiesModified;
    }
    
    public boolean isTextModified() {
        return isTextModified;
    }

    public void setTextModified(boolean textModified) {
        isTextModified = textModified;
    }
    
    public FSPathChangeKind getChangeKind() {
        return myChangeKind;
    }

    public void setChangeKind(FSPathChangeKind changeKind) {
        myChangeKind = changeKind;
        super.setChangeType(FSPathChangeKind.getType(changeKind));
    }

    public FSID getRevNodeId() {
        return myRevNodeId;
    }
    
    public void setRevNodeId(FSID revNodeId) {
        myRevNodeId = revNodeId;
    }

    public void setCopyRevision(long revision) {
        super.setCopyRevision(revision);
    }
    
    public void setCopyPath(String path) {
        super.setCopyPath(path);
    }

    public void setNodeKind(SVNNodeKind nodeKind) {
        super.setNodeKind(nodeKind);
    }

    public static FSPathChange fromString(String changeLine, String copyfromLine) throws SVNException {
        int delimiterInd = changeLine.indexOf(' ');

        //String[] piecesOfChangeLine = changeLine.split(" ", 5);
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        String id = changeLine.substring(0, delimiterInd);
        FSID nodeRevID = FSID.fromString(id);
        
        changeLine = changeLine.substring(delimiterInd + 1);
        delimiterInd = changeLine.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String changesKindStr = changeLine.substring(0, delimiterInd);
        int dashIndex = changesKindStr.indexOf("-");
        SVNNodeKind nodeKind = SVNNodeKind.UNKNOWN;
        if (dashIndex >=0) {
            String nodeKindStr = changesKindStr.substring(dashIndex + 1);
            changesKindStr = changesKindStr.substring(0, dashIndex);
            if (SVNNodeKind.FILE.toString().equals(nodeKindStr)) {
                nodeKind = SVNNodeKind.FILE;
            } else if (SVNNodeKind.DIR.toString().equals(nodeKindStr)) {
                nodeKind = SVNNodeKind.DIR;
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
        FSPathChangeKind changesKind = FSPathChangeKind.fromString(changesKindStr);
        if (changesKind == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid change kind in rev file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        changeLine = changeLine.substring(delimiterInd + 1);
        delimiterInd = changeLine.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String textModeStr = changeLine.substring(0, delimiterInd);
        
        boolean textModeBool = false;
        if (FSPathChange.FLAG_TRUE.equals(textModeStr)) {
            textModeBool = true;
        } else if (FSPathChange.FLAG_FALSE.equals(textModeStr)) {
            textModeBool = false;
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid text-mod flag in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        changeLine = changeLine.substring(delimiterInd + 1);
        delimiterInd = changeLine.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String propModeStr = changeLine.substring(0, delimiterInd);
        
        boolean propModeBool = false;
        if (FSPathChange.FLAG_TRUE.equals(propModeStr)) {
            propModeBool = true;
        } else if (FSPathChange.FLAG_FALSE.equals(propModeStr)) {
            propModeBool = false;
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid prop-mod flag in rev-file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        String pathStr = changeLine.substring(delimiterInd + 1);
        
        String copyfromPath = null;
        long copyfromRevision = SVNRepository.INVALID_REVISION;
        
        if (copyfromLine != null && copyfromLine.length() != 0) {
            delimiterInd = copyfromLine.indexOf(' ');

            if (delimiterInd == -1) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid changes line in rev-file");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            try {
                copyfromRevision = Long.parseLong(copyfromLine.substring(0, delimiterInd));
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, nfe), SVNLogType.FSFS);
            }
            copyfromPath = copyfromLine.substring(delimiterInd + 1);
        }

        return new FSPathChange(pathStr, nodeRevID, changesKind, textModeBool, propModeBool, copyfromPath, copyfromRevision, nodeKind);
    }

}
