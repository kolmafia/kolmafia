package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc2.ISvnCommitRunner;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.SVNCommitItem;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;

/**
 * Represents storage for <code>SvnCommitItem</code>
 * objects which represent information on versioned items intended
 * for being committed to a repository.
 *
 * <p>
 * Used by commit-related operations to collect and hold information on paths that are to be committed.
 * Each <code>SvnCommitPacket</code> is committed in a single transaction.
 *
 * @author  TMate Software Ltd.
 * @see     SvnCommitItem
 */
public class SvnCommitPacket {
    
    private Map<SVNURL, Collection<SvnCommitItem>> items;
    private Map<String, SvnCommitItem> itemsByPath;
    private Object lockingContext;
    private ISvnCommitRunner runner;
    private Map<SVNURL, String> lockTokens;
    private Set<String> skippedPaths;
    private AtomicInteger sharedIndex;
    
    /**
     * Creates a commit packet and initializes its fields with empty lists. 
     */
    public SvnCommitPacket() {
        items = new HashMap<SVNURL, Collection<SvnCommitItem>>();
        itemsByPath = new HashMap<String, SvnCommitItem>();
        lockTokens = new HashMap<SVNURL, String>();
        skippedPaths = new HashSet<String>();
    }

    private SvnCommitPacket(Map<SVNURL, Collection<SvnCommitItem>> items, Map<String, SvnCommitItem> itemsByPath, Object lockingContext, Map<SVNURL, String> lockTokens, ISvnCommitRunner runner, Set<String> skippedPaths) {
        this.items = items;
        this.itemsByPath = itemsByPath;
        this.lockingContext = lockingContext;
        this.lockTokens = lockTokens;
        this.runner = runner;
        this.skippedPaths = skippedPaths;
    }

    /**
     * Tests if the commit packet contains the commit item with the path
     * @param path the path of the commit item to test
     * @return <code>true</code> if commit item with the path is contained in the commit packet, otherwise <code>false</code>
     */
    public boolean hasItem(File path) {
        return itemsByPath.containsKey(SVNFileUtil.getFilePath(path));
    }

    /**
     * Returns the commit item with the path
     * @param path the path of the commit item
     * @return commit item 
     */
    public SvnCommitItem getItem(File path) {
        return itemsByPath.get(SVNFileUtil.getFilePath(path));
    }
    
    /**
     * Returns all unique repository root URLs of all commit items in the commit packet
     * @return unmodifiable list of URLs of the commit packet 
     */
    public Collection<SVNURL> getRepositoryRoots() {
        return Collections.unmodifiableCollection(items.keySet());
    }

    /**
     * Returns all commit items in the commit packet with the corresponding repository root URL 
     * @return unmodifiable list of commit items containing info of versioned items to be committed
     */
    public Collection<SvnCommitItem> getItems(SVNURL url) {
        return Collections.unmodifiableCollection(items.get(url));
    }

    /**
     * Adds commit item to the commit packet with the repository root URL.
     * @param item commit item
     * @param repositoryRoot repository root URL
     */
    public void addItem(SvnCommitItem item, SVNURL repositoryRoot) {
        if (!items.containsKey(repositoryRoot)) {
            items.put(repositoryRoot, new HashSet<SvnCommitItem>());
        }

        items.get(repositoryRoot).add(item);
        itemsByPath.put(item.getPath() != null ? SVNFileUtil.getFilePath(item.getPath()) : null, item);
    }
    
    /**
     * Adds commit item with the path, kind, repository root URL, repository path, revision number, 
     * copied from path, copied from revision number, flags to the commit packet. 
     * 
     * @param path path of the commit item
     * @param kind node kind of the commit item
     * @param repositoryRoot repository root URL of the commit item
     * @param repositoryPath repository path of the commit item 
     * @param revision revision number of the commit item
     * @param copyFromPath path from those commit item was copied
     * @param copyFromRevision revision of the repository item from those commit item was copied
     * @param flags commit item flags
     * @return newly created commit item with initialized fields 
     * @throws SVNException if URL parse error occurred
     */
    public SvnCommitItem addItem(File path, SVNNodeKind kind, SVNURL repositoryRoot, String repositoryPath, long revision,
            String copyFromPath, long copyFromRevision, int flags) throws SVNException {
        SvnCommitItem item = new SvnCommitItem();
        item.setPath(path);
        item.setKind(kind);
        item.setUrl(repositoryRoot.appendPath(repositoryPath, false));
        item.setRevision(revision);
        if (copyFromPath != null) {
            item.setCopyFromUrl(repositoryRoot.appendPath(copyFromPath, false));
            item.setCopyFromRevision(copyFromRevision);
        } else {
            item.setCopyFromRevision(-1);
        }
        item.setFlags(flags);

        addItem(item, repositoryRoot);
        
        return item;
    }
    
    /**
     * Adds commit item with the path, repository root URL, kind, URL, revision number, 
     * revision number, copied from path, copied from revision number, flags to the commit packet. 
     * 
     * @param path path of the commit item
     * @param rootUrl repository root URL of the commit item
     * @param kind node kind of the commit item
     * @param url repository URL of the commit item
     * @param revision revision number of the commit item
     * @param copyFromUrl url from those commit item was copied
     * @param copyFromRevision revision of the repository item from those commit item was copied
     * @param flags commit item flags
     * @return newly created commit item with initialized fields 
     * @throws SVNException if URL parse error occurred
     */
    public SvnCommitItem addItem(File path, SVNURL rootUrl, SVNNodeKind kind, SVNURL url, long revision,
            SVNURL copyFromUrl, long copyFromRevision, int flags) throws SVNException {
        SvnCommitItem item = new SvnCommitItem();
        item.setPath(path);
        item.setKind(kind);
        item.setUrl(url);
        item.setRevision(revision);
        if (copyFromUrl!= null) {
            item.setCopyFromUrl(copyFromUrl);
            item.setCopyFromRevision(copyFromRevision);
        } else {
            item.setCopyFromRevision(-1);
        }
        item.setFlags(flags);
        
        if (!items.containsKey(rootUrl)) {
            items.put(rootUrl, new HashSet<SvnCommitItem>());
        }
        
        items.get(rootUrl).add(item);
        itemsByPath.put(SVNFileUtil.getFilePath(path), item);
        return item;
    }

    /**
     * 
     * @param commitRunner
     * @param context
     */
    public void setLockingContext(ISvnCommitRunner commitRunner, Object context) {
        lockingContext = context;        
        runner = commitRunner;
    }
    
    /**
     * Disposes the commit packet, if commit runner is set method calls 
     * {@link ISvnCommitRunner#disposeCommitPacket(Object)} with the commit packet
     *
     * @throws SVNException
     */
    public void dispose() throws SVNException {
        try {
            if (runner != null) {
                runner.disposeCommitPacket(lockingContext, isLastPacket());
            }
        } finally {
            if (sharedIndex != null) {
                sharedIndex.decrementAndGet();
            }
            if (items != null) {
                items.clear();
            }
            if (itemsByPath != null) {
                itemsByPath.clear();
            }
            runner = null;
            lockingContext = null;
        }
    }

    /**
    * Sets commit packet's lock tokens, containing the information about locks within commit packet URLs.
    * @param lockTokens hash of URL, lock tokens for this URL 
    */
    public void setLockTokens(Map<SVNURL, String> lockTokens) {
        this.lockTokens = lockTokens;
    }
    
    /**
     * Returns all lock tokens of commit packet.
     * 
     * @return hash of URL, lock tokens
     */
    public Map<SVNURL, String> getLockTokens() {
        return lockTokens;
    }
    
    /**
     * Tests whether the commit packet has commit items.
     * 
     * @return <code>true</code> if the commit packet has no commit items, otherwise <code>false</code>
     */
    public boolean isEmpty() {
        for (SVNURL rootUrl : getRepositoryRoots()) {
            if (!isEmpty(rootUrl)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether the commit packet has commit items with the repository root URL.
     * 
     * @return <code>true</code> if the commit packet has no commit items with the repository root, otherwise <code>false</code>
     */
    public boolean isEmpty(SVNURL repositoryRootUrl) {
        for (SvnCommitItem item : getItems(repositoryRootUrl)) {
            if (item.getFlags() != SvnCommitItem.LOCK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns commit packet's locking context.
     * @return the locking context for the commit packet
     */
    public Object getLockingContext() {
        return lockingContext;
    }

    /**
     * Returns commit packet's runner.
     * @return the runner for the commit packet
     */
    public ISvnCommitRunner getRunner() {
        return runner;
    }

    public void setItemSkipped(File file, boolean skipped) {
        final String path = SVNFileUtil.getFilePath(file);
        if (skipped) {
            skippedPaths.add(path);
        } else {
            skippedPaths.remove(path);
        }
        if (lockingContext != null && lockingContext instanceof SVNCommitPacket && !(lockingContext instanceof SvnCodec.SVNCommitPacketWrapper)) {
            final SvnCommitItem commitItem = itemsByPath.get(path);
            if (commitItem != null) {
                final SVNCommitPacket oldPacket = (SVNCommitPacket) lockingContext;
                final SVNCommitItem[] oldItems = oldPacket.getCommitItems();
                for (SVNCommitItem oldItem : oldItems) {
                    if (SVNFileUtil.getFilePath(oldItem.getFile()).equals(path)) {
                        oldPacket.setCommitItemSkipped(oldItem, true);
                        break;
                    }
                }
            }
        }
    }

    public boolean isItemSkipped(File file) {
        return skippedPaths.contains(SVNFileUtil.getFilePath(file));
    }

    public SvnCommitPacket removeSkippedItems() {
        final HashMap<String, SvnCommitItem> filteredItemsByPath = new HashMap<String, SvnCommitItem>();
        final Map<SVNURL, Collection<SvnCommitItem>> filteredItems = new HashMap<SVNURL, Collection<SvnCommitItem>>();
        final Map<SVNURL, String> filteredLockTokens = new HashMap<SVNURL, String>(this.lockTokens);
        Object filteredLockingContext = lockingContext;

        for (Map.Entry<String, SvnCommitItem> entry : this.itemsByPath.entrySet()) {
            final String path = entry.getKey();
            SvnCommitItem commitItem = entry.getValue();

            if (!skippedPaths.contains(path)) {
                filteredItemsByPath.put(path, commitItem);
            }
        }

        for (Map.Entry<SVNURL, Collection<SvnCommitItem>> entry : this.items.entrySet()) {
            final SVNURL url = entry.getKey();
            final Collection<SvnCommitItem> commitItems = entry.getValue();

            final List<SvnCommitItem> filteredCommitItems = new ArrayList<SvnCommitItem>();
            for (SvnCommitItem commitItem : commitItems) {
                final String path = SVNFileUtil.getFilePath(commitItem.getPath());
                if (!skippedPaths.contains(path)) {
                    filteredCommitItems.add(commitItem);
                } else {
                    lockTokens.remove(commitItem.getUrl());
                }
            }

            if (filteredCommitItems.size() > 0) {
                filteredItems.put(url, filteredCommitItems);
            }
        }
        SvnCommitPacket result = new SvnCommitPacket(filteredItems, filteredItemsByPath, filteredLockingContext, filteredLockTokens, runner, skippedPaths);
        result.sharedIndex = sharedIndex;
        return result;
    }

    SvnCommitPacket[] split(boolean combinePackets) throws SVNException {
        final Map<String, SvnCommitPacket> splitPackets = new HashMap<String, SvnCommitPacket>();
        final AtomicInteger sharedIndex = new AtomicInteger(0);
        for (SVNURL root : getRepositoryRoots()) {
            Collection<SvnCommitItem> items = getItems(root);
            for (SvnCommitItem item : items) {
                if (isItemSkipped(item.getPath())) {
                    continue;
                }
                final String key = getItemKey(item, root, combinePackets);
                if (!splitPackets.containsKey(key)) {
                    final SvnCommitPacket newPacket = new SvnCommitPacket();
                    newPacket.runner = this.runner;
                    newPacket.sharedIndex = sharedIndex;
                    newPacket.setLockTokens(getLockTokens());
                    sharedIndex.incrementAndGet();
                    splitPackets.put(key, newPacket);
                }
                splitPackets.get(key).addItem(item, root);
            }
        }
        
        for (SvnCommitPacket splitPacket : splitPackets.values()) {
            splitPacket.lockingContext = this.lockingContext;
        }
        return splitPackets.values().toArray(new SvnCommitPacket[splitPackets.size()]);
    }

    private String getItemKey(SvnCommitItem item, SVNURL rootURL, boolean combinePackets) throws SVNException {
        if (combinePackets) {
            return rootURL.toString();
        }
        
        final File wcRoot = SvnOperationFactory.getWorkingCopyRoot(item.getKind() == SVNNodeKind.FILE ? item.getPath().getParentFile() : item.getPath(), true);
        return rootURL.toString() + ":" + wcRoot.getAbsolutePath();
    }

    public boolean isLastPacket() {
        return sharedIndex == null || sharedIndex.get() == 1;
    }
}
