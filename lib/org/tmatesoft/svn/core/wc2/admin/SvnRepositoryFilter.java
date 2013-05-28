package org.tmatesoft.svn.core.wc2.admin;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnReceivingOperation;

public class SvnRepositoryFilter extends SvnReceivingOperation<SVNAdminEvent> {
    
    private InputStream dumpStream;
    private OutputStream resultDumpStream;
    private boolean exclude;
    private boolean renumberRevisions;
    private boolean dropEmptyRevisions;
    private boolean preserveRevisionProperties;
    private Collection<String> prefixes;
    private boolean skipMissingMergeSources;
    
    public SvnRepositoryFilter(SvnOperationFactory factory) {
        super(factory);
    }

	public InputStream getDumpStream() {
		return dumpStream;
	}

	public void setDumpStream(InputStream dumpStream) {
		this.dumpStream = dumpStream;
	}

	public OutputStream getResultDumpStream() {
		return resultDumpStream;
	}

	public void setResultDumpStream(OutputStream resultDumpStream) {
		this.resultDumpStream = resultDumpStream;
	}

	public boolean isExclude() {
		return exclude;
	}

	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}

	public boolean isRenumberRevisions() {
		return renumberRevisions;
	}

	public void setRenumberRevisions(boolean renumberRevisions) {
		this.renumberRevisions = renumberRevisions;
	}

	public boolean isDropEmptyRevisions() {
		return dropEmptyRevisions;
	}

	public void setDropEmptyRevisions(boolean dropEmptyRevisions) {
		this.dropEmptyRevisions = dropEmptyRevisions;
	}

	public boolean isPreserveRevisionProperties() {
		return preserveRevisionProperties;
	}

	public void setPreserveRevisionProperties(boolean preserveRevisionProperties) {
		this.preserveRevisionProperties = preserveRevisionProperties;
	}

	public Collection<String> getPrefixes() {
		return prefixes;
	}

	public void setPrefixes(Collection<String> prefixes) {
		this.prefixes = prefixes;
	}

	public boolean isSkipMissingMergeSources() {
		return skipMissingMergeSources;
	}

	public void setSkipMissingMergeSources(boolean skipMissingMergeSources) {
		this.skipMissingMergeSources = skipMissingMergeSources;
	}

    @Override
    protected int getMinimumTargetsCount() {
        return 0;
    }
}
