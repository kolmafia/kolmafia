package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnMergeResult;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.OutputStream;
import java.util.*;

public class SvnNgRemoteMergeEditor implements ISVNEditor {

    private final ISvnDiffCallback2 processor;

    private long targetRevision;
    private long revision;

    private SvnDiffCallbackResult mergeResult;

    private DirectoryBaton currentDirectory;
    private FileBaton currentFile;

    private SVNRepository repository;
    private boolean textDeltas;

    private final Collection<File> tmpFiles;
    private File emptyFile;
    private boolean pureRemoteDiff;
    private File globalTmpDir;
    private SVNWCContext context;
    private File target;

    public SvnNgRemoteMergeEditor(File target, SVNWCContext context, SVNRepository repository, long revision, ISvnDiffCallback2 processor, boolean textDeltas) {
        this.target = target;
        this.context = context;
        this.repository = repository;
        this.revision = revision;
        this.processor = processor;
        this.textDeltas = textDeltas;
        this.mergeResult = new SvnDiffCallbackResult();
        this.tmpFiles = new HashSet<File>();
        this.pureRemoteDiff = target == null;
    }

    public void targetRevision(long revision) throws SVNException {
        this.targetRevision = revision;
    }

    public void openRoot(long revision) throws SVNException {
        DirectoryBaton db = new DirectoryBaton("", null, false, revision);
        db.leftSource = new SvnDiffSource(this.revision);
        db.rightSource = new SvnDiffSource(this.targetRevision);

        mergeResult.reset();
        processor.dirOpened(mergeResult, SVNFileUtil.createFilePath(""), db.leftSource, db.rightSource, null, null);
        db.skip = mergeResult.skip;
        db.skipChildren = mergeResult.skipChildren;
        db.pdb = mergeResult.newBaton;

        this.currentDirectory = db;
    }

    private void diffDeletedFile(String path, DirectoryBaton db) throws SVNException {
        FileBaton fb = new FileBaton(path, db, false);
        boolean skip = false;
        SvnDiffSource leftSource = new SvnDiffSource(this.revision);

        checkCancelled();

        mergeResult.reset();
        processor.fileOpened(mergeResult, SVNFileUtil.createFilePath(path), leftSource, null, null, false, db.pdb);
        skip = mergeResult.skip;

        checkCancelled();

        if (skip) {
            return;
        }
        getFileFromRepository(fb, !this.textDeltas);

        mergeResult.reset();
        processor.fileDeleted(mergeResult, SVNFileUtil.createFilePath(fb.path), leftSource, fb.pathStartRevision, fb.pristineProps);
    }

    private void diffDeletedDirectory(String path, DirectoryBaton parentBaton) throws SVNException {
        SvnDiffSource leftSource = new SvnDiffSource(this.revision);
        DirectoryBaton db = new DirectoryBaton(path, parentBaton, false, -1);
        assert SVNRevision.isValidRevisionNumber(this.revision);

        checkCancelled();

        mergeResult.reset();
        processor.dirOpened(mergeResult, SVNFileUtil.createFilePath(path), leftSource, null, null, parentBaton.pdb);
        boolean skip = mergeResult.skip;
        boolean skipChildren = mergeResult.skipChildren;
        db.pdb = mergeResult.newBaton;
        SVNProperties leftProps = null;
        List<SVNDirEntry> dirEntries = null;
        if (!skip || !skipChildren) {
            dirEntries = skipChildren ? null : new ArrayList<SVNDirEntry>();
            leftProps = skip ? null : new SVNProperties();
            repository.getDir(path, this.revision, leftProps, SVNDirEntry.DIRENT_KIND, dirEntries);
        }
        if (!skipChildren) {
            for (SVNDirEntry dirEntry : dirEntries) {
                String name = dirEntry.getName();
                String childPath = SVNPathUtil.append(path, name);
                if (dirEntry.getKind() == SVNNodeKind.FILE) {
                    diffDeletedFile(childPath, db);
                } else if (dirEntry.getKind() == SVNNodeKind.DIR) {
                    diffDeletedDirectory(childPath, db);
                }
            }
        }
        if (!skip) {
            mergeResult.reset();
            processor.dirDeleted(mergeResult, SVNFileUtil.createFilePath(path), leftSource, leftProps, db.pdb);
        }
    }

    private void checkCancelled() throws SVNCancelException {
        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null) {
            eventHandler.checkCancelled();
        }
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        DirectoryBaton parentBaton = currentDirectory;
        if (parentBaton.skipChildren) {
            return;
        }
        SVNNodeKind kind = repository.checkPath(path, this.revision);
        if (kind == SVNNodeKind.FILE) {
            diffDeletedFile(path, parentBaton);
        } else if (kind == SVNNodeKind.DIR) {
            diffDeletedDirectory(path, parentBaton);
        }
    }

    public void absentDir(String path) throws SVNException {
        mergeResult.reset();
        processor.nodeAbsent(mergeResult, SVNFileUtil.createFilePath(path), currentDirectory.pdb);
    }

    public void absentFile(String path) throws SVNException {
        mergeResult.reset();
        processor.nodeAbsent(mergeResult, SVNFileUtil.createFilePath(path), currentDirectory.pdb);
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirectoryBaton pb = currentDirectory;
        DirectoryBaton db = new DirectoryBaton(path, pb, true, -1);
        currentDirectory = db;

        if (pb.skipChildren) {
            db.skip = true;
            db.skipChildren = true;
            return;
        }
        db.rightSource = new SvnDiffSource(this.targetRevision);
        mergeResult.reset();
        processor.dirOpened(mergeResult, SVNFileUtil.createFilePath(db.path), null, db.rightSource, null, pb.pdb);
        db.skip = mergeResult.skip;
        db.skipChildren = mergeResult.skipChildren;
        db.pdb = mergeResult.newBaton;
    }

    public void openDir(String path, long revision) throws SVNException {
        DirectoryBaton pb = currentDirectory;
        DirectoryBaton db = new DirectoryBaton(path, pb, false, revision);
        currentDirectory = db;

        if (pb.skipChildren) {
            db.skip = true;
            db.skipChildren = true;
            return;
        }
        db.leftSource = new SvnDiffSource(this.revision);
        db.rightSource = new SvnDiffSource(this.targetRevision);

        mergeResult.reset();
        processor.dirOpened(mergeResult, SVNFileUtil.createFilePath(path), db.leftSource, db.rightSource, null, pb != null ? pb.pdb : null);
        db.skip = mergeResult.skip;
        db.skipChildren = mergeResult.skipChildren;
        db.pdb = mergeResult.newBaton;
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        DirectoryBaton db = currentDirectory;
        if (db.skip) {
            return;
        }
        if (SVNProperty.isWorkingCopyProperty(name)) {
            return;
        } else if (SVNProperty.isRegularProperty(name)) {
            db.hasPropChange = true;
        }
        if (db.propChanges == null) {
            db.propChanges = new SVNProperties();
        }
        db.propChanges.put(name, value);
    }

    public void closeDir() throws SVNException {
        DirectoryBaton db = currentDirectory;
        boolean sendChanged = false;
        SVNProperties pristineProps;
        if ((db.hasPropChange || db.added) && !db.skip) {
            if (db.added) {
                pristineProps = new SVNProperties();
            } else {
                pristineProps = new SVNProperties();
                repository.getDir(db.path, db.baseRevision, pristineProps, 0, (Collection)null);
            }
            if (db.propChanges.size() > 0) {
                removeNonPropChanges(pristineProps, db.propChanges);
            }
            if (db.propChanges.size() > 0 || db.added) {
                SVNProperties rightProps = new SVNProperties(pristineProps);
                rightProps.putAll(db.propChanges);
                rightProps.removeNullValues();
                if (db.added) {
                    mergeResult.reset();
                    processor.dirAdded(mergeResult, SVNFileUtil.createFilePath(db.path), null, db.rightSource, null, rightProps, db.pdb);
                } else {
                    mergeResult.reset();
                    processor.dirChanged(mergeResult, SVNFileUtil.createFilePath(db.path), db.leftSource, db.rightSource, pristineProps, rightProps, db.propChanges, db.pdb);
                }
                sendChanged = true;
            }
        }
        if (!db.skip && !sendChanged) {
            mergeResult.reset();
            processor.dirClosed(mergeResult, SVNFileUtil.createFilePath(db.path), db.leftSource, db.rightSource, db.pdb);
        }
        currentDirectory = currentDirectory.parentBaton;
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirectoryBaton pb = currentDirectory;
        FileBaton fb = new FileBaton(path, pb, true);
        currentFile = fb;

        if (pb.skipChildren) {
            fb.skip = true;
            return;
        }
        fb.pristineProps = new SVNProperties();
        fb.rightSource = new SvnDiffSource(this.targetRevision);
        mergeResult.reset();
        processor.fileOpened(mergeResult, SVNFileUtil.createFilePath(path), null, fb.rightSource, null, false, pb.pdb);
        fb.skip = mergeResult.skip;
    }

    public void openFile(String path, long revision) throws SVNException {
        DirectoryBaton pb = currentDirectory;
        FileBaton fb = new FileBaton(path, pb, false);
        currentFile = fb;

        if (pb.skipChildren) {
            fb.skip = true;
            return;
        }
        fb.baseRevision = revision;
        fb.leftSource = new SvnDiffSource(this.revision);
        fb.rightSource = new SvnDiffSource(this.targetRevision);

        mergeResult.reset();
        processor.fileOpened(mergeResult, SVNFileUtil.createFilePath(path), fb.leftSource, fb.rightSource, null, false, pb.pdb);
        fb.skip = mergeResult.skip;
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        FileBaton fb = currentFile;
        if (fb.skip) {
            return;
        }
        if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            return;
        } else if (SVNProperty.isRegularProperty(propertyName)) {
            fb.hasPropChange = true;
        }

        if (fb.propChanges == null) {
            fb.propChanges = new SVNProperties();
        }
        fb.propChanges.put(propertyName, propertyValue);
    }

    public void closeFile(String path, String expectedChecksum) throws SVNException {
        FileBaton fb = currentFile;
        if (fb.skip) {
            return;
        }
        if (expectedChecksum != null && textDeltas) {
            if (fb.resultChecksum != null && !expectedChecksum.equals(fb.resultChecksum)) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", fb.path);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
        if (fb.added || fb.pathEndRevision != null || fb.hasPropChange) {
            if (!fb.added && fb.pristineProps == null) {
                getFileFromRepository(fb, true);
            }
            if (fb.pristineProps != null) {
                removeNonPropChanges(fb.pristineProps, fb.propChanges);
            }
            SVNProperties rightProps = fb.pristineProps == null ? new SVNProperties() : new SVNProperties(fb.pristineProps);
            rightProps.putAll(fb.propChanges);
            rightProps.removeNullValues();

            if (fb.added) {
                mergeResult.reset();
                processor.fileAdded(mergeResult, SVNFileUtil.createFilePath(fb.path), null, fb.rightSource, null, fb.pathEndRevision, null, rightProps);
            } else {
                mergeResult.reset();
                boolean contentChanged = fb.baseChecksum== null || !fb.baseChecksum.equals(expectedChecksum);
                File leftFile = fb.pathEndRevision != null && contentChanged ? fb.pathStartRevision : null;
                File pathEndRevision = contentChanged ? fb.pathEndRevision : null;
                processor.fileChanged(mergeResult, SVNFileUtil.createFilePath(fb.path), fb.leftSource, fb.rightSource, leftFile, pathEndRevision, fb.pristineProps, rightProps, fb.pathEndRevision != null, fb.propChanges);
            }
        }
        currentFile = null;
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        cleanup();
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        if (currentFile.skip) {
            return;
        }
        currentFile.deltaProcessor = new SVNDeltaProcessor();
        if (!currentFile.added) {
            getFileFromRepository(currentFile, false);
        } else {
            currentFile.pathStartRevision = getEmptyFile();
        }
        currentFile.pathEndRevision = createUniqueFile(SVNPathUtil.tail(path));
        tmpFiles.add(currentFile.pathEndRevision);
        currentFile.baseChecksum = baseChecksum;
        currentFile.deltaProcessor.applyTextDelta(currentFile.pathStartRevision, currentFile.pathEndRevision, true);
    }

    private File getEmptyFile() throws SVNException {
        if (emptyFile == null) {
            emptyFile = createUniqueFile("empty");
            tmpFiles.add(emptyFile);
        }
        return emptyFile;
    }

    private File createUniqueFile(String name) throws SVNException {
        File tmpDir = pureRemoteDiff ? getGlobalTmpDir() : context.getDb().getWCRootTempDir(target);
        return SVNFileUtil.createUniqueFile(tmpDir, name, ".tmp", false);
    }

    public File getGlobalTmpDir() throws SVNException {
        if (globalTmpDir == null) {
            globalTmpDir = SVNFileUtil.createTempDirectory("svndiff");
        }
        return globalTmpDir;
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        if (currentFile.deltaProcessor != null) {
            return currentFile.deltaProcessor.textDeltaChunk(diffWindow);
        }
        return SVNFileUtil.DUMMY_OUT;
    }

    public void textDeltaEnd(String path) throws SVNException {
        if (currentFile.deltaProcessor != null) {
            String checksum = currentFile.deltaProcessor.textDeltaEnd();
            currentFile.resultChecksum = checksum;
        }
    }

    private void getFileFromRepository(FileBaton fileBaton, boolean propsOnly) throws SVNException {
        if (!propsOnly) {
            fileBaton.pathStartRevision = createUniqueFile(SVNPathUtil.tail(fileBaton.path));

            OutputStream outputStream = SVNFileUtil.openFileForWriting(fileBaton.pathStartRevision);
            try {
                fileBaton.pristineProps = new SVNProperties();
                repository.getFile(fileBaton.path, fileBaton.baseRevision, fileBaton.pristineProps, outputStream);
            } finally {
                SVNFileUtil.closeFile(outputStream);
            }
        } else {
            fileBaton.pristineProps = new SVNProperties();
            repository.getFile(fileBaton.path, fileBaton.baseRevision, fileBaton.pristineProps, null);
        }
    }

    private void removeNonPropChanges(SVNProperties pristineProps, SVNProperties propChanges) {
        for (Iterator<String> iterator = propChanges.nameSet().iterator(); iterator.hasNext(); ) {
            final String propName = iterator.next();
            SVNPropertyValue propertyValue = propChanges.getSVNPropertyValue(propName);
            if (propertyValue != null) {
                SVNPropertyValue oldValue = pristineProps.getSVNPropertyValue(propName);
                if (oldValue != null && oldValue.equals(propertyValue)) {
                    iterator.remove();
                }
            }
        }
    }

    public void cleanup() {
        for (File tmpFile : tmpFiles) {
            try {
                SVNFileUtil.deleteFile(tmpFile);
            } catch (SVNException ignore) {
            }
        }
        if (globalTmpDir != null) {
            SVNFileUtil.deleteAll(globalTmpDir, true);
        }
    }

    private class DirectoryBaton {
        private String path;
        private boolean added;
        private boolean treeConflicted;
        private boolean skip;
        private boolean skipChildren;
        private DirectoryBaton parentBaton;
        private SVNProperties propChanges;
        private boolean hasPropChange;

        private SvnDiffSource leftSource;
        private SvnDiffSource rightSource;

        private long baseRevision;
        private Object pdb;

        private DirectoryBaton(String path, DirectoryBaton parentBaton, boolean added, long baseRevision) {
            this.path = path;
            this.parentBaton = parentBaton;
            this.added = added;
            this.baseRevision = baseRevision;
            this.skip = false;
            this.skipChildren = false;
            this.propChanges = new SVNProperties();
            this.baseRevision = revision;
        }
    }

    private class FileBaton {
        private String path;
        private DirectoryBaton parentBaton;
        private boolean added;
        private boolean treeConflicted;
        private boolean skip;
        private File pathStartRevision;
        private File pathEndRevision;
        private SVNProperties pristineProps;
        private long baseRevision;
        private SvnDiffSource leftSource;
        private SvnDiffSource rightSource;
        public boolean hasPropChange;
        public SVNProperties propChanges;
        public String baseChecksum;
        public String resultChecksum;
        public SVNDeltaProcessor deltaProcessor;

        private FileBaton(String path, DirectoryBaton parentBaton, boolean added) {
            this.path = path;
            this.parentBaton = parentBaton;
            this.added = added;
            this.baseRevision = revision;
        }
    }
}
