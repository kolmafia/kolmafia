package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.InheritedProperties;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnInheritedProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgGetProperties extends SvnNgOperationRunner<SVNProperties, SvnGetProperties> {
    
    @Override
    protected SVNProperties run(SVNWCContext context) throws SVNException {
        for (SvnTarget target : getOperation().getTargets()) {
            if (target.isFile()) {
                run(context, target.getFile());
            }
        }
        return getOperation().first();
    }

    protected SVNProperties run(SVNWCContext context, File target) throws SVNException {
        boolean pristine = getOperation().getRevision() == SVNRevision.COMMITTED || getOperation().getRevision() == SVNRevision.BASE;
        SVNNodeKind kind = context.getDb().readKind(target, true, pristine, false);
        
        if (kind == SVNNodeKind.UNKNOWN || kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNVERSIONED_RESOURCE, "''{0}'' is not under version control", target);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (getOperation().getTargetInheritedPropertiesReceiver() != null) {
            SVNRevision pegRevision = getOperation().getFirstTarget().getResolvedPegRevision(SVNRevision.HEAD, SVNRevision.WORKING);
            SVNRevision revision = getOperation().getRevision() == SVNRevision.UNDEFINED ? pegRevision : getOperation().getRevision();
            
            final boolean hasLocalIProps = (pegRevision == SVNRevision.WORKING || pegRevision == SVNRevision.UNDEFINED) &&
                    (revision == SVNRevision.WORKING || revision == SVNRevision.UNDEFINED);
            if (hasLocalIProps) {
                getLocaliProps(context, target);
            } else {
                final Structure<NodeOriginInfo> origin = context.getNodeOrigin(target, false, NodeOriginInfo.isCopy, NodeOriginInfo.copyRootAbsPath, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl,
                        NodeOriginInfo.reposUuid, NodeOriginInfo.revision);
                final File reposRelPath = origin.get(NodeOriginInfo.reposRelpath);
                if (reposRelPath != null) {
                    final SVNURL rootURL = origin.get(NodeOriginInfo.reposRootUrl);
                    final SVNURL url = rootURL.appendPath(SVNFileUtil.getFilePath(reposRelPath), false);
                    
                    Structure<RevisionsPair> revisionPair = null;
                    if (pegRevision.isLocal()) {
                        revisionPair = getRepositoryAccess().getRevisionNumber(null, getOperation().getFirstTarget(), pegRevision, revisionPair);
                        final long pegrevnum = revisionPair.lng(RevisionsPair.revNumber);
                        pegRevision = SVNRevision.create(pegrevnum);
                    }
                    if (revision.isLocal()) {
                        revisionPair = getRepositoryAccess().getRevisionNumber(null, getOperation().getFirstTarget(), revision, revisionPair);
                        long revnum = revisionPair.lng(RevisionsPair.revNumber);
                        revision = SVNRevision.create(revnum);
                    }
                    
                    SvnTarget opTarget = getOperation().getFirstTarget();
                    Structure<RepositoryInfo> repositoryInfo = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(url), revision, pegRevision, null);                    
                    final SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
                    long revnum = repositoryInfo.lng(RepositoryInfo.revision);
                    repositoryInfo.release();
                    final SVNURL repositoryRoot = repository.getRepositoryRoot(true);
                    final Map<String, SVNProperties> inheritedProperties = repository.getInheritedProperties("", revnum, null);
                    final List<SvnInheritedProperties> result = new ArrayList<SvnInheritedProperties>();
                    for (String path : inheritedProperties.keySet()) {
                        final SvnInheritedProperties propItem = new SvnInheritedProperties();
                        propItem.setTarget(SvnTarget.fromURL(repositoryRoot.appendPath(path, false)));
                        propItem.setProperties(inheritedProperties.get(path));
                        result.add(propItem);
                    }
                    if (!result.isEmpty()) {
                        getOperation().getTargetInheritedPropertiesReceiver().receive(opTarget, result);
                    }
                }

            }
        }
        
        if (kind == SVNNodeKind.DIR) {
            if (getOperation().getDepth() == SVNDepth.EMPTY) {
                if (!matchesChangelist(target)) {
                    return getOperation().first();
                }
                SVNProperties properties = null;
                if (pristine) {
                    properties = context.getDb().readPristineProperties(target);
                } else {
                    properties = context.getDb().readProperties(target);
                }
                if (properties != null && !properties.isEmpty()) {
                    getOperation().receive(SvnTarget.fromFile(target), properties);
                }
            } else {
                SVNWCDb db = (SVNWCDb) context.getDb();
                db.readPropertiesRecursively(
                        target, 
                        getOperation().getDepth(), 
                        false, 
                        pristine, 
                        getOperation().getApplicableChangelists(), 
                        getOperation());
            }
        } else {
            SVNProperties properties = null;
            if (pristine) {
                properties = context.getDb().readPristineProperties(target);
            } else {
                if (!context.isNodeStatusDeleted(target)) {
                    properties = context.getDb().readProperties(target);
                }
            }
            if (properties != null && !properties.isEmpty()) {
                getOperation().receive(SvnTarget.fromFile(target), properties);
            }
        }        
        return getOperation().first();
    }

    private void getLocaliProps(SVNWCContext context, File target) throws SVNException {
        final DirParsedInfo pdh = ((SVNWCDb) context.getDb()).parseDir(target, Mode.ReadOnly);
        final SVNWCDbRoot wcRoot = pdh.wcDbDir.getWCRoot();
        final List<Structure<InheritedProperties>> inheritedProps = SvnWcDbProperties.readInheritedProperties(wcRoot, pdh.localRelPath, null);
        final List<SvnInheritedProperties> resultList = new ArrayList<SvnInheritedProperties>();

        SVNURL repositoryRoot = null;
        if (inheritedProps != null && !inheritedProps.isEmpty()) {
            for (Structure<InheritedProperties> props : inheritedProps) {
                final SvnInheritedProperties result = new SvnInheritedProperties();
                result.setProperties(props.<SVNProperties>get(InheritedProperties.properties));
                final String pathOrURL = props.<String>get(InheritedProperties.pathOrURL);
                if (SVNPathUtil.isURL(pathOrURL)) {
                    result.setTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(pathOrURL)));
                } else if (SVNPathUtil.isAbsolute(pathOrURL)) {
                    final File absolutePath = SVNFileUtil.createFilePath(pathOrURL);
                    result.setTarget(SvnTarget.fromFile(absolutePath));
                } else {             
                    if (repositoryRoot == null) {
                        final Structure<NodeInfo> info = context.getDb().readInfo(target, NodeInfo.reposRootUrl);
                        repositoryRoot = info.get(NodeInfo.reposRootUrl);
                    }
                    if (repositoryRoot == null) {
                        ISVNWCDb.WCDbAdditionInfo additionInfo = context.getDb().scanAddition(target, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.reposRootUrl);
                        repositoryRoot = additionInfo.reposRootUrl;
                    }
                    result.setTarget(SvnTarget.fromURL(repositoryRoot.appendPath(pathOrURL, false)));
                }
                resultList.add(result);
            }
            getOperation().getTargetInheritedPropertiesReceiver().receive(getOperation().getFirstTarget(), resultList);
        }
    }

    @Override
    public boolean isApplicable(SvnGetProperties operation, SvnWcGeneration wcGeneration) throws SVNException {
        return !operation.isRevisionProperties() && super.isApplicable(operation, wcGeneration);
    }

}
