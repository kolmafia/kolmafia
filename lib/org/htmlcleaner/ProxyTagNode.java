package org.htmlcleaner;

/**
 * A {@link TagNode} that only really holds whitespace or comments - allows
 * using {@link ContentNode} in places where a {@link TagNode} is expected.
 * <p/>
 * This class is currently just a short-lived intermediate artifact generated 
 * from {@link HtmlCleaner} while cleaning an html file and descarded 
 * before the results are returned.
 * 
 * @author andyhot
 */
class ProxyTagNode extends TagNode {
	private ContentNode token;
	private CommentNode comment;
	private TagNode bodyNode;
	
	public ProxyTagNode(ContentNode token, TagNode bodyNode) {
		super("");
		this.token = token;
		this.bodyNode = bodyNode;
	}
	
	public ProxyTagNode(CommentNode comment, TagNode bodyNode) {
		super("");
		this.comment = comment;
		this.bodyNode = bodyNode;
	}	

	@Override
	public TagNode getParent() {
		return null;
	}
	
	@Override
	public boolean removeFromTree() {
		bodyNode.removeChild(getToken());
		return true;
	}	
	
	public BaseToken getToken() {
		return token!=null ? token : comment;
	}	
	
	public String getContent() {
		return token!=null ? token.getContent() : comment.getContent();
	}

}
