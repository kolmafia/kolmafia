package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.ISVNFileContentFetcher;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc2.ISvnAddParameters;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.hooks.ISvnPropertyValueProvider;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgPropertiesManager {

    public static Collection<String> getGlobalIgnores(ISVNOptions options) {
        Collection<String> allPatterns = new HashSet<String>();
        String[] ignores = options.getIgnorePatterns();
        for (int i = 0; ignores != null && i < ignores.length; i++) {
            allPatterns.add(ignores[i]);
        }
        return allPatterns;
        
    }
    
    public static Collection<String> getEffectiveIgnores(SVNWCContext context, File absPath, Collection<String> globalIgnores) {
        Collection<String> allPatterns = new HashSet<String>();
        if (globalIgnores != null) {
            allPatterns.addAll(globalIgnores);
        } else {
            allPatterns.addAll(getGlobalIgnores(context.getOptions()));
        }
        
        if (context != null && absPath != null) {
            try {
                String ignoreProperty = context.getProperty(absPath, SVNProperty.IGNORE);
                if (ignoreProperty != null) {
                    for (StringTokenizer tokens = new StringTokenizer(ignoreProperty, "\r\n"); tokens.hasMoreTokens();) {
                        String token = tokens.nextToken().trim();
                        if (token.length() > 0) {
                            allPatterns.add(token);
                        }
                    }
                }
            } catch (SVNException e) {
            }
        }
        return allPatterns;
    }

    public static boolean isIgnored(String name, Collection<String> patterns) {
        if (patterns == null) {
            return false;
        }
        for (Iterator<String> ps = patterns.iterator(); ps.hasNext();) {
            String pattern = (String) ps.next();
            if (DefaultSVNOptions.matches(pattern, name)) {
                return true;
            }
        }
        return false;
    }

    public static void setProperty(final SVNWCContext context, File path, final String propertyName, final SVNPropertyValue propertyValue, SVNDepth depth, final boolean skipChecks, 
            final ISVNEventHandler eventHandler, Collection<String> changelists) throws SVNException {
        setProperty(context, path, propertyName, propertyValue, null, depth, skipChecks, eventHandler, null, changelists);
    }

    public static void setProperty(final SVNWCContext context, File path, final String propertyName, final SVNPropertyValue propertyValue, SVNDepth depth, final boolean skipChecks, 
            final ISVNEventHandler eventHandler, final ISvnObjectReceiver<SVNPropertyData> receiver, Collection<String> changelists) throws SVNException {
        setProperty(context, path, propertyName, propertyValue, null, depth, skipChecks, eventHandler, receiver, changelists);
    }

    public static void setProperty(final SVNWCContext context, File path, final String propertyName, final SVNPropertyValue propertyValue, final ISvnPropertyValueProvider pvProvider, SVNDepth depth, final boolean skipChecks, 
            final ISVNEventHandler eventHandler, final ISvnObjectReceiver<SVNPropertyData> receiver, final Collection<String> changelists) throws SVNException {
        if (propertyName != null) {
            if (SVNProperty.isEntryProperty(propertyName)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_PROP_KIND, "Property ''{0}'' is an entry property", propertyName);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (SVNProperty.isWorkingCopyProperty(propertyName)) {
                SVNProperties wcProps = context.getDb().getBaseDavCache(path);
                if (wcProps == null && propertyValue != null) {
                    wcProps = new SVNProperties();
                }
                if (wcProps != null) {
                    if (propertyValue != null) {
                        wcProps.put(propertyName, propertyValue);
                    } else {
                        wcProps.remove(propertyName);
                    }
                }
                context.getDb().setBaseDavCache(path, wcProps);
                return;
            }
        }
        SVNNodeKind kind = context.readKind(path, true);
        File dirPath;
        if (kind == SVNNodeKind.DIR) {
            dirPath = path;
        } else {
            dirPath = SVNFileUtil.getParentFile(path);
        }
        context.writeCheck(dirPath);
        if (depth == SVNDepth.EMPTY) {
            if (!context.matchesChangelist(path, changelists)) {
                return;
            }
            setProperty(context, path, kind, propertyName, propertyValue, pvProvider, skipChecks, eventHandler, receiver);
        } else {
            context.nodeWalkChildren(path, new SVNWCContext.ISVNWCNodeHandler() {
                public void nodeFound(File localAbspath, SVNWCDbKind kind) throws SVNException {
                    try {
                        setProperty(context, localAbspath, kind.toNodeKind(), propertyName, propertyValue, pvProvider, skipChecks, eventHandler, receiver);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ILLEGAL_TARGET ||
                                e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_INVALID_SCHEDULE) {
                            return;
                        }
                        throw e;
                    }
                }
            }, false, depth, changelists);
        }
    }

    public static void setProperty(final SVNWCContext context, final File path, SVNNodeKind kind, String propertyName, SVNPropertyValue value, 
            ISvnPropertyValueProvider pvProvider, boolean skipChecks, ISVNEventHandler eventHandler, ISvnObjectReceiver<SVNPropertyData> receiver) throws SVNException {
        Structure<NodeInfo> nodeInfo = context.getDb().readInfo(path, NodeInfo.status);
        ISVNWCDb.SVNWCDbStatus status = nodeInfo.get(NodeInfo.status);
        if (status != SVNWCDbStatus.Normal && status != SVNWCDbStatus.Added && status != SVNWCDbStatus.Incomplete) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE,
                    "Can''t set properties on ''{0}'': invalid status for updating properties.",
                    path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (pvProvider == null && value != null && SVNProperty.isSVNProperty(propertyName)) {            
            final ISVNFileContentFetcher fetcher = new ISVNFileContentFetcher() {                
                public SVNPropertyValue getProperty(String propertyName) throws SVNException {
                    return context.getPropertyValue(path, propertyName);
                }                
                public boolean fileIsBinary() throws SVNException {
                    SVNPropertyValue mimeType = context.getPropertyValue(path, SVNProperty.MIME_TYPE);
                    return mimeType != null && SVNProperty.isBinaryMimeType(mimeType.getString());
                }                
                public void fetchFileContent(OutputStream os) throws SVNException {
                    InputStream is = null;
                    try {
                        is = SVNFileUtil.openFileForReading(path);
                        SVNTranslator.copy(is, os);
                    } catch (IOExceptionWrapper ioew) {
                        throw ioew.getOriginalException();
                    } catch (IOException e) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    } finally {
                        SVNFileUtil.closeFile(is);
                    }
                }
            };
            SVNPropertyValue pv = SVNPropertiesManager.validatePropertyValue(path, kind, propertyName, value, skipChecks, context.getOptions(), fetcher);
            value = pv;
        }
        SVNSkel workItems = null;
        if (pvProvider == null && kind == SVNNodeKind.FILE && (SVNProperty.EXECUTABLE.equals(propertyName) || SVNProperty.NEEDS_LOCK.equals(propertyName))) {
            workItems = context.wqBuildSyncFileFlags(path);
        }        
        
        SVNProperties properties = new SVNProperties(); 
        try {
            properties = context.getDb().readProperties(path);
        } catch (SVNException e) {
            SVNErrorMessage err = e.getErrorMessage().wrap("Failed to load current properties");
            SVNErrorManager.error(err, e, SVNLogType.WC);
        }
        if (pvProvider != null) {
            
            SVNPropertyValue oldKeywords = properties.getSVNPropertyValue(SVNProperty.KEYWORDS);
            SVNPropertyValue oldEolStyle = properties.getSVNPropertyValue(SVNProperty.EOL_STYLE);
            boolean wasExecutable = properties.containsName(SVNProperty.EXECUTABLE);
            boolean wasNeedsLock = properties.containsName(SVNProperty.NEEDS_LOCK);
            
            properties = pvProvider.providePropertyValues(path, properties);
            
            if (properties == null) {
                properties = new SVNProperties();
            }
            
            SVNPropertyValue newKeywords = properties.getSVNPropertyValue(SVNProperty.KEYWORDS);
            SVNPropertyValue newEolStyle = properties.getSVNPropertyValue(SVNProperty.EOL_STYLE);
            boolean isExecutable = properties.containsName(SVNProperty.EXECUTABLE);
            boolean isNeedsLock = properties.containsName(SVNProperty.NEEDS_LOCK);

            if (isExecutable != wasExecutable || isNeedsLock != wasNeedsLock) {
                workItems = context.wqBuildSyncFileFlags(path);
            }
            
            boolean clearRecordedInfo = !equals(oldKeywords, newKeywords) || !equals(oldEolStyle, newEolStyle);
            
            context.getDb().opSetProps(path, properties, null, clearRecordedInfo, workItems);
            if (workItems != null) {
                context.wqRun(path);
            }
            return;
        }

        boolean clearRecordedInfo = false;
        if (kind == SVNNodeKind.FILE && SVNProperty.KEYWORDS.equals(propertyName)) {
            String oldValue = properties.getStringValue(SVNProperty.KEYWORDS);
            Map<String, byte[]> keywords = oldValue != null ? context.getKeyWords(path, oldValue) : new HashMap<String, byte[]>();
            Map<String, byte[]> newKeywords = value != null ? context.getKeyWords(path, value.getString()) : new HashMap<String, byte[]>();
            if (!keywords.equals(newKeywords)) {
                clearRecordedInfo = true;
            }
        } else if (kind == SVNNodeKind.FILE && SVNProperty.EOL_STYLE.equals(propertyName)) {
            String oldValue = properties.getStringValue(SVNProperty.EOL_STYLE);
            if (oldValue == null || value == null) {
                clearRecordedInfo = (oldValue != null && value == null) || (oldValue == null && value != null);
            } else {
                clearRecordedInfo = !SVNPropertyValue.create(oldValue).equals(value);
            }
        }

        SVNEventAction action;
        if (!properties.containsName(propertyName)) {
            action = value == null ? SVNEventAction.PROPERTY_DELETE_NONEXISTENT : SVNEventAction.PROPERTY_ADD; 
        } else {
            action = value == null ? SVNEventAction.PROPERTY_DELETE : SVNEventAction.PROPERTY_MODIFY; 
        }            
        if (value != null) {
            properties.put(propertyName, value);
        } else {
            properties.remove(propertyName);
        }
        context.getDb().opSetProps(path, properties, null, clearRecordedInfo, workItems);
        if (workItems != null) {
            context.wqRun(path);
        }
        if (receiver != null) {
            receiver.receive(SvnTarget.fromFile(path), new SVNPropertyData(propertyName, value, context.getOptions()));
        }
        if (eventHandler != null) {
            SVNEvent event = SVNEventFactory.createSVNEvent(path, SVNNodeKind.NONE, 
                    null, -1, action, action, null, null, 1, 1, null, propertyName);
            eventHandler.handleEvent(event, -1);
        }
    }

    public static void setAutoProperties(final SVNWCContext context, final File path, final SVNProperties properties,
            final ISvnAddParameters addParameters, Runnable onValidationError) throws SVNException {
        final ISVNFileContentFetcher fetcher = new ISVNFileContentFetcher() {                
            public SVNPropertyValue getProperty(String propertyName) throws SVNException {
                return properties.getSVNPropertyValue(propertyName);
            }                
            public boolean fileIsBinary() throws SVNException {
                SVNPropertyValue mimeType = getProperty(SVNProperty.MIME_TYPE);
                return mimeType != null && SVNProperty.isBinaryMimeType(mimeType.getString());
            }                
            public void fetchFileContent(OutputStream os) throws SVNException {
                InputStream is = null;
                try {
                    is = SVNFileUtil.openFileForReading(path);
                    SVNTranslator.copy(is, os);
                } catch (IOExceptionWrapper ioew) {
                    throw ioew.getOriginalException();
                } catch (IOException e) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                    SVNErrorManager.error(err, SVNLogType.WC);
                } finally {
                    SVNFileUtil.closeFile(is);
                }
            }
        };
        
        final SVNProperties validation = new SVNProperties(properties);
        String detectedMimeType = null;
        for(String propertyName : validation.nameSet()) {
            try {
                final SVNPropertyValue pv = SVNPropertiesManager.validatePropertyValue(path, 
                        SVNNodeKind.FILE, propertyName, properties.getSVNPropertyValue(propertyName), false, context.getOptions(), fetcher);
                properties.put(propertyName, pv);
            } catch (SVNException e) {
                if (SVNProperty.EOL_STYLE.equals(propertyName) &&
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.ILLEGAL_TARGET &&
                        e.getErrorMessage().getMessage().indexOf("newlines") >= 0) {
                    ISvnAddParameters.Action action = addParameters.onInconsistentEOLs(path);
                    if (action == ISvnAddParameters.Action.REPORT_ERROR) {
                        try {
                            onValidationError.run();
                        } catch (Throwable th) {
                            SVNDebugLog.getDefaultLog().log(SVNLogType.WC, th, Level.INFO);
                        }
                            
                        throw e;
                    } else if (action == ISvnAddParameters.Action.ADD_AS_IS) {
                        properties.remove(propertyName);
                    } else if (action == ISvnAddParameters.Action.ADD_AS_BINARY) {
                        properties.remove(propertyName);
                        detectedMimeType = SVNFileUtil.BINARY_MIME_TYPE;
                    }
                } else {
                    try {
                        onValidationError.run();
                    } catch (Throwable th) {
                        SVNDebugLog.getDefaultLog().log(SVNLogType.WC, th, Level.INFO);
                    }
                    throw e;
                }
            }
        }
        if (detectedMimeType != null) {
            properties.put(SVNProperty.MIME_TYPE, detectedMimeType);
        }
        SVNSkel workItems = null;
        if (properties.containsName(SVNProperty.EXECUTABLE) || properties.containsName(SVNProperty.NEEDS_LOCK)) {
            workItems = context.wqBuildSyncFileFlags(path);
        }        

        final boolean clearRecordedInfo = properties.containsName(SVNProperty.KEYWORDS) || properties.containsName(SVNProperty.EOL_STYLE);
        context.getDb().opSetProps(path, properties, null, clearRecordedInfo, workItems);
        if (workItems != null) {
            context.wqRun(path);
        }
    }

    private static boolean equals(SVNPropertyValue oldValue, SVNPropertyValue newValue) {
        if (oldValue == null || newValue == null) {
            return oldValue == newValue;
        }
        return oldValue.equals(newValue);
    }

    public static void checkPropertyName(String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        if (SVNRevisionProperty.isRevisionProperty(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "Revision property ''{0}'' not allowed in this context", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (propertyValue != null && !SVNPropertiesManager.isValidPropertyName(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "Bad property name: ''{0}''", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_PROPERTY_NAME, "''{0}'' is a wcprop, thus not accessible to clients", propertyName);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    public static void categorizeProperties(SVNProperties props, SVNProperties regular, SVNProperties entry, SVNProperties working) {
        for (String name : props.nameSet()) {
            SVNPropertyValue pv = props.getSVNPropertyValue(name);
            if (SVNProperty.isEntryProperty(name) && entry != null) {
                entry.put(name, pv);
            } else if (SVNProperty.isRegularProperty(name) && regular != null) {
                regular.put(name, pv);
            } else if (SVNProperty.isWorkingCopyProperty(name) && working != null) {
                working.put(name, pv);
            }
        }
    }
}