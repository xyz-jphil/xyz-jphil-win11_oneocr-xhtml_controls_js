package xyz.jphil.win11_oneocr.xhtml_controls_js;

import java.util.List;
import java.util.Optional;

/**
 * Provides factory methods for creating OCR data structures.
 * Deep module that encapsulates default values and empty state creation.
 */
public final class OCRDataFactory {
    
    private OCRDataFactory() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Create empty OCR data with sensible defaults.
     * Used as fallback when no OCR section is found in the document.
     * Provides standard image dimensions and empty collections.
     */
    public static OCRData createEmptyOCRData() {
        return new OCRData(
            new Metadata("", 800, 600, 0.0, 0.0, 0, 0),
            List.of(),
            Optional.empty()
        );
    }
}