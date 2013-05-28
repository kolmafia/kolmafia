package org.tmatesoft.svn.core.internal.wc2.ng;
 
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.*;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpgrade;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
 
public class SvnNgUpgradeSDb {
	private static String PRISTINE_STORAGE_EXT = ".svn-base";
	/* Number of characters in a pristine file basename, in WC format <= 28. */
	private static int PRISTINE_BASENAME_OLD_LEN = 40;
	
    /* Return a string indicating the released version (or versions) of Subversion that used WC format number WC_FORMAT, or some other
     * suitable string if no released version used WC_FORMAT.
     *
     * ##It's not ideal to encode this sort of knowledge in this low-level library.  On the other hand, it doesn't need to be updated often and
     * should be easily found when it does need to be updated.  */
    private static String versionStringFromFormat(int wcFormat) {
        switch (wcFormat) {
            case 4: return "<=1.3";
            case 8: return "1.4";
            case 9: return "1.5";
            case 10: return "1.6";
        }
      return "(unreleased development version)";
    }
    
    public static int upgrade(final File wcRootAbsPath, final SVNSqlJetDb sDb, int startFormat) throws SVNException {
        int resultFormat = 0;
        File bumpWcRootAbsPath = wcRootAbsPath;
        
        if (startFormat < SVNWCContext.WC_NG_VERSION /* 12 */) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Working copy ''{0}'' is too old (format {1}, created by Subversion {2})", 
            		wcRootAbsPath, startFormat, versionStringFromFormat(startFormat));
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        /* Early WCNG formats no longer supported. */
        if (startFormat < 19) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, 
                    "Working copy ''{0}'' is an old development version (format {1}); to upgrade it, use a format 18 client, then " +
                    "use ''tools/dev/wc-ng/bump-to-19.py'', then use the current client",
                    wcRootAbsPath, startFormat);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        /* ##need lock-out. only one upgrade at a time. note that other code cannot use this un-upgraded database until we finish the upgrade.  */
 
        /* Note: none of these have "break" statements; the fall-through is intentional. */
        switch (startFormat)
        {
          case 19:
        	  runBump(sDb, wcRootAbsPath, new bumpTo20());
        	  resultFormat = 20;
          case 20:
        	  runBump(sDb, wcRootAbsPath, new bumpTo21());
        	  resultFormat = 21;
          case 21:
        	  runBump(sDb, wcRootAbsPath, new bumpTo22());
        	  resultFormat = 22;
          case 22:
        	  runBump(sDb, wcRootAbsPath, new bumpTo23());
        	  resultFormat = 23;
          case 23:
        	  runBump(sDb, wcRootAbsPath, new bumpTo24());
        	  resultFormat = 24;
          case 24:
        	  runBump(sDb, wcRootAbsPath, new bumpTo25());
        	  resultFormat = 25;
          case 25:
        	  runBump(sDb, wcRootAbsPath, new bumpTo26());
        	  resultFormat = 26;
          case 26:
        	  runBump(sDb, wcRootAbsPath, new bumpTo27());
        	  resultFormat = 27;
          case 27:
        	  runBump(sDb, wcRootAbsPath, new bumpTo28());
        	  resultFormat = 28;
          case 28:
        	  runBump(sDb, wcRootAbsPath, new bumpTo29());
        	  resultFormat = 29;
 
          /* ##future bumps go here.  */
          //#if 0
                //case XXX-1:
                /* Revamp the recording of tree conflicts.  */
                //SVN_ERR(svn_sqlite__with_transaction(sdb, bump_to_XXX, &bb, scratch_pool));
                //*result_format = XXX;
                /* FALLTHROUGH  */
           //#endif*/
           
        }
 
        /*
      #ifdef SVN_DEBUG
      if (*result_format != start_format)
        {
          int schema_version;
          SVN_ERR(svn_sqlite__read_schema_version(&schema_version, sdb, scratch_pool));
 
          /* If this assertion fails the schema isn't updated correctly /
          SVN_ERR_ASSERT(schema_version == *result_format);
        }
      #endif
      */
 
      /* Zap anything that might be remaining or escaped our notice.  */
      SvnOldUpgrade.wipeObsoleteFiles(wcRootAbsPath);
 
      return resultFormat;
    }
    
    private static void migrateTreeConflictData(SVNSqlJetDb sDb) throws SVNException
    {
        SVNSqlJetStatement stmt = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields>(sDb, SVNWCDbSchema.ACTUAL_NODE) {
            protected void defineFields() {
                fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id);
                fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
                fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
            }
            protected boolean isFilterPassed() throws SVNException {
                return !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
            }
        };
        /* Iterate over each node which has a set of tree conflicts, then insert all of them into the new schema.  */
        try {
            while (stmt.next()) {
                migrateSingleTreeConflictData(sDb, 
                        stmt.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data),
                        stmt.getColumnLong(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id),
                        SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath)));
            }
        } finally {
            stmt.reset();
        }
        
        /* Erase all the old tree conflict data.  */
        stmt = new SVNSqlJetUpdateStatement(sDb, SVNWCDbSchema.ACTUAL_NODE) {
            public Map<String, Object> getUpdateValues() throws SVNException {
                Map<String, Object> rowValues = getRowValues();
                rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), null);
                return rowValues;
            }
        };
        try {
            stmt.exec();
        } finally {
            stmt.reset();
        }
    }
        
    private static void migrateSingleTreeConflictData(SVNSqlJetDb sDb, String treeConflictData, long wcId, File localRelPath) throws SVNException {
        Map conflicts = SVNTreeConflictUtil.readTreeConflicts(localRelPath, treeConflictData);
        for (Iterator keys = conflicts.keySet().iterator(); keys.hasNext();) {
            File entryPath = (File)keys.next();
            SVNTreeConflictDescription conflict = (SVNTreeConflictDescription) conflicts.get(entryPath);
            
            //String conflictRelpath = SVNFileUtil.getFilePath(
            //      SVNFileUtil.createFilePath(localRelPath, SVNFileUtil.getBasePath(conflict.getPath())));
                 
            /* See if we need to update or insert an ACTUAL node. */
            SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE); 
            stmt.bindf("is", wcId, conflict.getPath());
            
            boolean haveRow = false;
            try {
                haveRow = stmt.next();
            } finally {
                stmt.reset();
            }
            
            if (haveRow) {
                /* There is an existing ACTUAL row, so just update it. */
                stmt = sDb.getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CONFLICT_DATA);
            } else {
                 /* We need to insert an ACTUAL row with the tree conflict data. */
                stmt = sDb.getStatement(SVNWCDbStatements.INSERT_ACTUAL_CONFLICT_DATA);
            }
            
            stmt.bindf("iss", wcId, conflict.getPath(), SVNTreeConflictUtil.getSingleTreeConflictData(conflict));
            if (!haveRow)
                stmt.bindString(4, SVNFileUtil.getFilePath(localRelPath));
            
            try {
                stmt.exec();
            } finally {
                stmt.reset();
            }
            
        }
    }
    
    private static void setVersion(SVNSqlJetDb sDb, int version) throws SVNException {
        try {
            sDb.getDb().pragma("pragma user_version = " + version);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
    }
    
    private interface Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException;
    }
    
    private static void runBump(SVNSqlJetDb sDb, File wcRootAbsPath, Bumpable bump) throws SVNException {
    	sDb.beginTransaction(SqlJetTransactionMode.WRITE);
        try {
        	bump.bumpTo(sDb, wcRootAbsPath);
        } catch (SVNException e) {
            sDb.rollback();
            throw e;
        }
        finally {
            sDb.commit();
        }
    }
    
    /* UPDATE tableName SET checksum=(SELECT checksum FROM pristine WHERE md5_checksum=tableName.checksum)
	   WHERE EXISTS(SELECT 1 FROM pristine WHERE md5_checksum=WORKING_NODE.checksum); */
    private static class UpdateChecksum {
    	private SVNSqlJetDb sDb;
    	private Enum<?> tableName;
    	
    	public UpdateChecksum(SVNSqlJetDb sDb, Enum<?> tableName) {
            this.sDb = sDb;
            this.tableName = tableName;
        }
    	
    	public void run() throws SVNException {
	    	SVNSqlJetUpdateStatement stmt = new SVNSqlJetUpdateStatement(sDb, tableName) {
	    		private SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.PRISTINE__Fields> select = 
	    				new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.PRISTINE__Fields>(sDb, SVNWCDbSchema.PRISTINE) {
	    					protected boolean isFilterPassed() throws SVNException {
	    						return ((String)getBind(1)).equals(getColumnString(SVNWCDbSchema.PRISTINE__Fields.md5_checksum));
	    					}
	    					protected void defineFields() {
	    		                fields.add(SVNWCDbSchema.PRISTINE__Fields.checksum);
	    		            }
	    					protected Object[] getWhere() throws SVNException {
	    				        return new Object[] {};
	    				    }
	    				};
	    		
	    		public Map<String, Object> getUpdateValues() throws SVNException {
	    			return null;
	    		}
	    				
	    		public long exec() throws SVNException {
	    	        long n = 0;
	    	        try {
	    	            statementStarted();
	    	            while (next()) {
	    	            	Map<String, Object> rowValues = getRowValues();
	    	            	String checksum = (String)rowValues.get("checksum");
	    	            	if (checksum == null)
	    	            		continue;
	    	            	select.bindString(1, checksum);
	    	            	try {
	    	            		if (select.next()) {
	    	            			rowValues.put("checksum", select.getColumnString(SVNWCDbSchema.PRISTINE__Fields.checksum));
	    	            		} else {
	    	            			continue;
	    	            		}
	    	            	} finally {
	    	            		select.reset();
	    	            	}
	    	            	
	    	                update(rowValues);
	    	                n++;
	    	            }
	    	            statementCompleted(null);
	    	        } catch (SqlJetException e) {
	    	            statementCompleted(e);
	    	            SVNSqlJetDb.createSqlJetError(e);
	    	        }
	    	        return n;
	    	    }
	    	};
	    	
	    	try {
				stmt.exec();
			} finally {
				stmt.reset();
			}
    	}
    }
    
    private static class bumpTo20 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	try {
	    		sDb.getDb().createTable("CREATE TABLE NODES ( wc_id  INTEGER NOT NULL REFERENCES WCROOT (id), local_relpath  TEXT NOT NULL, op_depth INTEGER NOT NULL, "
	                + "  parent_relpath  TEXT, repos_id  INTEGER REFERENCES REPOSITORY (id), repos_path  TEXT, revision  INTEGER, presence  TEXT NOT NULL, "
	                + "  moved_here  INTEGER, moved_to  TEXT, kind  TEXT NOT NULL, properties  BLOB, depth  TEXT, checksum  TEXT REFERENCES PRISTINE (checksum), symlink_target  TEXT, "
	                + "  changed_revision  INTEGER, changed_date INTEGER, changed_author TEXT, translated_size  INTEGER, last_mod_time  INTEGER, "
	                + "  dav_cache  BLOB, file_external  TEXT, PRIMARY KEY (wc_id, local_relpath, op_depth) ); ");
	    		sDb.getDb().createIndex("CREATE INDEX I_NODES_PARENT ON NODES (wc_id, parent_relpath, op_depth); ");
	    		
	    		/*
	    		UPDATE BASE_NODE SET checksum=(SELECT checksum FROM pristine WHERE md5_checksum=BASE_NODE.checksum)
				WHERE EXISTS(SELECT 1 FROM pristine WHERE md5_checksum=BASE_NODE.checksum);
	    		 */
	    		UpdateChecksum uc = new UpdateChecksum(sDb, SVNWCDbSchema.BASE_NODE);
	    		uc.run();
	    
		    	/*
	    		UPDATE WORKING_NODE SET checksum=(SELECT checksum FROM pristine WHERE md5_checksum=WORKING_NODE.checksum)
				WHERE EXISTS(SELECT 1 FROM pristine WHERE md5_checksum=WORKING_NODE.checksum);
		    	 */
	    		uc = new UpdateChecksum(sDb, SVNWCDbSchema.WORKING_NODE);
	    		uc.run();
	    		
	    		/*
				INSERT INTO NODES (
					wc_id, local_relpath, op_depth, parent_relpath,
					repos_id, repos_path, revision,
					presence, depth, moved_here, moved_to, kind,
					changed_revision, changed_date, changed_author,
					checksum, properties, translated_size, last_mod_time,
					dav_cache, symlink_target, file_external )
					SELECT wc_id, local_relpath, 0 /*op_depth/, parent_relpath,
					repos_id, repos_relpath, revnum,
					presence, depth, NULL /*moved_here/, NULL /*moved_to/, kind,
					changed_rev, changed_date, changed_author,
					checksum, properties, translated_size, last_mod_time,
					dav_cache, symlink_target, file_external
				FROM BASE_NODE;
				*/
	    		SVNSqlJetInsertStatement stmt = new SVNSqlJetInsertStatement(sDb, SVNWCDbSchema.NODES) {
	    			private SVNSqlJetSelectStatement select = new SVNSqlJetSelectStatement(sDb.getTemporaryDb(), SVNWCDbSchema.BASE_NODE) {};

	    		    public long exec() throws SVNException {
	    		        try {
	    		            int n = 0;
	    		            while (select.next()) {
	    		                try {
	    		                    table.insertByFieldNamesOr(null, getInsertValues());
	    		                    n++;
	    		                } catch (SqlJetException e) {
	    		                    SVNSqlJetDb.createSqlJetError(e);
	    		                    return -1;
	    		                }
	    		            }
	    		            return n;
	    		        } finally {
	    		            select.reset();
	    		        }
	    		    }
	    		    
	    		    protected Map<String, Object> getInsertValues() throws SVNException {
	    		    	Map<String,Object> selectedRow = select.getRowValues();
	    		    	Map<String, Object> insertValues = new HashMap<String, Object>();
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.wc_id.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.wc_id.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.local_relpath.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.local_relpath.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), 0);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.parent_relpath.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.parent_relpath.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.repos_id.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.repos_id.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.repos_path.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.repos_path.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.revision.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.revnum.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.presence.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.presence.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.depth.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.depth.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.moved_here.toString(), null);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.moved_to.toString(), null);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.kind.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.kind.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_revision.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.changed_rev.toString()));	    		    	
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_date.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.changed_date.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_author.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.changed_author.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.checksum.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.checksum.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.properties.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.properties.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.translated_size.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.translated_size.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.last_mod_time.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.last_mod_time.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.dav_cache.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.dav_cache.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.symlink_target.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.symlink_target.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.file_external.toString(), selectedRow.get(SVNWCDbSchema.BASE_NODE__Fields.file_external.toString()));
	    		    	return insertValues;
	    		    }

	    		};
	    		
	    		try {
	    			stmt.exec();
	    		} finally {
	    			stmt.reset();
	    		}
	    		/*INSERT INTO NODES (
					wc_id, local_relpath, op_depth, parent_relpath,
					repos_id, repos_path, revision,
					presence, depth, moved_here, moved_to, kind,
					changed_revision, changed_date, changed_author,
					checksum, properties, translated_size, last_mod_time,
					dav_cache, symlink_target, file_external )
				SELECT wc_id, local_relpath, 2 /*op_depth/, parent_relpath,
					copyfrom_repos_id, copyfrom_repos_path, copyfrom_revnum,
					presence, depth, NULL /*moved_here/, NULL /*moved_to/, kind,
					changed_rev, changed_date, changed_author,
					checksum, properties, translated_size, last_mod_time,
					NULL /*dav_cache/, symlink_target, NULL /*file_external/
				FROM WORKING_NODE;
				 */
	    		stmt = new SVNSqlJetInsertStatement(sDb, SVNWCDbSchema.NODES) {
	    			private SVNSqlJetSelectStatement select = new SVNSqlJetSelectStatement(sDb.getTemporaryDb(), SVNWCDbSchema.BASE_NODE) {};

	    		    public long exec() throws SVNException {
	    		        try {
	    		            int n = 0;
	    		            while (select.next()) {
	    		                try {
	    		                    table.insertByFieldNamesOr(null, getInsertValues());
	    		                    n++;
	    		                } catch (SqlJetException e) {
	    		                    SVNSqlJetDb.createSqlJetError(e);
	    		                    return -1;
	    		                }
	    		            }
	    		            return n;
	    		        } finally {
	    		            select.reset();
	    		        }
	    		    }
	    		    
	    		    protected Map<String, Object> getInsertValues() throws SVNException {
	    		    	Map<String,Object> selectedRow = select.getRowValues();
	    		    	Map<String, Object> insertValues = new HashMap<String, Object>();
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.wc_id.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.wc_id.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.local_relpath.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.local_relpath.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), 2);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.parent_relpath.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.parent_relpath.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.repos_id.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.copyfrom_repos_id.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.repos_path.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.copyfrom_repos_path.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.revision.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.copyfrom_revnum.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.presence.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.presence.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.depth.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.depth.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.moved_here.toString(), null);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.moved_to.toString(), null);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.kind.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.kind.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_revision.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.changed_rev.toString()));	    		    	
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_date.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.changed_date.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.changed_author.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.changed_author.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.checksum.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.checksum.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.properties.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.properties.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.translated_size.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.translated_size.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.last_mod_time.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.last_mod_time.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.dav_cache.toString(), null);
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.symlink_target.toString(), selectedRow.get(SVNWCDbSchema.WORKING_NODE__Fields.symlink_target.toString()));
	    		    	insertValues.put(SVNWCDbSchema.NODES__Fields.file_external.toString(), null);
	    		    	return insertValues;
	    		    }

	    		};
	    		
	    		try {
	    			stmt.exec();
	    		} finally {
	    			stmt.reset();
	    		}
	    		
	    		sDb.getDb().dropTable("BASE_NODE");
				sDb.getDb().dropTable("WORKING_NODE");
				
	    	} catch (SqlJetException e) {
	            SVNSqlJetDb.createSqlJetError(e);
	        }
        
	    	setVersion(sDb, (int)21);    
    	}
    }
    
    private static class bumpTo21 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	        setVersion(sDb, (int)21);
	        migrateTreeConflictData(sDb);
    	}
    }
    
    private static class bumpTo22 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*-- STMT_UPGRADE_TO_22
	        UPDATE actual_node SET tree_conflict_data = conflict_data;
	        UPDATE actual_node SET conflict_data = NULL;
	        */
	        SVNSqlJetUpdateStatement stmt = new SVNSqlJetUpdateStatement(sDb, SVNWCDbSchema.ACTUAL_NODE) {
	            public Map<String, Object> getUpdateValues() throws SVNException {
	                Map<String, Object> rowValues = getRowValues();
	                rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), 
	                        rowValues.get(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data.toString()));
	                return rowValues;
	            }
	        };
	        try {
	            stmt.exec();
	        } finally {
	            stmt.reset();
	        }
	        
	        stmt = new SVNSqlJetUpdateStatement(sDb, SVNWCDbSchema.ACTUAL_NODE) {
	            public Map<String, Object> getUpdateValues() throws SVNException {
	                Map<String, Object> rowValues = getRowValues();
	                rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data.toString(), null);
	                return rowValues;
	            }
	        };
	        try {
	            stmt.exec();
	        } finally {
	            stmt.reset();
	        }
	 
	        setVersion(sDb, (int)22);
    	}
    }
    
    private static class bumpTo23 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	        //-- STMT_HAS_WORKING_NODES
	        //SELECT 1 FROM nodes WHERE op_depth > 0
	        //LIMIT 1
	        SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> stmt = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields>(sDb, SVNWCDbSchema.NODES) {
	            protected void defineFields() {
	                fields.add(SVNWCDbSchema.NODES__Fields.wc_id);
	            }
	            protected boolean isFilterPassed() throws SVNException {
	                return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
	            }
	        };
	        
	        boolean haveRow = false;
	        try {
	            haveRow = stmt.next();
	        } finally {
	            stmt.reset();
	        }
	        
	        if (haveRow) {
	            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
	                    "The working copy at ''{0}'' is format 22 with WORKING nodes; use a format 22 client to diff/revert before using this client", 
	                    wcRootAbsPath);
	            SVNErrorManager.error(err, SVNLogType.WC);
	        }
	        
	 
	        setVersion(sDb, (int)23);
    	}
    
    }
    
    private static class bumpTo24 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*-- STMT_UPGRADE_TO_25
	    	UPDATE pristine SET refcount = (SELECT COUNT(*) FROM nodes WHERE checksum = pristine.checksum /*OR checksum = pristine.md5_checksum/);
	    	*/
	    	SVNSqlJetUpdateStatement stmt = new SVNSqlJetUpdateStatement(sDb, SVNWCDbSchema.PRISTINE) {
	    		private SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> select = 
	    				new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields>(sDb, SVNWCDbSchema.NODES) {
	    					protected boolean isFilterPassed() throws SVNException {
	    						return ((String)getBind(1)).equals(getColumnString(SVNWCDbSchema.NODES__Fields.checksum));
	    					}
	    					protected void defineFields() {
	    		                fields.add(SVNWCDbSchema.NODES__Fields.wc_id);
	    		            }
	    					protected Object[] getWhere() throws SVNException {
	    				        return new Object[] {};
	    				    }
	    				};
	    				
	    		public Map<String, Object> getUpdateValues() throws SVNException {
	    			Map<String, Object> rowValues = getRowValues();
	    			
	    			select.bindString(1, (String)rowValues.get(SVNWCDbSchema.PRISTINE__Fields.checksum.toString()));
	    			long rowCount = 0;
	    			try {
	    				while (select.next())
	    					rowCount++;
	    			} finally {
	    				select.reset();
	    			}
					rowValues.put(SVNWCDbSchema.PRISTINE__Fields.refcount.toString(), rowCount);
					return rowValues;
				}
	    	};
	    	
	    	try {
				stmt.exec();
			} finally {
				stmt.reset();
			}
	    	
	    	setVersion(sDb, (int)24);
	    	
	    	try {
	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_insert_trigger AFTER INSERT ON nodes WHEN NEW.checksum IS NOT NULL BEGIN UPDATE pristine SET refcount = refcount + 1 WHERE checksum = NEW.checksum; END;");
	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_delete_trigger AFTER DELETE ON nodes WHEN OLD.checksum IS NOT NULL BEGIN UPDATE pristine SET refcount = refcount - 1 WHERE checksum = OLD.checksum; END;");
	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_update_checksum_trigger AFTER UPDATE OF checksum ON nodes WHEN NEW.checksum IS NOT OLD.checksum BEGIN UPDATE pristine SET refcount = refcount + 1 WHERE checksum = NEW.checksum; UPDATE pristine SET refcount = refcount - 1 WHERE checksum = OLD.checksum; END;");
	    	} catch (SqlJetException e) {
	            SVNSqlJetDb.createSqlJetError(e);
	        }
    	}
    }
    
    private static class bumpTo25 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*-- STMT_UPGRADE_TO_25
	    	//DROP VIEW IF EXISTS NODES_CURRENT;
	    	CREATE VIEW NODES_CURRENT AS
	    	  SELECT * FROM nodes JOIN (SELECT wc_id, local_relpath, MAX(op_depth) AS op_depth FROM nodes GROUP BY wc_id, local_relpath) AS filter
	    	    ON nodes.wc_id = filter.wc_id AND nodes.local_relpath = filter.local_relpath AND nodes.op_depth = filter.op_depth;
	    	*/
	    	
	    	try {
	    		sDb.getDb().createView(
	    				"CREATE VIEW NODES_CURRENT AS " + 
	    				"SELECT * FROM nodes JOIN (SELECT wc_id, local_relpath, MAX(op_depth) AS op_depth FROM nodes GROUP BY wc_id, local_relpath) AS filter " +
	    				"ON nodes.wc_id = filter.wc_id AND nodes.local_relpath = filter.local_relpath AND nodes.op_depth = filter.op_depth;");
	    	} catch (SqlJetException e) {
	            SVNSqlJetDb.createSqlJetError(e);
	        }
	
	    	setVersion(sDb, (int)25);
    	}
    }
    
    private static class bumpTo26 implements Bumpable {
    	public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*-- STMT_UPGRADE_TO_26
	    	* DROP VIEW IF EXISTS NODES_BASE;
	    	* CREATE VIEW NODES_BASE AS
	    	* SELECT * FROM nodes
	    	*  WHERE op_depth = 0;
	    	*/
	    	
	    	try {
	    		if (sDb.getDb().getSchema().getViewNames().contains("NODES_BASE")) {
	    			sDb.getDb().dropView("NODES_BASE");
	    		}
	    		sDb.getDb().createView("CREATE VIEW NODES_BASE AS SELECT * FROM nodes WHERE op_depth = 0;"); 
	    	} catch (SqlJetException e) {
	            SVNSqlJetDb.createSqlJetError(e);
	        }
	    	
	    	setVersion(sDb, (int)26);
	    }
    }
    
    private static class bumpTo27 implements Bumpable {
	    public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*
	    	-- STMT_HAS_ACTUAL_NODES_CONFLICTS
	    	SELECT 1 FROM actual_node
	    	WHERE NOT ((prop_reject IS NULL) AND (conflict_old IS NULL)
	    	           AND (conflict_new IS NULL) AND (conflict_working IS NULL)
	    	           AND (tree_conflict_data IS NULL))
	    	LIMIT 1
	    	*/
	
	    	  SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> stmt = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields>(sDb, SVNWCDbSchema.ACTUAL_NODE) {
	              protected void defineFields() {
	                  fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id);
	              }
	              protected boolean isFilterPassed() throws SVNException {
	            	  return !(isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) &&
	            			  isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) &&
	            			  isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) &&
	            			  isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working) &&
	            			  isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data)); 
	              }
	          };
	          
	          boolean haveRow = false;
	          try {
	              haveRow = stmt.next();
	          } finally {
	              stmt.reset();
	          }

	    	  if (haveRow) {
	              SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
	                      "The working copy at ''{0}'' is format 26 with conflicts; use a format 26 client to resolve before using this client", 
	                      wcRootAbsPath);
	              SVNErrorManager.error(err, SVNLogType.WC);
	          }
	         	    	  
	    	  setVersion(sDb, (int)27);
	    }
    }
    
    private static class bumpTo28 implements Bumpable {
	    public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/*-- STMT_UPGRADE_TO_28
	    	 * UPDATE NODES SET checksum=(SELECT checksum FROM pristine WHERE md5_checksum=nodes.checksum)
			 * WHERE EXISTS(SELECT 1 FROM pristine WHERE md5_checksum=nodes.checksum);
	    	 */
	    	UpdateChecksum uc = new UpdateChecksum(sDb, SVNWCDbSchema.NODES);
	    	uc.run();
	    	
	    	setVersion(sDb, (int)28);
	    }
    }
    
    private static class bumpTo29 implements Bumpable {
	    public void bumpTo(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
	    	/* Rename all pristine files, adding a ".svn-base" suffix. */
	    	File pristineDirAbsPath = SVNWCUtils.admChild(wcRootAbsPath, ISVNWCDb.PRISTINE_STORAGE_RELPATH);
	    	for (File dir: SVNFileListUtil.listFiles(pristineDirAbsPath)) {
	    		for (File file: SVNFileListUtil.listFiles(dir)) {
		    		/* If FINFO indicates that ABSPATH names a file, rename it to '<ABSPATH>.svn-base'.
		    	    *
		    	    * Ignore any file whose name is not the expected length, in order to make life easier for any developer 
		    	    * who runs this code twice or has some non-standard files in the pristine directory.
		    	    */
		    		if (SVNFileType.getType(file) == SVNFileType.FILE && SVNFileUtil.getFileName(file).length() == PRISTINE_BASENAME_OLD_LEN) {
		    		   File newAbsPath = SVNFileUtil.createFilePath(SVNFileUtil.getFilePath(file) + PRISTINE_STORAGE_EXT);
		    		   SVNFileUtil.rename(file, newAbsPath);
	   		       }
	    		}
	    	}
	    	
	    	/* Externals */
	    	try {
	    		sDb.getDb().createTable("CREATE TABLE EXTERNALS ( " +
		                "  wc_id  INTEGER NOT NULL REFERENCES WCROOT (id), " +
		                "  local_relpath  TEXT NOT NULL, " +
		                "  parent_relpath  TEXT NOT NULL, " +
		                "  repos_id  INTEGER NOT NULL REFERENCES REPOSITORY (id), " +
		                "  presence  TEXT NOT NULL, " +
		                "  kind  TEXT NOT NULL, " +
		                "  def_local_relpath         TEXT NOT NULL, " +
		                "  def_repos_relpath         TEXT NOT NULL, " +
		                "  def_operational_revision  TEXT, " +
		                "  def_revision              TEXT, " +
		                "  PRIMARY KEY (wc_id, local_relpath) " +
		                "); "); 
	    		sDb.getDb().createIndex("CREATE INDEX I_EXTERNALS_PARENT ON EXTERNALS (wc_id, parent_relpath);");
	    		sDb.getDb().createIndex("CREATE UNIQUE INDEX I_EXTERNALS_DEFINED ON EXTERNALS (wc_id, def_local_relpath, local_relpath);");
	    	} catch (SqlJetException e) {
	            SVNSqlJetDb.createSqlJetError(e);
	        }
	    	
	    	upgradeExternals(sDb, wcRootAbsPath);
	    	  
         	 /* Format 29 introduces the EXTERNALS table (See STMT_CREATE_TRIGGERS) and optimizes a few trigger definitions. ... */
         	 //-- STMT_UPGRADE_TO_29
         	 try {
         		if (sDb.getDb().getSchema().getTriggerNames().contains("nodes_update_checksum_trigger")) {
         			sDb.getDb().dropTrigger("nodes_update_checksum_trigger");
                }
         		if (sDb.getDb().getSchema().getTriggerNames().contains("nodes_insert_trigger")) {
         			sDb.getDb().dropTrigger("nodes_insert_trigger");
                }
         		if (sDb.getDb().getSchema().getTriggerNames().contains("nodes_delete_trigger")) {
         			sDb.getDb().dropTrigger("nodes_delete_trigger");
                }
 	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_update_checksum_trigger AFTER UPDATE OF checksum ON nodes WHEN NEW.checksum IS NOT OLD.checksum BEGIN UPDATE pristine SET refcount = refcount + 1 WHERE checksum = NEW.checksum; UPDATE pristine SET refcount = refcount - 1 WHERE checksum = OLD.checksum; END;");
 	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_insert_trigger AFTER INSERT ON nodes WHEN NEW.checksum IS NOT NULL BEGIN UPDATE pristine SET refcount = refcount + 1 WHERE checksum = NEW.checksum; END;");
 	    		sDb.getDb().createTrigger("CREATE TRIGGER nodes_delete_trigger AFTER DELETE ON nodes WHEN OLD.checksum IS NOT NULL BEGIN UPDATE pristine SET refcount = refcount - 1 WHERE checksum = OLD.checksum; END;");
 	    	} catch (SqlJetException e) {
 	            SVNSqlJetDb.createSqlJetError(e);
 	        }

         	setVersion(sDb, (int)29);
	    }
    }
    
    private static void upgradeExternals(SVNSqlJetDb sDb, File wcRootAbsPath) throws SVNException {
        SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.SELECT_EXTERNAL_PROPERTIES);

    	/* ### For this intermediate upgrade we just assume WC_ID = 1.
        ### Before this bump we lost track of externals all the time, so lets keep this easy. */
    	stmt.bindf("is", 1, "");
    	try {
            while (stmt.next()) {
            	SVNProperties props = stmt.getColumnProperties(SVNWCDbSchema.NODES__Fields.properties);
            	String externalsValues = props.getStringValue(SVNProperty.EXTERNALS);
            	File localRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.NODES__Fields.properties));
            	File localAbsPath = SVNFileUtil.createFilePath(wcRootAbsPath, localRelPath);
                
            	if (externalsValues != null) {
            		SVNExternal[] externalDefs = SVNExternal.parseExternals(localAbsPath, externalsValues);
        		    for (SVNExternal externalDef : externalDefs) {
        		    	File externalPath = SVNFileUtil.createFilePath(localAbsPath, externalDef.getPath());
        		    	
        		    	/* Insert dummy externals definitions: Insert an unknown external, to make sure it will be cleaned up when it is not
                           updated on the next update. */
                        SVNSqlJetStatement addStmt = sDb.getStatement(SVNWCDbStatements.INSERT_EXTERNAL);
                        try {
                            addStmt.bindf("isssssis",
                                    1, /* wc_id*/
                                    SVNFileUtil.getFilePath(externalPath),
                                    SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(externalPath)),
                                    "normal",
                                    "unknown",
                                    SVNFileUtil.getFilePath(localRelPath),
                                    1, /* repos_id */
                                    "" /* repos_relpath */);
                            addStmt.exec();
                        } finally {
                            addStmt.reset();
                        }
                    }
            	}
            }
        } finally {
            stmt.reset();
        }
    }
       
                  
                
    
    
   
    
     
   
    
    
    
    
    
    
}
