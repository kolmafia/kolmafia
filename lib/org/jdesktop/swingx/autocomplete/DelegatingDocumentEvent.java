/**
 * 
 */
package org.jdesktop.swingx.autocomplete;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * @author Karl George Schaefer
 *
 */
final class DelegatingDocumentEvent implements DocumentEvent {
    private final Document resourcedDocument;
    private final DocumentEvent sourceEvent;
    
    public DelegatingDocumentEvent(Document resourcedDocument, DocumentEvent sourceEvent) {
        this.resourcedDocument = resourcedDocument;
        this.sourceEvent = sourceEvent;
    }
    
    /**
     * {@inheritDoc}
     */
    public ElementChange getChange(Element elem) {
        return sourceEvent.getChange(elem);
    }

    /**
     * {@inheritDoc}
     */
    public Document getDocument() {
        return resourcedDocument;
    }

    /**
     * {@inheritDoc}
     */
    public int getLength() {
        return sourceEvent.getLength();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffset() {
        return sourceEvent.getOffset();
    }

    /**
     * {@inheritDoc}
     */
    public EventType getType() {
        return sourceEvent.getType();
    }

}
