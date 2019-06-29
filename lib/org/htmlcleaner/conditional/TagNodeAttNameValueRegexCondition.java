package org.htmlcleaner.conditional;

import java.util.Map;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

/**
 * Checks if node has specified attribute with specified value.
 */
public class TagNodeAttNameValueRegexCondition implements ITagNodeCondition {
    private Pattern attNameRegex;
    private Pattern attValueRegex;

    public TagNodeAttNameValueRegexCondition(Pattern attNameRegex, Pattern attValueRegex) {
        this.attNameRegex = attNameRegex;
        this.attValueRegex = attValueRegex;
    }

    public boolean satisfy(TagNode tagNode) {
        if (tagNode != null ) {
            for(Map.Entry<String, String>entry: tagNode.getAttributes().entrySet()) {
                if ( (attNameRegex == null || attNameRegex.matcher(entry.getKey()).find()) && (attValueRegex == null || attValueRegex.matcher( entry.getValue() ).find())) {
                    return true;
                }
            }
        }
        return false;
    }
}