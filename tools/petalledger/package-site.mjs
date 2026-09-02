#!/usr/bin/env node

/**
 * Assemble a self-contained Petal-Ledger site for static hosting.
 *
 * The tracked manifest intentionally points at source-tree textures so that
 * the page is useful while it is opened from this repository.  A deployed
 * site must not depend on that checkout, so this script copies only the
 * referenced PNGs and rewrites their URLs to the site's local assets folder.
 */

import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "../..");
const siteDir = path.resolve(root, "build/textledger-site");
const pageFile = path.resolve(root, "tools/petalledger/Petal-Ledger.html");
const manifestFile = path.resolve(root, "tools/petalledger/data/repository-manifest.js");

function readManifest(file) {
  const source = fs.readFileSync(file, "utf8");
  const marker = "window.__TEXTLEDGER_MANIFEST__ =";
  const start = source.indexOf(marker);
  if (start < 0) throw new Error(`Manifest marker not found in ${file}`);
  const json = source.slice(start + marker.length).trim().replace(/;\s*$/, "");
  return JSON.parse(json);
}

function writeManifest(file, manifest) {
  fs.writeFileSync(file, `window.__TEXTLEDGER_MANIFEST__ = ${JSON.stringify(manifest)};\n`, "utf8");
}

function sourceTexturePath(entry) {
  if (!entry.iconSource) return null;
  const source = path.resolve(root, "tools/petalledger", entry.iconSource);
  const relative = path.relative(root, source);
  if (relative.startsWith("..") || path.isAbsolute(relative)) return null;
  return source;
}

function deployedTexturePath(source) {
  const relative = path.relative(path.join(root, "src/main/resources"), source);
  if (relative.startsWith("..") || path.isAbsolute(relative)) return null;
  return path.join(siteDir, relative.replace(/^assets[\\/]/, "assets/"));
}

fs.rmSync(siteDir, { recursive: true, force: true });
fs.mkdirSync(path.join(siteDir, "data"), { recursive: true });
fs.copyFileSync(pageFile, path.join(siteDir, "index.html"));

const manifest = readManifest(manifestFile);
manifest.assetPrefix = "assets/";
let copied = 0;
for (const entry of manifest.entries ?? []) {
  const source = sourceTexturePath(entry);
  const destination = source && deployedTexturePath(source);
  if (!source || !destination || !fs.existsSync(source)) {
    delete entry.iconSource;
    continue;
  }
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.copyFileSync(source, destination);
  entry.iconSource = path.posix.join("assets", path.relative(path.join(siteDir, "assets"), destination).split(path.sep).join("/"));
  copied += 1;
}

writeManifest(path.join(siteDir, "data/repository-manifest.js"), manifest);
console.log(`Petal-Ledger site written to ${path.relative(root, siteDir)} (${manifest.entries?.length ?? 0} entries, ${copied} icons).`);
