package org.htmlcleaner;

/**
 *
 *
 */
public enum OptionalOutput {
    /**
     * Never outputed even if supplied in the source.
     */
    omit,
    /**
     * outputed ONLY if supplied in the source.
     */
    preserve,
    /**
     * Always outputed, if information is not supplied in the source a default is created.
     */
    alwaysOutput;
}
