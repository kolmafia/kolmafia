package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumInputStream;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SVNEditorProxy implements ISVNEditor {

    private final ISVNEditor2 editor2;
    private final ISVNEditorProxyCallbacks proxyCallbacks;
    private DirectoryBaton currentDirectoryBaton;
    private FileBaton currentFileBaton;

    private InputStream source;
    private OutputStream target;
    private SVNDeltaProcessor svnDeltaProcessor;
    private boolean closed;

    private List<String> pathOrder;
    private Map<String, ChangeNode> changes;
    private SVNURL repositoryRoot;
    private String baseRelPath;
    private File tempDirectory;

    public SVNEditorProxy(ISVNEditor2 editor2, ISVNEditorProxyCallbacks proxyCallbacks) {
        this.editor2 = editor2;
        this.proxyCallbacks = proxyCallbacks;
        this.svnDeltaProcessor = new SVNDeltaProcessor();
        this.tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        this.changes = new HashMap<String, ChangeNode>();
        this.pathOrder = new ArrayList<String>();
    }

    public void setRepositoryRoot(SVNURL repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public void setBaseRelPath(String baseRelPath) {
        this.baseRelPath = baseRelPath;
    }

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public void targetRevision(long revision) throws SVNException {
        proxyCallbacks.getExtraCallbacks().targetRevision(revision);
    }

    public void openRoot(long revision) throws SVNException {
        final DirectoryBaton directoryBaton = new DirectoryBaton();
        directoryBaton.path = "";

        proxyCallbacks.getExtraCallbacks().startEdit(revision);

        directoryBaton.parent = currentDirectoryBaton;
        currentDirectoryBaton = directoryBaton;
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        String relPath = mapToReposRelPath(path);
        ChangeNode change = locateChange(relPath);

        long baseRevision;
        if (SVNRevision.isValidRevisionNumber(revision)) {
            baseRevision = revision;
        } else {
            baseRevision = currentDirectoryBaton.baseRevision;
        }

        assert change.action == RestructureAction.RESTRUCTURE_NONE;
        change.action = RestructureAction.RESTRUCTURE_DELETE;

        assert !SVNRevision.isValidRevisionNumber(change.deleting) || change.deleting == baseRevision;

        change.deleting = baseRevision;
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        final DirectoryBaton directoryBaton = new DirectoryBaton();

        final String relPath = mapToReposRelPath(path);
        ChangeNode change = locateChange(relPath);
        change.action = RestructureAction.RESTRUCTURE_ADD;
        change.kind = SVNNodeKind.DIR;

        directoryBaton.path = relPath;
        directoryBaton.baseRevision = currentDirectoryBaton.baseRevision;

        if (copyFromPath == null) {
            if (currentDirectoryBaton.copyFromRelPath != null) {
                String name = SVNPathUtil.tail(relPath);
                directoryBaton.copyFromRelPath = SVNPathUtil.append(currentDirectoryBaton.copyFromRelPath, name);
                directoryBaton.copyFromRevision = currentDirectoryBaton.copyFromRevision;
            }
        } else {
            change.copyFromPath = mapToReposRelPath(copyFromPath);
            change.copyFromRevision = copyFromRevision;
            directoryBaton.copyFromRelPath = change.copyFromPath;
            directoryBaton.copyFromRevision = change.copyFromRevision;
        }
        directoryBaton.parent = currentDirectoryBaton;
        currentDirectoryBaton = directoryBaton;
    }

    public void openDir(String path, long revision) throws SVNException {
        final DirectoryBaton directoryBaton = new DirectoryBaton();

        String relPath = mapToReposRelPath(path);
        directoryBaton.path = relPath;
        directoryBaton.baseRevision = revision;

        if (currentDirectoryBaton.copyFromRelPath != null) {
            String name = SVNPathUtil.tail(relPath);
            directoryBaton.copyFromRelPath = SVNPathUtil.append(currentDirectoryBaton.copyFromRelPath, name);
            directoryBaton.copyFromRevision = currentDirectoryBaton.copyFromRevision;
        }
        directoryBaton.parent = currentDirectoryBaton;
        currentDirectoryBaton = directoryBaton;
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        applyPropEdit(currentDirectoryBaton.path, SVNNodeKind.DIR, currentDirectoryBaton.baseRevision, name, value);
    }

    public void closeDir() throws SVNException {
        currentDirectoryBaton = currentDirectoryBaton.parent;
    }

    public void absentDir(String path) throws SVNException {
        final String relPath = mapToReposRelPath(path);
        ChangeNode change = locateChange(relPath);

        change.action = RestructureAction.RESTRUCTURE_ADD_ABSENT;
        change.kind = SVNNodeKind.DIR;
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        currentFileBaton = new FileBaton();

        final String relPath = mapToReposRelPath(path);
        ChangeNode change = locateChange(relPath);

        change.action = RestructureAction.RESTRUCTURE_ADD;
        change.kind = SVNNodeKind.FILE;

        currentFileBaton.path = relPath;
        currentFileBaton.baseRevision = currentDirectoryBaton.baseRevision;

        if (copyFromPath == null) {
            currentFileBaton.deltaBase = null;
        } else {
            change.copyFromPath = mapToReposRelPath(copyFromPath);
            change.copyFromRevision = copyFromRevision;

            currentFileBaton.deltaBase = proxyCallbacks.fetchBase(copyFromPath, copyFromRevision);
        }
    }

    public void openFile(String path, long revision) throws SVNException {
        currentFileBaton = new FileBaton();

        final String relPath = mapToReposRelPath(path);

        currentFileBaton.path = relPath;
        currentFileBaton.baseRevision = revision;

        if (currentDirectoryBaton.copyFromRelPath != null) {
            String name = SVNPathUtil.tail(relPath);
            String copyFromRelPath = SVNPathUtil.append(currentDirectoryBaton.copyFromRelPath, name);

            currentFileBaton.deltaBase = proxyCallbacks.fetchBase(copyFromRelPath, currentDirectoryBaton.copyFromRevision);
        } else {
            currentFileBaton.deltaBase = proxyCallbacks.fetchBase(relPath, revision);
        }
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {

        final ChangeNode change = locateChange(currentFileBaton.path);
        assert !change.contentsChanged;
        assert change.contentsAbsPath == null;
        assert !SVNRevision.isValidRevisionNumber(change.changing) || change.changing == currentFileBaton.baseRevision;
        change.changing = currentFileBaton.baseRevision;

        if (currentFileBaton.deltaBase == null) {
            source = SVNFileUtil.DUMMY_IN;
        } else {
            source = openDeltaBase(currentFileBaton);
        }
        change.contentsChanged = true;

        target = openDeltaTarget(currentFileBaton);
        change.contentsAbsPath = currentFileBaton.deltaTarget;

        svnDeltaProcessor.applyTextDelta(source, target, true);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return svnDeltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        svnDeltaProcessor.textDeltaEnd();
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        if (SVNProperty.LOCK_TOKEN.equals(propertyName) && propertyValue == null) {
            String relPath = mapToReposRelPath(currentFileBaton.path);
            ChangeNode change = locateChange(relPath);
            change.unlock = true;
        }
        applyPropEdit(currentFileBaton.path, SVNNodeKind.FILE, currentFileBaton.baseRevision, propertyName, propertyValue);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        SVNFileUtil.closeFile(source);
        SVNFileUtil.closeFile(target);
        try {
            SVNFileUtil.deleteFile(currentFileBaton.deltaTarget);
        } catch (SVNException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.CLIENT, e, Level.ALL);
        }
    }

    public void absentFile(String path) throws SVNException {
        String relPath = mapToReposRelPath(path);
        ChangeNode change = locateChange(relPath);

        change.action = RestructureAction.RESTRUCTURE_ADD_ABSENT;
        change.kind = SVNNodeKind.FILE;
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        runEv2Actions();
        closed = true;
        editor2.complete();
        return null;
    }

    public void abortEdit() throws SVNException {
        runEv2Actions();
        if (!closed) {
            editor2.abort();
        }
    }

    private void applyPropEdit(String relPath, SVNNodeKind kind, long baseRevision, String name, SVNPropertyValue value) {
        ChangeNode change = locateChange(relPath);

        assert change.kind == SVNNodeKind.UNKNOWN || change.kind == kind;
        change.kind = kind;

        assert !SVNRevision.isValidRevisionNumber(change.changing) || (change.changing == baseRevision);
        change.changing = baseRevision;

        if (change.properties == null) {
            if (change.copyFromPath != null) {
                change.properties = proxyCallbacks.fetchProperties(change.copyFromPath, change.copyFromRevision);
            } else if (change.action == RestructureAction.RESTRUCTURE_ADD) {
                change.properties = new SVNProperties();
            } else {
                change.properties = proxyCallbacks.fetchProperties(relPath, baseRevision);
            }
        }
        if (change.properties == null) {
            change.properties = new SVNProperties();
        }
        if (value == null) {
            change.properties.put(name, (SVNPropertyValue)null);
        } else {
            change.properties.put(name, value);
        }
    }

    private void runEv2Actions() throws SVNException {
        for (String reposRelPath : pathOrder) {
            final ChangeNode change = changes.get(reposRelPath);
            processAction(reposRelPath, change);
        }
    }

    private void processAction(String reposRelPath, ChangeNode change) throws SVNException {
        SVNProperties props = null;
        InputStream contents = null;
        SVNNodeKind kind = SVNNodeKind.UNKNOWN;
        SvnChecksum checksum = null;

        if (change.unlock) {
            proxyCallbacks.unlock(reposRelPath);
        }

        if (change.action == RestructureAction.RESTRUCTURE_DELETE) {
            editor2.delete(reposRelPath, change.deleting);
            return;
        }
        if (change.action == RestructureAction.RESTRUCTURE_ADD_ABSENT) {
            editor2.addAbsent(reposRelPath, change.kind, change.deleting);
        }
        if (change.contentsChanged) {
            kind = SVNNodeKind.FILE;

            if (change.contentsAbsPath != null) {
                checksum = fileChecksum(change.contentsAbsPath, SvnChecksum.Kind.sha1);
                contents = SVNFileUtil.openFileForReading(change.contentsAbsPath);
            } else {
                contents = SVNFileUtil.DUMMY_IN;
                checksum = new SvnChecksum(SvnChecksum.Kind.sha1, SvnChecksum.SHA1_EMPTY);
            }
        }
        if (change.properties != null) {
            kind = change.kind;
            props = change.properties;
        }
        if (change.action == RestructureAction.RESTRUCTURE_ADD) {
            long replacesRevision = change.deleting;
            kind = change.kind;

            if (change.copyFromPath != null) {
                editor2.copy(change.copyFromPath, change.copyFromRevision, reposRelPath, replacesRevision);
            } else {
                if (props == null) {
                    props = new SVNProperties();
                }
                if (kind == SVNNodeKind.DIR) {
                    List<String> children = getChildren(reposRelPath);
                    editor2.addDir(reposRelPath, children, props, replacesRevision);
                } else {
                    if (change.contentsAbsPath == null) {
                        contents = SVNFileUtil.DUMMY_IN;
                        checksum = new SvnChecksum(SvnChecksum.Kind.sha1, SvnChecksum.SHA1_EMPTY);
                    }
                    editor2.addFile(reposRelPath, checksum, contents, props, replacesRevision);
                }
            }
        }
        if (props != null || contents != null) {
            if (kind == SVNNodeKind.DIR) {
                editor2.alterDir(reposRelPath, change.changing, null, props);
            } else {
                editor2.alterFile(reposRelPath, change.changing, props, checksum, contents);
            }
        }
    }

    private SvnChecksum fileChecksum(File contentsAbsPath, SvnChecksum.Kind kind) throws SVNException {
        final SVNChecksumInputStream checksumInputStream = new SVNChecksumInputStream(SVNFileUtil.openFileForReading(contentsAbsPath), kind.name());
        try {
            final byte[] buffer = new byte[2048];

            while (true) {
                final int bytesRead;
                bytesRead = checksumInputStream.read(buffer);
                if (bytesRead < 0) {
                    return new SvnChecksum(kind, checksumInputStream.getDigest());
                }
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
        } finally {
            SVNFileUtil.closeFile(checksumInputStream);
        }
        return null;
    }

    private List<String> getChildren(String path) {
        final List<String> children = new ArrayList<String>();

        for (Map.Entry<String, ChangeNode> entry : changes.entrySet()) {
            final String reposRelPath = entry.getKey();
            String child = SVNPathUtil.getPathAsChild(path, reposRelPath);

            if (child == null || child.length() == 0) {
                continue;
            }
            if (child.indexOf('/') >= 0) {
                continue;
            }
            children.add(child);
        }
        return children;
    }

    private ChangeNode locateChange(String relPath) {
        ChangeNode change = changes.get(relPath);
        if (change != null) {
            return change;
        }
        pathOrder.add(relPath);

        change = new ChangeNode();
        change.changing = SVNRepository.INVALID_REVISION;
        change.deleting = SVNRepository.INVALID_REVISION;
        change.kind = SVNNodeKind.UNKNOWN;
        changes.put(relPath, change);
        return change;
    }

    private String mapToReposRelPath(String pathOrUrl) {
        if (SVNPathUtil.isURL(pathOrUrl)) {
            return SVNPathUtil.getPathAsChild(SVNEncodingUtil.uriDecode(pathOrUrl), repositoryRoot.toDecodedString());
        } else {
            return SVNPathUtil.append(baseRelPath, pathOrUrl.startsWith("/") ? pathOrUrl.substring("/".length()) : pathOrUrl);
        }
    }

    private InputStream openDeltaBase(FileBaton fileBaton) throws SVNException {
        return SVNFileUtil.openFileForReading(fileBaton.deltaBase);
    }

    private OutputStream openDeltaTarget(FileBaton fileBaton) throws SVNException {
        fileBaton.deltaTarget = SVNFileUtil.createUniqueFile(tempDirectory, "editor", ".tmp", false);
        return SVNFileUtil.openFileForWriting(fileBaton.deltaTarget);
    }

    private static enum RestructureAction {
        RESTRUCTURE_NONE, RESTRUCTURE_ADD, RESTRUCTURE_ADD_ABSENT, RESTRUCTURE_DELETE
    }

    private static class ChangeNode {
        private RestructureAction action;
        private SVNNodeKind kind;
        private long changing;
        private long deleting;
        private SVNProperties properties;
        private boolean contentsChanged;
        private File contentsAbsPath;
        private SvnChecksum checksum;
        private String copyFromPath;
        private long copyFromRevision;
        private boolean unlock;
    }

    private static class DirectoryBaton {
        public long baseRevision;
        public String path;
        public DirectoryBaton parent;
        public String copyFromRelPath;
        public long copyFromRevision;
    }

    private static class FileBaton {

        public String path;
        public long baseRevision;
        public File deltaBase;
        public File deltaTarget;
    }
}
