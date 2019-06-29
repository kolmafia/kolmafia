package org.htmlcleaner.conditional;

import org.htmlcleaner.TagNode;

/**
 * Checks if node has specified attribute with specified value.
 */
public class TagNodeAttValueCondition implements ITagNodeCondition {
    private String attName;
    private String attValue;
    private boolean isCaseSensitive;

    public TagNodeAttValueCondition(String attName, String attValue, boolean isCaseSensitive) {
        this.attName = attName;
        this.attValue = attValue;
        this.isCaseSensitive = isCaseSensitive;
    }

    public boolean satisfy(TagNode tagNode) {
        if (tagNode == null || attName == null || attValue == null) {
            return false;
        } else {
            return isCaseSensitive ?
                    attValue.equals( tagNode.getAttributeByName(attName) ) :
                    attValue.equalsIgnoreCase( tagNode.getAttributeByName(attName) );
        }
    }
}