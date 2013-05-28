package org.tmatesoft.svn.core.wc;

import java.util.Map;
import java.io.File;

public interface ISVNStatusFileProvider {
    /**
     * Returns Map (key = file name, value = java.io.File) of files under dir that client is interested in
     * @return should return null for the case when file list should be calculated outside
     */
    Map<String, File> getChildrenFiles(File parent);
}
