package xyz.jphil.win11_oneocr.xhtml_controls_js;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Provides notification and UI popup utilities.
 * Deep module that handles popup positioning, styling, and auto-removal complexity.
 */
public final class NotificationUtilities {
    
    private NotificationUtilities() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Show temporary notification message in center of screen.
     * Auto-removes after 2 seconds with fade styling.
     */
    public static void showNotification(String message) {
        var notification = (HTMLElement) getDocument().createElement("div");
        
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
        
        getDocument().getBody().appendChild(notification);
        
        // Auto-remove after 2 seconds
        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (notification.getParentNode() != null) {
                    DomUtilities.removeElement(notification);
                }
            }
        }, 2000);
    }
    
    /**
     * Show detailed word information popup near mouse cursor.
     * Displays word text, confidence, index, and bounding box coordinates.
     * Auto-removes after 3 seconds.
     */
    public static void showSVGWordDetails(WordData word, MouseEvent event) {
        var popup = (HTMLElement) getDocument().createElement("div");
        
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
        
        getDocument().getBody().appendChild(popup);
        
        // Auto-remove popup after 3 seconds
        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (popup.getParentNode() != null) {
                    DomUtilities.removeElement(popup);
                }
            }
        }, 3000);
    }
    
    /**
     * Position control element within or relative to target element.
     * Handles different positioning strategies based on type parameter.
     */
    public static void positionControlsWithinElement(HTMLElement controls, HTMLElement element, String type) {
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
    
    /**
     * Get global document instance - convenience method.
     */
    private static HTMLDocument getDocument() {
        return Window.current().getDocument();
    }
}