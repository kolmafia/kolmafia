package org.htmlcleaner.audit;

import java.util.logging.Logger;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.conditional.ITagNodeCondition;

public class HtmlModificationListenerLogger implements HtmlModificationListener {


    private Logger log;

    public HtmlModificationListenerLogger(Logger log) {
        this.log = log;
    }
    public void fireConditionModification(ITagNodeCondition condition, TagNode tagNode) {
        this.log.info("fireConditionModification:"+condition+" at "+tagNode);
    }

    public void fireHtmlError(boolean safety, TagNode tagNode, ErrorType errorType) {
        this.log.info("fireHtmlError:"+errorType+"("+safety+") at "+tagNode);
    }

    public void fireUglyHtml(boolean safety, TagNode tagNode, ErrorType errorType) {
        this.log.info("fireConditionModification:"+errorType+"("+safety+") at "+tagNode);
    }

    public void fireUserDefinedModification(boolean safety, TagNode tagNode, ErrorType errorType) {
        this.log.info("fireConditionModification"+errorType+"("+safety+") at "+tagNode);
    }

}
