package org.tmatesoft.svn.core.internal.wc2.old;

import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNDiffClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldMerge extends SvnOldRunner<Void, SvnMerge> {

    @Override
    protected Void run() throws SVNException {
        
        SVNDiffClient16 diffClient = new SVNDiffClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        diffClient.setMergeOptions(getOperation().getMergeOptions());
        diffClient.setEventHandler(getOperation().getEventHandler());
        
        if (getOperation().isReintegrate()) {
            if (getOperation().getSource().isURL()) {
                diffClient.doMergeReIntegrate(
                        getOperation().getSource().getURL(), 
                        getOperation().getSource().getResolvedPegRevision(), 
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().isDryRun());
            } else {
                diffClient.doMergeReIntegrate(
                        getOperation().getSource().getFile(), 
                        getOperation().getSource().getResolvedPegRevision(), 
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().isDryRun());
            }
        } else if (getOperation().getRevisionRanges() != null) {
            Collection<SVNRevisionRange> oldRanges = new ArrayList<SVNRevisionRange>();
            for (SvnRevisionRange range : getOperation().getRevisionRanges()) {
                oldRanges.add(SvnCodec.revisionRange(range));
            }
            if (getOperation().getSource().isURL()) {
                diffClient.doMerge(getOperation().getSource().getURL(), 
                        getOperation().getSource().getResolvedPegRevision(),
                        oldRanges,
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            } else {
                diffClient.doMerge(getOperation().getSource().getFile(), 
                        getOperation().getSource().getResolvedPegRevision(),
                        oldRanges,
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            }
        } else {
            SvnTarget firstSource = getOperation().getFirstSource();
            SvnTarget secondSource = getOperation().getSecondSource();
            
            if (firstSource.isURL() && secondSource.isURL()) {
                diffClient.doMerge(firstSource.getURL(), 
                        firstSource.getResolvedPegRevision(), 
                        secondSource.getURL(), 
                        secondSource.getResolvedPegRevision(),
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            } else if (firstSource.isURL() && secondSource.isFile()) {
                diffClient.doMerge(firstSource.getURL(), 
                        firstSource.getResolvedPegRevision(), 
                        secondSource.getFile(), 
                        secondSource.getResolvedPegRevision(),
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            } else if (firstSource.isFile() && secondSource.isURL()) {
                diffClient.doMerge(firstSource.getFile(), 
                        firstSource.getResolvedPegRevision(), 
                        secondSource.getURL(), 
                        secondSource.getResolvedPegRevision(),
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            } else if (firstSource.isFile() && secondSource.isFile()) {
                diffClient.doMerge(firstSource.getFile(), 
                        firstSource.getResolvedPegRevision(), 
                        secondSource.getFile(),
                        secondSource.getResolvedPegRevision(),
                        getOperation().getFirstTarget().getFile(), 
                        getOperation().getDepth(), 
                        !getOperation().isIgnoreAncestry(), 
                        getOperation().isForce(), 
                        getOperation().isDryRun(), 
                        getOperation().isRecordOnly());
            }
        }
        return null;
    }

}
