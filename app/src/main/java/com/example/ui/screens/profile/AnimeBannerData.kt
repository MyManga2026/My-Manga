package com.example.ui.screens.profile

data class AnimeBanner(
    val id: Int,
    val title: String,
    val anime: String,
    val category: String,
    val imageUrl: String
)

object AnimeBannerRepository {
    val categories = listOf(
        "All",
        "Popular",
        "Action & Shonen",
        "Dark & Supernatural",
        "Fantasy & Adventure",
        "Modern & Sci-Fi"
    )

    val banners = listOf(
        AnimeBanner(1, "Solo Leveling - Shadow Monarch", "Solo Leveling", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-37yfQA3ym8PA.jpg"),
        AnimeBanner(2, "Attack on Titan - Rumbling & Survey Corps", "Attack on Titan", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-8jpFCOcDmneX.jpg"),
        AnimeBanner(3, "Jujutsu Kaisen - Shibuya Incident", "Jujutsu Kaisen", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113415-jQBSkxWAAk83.jpg"),
        AnimeBanner(4, "Demon Slayer - Entertainment District", "Demon Slayer", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101922-33MtJGsUSxga.jpg"),
        AnimeBanner(5, "Chainsaw Man - Devil Hunter", "Chainsaw Man", "Dark & Supernatural", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/127230-o8IRwCGVr9KW.jpg"),
        AnimeBanner(6, "One Piece - Egghead & Wano", "One Piece", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg"),
        AnimeBanner(7, "Bleach - Thousand-Year Blood War", "Bleach", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/269-08ar2HJOUAuL.jpg"),
        AnimeBanner(8, "Naruto Shippuden - Shinobi World", "Naruto", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1735.jpg"),
        AnimeBanner(9, "Tokyo Ghoul - Anteiku & Kaneki", "Tokyo Ghoul", "Dark & Supernatural", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20605-RCJ7M71zLmrh.jpg"),
        AnimeBanner(10, "Death Note - Shinigami Realm", "Death Note", "Dark & Supernatural", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1535.jpg"),
        AnimeBanner(11, "Fullmetal Alchemist: Brotherhood", "Fullmetal Alchemist", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/5114-q0V5URebphSG.jpg"),
        AnimeBanner(12, "Frieren: Beyond Journey's End", "Frieren", "Fantasy & Adventure", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-ivXNJ23SM1xB.jpg"),
        AnimeBanner(13, "Hunter x Hunter - Chimera Ant", "Hunter x Hunter", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11061-8WkkTZ6duKpq.jpg"),
        AnimeBanner(14, "Vinland Saga - Farmland & War", "Vinland Saga", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101348-pivKKffCAwAY.jpg"),
        AnimeBanner(15, "My Hero Academia - Plus Ultra", "My Hero Academia", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21459-yeVkolGKdGUV.jpg"),
        AnimeBanner(16, "SPY x FAMILY - Operation Strix", "SPY x FAMILY", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/140960-Z7xSvkRxHKfj.jpg"),
        AnimeBanner(17, "Cyberpunk: Edgerunners - Night City", "Cyberpunk: Edgerunners", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/120377-c15oLS8CA31s.jpg"),
        AnimeBanner(18, "Neon Genesis Evangelion - Tokyo-3", "Neon Genesis Evangelion", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/30-gEMoHHIqxDgN.jpg"),
        AnimeBanner(19, "Steins;Gate - Divergence Meter", "Steins;Gate", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/n9253-JIhmKgBKsWUN.jpg"),
        AnimeBanner(20, "Mob Psycho 100 - Psycho Helmet", "Mob Psycho 100", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21507-Qx8bGsLXUgLo.jpg"),
        AnimeBanner(21, "Haikyu!! - Karasuno Fly High", "Haikyu!!", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20464-PpYjO9cPN1gs.jpg"),
        AnimeBanner(22, "Dragon Ball Super - Universe Survival", "Dragon Ball", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21175-Qo9gdGPUm4Im.jpg"),
        AnimeBanner(23, "JoJo's Bizarre Adventure - Stand Power", "JoJo's Bizarre Adventure", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/14719-m7Hv2AxcNNjM.jpg"),
        AnimeBanner(24, "Violet Evergarden - Auto Memory Doll", "Violet Evergarden", "Fantasy & Adventure", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21827-ROucgYiiiSpR.jpg"),
        AnimeBanner(25, "Oshi no Ko - B-Komachi Stage", "Oshi no Ko", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/150672-ISwoA0eS722H.jpg"),
        AnimeBanner(26, "Black Clover - Black Bulls & Grimoire", "Black Clover", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/97940-1URQdQ4U1a0b.jpg"),
        AnimeBanner(27, "One-Punch Man - Hero for Fun", "One-Punch Man", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21087-sHb9zUZFsHe1.jpg"),
        AnimeBanner(28, "Sword Art Online - Aincrad & Underworld", "Sword Art Online", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11757-TlEEV9weG4Ag.jpg"),
        AnimeBanner(29, "DAN DA DAN - Spirits & Aliens", "DAN DA DAN", "Action & Shonen", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171018-SpwPNAduszXl.jpg"),
        AnimeBanner(30, "Mushoku Tensei - Jobless Reincarnation", "Mushoku Tensei", "Fantasy & Adventure", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/108465-RgsRpTMhP9Sv.jpg"),
        AnimeBanner(31, "Dr. STONE - Kingdom of Science", "Dr. STONE", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/105333-KWKGvBM8Hyga.jpg"),
        AnimeBanner(32, "That Time I Got Reincarnated as a Slime", "Slime Isekai", "Fantasy & Adventure", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101280-9t7J3774n955.jpg"),
        AnimeBanner(33, "Cowboy Bebop - Space Cowboy", "Cowboy Bebop", "Modern & Sci-Fi", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1-OquNCNB6srGe.jpg"),
        AnimeBanner(34, "Code Geass - Lelouch of the Rebellion", "Code Geass", "Dark & Supernatural", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1575.jpg"),
        AnimeBanner(35, "Classroom of the Elite - White Room", "Classroom of the Elite", "Dark & Supernatural", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/98659-u46B5RCNl9il.jpg"),
        AnimeBanner(36, "Kaguya-sama: Love is War", "Kaguya-sama", "Popular", "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101921-GgvvFhlNhzlF.jpg")
    )

    fun getDefaultBanner(): AnimeBanner = banners.first() // Solo Leveling

    fun getValidBannerUri(savedUri: String?): String {
        if (savedUri.isNullOrBlank()) return getDefaultBanner().imageUrl
        val exists = banners.any { it.imageUrl == savedUri }
        if (exists) return savedUri
        
        // Attempt fuzzy match for anime name in URL or title
        val lowerUri = savedUri.lowercase()
        val matched = banners.firstOrNull { banner ->
            val key = banner.anime.lowercase().replace(Regex("[^a-z0-9]"), "")
            key.isNotEmpty() && lowerUri.contains(key)
        }
        return matched?.imageUrl ?: getDefaultBanner().imageUrl
    }

    fun getBannerForAnimeOrTitle(query: String): AnimeBanner {
        return banners.find {
            it.anime.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
        } ?: banners.first()
    }
}
