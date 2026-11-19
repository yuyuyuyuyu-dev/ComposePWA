package dev.yuyuyuyuyu.composepwa

import org.gradle.api.provider.Property

abstract class ComposePwaExtension {
    abstract val workboxMode: Property<WorkboxMode>
}
