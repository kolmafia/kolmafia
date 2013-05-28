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
package org.tmatesoft.svn.core.internal.util.jna;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNGLibrary extends Library {

    String g_get_application_name();

    void g_set_application_name(String applicationName);

    Pointer g_main_loop_new(Pointer context, boolean isRunning);

    void g_main_loop_run(Pointer context);

    void g_main_loop_quit(Pointer context);

    class GList extends Structure {

        public GList(Pointer p) {
            super(p);
        }

        public Pointer data;
        public Pointer next;
        public Pointer previous;

        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "next", "previous");
        }
    }
}
