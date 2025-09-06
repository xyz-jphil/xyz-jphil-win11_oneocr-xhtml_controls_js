package xyz.jphil.win11_oneocr.xhtml_controls_js.utilities;

import org.teavm.jso.JSBody;
import xyz.jphil.win11_oneocr.xhtml_controls_js.LineData;
import xyz.jphil.win11_oneocr.xhtml_controls_js.WordData;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides text extraction and clipboard operations.
 * Deep module that handles text joining and clipboard integration complexity.
 */
public final class TextUtilities {
    
    private TextUtilities() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Extract text from a single line of OCR data.
     * Joins all word texts with spaces for natural reading.
     */
    public static String extractLineText(LineData line) {
        return line.words().stream()
            .map(WordData::text)
            .collect(Collectors.joining(" "));
    }
    
    /**
     * Extract text from multiple lines of OCR data.
     * Each line becomes a separate line in the output text.
     */
    public static String extractPageText(List<LineData> lines) {
        return lines.stream()
            .map(TextUtilities::extractLineText)
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * Copy text to system clipboard and show notification.
     * Combines clipboard operation with user feedback.
     */
    public static void copyTextWithNotification(String text) {
        copyToClipboardSimple(text);
        NotificationUtilities.showNotification("Copied to clipboard!");
    }
    
    /**
     * Copy line text to clipboard with notification.
     * Convenience method for single line operations.
     */
    public static void copyLineTextWithNotification(LineData line) {
        var text = extractLineText(line);
        copyTextWithNotification(text);
    }
    
    /**
     * Copy page text to clipboard with notification.
     * Convenience method for multi-line operations.
     */
    public static void copyPageTextWithNotification(List<LineData> lines) {
        var text = extractPageText(lines);
        copyTextWithNotification(text);
    }
    
    /**
     * Copy text to system clipboard using browser API.
     * Uses navigator.clipboard.writeText() for modern browser compatibility.
     */
    @JSBody(params = {"text"}, script = """
        navigator.clipboard.writeText(text).then(function() {
            console.log('Copied to clipboard!');
        }).catch(function(err) {
            console.error('Failed to copy text: ', err);
        });
        """)
    private static native void copyToClipboardSimple(String text);
}