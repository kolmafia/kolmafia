package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbUpgradeData;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpgrade.TextBaseInfo;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnChecksum.Kind;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SvnOldUpgradeEntries {
	
	public static WriteBaton writeUpgradedEntries(WriteBaton parentNode, SVNWCDb db,  SVNWCDbUpgradeData upgradeData, File dirAbsPath, 
			Map<String, SVNEntry> entries, SVNHashMap textBases) throws SVNException {
		WriteBaton dirNode = new WriteBaton();
		
		SVNEntry thisDir = entries.get("");
		/* If there is no "this dir" entry, something is wrong. */
		if (thisDir == null) {
			SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "No default entry in directory ''{0}''", dirAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
		}  
		File oldRootAbsPath = SVNFileUtil.createFilePath(SVNPathUtil.getCommonPathAncestor(
				SVNPathUtil.getAbsolutePath(dirAbsPath.getAbsolutePath()), SVNPathUtil.getAbsolutePath(upgradeData.rootAbsPath.getAbsolutePath())));
		
		assert(oldRootAbsPath != null);
		File dirRelPath = SVNWCUtils.skipAncestor(oldRootAbsPath, dirAbsPath);
		
		/* Write out "this dir" */
		dirNode = writeEntry(true, parentNode, db, upgradeData, thisDir, null, dirRelPath, 
				SVNFileUtil.createFilePath(upgradeData.rootAbsPath, dirRelPath), oldRootAbsPath, thisDir, false);
				
		for (Iterator<String> names = entries.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            SVNEntry entry = (SVNEntry) entries.get(name);
            TextBaseInfo info = (TextBaseInfo)textBases.get(name);
            if ("".equals(name)) { 
            	continue;
            }
            
            /* Write the entry. Pass TRUE for create locks, because we still use this function for upgrading old working copies. */
            File childAbsPath =  SVNFileUtil.createFilePath(dirAbsPath, name);
            File childRelPath = SVNWCUtils.skipAncestor(oldRootAbsPath, childAbsPath);
            writeEntry(false, dirNode, db, upgradeData, entry, info, childRelPath, 
            		SVNFileUtil.createFilePath(upgradeData.rootAbsPath, childRelPath), oldRootAbsPath, thisDir, true);
		}
		
		if (dirNode.treeConflicts != null) {
			writeActualOnlyEntries(dirNode.treeConflicts, upgradeData.root.getSDb(), upgradeData.workingCopyId, SVNFileUtil.getFilePath(dirRelPath));
		}
	
		return dirNode;
	}
	
	private static class DbNode {
		long wcId;
		String localRelPath;
		long opDepth;
		long reposId;
		String reposRelPath;
		String parentRelPath;
		SVNWCDbStatus presence = SVNWCDbStatus.Normal;
		long revision;
		SVNNodeKind kind;  /* ### should switch to svn_wc__db_kind_t */
		SvnChecksum checksum;
		long translatedSize;
		long changedRev;
		SVNDate changedDate;
		String changedAuthor;
		SVNDepth depth;
		SVNDate lastModTime;
		SVNProperties properties;
		boolean isFileExternal;
	};
	
	private static class DbActualNode {
		long wcId;
		String localRelPath;
		String parentRelPath;
		SVNProperties properties;
		String conflictOld;
		String conflictNew;
		String conflictWorking;
		String propReject;
		String changelist;
		/* ### enum for text_mod */
		String treeConflictData;
	}
		
	static class WriteBaton {
		DbNode base;
		DbNode work;
		DbNode belowWork;
		Map<String, String> treeConflicts;
	};
	
	/* Write the information for ENTRY to WC_DB.  The WC_ID, REPOS_ID and REPOS_ROOT will all be used for writing ENTRY.
	  Transitioning from straight sql to using the wc_db APIs.  For the time being, we'll need both parameters. */
	private static WriteBaton writeEntry(boolean isCalculateEntryNode, WriteBaton parentNode, SVNWCDb db, SVNWCDbUpgradeData upgradeData, SVNEntry entry, TextBaseInfo textBaseInfo,
			File localRelPath, File tmpEntryAbsPath, File rootAbsPath, SVNEntry thisDir, boolean isCreateLocks) throws SVNException {
		DbNode baseNode = null;
		DbNode workingNode = null;
		DbNode belowWorkingNode = null;
		DbActualNode actualNode = null;
		
		String parentRelPath = null;
		if (localRelPath != null) {
			parentRelPath = SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(localRelPath));
		}
		
		/* This is how it should work, it doesn't work like this yet because we need proper op_depth to layer the working nodes.

	     Using "svn add", "svn rm", "svn cp" only files can be replaced pre-wcng; directories can only be normal, deleted or added.
	     Files cannot be replaced within a deleted directory, so replaced files can only exist in a normal directory, or a directory that
	     is added+copied.  In a normal directory a replaced file needs a base node and a working node, in an added+copied directory a
	     replaced file needs two working nodes at different op-depths.

	     With just the above operations the conversion for files and directories is straightforward:

	           pre-wcng                             wcng
	     parent         child                 parent     child

	     normal         normal                base       base
	     add+copied     normal+copied         work       work
	     normal+copied  normal+copied         work       work
	     normal         delete                base       base+work
	     delete         delete                base+work  base+work
	     add+copied     delete                work       work
	     normal         add                   base       work
	     add            add                   work       work
	     add+copied     add                   work       work
	     normal         add+copied            base       work
	     add            add+copied            work       work
	     add+copied     add+copied            work       work
	     normal         replace               base       base+work
	     add+copied     replace               work       work+work
	     normal         replace+copied        base       base+work
	     add+copied     replace+copied        work       work+work

	     However "svn merge" make this more complicated.  The pre-wcng "svn merge" is capable of replacing a directory, that is it can
	     mark the whole tree deleted, and then copy another tree on top. 
	     The entries then represent the replacing tree overlayed on the deleted tree.

	       original       replace          schedule in
	       tree           tree             combined tree

	       A              A                replace+copied
	       A/f                             delete+copied
	       A/g            A/g              replace+copied
	                      A/h              add+copied
	       A/B            A/B              replace+copied
	       A/B/f                           delete+copied
	       A/B/g          A/B/g            replace+copied
	                      A/B/h            add+copied
	       A/C                             delete+copied
	       A/C/f                           delete+copied
	                      A/D              add+copied
	                      A/D/f            add+copied

	     The original tree could be normal tree, or an add+copied tree. 
	     Committing such a merge generally worked, but making further tree modifications before commit sometimes failed.

	     The root of the replace is handled like the file replace:

	           pre-wcng                             wcng
	     parent         child                 parent     child

	     normal         replace+copied        base       base+work
	     add+copied     replace+copied        work       work+work

	     although obviously the node is a directory rather then a file.
	     There are then more conversion states where the parent is replaced.

	           pre-wcng                                wcng
	     parent           child              parent            child

	     replace+copied   add                [base|work]+work  work
	     replace+copied   add+copied         [base|work]+work  work
	     replace+copied   delete+copied      [base|work]+work  [base|work]+work
	     delete+copied    delete+copied      [base|work]+work  [base|work]+work
	     replace+copied   replace+copied     [base|work]+work  [base|work]+work
	  */

        if (parentNode == null && entry.getSchedule() != null) {
            String scheduleOperation;
            if (SVNProperty.SCHEDULE_ADD.equals(entry.getSchedule())) {
                scheduleOperation = "addition";
            } else if (SVNProperty.SCHEDULE_DELETE.equals(entry.getSchedule())) {
                scheduleOperation = "deletion";
            } else if (SVNProperty.SCHEDULE_REPLACE.equals(entry.getSchedule())) {
                scheduleOperation = "replacement";
            } else {
                scheduleOperation = entry.getSchedule();
            }

            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE, "Working copy root directory is scheduled for {0}; revert it before upgrade.", scheduleOperation);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

		assert(parentNode != null || entry.getSchedule() == null);
		assert(parentNode == null || parentNode.base != null || parentNode.belowWork != null || parentNode.work != null);
		
		if (entry.getSchedule() == null) {
			if (entry.isCopied() || 
				(entry.getDepth() == SVNDepth.EXCLUDE && parentNode != null && parentNode.base == null && parentNode.work != null)) {
				workingNode = new DbNode();
			} else {
				baseNode = new DbNode();
			}
		}
		else if (entry.isScheduledForAddition()) {
			workingNode = new DbNode();
			if (entry.isDeleted()) {
				if (parentNode != null && parentNode.base != null)
					baseNode = new DbNode();
				else
					belowWorkingNode = new DbNode();
			}
		}
		else if (entry.isScheduledForDeletion()) {
			workingNode = new DbNode();
			if (parentNode != null && parentNode.base != null) 
				baseNode = new DbNode();
			if (parentNode != null && parentNode.work != null)
				belowWorkingNode = new DbNode();
		}
		else if (entry.isScheduledForReplacement()) {
			workingNode = new DbNode();
			if (parentNode != null && parentNode.base != null) 
				baseNode = new DbNode();
			else
				belowWorkingNode = new DbNode();
		}
		
		/* Something deleted in this revision means there should always be a BASE node to indicate the not-present node.  */
		if (entry.isDeleted()) {
			assert(baseNode != null || belowWorkingNode != null);
			assert(!entry.isIncomplete());
			if (baseNode != null)
				baseNode.presence = SVNWCDbStatus.NotPresent;
			else
				belowWorkingNode.presence = SVNWCDbStatus.NotPresent;
		} else if (entry.isAbsent()) {
			assert(baseNode != null && workingNode == null && belowWorkingNode == null);
			assert(!entry.isIncomplete());
			baseNode.presence = SVNWCDbStatus.ServerExcluded;
		}
		
		if (entry.isCopied()) {
			if (SvnOldUpgrade.getEntryCopyFromURL(entry) != null) {
				workingNode.reposId = upgradeData.repositoryId;
				String relPath = SVNURLUtil.getRelativeURL(SvnOldUpgrade.getEntryRepositoryRootURL(thisDir), SvnOldUpgrade.getEntryCopyFromURL(entry), false);
				if (relPath == null)
					workingNode.reposRelPath = null;
				else 
					workingNode.reposRelPath = relPath;
				workingNode.revision = entry.getCopyFromRevision();
				workingNode.opDepth = SVNWCUtils.relpathDepth(localRelPath);
			} else if (parentNode != null && parentNode.work != null && parentNode.work.reposRelPath != null) {
				workingNode.reposId = upgradeData.repositoryId;
				workingNode.reposRelPath = SVNPathUtil.append(parentNode.work.reposRelPath, SVNFileUtil.getFileName(localRelPath));
				workingNode.revision = parentNode.work.revision;
				workingNode.opDepth = parentNode.work.opDepth;
			} else {
				SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "No copyfrom URL for ''{0}''", localRelPath);
	            SVNErrorManager.error(err, SVNLogType.WC);
			}
		}
		
		if (entry.getConflictOld() != null) {
			actualNode = new DbActualNode();
			if (parentRelPath != null && entry.getConflictOld() != null)
				actualNode.conflictOld = SVNPathUtil.append(parentRelPath, entry.getConflictOld());
			else
				actualNode.conflictOld = entry.getConflictOld();
			
			if (parentRelPath != null && entry.getConflictNew() != null)
				actualNode.conflictNew = SVNPathUtil.append(parentRelPath, entry.getConflictNew());
			else
				actualNode.conflictNew = entry.getConflictNew();
			
			if (parentRelPath != null && entry.getConflictWorking() != null)
				actualNode.conflictWorking = SVNPathUtil.append(parentRelPath, entry.getConflictWorking());
			else
				actualNode.conflictWorking = entry.getConflictWorking();
		}
		
		if (entry.getPropRejectFile() != null) {
			actualNode = new DbActualNode();
			actualNode.propReject = SVNPathUtil.append(
					entry.isDirectory() ? SVNFileUtil.getFilePath(localRelPath) : parentRelPath, entry.getPropRejectFile());
			
		}
		
		if (entry.getChangelistName() != null) {
			actualNode = new DbActualNode();
			actualNode.changelist = entry.getChangelistName();
		}
		
		Map<String, String> treeConflicts = null;
		/* ### set the text_mod value? */
		if (isCalculateEntryNode && entry.getTreeConflictData() != null) {
			/* Issues #3840/#3916: 1.6 stores multiple tree conflicts on the parent node, 1.7 stores them directly on the conflited nodes.
	         So "((skel1) (skel2))" becomes "(skel1)" and "(skel2)" */
			
			treeConflicts = new HashMap<String,String>();
			Map<File, SVNTreeConflictDescription> tcs = entry.getTreeConflicts();
	        for (Iterator<File> keys = tcs.keySet().iterator(); keys.hasNext();) {
	            File entryPath = keys.next();
	            SVNTreeConflictDescription conflict = (SVNTreeConflictDescription) tcs.get(entryPath);
				assert(conflict.isTreeConflict());
				/* Fix dubious data stored by old clients, local adds don't have a repository URL. */
				if (conflict.getConflictReason() == SVNConflictReason.ADDED) {
					conflict.setSourceLeftVersion(null);
				}
				String key = SVNFileUtil.getFilePath(SVNWCUtils.skipAncestor(rootAbsPath, conflict.getPath()));
				treeConflicts.put(key, SVNTreeConflictUtil.getSingleTreeConflictData(conflict));
            }
		}
		
		if (parentNode != null && parentNode.treeConflicts != null) {
			String treeConflictData = (String)parentNode.treeConflicts.get(SVNFileUtil.getFilePath(localRelPath));
			if (treeConflictData != null) {
				actualNode = new DbActualNode();
				actualNode.treeConflictData = treeConflictData;
				/* Reset hash so that we don't write the row again when writing actual-only nodes */
				parentNode.treeConflicts.remove(SVNFileUtil.getFilePath(localRelPath));
			}
			
			
		}
		
		if (entry.getExternalFilePath() != null) {
			baseNode = new DbNode();
		}
		
		/* Insert the base node. */
		if (baseNode != null) {
			baseNode.wcId = upgradeData.workingCopyId;
			baseNode.localRelPath = SVNFileUtil.getFilePath(localRelPath);
			baseNode.opDepth = 0;
			baseNode.parentRelPath = parentRelPath;
			baseNode.revision = entry.getRevision();
			baseNode.lastModTime = entry.isFile() ? SVNDate.parseDate(entry.getTextTime()) : null;
			baseNode.translatedSize = entry.getWorkingSize();
			if (entry.getDepth() != SVNDepth.EXCLUDE) {
				baseNode.depth = entry.getDepth();
			} else {
				baseNode.presence = SVNWCDbStatus.Excluded;
				baseNode.depth = SVNDepth.INFINITY;
			}
			if (entry.isDeleted()) {
				assert(baseNode.presence == SVNWCDbStatus.NotPresent);
				baseNode.kind = entry.getKind();
			} else if (entry.isAbsent()) {
				assert(baseNode.presence == SVNWCDbStatus.ServerExcluded);
				/* ### should be svn_node_unknown, but let's store what we have. */
				baseNode.kind = entry.getKind();
				/* Store the most likely revision in the node to avoid base nodes without a valid revision. Of course
	             we remember that the data is still incomplete. */
				if (baseNode.revision == ISVNWCDb.INVALID_REVNUM && parentNode != null && parentNode.base != null)
					baseNode.revision = parentNode.base.revision;
			} else {
				baseNode.kind = entry.getKind();
				if (baseNode.presence != SVNWCDbStatus.Excluded) {
					/* All subdirs are initially incomplete, they stop being incomplete when the entries file in the subdir is
	                 upgraded and remain incomplete if that doesn't happen. */
					if (entry.isDirectory() && !"".equals(entry.getName())) {
						baseNode.presence = SVNWCDbStatus.Incomplete;
						/* Store the most likely revision in the node to avoid base nodes without a valid revision. Of course
	                     we remember that the data is still incomplete. */
						if (parentNode != null && parentNode.base != null) {
							baseNode.revision = parentNode.base.revision;
						}
					} else if (entry.isIncomplete()) {
							/* ### nobody should have set the presence.  */
							assert(baseNode.presence == SVNWCDbStatus.Normal);
							baseNode.presence = SVNWCDbStatus.Incomplete;
						}
					}
			}
			
			if (entry.isDirectory()) {
				baseNode.checksum = null;
			} else {
				if (textBaseInfo != null && textBaseInfo.revertBase != null && textBaseInfo.revertBase.sha1Checksum != null) {
					baseNode.checksum = textBaseInfo.revertBase.sha1Checksum;
				} else if (textBaseInfo != null && textBaseInfo.normalBase != null && textBaseInfo.normalBase.sha1Checksum != null) {
					baseNode.checksum = textBaseInfo.normalBase.sha1Checksum;
				} else {
					baseNode.checksum = null;
				}
				 /* The base MD5 checksum is available in the entry, unless there is a copied WORKING node.  
				  * If possible, verify that the entry checksum matches the base file that we found. */
				if (!(workingNode != null && entry.isCopied())) {
					SvnChecksum entryMd5Checksum = new SvnChecksum(Kind.md5, entry.getChecksum());
					SvnChecksum foundMd5Checksum = null;
					if (textBaseInfo != null && textBaseInfo.revertBase != null && textBaseInfo.revertBase.md5Checksum != null) {
						foundMd5Checksum = textBaseInfo.revertBase.md5Checksum;
					} else if (textBaseInfo != null && textBaseInfo.normalBase != null && textBaseInfo.normalBase.md5Checksum != null) {
						foundMd5Checksum = textBaseInfo.normalBase.md5Checksum;
					}
										
					if (entryMd5Checksum.getDigest() != null && foundMd5Checksum != null && !entryMd5Checksum.equals(foundMd5Checksum)) {
						SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, 
							"Bad base MD5 checksum for ''{0}''; expected: ''{1}''; found ''{2}'';",
									SVNFileUtil.createFilePath(rootAbsPath, localRelPath), entryMd5Checksum, foundMd5Checksum);
			           SVNErrorManager.error(err, SVNLogType.WC);
					}  else {
		                  /* ### Not sure what conditions this should cover. */
		                  /* SVN_ERR_ASSERT(entry->deleted || ...); */
		            }
					
				}
			}
			
			if (SvnOldUpgrade.getEntryRepositoryRootURL(thisDir) != null) {
				baseNode.reposId = upgradeData.repositoryId;
				if (SvnOldUpgrade.getEntryURL(entry) != null) {
					String relPath = SVNURLUtil.getRelativeURL(SvnOldUpgrade.getEntryRepositoryRootURL(thisDir), SvnOldUpgrade.getEntryURL(entry), false);
					baseNode.reposRelPath = relPath != null ? relPath : "";
				} else {
					String relPath = SVNURLUtil.getRelativeURL(SvnOldUpgrade.getEntryRepositoryRootURL(thisDir), SvnOldUpgrade.getEntryURL(thisDir), false);
					if (relPath == null) {
						baseNode.reposRelPath = entry.getName();
					} else {
						baseNode.reposRelPath = SVNPathUtil.append(relPath, entry.getName());
					}
				}
			}

		      /* TODO: These values should always be present, if they are missing 
		       * during an upgrade, set a flag, and then ask the user to talk to the server.

		         Note: cmt_rev is the distinguishing value. The others may be 0 or NULL if the corresponding revprop has been deleted.  */
			
			baseNode.changedRev = entry.getCommittedRevision();
			baseNode.changedDate = SVNDate.parseDate(entry.getCommittedDate());
			baseNode.changedAuthor = entry.getAuthor();
			
			if (entry.getExternalFilePath() != null) {
				baseNode.isFileExternal = true;
			}
			
			insertNode(upgradeData.root.getSDb(), baseNode);
			
			/* We have to insert the lock after the base node, because the node
	         must exist to lookup various bits of repos related information for the abs path. */
			
			if (entry.getLockToken() != null && isCreateLocks) {
				SVNWCDbLock lock = new SVNWCDbLock();
				lock.token = entry.getLockToken();
				lock.owner = entry.getLockOwner();
				lock.comment = entry.getLockComment();
				lock.date = SVNDate.parseDate(entry.getLockCreationDate());
				
				db.addLock(tmpEntryAbsPath, lock);
			}
		}
		
		if (belowWorkingNode != null) {
			DbNode work = parentNode.belowWork != null ? parentNode.belowWork : parentNode.work;
			belowWorkingNode.wcId = upgradeData.workingCopyId;
			belowWorkingNode.localRelPath = SVNFileUtil.getFilePath(localRelPath);
			belowWorkingNode.opDepth = work.opDepth;
			belowWorkingNode.parentRelPath = parentRelPath;
			belowWorkingNode.presence = SVNWCDbStatus.Normal;
			belowWorkingNode.kind = entry.getKind();
			belowWorkingNode.reposId = upgradeData.repositoryId;
			
			if (work.reposRelPath != null) {
				belowWorkingNode.reposRelPath = SVNPathUtil.append(work.reposRelPath, entry.getName());
			} else {
				belowWorkingNode.reposRelPath = null;
			}
			belowWorkingNode.revision = parentNode.work.revision;
		    
			/* The revert_base checksum isn't available in the entry structure, so the caller provides it. */

			/* text_base_info is NULL for files scheduled to be added. */
			belowWorkingNode.checksum = null;
			if (textBaseInfo != null) {
				if (entry.isScheduledForDeletion()) {
					belowWorkingNode.checksum = textBaseInfo.normalBase.sha1Checksum;
				} else {
					belowWorkingNode.checksum = textBaseInfo.revertBase.sha1Checksum;
				}
			}
			
			belowWorkingNode.translatedSize = 0;
			belowWorkingNode.changedRev = ISVNWCDb.INVALID_REVNUM;
			belowWorkingNode.changedDate = null;
			belowWorkingNode.changedAuthor = null;
			belowWorkingNode.depth = SVNDepth.INFINITY;
			belowWorkingNode.lastModTime = null;
			belowWorkingNode.properties = null;
			
			insertNode(upgradeData.root.getSDb(), belowWorkingNode);
		}
		
		/* Insert the working node. */
		if (workingNode != null) {
			workingNode.wcId = upgradeData.workingCopyId;
			workingNode.localRelPath = SVNFileUtil.getFilePath(localRelPath);
			workingNode.parentRelPath = parentRelPath;
			workingNode.changedRev = ISVNWCDb.INVALID_REVNUM;
			workingNode.lastModTime = SVNDate.parseDate(entry.getTextTime());
			workingNode.translatedSize = entry.getWorkingSize();
			
			if (entry.getDepth() != SVNDepth.EXCLUDE) {
				workingNode.depth = entry.getDepth();
			} else {
				workingNode.presence = SVNWCDbStatus.Excluded;
				workingNode.depth = SVNDepth.INFINITY;
			}
			
			if (entry.isDirectory()) {
				workingNode.checksum = null;
			} else {
				/* text_base_info is NULL for files scheduled to be added. */
				if (textBaseInfo != null) {
					workingNode.checksum = textBaseInfo.normalBase.sha1Checksum;
				}
				/* If an MD5 checksum is present in the entry, we can verify that it matches the MD5 of the base file we found earlier. */
				/*#ifdef SVN_DEBUG
				if (entry->checksum && text_base_info)
		          {
		            svn_checksum_t *md5_checksum;
		            SVN_ERR(svn_checksum_parse_hex(&md5_checksum, svn_checksum_md5,
		                                           entry->checksum, result_pool));
		            SVN_ERR_ASSERT(
		              md5_checksum && text_base_info->normal_base.md5_checksum);
		            SVN_ERR_ASSERT(svn_checksum_match(
		              md5_checksum, text_base_info->normal_base.md5_checksum));
		          }
		         #endif*/
			}
			workingNode.kind = entry.getKind();
			if (workingNode.presence != SVNWCDbStatus.Excluded) {
				/* All subdirs start of incomplete, and stop being incomplete when the entries file in the subdir is upgraded. */
				if (entry.isDirectory() && !"".equals(entry.getName())) {
					workingNode.presence = SVNWCDbStatus.Incomplete;
					workingNode.kind = SVNNodeKind.DIR;
				} else if (entry.isScheduledForDeletion()) {
					workingNode.presence = SVNWCDbStatus.BaseDeleted;
					workingNode.kind = entry.getKind();
				} else {
					/* presence == normal  */
					workingNode.kind = entry.getKind();
					if (entry.isIncomplete()) {
						/* We shouldn't be overwriting another status.  */
						assert(workingNode.presence == SVNWCDbStatus.Normal);
						workingNode.presence = SVNWCDbStatus.Incomplete;
					}
				}
			}
			
			 /* These should generally be unset for added and deleted files,
	         and contain whatever information we have for copied files. Let's just store whatever we have.

	         Note: cmt_rev is the distinguishing value. The others may be 0 or NULL if the corresponding revprop has been deleted.  */
			if (workingNode.presence != SVNWCDbStatus.BaseDeleted) {
				workingNode.changedRev = entry.getCommittedRevision();
				workingNode.changedDate =  SVNDate.parseDate(entry.getCommittedDate());
				workingNode.changedAuthor = entry.getAuthor();
			}
			
			if (entry.isScheduledForDeletion() && parentNode != null && parentNode.work != null && parentNode.work.presence == SVNWCDbStatus.BaseDeleted) {
				workingNode.opDepth = parentNode.work.opDepth;
			} else if (!entry.isCopied()) {
				workingNode.opDepth = SVNWCUtils.relpathDepth(localRelPath);
			}
			
			insertNode(upgradeData.root.getSDb(), workingNode);
		}
		
		/* Insert the actual node. */
		if (actualNode != null) {
			actualNode.wcId = upgradeData.workingCopyId;
			actualNode.localRelPath = SVNFileUtil.getFilePath(localRelPath);
			actualNode.parentRelPath = parentRelPath;
			insertActualNode(upgradeData.root.getSDb(), actualNode);
		}
		
		WriteBaton entryNode = null;
		if (isCalculateEntryNode) {
			entryNode = new WriteBaton();
			entryNode.base = baseNode;
			entryNode.work = workingNode;
			entryNode.belowWork = belowWorkingNode;
			entryNode.treeConflicts = treeConflicts;
		}
		
		if (entry.getExternalFilePath() != null) {
			/* TODO: Maybe add a file external registration inside EXTERNALS here, 
            to allow removing file externals that aren't referenced from svn:externals.
      		The svn:externals values are processed anyway after everything is upgraded */
		}
		return entryNode;
	}
	
	/* No transaction required: called from write_entry which is itself transaction-wrapped. */
	private static void insertNode(SVNSqlJetDb sDb, DbNode node) throws SVNException {
		assert(node.opDepth > 0 || node.reposRelPath != null);
		SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.INSERT_NODE);
        try {
            stmt.bindf("isisnnnnsnrisnnni",
                    node.wcId,
                    node.localRelPath == null ? "" : node.localRelPath,
                    node.opDepth,
                    node.parentRelPath,
                    /* Setting depth for files? */
                    (node.kind == SVNNodeKind.DIR) ? SVNDepth.asString(node.depth) : null,
                    node.changedRev,
                    node.changedDate != null ? node.changedDate : null,
                    node.changedAuthor,
                    node.lastModTime
                    );

            if (node.reposRelPath != null) {
                stmt.bindLong(5, node.reposId);
                stmt.bindString(6, node.reposRelPath);
                stmt.bindLong(7, node.revision);
            }

            stmt.bindString(8, SvnWcDbStatementUtil.getPresenceText(node.presence));

            if (node.kind == SVNNodeKind.NONE)
                stmt.bindString(10, "unknown");
            else
                stmt.bindString(10, node.kind.toString());

            if (node.kind == SVNNodeKind.FILE)
                stmt.bindChecksum(14, node.checksum);

            if (node.properties != null)  /* ### Never set, props done later */
                stmt.bindProperties(15, node.properties);

            if (node.translatedSize != ISVNWCDb.INVALID_FILESIZE)
                stmt.bindLong(16, node.translatedSize);

            if (node.isFileExternal)
                stmt.bindLong(20, 1);

            stmt.done();
        } finally {
            stmt.reset();
        }
	}
	
	private static void insertActualNode(SVNSqlJetDb sDb, DbActualNode actualNode) throws SVNException {
		SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.INSERT_ACTUAL_NODE);
        try {
            stmt.bindLong(1, actualNode.wcId);
            stmt.bindString(2, actualNode.localRelPath);
            stmt.bindString(3, actualNode.parentRelPath);
            if (actualNode.properties != null)
                stmt.bindProperties(4, actualNode.properties);
            if (actualNode.conflictOld != null) {
                stmt.bindString(5, actualNode.conflictOld);
                stmt.bindString(6, actualNode.conflictNew);
                stmt.bindString(7, actualNode.conflictWorking);
            }
            if (actualNode.propReject != null)
                stmt.bindString(8, actualNode.propReject);
            if (actualNode.changelist != null)
                stmt.bindString(9, actualNode.changelist);
            if (actualNode.treeConflictData != null)
                stmt.bindString(10, actualNode.treeConflictData);
            stmt.done();
        } finally {
            stmt.reset();
        }
	}
	
	private static void writeActualOnlyEntries(Map<String, String> treeConflicts, SVNSqlJetDb sDb, long wcId, String dirRelPath) throws SVNException {
		for (Iterator<String> items = treeConflicts.keySet().iterator(); items.hasNext();) {
			String path = items.next();
			DbActualNode actualNode = new DbActualNode();
			actualNode.wcId = wcId;
			actualNode.localRelPath = path;
			actualNode.parentRelPath = dirRelPath;
			actualNode.treeConflictData = treeConflicts.get(path);
			insertActualNode(sDb, actualNode);
		}
	}
	
 }
