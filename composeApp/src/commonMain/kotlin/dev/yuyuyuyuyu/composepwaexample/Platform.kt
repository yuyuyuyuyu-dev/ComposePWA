package dev.yuyuyuyuyu.composepwaexample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform