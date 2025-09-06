package xyz.jphil.win11_oneocr.xhtml_controls_js;

import java.util.List;
import java.util.Optional;

public record LineData(
    int id,
    Optional<BoundingBox> boundingBox,
    List<WordData> words
) {}