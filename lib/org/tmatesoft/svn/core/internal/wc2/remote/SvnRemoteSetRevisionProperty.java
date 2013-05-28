package org.tmatesoft.svn.core.internal.wc2.remote;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnSetProperty;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteSetRevisionProperty extends SvnRemoteOperationRunner<SVNPropertyData, SvnSetProperty> {

    @Override
    public boolean isApplicable(SvnSetProperty operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.isRevisionProperty();
    }
    
    @Override
    protected SVNPropertyData run() throws SVNException {
        
        String propertyName = getOperation().getPropertyName();
        SVNPropertyValue propertyValue = getOperation().getPropertyValue();
        boolean force = getOperation().isForce();
        
        SvnRepositoryAccess access = getRepositoryAccess();
        Structure<RepositoryInfo> repositoryInfo = access.createRepositoryFor(getOperation().getFirstTarget(), SVNRevision.HEAD, SVNRevision.HEAD, null);
        SVNRepository repository = repositoryInfo.get(RepositoryInfo.repository);
        repositoryInfo.release();
        
        if (propertyValue != null && !SVNPropertiesManager.isValidPropertyName(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "Bad property name ''{0}''", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (!force && SVNRevisionProperty.AUTHOR.equals(propertyName) && propertyValue != null && propertyValue.isString() && propertyValue.getString().indexOf('\n') >= 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_REVISION_AUTHOR_CONTAINS_NEWLINE, "Value will not be set unless forced");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "''{0}'' is a wcprop, thus not accessible to clients", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (SVNProperty.isEntryProperty(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "Property ''{0}'' is an entry property", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        Structure<RevisionsPair> revPair = access.getRevisionNumber(repository, null, getOperation().getRevision(), null);
        long revNumber = revPair.lng(RevisionsPair.revNumber);
        repository.setRevisionPropertyValue(revNumber, propertyName, propertyValue);
        
        return new SVNPropertyData(propertyName, propertyValue, getOperation().getOptions());
    }

}
