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
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.internal.util.SVNHashMap;



/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNFileListUtil {
    
    private static boolean ourIsCompositionEnabled = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("svnkit.fs.composeFileNames", "true"));

    public static synchronized void setCompositionEnabled(boolean enabled) {
        ourIsCompositionEnabled = enabled;
    }

    public static synchronized boolean isCompositionEnabled() {
        return ourIsCompositionEnabled;
    }

    /**
     * This method is a replacement for file.list(), which composes decomposed file names (e.g. umlauts in file names on the Mac).
     */
    private static String[] list(File directory) {
        if (!SVNFileUtil.isOSX) {
            return directory.list();
        }
        final String[] fileNames = directory.list();
        if (fileNames == null) {
            return null;
        }

        final String[] composedFileNames = new String[fileNames.length];
        for (int i = 0; i < composedFileNames.length; i++) {
            if (!isCompositionEnabled()) {
                composedFileNames[i] = decompose(fileNames[i]);
            } else {
                composedFileNames[i] = compose(fileNames[i]);
            }
        }
        return composedFileNames;
    }

    /**
     * This method is a replacement for file.listFiles(), which composes decomposed file names (e.g. umlauts in file names on the Mac).
     */
    public static File[] listFiles(File directory) {
        if (SVNFileUtil.isOSX) {
            final String[] fileNames = list(directory);
            if (fileNames == null) {
                return null;
            }
    
            File[] files = new File[fileNames.length];
            for (int i = 0; i < files.length; i++) {
                files[i] = new File(directory.getPath(), fileNames[i]);
            }
            return files;
        } else if (SVNFileUtil.isOpenVMS) {
            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                return files;
            }
            File[] processed = new File[files.length];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String name = file.getName();
                if (file.isFile() && name.endsWith(".")) {
                    // chances there is a file without extension and '.' added by openVMS.
                    name = name.substring(0, name.lastIndexOf('.'));
                    file = new File(directory, name);
                    if (file.exists() && file.isFile()) {
                        processed[i] = file;
                        continue;
                    }
                } 
                processed[i] = file;
            }
            return processed;
        }

	    final File[] files = directory.listFiles();
	    return files != null ? sort(files) : null;
    }
    
    private static File[] sort(File[] files) {
        final Map<String,File> map = new SVNHashMap();
        for (int i = 0; i < files.length; i++) {
            map.put(files[i].getName(), files[i]);
        }
        return map.values().toArray(new File[map.size()]);
    }

    private static String compose(String decomposedString) {
        if (decomposedString == null) {
            return null;
        }

        StringBuffer buffer = null;
        for (int i = 1, length = decomposedString.length(); i < length; i++) {
            final char chr = decomposedString.charAt(i);
            if (chr == '\u0300') { // grave `
                buffer = compose(i, "AaEeIiOoUu", "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9", decomposedString, buffer);
            }
            else if (chr == '\u0301') { // acute '
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD", decomposedString, buffer);
            }
            else if (chr == '\u0302') { // circumflex ^
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177", decomposedString, buffer);
            }
            else if (chr == '\u0303') { // tilde ~
                buffer = compose(i, "AaNnOoUu", "\u00C3\u00E3\u00D1\u00F1\u00D5\u00F5\u0168\u0169", decomposedString, buffer);
            }
            else if (chr == '\u0308') { // umlaut/dieresis (two dots above)
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF", decomposedString, buffer);
            }
            else if (chr == '\u030A') { // ring above (as in Angstrom)
                buffer = compose(i, "Aa", "\u00C5\u00E5", decomposedString, buffer);
            }
            else if (chr == '\u0327') { // cedilla ,
                buffer = compose(i, "Cc", "\u00C7\u00E7", decomposedString, buffer);
            }
            else if (buffer != null) {
                buffer.append(chr);
            }
        }

        if (buffer == null) {
            return decomposedString;
        }

        return buffer.toString();
    }

    private static String decompose(String composedString) {
        if (composedString == null) {
            return null;
        }

        StringBuffer buffer = null;
        for (int i = 0, length = composedString.length(); i < length; i++) {
            final char chr = composedString.charAt(i);
            switch (chr) {
            case '\u00C0': buffer = decompose("A\u0300", i, composedString, buffer); break;
            case '\u00C1': buffer = decompose("A\u0301", i, composedString, buffer); break;
            case '\u00C2': buffer = decompose("A\u0302", i, composedString, buffer); break;
            case '\u00C3': buffer = decompose("A\u0303", i, composedString, buffer); break;
            case '\u00C4': buffer = decompose("A\u0308", i, composedString, buffer); break;
            case '\u00C5': buffer = decompose("A\u030A", i, composedString, buffer); break;
            case '\u00C7': buffer = decompose("C\u0327", i, composedString, buffer); break;
            case '\u00C8': buffer = decompose("E\u0300", i, composedString, buffer); break;
            case '\u00C9': buffer = decompose("E\u0301", i, composedString, buffer); break;
            case '\u00CA': buffer = decompose("E\u0302", i, composedString, buffer); break;
            case '\u00CB': buffer = decompose("E\u0308", i, composedString, buffer); break;
            case '\u00CC': buffer = decompose("I\u0300", i, composedString, buffer); break;
            case '\u00CD': buffer = decompose("I\u0301", i, composedString, buffer); break;
            case '\u00CE': buffer = decompose("I\u0302", i, composedString, buffer); break;
            case '\u00CF': buffer = decompose("I\u0308", i, composedString, buffer); break;
            case '\u00D1': buffer = decompose("N\u0303", i, composedString, buffer); break;
            case '\u00D2': buffer = decompose("O\u0300", i, composedString, buffer); break;
            case '\u00D3': buffer = decompose("O\u0301", i, composedString, buffer); break;
            case '\u00D4': buffer = decompose("O\u0302", i, composedString, buffer); break;
            case '\u00D5': buffer = decompose("O\u0303", i, composedString, buffer); break;
            case '\u00D6': buffer = decompose("O\u0308", i, composedString, buffer); break;
            case '\u00D9': buffer = decompose("U\u0300", i, composedString, buffer); break;
            case '\u00DA': buffer = decompose("U\u0301", i, composedString, buffer); break;
            case '\u00DB': buffer = decompose("U\u0302", i, composedString, buffer); break;
            case '\u00DC': buffer = decompose("U\u0308", i, composedString, buffer); break;
            case '\u00DD': buffer = decompose("Y\u0301", i, composedString, buffer); break;
            case '\u00E0': buffer = decompose("a\u0300", i, composedString, buffer); break;
            case '\u00E1': buffer = decompose("a\u0301", i, composedString, buffer); break;
            case '\u00E2': buffer = decompose("a\u0302", i, composedString, buffer); break;
            case '\u00E3': buffer = decompose("a\u0303", i, composedString, buffer); break;
            case '\u00E4': buffer = decompose("a\u0308", i, composedString, buffer); break;
            case '\u00E5': buffer = decompose("a\u030A", i, composedString, buffer); break;
            case '\u00E7': buffer = decompose("c\u0327", i, composedString, buffer); break;
            case '\u00E8': buffer = decompose("e\u0300", i, composedString, buffer); break;
            case '\u00E9': buffer = decompose("e\u0301", i, composedString, buffer); break;
            case '\u00EA': buffer = decompose("e\u0302", i, composedString, buffer); break;
            case '\u00EB': buffer = decompose("e\u0308", i, composedString, buffer); break;
            case '\u00EC': buffer = decompose("i\u0300", i, composedString, buffer); break;
            case '\u00ED': buffer = decompose("i\u0301", i, composedString, buffer); break;
            case '\u00EE': buffer = decompose("i\u0302", i, composedString, buffer); break;
            case '\u00EF': buffer = decompose("i\u0308", i, composedString, buffer); break;
            case '\u00FF': buffer = decompose("y\u0308", i, composedString, buffer); break;
            case '\u00F1': buffer = decompose("n\u0303", i, composedString, buffer); break;
            case '\u00F2': buffer = decompose("o\u0300", i, composedString, buffer); break;
            case '\u00F3': buffer = decompose("o\u0301", i, composedString, buffer); break;
            case '\u00F4': buffer = decompose("o\u0302", i, composedString, buffer); break;
            case '\u00F5': buffer = decompose("o\u0303", i, composedString, buffer); break;
            case '\u00F6': buffer = decompose("o\u0308", i, composedString, buffer); break;
            case '\u00F9': buffer = decompose("u\u0300", i, composedString, buffer); break;
            case '\u00FA': buffer = decompose("u\u0301", i, composedString, buffer); break;
            case '\u00FB': buffer = decompose("u\u0302", i, composedString, buffer); break;
            case '\u00FC': buffer = decompose("u\u0308", i, composedString, buffer); break;
            case '\u00FD': buffer = decompose("y\u0301", i, composedString, buffer); break;
            case '\u0168': buffer = decompose("U\u0303", i, composedString, buffer); break;
            case '\u0169': buffer = decompose("u\u0303", i, composedString, buffer); break;
            case '\u0176': buffer = decompose("Y\u0302", i, composedString, buffer); break;
            case '\u0177': buffer = decompose("y\u0302", i, composedString, buffer); break;
            case '\u0178': buffer = decompose("Y\u0308", i, composedString, buffer); break;
            default:
                if (buffer != null) {
                    buffer.append(chr);
                }
                break;
            }
        }

        if (buffer == null) {
            return composedString;
        }

        return buffer.toString();
    }

    // Utils ==================================================================

    private static StringBuffer decompose(String replacement, int index, String composedString, StringBuffer buffer) {
        if (buffer == null) {
            buffer = new StringBuffer(composedString.length());
            buffer.append(composedString, 0, index);
        }
        buffer.append(replacement);
        return buffer;
    }
    
    private static StringBuffer compose(int i, String decomposedChars, String composedChars, String decomposedString, StringBuffer buffer) {
        final char previousChar = decomposedString.charAt(i - 1);
        final int decomposedIndex = decomposedChars.indexOf(previousChar);
        if (decomposedIndex >= 0) {
            if (buffer == null) {
                buffer = new StringBuffer(decomposedString.length() + 2);
                buffer.append(decomposedString.substring(0, i - 1));
            }
            else {
                buffer.delete(buffer.length() - 1, buffer.length());
            }

            buffer.append(composedChars.charAt(decomposedIndex));
        }
        else {
            if (buffer == null) {
                buffer = new StringBuffer(decomposedString.length() + 2);
                buffer.append(decomposedString.substring(0, i));
            }
        }
        return buffer;
    }

    

}
