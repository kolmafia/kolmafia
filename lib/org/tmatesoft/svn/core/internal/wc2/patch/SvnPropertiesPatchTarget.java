package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallback;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class SvnPropertiesPatchTarget extends SvnTargetContent {

    public static SvnPropertiesPatchTarget initPropTarget(String propName, SvnDiffCallback.OperationKind operation, SVNWCContext context, File absPath) throws SVNException {
        SvnPropertiesPatchTarget propPatchTarget = new SvnPropertiesPatchTarget();
        propPatchTarget.setCurrentLine(1);
        propPatchTarget.setEolStyle(SVNWCContext.SVNEolStyle.None);
        propPatchTarget.setName(propName);
        propPatchTarget.setOperation(operation);

        SVNPropertyValue value;
        try {
            SVNProperties actualProps = context.getActualProps(absPath);
            value = actualProps.getSVNPropertyValue(propName);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                value = null;
            } else {
                throw e;
            }
        }
        propPatchTarget.setExisted(value != null);
        propPatchTarget.setValue(value);
        propPatchTarget.setPatchedValue(SVNPropertyValue.create(""));

        PropReadCallbacks propCallbacks = new PropReadCallbacks(propPatchTarget, value, 0);
        propPatchTarget.setReadBaton(propCallbacks);
        propPatchTarget.setReadLineCallback(propCallbacks);
        propPatchTarget.setTellCallback(propCallbacks);
        propPatchTarget.setSeekCallback(propCallbacks);
        propPatchTarget.setWriteCallback(propCallbacks);
        propPatchTarget.setWriteBaton(propPatchTarget.getPatchedValue());

        return propPatchTarget;
    }

    private String name;
    private SVNPropertyValue value;
    private SVNPropertyValue patchedValue;
    private SvnDiffCallback.OperationKind operation;

    public String getName() {
        return name;
    }

    public SVNPropertyValue getValue() {
        return value;
    }

    public SVNPropertyValue getPatchedValue() {
        return patchedValue;
    }

    public SvnDiffCallback.OperationKind getOperation() {
        return operation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(SVNPropertyValue value) {
        this.value = value;
    }

    public void setPatchedValue(SVNPropertyValue patchedValue) {
        this.patchedValue = patchedValue;
    }

    public void setOperation(SvnDiffCallback.OperationKind operation) {
        this.operation = operation;
    }

    private static class PropReadCallbacks implements ITellCallback, IRealLineCallback, ISeekCallback, IWriteCallback {

        private final SvnPropertiesPatchTarget propPatchTarget;
        private SVNPropertyValue value;
        private long offset;

        private PropReadCallbacks(SvnPropertiesPatchTarget propPatchTarget, SVNPropertyValue value, long offset) {
            this.propPatchTarget = propPatchTarget;
            this.value = value;
            this.offset = offset;
        }

        public String readLine(Object baton, String[] eolStr, boolean[] eof) throws SVNException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] valueBytes = SVNPropertyValue.getPropertyAsBytes(value);
                if (offset >= valueBytes.length) {
                    eolStr[0] = null;
                    eof[0] = true;
                    return null;
                }

                eolStr[0] = null;
                boolean foundEof = false;
                int pos;
                do {
                    pos = (int) offset;
                    offset++;

                    if (pos == valueBytes.length || valueBytes[pos] == '\0') {
                        foundEof = true;
                        break;
                    } else if (valueBytes[pos] == '\n') {
                        eolStr[0] = "\n";
                    } else if (valueBytes[pos] == '\r') {
                        eolStr[0] = "\r";
                        if (pos < valueBytes.length && valueBytes[pos + 1] == '\n') {
                            eolStr[0] = "\r\n";
                            offset++;
                        }
                    } else {
                        byteArrayOutputStream.write(valueBytes[pos]);
                    }

                    if (eolStr[0] != null) {
                        break;
                    }
                } while (pos < valueBytes.length);

                if (eof != null) {
                    eof[0] = foundEof;
                }
                return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            } finally {
                SVNFileUtil.closeFile(byteArrayOutputStream);
            }
            return null;
        }

        public void seek(Object object, long offset) {
            PropReadCallbacks propReadCallbacks = (PropReadCallbacks) object;
            propReadCallbacks.offset = offset;
        }

        public long tell(Object readBaton) {
            PropReadCallbacks propReadCallbacks = (PropReadCallbacks) readBaton;
            return propReadCallbacks.offset;
        }

        public void write(Object writeBaton, String s) {
            propPatchTarget.patchedValue = SVNPropertyValue.create(SVNPropertyValue.getPropertyAsString(propPatchTarget.patchedValue) + s);
        }
    }
}
