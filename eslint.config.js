const js = require("@eslint/js");
const globals = require("globals");
const yml = require("eslint-plugin-yml");

// @eslint/json is published as ESM; interop for this CommonJS config.
const jsonModule = require("@eslint/json");
const json = jsonModule.default ?? jsonModule;

module.exports = [
  {
    ignores: [
      "**/build/**",
      "**/node_modules/**",
      ".gradle/**",
      ".idea/**",
      ".kotlin/**",
      ".claude/**",
      "kotlin-js-store/**",
      "package-lock.json",
      // The example app is a faithful, drop-in user project; its plugin OUTPUTS are
      // verified separately (ComposePWA output CI job). "tmp/" is a local scratch dir.
      "example/**",
      "tmp/**",
      // NOTE: the JS assets the ComposePWA plugin emits (registerServiceWorker.js,
      // workbox-config-*.js) are intentionally NOT ignored — they are checked by the
      // "ComposePWA output" CI job, which configures them as browser/Node scripts below.
    ],
  },

  // JavaScript
  {
    files: ["**/*.js"],
    ...js.configs.recommended,
  },
  {
    // Browser-side service worker registration script.
    files: ["**/registerServiceWorker.js"],
    languageOptions: {
      sourceType: "script",
      globals: { ...globals.browser },
    },
  },
  {
    // Node/CommonJS configuration files.
    files: ["**/workbox-config-*.js", "eslint.config.js"],
    languageOptions: {
      sourceType: "commonjs",
      globals: { ...globals.node },
    },
  },

  // JSON
  {
    files: ["**/*.json"],
    language: "json/json",
    ...json.configs.recommended,
  },
  {
    files: ["**/*.jsonc"],
    language: "json/jsonc",
    ...json.configs.recommended,
  },

  // YAML (formatting rules are turned off so Prettier owns formatting)
  ...yml.configs["flat/recommended"],
  ...yml.configs["flat/prettier"],
  {
    files: ["**/*.{yml,yaml}"],
    rules: {
      // `workflow_dispatch:` and similar empty values are valid GitHub Actions idioms.
      "yml/no-empty-mapping-value": "off",
    },
  },
];
