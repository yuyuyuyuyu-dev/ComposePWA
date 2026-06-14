package dev.yuyuyuyuyu.composepwaexample

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
