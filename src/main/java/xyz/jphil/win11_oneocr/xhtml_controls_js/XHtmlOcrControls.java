package xyz.jphil.win11_oneocr.xhtml_controls_js;

import org.teavm.jso.JSBody;
//import org.teavm.jso.dom.html.*;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.browser.Window;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

// Static imports for utility methods - organized in utilities sub-package
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.AttributeParser.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.DomUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.OCRDataFactory.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.SvgUtilities.*;
import xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.UIElementFactory;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.UIElementFactory.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.NotificationUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.utilities.TextUtilities.*;


/**
 * OCR XHTML Interactive Viewer - TeaVM Implementation
 * Converted from JavaScript using modern Java features (JDK 21 compatible with TeaVM)
 */
public class XHtmlOcrControls {
    
    // Debug flag - set to false to disable verbose logging
    private static final boolean VERBOSE_DEBUG = true;
    
    // Static initializer - fires immediately when class loads
    static {
        console("*** TeaVM OCRViewer class loaded! JavaScript is executing! ***");
    }
    
    
    // Instance variables - updated for multi-page support
    private final Config config = Config.DEFAULT;
    private final MultiPageManager pageManager = new MultiPageManager();
    private final OCRPageProcessor pageProcessor = new OCRPageProcessor(config);
    private ViewerState state = ViewerState.DEFAULT;
    private final HTMLDocument document = Window.current().getDocument();
    private Timer hideControlsTimer;
    
    // Multi-page state
    private List<OCRData> allPagesData = new ArrayList<>();
    private boolean isMultiPageDocument = false;
    
    public static void main(String[] args) {
        debug("main() called - starting TeaVM OCR Viewer");
        XHtmlOcrControls viewer = new XHtmlOcrControls();
        
        
        if (true/*isDocumentReady()*/) { 
            // document is already loaded because we are using a loading script
            debug("Document is ready, initializing immediately");
            viewer.initializeOCRViewer();
        } else {
            debug("Document not ready, waiting for DOMContentLoaded");
            Window.current().getDocument().addEventListener("DOMContentLoaded", evt -> {
                debug("DOMContentLoaded fired, now initializing");
                viewer.initializeOCRViewer();
            });
        }
        debug("main() completed");
    }
    
    public void initializeOCRViewer() {
        debug("initializeOCRViewer() called - Multi-page OCR Viewer");
        if (state.initialized()) {
            debug("Already initialized, skipping");
            return;
        }
        
        // Detect document type
        isMultiPageDocument = pageManager.isMultiPage();
        debug("Document type: " + (isMultiPageDocument ? "Multi-page" : "Single-page"));
        
        // Clean up any existing content
        debug("Cleaning up DOM...");
        cleanupDOM();
        
        // Process all pages
        debug("Processing pages...");
        pageManager.processAllPages((pageElement, pageNumber, isMultiPage) -> {
            var pageData = pageProcessor.processPage(pageElement, pageNumber, isMultiPage);
            allPagesData.add(pageData);
        });
        
        // Create document-level controls
        debug("Creating document controls...");
        createDocumentControls();
        
        // Create page-level SVG sections
        debug("Creating SVG sections...");
        createAllSVGSections();
        
        // Bind global event handlers
        debug("Binding event handlers...");
        bindDocumentEventHandlers();
        
        // Update display
        debug("Updating display...");
        updateDisplay();
        
        state = state.withInitialized(true);
        debug("Multi-page OCR Viewer initialized successfully!");
        debug("Processed " + allPagesData.size() + " pages");
        
        // Debug export for first page
        if (!allPagesData.isEmpty()) {
            debug("Exporting debug info for first page...");
            exportPageDebugInfo(0);
        }
        
        debug("initializeOCRViewer() completed");
    }
    
    // Old single-page methods removed - now handled by OCRPageProcessor
    
    // New multi-page methods
    
    private void createDocumentControls() {
        debug("Creating document-level control panel...");
        
        // Unified control panel for both single and multi-page documents
        createControlPanel();
    }
    
    private void createControlPanel() {
        // Global controls for sticky control bar - all OFF by default for clean XHTML experience
        var globalControls = List.of(
            new UIElementFactory.ControlConfig("toggle-line-boxes", "Line Boxes", false),
            new UIElementFactory.ControlConfig("toggle-word-boxes", "Word Boxes", false),
            new UIElementFactory.ControlConfig("toggle-xhtml-text", "XHTML Text", true),  // Keep original XHTML visible
            new UIElementFactory.ControlConfig("toggle-svg-text", "SVG Text", false),
            new UIElementFactory.ControlConfig("toggle-hover-controls", "Hover Controls", false),
            new UIElementFactory.ControlConfig("toggle-svg-section", "SVG Section", false),
            new UIElementFactory.ControlConfig("toggle-svg-background", "SVG Background", false)
        );
        
        // Create sticky control bar at top
        var stickyControlBar = UIElementFactory.createStickyControlBar(globalControls);
        document.getBody().insertBefore(stickyControlBar, document.getBody().getFirstChild());
        
        // Add document info panel - works for both single and multi-page
        var docInfo = pageManager.getDocumentMetadata();
        var infoPanel = (HTMLElement) document.createElement("div");
        infoPanel.setClassName("document-info-panel");
        infoPanel.setInnerHTML("<div style='padding: 8px 16px; background: #f8f9fa; border-bottom: 1px solid #dee2e6; font-size: 13px;'>" +
                              "<strong>" + docInfo.pageCount() + " pages</strong> | " + 
                              docInfo.totalWords() + " words | " + 
                              Math.round(docInfo.averageConfidence() * 1000.0) / 10.0 + "% avg confidence</div>");
        
        // Insert info panel after control bar
        var controlBar = document.getElementById("top-control-bar");
        if (controlBar != null && controlBar.getNextSibling() != null) {
            document.getBody().insertBefore(infoPanel, controlBar.getNextSibling());
        } else {
            document.getBody().appendChild(infoPanel);
        }
    }
    
    private void createAllSVGSections() {
        debug("Creating SVG sections for all pages...");
        
        for (int i = 0; i < allPagesData.size(); i++) {
            var pageData = allPagesData.get(i);
            var pageElement = pageManager.getPage(i + 1);
            
            if (pageElement.isPresent()) {
                createSVGSectionForPage(pageElement.get(), pageData, i + 1);
            }
        }
    }
    
    private void createSVGSectionForPage(HTMLElement pageElement, OCRData pageData, int pageNumber) {
        debug("Creating SVG section for page " + pageNumber);
        
        // Find the ocrContent element within this specific page
        var ocrContent = pageElement.querySelector(".ocrContent");
        if (ocrContent == null) {
            debug("ERROR: Missing .ocrContent element for page " + pageNumber);
            return;
        }
        
        // Create SVG container for this specific page
        var svgContainer = (HTMLElement) document.createElement("div");
        svgContainer.setClassName("svg-content page-" + pageNumber);
        svgContainer.setAttribute("data-page", String.valueOf(pageNumber));
        
        var containerStyle = """
            position: relative;
            margin-top: 20px;
            border-top: 2px solid #ddd;
            padding: 20px;
            background: #fafafa;
            overflow: auto;
            max-width: 100%;
            """;
        svgContainer.getStyle().setCssText(containerStyle);
        
        // Add page number header
        var pageHeader = (HTMLElement) document.createElement("h3");
        pageHeader.setTextContent("Page " + pageNumber + " - SVG Visualization");
        pageHeader.getStyle().setCssText("margin-top: 0; color: #666;");
        svgContainer.appendChild(pageHeader);
        
        // Generate SVG for this specific page data
        var svg = generateSVGFromPageData(pageData);
        svgContainer.appendChild(svg);
        
        // Insert after the ocrContent within this page
        ocrContent.getParentNode().insertBefore(svgContainer, ocrContent.getNextSibling());
        debug("SVG section created and inserted for page " + pageNumber);
    }
    
    private void bindDocumentEventHandlers() {
        debug("Binding document-level event handlers...");
        bindEventHandlers(); // Use existing logic for now
    }
    
    private void exportPageDebugInfo(int pageIndex) {
        if (pageIndex < allPagesData.size()) {
            var pageData = allPagesData.get(pageIndex);
            debug("=== DEBUG: Page " + (pageIndex + 1) + " ===");
            debug("Words: " + pageData.metadata().totalWords() + 
                  ", Lines: " + pageData.metadata().totalLines() +
                  ", Confidence: " + Math.round(pageData.metadata().averageConfidence() * 1000.0) / 10.0 + "%");
        }
    }
    
    // ControlConfig record moved to UIElementFactory - using static import
    
    // UI creation methods moved to UIElementFactory - using static imports
    
    private void setupHTMLSection() {
        // Process ALL pages - get all section elements
        var sections = document.querySelectorAll("section.win11OneOcrPage");
        debug("Setting up HTML sections for " + sections.getLength() + " pages");
        
        for (int pageIndex = 0; pageIndex < sections.getLength(); pageIndex++) {
            var pageSection = (HTMLElement) sections.get(pageIndex);
            var pageNumber = pageIndex + 1;
            
            debug("Setting up HTML section for page " + pageNumber);
            setupHTMLSectionForPage(pageSection, pageIndex, pageNumber);
        }
        
        debug("HTML section setup completed for all pages");
    }
    
    private void setupHTMLSectionForPage(HTMLElement pageSection, int pageIndex, int pageNumber) {
        // Apply confidence classes for this specific page
        if (pageIndex < allPagesData.size()) {
            var pageData = allPagesData.get(pageIndex);
            pageData.lines().forEach(line -> applyConfidenceClassesForPage(line, pageSection));
        }
        
        // Add hover controls for this page
        addCleanHoverControlsForPage(pageSection, pageIndex, pageNumber);
    }
    
    
    private void applyConfidenceClassesForPage(LineData line, HTMLElement pageSection) {
        // Add line number to segment within this specific page (FIXED: Page-scoped selector)
        var segmentElement = pageSection.querySelector("segment:nth-child(" + (line.id() + 1) + ")");
        if (segmentElement != null) {
            segmentElement.setAttribute("data-line-number", String.valueOf(line.id() + 1));
            debug("Added data-line-number=" + (line.id() + 1) + " to segment " + line.id() + " on page");
        }
        
        // Apply confidence classes to words within this specific page (FIXED: Page-scoped selectors)
        for (int index = 0; index < line.words().size(); index++) {
            var word = line.words().get(index);
            var wordElement = pageSection.querySelector(
                "segment:nth-child(" + (line.id() + 1) + ") w:nth-child(" + (index + 1) + ")");
            
            if (wordElement != null) {
                var wEle = (HTMLElement)wordElement;
                wEle.setClassName("");
                var level = ConfidenceLevel.fromConfidence(word.confidence(), config);
                wEle.getClassList().add(level.htmlClass());
            }
        }
    }
    
    private void addCleanHoverControls() {
        var sections = document.querySelectorAll("section.win11OneOcrPage");
        debug("Adding hover controls for " + sections.getLength() + " pages");
        
        for (int pageIndex = 0; pageIndex < sections.getLength(); pageIndex++) {
            var pageSection = (HTMLElement) sections.get(pageIndex);
            var pageNumber = pageIndex + 1;
            
            debug("Adding hover controls for page " + pageNumber);
            addCleanHoverControlsForPage(pageSection, pageIndex, pageNumber);
        }
    }
    
    private void addCleanHoverControlsForPage(HTMLElement pageSection, int pageIndex, int pageNumber) {
        var segments = pageSection.querySelectorAll("segment");
        debug("Adding hover controls to " + segments.getLength() + " segments in page " + pageNumber);
        
        IntStream.range(0, segments.getLength())
            .forEach(i -> addHoverControlsToSegment((HTMLElement)segments.get(i), i));
    }
    
    private void addHoverControlsToSegment(HTMLElement segment, int lineIndex) {
        segment.addEventListener("mouseenter", evt -> {
            if (!state.enableHoverControls()) return;
            cancelHideControlsTimer();
            showLineControls((MouseEvent) evt, lineIndex, segment);
        });
        
        segment.addEventListener("mouseleave", evt -> {
            scheduleHideControls(evt);
        });
    }
    
    private void showLineControls(MouseEvent event, int lineIndex, HTMLElement segment) {
        hideControls();
        
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        if (lineIndex >= currentPageData.lines().size()) return;
        
        var controls = createFloatingControls();
        var copyButton = createCopyButton(lineIndex);
        controls.appendChild(copyButton);
        
        positionControlsWithinElement(controls, segment, "line");
        document.getBody().appendChild(controls);
    }
    
    private HTMLElement createCopyButton(int lineIndex) {
        var copyButton = (HTMLButtonElement) document.createElement("button");
        copyButton.setInnerHTML("📋"); // Copy icon
        copyButton.setTitle("Copy line " + (lineIndex + 1));
        
        // Apply styles using text blocks (JDK 15+)
        var buttonStyle = """
            background: none;
            border: none;
            color: white;
            cursor: pointer;
            font-size: 12px;
            padding: 2px 4px;
            border-radius: 2px;
            """;
        copyButton.getStyle().setCssText(buttonStyle);
        
        copyButton.addEventListener("click", evt -> copyLineText(lineIndex));
        copyButton.addEventListener("mouseenter", evt -> 
            copyButton.getStyle().setProperty("background", "rgba(255, 255, 255, 0.2)"));
        copyButton.addEventListener("mouseleave", evt -> 
            copyButton.getStyle().setProperty("background", "none"));
        
        return copyButton;
    }
    
    // Text extraction and clipboard methods moved to TextUtilities - using static imports
    
    private void copyLineText(int lineIndex) {
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        if (lineIndex >= currentPageData.lines().size()) return;
        copyLineTextWithNotification(currentPageData.lines().get(lineIndex));
    }
    
    private void copyPageText() {
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        copyPageTextWithNotification(currentPageData.lines());
    }
    
    @JSBody(params = {"text"}, script = """
        navigator.clipboard.writeText(text).then(function() {
            console.log('Copied to clipboard');
        }).catch(function(err) {
            console.error('Failed to copy: ', err);
        });
        """)
    private static native void copyToClipboard(String text);
    
    
    private HTMLElement createSVGSection() {
        debug("Creating SVG section...");
        var section = document.querySelector("section.win11OneOcrPage");
        var ocrContent = document.querySelector(".ocrContent");
        if (section == null || ocrContent == null) {
            debug("ERROR: Missing required elements for SVG section (section or .ocrContent)");
            return null;
        }
        
        var svgContainer = (HTMLElement) document.createElement("div");
        svgContainer.setClassName("svg-content");
        
        var containerStyle = """
            position: relative;
            margin-top: 20px;
            border-top: 2px solid #ddd;
            padding: 20px;
            background: #fafafa;
            overflow: auto;
            max-width: 100%;
            """;
        svgContainer.getStyle().setCssText(containerStyle);
        
        var svg = generateSVGFromVirtualDOM();
        svgContainer.appendChild(svg);
        
        // Insert after ocrContent
        ocrContent.getParentNode().insertBefore(svgContainer, ocrContent.getNextSibling());
        debug("SVG section created and inserted into DOM");
        
        return svgContainer;
    }
    
    private HTMLElement generateSVGFromVirtualDOM() {
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        var metadata = currentPageData.metadata();
        var svg = (HTMLElement)document.createElementNS("http://www.w3.org/2000/svg", "svg");
        
        svg.setAttribute("width", String.valueOf(metadata.imageWidth()));
        svg.setAttribute("height", String.valueOf(metadata.imageHeight()));
        svg.setAttribute("viewBox", "0 0 " + metadata.imageWidth() + " " + metadata.imageHeight());
        
        var svgStyle = """
            border: 1px solid #ccc;
            background: white;
            width: 100%;
            height: auto;
            """;
        svg.getStyle().setCssText(svgStyle);
        
        // Add SVG content using method chaining
        addSVGStyles(svg);
        addBackgroundLayer(svg);
        addWordLayers(svg);
        
        return svg;
    }
    
    private HTMLElement generateSVGFromPageData(OCRData pageData) {
        var metadata = pageData.metadata();
        var svg = (HTMLElement)document.createElementNS("http://www.w3.org/2000/svg", "svg");
        
        svg.setAttribute("width", String.valueOf(metadata.imageWidth()));
        svg.setAttribute("height", String.valueOf(metadata.imageHeight()));
        svg.setAttribute("viewBox", "0 0 " + metadata.imageWidth() + " " + metadata.imageHeight());
        
        var svgStyle = """
            border: 1px solid #ccc;
            background: white;
            width: 100%;
            height: auto;
            """;
        svg.getStyle().setCssText(svgStyle);
        
        // Add SVG content using method chaining
        addSVGStyles(svg);
        addBackgroundLayer(svg, pageData);
        addWordLayers(svg, pageData);
        
        return svg;
    }
    
    private void addSVGStyles(HTMLElement svg) {
        var defs = document.createElementNS("http://www.w3.org/2000/svg", "defs");
        var style = document.createElementNS("http://www.w3.org/2000/svg", "style");
        
        // Using text blocks for clean CSS
        var cssContent = """
            .line-box { fill: none; stroke: #000000; stroke-width: 0.8; stroke-dasharray: 4,2; }
            .word-box-high { fill: none; stroke: #00aa00; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-box-med { fill: none; stroke: #ffaa00; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-box-low { fill: none; stroke: #ff0000; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-text { font-family: Arial, sans-serif; font-size: 12px; fill: #0066cc; font-weight: bold; }
            .svg-layer { display: block; }
            .svg-layer.hidden { display: none; }
            """;
        
        style.setTextContent(cssContent);
        defs.appendChild(style);
        svg.appendChild(defs);
    }
    
    private void bindEventHandlers() {
        debug("Binding event handlers for control toggles...");
        var eventBindings = Map.of(
            "toggle-line-boxes", (EventListener<Event>) evt -> 
                state = state.withShowLineBoxes(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-word-boxes", (EventListener<Event>) evt -> 
                state = state.withShowWordBoxes(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-xhtml-text", (EventListener<Event>) evt -> 
                state = state.withShowXHTMLText(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-svg-text", (EventListener<Event>) evt -> 
                state = state.withShowSVGText(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-hover-controls", (EventListener<Event>) evt -> 
                state = state.withEnableHoverControls(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-svg-section", (EventListener<Event>) evt -> 
                state = state.withShowSVGSection(((HTMLInputElement) evt.getTarget()).isChecked()),
            "toggle-svg-background", (EventListener<Event>) evt -> 
                state = state.withShowSVGBackground(((HTMLInputElement) evt.getTarget()).isChecked())
        );
        
        eventBindings.forEach((id, listener) -> {
            var element = document.getElementById(id);
            if (element != null) {
                debug("Bound event handler for: " + id);
                element.addEventListener("change", e -> {
                    debug("Toggle changed: " + id + " = " + ((HTMLInputElement) e.getTarget()).isChecked());
                    listener.handleEvent(e);
                    updateDisplay();
                });
            } else {
                debug("WARNING: Element not found for binding: " + id);
            }
        });
        debug("Event handler binding completed");
    }
    
    private void updateDisplay() {
        debug("updateDisplay() called with state: showLineBoxes=" + state.showLineBoxes() + 
              ", showWordBoxes=" + state.showWordBoxes() + 
              ", showXHTMLText=" + state.showXHTMLText() + ", showSVGSection=" + state.showSVGSection());
        
        // Update ALL pages - get all section elements
        var sections = document.querySelectorAll("section.win11OneOcrPage");
        debug("Found " + sections.getLength() + " page sections to update");
        
        for (int pageIndex = 0; pageIndex < sections.getLength(); pageIndex++) {
            var section = (HTMLElement) sections.get(pageIndex);
            updateDisplayForPage(section, pageIndex + 1);
        }
        
        debug("updateDisplay() completed for all pages");
    }
    
    private void updateDisplayForPage(HTMLElement pageSection, int pageNumber) {
        debug("Updating display for page " + pageNumber);
        
        // Update line boxes within this specific page
        var segments = pageSection.querySelectorAll("segment");
        for (int i = 0; i < segments.getLength(); i++) {
            var segment = (HTMLElement) segments.get(i);
            updateElementVisibility(segment, state.showLineBoxes(), "show-line-boxes");
        }
        debug("Updated " + segments.getLength() + " segments on page " + pageNumber);
        
        // Update page-level display options
        updateElementVisibility(pageSection, state.showWordBoxes(), "show-word-boxes");
        updateElementVisibility(pageSection, !state.showXHTMLText(), "hide-text");
        
        // Update SVG layers within this page
        updateSVGLayerVisibilityForPage(pageSection, "svg-background-layer", state.showSVGBackground());
        updateSVGLayerVisibilityForPage(pageSection, "svg-line-boxes", state.showLineBoxes());
        updateSVGLayerVisibilityForPage(pageSection, "svg-word-boxes", state.showWordBoxes());
        updateSVGLayerVisibilityForPage(pageSection, "svg-text-layer", state.showSVGText());
        
        // Update SVG section visibility for this page
        var svgContainer = pageSection.querySelector(".svg-content");
        if (svgContainer != null) {
            String displayValue = state.showSVGSection() ? "block" : "none";
            ((HTMLElement) svgContainer).getStyle().setProperty("display", displayValue);
            debug("Page " + pageNumber + " SVG section visibility set to: " + displayValue);
        } else {
            debug("WARNING: No .svg-content found for page " + pageNumber);
        }
    }
    
    // updateElementVisibility moved to DomUtilities - using static import
    
    private void updateSVGLayerVisibility(String id, boolean visible) {
        var layer = document.getElementById(id);
        if (layer != null) {
            if (visible) {
                layer.getClassList().remove("hidden");
            } else {
                layer.getClassList().add("hidden");
            }
        }
    }
    
    private void updateSVGLayerVisibilityForPage(HTMLElement pageSection, String layerId, boolean visible) {
        var layer = pageSection.querySelector("#" + layerId);
        if (layer != null) {
            if (visible) {
                ((HTMLElement) layer).getClassList().remove("hidden");
            } else {
                ((HTMLElement) layer).getClassList().add("hidden");
            }
        }
    }
    
    private void exportLineSVGToConsole(int lineIndex) {
        console("\n=== DEBUG: SVG Elements for Line " + (lineIndex + 1) + " ===");
        
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        if (lineIndex >= currentPageData.lines().size()) {
            console("No data for line " + (lineIndex + 1));
            return;
        }
        
        var line = currentPageData.lines().get(lineIndex);
        console("Line " + (lineIndex + 1) + " has " + line.words().size() + " words:");
        
        for (int i = 0; i < line.words().size(); i++) {
            final int index = i;
            var word = line.words().get(index);
            word.boundingBox().ifPresent(bbox -> {
                var level = ConfidenceLevel.fromConfidence(word.confidence(), config);
                var points = bbox.toPolygonPoints();
                
                console("Word " + index + ": \"" + word.text() + "\"");
                console("  <polygon id=\"word-" + lineIndex + "-" + index + "\" points=\"" + points + "\" class=\"" + level.svgClass() + "\" />");
                console("  Confidence: " + Math.round(word.confidence() * 1000.0) / 10.0 + "% | BBox: " + 
                    Math.round(bbox.x1() * 10.0) / 10.0 + "," + Math.round(bbox.y1() * 10.0) / 10.0 + "," + 
                    Math.round(bbox.x2() * 10.0) / 10.0 + "," + Math.round(bbox.y2() * 10.0) / 10.0);
                console("");
            });
        }
        
        console("=== END DEBUG ===\n");
    }
    
    // Utility methods moved to static utility classes - using static imports
    
    //@JSBody(params = {"message"}, script = "console.log(message);")
    private static /*native*/ void console(String message){
        System.out.println(message); // tea vm makes this as console.log by itself
    }
    
    // Debug helper method
    private static void debug(String message) {
        if (VERBOSE_DEBUG) {
            System.out.println("[TeaVM-OCRViewer] " + message);
            //console("[TeaVM-OCRViewer] " + message);
        }
    }
    
    // Timer implementation for TeaVM
    private void scheduleHideControls(Event event) {
        if (hideControlsTimer != null) {
            hideControlsTimer.cancel();
        }
        hideControlsTimer = new Timer();
        hideControlsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                hideControls();
            }
        }, 100);
    }
    
    private void cancelHideControlsTimer() {
        if (hideControlsTimer != null) {
            hideControlsTimer.cancel();
            hideControlsTimer = null;
        }
    }
    
    private void hideControls() {
        var existing = document.getElementById("floating-controls");
        if (existing != null) {
            removeElement(existing);//existing.remove();
        }
    }
    
    // Complete helper method implementations
    
    // UI creation methods moved to UIElementFactory - using static imports
    
    private void updateStats() {
        var statsDiv = document.getElementById("ocr-stats");
        if (statsDiv == null || allPagesData.isEmpty()) return;
        
        var currentPageData = allPagesData.get(0);
        var meta = currentPageData.metadata();
        var statsHtml = "<div>" + meta.totalLines() + " lines, " + meta.totalWords() + " words</div>" +
                        "<div>Avg confidence: " + Math.round(meta.averageConfidence() * 1000.0) / 10.0 + "%</div>" +
                        "<div>Page angle: " + Math.round(meta.angle() * 10.0) / 10.0 + "°</div>";
        
        ((HTMLElement) statsDiv).setInnerHTML(statsHtml);
    }
    
    // UI positioning methods moved to NotificationUtilities - using static imports
    
    private void addPageCopyButton() {
        var section = document.querySelector("section.win11OneOcrPage");
        if (section == null) return;
        
        var copyBtn = (HTMLButtonElement) document.createElement("button");
        copyBtn.setTextContent("Copy Page");
        
        var buttonStyle = """
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
            """;
        copyBtn.getStyle().setCssText(buttonStyle);
        
        copyBtn.addEventListener("click", evt -> copyPageText());
        section.appendChild(copyBtn);
    }
    
    private void cleanupDOM() {
        // Remove existing control panel
        removeFromDocument(".control-panel");
        
        // Remove existing hover controls and info popups
        removeAllFromDocument(".copy-controls, .info-popup");

        // Remove existing SVG section
        removeFromDocument(".svg-content");
        
        // Remove page copy buttons
        removeAllFromDocument("section.win11OneOcrPage button");
        
        // Remove background image elements
        removeAllFromDocument(".background-image");
    }
    
    // DOM manipulation methods moved to DomUtilities - using static imports with global document
    
    private Optional<Element> addBackgroundLayer(Element svg) {
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        currentPageData.backgroundImagePath().ifPresent(imagePath -> {
            var bgGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
            bgGroup.setAttribute("id", "svg-background-layer");
            bgGroup.setAttribute("class", "svg-layer hidden");
            
            var bgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
            bgImage.setAttribute("href", imagePath);
            bgImage.setAttribute("x", "0");
            bgImage.setAttribute("y", "0");
            bgImage.setAttribute("width", String.valueOf(currentPageData.metadata().imageWidth()));
            bgImage.setAttribute("height", String.valueOf(currentPageData.metadata().imageHeight()));
            bgImage.setAttribute("preserveAspectRatio", "none");
            bgImage.setAttribute("opacity", "1.0");
            
            bgGroup.appendChild(bgImage);
            svg.appendChild(bgGroup);
        });
        
        return Optional.of(svg);
    }
    
    // Overloaded method that accepts specific page data
    private Optional<Element> addBackgroundLayer(Element svg, OCRData pageData) {
        pageData.backgroundImagePath().ifPresent(imagePath -> {
            var bgGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
            bgGroup.setAttribute("id", "svg-background-layer");
            bgGroup.setAttribute("class", "svg-layer hidden");
            
            var bgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
            bgImage.setAttribute("href", imagePath);
            bgImage.setAttribute("x", "0");
            bgImage.setAttribute("y", "0");
            bgImage.setAttribute("width", String.valueOf(pageData.metadata().imageWidth()));
            bgImage.setAttribute("height", String.valueOf(pageData.metadata().imageHeight()));
            bgImage.setAttribute("preserveAspectRatio", "none");
            bgImage.setAttribute("opacity", "1.0");
            
            bgGroup.appendChild(bgImage);
            svg.appendChild(bgGroup);
        });
        
        return Optional.of(svg);
    }
    
    private Optional<Element> addWordLayers(Element svg) {
        var currentPageData = !allPagesData.isEmpty() ? allPagesData.get(0) : createEmptyOCRData();
        var metadata = currentPageData.metadata();
        
        // Create layer groups
        var lineBoxGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        lineBoxGroup.setAttribute("id", "svg-line-boxes");
        lineBoxGroup.setAttribute("class", "svg-layer hidden");
        
        var wordBoxGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        wordBoxGroup.setAttribute("id", "svg-word-boxes");
        wordBoxGroup.setAttribute("class", "svg-layer hidden");
        
        var textGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        textGroup.setAttribute("id", "svg-text-layer");
        textGroup.setAttribute("class", "svg-layer");
        
        // Process words using streams and modern Java features
        currentPageData.lines().stream()
            .flatMap(line -> IntStream.range(0, line.words().size())
                .mapToObj(wordIndex -> new WordWithPosition(line, wordIndex, line.words().get(wordIndex))))
            .filter(wp -> wp.word().boundingBox().isPresent())
            .forEach(wp -> addWordToSVG(wp, wordBoxGroup, textGroup));
        
        svg.appendChild(lineBoxGroup);
        svg.appendChild(wordBoxGroup);
        svg.appendChild(textGroup);
        
        return Optional.of(svg);
    }
    
    // Overloaded method that accepts specific page data
    private Optional<Element> addWordLayers(Element svg, OCRData pageData) {
        var metadata = pageData.metadata();
        
        // Create layer groups
        var lineBoxGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        lineBoxGroup.setAttribute("id", "svg-line-boxes");
        lineBoxGroup.setAttribute("class", "svg-layer hidden");
        
        var wordBoxGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        wordBoxGroup.setAttribute("id", "svg-word-boxes");
        wordBoxGroup.setAttribute("class", "svg-layer hidden");
        
        var textGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
        textGroup.setAttribute("id", "svg-text-layer");
        textGroup.setAttribute("class", "svg-layer");
        
        // Process words using streams and modern Java features
        pageData.lines().stream()
            .flatMap(line -> IntStream.range(0, line.words().size())
                .mapToObj(wordIndex -> new WordWithPosition(line, wordIndex, line.words().get(wordIndex))))
            .filter(wp -> wp.word().boundingBox().isPresent())
            .forEach(wp -> addWordToSVG(wp, wordBoxGroup, textGroup));
        
        svg.appendChild(lineBoxGroup);
        svg.appendChild(wordBoxGroup);
        svg.appendChild(textGroup);
        
        return Optional.of(svg);
    }
    
    private record WordWithPosition(LineData line, int wordIndex, WordData word) {}
    
    private void addWordToSVG(WordWithPosition wp, Element wordBoxGroup, Element textGroup) {
        var bbox = wp.word().boundingBox().orElseThrow();
        var confidence = wp.word().confidence();
        var level = ConfidenceLevel.fromConfidence(confidence, config);
        
        // Create word bounding box polygon
        var polygon = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
        polygon.setAttribute("points", bbox.toPolygonPoints());
        polygon.setAttribute("class", level.svgClass());
        polygon.setAttribute("id", "word-" + wp.line().id() + "-" + wp.wordIndex());
        
        // Create text element with Java-calculated positioning
        var text = createSVGTextElement(wp.word(), bbox, wp.line().id(), wp.wordIndex());
        
        // Add click handler for word details
        var group = (HTMLElement)document.createElementNS("http://www.w3.org/2000/svg", "g");
        group.getStyle().setProperty("cursor", "pointer");
        group.appendChild(polygon.cloneNode(true));
        group.appendChild(text.cloneNode(true));
        
        group.addEventListener("click", evt -> showSVGWordDetails(wp.word(), (MouseEvent) evt));
        
        wordBoxGroup.appendChild(polygon);
        textGroup.appendChild(text);
    }
    
    // createSVGTextElement moved to SvgUtilities - using static import with global document
    
    // Notification and popup methods moved to NotificationUtilities - using static imports
    // Clipboard methods moved to TextUtilities - using static imports
    
    // Enhanced element querying moved to DomUtilities - using static imports with global document
}