package xyz.jphil.win11_oneocr.xhtml_controls_js;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import java.util.List;

/**
 * Creates HTML UI elements with styling.
 * Deep module that handles DOM element creation and styling complexity.
 */
public final class UIElementFactory {
    
    private UIElementFactory() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Record for control configuration data.
     */
    public record ControlConfig(String id, String label, boolean defaultChecked) {}
    
    /**
     * Record for legend item configuration data.
     */
    public record LegendItemConfig(String colorClass, String text) {}
    
    /**
     * Create hover hint element with standard styling.
     */
    public static void createHoverHint(HTMLElement parent) {
        var hoverHint = (HTMLElement) getDocument().createElement("div");
        hoverHint.setClassName("hover-hint");
        hoverHint.setTextContent("Hover for controls...");
        parent.appendChild(hoverHint);
    }
    
    /**
     * Create title element with standard styling.
     */
    public static void createTitle(HTMLElement parent, String titleText) {
        var title = (HTMLElement) getDocument().createElement("h3");
        title.setTextContent(titleText);
        parent.appendChild(title);
    }
    
    /**
     * Create confidence legend with standard confidence levels.
     * Returns fully configured legend element with high/medium/low confidence indicators.
     */
    public static HTMLElement createConfidenceLegend() {
        var legend = (HTMLElement) getDocument().createElement("div");
        legend.setClassName("confidence-legend");
        
        var legendTitle = (HTMLElement) getDocument().createElement("h4");
        legendTitle.setTextContent("Confidence Legend");
        legend.appendChild(legendTitle);
        
        // Create legend items using modern Java features
        var legendItems = List.of(
            new LegendItemConfig("legend-high", "High (≥80%)"),
            new LegendItemConfig("legend-med", "Medium (50-79%)"),
            new LegendItemConfig("legend-low", "Low (<50%)")
        );
        
        legendItems.stream()
            .map(UIElementFactory::createLegendItem)
            .forEach(legend::appendChild);
        
        return legend;
    }
    
    /**
     * Create individual legend item with color indicator.
     */
    public static HTMLElement createLegendItem(LegendItemConfig config) {
        var item = (HTMLElement) getDocument().createElement("div");
        item.setClassName("legend-item");
        
        var color = (HTMLElement) getDocument().createElement("div");
        color.setClassName("legend-color " + config.colorClass());
        
        var span = (HTMLElement) getDocument().createElement("span");
        span.setTextContent(config.text());
        
        item.appendChild(color);
        item.appendChild(span);
        
        return item;
    }
    
    /**
     * Create stats container for OCR statistics display.
     */
    public static HTMLElement createStatsContainer() {
        var stats = (HTMLElement) getDocument().createElement("div");
        stats.setClassName("stats");
        
        var statsDiv = (HTMLElement) getDocument().createElement("div");
        statsDiv.setId("ocr-stats");
        stats.appendChild(statsDiv);
        
        return stats;
    }
    
    /**
     * Create floating controls container with standard styling.
     */
    public static HTMLElement createFloatingControls() {
        var controls = (HTMLElement) getDocument().createElement("div");
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
    
    /**
     * Create control group with label and toggle switch.
     */
    public static HTMLElement createControlGroup(ControlConfig config) {
        var group = (HTMLElement) getDocument().createElement("div");
        group.setClassName("control-group");
        
        var label = (HTMLElement) getDocument().createElement("label");
        label.setAttribute("for", config.id());
        label.setTextContent(config.label());
        
        var toggleSwitch = createToggleSwitch(config.id(), config.defaultChecked());
        
        group.appendChild(label);
        group.appendChild(toggleSwitch);
        
        return group;
    }
    
    /**
     * Create toggle switch with checkbox input and slider styling.
     */
    public static HTMLElement createToggleSwitch(String id, boolean checked) {
        var toggleSwitch = (HTMLElement) getDocument().createElement("div");
        toggleSwitch.setClassName("toggle-switch");
        
        var input = (HTMLInputElement) getDocument().createElement("input");
        input.setType("checkbox");
        input.setId(id);
        input.setChecked(checked);
        
        var slider = (HTMLElement) getDocument().createElement("span");
        slider.setClassName("slider");
        
        toggleSwitch.appendChild(input);
        toggleSwitch.appendChild(slider);
        
        return toggleSwitch;
    }
    
    /**
     * Get global document instance - convenience method.
     */
    private static HTMLDocument getDocument() {
        return Window.current().getDocument();
    }
}