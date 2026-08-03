"use strict";

const assert = require("node:assert/strict");
const {
    inferUseSequence,
    useSequenceTransitions
} = require("./sap_animation_exporter.js");

const hammer = inferUseSequence([
    "use/hammer",
    "use/hammer_outro",
    "use/hammer_intro"
]);
assert.deepEqual(hammer, {
    intro: "use/hammer_intro",
    loop: "use/hammer",
    outro: "use/hammer_outro"
});
assert.deepEqual(useSequenceTransitions(hammer), [
    {from: "use/hammer_intro", to: "use/hammer", duration: 0},
    {from: "use/hammer_intro", to: "use/hammer_outro", duration: 0.08},
    {from: "use/hammer", to: "use/hammer_outro", duration: 0.08},
    {from: "use/hammer_outro", to: "use/hammer_intro", duration: 0.08}
]);

assert.equal(inferUseSequence([
    "use/hammer",
    "use/hammer_intro"
]), null);

assert.equal(inferUseSequence([
    "use/hammer",
    "use/hammer_intro",
    "use/hammer_outro",
    "use/chisel",
    "use/chisel_intro",
    "use/chisel_outro"
]), null);
