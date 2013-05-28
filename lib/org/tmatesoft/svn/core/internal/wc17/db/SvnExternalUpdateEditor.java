package org.tmatesoft.svn.core.internal.wc17.db;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNUpdateEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergeInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergePropertiesInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.WritableBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnExternalUpdateEditor implements ISVNUpdateEditor {

    private long targetRevision;
    private String name;
    private long originalRevision;
    private File localAbsPath;
    private SVNWCContext context;
    private SvnChecksum originalChecksum;
    private File wriAbsPath;
    private SVNDeltaProcessor deltaProcessor;
    
    private WritableBaseInfo currentBase;
    private SVNProperties davPropChanges;
    private SVNProperties entryPropChanges;
    private SVNProperties regularPropChanges;
    private SVNURL url;
    private SVNURL reposRootUrl;
    private long changedRev;
    private String changedAuthor;
    private SVNDate changedDate;
    private String[] extPatterns;
    private boolean useCommitTimes;
    private String reposUuid;
    private File recordAncestorAbspath;
    private File recordedReposRelPath;
    private long recordedPegRevision;
    private long recordedRevision;
    private SvnChecksum newMd5Checksum;
    private SvnChecksum newSha1Checksum;
    private boolean fileClosed;
    
    public static ISVNUpdateEditor createEditor(SVNWCContext context, File localAbsPath, File wriAbsPath,
            SVNURL url, SVNURL reposRootUrl, String reposUuid, boolean useCommitTimes, String[] preservedExts,
            File recordAncestorAbsPath, SVNURL recordedUrl, SVNRevision recordedPegRev, SVNRevision recordedRev) { 
        
        SvnExternalUpdateEditor editor = new SvnExternalUpdateEditor();
        editor.context = context;
        editor.localAbsPath = localAbsPath;
        editor.wriAbsPath = wriAbsPath != null ? wriAbsPath : SVNFileUtil.getParentFile(localAbsPath);
        editor.url = url;
        editor.reposRootUrl = reposRootUrl;
        editor.reposUuid = reposUuid;
        editor.name = SVNFileUtil.getFileName(localAbsPath);
        editor.useCommitTimes = useCommitTimes;
        editor.extPatterns = preservedExts;
        
        editor.recordAncestorAbspath = recordAncestorAbsPath;
        editor.recordedReposRelPath = SVNFileUtil.createFilePath(SVNURLUtil.getRelativeURL(reposRootUrl, recordedUrl, false));
        editor.recordedPegRevision = recordedPegRev != null ? recordedPegRev.getNumber() : SVNWCContext.INVALID_REVNUM;
        editor.recordedRevision = recordedRev != null ? recordedRev.getNumber() : SVNWCContext.INVALID_REVNUM;
        
        return (ISVNUpdateEditor) SVNCancellableEditor.newInstance(editor, context.getEventHandler(), null);
    }
    
    public SvnExternalUpdateEditor() {
        deltaProcessor = new SVNDeltaProcessor();
    }

    public void targetRevision(long revision) throws SVNException {
        targetRevision = revision;
    }

    public void openRoot(long revision) throws SVNException {
    }

    public void deleteEntry(String path, long revision) throws SVNException {
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
    }

    public void openDir(String path, long revision) throws SVNException {
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
    }

    public void closeDir() throws SVNException {
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        String name = SVNPathUtil.tail(path);
        if (!name.equals(this.name)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "This editor can only update ''{0}''", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        originalRevision = SVNWCContext.INVALID_REVNUM;
    }

    public void openFile(String path, long revision) throws SVNException {
        String name = SVNPathUtil.tail(path);
        if (!name.equals(this.name)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "This editor can only update ''{0}''", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        WCDbBaseInfo baseInfo = context.getDb().getBaseInfo(localAbsPath, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.changedRev, 
                BaseInfoField.changedAuthor, BaseInfoField.changedDate, BaseInfoField.checksum);
        originalRevision = baseInfo.revision;
        changedRev = baseInfo.changedRev;
        changedDate = baseInfo.changedDate;
        changedAuthor = baseInfo.changedAuthor;
        originalChecksum = baseInfo.checksum;

        if (baseInfo.kind != SVNWCDbKind.File) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Node ''{0}'' is not existing file external", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        if (SVNProperty.isRegularProperty(propertyName)) {
            if (regularPropChanges == null) {
                regularPropChanges = new SVNProperties();
            }
            regularPropChanges.put(propertyName, propertyValue);
        } else if (SVNProperty.isEntryProperty(propertyName)) {
            if (entryPropChanges == null) {
                entryPropChanges = new SVNProperties();
            }
            entryPropChanges.put(propertyName, propertyValue);
        } else if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            if (davPropChanges == null) {
                davPropChanges = new SVNProperties();
            }
            davPropChanges.put(propertyName, propertyValue);
        }
    }

    public void closeFile(String path, String expectedMd5Digest) throws SVNException {
        fileClosed = true;
        
        if (expectedMd5Digest != null) {
            SvnChecksum expectedMd5Checksum = new SvnChecksum(SvnChecksum.Kind.md5, expectedMd5Digest);
            SvnChecksum actualMd5Checksum = newMd5Checksum;
            if (actualMd5Checksum == null) {
                actualMd5Checksum = originalChecksum;
                if (actualMd5Checksum != null && actualMd5Checksum.getKind() != SvnChecksum.Kind.md5) {
                    actualMd5Checksum = context.getDb().getPristineMD5(wriAbsPath, actualMd5Checksum);
                }
            }
            if (!expectedMd5Checksum.equals(actualMd5Checksum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, 
                        "Checksum mismatch for ''{0}'':\n" +
                        "   expected: ''{1}''\n" +
                        "     actual: ''{2}''\n", localAbsPath, expectedMd5Checksum, actualMd5Checksum);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        if (newSha1Checksum != null) {
            context.getDb().installPristine(currentBase.tempBaseAbspath, currentBase.getSHA1Checksum(), currentBase.getMD5Checksum());
            SVNFileUtil.deleteFile(currentBase.tempBaseAbspath);
        }
        boolean added = originalRevision == SVNWCContext.INVALID_REVNUM;
        File reposRelPath = SVNFileUtil.createFilePath(SVNURLUtil.getRelativeURL(reposRootUrl, url, false));
        
        SvnChecksum newChecksum = null;
        SVNProperties actualProperties = null;
        SVNProperties baseProperties = new SVNProperties();
        SVNSkel allWorkItems = null;
        if (!added) {
            newChecksum = originalChecksum;
            baseProperties = context.getDb().getBaseProps(localAbsPath);
            actualProperties = context.getDb().readProperties(localAbsPath);
        }
        if (actualProperties == null) {
            actualProperties = new SVNProperties();
        }
        if (newSha1Checksum != null) {
            newChecksum = newSha1Checksum;
        }
        
        for (String propName : entryPropChanges.nameSet()) {
            SVNPropertyValue value = entryPropChanges.getSVNPropertyValue(propName);
            if (value == null) {
                continue;
            }
            if (SVNProperty.COMMITTED_DATE.equals(propName)) {
                changedDate = SVNDate.parseDate(value.getString());
            } else if (SVNProperty.LAST_AUTHOR.equals(propName)) {
                changedAuthor = value.getString();
            } else if (SVNProperty.COMMITTED_REVISION.equals(propName)) {
                try {
                    changedRev = Long.parseLong(value.getString());
                } catch (NumberFormatException nfe) {
                }
            }
        }
        SVNStatusType contentState = SVNStatusType.UNKNOWN;
        SVNStatusType propState = SVNStatusType.UNKNOWN;
        
        SVNProperties newActualProperties;
        SVNProperties newPristineProperties;
        if (regularPropChanges != null) {
            MergePropertiesInfo mergeInfo = new MergePropertiesInfo();
            mergeInfo.newActualProperties = new SVNProperties();
            mergeInfo.newBaseProperties = new SVNProperties();

            mergeInfo = context.mergeProperties2(mergeInfo, localAbsPath, SVNWCDbKind.File, 
                    null, null, null, baseProperties, actualProperties, regularPropChanges, 
                    true, false);
            if (mergeInfo.workItems != null) {
                allWorkItems = context.wqMerge(allWorkItems, mergeInfo.workItems);
            }
            propState = mergeInfo.mergeOutcome;
            newActualProperties = mergeInfo.newActualProperties;
            newPristineProperties = mergeInfo.newBaseProperties;
        } else {
            newActualProperties = baseProperties;
            newPristineProperties = actualProperties;
        }
        boolean installPristine = false;
        boolean obstructed = false;

        if (newSha1Checksum != null) {
            SVNFileType fileType = SVNFileType.getType(localAbsPath);
            SVNNodeKind kind = SVNFileType.getNodeKind(fileType);
            if (kind == SVNNodeKind.NONE) {
                installPristine = true;
                contentState = SVNStatusType.CHANGED;
            } else if (kind != SVNNodeKind.FILE) {
                obstructed = true;
                contentState = SVNStatusType.UNCHANGED;
            } else {
                boolean isModified = context.isTextModified(localAbsPath, false);
                if (!isModified) {
                    installPristine = true;
                    contentState = SVNStatusType.CHANGED;
                } else {
                    SVNProperties propChanges = new SVNProperties();
                    if (regularPropChanges != null) {
                        propChanges.putAll(regularPropChanges);
                    } 
                    if (entryPropChanges != null) {
                        propChanges.putAll(entryPropChanges);
                    }
                    if (davPropChanges != null) {
                        propChanges.putAll(davPropChanges);
                    }
                    MergeInfo outcome = SVNUpdateEditor17.performFileMerge(context, localAbsPath, wriAbsPath, newChecksum, originalChecksum, actualProperties, 
                            extPatterns, originalRevision, targetRevision, propChanges);
                    if (outcome.workItems != null) {
                        allWorkItems = context.wqMerge(allWorkItems, outcome.workItems);
                    }
                    contentState = outcome.mergeOutcome;
                }
            }
            
            if (installPristine) {
                SVNSkel workItem = context.wqBuildFileInstall(localAbsPath, null, useCommitTimes, true);
                allWorkItems = context.wqMerge(allWorkItems, workItem);
            }
        } else {
            contentState = SVNStatusType.UNCHANGED;
        }
        
        if (davPropChanges != null) {
            davPropChanges.removeNullValues();
        }
        SvnWcDbExternals.addExternalFile(context, 
                localAbsPath, 
                wriAbsPath, 
                reposRelPath, 
                reposRootUrl, 
                reposUuid, 
                targetRevision, 
                newPristineProperties,
                changedRev,
                changedDate,
                changedAuthor,
                newChecksum,
                davPropChanges,
                recordAncestorAbspath,
                recordedReposRelPath,
                recordedPegRevision,
                recordedRevision,
                true,
                newActualProperties,
                false,
                allWorkItems);
        
        context.wqRun(wriAbsPath);
        
        if (context.getEventHandler() != null) {
            SVNEventAction action = null;
            if (originalRevision != SVNWCContext.INVALID_REVNUM) {
                action = obstructed ? SVNEventAction.UPDATE_SHADOWED_UPDATE : SVNEventAction.UPDATE_UPDATE;
            } else {
                action = obstructed ? SVNEventAction.UPDATE_SHADOWED_ADD : SVNEventAction.UPDATE_ADD;
            }
            String mimeType = context.getProperty(localAbsPath, SVNProperty.MIME_TYPE);
            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.FILE, mimeType, targetRevision, contentState, propState, null, action, null, null, null);
            event.setPreviousRevision(originalRevision);
            context.getEventHandler().handleEvent(event, -1);
        }
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        if (!fileClosed) {
            context.getDb().opBumpRevisionPostUpdate(localAbsPath, SVNDepth.INFINITY, null, null, null, targetRevision, null);
        }
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public void applyTextDelta(String path, String baseChecksumDigest) throws SVNException {
        InputStream source = SVNFileUtil.DUMMY_IN;
        if (originalChecksum != null) {
            if (baseChecksumDigest != null) {
                SvnChecksum expectedChecksum = new SvnChecksum(SvnChecksum.Kind.md5, baseChecksumDigest);
                SvnChecksum originalMd5;
                if (originalChecksum.getKind() != SvnChecksum.Kind.md5) {
                    originalMd5 = context.getDb().getPristineMD5(wriAbsPath, originalChecksum);
                } else {
                    originalMd5 = originalChecksum;
                }
                if (!expectedChecksum.equals(originalMd5)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, 
                            "Base checksum mismatch for ''{0}'':\n" +
                            "   expected: ''{1}''\n" +
                            "     actual: ''{2}''\n", localAbsPath, expectedChecksum, originalMd5);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            source = context.getDb().readPristine(wriAbsPath, originalChecksum);
        }
        
        currentBase = context.openWritableBase(wriAbsPath, true, true);
        
        deltaProcessor.applyTextDelta(source, currentBase.stream, true);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        if (currentBase == null) {
            return SVNFileUtil.DUMMY_OUT;
        }
        try {
            deltaProcessor.textDeltaChunk(diffWindow);
        } catch (SVNException svne) {
            deltaProcessor.textDeltaEnd();
            
            SVNFileUtil.deleteFile(currentBase.tempBaseAbspath);
            currentBase.tempBaseAbspath = null;
            throw svne;
        }
        return SVNFileUtil.DUMMY_OUT;
    }

    public void textDeltaEnd(String path) throws SVNException {
        if (currentBase == null) {
            return;
        }
        deltaProcessor.textDeltaEnd();        
        newSha1Checksum = currentBase.getSHA1Checksum();
        newMd5Checksum = currentBase.getMD5Checksum();
    }

    public long getTargetRevision() {
        return targetRevision;
    }

}
