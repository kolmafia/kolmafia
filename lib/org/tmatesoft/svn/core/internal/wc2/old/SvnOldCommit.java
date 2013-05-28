package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc16.SVNCommitClient16;
import org.tmatesoft.svn.core.internal.wc2.ISvnCommitRunner;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnCommitPacket;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnOldCommit extends SvnOldRunner<SVNCommitInfo, SvnCommit> implements ISvnCommitRunner {

    public SvnCommitPacket collectCommitItems(SvnCommit operation) throws SVNException {
        setOperation(operation);
        SVNCommitClient16 client = new SVNCommitClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        client.setCommitParameters(SvnCodec.commitParameters(getOperation().getCommitParameters()));

        File[] paths = new File[getOperation().getTargets().size()];
        int i = 0;
        for (SvnTarget tgt : getOperation().getTargets()) {
            paths[i++] = tgt.getFile();
        }
        
        String[] changelists = null;
        if (getOperation().getApplicableChangelists() != null && !getOperation().getApplicableChangelists().isEmpty()) {
            changelists = getOperation().getApplicableChangelists().toArray(new String[getOperation().getApplicableChangelists().size()]);
        }
        SVNCommitPacket[] packets = client.doCollectCommitItems(
                paths, getOperation().isKeepLocks(), getOperation().isForce(), getOperation().getDepth(), true, changelists);
        if (packets != null && packets.length == 1) {
            return SvnCodec.commitPacket(this, packets[0]);
        } else  if (packets != null && packets.length > 1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                    "Commit from different working copies belonging to different repositories is not supported");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return null;
    }

    @Override
    protected SVNCommitInfo run() throws SVNException {
        SvnCommitPacket packet = getOperation().collectCommitItems();
        if (packet == null || packet.isEmpty()) {
            //if there's no changes, the packet will be null
            return null;
        }
        SVNCommitPacket oldPacket = (SVNCommitPacket) packet.getLockingContext();
        
        SVNCommitClient16 client = new SVNCommitClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        client.setCommitParameters(SvnCodec.commitParameters(getOperation().getCommitParameters()));
        
        SVNCommitInfo info = client.doCommit(oldPacket, getOperation().isKeepLocks(), getOperation().isKeepChangelists(), getOperation().getCommitMessage(), getOperation().getRevisionProperties());
        if (info != null) {
            getOperation().receive(getOperation().getFirstTarget(), info);
        }
        return info;
    }

    public void disposeCommitPacket(Object lockingContext, boolean disposeParentContext) throws SVNException {
        if (lockingContext instanceof SVNCommitPacket[]) {
            SVNCommitPacket[] packets = (SVNCommitPacket[]) lockingContext;
            for (int i = 0; i < packets.length; i++) {
                try {
                    packets[i].dispose();
                } catch (SVNException e) {
                    //
                }
            }
        } else if (lockingContext instanceof SVNCommitPacket) {
            ((SVNCommitPacket) lockingContext).dispose();
        }
    }

    public Object splitLockingContext(Object lockingContext, SvnCommitPacket newPacket) {
        if (lockingContext instanceof SVNCommitPacket[]) {
            final SVNCommitPacket[] oldPackets = (SVNCommitPacket[]) lockingContext;
            // TODO 
            return lockingContext;
        } else {
            return lockingContext;
        }
    }
}
