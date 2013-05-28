package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.wc.ISVNMerger;
import org.tmatesoft.svn.core.wc.ISVNMergerFactory;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;

/**
 * Merge driver interface used by <code>SVNKit</code> in merging operations. 
 * 
 * <p>
 * Merge drivers are created by a merger factory implementing the 
 * {@link ISVNMergerFactory} interface. 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see org.tmatesoft.svn.core.internal.wc17.DefaultSvnMerger
 */
public interface ISvnMerger extends ISVNMerger {
    
	/**
	 * Performs a text merge.
	 * 
	 * @param baseMerger
	 * @param resultFile
	 * @param targetAbspath working copy absolute path of the target
	 * @param detranslatedTargetAbspath
	 * @param leftAbspath
	 * @param rightAbspath
	 * @param targetLabel
	 * @param leftLabel
	 * @param rightLabel
	 * @param options merge options to take into account
	 * @return result of merging
	 * @throws SVNException
	 */
    public SvnMergeResult mergeText(
            ISvnMerger baseMerger,
            File resultFile, 
            File targetAbspath, 
            File detranslatedTargetAbspath, 
            File leftAbspath, 
            File rightAbspath, 
            String targetLabel, 
            String leftLabel, 
            String rightLabel, 
            SVNDiffOptions options) throws SVNException;
    
    /**
     * Merges the property changes <code>propChanges</code> based on <code>serverBaseProperties</code> 
     * into the working copy <code>localAbsPath</code>
     * 
     * @param baseMerger
     * @param localAbsPath working copy absolute path
     * @param kind node kind
     * @param leftVersion
     * @param rightVersion
     * @param serverBaseProperties properties that come from the server
     * @param pristineProperties  pristine properties
     * @param actualProperties actual (working) properties
     * @param propChanges property changes that come from the repository
     * @param baseMerge if <code>false</code>, then changes only working properties; otherwise, changes both the base and working properties
     * @param dryRun if <code>true</code>, merge is simulated only, no real changes are done
     * @return result of merging 
     * @throws SVNException
     */
    public SvnMergeResult mergeProperties(
            ISvnMerger baseMerger,
            File localAbsPath, 
            SVNNodeKind kind, 
            SVNConflictVersion leftVersion, 
            SVNConflictVersion rightVersion,
            SVNProperties serverBaseProperties, 
            SVNProperties pristineProperties, 
            SVNProperties actualProperties, 
            SVNProperties propChanges,
            boolean baseMerge, 
            boolean dryRun) throws SVNException;
}
