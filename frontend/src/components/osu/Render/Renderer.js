// pixiRenderer.js
import * as PIXI from 'pixi.js';
import {Application, Assets, Sprite} from "pixi.js";
import { downloadAndParseTextFile } from './parser';
import axios from "axios";

const app = new Application();

var currentTime = 0;

var beatmap;

var app_width = 1280;
var app_height = 720;

export async function createPixiApp() {

    await app.init({
        background: '#9acad5',
        width: app_width,
        height: app_height,
    });

    const basicText = new PIXI.Text({ text: currentTime });

    basicText.x = 10;
    basicText.y = 10;

    app.stage.addChild(basicText);

    const texture = await Assets.load('/img/osu/hitcircle.png');
    const approach_texture = await Assets.load('/img/osu/approachcircle.png');
    const hitcircle_texture = await Assets.load('/img/osu/hitcircle.png');
    const hitcircleoverlay_texture = await Assets.load('/img/osu/hitcircleoverlay.png');

    const url = '/beatmap/104229';

    // Call downloadAndParseTextFile and handle the parsed data
    beatmap = await downloadAndParseTextFile(url);

    console.log(beatmap);

    const approachCircles = {};

    Object.entries(beatmap["HitObjects"]).forEach(([time, hitObject]) => {
        const approachSprite = new PIXI.Sprite(approach_texture);
        approachSprite.x = hitObject.x / 512 * app_width - approachSprite.width / 2;
        approachSprite.y = hitObject.y / 384 * app_height - approachSprite.height / 2;
        approachSprite.anchor.set(0.5);
        approachSprite.alpha = 0;
        approachCircles[time] = approachSprite;

        // Hit Circle
        const hitCircleSprite = new PIXI.Sprite(hitcircle_texture);
        hitCircleSprite.x = hitObject.x / 512 * app_width - hitCircleSprite.width / 2;
        hitCircleSprite.y = hitObject.y / 384 * app_height - hitCircleSprite.height / 2;
        hitCircleSprite.anchor.set(0.5);
        hitCircleSprite.alpha = 0; // Initially invisible
        // app.stage.addChild(hitCircleSprite);

        // Hit Circle Overlay
        const overlaySprite = new PIXI.Sprite(hitcircleoverlay_texture);
        overlaySprite.x = hitObject.x / 512 * app_width - overlaySprite.width / 2;
        overlaySprite.y = hitObject.y / 384 * app_height - overlaySprite.height / 2;
        overlaySprite.anchor.set(0.5);
        overlaySprite.alpha = 0; // Initially invisible
        // app.stage.addChild(overlaySprite);

        // If Slider
        if (hitObject.type.includes("Slider")) {
            const sliderInfo = hitObject.sliderInfo;
            const slider = new PIXI.Graphics();

            drawBezierCurveWithAnyNumberOfPoints(slider, sliderInfo.anchorPositions);
            slider.stroke({ width: overlaySprite.width / 2, color: 0x444444 });

            hitObject.sliderSprite = slider;
        }

        // Store hitCircle and overlay sprites for later access
        hitObject.hitCircleSprite = hitCircleSprite;
        hitObject.overlaySprite = overlaySprite;

    });

    // const slider = new PIXI.Graphics();
    //
    // slider.moveTo(50, 350);
    // slider.bezierCurveTo(50, 350, 100, 100, 200, 100);
    // slider.stroke({ width: 4, color: 0xffd900 });
    //
    // app.stage.addChild(slider);


    app.ticker.add((timeInfo) => {
        currentTime += timeInfo.deltaMS;
        basicText.text = "currentTime:" + currentTime;

        Object.entries(approachCircles).forEach(([time, approachSprite]) => {
            const hitTime = parseInt(time);
            const timeDiff = hitTime - currentTime;
            const hitObject = beatmap["HitObjects"][time];

            const ARms = 450;

            // Adjust scale and visibility based on timeDiff
            if (timeDiff > 0 && timeDiff <= ARms) {
                approachSprite.alpha = 1; // Make approach circle visible
                const preemptRate = - (currentTime - hitTime) / ARms;
                approachSprite.scale.set(1 + preemptRate * 3);
                app.stage.addChild(approachSprite);
                app.stage.addChild(hitObject.hitCircleSprite);
                app.stage.addChild(hitObject.overlaySprite);

                if (hitObject.type.includes("Slider")) {
                    app.stage.addChild(hitObject.sliderSprite);
                }

                // Make hitCircle and overlay visible
                hitObject.hitCircleSprite.alpha = 1;
                hitObject.overlaySprite.alpha = 1;
            } else if (timeDiff <= 0) {
                // Remove sprites if currentTime has passed hitTime
                app.stage.removeChild(approachSprite);
                app.stage.removeChild(hitObject.hitCircleSprite);
                app.stage.removeChild(hitObject.overlaySprite);
                if (hitObject.type.includes("Slider")) {
                    app.stage.removeChild(hitObject.sliderSprite);
                }
                delete approachCircles[time];
            }
        });
    });

    return app;
}

export function resetTimer() {
    currentTime = 0;
}

function getTimingPointAt(givenTime) {
    const timingPoints = beatmap["TimingPoints"];
    let closestBPM = null;
    let closestSV = null;

    Object.entries(timingPoints).forEach(([time, beatLength]) => {
        const timeInt = parseInt(time);
        const beatLengthValue = parseFloat(beatLength);
        if (timeInt < givenTime) {
            if (beatLengthValue > 0) { // BPM
                const bpm = 60000 / beatLengthValue;
                if (!closestBPM || timeInt > closestBPM.time) {
                    closestBPM = { time: timeInt, bpm, beatLengthValue };
                }
            } else if (beatLengthValue < 0) { // SV
                if (!closestSV || timeInt > closestSV.time) {
                    closestSV = { time: timeInt, sv: beatLengthValue };
                }
            }
        }
    });

    return { closestBPM, closestSV };
}

function drawBezierCurveWithAnyNumberOfPoints(slider, anchorPositions) {
    if (anchorPositions.length < 2) {
        // Not enough points to draw anything
        return;
    }

    // Move to the start point
    slider.moveTo(anchorPositions[0].x, anchorPositions[0].y);

    if (anchorPositions.length === 2) {
        // Draw a straight line for two points
        slider.lineTo(anchorPositions[1].x, anchorPositions[1].y);
    } else {
        // Draw Bezier curves for more than two points
        for (let i = 1; i < anchorPositions.length; i += 3) {
            if (i + 2 < anchorPositions.length) {
                // Enough points for a cubic Bezier curve
                slider.bezierCurveTo(
                    anchorPositions[i].x, anchorPositions[i].y, // First control point
                    anchorPositions[i + 1].x, anchorPositions[i + 1].y, // Second control point
                    anchorPositions[i + 2].x, anchorPositions[i + 2].y // End point
                );
            } else if (i + 1 < anchorPositions.length) {
                // Only enough points for a quadratic Bezier curve
                slider.quadraticCurveTo(
                    anchorPositions[i].x, anchorPositions[i].y, // Control point
                    anchorPositions[i + 1].x, anchorPositions[i + 1].y // End point
                );
            } else {
                // Only two points left, draw a straight line
                slider.lineTo(anchorPositions[i].x, anchorPositions[i].y);
            }
        }
    }

    console.log("Drawing Bezier curve with anchor positions:", anchorPositions, slider);
}