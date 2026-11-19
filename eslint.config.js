const js = require("@eslint/js");
const globals = require("globals");
const jsonModule = require("@eslint/json");
const json = jsonModule.default ?? jsonModule;

module.exports = [
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
		files: ["**/serviceWorker.js"],
		languageOptions: {
			sourceType: "script",
			globals: { ...globals.serviceworker, workbox: "readonly" },
		},
	},
	{
		files: ["**/workbox-config-*.js", "**/workbox-inject-manifest-config-*.js"],
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
