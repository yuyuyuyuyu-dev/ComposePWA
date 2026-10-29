const js = require("@eslint/js");
const globals = require("globals");
const jsonModule = require("@eslint/json");
const json = jsonModule.default ?? jsonModule;

module.exports = [
	{
		ignores: [
			"**/*.js",
			"**/*.json",
			"!plugin/src/main/resources/manifest.json",
			"!plugin/src/main/resources/registerServiceWorker.js",
			"!plugin/src/main/resources/workbox-config-for-js.js",
			"!plugin/src/main/resources/workbox-config-for-wasm.js",
		],
	},
	{
		files: ["**/*.js"],
		...js.configs.recommended,
	},
	{
		files: ["**/registerServiceWorker.js"],
		languageOptions: {
			sourceType: "script",
			globals: { ...globals.browser },
		},
	},
	{
		files: ["**/workbox-config-*.js"],
		languageOptions: {
			sourceType: "commonjs",
			globals: { ...globals.node },
		},
	},
	{
		files: ["**/*.json"],
		language: "json/json",
		...json.configs.recommended,
	},
];
