package org.tmatesoft.svn.core.internal.wc17.db;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnChecksum;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnDate;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnInt64;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnKind;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPath;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPresence;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnText;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.reset;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.WalkerChildInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnWcDbReader extends SvnWcDbShared {
    
    public enum ReplaceInfo {
        replaced,
        baseReplace,
        replaceRoot
    }

    public enum InstallInfo {
        wcRootAbsPath,
        sha1Checksum,
        pristineProps,
        changedDate,
    }
    
    public static Collection<File> getServerExcludedNodes(SVNWCDb db, File path) throws SVNException {
        DirParsedInfo dirInfo = db.obtainWcRoot(path);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        File localRelPath = dirInfo.localRelPath;
        
        Collection<File> result = new ArrayList<File>();
        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_ALL_SERVER_EXCLUDED_NODES);
        try {
            stmt.bindf("isi", wcId, localRelPath, 0);
            while(stmt.next()) {
                final File localPath = getColumnPath(stmt, NODES__Fields.local_relpath);
                final File absPath = dirInfo.wcDbDir.getWCRoot().getAbsPath(localPath);
                result.add(absPath);
            }
        } finally {
            reset(stmt);
        }
        return result;
        
    }

    public static Collection<File> getNotPresentDescendants(SVNWCDb db, File parentPath) throws SVNException {
        DirParsedInfo dirInfo = db.obtainWcRoot(parentPath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        File localRelPath = dirInfo.localRelPath;
        
        Collection<File> result = new ArrayList<File>();
        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NOT_PRESENT_DESCENDANTS);
        try {
            stmt.bindf("isi", wcId, localRelPath, SVNWCUtils.relpathDepth(localRelPath));
            while(stmt.next()) {
                result.add(new File(SVNWCUtils.getPathAsChild(localRelPath, getColumnPath(stmt, NODES__Fields.local_relpath))));
            }
        } finally {
            reset(stmt);
        }
        return result;
        
    }
    
    public static Structure<ReplaceInfo> readNodeReplaceInfo(SVNWCDb db, File localAbspath, ReplaceInfo... fields) throws SVNException {
        
        Structure<ReplaceInfo> result = Structure.obtain(ReplaceInfo.class, fields);
        result.set(ReplaceInfo.replaced, false);
        if (result.hasField(ReplaceInfo.baseReplace)) {
            result.set(ReplaceInfo.baseReplace, false);
        }
        if (result.hasField(ReplaceInfo.replaceRoot)) {
            result.set(ReplaceInfo.replaceRoot, false);
        }
        
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        File localRelPath = dirInfo.localRelPath;
        
        SVNSqlJetStatement stmt = null;
        begingReadTransaction(dirInfo.wcDbDir.getWCRoot());
        try {
            stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            stmt.bindf("is", wcId, localRelPath);
            if (!stmt.next()) {
                nodeNotFound(localAbspath);
            }
            if (getColumnPresence(stmt) != SVNWCDbStatus.Normal) {
                return result;
            }
            if (!stmt.next()) {
                return result;
            }
            
            SVNWCDbStatus replacedStatus = getColumnPresence(stmt);
            if (replacedStatus != SVNWCDbStatus.NotPresent
                    && replacedStatus != SVNWCDbStatus.Excluded
                    && replacedStatus != SVNWCDbStatus.ServerExcluded
                    && replacedStatus != SVNWCDbStatus.BaseDeleted) {
                result.set(ReplaceInfo.replaced, true);
            }
            long replacedOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
            if (result.hasField(ReplaceInfo.baseReplace)) {
                long opDepth = replacedOpDepth;
                boolean haveRow = true;
                while (opDepth != 0 && haveRow) {
                    haveRow = stmt.next();
                    if (haveRow) {
                        opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                    }
                }
                if (haveRow && opDepth == 0) {
                    SVNWCDbStatus baseStatus = getColumnPresence(stmt);
                    result.set(ReplaceInfo.baseReplace, baseStatus != SVNWCDbStatus.NotPresent);
                }
            }
            reset(stmt);
            
            if (!result.is(ReplaceInfo.replaced) || !result.hasField(ReplaceInfo.replaceRoot)) {
                return result;
            }
            if (replacedStatus != SVNWCDbStatus.BaseDeleted) {
                stmt.bindf("is", wcId, SVNFileUtil.getFileDir(localRelPath));
                if (stmt.next()) {
                    long parentOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                    if (parentOpDepth >= replacedOpDepth) {
                        result.set(ReplaceInfo.replaceRoot, parentOpDepth == replacedOpDepth);
                        return result;
                    }
                    boolean haveRow = stmt.next();
                    if (haveRow) {
                        parentOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                    }
                    if (!haveRow) {
                        result.set(ReplaceInfo.replaceRoot, true);
                    } else if (parentOpDepth < replacedOpDepth) {
                        result.set(ReplaceInfo.replaceRoot, true);
                    }
                    reset(stmt);
                }
            }
        } finally {
            reset(stmt);
            commitTransaction(dirInfo.wcDbDir.getWCRoot());
        }
        return result;
    }

    public static Structure<InstallInfo> readNodeInstallInfo(SVNWCDb db, File localAbspath, InstallInfo... fields) throws SVNException {
        final Structure<InstallInfo> result = Structure.obtain(InstallInfo.class, fields);
        
        final DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        final SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        final long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        final File localRelPath = dirInfo.localRelPath;
        
        if (result.hasField(InstallInfo.wcRootAbsPath)) {
            result.set(InstallInfo.wcRootAbsPath, dirInfo.wcDbDir.getWCRoot().getAbsPath());
        }
        
        begingReadTransaction(dirInfo.wcDbDir.getWCRoot());
        SVNSqlJetStatement stmt = null;
        try {
            stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            stmt.bindf("is", wcId, localRelPath);
            if (!stmt.next()) {
                nodeIsNotInstallable(localAbspath);
            } else {
                if (result.hasField(InstallInfo.changedDate)) {
                    result.set(InstallInfo.changedDate, getColumnDate(stmt, NODES__Fields.changed_date));
                }
                if (result.hasField(InstallInfo.sha1Checksum)) {
                    result.set(InstallInfo.sha1Checksum, getColumnChecksum(stmt, NODES__Fields.checksum));
                }
                if (result.hasField(InstallInfo.pristineProps)) {
                    result.set(InstallInfo.pristineProps, getColumnProperties(stmt, NODES__Fields.properties));
                }
            }
            reset(stmt);
        } finally {
            reset(stmt);
            commitTransaction(dirInfo.wcDbDir.getWCRoot());
        }
        
        return result;
    }
    
    public static long[] getMinAndMaxRevisions(SVNWCDb db, File localAbsPath) throws SVNException {

        DirParsedInfo dirInfo = db.obtainWcRoot(localAbsPath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        final long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        final File localRelpath = dirInfo.localRelPath;
        final long[] revs = new long[] { -1, -1, -1, -1 };

        SVNSqlJetSelectStatement stmt = new SVNSqlJetSelectStatement(sdb, SVNWCDbSchema.NODES) {
            
            @Override
            protected Object[] getWhere() throws SVNException {
                return new Object[] {wcId};
            }

            @Override
            protected boolean isFilterPassed() throws SVNException {
                
                String path = getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath);
                if ("".equals(localRelpath.getPath()) || path.equals(localRelpath.getPath()) || path.startsWith(localRelpath.getPath() + "/")) {
                    long depth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
                    if (depth != 0) {
                        return false;
                    }
                    String presence = getColumnString(SVNWCDbSchema.NODES__Fields.presence);
                    if (!("normal".equals(presence) || "incomplete".equals(presence))) {
                        return false;
                    }
                    long rev = getColumnLong(SVNWCDbSchema.NODES__Fields.revision);
                    long changedRev = getColumnLong(SVNWCDbSchema.NODES__Fields.revision);
                    if (getColumnBoolean(SVNWCDbSchema.NODES__Fields.file_external)) {
                        return false;
                    }
                    if (revs[0] < 0 || revs[0] > rev) {
                        revs[0] = rev;
                    }
                    if (revs[1] < 0 || revs[1] < rev) {
                        revs[1] = rev;
                    }
                    if (revs[2] < 0 || revs[2] > changedRev) {
                        revs[2] = changedRev;
                    }
                    if (revs[3] < 0 || revs[3] < changedRev) {
                        revs[3] = changedRev;
                    }
                }
                return false;
            }
        };
        try {
            while(stmt.next()) {}
        } finally {
            reset(stmt);
        }
                
        return revs;
    }
    
    public static Map<String, Structure<WalkerChildInfo>> readWalkerChildrenInfo(SVNWCDb db, File localAbspath, Map<String, Structure<WalkerChildInfo>> children) throws SVNException {
        
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        
        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_CHILDREN_WALKER_INFO);
        if (children == null) {
            children = new HashMap<String, Structure<WalkerChildInfo>>();
        }
        
        try {
            stmt.bindf("is", wcId, dirInfo.localRelPath);
            while(stmt.next()) {
                File childPath = SVNFileUtil.createFilePath(getColumnText(stmt, NODES__Fields.local_relpath));
                String childName = SVNFileUtil.getFileName(childPath);
                
                Structure<WalkerChildInfo> childInfo = children.get(childName);
                if (childInfo == null) {
                    childInfo = Structure.obtain(WalkerChildInfo.class);
                    children.put(childName, childInfo);
                }
                long opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                if (opDepth > 0) {
                    childInfo.set(WalkerChildInfo.status, SVNWCDb.getWorkingStatus(getColumnPresence(stmt)));
                } else {
                    childInfo.set(WalkerChildInfo.status, getColumnPresence(stmt));
                }
                childInfo.set(WalkerChildInfo.kind, getColumnKind(stmt, NODES__Fields.kind));            
            }
        } finally {
            reset(stmt);
        }
        
        return children;
    }

    public static boolean hasSwitchedSubtrees(SVNWCDb db, File localAbspath) throws SVNException {
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        String localRelPathStr = dirInfo.localRelPath.getPath().replace(File.separatorChar, '/');
        
        SqlJetDb sqljetDb = sdb.getDb();
        String parentReposRelpath = ""; 

        ISqlJetCursor cursor = null;
        try {
            sqljetDb.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            ISqlJetTable nodesTable = sqljetDb.getTable(SVNWCDbSchema.NODES.toString());
            String parentRelPath = null;
            Map<String, String> parents = new HashMap<String, String>();
            cursor = nodesTable.scope(null, new Object[] {wcId, localRelPathStr}, null);
            if ("".equals(localRelPathStr)) {
                if (!cursor.eof()) {
                    parentReposRelpath = cursor.getString(SVNWCDbSchema.NODES__Fields.repos_path.toString());
                    parents.put("", parentReposRelpath);
                    cursor.next();
                } 
            } else if (!"".equals(localRelPathStr)) {
                parentRelPath = localRelPathStr;
            }
            boolean matched = false;
            while(!cursor.eof()) {
                String rowRelPath = cursor.getString(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                boolean fileExternal = cursor.getBoolean(SVNWCDbSchema.NODES__Fields.file_external.toString());
                if (fileExternal) {
                } else if (rowRelPath.equals(parentRelPath)) {
                    long opDepth = cursor.getInteger(SVNWCDbSchema.NODES__Fields.op_depth.toString());
                    if (opDepth == 0) {
                        parents.put(rowRelPath, cursor.getString(SVNWCDbSchema.NODES__Fields.repos_path.toString()));
                    }
                } else if ("".equals(localRelPathStr) || rowRelPath.startsWith(localRelPathStr + "/")) {
                    matched = true;
                    long opDepth = cursor.getInteger(SVNWCDbSchema.NODES__Fields.op_depth.toString());
                    if (opDepth == 0) {
                        String rowReposRelpath = cursor.getString(SVNWCDbSchema.NODES__Fields.repos_path.toString());
                        String rowParentRelpath = cursor.getString(SVNWCDbSchema.NODES__Fields.parent_relpath.toString());
                        if ("dir".equals(cursor.getString(SVNWCDbSchema.NODES__Fields.kind.toString()))) {
                            parents.put(rowRelPath, rowReposRelpath);
                        }
                        parentReposRelpath = parents.get(rowParentRelpath);
                        String expectedReposRelpath = SVNPathUtil.append(parentReposRelpath, SVNPathUtil.tail(rowRelPath));
                        if (!rowReposRelpath.equals(expectedReposRelpath)) {
                            return true;
                        }
                    }
                } else if (matched) {
                    matched = false;
                    break;
                }
                cursor.next();
            }
        } catch (SqlJetException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
            SVNErrorManager.error(err, SVNLogType.WC);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SqlJetException e) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            try {
                sqljetDb.commit();
            } catch (SqlJetException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        return false;
    }

    public static boolean hasLocalModifications(SVNWCContext context, File localAbspath) throws SVNException {
        SVNWCDb db = (SVNWCDb) context.getDb();
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        String localRelPathStr = dirInfo.localRelPath.getPath().replace(File.separatorChar, '/');
        
        SqlJetDb sqljetDb = sdb.getDb();
        ISqlJetCursor cursor = null;
        boolean matched = false;
        try {
            sqljetDb.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            ISqlJetTable nodesTable = sqljetDb.getTable(SVNWCDbSchema.NODES.toString());
            cursor = nodesTable.scope(null, new Object[] {wcId, localRelPathStr}, null);
            // tree modifications
            while(!cursor.eof()) {
                String rowRelPath = cursor.getString(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                if ("".equals(localRelPathStr) || rowRelPath.equals(localRelPathStr) || rowRelPath.startsWith(localRelPathStr + "/")) {
                    matched = true;
                    long opDepth = cursor.getInteger(SVNWCDbSchema.NODES__Fields.op_depth.toString());
                    if (opDepth > 0) {
                        return true;
                    }
                } else if (matched) {
                    matched = false;
                    break;
                }
                cursor.next();
            }
            cursor.close();
            ISqlJetTable actualNodesTable = sqljetDb.getTable(SVNWCDbSchema.ACTUAL_NODE.toString());
            
            // prop mods
            cursor = actualNodesTable.scope(null, new Object[] {wcId, localRelPathStr}, null);
            while(!cursor.eof()) {
                String rowRelPath = cursor.getString(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                if ("".equals(localRelPathStr) || rowRelPath.equals(localRelPathStr) || rowRelPath.startsWith(localRelPathStr + "/")) {
                    matched = true;
                    if (cursor.getBlobAsArray(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString()) != null) {
                        return true;
                    }
                } else if (matched) {
                    matched = false;
                    break;
                }
                cursor.next();
            }
            cursor.close();
            
            // text mods.
            cursor = nodesTable.scope(null, new Object[] {wcId, localRelPathStr}, null);
            while(!cursor.eof()) {
                String rowRelPath = cursor.getString(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                if ("".equals(localRelPathStr) || rowRelPath.equals(localRelPathStr) || rowRelPath.startsWith(localRelPathStr + "/")) {
                    matched = true;
                    String kind = cursor.getString(SVNWCDbSchema.NODES__Fields.kind.toString());
                    if ("file".equals(kind)) {
                        String presence = cursor.getString(SVNWCDbSchema.NODES__Fields.presence.toString());
                        if ("normal".equals(presence) && !cursor.getBoolean(SVNWCDbSchema.NODES__Fields.file_external.toString())) {
                            File localFile = dirInfo.wcDbDir.getWCRoot().getAbsPath(new File(rowRelPath));
                            SVNFileType ft = SVNFileType.getType(localFile);
                            if (!(ft == SVNFileType.FILE || ft == SVNFileType.SYMLINK)) {
                                return true;
                            }
                            long size = cursor.getInteger(SVNWCDbSchema.NODES__Fields.translated_size.toString());
                            long date = cursor.getInteger(SVNWCDbSchema.NODES__Fields.last_mod_time.toString());
                            if (size != -1 && date != 0) {
                                if (size != SVNFileUtil.getFileLength(localFile)) {
                                    return true;
                                }
                                if (date/1000 == SVNFileUtil.getFileLastModified(localFile)) {
                                    cursor.next();
                                    continue;
                                }
                            }
                            if (context.isTextModified(localFile, false)) {
                                return true;
                            }
                        }
                    }
                } else if (matched) {
                    matched = false;
                    break;
                }
                cursor.next();
            }
            cursor.close();

        } catch (SqlJetException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
            SVNErrorManager.error(err, SVNLogType.WC);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SqlJetException e) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            try {
                sqljetDb.commit();
            } catch (SqlJetException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        return false;
    }

    public static boolean isSparseCheckout(SVNWCDb db, File localAbspath) throws SVNException {
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        SVNSqlJetDb sdb = dirInfo.wcDbDir.getWCRoot().getSDb();
        long wcId = dirInfo.wcDbDir.getWCRoot().getWcId();
        String localRelPathStr = dirInfo.localRelPath.getPath().replace(File.separatorChar, '/');
        
        SqlJetDb sqljetDb = sdb.getDb();

        ISqlJetCursor cursor = null;
        try {
            sqljetDb.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            ISqlJetTable nodesTable = sqljetDb.getTable(SVNWCDbSchema.NODES.toString());
            cursor = nodesTable.scope(null, new Object[] {wcId, localRelPathStr}, null);
            boolean matched = false;
            while(!cursor.eof()) {
                String rowRelPath = cursor.getString(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                boolean fileExternal = cursor.getBoolean(SVNWCDbSchema.NODES__Fields.file_external.toString());
                if (fileExternal) {
                } else if ("".equals(localRelPathStr) || rowRelPath.equals(localRelPathStr) || rowRelPath.startsWith(localRelPathStr + "/")) {
                    matched = true;
                    long opDepth = cursor.getInteger(SVNWCDbSchema.NODES__Fields.op_depth.toString());
                    if (opDepth == 0) {
                        SVNWCDbStatus presence = SvnWcDbStatementUtil.parsePresence(cursor.getString(SVNWCDbSchema.NODES__Fields.presence.toString()));
                        if (presence == SVNWCDbStatus.Excluded || presence == SVNWCDbStatus.ServerExcluded) {
                            return true;
                        }
                        SVNDepth depth = SvnWcDbStatementUtil.parseDepth(cursor.getString(SVNWCDbSchema.NODES__Fields.depth.toString()));
                        if (depth != SVNDepth.UNKNOWN && depth != SVNDepth.INFINITY) {
                            return true;
                        }
                    }
                } else if (matched) {
                    matched = false;
                    break;
                }
                cursor.next();
            }
        } catch (SqlJetException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
            SVNErrorManager.error(err, SVNLogType.WC);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SqlJetException e) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            try {
                sqljetDb.commit();
            } catch (SqlJetException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        return false;
    }

}
