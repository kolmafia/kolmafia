/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17.db.statement;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public enum SVNWCDbSchema {

    WCROOT(WCROOT__Fields.class, WCROOT__Indices.class),

    LOCK(LOCK__Fields.class),

    REPOSITORY(REPOSITORY__Fields.class, REPOSITORY__Indices.class),

    ACTUAL_NODE(ACTUAL_NODE__Fields.class, ACTUAL_NODE__Indices.class),

    WC_LOCK(WC_LOCK__Fields.class),

    PRISTINE(PRISTINE__Fields.class),

    NODES(NODES__Fields.class, NODES__Indices.class),

    WORK_QUEUE(WORK_QUEUE__Fields.class),
    
    EXTERNALS(EXTERNALS__Fields.class),
    
    TARGETS_LIST(TARGETS_LIST__Fields.class, TARGETS_LIST__Indices.class),

    NODE_PROPS_CACHE(NODE_PROPS_CACHE__Fields.class),

    DELETE_LIST( DELETE_LIST__Fields.class),

    REVERT_LIST( REVERT_LIST__Fields.class),
    
    CHANGELIST_LIST(CHANGELIST_LIST__Fields.class),
    //19 version of sDb
    BASE_NODE(BASE_NODE__Fields.class),
    //19 version of sDb
    WORKING_NODE(WORKING_NODE__Fields.class)
    ;

    final public Class<? extends Enum<?>> fields;
    final public Class<? extends Enum<?>> indices;

    private SVNWCDbSchema(Class<? extends Enum<?>> fields) {
        this.fields = fields;
        this.indices = Empty.class;
    }

    private SVNWCDbSchema(Class<? extends Enum<?>> fields, Class<? extends Enum<?>> indices) {
        this.fields = fields;
        this.indices = indices;
    }

    public enum Empty {
    }

    public enum WCROOT__Fields {
        id, local_abspath;
    }

    public enum WCROOT__Indices {
        I_LOCAL_ABSPATH;
    }

    public enum LOCK__Fields {
        repos_id, repos_relpath, lock_token, lock_owner, lock_comment, lock_date
    }

    public enum REPOSITORY__Fields {
        id, root, uuid
    }

    public enum REPOSITORY__Indices {
        I_UUID, I_ROOT
    }

    public enum ACTUAL_NODE__Fields {
        wc_id, local_relpath, parent_relpath, properties, conflict_old, conflict_new, conflict_working, prop_reject, changelist, text_mod, tree_conflict_data, conflict_data, older_checksum, left_checksum, right_checksum;
    }

    public enum ACTUAL_NODE__Indices {
        I_ACTUAL_PARENT, I_ACTUAL_CHANGELIST;
    }

    public enum WC_LOCK__Fields {
        wc_id, local_dir_relpath, locked_levels;
    }

    public enum PRISTINE__Fields {
        checksum, compression, size, refcount, md5_checksum
    }

    public enum NODES__Fields {
        wc_id, local_relpath, op_depth, parent_relpath, repos_id, repos_path, revision, presence, moved_here, moved_to, kind, properties, depth, checksum, symlink_target, changed_revision, changed_date, changed_author, translated_size, last_mod_time, dav_cache, file_external;
    }

    public enum NODES__Indices {
        I_NODES_PARENT;
    }

    public enum WORK_QUEUE__Fields {
        id, work;
    }

    public enum EXTERNALS__Fields {
        wc_id, local_relpath, parent_relpath, repos_id, presence, kind, def_local_relpath, def_repos_relpath, def_operational_revision, def_revision;
    }
    
    public enum REVPROP__Fields {
        properties;
    }
    public enum TARGETS_LIST__Fields {
        wc_id, local_relpath, parent_relpath, kind;
    }
    public enum TARGETS_LIST__Indices {
        targets_list_kind;
    }
    
    public enum NODE_PROPS_CACHE__Fields {
        local_Relpath, kind, properties;
    }

    public enum DELETE_LIST__Fields {
        local_relpath;
    }

    public enum REVERT_LIST__Fields {
        local_relpath, actual, conflict_old, conflict_new, conflict_working, prop_reject, notify, op_depth, repos_id, kind;
    }
    
    public enum CHANGELIST_LIST__Fields {
    	wc_id, local_relpath, notify, changelist;
    }
    
    public enum BASE_NODE__Fields {
        wc_id, local_relpath, parent_relpath, repos_id, repos_path, revnum, presence, kind, properties, depth, checksum, symlink_target, changed_rev, changed_date, changed_author, translated_size, last_mod_time, dav_cache, file_external;
    }
    
    public enum WORKING_NODE__Fields {
        wc_id, local_relpath, parent_relpath, copyfrom_repos_id, copyfrom_repos_path, copyfrom_revnum, presence, kind, properties, depth, checksum, symlink_target, changed_rev, changed_date, changed_author, translated_size, last_mod_time;
    }
    
    
}
