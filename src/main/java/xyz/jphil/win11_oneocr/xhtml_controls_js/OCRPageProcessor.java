package xyz.jphil.win11_oneocr.xhtml_controls_js;

import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.browser.Window;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.AttributeParser.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.DomUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.UIElementFactory.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.NotificationUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.TextUtilities.*;

/**
 * Processes individual OCR pages.
 * Converts page DOM to OCR data, applies styling, and adds interactivity.
 * Deep module that encapsulates all single-page processing complexity.
 */
public class OCRPageProcessor {
    
    private final Config config;
    
    public OCRPageProcessor(Config config) {
        this.config = config;
    }
    
    /**
     * Process a single OCR page section.
     * Extracts OCR data, applies confidence styling, adds hover controls.
     */
    public OCRData processPage(HTMLElement pageSection, int pageNumber, boolean isMultiPage) {
        debug("Processing page " + pageNumber + " (multi-page: " + isMultiPage + ")");
        
        // Extract OCR data from page DOM
        var ocrData = buildOCRDataFromPage(pageSection);
        
        // Apply confidence-based styling to words
        applyConfidenceClassesToPage(pageSection, ocrData);
        
        // Add interactive hover controls
        if (!isMultiPage) {
            // Single-page gets full interactivity
            addHoverControlsToPage(pageSection, ocrData);
            addPageCopyButtonToPage(pageSection, ocrData);
        } else {
            // Multi-page gets simplified controls to avoid clutter
            addSimplifiedControlsToPage(pageSection, ocrData, pageNumber);
        }
        
        debug("Page " + pageNumber + " processed: " + ocrData.metadata().totalWords() + " words, " + 
              ocrData.metadata().totalLines() + " lines");
        
        return ocrData;
    }
    
    /**
     * Extract OCR data from page section DOM.
     * Converts XHTML structure to structured OCR data model.
     */
    private OCRData buildOCRDataFromPage(HTMLElement pageSection) {
        // Extract metadata from section attributes
        var metadata = new Metadata(
            Optional.ofNullable(pageSection.getAttribute("srcName")).orElse("Unknown"),
            parseIntAttribute(pageSection, "imgWidth", 800),
            parseIntAttribute(pageSection, "imgHeight", 600),
            parseDoubleAttribute(pageSection, "angle", 0.0),
            parseDoubleAttribute(pageSection, "averageConfidence", 0.0),
            parseIntAttribute(pageSection, "ocrWordsCount", 0),
            parseIntAttribute(pageSection, "ocrSegmentsCount", 0)
        );
        
        // Extract lines from segments
        var segments = pageSection.querySelectorAll("segment");
        var lines = IntStream.range(0, segments.getLength())
            .mapToObj(i -> buildLineDataFromSegment(segments.get(i), i))
            .toList();
        
        return new OCRData(
            metadata,
            lines,
            Optional.of(metadata.filename())
        );
    }
    
    /**
     * Build line data from segment element.
     */
    private LineData buildLineDataFromSegment(Element segment, int lineIndex) {
        var words = segment.querySelectorAll("w");
        var wordList = IntStream.range(0, words.getLength())
            .mapToObj(i -> {
                var word = words.get(i);
                return new WordData(
                    word.getTextContent().trim(),
                    parseDoubleAttribute(word, "p", 0.0),
                    parseIntAttribute(word, "i", i),
                    parseBoundingBox(word.getAttribute("b"))
                );
            })
            .toList();
        
        return new LineData(lineIndex, Optional.empty(), wordList);
    }
    
    /**
     * Apply confidence-based CSS classes to words in page.
     */
    private void applyConfidenceClassesToPage(HTMLElement pageSection, OCRData ocrData) {
        for (int lineIndex = 0; lineIndex < ocrData.lines().size(); lineIndex++) {
            var line = ocrData.lines().get(lineIndex);
            
            // Add line number for CSS targeting
            var segmentElement = pageSection.querySelector("segment:nth-child(" + (lineIndex + 1) + ")");
            if (segmentElement != null) {
                segmentElement.setAttribute("data-line-number", String.valueOf(lineIndex + 1));
            }
            
            // Apply confidence classes to words
            for (int wordIndex = 0; wordIndex < line.words().size(); wordIndex++) {
                var word = line.words().get(wordIndex);
                var wordElement = pageSection.querySelector(
                    "segment:nth-child(" + (lineIndex + 1) + ") w:nth-child(" + (wordIndex + 1) + ")");
                
                if (wordElement != null) {
                    var wEle = (HTMLElement) wordElement;
                    wEle.setClassName("");
                    var level = ConfidenceLevel.fromConfidence(word.confidence(), config);
                    wEle.getClassList().add(level.htmlClass());
                }
            }
        }
    }
    
    /**
     * Add hover controls to page (single-page version).
     */
    private void addHoverControlsToPage(HTMLElement pageSection, OCRData ocrData) {
        var segments = pageSection.querySelectorAll("segment");
        IntStream.range(0, segments.getLength())
            .forEach(i -> {
                var segment = (HTMLElement) segments.get(i);
                addHoverToSegment(segment, i, ocrData);
            });
    }
    
    /**
     * Add simplified controls for multi-page (less cluttered).
     */
    private void addSimplifiedControlsToPage(HTMLElement pageSection, OCRData ocrData, int pageNumber) {
        // Add page identifier
        pageSection.setAttribute("data-processed-page", String.valueOf(pageNumber));
        
        // Add simple copy button in corner
        var copyBtn = createSimplePageCopyButton(pageNumber, ocrData);
        pageSection.appendChild(copyBtn);
    }
    
    /**
     * Add hover controls to individual segment.
     */
    private void addHoverToSegment(HTMLElement segment, int lineIndex, OCRData ocrData) {
        segment.addEventListener("mouseenter", evt -> {
            var controls = createFloatingControls();
            var copyButton = createLineCopyButton(lineIndex, ocrData);
            controls.appendChild(copyButton);
            
            positionControlsWithinElement(controls, segment, "line");
            Window.current().getDocument().getBody().appendChild(controls);
        });
        
        segment.addEventListener("mouseleave", evt -> {
            hideFloatingControls();
        });
    }
    
    /**
     * Add page copy button (single-page version).
     */
    private void addPageCopyButtonToPage(HTMLElement pageSection, OCRData ocrData) {
        var copyBtn = createPageCopyButton(ocrData);
        pageSection.appendChild(copyBtn);
    }
    
    /**
     * Create line copy button for hover controls.
     */
    private HTMLElement createLineCopyButton(int lineIndex, OCRData ocrData) {
        var button = (HTMLElement) Window.current().getDocument().createElement("button");
        button.setTextContent("📋 Line " + (lineIndex + 1));
        button.addEventListener("click", evt -> {
            if (lineIndex < ocrData.lines().size()) {
                var line = ocrData.lines().get(lineIndex);
                var text = line.words().stream()
                    .map(WordData::text)
                    .collect(java.util.stream.Collectors.joining(" "));
                copyTextWithNotification(text);
            }
        });
        return button;
    }
    
    /**
     * Create page copy button.
     */
    private HTMLElement createPageCopyButton(OCRData ocrData) {
        var button = (HTMLElement) Window.current().getDocument().createElement("button");
        button.setTextContent("📄 Copy Page");
        button.getStyle().setCssText("""
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(0, 0, 0, 0.8);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 5px 10px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 11px;
            z-index: 40;
            """);
        
        button.addEventListener("click", evt -> {
            copyPageTextWithNotification(ocrData.lines());
        });
        
        return button;
    }
    
    /**
     * Create simple copy button for multi-page.
     */
    private HTMLElement createSimplePageCopyButton(int pageNumber, OCRData ocrData) {
        var button = (HTMLElement) Window.current().getDocument().createElement("button");
        button.setTextContent("📄 " + pageNumber);
        button.setTitle("Copy page " + pageNumber + " text");
        button.getStyle().setCssText("""
            position: absolute;
            top: 5px;
            right: 5px;
            background: rgba(0, 0, 0, 0.7);
            color: white;
            border: none;
            padding: 3px 6px;
            border-radius: 3px;
            cursor: pointer;
            font-size: 10px;
            z-index: 30;
            """);
        
        button.addEventListener("click", evt -> {
            copyPageTextWithNotification(ocrData.lines());
        });
        
        return button;
    }
    
    /**
     * Hide floating controls.
     */
    private void hideFloatingControls() {
        var existing = Window.current().getDocument().getElementById("floating-controls");
        if (existing != null) {
            removeElement(existing);
        }
    }
    
    // Debug helper
    private static void debug(String message) {
        System.out.println("[OCRPageProcessor] " + message);
    }
}