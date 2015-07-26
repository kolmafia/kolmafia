package org.tmatesoft.svn.core.internal.wc17.db;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnBlob;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnInheritedProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnInt64;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnKind;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPath;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPresence;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.isColumnNull;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.reset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.InheritedProperties;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbNodesBase;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbNodesCurrent;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.WCROOT__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectIPropsNode;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

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
        }        
        return props;
    }

    public static SVNProperties readChangedProperties(SVNWCDbRoot root, File relpath) throws SVNException {
        SVNSqlJetStatement stmt = null;
        try {
            stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_PROPS);
            stmt.bindf("is", root.getWcId(), relpath);
            if (stmt.next() && !isColumnNull(stmt, ACTUAL_NODE__Fields.properties)) {
                return getColumnProperties(stmt, ACTUAL_NODE__Fields.properties);
            } 
        } finally {
            reset(stmt);
        }        
        return null;
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
        final Collection<Properties> propsList = cacheProperties(root, relpath, depth, baseProperties, pristineProperties, changelists);
        for (Properties properties : propsList) {
            SVNProperties props = SVNSqlJetStatement.parseProperties(properties.properties);
            File target = new File(properties.relPath);
            
            File absolutePath = root.getAbsPath(target);
            receiver.receive(SvnTarget.fromFile(absolutePath), props);
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
    
    private static class Properties {
    	
    	public Properties(String path, byte[] props) {
    		this.relPath = path;
    		this.properties = props;
    	}
    	
    	public String relPath;
    	public byte[] properties;
    }
    
    
    private static Collection<SvnWcDbProperties.Properties> cacheProperties(SVNWCDbRoot root, File relpath, SVNDepth depth, boolean baseProperties, boolean pristineProperties, Collection<String> changelists) throws SVNException {
        SVNSqlJetSelectStatement propertiesSelectStmt = null;
        
        root.getSDb().beginTransaction(SqlJetTransactionMode.READ_ONLY);
        Collection<Properties> result = new ArrayList<SvnWcDbProperties.Properties>();
        try {
        	// 1. get targets list (relpath, wcid, kind)
            final Collection<Target> targets = collectTargets(root, relpath, depth, changelists);
            if (baseProperties) {
                propertiesSelectStmt = new SVNWCDbNodesBase(root.getSDb());
            } else if (pristineProperties) {
                propertiesSelectStmt = new SVNWCDbNodesCurrent(root.getSDb());
            } else {
                propertiesSelectStmt = new SVNWCDbNodesCurrent(root.getSDb());
            }
            
        	// 2. for each target select properties.
            for (Target target : targets) {
                String localRelpath = target.relPath;
                long wcId = target.wcId;
                
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
                
            	// 3. put props into result map.
                if (props != null && props.length > 2) {
                	result.add(new Properties(localRelpath, props));
                }
            }
        } finally {
            try {
                reset(propertiesSelectStmt);
            } finally {
                root.getSDb().commit();
            }
        }
        return result;
    }
    
    public static Map<File, File> getInheritedPropertiesNodes(SVNWCDbRoot root, File localRelPath, SVNDepth depth) throws SVNException {
        final Map<File, File> result = new HashMap<File, File>();
        SVNWCDbSelectIPropsNode stmt = null;
        stmt = (SVNWCDbSelectIPropsNode) root.getSDb().getStatement(SVNWCDbStatements.SELECT_IPROPS_NODE);
        try {
            stmt.setDepth(SVNDepth.EMPTY);
            stmt.bindf("is", root.getWcId(), localRelPath);
            if (stmt.next()) {
                final File path = root.getAbsPath(getColumnPath(stmt, NODES__Fields.local_relpath));
                result.put(path, getColumnPath(stmt, NODES__Fields.repos_path));
            }
        } finally {
            reset(stmt);
        }
        if (depth == SVNDepth.EMPTY) {
            return result;
        }
        stmt = (SVNWCDbSelectIPropsNode) root.getSDb().getStatement(SVNWCDbStatements.SELECT_IPROPS_NODE);
        try {
            stmt.setDepth(depth);
            stmt.bindf("is", root.getWcId(), localRelPath);
            while(stmt.next()) {
                final File path = root.getAbsPath(getColumnPath(stmt, NODES__Fields.local_relpath));
                result.put(path, getColumnPath(stmt, NODES__Fields.repos_path));
            }
        } finally {
            reset(stmt);
        }
        
        return result;
    }
    
    public static List<Structure<InheritedProperties>> readInheritedProperties(SVNWCDbRoot root, File localRelPath, String propertyName) throws SVNException {
        SVNSqlJetStatement stmt = null;
        File relPath = localRelPath;
        File parentRelPath = null;
        File expectedParentReposRelPath = null;
        
        final List<Structure<InheritedProperties>> inheritedProperties = new ArrayList<Structure<InheritedProperties>>();
        List<Structure<InheritedProperties>> cachedProperties = null; 

        try {
            stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            
            while(relPath != null) {
                SVNProperties nodeProps = null;
                
                parentRelPath = "".equals(relPath.getPath()) ? null : SVNFileUtil.getFileDir(relPath);
                stmt.bindf("is", root.getWcId(), relPath);
                if (!stmt.next()) {
                    nodeNotFound(root, relPath);
                }
                final long opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                SVNWCDbStatus status = getColumnPresence(stmt, NODES__Fields.presence);
                if (status != SVNWCDbStatus.Normal && status != SVNWCDbStatus.Incomplete) {
                    final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS,
                            "The node ''{0}'' has a status that has no properites", root.getAbsPath(relPath));
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                if (opDepth > 0) {                    
                } else if (expectedParentReposRelPath != null) {
                    final File reposRelPath = getColumnPath(stmt, NODES__Fields.repos_path);
                    if (!expectedParentReposRelPath.equals(reposRelPath)) {
                        reset(stmt);
                        break;
                    }
                    expectedParentReposRelPath = SVNFileUtil.getFileDir(expectedParentReposRelPath);
                } else {
                    final File reposRelPath = getColumnPath(stmt, NODES__Fields.repos_path);
                    expectedParentReposRelPath = SVNFileUtil.getFileDir(reposRelPath);
                }
                
                if (opDepth == 0 && !isColumnNull(stmt, NODES__Fields.inherited_props)) {
                    final byte[] inheritedPropsBlob = getColumnBlob(stmt, NODES__Fields.inherited_props);
                    if (inheritedPropsBlob != null && !Arrays.equals(SvnWcDbShared.EMPTY_PROPS_BLOB, inheritedPropsBlob)) {
                        cachedProperties = getColumnInheritedProperties(stmt, NODES__Fields.inherited_props);
                        parentRelPath = null;
                    }
                }
                
                nodeProps = getColumnProperties(stmt, NODES__Fields.properties);
                
                reset(stmt);
                if (!relPath.equals(localRelPath)) {
                    final SVNProperties changedProps = readChangedProperties(root, relPath);
                    if (changedProps != null) {
                        nodeProps = changedProps;
                    }
                    if (nodeProps != null && !nodeProps.isEmpty()) {
                        if (propertyName != null) {
                            final SVNProperties filteredProperites = new SVNProperties();
                            if (nodeProps.containsName(propertyName)) {
                                filteredProperites.put(propertyName, nodeProps.getSVNPropertyValue(propertyName));
                            }
                            nodeProps = filteredProperites;
                        } 
                        if (nodeProps != null && !nodeProps.isEmpty()) {
                            final Structure<InheritedProperties> inheritedProperitesElement = Structure.obtain(InheritedProperties.class);
                            inheritedProperitesElement.set(InheritedProperties.pathOrURL, SVNFileUtil.getFilePath(root.getAbsPath(relPath)));
                            inheritedProperitesElement.set(InheritedProperties.properties, nodeProps);
                            inheritedProperties.add(0, inheritedProperitesElement);
                        }
                    }
                }
                relPath = parentRelPath;
            }
            
            if (cachedProperties != null) {
                for (Structure<InheritedProperties> element : cachedProperties) {
                    SVNProperties props = element.get(InheritedProperties.properties);
                    if (props == null || props.isEmpty()) {
                        continue;
                    }
                    if (propertyName != null) {
                        if (!props.containsName(propertyName)) {
                            continue;
                        }
                        final SVNProperties filteredProperties = new SVNProperties();
                        filteredProperties.put(propertyName, props.getSVNPropertyValue(propertyName));
                        props = filteredProperties;
                    }
                    if (!props.isEmpty()) {
                        element.set(InheritedProperties.properties, props);
                        inheritedProperties.add(0, element);
                    }
                }
            }
        } finally {
            reset(stmt);
        }
        
        return inheritedProperties;
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
}
