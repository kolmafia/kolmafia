package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbRelocate;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbRelocate.ISvnRelocateValidator;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnRelocate;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgRelocate extends SvnNgOperationRunner<SVNURL, SvnRelocate> implements ISvnRelocateValidator {
    
    private Map<String, SVNURL> collectedUuids;

    @Override
    protected SVNURL run(SVNWCContext context) throws SVNException {
        if (getOperation().getFromUrl() == null) {
            SVNURL fromURL = context.getNodeUrl(getFirstTarget());
            getOperation().setFromUrl(fromURL);
        }
        if (getOperation().isIgnoreExternals()) {
            SvnWcDbRelocate.relocate(context, getFirstTarget(), getOperation().getFromUrl(), getOperation().getToUrl(), this);
            return getOperation().getToUrl();
        }
        
        SVNURL oldReposRootUrl = context.getNodeReposInfo(getFirstTarget()).reposRootUrl;
        
        SvnWcDbRelocate.relocate(context, getFirstTarget(), getOperation().getFromUrl(), getOperation().getToUrl(), this);
        
        SVNURL newReposRootUrl = context.getNodeReposInfo(getFirstTarget()).reposRootUrl;
        SVNExternalsStore externalsStore = new SVNExternalsStore();        
        context.getDb().gatherExternalDefinitions(getFirstTarget(), externalsStore);
        
        for(File externalAbsPath : externalsStore.getNewExternals().keySet()) {
            String externalDefinition = externalsStore.getNewExternals().get(externalAbsPath);
            SVNExternal[] externals = SVNExternal.parseExternals(externalAbsPath, externalDefinition);
            if (externals != null && externals.length > 0) {
                relocateExternals(externalAbsPath, externals, oldReposRootUrl, newReposRootUrl);
            }
        }
        
        return getOperation().getToUrl();
    }

    private void relocateExternals(File localAbsPath, SVNExternal[] externals, SVNURL oldReposRootUrl, SVNURL newReposRootUrl) throws SVNException {
        for (int i = 0; i < externals.length; i++) {
            String rawUrl = externals[i].getUnresolvedUrl();
            if (!(rawUrl.startsWith("../") || rawUrl.startsWith("^/"))) {
                continue;
            }
            File targetPath = SVNFileUtil.createFilePath(localAbsPath, externals[i].getPath());
            try {
                SVNURL targetRepositoryRootUrl = getWcContext().getNodeReposInfo(targetPath).reposRootUrl;
                if (targetRepositoryRootUrl.equals(oldReposRootUrl)) {
                    SvnWcDbRelocate.relocate(getWcContext(), targetPath, oldReposRootUrl, newReposRootUrl, this);
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    continue;
                }
            }
        }
    }

    public void validateRelocation(String uuid, SVNURL url, SVNURL rootUrl) throws SVNException {
        String urlUuid = null;
        SVNURL urlRoot = null;
        for (String uu : collectedUuids.keySet()) {
            SVNURL root = collectedUuids.get(uu);
            if (SVNURLUtil.getRelativeURL(root, url, false) != null) {
                urlUuid = uu;
                urlRoot = root;
                break;
            }
        }
        if (urlUuid == null) {
            SVNRepository repository = getRepositoryAccess().createRepository(url, null);
            urlUuid = repository.getRepositoryUUID(true);
            urlRoot = repository.getRepositoryRoot(true);
        }
        if (rootUrl != null && !rootUrl.equals(urlRoot)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_RELOCATION, "''{0}'' is not the root of the repository", url);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (uuid != null && !uuid.equals(urlUuid)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_RELOCATION, 
                    "The repository at ''{0}'' has uuid ''{1}'', but the WC has ''{2}''", url, urlUuid, uuid);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    @Override
    public void reset(SvnWcGeneration wcGeneration) {
        super.reset(wcGeneration);
        collectedUuids = new HashMap<String, SVNURL>();
    }
}
