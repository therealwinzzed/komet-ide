package winzzed.komet.ide

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
