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
package org.tmatesoft.svn.core.internal.wc.admin;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNCharsetInputStream;
import org.tmatesoft.svn.core.internal.util.SVNCharsetOutputStream;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.SVNAdminUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNTranslator {

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    
    private static CodingErrorAction onMalformedInputAction = CodingErrorAction.REPORT;
    private static CodingErrorAction onUnmappableCharacterAction = CodingErrorAction.REPORT;

    public static String translateString(String str, byte[] eol, Map<String, byte[]> keywords, boolean repair, boolean expand) throws SVNException {
        ByteArrayOutputStream bufferOS = new ByteArrayOutputStream();
        OutputStream resultOS = null;
        try {
            resultOS = getTranslatingOutputStream(bufferOS, null, eol, repair, keywords, expand);
            resultOS.write(str.getBytes());
        } catch (IOException e) {
            translationError(null, e);
        } finally {
            SVNFileUtil.closeFile(resultOS);
        }

        return new String(bufferOS.toByteArray());
    }

    public static void translate(SVNAdminArea adminArea, String name, String srcPath,
                                 String dstPath, boolean expand) throws SVNException {
        translate(adminArea, name, srcPath, dstPath, false, expand);
    }

    public static void translate(SVNAdminArea adminArea, String name, String srcPath,
                                 String dstPath, boolean safelyEncode, boolean expand) throws SVNException {
        translate(adminArea, name, adminArea.getFile(srcPath), adminArea.getFile(dstPath), null, safelyEncode, expand);
    }
    public static void translate(SVNAdminArea adminArea, String name, String srcPath,
                                 String dstPath, String customEOLStyle, boolean expand) throws SVNException {
        translate(adminArea, name, adminArea.getFile(srcPath), adminArea.getFile(dstPath), customEOLStyle, expand);
    }

    public static void translate(SVNAdminArea adminArea, String name, File src,
                                 File dst, boolean expand) throws SVNException {
        translate(adminArea, name, src, dst, null, expand);
    }

    public static void translate(SVNAdminArea adminArea, String name, File src,
                                 File dst, boolean safelyEncode, boolean expand) throws SVNException {
        translate(adminArea, name, src, dst, null,safelyEncode, expand);
    }

    public static void translate(SVNAdminArea adminArea, String name, File src,
                                 File dst, String customEOLStyle, boolean expand) throws SVNException {
        translate(adminArea, name, src, dst, customEOLStyle, false, expand);
    }

    public static void translate(SVNAdminArea adminArea, String name, File src,
                                 File dst, String customEOLStyle, boolean safelyEncode, boolean expand) throws SVNException {
        ISVNOptions options = adminArea.getWCAccess().getOptions();
        SVNVersionedProperties props = adminArea.getProperties(name);
        String keywords = props.getStringPropertyValue(SVNProperty.KEYWORDS);
        String mimeType = props.getStringPropertyValue(SVNProperty.MIME_TYPE);
        String charset = getCharset(props.getStringPropertyValue(SVNProperty.CHARSET), mimeType, adminArea.getFile(name).getPath(), options);
        String eolStyle = null;
        if (customEOLStyle != null) {
            eolStyle = customEOLStyle;
        } else {
            eolStyle = props.getStringPropertyValue(SVNProperty.EOL_STYLE);
        }
        boolean special = props.getPropertyValue(SVNProperty.SPECIAL) != null;
        Map<String, byte[]> keywordsMap = null;
        byte[] eols;
        if (keywords != null) {
            if (expand) {
                SVNEntry entry = adminArea.getVersionedEntry(name, true);
                String url = entry.getURL();
                String repositoryRoot = entry.getRepositoryRoot();
                String author = entry.getAuthor();
                String date = entry.getCommittedDate();
                String rev = Long.toString(entry.getCommittedRevision());
                keywordsMap = computeKeywords(keywords, url, repositoryRoot, author, date, rev, options);
            } else {
                keywordsMap = computeKeywords(keywords, null, null, null, null, null, null);
            }
        }
        if (!expand) {
            eols = getBaseEOL(eolStyle);
        } else {
            eols = getEOL(eolStyle, options);
        }

        if (expand && charset != null && safelyEncode) {
            File tmp = SVNAdminUtil.createTmpFile(adminArea, name, ".tmp", true);
            try {
                translate(src, tmp, charset, eols, keywordsMap, special, false);
                translate(tmp, dst, charset, eols, keywordsMap, special, true);
            } finally {
                SVNFileUtil.deleteFile(tmp);
            }
        } else {
            translate(src, dst, charset, eols, keywordsMap, special, expand);
        }
    }

    public static void translate(File src, File dst, String charset, byte[] eol, Map<String, byte[]> keywords, boolean special, boolean expand) throws SVNException {
        if (src == null || dst == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS), SVNLogType.DEFAULT);
            return;
        }
        if (src.equals(dst)) {
            return;
        }
        if (special) {
            if (SVNFileType.getType(dst) != SVNFileType.NONE) {
                dst.delete();
            }
            if (!SVNFileUtil.symlinksSupported()) {
                SVNFileUtil.copyFile(src, dst, true);
            } else if (expand) {
                // create symlink to target, and create it at dst
                SVNFileUtil.createSymlink(dst, src);
            } else {
                SVNFileUtil.detranslateSymlink(src, dst);
            }
            return;
        }
        if ((charset == null || SVNProperty.isUTF8(charset)) && eol == null && (keywords == null || keywords.isEmpty())) {
            // no expansion, fast copy.
            SVNFileUtil.copyFile(src, dst, false, false);
            return;
        }
        OutputStream os = SVNFileUtil.openFileForWriting(dst);
        OutputStream tos = getTranslatingOutputStream(os, charset, eol, true, keywords, expand);
        InputStream is = SVNFileUtil.openFileForReading(src, SVNLogType.WC);
        try {
            copy(is, tos);
        } catch (IOException e) {
            translationError(dst, e);
        } finally {
            SVNFileUtil.closeFile(tos);
            SVNFileUtil.closeFile(os);
            SVNFileUtil.closeFile(is);
        }
    }

    public static InputStream getTranslatedStream(SVNAdminArea adminArea, String name, boolean translateToNormalForm, boolean repairEOL) throws SVNException {
        ISVNOptions options = adminArea.getWCAccess().getOptions();
        SVNVersionedProperties props = adminArea.getProperties(name);
        String mimeType = props.getStringPropertyValue(SVNProperty.MIME_TYPE);
        String charset = getCharset(props.getStringPropertyValue(SVNProperty.CHARSET), mimeType, adminArea.getFile(name).getPath(), options);
        String eolStyle = props.getStringPropertyValue(SVNProperty.EOL_STYLE);
        String keywords = props.getStringPropertyValue(SVNProperty.KEYWORDS);
        boolean special = props.getPropertyValue(SVNProperty.SPECIAL) != null;
        File src = adminArea.getFile(name);
        if (special) {
            if (!SVNFileUtil.symlinksSupported()) {
                return SVNFileUtil.openFileForReading(src, SVNLogType.WC);
            }
            if (SVNFileType.getType(src) != SVNFileType.SYMLINK) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot detranslate symbolic link ''{0}''; file does not exist or not a symbolic link", src);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            String linkPath = SVNFileUtil.getSymlinkName(src);
            if (linkPath == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot detranslate symbolic link ''{0}''; file does not exist or not a symbolic link", src);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                os.write("link ".getBytes("UTF-8"));
                os.write(linkPath.getBytes("UTF-8"));
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
            } finally {
                SVNFileUtil.closeFile(os);
            }
            return new ByteArrayInputStream(os.toByteArray());
        }
        boolean translationRequired = special || keywords != null || eolStyle != null || charset != null;
        if (translationRequired) {
            byte[] eol = getBaseEOL(eolStyle);
            if (translateToNormalForm) {
                if (eolStyle != null && eol == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL);
                    SVNErrorManager.error(err, SVNLogType.DEFAULT);
                }
                Map<String, byte[]> keywordsMap = computeKeywords(keywords, null, null, null, null, null, null);
                boolean repair = (eolStyle != null && eol != null && !SVNProperty.EOL_STYLE_NATIVE.equals(eolStyle)) || repairEOL;
                return getTranslatingInputStream(SVNFileUtil.openFileForReading(src, SVNLogType.WC), charset, eol, repair, keywordsMap, false);
            }

            SVNEntry entry = adminArea.getVersionedEntry(name, false);
            String url = entry.getURL();
            String repositoryRoot = entry.getRepositoryRoot();
            String author = entry.getAuthor();
            String date = entry.getCommittedDate();
            String rev = Long.toString(entry.getCommittedRevision());
            Map<String, byte[]> keywordsMap = computeKeywords(keywords, url, repositoryRoot, author, date, rev, options);
            return getTranslatingInputStream(SVNFileUtil.openFileForReading(src, SVNLogType.WC), charset, eol, true, keywordsMap, true);
        }
        return SVNFileUtil.openFileForReading(src, SVNLogType.WC);
    }

    public static File getTranslatedFile(SVNAdminArea dir, String name, File src, boolean forceEOLRepair, boolean useGlobalTmp, boolean forceCopy, boolean toNormalFormat) throws SVNException {
        ISVNOptions options = dir.getWCAccess().getOptions();
        SVNVersionedProperties props = dir.getProperties(name);
        String mimeType = props.getStringPropertyValue(SVNProperty.MIME_TYPE);
        String charset = getCharset(props.getStringPropertyValue(SVNProperty.CHARSET), mimeType, dir.getFile(name).getPath(), options);
        String eolStyle = props.getStringPropertyValue(SVNProperty.EOL_STYLE);
        String keywords = props.getStringPropertyValue(SVNProperty.KEYWORDS);
        boolean special = props.getPropertyValue(SVNProperty.SPECIAL) != null;
        boolean needsTranslation = charset != null || eolStyle != null || keywords != null || special;
        File result = null;
        if (!needsTranslation && !forceCopy) {
            result = src;
        } else {
            if (useGlobalTmp) {
                result = SVNFileUtil.createTempFile("svndiff", ".tmp");
            } else {
                result = SVNAdminUtil.createTmpFile(dir, name, ".tmp", true);
            }
            if (toNormalFormat) {
                translateToNormalForm(src, result, charset, eolStyle, forceEOLRepair, keywords, special);
            } else {
                SVNEntry entry = dir.getVersionedEntry(name, false);
                String url = entry.getURL();
                String repositoryRoot = entry.getRepositoryRoot();
                String author = entry.getAuthor();
                String date = entry.getCommittedDate();
                String rev = Long.toString(entry.getCommittedRevision());
                Map<String, byte[]> keywordsMap = computeKeywords(keywords, url, repositoryRoot, author, date, rev, options);
                copyAndTranslate(src, result, charset, getEOL(eolStyle, options), keywordsMap, special, true, true);
            }
        }
        return result;
    }

    public static File maybeUpdateTargetEOLs(SVNAdminArea dir, File target, SVNProperties propDiff) throws SVNException {
        String eolStyle = null;
        if (propDiff != null && propDiff.containsName(SVNProperty.EOL_STYLE) && propDiff.getStringValue(SVNProperty.EOL_STYLE) != null) {
            eolStyle = propDiff.getStringValue(SVNProperty.EOL_STYLE);
            ISVNOptions options = dir.getWCAccess().getOptions();
            byte[] eol = getEOL(eolStyle, options);
            File tmpFile = SVNAdminUtil.createTmpFile(dir);
            copyAndTranslate(target, tmpFile, null, eol, null, false, false, true);
            return tmpFile;
        }
        return target;
    }

    public static File detranslateWorkingCopy(SVNAdminArea dir, String name, SVNProperties propDiff, boolean force) throws SVNException {
        SVNVersionedProperties props = dir.getProperties(name);
        boolean isLocalBinary = SVNProperty.isBinaryMimeType(props.getStringPropertyValue(SVNProperty.MIME_TYPE));

        String mimeType = null;
        String charsetProp = null;
        String eolStyle = null;
        String keywords = null;
        boolean isSpecial = false;
        boolean isRemoteHasBinary = propDiff != null && propDiff.containsName(SVNProperty.MIME_TYPE);
        boolean isRemoteBinaryRemoved = isRemoteHasBinary && !SVNProperty.isBinaryMimeType(propDiff.getStringValue(SVNProperty.MIME_TYPE));
        boolean isRemoteBinary = isRemoteHasBinary && SVNProperty.isBinaryMimeType(propDiff.getStringValue(SVNProperty.MIME_TYPE));

        if (!isLocalBinary && isRemoteBinary) {
            isSpecial = props.getPropertyValue(SVNProperty.SPECIAL) != null;
            keywords = props.getStringPropertyValue(SVNProperty.KEYWORDS);
            charsetProp = props.getStringPropertyValue(SVNProperty.CHARSET);
            mimeType = props.getStringPropertyValue(SVNProperty.MIME_TYPE);
        } else if (!isLocalBinary || isRemoteBinaryRemoved) {
            isSpecial = props.getPropertyValue(SVNProperty.SPECIAL) != null;
            if (!isSpecial) {
                if (propDiff != null && propDiff.getStringValue(SVNProperty.EOL_STYLE) != null) {
                    eolStyle = propDiff.getStringValue(SVNProperty.EOL_STYLE);
                } else if (!isLocalBinary) {
                    eolStyle = props.getStringPropertyValue(SVNProperty.EOL_STYLE);
                }

                if (propDiff != null && propDiff.getStringValue(SVNProperty.CHARSET) != null) {
                    charsetProp = propDiff.getStringValue(SVNProperty.CHARSET);
                    mimeType = propDiff.getStringValue(SVNProperty.MIME_TYPE);
                } else if (!isLocalBinary) {
                    charsetProp = props.getStringPropertyValue(SVNProperty.CHARSET);
                    mimeType = props.getStringPropertyValue(SVNProperty.MIME_TYPE);
                }

                if (!isLocalBinary) {
                    keywords = props.getStringPropertyValue(SVNProperty.KEYWORDS);
                }
            }
        }

        File detranslatedFile = null;
        ISVNOptions options = dir.getWCAccess().getOptions();
        String charset = getCharset(charsetProp, mimeType, dir.getFile(name).getPath(), options);
        if (force || charset != null || keywords != null || eolStyle != null || isSpecial) {
            File tmpFile = SVNAdminUtil.createTmpFile(dir);
            translateToNormalForm(dir.getFile(name), tmpFile, charset, eolStyle, true, keywords, isSpecial);
            detranslatedFile = tmpFile;
        } else {
            detranslatedFile = dir.getFile(name);
        }

        return detranslatedFile;
    }

    private static void translateToNormalForm(File source, File destination, String charset, String eolStyle, boolean alwaysRepairEOLs, String keywords, boolean isSpecial) throws SVNException {
        byte[] eol = getBaseEOL(eolStyle);
        if (eolStyle != null && eol == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        Map<String, byte[]> keywordsMap = computeKeywords(keywords, null, null, null, null, null, null);
        boolean repair = (eolStyle != null && eol != null && !SVNProperty.EOL_STYLE_NATIVE.equals(eolStyle)) || alwaysRepairEOLs;
        copyAndTranslate(source, destination, charset, eol, keywordsMap, isSpecial, false, repair);
    }

    public static void copyAndTranslate(File source, File destination, String charset, byte[] eol, Map<String, byte[]> keywords, boolean special, boolean expand, boolean repair) throws SVNException {
        boolean isSpecialPath = false;
        if (SVNFileUtil.symlinksSupported()) {
            SVNFileType type = SVNFileType.getType(source);
            isSpecialPath = type == SVNFileType.SYMLINK;
        }

        if (special || isSpecialPath) {
            if (destination.exists()) {
                destination.delete();
            }
            if (!SVNFileUtil.symlinksSupported()) {
                SVNFileUtil.copyFile(source, destination, true);
            } else if (expand) {
                if (isSpecialPath) {
                    SVNFileUtil.createSymlink(destination, SVNFileUtil.getSymlinkName(source));
                } else {
                    SVNFileUtil.createSymlink(destination, source);
                }
            } else {
                SVNFileUtil.detranslateSymlink(source, destination);
            }
            return;

        }
        if ((charset == null || SVNProperty.isUTF8(charset)) && eol == null && (keywords == null || keywords.isEmpty())) {
            // no expansion, fast copy.
            SVNFileUtil.copyFile(source, destination, false);
            return;
        }

        OutputStream dst = null;
        InputStream src = null;
        OutputStream translatingStream = null;
        try {
            dst = SVNFileUtil.openFileForWriting(destination);
            src = SVNFileUtil.openFileForReading(source, SVNLogType.WC);
            translatingStream = getTranslatingOutputStream(dst, charset, eol, repair, keywords, expand);
            SVNTranslator.copy(src, translatingStream);
        } catch (IOExceptionWrapper ew) {
            if (ew.getOriginalException().getErrorMessage().getErrorCode() == SVNErrorCode.IO_INCONSISTENT_EOL) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_INCONSISTENT_EOL, "File ''{0}'' has inconsistent newlines", source);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            throw ew.getOriginalException();
        } catch (IOException e) {
            translationError(destination, e);
        } finally {
            try {
                if (dst != null) {
                    try {
                        dst.flush();
                    } catch (IOException ioe) {
                        checkWrappedException(ioe, source);
                    }
                }
                if (translatingStream != null) {
                    try {
                        translatingStream.flush();
                    } catch (IOException ioe) {
                        checkWrappedException(ioe, source);
                    }
                }
            } finally {
                SVNFileUtil.closeFile(src);
                SVNFileUtil.closeFile(translatingStream);
                SVNFileUtil.closeFile(dst);
            }
        }
    }

    private static void checkWrappedException(IOException ioe, File file) throws SVNException {
        if(ioe instanceof IOExceptionWrapper) {
            IOExceptionWrapper ew = (IOExceptionWrapper)ioe;
            if (ew.getOriginalException().getErrorMessage().getErrorCode() == SVNErrorCode.IO_INCONSISTENT_EOL) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_INCONSISTENT_EOL, "File ''{0}'' has inconsistent newlines", file);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            throw ew.getOriginalException();
        }
    }

    public static boolean checkNewLines(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return true;
        }
        InputStream is = null;
        try {
            is = SVNFileUtil.openFileForReading(file, SVNLogType.WC);
            int r;
            byte[] lastFoundEOL = null;
            byte[] currentEOL = null;
            while ((r = is.read()) >= 0) {
                if (r == '\n') {
                    currentEOL = SVNProperty.EOL_LF_BYTES;
                } else if (r == '\r') {
                    currentEOL = SVNProperty.EOL_CR_BYTES;
                    r = is.read();
                    if (r == '\n') {
                        currentEOL = SVNProperty.EOL_CRLF_BYTES;
                    }
                }
                if (lastFoundEOL == null) {
                    lastFoundEOL = currentEOL;
                } else if (currentEOL != null && lastFoundEOL != currentEOL) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        } catch (SVNException e) {
            return false;
        } finally {
            SVNFileUtil.closeFile(is);
        }
        return true;
    }

    public static void copy(InputStream src, OutputStream dst) throws IOException {
        byte[] buffer = new byte[8192];
        while (true) {
            int read = src.read(buffer);
            if (read < 0) {
                return;
            } else if (read == 0) {
                continue;
            }
            dst.write(buffer, 0, read);
        }
    }

    public static OutputStream getTranslatingOutputStream(OutputStream out, String charset, byte[] eol, boolean repair, Map<String, byte[]> keywords, boolean expand) {
        if (charset == null || SVNProperty.isUTF8(charset)) {
            return new SVNTranslatorOutputStream(out, eol, repair, keywords, expand);
        }
        Charset cs = Charset.forName(charset);
        byte[] lf = SVNProperty.EOL_LF_BYTES;
        if (expand) {
            if (eol != null) {
                byte[] convertedEOL = convertEOL(lf, UTF8_CHARSET, cs);
                boolean encodingConvertsEOL = !Arrays.equals(lf, convertedEOL);
                if (Arrays.equals(convertedEOL, eol) && encodingConvertsEOL) {
                    out = new SVNCharsetOutputStream(out, UTF8_CHARSET, cs, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
                    if (keywords != null) {
                        out = new SVNTranslatorOutputStream(out, null, false, keywords, expand);
                    }
                    return out;
                }
            }
            out = new SVNCharsetOutputStream(out, UTF8_CHARSET, cs, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
            return new SVNTranslatorOutputStream(out, eol, repair, keywords, expand);
        }
        if (eol != null) {
            byte[] convertedEOL = convertEOL(eol, cs, UTF8_CHARSET);
            boolean encodingConvertsEOL = !Arrays.equals(eol, convertedEOL);
            if (Arrays.equals(convertedEOL, lf) && encodingConvertsEOL) {
                if (keywords != null) {
                    out = new SVNTranslatorOutputStream(out, null, false, keywords, expand);
                }
                return new SVNCharsetOutputStream(out, cs, UTF8_CHARSET, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
            }
        }
        out = new SVNTranslatorOutputStream(out, eol, repair, keywords, expand);
        return new SVNCharsetOutputStream(out, cs, UTF8_CHARSET, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
    }

    public static InputStream getTranslatingInputStream(InputStream in, String charset, byte[] eol, boolean repair, Map<String, byte[]> keywords, boolean expand) {
        if (charset == null || SVNProperty.isUTF8(charset)) {
            return new SVNTranslatorInputStream(in, eol, repair, keywords, expand);
        }
        final Charset cs = Charset.forName(charset);
        byte[] lf = SVNProperty.EOL_LF_BYTES;
        if (expand) {
            if (eol != null) {
                byte[] convertedEOL = convertEOL(lf, UTF8_CHARSET, cs);
                boolean encodingConvertsEOL = !Arrays.equals(convertedEOL, lf);
                if (Arrays.equals(eol, convertedEOL) && encodingConvertsEOL) {
                    if (keywords != null) {
                        in = new SVNTranslatorInputStream(in, null, false, keywords, expand);
                    }
                    return new SVNCharsetInputStream(in, UTF8_CHARSET, cs, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
                }
            }
            in = new SVNTranslatorInputStream(in, eol, repair, keywords, expand);
            return new SVNCharsetInputStream(in, UTF8_CHARSET, cs, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
        }
        if (eol != null) {
            byte[] convertedEOL = convertEOL(eol, cs, UTF8_CHARSET);
            boolean charsetConvertsEOL = !Arrays.equals(convertedEOL, eol);
            if (Arrays.equals(lf, convertedEOL) && charsetConvertsEOL) {
                in = new SVNCharsetInputStream(in, cs, UTF8_CHARSET, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
                if (keywords != null) {
                    in = new SVNTranslatorInputStream(in, null, false, keywords, expand);
                }
                return in;
            }
        }
        in = new SVNCharsetInputStream(in, cs, UTF8_CHARSET, getOnMalformedInputAction(), getOnUnmappableCharacterAction());
        return new SVNTranslatorInputStream(in, eol, repair, keywords, expand);
    }

    private static byte[] convertEOL(byte[] eol, Charset from, Charset to) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(eol);
        CharBuffer charBuffer = from.decode(byteBuffer);
        ByteBuffer resultBuffer = to.encode(charBuffer);
        byte[] result = new byte[resultBuffer.limit()];
        resultBuffer.get(result, 0, result.length);
        return result;
    }

    public static Map<String, byte[]> computeKeywords(String keywords, String locationUrl, String repositoryRoot, String a, String d, String r, ISVNOptions options) {
        if (keywords == null) {
            return Collections.emptyMap();
        }
        boolean expand = locationUrl != null;
        boolean expandCustomKeywords = expand; //if we want to prevent custom keywords from expanding, change this
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        try {
            SVNKeywordFormatter keywordFormatter = new SVNKeywordFormatter(expand, expandCustomKeywords, locationUrl == null ? null : SVNURL.parseURIEncoded(locationUrl), repositoryRoot == null ? null : SVNURL.parseURIEncoded(repositoryRoot), a, d, r, options);

            for (StringTokenizer tokens = new StringTokenizer(keywords, " \t\n\b\r\f"); tokens.hasMoreTokens();) {
                String token = tokens.nextToken();
                String customFormat = null;
                if (expandCustomKeywords) {
                    int pos = token.lastIndexOf('=');
                    if (pos >= 0) {
                        customFormat = token.substring(pos + 1);
                        token = token.substring(0, pos);
                    }
                }
                if (customFormat != null) {
                    byte[] customValue = keywordFormatter.format(customFormat);
                    map.put(token, customValue);
                } else if ("LastChangedDate".equals(token) || "Date".equalsIgnoreCase(token)) {
                    byte[] date = keywordFormatter.format("%D");
                    map.put("LastChangedDate", date);
                    map.put("Date", date);
                } else
                if ("LastChangedRevision".equals(token) || "Revision".equals(token) || "Rev".equalsIgnoreCase(token)) {
                    byte[] revision = keywordFormatter.format("%r");
                    map.put("LastChangedRevision", revision);
                    map.put("Revision", revision);
                    map.put("Rev", revision);
                } else if ("LastChangedBy".equals(token) || "Author".equalsIgnoreCase(token)) {
                    byte[] author = keywordFormatter.format("%a");
                    map.put("LastChangedBy", author);
                    map.put("Author", author);
                } else if ("HeadURL".equals(token) || "URL".equalsIgnoreCase(token)) {
                    byte[] url = keywordFormatter.format("%u");
                    map.put("HeadURL", url);
                    map.put("URL", url);
                } else if ("Id".equalsIgnoreCase(token)) {
                    byte[] id = keywordFormatter.format("%b %r %d %a");
                    map.put("Id", expand ? id : null);
                } else if ("Header".equalsIgnoreCase(token)) {
                    byte[] header = keywordFormatter.format("%u %r %d %a");
                    map.put("Header", expand ? header : null);
                }
            }
        } catch (SVNException e) {
            //
        }
        return map;
    }

    public static byte[] getEOL(String eolStyle, ISVNOptions options) {
        if (SVNProperty.EOL_STYLE_NATIVE.equals(eolStyle)) {
            return options.getNativeEOL();
        } else if (SVNProperty.EOL_STYLE_LF.equals(eolStyle)) {
            return SVNProperty.EOL_LF_BYTES;
        } else if (SVNProperty.EOL_STYLE_CR.equals(eolStyle)) {
            return SVNProperty.EOL_CR_BYTES;
        } else if (SVNProperty.EOL_STYLE_CRLF.equals(eolStyle)) {
            return SVNProperty.EOL_CRLF_BYTES;
        }
        return null;
    }

    public static byte[] getBaseEOL(String eolStyle) {
        if (SVNProperty.EOL_STYLE_NATIVE.equals(eolStyle)) {
            return SVNProperty.EOL_LF_BYTES;
        } else if (SVNProperty.EOL_STYLE_CR.equals(eolStyle)) {
            return SVNProperty.EOL_CR_BYTES;
        } else if (SVNProperty.EOL_STYLE_LF.equals(eolStyle)) {
            return SVNProperty.EOL_LF_BYTES;
        } else if (SVNProperty.EOL_STYLE_CRLF.equals(eolStyle)) {
            return SVNProperty.EOL_CRLF_BYTES;
        }
        return null;
    }

    public static String getCharset(String charset, String mimeType, Object path, ISVNOptions options) throws SVNException {
        if (charset == null) {
            charset = getGlobalCharset(options, mimeType);
        }
        if (SVNProperty.NATIVE.equals(charset)) {
            charset = options.getNativeCharset();
        }
        if (charset == null) {
            return null;
        }
        boolean isSupported;
        try {
            isSupported = Charset.isSupported(charset);
        } catch (IllegalCharsetNameException e) {
            isSupported = false;
        }
        if (!isSupported) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Charset ''{0}'' is not supported on this computer; change svnkit:charset property value or remove that property for file ''{1}''",
                    new Object[]{charset, path}), SVNLogType.DEFAULT);
        }
        return charset;
    }

    private static String getGlobalCharset(ISVNOptions options, String mimeType) {
        if (options instanceof DefaultSVNOptions && SVNProperty.isTextMimeType(mimeType)) {
            DefaultSVNOptions defaults = (DefaultSVNOptions) options;
            return defaults.getGlobalCharset();
        }
        return null;
    }


    public static void translationError(File path, IOException e) throws SVNException {
        SVNErrorMessage error;
        if (path != null) {
            error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Failed to translate ''{0}''", new Object[]{path});
        } else {
            error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Translation failed");
        }
        Throwable cause = e;
        if (e instanceof IOExceptionWrapper) {
            IOExceptionWrapper wrapper = (IOExceptionWrapper) e;
            SVNException wrappedException = wrapper.getOriginalException();
            if (wrappedException != null) {
                error.setChildErrorMessage(wrappedException.getErrorMessage());
                cause = wrappedException;
            }
        }
        SVNErrorManager.error(error, cause, SVNLogType.DEFAULT);
    }
    
    public static synchronized CodingErrorAction getOnMalformedInputAction() {
        return onMalformedInputAction;
    }

    public static synchronized CodingErrorAction getOnUnmappableCharacterAction() {
        return onUnmappableCharacterAction;
    }

    public static synchronized void setEncoderActions(CodingErrorAction onMalformedInput, CodingErrorAction onUnmappableCharacter) {
        onMalformedInputAction = onMalformedInput;
        onUnmappableCharacterAction = onUnmappableCharacter;
    }

    private static class SVNKeywordFormatter {
        private boolean expand;
        private boolean expandCustomKeywords;
        private String authorString;
        private String dateString;
        private String revisionString;
        private ISVNOptions options;
        private byte[] date;
        private byte[] idDate;
        private byte[] url;
        private byte[] baseUrl;
        private byte[] repositoryRoot;
        private byte[] reposRelPath;
        private byte[] rev;
        private byte[] author;
        private byte[] name;
        private byte[] id;
        private byte[] header;
        private Date javaDate;
        private SVNURL locationUrl;
        private SVNURL repositoryRootUrl;

        private SVNKeywordFormatter(boolean expand, boolean expandCustomKeywords, SVNURL locationUrl, SVNURL repositoryRootUrl, String authorString, String dateString, String revisionString, ISVNOptions options) {
            this.expand = expand;
            this.expandCustomKeywords = expandCustomKeywords;
            this.authorString = authorString;
            this.dateString = dateString;
            this.revisionString = revisionString;
            this.options = options;
            this.locationUrl = locationUrl;
            this.repositoryRootUrl = repositoryRootUrl;
            date = null;
            idDate = null;
            url = null;
            rev = null;
            author = null;
            name = null;
            id = null;
            header = null;
        }

        private byte[] format(String format) throws SVNException {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                int pos;
                while (true) {
                    pos = 0;

                    while (pos < format.length() && format.charAt(pos) != '%') {
                        pos++;
                    }

                    byteArrayOutputStream.write(format.substring(0, pos).getBytes("UTF-8"));

                    if (pos == format.length()) {
                        break;
                    }

                    if (pos + 1 == format.length()) {
                        byteArrayOutputStream.write(format.charAt(pos));
                        pos--;
                    } else {
                        switch (format.charAt(pos + 1)) {
                            case 'a':
                                byte[] author = getAuthor();
                                if (author != null) {
                                    byteArrayOutputStream.write(author);
                                }
                                break;
                            case 'b':
                                byte[] baseUrl = getName();
                                if (baseUrl != null) {
                                    byteArrayOutputStream.write(baseUrl);
                                }
                                break;
                            case 'd':
                                byte[] idDate = getIdDate();
                                if (idDate != null) {
                                    byteArrayOutputStream.write(idDate);
                                }
                                break;
                            case 'D':
                                byte[] date = getDate();
                                if (date != null) {
                                    byteArrayOutputStream.write(date);
                                }
                                break;
                            case 'P':
                                byte[] reposRelPath = getReposRelPath();
                                if (reposRelPath != null) {
                                    byteArrayOutputStream.write(reposRelPath);
                                }
                                break;
                            case 'R':
                                byte[] repositoryRoot = getRepositoryRoot();
                                if (repositoryRoot != null) {
                                    byteArrayOutputStream.write(repositoryRoot);
                                }
                                break;
                            case 'r':
                                final byte[] revision = getRevision();
                                if (revision != null) {
                                    byteArrayOutputStream.write(revision);
                                }
                                break;
                            case 'u':
                                final byte[] url = getUrl();
                                if (url != null) {
                                    byteArrayOutputStream.write(url);
                                }
                                break;
                            case '_':
                                byteArrayOutputStream.write(' ');
                                break;
                            case '%':
                                byteArrayOutputStream.write('%');
                                break;
                            case 'H':
                                byteArrayOutputStream.write(format("%P%_%r%_%d%_%a"));
                                break;
                            case 'I':
                                byteArrayOutputStream.write(format("%b%_%r%_%d%_%a"));
                                break;
                            default:
                                byteArrayOutputStream.write(format.charAt(pos));
                                byteArrayOutputStream.write(format.charAt(pos + 1));
                                break;
                        }
                    }

                    format = format.substring(pos + 2);
                }
                return byteArrayOutputStream.toByteArray();
            } catch (UnsupportedEncodingException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.CLIENT);
            } finally {
                SVNFileUtil.closeFile(byteArrayOutputStream);
            }
            return null;
        }

        private Date getJavaDate() {
            if (javaDate == null) {
                javaDate = dateString == null ? null : SVNDate.parseDate(dateString);
            }
            return javaDate;
        }

        private byte[] getAuthor() throws UnsupportedEncodingException {
            if (author == null) {
                author = expand ? (authorString == null ? new byte[0] : authorString.getBytes("UTF-8")) : author;
            }
            return author;
        }

        private byte[] getDate() throws UnsupportedEncodingException {
            if (date == null) {
                date = expand ? SVNDate.formatHumanDate(getJavaDate(), options).getBytes("UTF-8") : date;
            }
            return date;
        }

        private byte[] getRevision() throws UnsupportedEncodingException {
            if (rev == null) {
                rev = expand ? revisionString.getBytes("UTF-8") : rev;
            }
            return rev;
        }

        private byte[] getUrl() throws UnsupportedEncodingException {
            if (url == null) {
                if (locationUrl != null) {
                    url = expand ? locationUrl.toString().getBytes("UTF-8") : url;
                }
            }
            return url;
        }

        private byte[] getBaseUrl() throws UnsupportedEncodingException, SVNException {
            if (baseUrl == null) {
                if (locationUrl != null) {
                    baseUrl = locationUrl.removePathTail().toString().getBytes("UTF-8");
                }
            }
            return baseUrl;
        }

        private byte[] getRepositoryRoot() throws UnsupportedEncodingException {
            if (repositoryRoot == null) {
                if (repositoryRootUrl != null) {
                    repositoryRoot = repositoryRootUrl.toString().getBytes("UTF-8");
                }
            }
            return repositoryRoot;
        }

        private byte[] getReposRelPath() throws UnsupportedEncodingException {
            if (reposRelPath == null) {
                if (repositoryRootUrl != null && locationUrl != null) {
                    reposRelPath = SVNPathUtil.getRelativePath(repositoryRootUrl.toDecodedString(), locationUrl.toDecodedString()).getBytes("UTF-8");
                }
            }
            return reposRelPath;
        }

        private byte[] getName() throws UnsupportedEncodingException {
            if (name == null) {
                if (locationUrl != null) {
                    name = SVNEncodingUtil.uriDecode(SVNPathUtil.tail(locationUrl.toDecodedString())).getBytes("UTF-8");
                }
            }
            return name;
        }

        private byte[] getIdDate() throws UnsupportedEncodingException {
            if (idDate == null) {
                idDate = SVNDate.formatShortDate(getJavaDate()).getBytes("UTF-8");
            }
            return idDate;
        }

        private byte[] getId() throws IOException {
            if (id == null) {
                if (expand) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bos.write(getName());
                    bos.write(' ');
                    bos.write(getRevision());
                    bos.write(' ');
                    bos.write(getIdDate());
                    bos.write(' ');
                    bos.write(getAuthor());
                    bos.close();
                    id = bos.toByteArray();
                }
            }
            return id;
        }

        private byte[] getHeader() throws IOException {
            if (header == null) {
                if (expand) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bos.write(getUrl());
                    bos.write(' ');
                    bos.write(getRevision());
                    bos.write(' ');
                    bos.write(getIdDate());
                    bos.write(' ');
                    bos.write(getAuthor());
                    bos.close();
                    header = bos.toByteArray();
                }
            }
            return header;
        }
    }
}
