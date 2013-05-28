package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Indices;

public class SVNWCDbCollectTargets extends SVNWCDbNodesCurrent {

    private SVNDepth depth;
    private File target;
    private Collection<String> changelists;
    private SVNSqlJetSelectStatement selectChangelist;
    private Set<String> receivedPaths;

    public SVNWCDbCollectTargets(SVNSqlJetDb sDb, long wcId, File target, SVNDepth depth, Collection<String> changelists) throws SVNException {
        super(sDb);
        setDepth(depth);
        setTarget(target);
        setChangelists(changelists);
        
        if (getChangelists() != null && !getChangelists().isEmpty()) {
            selectChangelist = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE);
        }        
        if (getDepth() == SVNDepth.FILES || getDepth() == SVNDepth.IMMEDIATES) {
            setIndexName(NODES__Indices.I_NODES_PARENT.toString());
            receivedPaths = new HashSet<String>();
        }
        if (getDepth() == SVNDepth.FILES || getDepth() == SVNDepth.IMMEDIATES || getDepth() == SVNDepth.EMPTY) {
            bindf("is", wcId, getTarget());
        } else {
            bindf("i", wcId);
        }
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (getDepth() == SVNDepth.FILES || getDepth() == SVNDepth.IMMEDIATES) {
            // test if we had one with that name.
            if (!receivedPaths.add(getColumnString(NODES__Fields.local_relpath))) {
                return false;
            }
        } else if (!super.isFilterPassed()) {
            return false;
        }        
        if (getDepth() == SVNDepth.FILES) {
            String kind = getColumnString(NODES__Fields.kind);
            return "file".equals(kind) && matchesChangelist();
        } else if (getDepth() == SVNDepth.INFINITY) {
            String targetPath = SVNFileUtil.getFilePath(getTarget());
            if ("".equals(targetPath)) {
                return matchesChangelist();
            }
            String rowPath = getColumnString(NODES__Fields.local_relpath);
            return (targetPath.equals(rowPath) || rowPath.startsWith(targetPath + '/')) && matchesChangelist();
        }        
        return matchesChangelist();
    }
    
    private boolean matchesChangelist() throws SVNException {
        if (getChangelists() == null || getChangelists().isEmpty()) {
            return true;
        }
        try {
            selectChangelist.bindf("is", getColumnLong(NODES__Fields.wc_id), getColumnString(NODES__Fields.local_relpath));
            if (selectChangelist.next()) {
                return changelists.contains(selectChangelist.getColumnString(ACTUAL_NODE__Fields.changelist));
            }
        } finally {
            selectChangelist.reset();
        }
        return false;
    }

    private File getTarget() {
        return target;
    }
    private SVNDepth getDepth() {
        return depth;
    }
    private Collection<String> getChangelists() {
        return changelists;
    }
    private void setDepth(SVNDepth depth) {
        this.depth = depth;
    }
    private void setTarget(File target) {
        this.target = target;
    }
    private void setChangelists(Collection<String> changelists) {
        this.changelists = changelists == null ? changelists : new HashSet<String>(changelists);
    }
    
}
