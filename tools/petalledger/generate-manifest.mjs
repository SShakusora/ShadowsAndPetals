#!/usr/bin/env node

/**
 * Build the data consumed by Petal-Ledger from generated language resources.
 *
 * The generator deliberately reads the data-generator output instead of trying
 * to infer registrations with regular expressions.  Run it after
 * `gradlew.bat runClientData` (or point --resources at another generated
 * resource directory):
 *
 *   node tools/petalledger/generate-manifest.mjs > tools/petalledger/data/repository-manifest.js
 */

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = path.resolve(import.meta.dirname, "../..");
const args = new Map();
for (let i = 2; i < process.argv.length; i += 1) {
  if (process.argv[i].startsWith("--")) args.set(process.argv[i], process.argv[i + 1]);
}

const resourceRoot = path.resolve(root, args.get("--resources") ?? "src/generated/resources");
const namespace = args.get("--namespace") ?? "shadowsandpetals";
const locales = (args.get("--locales") ?? "en_us,zh_cn").split(",").map(value => value.trim()).filter(Boolean);
const langDir = path.join(resourceRoot, "assets", namespace, "lang");

function readLocale(locale) {
  const file = path.join(langDir, `${locale}.json`);
  if (!fs.existsSync(file)) return {};
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

const translations = Object.fromEntries(locales.map(locale => [locale, readLocale(locale)]));
const allKeys = [...new Set(locales.flatMap(locale => Object.keys(translations[locale])))].sort();
const baseLocale = locales[0] ?? "en_us";

function classify(key) {
  const match = key.match(/^(item|block)\.([^\.]+)\.([^\.]+)$/);
  if (match) return { type: match[1], id: match[3], context: match[1] };
  const tooltip = key.match(/^(item|block)\.([^\.]+)\.([^\.]+)\.tooltip\./);
  if (tooltip) {
    return {
      type: "language",
      id: key,
      context: "tooltip",
      ownerKey: `item.${tooltip[2]}.${tooltip[3]}`,
    };
  }
  const prefix = key.split(".")[0];
  const contexts = {
    advancements: "advancement",
    config: "config",
    container: "container",
    entity: "entity",
    fluid_type: "fluid",
    itemGroup: "creative_tab",
    jade: "jade",
    jei: "jei",
    message: "message",
    subtitles: "subtitle",
    tooltip: "tooltip",
  };
  return { type: "language", id: key, context: contexts[prefix] ?? "language" };
}

function valuesFor(key) {
  return Object.fromEntries(locales.map(locale => [locale, translations[locale][key] ?? ""]));
}

function tooltipFor(baseKey) {
  const prefix = `${baseKey}.tooltip.`;
  const keys = allKeys.filter(key => key.startsWith(prefix));
  if (!keys.length) return null;
  const result = { summary: null, behaviours: [], actions: [] };
  for (const key of keys) {
    const suffix = key.slice(prefix.length);
    const match = suffix.match(/^(condition|behaviour|control|action)(\d+)$/);
    if (suffix === "summary") result.summary = { key, values: valuesFor(key) };
    else if (match) {
      const [, kind, index] = match;
      const target = kind === "condition" || kind === "behaviour" ? result.behaviours : result.actions;
      let row = target.find(entry => entry.index === Number(index));
      if (!row) {
        row = { index: Number(index), condition: null, text: null };
        target.push(row);
      }
      const field = kind === "condition" || kind === "control" ? "condition" : "text";
      row[field] = { key, values: valuesFor(key) };
    }
  }
  result.behaviours.sort((a, b) => a.index - b.index);
  result.actions.sort((a, b) => a.index - b.index);
  return result;
}

function sourceHint(entry, key) {
  if (entry.type === "item" || key.startsWith(`item.${namespace}.`)) {
    return "src/main/java/com/sshakusora/shadowsandpetals/registries/ItemRegistry.java";
  }
  if (entry.type === "block" || key.startsWith(`block.${namespace}.`)) {
    return "src/main/java/com/sshakusora/shadowsandpetals/registries/BlockRegistry.java";
  }
  return "src/main/java/com/sshakusora/shadowsandpetals/data/BuiltinLanguageKeys.java";
}

const entries = allKeys.map(key => {
  const classified = classify(key);
  const isContent = classified.type === "item" || classified.type === "block";
  const entry = {
    key,
    id: classified.id,
    type: classified.type,
    context: classified.context,
    values: valuesFor(key),
    source: sourceHint({ type: classified.type }, key),
  };
  if (classified.ownerKey) entry.ownerKey = classified.ownerKey;
  if (isContent) {
    entry.displayNameKey = key;
    entry.tooltip = classified.type === "item" ? tooltipFor(key) : null;
    const icon = path.join(root, "src", "main", "resources", "assets", namespace, "textures", "item", `${classified.id}.png`);
    const blockIcon = path.join(root, "src", "main", "resources", "assets", namespace, "textures", "block", `${classified.id}.png`);
    if (fs.existsSync(icon)) entry.iconSource = `../../src/main/resources/assets/${namespace}/textures/item/${classified.id}.png`;
    else if (fs.existsSync(blockIcon)) entry.iconSource = `../../src/main/resources/assets/${namespace}/textures/block/${classified.id}.png`;
    if (classified.id.includes("wind_chime")) entry.dynamic = { kind: "wind_chime_colors" };
    if (classified.id.startsWith("rockery_")) {
      const dimensions = classified.id.slice("rockery_".length).split("_").map(Number);
      entry.component = { kind: "rockery_preview", minimumWidth: 100, dimensions };
    }
  }
  return entry;
});

const commit = (() => {
  try { return execFileSync("git", ["rev-parse", "HEAD"], { cwd: root, encoding: "utf8" }).trim(); }
  catch { return "unknown"; }
})();
const branch = (() => {
  try { return execFileSync("git", ["branch", "--show-current"], { cwd: root, encoding: "utf8" }).trim(); }
  catch { return "unknown"; }
})();

const manifest = {
  schemaVersion: 2,
  repository: { owner: "SShakusora", name: "ShadowsAndPetals", branch, commit, namespace },
  generatedAt: new Date().toISOString(),
  locales,
  baseLocale,
  entries,
};

process.stdout.write(`window.__TEXTLEDGER_MANIFEST__ = ${JSON.stringify(manifest)};\n`);
