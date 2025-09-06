# Win11 OneOCR XHTML Controls JavaScript Generator

This project generates the interactive JavaScript controls for Win11 OneOCR Semantic XHTML5 documents. It uses TeaVM to transpile Java code to JavaScript, maintaining our pure Java development approach.

## Overview

This Maven project produces `XHtmlOcrControls.js` - the interactive JavaScript that powers OCR document visualization features like:
- Background image toggle
- Line and word bounding boxes
- Confidence-based color coding  
- Layout mode switching
- Keyboard shortcuts (Ctrl+1-5)

## Architecture

- **Input**: Java source code with TeaVM annotations
- **Build Tool**: Maven with TeaVM plugin
- **Output**: `target/generated/js/XHtmlOcrControls.js`
- **Deployment Target**: GitHub Pages CDN at `https://xyz-jphil.github.io/win11_oneocr_semantic_xhtml/`

## Build Requirements

- **JDK**: 11 or higher
- **Maven**: 3.6+
- **TeaVM**: Configured via Maven plugin
- **IDE**: NetBeans (recommended)

## Building

```bash
mvn clean compile
```

The generated JavaScript will be available at:
```
target/generated/js/XHtmlOcrControls.js
target/generated/js/XHtmlOcrControls.js.map
```

## Development Workflow

1. **Write Java Code**: Implement interactive features using TeaVM-compatible Java in NetBeans
2. **Build**: Run Maven to generate JavaScript
3. **Deploy**: Copy generated `XHtmlOcrControls.js` to the web repository for GitHub Pages hosting
4. **Test**: Verify functionality with XHTML OCR documents

## Pure Java Approach

This project maintains our commitment to pure Java development:
- **No Native Code**: FFM eliminated the need for C/C++ bindings
- **No Manual JavaScript**: TeaVM transpiles Java to browser-compatible JavaScript
- **No HTML Templating**: XHTML documents generated using Java DSL
- **Unified Skillset**: Pure Java team can contribute across the entire stack

## Why Java to JavaScript?

- **Type Safety**: Catch errors at compile time rather than runtime
- **IDE Support**: Full NetBeans support with code completion and refactoring
- **Team Consistency**: Pure Java team maintains productivity without context switching
- **Maintainability**: Structured OOP approach for complex interactive features

## Integration

The generated JavaScript is served via GitHub Pages CDN and dynamically loaded by OCR documents, ensuring:
- **Backward Compatibility**: Existing OCR documents continue to work
- **Independent Updates**: Controls can be upgraded without touching OCR documents
- **Clean Separation**: Document generation and presentation remain decoupled
- **Stable CDN URL**: Consistent access via GitHub Pages hosting

## Related Projects

- **Web Repository**: [win11_oneocr_semantic_xhtml](https://github.com/xyz-jphil/win11_oneocr_semantic_xhtml) - GitHub Pages hosting for generated JavaScript
- **Document Generator**: [xyz-jphil-win11_oneocr-tools](https://github.com/xyz-jphil/xyz-jphil-win11_oneocr-tools) - Creates XHTML documents
- **Core API**: [xyz-jphil-win11_oneocr-api](https://github.com/xyz-jphil/xyz-jphil-win11_oneocr-api) - Java FFM bindings for Windows OCR

---

*Part of the xyz-jphil Win11 OneOCR project - leveraging Windows 11's built-in OCR for Java applications.*