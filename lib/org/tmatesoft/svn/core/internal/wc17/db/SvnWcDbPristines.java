package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetTransaction;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.PRISTINE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnChecksum;

public class SvnWcDbPristines extends SvnWcDbShared {
	
	private static final String PRISTINE_STORAGE_EXT = ".svn-base";
	
	 private static class RemoveUnreferencedPristine implements SVNSqlJetTransaction {

	        public SvnChecksum sha1_checksum;
	        public File pristineAbsPath;
	        SVNWCDbRoot root;
	        
	        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
	        	SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_PRISTINE_IF_UNREFERENCED);
                try {
                    stmt.bindf("s", sha1_checksum);
                    Long affectedRows = stmt.done();
                    if (affectedRows > 0) {
                        pristineRemoveFile(true);
                    }
                } finally {
                    stmt.reset();
                }
	        }
	        
	        /* Remove the file at FILE_ABSPATH in such a way that we could re-create a
	         * new file of the same name at any time thereafter.
	         *
	         * On Windows, the file will not disappear immediately from the directory if
	         * it is still being read so the best thing to do is first rename it to a
	         * unique name. */
	        private void pristineRemoveFile(boolean ignoreEnoent) throws SVNException { 
	        	if (SVNFileUtil.isWindows) {
		    		File temDirAbsPath = getPristineTempDir(root, root.getAbsPath());
		    		File tempAbsPath = SVNFileUtil.createUniqueFile(temDirAbsPath, "pristine", ".tmp", false);
		    		try {
		    			SVNFileUtil.rename(pristineAbsPath, tempAbsPath);
		    			pristineAbsPath = tempAbsPath;
		    		}
		    		catch (SVNException e){
		    			if (!(ignoreEnoent && SVNFileType.getType(tempAbsPath) == SVNFileType.NONE))
		    				throw e;
		    		}
	        	}
	    		SVNFileUtil.deleteFile(pristineAbsPath);
	    	}

	    }
	 
	 public static void cleanupPristine(SVNWCDbRoot root, File localAbsPath) throws SVNException {
	    	SVNSqlJetStatement selectList = root.getSDb().getStatement(SVNWCDbStatements.SELECT_UNREFERENCED_PRISTINES);
	        try {
	            while(selectList.next()) {
	                SvnChecksum sha1_checksum = SvnChecksum.fromString(selectList.getColumnString(PRISTINE__Fields.checksum));
	                removePristineIfUnreferenced(root, localAbsPath, sha1_checksum);
	            }
	        } finally {
	        	selectList.reset();
	        }
	    }
	    
	 private static void removePristineIfUnreferenced(SVNWCDbRoot root, File localAbsPath, SvnChecksum sha1_checksum) throws SVNException {
	    	RemoveUnreferencedPristine rup = new RemoveUnreferencedPristine();
	    	rup.sha1_checksum = sha1_checksum;
	    	rup.pristineAbsPath = getPristineFileName(root, sha1_checksum, false);
	    	rup.root = root;
	    	root.getSDb().runTransaction(rup);
	 }
	    
	 public static File getPristineTempDir(SVNWCDbRoot root, File wcRootAbsPath) throws SVNException {
	        return SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(root.getAbsPath(), SVNFileUtil.getAdminDirectoryName()), ISVNWCDb.PRISTINE_TEMPDIR_RELPATH);
	 }
	 
	 public static File getPristineFuturePath(SVNWCDbRoot root, SvnChecksum sha1Checksum) {
		 return getPristineFileName(root, sha1Checksum, false);
	 }
	    
	 public static File getPristineFileName(SVNWCDbRoot root, SvnChecksum sha1Checksum, boolean createSubdir) {
	        /* ### code is in transition. make sure we have the proper data. */
	        assert (root != null);
	        assert (sha1Checksum != null);
	        assert (sha1Checksum.getKind() == SvnChecksum.Kind.sha1);

	        /*
	         * ### need to fix this to use a symbol for ".svn". we don't need ### to
	         * use join_many since we know "/" is the separator for ### internal
	         * canonical paths
	         */
	        File base_dir_abspath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(root.getAbsPath(), SVNFileUtil.getAdminDirectoryName()), ISVNWCDb.PRISTINE_STORAGE_RELPATH);

	        String hexdigest = sha1Checksum.getDigest();

	        /* We should have a valid checksum and (thus) a valid digest. */
	        assert (hexdigest != null);

	        /* Get the first two characters of the digest, for the subdir. */
	        String subdir = hexdigest.substring(0, 2);

	        if (createSubdir) {
	            File subdirAbspath = SVNFileUtil.createFilePath(base_dir_abspath, subdir);
	            subdirAbspath.mkdirs();
	            /*
	             * Whatever error may have occurred... ignore it. Typically, this
	             * will be "directory already exists", but if it is something
	             * different*, then presumably another error will follow when we try
	             * to access the file within this (missing?) pristine subdir.
	             */
	        }

	        /* The file is located at DIR/.svn/pristine/XX/XXYYZZ... */
	        return SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(base_dir_abspath, subdir), hexdigest + PRISTINE_STORAGE_EXT);
	 }
	    
	 public static boolean checkPristine(SVNWCDbRoot root, SvnChecksum sha1Checksum) throws SVNException {
	        boolean haveRow = false;
	        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_PRISTINE_MD5_CHECKSUM);
	        stmt.bindf("s", sha1Checksum);
	        try {
	            haveRow = stmt.next();
	        } finally {
	            stmt.reset();
	        }
	        if (haveRow) {
	            File pristineAbspath = getPristineFileName(root, sha1Checksum, false);
	            SVNNodeKind kindOnDisk = SVNFileType.getNodeKind(SVNFileType.getType(pristineAbspath));
	            if (kindOnDisk != SVNNodeKind.FILE) {
	                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, "The pristine text with checksum ''{0}'' was found in the DB but not on disk", sha1Checksum);
	                SVNErrorManager.error(err, SVNLogType.WC);
	            }
	        }
	        return haveRow;
	 }

	 public static SvnChecksum getPristineSHA1(SVNWCDbRoot root, SvnChecksum md5Checksum) throws SVNException {
	        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_PRISTINE_SHA1_CHECKSUM);
	        try {
	            stmt.bindChecksum(1, md5Checksum);
	            boolean have_row = stmt.next();
	            if (!have_row) {
	                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, "The pristine text with MD5 checksum ''{0}'' not found", md5Checksum.toString());
	                SVNErrorManager.error(err, SVNLogType.WC);
	                return null;
	            }
	            SvnChecksum sha1_checksum = getColumnChecksum(stmt, PRISTINE__Fields.checksum);
	            assert (sha1_checksum.getKind() == SvnChecksum.Kind.sha1);
	            return sha1_checksum;
	        } finally {
	            stmt.reset();
	        }
	    }
	 
	 public static File getPristinePath(SVNWCDbRoot root, SvnChecksum sha1Checksum) throws SVNException {
	        boolean present = checkPristine(root, sha1Checksum);
	        if (!present) {
	            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, "Pristine text not found");
	            SVNErrorManager.error(err, SVNLogType.WC);
	        }
	        return getPristineFileName(root, sha1Checksum, false);
	    }
	 
	 public static void removePristine(SVNWCDbRoot root, SvnChecksum sha1Checksum) throws SVNException {
	        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.LOOK_FOR_WORK);
	        boolean haveRow;
	        try {
	            haveRow = stmt.next();
	        } finally {
	            stmt.reset();
	        }
	        if (haveRow) {
	            return;
	        }
	        pristineRemove(root, sha1Checksum);
	    }

    private static void pristineRemove(SVNWCDbRoot root, SvnChecksum sha1Checksum) throws SVNException {
        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_PRISTINE);
        try {
            stmt.bindChecksum(1, sha1Checksum);

            if (stmt.done() != 0) {
                File pristineAbspath = getPristineFileName(root, sha1Checksum, true);
                SVNFileUtil.deleteFile(pristineAbspath);
            }
        } finally {
            stmt.reset();
        }
    }

    public static void installPristine(SVNWCDbRoot root, File tempfileAbspath, SvnChecksum sha1Checksum, SvnChecksum md5Checksum) throws SVNException {
        File pristineAbspath = getPristineFileName(root, sha1Checksum, true);
        if (pristineAbspath.isFile()) {
            SVNFileUtil.deleteFile(tempfileAbspath);
            return;
        }
        SVNFileUtil.rename(tempfileAbspath, pristineAbspath);
        long size = pristineAbspath.length();
        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_PRISTINE);
        try {
            stmt.bindChecksum(1, sha1Checksum);
            stmt.bindChecksum(2, md5Checksum);
            stmt.bindLong(3, size);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }
	
	public static InputStream readPristine(SVNWCDbRoot root, File wcRootAbsPath, SvnChecksum sha1Checksum) throws SVNException {
        File pristine_abspath = getPristineFileName(root, sha1Checksum, false);
        return SVNFileUtil.openFileForReading(pristine_abspath);
    }
	
	
	public static void fixPristinesRefCount(SVNWCDbRoot root) throws SVNException {
	    Map<String, Long> refcountTable = new HashMap<String, Long>();
	    begingWriteTransaction(root);
	    try {
	        ISqlJetTable nodesTable = root.getSDb().getDb().getTable(SVNWCDbSchema.NODES.toString());
	        ISqlJetCursor cursor = nodesTable.open();
	        try {
    	        while(!cursor.eof()) {
    	            long opDepth = cursor.getInteger(NODES__Fields.op_depth.toString());
    	            if (opDepth == 0) {
        	            String checksum = cursor.getString(NODES__Fields.checksum.toString());
        	            if (refcountTable.containsKey(checksum)) {
        	                refcountTable.put(checksum, refcountTable.get(checksum) + 1);
        	            } else {
        	                refcountTable.put(checksum, 1l);
        	            }
    	            }
    	            cursor.next();
    	        }
	        } finally {
	            cursor.close();
	        }

	        ISqlJetTable pTable = root.getSDb().getDb().getTable(SVNWCDbSchema.PRISTINE.toString());
	        
	        for (Iterator<String> checksums = refcountTable.keySet().iterator(); checksums.hasNext();) {
	            String checksum = checksums.next();
                cursor = pTable.lookup(null, checksum);
                try {
                    if (!cursor.eof()) {
                        long refCount = cursor.getInteger(PRISTINE__Fields.refcount.toString());
                        if (refCount != refcountTable.get(checksum)) {
                            Map<String, Object> value = new HashMap<String, Object>();
                            cursor.updateByFieldNames(value);
                        }
                        checksums.remove();
                    } 
                } finally {
                    cursor.close();
                }
            }
	    } catch (SqlJetException e) {
	        rollbackTransaction(root);
	        SVNSqlJetDb.createSqlJetError(e);
        } finally {
	        commitTransaction(root);
	    }
	}

    public static void checkPristineChecksumRefcounts(SVNWCDbRoot root) throws SVNException {
        final Map<SvnChecksum, Integer> correctChecksumRefcounts = calculateCorrectChecksumRefcounts(root);
        final Map<SvnChecksum, Integer> checksumRefcountsFromTable = loadChecksumsRefcountsFromTable(root);

        for (Map.Entry<SvnChecksum, Integer> entry : checksumRefcountsFromTable.entrySet()) {
            final SvnChecksum sha1Checksum = entry.getKey();
            final Integer refCountInteger = entry.getValue();
            final Integer correctRefCountInteger = correctChecksumRefcounts.get(sha1Checksum);

            final int refCount = refCountInteger == null ? 0 : refCountInteger;
            final int correctRefCount = correctRefCountInteger == null ? 0 : correctRefCountInteger;

            if (correctRefCount != refCount) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "Working copy ''{0}'' is corrupted: {1} table contains incorrect ''refcount'' value {2} for checksum {3} (instead of {4})",
                        root.getAbsPath().getAbsolutePath().replace('/', File.separatorChar), SVNWCDbSchema.PRISTINE.name(), refCount, sha1Checksum, correctRefCount);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

        for (Map.Entry<SvnChecksum, Integer> entry : correctChecksumRefcounts.entrySet()) {
            final SvnChecksum sha1Checksum = entry.getKey();
            if (!checksumRefcountsFromTable.containsKey(sha1Checksum)) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "Working copy ''{0}'' is corrupted: checksum {1} that is present in {2} table is not listed in {3} table",
                        root.getAbsPath().getAbsolutePath().replace('/', File.separatorChar), sha1Checksum, SVNWCDbSchema.NODES.name(), SVNWCDbSchema.PRISTINE.name());
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
    }

    private static Map<SvnChecksum, Integer> calculateCorrectChecksumRefcounts(SVNWCDbRoot root) throws SVNException {
        Map<SvnChecksum, Integer> checksumToRefCount = new HashMap<SvnChecksum, Integer>();

        final SqlJetDb db = root.getSDb().getDb();
        try {
            final ISqlJetTable nodesTable = db.getTable(SVNWCDbSchema.NODES.name());
            db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            final ISqlJetCursor cursor = nodesTable.open();

            for (; !cursor.eof(); cursor.next()) {
                String sha1ChecksumString = cursor.getString(SVNWCDbSchema.NODES__Fields.checksum.name());
                if (sha1ChecksumString == null) {
                    continue;
                }
                SvnChecksum sha1Checksum = SvnChecksum.fromString(sha1ChecksumString);

                Integer refCount = checksumToRefCount.get(sha1Checksum);
                int incrementedRefCount = refCount == null ? 1 : refCount + 1;
                checksumToRefCount.put(sha1Checksum, incrementedRefCount);
            }
            cursor.close();
        } catch (SqlJetException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, e);
            SVNErrorManager.error(errorMessage, e, SVNLogType.WC);
        } finally {
            try {
                db.commit();
            } catch (SqlJetException ignore) {
            }
        }

        return checksumToRefCount;
    }

    private static Map<SvnChecksum, Integer> loadChecksumsRefcountsFromTable(SVNWCDbRoot root) throws SVNException {
        Map<SvnChecksum, Integer> checksumToRefCount = new HashMap<SvnChecksum, Integer>();

        final SqlJetDb db = root.getSDb().getDb();
        try {
            final ISqlJetTable pristineTable = db.getTable(SVNWCDbSchema.PRISTINE.name());
            db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            final ISqlJetCursor cursor = pristineTable.open();

            for (; !cursor.eof(); cursor.next()) {
                String sha1ChecksumString = cursor.getString(PRISTINE__Fields.checksum.name());
                if (sha1ChecksumString == null) {
                    continue;
                }
                SvnChecksum sha1Checksum = SvnChecksum.fromString(sha1ChecksumString);
                long refcount = cursor.getInteger(PRISTINE__Fields.refcount.name());

                checksumToRefCount.put(sha1Checksum, (int)refcount);
            }
            cursor.close();
        } catch (SqlJetException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, e);
            SVNErrorManager.error(errorMessage, e, SVNLogType.WC);
        } finally {
            try {
                db.commit();
            } catch (SqlJetException ignore) {
            }
        }

        return checksumToRefCount;
    }
}

