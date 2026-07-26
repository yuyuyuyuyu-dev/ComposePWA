// The root project builds nothing; this file exists only because the version
// catalog formatter has to be applied to the project that owns the catalog.
plugins {
    alias(libs.plugins.versionCatalogUpdate)
}
