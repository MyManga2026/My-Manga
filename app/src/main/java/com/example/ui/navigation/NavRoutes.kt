package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Explore : Screen("explore")
    object Search : Screen("search")
    object Library : Screen("library?tab={tab}") {
        fun createRoute(tab: Int = 0) = "library?tab=$tab"
    }
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object OwnerPanel : Screen("owner_panel")
    object MangaDetail : Screen("manga_detail/{mangaId}") {
        fun createRoute(mangaId: Int) = "manga_detail/$mangaId"
    }
    object Reader : Screen("reader/{mangaId}/{chapterId}/{chapterTitle}") {
        fun createRoute(mangaId: Int, chapterId: String, chapterTitle: String): String {
            val encodedChapterId = java.net.URLEncoder.encode(chapterId, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(chapterTitle, "UTF-8")
            return "reader/$mangaId/$encodedChapterId/$encodedTitle"
        }
    }
}
