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
    
    // Configuration using records (JDK 14+)
    public record Config(
        ConfidenceThresholds confidenceThresholds
    ) {
        public static final Config DEFAULT = new Config(
            new ConfidenceThresholds(0.8, 0.5)
        );
    }
    
    public record ConfidenceThresholds(double high, double med) {}
    
    // OCR Data Model using records
    public record BoundingBox(
        double x1, double y1, double x2, double y2,
        double x3, double y3, double x4, double y4
    ) {
        public double height() {
            return Math.abs(y3 - y1);
        }
        
        public double minX() {
            return Math.min(x1, x4);
        }
        
        public double minY() {
            return Math.min(y1, y2);
        }
        
        public String toPolygonPoints() {
            // Ensure proper polygon closure by setting x4=x1 for proper rectangle (matching old JS logic)
            return Math.round(x1*10)/10.0 + "," + Math.round(y1*10)/10.0 + " " +
                   Math.round(x2*10)/10.0 + "," + Math.round(y2*10)/10.0 + " " +
                   Math.round(x3*10)/10.0 + "," + Math.round(y3*10)/10.0 + " " +
                   Math.round(x1*10)/10.0 + "," + Math.round(y4*10)/10.0; // x4=x1 for proper closure
        }
    }
    
    public record WordData(
        String text,
        double confidence,
        int index,
        Optional<BoundingBox> boundingBox
    ) {}
    
    public record LineData(
        int id,
        Optional<BoundingBox> boundingBox,
        List<WordData> words
    ) {}
    
    public record Metadata(
        String filename,
        int imageWidth,
        int imageHeight,
        double angle,
        double averageConfidence,
        int totalWords,
        int totalLines
    ) {}
    
    public record OCRData(
        Metadata metadata,
        List<LineData> lines,
        Optional<String> backgroundImagePath
    ) {}
    
    // State management using records
    public record ViewerState(
        boolean showLineBoxes,
        boolean showWordBoxes,
        boolean showXHTMLText,
        boolean showSVGText,
        boolean enableHoverControls,
        boolean showSVGSection,
        boolean showSVGBackground,
        boolean initialized
    ) {
        public static final ViewerState DEFAULT = new ViewerState(
            false, true, true, false, true, true, true, false
        );
        
        public ViewerState withInitialized(boolean initialized) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowLineBoxes(boolean showLineBoxes) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowWordBoxes(boolean showWordBoxes) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowXHTMLText(boolean showXHTMLText) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowSVGText(boolean showSVGText) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withEnableHoverControls(boolean enableHoverControls) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowSVGSection(boolean showSVGSection) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
        
        public ViewerState withShowSVGBackground(boolean showSVGBackground) {
            return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
                showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
        }
    }
    
    // Confidence classification using enhanced switch expressions (JDK 14+)
    public enum ConfidenceLevel {
        HIGH("confidence-high", "word-box-high"),
        MEDIUM("confidence-med", "word-box-med"),
        LOW("confidence-low", "word-box-low");
        
        private final String htmlClass;
        private final String svgClass;
        
        ConfidenceLevel(String htmlClass, String svgClass) {
            this.htmlClass = htmlClass;
            this.svgClass = svgClass;
        }
        
        public String htmlClass() { return htmlClass; }
        public String svgClass() { return svgClass; }
        
        public static ConfidenceLevel fromConfidence(double confidence, Config config) {
            return confidence >= config.confidenceThresholds().high() ? HIGH :
                   confidence >= config.confidenceThresholds().med() ? MEDIUM : LOW;
        }
    }
    
    // Instance variables
    private final Config config = Config.DEFAULT;
    private OCRData ocrData;
    private ViewerState state = ViewerState.DEFAULT;
    private final HTMLDocument document = Window.current().getDocument();
    private Timer hideControlsTimer;
    
    /*@JSBody(script = "return typeof document !== 'undefined' && (document.readyState === 'complete' || document.readyState === 'interactive');")*/
    private static /*native*/ boolean isDocumentReady(){
        return true; // document is already loaded because we are using a loading script
    }
    
    public static void main(String[] args) {
        debug("main() called - starting TeaVM OCR Viewer");
        XHtmlOcrControls viewer = new XHtmlOcrControls();
        
        if (isDocumentReady()) {
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
    
    private Optional<BoundingBox> parseBoundingBox(String boundingBoxStr) {
        if (boundingBoxStr == null || boundingBoxStr.isBlank()) return Optional.empty();
        
        try {
            var coords = Arrays.stream(boundingBoxStr.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
                
            return coords.length >= 8 ? 
                Optional.of(new BoundingBox(coords[0], coords[1], coords[2], coords[3],
                                          coords[4], coords[5], coords[6], coords[7])) :
                Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    private void createControlPanel() {
        debug("Creating control panel...");
        var controlPanel = (HTMLElement) document.createElement("div");
        controlPanel.setClassName("control-panel");
        
        // Create control elements using method references and lambdas
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
    
    private record ControlConfig(String id, String label, boolean defaultChecked) {}
    
    private HTMLElement createControlGroup(ControlConfig config) {
        var group = (HTMLElement) document.createElement("div");
        group.setClassName("control-group");
        
        var label = (HTMLElement) document.createElement("label");
        label.setAttribute("for", config.id());
        label.setTextContent(config.label());
        
        var toggleSwitch = createToggleSwitch(config.id(), config.defaultChecked());
        
        group.appendChild(label);
        group.appendChild(toggleSwitch);
        
        return group;
    }
    
    private HTMLElement createToggleSwitch(String id, boolean checked) {
        var toggleSwitch = (HTMLElement) document.createElement("div");
        toggleSwitch.setClassName("toggle-switch");
        
        var input = (HTMLInputElement) document.createElement("input");
        input.setType("checkbox");
        input.setId(id);
        input.setChecked(checked);
        
        var slider = (HTMLElement) document.createElement("span");
        slider.setClassName("slider");
        
        toggleSwitch.appendChild(input);
        toggleSwitch.appendChild(slider);
        
        return toggleSwitch;
    }
    
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
        for (int index = 0; index < line.words.size(); index++) {
            var word = line.words.get(index);
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
    
    private void copyLineText(int lineIndex) {
        if (lineIndex >= ocrData.lines().size()) return;
        
        var line = ocrData.lines().get(lineIndex);
        var text = line.words().stream()
            .map(WordData::text)
            .collect(java.util.stream.Collectors.joining(" "));
            
        copyToClipboardSimple(text);
        showNotification("Copied to clipboard!");
    }
    
    private void copyPageText() {
        var allText = ocrData.lines().stream()
            .map(line -> line.words().stream()
                .map(WordData::text)
                .collect(java.util.stream.Collectors.joining(" ")))
            .collect(java.util.stream.Collectors.joining("\n"));
            
        copyToClipboardSimple(allText);
        showNotification("Copied to clipboard!");
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
    
    private void updateElementVisibility(HTMLElement element, boolean condition, String className) {
        if (condition) {
            element.getClassList().add(className);
        } else {
            element.getClassList().remove(className);
        }
    }
    
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
    
    // Utility methods
    private int parseIntAttribute(Element element, String attr, int defaultValue) {
        try {
            var value = element.getAttribute(attr);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private double parseDoubleAttribute(Element element, String attr, double defaultValue) {
        try {
            var value = element.getAttribute(attr);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private OCRData createEmptyOCRData() {
        return new OCRData(
            new Metadata("", 800, 600, 0.0, 0.0, 0, 0),
            List.of(),
            Optional.empty()
        );
    }
    
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
    
    private void createHoverHint(HTMLElement controlPanel) {
        var hoverHint = (HTMLElement) document.createElement("div");
        hoverHint.setClassName("hover-hint");
        hoverHint.setTextContent("Hover for controls...");
        controlPanel.appendChild(hoverHint);
    }
    
    private void createTitle(HTMLElement controlPanel, String titleText) {
        var title = (HTMLElement) document.createElement("h3");
        title.setTextContent(titleText);
        controlPanel.appendChild(title);
    }
    
    private HTMLElement createConfidenceLegend() {
        var legend = (HTMLElement) document.createElement("div");
        legend.setClassName("confidence-legend");
        
        var legendTitle = (HTMLElement) document.createElement("h4");
        legendTitle.setTextContent("Confidence Legend");
        legend.appendChild(legendTitle);
        
        // Create legend items using modern Java features
        var legendItems = List.of(
            new LegendItemConfig("legend-high", "High (≥80%)"),
            new LegendItemConfig("legend-med", "Medium (50-79%)"),
            new LegendItemConfig("legend-low", "Low (<50%)")
        );
        
        legendItems.stream()
            .map(this::createLegendItem)
            .forEach(legend::appendChild);
        
        return legend;
    }
    
    private record LegendItemConfig(String colorClass, String text) {}
    
    private HTMLElement createLegendItem(LegendItemConfig config) {
        var item = (HTMLElement) document.createElement("div");
        item.setClassName("legend-item");
        
        var color = (HTMLElement) document.createElement("div");
        color.setClassName("legend-color " + config.colorClass());
        
        var span = (HTMLElement) document.createElement("span");
        span.setTextContent(config.text());
        
        item.appendChild(color);
        item.appendChild(span);
        
        return item;
    }
    
    private HTMLElement createStatsContainer() {
        var stats = (HTMLElement) document.createElement("div");
        stats.setClassName("stats");
        
        var statsDiv = (HTMLElement) document.createElement("div");
        statsDiv.setId("ocr-stats");
        stats.appendChild(statsDiv);
        
        return stats;
    }
    
    private void updateStats() {
        var statsDiv = document.getElementById("ocr-stats");
        if (statsDiv == null || ocrData == null) return;
        
        var meta = ocrData.metadata();
        var statsHtml = "<div>" + meta.totalLines() + " lines, " + meta.totalWords() + " words</div>" +
                        "<div>Avg confidence: " + Math.round(meta.averageConfidence() * 1000.0) / 10.0 + "%</div>" +
                        "<div>Page angle: " + Math.round(meta.angle() * 10.0) / 10.0 + "°</div>";
        
        ((HTMLElement) statsDiv).setInnerHTML(statsHtml);
    }
    
    private HTMLElement createFloatingControls() {
        var controls = (HTMLElement) document.createElement("div");
        controls.setId("floating-controls");
        
        var controlsStyle = """
            position: absolute;
            background: rgba(0, 0, 0, 0.9);
            color: white;
            padding: 5px 10px;
            border-radius: 4px;
            font-size: 11px;
            z-index: 1000;
            white-space: nowrap;
            pointer-events: auto;
            """;
        controls.getStyle().setCssText(controlsStyle);
        
        return controls;
    }
    
    private void positionControlsWithinElement(HTMLElement controls, HTMLElement element, String type) {
        var rect = element.getBoundingClientRect();
        var scrollLeft = Window.current().getScreenX();
        var scrollTop = Window.current().getScreenY();
        
        switch (type) {
            case "line" -> {
                // Position copy button at right edge, centered vertically
                var x = rect.getRight() + scrollLeft - 25;
                var y = rect.getTop() + scrollTop + (rect.getHeight() / 2) - 10;
                
                controls.getStyle().setProperty("left", x + "px");
                controls.getStyle().setProperty("top", y + "px");
            }
            default -> {
                // Default positioning
                controls.getStyle().setProperty("left", "10px");
                controls.getStyle().setProperty("top", "10px");
            }
        }
    }
    
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
    
    private void removeElement(Element element) {
        if (element != null && element.getParentNode() != null) {
            element.getParentNode().removeChild(element);
        }
    }
    
    private void removeFromDocument(String query){
        var element = document.querySelector(query);
        if (element != null) {
            removeElement(element); //element.remove();
        }
    }
    
    private void removeAllFromDocument(String query){
        var elements = document.querySelectorAll(query);
        for (int i = 0; i < elements.getLength(); i++) {
            var element = elements.item(i);
            removeElement(element); //Element::remove
        }
    }
    
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
    
    private Element createSVGTextElement(WordData word, BoundingBox bbox, int lineId, int wordIndex) {
        var boxHeight = bbox.height();
        var fontSize = Math.max(8, Math.min(boxHeight * 0.7, 24));
        var textX = bbox.minX() + 2;
        var textY = bbox.minY() + (boxHeight * 0.75);
        
        var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttribute("x", String.valueOf(Math.round(textX*10)/10.0));
        text.setAttribute("y", String.valueOf(Math.round(textY*10)/10.0));
        text.setAttribute("class", "word-text");
        text.setAttribute("style", "font-size: " + Math.round(fontSize*10)/10.0 + "px;");
        text.setAttribute("title", "Confidence: " + Math.round(word.confidence() * 1000)/10.0 + "%");
        text.setTextContent(word.text());
        
        return text;
    }
    
    private void showSVGWordDetails(WordData word, MouseEvent event) {
        var popup = (HTMLElement) document.createElement("div");
        
        var popupStyle = """
            position: fixed;
            background: rgba(0, 0, 0, 0.9);
            color: white;
            padding: 10px 15px;
            border-radius: 5px;
            font-size: 12px;
            font-family: monospace;
            z-index: 1000;
            max-width: 250px;
            pointer-events: none;
            """;
        popup.getStyle().setCssText(popupStyle);
        
        var popupContent = word.boundingBox()
            .map(bbox -> "<div><strong>Word:</strong> \"" + word.text() + "\"</div>" +
                "<div><strong>Confidence:</strong> " + Math.round(word.confidence() * 1000.0) / 10.0 + "%</div>" +
                "<div><strong>Index:</strong> " + word.index() + "</div>" +
                "<div><strong>Bounds:</strong> " + Math.round(bbox.x1() * 10.0) / 10.0 + "," + Math.round(bbox.y1() * 10.0) / 10.0 + " to " + Math.round(bbox.x2() * 10.0) / 10.0 + "," + Math.round(bbox.y2() * 10.0) / 10.0 + "</div>")
            .orElse("<div><strong>Word:</strong> \"" + word.text() + "\"</div>" +
                "<div><strong>Confidence:</strong> " + Math.round(word.confidence() * 1000.0) / 10.0 + "%</div>" +
                "<div><strong>Index:</strong> " + word.index() + "</div>" +
                "<div><strong>Bounds:</strong> N/A</div>");
        
        popup.setInnerHTML(popupContent);
        
        popup.getStyle().setProperty("left", (event.getClientX() + 10) + "px");
        popup.getStyle().setProperty("top", (event.getClientY() + 10) + "px");
        
        document.getBody().appendChild(popup);
        
        // Auto-remove popup after 3 seconds
        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (popup.getParentNode() != null) {
                    removeElement(popup); //popup.remove();
                }
            }
        }, 3000);
    }
    
    private void showNotification(String message) {
        var notification = (HTMLElement) document.createElement("div");
        
        var notificationStyle = """
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0, 0, 0, 0.8);
            color: white;
            padding: 10px 20px;
            border-radius: 4px;
            z-index: 1000;
            font-size: 12px;
            """;
        notification.getStyle().setCssText(notificationStyle);
        notification.setTextContent(message);
        
        document.getBody().appendChild(notification);
        
        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (notification.getParentNode() != null) {
                    removeElement(notification); 
                    //notification.remove();
                }
            }
        }, 2000);
    }
    
    @JSBody(params = {"text"}, script = """
        navigator.clipboard.writeText(text).then(function() {
            console.log('Copied to clipboard!');
        }).catch(function(err) {
            console.error('Failed to copy text: ', err);
        });
        """)
    private static native void copyToClipboardSimple(String text);
    
    // Enhanced element querying with null safety
    private Optional<Element> queryOptional(String selector) {
        
        return Optional.ofNullable(document.querySelector(selector));
    }
    
    private List<HTMLElement> queryAll(String selector) {
        var nodeList = document.querySelectorAll(selector);
        return IntStream.range(0, nodeList.getLength())
            .mapToObj(i->(HTMLElement)nodeList.item(i))
            //.mapToObj(nodeList::get) //type issue
            .toList();
    }
}