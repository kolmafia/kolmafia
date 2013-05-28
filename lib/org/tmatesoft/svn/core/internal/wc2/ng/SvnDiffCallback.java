package org.tmatesoft.svn.core.internal.wc2.ng;

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
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnDiffCallback implements ISvnDiffCallback {

    private ISvnDiffGenerator generator;
    private OutputStream outputStream;
    private long revision2;
    private long revision1;

    public SvnDiffCallback(ISvnDiffGenerator generator, long rev1, long rev2, OutputStream outputStream) {
        this.generator = generator;
        this.outputStream = outputStream;
        this.revision1 = rev1;
        this.revision2 = rev2;
    }

    public void fileOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
    }

    public void fileChanged(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        if (leftFile != null) {
            displayContentChanged(path, leftFile, rightFile, rev1, rev2, mimeType1, mimeType2, propChanges, originalProperties, OperationKind.Modified, null);
        }
        if (propChanges != null && !propChanges.isEmpty()) {
            propertiesChanged(path, revision1, revision2, false, propChanges, originalProperties);
        }
    }

    public void fileAdded(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2, File copyFromPath, long copyFromRevision, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        generator.setForceEmpty(true);
        if (rightFile != null && copyFromPath != null) {
            displayContentChanged(path, leftFile, rightFile, rev1, rev2, mimeType1, mimeType2, propChanges, originalProperties, OperationKind.Copied, copyFromPath);
        } else if (rightFile != null) {
            displayContentChanged(path, leftFile, rightFile, rev1, rev2, mimeType1, mimeType2, propChanges, originalProperties, OperationKind.Added, null);
        }

        if (propChanges != null && !propChanges.isEmpty()) {
            //we do not rev1 and rev2 here because SVN doesn't
            propertiesChanged(path, revision1, revision2, false, propChanges, originalProperties);
        }
        generator.setForceEmpty(false);
    }

    public void fileDeleted(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, String mimeType1, String mimeType2, SVNProperties originalProperties) throws SVNException {
        displayContentChanged(path, leftFile, null, revision1, revision2, mimeType1, mimeType2, null, originalProperties, OperationKind.Deleted, null);
    }

    public void dirDeleted(SvnDiffCallbackResult result, File path) throws SVNException {
        generator.displayDeletedDirectory(getTarget(path), getRevisionString(revision1), getRevisionString(revision2), outputStream);
    }

    public void dirOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
    }

    public void dirAdded(SvnDiffCallbackResult result, File path, long revision, String copyFromPath, long copyFromRevision) throws SVNException {
        generator.displayAddedDirectory(getTarget(path), getRevisionString(revision1), getRevisionString(revision), outputStream);
    }

    public void dirPropsChanged(SvnDiffCallbackResult result, File path, boolean dirWasAdded, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        originalProperties = originalProperties == null ? new SVNProperties() : originalProperties;
        propChanges = propChanges == null ? new SVNProperties() : propChanges;
        SVNProperties regularDiff = getRegularProperties(propChanges);
        if (regularDiff == null || regularDiff.isEmpty()) {
            return;
        }
        generator.displayPropsChanged(getTarget(path), getRevisionString(revision1), getRevisionString(revision2), dirWasAdded, originalProperties, regularDiff, outputStream);
    }

    public void dirClosed(SvnDiffCallbackResult result, File path, boolean dirWasAdded) throws SVNException {
    }

    private String getRevisionString(long revision) {
        if (revision >= 0) {
            return "(revision " + revision + ")";
        }
        return "(working copy)";
    }

    private static SVNProperties getRegularProperties(SVNProperties propChanges) {
        if (propChanges == null) {
            return null;
        }
        final SVNProperties regularPropertiesChanges = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, regularPropertiesChanges, null, null);
        return regularPropertiesChanges;
    }

    public void propertiesChanged(File path, long revision1, long revision2, boolean dirWasAdded, SVNProperties diff, SVNProperties originalProperties) throws SVNException {
        originalProperties = originalProperties == null ? new SVNProperties() : originalProperties;
        diff = diff == null ? new SVNProperties() : diff;
        SVNProperties regularDiff = getRegularProperties(diff);

        if (regularDiff != null && !regularDiff.isEmpty()) {
            generator.displayPropsChanged(getTarget(path), getRevisionString(revision1), getRevisionString(revision2), dirWasAdded, originalProperties, regularDiff, outputStream);
        }
    }

    private void displayContentChanged(File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2, SVNProperties propChanges, SVNProperties originalProperties, OperationKind operation, File copyFromPath) throws SVNException {
        boolean resetEncoding = false;
        OutputStream result = outputStream;
        String encoding = defineEncoding(originalProperties, propChanges);
        if (encoding != null) {
            generator.setEncoding(encoding);
            resetEncoding = true;
        } else {
            String conversionEncoding = defineConversionEncoding(originalProperties, propChanges);
            if (conversionEncoding != null) {
                resetEncoding = adjustDiffGenerator("UTF-8");
                result = new SVNCharsetOutputStream(result, Charset.forName("UTF-8"), Charset.forName(conversionEncoding), CodingErrorAction.IGNORE, CodingErrorAction.IGNORE);
            }
        }
        try {
            generator.displayContentChanged(getTarget(path), leftFile, rightFile, getRevisionString(rev1), getRevisionString(rev2), mimeType1, mimeType2, operation, copyFromPath, result);
        } finally {
            if (resetEncoding) {
                generator.setEncoding(null);
                generator.setEOL(null);
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

    private String defineEncoding(SVNProperties properties, SVNProperties diff) {
        ISvnDiffGenerator defaultGenerator = generator;
        if (defaultGenerator.getEncoding() != null) {
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
        return null;
    }

    private String getCharsetByMimeType(SVNProperties properties, ISvnDiffGenerator generator) {
        if (properties == null) {
            return null;
        }
        String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);
        String charset = SVNPropertiesManager.determineEncodingByMimeType(mimeType);
        return getCharset(charset, generator, false);
    }

    private String getCharset(SVNProperties properties, ISvnDiffGenerator generator) {
        if (properties == null) {
            return null;
        }
        String charset = properties.getStringValue(SVNProperty.CHARSET);
        return getCharset(charset, generator, true);
    }

    private String getCharset(String charset, ISvnDiffGenerator generator, boolean allowNative) {
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

    private String defineConversionEncoding(SVNProperties properties, SVNProperties diff) {
        ISvnDiffGenerator defaultGenerator = generator;
        if (defaultGenerator.getEncoding() != null) {
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
        return null;
    }

    private boolean adjustDiffGenerator(String charset) {
        boolean encodingAdjusted = false;
        if (generator.getEncoding() == null) {
            generator.setEncoding(charset);
            encodingAdjusted = true;
        }
        if (generator.getEOL() == null) {
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

    private SvnTarget getTarget(File path) {
        return SvnTarget.fromFile(path);
    }

    public enum OperationKind {
        Unchanged, Added, Deleted, Copied, Moved, Modified;
    }
}
