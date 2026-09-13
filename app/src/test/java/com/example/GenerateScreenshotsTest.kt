package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MangaPillTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GenerateScreenshotsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val RedPrimary = Color(0xFFE53935)
  private val DarkBg = Color(0xFF0C0D12)
  private val SurfaceDark = Color(0xFF161822)
  private val CardBg = Color(0xFF1E212D)

  @Test
  fun capture_logo() {
    composeTestRule.setContent {
      Box(
        modifier = Modifier
          .size(512.dp)
          .background(DarkBg),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          // Logo Icon Canvas
          androidx.compose.foundation.Canvas(modifier = Modifier.size(240.dp, 180.dp)) {
            val w = size.width
            val h = size.height

            // Rising Sun
            drawCircle(
              color = RedPrimary,
              radius = w * 0.13f,
              center = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.22f)
            )

            // Torii Gate on top-left
            val toriiPath = Path().apply {
              // Top curved beam
              moveTo(w * 0.22f, h * 0.16f)
              lineTo(w * 0.46f, h * 0.16f)
              lineTo(w * 0.46f, h * 0.20f)
              lineTo(w * 0.22f, h * 0.20f)
              close()
              // Second horizontal beam
              moveTo(w * 0.25f, h * 0.23f)
              lineTo(w * 0.43f, h * 0.23f)
              lineTo(w * 0.43f, h * 0.26f)
              lineTo(w * 0.25f, h * 0.26f)
              close()
              // Left post
              moveTo(w * 0.28f, h * 0.18f)
              lineTo(w * 0.32f, h * 0.18f)
              lineTo(w * 0.32f, h * 0.42f)
              lineTo(w * 0.28f, h * 0.42f)
              close()
              // Right post
              moveTo(w * 0.38f, h * 0.18f)
              lineTo(w * 0.42f, h * 0.18f)
              lineTo(w * 0.42f, h * 0.42f)
              lineTo(w * 0.38f, h * 0.42f)
              close()
            }
            drawPath(toriiPath, color = RedPrimary, style = Fill)

            // Left Book Page (Open perspective)
            val leftPage = Path().apply {
              moveTo(w * 0.18f, h * 0.38f)
              lineTo(w * 0.48f, h * 0.46f)
              lineTo(w * 0.48f, h * 0.94f)
              lineTo(w * 0.18f, h * 0.86f)
              close()
            }
            drawPath(leftPage, color = Color.White, style = Fill)

            // Right Book Page
            val rightPage = Path().apply {
              moveTo(w * 0.52f, h * 0.46f)
              lineTo(w * 0.82f, h * 0.38f)
              lineTo(w * 0.82f, h * 0.86f)
              lineTo(w * 0.52f, h * 0.94f)
              close()
            }
            drawPath(rightPage, color = Color.White, style = Fill)

            // Left page panel (black inset)
            val leftInset = Path().apply {
              moveTo(w * 0.22f, h * 0.42f)
              lineTo(w * 0.45f, h * 0.48f)
              lineTo(w * 0.45f, h * 0.89f)
              lineTo(w * 0.22f, h * 0.83f)
              close()
            }
            drawPath(leftInset, color = DarkBg, style = Fill)

            // Right page panel (black inset)
            val rightInset = Path().apply {
              moveTo(w * 0.55f, h * 0.48f)
              lineTo(w * 0.78f, h * 0.42f)
              lineTo(w * 0.78f, h * 0.83f)
              lineTo(w * 0.55f, h * 0.89f)
              close()
            }
            drawPath(rightInset, color = DarkBg, style = Fill)

            // White panels inside left page
            val leftPanel1 = Path().apply {
              moveTo(w * 0.24f, h * 0.60f)
              lineTo(w * 0.34f, h * 0.62f)
              lineTo(w * 0.34f, h * 0.80f)
              lineTo(w * 0.24f, h * 0.78f)
              close()
            }
            drawPath(leftPanel1, color = Color.White, style = Fill)

            // White diagonal panel inside right page
            val rightPanel1 = Path().apply {
              moveTo(w * 0.58f, h * 0.56f)
              lineTo(w * 0.74f, h * 0.50f)
              lineTo(w * 0.74f, h * 0.78f)
              lineTo(w * 0.58f, h * 0.84f)
              close()
            }
            drawPath(rightPanel1, color = Color.White, style = Fill)
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Brand Typography: MyManga
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "My",
              color = Color.White,
              fontSize = 54.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = (-1).sp
            )
            Text(
              text = "Manga",
              color = RedPrimary,
              fontSize = 54.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif,
              letterSpacing = (-1).sp
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Modern Manga & Webtoon Reader",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "../screenshots/logo.png")
  }

  @Test
  fun capture_explore_screen() {
    composeTestRule.setContent {
      MangaPillTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedPrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "MyManga",
                  color = Color.White,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
              }
            }

            // Hero Featured Carousel Card
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.verticalGradient(
                    listOf(Color(0xFF2C1014), Color(0xFF12080B))
                  )
                )
                .border(1.dp, Color(0xFF421D22), RoundedCornerShape(16.dp))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(RedPrimary)
                      .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text("#1 TRENDING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("4.95", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Solo Leveling: Ragnarok", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Action • Fantasy • Supernatural • 120 Chapters", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = {},
                  colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Read Chapter 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Genre Chips
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("🔥 All", "Action", "Fantasy", "Romance", "Manhwa").forEachIndexed { i, genre ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (i == 0) RedPrimary else SurfaceDark)
                    .border(1.dp, if (i == 0) RedPrimary else Color(0xFF262835), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(genre, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Header: Popular Manga
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Popular Manga", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              Text("See All", color = RedPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manga Grid Cards
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              val mangaList = listOf(
                Pair("Jujutsu Kaisen", "Ch. 271 • 4.9 ★"),
                Pair("Chainsaw Man", "Ch. 178 • 4.8 ★"),
                Pair("One Piece", "Ch. 1126 • 5.0 ★")
              )
              mangaList.forEach { (title, subtitle) ->
                Column(modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(145.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(CardBg)
                      .border(1.dp, Color(0xFF2A2D3C), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = RedPrimary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  Text(subtitle, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                }
              }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Navigation Bar
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = RedPrimary)
                Text("Explore", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Text("Search", color = Color.Gray, fontSize = 11.sp)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.Gray)
                Text("Library", color = Color.Gray, fontSize = 11.sp)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
                Text("Settings", color = Color.Gray, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "../screenshots/01_explore_screen.png")
  }

  @Test
  fun capture_search_screen() {
    composeTestRule.setContent {
      MangaPillTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
              text = "Explore & Search",
              color = Color.White,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            // Search Bar
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF262838), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Search Attack on Titan, Berserk...", color = Color.Gray, fontSize = 14.sp)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Selector Tabs
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("All Sources", "MangaDex", "Mangapill", "WeebCentral").forEachIndexed { i, src ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (i == 1) RedPrimary else SurfaceDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(src, color = Color.White, fontSize = 11.sp, fontWeight = if (i == 1) FontWeight.Bold else FontWeight.Normal)
                }
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search Results List
            Text(
              text = "Results (34 found)",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              val searchItems = listOf(
                Triple("Attack on Titan", "139 Chapters • Completed", "Isayama Hajime"),
                Triple("Attack on Titan: No Regrets", "8 Chapters • Completed", "Gun Snark"),
                Triple("Attack on Titan: Junior High", "45 Chapters • Completed", "Saki Nakagawa")
              )
              searchItems.forEach { (title, status, author) ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color(0xFF222430), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(55.dp, 75.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(CardBg),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(24.dp))
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(author, color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(Color(0xFF2E7D32))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text("COMPLETED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(status, color = Color.Gray, fontSize = 11.sp)
                    }
                  }
                  Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Gray)
                }
              }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Nav
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.Gray)
                Text("Explore", color = Color.Gray, fontSize = 11.sp)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary)
                Text("Search", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.Gray)
                Text("Library", color = Color.Gray, fontSize = 11.sp)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
                Text("Settings", color = Color.Gray, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "../screenshots/02_search_screen.png")
  }

  @Test
  fun capture_detail_screen() {
    composeTestRule.setContent {
      MangaPillTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
              Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
              }
            }

            // Manga Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(105.dp, 150.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(CardBg)
                  .border(1.dp, RedPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(44.dp))
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text("Attack on Titan", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Shingeki no Kyojin", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("4.92 (38.4k votes)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("139 Chapters • Completed", color = Color.Gray, fontSize = 12.sp)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read Ch. 1", fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF3B3E52)),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
              ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Genre Chips
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("Action", "Dark Fantasy", "Post-Apocalyptic", "Mystery").forEach { tag ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(tag, color = Color.LightGray, fontSize = 11.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chapters Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Chapters (139)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("Sort Descending", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chapter items
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(
                Pair("Chapter 139: Toward the Tree on That Hill", "Apr 9, 2021 • 45 pages"),
                Pair("Chapter 138: A Long Dream", "Mar 9, 2021 • 45 pages"),
                Pair("Chapter 137: Titans", "Feb 9, 2021 • 45 pages"),
                Pair("Chapter 136: Devote Your Hearts", "Jan 9, 2021 • 45 pages")
              ).forEach { (chTitle, date) ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(chTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(date, color = Color.Gray, fontSize = 11.sp)
                  }
                  Icon(Icons.Default.Download, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "../screenshots/03_detail_screen.png")
  }

  @Test
  fun capture_reader_screen() {
    composeTestRule.setContent {
      MangaPillTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text("Attack on Titan", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  Text("Chapter 1: To You, 2,000 Years From Now", color = Color.LightGray, fontSize = 11.sp)
                }
              }
              Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }

            // Manga Reading Content (Simulated Comic Page Panels)
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Big top panel
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1.2f)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF14151C))
                  .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("⚔️", fontSize = 48.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    "\"On that day, mankind received a grim reminder...\"",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                  )
                }
              }

              // Two middle panels
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF181A22))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("WALL MARIA", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF181A22))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("DISTRICT OF SHIGANSHINA", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
              }

              // Bottom wide panel
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1.1f)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF14151C))
                  .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text("THE COLOSSAL TITAN APPEARS", color = RedPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
              }
            }

            // Bottom Reader HUD
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Page 12 / 45", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Webtoon Mode (Vertical)", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Reading Progress Bar
              LinearProgressIndicator(
                progress = { 12f / 45f },
                color = RedPrimary,
                trackColor = Color(0xFF2A2C38),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(CircleShape)
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = {},
                  colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  modifier = Modifier.height(34.dp)
                ) {
                  Text("Prev Chapter", color = Color.White, fontSize = 11.sp)
                }

                Text("Ch. 1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Button(
                  onClick = {},
                  colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  modifier = Modifier.height(34.dp)
                ) {
                  Text("Next Chapter", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "../screenshots/04_reader_screen.png")
  }
}
