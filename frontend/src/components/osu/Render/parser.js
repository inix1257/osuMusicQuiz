export async function downloadAndParseTextFile(url) {
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const text = await response.text();
        const lines = text.split('\n')
        const result = {};
        let currentSection = "";

        lines.forEach(line => {
            // Check if the line is a section header
            if (line.startsWith('[') && line.endsWith(']')) {
                currentSection = line.slice(1, -1); // Remove the brackets
                result[currentSection] = {};
            } else {
                // Split the line into key and value
                if (currentSection === "TimingPoints" || currentSection === "HitObjects") {
                    if (currentSection === "TimingPoints") {
                        const timingPoint = line.split(',');
                        const offset = timingPoint[0];
                        const beatLength = timingPoint[1];
                        result[currentSection][offset] = beatLength;
                    } else {
                        const hitObject = line.split(',');
                        const x = hitObject[0];
                        const y = hitObject[1];
                        const time = hitObject[2];
                        const type = parseHitObjectType(hitObject[3]);
                        const hitSound = hitObject[4];
                        const extras = hitObject.slice(5);

                        var sliderInfo;

                        if (type.includes("Slider")) {
                            sliderInfo = parseSliderInfo(hitObject.slice(5));
                            sliderInfo.anchorPositions.unshift({x: Number(x), y: Number(y)});
                        }

                        result[currentSection][time] = {
                            x,
                            y,
                            type,
                            hitSound,
                            extras
                        };

                        if (sliderInfo) {
                            result[currentSection][time].sliderInfo = sliderInfo;
                        }
                    }
                } else {
                    const [key, value] = line.split(':').map(part => part.trim());
                    if (key && value) {
                        // Handle special case where value can be a list (e.g., Bookmarks)
                        if (value.includes(',')) {
                            result[currentSection][key] = value.split(',').map(v => v.trim());
                        } else {
                            result[currentSection][key] = value;
                        }
                    }
                }
            }
        });

        return result;
    } catch (error) {
        console.error('Error downloading or parsing the file:', error);
        return null;
    }
}

function parseHitObjectType(type) {
    const hitObjectTypes = {
        0: "Circle",
        1: "Slider",
        2: "New Combo",
        3: "Spinner"
    };

    let parsedTypes = [];
    let typeValue = parseInt(type, 10);

    // Check each bit flag
    if (typeValue & 1) parsedTypes.push(hitObjectTypes[0]);
    if (typeValue & 2) parsedTypes.push(hitObjectTypes[1]);
    if (typeValue & 4) parsedTypes.push(hitObjectTypes[2]);
    if (typeValue & 8) parsedTypes.push(hitObjectTypes[3]);

    return parsedTypes.length > 0 ? parsedTypes : "Unknown";
}

function parseSliderInfo(extras) {
    const sliderInfo = extras[0]; // Assuming extras[0] contains the slider anchor info
    const parts = sliderInfo.split('|');
    const sliderType = parts[0]; // The type of the slider
    const anchorPositions = parts.slice(1).map(pos => {
        const [x, y] = pos.split(':').map(Number);
        return {x, y};
    });

    const sliderRepeat = extras[1];
    const sliderLength = extras[2];

    return {
        sliderType,
        sliderRepeat,
        sliderLength,
        anchorPositions
    };
}