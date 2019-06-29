package org.htmlcleaner;

import java.util.concurrent.ConcurrentMap;

/**It contains the MathML tags to use with Html5 tags
 * 
 * @author User
 *
 */
public class MathMLTagProvider {
    
    private static final String CLOSE_BEFORE_TAGS = "menclose,mpadded,mphantom,mfenced,mstyle,merror,msqrt,mroot,maligngroup,malignmark,mlabeledtr,ms,mi,mo,mn,mfrac,mtext,mspace,mglyph,p,details,summary,menuitem,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml";
    
    public MathMLTagProvider(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap) {
    	presentationMarkup(tagInfo,tagInfoMap);
    }
    
   public void presentationMarkup(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap){
	   tokenElements(tagInfo,tagInfoMap);
	   layoutElements(tagInfo,tagInfoMap);
	   scriptElements(tagInfo,tagInfoMap);
	   tableElements(tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("maction", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("maction", tagInfo,tagInfoMap);
       
   }
   
   
   public void tokenElements(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap){
	   tagInfo = new TagInfo("mi", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mi", tagInfo,tagInfoMap);
	
	   tagInfo = new TagInfo("mn", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mn", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mo", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mo", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mtext", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mtext", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mspace", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mspace", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("ms", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("ms", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mglyph", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.optional, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mglyph", tagInfo,tagInfoMap);
   }
   
   
   public void layoutElements(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap){
	   
	   tagInfo = new TagInfo("mrow", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mrow", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mfrac", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mfrac", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("msqrt", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("msqrt", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mroot", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mroot", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mstyle", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mstyle", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("merror", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("merror", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mpadded", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mpadded", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mphantom", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mphantom", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mfenced", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mfenced", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("menclose", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("menclose", tagInfo,tagInfoMap);
	   
   }
    
    
   public void scriptElements(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap){
	   tagInfo = new TagInfo("msub", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("msub", tagInfo,tagInfoMap); 
	   
	   tagInfo = new TagInfo("msup", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.inline);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("msup", tagInfo,tagInfoMap); 
	   
	   tagInfo = new TagInfo("msubsup", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("msubsup", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("munder", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("munder", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mover", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mover", tagInfo,tagInfoMap); 
	   
	   tagInfo = new TagInfo("munderover", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("munderover", tagInfo,tagInfoMap); 
	   
	   tagInfo = new TagInfo("mmultiscripts", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("mmultiscripts", tagInfo,tagInfoMap); 
	   
   }
   
   public void tableElements(TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap){
	   tagInfo = new TagInfo("mtable", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   tagInfo.defineAllowedChildrenTags("mtr,mtd,mo,mn,mlabeledtr");
	   this.put("mtable", tagInfo,tagInfoMap); 
	   
	   tagInfo = new TagInfo("mlabeledtr", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   tagInfo.defineRequiredEnclosingTags("mtable");
	   tagInfo.defineFatalTags("mtable");
	   this.put("mlabeledtr", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mtr", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   tagInfo.defineAllowedChildrenTags("mtd,mlabeledtr");
	   //tagInfo.defineRequiredEnclosingTags("mtable");
	   this.put("mtr", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("mtd", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   //tagInfo.defineRequiredEnclosingTags("mtr");
	   //tagInfo.defineFatalTags("mtable");
	   this.put("mtd", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("maligngroup", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("maligngroup", tagInfo,tagInfoMap);
	   
	   tagInfo = new TagInfo("malignmark", ContentType.all, BelongsTo.BODY, false, false, false, CloseTag.required, Display.block);
	   tagInfo.defineCloseBeforeTags(CLOSE_BEFORE_TAGS);
	   this.put("malignmark", tagInfo,tagInfoMap);
	   
   }
   
   
    protected void put(String tagName, TagInfo tagInfo,ConcurrentMap<String, TagInfo> tagInfoMap) {
        tagInfoMap.put(tagName, tagInfo);
    }

    public TagInfo getTagInfo(String tagName,ConcurrentMap<String, TagInfo> tagInfoMap) {
        if ( tagName == null) {
            return null;
        } else {
            return tagInfoMap.get(tagName);
        }
    }
    
}
