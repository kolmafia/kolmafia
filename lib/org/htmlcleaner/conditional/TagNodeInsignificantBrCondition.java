package org.htmlcleaner.conditional;

import java.util.List;

import org.htmlcleaner.TagNode;

/**
 * Checks if node is an insignificant br tag -- is placed at the end or at the
 * start of a block.
 * 
 * @author Konstantin Burov (aectann@gmail.com)
 */
public class TagNodeInsignificantBrCondition implements ITagNodeCondition {

	private static final String BR_TAG = "br";
	
	public TagNodeInsignificantBrCondition() {
	}

	public boolean satisfy(TagNode tagNode) {
		if (!isBrNode(tagNode)) {
			return false;
		}
		TagNode parent = tagNode.getParent();
		List children = parent.getAllChildren();
		int brIndex = children.indexOf(tagNode);		
		return checkSublist(0, brIndex, children) || checkSublist (brIndex, children.size(), children);
	}

	private boolean isBrNode(TagNode tagNode) {
		return tagNode != null && BR_TAG.equals(tagNode.getName());
	}

	private boolean checkSublist(int start, int end, List list) {
		List sublist = list.subList(start, end);
		for (Object object : sublist) {
			if(!(object instanceof TagNode)){
				return false;
			}
			TagNode node = (TagNode) object;
			if(!isBrNode(node)&&!node.isPruned()){
				return false;
			}
		}
		return true;
	}
}
