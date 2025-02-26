import { Application, Assets, Sprite } from 'pixi.js';
import hitcircleImage from '@/assets/osu/hitcircle.png';
import hitcircleoverlayImage from '@/assets/osu/hitcircleoverlay.png';
import approachImage from '@/assets/osu/approachcircle.png';
import reverseImage from '@/assets/osu/reversearrow.png';
import * as PIXI from "pixi.js";
import {downloadAndParseTextFile} from "@/components/osu/Render/parser";
import {PathPoint} from "@/components/osu/Render/PathPoint";
import {Bezier} from "@/components/osu/Render/Bezier";
import {getTimingPointAt} from "@/components/osu/Render/utils";
import {getFollowPosition, getCircleCenter} from "@/components/osu/Render/utils";
import normal_hitnormal from '@/assets/osu/normal-hitnormal.wav';
import normal_hitwhistle from '@/assets/osu/normal-hitwhistle.wav';
import normal_hitfinish from '@/assets/osu/normal-hitfinish.wav';
import normal_hitclap from '@/assets/osu/normal-hitclap.wav';
import soft_hitnormal from '@/assets/osu/soft-hitnormal.wav';
import soft_hitwhistle from '@/assets/osu/soft-hitwhistle.wav';
import soft_hitfinish from '@/assets/osu/soft-hitfinish.wav';
import soft_hitclap from '@/assets/osu/soft-hitclap.wav';
import drum_hitnormal from '@/assets/osu/drum-hitnormal.wav';
import drum_hitwhistle from '@/assets/osu/drum-hitwhistle.wav';
import drum_hitfinish from '@/assets/osu/drum-hitfinish.wav';
import drum_hitclap from '@/assets/osu/drum-hitclap.wav';
import { sound } from '@pixi/sound';

const display_width = 1280 / 2;
const display_height = 720 / 2;
const center_x = display_width / 2;
const center_y = display_height / 2;
const grid_unit = display_height * 0.8 / 384;
const top_left_x = center_x - 256 * grid_unit;
const top_left_y = center_y - 192 * grid_unit;

var beatmap;
var currentTime = 0;
var updateTime = true;
var counter_approach = 0;
var counter_hitcircle = 0;

var app;

var updating = false;

export async function createPixiApp() {
    app = new Application();
    await app.init({ background: '#6fa9d5', width: display_width, height: display_height, antialias: true });
    app.stage.sortableChildren = true;
    document.getElementById('pixi-container').appendChild(app.canvas);

    sound.init();
    sound.volumeAll = 0.1;
    sound.disableAutoPause = true;
    sound.add('normal-hitnormal', normal_hitnormal);
    sound.add('normal-hitwhistle', normal_hitwhistle);
    sound.add('normal-hitfinish', normal_hitfinish);
    sound.add('normal-hitclap', normal_hitclap);
    sound.add('soft-hitnormal', soft_hitnormal);
    sound.add('soft-hitwhistle', soft_hitwhistle);
    sound.add('soft-hitfinish', soft_hitfinish);
    sound.add('soft-hitclap', soft_hitclap);
    sound.add('drum-hitnormal', drum_hitnormal);
    sound.add('drum-hitwhistle', drum_hitwhistle);
    sound.add('drum-hitfinish', drum_hitfinish);
    sound.add('drum-hitclap', drum_hitclap);

    const textElements = setupTextElements(app);
    const textures = await loadAssets();

    app.ticker.add((time) => {
        if (updating || !beatmap) return;
        const { preempt, fade_in, ARms } = calculateARValues(beatmap.Difficulty.ApproachRate);
        update(app, time, textElements, textures, preempt, fade_in, ARms)
    });

    return app;
}

export function setCurrentTime(newTime) {
    currentTime = newTime;

    if (beatmap) {
        const { preempt, fade_in, ARms } = calculateARValues(beatmap.Difficulty.ApproachRate);
        const entries = Object.entries(beatmap["HitObjects"]);
        for (let i = entries.length - 1; i >= 0; i--) {
            const [time, hitObject] = entries[i];
            const hitTime = parseInt(time);
            const timeDiff = hitTime - currentTime;
            const alpha = Math.min(1, (preempt - timeDiff) / fade_in);

            hitObject.hitsound = {};
            hitObject.hitsound.sliderRepeat = {};

            removeHitObject(app, hitObject);
        }
    }
}

export function setCurrentTimeToPreviewPoint() {
    if (beatmap) {
        setCurrentTime(parseInt(beatmap["General"]["PreviewTime"]));
    }
}

export async function setBeatmap(beatmapId) {
    updating = true;
    var beatmapUrl = '/beatmap/' + beatmapId;
    setCurrentTime(-9999);
    beatmap = await downloadAndParseTextFile(beatmapUrl);
    setCurrentTimeToPreviewPoint();
    updating = false;
}

export async function setBeatmapBypass(beatmapId) {
    updating = true;
    var beatmapUrl = '/beatmapbp/' + beatmapId;
    setCurrentTime(-9999);
    beatmap = await downloadAndParseTextFile(beatmapUrl);
    setCurrentTime(parseInt(beatmap["General"]["PreviewTime"]));
    updating = false;
}

function setupTextElements(app) {
    const text_currentTime = new PIXI.Text({ text: currentTime });
    const text_count_approach = new PIXI.Text({ text: counter_approach });
    const text_count_hitcircle = new PIXI.Text({ text: counter_hitcircle });
    const text_fps = new PIXI.Text({ text: app.ticker.FPS });

    text_currentTime.x = 10;
    text_currentTime.y = 10;
    text_count_approach.x = 10;
    text_count_approach.y = 50;
    text_count_hitcircle.x = 10;
    text_count_hitcircle.y = 90;
    text_fps.x = 10;
    text_fps.y = 130;

    // app.stage.addChild(text_currentTime);
    // app.stage.addChild(text_count_approach);
    // app.stage.addChild(text_count_hitcircle);
    // app.stage.addChild(text_fps);

    return { text_currentTime, text_count_approach, text_count_hitcircle, text_fps };
}

async function loadAssets() {
    const texture_approach = await Assets.load(approachImage);
    const texture_hitcircle = await Assets.load(hitcircleImage);
    const texture_hitcircleoverlay = await Assets.load(hitcircleoverlayImage);
    const texture_reverse = await Assets.load(reverseImage);

    return { texture_approach, texture_hitcircle, texture_hitcircleoverlay, texture_reverse };
}

function calculateARValues(AR) {
    let preempt, fade_in;

    if (AR < 5) {
        preempt = 1200 + 600 * (5 - AR) / 5;
        fade_in = 800 + 400 * (5 - AR) / 5;
    } else if (AR === 5) {
        preempt = 1200;
        fade_in = 800;
    } else {
        preempt = 1200 - 750 * (AR - 5) / 5;
        fade_in = 800 - 500 * (AR - 5) / 5;
    }

    const ARms = preempt;
    return { preempt, fade_in, ARms };
}

function update(app, time, textElements, textures, preempt, fade_in, ARms) {
    if (updateTime) currentTime += time.deltaMS;

    if (!beatmap) return;
    if (!beatmap["HitObjects"]) return;

    textElements.text_currentTime.text = new Date(currentTime).toISOString().substr(11, 10);
    textElements.text_fps.text = app.ticker.speed;

    counter_approach = 0;
    counter_hitcircle = 0;

    const entries = Object.entries(beatmap["HitObjects"]);
    for (let i = entries.length - 1; i >= 0; i--) {
        const [time, hitObject] = entries[i];
        const hitTime = parseInt(time);
        const timeDiff = hitTime - currentTime;
        const alpha = Math.min(1, (preempt - timeDiff) / fade_in);

        if (timeDiff < ARms && timeDiff > 0) {
            counter_hitcircle++;
            addHitObject(app, hitObject, textures, alpha, ARms);
        } else {
            updateHitObject(app, hitObject, timeDiff, hitTime);
        }
    }

    textElements.text_count_approach.text = counter_approach;
    textElements.text_count_hitcircle.text = counter_hitcircle;
}

function addHitObject(app, hitObject, textures, alpha, ARms) {
    const hitTime = hitObject.time;
    const zIndex = -hitTime;
    const timeDiff = hitTime - currentTime;

    if (hitObject.type.includes("Spinner")) {
        if (!hitObject.approachSprite) {
            const approachSprite = new PIXI.Sprite(textures.texture_approach);
            approachSprite.x = center_x;
            approachSprite.y = center_y;
            approachSprite.width = grid_unit * 512;
            approachSprite.height = grid_unit * 512;
            approachSprite.anchor.set(0.5);
            approachSprite.scale.set(1);

            app.stage.addChild(approachSprite);
            hitObject.approachSprite = approachSprite;
        }

        return;
    }

    if (!hitObject.hitCircleSprite) {
        const pos_x = top_left_x + (hitObject.x) * grid_unit;
        const pos_y = top_left_y + (hitObject.y) * grid_unit;
        const circleSize = (54.4 - 4.48 * beatmap["Difficulty"]["CircleSize"]) * grid_unit * 2;
        const comboColors = [
            [1, 0.6, 0],
            [1, 0, 0],
            [0.2, 0.6, 1],
            [0.2, 1, 0],
        ];
        const comboColorIndex = hitObject.comboColor;

        const hitCircleSprite = new PIXI.Sprite(textures.texture_hitcircle);
        hitCircleSprite.x = pos_x;
        hitCircleSprite.y = pos_y;
        hitCircleSprite.width = circleSize;
        hitCircleSprite.height = circleSize;
        hitCircleSprite.anchor.set(0.5);
        hitCircleSprite.tint = comboColors[comboColorIndex % comboColors.length];
        hitCircleSprite.zIndex = zIndex;
        hitObject.hitCircleSprite = hitCircleSprite;

        const hitCircleOverlaySprite = new PIXI.Sprite(textures.texture_hitcircleoverlay);
        hitCircleOverlaySprite.x = pos_x;
        hitCircleOverlaySprite.y = pos_y;
        hitCircleOverlaySprite.width = circleSize;
        hitCircleOverlaySprite.height = circleSize;
        hitCircleOverlaySprite.anchor.set(0.5);
        hitCircleOverlaySprite.zIndex = zIndex;
        hitObject.hitCircleOverlaySprite = hitCircleOverlaySprite;

        const approachSprite = new PIXI.Sprite(textures.texture_approach);
        approachSprite.x = pos_x;
        approachSprite.y = pos_y;
        approachSprite.anchor.set(0.5);
        approachSprite.tint = comboColors[comboColorIndex % comboColors.length];
        approachSprite.zIndex = 99999999;
        hitObject.approachSprite = approachSprite;

        const comboText = new PIXI.Text({ text: hitObject.combo });
        comboText.x = pos_x;
        comboText.y = pos_y;
        const textWidthScale = circleSize / comboText.width * 0.4;
        const textHeightScale = circleSize / comboText.height * 0.4;

        comboText.scale.set(textWidthScale < textHeightScale ? textWidthScale : textHeightScale);
        comboText.anchor.set(0.5);
        comboText.tint = 0x000000;
        comboText.zIndex = zIndex;
        hitObject.comboText = comboText;

        if (hitObject.type.includes("Slider")) {
            addSlider(app, hitObject, textures, pos_x, pos_y, circleSize, comboColors, comboColorIndex);
        }

        app.stage.addChild(approachSprite);
        app.stage.addChild(hitCircleSprite);
        app.stage.addChild(hitCircleOverlaySprite);
        app.stage.addChild(comboText);

    }

    if (hitObject.approachSprite) {
        const scale = 0.9 + (timeDiff / ARms) * 3;
        hitObject.approachSprite.width = hitObject.hitCircleSprite.width * scale;
        hitObject.approachSprite.height = hitObject.hitCircleSprite.height * scale;
        hitObject.approachSprite.alpha = Math.min(1, alpha);

        if (scale <= 1 && !hitObject.hitsound.head) {
            const timingPoint = getTimingPointAt(hitTime, beatmap);

            if (hitObject.type.includes("Circle")) {
                let normalSet, additionSet;
                if (hitObject.extras) {
                    normalSet = hitObject.extras.split(':')[0] == 0 ? timingPoint.sampleSet : hitObject.extras.split(':')[0];
                    additionSet = hitObject.extras.split(':')[1] == 0 ? normalSet : hitObject.extras.split(':')[1];
                } else {
                    normalSet = timingPoint.sampleSet;
                    additionSet = normalSet;
                }

                playHitsound(normalSet, additionSet, hitObject.hitSound);

            } else if (hitObject.type.includes("Slider")) {
                if (hitObject.sliderInfo.edgeSounds && hitObject.sliderInfo.edgeSets) {
                    const hitsound = hitObject.sliderInfo.edgeSounds.split('|')[0];
                    const edgeSet = hitObject.sliderInfo.edgeSets.split('|')[0];
                    const normalSet = edgeSet.split(':')[0] == 0 ? timingPoint.sampleSet : edgeSet.split(':')[0];
                    const additionSet = edgeSet.split(':')[1] == 0 ? normalSet : edgeSet.split(':')[1];
                    playHitsound(normalSet, additionSet, hitsound);
                } else {
                    const normalSet = timingPoint.sampleSet;
                    playHitsound(normalSet, normalSet, 0);
                }
            }

            hitObject.hitsound.head = true;
        }
    }
    if (hitObject.hitCircleSprite) {
        hitObject.comboText.alpha = Math.min(1, alpha);
        hitObject.hitCircleSprite.alpha = Math.min(1, alpha);
        hitObject.hitCircleOverlaySprite.alpha = Math.min(1, alpha);
    }
    if (hitObject.sliderSprite) {
        hitObject.sliderSprite.alpha = Math.min(1, alpha);
        hitObject.hitCircleSprite_sliderend.alpha = Math.min(1, alpha);
        hitObject.hitCircleOverlaySprite_sliderend.alpha = Math.min(1, alpha);
        if (hitObject.reverseSprite) {
            hitObject.reverseSprite.alpha = Math.min(1, alpha);
        }
    }
}

function addSlider(app, hitObject, textures, pos_x, pos_y, circleSize, comboColors, comboColorIndex) {
    const zIndex = -hitObject.time;
    const sliderInfo = hitObject.sliderInfo;
    let sliderPath;

    switch (sliderInfo.sliderType) {
        case "P":
            sliderPath = perfectCurve(sliderInfo);
            break;
        default:
            sliderPath = bezierCurve(sliderInfo);
    }

    sliderPath.stroke({
        width: hitObject.hitCircleSprite.width * 0.9,
        color: 0x444444,
        join: 'round',
        cap: 'round'
    });

    sliderPath.zIndex = zIndex;

    app.stage.addChild(sliderPath);
    hitObject.sliderSprite = sliderPath;

    const hitCircleSprite_sliderend = new PIXI.Sprite(textures.texture_hitcircle);
    hitCircleSprite_sliderend.x = sliderInfo.sliderEndPos.x;
    hitCircleSprite_sliderend.y = sliderInfo.sliderEndPos.y;
    hitCircleSprite_sliderend.width = circleSize;
    hitCircleSprite_sliderend.height = circleSize;
    hitCircleSprite_sliderend.anchor.set(0.5);
    hitCircleSprite_sliderend.tint = comboColors[comboColorIndex % comboColors.length];
    hitCircleSprite_sliderend.zIndex = zIndex;
    app.stage.addChild(hitCircleSprite_sliderend);
    hitObject.hitCircleSprite_sliderend = hitCircleSprite_sliderend;

    const hitCircleOverlaySprite_sliderend = new PIXI.Sprite(textures.texture_hitcircleoverlay);
    hitCircleOverlaySprite_sliderend.x = sliderInfo.sliderEndPos.x;
    hitCircleOverlaySprite_sliderend.y = sliderInfo.sliderEndPos.y;
    hitCircleOverlaySprite_sliderend.width = circleSize;
    hitCircleOverlaySprite_sliderend.height = circleSize;
    hitCircleOverlaySprite_sliderend.anchor.set(0.5);
    hitCircleOverlaySprite_sliderend.zIndex = zIndex;
    app.stage.addChild(hitCircleOverlaySprite_sliderend);
    hitObject.hitCircleOverlaySprite_sliderend = hitCircleOverlaySprite_sliderend;

    if (sliderInfo.sliderRepeat > 1) {
        const reverseSprite = new PIXI.Sprite(textures.texture_reverse);
        reverseSprite.x = sliderInfo.sliderEndPos.x;
        reverseSprite.y = sliderInfo.sliderEndPos.y;
        reverseSprite.width = circleSize * 0.6;
        reverseSprite.height = circleSize * 0.6;
        reverseSprite.anchor.set(0.5);
        reverseSprite.zIndex = zIndex;
        app.stage.addChild(reverseSprite);
        hitObject.reverseSprite = reverseSprite;
    }

    const followCircle = new PIXI.Graphics();
    followCircle.beginFill(0xEEEEEE);
    followCircle.drawCircle(0, 0, hitObject.hitCircleSprite.width / 2.4);
    followCircle.endFill();
    followCircle.x = hitObject.x / 512 * display_width - hitObject.hitCircleSprite.width / 2;
    followCircle.y = hitObject.y / 384 * display_height - hitObject.hitCircleSprite.height / 2;
    followCircle.alpha = 0;
    followCircle.zIndex = zIndex+1;
    app.stage.addChild(followCircle);
    hitObject.followCircle = followCircle;
}

function updateHitObject(app, hitObject, timeDiff, hitTime) {
    if (hitObject.followCircle && timeDiff < 0) {
        const timingPoint = getTimingPointAt(hitTime, beatmap);
        const beatLength = timingPoint.closestBPM.beatLengthValue;
        const baseSV = beatmap["Difficulty"]["SliderMultiplier"];
        const speed = timingPoint.closestSV ? timingPoint.closestSV.sv : -100;
        const sliderRepeat = hitObject.sliderInfo.sliderRepeat;
        const sliderLength = hitObject.sliderInfo.sliderLength;
        const sliderDuration = sliderLength / (100 * baseSV * (-100 / speed)) * beatLength * sliderRepeat - 10;

        if (currentTime <= hitTime + sliderDuration) {
            hitObject.sliderRepeat = sliderRepeat;
            hitObject.sliderLength = sliderLength;
            hitObject.sliderDuration = sliderDuration;

            var sliderProgress = (currentTime - hitTime) / hitObject.sliderDuration * hitObject.sliderRepeat;
            var sliderProgressIndex = Math.floor(sliderProgress);
            hitObject.followCircle.alpha = 0.7;

            if (!hitObject.hitsound.sliderRepeat[sliderProgressIndex] && sliderProgressIndex < hitObject.sliderRepeat && sliderProgressIndex > 0) {
                if (hitObject.sliderInfo.edgeSounds) {
                    const edgeSound = hitObject.sliderInfo.edgeSounds.split('|')[sliderProgressIndex];
                    var normalSet, additionSet;

                    if (hitObject.sliderInfo.edgeSets) {
                        const edgeSet = hitObject.sliderInfo.edgeSets.split('|')[sliderProgressIndex];
                        normalSet = edgeSet.split(':')[0] == 0 ? timingPoint.sampleSet : edgeSet.split(':')[0];
                        additionSet = edgeSet.split(':')[1] == 0 ? normalSet : edgeSet.split(':')[1];
                    } else {
                        normalSet = timingPoint.sampleSet;
                        additionSet = normalSet;
                    }

                    playHitsound(normalSet, additionSet, edgeSound);
                } else {
                    const normalset = timingPoint.sampleSet;

                    playHitsound(normalset, normalset, 0);
                }

                hitObject.hitsound.sliderRepeat[sliderProgressIndex] = true;
            }

            const position = getFollowPosition(hitObject, hitTime, currentTime, grid_unit);

            hitObject.followCircle.x = position.x;
            hitObject.followCircle.y = position.y;
        } else {
            if (!hitObject.hitsound.sliderend) {
                if (hitObject.sliderInfo.edgeSounds && hitObject.sliderInfo.edgeSets) {
                    const edgeSound = hitObject.sliderInfo.edgeSounds.split('|')[hitObject.sliderRepeat];
                    const edgeSet = hitObject.sliderInfo.edgeSets.split('|')[hitObject.sliderRepeat];
                    const normalSet = edgeSet.split(':')[0] == 0 ? timingPoint.sampleSet : edgeSet.split(':')[0];
                    const additionSet = edgeSet.split(':')[1] == 0 ? normalSet : edgeSet.split(':')[1];
                    playHitsound(normalSet, additionSet, edgeSound);
                } else {
                    const normalSet = timingPoint.sampleSet;
                    playHitsound(normalSet, normalSet, 0);
                }
                hitObject.hitsound.sliderend = true;
            }

            removeHitObject(app, hitObject);
        }
    }

    if (hitObject.approachSprite || hitObject.sliderSprite) {
        if (hitObject.type.includes("Slider")) {
            const timingPoint = getTimingPointAt(hitTime, beatmap);
            const beatLength = timingPoint.closestBPM.beatLengthValue;
            const sliderRepeat = hitObject.sliderInfo.sliderRepeat;
            const sliderLength = hitObject.sliderInfo.sliderLength;
            const sliderDuration = beatLength * sliderRepeat * sliderLength / 100;

            if (currentTime > hitTime + sliderDuration) {
                removeHitObject(app, hitObject);
            }
        } else if (hitObject.type.includes("Spinner")) {
            const endTime = hitObject.extras[0];
            const duration = endTime - hitTime;
            const timeElapsed = currentTime - hitTime;
            const scale = 1 - timeElapsed / duration;
            hitObject.approachSprite.scale.set(scale);

            if (currentTime > endTime) {
                removeHitObject(app, hitObject);
            }
        } else {
            const thresholdMs = 250;
            if (timeDiff <= 0 && timeDiff > -thresholdMs) {
                const alpha = 1 - Math.min(1, -timeDiff / thresholdMs);
                hitObject.hitCircleSprite.alpha = alpha;
                hitObject.hitCircleOverlaySprite.alpha = alpha;
                hitObject.comboText.alpha = alpha;
                hitObject.approachSprite.alpha = 0;

                const circleSize = (54.4 - 4.48 * beatmap["Difficulty"]["CircleSize"]) * grid_unit * 2;
                const size = circleSize + (Math.min(1, -timeDiff / thresholdMs)) * circleSize * 0.3;
                hitObject.hitCircleSprite.width = size;
                hitObject.hitCircleSprite.height = size;
                hitObject.hitCircleOverlaySprite.width = size;
                hitObject.hitCircleOverlaySprite.height = size;
            }
            if (timeDiff <= -thresholdMs) {
                removeHitObject(app, hitObject);
            }
        }
    }
}

function removeHitObject(app, hitObject) {
    app.stage.removeChild(hitObject.hitCircleSprite);
    app.stage.removeChild(hitObject.approachSprite);
    app.stage.removeChild(hitObject.hitCircleOverlaySprite);
    app.stage.removeChild(hitObject.reverseSprite);
    app.stage.removeChild(hitObject.comboText);
    hitObject.hitCircleSprite = null;
    hitObject.hitCircleOverlaySprite = null;
    hitObject.approachSprite = null;
    hitObject.reverseSprite = null;
    hitObject.comboText = null;
    app.stage.removeChild(hitObject.sliderSprite);
    app.stage.removeChild(hitObject.hitCircleOverlaySprite_sliderend);
    app.stage.removeChild(hitObject.hitCircleSprite_sliderend);
    app.stage.removeChild(hitObject.followCircle);
    hitObject.sliderSprite = null;
    hitObject.hitCircleSprite_sliderend = null;
    hitObject.hitCircleOverlaySprite_sliderend = null;
    hitObject.followCircle = null;
}

function bezierCurve(sliderInfo) {
    let anchorPositions = sliderInfo.anchorPositions;

    if (!sliderInfo.adjusted) {
        adjustAnchorPositions(anchorPositions);
        sliderInfo.adjusted = true;
    }

    let sliderTotalLength = sliderInfo.sliderLength * grid_unit;
    let totalLength = 0;
    let arrPn = [];
    let bezier = new Bezier();
    arrPn.push(new PathPoint(anchorPositions[0].x, anchorPositions[0].y));

    let sliderPath = new PIXI.Graphics();
    sliderPath.moveTo(anchorPositions[0].x, anchorPositions[0].y);

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
            let muGap = 10 / sliderTotalLength;

            let startP = new PathPoint();
            let endP = new PathPoint();

            for (let mu = 0; mu <= 1; mu += muGap) {
                startP.x = bezier.getResult().x;
                startP.y = bezier.getResult().y;
                bezier.setMu(mu);
                bezier.bezierCalc();
                endP.x = bezier.getResult().x;
                endP.y = bezier.getResult().y;

                sliderInfo.sliderEndPos = startP;

                if (endP.x && endP.y) {
                    sliderPath.lineTo(endP.x, endP.y);
                    sliderInfo.sliderEndPos = endP;
                }

                if (mu === 0) continue;

                totalLength += Math.sqrt(Math.pow(Math.abs(startP.x - endP.x), 2) + Math.pow(Math.abs(startP.y - endP.y), 2));
                if (totalLength >= sliderTotalLength) break;
            }

            arrPn = [];
            bezier = new Bezier();

            if (i < anchorPositions.length - 1) {
                let p = new PathPoint();
                p.x = prevPos.x;
                p.y = prevPos.y;
                arrPn.push(p);
            } else if (i === anchorPositions.length - 1) {
                let p = new PathPoint();
                p.x = currPos.x;
                p.y = currPos.y;
                arrPn.push(p);
            }
        }

        let p = new PathPoint();
        p.x = anchorPositions[i].x;
        p.y = anchorPositions[i].y;
        arrPn.push(p);
    }

    return sliderPath;
}

function perfectCurve(sliderInfo){
    let anchorPositions = sliderInfo.anchorPositions;

    if (!sliderInfo.adjusted) {
        adjustAnchorPositions(anchorPositions);
        sliderInfo.adjusted = true;
    }

    let sliderTotalLength = sliderInfo.sliderLength * grid_unit;
    let totalLength = 0;

    let sliderPath = new PIXI.Graphics();
    sliderPath.moveTo(anchorPositions[0].x, anchorPositions[0].y);

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

    if (!anticlockwise && (endAngle - startAngle) < 0) { endAngle += 2 * Math.PI; }
    if (anticlockwise && (endAngle - startAngle) > 0) { endAngle -= 2 * Math.PI; }

    let angleStep = (endAngle - startAngle) / 100;

    let prevX, prevY;

    for (let i = 0; i <= 100; i++) {
        const currentAngle = startAngle + angleStep * i;
        const x = circleCenter.x + radius * Math.cos(currentAngle);
        const y = circleCenter.y + radius * Math.sin(currentAngle);
        sliderPath.lineTo(x, y);

        if (i === 0) {
            prevX = x;
            prevY = y;
        } else {
            totalLength += Math.sqrt(Math.pow(Math.abs(x - prevX), 2) + Math.pow(Math.abs(y - prevY), 2));
            prevX = x;
            prevY = y;
            if (totalLength >= sliderTotalLength) break;
        }
    }

    sliderInfo.sliderEndPos = { x: prevX, y: prevY };

    return sliderPath;
}

function adjustAnchorPositions(anchorPositions) {
    for (let i = 0; i < anchorPositions.length; i++) {
        anchorPositions[i].x = top_left_x + (anchorPositions[i].x) * grid_unit;
        anchorPositions[i].y = top_left_y + (anchorPositions[i].y) * grid_unit;
    }
}

function playHitsound(normalset, additionSet, hitsound) {
    normalset = parseInt(normalset);
    additionSet = parseInt(additionSet);
    hitsound = parseInt(hitsound);

    switch (normalset) {
        case 1:
            sound.play('normal-hitnormal');
            break;
        case 2:
            sound.play('soft-hitnormal');
            break;
        case 3:
            sound.play('drum-hitnormal');
            break;
    }

    if (additionSet === 0) additionSet = normalset;

    switch (additionSet) {
        case 1:
            if (hitsound & 2) sound.play('normal-hitwhistle');
            if (hitsound & 4) sound.play('normal-hitfinish');
            if (hitsound & 8) sound.play('normal-hitclap');
            break;
        case 2:
            if (hitsound & 2) sound.play('soft-hitwhistle');
            if (hitsound & 4) sound.play('soft-hitfinish');
            if (hitsound & 8) sound.play('soft-hitclap');
            break;
        case 3:
            if (hitsound & 2) sound.play('drum-hitwhistle');
            if (hitsound & 4) sound.play('drum-hitfinish');
            if (hitsound & 8) sound.play('drum-hitclap');
            break;
    }
}

export function setVolume(volume) {
    sound.volumeAll = volume;
}