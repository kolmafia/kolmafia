package org.tmatesoft.svn.core.internal.wc;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetProperties;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SVNExternalsUtil {

    public static Map<String, SVNPropertyValue> resolvePinnedExternals(SVNWCContext context, SvnRepositoryAccess repositoryAccess, Map<SvnTarget, List<SVNExternal>> externalsToPin, SvnTarget pairSource, SvnTarget pairDst, long pairSourceRevision, SVNRepository svnRepository, SVNURL reposRootUrl) throws SVNException {
        final Map<String, SVNPropertyValue> pinnedExternals = new HashMap<String, SVNPropertyValue>();
        final Map<SvnTarget, SVNPropertyValue> externalsProps = new HashMap<SvnTarget, SVNPropertyValue>();

        SVNURL oldUrl = null;
        if (pairSource.isURL()) {
            oldUrl = svnRepository.getLocation();

            svnRepository.setLocation(pairSource.getURL(), false);

            SvnRemoteGetProperties.remotePropertyGet(pairSource.getURL(), SVNNodeKind.DIR, "", svnRepository, pairSourceRevision, SVNDepth.INFINITY, new ISvnObjectReceiver<SVNProperties>() {
                @Override
                public void receive(SvnTarget target, SVNProperties properties) throws SVNException {
                    if (properties != null) {
                        final SVNPropertyValue externalsValue = properties.getSVNPropertyValue(SVNProperty.EXTERNALS);
                        if (externalsValue != null) {
                            externalsProps.put(target, externalsValue);
                        }
                    }
                }
            });
        } else {
            SVNExternalsStore externalsStore = new SVNExternalsStore();
            context.getDb().gatherExternalDefinitions(pairSource.getFile(), externalsStore);

            final Map<File, String> newExternals = externalsStore.getNewExternals();
            for (Map.Entry<File, String> entry : newExternals.entrySet()) {
                final File localAbsPath = entry.getKey();
                final String externalsValue = entry.getValue();
                externalsProps.put(SvnTarget.fromFile(localAbsPath), SVNPropertyValue.create(externalsValue));
            }
        }

        if (externalsProps.size() == 0) {
            if (oldUrl != null) {
                svnRepository.setLocation(oldUrl, false);
            }
            return Collections.emptyMap();
        }

        for (Map.Entry<SvnTarget, SVNPropertyValue> entry : externalsProps.entrySet()) {
            final SvnTarget localAbsPathOrUrl = entry.getKey();
            final SVNPropertyValue externalsPropValue = entry.getValue();

            SVNPropertyValue newPropVal = pinExternalProps(context, repositoryAccess, externalsPropValue, externalsToPin, reposRootUrl, localAbsPathOrUrl);

            if (newPropVal != null) {
                final String relativePath = SVNPathUtil.getRelativePath(pairSource.getPathOrUrlDecodedString(), localAbsPathOrUrl.getPathOrUrlDecodedString());
                pinnedExternals.put(relativePath, newPropVal);
            }
        }

        if (oldUrl != null) {
            svnRepository.setLocation(oldUrl, false);
        }

        return pinnedExternals;
    }

    private static SVNPropertyValue pinExternalProps(SVNWCContext context, SvnRepositoryAccess repositoryAccess, SVNPropertyValue externalsPropValue, Map<SvnTarget, List<SVNExternal>> externalsToPin, SVNURL reposRootUrl, SvnTarget localAbsPathOrUrl) throws SVNException {
        final StringBuilder stringBuilder = new StringBuilder();

        final SVNExternal[] externals = SVNExternal.parseExternals(localAbsPathOrUrl, SVNPropertyValue.getPropertyAsString(externalsPropValue));
        List<SVNExternal> itemsToPin;
        if (externalsToPin != null) {
            itemsToPin = externalsToPin.get(localAbsPathOrUrl);
            if (itemsToPin == null) {
                return null;
            }
        } else {
            itemsToPin = null;
        }
        int pinnedItems = 0;
        for (SVNExternal item : externals) {
            SVNRevision externalPegRev;
            if (itemsToPin != null) {
                SVNExternal itemToPin = null;

                for (SVNExternal current : itemsToPin) {
                    if (current != null && current.getUnresolvedUrl().equals(item.getUnresolvedUrl()) && current.getPath().equals(item.getPath())) {
                        itemToPin = current;
                        break;
                    }
                }

                if (itemToPin == null) {
                    externalPegRev = SVNRevision.UNDEFINED;
                    String externalDescription = makeExternalDescription(localAbsPathOrUrl, item, externalPegRev);
                    stringBuilder.append(externalDescription);
                    continue;
                }
            }

            if (item.getPegRevision().getDate() != null) {
                externalPegRev = item.getPegRevision();
            } else if (SVNRevision.isValidRevisionNumber(item.getPegRevision().getNumber())) {
                externalPegRev = item.getPegRevision();
            } else {
                assert item.getPegRevision() == SVNRevision.HEAD || item.getPegRevision() == SVNRevision.UNDEFINED;

                pinnedItems++;

                if (localAbsPathOrUrl.isURL()) {
                    SVNURL resolvedURL = item.resolveURL(reposRootUrl, localAbsPathOrUrl.getURL());
                    SVNRepository svnRepository = repositoryAccess.createRepository(resolvedURL, null, true);
                    externalPegRev = SVNRevision.create(svnRepository.getLatestRevision());
                } else {
                    File externalAbsPath = SVNFileUtil.createFilePath(localAbsPathOrUrl.getFile(), item.getPath());
                    ISVNWCDb.SVNWCDbKind externalKind;
                    try {
                        Structure<StructureFields.ExternalNodeInfo> externalNodeInfoStructure = SvnWcDbExternals.readExternal(context, externalAbsPath, localAbsPathOrUrl.getFile(), StructureFields.ExternalNodeInfo.kind, StructureFields.ExternalNodeInfo.presence);
                        assert externalNodeInfoStructure != null;
                        assert externalNodeInfoStructure.hasField(StructureFields.ExternalNodeInfo.presence);

                        ISVNWCDb.SVNWCDbStatus status = externalNodeInfoStructure.get(StructureFields.ExternalNodeInfo.presence);

                        if (status != ISVNWCDb.SVNWCDbStatus.Normal) {
                            externalKind = ISVNWCDb.SVNWCDbKind.Unknown;
                        } else {
                            ISVNWCDb.SVNWCDbKind kind = externalNodeInfoStructure.get(StructureFields.ExternalNodeInfo.kind);
                            switch (kind) {
                                case File:
                                case Symlink:
                                    externalKind = ISVNWCDb.SVNWCDbKind.File;
                                    break;
                                case Dir:
                                    externalKind = ISVNWCDb.SVNWCDbKind.Dir;
                                    break;
                                default:
                                    externalKind = null;
                                    break;
                            }
                        }
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                            throw e;
                        }
                        externalKind = null;
                    }
                    long externalCheckedOutRevision = 0;
                    if (externalKind == null) {
                        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot pin external ''{0}'' defined in {1} at ''{2}'' because it is not checked out in the working copy at ''{3}''", item.getUnresolvedUrl(), SVNProperty.EXTERNALS, localAbsPathOrUrl.getFile(), externalAbsPath.getAbsolutePath(), SVNProperty.EXTERNALS);
                        SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
                    } else if (externalKind == ISVNWCDb.SVNWCDbKind.Dir) {

                        boolean isSwitched = SvnWcDbReader.hasSwitchedSubtrees((SVNWCDb) context.getDb(), externalAbsPath);
                        if (isSwitched) {
                            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot pin external ''{0}'' defined in {1} at ''{2}'' because ''{3}'' has switched subtrees (switches cannot be represented in {4})", item.getUnresolvedUrl(), SVNProperty.EXTERNALS, localAbsPathOrUrl.getFile(), externalAbsPath.getAbsolutePath(), SVNProperty.EXTERNALS);
                            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
                        }

                        boolean isModified = SvnWcDbReader.hasLocalModifications(context, externalAbsPath);
                        if (isModified) {
                            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot pin external ''{0}'' defined in {1} at ''{2}'' because ''{3}'' has local modifications (local modifications cannot be represented in {4})", item.getUnresolvedUrl(), SVNProperty.EXTERNALS, localAbsPathOrUrl.getFile(), externalAbsPath.getAbsolutePath(), SVNProperty.EXTERNALS);
                            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
                        }
                        long[] minMaxRevisions = SvnWcDbReader.getMinAndMaxRevisions((SVNWCDb) context.getDb(), externalAbsPath);
                        if (minMaxRevisions[0] != minMaxRevisions[1]) {
                            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot pin external ''{0}'' defined in {1} at ''{2}'' because ''{3}'' is a mixed-revision working copy (mixed-revisions cannot be represented in {4})", item.getUnresolvedUrl(), SVNProperty.EXTERNALS, localAbsPathOrUrl.getFile(), externalAbsPath.getAbsolutePath(), SVNProperty.EXTERNALS);
                            SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
                        }
                        externalCheckedOutRevision = minMaxRevisions[0];
                    } else {
                        assert externalKind == ISVNWCDb.SVNWCDbKind.File;
                        SVNWCContext.SVNWCNodeReposInfo nodeReposInfo = context.getNodeReposInfo(externalAbsPath);
                        externalCheckedOutRevision = nodeReposInfo.revision;
                    }
                    externalPegRev = SVNRevision.create(externalCheckedOutRevision);
                }
            }
            assert externalPegRev.getDate() != null || SVNRevision.isValidRevisionNumber(externalPegRev.getNumber());
            final String pinnedDescription = makeExternalDescription(localAbsPathOrUrl, item, externalPegRev);
            stringBuilder.append(pinnedDescription);
        }
        if (pinnedItems > 0) {
            return SVNPropertyValue.create(stringBuilder.toString());
        } else {
            return null;
        }
    }

    private static String makeExternalDescription(SvnTarget localAbsPathOrUrl, SVNExternal item, SVNRevision externalPegRevision) throws SVNException {
        String parserRevisionString = item.getRevisionString();
        String parserPegRevisionString = item.getPegRevisionString();

        String revisionString;
        String pegRevisionString;
        switch (item.getFormat()) {
            case 1:
                if (externalPegRevision == SVNRevision.UNDEFINED) {
                    revisionString = parserRevisionString + " ";
                } else if (parserRevisionString != null && item.getRevision() != SVNRevision.HEAD) {
                    revisionString = parserRevisionString + " ";
                } else {
                    assert SVNRevision.isValidRevisionNumber(externalPegRevision.getNumber());
                    revisionString = "-r" + externalPegRevision.getNumber() + " ";
                }
                return maybeQuote(item.getPath()) + " " + revisionString + maybeQuote(item.getRawURL()) + "\n";
            case 2:
                if (externalPegRevision == SVNRevision.UNDEFINED) {
                    revisionString = parserRevisionString + " ";
                } else if (parserRevisionString != null && item.getRevision() != SVNRevision.HEAD) {
                    revisionString = parserRevisionString + " ";
                } else {
                    revisionString = "";
                }

                if (externalPegRevision == SVNRevision.UNDEFINED) {
                    pegRevisionString = parserPegRevisionString != null ? parserPegRevisionString : "";
                } else if (parserPegRevisionString != null && item.getRevision() != SVNRevision.HEAD) {
                    pegRevisionString = parserPegRevisionString;
                } else {
                    assert SVNRevision.isValidRevisionNumber(externalPegRevision.getNumber());
                    pegRevisionString = "@" + externalPegRevision.getNumber();
                }
                return revisionString + maybeQuote(item.getRawURL() + pegRevisionString) + " " + maybeQuote(item.getPath()) + "\n";
            default:
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_EXTERNALS_DESCRIPTION, "{0} property defined at ''{1}'' is using an unsupported syntax", SVNProperty.EXTERNALS, localAbsPathOrUrl.getFile());
                SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
                break;
        }
        return null;
    }

    private static String maybeQuote(String s) {
        final String[] argv = s.split("\\s");
        if (argv.length == 1 && argv[0].equals(s)) {
            return s;
        }
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('\"');
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\\' || c == '\"' || c == '\'') {
                stringBuilder.append('\\');
            }
            stringBuilder.append(c);
        }
        stringBuilder.append('\"');
        return stringBuilder.toString();
    }
}
