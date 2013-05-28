package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetTableStatement;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbCreateSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.ArrayList;

public class SvnWcDbChangelist extends SvnWcDbShared {
	public static void cleanupPristine(SVNWCDbRoot root, File localAbsPath) throws SVNException {
		
	}
	
	public static void setChangelist(SVNWCDbRoot root, File localRelPath, String changelistName, String[] changeLists, SVNDepth depth, ISVNEventHandler eventHandler) throws SVNException {
    	try {
        	Changelist cl = new Changelist();
        	cl.localRelPath = localRelPath;
        	cl.wcRoot = root;
        	cl.changelistName = changelistName;
        	cl.changeLists = changeLists;
        	cl.depth = depth;
        	cl.eventHandler = eventHandler;
        	cl.setChangelist();
        	cl.notifyChangelist();
        }
        finally {
        	try {
        		SVNSqlJetStatement dropList = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_CHANGELIST_LIST, -1);
                try {
        		    dropList.done();
                } finally {
                    dropList.reset();
                }
        	}
        	catch (SVNException e) {}
        }
    }
	
	private static class Changelist {
        String changelistName;
        String[] changeLists;
        SVNDepth depth;
        ISVNEventHandler eventHandler;
        File localRelPath;
        SVNWCDbRoot wcRoot;
                
        public void setChangelist() throws SVNException {
        	
        	SvnChangelistActualNodesTrigger changelistTrigger = new SvnChangelistActualNodesTrigger(wcRoot.getSDb());
        	wcRoot.getSDb().beginTransaction(SqlJetTransactionMode.WRITE);
        	
        	try {
        		populateTargetsTree();
	        	
	        	/* Ensure we have actual nodes for our targets. */
	        	SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_EMPTIES);
                try {
	        	    stmt.done();
                } finally {
                    stmt.reset();
                }
	        	
	        	/* Now create our notification table. */
	        	stmt = new SVNWCDbCreateSchema(wcRoot.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.CHANGELIST_LIST, -1);
                try {
	        	    stmt.done();
                } finally {
                    stmt.reset();
                }
	        	
	        	ArrayList<String> targetList = new ArrayList<String>();
	        	stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.SELECT_TARGETS_LIST); 
	        	stmt.bindf("i", wcRoot.getWcId());
	        	try {
	        		while (stmt.next()) {
	        			targetList.add(stmt.getColumnString(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath));
	        		}
	        	}
	        	finally {
	        		stmt.reset();
	        	}
	        	
	        	/* Update our changelists. */
	        	for (String localRelPath : targetList) {
	        		SVNSqlJetStatement updateChangelist = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CHANGELISTS);
                    try {
                        ((SVNSqlJetTableStatement) updateChangelist).addTrigger(changelistTrigger);
                        updateChangelist.bindf("iss", wcRoot.getWcId(), localRelPath, changelistName);
                        updateChangelist.done();
                    } finally {
                        stmt.reset();
                    }
	            }
	        		        	
	        	/* this version doesn't work, trigger breaks outer stmt cursor
	        	stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.SELECT_TARGETS_LIST); 
	        	stmt.bindf("i", wcRoot.getWcId());
	        	while (stmt.next()) {
	        		SVNSqlJetStatement updateChangelist = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CHANGELISTS);
	        		((SVNSqlJetTableStatement) updateChangelist).addTrigger(changelistTrigger); 
	                updateChangelist.bindf("iss", wcRoot.getWcId(), stmt.getColumnString(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath), changelistName);
	                updateChangelist.done();
	        	}
	        	stmt.reset();
	        	*/
	        	
	        	if (changelistName != null){
	            	stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.MARK_SKIPPED_CHANGELIST_DIRS);
                    try {
                        ((SVNSqlJetTableStatement) stmt).addTrigger(changelistTrigger);
                        stmt.bindf("s", changelistName);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
	            }
	            else {
	            	stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_EMPTIES);
                    try {
                        stmt.bindf("i", wcRoot.getWcId());
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
	            }
	            
        	} catch (SVNException e) {
        		wcRoot.getSDb().rollback();
        		throw e;
	        } finally {
	        	wcRoot.getSDb().commit();
	        }
        }
        
        public void populateTargetsTree() throws SVNException
        {
        	long affectedRows = 0;
        	
        	SVNSqlJetStatement stmt = new SVNWCDbCreateSchema(wcRoot.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.TARGETS_LIST, -1);
            try {
        	    stmt.done();
            } finally {
                stmt.reset();
            }
        	    	
        	if (changeLists != null && changeLists.length > 0) {
                for (int i = 0; i < changeLists.length; i++) {
                    if (depth == SVNDepth.EMPTY) {
                        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_WITH_CHANGELIST);
                    } else if (depth == SVNDepth.FILES){
                        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_FILES_WITH_CHANGELIST);
                    } else if (depth == SVNDepth.IMMEDIATES){
                        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_IMMEDIATES_WITH_CHANGELIST);
                    } else if (depth == SVNDepth.INFINITY){
                        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_INFINITY_WITH_CHANGELIST);
                    }
        		    if (stmt != null) {
            			String changelist = changeLists[i];
                        try {
                            stmt.bindf("iss", wcRoot.getWcId(), localRelPath, changelist);
                            affectedRows += stmt.done();
                        } finally {
                            stmt.reset();
                        }
                    }
        		}
        	}
        	else {
        		if (depth.getId() == SVNDepth.EMPTY.getId()) {
        			stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET2);
        		} else if (depth.getId() == SVNDepth.FILES.getId()){
        			stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_FILES);
        		} else if (depth.getId() == SVNDepth.IMMEDIATES.getId()){
        			stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_IMMEDIATES);
        		} else if (depth.getId() == SVNDepth.INFINITY.getId()){
        			stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_TARGET_DEPTH_INFINITY);
        		}

                try {
                    stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                    affectedRows = stmt.done();
                } finally {
                    stmt.reset();
                }
    			
        		//default:
                /* We don't know how to handle unknown or exclude. */
                //SVN_ERR_MALFUNCTION();
                //break;
        		
        	}
        	
        	/* Does the target exist? */
        	if (affectedRows == 0)
        	{
        		boolean exists = doesNodeExists(wcRoot, localRelPath);
        	    if (!exists) {
        	    	SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", wcRoot.getAbsPath(localRelPath).getPath());
        	    	SVNErrorManager.error(err, SVNLogType.WC);
        	    }
        	 }
        	
        }
        
        public void notifyChangelist() throws SVNException
        {
        	if (eventHandler != null)
    		{
    	    	SVNSqlJetStatement stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.SELECT_CHANGELIST_LIST);
    	    	
    	    	try {
	    	    	while (stmt.next()) {
	    	    		String notifyRelPath = stmt.getColumnString(SVNWCDbSchema.CHANGELIST_LIST__Fields.local_relpath);
	    	    		File notifyAbspath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), notifyRelPath);
	    	    		Long notifyAction = stmt.getColumnLong(SVNWCDbSchema.CHANGELIST_LIST__Fields.notify);
	    	    		String changelistName = stmt.getColumnString(SVNWCDbSchema.CHANGELIST_LIST__Fields.changelist);
	    	    		SVNEventAction eventAction = null;
	    	    		if (notifyAction == SVNEventAction.CHANGELIST_CLEAR.getID())
	    	    			eventAction = SVNEventAction.CHANGELIST_CLEAR;
	    	    		else if (notifyAction == SVNEventAction.CHANGELIST_MOVED.getID())
	    	    			eventAction = SVNEventAction.CHANGELIST_MOVED;
	    	    		else if (notifyAction == SVNEventAction.CHANGELIST_SET.getID())
	    	    			eventAction = SVNEventAction.CHANGELIST_SET;
	    	    		
	    	    		SVNEvent event = SVNEventFactory.createSVNEvent(notifyAbspath, SVNNodeKind.NONE, null, -1, null, null, null,  
	    	    				eventAction, eventAction, null, null, changelistName);
	    	    		
	    	    		eventHandler.checkCancelled();
	    	    		eventHandler.handleEvent(event, -1);
	    	    	}
    	    	} finally {
    				try {
    					if (stmt != null) {
    						stmt.reset();
    					} 
    				} catch (SVNException e) {}
    			}
            }
        	
        	
        }


    };
    
}
