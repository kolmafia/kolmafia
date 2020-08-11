package org.tmatesoft.svn.core.internal.util;

import java.util.EnumSet;

public enum SVNCertificateFailureKind {
    UNKNOWN_CA(8),
    CN_MISMATCH(4),
    EXPIRED(2),
    NOT_YET_VALID(1),
    OTHER(0xfffffff0);

    private final int code;

    SVNCertificateFailureKind(int code) {
        this.code = code;
    }

    public static int createMask(EnumSet<SVNCertificateFailureKind> failureKinds) {
        int mask = 0;
        if (failureKinds != null) {
            for (SVNCertificateFailureKind failureKind : failureKinds) {
                mask |= failureKind.code;
            }
        }
        return mask;
    }

    public static SVNCertificateFailureKind fromString(String s) {
        for (SVNCertificateFailureKind failureKind : values()) {
            if (failureKind.asString().equals(s)) {
                return failureKind;
            }
        }
        return null;
    }

    public String asString() {
        return name().toLowerCase().replace('_', '-');
    }

    public int getCode() {
        return code;
    }
}
