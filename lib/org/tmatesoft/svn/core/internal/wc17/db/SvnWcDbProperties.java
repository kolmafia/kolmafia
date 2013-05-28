package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.statement.*;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.*;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.*;

public class SvnWcDbProperties extends SvnWcDbShared {
	
	private static final int WC__NO_REVERT_FILES  = 4;
    
    public static SVNProperties readProperties(SVNWCDbRoot root, File relpath) throws SVNException {
        SVNProperties props = null;
        SVNSqlJetStatement stmt = null;
        try {
            stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_PROPS);
            stmt.bindf("is", root.getWcId(), relpath);
            if (stmt.next() && !isColumnNull(stmt, ACTUAL_NODE__Fields.properties)) {
                props = getColumnProperties(stmt, ACTUAL_NODE__Fields.properties);
            } 
            if (props != null) {
                return props;
            }
            props = readPristineProperties(root, relpath);
            if (props == null) {
                return new SVNProperties();
            }
        } finally {
            reset(stmt);
        }        return props;
    }
    
    public static SVNProperties readPristineProperties(SVNWCDbRoot root, File relpath) throws SVNException {
        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_PROPS);
        try {
            stmt.bindf("is", root.getWcId(), relpath);
            if (!stmt.next()) {
                nodeNotFound(root.getAbsPath(relpath));
            }
            SVNWCDbStatus presence = getColumnPresence(stmt);
            if (presence == SVNWCDbStatus.BaseDeleted) {
                boolean haveRow = stmt.next();
                assert (haveRow);
                presence = getColumnPresence(stmt);
            }
            if (presence == SVNWCDbStatus.Normal || presence == SVNWCDbStatus.Incomplete) {
                SVNProperties props = getColumnProperties(stmt, SVNWCDbSchema.NODES__Fields.properties);
                if (props == null) {
                    props = new SVNProperties();
                }
                return props;
            }
            return null;
        } finally {
            reset(stmt);
        }
    }
    
    public static void readPropertiesRecursively(SVNWCDbRoot root, File relpath, SVNDepth depth, boolean baseProperties, boolean pristineProperties, Collection<String> changelists,
            ISvnObjectReceiver<SVNProperties> receiver) throws SVNException {
        SVNSqlJetSelectStatement stmt = null;

        root.getSDb().getTemporaryDb().beginTransaction(SqlJetTransactionMode.WRITE);        
        try {
            try {
                cacheProperties(root, relpath, depth, baseProperties, pristineProperties, changelists);            
                stmt = new SVNSqlJetSelectStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.NODE_PROPS_CACHE);
                while(stmt.next()) {
                    SVNProperties props = getColumnProperties(stmt, NODE_PROPS_CACHE__Fields.properties);
                    File target = getColumnPath(stmt, NODE_PROPS_CACHE__Fields.local_Relpath);
                    
                    File absolutePath = root.getAbsPath(target);
                    receiver.receive(SvnTarget.fromFile(absolutePath), props);
                }            
            } finally {        
                reset(stmt);
                SVNSqlJetStatement dropCache = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_NODE_PROPS_CACHE, -1);
                try {
                    dropCache.done();
                } finally {
                    dropCache.reset();
                }
            }
        } catch (SVNException e) {
            root.getSDb().getTemporaryDb().rollback();
            throw e;
        } finally {
            root.getSDb().getTemporaryDb().commit();
        }
    }
    
    /* Set the ACTUAL_NODE properties column for (WC_ID, LOCAL_RELPATH) to * PROPS. */
    private static void setActualProps(SVNWCDbRoot root, File localRelPath, SVNProperties properties) throws SVNException {
    	SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_PROPS);
    	long affectedRows = 0;
    	try {
    		stmt.bindf("is", root.getWcId(), localRelPath);
    		stmt.bindProperties(3, properties);
    		affectedRows = stmt.exec();
    	}
    	finally {
    		stmt.reset();
    	}
		
		if (affectedRows == 1 || properties.size() == 0) 
			return;
		
		/* We have to insert a row in ACTUAL */
		try {
			stmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_PROPS);
			stmt.bindf("is", root.getWcId(), localRelPath);
			if (localRelPath != null) {
				stmt.bindString(3, SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(localRelPath)));
			}
			stmt.bindProperties(4, properties);
			stmt.exec();
		}
		finally {
			stmt.reset();
		}
			
		
    }
    
    public static void upgradeApplyProperties(SVNWCDbRoot root, File dirAbsPath, File localRelPath, 
    		SVNProperties baseProps, SVNProperties workingProps, SVNProperties revertProps, int originalFormat) throws SVNException {
    	long topOpDepth = -1;
    	long belowOpDepth = -1;
    	SVNWCDbStatus topPresence = SVNWCDbStatus.NotPresent;
    	SVNWCDbStatus belowPresence = SVNWCDbStatus.NotPresent;
    	SVNWCDbKind kind = SVNWCDbKind.Unknown;
    	long affectedRows;
    	
    	SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
    	try {
	    	stmt.bindf("is", root.getWcId(), localRelPath);
	    	boolean haveRow = stmt.next();
	        if (haveRow) {
	        	topOpDepth = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth);
	        	topPresence = getColumnPresence(stmt, SVNWCDbSchema.NODES__Fields.presence);
	        	kind = getColumnKind(stmt, SVNWCDbSchema.NODES__Fields.kind);
	            haveRow = stmt.next();
	            if (haveRow) {
	            	belowOpDepth = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth);
	            	belowPresence = getColumnPresence(stmt, SVNWCDbSchema.NODES__Fields.presence);
	            }
	        }
    	} finally {
    		stmt.reset();
    	}

    	if (originalFormat > WC__NO_REVERT_FILES 
    	        && revertProps == null 
    	        && topOpDepth != -1
    	        && topPresence == SVNWCDbStatus.Normal
    			&& belowOpDepth != -1 
    			&& belowPresence != SVNWCDbStatus.NotPresent) {
    		/* There should be REVERT_PROPS, so it appears that we just ran into the described bug. Sigh.  */
    		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, 
    				"The properties of ''{0}'' are in an indeterminate state and cannot be upgraded. See issue #2530.", 
    					SVNFileUtil.createFilePath(dirAbsPath, localRelPath));
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
    	}
    	/* Need at least one row, or two rows if there are revert props */
    	if (topOpDepth == -1 || (belowOpDepth == -1 && revertProps != null)) {
    		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, 
    				"Insufficient NODES rows for ''{0}''", SVNFileUtil.createFilePath(dirAbsPath, localRelPath));
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
    	}
    	/* one row, base props only: upper row gets base props
        two rows, base props only: lower row gets base props
        two rows, revert props only: lower row gets revert props
        two rows, base and revert props: upper row gets base, lower gets revert */
    	if (revertProps != null || belowOpDepth == -1) {
    		stmt = root.getSDb().getStatement(SVNWCDbStatements.UPDATE_NODE_PROPS);
    		try {
    			stmt.bindf("isi", root.getWcId(), localRelPath, topOpDepth);
    			stmt.bindProperties(4, baseProps);
    			affectedRows = stmt.exec();
    			assert(affectedRows == 1);
    		}
    		finally {
    			stmt.reset();
    		}
    	}
    	
    	if (belowOpDepth != -1) {
    		SVNProperties props = revertProps != null ? revertProps : baseProps;
    		stmt = root.getSDb().getStatement(SVNWCDbStatements.UPDATE_NODE_PROPS);
    		try {
    			stmt.bindf("isi", root.getWcId(), localRelPath, belowOpDepth);
    			stmt.bindProperties(4, props);
    			affectedRows = stmt.exec();
    			assert(affectedRows == 1);
    		}
    		finally {
    			stmt.reset();
    		}
    	}

    	if (workingProps != null && baseProps != null) {
    		SVNProperties diffs = FSRepositoryUtil.getPropsDiffs(workingProps, baseProps);
    		if (diffs.isEmpty())
    			workingProps.clear(); 
    	}
    	if (workingProps != null) {
    		setActualProps(root, localRelPath, workingProps);
    	}
    	if (kind == SVNWCDbKind.Dir) {
    		SVNProperties props = workingProps;
    		if (props == null) {
    			props = baseProps;
    		}
    		String externals = props != null ? props.getStringValue(SVNProperty.EXTERNALS) : null;
    		if (externals != null && !"".equals(externals)) {
    			SVNExternal[] externalsList = SVNExternal.parseExternals(
    					SVNFileUtil.createFilePath(dirAbsPath, localRelPath), externals);
    			for (SVNExternal externalItem : externalsList) {
    				File itemRelPah = SVNFileUtil.createFilePath(localRelPath, externalItem.getPath());
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_EXTERNAL_UPGRADE);
                    try {
                        stmt.bindf("issssis",
                                root.getWcId(),
                                itemRelPah,
                                SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(itemRelPah)),
                                SvnWcDbStatementUtil.getPresenceText(SVNWCDbStatus.Normal),
                                localRelPath,
                                1, /* repos_id */
                                ""  /* repos_relpath */);
                        stmt.exec();
                    } finally {
                        stmt.reset();
                    }
    			}
    		}

    	}
    }
    
    public static void upgradeApplyDavCache(SVNWCDbRoot root, File dirRelPath, Map<String, SVNProperties> cacheValues) throws SVNException {
    	SVNSqlJetStatement selectRoot = root.getSDb().getStatement(SVNWCDbStatements.SELECT_WCROOT_NULL);
    	long wcId = 0;
    	try {
    		if (selectRoot.next())
    			wcId = selectRoot.getColumnLong(WCROOT__Fields.id);
    	}
    	finally {
    		selectRoot.reset();
    	}

        /* Iterate over all the wcprops, writing each one to the wc_db. */
        for (Iterator<String> names = cacheValues.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            SVNProperties props = (SVNProperties) cacheValues.get(name);
            if (props.size() > 0) {
                File localRelPath = SVNFileUtil.createFilePath(dirRelPath, name);

                SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.UPDATE_BASE_NODE_DAV_CACHE);
                stmt.bindf("is", wcId, localRelPath);
            	stmt.bindProperties(3, props);
            	try {
            		stmt.exec();
            	}
            	finally {
            		stmt.reset();
            	}
            }
    	}
    	
    }
    
    private static void cacheProperties(SVNWCDbRoot root, File relpath, SVNDepth depth, boolean baseProperties, boolean pristineProperties, Collection<String> changelists) throws SVNException {
        SVNSqlJetStatement stmt = null;
        InsertIntoPropertiesCache insertStmt = null;
        SVNSqlJetSelectStatement propertiesSelectStmt = null;
        
        root.getSDb().beginTransaction(SqlJetTransactionMode.READ_ONLY);
        try {
            collectTargets(root, relpath, depth, changelists);
            stmt = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.NODE_PROPS_CACHE, -1);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }
            
            if (baseProperties) {
                propertiesSelectStmt = new SVNWCDbNodesBase(root.getSDb());
            } else if (pristineProperties) {
                propertiesSelectStmt = new SVNWCDbNodesCurrent(root.getSDb());
            } else {
                propertiesSelectStmt = new SVNWCDbNodesCurrent(root.getSDb());
            }
            
            insertStmt = new InsertIntoPropertiesCache(root.getSDb().getTemporaryDb());
            
            stmt = new SVNSqlJetSelectStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.TARGETS_LIST);
            stmt.bindf("i", root.getWcId());
            
            while(stmt.next()) {
                String localRelpath = getColumnText(stmt, TARGETS_LIST__Fields.local_relpath);
                long wcId = getColumnInt64(stmt, TARGETS_LIST__Fields.wc_id);
                String kind = getColumnText(stmt, TARGETS_LIST__Fields.kind);
                
                propertiesSelectStmt.bindf("is", wcId, localRelpath);
                byte[] props = null;
                try {
                    if (propertiesSelectStmt.next()) {
                        SVNWCDbStatus presence = getColumnPresence(propertiesSelectStmt);
                        if (baseProperties) {
                            if (presence == SVNWCDbStatus.Normal || presence == SVNWCDbStatus.Incomplete) {
                                props = getColumnBlob(propertiesSelectStmt, SVNWCDbSchema.NODES__Fields.properties);
                            }
                        } else if (pristineProperties) {
                            if (presence == SVNWCDbStatus.Normal || presence == SVNWCDbStatus.Incomplete || presence == SVNWCDbStatus.BaseDeleted) {
                                props = getColumnBlob(propertiesSelectStmt, NODES__Fields.properties);
                            }
                            if (props == null) {
                                long rowOpDepth = getColumnInt64(propertiesSelectStmt, NODES__Fields.op_depth);
                                if (rowOpDepth > 0 && getColumnPresence(propertiesSelectStmt, NODES__Fields.presence) == SVNWCDbStatus.BaseDeleted) {
                                    SelectRowWithMaxOpDepth query = new SelectRowWithMaxOpDepth(root.getSDb(), rowOpDepth);
                                    try {
                                        query.bindf("is", wcId, localRelpath);
                                        if (query.next()) {
                                            props = getColumnBlob(query, NODES__Fields.properties);
                                        }
                                    } finally {
                                        reset(query);
                                    }
                                }
                            }
                        } else {
                            if (presence == SVNWCDbStatus.Normal || presence == SVNWCDbStatus.Incomplete) {
                                props = getColumnBlob(propertiesSelectStmt, NODES__Fields.properties);
                            }
                            SVNSqlJetSelectStatement query = new SVNSqlJetSelectStatement(root.getSDb(), SVNWCDbSchema.ACTUAL_NODE);
                            byte[] actualProps = null;
                            try {
                                query.bindf("is", wcId, localRelpath);
                                if (query.next()) {
                                    actualProps = getColumnBlob(query, ACTUAL_NODE__Fields.properties);
                                }
                            } finally {
                                reset(query);
                            }
                            props = actualProps != null ? actualProps : props;
                        }
                    }
                } finally {
                    reset(propertiesSelectStmt);
                }
                
                if (props != null && props.length > 2) {
                    try {
                        insertStmt.putInsertValue(NODE_PROPS_CACHE__Fields.local_Relpath, localRelpath);
                        insertStmt.putInsertValue(NODE_PROPS_CACHE__Fields.kind, kind);
                        insertStmt.putInsertValue(NODE_PROPS_CACHE__Fields.properties, props);
                        
                        insertStmt.exec();
                    } finally {
                        insertStmt.reset();
                    }
                }
            }
        } finally {
            try {
                reset(stmt);
                reset(insertStmt);
                reset(propertiesSelectStmt);

                SVNSqlJetStatement dropTargets = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_TARGETS_LIST, -1);
                try {
                    dropTargets.done();
                } finally {
                    dropTargets.reset();
                }
            } finally {
                root.getSDb().commit();
            }
        }
    }
    
    /*
     * SELECT properties FROM nodes nn
                 WHERE  //n.presence = 'base-deleted' 
                   AND nn.wc_id = n.wc_id
                   AND nn.local_relpath = n.local_relpath
                   AND nn.op_depth < n.op_depth
                 ORDER BY op_depth DESC
     */
    private static class SelectRowWithMaxOpDepth extends SVNSqlJetSelectStatement {

        private long opDepth;

        public SelectRowWithMaxOpDepth(SVNSqlJetDb sDb, long opDepth) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
            this.opDepth = opDepth;
        }
        @Override
        protected ISqlJetCursor openCursor() throws SVNException {
            try {
                return super.openCursor().reverse();
            } catch (SqlJetException e) {
                return null;
            }
        }
        @Override
        protected boolean isFilterPassed() throws SVNException {
            long rowOpDepth = getColumnLong(NODES__Fields.op_depth);
            return rowOpDepth < opDepth; 
        }
    }
    
    private static class InsertIntoPropertiesCache extends SVNSqlJetInsertStatement {
        
        private HashMap<String, Object> insertValues;

        public InsertIntoPropertiesCache(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODE_PROPS_CACHE);
            insertValues = new HashMap<String, Object>();
        }
        
        public void putInsertValue(Enum<?> f, Object value) {
            insertValues.put(f.toString(), value);
        }
        
        @Override
        public void reset() throws SVNException {
            super.reset();
            insertValues.clear();
        }

        @Override
        protected Map<String, Object> getInsertValues() throws SVNException {
            return insertValues;
        }
    }
}
