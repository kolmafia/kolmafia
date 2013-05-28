package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.FSMergerBySequence;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNDiffConflictChoiceStyle;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNLog;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ConflictMarkersInfo;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;
import org.tmatesoft.svn.core.wc.SVNMergeResult;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnMerger;
import org.tmatesoft.svn.core.wc2.SvnMergeResult;
import org.tmatesoft.svn.util.SVNLogType;

import de.regnis.q.sequence.line.QSequenceLineRAData;
import de.regnis.q.sequence.line.QSequenceLineRAFileData;

public class DefaultSvnMerger implements ISvnMerger {
    
    private final SVNWCContext context;
    private SVNSkel workItems;

    public DefaultSvnMerger(SVNWCContext context) {
        this.context = context;
    }
    
    public SVNSkel getWorkItems() {
        return workItems;
    }

    public SVNMergeResult mergeText(SVNMergeFileSet files, boolean dryRun, SVNDiffOptions options) throws SVNException {
        return null;
    }

    public SVNMergeResult mergeProperties(String localPath, SVNProperties workingProperties, SVNProperties baseProperties, SVNProperties serverBaseProps, SVNProperties propDiff, SVNAdminArea adminArea, SVNLog log, boolean baseMerge, boolean dryRun) throws SVNException {
        return null;
    }
    
    public SvnMergeResult mergeText(ISvnMerger baseMerger, File resultFile,
            File targetAbspath,
            File detranslatedTargetAbspath, File leftAbspath,
            File rightAbspath, String targetLabel, String leftLabel,
            String rightLabel, SVNDiffOptions options) throws SVNException {
        
        ConflictMarkersInfo markersInfo = context.initConflictMarkers(targetLabel, leftLabel, rightLabel);
        String targetMarker = markersInfo.targetMarker;
        String leftMarker = markersInfo.leftMarker;
        String rightMarker = markersInfo.rightMarker;
        FSMergerBySequence merger = new FSMergerBySequence(targetMarker.getBytes(), SVNWCContext.CONFLICT_SEPARATOR, rightMarker.getBytes(), leftMarker.getBytes());
        int mergeResult = 0;
        RandomAccessFile localIS = null;
        RandomAccessFile latestIS = null;
        RandomAccessFile baseIS = null;
        OutputStream result = null;
        try {
            result = SVNFileUtil.openFileForWriting(resultFile);
            localIS = new RandomAccessFile(detranslatedTargetAbspath, "r");
            latestIS = new RandomAccessFile(rightAbspath, "r");
            baseIS = new RandomAccessFile(leftAbspath, "r");

            QSequenceLineRAData baseData = new QSequenceLineRAFileData(baseIS);
            QSequenceLineRAData localData = new QSequenceLineRAFileData(localIS);
            QSequenceLineRAData latestData = new QSequenceLineRAFileData(latestIS);
            mergeResult = merger.merge(baseData, localData, latestData, options, result, SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.WC);
        } finally {
            SVNFileUtil.closeFile(result);
            SVNFileUtil.closeFile(localIS);
            SVNFileUtil.closeFile(baseIS);
            SVNFileUtil.closeFile(latestIS);
        }
        
        if (mergeResult == FSMergerBySequence.CONFLICTED) {
            return SvnMergeResult.create(SVNStatusType.CONFLICTED);
        }
        return SvnMergeResult.create(SVNStatusType.MERGED);
    }

    public SvnMergeResult mergeProperties(ISvnMerger baseMerger,
            File localAbsPath, SVNNodeKind kind,
            SVNConflictVersion leftVersion, SVNConflictVersion rightVersion,
            SVNProperties serverBaseProperties,
            SVNProperties pristineProperties, SVNProperties actualProperties,
            SVNProperties propChanges, boolean baseMerge, boolean dryRun) throws SVNException {
        
        this.workItems = null;
        SVNSkel conflictSkel = null;
        boolean isDir = (kind == SVNNodeKind.DIR);
        ISVNConflictHandler conflictResolver = context.getOptions().getConflictResolver();
        
        SVNStatusType mergeOutcome = SVNStatusType.UNCHANGED;
        
        if (propChanges != null) {
            for (Iterator<?> i = propChanges.nameSet().iterator(); i.hasNext();) {
                context.checkCancelled();
                String propname = (String) i.next();
                SVNPropertyValue toVal = propChanges.getSVNPropertyValue(propname);
                SVNPropertyValue fromVal = serverBaseProperties.getSVNPropertyValue(propname);
                SVNPropertyValue baseVal = pristineProperties.getSVNPropertyValue(propname);
                boolean conflictRemains;
                if (baseMerge) {
                    if (toVal != null) {
                        pristineProperties.put(propname, toVal);
                    } else {
                        pristineProperties.remove(propname);
                    }
                }
                SVNPropertyValue mineVal = actualProperties.getSVNPropertyValue(propname);
                mergeOutcome = context.setPropMergeState(mergeOutcome, SVNStatusType.CHANGED);
                if (fromVal == null) {
                    SVNWCContext.MergePropStatusInfo mergePropStatus = context.applySinglePropAdd(mergeOutcome, localAbsPath, leftVersion, rightVersion, isDir, actualProperties, propname, baseVal, toVal, conflictResolver, dryRun);
                    mergeOutcome = mergePropStatus.state;
                    conflictRemains = mergePropStatus.conflictRemains;
                } else if (toVal == null) {
                    SVNWCContext.MergePropStatusInfo mergePropStatus = context.applySinglePropDelete(mergeOutcome, localAbsPath, leftVersion, rightVersion, isDir, actualProperties, propname, baseVal, fromVal, conflictResolver, dryRun);
                    mergeOutcome = mergePropStatus.state;
                    conflictRemains = mergePropStatus.conflictRemains;
                } else {
                    SVNWCContext.MergePropStatusInfo mergePropStatus = context.applySinglePropChange(mergeOutcome, localAbsPath, leftVersion, rightVersion, isDir, actualProperties, propname, baseVal, fromVal, toVal, conflictResolver, dryRun);
                    mergeOutcome = mergePropStatus.state;
                    conflictRemains = mergePropStatus.conflictRemains;
                }
                if (conflictRemains) {
                    mergeOutcome = context.setPropMergeState(mergeOutcome, SVNStatusType.CONFLICTED);
                    if (dryRun) {
                        continue;
                    }
                    if (conflictSkel == null) {
                        conflictSkel = SVNSkel.createEmptyList();
                    }
                    context.conflictSkelAddPropConflict(conflictSkel, propname, baseVal, mineVal, toVal, fromVal);
                }
            }
        }
        
        if (dryRun) {
            return SvnMergeResult.create(mergeOutcome);
        }
        SvnMergeResult result = SvnMergeResult.create(mergeOutcome);
        result.getBaseProperties().putAll(pristineProperties);
        result.getActualProperties().putAll(actualProperties);
        
        SVNSkel workItems = null;
        if (conflictSkel != null) {
            File rejectPath = context.getPrejfileAbspath(localAbsPath);
            if (rejectPath == null) {
                File rejectDirpath;
                String rejectFilename;
                if (isDir) {
                    rejectDirpath = localAbsPath;
                    rejectFilename = SVNWCContext.THIS_DIR_PREJ;
                } else {
                    rejectDirpath = SVNFileUtil.getFileDir(localAbsPath);
                    rejectFilename = SVNFileUtil.getFileName(localAbsPath);
                }
                rejectPath = SVNFileUtil.createUniqueFile(rejectDirpath, rejectFilename, SVNWCContext.PROP_REJ_EXT, false);
                SVNSkel workItem = context.wqBuildSetPropertyConflictMarkerTemp(localAbsPath, rejectPath);
                workItems = context.wqMerge(workItems, workItem);
            }
            SVNSkel workItem = context.wqBuildPrejInstall(localAbsPath, conflictSkel);
            workItems = context.wqMerge(workItems, workItem);
        }
        this.workItems = workItems;
        return result;
    }

}
