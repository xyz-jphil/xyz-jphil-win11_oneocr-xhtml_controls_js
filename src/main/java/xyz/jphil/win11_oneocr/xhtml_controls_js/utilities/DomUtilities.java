package xyz.jphil.win11_oneocr.xhtml_controls_js.utilities;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Provides utilities for DOM element manipulation and visibility control.
 * Deep module that hides DOM complexity behind simple static methods.
 */
public final class DomUtilities {
    
    private DomUtilities() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Safely removes an element from the DOM if it exists and has a parent.
     * Handles null elements gracefully.
     */
    public static void removeElement(Element element) {
        if (element != null && element.getParentNode() != null) {
            element.getParentNode().removeChild(element);
        }
    }
    
    /**
     * Updates element visibility by adding/removing CSS class names.
     * Uses classList API for proper CSS class management.
     */
    public static void updateElementVisibility(HTMLElement element, boolean condition, String className) {
        if (condition) {
            element.getClassList().add(className);
        } else {
            element.getClassList().remove(className);
        }
    }
    
    /**
     * Query DOM with null safety, returning Optional for safe handling.
     * Eliminates null pointer exceptions in DOM queries.
     */
    public static Optional<Element> queryOptional(String selector, HTMLDocument document) {
        return Optional.ofNullable(document.querySelector(selector));
    }
    
    /**
     * Query all matching DOM elements and return as List for modern Java streams.
     * Converts NodeList to List for better API usability.
     */
    public static List<HTMLElement> queryAll(String selector, HTMLDocument document) {
        var nodeList = document.querySelectorAll(selector);
        return IntStream.range(0, nodeList.getLength())
            .mapToObj(i -> (HTMLElement) nodeList.item(i))
            .toList();
    }
    
    /**
     * Remove first matching element from document by CSS selector.
     * Combines query and removal in single operation.
     */
    public static void removeFromDocument(String query, HTMLDocument document) {
        var element = document.querySelector(query);
        if (element != null) {
            removeElement(element);
        }
    }
    
    /**
     * Remove all matching elements from document by CSS selector.
     * Efficient bulk removal operation.
     */
    public static void removeAllFromDocument(String query, HTMLDocument document) {
        var elements = document.querySelectorAll(query);
        for (int i = 0; i < elements.getLength(); i++) {
            var element = elements.item(i);
            removeElement(element);
        }
    }
    
    // Convenience methods that use global document - eliminates need for wrapper methods
    
    /**
     * Query global DOM with null safety, returning Optional for safe handling.
     * Uses Window.current().getDocument() - the global document constant.
     */
    public static Optional<Element> queryOptional(String selector) {
        return queryOptional(selector, Window.current().getDocument());
    }
    
    /**
     * Query all matching elements from global document and return as List.
     * Uses Window.current().getDocument() - the global document constant.
     */
    public static List<HTMLElement> queryAll(String selector) {
        return queryAll(selector, Window.current().getDocument());
    }
    
    /**
     * Remove first matching element from global document by CSS selector.
     * Uses Window.current().getDocument() - the global document constant.
     */
    public static void removeFromDocument(String query) {
        removeFromDocument(query, Window.current().getDocument());
    }
    
    /**
     * Remove all matching elements from global document by CSS selector.
     * Uses Window.current().getDocument() - the global document constant.
     */
    public static void removeAllFromDocument(String query) {
        removeAllFromDocument(query, Window.current().getDocument());
    }
}