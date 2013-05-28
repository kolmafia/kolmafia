package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableOutputStream;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNExportEditor;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteExport extends SvnRemoteOperationRunner<Long, SvnExport> {

    @Override
    public boolean isApplicable(SvnExport operation, SvnWcGeneration wcGeneration) throws SVNException {
        // remote source
        if (!operation.getSource().isLocal()) {
            return true;
        }
        return false;
    }


    @Override
    protected Long run() throws SVNException {
        SvnTarget exportSource = getOperation().getSource();
        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(exportSource, getOperation().getRevision(), exportSource.getResolvedPegRevision(), exportSource.getFile());
        final long revNumber = repositoryInfo.lng(RepositoryInfo.revision);
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        repositoryInfo.release();
        
        File dstPath = getOperation().getFirstTarget().getFile();
        SVNDepth depth = getOperation().getDepth();
        boolean force = getOperation().isForce();
        boolean expandKeywords = getOperation().isExpandKeywords();
        String eolStyle = getOperation().getEolStyle();
        boolean ignoreExternals = getOperation().isIgnoreExternals();
        ISVNOptions options = getOperation().getOptions();
        
        SVNNodeKind dstKind = repository.checkPath("", revNumber);
        if (dstKind == SVNNodeKind.DIR) {
            SVNExportEditor editor = new SVNExportEditor(this, repository.getLocation().toString(), dstPath, force, eolStyle, expandKeywords, options);
            repository.update(revNumber, null, depth, false, new ISVNReporterBaton() {

                public void report(ISVNReporter reporter) throws SVNException {
                    reporter.setPath("", null, revNumber, SVNDepth.INFINITY, true);
                    reporter.finishReport();
                }
            }, SVNCancellableEditor.newInstance(editor, this, null));

            SVNFileType fileType = SVNFileType.getType(dstPath);
            if (fileType == SVNFileType.NONE) {
                editor.openRoot(revNumber);
            }
            if (!ignoreExternals && depth == SVNDepth.INFINITY) {
                Map<String,String> externals = editor.getCollectedExternals();
                handleExternals(externals, repository.getLocation(), dstPath, repository.getRepositoryRoot(true));
            }
        } else if (dstKind == SVNNodeKind.FILE) {
            String url = repository.getLocation().toString();
            if (dstPath.isDirectory()) {
                dstPath = new File(dstPath, SVNEncodingUtil.uriDecode(SVNPathUtil.tail(url)));
            }
            if (dstPath.exists()) {
                if (!force) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Path ''{0}'' already exists", dstPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } else {
                dstPath.getParentFile().mkdirs();
            }
            SVNProperties properties = new SVNProperties();
            OutputStream os = null;
            File tmpFile = SVNFileUtil.createUniqueFile(dstPath.getParentFile(), ".export", ".tmp", false);
            try {
                os = SVNFileUtil.openFileForWriting(tmpFile);
                try {
                    repository.getFile("", revNumber, properties, new SVNCancellableOutputStream(os, this));
                } finally {
                    SVNFileUtil.closeFile(os);
                }
                if (force && dstPath.exists()) {
                    SVNFileUtil.deleteAll(dstPath, this);
                }
                if (!expandKeywords) {
                    properties.put(SVNProperty.MIME_TYPE, "application/octet-stream");
                }
                String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);
                boolean binary = SVNProperty.isBinaryMimeType(mimeType);
                String charset = SVNTranslator.getCharset(properties.getStringValue(SVNProperty.CHARSET), mimeType, url, options);
                Map<String, byte[]> keywords = SVNTranslator.computeKeywords(properties.getStringValue(SVNProperty.KEYWORDS), url, properties.getStringValue(SVNProperty.LAST_AUTHOR), properties
                        .getStringValue(SVNProperty.COMMITTED_DATE), properties.getStringValue(SVNProperty.COMMITTED_REVISION), options);
                byte[] eols = null;
                if (SVNProperty.EOL_STYLE_NATIVE.equals(properties.getStringValue(SVNProperty.EOL_STYLE))) {
                    eols = SVNTranslator.getEOL(eolStyle != null ? eolStyle : properties.getStringValue(SVNProperty.EOL_STYLE), options);
                } else if (properties.containsName(SVNProperty.EOL_STYLE)) {
                    eols = SVNTranslator.getEOL(properties.getStringValue(SVNProperty.EOL_STYLE), options);
                }
                if (binary) {
                    charset = null;
                    eols = null;
                    keywords = null;
                }
                SVNTranslator.translate(tmpFile, dstPath, charset, eols, keywords, properties.getStringValue(SVNProperty.SPECIAL) != null, true);
            } finally {
                SVNFileUtil.deleteFile(tmpFile);
            }
            if (properties.getStringValue(SVNProperty.EXECUTABLE) != null) {
                SVNFileUtil.setExecutable(dstPath, true);
            }
            
            handleEvent(SVNEventFactory.createSVNEvent(dstPath, SVNNodeKind.FILE, null, SVNRepository.INVALID_REVISION, SVNEventAction.UPDATE_ADD, null, null, null));
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' doesn't exist", repository.getLocation());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        handleEvent(SVNEventFactory.createSVNEvent(dstPath, SVNNodeKind.NONE, null, revNumber, SVNEventAction.UPDATE_COMPLETED, null, null, null));

        return revNumber;
    }
    
    private void handleExternals(Map<String, String> externals, SVNURL fromUrl, File toPath, SVNURL reposRootUrl) throws SVNException {
        
        for (String relativePath : externals.keySet()) {
            File dirPath = new File(toPath, relativePath);
            String externalValue = externals.get(relativePath);
            
            SVNExternal[] exts = SVNExternal.parseExternals(dirPath, externalValue);
            SVNURL directoryUrl = fromUrl.appendPath(relativePath, false);
            for (int i = 0; i < exts.length; i++) {
                File externalDirectory = new File(dirPath, exts[i].getPath());
                File parentDirectory = SVNFileUtil.getParentFile(externalDirectory);
                if (parentDirectory != null) {
                    SVNFileUtil.ensureDirectoryExists(parentDirectory);
                }
                exts[i].resolveURL(reposRootUrl, directoryUrl);
                SVNURL externalUrl = exts[i].getResolvedURL();
                
                SvnExport export = getOperation().getOperationFactory().createExport();
                export.setSource(SvnTarget.fromURL(externalUrl, exts[i].getPegRevision()));
                export.setSingleTarget(SvnTarget.fromFile(externalDirectory));
                export.setDepth(SVNDepth.INFINITY);
                export.setExpandKeywords(getOperation().isExpandKeywords());
                export.setEolStyle(getOperation().getEolStyle());
                export.setRevision(exts[i].getRevision());
                export.setForce(true);
                export.setIgnoreExternals(false);
                export.setSleepForTimestamp(false);
                
                try {
                    export.run();
                } catch (SVNCancelException e) {
                    throw e;
                } catch (SVNException e) {
                    handleEvent(SVNEventFactory.createSVNEvent(externalDirectory, SVNNodeKind.NONE, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.UPDATE_EXTERNAL_REMOVED, 
                            e.getErrorMessage(), null));
                }
            }
        }
        
    }
}
