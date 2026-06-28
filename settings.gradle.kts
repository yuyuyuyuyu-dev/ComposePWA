rootProject.name = "ComposePWA"

// This repository is a thin umbrella: the Gradle plugin lives in `plugin/` and the
// sample consumer in `example/`, each a standalone build with its own version catalog.
// Including the plugin build here lets `./gradlew :plugin:publishPlugins` run from the root.
includeBuild("plugin")
