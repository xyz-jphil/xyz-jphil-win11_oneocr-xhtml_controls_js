// OCR XHTML Interactive Controls - Virtual DOM Approach
(function() {
    'use strict';
    
    // Configuration
    const CONFIG = {
        confidenceThresholds: {
            high: 0.8,
            med: 0.5
        }
    };
    
    // Virtual DOM - OCR Data Model
    let ocrData = {
        metadata: {
            filename: '',
            imageWidth: 0,
            imageHeight: 0,
            angle: 0,
            averageConfidence: 0,
            totalWords: 0,
            totalLines: 0
        },
        lines: [], // Each line contains: {id, boundingBox, words: [{text, confidence, index, boundingBox}]}
        backgroundImagePath: null
    };
    
    // State management
    const state = {
        showLineBoxes: false,
        showWordBoxes: true,        // Show SVG word boxes by default
        showXHTMLText: true,        // Show XHTML text by default
        showSVGText: false,         // Hide SVG text by default
        enableHoverControls: true,
        showSVGSection: true,
        showSVGBackground: true,    // Show SVG background image by default
        initialized: false
    };
    
    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initializeOCRViewer();
    });
    
    function initializeOCRViewer() {
        if (state.initialized) return;
        
        // Build virtual DOM from XHTML
        buildVirtualDOMFromXHTML();
        
        // Clean up existing DOM and rebuild
        cleanupDOM();
        createControlPanel();
        setupHTMLSection();
        createSVGSection();
        bindEventHandlers();
        updateDisplay();
        
        state.initialized = true;
        console.log('OCR XHTML Viewer initialized with Virtual DOM');
        
        // Debug: Export 3rd line SVG elements to console for comparison
        setTimeout(() => exportLineSVGToConsole(2), 1000);
    }
    
    /**
     * Build virtual DOM data structure from XHTML elements
     */
    function buildVirtualDOMFromXHTML() {
        const section = document.querySelector('section.win11OneOcrPage');
        if (!section) return;
        
        // Extract metadata
        ocrData.metadata = {
            filename: section.getAttribute('srcName') || 'Unknown',
            imageWidth: parseInt(section.getAttribute('imgWidth')) || 800,
            imageHeight: parseInt(section.getAttribute('imgHeight')) || 600,
            angle: parseFloat(section.getAttribute('angle')) || 0,
            averageConfidence: parseFloat(section.getAttribute('averageOcrConfidence')) || 0,
            totalWords: parseInt(section.getAttribute('ocrWordsCount')) || 0,
            totalLines: parseInt(section.getAttribute('ocrSegmentsCount')) || 0
        };
        
        ocrData.backgroundImagePath = ocrData.metadata.filename;
        
        // Extract lines and words
        ocrData.lines = [];
        const segments = document.querySelectorAll('segment');
        
        segments.forEach((segment, lineIndex) => {
            const lineData = {
                id: lineIndex,
                boundingBox: null, // segments don't have bounding boxes in our format
                words: []
            };
            
            const words = segment.querySelectorAll('w');
            words.forEach((word, wordIndex) => {
                const boundingBox = word.getAttribute('b');
                const wordData = {
                    text: word.textContent.trim(),
                    confidence: parseFloat(word.getAttribute('p')) || 0,
                    index: parseInt(word.getAttribute('i')) || wordIndex,
                    boundingBox: boundingBox ? parseBoundingBox(boundingBox) : null
                };
                lineData.words.push(wordData);
            });
            
            ocrData.lines.push(lineData);
        });
        
        console.log('Virtual DOM built:', ocrData);
        console.log(`Image dimensions: ${ocrData.metadata.imageWidth} x ${ocrData.metadata.imageHeight}`);
    }
    
    /**
     * Parse bounding box string "x1,y1,x2,y2" into object with validation
     */
    function parseBoundingBox(boundingBoxStr) {
        const coords = boundingBoxStr.split(',').map(parseFloat);
        if (coords.length >= 8) {
            // Use all 8 coordinates for 4-point polygon (x1,y1,x2,y2,x3,y3,x4,y4)
            const result = {
                x1: coords[0],
                y1: coords[1],
                x2: coords[2], 
                y2: coords[3],
                x3: coords[4],
                y3: coords[5],
                x4: coords[6],
                y4: coords[7]
            };
            
            return result;
        }
        return null;
    }
    
    /**
     * Check if element is or is contained within an element with given ID
     * (Compatible replacement for closest())
     */
    function isElementOrChild(element, parentId) {
        if (!element) return false;
        
        // Check current element
        if (element.id === parentId) return true;
        
        // Check parents up the tree
        let current = element.parentElement;
        while (current) {
            if (current.id === parentId) return true;
            current = current.parentElement;
        }
        
        return false;
    }
    
    /**
     * Clean up existing hover controls and other dynamically added elements
     */
    function cleanupDOM() {
        // Remove existing control panel
        const existingPanel = document.querySelector('.control-panel');
        if (existingPanel) existingPanel.remove();
        
        // Remove existing hover controls
        document.querySelectorAll('.copy-controls, .info-popup').forEach(el => el.remove());
        
        // Remove existing SVG section
        const existingSVG = document.querySelector('.svg-content');
        if (existingSVG) existingSVG.remove();
        
        // Remove page copy button
        document.querySelectorAll('section.win11OneOcrPage button').forEach(el => el.remove());
        
        // Remove background image elements
        document.querySelectorAll('.background-image').forEach(el => el.remove());
    }
    
    /**
     * Create control panel using virtual DOM data
     */
    function createControlPanel() {
        const controlPanel = document.createElement('div');
        controlPanel.className = 'control-panel';
        
        // Hover hint
        const hoverHint = document.createElement('div');
        hoverHint.className = 'hover-hint';
        hoverHint.textContent = 'Hover for controls...';
        controlPanel.appendChild(hoverHint);
        
        // Title
        const title = document.createElement('h3');
        title.textContent = 'OCR Display Controls';
        controlPanel.appendChild(title);
        
        // Line Boxes Control
        controlPanel.appendChild(createControlGroup('toggle-line-boxes', 'Line Boxes', false));
        
        // Word Boxes Control
        controlPanel.appendChild(createControlGroup('toggle-word-boxes', 'Word Boxes', true));
        
        // XHTML Text Content Control
        controlPanel.appendChild(createControlGroup('toggle-xhtml-text', 'Text Content (XHTML)', true));
        
        // SVG Text Content Control
        controlPanel.appendChild(createControlGroup('toggle-svg-text', 'Text Content (SVG)', false));
        
        // Hover Controls Toggle
        controlPanel.appendChild(createControlGroup('toggle-hover-controls', 'Hover Controls', true));
        
        // SVG Section Toggle
        controlPanel.appendChild(createControlGroup('toggle-svg-section', 'SVG Section', true));
        
        // SVG Background Toggle
        controlPanel.appendChild(createControlGroup('toggle-svg-background', 'SVG Background', true));
        
        // Confidence Legend
        const legend = document.createElement('div');
        legend.className = 'confidence-legend';
        
        const legendTitle = document.createElement('h4');
        legendTitle.textContent = 'Confidence Legend';
        legend.appendChild(legendTitle);
        
        legend.appendChild(createLegendItem('legend-high', 'High (≥80%)'));
        legend.appendChild(createLegendItem('legend-med', 'Medium (50-79%)'));
        legend.appendChild(createLegendItem('legend-low', 'Low (<50%)'));
        
        controlPanel.appendChild(legend);
        
        // Stats container
        const stats = document.createElement('div');
        stats.className = 'stats';
        const statsDiv = document.createElement('div');
        statsDiv.id = 'ocr-stats';
        stats.appendChild(statsDiv);
        controlPanel.appendChild(stats);
        
        document.body.appendChild(controlPanel);
        
        // Update stats
        updateStats();
    }
    
    function createControlGroup(id, label, checked) {
        const group = document.createElement('div');
        group.className = 'control-group';
        
        const labelEl = document.createElement('label');
        labelEl.setAttribute('for', id);
        labelEl.textContent = label;
        
        const toggleSwitch = document.createElement('div');
        toggleSwitch.className = 'toggle-switch';
        
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.id = id;
        if (checked) input.checked = true;
        
        const slider = document.createElement('span');
        slider.className = 'slider';
        
        toggleSwitch.appendChild(input);
        toggleSwitch.appendChild(slider);
        
        group.appendChild(labelEl);
        group.appendChild(toggleSwitch);
        
        return group;
    }
    
    function createLegendItem(colorClass, text) {
        const item = document.createElement('div');
        item.className = 'legend-item';
        
        const color = document.createElement('div');
        color.className = 'legend-color ' + colorClass;
        
        const span = document.createElement('span');
        span.textContent = text;
        
        item.appendChild(color);
        item.appendChild(span);
        
        return item;
    }
    
    /**
     * Setup HTML section with clean hover controls
     */
    function setupHTMLSection() {
        const section = document.querySelector('section.win11OneOcrPage');
        if (!section) return;
        
        // Apply confidence classes to words and line numbers to segments
        ocrData.lines.forEach((line, lineIndex) => {
            // Add line number to segment
            const segmentElement = document.querySelector(`segment:nth-child(${lineIndex + 1})`);
            if (segmentElement) {
                segmentElement.setAttribute('data-line-number', lineIndex + 1);
            }
            
            line.words.forEach((wordData, wordIndex) => {
                const wordElement = document.querySelector(`segment:nth-child(${lineIndex + 1}) w:nth-child(${wordIndex + 1})`);
                if (wordElement) {
                    // Clear existing classes
                    wordElement.className = '';
                    
                    // Add confidence class
                    if (wordData.confidence >= CONFIG.confidenceThresholds.high) {
                        wordElement.classList.add('confidence-high');
                    } else if (wordData.confidence >= CONFIG.confidenceThresholds.med) {
                        wordElement.classList.add('confidence-med');
                    } else {
                        wordElement.classList.add('confidence-low');
                    }
                }
            });
        });
        
        // Add clean hover controls
        addCleanHoverControls();
        
        // Add copy page button
        addPageCopyButton();
    }
    
    /**
     * Add simplified hover controls - only segment-level copy
     */
    function addCleanHoverControls() {
        const section = document.querySelector('section.win11OneOcrPage');
        if (!section) return;
        
        let hideTimeout = null;
        
        // Add controls to lines (segments) only
        const segments = document.querySelectorAll('segment');
        segments.forEach((segment, lineIndex) => {
            segment.addEventListener('mouseenter', (e) => {
                if (!state.enableHoverControls) return;
                clearTimeout(hideTimeout);
                showLineControls(e, lineIndex, segment);
            });
            
            segment.addEventListener('mouseleave', (e) => {
                // Delay hiding to allow moving to the control
                hideTimeout = setTimeout(() => {
                    const controls = document.getElementById('floating-controls');
                    if (controls && e.relatedTarget && !isElementOrChild(e.relatedTarget, 'floating-controls')) {
                        hideControls();
                    } else if (!controls) {
                        hideControls();
                    }
                }, 100);
            });
        });
        
        // Add global listener to handle controls hover
        document.addEventListener('mouseover', (e) => {
            if (isElementOrChild(e.target, 'floating-controls')) {
                clearTimeout(hideTimeout);
            }
        });
        
        document.addEventListener('mouseleave', (e) => {
            if (isElementOrChild(e.target, 'floating-controls')) {
                hideTimeout = setTimeout(hideControls, 100);
            }
        });
    }
    
    /**
     * Show line controls with copy icon - positioned on right side
     */
    function showLineControls(event, lineIndex, segmentElement) {
        hideControls(); // Clear existing
        
        const lineData = ocrData.lines[lineIndex];
        if (!lineData) return;
        
        const controls = createFloatingControls();
        controls.style.cssText = `
            position: absolute;
            background: rgba(0, 0, 0, 0.8);
            color: white;
            padding: 3px 6px;
            border-radius: 3px;
            font-size: 10px;
            z-index: 1000;
            white-space: nowrap;
            pointer-events: auto;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.2);
        `;
        
        // Create copy button with copy icon
        const copyButton = document.createElement('button');
        copyButton.innerHTML = '⧉'; // Copy icon
        copyButton.title = `Copy line ${lineIndex + 1}`;
        copyButton.style.cssText = `
            background: none;
            border: none;
            color: white;
            cursor: pointer;
            font-size: 12px;
            padding: 2px 4px;
            border-radius: 2px;
        `;
        copyButton.onclick = () => copyLineText(lineIndex);
        copyButton.onmouseenter = () => copyButton.style.background = 'rgba(255, 255, 255, 0.2)';
        copyButton.onmouseleave = () => copyButton.style.background = 'none';
        
        controls.appendChild(copyButton);
        
        positionControlsWithinElement(controls, segmentElement, 'line');
        document.body.appendChild(controls);
    }
    
    /**
     * Create floating controls element
     */
    function createFloatingControls() {
        const controls = document.createElement('div');
        controls.id = 'floating-controls';
        controls.style.cssText = `
            position: absolute;
            background: rgba(0, 0, 0, 0.9);
            color: white;
            padding: 5px 10px;
            border-radius: 4px;
            font-size: 11px;
            z-index: 1000;
            white-space: nowrap;
            pointer-events: auto;
        `;
        return controls;
    }
    
    /**
     * Position floating controls within element boundaries
     */
    function positionControlsWithinElement(controls, element, type) {
        const rect = element.getBoundingClientRect();
        const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (type === 'line') {
            // For lines, position copy button at the right edge, centered vertically
            const x = rect.right + scrollLeft - 25; // Small offset from right edge
            const y = rect.top + scrollTop + (rect.height / 2) - 10; // Center vertically
            
            controls.style.left = x + 'px';
            controls.style.top = y + 'px';
        }
    }
    
    /**
     * Hide all floating controls
     */
    function hideControls() {
        const existing = document.getElementById('floating-controls');
        if (existing) existing.remove();
    }
    
    /**
     * Add page copy button
     */
    function addPageCopyButton() {
        const section = document.querySelector('section.win11OneOcrPage');
        if (!section) return;
        
        const copyBtn = document.createElement('button');
        copyBtn.textContent = 'Copy Page';
        copyBtn.style.cssText = `
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
        `;
        copyBtn.onclick = copyPageText;
        
        section.appendChild(copyBtn);
    }
    
    /**
     * Copy functions using virtual DOM - clean text only
     */
    function copyLineText(lineIndex) {
        const lineData = ocrData.lines[lineIndex];
        if (!lineData) return;
        
        const text = lineData.words.map(word => word.text).join(' ');
        copyToClipboard(text);
    }
    
    function copyPageText() {
        const allText = ocrData.lines
            .map(line => line.words.map(word => word.text).join(' '))
            .join('\n');  // Use actual newline character, not escaped string
        copyToClipboard(allText);
    }
    
    function copyToClipboard(text) {
        navigator.clipboard.writeText(text).then(() => {
            showNotification('Copied to clipboard!');
        }).catch(err => {
            console.error('Failed to copy text: ', err);
            showNotification('Copy failed - see console');
        });
    }
    
    function showNotification(message) {
        const notification = document.createElement('div');
        notification.style.cssText = `
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
        `;
        notification.textContent = message;
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 2000);
    }
    
    /**
     * Create SVG section using virtual DOM data - following Java implementation
     */
    function createSVGSection() {
        const section = document.querySelector('section.win11OneOcrPage');
        const ocrContent = document.querySelector('.ocrContent');
        if (!section || !ocrContent) return;
        
        // Create SVG container - responsive
        const svgContainer = document.createElement('div');
        svgContainer.className = 'svg-content';
        svgContainer.style.cssText = `
            position: relative;
            margin-top: 20px;
            border-top: 2px solid #ddd;
            padding: 20px;
            background: #fafafa;
            overflow: auto;
            max-width: 100%;
        `;
        
        // Add title
        const title = document.createElement('h3');
        title.textContent = 'Precise SVG Layout';
        title.style.cssText = 'margin: 0 0 15px 0; color: #666; font-size: 14px;';
        //svgContainer.appendChild(title); // disabled for now
        
        // Generate SVG from virtual DOM
        const svgElement = generateSVGFromVirtualDOM();
        svgContainer.appendChild(svgElement);
        
        // Add after ocrContent
        ocrContent.parentNode.insertBefore(svgContainer, ocrContent.nextSibling);
    }
    
    /**
     * Generate SVG from virtual DOM - following Java SvgVisualizer approach
     */
    function generateSVGFromVirtualDOM() {
        // Use exact image dimensions from metadata (matching Java implementation)
        const svgWidth = ocrData.metadata.imageWidth || 800;
        const svgHeight = ocrData.metadata.imageHeight || 600;
        
        // Create responsive SVG (matching Java implementation structure)
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('width', svgWidth);
        svg.setAttribute('height', svgHeight);
        svg.setAttribute('viewBox', `0 0 ${svgWidth} ${svgHeight}`);
        svg.style.cssText = `
            border: 1px solid #ccc;
            background: white;
            width: 100%;
            height: auto;
        `;
        
        // Add styles (matching Java implementation)
        const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
        const style = document.createElementNS('http://www.w3.org/2000/svg', 'style');
        style.textContent = `
            .line-box { fill: none; stroke: #000000; stroke-width: 0.8; stroke-dasharray: 4,2; }
            .word-box-high { fill: none; stroke: #00aa00; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-box-med { fill: none; stroke: #ffaa00; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-box-low { fill: none; stroke: #ff0000; stroke-width: 0.6; stroke-dasharray: 2,1; }
            .word-text { font-family: Arial, sans-serif; font-size: 12px; fill: #0066cc; font-weight: bold; }
            .svg-layer { display: block; }
            .svg-layer.hidden { display: none; }
        `;
        defs.appendChild(style);
        svg.appendChild(defs);
        
        // Background image layer (matching Java implementation)
        if (ocrData.backgroundImagePath) {
            const bgGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            bgGroup.setAttribute('id', 'svg-background-layer');
            bgGroup.setAttribute('class', 'svg-layer hidden');
            
            const bgImage = document.createElementNS('http://www.w3.org/2000/svg', 'image');
            bgImage.setAttribute('href', ocrData.backgroundImagePath);
            bgImage.setAttribute('x', '0');
            bgImage.setAttribute('y', '0');
            bgImage.setAttribute('width', svgWidth);
            bgImage.setAttribute('height', svgHeight);
            bgImage.setAttribute('preserveAspectRatio', 'none'); // Matching Java: preserveAspectRatio="none"
            bgImage.setAttribute('opacity', '1.0'); // Full opacity initially, can be controlled
            
            bgGroup.appendChild(bgImage);
            svg.appendChild(bgGroup);
        }
        
        // Line boxes layer (matching Java sample)
        const lineBoxGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        lineBoxGroup.setAttribute('id', 'svg-line-boxes');
        lineBoxGroup.setAttribute('class', 'svg-layer hidden');
        
        // Word boxes layer
        const wordBoxGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        wordBoxGroup.setAttribute('id', 'svg-word-boxes');
        wordBoxGroup.setAttribute('class', 'svg-layer hidden');
        
        // Text layer
        const textGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        textGroup.setAttribute('id', 'svg-text-layer');
        textGroup.setAttribute('class', 'svg-layer');
        
        // Add line boxes (if we had line bounding box data)
        // Note: Our current format doesn't have line bounding boxes, only word bounding boxes
        // This would need line-level bounding box data to implement properly
        
        // Add words from virtual DOM
        ocrData.lines.forEach((line, lineIndex) => {
            line.words.forEach((word, wordIndex) => {
                if (!word.boundingBox) return;
                
                const bbox = word.boundingBox;
                const confidence = word.confidence;
                
                // Word bounding box - use actual 4-point polygon coordinates (matching Java implementation)
                const polygon = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
                // Ensure proper polygon closure (Java seems to correct x4 to match x1 for proper rectangle)
                const points = `${bbox.x1},${bbox.y1} ${bbox.x2},${bbox.y2} ${bbox.x3},${bbox.y3} ${bbox.x1},${bbox.y4}`;
                polygon.setAttribute('points', points);
                polygon.setAttribute('class', getConfidenceClass(confidence));
                polygon.setAttribute('id', `word-${lineIndex}-${wordIndex}`);
                
                // Word text (exactly matching Java SvgVisualizer logic)
                const boxHeight = Math.abs(bbox.y3 - bbox.y1); // Java: Math.abs(bbox.y3() - bbox.y1())
                const fontSize = Math.max(8, Math.min(boxHeight * 0.7, 24)); // Java: Math.max(8, Math.min(boxHeight * 0.7, 24))
                const textX = Math.min(bbox.x1, bbox.x4) + 2; // Java: Math.min(bbox.x1(), bbox.x4()) + 2
                const textY = Math.min(bbox.y1, bbox.y2) + (boxHeight * 0.75); // Java: Math.min(bbox.y1(), bbox.y2()) + (boxHeight * 0.75)
                
                const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                text.setAttribute('x', textX.toFixed(1));
                text.setAttribute('y', textY.toFixed(1));
                text.setAttribute('class', 'word-text');
                text.setAttribute('style', `font-size: ${fontSize.toFixed(1)}px;`);
                text.setAttribute('title', `Confidence: ${(word.confidence * 100).toFixed(1)}%`);
                text.textContent = word.text;
                
                // Add hover effect
                const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                group.style.cursor = 'pointer';
                group.appendChild(polygon);
                group.appendChild(text.cloneNode(true));
                
                // Click handler
                group.addEventListener('click', () => {
                    showSVGWordDetails(word, event);
                });
                
                wordBoxGroup.appendChild(polygon.cloneNode(true));
                textGroup.appendChild(text);
            });
        });
        
        svg.appendChild(lineBoxGroup);
        svg.appendChild(wordBoxGroup);
        svg.appendChild(textGroup);
        
        return svg;
    }
    
    function getConfidenceClass(confidence) {
        if (confidence >= CONFIG.confidenceThresholds.high) return 'word-box-high';
        if (confidence >= CONFIG.confidenceThresholds.med) return 'word-box-med';
        return 'word-box-low';
    }
    
    function showSVGWordDetails(word, event) {
        const popup = document.createElement('div');
        popup.style.cssText = `
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
        `;
        
        popup.innerHTML = `
            <div><strong>Word:</strong> "${word.text}"</div>
            <div><strong>Confidence:</strong> ${(word.confidence * 100).toFixed(1)}%</div>
            <div><strong>Index:</strong> ${word.index}</div>
            <div><strong>Bounds:</strong> ${word.boundingBox ? `${word.boundingBox.x1},${word.boundingBox.y1} to ${word.boundingBox.x2},${word.boundingBox.y2}` : 'N/A'}</div>
        `;
        
        popup.style.left = event.clientX + 10 + 'px';
        popup.style.top = event.clientY + 10 + 'px';
        
        document.body.appendChild(popup);
        
        setTimeout(() => {
            if (popup.parentNode) {
                popup.parentNode.removeChild(popup);
            }
        }, 3000);
    }
    
    /**
     * Bind event handlers
     */
    function bindEventHandlers() {
        document.getElementById('toggle-line-boxes').addEventListener('change', function(e) {
            state.showLineBoxes = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-word-boxes').addEventListener('change', function(e) {
            state.showWordBoxes = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-xhtml-text').addEventListener('change', function(e) {
            state.showXHTMLText = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-svg-text').addEventListener('change', function(e) {
            state.showSVGText = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-hover-controls').addEventListener('change', function(e) {
            state.enableHoverControls = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-svg-section').addEventListener('change', function(e) {
            state.showSVGSection = e.target.checked;
            updateDisplay();
        });
        
        document.getElementById('toggle-svg-background').addEventListener('change', function(e) {
            state.showSVGBackground = e.target.checked;
            updateDisplay();
        });
    }
    
    /**
     * Update display based on current state
     */
    function updateDisplay() {
        const section = document.querySelector('section.win11OneOcrPage');
        if (!section) return;
        
        // Line boxes (segments)
        document.querySelectorAll('segment').forEach(segment => {
            if (state.showLineBoxes) {
                segment.classList.add('show-line-boxes');
            } else {
                segment.classList.remove('show-line-boxes');
            }
        });
        
        // Word boxes
        if (state.showWordBoxes) {
            section.classList.add('show-word-boxes');
        } else {
            section.classList.remove('show-word-boxes');
        }
        
        // XHTML Text content
        if (!state.showXHTMLText) {
            section.classList.add('hide-text');
        } else {
            section.classList.remove('hide-text');
        }
        
        // Hover controls
        const pageCopyBtn = section.querySelector('button');
        if (pageCopyBtn) {
            pageCopyBtn.style.display = state.enableHoverControls ? 'block' : 'none';
        }
        
        // SVG section visibility
        const svgContainer = document.querySelector('.svg-content');
        if (svgContainer) {
            svgContainer.style.display = state.showSVGSection ? 'block' : 'none';
        }
        
        // SVG layers
        const svgBgLayer = document.getElementById('svg-background-layer');
        if (svgBgLayer) {
            if (state.showSVGBackground) {
                svgBgLayer.classList.remove('hidden');
            } else {
                svgBgLayer.classList.add('hidden');
            }
        }
        
        const svgLineBoxes = document.getElementById('svg-line-boxes');
        if (svgLineBoxes) {
            if (state.showLineBoxes) {
                svgLineBoxes.classList.remove('hidden');
            } else {
                svgLineBoxes.classList.add('hidden');
            }
        }
        
        const svgWordBoxes = document.getElementById('svg-word-boxes');
        if (svgWordBoxes) {
            if (state.showWordBoxes) {
                svgWordBoxes.classList.remove('hidden');
            } else {
                svgWordBoxes.classList.add('hidden');
            }
        }
        
        const svgTextLayer = document.getElementById('svg-text-layer');
        if (svgTextLayer) {
            if (state.showSVGText) {
                svgTextLayer.classList.remove('hidden');
            } else {
                svgTextLayer.classList.add('hidden');
            }
        }
    }
    
    function updateStats() {
        const statsDiv = document.getElementById('ocr-stats');
        if (statsDiv && ocrData.metadata) {
            const meta = ocrData.metadata;
            
            statsDiv.innerHTML = `
                <div>${meta.totalLines} lines, ${meta.totalWords} words</div>
                <div>Avg confidence: ${(meta.averageConfidence * 100).toFixed(1)}%</div>
                <div>Page angle: ${meta.angle.toFixed(1)}°</div>
            `;
        }
    }
    
    /**
     * Debug function to export SVG elements for a specific line to console
     */
    function exportLineSVGToConsole(lineIndex) {
        console.log(`\n=== DEBUG: SVG Elements for Line ${lineIndex + 1} ===`);
        
        const lineData = ocrData.lines[lineIndex];
        if (!lineData) {
            console.log(`No data for line ${lineIndex + 1}`);
            return;
        }
        
        console.log(`Line ${lineIndex + 1} has ${lineData.words.length} words:`);
        
        // Export word polygons
        lineData.words.forEach((word, wordIndex) => {
            if (!word.boundingBox) return;
            
            const bbox = word.boundingBox;
            const confidence = word.confidence;
            const confClass = getConfidenceClass(confidence);
            
            // Generate polygon points (matching our code)
            const points = `${bbox.x1},${bbox.y1} ${bbox.x2},${bbox.y1} ${bbox.x2},${bbox.y2} ${bbox.x1},${bbox.y2}`;
            
            console.log(`Word ${wordIndex}: "${word.text}"`);
            console.log(`  <polygon id="word-${lineIndex}-${wordIndex}" points="${points}" class="${confClass}" />`);
            
            // Generate text element
            const boxHeight = Math.abs(bbox.y2 - bbox.y1);
            const fontSize = Math.max(8, Math.min(boxHeight * 0.7, 24));
            const textX = bbox.x1 + 2;
            const textY = bbox.y2 - (boxHeight * 0.25);
            
            console.log(`  <text x="${textX.toFixed(1)}" y="${textY.toFixed(1)}" class="word-text" style="font-size: ${fontSize.toFixed(1)}px;" title="Confidence: ${(confidence * 100).toFixed(1)}%">${word.text}</text>`);
            console.log(`  Confidence: ${(confidence * 100).toFixed(1)}% | BBox: ${bbox.x1},${bbox.y1},${bbox.x2},${bbox.y2}`);
            console.log('');
        });
        
        console.log('=== END DEBUG ===\n');
    }
    
    // Functions are now used directly via onclick handlers, no need for global exposure
    
})();