"use strict";

/**
 * Derives the LEFT curtain's quadrant models by mirroring the RIGHT curtain's
 * hand-authored quadrants: x -> 16 - x per element (and per rotation origin),
 * east/west faces swap with their U axis flipped, and authored Y rotations
 * negate. X-rotated rings keep their angle (mirroring a Y-plane preserves an
 * X-axis rotation's sense within the swapped frame).
 *
 * Inputs (authored, RIGHT curtain):
 *   large_curtain_right_{l1,l2,r1,r2}.json
 *   large_curtain_right_open_{l1,l2,r1,r2}.json
 * Outputs (experimental, LEFT curtain):
 *   large_curtain_left_{l1,l2,r1,r2}.json
 *   large_curtain_left_open_{l1,l2,r1,r2}.json
 *
 * The LEFT curtain is the mirror partner: its bunching (outer) column is the
 * mirrored one, so quadrant l/r swap roles — the mirrored r-quadrants become
 * the LEFT curtain's outer column files.
 *
 * Usage:  node tools/curtain/mirror_large_curtain.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const dir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals", "models", "block", "large_curtain"
);

const QUADRANTS = ["l1", "l2", "r1", "r2"];
const FACE_MIRROR = { north: "north", south: "south", east: "west", west: "east", up: "up", down: "down" };

function mirrorPoint(point) {
    return [16 - point[0], point[1], point[2]];
}

function mirrorRange(from, to) {
    const a = mirrorPoint(from);
    const b = mirrorPoint(to);
    return [
        [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])],
        [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])]
    ];
}

function mirrorFace(face) {
    const out = JSON.parse(JSON.stringify(face));
    if (Array.isArray(out.uv) && out.uv.length === 4) {
        const [u0, v0, u1, v1] = out.uv;
        out.uv = [16 - u1, v0, 16 - u0, v1];
    }
    return out;
}

function mirrorElement(element) {
    const out = JSON.parse(JSON.stringify(element));
    [out.from, out.to] = mirrorRange(out.from, out.to);
    if (out.rotation) {
        out.rotation.origin = mirrorPoint(out.rotation.origin);
        if (out.rotation.axis === "y") {
            out.rotation.angle = -out.rotation.angle;
        }
    }
    if (out.faces) {
        const faces = {};
        for (const [direction, face] of Object.entries(out.faces)) {
            faces[FACE_MIRROR[direction] || direction] = mirrorFace(face);
        }
        out.faces = faces;
    }
    return out;
}

function mirrorModel(model) {
    return {
        textures: model.textures,
        elements: (model.elements || []).map(mirrorElement)
    };
}

function main() {
    for (const quadrant of QUADRANTS) {
        for (const pose of ["", "open_"]) {
            const sourceName = "large_curtain_right_" + pose + quadrant + ".json";
            const targetName = "large_curtain_left_" + pose + quadrant + ".json";
            const source = JSON.parse(fs.readFileSync(path.join(dir, sourceName), "utf8"));
            const mirrored = mirrorModel(source);
            fs.writeFileSync(path.join(dir, targetName), JSON.stringify(mirrored, null, 2) + "\n");
            console.log("mirror: " + sourceName + " -> " + targetName
                    + " (" + (mirrored.elements || []).length + " elements)");
        }
    }
}

main();