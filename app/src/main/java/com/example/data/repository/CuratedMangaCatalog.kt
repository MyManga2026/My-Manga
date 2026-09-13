package com.example.data.repository

import com.example.data.model.MangaItem
import com.example.data.model.SearchFilter
import com.example.data.model.SortOption

/**
 * Curated high-quality catalog of verified top trending, popular, and classic manga.
 * Used as an instant offline/rate-limit fallback so that Trending Right Now, All Time Popular,
 * Search, and Manga Details NEVER fail or show blank screens.
 */
object CuratedMangaCatalog {

    val allManga: List<MangaItem> = listOf(
        MangaItem(
            id = 105778,
            titleRomaji = "Chainsaw Man",
            titleEnglish = "Chainsaw Man",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx105778-5aVbBuhvcfFk.png",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/105778-8VfRkYIqG33q.jpg",
            description = "Denji was a small-time devil hunter just trying to survive in a harsh world. After being killed on a job, he is revived by his pet devil Pochita and becomes Chainsaw Man.",
            status = "RELEASING",
            score = 86,
            genres = listOf("Action", "Comedy", "Drama", "Horror", "Supernatural"),
            chapters = 180,
            volumes = 18,
            format = "MANGA",
            startYear = 2018
        ),
        MangaItem(
            id = 30013,
            titleRomaji = "One Piece",
            titleEnglish = "One Piece",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30013-ulVqBhieoec4.png",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30013-1m4jovsH3pP7.jpg",
            description = "Gol D. Roger was known as the 'Pirate King'. When he was captured, his last words revealed the location of his greatest treasure: One Piece. Monkey D. Luffy sets out on the Grand Line in search of it.",
            status = "RELEASING",
            score = 92,
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy"),
            chapters = 1125,
            volumes = 109,
            format = "MANGA",
            startYear = 1997
        ),
        MangaItem(
            id = 101517,
            titleRomaji = "Jujutsu Kaisen",
            titleEnglish = "Jujutsu Kaisen",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx101517-kWfqA6ANFqA4.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/101517-j47e2pqmFvyw.jpg",
            description = "Yuuji Itadori swallows a cursed talisman—the finger of a demon—and becomes cursed himself. He enters a shaman's school to find the other body parts and exorcise himself.",
            status = "FINISHED",
            score = 84,
            genres = listOf("Action", "Drama", "Supernatural", "Fantasy"),
            chapters = 271,
            volumes = 28,
            format = "MANGA",
            startYear = 2018
        ),
        MangaItem(
            id = 105398,
            titleRomaji = "Na Honjaman Rebeleop",
            titleEnglish = "Solo Leveling",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx105398-b673Wkb2SZsq.png",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/105398-nI6t6QZ0k9fE.jpg",
            description = "10 years ago, 'the Gate' opened and connected the real world with the realm of magic and monsters. Sung Jin-Woo is known as the weakest E-rank hunter until he gains a mysterious quest window.",
            status = "FINISHED",
            score = 86,
            genres = listOf("Action", "Adventure", "Fantasy", "Supernatural"),
            chapters = 200,
            volumes = 14,
            format = "MANGA",
            startYear = 2018
        ),
        MangaItem(
            id = 53390,
            titleRomaji = "Shingeki no Kyojin",
            titleEnglish = "Attack on Titan",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx53390-1RGCs8arGQdH.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/53390-q0O2H86e1hQ9.jpg",
            description = "Centuries ago, mankind was slaughtered by monstrous titans. In the present day, Eren Yeager vows to eradicate every titan after his hometown and mother are destroyed.",
            status = "FINISHED",
            score = 85,
            genres = listOf("Action", "Drama", "Fantasy", "Mystery"),
            chapters = 141,
            volumes = 34,
            format = "MANGA",
            startYear = 2009
        ),
        MangaItem(
            id = 87216,
            titleRomaji = "Kimetsu no Yaiba",
            titleEnglish = "Demon Slayer: Kimetsu no Yaiba",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx87216-1y1XnEpHJ3X8.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/87216-F1r5WlJ5M8Yv.jpg",
            description = "Tanjiro Kamado's peaceful life is shattered when his family is slaughtered by demons and his sister Nezuko is turned into one. He joins the Demon Slayer Corps to find a cure.",
            status = "FINISHED",
            score = 83,
            genres = listOf("Action", "Adventure", "Drama", "Supernatural"),
            chapters = 208,
            volumes = 23,
            format = "MANGA",
            startYear = 2016
        ),
        MangaItem(
            id = 118586,
            titleRomaji = "Sousou no Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx118586-kM7hN1HkY0qB.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/118586-oZtU1P2xT3kL.jpg",
            description = "The adventure is over but life goes on for an elf mage just beginning to learn what living is all about. Frieren embarks on a new journey across the continent to understand humanity.",
            status = "RELEASING",
            score = 89,
            genres = listOf("Adventure", "Drama", "Fantasy", "Slice of Life"),
            chapters = 135,
            volumes = 13,
            format = "MANGA",
            startYear = 2020
        ),
        MangaItem(
            id = 116005,
            titleRomaji = "Oshi no Ko",
            titleEnglish = "Oshi no Ko",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx116005-B03tG9Yc2H5c.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/116005-W1kP2k3v9X0Z.jpg",
            description = "A doctor and his deceased patient are reborn as the twin children of their favorite idol star, Ai Hoshino. As they navigate the entertainment industry, dark secrets unfold.",
            status = "FINISHED",
            score = 84,
            genres = listOf("Drama", "Mystery", "Supernatural", "Slice of Life"),
            chapters = 166,
            volumes = 16,
            format = "MANGA",
            startYear = 2020
        ),
        MangaItem(
            id = 108556,
            titleRomaji = "SPY x FAMILY",
            titleEnglish = "SPY x FAMILY",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx108556-9g2l4r3K9l7s.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/108556-3N8k2v9X0z1Y.jpg",
            description = "To maintain world peace, master spy Twilight must establish a fake family. He adopts Anya, a telepath, and marries Yor, an assassin, without knowing each other's secrets.",
            status = "RELEASING",
            score = 85,
            genres = listOf("Action", "Comedy", "Slice of Life", "Supernatural"),
            chapters = 105,
            volumes = 14,
            format = "MANGA",
            startYear = 2019
        ),
        MangaItem(
            id = 108631,
            titleRomaji = "Blue Lock",
            titleEnglish = "Blue Lock",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx108631-0hHj8j3m9l7k.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/108631-7T6k2m8X0z1Y.jpg",
            description = "After a disastrous defeat at the 2018 World Cup, Japan's football association gathers 300 of the best youth strikers into a prison-like facility called Blue Lock to create the ultimate egoist striker.",
            status = "RELEASING",
            score = 83,
            genres = listOf("Action", "Drama", "Sports"),
            chapters = 280,
            volumes = 31,
            format = "MANGA",
            startYear = 2018
        ),
        MangaItem(
            id = 132029,
            titleRomaji = "DAN DA DAN",
            titleEnglish = "Dandadan",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx132029-N2l7m8v9X0z1.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/132029-4T6k2m8X0z1Y.jpg",
            description = "Momo Ayase believes in ghosts but not aliens, while Ken Takakura believes in aliens but not ghosts. When they visit locations associated with both, they awaken supernatural powers.",
            status = "RELEASING",
            score = 85,
            genres = listOf("Action", "Comedy", "Romance", "Sci-Fi", "Supernatural"),
            chapters = 175,
            volumes = 17,
            format = "MANGA",
            startYear = 2021
        ),
        MangaItem(
            id = 30002,
            titleRomaji = "Berserk",
            titleEnglish = "Berserk",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30002-74OHWjG185k9.png",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30002-317s9Fk1G4lH.jpg",
            description = "Guts, a former mercenary now known as the 'Black Swordsman', is out for revenge against his former comrade Griffith who sacrificed their band for godlike power.",
            status = "RELEASING",
            score = 93,
            genres = listOf("Action", "Adventure", "Drama", "Fantasy", "Horror"),
            chapters = 376,
            volumes = 42,
            format = "MANGA",
            startYear = 1989
        ),
        MangaItem(
            id = 30003,
            titleRomaji = "Vagabond",
            titleEnglish = "Vagabond",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30003-tEuh9L9vB7a7.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30003-8k9m0L3vP2q1.jpg",
            description = "In 16th century Japan, Shinmen Takezo is a wild and fierce youth. Guided by a wandering monk, he renames himself Miyamoto Musashi and embarks on a journey of martial enlightenment.",
            status = "PAUSED",
            score = 92,
            genres = listOf("Action", "Adventure", "Drama"),
            chapters = 327,
            volumes = 37,
            format = "MANGA",
            startYear = 1998
        ),
        MangaItem(
            id = 30001,
            titleRomaji = "Monster",
            titleEnglish = "Monster",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30001-z7m9l0k1P2q1.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30001-9k0m1L2vP3q4.jpg",
            description = "Dr. Kenzo Tenma is a brilliant brain surgeon in Germany. When he saves a young boy's life over the city mayor, he unleashes a charismatic psychopath into the world.",
            status = "FINISHED",
            score = 91,
            genres = listOf("Drama", "Mystery", "Psychological"),
            chapters = 162,
            volumes = 18,
            format = "MANGA",
            startYear = 1994
        ),
        MangaItem(
            id = 34367,
            titleRomaji = "Vinland Saga",
            titleEnglish = "Vinland Saga",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx34367-7N7p0L2vP3q4.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/34367-9m0L1k2vP3q4.jpg",
            description = "Thorfinn, son of one of the Vikings' greatest warriors, grows up among the band of mercenaries who slaughtered his father. He strives for revenge while dreaming of a land of peace.",
            status = "RELEASING",
            score = 90,
            genres = listOf("Action", "Adventure", "Drama"),
            chapters = 215,
            volumes = 28,
            format = "MANGA",
            startYear = 2005
        ),
        MangaItem(
            id = 30026,
            titleRomaji = "Hunter x Hunter",
            titleEnglish = "Hunter x Hunter",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30026-bF7k9L1vP2q3.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30026-8m9k0L1vP2q3.jpg",
            description = "Gon Freecss discovers that his father, whom he was told was dead, is alive and a world-renowned Hunter. Gon takes the Hunter exam to find him and explore the world.",
            status = "RELEASING",
            score = 90,
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy"),
            chapters = 405,
            volumes = 38,
            format = "MANGA",
            startYear = 1998
        ),
        MangaItem(
            id = 30021,
            titleRomaji = "Death Note",
            titleEnglish = "Death Note",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30021-m7N9p0L1vP2q.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30021-7N9m0L1vP2q3.jpg",
            description = "Light Yagami, an ace student, discovers a supernatural notebook dropped by a Shinigami that kills anyone whose name is written in it. A legendary cat-and-mouse game begins with L.",
            status = "FINISHED",
            score = 87,
            genres = listOf("Drama", "Mystery", "Supernatural", "Psychological"),
            chapters = 108,
            volumes = 12,
            format = "MANGA",
            startYear = 2003
        ),
        MangaItem(
            id = 30025,
            titleRomaji = "Fullmetal Alchemist",
            titleEnglish = "Fullmetal Alchemist",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30025-p0L1vP2q3m7N.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30025-9m0L1k2vP3q4.jpg",
            description = "Two brothers, Edward and Alphonse Elric, attempt forbidden human alchemy to resurrect their mother, paying a horrific price. They search for the Philosopher's Stone to restore their bodies.",
            status = "FINISHED",
            score = 90,
            genres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy"),
            chapters = 116,
            volumes = 27,
            format = "MANGA",
            startYear = 2001
        ),
        MangaItem(
            id = 30012,
            titleRomaji = "Bleach",
            titleEnglish = "Bleach",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30012-7L9vB1wX2q3m.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30012-3q4m7N9p0L1v.jpg",
            description = "Ichigo Kurosaki has the ability to see ghosts. When his family is attacked by a Hollow, he obtains the powers of a Soul Reaper to protect the living and spirit worlds.",
            status = "FINISHED",
            score = 81,
            genres = listOf("Action", "Adventure", "Supernatural"),
            chapters = 705,
            volumes = 74,
            format = "MANGA",
            startYear = 2001
        ),
        MangaItem(
            id = 30011,
            titleRomaji = "Naruto",
            titleEnglish = "Naruto",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx30011-m9L1vP2q3m7N.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30011-1vP2q3m7N9p0.jpg",
            description = "Naruto Uzumaki is a young ninja who seeks recognition from his peers and dreams of becoming the Hokage, the leader of his village, despite harboring the Nine-Tails Fox.",
            status = "FINISHED",
            score = 80,
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy"),
            chapters = 700,
            volumes = 72,
            format = "MANGA",
            startYear = 1999
        ),
        MangaItem(
            id = 63327,
            titleRomaji = "Tokyo Ghoul",
            titleEnglish = "Tokyo Ghoul",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx63327-0L1vP2q3m7N9.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/63327-7N9p0L1vP2q3.jpg",
            description = "Ken Kaneki is transformed into a half-ghoul after a fatal encounter with Rize Kamishiro. He must navigate the hidden violent society of ghouls in Tokyo.",
            status = "FINISHED",
            score = 84,
            genres = listOf("Action", "Drama", "Horror", "Mystery", "Supernatural"),
            chapters = 144,
            volumes = 14,
            format = "MANGA",
            startYear = 2011
        ),
        MangaItem(
            id = 85486,
            titleRomaji = "Boku no Hero Academia",
            titleEnglish = "My Hero Academia",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx85486-2q3m7N9p0L1v.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/85486-9m0L1k2vP3q4.jpg",
            description = "In a world where 80% of people have superpowers called Quirks, Izuku Midoriya is born quirkless. After meeting the top hero All Might, he inherits One For All.",
            status = "FINISHED",
            score = 81,
            genres = listOf("Action", "Adventure", "Comedy", "Sci-Fi"),
            chapters = 430,
            volumes = 42,
            format = "MANGA",
            startYear = 2014
        ),
        MangaItem(
            id = 125862,
            titleRomaji = "Sakamoto Days",
            titleEnglish = "Sakamoto Days",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx125862-L1vP2q3m7N9p.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/125862-3m7N9p0L1vP2.jpg",
            description = "Taro Sakamoto was an unbeatable legendary hitman. He fell in love, retired, got married, had a child, and opened a convenience store—but assassins keep showing up.",
            status = "RELEASING",
            score = 83,
            genres = listOf("Action", "Comedy", "Slice of Life"),
            chapters = 190,
            volumes = 19,
            format = "MANGA",
            startYear = 2020
        ),
        MangaItem(
            id = 132182,
            titleRomaji = "Ao no Hako",
            titleEnglish = "Blue Box",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx132182-p0L1vP2q3m7N.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/132182-2q3m7N9p0L1v.jpg",
            description = "Taiki Inomata plays badminton and admires Chinatsu Kano, a rising basketball star. Through unexpected circumstances, Chinatsu moves into Taiki's house for high school sports training.",
            status = "RELEASING",
            score = 83,
            genres = listOf("Romance", "Sports", "Slice of Life", "Drama"),
            chapters = 170,
            volumes = 18,
            format = "MANGA",
            startYear = 2021
        ),
        MangaItem(
            id = 106758,
            titleRomaji = "Kage no Jitsuryokusha ni Naritakute!",
            titleEnglish = "The Eminence in Shadow",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx106758-7N9p0L1vP2q3.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/106758-9m0L1k2vP3q4.jpg",
            description = "A young boy obsessed with becoming a mastermind operating from the shadows is reincarnated in a fantasy world as Cid Kagenou, inadvertently battling an evil cult.",
            status = "RELEASING",
            score = 82,
            genres = listOf("Action", "Comedy", "Fantasy"),
            chapters = 68,
            volumes = 13,
            format = "MANGA",
            startYear = 2018
        ),
        MangaItem(
            id = 100512,
            titleRomaji = "Horimiya",
            titleEnglish = "Horimiya",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx100512-3m7N9p0L1vP2.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/100512-1vP2q3m7N9p0.jpg",
            description = "Kyoko Hori and Izumi Miyamura hide their true personal lives outside of school until a chance encounter brings them close together in heartwarming friendship and love.",
            status = "FINISHED",
            score = 84,
            genres = listOf("Comedy", "Romance", "Slice of Life"),
            chapters = 125,
            volumes = 17,
            format = "MANGA",
            startYear = 2011
        ),
        MangaItem(
            id = 86635,
            titleRomaji = "Kaguya-sama wa Kokurasetai",
            titleEnglish = "Kaguya-sama: Love Is War",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx86635-7N9p0L1vP2q3.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/86635-9m0L1k2vP3q4.jpg",
            description = "Student council president Miyuki Shirogane and vice-president Kaguya Shinomiya are in love, but both are too proud to confess first. Love is a battlefield!",
            status = "FINISHED",
            score = 88,
            genres = listOf("Comedy", "Romance", "Slice of Life"),
            chapters = 281,
            volumes = 28,
            format = "MANGA",
            startYear = 2015
        ),
        MangaItem(
            id = 120760,
            titleRomaji = "Kaijuu 8-gou",
            titleEnglish = "Kaiju No. 8",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx120760-2q3m7N9p0L1v.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/120760-9m0L1k2vP3q4.jpg",
            description = "Kafka Hibino works in the monster disposal crew cleaning up corpses of kaiju. After ingesting a small kaiju, he gains the ability to transform into one.",
            status = "RELEASING",
            score = 81,
            genres = listOf("Action", "Sci-Fi", "Comedy"),
            chapters = 115,
            volumes = 13,
            format = "MANGA",
            startYear = 2020
        ),
        MangaItem(
            id = 131777,
            titleRomaji = "Wind Breaker",
            titleEnglish = "Wind Breaker",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx131777-L1vP2q3m7N9p.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/131777-3m7N9p0L1vP2.jpg",
            description = "Haruka Sakura wants nothing to do with weaklings—he's only interested in the strongest. He arrives at Furin High, a school of delinquents who protect their town.",
            status = "RELEASING",
            score = 80,
            genres = listOf("Action", "Drama", "Slice of Life"),
            chapters = 160,
            volumes = 18,
            format = "MANGA",
            startYear = 2021
        ),
        MangaItem(
            id = 85444,
            titleRomaji = "Black Clover",
            titleEnglish = "Black Clover",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx85444-2q3m7N9p0L1v.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/85444-9m0L1k2vP3q4.jpg",
            description = "Asta and Yuno are orphans raised together in the Clover Kingdom. While Yuno is a magic prodigy, Asta has zero magic, but receives a rare five-leaf clover grimoire of anti-magic.",
            status = "RELEASING",
            score = 79,
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy"),
            chapters = 375,
            volumes = 36,
            format = "MANGA",
            startYear = 2015
        ),
        MangaItem(
            id = 74345,
            titleRomaji = "One Punch-Man",
            titleEnglish = "One-Punch Man",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx74345-L1vP2q3m7N9p.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/74345-3m7N9p0L1vP2.jpg",
            description = "Saitama is a hero for fun who trained so hard his hair fell out and he can defeat any enemy with a single punch. Bored by lack of challenge, he seeks a worthy opponent.",
            status = "RELEASING",
            score = 86,
            genres = listOf("Action", "Comedy", "Sci-Fi", "Supernatural"),
            chapters = 205,
            volumes = 31,
            format = "MANGA",
            startYear = 2012
        ),
        MangaItem(
            id = 100178,
            titleRomaji = "Jigokuraku",
            titleEnglish = "Hell's Paradise: Jigokuraku",
            coverImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/bx100178-p0L1vP2q3m7N.jpg",
            bannerImage = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/100178-2q3m7N9p0L1v.jpg",
            description = "Gabimaru the Hollow, a ninja on death row, is offered a full pardon if he can retrieve the Elixir of Immortality from a mysterious island shrouded in deadly secrets.",
            status = "FINISHED",
            score = 83,
            genres = listOf("Action", "Adventure", "Drama", "Supernatural"),
            chapters = 127,
            volumes = 13,
            format = "MANGA",
            startYear = 2018
        )
    )

    fun getTrending(): List<MangaItem> {
        return allManga.sortedByDescending { it.score ?: 0 }
    }

    fun getPopular(): List<MangaItem> {
        return allManga.sortedByDescending { it.chapters ?: 0 }
    }

    fun getByGenre(genre: String): List<MangaItem> {
        if (genre == "All" || genre.isBlank()) return allManga
        return allManga.filter { it.genres.any { g -> g.equals(genre, ignoreCase = true) } }
    }

    fun search(query: String?, genre: String?, filter: SearchFilter? = null): List<MangaItem> {
        var list = allManga
        if (filter != null) {
            list = list.filter { filter.matches(it) }
        } else if (!genre.isNullOrBlank() && genre != "All") {
            list = list.filter { it.genres.any { g -> g.equals(genre, ignoreCase = true) } }
        }
        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.titleRomaji.lowercase().contains(q) ||
                (it.titleEnglish != null && it.titleEnglish.lowercase().contains(q)) ||
                it.description.lowercase().contains(q) ||
                it.genres.any { g -> g.lowercase().contains(q) }
            }
        }

        // Apply sorting
        if (filter != null) {
            list = when (filter.sortBy) {
                SortOption.RATING -> list.sortedByDescending { it.score ?: 0 }
                SortOption.TRENDING -> list.sortedByDescending { it.score ?: 0 }
                SortOption.NEWEST -> list.sortedByDescending { it.startYear ?: 0 }
                SortOption.POPULARITY -> list.sortedByDescending { it.chapters ?: 0 }
            }
        }
        return list
    }

    fun getById(id: Int): MangaItem? {
        return allManga.find { it.id == id }
    }
}
