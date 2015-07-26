package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpgrade;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgUpgrade extends SvnNgOperationRunner<SvnWcGeneration, SvnUpgrade> {

    @Override
    protected SvnWcGeneration run(SVNWCContext context) throws SVNException {
        ISVNWCDb db = context.getDb();
        if (db != null && db instanceof SVNWCDb) {
            final File localAbsPath = getFirstTarget();
            SVNWCDb.DirParsedInfo dirParsedInfo = ((SVNWCDb) db).parseDir(localAbsPath, SVNSqlJetDb.Mode.ReadOnly, true, false);
            int format = dirParsedInfo.wcDbDir.getWCRoot().getFormat();

            if (format < getOperation().getTargetWorkingCopyFormat()) {
                SvnNgUpgradeSDb.upgrade(localAbsPath, (SVNWCDb) db, db.getSDb(dirParsedInfo.wcDbDir.getWCRoot().getAbsPath()), format, context.getEventHandler());
            }

            final SVNURL[] lastRepos = {null};
            final String[] lastUuid = {null};

            final SvnGetProperties getProperties = getOperation().getOperationFactory().createGetProperties();
            getProperties.setDepth(SVNDepth.INFINITY);
            getProperties.setSingleTarget(getOperation().getFirstTarget());
            getProperties.setReceiver(new ISvnObjectReceiver<SVNProperties>() {
                public void receive(SvnTarget target, SVNProperties properties) throws SVNException {
                    File externalsParentAbsPath = target.getFile();
                    File externalsParentRelPath = SVNFileUtil.skipAncestor(localAbsPath, externalsParentAbsPath);

                    String externalsValuesString = properties.getStringValue(SVNProperty.EXTERNALS);
                    try {
                        SVNWCContext.SVNWCNodeReposInfo nodeReposInfo = getWcContext().getNodeReposInfo(externalsParentAbsPath);
                        SVNURL externalsParentReposRootUrl = nodeReposInfo.reposRootUrl;
                        SVNURL externalsParentUrl = externalsParentReposRootUrl.appendPath(SVNFileUtil.getFilePath(externalsParentRelPath), false);

                        if (externalsValuesString != null) {
                            SVNExternal[] externals = SVNExternal.parseExternals(externalsParentAbsPath, externalsValuesString);
                            for (SVNExternal external : externals) {
                                File externalsAbsPath = SVNFileUtil.createFilePath(externalsParentAbsPath, external.getPath());
                                try {
                                    SVNURL resolvedUrl = external.resolveURL(externalsParentReposRootUrl, externalsParentUrl);

                                    boolean upgradeRequired = false;
                                    SVNNodeKind externalKind;
                                    try {
                                        externalKind = getWcContext().getDb().readKind(externalsAbsPath, true, true, false);
                                        try {
                                            SvnGetStatus getStatus = getOperation().getOperationFactory().createGetStatus();
                                            getStatus.setDepth(SVNDepth.EMPTY);
                                            getStatus.setSingleTarget(SvnTarget.fromFile(externalsAbsPath));
                                            SvnStatus status = getStatus.run();

                                            if (status != null) {
                                                int workingCopyFormat = status.getWorkingCopyFormat();
                                                if (workingCopyFormat < getOperation().getTargetWorkingCopyFormat()) {
                                                    upgradeRequired = true;
                                                }
                                            }
                                        } catch (SVNException e) {
                                        }
                                    } catch (SVNException e) {
                                        if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_UPGRADE_REQUIRED) {
                                            upgradeRequired = true;
                                        } else {
                                            throw e;
                                        }
                                    }
                                    if (upgradeRequired) {
                                        SvnOperationFactory operationFactory = getOperation().getOperationFactory();
                                        SvnUpgrade upgrade = operationFactory.createUpgrade();
                                        upgrade.setSingleTarget(SvnTarget.fromFile(externalsAbsPath));
                                        upgrade.run();
                                    }
                                    externalKind = getWcContext().getDb().readKind(externalsAbsPath, true, true, false);

                                    SVNWCContext.SVNWCNodeReposInfo reposInfo = getWcContext().getNodeReposInfo(externalsAbsPath);
                                    SVNURL reposRootUrl = reposInfo.reposRootUrl;
                                    String reposUuid = reposInfo.reposUuid;
                                    File reposRelPath = reposInfo.reposRelPath;

                                    if (resolvedUrl != null && reposRootUrl != null && !resolvedUrl.equals(reposRootUrl.appendPath(SVNFileUtil.getFilePath(reposRelPath), false))) {
                                        SVNWCDb.ReposInfo fetchedReposInfo = fetchReposInfo(resolvedUrl, lastRepos[0], lastUuid[0]);
                                        lastRepos[0] = SVNURL.parseURIEncoded(fetchedReposInfo.reposRootUrl);
                                        lastUuid[0] = fetchedReposInfo.reposUuid;

                                        reposRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(reposRootUrl.toDecodedString(), resolvedUrl.toDecodedString()));

                                        externalKind = SVNNodeKind.UNKNOWN;
                                    }

                                    long pegRevision = external.getPegRevision().getNumber();
                                    long revision = external.getRevision().getNumber();

                                    upgradeAddExternalInfo(externalsAbsPath, externalKind, externalsParentAbsPath, reposRelPath, reposRootUrl, reposUuid, pegRevision, revision);
                                } catch (SVNException e) {
                                    SVNEvent event = SVNEventFactory.createSVNEvent(externalsAbsPath, SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.FAILED_EXTERNAL, e.getErrorMessage(), null);
                                    ISVNEventHandler eventHandler = getOperation().getEventHandler();
                                    if (eventHandler != null) {
                                        eventHandler.handleEvent(event, UNKNOWN);
                                    }
                                }
                            }
                        }

                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.CLIENT_INVALID_EXTERNALS_DESCRIPTION) {
                            throw e;
                        }
                        SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.FAILED_EXTERNAL, e.getErrorMessage(), null);
                        ISVNEventHandler eventHandler = getOperation().getEventHandler();
                        if (eventHandler != null) {
                            eventHandler.handleEvent(event, UNKNOWN);
                        }
                    }

                }
            });
            getProperties.run();

        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OP_ON_CWD, "Can''t upgrade ''{0}'' as it is not a pre-1.7 working copy directory",
                    getOperation().getFirstTarget().getFile().getAbsolutePath());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return SvnWcGeneration.V17;
    }

    public void upgradeAddExternalInfo(File localAbsPath, SVNNodeKind kind, File defLocalAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long defPegRevision, long defRevision) throws SVNException {
        SVNNodeKind dbKind = SVNNodeKind.UNKNOWN;
        if (kind == SVNNodeKind.DIR) {
            dbKind = SVNNodeKind.DIR;
        } else if (kind == SVNNodeKind.FILE) {
            dbKind = SVNNodeKind.FILE;
        } else if (kind == SVNNodeKind.UNKNOWN) {
            dbKind = SVNNodeKind.UNKNOWN;
        } else {
            SVNErrorManager.assertionFailure(false, null, SVNLogType.WC);
        }

        getWcContext().getDb().upgradeInsertExternal(localAbsPath, dbKind, SVNFileUtil.getParentFile(localAbsPath), defLocalAbsPath, reposRelPath, reposRootUrl, reposUuid, defPegRevision, defRevision);
    }

    public SVNWCDb.ReposInfo fetchReposInfo(SVNURL url, SVNURL lastRepos, String lastUuid) throws SVNException {
        if (lastRepos != null && SVNPathUtil.isAncestor(lastRepos.toString(), url.toString())) {
            SVNWCDb.ReposInfo reposInfo = new SVNWCDb.ReposInfo();
            reposInfo.reposRootUrl = lastRepos.toString();
            reposInfo.reposUuid = lastUuid;
            return reposInfo;
        }
        SVNRepository repository = getRepositoryAccess().createRepository(url, null, true);
        SVNWCDb.ReposInfo reposInfo = new SVNWCDb.ReposInfo();
        reposInfo.reposRootUrl = repository.getRepositoryRoot(true).toString();
        reposInfo.reposUuid = repository.getRepositoryUUID(true);
        return reposInfo;
    }

}
