package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumOutputStream;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.OutputStream;
import java.util.*;

public class SvnNgRemoteDiffEditor2 implements ISVNEditor {

    private long revision;
    private long targetRevision;
    private SVNRepository repository;
    private ISvnDiffCallback2 callback;

    private SvnDiffCallbackResult result;
    private SVNDeltaProcessor deltaProcessor;
    private boolean textDeltas;

    private DirBaton dirBaton;
    private FileBaton fileBaton;

    private Set<File> tempFiles;
    private File emptyFile;

    public SvnNgRemoteDiffEditor2(long revision, boolean textDeltas, SVNRepository repository, ISvnDiffCallback2 callback) {
        this.revision = revision;
        this.repository = repository;
        this.callback = callback;
        this.result = new SvnDiffCallbackResult();
        this.textDeltas = textDeltas;
        this.deltaProcessor = new SVNDeltaProcessor();
        this.tempFiles = new HashSet<File>();
    }

    public void targetRevision(long revision) throws SVNException {
        targetRevision = revision;
    }

    public void openRoot(long revision) throws SVNException {
        dirBaton = new DirBaton("", null, false, revision);
        dirBaton.leftSource = new SvnDiffSource(this.revision);
        dirBaton.rightSource = new SvnDiffSource(this.targetRevision);

        result.reset();
        callback.dirOpened(result, SVNFileUtil.createFilePath(""), dirBaton.leftSource, dirBaton.rightSource, null, null);
        dirBaton.skip = result.skip;
        dirBaton.skipChildren = result.skipChildren;
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        try {
            DirBaton pb = dirBaton;
            if (pb.skipChildren) {
                return;
            }
            SVNNodeKind kind = repository.checkPath(path, this.revision);
            if (kind == SVNNodeKind.FILE) {
                diffDeletedFile(path);
            } else if (kind == SVNNodeKind.DIR) {
                diffDeletedDirectory(path);
            }
        } finally {
            cleanupTempFiles();
        }
    }

    public void absentDir(String path) throws SVNException {
        result.reset();
        callback.nodeAbsent(result, SVNFileUtil.createFilePath(path), null);
        dirBaton.skip = result.skip;
        dirBaton.skipChildren = result.skipChildren;
    }

    public void absentFile(String path) throws SVNException {
        result.reset();
        callback.nodeAbsent(result, SVNFileUtil.createFilePath(path), null);
        dirBaton.skip = result.skip;
        dirBaton.skipChildren = result.skipChildren;
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton pb = dirBaton;
        dirBaton = new DirBaton(path, pb, true, SVNRepository.INVALID_REVISION);

        if (pb.skipChildren) {
            dirBaton.skip = true;
            dirBaton.skipChildren = true;
            return;
        }

        dirBaton.rightSource = new SvnDiffSource(this.targetRevision);
        result.reset();
        callback.dirOpened(result, SVNFileUtil.createFilePath(dirBaton.path), null, dirBaton.rightSource, null, null);
        dirBaton.skip = result.skip;
        dirBaton.skipChildren = result.skipChildren;
    }

    public void openDir(String path, long revision) throws SVNException {
        DirBaton pb = dirBaton;
        dirBaton = new DirBaton(path, pb, false, revision);

        if (pb.skipChildren) {
            dirBaton.skip = true;
            dirBaton.skipChildren = true;
            return;
        }

        dirBaton.leftSource = new SvnDiffSource(this.revision);
        dirBaton.rightSource = new SvnDiffSource(this.targetRevision);

        result.reset();
        callback.dirOpened(result, SVNFileUtil.createFilePath(dirBaton.path), dirBaton.leftSource, dirBaton.rightSource, null, null);
        dirBaton.skip = result.skip;
        dirBaton.skipChildren = result.skipChildren;
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (dirBaton.skip) {
            return;
        }
        if (SVNProperty.isWorkingCopyProperty(name)) {
            return;
        } else if (SVNProperty.isRegularProperty(name)) {
            dirBaton.hasPropChange = true;
        }

        dirBaton.propChanges.put(name, value);
    }

    public void closeDir() throws SVNException {
        try {
        boolean sendChanged = false;
        SVNProperties pristineProps = new SVNProperties();

        if ((dirBaton.hasPropChange || dirBaton.added) && !dirBaton.skip) {
            if (dirBaton.added) {
            } else {
                repository.getDir(dirBaton.path, dirBaton.baseRevision, pristineProps, (ISVNDirEntryHandler) null);
            }

            if (dirBaton.propChanges.size() > 0) {
                dirBaton.propChanges = removeNonPropChanges(pristineProps, dirBaton.propChanges);
            }

            if (dirBaton.propChanges.size() > 0 || dirBaton.added) {
                SVNProperties rightProps = new SVNProperties(pristineProps);
                rightProps.putAll(dirBaton.propChanges);
                rightProps.removeNullValues();

                if (dirBaton.added) {
                    result.reset();
                    callback.dirAdded(result, SVNFileUtil.createFilePath(dirBaton.path), null, dirBaton.rightSource, null, rightProps, null);
                    dirBaton.skip = result.skip;
                    dirBaton.skipChildren = result.skipChildren;
                } else {
                    result.reset();
                    callback.dirChanged(result, SVNFileUtil.createFilePath(dirBaton.path), dirBaton.leftSource, dirBaton.rightSource, pristineProps, rightProps, dirBaton.propChanges, null);
                    dirBaton.skip = result.skip;
                    dirBaton.skipChildren = result.skipChildren;
                }

                sendChanged = true;
            }
        }
        if (!dirBaton.skip && !sendChanged) {
            result.reset();
            callback.dirClosed(result, SVNFileUtil.createFilePath(dirBaton.path), dirBaton.leftSource, dirBaton.rightSource, null);
            dirBaton.skip = result.skip;
            dirBaton.skipChildren = result.skipChildren;
        }
        } finally {
            dirBaton = dirBaton.parentBaton;
        }
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton pb = dirBaton;

        FileBaton fb = fileBaton = new FileBaton(path, pb, true);
        if (pb.skipChildren) {
            fb.skip = true;
            return;
        }
        fb.pristineProps = new SVNProperties();
        fb.rightSource = new SvnDiffSource(this.targetRevision);
        result.reset();
        callback.fileOpened(result, SVNFileUtil.createFilePath(fb.path), null, fb.rightSource, null, false, null);
        fb.skip = result.skip;
    }

    public void openFile(String path, long revision) throws SVNException {
        DirBaton pb = dirBaton;

        FileBaton fb = fileBaton = new FileBaton(path, pb, false);
        if (pb.skipChildren) {
            fb.skip = true;
            return;
        }
        fb.baseRevision = revision;
        fb.leftSource = new SvnDiffSource(this.revision);
        fb.rightSource = new SvnDiffSource(this.targetRevision);

        result.reset();
        callback.fileOpened(result, SVNFileUtil.createFilePath(fb.path), fb.leftSource, fb.rightSource, null, false, null);
        fb.skip = result.skip;
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        FileBaton fb = fileBaton;

        if (fb.skip) {
            return;
        }

        if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            return;
        } else if (SVNProperty.isRegularProperty(propertyName)) {
            fb.hasPropChanges = true;
        }

        fb.propChanges.put(propertyName, propertyValue);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        try {
            FileBaton fb = fileBaton;
            if (fb.skip) {
                return;
            }

            if (textChecksum != null && this.textDeltas) {
                if (fb.resultMd5Checksum != null && !textChecksum.equals(fb.resultMd5Checksum)) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", fb.path);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }

            if (fb.added || (fb.pathEndRevision != null || !this.textDeltas) || fb.hasPropChanges) {
                SVNProperties rightProps;

                if (!fb.added && fb.pristineProps == null) {
                    getFileFromRa(fb, true);
                }

                String oldChecksum = fb.pristineProps.getStringValue(SVNProperty.CHECKSUM);

                if (fb.pristineProps != null) {
                    fb.propChanges = removeNonPropChanges(fb.pristineProps, fb.propChanges);
                }

                rightProps = new SVNProperties(fb.pristineProps);
                rightProps.putAll(fb.propChanges);
                rightProps.removeNullValues();

                if (fb.added) {
                    result.reset();
                    callback.fileAdded(result, SVNFileUtil.createFilePath(fb.path), null, fb.rightSource, null, fb.pathEndRevision, null, rightProps);
                } else {
                    result.reset();
                    boolean fileModified = fb.pathEndRevision != null;
                    if (textChecksum != null && oldChecksum != null) {
                        //SVNKit is different from SVN: it always sends applyTextDelta, but Subversion --- only for changed files
                        fileModified = !textChecksum.equals(oldChecksum);
                    }
                    if (fileModified && !textDeltas) {
                        fb.pathStartRevision = getEmptyFile();
                        fb.pathEndRevision = getEmptyFile();
                    }
                    callback.fileChanged(result, SVNFileUtil.createFilePath(fb.path), fb.leftSource, fb.rightSource, fb.pathEndRevision != null ? fb.pathStartRevision : null, fb.pathEndRevision, fb.pristineProps, rightProps, fileModified, fb.propChanges);
                }
            }
        } finally {
            cleanupTempFiles();
        }
    }

    private File getEmptyFile() throws SVNException {
        if (this.emptyFile == null) {
            this.emptyFile = SVNFileUtil.createTempFile("", "");
        }
        return this.emptyFile;
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        FileBaton fb = fileBaton;

        if (fb.skip) {
            return;
        }

        if (!this.textDeltas) {
            fb.pathStartRevision = null;
            fb.pathEndRevision = null;
            return;
        }

        if (!fb.added) {
            getFileFromRa(fb, false);
        } else {
            fb.pathStartRevision = null;
        }

        if (baseChecksum != null && !fb.startMd5Checksum.equals(baseChecksum)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Base checksum mismatch for ''{0}''", fb.path);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (fb.pathEndRevision == null) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            fb.pathEndRevision = SVNFileUtil.createUniqueFile(tmpDir, "svn", "tmp", true);
            tempFiles.add(fb.pathEndRevision);
        }

        if (fb.pathStartRevision == null) {
            deltaProcessor.applyTextDelta(SVNFileUtil.DUMMY_IN, fb.pathEndRevision, true);
        } else {
            deltaProcessor.applyTextDelta(fb.pathStartRevision, fb.pathEndRevision, true);
        }
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        if (!this.textDeltas) {
            return null;
        }
        return deltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        if (!this.textDeltas) {
            return;
        }
        FileBaton fb = fileBaton;
        fb.resultMd5Checksum = deltaProcessor.textDeltaEnd();
    }

    public void cleanup() {
        cleanupTempFiles();

        if (emptyFile != null) {
            try {
                SVNFileUtil.deleteFile(emptyFile);
            } catch (SVNException e) {
                //ignore
            }
            emptyFile = null;
        }
    }

    public void cleanupTempFiles() {
        for (File tempFile : tempFiles) {
            try {
                SVNFileUtil.deleteFile(tempFile);
            } catch (SVNException e) {
                //ignore
            }
        }
        tempFiles.clear();
    }

    private void diffDeletedFile(String path) throws SVNException {
        FileBaton fb = new FileBaton(path, dirBaton, false);
        boolean skip = false;
        SvnDiffSource leftSource = new SvnDiffSource(this.revision);

        result.reset();
        callback.fileOpened(result, SVNFileUtil.createFilePath(path), leftSource, null, null, false, null);
        skip = result.skip;

        if (skip) {
            return;
        }

        getFileFromRa(fb, !textDeltas);

        result.reset();
        callback.fileDeleted(result, SVNFileUtil.createFilePath(fb.path), leftSource, fb.pathStartRevision, fb.pristineProps);
    }

    private void diffDeletedDirectory(String path) throws SVNException {
        boolean skip = false;
        boolean skipChildren = false;

        SvnDiffSource leftSource = new SvnDiffSource(this.revision);
        DirBaton pb = dirBaton;
        DirBaton db = new DirBaton(path, pb, false, SVNRepository.INVALID_REVISION);

        assert SVNRevision.isValidRevisionNumber(this.revision);

        result.reset();
        callback.dirOpened(result, SVNFileUtil.createFilePath(path), leftSource, null, null, null);
        skip = result.skip;
        skipChildren = result.skipChildren;

        SVNProperties leftProps = new SVNProperties();
        List<SVNDirEntry> dirEntries = new ArrayList<SVNDirEntry>();
        if (!skip || !skipChildren) {
            repository.getDir(path, this.revision, skip ? null : leftProps, skipChildren ? null : dirEntries);
        }
        if (!skipChildren) {
            for (SVNDirEntry dirEntry : dirEntries) {
                String name = dirEntry.getName();
                String childPath = SVNPathUtil.append(path, name);

                if (dirEntry.getKind() == SVNNodeKind.FILE) {
                    diffDeletedFile(childPath);
                } else if (dirEntry.getKind() == SVNNodeKind.DIR) {
                    diffDeletedDirectory(childPath);
                }
            }
        }

        if (!skip) {
            result.reset();
            callback.dirDeleted(result, SVNFileUtil.createFilePath(path), leftSource, leftProps, null);
        }
    }

    private SVNProperties removeNonPropChanges(SVNProperties pristineProps, SVNProperties changes) {
        SVNProperties newChanges = new SVNProperties();

        for (Iterator<Map.Entry<String, SVNPropertyValue>> iterator = changes.asMap().entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<String, SVNPropertyValue> entry = iterator.next();
            String name = entry.getKey();
            SVNPropertyValue value = entry.getValue();

            boolean remove = false;
            if (value != null) {
                SVNPropertyValue oldValue = pristineProps.getSVNPropertyValue(name);

                if (oldValue != null && oldValue.equals(value)) {
                    remove = true;
                }
            }
            if (!remove) {
                newChanges.put(name, value);
            }
        }
        return newChanges;
    }

    private void getFileFromRa(FileBaton fb, boolean propsOnly) throws SVNException {
        if (fb.pristineProps == null) {
            fb.pristineProps = new SVNProperties();
        }
        if (!propsOnly) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            fb.pathStartRevision = SVNFileUtil.createUniqueFile(tmpDir, "svn", "tmp", true);
            tempFiles.add(fb.pathStartRevision);
            OutputStream outputStream = null;
            SVNChecksumOutputStream checksumOutputStream = null;
            try {
                outputStream = SVNFileUtil.openFileForWriting(fb.pathStartRevision);
                checksumOutputStream = new SVNChecksumOutputStream(outputStream, SVNChecksumOutputStream.MD5_ALGORITHM, false);

                repository.getFile(fb.path, fb.baseRevision, fb.pristineProps, checksumOutputStream);
                fb.startMd5Checksum = checksumOutputStream.getDigest();
            } finally {
                SVNFileUtil.closeFile(checksumOutputStream);
                SVNFileUtil.closeFile(outputStream);//close original output stream because checksumOutputStream won't close it
            }
        } else {
            repository.getFile(fb.path, fb.baseRevision, fb.pristineProps, null);
        }
    }

    private static class DirBaton {
        private boolean added;
        private boolean treeConflicted;
        private boolean skip;
        private boolean skipChildren;
        private String path;
        private DirBaton parentBaton;
        private long baseRevision;
        private SvnDiffSource leftSource;
        private SvnDiffSource rightSource;
        public boolean hasPropChange;
        public SVNProperties propChanges;

        public DirBaton(String path, DirBaton parentBaton, boolean added, long baseRevision) {
            this.path = path;
            this.parentBaton = parentBaton;
            this.added = added;
            this.baseRevision = baseRevision;
            this.propChanges = new SVNProperties();
        }
    }

    private class FileBaton {
        private boolean added;
        private boolean treeConflicted;
        private boolean skip;
        private String path;
        private File pathStartRevision;
        private SVNProperties pristineProps;
        private long baseRevision;
        private File pathEndRevision;
        private String startMd5Checksum;
        private String resultMd5Checksum;
        private SVNProperties propChanges;
        private boolean hasPropChanges;
        private SvnDiffSource leftSource;
        private SvnDiffSource rightSource;
        private DirBaton parentBaton;

        public FileBaton(String path, DirBaton parentBaton, boolean added) {
            this.path = path;
            this.parentBaton = parentBaton;
            this.added = added;
            this.propChanges = new SVNProperties();
            this.baseRevision = SvnNgRemoteDiffEditor2.this.revision;
        }
    }
}
