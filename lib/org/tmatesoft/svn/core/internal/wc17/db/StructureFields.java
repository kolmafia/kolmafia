package org.tmatesoft.svn.core.internal.wc17.db;

import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure.TypeSafety;
import org.tmatesoft.svn.core.wc2.SvnChecksum;

public class StructureFields {
    
    public enum PristineInfo implements TypeSafety {
        
        status(SVNWCDbStatus.class), 
        kind(SVNWCDbKind.class), 
        changed_rev(Long.TYPE), 
        changed_author(String.class), 
        changed_date(SVNDate.class),
        depth(SVNDepth.class), 
        checksum(SvnChecksum.class), 
        target(String.class),
        hadProps(Boolean.TYPE);

        public static final Collection<PristineInfo> all = Collections.emptyList(); 
        public static final Collection<PristineInfo> defaults = all;
        
        private Class<?> valueType;

        private PristineInfo(Class<?> valueType) {
            this.valueType = valueType; 
        }

        public Class<?> getType() {
            return valueType;
        }
    }
    
    public enum RepositoryInfo implements TypeSafety {
        reposRootUrl(SVNURL.class),
        reposUuid(String.class);
        
        private Class<?> valueType;

        private RepositoryInfo(Class<?> valueType) {
            this.valueType = valueType; 
        }

        public Class<?> getType() {
            return valueType;
        }
    }
    
    public enum NodeOriginInfo {
        isCopy,
        revision,
        reposRelpath,
        reposRootUrl,
        reposUuid,
        copyRootAbsPath;
    }
    
    public enum NodeInfo {
        status, 
        kind, 
        revision, 
        reposRelPath, 
        reposId, 
        reposRootUrl, 
        reposUuid, 
        changedRev, 
        changedDate, 
        changedAuthor, 
        recordedTime, 
        depth, 
        checksum, 
        recordedSize, 
        target, 
        changelist, 
        originalReposId, 
        originalReposRelpath, 
        originalRootUrl, 
        originalUuid, 
        originalRevision, 
        textMod, 
        propsMod, 
        conflicted, 
        lock, 
        haveBase, 
        haveWork, 
        opRoot, 
        hadProps, 
        haveMoreWork,
        /* base info field */
        updateRoot;
    }
    
    public enum WalkerChildInfo {
        status, 
        kind;
    }
    
    public enum ExternalNodeInfo {
        kind,
        presence,
        reposId,
        reposRelPath,
        revision,
        reposRootUrl,
        reposUuid,
        properties,
        changedRevision,
        changedDate,
        changedAuthor,
        davCache,
        checksum,
        target,
        recordAncestorRelPath,
        recordedReposRelPath,
        recordedPegRevision,
        recordedRevision,
        conflict,
        updateActualProperties,
        newActualProperties,
        keepRecordedInfo,
        workItems;
    }
    
    public enum DeletionInfo {
        baseDelRelPath,
        movedToRelPath,
        workDelRelPath, 
        workDelAbsPath;
    }
    
    public enum AdditionInfo {
        status, 
        opRootAbsPath, 
        opRootRelPath,
        reposRelPath, 
        reposId,
        reposRootUrl, 
        reposUuid, 
        originalReposRelPath, 
        originalReposId,
        originalRootUrl, 
        originalUuid, 
        originalRevision
    }
    
    
}
