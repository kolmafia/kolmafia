package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNException;

/**
 * Represents a checksum for SVN files.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnChecksum {
    
	/**
	 * Describes the kind of used hash algorithm for computing the checksum. This can be:
	 * <ul>
	 * <li>sha1 - sha1 algorithm
	 * <li>md5  - md5 algorithm
	 * </ul>
	 * 
	 * @author TMate Software Ltd.
	 * @version 1.7
	 */
    public enum Kind { sha1, md5 }
    
    private Kind kind;
    private String digest;
    
    /**
     * Constructs an <b>SvnChecksum</b> object with the
     * specified parameters. 
     * 
     * @param kind kind of checksum
     * @param digest computed checksum value
     */
    public SvnChecksum(Kind kind, String digest) {
        setKind(kind);
        setDigest(digest);
    }

    /**
     * Gets king of checksum.
     * 
     * @return checksum kind
     */
    public Kind getKind() {
        return kind;
    }
    
    /**
     * Gets computed checksum value.
     * 
     * @return computed checksum value
     */
    public String getDigest() {
        return digest;
    }
    
    /**
     * Sets kind of checksum.
     * 
     * @param kind checksum kind
     */
    public void setKind(Kind kind) {
        this.kind = kind;
    }
    
    /**
     * Sets computed checksum value.
     * 
     * @param digest computed checksum value
     */
    public void setDigest(String digest) {
        this.digest = digest;
    }
    
    /**
     * Returns <code>String</code> representation of checksum.
     * 
     * @return <code>String</code> representation of checksum
     */
    public String toString() {
        return '$' + (getKind() == Kind.md5 ? "md5 $" : "sha1$") + getDigest();
    }
    
    @Override
    public int hashCode() {
        int hashCode = 17;
        if (digest != null) {
            hashCode += 37*digest.hashCode();
        }
        if (kind != null) {
            hashCode += 37*kind.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != SvnChecksum.class) {
            return false;
        }        SvnChecksum other = (SvnChecksum) obj;
        
        boolean equals = true;
        if (other.digest == null) {
            equals = this.digest == null;
        } else {
            equals = other.digest.equals(this.digest);
        }
        if (equals) {
            equals = other.kind == this.kind;
        }
        return equals;
    }

    /**
     * Creates checksum object from <code>String</code>.
     * 
     * @param checksum checksum represented in <code>String</code>
     */
    public static SvnChecksum fromString(String checksum) throws SVNException {
        if (checksum == null || checksum.length() < 7) {
            return null;
        }
        Kind kind = checksum.charAt(1) == 'm' ? Kind.md5: Kind.sha1;
        String digest = checksum.substring(6);
        return new SvnChecksum(kind, digest);
    }


}
