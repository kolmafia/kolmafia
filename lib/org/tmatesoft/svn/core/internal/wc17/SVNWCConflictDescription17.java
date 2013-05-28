/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNPropertyConflictDescription;
import org.tmatesoft.svn.core.wc.SVNTextConflictDescription;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;

public class SVNWCConflictDescription17 {

    public static enum ConflictKind {
        TEXT, PROPERTY, TREE;
    }

    private File localAbspath;
    private SVNNodeKind nodeKind;
    private ConflictKind kind;
    private String propertyName;
    private boolean isBinary;
    private String mimeType;
    private SVNConflictAction action;
    private SVNConflictReason reason;
    private File baseFile;
    private File theirFile;
    private File myFile;
    private File mergedFile;
    private SVNOperation operation;
    private SVNConflictVersion srcLeftVersion;
    private SVNConflictVersion srcRightVersion;

    public static SVNWCConflictDescription17 createText(File localAbspath) {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNWCConflictDescription17 conflict = new SVNWCConflictDescription17();
        conflict.localAbspath = localAbspath;
        conflict.nodeKind = SVNNodeKind.FILE;
        conflict.kind = ConflictKind.TEXT;
        conflict.action = SVNConflictAction.EDIT;
        conflict.reason = SVNConflictReason.EDITED;
        return conflict;
    }

    public static SVNWCConflictDescription17 createProp(File localAbspath, SVNNodeKind nodeKind, String propertyName) {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNWCConflictDescription17 conflict = new SVNWCConflictDescription17();
        conflict.localAbspath = localAbspath;
        conflict.nodeKind = nodeKind;
        conflict.kind = ConflictKind.PROPERTY;
        conflict.propertyName = propertyName;
        return conflict;
    }

    public static SVNWCConflictDescription17 createTree(File localAbspath, SVNNodeKind nodeKind, SVNOperation operation, SVNConflictVersion srcLeftVersion, SVNConflictVersion srcRightVersion) {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNWCConflictDescription17 conflict = new SVNWCConflictDescription17();
        conflict.localAbspath = localAbspath;
        conflict.nodeKind = nodeKind;
        conflict.kind = ConflictKind.TREE;
        conflict.operation = operation;
        conflict.srcLeftVersion = srcLeftVersion;
        conflict.srcRightVersion = srcRightVersion;
        return conflict;
    }

    public SVNConflictDescription toConflictDescription() {
        String wcPath = localAbspath != null ? localAbspath.getPath() : null;
        switch (kind) {
            case PROPERTY:
                return new SVNPropertyConflictDescription(new SVNMergeFileSet(null, null, baseFile, myFile, wcPath, theirFile, mergedFile, null, mimeType), nodeKind, propertyName, action, reason);
            case TEXT:
                return new SVNTextConflictDescription(new SVNMergeFileSet(null, null, baseFile, myFile, wcPath, theirFile, mergedFile, null, mimeType), nodeKind, action, reason);
            case TREE:
                return new SVNTreeConflictDescription(localAbspath, nodeKind, action, reason, operation, srcLeftVersion, srcRightVersion);
        }
        return null;
    }

    public File getLocalAbspath() {
        return localAbspath;
    }

    public void setLocalAbspath(File localAbspath) {
        this.localAbspath = localAbspath;
    }

    public SVNNodeKind getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(SVNNodeKind nodeKind) {
        this.nodeKind = nodeKind;
    }

    public ConflictKind getKind() {
        return kind;
    }

    public void setKind(ConflictKind kind) {
        this.kind = kind;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public boolean isBinary() {
        return isBinary;
    }

    public void setBinary(boolean isBinary) {
        this.isBinary = isBinary;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public SVNConflictAction getAction() {
        return action;
    }

    public void setAction(SVNConflictAction action) {
        this.action = action;
    }

    public SVNConflictReason getReason() {
        return reason;
    }

    public void setReason(SVNConflictReason reason) {
        this.reason = reason;
    }

    public File getBaseFile() {
        return baseFile;
    }

    public void setBaseFile(File baseFile) {
        this.baseFile = baseFile;
    }

    public File getTheirFile() {
        return theirFile;
    }

    public void setTheirFile(File theirFile) {
        this.theirFile = theirFile;
    }

    public File getMyFile() {
        return myFile;
    }

    public void setMyFile(File file) {
        myFile = file;
    }

    public File getMergedFile() {
        return mergedFile;
    }

    public void setMergedFile(File mergedFile) {
        this.mergedFile = mergedFile;
    }

    public SVNOperation getOperation() {
        return operation;
    }

    public void setOperation(SVNOperation operation) {
        this.operation = operation;
    }

    public SVNConflictVersion getSrcLeftVersion() {
        return srcLeftVersion;
    }

    public void setSrcLeftVersion(SVNConflictVersion srcLeftVersion) {
        this.srcLeftVersion = srcLeftVersion;
    }

    public SVNConflictVersion getSrcRightVersion() {
        return srcRightVersion;
    }

    public void setSrcRightVersion(SVNConflictVersion srcRightVersion) {
        this.srcRightVersion = srcRightVersion;
    }

}
