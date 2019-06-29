/*  
    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of HtmlCleaner may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "HtmlCleaner" in the
    subject line.
*/
package org.htmlcleaner.audit;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.conditional.ITagNodeCondition;

/**
 * Implementors can be registered on {@link org.htmlcleaner.CleanerProperties} to receive notifications about
 * modifications made by html cleaner.
 * 
 * @author Konstantin Burov (aectann@gmail.com)
 *
 */
public interface HtmlModificationListener {

    /**
     * Fired when cleaner fixes some error in html syntax.
     * 
     * @param certain - true if change made doesn't hurts end document.
     * @param tagNode - problematic node.
     * @param errorType
     */
    void fireHtmlError(boolean certain, TagNode tagNode, ErrorType errorType);

    /**
     * Fired when cleaner fixes ugly html -- when syntax was correct but task was implemented by weird code.
     * For example when deprecated tags are removed.
     * 
     * @param certainty - true if change made doesn't hurts end document.
     * @param tagNode - problematic node.
     * @param errorType
     */
    void fireUglyHtml(boolean certainty, TagNode tagNode, ErrorType errorType);

    /**
     * Fired when cleaner modifies html due to {@link ITagNodeCondition} match.
     * 
     * @param condition that was applied to make the modification
     * @param tagNode - problematic node.
     */
    void fireConditionModification(ITagNodeCondition condition, TagNode tagNode);

    /**
     * Fired when cleaner modifies html due to user specified rules.
     * 
     * @param certainty - true if change made doesn't hurts end document.
     * @param tagNode - problematic node.
     * @param errorType
     */
    void fireUserDefinedModification(boolean certainty, TagNode tagNode, ErrorType errorType);

}
