package xyz.jphil.win11_oneocr.xhtml_controls_js;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.browser.Window;

import java.util.List;
import java.util.Optional;

import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.DomUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.AttributeParser.*;

/**
 * Manages multiple OCR pages in a document.
 * Discovers section.win11OneOcrPage elements and determines if document is multi-page.
 * Deep module that handles multi-page detection and coordination complexity.
 */
public class MultiPageManager {
    
    private final HTMLDocument document;
    
    public MultiPageManager() {
        this.document = Window.current().getDocument();
    }
    
    /**
     * Detect if document contains multiple pages.
     * Checks for meta[name="pagesCount"] or counts section.win11OneOcrPage elements.
     */
    public boolean isMultiPage() {
        // First check meta tag
        var pageCountMeta = document.querySelector("meta[name=\"pagesCount\"]");
        if (pageCountMeta != null) {
            var pageCount = parseIntAttribute(pageCountMeta, "content", 1);
            debug("Found pagesCount meta tag: " + pageCount + " pages");
            return pageCount > 1;
        }
        
        // Fallback: count actual sections
        var pageCount = getAllPages().size();
        debug("No pagesCount meta, counted " + pageCount + " section elements");
        return pageCount > 1;
    }
    
    /**
     * Get total page count in document.
     */
    public int getPageCount() {
        var pageCountMeta = document.querySelector("meta[name=\"pagesCount\"]");
        if (pageCountMeta != null) {
            return parseIntAttribute(pageCountMeta, "content", 1);
        }
        return getAllPages().size();
    }
    
    /**
     * Get all OCR page sections in document.
     * Returns list ordered by DOM appearance (should match pageNum attributes).
     */
    public List<HTMLElement> getAllPages() {
        return queryAll("section.win11OneOcrPage");
    }
    
    /**
     * Get specific page by number (1-indexed).
     * First tries pageNum attribute, falls back to DOM position.
     */
    public Optional<HTMLElement> getPage(int pageNumber) {
        // Try finding by pageNum attribute first
        var pageByAttr = document.querySelector("section.win11OneOcrPage[pageNum=\"" + pageNumber + "\"]");
        if (pageByAttr != null) {
            return Optional.of((HTMLElement) pageByAttr);
        }
        
        // Fallback: use DOM position (1-indexed)
        var allPages = getAllPages();
        if (pageNumber >= 1 && pageNumber <= allPages.size()) {
            return Optional.of(allPages.get(pageNumber - 1));
        }
        
        return Optional.empty();
    }
    
    /**
     * Get document-level metadata from meta tags.
     * Returns document-wide statistics for multi-page documents.
     */
    public DocumentMetadata getDocumentMetadata() {
        var totalWords = parseMetaContent("totalWords", 0);
        var totalSegments = parseMetaContent("totalSegments", 0);  
        var averageConfidence = parseMetaContent("averageConfidence", 0.0);
        var pageCount = getPageCount();
        
        return new DocumentMetadata(pageCount, totalWords, totalSegments, averageConfidence);
    }
    
    /**
     * Process all pages using provided processor function.
     * Handles both single-page and multi-page documents uniformly.
     */
    public void processAllPages(PageProcessor processor) {
        var pages = getAllPages();
        var isMulti = isMultiPage();
        
        debug("Processing " + pages.size() + " pages (multi-page: " + isMulti + ")");
        
        for (int i = 0; i < pages.size(); i++) {
            var page = pages.get(i);
            var pageNumber = getPageNumber(page).orElse(i + 1);
            
            try {
                debug("Processing page " + pageNumber + " (" + getPageSourceName(page) + ")");
                processor.processPage(page, pageNumber, isMulti);
                debug("Page " + pageNumber + " processed successfully");
            } catch (Exception e) {
                debug("ERROR processing page " + pageNumber + ": " + e.getMessage());
                // Continue with other pages - don't let one failure stop all
            }
        }
        
        debug("All pages processed");
    }
    
    /**
     * Get page number from pageNum attribute.
     */
    private Optional<Integer> getPageNumber(HTMLElement page) {
        var pageNumAttr = page.getAttribute("pageNum");
        if (pageNumAttr != null) {
            try {
                return Optional.of(Integer.parseInt(pageNumAttr));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get page source name from srcName attribute.
     */
    private String getPageSourceName(HTMLElement page) {
        return Optional.ofNullable(page.getAttribute("srcName")).orElse("unknown");
    }
    
    /**
     * Parse meta tag content as integer.
     */
    private int parseMetaContent(String metaName, int defaultValue) {
        var meta = document.querySelector("meta[name=\"" + metaName + "\"]");
        return meta != null ? parseIntAttribute(meta, "content", defaultValue) : defaultValue;
    }
    
    /**
     * Parse meta tag content as double.
     */
    private double parseMetaContent(String metaName, double defaultValue) {
        var meta = document.querySelector("meta[name=\"" + metaName + "\"]");
        return meta != null ? parseDoubleAttribute(meta, "content", defaultValue) : defaultValue;
    }
    
    /**
     * Document-level metadata record.
     */
    public record DocumentMetadata(
        int pageCount,
        int totalWords, 
        int totalSegments,
        double averageConfidence
    ) {}
    
    /**
     * Interface for page processing operations.
     */
    @FunctionalInterface
    public interface PageProcessor {
        void processPage(HTMLElement pageElement, int pageNumber, boolean isMultiPage) throws Exception;
    }
    
    // Debug helper
    private static void debug(String message) {
        System.out.println("[MultiPageManager] " + message);
    }
}