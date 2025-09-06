package xyz.jphil.win11_oneocr.xhtml_controls_js.utilities;

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
     * Create sticky top control bar with pin/unpin toggle functionality.
     * When pinned: position sticky, stays at top while scrolling.
     * When unpinned: position static, scrolls with content.
     */
    public static HTMLElement createStickyControlBar(List<ControlConfig> controls) {
        var controlBar = (HTMLElement) getDocument().createElement("div");
        controlBar.setClassName("control-bar sticky-pinned");
        controlBar.setId("top-control-bar");
        
        // Sticky control bar styles
        var barStyle = "position: sticky; " +
                      "top: 0; " +
                      "z-index: 1000; " +
                      "background: linear-gradient(135deg, #2c3e50, #34495e); " +
                      "color: white; " +
                      "padding: 8px 16px; " +
                      "border-bottom: 2px solid #3498db; " +
                      "box-shadow: 0 2px 8px rgba(0,0,0,0.3); " +
                      "display: flex; " +
                      "flex-wrap: wrap; " +
                      "align-items: center; " +
                      "gap: 12px; " +
                      "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                      "font-size: 13px; " +
                      "transition: all 0.3s ease;";
        controlBar.getStyle().setCssText(barStyle);
        
        // Create pin/unpin toggle button
        var pinToggle = createPinToggleButton();
        controlBar.appendChild(pinToggle);
        
        // Add separator
        var separator = (HTMLElement) getDocument().createElement("div");
        separator.getStyle().setCssText("width: 1px; height: 20px; background: rgba(255,255,255,0.3); margin: 0 4px;");
        controlBar.appendChild(separator);
        
        // Add control groups in responsive layout
        controls.forEach(config -> {
            var controlGroup = createCompactControlGroup(config);
            controlBar.appendChild(controlGroup);
        });
        
        return controlBar;
    }
    
    /**
     * Create pin/unpin toggle button for sticky control bar.
     */
    private static HTMLElement createPinToggleButton() {
        var pinButton = (HTMLElement) getDocument().createElement("button");
        pinButton.setId("pin-toggle-btn");
        pinButton.setInnerHTML("📌"); // Pin icon
        pinButton.setTitle("Click to unpin (scroll with content)");
        
        var buttonStyle = "background: rgba(52, 152, 219, 0.2); " +
                         "border: 1px solid rgba(52, 152, 219, 0.5); " +
                         "color: #3498db; " +
                         "padding: 4px 8px; " +
                         "border-radius: 4px; " +
                         "cursor: pointer; " +
                         "font-size: 14px; " +
                         "transition: all 0.2s ease;";
        pinButton.getStyle().setCssText(buttonStyle);
        
        // Add pin/unpin toggle functionality
        pinButton.addEventListener("click", evt -> toggleControlBarPin());
        
        // Hover effects
        pinButton.addEventListener("mouseenter", evt -> 
            pinButton.getStyle().setProperty("background", "rgba(52, 152, 219, 0.4)"));
        pinButton.addEventListener("mouseleave", evt -> 
            pinButton.getStyle().setProperty("background", "rgba(52, 152, 219, 0.2)"));
        
        return pinButton;
    }
    
    /**
     * Toggle between pinned (sticky) and unpinned (static) control bar.
     */
    private static void toggleControlBarPin() {
        var controlBar = getDocument().getElementById("top-control-bar");
        var pinButton = getDocument().getElementById("pin-toggle-btn");
        
        if (controlBar != null && pinButton != null) {
            if (controlBar.getClassList().contains("sticky-pinned")) {
                // Unpin: make it scroll with content
                controlBar.getClassList().remove("sticky-pinned");
                controlBar.getClassList().add("sticky-unpinned");
                controlBar.getStyle().setProperty("position", "static");
                pinButton.setInnerHTML("📍"); // Different icon for unpinned
                pinButton.setTitle("Click to pin (stay at top while scrolling)");
                pinButton.getStyle().setProperty("color", "#e74c3c");
            } else {
                // Pin: make it stick to top
                controlBar.getClassList().remove("sticky-unpinned");
                controlBar.getClassList().add("sticky-pinned");
                controlBar.getStyle().setProperty("position", "sticky");
                pinButton.setInnerHTML("📌"); // Pin icon
                pinButton.setTitle("Click to unpin (scroll with content)");
                pinButton.getStyle().setProperty("color", "#3498db");
            }
        }
    }
    
    /**
     * Create compact control group for horizontal layout in control bar.
     */
    private static HTMLElement createCompactControlGroup(ControlConfig config) {
        var group = (HTMLElement) getDocument().createElement("label");
        group.setAttribute("for", config.id());
        group.setClassName("compact-control-group");
        
        var groupStyle = "display: flex; " +
                        "align-items: center; " +
                        "gap: 6px; " +
                        "padding: 2px 6px; " +
                        "border-radius: 4px; " +
                        "background: rgba(255,255,255,0.1); " +
                        "transition: background 0.2s ease; " +
                        "cursor: pointer; " +
                        "user-select: none;";
        group.getStyle().setCssText(groupStyle);
        
        var labelText = (HTMLElement) getDocument().createElement("span");
        labelText.setTextContent(config.label());
        labelText.getStyle().setCssText("font-size: 12px;");
        
        var toggle = createCompactToggleSwitch(config.id(), config.defaultChecked());
        
        group.appendChild(labelText);
        group.appendChild(toggle);
        
        // Hover effect
        group.addEventListener("mouseenter", evt -> 
            group.getStyle().setProperty("background", "rgba(255,255,255,0.15)"));
        group.addEventListener("mouseleave", evt -> 
            group.getStyle().setProperty("background", "rgba(255,255,255,0.1)"));
        
        return group;
    }
    
    /**
     * Create compact toggle switch for horizontal control bar layout.
     */
    private static HTMLElement createCompactToggleSwitch(String id, boolean checked) {
        var toggleSwitch = (HTMLElement) getDocument().createElement("div");
        toggleSwitch.setClassName("compact-toggle-switch");
        
        var switchStyle = "position: relative; " +
                         "width: 32px; " +
                         "height: 16px; " +
                         "background: rgba(255,255,255,0.2); " +
                         "border-radius: 16px; " +
                         "transition: background 0.3s ease; " +
                         "cursor: pointer;";
        toggleSwitch.getStyle().setCssText(switchStyle);
        
        var input = (HTMLInputElement) getDocument().createElement("input");
        input.setType("checkbox");
        input.setId(id);
        input.setChecked(checked);
        input.getStyle().setCssText("position: absolute; opacity: 0; pointer-events: none;");
        
        var slider = (HTMLElement) getDocument().createElement("span");
        slider.setClassName("compact-slider");
        var translateX = checked ? "16px" : "0px";
        var sliderStyle = "position: absolute; " +
                         "top: 2px; " +
                         "left: 2px; " +
                         "width: 12px; " +
                         "height: 12px; " +
                         "background: white; " +
                         "border-radius: 50%; " +
                         "transition: transform 0.3s ease; " +
                         "transform: translateX(" + translateX + ");";
        slider.getStyle().setCssText(sliderStyle);
        
        // Update styles based on checked state
        if (checked) {
            toggleSwitch.getStyle().setProperty("background", "#3498db");
        }
        
        toggleSwitch.appendChild(input);
        toggleSwitch.appendChild(slider);
        
        // Add change event listener to update visual state when input changes
        input.addEventListener("change", evt -> {
            boolean isChecked = input.isChecked();
            if (isChecked) {
                toggleSwitch.getStyle().setProperty("background", "#3498db");
                slider.getStyle().setProperty("transform", "translateX(16px)");
            } else {
                toggleSwitch.getStyle().setProperty("background", "rgba(255,255,255,0.2)");
                slider.getStyle().setProperty("transform", "translateX(0px)");
            }
        });
        
        return toggleSwitch;
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
        
        var controlsStyle = "position: absolute; " +
                           "background: rgba(0, 0, 0, 0.9); " +
                           "color: white; " +
                           "padding: 5px 10px; " +
                           "border-radius: 4px; " +
                           "font-size: 11px; " +
                           "z-index: 1000; " +
                           "white-space: nowrap; " +
                           "pointer-events: auto;";
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