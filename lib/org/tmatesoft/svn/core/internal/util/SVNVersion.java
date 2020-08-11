package org.tmatesoft.svn.core.internal.util;

public class SVNVersion implements Comparable<SVNVersion> {

    public static SVNVersion parse(String versionString) {
        if (versionString == null) {
            return null;
        }
        try {
            final String[] fields = versionString.split("\\.");
            if (fields.length == 1) {
                final int major = Integer.parseInt(fields[0]);
                return new SVNVersion(major, -1, -1, null);
            } else if (fields.length == 2) {
                final int major = Integer.parseInt(fields[0]);
                final int minor = Integer.parseInt(fields[1]);
                return new SVNVersion(major, minor, -1, null);
            } else if (fields.length == 3) {
                final int major = Integer.parseInt(fields[0]);
                final int minor = Integer.parseInt(fields[1]);
                final String[] fields2 = fields[2].split("-");
                if (fields2.length == 1) {
                    final int micro = Integer.parseInt(fields2[0]);
                    return new SVNVersion(major, minor, micro, null);
                } else {
                    final int micro = Integer.parseInt(fields2[0]);
                    final String build = fields2[1];
                    return new SVNVersion(major, minor, micro, build);
                }
            }
        } catch (NumberFormatException e) {
            //
        }
        return null;
    }

    private final int major;
    private final int minor;
    private final int micro;
    private final String build;

    public SVNVersion(int major, int minor, int micro, String build) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.build = build;
    }

    public SVNVersion removeBuild() {
        return new SVNVersion(major, minor, micro, null);
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(major);
        if (minor >= 0) {
            stringBuilder.append(".");
            stringBuilder.append(minor);
        }
        if (micro >= 0) {
            stringBuilder.append(".");
            stringBuilder.append(micro);
        }
        if (build != null) {
            stringBuilder.append("-");
            stringBuilder.append(build);
        }
        return stringBuilder.toString();
    }

    @Override
    public int compareTo(SVNVersion o) {
        if (o == null) {
            return 1;
        }
        if (o == this) {
            return 0;
        }
        int diff = major - o.major;
        if (diff != 0) {
            return diff;
        }
        //here we use the fact that 12.0 is actually 12.0.(-1) and it's < 12.0.1
        diff = minor - o.minor;
        if (diff != 0) {
            return diff;
        }
        diff = micro - o.micro;
        if (diff != 0) {
            return diff;
        }
        //compare builds alphabetically, is there's something better than that?
        if (build == null) {
            return o.build == null ? 0 : -1;
        }
        if (o.build == null) {
            return 1;
        }
        return build.compareTo(o.build);
    }
}
