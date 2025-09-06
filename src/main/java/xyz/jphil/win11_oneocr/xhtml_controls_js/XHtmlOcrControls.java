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

// Static imports for utility methods - minimal refactoring impact
import static xyz.jphil.win11_oneocr.xhtml_controls_js.AttributeParser.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.DomUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.OCRDataFactory.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.SvgUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.UIElementFactory.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.NotificationUtilities.*;
import static xyz.jphil.win11_oneocr.xhtml_controls_js.TextUtilities.*;


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
    
    
    // Instance variables
    private final Config config = Config.DEFAULT;
    private OCRData ocrData;
    private ViewerState state = ViewerState.DEFAULT;
    private final HTMLDocument document = Window.current().getDocument();
    private Timer hideControlsTimer;
    
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
        debug("initializeOCRViewer() called");
        if (state.initialized()) {
            debug("Already initialized, skipping");
            return;
        }
        
        debug("Building virtual DOM from XHTML...");
        ocrData = buildVirtualDOMFromXHTML();
        
        debug("Cleaning up DOM...");
        cleanupDOM();
        debug("Creating control panel...");
        createControlPanel();
        debug("Setting up HTML section...");
        setupHTMLSection();
        debug("Creating SVG section...");
        createSVGSection();
        debug("Binding event handlers...");
        bindEventHandlers();
        debug("Updating display...");
        updateDisplay();
        
        state = state.withInitialized(true);
        debug("OCR XHTML Viewer initialized with TeaVM - SUCCESS!");
        
        // Debug export
        debug("Exporting debug info for line 2...");
        exportLineSVGToConsole(2);
        debug("initializeOCRViewer() completed");
    }
    
    private OCRData buildVirtualDOMFromXHTML() {
        debug("buildVirtualDOMFromXHTML() - looking for section.win11OneOcrPage");
        var section = document.querySelector("section.win11OneOcrPage");
        if (section == null) {
            debug("ERROR: No section.win11OneOcrPage found! Creating empty OCR data.");
            return createEmptyOCRData();
        }
        debug("Found section.win11OneOcrPage element");
        
        // Extract metadata using enhanced pattern matching approach
        var metadata = new Metadata(
            Optional.ofNullable(section.getAttribute("srcName")).orElse("Unknown"),
            parseIntAttribute(section, "imgWidth", 800),
            parseIntAttribute(section, "imgHeight", 600),
            parseDoubleAttribute(section, "angle", 0.0),
            parseDoubleAttribute(section, "averageOcrConfidence", 0.0),
            parseIntAttribute(section, "ocrWordsCount", 0),
            parseIntAttribute(section, "ocrSegmentsCount", 0)
        );
        
        // Extract lines using streams and modern Java features
        var segments = document.querySelectorAll("segment");
        var lines = IntStream.range(0, segments.getLength())
            .mapToObj(i -> buildLineData(segments.get(i), i))
            .toList(); // JDK 16+ - List.toList()
        
        debug("Virtual DOM built with " + lines.size() + " lines, " + 
               metadata.totalWords() + " words, avg confidence: " + 
               Math.round(metadata.averageConfidence() * 1000.0) / 10.0 + "%");
        
        return new OCRData(
            metadata,
            lines,
            Optional.of(metadata.filename())
        );
    }
    
    private LineData buildLineData(Element segment, int lineIndex) {
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
    
    // parseBoundingBox moved to AttributeParser - using static import
    
    private void createControlPanel() {
        debug("Creating control panel...");
        var controlPanel = (HTMLElement) document.createElement("div");
        controlPanel.setClassName("control-panel");
        
        // Create control elements using static utility methods
        createHoverHint(controlPanel);
        createTitle(controlPanel, "OCR Display Controls");
        
        // Control groups using switch expressions for default values
        var controls = List.of(
            new ControlConfig("toggle-line-boxes", "Line Boxes", false),
            new ControlConfig("toggle-word-boxes", "Word Boxes", true),
            new ControlConfig("toggle-xhtml-text", "Text Content (XHTML)", true),
            new ControlConfig("toggle-svg-text", "Text Content (SVG)", false),
            new ControlConfig("toggle-hover-controls", "Hover Controls", true),
            new ControlConfig("toggle-svg-section", "SVG Section", true),
            new ControlConfig("toggle-svg-background", "SVG Background", true)
        );
        
        controls.forEach(config -> controlPanel.appendChild(createControlGroup(config)));
        
        // Confidence legend
        controlPanel.appendChild(createConfidenceLegend());
        
        // Stats container
        var stats = createStatsContainer();
        controlPanel.appendChild(stats);
        
        document.getBody().appendChild(controlPanel);
        debug("Control panel added to DOM");
        updateStats();
    }
    
    // ControlConfig record moved to UIElementFactory - using static import
    
    // UI creation methods moved to UIElementFactory - using static imports
    
    private void setupHTMLSection() {
        var section = document.querySelector("section.win11OneOcrPage");
        if (section == null) return;
        
        // Apply confidence classes using streams and enhanced switch
        ocrData.lines().forEach(this::applyConfidenceClasses);
        
        addCleanHoverControls();
        addPageCopyButton();
    }
    
    private void applyConfidenceClasses(LineData line) {
        // Add line number to segment (required for CSS line number display)
        var segmentElement = document.querySelector("segment:nth-child(" + (line.id() + 1) + ")");
        if (segmentElement != null) {
            segmentElement.setAttribute("data-line-number", String.valueOf(line.id() + 1));
            debug("Added data-line-number=" + (line.id() + 1) + " to segment " + line.id());
        }
        
        // Apply confidence classes to words
        for (int index = 0; index < line.words().size(); index++) {
            var word = line.words().get(index);
            var wordElement = document.querySelector(
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
        var section = document.querySelector("section.win11OneOcrPage");
        if (section == null) return;
        
        var segments = document.querySelectorAll("segment");
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
        
        if (lineIndex >= ocrData.lines().size()) return;
        
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
        if (lineIndex >= ocrData.lines().size()) return;
        copyLineTextWithNotification(ocrData.lines().get(lineIndex));
    }
    
    private void copyPageText() {
        copyPageTextWithNotification(ocrData.lines());
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
        var metadata = ocrData.metadata();
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
        var section = document.querySelector("section.win11OneOcrPage");
        if (section == null) {
            debug("ERROR: section.win11OneOcrPage not found in updateDisplay()");
            return;
        }
        
        // Update line boxes - add/remove show-line-boxes class on each segment (matching JavaScript)
        var segments = document.querySelectorAll("segment");
        for (int i = 0; i < segments.getLength(); i++) {
            var segment = (HTMLElement) segments.get(i);
            updateElementVisibility(segment, state.showLineBoxes(), "show-line-boxes");
        }
        debug("Updated line boxes visibility on " + segments.getLength() + " segments: " + state.showLineBoxes());
        
        // Update display using pattern matching-like approach with switch expressions
        updateElementVisibility(section, state.showWordBoxes(), "show-word-boxes");
        updateElementVisibility(section, !state.showXHTMLText(), "hide-text");
        
        // Update SVG layers
        updateSVGLayerVisibility("svg-background-layer", state.showSVGBackground());
        updateSVGLayerVisibility("svg-line-boxes", state.showLineBoxes());
        updateSVGLayerVisibility("svg-word-boxes", state.showWordBoxes());
        updateSVGLayerVisibility("svg-text-layer", state.showSVGText());
        
        // Update SVG section visibility
        var svgContainer = document.querySelector(".svg-content");
        if (svgContainer != null) {
            String displayValue = state.showSVGSection() ? "block" : "none";
            ((HTMLElement) svgContainer).getStyle().setProperty("display", displayValue);
            debug("SVG section visibility set to: " + displayValue);
        } else {
            debug("WARNING: .svg-content not found for visibility update");
        }
        debug("updateDisplay() completed");
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
    
    private void exportLineSVGToConsole(int lineIndex) {
        console("\n=== DEBUG: SVG Elements for Line " + (lineIndex + 1) + " ===");
        
        if (lineIndex >= ocrData.lines().size()) {
            console("No data for line " + (lineIndex + 1));
            return;
        }
        
        var line = ocrData.lines().get(lineIndex);
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
        if (statsDiv == null || ocrData == null) return;
        
        var meta = ocrData.metadata();
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
        ocrData.backgroundImagePath().ifPresent(imagePath -> {
            var bgGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
            bgGroup.setAttribute("id", "svg-background-layer");
            bgGroup.setAttribute("class", "svg-layer hidden");
            
            var bgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
            bgImage.setAttribute("href", imagePath);
            bgImage.setAttribute("x", "0");
            bgImage.setAttribute("y", "0");
            bgImage.setAttribute("width", String.valueOf(ocrData.metadata().imageWidth()));
            bgImage.setAttribute("height", String.valueOf(ocrData.metadata().imageHeight()));
            bgImage.setAttribute("preserveAspectRatio", "none");
            bgImage.setAttribute("opacity", "1.0");
            
            bgGroup.appendChild(bgImage);
            svg.appendChild(bgGroup);
        });
        
        return Optional.of(svg);
    }
    
    private Optional<Element> addWordLayers(Element svg) {
        var metadata = ocrData.metadata();
        
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
        ocrData.lines().stream()
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