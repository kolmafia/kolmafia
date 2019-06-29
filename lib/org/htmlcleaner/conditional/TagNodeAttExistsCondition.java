package org.htmlcleaner.conditional;

import org.htmlcleaner.TagNode;

/**
 * Checks if node contains specified attribute.
 */
public class TagNodeAttExistsCondition implements ITagNodeCondition {
    private String attName;

    public TagNodeAttExistsCondition(String attName) {
        this.attName = attName;
    }

    public boolean satisfy(TagNode tagNode) {
        return tagNode == null ? false : tagNode.getAttributes().containsKey( attName.toLowerCase() );
    }
}