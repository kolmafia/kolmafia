/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNStreamGobbler;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSHooks {

    public static final String SVN_REPOS_HOOK_START_COMMIT = "start-commit";
    public static final String SVN_REPOS_HOOK_PRE_COMMIT = "pre-commit";
    public static final String SVN_REPOS_HOOK_POST_COMMIT = "post-commit";
    public static final String SVN_REPOS_HOOK_PRE_REVPROP_CHANGE = "pre-revprop-change";
    public static final String SVN_REPOS_HOOK_POST_REVPROP_CHANGE = "post-revprop-change";
    public static final String SVN_REPOS_HOOK_PRE_LOCK = "pre-lock";
    public static final String SVN_REPOS_HOOK_POST_LOCK = "post-lock";
    public static final String SVN_REPOS_HOOK_PRE_UNLOCK = "pre-unlock";
    public static final String SVN_REPOS_HOOK_POST_UNLOCK = "post-unlock";
    public static final String SVN_REPOS_HOOK_READ_SENTINEL = "read-sentinels";
    public static final String SVN_REPOS_HOOK_WRITE_SENTINEL = "write-sentinels";
    public static final String SVN_REPOS_HOOK_DESC_EXT = ".tmpl";
    public static final String SVN_REPOS_HOOKS_DIR = "hooks";
    public static final String REVPROP_DELETE = "D";
    public static final String REVPROP_ADD = "A";
    public static final String REVPROP_MODIFY = "M";
    private static final String[] winExtensions = {
            ".exe", ".bat", ".cmd"
    };
    
    private static Boolean ourIsHooksEnabled;
    
    public static void setHooksEnabled(boolean enabled) {
        ourIsHooksEnabled = enabled ? Boolean.TRUE : Boolean.FALSE;
    }
    
    public static boolean isHooksEnabled() {
        if (ourIsHooksEnabled == null) {
            ourIsHooksEnabled = Boolean.valueOf(System.getProperty("svnkit.hooksEnabled", System.getProperty("javasvn.hooksEnabled", "true")));
        }
        return ourIsHooksEnabled.booleanValue();
    }

    public static String runPreLockHook(File reposRootDir, String path, String username, String comment, boolean stealLock) throws SVNException {
        username = username == null ? "" : username;
        path = path == null ? "" : path;
        return runHook(reposRootDir, SVN_REPOS_HOOK_PRE_LOCK, new String[] {path, username, comment != null ? comment : "", stealLock ? "1" : "0"}, null);
    }

    public static void runPostLockHook(File reposRootDir, String[] paths, String username) throws SVNException {
        StringBuffer pathsStr = new StringBuffer();
        for (int i = 0; i < paths.length; i++) {
            pathsStr.append(paths[i]);
            pathsStr.append("\n");
        }
        runLockHook(reposRootDir, SVN_REPOS_HOOK_POST_LOCK, null, username, pathsStr.toString());
    }

    public static void runPreUnlockHook(File reposRootDir, String path, String username) throws SVNException {
        runLockHook(reposRootDir, SVN_REPOS_HOOK_PRE_UNLOCK, path, username, null);
    }

    public static void runPostUnlockHook(File reposRootDir, String[] paths, String username) throws SVNException {
        StringBuffer pathsStr = new StringBuffer();
        for (int i = 0; i < paths.length; i++) {
            pathsStr.append(paths[i]);
            pathsStr.append("\n");
        }
        runLockHook(reposRootDir, SVN_REPOS_HOOK_POST_UNLOCK, null, username, pathsStr.toString());
    }

    private static void runLockHook(File reposRootDir, String hookName, String path, String username, String paths) throws SVNException {
        username = username == null ? "" : username;
        path = path == null ? "" : path;
        byte[] bytes = null;
        try {
            bytes = paths == null ? null : paths.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = paths.getBytes();
        }
        runHook(reposRootDir, hookName, new String[] {path, username}, bytes);
    }

    public static void runPreRevPropChangeHook(File reposRootDir, String propName, byte[] propNewValue, String author, long revision, String action) throws SVNException {
        runChangeRevPropHook(reposRootDir, SVN_REPOS_HOOK_PRE_REVPROP_CHANGE, propName, propNewValue, author, revision, action, true);
    }

    public static void runPostRevPropChangeHook(File reposRootDir, String propName, byte[] propOldValue, String author, long revision, String action) throws SVNException {
        runChangeRevPropHook(reposRootDir, SVN_REPOS_HOOK_POST_REVPROP_CHANGE, propName, propOldValue, author, revision, action, false);
    }
    
    private static void runChangeRevPropHook(File reposRootDir, String hookName, String propName, byte[] propValue, String author, long revision, String action, boolean isPre) throws SVNException {
        File hookFile = getHookFile(reposRootDir, hookName);
        if (hookFile == null) {
            if (isPre) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_DISABLED_FEATURE,
                        "Repository has not been enabled to accept revision propchanges;\nask the administrator to create a pre-revprop-change hook");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            return;
        } 
        author = author == null ? "" : author;
        runHook(reposRootDir, hookName, new String[] {String.valueOf(revision), author, propName, action}, propValue);
    }

    public static void runStartCommitHook(File reposRootDir, String author, List<?> capabilities) throws SVNException {
        author = author == null ? "" : author;
        String capsString = getCapabilitiesAsString(capabilities);
        String[] args = capsString == null ? new String[] { author } : new String[] { author, capsString }; 
        runHook(reposRootDir, SVN_REPOS_HOOK_START_COMMIT, args, null);
    }

    public static void runPreCommitHook(File reposRootDir, String txnName) throws SVNException {
        runHook(reposRootDir, SVN_REPOS_HOOK_PRE_COMMIT, new String[] {txnName}, null);
    }

    public static void runPostCommitHook(File reposRootDir, long committedRevision) throws SVNException {
        runHook(reposRootDir, SVN_REPOS_HOOK_POST_COMMIT, new String[] {String.valueOf(committedRevision)}, null);
    }

    private static String runHook(File reposRootDir, String hookName, String[] args, byte[] input) throws SVNException {
        File hookFile = getHookFile(reposRootDir, hookName);
        if (hookFile == null) {
            return null;
        }
        if (args == null) {
            args = new String[0];
        }
        Process hookProc = null;
        String reposPath = reposRootDir.getAbsolutePath().replace(File.separatorChar, '/');
        String executableName = hookFile.getName().toLowerCase();
        boolean useCmd = (executableName.endsWith(".bat") || executableName.endsWith(".cmd")) && SVNFileUtil.isWindows;
        String[] cmd = useCmd ? new String[4 + args.length] : new String[2 + args.length];
        
        if (useCmd) {
            cmd = new String[] {"cmd", "/C", ""};
            cmd[2] = "\"" + "\"" + hookFile.getAbsolutePath() + "\" \"" + reposPath + "\"";
            for (int i = 0; i < args.length; i++) {
                cmd[2] += " \"" + args[i] + "\"";
            }
            cmd[2] += "\"";
        } else {
            int i = 0;
            if (useCmd) {
                cmd[0] = "cmd";
                cmd[1] = "/C";
                i = 2;
            }
            cmd[i] = hookFile.getAbsolutePath();
            i++;
            cmd[i] = reposPath;
            i++;
            for(int j = 0; j < args.length; j++) {
                cmd[i + j] = args[j];
            }
        }
        try {
            hookProc = Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "Failed to start ''{0}'' hook: {1}", new Object[] {
                    hookFile, ioe.getLocalizedMessage()
            });
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }
        return feedHook(hookFile, hookName, hookProc, input);
    }


    private static String feedHook(File hook, String hookName, Process hookProcess, byte[] stdInValue) throws SVNException {
        if (hookProcess == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "Failed to start ''{0}'' hook", hook);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        SVNStreamGobbler inputGobbler = new SVNStreamGobbler(hookProcess.getInputStream());
        SVNStreamGobbler errorGobbler = new SVNStreamGobbler(hookProcess.getErrorStream());
        inputGobbler.start();
        errorGobbler.start();

        if (stdInValue != null) {
            OutputStream osToStdIn = hookProcess.getOutputStream();
            try {
                for (int i = 0; i < stdInValue.length; i += 1024) {
                    osToStdIn.write(stdInValue, i, Math.min(1024, stdInValue.length - i));
                    osToStdIn.flush();
                }
            } catch (IOException ioe) {
                // 
            } finally {
                SVNFileUtil.closeFile(osToStdIn);
            }
        }

        int rc = -1;
        try {
            inputGobbler.waitFor();
            errorGobbler.waitFor();
            rc = hookProcess.waitFor();
        } catch (InterruptedException ie) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "Failed to start ''{0}'' hook: {1}", new Object[] {
                    hook, ie.getLocalizedMessage()
            });
            SVNErrorManager.error(err, ie, SVNLogType.FSFS);
        } finally {
            errorGobbler.close();
            inputGobbler.close();
            hookProcess.destroy();
        }

        if (rc == 0 ) {
            if (errorGobbler.getError() != null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "''{0}'' hook succeeded, but error output could not be read", hookName);
                SVNErrorManager.error(err, errorGobbler.getError(), SVNLogType.FSFS);
            }
            return inputGobbler.getResult();
        } else {
            String actionName = null;
            if (SVN_REPOS_HOOK_START_COMMIT.equals(hookName) || SVN_REPOS_HOOK_PRE_COMMIT.equals(hookName)) {
                actionName = "Commit";
            } else if (SVN_REPOS_HOOK_PRE_REVPROP_CHANGE.equals(hookName)) {
                actionName = "Revprop change";
            } else if (SVN_REPOS_HOOK_PRE_LOCK.equals(hookName)) {
                actionName = "Lock";
            } else if (SVN_REPOS_HOOK_PRE_UNLOCK.equals(hookName)) {
                actionName = "Unlock";
            }
            String stdErrMessage = errorGobbler.getError() != null ? "[Error output could not be read.]" : errorGobbler.getResult();
            String errorMessage = actionName != null ? 
                    actionName + " blocked by {0} hook (exit code {1})" : "{0} hook failed (exit code {1})";
            if (stdErrMessage != null && stdErrMessage.length() > 0) {
                errorMessage += " with output:\n{2}";
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, errorMessage, new Object[] {hookName, new Integer(rc), stdErrMessage});
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } else {
                errorMessage += " with no output.";
            }
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, errorMessage, new Object[] {hookName, new Integer(rc)});
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return null;
    }

    private static File getHookFile(File reposRootDir, String hookName) throws SVNException {
        if (!isHooksEnabled()) {
            return null;
        }
        File hookFile = null;
        if (SVNFileUtil.isWindows) {
            for (int i = 0; i < winExtensions.length; i++) {
                hookFile = new File(getHooksDir(reposRootDir), hookName + winExtensions[i]);
                SVNFileType type = SVNFileType.getType(hookFile);
                if (type == SVNFileType.FILE) {
                    return hookFile;
                }
            }
        } else {
            hookFile = new File(getHooksDir(reposRootDir), hookName);
            SVNFileType type = SVNFileType.getType(hookFile);
            if (type == SVNFileType.FILE) {
                return hookFile;
            } else if (type == SVNFileType.SYMLINK) {
                // should first resolve the symlink and then decide if it's
                // broken and
                // throw an exception
                File realFile = SVNFileUtil.resolveSymlinkToFile(hookFile);
                if (realFile == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "Failed to run ''{0}'' hook; broken symlink", hookFile);
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                return hookFile;
            }
        }
        return null;
    }

    private static File getHooksDir(File reposRootDir) {
        return new File(reposRootDir, SVN_REPOS_HOOKS_DIR);
    }
    
    private static String getCapabilitiesAsString(List<?> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < capabilities.size(); i++) {
            Object cap = capabilities.get(i); 
            buffer.append(cap);
            if (i < capabilities.size() - 1) {
                buffer.append(":");
            }
        }
        return buffer.toString();
    }
}
