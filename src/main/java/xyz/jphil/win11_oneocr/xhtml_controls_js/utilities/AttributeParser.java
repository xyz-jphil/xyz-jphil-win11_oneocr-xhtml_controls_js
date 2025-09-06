package xyz.jphil.win11_oneocr.xhtml_controls_js.utilities;

import org.teavm.jso.dom.xml.Element;
import xyz.jphil.win11_oneocr.xhtml_controls_js.BoundingBox;

import java.util.Arrays;
import java.util.Optional;

/**
 * Provides utilities for parsing XML/HTML attributes to Java types.
 * Deep module that handles type conversion and error handling internally.
 */
public final class AttributeParser {
    
    private AttributeParser() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Parse integer attribute with default fallback.
     * Handles null values and NumberFormatException gracefully.
     */
    public static int parseIntAttribute(Element element, String attr, int defaultValue) {
        try {
            var value = element.getAttribute(attr);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse double attribute with default fallback.
     * Handles null values and NumberFormatException gracefully.
     */
    public static double parseDoubleAttribute(Element element, String attr, double defaultValue) {
        try {
            var value = element.getAttribute(attr);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse bounding box from comma-separated coordinate string.
     * Expects format: "x1,y1,x2,y2,x3,y3,x4,y4" (8 coordinates for polygon).
     * Returns empty Optional if parsing fails or insufficient coordinates.
     */
    public static Optional<BoundingBox> parseBoundingBox(String boundingBoxStr) {
        if (boundingBoxStr == null || boundingBoxStr.isBlank()) {
            return Optional.empty();
        }
        
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
}