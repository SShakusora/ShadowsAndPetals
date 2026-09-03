"use strict";

/**
 * Generates and splits the curtain model family.
 *
 * Stage 0 - bake open pose: reads each part's "opening" clip keyframes and
 * the rig pivots to compute the open-pose transform of every bone, then bakes
 * the closed white masters into open-pose masters (curtain_<part>_open.json).
 * Panels rotate 22.5 (authored) + 67.5 (clip) = 90 degrees about their rig
 * pivot, which maps their boxes onto new axis-aligned boxes, so the baked
 * model needs no element rotation at all. Faces are permuted to follow the
 * rotated geometry; texture UVs stay attached to their face.
 *
 * Stage 1 - derive: reads the white masters (curtain_upper_right/left.json,
 * curtain_lower_right/left.json aggregate models and the white_curtain.json hand
 * model) and writes one file per dye color:
 *   - curtain_<half>_<side>_<color>.json        closed aggregate model
 *   - curtain_<half>_<side>_open_<color>.json   open-pose aggregate model
 *   - <color>_curtain.json                      item/hand display model
 * Color variants use a "parent" reference to the white master and only
 * override the texture, so geometry edits never touch the derived files.
 *
 * Stage 2 - split: writes the per-bone model files that
 * CurtainBlockEntityRenderer binds to the rig bones. Those live in
 * models/block/curtain/curtain_<half>_<side>[_open]_<color>/. The placed
 * curtain is rendered through them, so after any geometry edit re-run this
 * script.
 *
 * Usage:  node tools/curtain/split_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals", "models", "block", "curtain"
);
const animDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals"
);

const COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
    "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
];

const NS = "shadowsandpetals:block/curtain/";
const WHITE_TEXTURE = NS + "white";

/**
 * Aggregate part definitions: source file base name (the white master) and
 * the element-index -> rig-bone mapping verified against each part's
 * bbmodel outliner. Keep the element order in the aggregate model stable.
 */
const PARTS = [
    {
        name: "curtain_upper_right",
        // Element 9 is the rail-end outcrop; it rides the static rail bone.
        boneOfElement: ["panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail"]
    },
    {
        name: "curtain_lower_right",
        // Lower panels bind straight to the panel bones.
        boneOfElement: ["panel_1", "panel_2", "panel_3", "panel_4"]
    },
    {
        name: "curtain_upper_left",
        boneOfElement: ["panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail"]
    },
    {
        name: "curtain_lower_left",
        // Left mirrors bind like the right ones.
        boneOfElement: ["panel_1", "panel_2", "panel_3", "panel_4"]
    }
];

// Clip resource per part used to bake the open pose.
const OPEN_CLIPS = {
    curtain_upper_right: "neoforge/animations/entity/curtain/upper_right/opening.json",
    curtain_lower_right: "neoforge/animations/entity/curtain/lower_right/opening.json",
    curtain_upper_left: "neoforge/animations/entity/curtain/upper_left/opening.json",
    curtain_lower_left: "neoforge/animations/entity/curtain/lower_left/opening.json"
};

// Rig resource per part (pivots for baking rotations).
const RIGS = {
    curtain_upper_right: "sap/animations/rigs/curtain/upper_right.json",
    curtain_lower_right: "sap/animations/rigs/curtain/lower_right.json",
    curtain_upper_left: "sap/animations/rigs/curtain/upper_left.json",
    curtain_lower_left: "sap/animations/rigs/curtain/lower_left.json"
};

function fail(message) {
    console.error("split_curtain_model: " + message);
    process.exit(1);
}

function readMaster(partName) {
    const file = path.join(curtainDir, partName + ".json");
    if (!fs.existsSync(file)) {
        fail(partName + ".json not found");
    }
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function readJson(relPath) {
    return JSON.parse(fs.readFileSync(path.join(animDir, relPath), "utf8"));
}

function writeModel(file, model) {
    fs.writeFileSync(file, JSON.stringify(model, null, 2) + "\n");
}

/**
 * Returns the texture keys of the master that point at the white curtain
 * texture; color variants must override exactly those keys.
 */
function whiteTextureKeys(model) {
    return Object.keys(model.textures).filter(key => model.textures[key] === WHITE_TEXTURE);
}

// ---------------------------------------------------------------------------
// Stage 0: bake the open pose.
// ---------------------------------------------------------------------------

/**
 * Face permutation under a Y rotation. The element face that pointed along
 * the old direction now points along the rotated direction; up and down
 * faces keep their direction.
 */
function rotateDirection(direction, degrees) {
    // +90: north -> east, east -> south, south -> west, west -> north.
    const clockwise = { north: "east", east: "south", south: "west", west: "north" };
    if (direction === "up" || direction === "down") {
        return direction;
    }
    let steps = 0;
    if (degrees > 0) {
        steps = Math.round(degrees / 90) % 4;
    } else {
        steps = (4 - Math.round(-degrees / 90) % 4) % 4;
    }
    let dir = direction;
    for (let i = 0; i < steps; i++) {
        dir = clockwise[dir];
    }
    return dir;
}

function rotateElement(element, bonePose, pivot) {
    const degrees = (bonePose.ry || 0) + (element.rotation && element.rotation.axis === "y"
        ? element.rotation.angle : 0);
    const tx = (bonePose.tx || 0);
    const ty = (bonePose.ty || 0);
    const tz = (bonePose.tz || 0);

    const corners = [];
    for (const x of [element.from[0], element.to[0]]) {
        for (const y of [element.from[1], element.to[1]]) {
            for (const z of [element.from[2], element.to[2]]) {
                corners.push(rotYAbout([x, y, z], pivot, degrees));
            }
        }
    }
    const xs = corners.map(c => c[0]);
    const ys = corners.map(c => c[1]);
    const zs = corners.map(c => c[2]);
    const out = {
        name: element.name,
        from: [
            Math.min(...xs) + tx,
            Math.min(...ys) + ty,
            Math.min(...zs) + tz
        ],
        to: [
            Math.max(...xs) + tx,
            Math.max(...ys) + ty,
            Math.max(...zs) + tz
        ]
    };
    if (element.rotation && (element.rotation.axis !== "y" || degrees === 0)) {
        // Non-Y rotations (hanging rings) keep their authored rotation.
        out.rotation = element.rotation;
    }
    if (degrees % 360 !== 0) {
        const faces = {};
        for (const [direction, face] of Object.entries(element.faces)) {
            faces[rotateDirection(direction, degrees)] = face;
        }
        out.faces = faces;
    } else {
        out.faces = element.faces;
    }
    return out;
}

/**
 * Reads the open clip and returns, per bone name, the final keyframe values:
 * a translation in model pixels and a rotation in degrees.
 */
function openPoseOf(part) {
    const clip = readJson(OPEN_CLIPS[part.name]);
    const pose = new Map();
    for (const anim of clip.animations) {
        const last = anim.keyframes[anim.keyframes.length - 1].target;
        if (anim.target === "minecraft:position") {
            pose.set(anim.bone, { tx: last[0], ty: last[1], tz: last[2], ry: 0 });
        } else if (anim.target === "minecraft:rotation") {
            const prev = pose.get(anim.bone) || { tx: 0, ty: 0, tz: 0 };
            pose.set(anim.bone, {
                tx: prev.tx, ty: prev.ty, tz: prev.tz,
                ry: last[1]
            });
        }
    }
    return pose;
}

function rigPivotOf(part, boneName) {
    const rig = readJson(RIGS[part.name]);
    const bone = rig.bones.find(b => b.name === boneName);
    if (!bone) {
        fail(RIGS[part.name] + " has no bone " + boneName);
    }
    return bone.pivot;
}

function rotYAbout(point, pivot, degrees) {
    const radians = (degrees * Math.PI) / 180;
    const cos = Math.cos(radians);
    const sin = Math.sin(radians);
    const dx = point[0] - pivot[0];
    const dz = point[2] - pivot[2];
    return [
        pivot[0] + dx * cos + dz * sin,
        point[1],
        pivot[2] - dx * sin + dz * cos
    ];
}

function bakeOpenMasters() {
    for (const part of PARTS) {
        const master = readMaster(part.name);
        const pose = openPoseOf(part);
        const rig = readJson(RIGS[part.name]);
        const parentOf = new Map();
        for (const bone of rig.bones) {
            if (bone.parent) {
                parentOf.set(bone.name, bone.parent);
            }
        }
        // Collapse each bone's full parent chain into one composed transform:
        // translations of the bone and all its ancestors accumulate, and the
        // bone itself contributes its rotation.
        const boneTransform = new Map();
        for (const boneName of pose.keys()) {
            let tx = 0;
            let ty = 0;
            let tz = 0;
            let ry = 0;
            let pivotBone = boneName;
            for (let current = boneName; current; current = parentOf.get(current)) {
                const bonePose = pose.get(current);
                if (!bonePose) {
                    continue;
                }
                tx += bonePose.tx;
                ty += bonePose.ty;
                tz += bonePose.tz;
                if (current === boneName) {
                    ry = bonePose.ry;
                } else if (tx !== 0 || ty !== 0 || tz !== 0) {
                    pivotBone = current;
                }
            }
            if (tx === 0 && ty === 0 && tz === 0 && ry === 0) {
                continue;
            }
            boneTransform.set(boneName, { tx, ty, tz, ry, pivotBone });
        }

        const elements = master.elements.map((element, index) => {
            const boneName = part.boneOfElement[index];
            const bonePose = boneTransform.get(boneName);
            if (!bonePose) {
                return element;
            }
            // Rotations happen about the animated ancestor's pivot; the rig
            // pivot equals the panel element's model-space origin.
            const pivot = element.rotation && element.rotation.axis === "y"
                ? element.rotation.origin
                : rigPivotOf(part, bonePose.pivotBone);
            return rotateElement(element, bonePose, pivot);
        });

        const openModel = { textures: master.textures, elements };
        writeModel(path.join(curtainDir, part.name + "_open.json"), openModel);
        console.log("bake: " + part.name + " -> open-pose master with " + elements.length + " elements");
    }
}

// ---------------------------------------------------------------------------
// Stage 1: derive the per-color aggregate and hand models from the masters.
// ---------------------------------------------------------------------------

function deriveColorVariants() {
    for (const base of PARTS.flatMap(part => [part.name, part.name + "_open"])) {
        const master = readMaster(base);
        const expected = PARTS.find(p => base === p.name || base === p.name + "_open");
        if (!Array.isArray(master.elements) || master.elements.length !== expected.boneOfElement.length) {
            fail(base + ".json: expected " + expected.boneOfElement.length + " elements, found "
                + (Array.isArray(master.elements) ? master.elements.length : "none"));
        }
        const keys = whiteTextureKeys(master);
        if (keys.length === 0) {
            fail(base + ".json does not reference the white curtain texture");
        }
        for (const color of COLORS) {
            if (color === "white") {
                // The white master already is the white variant.
                continue;
            }
            const variant = { parent: "shadowsandpetals:block/curtain/" + base, textures: {} };
            for (const key of keys) {
                variant.textures[key] = NS + color;
            }
            writeModel(path.join(curtainDir, base + "_" + color + ".json"), variant);
        }
        console.log("derive: " + base + " -> " + (COLORS.length - 1) + " color aggregates");
    }

    const handMaster = readMaster("white_curtain");
    const handKeys = whiteTextureKeys(handMaster);
    if (handKeys.length === 0) {
        fail("white_curtain.json does not reference the white curtain texture");
    }
    for (const color of COLORS) {
        if (color === "white") {
            continue;
        }
        const variant = {
            parent: "shadowsandpetals:block/curtain/white_curtain",
            textures: {}
        };
        for (const key of handKeys) {
            variant.textures[key] = NS + color;
        }
        writeModel(path.join(curtainDir, color + "_curtain.json"), variant);
    }
    console.log("derive: white_curtain -> " + (COLORS.length - 1) + " color hand models");
}

// ---------------------------------------------------------------------------
// Stage 2: split each aggregate model into per-bone files for the renderer.
// ---------------------------------------------------------------------------

function splitParts() {
    for (const part of PARTS) {
        for (const base of [part.name, part.name + "_open"]) {
            const master = readMaster(base);
            const keys = whiteTextureKeys(master);
            const byBone = new Map();
            master.elements.forEach((element, index) => {
                const bone = part.boneOfElement[index];
                if (!byBone.has(bone)) byBone.set(bone, []);
                byBone.get(bone).push(element);
            });
            for (const color of COLORS) {
                const bonesDir = path.join(curtainDir, base + (color === "white" ? "" : "_" + color));
                const retexture = color !== "white" && keys.length > 0;
                for (const [bone, elements] of byBone) {
                    const out = retexture
                        ? { textures: retextureMap(master, keys, color), elements }
                        : { textures: master.textures, elements };
                    fs.mkdirSync(bonesDir, { recursive: true });
                    writeModel(path.join(bonesDir, bone + ".json"), out);
                }
            }
            console.log("split_curtain_model: " + base + " -> wrote "
                + byBone.size + " per-bone models for " + COLORS.length + " colors (bones: "
                + [...byBone.keys()].join(", ") + ")");
        }
    }
}

function retextureMap(master, keys, color) {
    const textures = Object.assign({}, master.textures);
    for (const key of keys) {
        textures[key] = NS + color;
    }
    return textures;
}

bakeOpenMasters();
deriveColorVariants();
splitParts();
