package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.SvnUpgrade;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SvnNgDowngrade extends SvnNgOperationRunner<SvnWcGeneration, SvnUpgrade> {
    @Override
    protected SvnWcGeneration run(SVNWCContext context) throws SVNException {
        File localAbsPath = getFirstTarget();
        downgrade(getWcContext(), localAbsPath);
        return SvnWcGeneration.V17;
    }

    public void downgrade(SVNWCContext context, File localAbsPath) throws SVNException {
        SVNWCDb db = (SVNWCDb) context.getDb();

        SVNWCDb.DirParsedInfo parsed = db.parseDir(localAbsPath, SVNSqlJetDb.Mode.ReadOnly);
        SVNWCDbRoot wcRoot = parsed.wcDbDir.getWCRoot();
        File wcRootAbsPath = wcRoot.getAbsPath();

        SVNSqlJetDb sDb = wcRoot.getSDb();
        sDb.beginTransaction(SqlJetTransactionMode.WRITE);
        try {
            ArrayList<Long> wcIds = new ArrayList<Long>();
            ArrayList<File> paths = new ArrayList<File>();
            collectConflicts18(sDb, wcIds, paths);
            for (int i = 0; i < wcIds.size(); i++) {
                long wcId = wcIds.get(i);
                File childRelPath = paths.get(i);
                File childAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), childRelPath);

                List<SVNConflictDescription> conflictDescriptions = db.readConflicts(childAbsPath);

                File conflictOldAbsPath = null;
                File conflictNewAbsPath = null;
                File conflictWorkingAbsPath = null;

                File prejAbsPath = null;

                byte[] treeConflictData = null;

                for (SVNConflictDescription conflictDescription : conflictDescriptions) {
                    if (conflictDescription instanceof SVNTextConflictDescription) {
                        SVNTextConflictDescription textConflictDescription = (SVNTextConflictDescription) conflictDescription;
                        SVNMergeFileSet mergeFiles = textConflictDescription.getMergeFiles();

                        conflictOldAbsPath = mergeFiles.getBaseFile();
                        conflictWorkingAbsPath = mergeFiles.getLocalFile();
                        conflictNewAbsPath = mergeFiles.getRepositoryFile();

                    } else if (conflictDescription instanceof SVNPropertyConflictDescription) {
                        SVNPropertyConflictDescription propertyConflictDescription = (SVNPropertyConflictDescription) conflictDescription;

                        SVNMergeFileSet mergeFiles = propertyConflictDescription.getMergeFiles();
                        prejAbsPath = mergeFiles.getRepositoryFile();

                    } else if (conflictDescription instanceof SVNTreeConflictDescription) {
                        SVNTreeConflictDescription treeConflictDescription = (SVNTreeConflictDescription) conflictDescription;

                        treeConflictData = SVNTreeConflictUtil.getSingleTreeConflictRawData(treeConflictDescription);
                    }
                }

                File conflictOldRelPath = conflictOldAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictOldAbsPath);
                File conflictNewRelPath = conflictNewAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictNewAbsPath);
                File conflictWorkingRelPath = conflictWorkingAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictWorkingAbsPath);
                File prejRelPath = prejAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, prejAbsPath);

                downgradeConflict(sDb, wcId, childRelPath, conflictOldRelPath, conflictNewRelPath, conflictWorkingRelPath, prejRelPath, treeConflictData);
            }

            SvnNgUpgradeSDb.setVersion(sDb, 29);
        } catch (SVNException e) {
            sDb.rollback();
        } finally {
            sDb.commit();
        }
    }

    private void downgradeConflict(SVNSqlJetDb sDb, long wcId, File localAbsPath,
                                   File conflictOldRelPath, File conflictNewRelPath, File conflictWorkingRelPath,
                                   File prejRelPath, byte[] treeConflictData) throws SVNException {
        DowngradeConflictStatement downgradeConflictStatement = new DowngradeConflictStatement(sDb);
        try {
            downgradeConflictStatement.bindf("isssssb", wcId, localAbsPath,
                    conflictOldRelPath, conflictNewRelPath, conflictWorkingRelPath,
                    prejRelPath, treeConflictData);
            downgradeConflictStatement.done();
        } finally {
            downgradeConflictStatement.reset();
        }

    }

    private void collectConflicts18(SVNSqlJetDb sDb, List<Long> wcIds, List<File> paths) throws SVNException {
        SelectConflictsStatement selectConflictsStatement = new SelectConflictsStatement(sDb);
        try {
            while (selectConflictsStatement.next()) {
                long wcId = selectConflictsStatement.getColumnLong(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id);
                File localRelPath = SvnWcDbStatementUtil.getColumnPath(selectConflictsStatement, SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);

                wcIds.add(wcId);
                paths.add(localRelPath);
            }
        } finally {
            selectConflictsStatement.reset();
        }
    }

    private static class SelectConflictsStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

        public SelectConflictsStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id);
            fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
            fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return null;
        }
    }

    private static class DowngradeConflictStatement extends SVNSqlJetUpdateStatement {

        public DowngradeConflictStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        }

        @Override
        public Map<String, Object> getUpdateValues() throws SVNException {
            final Map<String, Object> values = new HashMap<String, Object>();
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.name(), getBind(3));
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.name(), getBind(4));
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.name(), getBind(5));
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.name(), getBind(6));
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.name(), getBind(7));
            values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data.name(), null);
            return values;
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[]{getBind(1), getBind(2)};
        }
    }
}
