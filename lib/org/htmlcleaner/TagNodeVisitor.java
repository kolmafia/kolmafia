package org.htmlcleaner;

/**
 * Defines action to be performed on TagNodes
 */
public interface TagNodeVisitor {

    /**
     * Action to be performed on single node in the tree
     * @param parentNode Parent of tagNode
     * @param htmlNode node visited
     * @return True if tree traversal should be continued, false if it has to stop.
     */
    public boolean visit(TagNode parentNode, HtmlNode htmlNode);

}