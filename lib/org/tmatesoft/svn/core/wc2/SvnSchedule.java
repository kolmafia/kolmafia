package org.tmatesoft.svn.core.wc2;

/**
 * Describe the kind of schedule. This can be:
 * <ul>
 * <li>NORMAL - item is not scheduled
 * <li>ADD - item is scheduled for add
 * <li>DELETE - item is scheduled for delete
 * <li>REPLACE - item is scheduled for replace
 * </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public enum SvnSchedule {
    NORMAL, ADD, DELETE, REPLACE;

    /**
     * Returns <code>String</code> representation of schedule,
     * empty string if item is not scheduled, schedule name otherwise.
     * 
     * @return schedule as <code>String</code>
     */
    public String asString() {
        if (NORMAL == this) {
            return null;
        }
        return name().toLowerCase();
    }

    /**
     * Creates <code>SvnSchedule</code> from <code>String</code> representation.
     * 
     * @param str schedule <code>String</code> representation
     * @return <code>SvnSchedule</code> object
     */
    public static SvnSchedule fromString(String str) {
        if (str == null || "".equals(str)) {
            return NORMAL;
        }
        return SvnSchedule.valueOf(str.toUpperCase());
    }
    
}
