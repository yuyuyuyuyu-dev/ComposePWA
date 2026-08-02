const js = require("@eslint/js");
const globals = require("globals");
const yml = require("eslint-plugin-yml");

// @eslint/json is published as ESM; interop for this CommonJS config.
const jsonModule = require("@eslint/json");
const json = jsonModule.default ?? jsonModule;

module.exports = [
  {
    ignores: [
      // A drop-in user project: it stays exactly as the IDE template generates it, so
      // that readers can tell at a glance what the plugin produced.
      "example/**",
      // ESLint does not read .gitignore, so the git-ignored paths that hold lintable
      // files are listed here — all except the assets the plugin emits into
      // test-projects/web-app/src/*/resources/, which stay linted where the plugin wrote
      // them (.github/workflows/tests.yml relies on that). The blocks below give those
      // files their runtime globals.
      "**/build/**",
      "tmp/**",
      "**/.idea/**",
      ".claude/**",
      "package-lock.json",
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
