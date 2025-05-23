package io.silv.sp2yt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform