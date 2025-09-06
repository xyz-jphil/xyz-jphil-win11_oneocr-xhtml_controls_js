package xyz.jphil.win11_oneocr.xhtml_controls_js.utilities;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.xml.Element;
import xyz.jphil.win11_oneocr.xhtml_controls_js.BoundingBox;
import xyz.jphil.win11_oneocr.xhtml_controls_js.WordData;

/**
 * Provides utilities for creating SVG elements and text positioning.
 * Deep module that handles SVG namespace creation and text positioning calculations.
 */
public final class SvgUtilities {
    
    private SvgUtilities() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Create SVG text element with calculated positioning and styling.
     * Handles font size calculation based on bounding box dimensions,
     * positioning for optimal text placement, and confidence display.
     */
    public static Element createSVGTextElement(WordData word, BoundingBox bbox, int lineId, int wordIndex, HTMLDocument document) {
        var boxHeight = bbox.height();
        var fontSize = Math.max(8, Math.min(boxHeight * 0.7, 24));
        var textX = bbox.minX() + 2;
        var textY = bbox.minY() + (boxHeight * 0.75);
        
        var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttribute("x", String.valueOf(Math.round(textX * 10) / 10.0));
        text.setAttribute("y", String.valueOf(Math.round(textY * 10) / 10.0));
        text.setAttribute("class", "word-text");
        text.setAttribute("style", "font-size: " + Math.round(fontSize * 10) / 10.0 + "px;");
        text.setAttribute("title", "Confidence: " + Math.round(word.confidence() * 1000) / 10.0 + "%");
        text.setTextContent(word.text());
        
        return text;
    }
    
    /**
     * Create SVG text element using global document.
     * Convenience method that uses Window.current().getDocument() - the global document constant.
     */
    public static Element createSVGTextElement(WordData word, BoundingBox bbox, int lineId, int wordIndex) {
        return createSVGTextElement(word, bbox, lineId, wordIndex, Window.current().getDocument());
    }
}