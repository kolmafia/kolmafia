package org.tmatesoft.svn.core.internal.wc17.db;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.wc2.SvnChecksum;

public class SvnWcDbStatementUtil {
    
    private static final EnumMap<SVNWCDbStatus, String> presenceMap = new EnumMap<SVNWCDbStatus, String>(SVNWCDbStatus.class);
    private static final HashMap<String, SVNWCDbStatus> presenceMap2 = new HashMap<String, SVNWCDbStatus>();
    
    static {
        presenceMap.put(SVNWCDbStatus.Normal, "normal");
        presenceMap.put(SVNWCDbStatus.Excluded, "excluded");
        presenceMap.put(SVNWCDbStatus.ServerExcluded, "absent");
        presenceMap.put(SVNWCDbStatus.NotPresent, "not-present");
        presenceMap.put(SVNWCDbStatus.Incomplete, "incomplete");
        presenceMap.put(SVNWCDbStatus.BaseDeleted, "base-deleted");

        presenceMap2.put("normal", SVNWCDbStatus.Normal);
        presenceMap2.put("absent", SVNWCDbStatus.ServerExcluded);
        presenceMap2.put("excluded", SVNWCDbStatus.Excluded);
        presenceMap2.put("not-present", SVNWCDbStatus.NotPresent);
        presenceMap2.put("incomplete", SVNWCDbStatus.Incomplete);
        presenceMap2.put("base-deleted", SVNWCDbStatus.BaseDeleted);
    };
    
    private static final EnumMap<SVNWCDbKind, String> kindMap = new EnumMap<SVNWCDbKind, String>(SVNWCDbKind.class);
    private static final HashMap<String, SVNWCDbKind> kindMap2 = new HashMap<String, SVNWCDbKind>();

    static {
        kindMap.put(SVNWCDbKind.File, "file");
        kindMap.put(SVNWCDbKind.Dir, "dir");
        kindMap.put(SVNWCDbKind.Symlink, "symlink");
        kindMap.put(SVNWCDbKind.Unknown, "unknown");

        kindMap2.put("file", SVNWCDbKind.File);
        kindMap2.put("dir", SVNWCDbKind.Dir);
        kindMap2.put("symlink", SVNWCDbKind.Symlink);
        kindMap2.put("unknown", SVNWCDbKind.Unknown);
    };
    
    public static SVNSqlJetStatement bindf(SVNSqlJetStatement stmt, String format, Object... binds) throws SVNException {
        if (binds != null) {
            for (int i = 0; i < binds.length; i++) {
                if (binds[i] instanceof SVNWCDbStatus) {
                    binds[i] = getPresenceText((SVNWCDbStatus) binds[i]);
                } else if (binds[i] instanceof SVNWCDbKind) {
                    binds[i] = getKindText((SVNWCDbKind) binds[i]);
                } else if (binds[i] instanceof File) {
                    binds[i] = SVNFileUtil.getFilePath((File) binds[i]);
                } else if (binds[i] instanceof SVNDate) {
                    binds[i] = ((SVNDate) binds[i]).getTimeInMicros();
                } else if (binds[i] instanceof SVNDepth) {
                    binds[i] = ((SVNDepth) binds[i]).toString();
                }
            }
        }
        
        stmt.bindf(format, binds);
        return stmt;
    }
    
    public static String getPresenceText(SVNWCDbStatus status) {
        return presenceMap.get(status);
    }
    
    public static String getKindText(SVNWCDbKind kind) {
        return kindMap.get(kind);
    }
    
    public static SVNWCDbStatus parsePresence(String presenceString) {
        return presenceMap2.get(presenceString);
    }

    public static SVNWCDbKind parseKind(String kindString) {
        return kindMap2.get(kindString);
    }
    
    public static SVNDepth parseDepth(String depthStr) {
        SVNDepth d = null;
        if (depthStr != null) {
            d = SVNDepth.fromString(depthStr);
        }
        if (d == null) {
            d = SVNDepth.UNKNOWN;
        }
        return d;
    }
    
    public static SVNWCDbStatus getColumnPresence(SVNSqlJetStatement stmt) throws SVNException {
        return getColumnToken(stmt, SVNWCDbSchema.NODES__Fields.presence, presenceMap2);
    }
    
    public static SVNWCDbStatus getColumnPresence(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return getColumnToken(stmt, f, presenceMap2);
    }
    
    public static SVNWCDbKind getColumnKind(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return getColumnToken(stmt, f, kindMap2);
    }

    public static SVNProperties getColumnProperties(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.getColumnProperties(f);
    }
    
    public static boolean hasColumnProperties(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.hasColumnProperties(f);
    }

    public static String getColumnText(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.getColumnString(f);
    }
    
    public static File getColumnPath(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        String path = stmt.getColumnString(f);
        if (path != null) {
            return SVNFileUtil.createFilePath(path);
        }
        return null;
    }

    public static SVNDepth getColumnDepth(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return parseDepth(stmt.getColumnString(f));
    }

    public static boolean isColumnNull(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.isColumnNull(f);
    }

    public static long getColumnInt64(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.getColumnLong(f);
    }

    public static byte[] getColumnBlob(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.getColumnBlob(f);
    }

    public static boolean getColumnBoolean(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        return stmt.getColumnBoolean(f);
    }

    public static long getColumnRevNum(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        if (isColumnNull(stmt, f))
            return ISVNWCDb.INVALID_REVNUM;
        return (int) getColumnInt64(stmt, f);
    }

    public static long getTranslatedSize(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        if (isColumnNull(stmt, f))
            return ISVNWCDb.INVALID_FILESIZE;
        return getColumnInt64(stmt, f);
    }

    public static <T extends Enum<T>> T getColumnToken(SVNSqlJetStatement stmt, Enum<?> f, Map<String, T> tokenMap) throws SVNException {
        return tokenMap.get(getColumnText(stmt, f));
    }

    public static SvnChecksum getColumnChecksum(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        String str = stmt.getColumnString(f);
        if (str == null) {
            return null;
        }        
        return SvnChecksum.fromString(str);
    }
    
    public static SVNDate getColumnDate(SVNSqlJetStatement stmt, Enum<?> f) throws SVNException {
        if (isColumnNull(stmt, f)) {
            return null;
        }
        return SVNWCUtils.readDate(getColumnInt64(stmt, f));
    }
    
    public static SVNWCDbLock getLockFromColumns(SVNSqlJetStatement stmt, Enum<?> tokenField, Enum<?> owner, Enum<?> comment, Enum<?> date) throws SVNException {
        if (isColumnNull(stmt, tokenField)) {
            return null;
        }
        SVNWCDbLock lock = new SVNWCDbLock();
        lock.token = getColumnText(stmt, tokenField);
        if (!isColumnNull(stmt, owner)) {
            lock.owner = getColumnText(stmt, owner);
        }
        if (!isColumnNull(stmt, SVNWCDbSchema.LOCK__Fields.lock_comment)) {
            lock.comment = getColumnText(stmt, comment);
        }
        if (!isColumnNull(stmt, SVNWCDbSchema.LOCK__Fields.lock_date)) {
            lock.date = getColumnDate(stmt, date);
        }
        return lock;

    }
    
    public static void reset(SVNSqlJetStatement stmt) throws SVNException {
        if (stmt != null) {
            stmt.reset();
        }
    }
}
