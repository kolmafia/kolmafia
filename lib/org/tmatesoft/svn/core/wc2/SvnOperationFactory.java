package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.internal.wc.admin.SVNVersionedProperties;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbOpenMode;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbDir;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbRoot;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbPristines;
import org.tmatesoft.svn.core.internal.wc2.ISvnCommitRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryCatImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryCopyRevisionPropertiesImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryCreateImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryDumpImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryFilterImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetAuthorImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetChangedDirectoriesImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetChangedImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetDateImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetDiffImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetHistoryImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetInfoImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetLockImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetLogImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetPropertiesImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetPropertyImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetRevisionPropertiesImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetRevisionPropertyImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetTreeImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetUUIDImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryGetYoungestImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryHotCopyImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryInitializeImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryListLocksImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryListTransactionsImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryLoadImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryPackImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryRecoverImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryRemoveLocksImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryRemoveTransactionsImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositorySetUUIDImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositorySyncInfoImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositorySynchronizeImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryUpgradeImpl;
import org.tmatesoft.svn.core.internal.wc2.admin.SvnRepositoryVerifyImpl;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgAdd;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCanonicalizeUrls;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCat;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCheckout;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCleanup;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCommit;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgDiff;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgExport;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetChangelistPaths;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetMergeInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetProperties;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetStatus;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetStatusSummary;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgLogMergeInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMarkReplaced;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMerge;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergePegged;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeReintegrate;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRelocate;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRemove;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgReposToWcCopy;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgResolve;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRevert;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSetChangelist;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSetLock;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSetProperty;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSuggestMergeSources;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSwitch;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgUnlock;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgUpdate;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgUpgrade;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgWcToReposCopy;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgWcToWcCopy;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldAdd;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldAnnotate;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCanonicalizeUrls;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCat;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCheckout;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCleanup;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCommit;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCopy;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldDiff;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldExport;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetChangelistPaths;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetInfo;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetMergeInfo;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetProperties;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetStatus;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetStatusSummary;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldImport;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldLogMergeInfo;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldMarkReplaced;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldMerge;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRelocate;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRemoteCopy;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRemove;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldResolve;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRevert;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSetChangelist;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSetLock;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSetProperty;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSuggestMergeSources;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSwitch;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUnlock;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpdate;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpgrade;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnNgReposToReposCopy;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteAnnotate;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteCat;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteDiff;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteDiffSummarize;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteExport;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetInfo;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetProperties;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetRevisionProperties;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteList;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteLog;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteRemoteDelete;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteRemoteMkDir;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteSetLock;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteSetPropertyImpl;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteSetRevisionProperty;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteUnlock;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCat;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCopyRevisionProperties;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCreate;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryDump;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryFilter;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetAuthor;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetChanged;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetChangedDirectories;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetDate;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetDiff;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetHistory;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetInfo;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetLock;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetLog;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetProperties;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetProperty;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetRevisionProperties;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetRevisionProperty;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetTree;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetUUID;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetYoungest;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryHotCopy;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryInitialize;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryListLocks;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryListTransactions;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryLoad;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryPack;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryRecover;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryRemoveLocks;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryRemoveTransactions;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySetUUID;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySyncInfo;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySynchronize;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryUpgrade;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryVerify;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents factory for the Svn* operations.
 * Contains corresponding create*() methods for all supported operations.
 * Provides operation options by implementing {@link ISvnOperationOptionsProvider} interface.
 * Handles working copy access and provides access to it {@link #getWcContext()}, {@link #isAutoCloseContext()}.
 * Has set of working copy utility methods: {@link #getWorkingCopyRoot(File, boolean)}, {@link #isWorkingCopyRoot(File)}, 
 * {@link #isVersionedDirectory(File)}, {@link #detectWcGeneration(File, boolean)},
 * {@link #setPrimaryWcGeneration(SvnWcGeneration)}, {@link #isPrimaryWcGenerationOnly()}
 *  
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnOperationFactory implements ISvnOperationOptionsProvider {
    
    private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> anyFormatOperationRunners;
    private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> noneOperationRunners;
    private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> v17OperationRunners;
    private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> v16OperationRunners;
    
    private ISVNAuthenticationManager authenticationManager;
    private ISVNCanceller canceller;
    private ISVNEventHandler eventHandler;
    private ISVNOptions options;
    private ISVNRepositoryPool repositoryPool;
    private ISvnOperationHandler operationHandler;
    
    private boolean autoCloseContext;
    private boolean autoDisposeRepositoryPool;
    private SVNWCContext wcContext;
    
    private SvnWcGeneration primaryWcGeneration;
    private int runLevel;

    /**
     * Creates operation factory and initializes it with empty <code>context</code>.
     */
    public SvnOperationFactory() {
        this(null);
        runLevel = 0;
    }
    
    /**
     * Creates operation factory and initializes it with <code>context</code>.
     * If <code>context</code> is set, retrieves its <code>options</code> and <code>eventHandler</code>
     * and sets <code>autoCloseContext</code> to <code>false</code>, otherwise 
     * sets <code>autoCloseContext</code> to <code>true</code>.
     * 
     * @param context operation's context
     */
    public SvnOperationFactory(SVNWCContext context) {
        wcContext = context;
        
        if (wcContext != null) {
            options = wcContext.getOptions();
            eventHandler = wcContext.getEventHandler();
        }
        setAutoCloseContext(wcContext == null);
        
        registerRunners();
    }
    
    private void registerRunners() {
        v17OperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
        v16OperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
        anyFormatOperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
        noneOperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();

        registerOperationRunner(SvnGetInfo.class, new SvnRemoteGetInfo());
        registerOperationRunner(SvnGetInfo.class, new SvnNgGetInfo());
        registerOperationRunner(SvnGetInfo.class, new SvnOldGetInfo());

        registerOperationRunner(SvnGetProperties.class, new SvnRemoteGetRevisionProperties());
        registerOperationRunner(SvnGetProperties.class, new SvnRemoteGetProperties());
        registerOperationRunner(SvnGetProperties.class, new SvnNgGetProperties());
        registerOperationRunner(SvnGetProperties.class, new SvnOldGetProperties());

        registerOperationRunner(SvnGetStatus.class, new SvnNgGetStatus());
        registerOperationRunner(SvnGetStatus.class, new SvnOldGetStatus());

        registerOperationRunner(SvnCheckout.class, new SvnNgCheckout());
        registerOperationRunner(SvnCheckout.class, new SvnOldCheckout());
        
        registerOperationRunner(SvnSwitch.class, new SvnNgSwitch());
        registerOperationRunner(SvnSwitch.class, new SvnOldSwitch());

        registerOperationRunner(SvnUpdate.class, new SvnNgUpdate());
        registerOperationRunner(SvnUpdate.class, new SvnOldUpdate());

        registerOperationRunner(SvnExport.class, new SvnRemoteExport());
        registerOperationRunner(SvnExport.class, new SvnNgExport());
        registerOperationRunner(SvnExport.class, new SvnOldExport());
        
        registerOperationRunner(SvnRelocate.class, new SvnNgRelocate());
        registerOperationRunner(SvnRelocate.class, new SvnOldRelocate());

        registerOperationRunner(SvnScheduleForAddition.class, new SvnNgAdd());
        registerOperationRunner(SvnScheduleForAddition.class, new SvnOldAdd());

        registerOperationRunner(SvnScheduleForRemoval.class, new SvnNgRemove());
        registerOperationRunner(SvnScheduleForRemoval.class, new SvnOldRemove());
        
        registerOperationRunner(SvnCommit.class, new SvnNgCommit());
        registerOperationRunner(SvnCommit.class, new SvnOldCommit());

        registerOperationRunner(SvnRevert.class, new SvnNgRevert());
        registerOperationRunner(SvnRevert.class, new SvnOldRevert());

        registerOperationRunner(SvnMarkReplaced.class, new SvnNgMarkReplaced());
        registerOperationRunner(SvnMarkReplaced.class, new SvnOldMarkReplaced());

        registerOperationRunner(SvnSetProperty.class, new SvnRemoteSetRevisionProperty());
        registerOperationRunner(SvnSetProperty.class, new SvnOldSetProperty());
        registerOperationRunner(SvnSetProperty.class, new SvnNgSetProperty());
        
        registerOperationRunner(SvnSetLock.class, new SvnRemoteSetLock());
        registerOperationRunner(SvnSetLock.class, new SvnOldSetLock());
        registerOperationRunner(SvnSetLock.class, new SvnNgSetLock());
        
        registerOperationRunner(SvnUnlock.class, new SvnRemoteUnlock());
        registerOperationRunner(SvnUnlock.class, new SvnOldUnlock());
        registerOperationRunner(SvnUnlock.class, new SvnNgUnlock());
        
        registerOperationRunner(SvnCat.class, new SvnRemoteCat());
        registerOperationRunner(SvnCat.class, new SvnNgCat());
        registerOperationRunner(SvnCat.class, new SvnOldCat());

        registerOperationRunner(SvnDiffSummarize.class, new SvnRemoteDiffSummarize());
        
        registerOperationRunner(SvnCopy.class, new SvnNgWcToWcCopy());
        registerOperationRunner(SvnCopy.class, new SvnNgReposToWcCopy());
        registerOperationRunner(SvnCopy.class, new SvnOldCopy());

        registerOperationRunner(SvnRemoteCopy.class, new SvnOldRemoteCopy());
        registerOperationRunner(SvnRemoteCopy.class, new SvnNgWcToReposCopy());
        registerOperationRunner(SvnRemoteCopy.class, new SvnNgReposToReposCopy());
        
        registerOperationRunner(SvnLog.class, new SvnRemoteLog());
        
        registerOperationRunner(SvnAnnotate.class, new SvnOldAnnotate());
        registerOperationRunner(SvnAnnotate.class, new SvnRemoteAnnotate());
        
        registerOperationRunner(SvnSetChangelist.class, new SvnOldSetChangelist());
        registerOperationRunner(SvnSetChangelist.class, new SvnNgSetChangelist());
        
        registerOperationRunner(SvnGetChangelistPaths.class, new SvnOldGetChangelistPaths());
        registerOperationRunner(SvnGetChangelistPaths.class, new SvnNgGetChangelistPaths());
        
        registerOperationRunner(SvnRemoteMkDir.class, new SvnRemoteRemoteMkDir());
        
        registerOperationRunner(SvnRemoteDelete.class, new SvnRemoteRemoteDelete());
        registerOperationRunner(SvnRemoteSetProperty.class, new SvnRemoteSetPropertyImpl());
        
        registerOperationRunner(SvnMerge.class, new SvnOldMerge());
        registerOperationRunner(SvnMerge.class, new SvnNgMergePegged());
        registerOperationRunner(SvnMerge.class, new SvnNgMergeReintegrate());
        registerOperationRunner(SvnMerge.class, new SvnNgMerge());

        registerOperationRunner(SvnDiff.class, new SvnOldDiff());
        registerOperationRunner(SvnDiff.class, new SvnNgDiff());
        registerOperationRunner(SvnDiff.class, new SvnRemoteDiff());

        registerOperationRunner(SvnCleanup.class, new SvnOldCleanup());
        registerOperationRunner(SvnCleanup.class, new SvnNgCleanup());
        
        registerOperationRunner(SvnImport.class, new SvnOldImport());
        
        registerOperationRunner(SvnResolve.class, new SvnOldResolve());
        registerOperationRunner(SvnResolve.class, new SvnNgResolve());
        
        registerOperationRunner(SvnList.class, new SvnRemoteList());
        
        registerOperationRunner(SvnLogMergeInfo.class, new SvnOldLogMergeInfo());
        registerOperationRunner(SvnLogMergeInfo.class, new SvnNgLogMergeInfo());
        registerOperationRunner(SvnGetMergeInfo.class, new SvnOldGetMergeInfo());
        registerOperationRunner(SvnGetMergeInfo.class, new SvnNgGetMergeInfo());

        registerOperationRunner(SvnSuggestMergeSources.class, new SvnNgSuggestMergeSources());
        registerOperationRunner(SvnSuggestMergeSources.class, new SvnOldSuggestMergeSources());

        registerOperationRunner(SvnCanonicalizeUrls.class, new SvnNgCanonicalizeUrls());
        registerOperationRunner(SvnCanonicalizeUrls.class, new SvnOldCanonicalizeUrls());
        
        registerOperationRunner(SvnRepositoryDump.class, new SvnRepositoryDumpImpl());
        registerOperationRunner(SvnRepositoryCreate.class, new SvnRepositoryCreateImpl());
        registerOperationRunner(SvnRepositoryHotCopy.class, new SvnRepositoryHotCopyImpl());
        registerOperationRunner(SvnRepositoryLoad.class, new SvnRepositoryLoadImpl());
        registerOperationRunner(SvnRepositoryListLocks.class, new SvnRepositoryListLocksImpl());
        registerOperationRunner(SvnRepositoryListTransactions.class, new SvnRepositoryListTransactionsImpl());
        registerOperationRunner(SvnRepositoryPack.class, new SvnRepositoryPackImpl());
        registerOperationRunner(SvnRepositoryRecover.class, new SvnRepositoryRecoverImpl());
        registerOperationRunner(SvnRepositoryRemoveLocks.class, new SvnRepositoryRemoveLocksImpl());
        registerOperationRunner(SvnRepositoryRemoveTransactions.class, new SvnRepositoryRemoveTransactionsImpl());
        registerOperationRunner(SvnRepositorySetUUID.class, new SvnRepositorySetUUIDImpl());
        registerOperationRunner(SvnRepositoryUpgrade.class, new SvnRepositoryUpgradeImpl());
        registerOperationRunner(SvnRepositoryVerify.class, new SvnRepositoryVerifyImpl());
        registerOperationRunner(SvnRepositoryInitialize.class, new SvnRepositoryInitializeImpl());
        registerOperationRunner(SvnRepositorySyncInfo.class, new SvnRepositorySyncInfoImpl());
        registerOperationRunner(SvnRepositoryCopyRevisionProperties.class, new SvnRepositoryCopyRevisionPropertiesImpl());
        registerOperationRunner(SvnRepositorySynchronize.class, new SvnRepositorySynchronizeImpl());
        registerOperationRunner(SvnRepositoryFilter.class, new SvnRepositoryFilterImpl());
        registerOperationRunner(SvnRepositoryGetAuthor.class, new SvnRepositoryGetAuthorImpl());
        registerOperationRunner(SvnRepositoryGetDate.class, new SvnRepositoryGetDateImpl());
        registerOperationRunner(SvnRepositoryGetInfo.class, new SvnRepositoryGetInfoImpl());
        registerOperationRunner(SvnRepositoryGetLock.class, new SvnRepositoryGetLockImpl());
        registerOperationRunner(SvnRepositoryGetLog.class, new SvnRepositoryGetLogImpl());
        registerOperationRunner(SvnRepositoryGetUUID.class, new SvnRepositoryGetUUIDImpl());
        registerOperationRunner(SvnRepositoryGetYoungest.class, new SvnRepositoryGetYoungestImpl());
        registerOperationRunner(SvnRepositoryGetProperty.class, new SvnRepositoryGetPropertyImpl());
        registerOperationRunner(SvnRepositoryGetRevisionProperty.class, new SvnRepositoryGetRevisionPropertyImpl());
        registerOperationRunner(SvnRepositoryGetProperties.class, new SvnRepositoryGetPropertiesImpl());
        registerOperationRunner(SvnRepositoryGetRevisionProperties.class, new SvnRepositoryGetRevisionPropertiesImpl());
        registerOperationRunner(SvnRepositoryCat.class, new SvnRepositoryCatImpl());
        registerOperationRunner(SvnRepositoryGetChanged.class, new SvnRepositoryGetChangedImpl());
        registerOperationRunner(SvnRepositoryGetChangedDirectories.class, new SvnRepositoryGetChangedDirectoriesImpl());
        registerOperationRunner(SvnRepositoryGetDiff.class, new SvnRepositoryGetDiffImpl());
        registerOperationRunner(SvnRepositoryGetHistory.class, new SvnRepositoryGetHistoryImpl());
        registerOperationRunner(SvnRepositoryGetTree.class, new SvnRepositoryGetTreeImpl());
        
        registerOperationRunner(SvnUpgrade.class, new SvnOldUpgrade());
        registerOperationRunner(SvnUpgrade.class, new SvnNgUpgrade());

        registerOperationRunner(SvnGetStatusSummary.class, new SvnOldGetStatusSummary());
        registerOperationRunner(SvnGetStatusSummary.class, new SvnNgGetStatusSummary());
    }
    
    /**
     * Returns whether to dispose context when operation finishes.
     *  
     * @return <code>true</code> if the context should be disposed, otherwise <code>false</code>
     */
    public boolean isAutoCloseContext() {
        return autoCloseContext;
    }

    /**
     * Sets whether to dispose context when operation finishes.
     *  
     * @param autoCloseContext <code>true</code> if the context should be disposed, otherwise <code>false</code>
     */
    public void setAutoCloseContext(boolean autoCloseContext) {
        this.autoCloseContext = autoCloseContext;
    }

    /**
     * Gets operation's authentication manager.
     * If not set, creates default authentication manager.
     * 
     * @return authentication manager
     */
    public ISVNAuthenticationManager getAuthenticationManager() {
        if (authenticationManager == null) {
            authenticationManager = SVNWCUtil.createDefaultAuthenticationManager();
        }
        return authenticationManager;
    }

    /**
     * Gets the cancel handler of the operation.
     * If client's <code>canceler</code> is not set, 
     * returns <code>eventHandler</code> as a canceler.
     * 
     * @return cancel handler
     */
    public ISVNCanceller getCanceller() {
        if (canceller == null && getEventHandler() != null) {
            return getEventHandler();
        }
        return canceller;
    }

    /**
     * Gets the event handler for the operation. This event handler will be
     * dispatched {@link SVNEvent} objects to provide detailed information about
     * actions and progress state of version control operations. 
     * If <code>wcContext</code> is set, returns {@link  SVNWCContext#getEventHandler()} 
     * 
     * @return handler for events
     * @see ISVNEventHandler
     */
    public ISVNEventHandler getEventHandler() {
        if (getWcContext() != null) {
            return getWcContext().getEventHandler();
        }
        return eventHandler;
    }

    /**
     * Get a callback that is called before and after each operation
     * @return a callback that is called before and after each operation
     */
    public ISvnOperationHandler getOperationHandler() {
        if (operationHandler == null) {
            operationHandler = ISvnOperationHandler.NOOP;
        }
        return operationHandler;
    }

    /**
     * Gets the pool of repositories.
     * If pool is not created, creates {@link DefaultSVNRepositoryPool} 
     * with the authentication manager, options, and <code>autoDisposeRepositoryPool</code> = <code>true</code>.
     * 
     * @return pool of repositories
     */
    public ISVNRepositoryPool getRepositoryPool() {
        if (repositoryPool == null) {
            repositoryPool = new DefaultSVNRepositoryPool(getAuthenticationManager(), getOptions());
            setAutoDisposeRepositoryPool(true);
        }
        return repositoryPool;
    }

    /**
     * Gets operation's options.
     * If options are not set, creates default readonly options.
     * 
     * @return options of the operation
     */
    public ISVNOptions getOptions() {
        if (options == null) {
            options = SVNWCUtil.createDefaultOptions(true);
        }
        return options;
    }

    /**
     * Sets operation's authentication manager.
     * If <code>repositoryPool</code> is set, set its authentication manager to this value.
     * 
     * @param authenticationManager authentication manager
     */
    public void setAuthenticationManager(ISVNAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        if (repositoryPool != null) {
            repositoryPool.setAuthenticationManager(authenticationManager);
        }
    }

    /**
     * Sets the cancel handler of the operation.
     * 
     * @param canceller cancel handler
     */
    public void setCanceller(ISVNCanceller canceller) {
        this.canceller = canceller;
    }

    /**
     * Sets the event handler for the operation. This event handler will be
     * dispatched {@link SVNEvent} objects to provide detailed information about
     * actions and progress state of version control operations. 
     * If <code>wcContext</code> is set, sets its event handler to this one. 
     * If <code>wcContext</code> is not set, disposes it.
     * 
     * @param eventHandler handler for events
     * @see ISVNEventHandler
     */
    public void setEventHandler(ISVNEventHandler eventHandler) {
        this.eventHandler = eventHandler;
        if (this.wcContext != null) {
            this.wcContext.setEventHandler(eventHandler);
        } else {
            disposeWcContext();
        }
    }

    /**
     * Sets operation's options and disposes working copy context.
     * 
     * @param options options of the operation
     */
    public void setOptions(ISVNOptions options) {
        this.options = options;
        disposeWcContext();
    }

    /**
     * Sets pool of repositories.
     * If <code>repositoryPool</code> is not <code>null</code>sets <code>autoDisposeRepositoryPool</code> to <code>false</code>,
     * otherwise to <code>true</code>
     * 
     * @param repositoryPool pool of repositories
     */
    public void setRepositoryPool(ISVNRepositoryPool repositoryPool) {
        this.repositoryPool = repositoryPool;
        setAutoDisposeRepositoryPool(repositoryPool == null);
    }
    
    /**
     * Sets a callback that is called before and after each operation
     * @param operationHandler callback to call before and after operation
     */
    public void setOperationHandler(ISvnOperationHandler operationHandler) {
        this.operationHandler = operationHandler;
    }

    /**
     * Disposes context and repository pool if needed.
     */
    public void dispose() {
        disposeWcContext();
        if (isAutoDisposeRepositoryPool() && repositoryPool != null) {
            repositoryPool.dispose();
        }
    }
    
    /**
     * Creates annotate operation.
     * @return new <code>SvnAnnotate</code> object
     */
    public SvnAnnotate createAnnotate() {
        return new SvnAnnotate(this);
    }
    
    /**
     * Creates cat operation.
     * @return new <code>SvnCat</code> object
     */
    public SvnCat createCat() {
        return new SvnCat(this);
    }

    /**
     * Creates import operation.
     * @return new <code>SvnImport</code> object
     */
    public SvnImport createImport() {
        return new SvnImport(this);
    }
    
    /**
     * Creates copy operation.
     * @return new <code>SvnCat</code> object
     */
    public SvnCopy createCopy() {
        return new SvnCopy(this);
    }
    
    /**
     * Creates remote copy operation.
     * @return new <code>SvnRemoteCopy</code> object
     */
    public SvnRemoteCopy createRemoteCopy() {
        return new SvnRemoteCopy(this);
    }
    
    /**
     * Creates remote make directory operation.
     * @return new <code>SvnRemoteMkDir</code> object
     */
    public SvnRemoteMkDir createRemoteMkDir() {
        return new SvnRemoteMkDir(this);
    }

    /**
     * Creates remote set property operation.
     * @return new <code>SvnRemoteSetProperty</code> object
     */
    public SvnRemoteSetProperty createRemoteSetProperty() {
        return new SvnRemoteSetProperty(this);
    }

    /**
     * Creates set changelist operation.
     * @return new <code>SvnSetChangelist</code> object
     */
    public SvnSetChangelist createSetChangelist() {
        return new SvnSetChangelist(this);
    }
    
    /**
     * Creates get changelist paths operation.
     * @return new <code>SvnGetChangelistPaths</code> object
     */
    public SvnGetChangelistPaths createGetChangelistPaths() {
        return new SvnGetChangelistPaths(this);
    }

    /**
     * Creates set lock operation.
     * @return new <code>SvnSetLock</code> object
     */
    public SvnSetLock createSetLock() {
        return new SvnSetLock(this);
    }

    /**
     * Creates unlock operation.
     * @return new <code>SvnUnlock</code> object
     */
    public SvnUnlock createUnlock() {
        return new SvnUnlock(this);
    }

    /**
     * Creates upgrade operation.
     * @return new <code>SvnUpgrade</code> object
     */
    public SvnUpgrade createUpgrade() {
        return new SvnUpgrade(this);
    }

    /**
     * Creates get info operation.
     * @return new <code>SvnGetInfo</code> object
     */
    public SvnGetInfo createGetInfo() {
        return new SvnGetInfo(this);
    }
    
    /**
     * Creates get properties operation.
     * @return new <code>SvnGetProperties</code> object
     */
    public SvnGetProperties createGetProperties() {
        return new SvnGetProperties(this);
    }

    /**
     * Creates get status operation.
     * @return new <code>SvnGetStatus</code> object
     */
    public SvnGetStatus createGetStatus() {
        return new SvnGetStatus(this);
    }
    
    /**
     * Creates update operation.
     * @return new <code>SvnUpdate</code> object
     */
    public SvnUpdate createUpdate() {
        return new SvnUpdate(this);
    }
    
    /**
     * Creates switch operation.
     * @return new <code>SvnSwitch</code> object
     */
    public SvnSwitch createSwitch() {
        return new SvnSwitch(this);
    }

    /**
     * Creates checkout operation.
     * @return new <code>SvnCheckout</code> object
     */
    public SvnCheckout createCheckout() {
        return new SvnCheckout(this);
    }

    /**
     * Creates relocate operation.
     * @return new <code>SvnRelocate</code> object
     */
    public SvnRelocate createRelocate() {
        return new SvnRelocate(this);
    }

    /**
     * Creates export operation.
     * @return new <code>SvnExport</code> object
     */
    public SvnExport createExport() {
        return new SvnExport(this);
    }
    
    /**
     * Creates add operation.
     * @return new <code>SvnScheduleForAddition</code> object
     */
    public SvnScheduleForAddition createScheduleForAddition() {
        return new SvnScheduleForAddition(this);
    }

    /**
     * Creates commit operation.
     * @return new <code>SvnCommit</code> object
     */
    public SvnCommit createCommit() {
        return new SvnCommit(this);
    }
    
    /**
     * Creates delete operation.
     * @return new <code>SvnScheduleForRemoval</code> object
     */
    public SvnScheduleForRemoval createScheduleForRemoval() {
        return new SvnScheduleForRemoval(this);
    }

    /**
     * Creates mark replaced operation.
     * @return new <code>SvnMarkReplaced</code> object
     */
    public SvnMarkReplaced createMarkReplaced() {
        return new SvnMarkReplaced(this);
    }

    /**
     * Creates revert operation.
     * @return new <code>SvnRevert</code> object
     */
    public SvnRevert createRevert() {
        return new SvnRevert(this);
    }

    /**
     * Creates set property operation.
     * @return new <code>SvnSetProperty</code> object
     */
    public SvnSetProperty createSetProperty() {
        return new SvnSetProperty(this);
    }
    
    /**
     * Creates log operation.
     * @return new <code>SvnLog</code> object
     */
    public SvnLog createLog() {
        return new SvnLog(this);
    }
    
    /**
     * Creates remote make directory operation.
     * @return new <code>SvnRemoteMkDir</code> object
     */
    public SvnRemoteMkDir createMkDir() {
        return new SvnRemoteMkDir(this);
    }
    
    /**
     * Creates remote delete operation.
     * @return new <code>SvnRemoteDelete</code> object
     */
    public SvnRemoteDelete createRemoteDelete() {
        return new SvnRemoteDelete(this);
    }
    
    /**
     * Creates merge operation.
     * @return new <code>SvnMerge</code> object
     */
    public SvnMerge createMerge() {
        return new SvnMerge(this);
    }
    
    /**
     * Creates diff operation.
     * @return new <code>SvnDiff</code> object
     */
    public SvnDiff createDiff() {
        return new SvnDiff(this);
    }
    
    /**
     * Creates diff summarize operation.
     * @return new <code>SvnDiffSummarize</code> object
     */
    public SvnDiffSummarize createDiffSummarize() {
        return new SvnDiffSummarize(this);
    }
    
    /**
     * Creates suggest merge sources operation.
     * @return new <code>SvnSuggestMergeSources</code> object
     */
    public SvnSuggestMergeSources createSuggestMergeSources() {
        return new SvnSuggestMergeSources(this);
    }

    /**
     * Creates get merge info operation.
     * @return new <code>SvnGetMergeInfo</code> object
     */
    public SvnGetMergeInfo createGetMergeInfo() {
        return new SvnGetMergeInfo(this);
    }
    
    /**
     * Creates log merge info operation.
     * @return new <code>SvnLogMergeInfo</code> object
     */
    public SvnLogMergeInfo createLogMergeInfo() {
        return new SvnLogMergeInfo(this);
    }
    
    /**
     * Creates resolve operation.
     * @return new <code>SvnResolve</code> object
     */
    public SvnResolve createResolve() {
        return new SvnResolve(this);
    }
    
    /**
     * Creates cleanup operation.
     * @return new <code>SvnCleanup</code> object
     */
    public SvnCleanup createCleanup() {
        return new SvnCleanup(this);
    }
    
    /**
     * Creates list operation.
     * @return new <code>SvnList</code> object
     */
    public SvnList createList() {
        return new SvnList(this);
    }
    
    /**
     * Creates canonicalize URLs operation.
     * @return new <code>SvnCanonicalizeUrls</code> object
     */
    public SvnCanonicalizeUrls createCanonicalizeUrls() {
        return new SvnCanonicalizeUrls(this);
    }
    
    /**
     * Creates repository dump administrative operation.
     * @return new <code>SvnRepositoryDump</code> object
     */
    public SvnRepositoryDump createRepositoryDump() {
        return new SvnRepositoryDump(this);
    }
    
    /**
     * Creates repository create administrative operation.
     * @return new <code>SvnRepositoryCreate</code> object
     */
    public SvnRepositoryCreate createRepositoryCreate() {
        return new SvnRepositoryCreate(this);
    }
    
    /**
     * Creates repository hot copy administrative operation.
     * @return new <code>SvnRepositoryHotCopy</code> object
     */
    public SvnRepositoryHotCopy createRepositoryHotCopy() {
        return new SvnRepositoryHotCopy(this);
    }
    
    /**
     * Creates repository load administrative operation.
     * @return new <code>SvnRepositoryLoad</code> object
     */
    public SvnRepositoryLoad createRepositoryLoad() {
        return new SvnRepositoryLoad(this);
    }
    
    /**
     * Creates administrative operation for retrieving list of locks from the repository.
     * @return new <code>SvnRepositoryListLocks</code> object
     */
    public SvnRepositoryListLocks createRepositoryListLocks() {
        return new SvnRepositoryListLocks(this);
    }
    
    /**
     * Creates administrative operation for retrieving list of transactions from the repository.
     * @return new <code>SvnRepositoryListTransactions</code> object
     */
    public SvnRepositoryListTransactions createRepositoryListTransactions() {
        return new SvnRepositoryListTransactions(this);
    }
    
    /**
     * Creates repository pack administrative operation.
     * @return new <code>SvnRepositoryPack</code> object
     */
    public SvnRepositoryPack createRepositoryPack() {
        return new SvnRepositoryPack(this);
    }
    
    /**
     * Creates repository recover administrative operation.
     * @return new <code>SvnRepositoryRecover</code> object
     */
    public SvnRepositoryRecover createRepositoryRecover() {
        return new SvnRepositoryRecover(this);
    }
    
    /**
     * Creates repository remove locks administrative operation.
     * @return new <code>SvnRepositoryRemoveLocks</code> object
     */
    public SvnRepositoryRemoveLocks createRepositoryRemoveLocks() {
        return new SvnRepositoryRemoveLocks(this);
    }
    
    /**
     * Creates repository remove transactions administrative operation.
     * @return new <code>SvnRepositoryRemoveTransactions</code> object
     */
    public SvnRepositoryRemoveTransactions createRepositoryRemoveTransactions() {
        return new SvnRepositoryRemoveTransactions(this);
    }
    
    /**
     * Creates repository set UUID administrative operation.
     * @return new <code>SvnRepositorySetUUID</code> object
     */
    public SvnRepositorySetUUID createRepositorySetUUID() {
        return new SvnRepositorySetUUID(this);
    }
    
    /**
     * Creates repository upgrade administrative operation.
     * @return new <code>SvnRepositoryUpgrade</code> object
     */
    public SvnRepositoryUpgrade createRepositoryUpgrade() {
        return new SvnRepositoryUpgrade(this);
    }
    
    /**
     * Creates repository verify administrative operation.
     * @return new <code>SvnRepositoryVerify</code> object
     */
    public SvnRepositoryVerify createRepositoryVerify() {
        return new SvnRepositoryVerify(this);
    }
    
    /**
     * Creates initialize synchronization operation.
     * @return new <code>SvnRepositoryInitialize</code> object
     */
    public SvnRepositoryInitialize createRepositoryInitialize() {
        return new SvnRepositoryInitialize(this);
    }
    
    /**
     * Creates operation for retrieving repository synchronization info.
     * @return new <code>SvnRepositorySyncInfo</code> object
     */
    public SvnRepositorySyncInfo createRepositorySyncInfo() {
        return new SvnRepositorySyncInfo(this);
    }
    
    /**
     * Creates copy revision properties synchronization operation.
     * @return new <code>SvnRepositoryCopyRevisionProperties</code> object
     */
    public SvnRepositoryCopyRevisionProperties createRepositoryCopyRevisionProperties() {
        return new SvnRepositoryCopyRevisionProperties(this);
    }
    
    /**
     * Creates repository synchronize operation.
     * @return new <code>SvnRepositorySynchronize</code> object
     */
    public SvnRepositorySynchronize createRepositorySynchronize() {
        return new SvnRepositorySynchronize(this);
    }
    
    /**
     * Creates dumpfilter operation.
     * @return new <code>SvnRepositoryFilter</code> object
     */
    public SvnRepositoryFilter createRepositoryFilter() {
        return new SvnRepositoryFilter(this);
    }
    
    /**
     * Creates operation for retrieving author from the repository.
     * @return new <code>SvnRepositoryGetAuthor</code> object
     */
    public SvnRepositoryGetAuthor createRepositoryGetAuthor() {
        return new SvnRepositoryGetAuthor(this);
    }
    
    /**
     * Creates operation for retrieving date from the repository.
     * @return new <code>SvnRepositoryGetDate</code> object
     */
    public SvnRepositoryGetDate createRepositoryGetDate() {
        return new SvnRepositoryGetDate(this);
    }
    
    /**
     * Creates operation for retrieving info from the repository.
     * @return new <code>SvnRepositoryGetInfo</code> object
     */
    public SvnRepositoryGetInfo createRepositoryGetInfo() {
        return new SvnRepositoryGetInfo(this);
    }
    
    /**
     * Creates operation for retrieving the lock from the repository.
     * @return new <code>SvnRepositoryGetLock</code> object
     */
    public SvnRepositoryGetLock createRepositoryGetLock() {
        return new SvnRepositoryGetLock(this);
    }
    
    /**
     * Creates operation for retrieving repository log.
     * @return new <code>SvnRepositoryGetLog</code> object
     */
    public SvnRepositoryGetLog createRepositoryGetLog() {
        return new SvnRepositoryGetLog(this);
    }
    
    /**
     * Creates operation for retrieving repository UUID.
     * @return new <code>SvnRepositoryGetUUID</code> object
     */
    public SvnRepositoryGetUUID createRepositoryGetUUID() {
        return new SvnRepositoryGetUUID(this);
    }
    
    /**
     * Creates operation for retrieving the latest revision from the repository.
     * @return new <code>SvnRepositoryGetYoungest</code> object
     */
    public SvnRepositoryGetYoungest createRepositoryGetYoungest() {
        return new SvnRepositoryGetYoungest(this);
    }
    
    /**
     * Creates operation for retrieving property from the repository.
     * @return new <code>SvnRepositoryGetProperty</code> object
     */
    public SvnRepositoryGetProperty createRepositoryGetProperty() {
        return new SvnRepositoryGetProperty(this);
    }
    
    /**
     * Creates operation for retrieving revision property from the repository.
     * @return new <code>SvnRepositoryGetRevisionProperty</code> object
     */
    public SvnRepositoryGetRevisionProperty createRepositoryGetRevisionProperty() {
        return new SvnRepositoryGetRevisionProperty(this);
    }
    
    /**
     * Creates operation for retrieving properties from the repository.
     * @return new <code>SvnRepositoryGetProperties</code> object
     */
    public SvnRepositoryGetProperties createRepositoryGetProperties() {
        return new SvnRepositoryGetProperties(this);
    }
    
    /**
     * Creates operation for retrieving file contents from the repository.
     * @return new <code>SvnRepositoryCat</code> object
     */
    public SvnRepositoryCat createRepositoryGetCat() {
        return new SvnRepositoryCat(this);
    }
    
    /**
     * Creates operation for retrieving changed paths from the repository.
     * @return new <code>SvnRepositoryGetChanged</code> object
     */
    public SvnRepositoryGetChanged createRepositoryGetChanged() {
        return new SvnRepositoryGetChanged(this);
    }
    
    /**
     * Creates operation for retrieving changed directories from the repository.
     * @return new <code>SvnRepositoryGetChangedDirectories</code> object
     */
    public SvnRepositoryGetChangedDirectories createRepositoryGetChangedDirectories() {
        return new SvnRepositoryGetChangedDirectories(this);
    }
    
    /**
     * Creates repository diff operation.
     * @return new <code>SvnRepositoryGetDiff</code> object
     */
    public SvnRepositoryGetDiff createRepositoryGetDiff() {
        return new SvnRepositoryGetDiff(this);
    }
    
    /**
     * Creates operation for retrieving the history from the repository.
     * @return new <code>SvnRepositoryGetHistory</code> object
     */
    public SvnRepositoryGetHistory createRepositoryGetHistory() {
        return new SvnRepositoryGetHistory(this);
    }
    
    /**
     * Creates operation for retrieving items tree from the repository.
     * @return new <code>SvnRepositoryGetTree</code> object
     */
    public SvnRepositoryGetTree createRepositoryGetTree() {
        return new SvnRepositoryGetTree(this);
    }
    
    /**
     * Creates operation for retrieving revision properties from the repository.
     * @return new <code>SvnRepositoryGetRevisionProperties</code> object
     */
    public SvnRepositoryGetRevisionProperties createRepositoryGetRevisionProperties() {
        return new SvnRepositoryGetRevisionProperties(this);
    }

    /**
     * Creates get status summary operation.
     * @return new <code>SvnStatusSummary</code> object
     */
    public SvnGetStatusSummary createGetStatusSummary() {
        return new SvnGetStatusSummary(this);
    }

    /**
     * Sets whether to dispose repository pool on {@link #dispose()} call.
     * This flag has sense only if <code>repositoryPool</code> field is not <code>null</code>.
     * Otherwise the flag value can be overwritten by {@link #setRepositoryPool(ISVNRepositoryPool)} or
     * {@link #getRepositoryPool()} calls.
     * @param dispose whether to dispose repository pool on {@link #dispose()} call
     */
    public void setAutoDisposeRepositoryPool(boolean dispose) {
        autoDisposeRepositoryPool = dispose;
    }

    protected Object run(SvnOperation<?> operation) throws SVNException {
        ISvnOperationRunner<?, SvnOperation<?>> runner = getImplementation(operation);
        if (runner != null) {
            SVNWCContext wcContext = null;
            runLevel++;
            try {
                wcContext = obtainWcContext(operation);
                if (runLevel == 1 && wcContext != null) {
                    wcContext.setSqliteJournalMode(operation.getSqliteJournalMode());
                }
                runner.setWcContext(wcContext);
                getOperationHandler().beforeOperation(operation);
                Object result = runner.run(operation);
                getOperationHandler().afterOperationSuccess(operation);

                assertRefCount(operation, wcContext);
                return result;
            } catch (SVNException e) {
                getOperationHandler().afterOperationFailure(operation);
                throw e;
            } finally {
                runLevel--;
                if (runLevel == 0) {
                    if (wcContext != null) {
                        wcContext.setSqliteJournalMode(null);
                    }
                    releaseWcContext(wcContext);
                }
            }
        }
        return null;
    }

    private void releaseWcContext(SVNWCContext wcContext) throws SVNException {
        // check is wcContext is empty of unfinished transactions
        if (wcContext != null) {
            wcContext.ensureNoUnfinishedTransactions();
        }
        if (isAutoCloseContext() && wcContext != null) {
            if (this.wcContext == wcContext) {
                disposeWcContext();
            } else {
                wcContext.close();
            }
        }
    }

    private SVNWCContext obtainWcContext(SvnOperation<?> operation) {
        if (wcContext == null) {
            wcContext = new SVNWCContext(getOptions(), getEventHandler());
        }
        return wcContext;
    }

    private void disposeWcContext() {
        if (wcContext != null) {
            wcContext.close();
            wcContext = null;
        }
    }
    
    private boolean isAutoDisposeRepositoryPool() {
        return autoDisposeRepositoryPool;
    }

    protected ISvnOperationRunner<?, SvnOperation<?>> getImplementation(SvnOperation<?> operation) throws SVNException {
        if (operation == null) {
            return null;
        }

        final boolean isAdditionMode = operation.getClass() == SvnScheduleForAddition.class;

        SvnWcGeneration wcGeneration = SvnWcGeneration.NOT_DETECTED;
        if (operation.getOperationalWorkingCopy() != null) {
            if (operation.getClass() == SvnCheckout.class) {
                if (SVNWCUtil.isVersionedDirectory(operation.getOperationalWorkingCopy())) {
                    wcGeneration = detectWcGeneration(operation.getOperationalWorkingCopy(), false, isAdditionMode);
                } else {
                    wcGeneration = getPrimaryWcGeneration();
                }
            } else {
                wcGeneration = detectWcGeneration(operation.getOperationalWorkingCopy(), false, isAdditionMode);
            }
        }
        
        if (wcGeneration == SvnWcGeneration.V16 && isPrimaryWcGenerationOnly() && operation.getClass() != SvnUpgrade.class) {
            File wcPath = operation.getOperationalWorkingCopy();
            int format = SVNAdminAreaFactory.checkWC(wcPath, true);
            if (format > 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_UPGRADE_REQUIRED, "Working copy ''{0}'' is too old (format {1}, created  by Subversion 1.6)",
                        new Object[] {wcPath, new Integer(format)});
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        final List<ISvnOperationRunner<?, SvnOperation<?>>> candidateRunners = new LinkedList<ISvnOperationRunner<?, SvnOperation<?>>>();
        
        candidateRunners.addAll(getRunners(operation.getClass(), anyFormatOperationRunners));
        if (wcGeneration == SvnWcGeneration.NOT_DETECTED) {
            candidateRunners.addAll(getRunners(operation.getClass(), noneOperationRunners));
        } else if (wcGeneration == SvnWcGeneration.V16) {
            candidateRunners.addAll(getRunners(operation.getClass(), v16OperationRunners));
        } else if (wcGeneration == SvnWcGeneration.V17) {
            candidateRunners.addAll(getRunners(operation.getClass(), v17OperationRunners));
        }
        
        ISvnOperationRunner<?, SvnOperation<?>> runner = null;
        
        for (ISvnOperationRunner<?, SvnOperation<?>> candidateRunner : candidateRunners) {
            boolean isApplicable = candidateRunner.isApplicable(operation, wcGeneration);
            if (!isApplicable) {
                continue;
            }
            runner = candidateRunner;
            break;
        }
        if (runner != null) {
            runner.reset(wcGeneration);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Runner for ''{0}'' command have not been found; probably not yet implement in this API.",
                    operation.getClass().getName());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return runner;
    }
    
    /**
     * Returns whether the operations should work only on primary working copy generation
     * (for example only on SVN 1.7 working copy) or on both primary and secondary generations.
     *
     * @return <code>true</code> operations should work only on primary working copy generation,
     * if <code>false</code> both primary and secondary generations are supported
     */
    public boolean isPrimaryWcGenerationOnly() {
        return "true".equalsIgnoreCase(System.getProperty("svnkit.wc.17only", null));
    }

    public boolean isAssertRefCount() {
        return "true".equalsIgnoreCase(System.getProperty("svnkit.wc.assertRefCount", null));
    }

    @SuppressWarnings("unchecked")
    protected void registerOperationRunner(Class<?> operationClass, ISvnOperationRunner<?, ? extends SvnOperation<?>> runner) {
        if (operationClass == null || runner == null) {
            return;
        }
        Collection<Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>> maps = new ArrayList<Map<Class<?>,List<ISvnOperationRunner<?, SvnOperation<?>>>>>();
        SvnWcGeneration[] scope = getRunnerScope(runner);
        if (scope == null || scope.length == 0) {
            maps.add(anyFormatOperationRunners);
        } else {
            Set<SvnWcGeneration> formatsSet = new HashSet<SvnWcGeneration>();
            formatsSet.addAll(Arrays.asList(scope));
            if (formatsSet.contains(SvnWcGeneration.NOT_DETECTED)) {
                maps.add(noneOperationRunners);
            }
            if (formatsSet.contains(SvnWcGeneration.V17)) {
                maps.add(v17OperationRunners);
            }
            if (formatsSet.contains(SvnWcGeneration.V16)) {
                maps.add(v16OperationRunners);
            }
        }
        for (Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> runnerMap : maps) {
            List<ISvnOperationRunner<?, SvnOperation<?>>> runners = runnerMap.get(operationClass);
            if (runners == null) {
                runners = new LinkedList<ISvnOperationRunner<?, SvnOperation<?>>>();
                runnerMap.put(operationClass, runners);
            }
            runners.add((ISvnOperationRunner<?, SvnOperation<?>>) runner);
        }
    }
    
    /**
     * Detects whether the versioned directory is working copy root.
     *
     * @param versionedDir directory to check
     * @return <code>true</code> if the directory is working copy root, otherwise <code>false</code>
     */
    public static boolean isWorkingCopyRoot(File versionedDir) {
        SVNWCDb db = new SVNWCDb();
        try {
            db.open(SVNWCDbOpenMode.ReadOnly, null, false, false);
            if (db.isWCRoot(versionedDir.getAbsoluteFile())) {
                return true;
            }
        } catch(SVNException e) {
        } finally {
            db.close();
        }
        SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
        try {
            wcAccess.open(versionedDir, false, false, false, 0, Level.FINEST);
            return wcAccess.isWCRoot(versionedDir);
        } catch (SVNException e) {
        } finally {
            try {
                wcAccess.close();
            } catch (SVNException e) {}
        }
        return false;
    }
    
    /**
     * Detects whether the directory is versioned directory.
     *
     * @param directory directory to check
     * @return <code>true</code> if the directory is versioned directory, otherwise <code>false</code>
     */
    public static boolean isVersionedDirectory(File directory) {
        return isVersionedDirectory(directory, false);
    }

    /**
     * Detects whether the directory is versioned directory in or (not in) the addition mode.
     *
     * @param directory directory to check
     * @param isAdditionMode <code>true</code> if it is addition mode, otherwise <code>false</code>
     * @return <code>true</code> if the directory is versioned directory, otherwise <code>false</code>
     */
    public static boolean isVersionedDirectory(File directory, boolean isAdditionMode) {
        if (directory == null) {
            return false;
        }
        final File localAbsPath = directory.getAbsoluteFile();
        if (localAbsPath.isFile()) {
            // obstruction.
            return false;
        }
        SVNWCDb db = new SVNWCDb();
        db.open(SVNWCDbOpenMode.ReadOnly, null, false, false);
        try {
            DirParsedInfo info = db.parseDir(localAbsPath, Mode.ReadOnly, true, isAdditionMode);
            if (info != null
                    && info.wcDbDir != null
                    && SVNWCDbDir.isUsable(info.wcDbDir)) {
                WCDbInfo nodeInfo = db.readInfo(localAbsPath, InfoField.status, InfoField.kind);
                if (nodeInfo != null) {
                    if (nodeInfo.kind != SVNWCDbKind.Dir) {
                        // obstruction
                        return false;
                    } else if (!(nodeInfo.status == SVNWCDbStatus.Excluded || nodeInfo.status == SVNWCDbStatus.ServerExcluded || nodeInfo.status == SVNWCDbStatus.NotPresent)) {
                        return true;
                    }
                }
            }
        } catch (SVNException e1) {
        } finally {
            db.close();
        }
        
        File adminDirectory = new File(directory, SVNFileUtil.getAdminDirectoryName());
        if (adminDirectory.isDirectory()) {
            SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
            try {
                wcAccess.open(directory, false, false, false, 0, Level.FINEST);
                return true;
            } catch (SVNException e) {
            } finally {
                try {
                    wcAccess.close();
                } catch (SVNException e) {
                    //
                }
            }
            
        }
        return false;
    }

    /**
     * Searches working copy root path by the versioned directory.
     *
     * @param versionedDir versioned directory
     * @param stopOnExternals <code>true</code> if externals should not be searched, otherwise <code>false</code>
     * @return working copy root
     * @throws SVNException
     */
    public static File getWorkingCopyRoot(File versionedDir, boolean stopOnExternals) throws SVNException {
        if (versionedDir == null || (!isVersionedDirectory(versionedDir) && !isVersionedDirectory(versionedDir.getParentFile()))) {
            return null;
        }
        versionedDir = versionedDir.getAbsoluteFile();
        SvnWcGeneration wcGeneration = SvnOperationFactory.detectWcGeneration(versionedDir, false);
        if (wcGeneration == SvnWcGeneration.NOT_DETECTED) {
            return null;
        } else if (wcGeneration == SvnWcGeneration.V17) {
            return getWorkingCopyRootNg(versionedDir, stopOnExternals);
        } else if (wcGeneration == SvnWcGeneration.V16) {
            return getWorkingCopyRootOld(versionedDir, stopOnExternals);
        }
        return null;
    }
    
    private static File getWorkingCopyRootOld(File versionedDir, boolean stopOnExternals) throws SVNException {
        if (versionedDir == null || (!isVersionedDirectory(versionedDir) && !isVersionedDirectory(versionedDir.getParentFile()))) {
            // both this dir and its parent are not versioned.
            return null;
        }
        File parent = versionedDir.getParentFile();
        if (parent == null) {
            return versionedDir;
        }

        if (isWorkingCopyRoot(versionedDir)) {
            // this is root.
            if (stopOnExternals) {
                return versionedDir;
            }
            File parentRoot = getWorkingCopyRoot(parent, stopOnExternals);
            if (parentRoot == null) {
                // if parent is not versioned return this dir.
                return versionedDir;
            }
            // parent is versioned. we have to check if it contains externals
            // definition for this dir.

            while (parent != null) {
                SVNWCAccess parentAccess = SVNWCAccess.newInstance(null);
                try {
                    SVNAdminArea dir = parentAccess.open(parent, false, 0);
                    SVNVersionedProperties props = dir.getProperties(dir.getThisDirName());
                    final String externalsProperty = props.getStringPropertyValue(SVNProperty.EXTERNALS);
                    SVNExternal[] externals = externalsProperty != null ? SVNExternal.parseExternals(dir.getRoot().getAbsolutePath(), externalsProperty) : new SVNExternal[0];
                    // now externals could point to our dir.
                    for (int i = 0; i < externals.length; i++) {
                        SVNExternal external = externals[i];
                        File externalFile = new File(parent, external.getPath());
                        if (externalFile.equals(versionedDir)) {
                            return parentRoot;
                        }
                    }
                } catch (SVNException e) {
                    if (e instanceof SVNCancelException) {
                        throw e;
                    }
                } finally {
                    parentAccess.close();
                }
                if (parent.equals(parentRoot)) {
                    break;
                }
                parent = parent.getParentFile();
            }
            return versionedDir;
        }

        return getWorkingCopyRootOld(parent, stopOnExternals);
    }

    private static File getWorkingCopyRootNg(File versionedDir, boolean stopOnExternals) throws SVNException {
        SVNWCDb db = new SVNWCDb();
        try {
            db.open(SVNWCDbOpenMode.ReadOnly, null, false, false);
            File wcRoot = db.getWCRoot(versionedDir);
            if (wcRoot == null) {
                return null;
            }
            if (stopOnExternals || wcRoot.getParentFile() == null) {
                return wcRoot;
            }
            // check if our root is external in the parent wc.
            try {
                File parentWcRoot = db.getWCRoot(wcRoot.getParentFile());
                SVNExternalsStore storage = new SVNExternalsStore();
                db.gatherExternalDefinitions(parentWcRoot, storage);
                for(File defPath : storage.getNewExternals().keySet()) {
                    String externalDefinition = storage.getNewExternals().get(defPath);
                    SVNExternal[] externals = SVNExternal.parseExternals(defPath, externalDefinition);
                    for (int i = 0; i < externals.length; i++) {
                        File targetAbsPath = SVNFileUtil.createFilePath(defPath, externals[i].getPath());
                        if (targetAbsPath.equals(wcRoot)) {
                            return getWorkingCopyRootNg(parentWcRoot, stopOnExternals);
                        }
                    }
                }
                return wcRoot;
            } catch (SVNException e) {
                return wcRoot;
            }
        } finally {
            db.close();
        }
    }

    /**
     *
     * Detects working copy generation (1.6 or 1.7 format) by the working copy path.
     * Recursively searches the by path's parents up to the root if <code>climbUp</code> is <code>true</code>.
     *
     * @param path working copy path
     * @param climbUp <code>true</code> if search recursively in path's parents, otherwise <code>false</code>
     * @return working copy generation
     * @throws SVNException
     */
    public static SvnWcGeneration detectWcGeneration(File path, boolean climbUp) throws SVNException {
        return detectWcGeneration(path, climbUp, false);
    }

    /**
     *
     * Detects working copy generation (1.6 or 1.7 format) by the working copy path in (not in) the addition mode.
     * Recursively searches the by path's parents up to the root if <code>climbUp</code> is <code>true</code>.
     *
     * @param path working copy path
     * @param climbUp <code>true</code> if search recursively in path's parents, otherwise <code>false</code>
     * @param isAdditionMode <code>true</code> if it is addition mode, otherwise <code>false</code>
     * @return working copy generation
     * @throws SVNException
     */
    public static SvnWcGeneration detectWcGeneration(File path, boolean climbUp, boolean isAdditionMode) throws SVNException {
        while(true) {
            if (path == null) {
                return SvnWcGeneration.NOT_DETECTED;
            }
            SVNWCDb db = new SVNWCDb();
            try {
                db.open(SVNWCDbOpenMode.ReadOnly, (ISVNOptions) null, true, false);
                DirParsedInfo info = db.parseDir(path, Mode.ReadOnly, true, isAdditionMode);
                if (info != null && SVNWCDbDir.isUsable(info.wcDbDir)) {
                    return SvnWcGeneration.V17;
                } else if (info != null
                        && info.wcDbDir != null
                        && info.wcDbDir.getWCRoot() != null
                        && info.wcDbDir.getWCRoot().getSDb() == null) {
                    return SvnWcGeneration.V16;
                }
                return SvnWcGeneration.NOT_DETECTED;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY) {
                    path = SVNFileUtil.getParentFile(path);
                    if (!climbUp) {
                        SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
                        try {
                            SVNAdminArea area = wcAccess.open(path, false, 0);
                            if (area != null) {
                                return SvnWcGeneration.V16;
                            }
                        } catch (SVNException inner) {
                            //
                        } finally {
                            wcAccess.close();
                        }
                        return SvnWcGeneration.NOT_DETECTED;
                    }
                    continue;
                } else if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_UNSUPPORTED_FORMAT) {
                    // there should be an exception for an 'add' and 'checkout' operations.
                    SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
                    if (SVNFileType.getType(path) != SVNFileType.DIRECTORY) {
                        path = SVNFileUtil.getParentFile(path);
                        if (path == null || SVNFileType.getType(path) != SVNFileType.DIRECTORY) {
                            if (climbUp) {
                                continue;
                            }
                            return SvnWcGeneration.NOT_DETECTED;
                        }
                    }
                    try {
                        SVNAdminArea area = wcAccess.open(path, false, 0);
                        if (area != null) {
                            return SvnWcGeneration.V16;
                        }
                    } catch (SVNException inner) {
                        if (climbUp && inner.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_DIRECTORY) {
                            path = SVNFileUtil.getParentFile(path);
                            continue;
                        }
                    } finally {
                        wcAccess.close();
                    }
                    return SvnWcGeneration.NOT_DETECTED;
                    
                } else {
                    throw e;
                }
            } finally {
                db.close();
            }
        }
    }
    
    private static List<ISvnOperationRunner<?, SvnOperation<?>>> getRunners(Class<?> clazz, Map<Class<?>, List<ISvnOperationRunner<?,SvnOperation<?>>>> map) {
        List<ISvnOperationRunner<?, SvnOperation<?>>> list = map.get(clazz);
        if (list == null) {
            list = Collections.emptyList();
        }
        return list;
    }

    /**
     * Returns working copy context.
     *
     * @return working copy context
     */
    public SVNWCContext getWcContext() {
        return wcContext;
    }

    /**
     * Returns primary (default) working copy generation.
     *
     * @return working copy generation
     */
    public SvnWcGeneration getPrimaryWcGeneration() {
        if (primaryWcGeneration == null) {
            String systemProperty = System.getProperty("svnkit.wc.17", "true");
            if (Boolean.toString(true).equalsIgnoreCase(systemProperty)) {
                primaryWcGeneration = SvnWcGeneration.V17;
            } else {
                primaryWcGeneration = SvnWcGeneration.V16;
            }
        }
        return primaryWcGeneration;
    }
    
    /**
     * Returns secondary working copy generation.
     *
     * @return working copy generation
     */
    public SvnWcGeneration getSecondaryWcGeneration() {
        return getPrimaryWcGeneration() == SvnWcGeneration.V17 ? SvnWcGeneration.V16 : SvnWcGeneration.V17;
    }

    /**
     * (Re)sets primary (default) working copy generation.
     * If <code>primaryWcGeneration</code> is not <code>null</code>,
     * registers operations' runners.
     *
     * @param primaryWcGeneration
     */
    public void setPrimaryWcGeneration(SvnWcGeneration primaryWcGeneration) {
        if (primaryWcGeneration == null) {
            return;
        }
        this.primaryWcGeneration = primaryWcGeneration;
        registerRunners();
    }
    
    private SvnWcGeneration[] getRunnerScope(ISvnOperationRunner<?, ? extends SvnOperation<?>> runner) {
        if (runner.getWcGeneration() == getPrimaryWcGeneration()) {
            return new SvnWcGeneration[] { getPrimaryWcGeneration(), SvnWcGeneration.NOT_DETECTED};
        } else if (runner.getWcGeneration() == getSecondaryWcGeneration()) {
            return new SvnWcGeneration[] { getSecondaryWcGeneration() };
        } else {
            // any.
            return new SvnWcGeneration[] { };
        }
    }

    SvnCommitPacket collectCommitItems(SvnCommit operation) throws SVNException {
        ISvnOperationRunner<?, SvnOperation<?>> runner = getImplementation(operation);
        if (runner instanceof ISvnCommitRunner) {
            SVNWCContext wcContext = null;
            runLevel++;
            try {
                wcContext = obtainWcContext(operation);
                runner.setWcContext(wcContext);
                return ((ISvnCommitRunner) runner).collectCommitItems(operation);
            } finally {
                // do not release context, it keeps locks.
                runLevel--;
            }
        }
        return new SvnCommitPacket();
    }

    private void assertRefCount(SvnOperation<?> operation, SVNWCContext wcContext) throws SVNException {
        if (!isAssertRefCount()) {
            return;
        }

        ISVNWCDb wcdb = wcContext.getDb();
        if (operation.isChangesWorkingCopy() && wcdb instanceof SVNWCDb) {
            File operationalWorkingCopy = operation.getOperationalWorkingCopy();
            if (operationalWorkingCopy != null) {
                DirParsedInfo dirParsedInfo = ((SVNWCDb) wcdb).parseDir(operationalWorkingCopy, Mode.ReadOnly);
                if (SVNWCDbDir.isUsable(dirParsedInfo.wcDbDir)) {
                    SVNWCDbRoot root = dirParsedInfo.wcDbDir.getWCRoot();
                    SvnWcDbPristines.checkPristineChecksumRefcounts(root);
                }
            }
        }
    }
    
}
