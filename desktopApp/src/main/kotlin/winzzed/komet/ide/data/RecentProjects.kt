package winzzed.komet.ide.data


object RecentProjects {
    private const val MAX_RECENT = 10

    fun get(): List<String> = RecentProjectsStore.get().take(MAX_RECENT)

    fun add(path: String) {
        val current = get().toMutableList()
        current.remove(path)
        current.add(0, path)
        while (current.size > MAX_RECENT) current.removeAt(current.size - 1)
        RecentProjectsStore.save(current)
    }

    fun remove(path: String) {
        RecentProjectsStore.save(get().filter { it != path })
    }
}
