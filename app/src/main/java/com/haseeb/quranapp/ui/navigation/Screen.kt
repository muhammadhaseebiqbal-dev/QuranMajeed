package com.haseeb.quranapp.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Ask : Screen("ask")
    object Settings : Screen("settings")
    object Reader : Screen("reader/{surahId}?scrollToAyah={scrollToAyah}") {
        fun createRoute(surahId: Int, scrollToAyah: Int = -1) = 
            if (scrollToAyah != -1) "reader/$surahId?scrollToAyah=$scrollToAyah" else "reader/$surahId"
    }
    object JuzReader : Screen("juzReader/{juzId}?scrollToAyah={scrollToAyah}") {
        fun createRoute(juzId: Int, scrollToAyah: Int = -1) = 
            if (scrollToAyah != -1) "juzReader/$juzId?scrollToAyah=$scrollToAyah" else "juzReader/$juzId"
    }
}
