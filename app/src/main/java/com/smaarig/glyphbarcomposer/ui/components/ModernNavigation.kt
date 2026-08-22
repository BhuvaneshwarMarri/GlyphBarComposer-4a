package com.smaarig.glyphbarcomposer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smaarig.glyphbarcomposer.ui.Screen
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

// ─── Nothing-style Bottom Navigation Bar ────────────────────────────────────
// Selected tab: full-height white pill that visually "hovers" over the dark
// bar via shadow elevation. Content inside the pill inverts to near-black.
// Unselected tabs: icon + label in dim gray, no background.
@Composable
fun ModernBottomNavigationBar(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: screens[0].route
    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(36.dp),
        border = BorderStroke(1.dp, Color(0xFF242424)),
        shadowElevation = 24.dp,
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            val maxWidth = maxWidth
            val itemWidth = maxWidth / screens.size
            
            // Sliding Highlight Pill
            val offset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessLow
                ),
                label = "pillOffset"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = offset.roundToPx(), y = 0) }
                    .width(itemWidth)
                    .height(52.dp)
                    .padding(horizontal = 3.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(26.dp),
                        clip = false,
                        ambientColor = Color.White.copy(alpha = 0.1f),
                        spotColor = Color.White.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF2A2A2A)) // Floating greyish pill
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) Color.White else Color(0xFF666666),
                        animationSpec = tween(300),
                        label = "contentColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = screen.label.uppercase(),
                                color = contentColor,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = nothingFont
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Nothing-style Navigation Rail ──────────────────────────────────────────
// Same pill treatment as the bottom bar but oriented vertically.
@Composable
fun ModernNavigationRail(navController: NavHostController, screens: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: screens[0].route
    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    NavigationRail(
        containerColor = Color.Black,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        },
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val maxHeight = maxHeight
            val itemHeight = 54.dp + 4.dp // item height + spacer
            
            // Sliding Highlight Pill
            val offset by animateDpAsState(
                targetValue = itemHeight * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessLow
                ),
                label = "railPillOffset"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = offset.roundToPx()) }
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(vertical = 2.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(27.dp),
                        clip = false,
                        ambientColor = Color.White.copy(alpha = 0.1f),
                        spotColor = Color.White.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color(0xFF2A2A2A))
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route

                    val contentColor by animateColorAsState(
                        targetValue = if (selected) Color.White else Color(0xFF666666),
                        animationSpec = tween(300),
                        label = "railContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(vertical = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = screen.label.uppercase(),
                                color = contentColor,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = nothingFont
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
