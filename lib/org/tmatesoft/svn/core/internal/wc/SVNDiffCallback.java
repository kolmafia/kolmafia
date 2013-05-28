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
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNCharsetOutputStream;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.ISVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNDiffCallback extends AbstractDiffCallback {

    private ISVNDiffGenerator myGenerator;
    private OutputStream myResult;
    private long myRevision2;
    private long myRevision1;

    private static final SVNStatusType[] EMPTY_STATUS = {SVNStatusType.UNKNOWN, SVNStatusType.UNKNOWN};

    public SVNDiffCallback(SVNAdminArea adminArea, ISVNDiffGenerator generator, long rev1, long rev2, OutputStream result) {
        super(adminArea);
        myGenerator = generator;
        myResult = result;
        myRevision1 = rev1;
        myRevision2 = rev2;
    }

    public File createTempDirectory() throws SVNException {
        return myGenerator.createTempDirectory();
    }

    public boolean isDiffUnversioned() {
        return myGenerator.isDiffUnversioned();
    }

    public boolean isDiffCopiedAsAdded() {
        return myGenerator.isDiffCopied();
    }

    public SVNStatusType directoryAdded(String path, long revision, boolean[] isTreeConflicted) throws SVNException {
        setIsConflicted(isTreeConflicted, false);
        myGenerator.displayAddedDirectory(getDisplayPath(path), getRevision(myRevision1), getRevision(revision));
        return SVNStatusType.UNKNOWN;
    }

    public SVNStatusType directoryDeleted(String path) throws SVNException {
        myGenerator.displayDeletedDirectory(getDisplayPath(path), getRevision(myRevision1), getRevision(myRevision2));
        return SVNStatusType.UNKNOWN;
    }

    public SVNStatusType[] fileAdded(String path, File file1, File file2, long revision1, long revision2, String mimeType1, 
            String mimeType2, SVNProperties originalProperties, SVNProperties diff, boolean[] isTreeConflicted) throws SVNException {
        if (!myGenerator.isDiffAdded()) {
            return EMPTY_STATUS;
        }
        if (file2 != null) {
            displayFileDiff(path, null, file2, revision1, revision2, mimeType1, mimeType2, originalProperties, diff);
        }
        if (diff != null && !diff.isEmpty()) {
            propertiesChanged(path, originalProperties, diff, null);
        }
        return EMPTY_STATUS;
    }

    public SVNStatusType[] fileChanged(String path, File file1, File file2, long revision1, long revision2, String mimeType1,
            String mimeType2, SVNProperties originalProperties, SVNProperties diff, boolean[] isTreeConflicted) throws SVNException {
        if (file1 != null) {
            displayFileDiff(path, file1, file2, revision1, revision2, mimeType1, mimeType2, originalProperties, diff);
        }
        if (diff != null && !diff.isEmpty()) {
            propertiesChanged(path, originalProperties, diff, null);
        }
        return EMPTY_STATUS;
    }

    public SVNStatusType fileDeleted(String path, File file1, File file2, String mimeType1, String mimeType2, SVNProperties originalProperties,
            boolean[] isTreeConflicted) throws SVNException {
        if (!myGenerator.isDiffDeleted()) {
            return SVNStatusType.UNKNOWN;
        }
        if (file1 != null) {
            displayFileDiff(path, file1, file2, myRevision1, myRevision2, mimeType1, mimeType2, originalProperties, null);
        }
        return SVNStatusType.UNKNOWN;
    }

    private void displayFileDiff(String path, File file1, File file2, long revision1, long revision2, String mimeType1, String mimeType2, SVNProperties originalProperties, SVNProperties diff) throws SVNException {
        boolean resetEncoding = false;
        OutputStream result = myResult;
        String encoding = defineEncoding(originalProperties, diff);
        if (encoding != null) {
            myGenerator.setEncoding(encoding);
            resetEncoding = true;
        } else {
            String conversionEncoding = defineConversionEncoding(originalProperties, diff);
            if (conversionEncoding != null) {
                resetEncoding = adjustDiffGenerator("UTF-8");
                result = new SVNCharsetOutputStream(result, Charset.forName("UTF-8"), Charset.forName(conversionEncoding), CodingErrorAction.IGNORE, CodingErrorAction.IGNORE);
            }
        }
        try {
            myGenerator.displayFileDiff(getDisplayPath(path), file1, file2, getRevision(revision1), getRevision(revision2), mimeType1, mimeType2, result);
        } finally {
            if (resetEncoding) {
                myGenerator.setEncoding(null);
                myGenerator.setEOL(null);
            }
            if (result instanceof SVNCharsetOutputStream) {
                try {
                    result.flush();
                } catch (IOException e) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e), e, SVNLogType.WC);
                }
            }
        }
    }

    private boolean adjustDiffGenerator(String charset) {
        if (myGenerator instanceof DefaultSVNDiffGenerator) {
            DefaultSVNDiffGenerator generator = (DefaultSVNDiffGenerator) myGenerator;
            boolean encodingAdjusted = false;
            if (!generator.hasEncoding()) {
                generator.setEncoding(charset);
                encodingAdjusted = true;
            }
            if (!generator.hasEOL()) {
                byte[] eol;
                String eolString = System.getProperty("line.separator");
                try {
                    eol = eolString.getBytes(charset);
                } catch (UnsupportedEncodingException e) {
                    eol = eolString.getBytes();
                }
                generator.setEOL(eol);
            }
            return encodingAdjusted;
        }
        return false;
    }

    public SVNStatusType propertiesChanged(String path, SVNProperties originalProperties, SVNProperties diff, boolean[] isTreeConflicted) throws SVNException {
        originalProperties = originalProperties == null ? new SVNProperties() : originalProperties;
        diff = diff == null ? new SVNProperties() : diff;
        SVNProperties regularDiff = new SVNProperties();
        categorizeProperties(diff, regularDiff, null, null);
        if (regularDiff.isEmpty()) {
            return SVNStatusType.UNKNOWN;
        }
        myGenerator.displayPropDiff(getDisplayPath(path), originalProperties, regularDiff, myResult);
        return SVNStatusType.UNKNOWN;
    }

    private String getRevision(long revision) {
        if (revision >= 0) {
            return "(revision " + revision + ")";
        }
        return "(working copy)";
    }

    private String defineEncoding(SVNProperties properties, SVNProperties diff) {
        if (myGenerator instanceof DefaultSVNDiffGenerator) {
            DefaultSVNDiffGenerator defaultGenerator = (DefaultSVNDiffGenerator) myGenerator;
            if (defaultGenerator.hasEncoding()) {
                return null;
            }

            String originalEncoding = getCharsetByMimeType(properties, defaultGenerator);
            if (originalEncoding != null) {
                return originalEncoding;
            }

            String changedEncoding = getCharsetByMimeType(diff, defaultGenerator);
            if (changedEncoding != null) {
                return changedEncoding;
            }
        }
        return null;
    }

    private String defineConversionEncoding(SVNProperties properties, SVNProperties diff) {
        if (myGenerator instanceof DefaultSVNDiffGenerator) {
            DefaultSVNDiffGenerator defaultGenerator = (DefaultSVNDiffGenerator) myGenerator;
            if (defaultGenerator.hasEncoding()) {
                return null;
            }
            String originalCharset = getCharset(properties, defaultGenerator);
            if (originalCharset != null) {
                return originalCharset;
            }

            String changedCharset = getCharset(diff, defaultGenerator);
            if (changedCharset != null) {
                return changedCharset;
            }

            String globalEncoding = getCharset(defaultGenerator.getGlobalEncoding(), defaultGenerator, false);
            if (globalEncoding != null) {
                return globalEncoding;
            }
        }
        return null;
    }

    private String getCharsetByMimeType(SVNProperties properties, DefaultSVNDiffGenerator generator) {
        if (properties == null) {
            return null;
        }
        String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);
        String charset = SVNPropertiesManager.determineEncodingByMimeType(mimeType);
        return getCharset(charset, generator, false);
    }

    private String getCharset(SVNProperties properties, DefaultSVNDiffGenerator generator) {
        if (properties == null) {
            return null;
        }
        String charset = properties.getStringValue(SVNProperty.CHARSET);
        return getCharset(charset, generator, true);
    }

    private String getCharset(String charset, DefaultSVNDiffGenerator generator, boolean allowNative) {
        if (charset == null) {
            return null;
        }
        if (allowNative && SVNProperty.NATIVE.equals(charset)) {
            return generator.getEncoding();
        }
        if (Charset.isSupported(charset)) {
            return charset;
        }
        return null;
    }

    public SVNStatusType directoryDeleted(String path, boolean[] isTreeConflicted) throws SVNException {
        setIsConflicted(isTreeConflicted, false);
        return directoryDeleted(path);
    }

    public void directoryOpened(String path, long revision, boolean[] isTreeConflicted) throws SVNException {
        setIsConflicted(isTreeConflicted, false);
    }

    public SVNStatusType[] directoryClosed(String path, boolean[] isTreeConflicted) throws SVNException {
        setIsConflicted(isTreeConflicted, false);
        return EMPTY_STATUS;
    }

}
