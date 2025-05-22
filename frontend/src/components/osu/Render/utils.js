import {PathPoint} from "@/components/osu/Render/PathPoint";
import {Bezier} from "@/components/osu/Render/Bezier";

export function getTimingPointAt(givenTime, beatmap) {
    const timingPoints = beatmap["TimingPoints"];
    let closestBPM = null;
    let closestSV = null;
    let sampleSet = null;

    Object.entries(timingPoints).forEach(([time, beatLengths]) => {
        const timeInt = parseInt(time);

        beatLengths.forEach(beatLength => {
            const beatLengthValue = parseFloat(beatLength[0]);
            if (!sampleSet) sampleSet = parseInt(beatLength[1]);

            if (timeInt <= givenTime) {
                if (beatLengthValue > 0) { // BPM
                    const bpm = 60000 / beatLengthValue;
                    if (!closestBPM || timeInt > closestBPM.time) {
                        closestBPM = { time: timeInt, bpm, beatLengthValue };
                    }
                } else { // SV
                    if (!closestSV || timeInt > closestSV.time) {
                        closestSV = { time: timeInt, sv: beatLengthValue };
                        sampleSet = parseInt(beatLength[1]);
                    }
                }
            }
        });
    });

    return { closestBPM, closestSV, sampleSet };
}

export function getFollowPosition(hitObject, hitTime, currentTime, grid_unit) {
    var sliderProgress = (currentTime - hitTime) / hitObject.sliderDuration * hitObject.sliderRepeat;
    let sliderInfo = hitObject.sliderInfo;

    // If something goes wrong, return the starting position as fallback
    if (!sliderInfo || !sliderInfo.anchorPositions || sliderInfo.anchorPositions.length === 0) {
        return { 
            x: sliderInfo?.anchorPositions?.[0]?.x || hitObject.hitCircleSprite?.x || 0,
            y: sliderInfo?.anchorPositions?.[0]?.y || hitObject.hitCircleSprite?.y || 0
        };
    }

    sliderProgress = sliderProgress % 2 > 1 ? 2 - (sliderProgress % 2) : sliderProgress % 2;

    let anchorPositions = sliderInfo.anchorPositions;
    const targetLength = sliderProgress * (hitObject.sliderLength * grid_unit);

    let position = {
        // Default to the first anchor position as fallback
        x: anchorPositions[0].x,
        y: anchorPositions[0].y
    };
    let accumulatedLength = 0;

    switch (sliderInfo.sliderType) {
        case "P":{
            try {
                const circleCenter = getCircleCenter(anchorPositions[0], anchorPositions[1], anchorPositions[2]);
                const radius = Math.sqrt(Math.pow(circleCenter.x - anchorPositions[0].x, 2) + Math.pow(circleCenter.y - anchorPositions[0].y, 2));

                let yDeltaA = anchorPositions[1].y - anchorPositions[0].y;
                let xDeltaA = anchorPositions[1].x - anchorPositions[0].x;
                let yDeltaB = anchorPositions[2].y - anchorPositions[1].y;
                let xDeltaB = anchorPositions[2].x - anchorPositions[1].x;

                const angleA = Math.atan2(anchorPositions[0].y - circleCenter.y, anchorPositions[0].x - circleCenter.x);
                const angleC = Math.atan2(anchorPositions[2].y - circleCenter.y, anchorPositions[2].x - circleCenter.x);

                const anticlockwise = (xDeltaB * yDeltaA - xDeltaA * yDeltaB) > 0;
                const startAngle = angleA;
                let endAngle = angleC;

                if (!anticlockwise && (endAngle - startAngle) < 0) {
                    endAngle += 2 * Math.PI;
                }
                if (anticlockwise && (endAngle - startAngle) > 0) {
                    endAngle -= 2 * Math.PI;
                }

                let angleStep = (endAngle - startAngle) / 100;

                let prevX = anchorPositions[0].x;
                let prevY = anchorPositions[0].y;
                let totalLength = 0;

                for (let i = 0; i <= 100; i++) {
                    const currentAngle = startAngle + angleStep * i;
                    const x = circleCenter.x + radius * Math.cos(currentAngle);
                    const y = circleCenter.y + radius * Math.sin(currentAngle);

                    if (i > 0) {
                        totalLength += Math.sqrt(Math.pow(Math.abs(x - prevX), 2) + Math.pow(Math.abs(y - prevY), 2));
                        if (totalLength >= targetLength) {
                            position = {x, y};
                            return position;
                        }
                    }
                    
                    prevX = x;
                    prevY = y;
                }
                
                // If we reached the end of the loop and didn't return, use the last position
                position = {x: prevX, y: prevY};
            } catch (e) {
                console.error("Error calculating perfect curve position", e);
                // Fallback to first anchor
            }
            break;
        }
        default: {
            try {
                let arrPn = [];
                let bezier = new Bezier();
                arrPn.push(new PathPoint(anchorPositions[0].x, anchorPositions[0].y));

                for (let i = 1; i < anchorPositions.length; i++) {
                    let prevPos = anchorPositions[i - 1];
                    let currPos = anchorPositions[i];
                    if (i === anchorPositions.length - 1 || PathPoint.compare(prevPos, currPos)) {
                        if (i === anchorPositions.length - 1) {
                            let p = new PathPoint();
                            p.x = anchorPositions[i].x;
                            p.y = anchorPositions[i].y;
                            arrPn.push(p);
                        }
                        bezier.setBezierN(arrPn);
                        const segmentLength = bezier.arcLength;
                        if (accumulatedLength + segmentLength >= targetLength) {
                            const segmentTargetLength = targetLength - accumulatedLength;
                            const mu = bezier.getMuForArcLength(segmentTargetLength);
                            bezier.setMu(mu);
                            bezier.bezierCalc();
                            const result = bezier.getResult();
                            if (result && result.x !== undefined && result.y !== undefined) {
                                position = result;
                                break;
                            }
                        }
                        accumulatedLength += segmentLength;
                        arrPn = [];
                        bezier = new Bezier();
                        if (i < anchorPositions.length - 1) {
                            let p = new PathPoint();
                            p.x = prevPos.x;
                            p.y = prevPos.y;
                            arrPn.push(p);
                        }
                    } else {
                        let p = new PathPoint();
                        p.x = anchorPositions[i].x;
                        p.y = anchorPositions[i].y;
                        arrPn.push(p);
                    }
                }
            } catch (e) {
                console.error("Error calculating bezier curve position", e);
                // Fallback to first anchor
            }
            break;
        }
    }

    return position;
}

export function getCircleCenter(p1, p2, p3) {
    const mid1 = { x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2 };
    const mid2 = { x: (p2.x + p3.x) / 2, y: (p2.y + p3.y) / 2 };

    const slope1 = (p2.y - p1.y) / (p2.x - p1.x);
    const slope2 = (p3.y - p2.y) / (p3.x - p2.x);

    let perpSlope1 = -1 / slope1;
    let perpSlope2 = -1 / slope2;

    // Handle special cases
    if (p1.x === p2.x) {
        perpSlope1 = 0;
    }
    if (p2.x === p3.x) {
        perpSlope2 = 0;
    }
    if (p1.y === p2.y) {
        perpSlope1 = -1000;
    }
    if (p2.y === p3.y) {
        perpSlope2 = -1000;
    }

    const centerX = (perpSlope1 * mid1.x - perpSlope2 * mid2.x + mid2.y - mid1.y) / (perpSlope1 - perpSlope2);
    const centerY = perpSlope1 * (centerX - mid1.x) + mid1.y;

    return { x: centerX, y: centerY };
}