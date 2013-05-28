package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc16.SVNStatusClient16;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnGetStatusSummary;
import org.tmatesoft.svn.core.wc2.SvnStatusSummary;

public class SvnOldGetStatusSummary extends SvnOldRunner<SvnStatusSummary, SvnGetStatusSummary> {

    @Override
    protected SvnStatusSummary run() throws SVNException {
        SVNWCAccess wcAccess = SVNWCAccess.newInstance(getOperation().getEventHandler());
        try {
            wcAccess.open(getFirstTarget(), false, 0);
        } finally {
            wcAccess.close();
        }
        SVNStatusClient16 statusClient = new SVNStatusClient16((ISVNAuthenticationManager) null, 
                getOperation().getOptions());
        statusClient.setIgnoreExternals(true);
        final long[] maxRevision = new long[1];
        final long[] minRevision = new long[] {
            -1
        };
        final boolean[] switched = new boolean[3];
        final String[] wcURL = new String[1];
        statusClient.doStatus(getFirstTarget(), SVNRevision.WORKING, SVNDepth.INFINITY, false, true, false, false, new ISVNStatusHandler() {

            public void handleStatus(SVNStatus status) {
                if (status.getEntryProperties() == null || status.getEntryProperties().isEmpty()) {
                    return;
                }
                if (status.getContentsStatus() != SVNStatusType.STATUS_ADDED && !status.isFileExternal()) {
                    SVNRevision revision = getOperation().isCommitted() ? status.getCommittedRevision() : status.getRevision();
                    if (revision != null) {
                        if (minRevision[0] < 0 || minRevision[0] > revision.getNumber()) {
                            minRevision[0] = revision.getNumber();
                        }
                        maxRevision[0] = Math.max(maxRevision[0], revision.getNumber());
                    }
                }
                switched[0] |= status.isSwitched();
                switched[1] |= status.getContentsStatus() != SVNStatusType.STATUS_NORMAL;
                switched[1] |= status.getPropertiesStatus() != SVNStatusType.STATUS_NORMAL && status.getPropertiesStatus() != SVNStatusType.STATUS_NONE;
                switched[2] |= status.getEntry() != null && status.getEntry().getDepth() != SVNDepth.INFINITY;
                if (wcURL[0] == null && status.getFile() != null && status.getFile().equals(getFirstTarget()) && status.getURL() != null) {
                    wcURL[0] = status.getURL().toString();
                }
            }
        }, null);
        if (!switched[0] && getOperation().getTrailUrl() != null) {
            if (wcURL[0] == null) {
                switched[0] = true;
            } else {
                switched[0] = !wcURL[0].endsWith(getOperation().getTrailUrl());
            }
        }
        SvnStatusSummary summary = new SvnStatusSummary();
        summary.setMaxRevision(maxRevision[0]);
        summary.setMinRevision(maxRevision[0]);
        summary.setModified(switched[1]);
        summary.setSwitched(switched[0]);
        summary.setSparseCheckout(switched[2]);
        
        StringBuffer id = new StringBuffer();
        id.append(minRevision[0]);
        if (minRevision[0] != maxRevision[0]) {
            id.append(":").append(maxRevision[0]);
        }
        if (switched[1]) {
            id.append("M");
        }
        if (switched[0]) {
            id.append("S");
        }
        if (switched[2]) {
            id.append("P");
        }
        
        return summary;
    }

}
