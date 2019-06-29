package org.htmlcleaner.conditional;

import org.htmlcleaner.TagNode;

/**
 * Used as base for different node checkers.
 */
public interface ITagNodeCondition {
    public boolean satisfy(TagNode tagNode);
}