"use strict";

/**
 * Generates and splits the curtain model family.
 *
 * Stage 1 - derive: reads the white masters (curtain_upper_r/l.json,
 * curtain_lower_r/l.json aggregate models and the white_curtain.json hand
 * model) and writes one file per dye color:
 *   - curtain_<half>_<side>_<color>.json   placed-block aggregate model
 *   - <color>_curtain.json                  item/hand display model
 * Color variants use a "parent" reference to the white master and only
 * override the texture, so geometry edits never touch the derived files.
 *
 * Stage 2 - split: writes the per-bone model files that
 * CurtainBlockEntityRenderer binds to the rig bones. Those live in
 * models/block/curtain/curtain_<half>_<side>_<color>/. The placed curtain
 * is rendered through them, so after any geometry edit re-run this script.
 *
 * Usage:  node tools/curtain/split_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals", "models", "block", "curtain"
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
        name: "curtain_upper_r",
        // Element 9 is the rail-end outcrop; it rides the static rail bone.
        boneOfElement: ["g1_1", "g1", "g2_1", "g2", "g3_1", "g3", "g4_1", "g4", "group", "group"]
    },
    {
        name: "curtain_lower_r",
        // Lower panels bind straight to the g1..g4 bones; each panel carries
        // both the gx translation and the gx_1 rotation of the upper rig.
        boneOfElement: ["g1", "g2", "g3", "g4"]
    },
    {
        name: "curtain_upper_l",
        boneOfElement: ["g1_1", "g1", "g2_1", "g2", "g3_1", "g3", "g4_1", "g4", "group", "group"]
    },
    {
        name: "curtain_lower_l",
        boneOfElement: ["g1", "g2", "g3", "g4"]
    }
];

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

/** Stage 1: derive the per-color aggregate and hand models from the masters. */
function deriveColorVariants() {
    for (const part of PARTS) {
        const master = readMaster(part.name);
        if (!Array.isArray(master.elements) || master.elements.length !== part.boneOfElement.length) {
            fail(part.name + ".json: expected " + part.boneOfElement.length + " elements, found "
                + (Array.isArray(master.elements) ? master.elements.length : "none"));
        }
        const keys = whiteTextureKeys(master);
        if (keys.length === 0) {
            fail(part.name + ".json does not reference the white curtain texture");
        }
        for (const color of COLORS) {
            const variant = { parent: "shadowsandpetals:block/curtain/" + part.name, textures: {} };
            for (const key of keys) {
                variant.textures[key] = NS + color;
            }
            if (color === "white") {
                // The white master already is the white variant.
                continue;
            }
            writeModel(path.join(curtainDir, part.name + "_" + color + ".json"), variant);
        }
        console.log("derive: " + part.name + " -> " + (COLORS.length - 1) + " color aggregates");
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

/**
 * Stage 2: split each aggregate model into per-bone files for the renderer.
 * White variants split from the white master directly; other colors resolve
 * the parent chain (variant -> master) and retexture the white texture keys.
 */
function splitParts() {
    for (const part of PARTS) {
        const master = readMaster(part.name);
        const keys = whiteTextureKeys(master);
        const byBone = new Map();
        master.elements.forEach((element, index) => {
            const bone = part.boneOfElement[index];
            if (!byBone.has(bone)) byBone.set(bone, []);
            byBone.get(bone).push(element);
        });
        for (const color of COLORS) {
            const bonesDir = path.join(curtainDir, part.name + (color === "white" ? "" : "_" + color));
            const retexture = color !== "white" && keys.length > 0;
            for (const [bone, elements] of byBone) {
                const out = retexture
                    ? { textures: retextureMap(master, keys, color), elements }
                    : { textures: master.textures, elements };
                fs.mkdirSync(bonesDir, { recursive: true });
                writeModel(path.join(bonesDir, bone + ".json"), out);
            }
        }
        console.log("split_curtain_model: " + part.name + " -> wrote "
            + byBone.size + " per-bone models for " + COLORS.length + " colors (bones: "
            + [...byBone.keys()].join(", ") + ")");
    }
}

function retextureMap(master, keys, color) {
    const textures = Object.assign({}, master.textures);
    for (const key of keys) {
        textures[key] = NS + color;
    }
    return textures;
}

deriveColorVariants();
splitParts();