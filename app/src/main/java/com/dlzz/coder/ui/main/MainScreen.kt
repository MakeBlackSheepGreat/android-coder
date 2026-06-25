package com.dlzz.coder.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.BridgeHost
import com.dlzz.coder.bridge.HostSession
import com.dlzz.coder.ui.files.FileListScreen
import com.dlzz.coder.ui.hosts.HostListScreen
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.i18n.Strings
import com.dlzz.coder.ui.sessions.SessionListScreen
import com.dlzz.coder.ui.settings.SettingsScreen
import com.dlzz.coder.ui.theme.accentColor
import com.dlzz.coder.ui.theme.cardSurfaceColor
import com.dlzz.coder.ui.theme.glassBottomBar
import com.dlzz.coder.ui.theme.glassCapsule
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.ui.theme.glassClickable
import com.dlzz.coder.ui.theme.glassTabIndicator
import com.dlzz.coder.ui.theme.rememberLayerBackdrop
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.kyant.backdrop.Backdrop

private enum class AgentHomePage {
    WORKSPACES,
    CHAT,
    WORKSPACE_BROWSER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    bridgeViewModel: BridgeViewModel,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToFilePreview: (String, String, String) -> Unit
) {
    var selectedPage by remember { mutableStateOf(AgentHomePage.CHAT) }
    var showSettings by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val hosts by bridgeViewModel.hosts.collectAsState()
    val activeHostId by bridgeViewModel.activeHostId.collectAsState()
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val activeHost = remember(hosts, activeHostId) {
        hosts.firstOrNull { it.id == activeHostId } ?: hosts.firstOrNull()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavigationPane = maxWidth >= 720.dp

        if (useNavigationPane) {
            Row(Modifier.fillMaxSize()) {
                AgentHomeNavigationPane(
                    selectedPage = selectedPage,
                    onPageSelected = { selectedPage = it },
                    onSettingsClick = { showSettings = true },
                    strings = strings,
                    hosts = hosts,
                    activeHost = activeHost,
                    hostSessions = hostSessions,
                    backdrop = backdrop,
                    modifier = Modifier
                        .width(328.dp)
                        .fillMaxHeight()
                )
                AgentHomePager(
                    selectedPage = selectedPage,
                    bridgeViewModel = bridgeViewModel,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToFilePreview = onNavigateToFilePreview,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                AgentHomePager(
                    selectedPage = selectedPage,
                    bridgeViewModel = bridgeViewModel,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToFilePreview = onNavigateToFilePreview,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 92.dp)
                )
                AgentHomeCompactBar(
                    selectedPage = selectedPage,
                    onPageSelected = { selectedPage = it },
                    onSettingsClick = { showSettings = true },
                    strings = strings,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                scrimColor = Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.46f else 0.22f)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                ) {
                    SettingsScreen(bridgeViewModel = bridgeViewModel)
                }
            }
        }
    }
}

@Composable
private fun AgentHomePager(
    selectedPage: AgentHomePage,
    bridgeViewModel: BridgeViewModel,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToFilePreview: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedPage,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (
                slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { width -> direction * width / 7 } +
                    fadeIn(animationSpec = tween(200)) +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                ).togetherWith(
                slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -direction * width / 9 } +
                    fadeOut(animationSpec = tween(160)) +
                    scaleOut(targetScale = 0.99f, animationSpec = tween(220, easing = FastOutSlowInEasing))
            ).using(SizeTransform(clip = false))
        },
        label = "agent-home-main-pager",
        modifier = modifier.statusBarsPadding()
    ) { page ->
        Box(Modifier.fillMaxSize()) {
            when (page) {
                AgentHomePage.WORKSPACES -> HostListScreen(bridgeViewModel = bridgeViewModel)
                AgentHomePage.CHAT -> SessionListScreen(
                    bridgeViewModel = bridgeViewModel,
                    onSessionClick = onNavigateToChat
                )
                AgentHomePage.WORKSPACE_BROWSER -> FileListScreen(
                    bridgeViewModel = bridgeViewModel,
                    onFileClick = onNavigateToFilePreview
                )
            }
        }
    }
}

@Composable
private fun AgentHomeNavigationPane(
    selectedPage: AgentHomePage,
    onPageSelected: (AgentHomePage) -> Unit,
    onSettingsClick: () -> Unit,
    strings: Strings,
    hosts: List<BridgeHost>,
    activeHost: BridgeHost?,
    hostSessions: List<HostSession>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        AgentHomeStatusCard(
            strings = strings,
            hosts = hosts,
            activeHost = activeHost,
            hostSessions = hostSessions
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgentHomeNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = strings.sessionsTitle,
                subtitle = currentSessionSubtitle(hostSessions, strings),
                selected = selectedPage == AgentHomePage.CHAT,
                onClick = { onPageSelected(AgentHomePage.CHAT) },
                backdrop = backdrop
            )
            AgentHomeNavItem(
                icon = Icons.Default.Folder,
                title = strings.hostsTitle,
                subtitle = workspacesSubtitle(hosts, activeHost, strings),
                selected = selectedPage == AgentHomePage.WORKSPACES,
                onClick = { onPageSelected(AgentHomePage.WORKSPACES) },
                backdrop = backdrop
            )
            AgentHomeNavItem(
                icon = Icons.Default.Description,
                title = strings.filesTitle,
                subtitle = filesSubtitle(hostSessions, activeHost, strings),
                selected = selectedPage == AgentHomePage.WORKSPACE_BROWSER,
                onClick = { onPageSelected(AgentHomePage.WORKSPACE_BROWSER) },
                backdrop = backdrop
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentHomeIconAction(Icons.Default.Home, strings.sessionsTitle, backdrop) {
                onPageSelected(AgentHomePage.CHAT)
            }
            AgentHomeIconAction(Icons.Default.Add, strings.newSession, backdrop) {
                onPageSelected(AgentHomePage.WORKSPACES)
            }
            AgentHomeIconAction(Icons.AutoMirrored.Filled.List, strings.sessionsTitle, backdrop) {
                onPageSelected(AgentHomePage.CHAT)
            }
            AgentHomeIconAction(Icons.Default.Settings, strings.settingsTitle, backdrop, onSettingsClick)
        }
    }
}

@Composable
private fun AgentHomeStatusCard(
    strings: Strings,
    hosts: List<BridgeHost>,
    activeHost: BridgeHost?,
    hostSessions: List<HostSession>
) {
    val connectedCount = hosts.count { it.connected }
    val statusText = when {
        hosts.isEmpty() -> strings.connectToBridge
        connectedCount > 0 -> strings.connected
        else -> strings.disconnected
    }
    val statusColor = when {
        connectedCount > 0 -> Color(0xFF34C759)
        hosts.isNotEmpty() -> Color(0xFFFF9500)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val detail = activeHost?.let { host ->
        host.workspaceTitle
            .ifBlank { host.workspacePath }
            .ifBlank { "${host.host}:${host.port}" }
    } ?: strings.hostTotal(hosts.size)

    Box(
        Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 14.dp, blur = 6.dp, lensNear = 14.dp, lensFar = 26.dp)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                strings.sessionCount(hostSessions.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AgentHomeNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val accent = accentColor()
    val iconColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "agent-home-nav-icon"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "agent-home-nav-lift"
    )
    val selectedModifier = if (selected) {
        Modifier.glassCard(
            backdrop = backdrop,
            cornerRadius = 14.dp,
            blur = 4.dp,
            lensNear = 12.dp,
            lensFar = 24.dp,
            surfaceColor = cardSurfaceColor().copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.58f)
        )
    } else {
        Modifier
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .graphicsLayer { translationY = -lift.toPx() }
            .then(selectedModifier)
            .glassClickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AgentHomeIconAction(
    icon: ImageVector,
    contentDescription: String,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(44.dp)
            .glassCapsule(backdrop = backdrop)
            .glassClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun AgentHomeCompactBar(
    selectedPage: AgentHomePage,
    onPageSelected: (AgentHomePage) -> Unit,
    onSettingsClick: () -> Unit,
    strings: Strings,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp, top = 8.dp)
            .height(64.dp)
            .glassBottomBar(backdrop = backdrop),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentHomeCompactBarItem(
                icon = Icons.Default.Folder,
                label = strings.hostsTab,
                selected = selectedPage == AgentHomePage.WORKSPACES,
                onClick = { onPageSelected(AgentHomePage.WORKSPACES) },
                backdrop = backdrop,
                modifier = Modifier.weight(1f)
            )
            AgentHomeCompactBarItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = strings.sessionsTab,
                selected = selectedPage == AgentHomePage.CHAT,
                onClick = { onPageSelected(AgentHomePage.CHAT) },
                backdrop = backdrop,
                modifier = Modifier.weight(1f)
            )
            AgentHomeCompactBarItem(
                icon = Icons.Default.Description,
                label = strings.filesTab,
                selected = selectedPage == AgentHomePage.WORKSPACE_BROWSER,
                onClick = { onPageSelected(AgentHomePage.WORKSPACE_BROWSER) },
                backdrop = backdrop,
                modifier = Modifier.weight(1f)
            )
            AgentHomeCompactBarItem(
                icon = Icons.Default.Settings,
                label = strings.settingsTab,
                selected = false,
                onClick = onSettingsClick,
                backdrop = backdrop,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AgentHomeCompactBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val accent = accentColor()
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "agent-home-compact-color"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "agent-home-compact-scale"
    )

    Box(
        modifier
            .height(54.dp)
            .glassClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .glassTabIndicator(backdrop = backdrop)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(19.dp))
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun currentSessionSubtitle(hostSessions: List<HostSession>, strings: Strings): String {
    val active = hostSessions.firstOrNull()
    return active?.session?.displayName().orEmpty().ifBlank { strings.sessionCount(hostSessions.size) }
}

private fun workspacesSubtitle(hosts: List<BridgeHost>, activeHost: BridgeHost?, strings: Strings): String {
    val host = activeHost ?: return strings.hostTotal(hosts.size)
    val workspace = host.workspaceTitle.ifBlank { host.workspacePath }.ifBlank { host.address }
    val status = if (host.connected) strings.connected else strings.disconnected
    return "$workspace / $status"
}

private fun filesSubtitle(hostSessions: List<HostSession>, activeHost: BridgeHost?, strings: Strings): String {
    val session = hostSessions.firstOrNull()?.session
    return session?.fileBasename()
        ?.ifBlank { session.workspaceTitle }
        ?.ifBlank { activeHost?.workspaceTitle.orEmpty() }
        ?.ifBlank { strings.noSessionForFiles }
        ?: strings.noSessionForFiles
}
