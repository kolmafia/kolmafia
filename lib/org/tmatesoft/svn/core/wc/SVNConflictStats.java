package org.tmatesoft.svn.core.wc;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SVNConflictStats {
    private Set<String> textConflicts;
    private Set<String> propConflicts;
    private Set<String> treeConflicts;

    private int resolvedTextConflictsCount;
    private int resolvedPropConflictsCount;
    private int resolvedTreeConflictsCount;
    private int skippedPathsCount;

    public SVNConflictStats() {
        textConflicts = new HashSet<String>();
        propConflicts = new HashSet<String>();
        treeConflicts = new HashSet<String>();
        resolvedTextConflictsCount = 0;
        resolvedPropConflictsCount = 0;
        resolvedTreeConflictsCount = 0;
        skippedPathsCount = 0;
    }

    public void incrementTextConflictsResolved(String path) {
        if (textConflicts.contains(path)) {
            textConflicts.remove(path);
            resolvedTextConflictsCount++;
        }
    }

    public void incrementPropConflictsResolved(String path) {
        if (propConflicts.contains(path)) {
            propConflicts.remove(path);
            resolvedPropConflictsCount++;
        }
    }

    public void incrementTreeConflictsResolved(String path) {
        if (treeConflicts.contains(path)) {
            treeConflicts.remove(path);
            resolvedTreeConflictsCount++;
        }
    }

    public int getResolvedTextConflictsCount() {
        return resolvedTextConflictsCount;
    }

    public int getResolvedPropConflictsCount() {
        return resolvedPropConflictsCount;
    }

    public int getResolvedTreeConflictsCount() {
        return resolvedTreeConflictsCount;
    }

    public void incrementSkippedPaths() {
        skippedPathsCount++;
    }

    public void storeTextConflict(String path) {
        textConflicts.add(path);
    }

    public void storePropConflict(String path) {
        propConflicts.add(path);
    }

    public void storeTreeConflict(String path) {
        treeConflicts.add(path);
    }

    public int getTextConflictsCount() {
        return textConflicts.size();
    }

    public int getPropConflictsCount() {
        return propConflicts.size();
    }

    public int getTreeConflictsCount() {
        return treeConflicts.size();
    }

    public int getSkippedPathsCount() {
        return skippedPathsCount;
    }
}
