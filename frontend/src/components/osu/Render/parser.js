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

        let comboIndex = 0;
        let comboColorIndex = 0;

        lines.forEach(line => {
            if (line.startsWith('[') && line.endsWith(']')) {
                currentSection = line.slice(1, -1);
                result[currentSection] = currentSection === "Colours" ? [] : {};
            } else {
                if (currentSection === "TimingPoints" || currentSection === "HitObjects") {
                    if (currentSection === "TimingPoints") {
                        const timingPoint = line.split(',');
                        const offset = timingPoint[0];
                        const beatLength = timingPoint[1];
                        const meter = timingPoint[2];
                        const sampleset = parseInt(timingPoint[3]);
                        const sampleIndex = timingPoint[4];
                        const volume = timingPoint[5];
                        const inherited = timingPoint[6];
                        const kiai = timingPoint[7];

                        if (!offset || !beatLength) {
                            return;
                        }

                        if (!result[currentSection][offset]) {
                            result[currentSection][offset] = [];
                        }

                        if (!result[currentSection][offset][inherited]) {
                            result[currentSection][offset][inherited] = [];
                        }

                        result[currentSection][offset][inherited].push(beatLength);
                        result[currentSection][offset][inherited].push(sampleset);
                    } else { // HitObjects
                        const hitObject = line.split(',');
                        const x = hitObject[0];
                        const y = hitObject[1];
                        const time = hitObject[2];
                        const type = parseHitObjectType(hitObject[3]);
                        const hitSound = hitObject[4];
                        const extras = type.includes("Circle") ? hitObject[5] : hitObject.slice(5);

                        if (type.includes("New Combo")) {
                            comboIndex = 1;
                            comboColorIndex++;
                        }

                        const combo = comboIndex;
                        const comboColor = comboColorIndex;

                        var sliderInfo;

                        if (type.includes("Slider")) {
                            sliderInfo = parseSliderInfo(hitObject.slice(5));
                            sliderInfo.anchorPositions.unshift({x: Number(x), y: Number(y)});
                        }

                        result[currentSection][time] = {
                            x,
                            y,
                            time,
                            type,
                            hitSound,
                            extras
                        };

                        result[currentSection][time].combo = combo;
                        result[currentSection][time].comboColor = comboColor;

                        result[currentSection][time].hitsound = {};

                        comboIndex++;

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

        if (!result["Difficulty"]["ApproachRate"]) {
            result["Difficulty"]["ApproachRate"] = result["Difficulty"]["OverallDifficulty"];
        }

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

    let deepCopyAnchorPositions = JSON.parse(JSON.stringify(anchorPositions[anchorPositions.length - 1]));

    anchorPositions.push(deepCopyAnchorPositions);

    const sliderRepeat = extras[1];
    const sliderLength = extras[2];
    const edgeSounds = extras[3];
    const edgeSets = extras[4];

    return {
        sliderType,
        sliderRepeat,
        sliderLength,
        anchorPositions,
        edgeSounds,
        edgeSets
    };
}