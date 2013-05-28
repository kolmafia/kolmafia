package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.sqljet.core.internal.SqlJetPagerJournalMode;
import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Base class for all Svn* operations. 
 * Encapsulates mostly used parameters, operation state, different methods used by implementations.
 * 
 * <p/>
 * Those parameters includes:
 * <ul>
 * <li>operation's target(s)</li>
 * <li>operation's revision</li>
 * <li>operation's depth</li>
 * <li>operation's changelists</li>
 * <li>whether to sleep after operation fails</li>
 * </ul>
 * 
 * <p/>
 * Those methods are:
 * <ul>
 * <li>base implementation of <code>run</code> method, starts the operation execution</li>
 * <li>methods for access to the factory that created the object and its options, event handler, canceler</li>
 * <li>variety of methods for getting, setting, recognition operation's targets</li>
 * <li>cancel the operation</li>
 * <li>access to the authentication manager</li>
 * </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @param <V> type of returning value in {@link #run()} method
 */
public class SvnOperation<V> implements ISvnOperationOptionsProvider {
    
    private SVNDepth depth;
    private Collection<SvnTarget> targets;
    private SVNRevision revision;
    private Collection<String> changelists;
    private SvnOperationFactory operationFactory;
    private boolean isSleepForTimestamp;
    
    private SqlJetPagerJournalMode sqliteJournalMode;
    
    private volatile boolean isCancelled;
    
    protected SvnOperation(SvnOperationFactory factory) {
        this.operationFactory = factory;
        initDefaults();
    }

    /**
     * Gets the event handler for the operation, provided by {@link SvnOperationFactory#getEventHandler()}. This event handler will be
     * dispatched {@link SVNEvent} objects to provide detailed information about
     * actions and progress state of version control operations performed by
     * <code>run()</code> method of <code>SVN*</code> operation classes. 
     * 
     * @return handler for events
     * @see ISVNEventHandler
     */
    public ISVNEventHandler getEventHandler() {
        return getOperationFactory().getEventHandler();
    }

    /**
     * Gets operation's options, provided by {@link SvnOperationFactory#getOptions()}.
     * 
     * @return options of the operation
     */
    public ISVNOptions getOptions() {
        return getOperationFactory().getOptions();
    }
    
    protected void initDefaults() {
        setDepth(SVNDepth.UNKNOWN);
        setSleepForTimestamp(true);
        setRevision(SVNRevision.UNDEFINED);
        this.targets = new ArrayList<SvnTarget>();
    }
    
    /**
     * Sets one target of the operation.
     *  
     * @param target target of the operation
     * @see SvnTarget
     */
    public void setSingleTarget(SvnTarget target) {
        this.targets = new ArrayList<SvnTarget>();
        if (target != null) {
            this.targets.add(target);
        }
    }

    /**
     * Adds one target to the operation's targets.
     * 
     * @param target target of the operation
     * @see SvnTarget
     */
    public void addTarget(SvnTarget target) {
        this.targets.add(target);
    }
    
    /**
     * Returns all targets of the operation.
     * 
     * @return targets of the operation
     * @see SvnTarget
     */
    public Collection<SvnTarget> getTargets() {
        return Collections.unmodifiableCollection(targets);
    }
    
    /**
     * Returns first target of the operation.
     * 
     * @return first target of the operation
     * @see SvnTarget
     */
    public SvnTarget getFirstTarget() {
        return targets != null && !targets.isEmpty() ? targets.iterator().next() : null;
    }
    
    /**
     * Sets the limit of the operation by depth.
     * 
     * @param depth depth of the operation
     */
    public void setDepth(SVNDepth depth) {
        this.depth = depth;
    }
    
    /**
     * Gets the limit of the operation by depth.
     * 
     * @return depth of the operation
     */
    public SVNDepth getDepth() {
        return depth;
    }
    
    /**
     * Sets revision of the operation. 
     * In most cases if revision equals {@link SVNRevision#UNDEFINED}, the operation's revision will be {@link SVNRevision#WORKING}
     * if target(s) are local; it will be will be {@link SVNRevision#HEAD} it targets are remote.
     * 
     * @param revision revision of the operation
     */
    public void setRevision(SVNRevision revision) {
        this.revision = revision;
    }

    /**
     * Gets revision to operate on.
     * 
     * @return revision of the operation
     * @see #setRevision(SVNRevision)
     */
    public SVNRevision getRevision() {
        return revision;
    }
    
    /**
     * Sets changelists to operate only on members of.
     * 
     * @param changelists changelists of the operation
     */
    public void setApplicalbeChangelists(Collection<String> changelists) {
        this.changelists = changelists;
    }
    
    /**
     * Gets changelists to operate only on members of.
     * 
     * @return changelists of the operation
     */
    public Collection<String> getApplicableChangelists() {
        if (this.changelists == null || this.changelists.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableCollection(this.changelists);
    }
    
    /**
     * Gets the factory that created the operation.
     * 
     * @return creation factory of the operations
     */
    public SvnOperationFactory getOperationFactory() {
        return this.operationFactory;
    }
    
    /**
     * Gets whether or not the operation has local targets.
     * 
     * @return <code>true</code> if the operation has local targets, otherwise <code>false</code>
     */
    public boolean hasLocalTargets() {
        for (SvnTarget target : getTargets()) {
            if (target.isLocal()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets whether or not the operation has remote targets.
     * 
     * @return <code>true</code> if the operation has remote targets, otherwise <code>false</code>
     */
    public boolean hasRemoteTargets() {
        for (SvnTarget target : getTargets()) {
            if (!target.isLocal()) {
                return true;
            }
        }
        return false;
    }
    
    protected void ensureEnoughTargets() throws SVNException {
        int targetsCount = getTargets().size();
        
        if (targetsCount < getMinimumTargetsCount()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "Wrong number of targets has been specified ({0}), at least {1} is required.",
                    new Object[] {new Integer(targetsCount), new Integer(getMinimumTargetsCount())},
                    SVNErrorMessage.TYPE_ERROR);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (targetsCount > getMaximumTargetsCount()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "Wrong number of targets has been specified ({0}), no more that {1} may be specified.",
                    new Object[] {new Integer(targetsCount),
                    new Integer(getMaximumTargetsCount())},
                    SVNErrorMessage.TYPE_ERROR);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    protected int getMinimumTargetsCount() {
        return 1;
    }

    protected int getMaximumTargetsCount() {
        return 1;
    }
    
    /**
     * Cancels the operation. Execution of operation will be stopped at the next point of checking <code>isCancelled</code> state.
     * If canceler is set, {@link ISVNCanceller#checkCancelled()} is called, 
     * otherwise {@link org.tmatesoft.svn.core.SVNCancelException} is raised at the point of checking <code>isCancelled</code> state.
     */
    public void cancel() {
        isCancelled = true;
    }
    
    /**
     * Gets whether or not the operation is cancelled.
     * 
     * @return <code>true</code> if the operation is cancelled, otherwise <code>false</code>
     */
    public boolean isCancelled() {
        return isCancelled;
    }
    
    /**
     * Executes the operation.
     * 
     * @return result depending on operation type
     * @throws SVNException
     */
    @SuppressWarnings("unchecked")
    public V run() throws SVNException {
        ensureArgumentsAreValid();
        return (V) getOperationFactory().run(this);
    }
    
    protected void ensureArgumentsAreValid() throws SVNException {
        ensureEnoughTargets();
        ensureHomohenousTargets();
    }
    
    protected boolean needsHomohenousTargets() {
        return true;
    }
    
    protected void ensureHomohenousTargets() throws SVNException {
        if (getTargets().size() <= 1) {
            return;
        }
        if (!needsHomohenousTargets()) {
            return;
        }
        if (hasLocalTargets() && hasRemoteTargets()) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot mix repository and working copy targets"), SVNLogType.WC);
        }
    }

    /**
     * Gets the operation's pool of repositories, provided by {@link SvnOperationFactory#getRepositoryPool()}.
     * 
     * @return pool of repositories
     */
    public ISVNRepositoryPool getRepositoryPool() {
        return getOperationFactory().getRepositoryPool();
    }

    /**
     * Gets operation's authentication manager, provided by {@link SvnOperationFactory#getAuthenticationManager()}.
     * 
     * @return authentication manager
     */
    public ISVNAuthenticationManager getAuthenticationManager() {
        return getOperationFactory().getAuthenticationManager();
    }

    /**
     * Gets the cancel handler of the operation, provided by {@link SvnOperationFactory#getCanceller() }.
     * 
     * @return cancel handler
     * @see #cancel()
     */
    public ISVNCanceller getCanceller() {
        return getOperationFactory().getCanceller();
    }

    /**
     * Gets whether or not the operation should sleep after if fails.
     * 
     * @return <code>true</code> if the operation should sleep, otherwise <code>false</code>
     * @see SvnUpdate
     * @since 1.7
     */
    public boolean isSleepForTimestamp() {
        return isSleepForTimestamp;
    }

    /**
     * Sets whether or not the operation should sleep after if fails.
     * 
     * @param isSleepForTimestamp <code>true</code> if the operation should sleep, otherwise <code>false</code>
     * @see SvnUpdate
     * @since 1.7
     */
    public void setSleepForTimestamp(boolean isSleepForTimestamp) {
        this.isSleepForTimestamp = isSleepForTimestamp;
    }

    /**
     * Analyzes the targets and returns whether or not operation has at least one file in targets.
     * 
     * @return <code>true</code> if operation has at least one file in targets, otherwise <code>false</code>
     */
    public boolean hasFileTargets() {
        for (SvnTarget target : getTargets()) {
            if (target.isFile()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets whether or not to use parent working copy format.
     * 
     * @return <code>true</code> if parent working copy format should be used, otherwise <code>false</code>
     */
    public boolean isUseParentWcFormat() {
        return false;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    public boolean isChangesWorkingCopy() {
        return true;
    }
    
    public SqlJetPagerJournalMode getSqliteJournalMode() {
        return sqliteJournalMode;
    }
    
    public void setSqliteJournalMode(SqlJetPagerJournalMode journalMode) {
        sqliteJournalMode = journalMode; 
    }

    protected File getOperationalWorkingCopy() {
        if (hasFileTargets()) {
            return getFirstTarget().getFile();
        }
        return null;
    }
}
