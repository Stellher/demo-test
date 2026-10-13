package com.example.bangumioauthdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

class BangumiHomeActivity : ComponentActivity() {
    private val vm: BangumiBrowseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OAuthSessionManager.initialize(application)
        if (OAuthSessionManager.session.value != null) RefreshScheduler.schedule(application)
        window.statusBarColor = android.graphics.Color.rgb(243, 247, 255)
        window.navigationBarColor = android.graphics.Color.rgb(249, 243, 250)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(colorScheme = BangumiColors) {
                BangumiHomeApp(vm = vm) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            OAuthSessionManager.ensureValidToken()
        }
    }
}

@Composable
private fun BangumiHomeApp(
    vm: BangumiBrowseViewModel,
    openOAuthLab: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val snackHost = remember { SnackbarHostState() }
    var currentTab by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    val subject = vm.selectedSubject
    val person = vm.selectedPerson
    val overlay = searchOpen || subject != null || person != null

    fun goBack() {
        when {
            person != null -> vm.closePerson()
            subject != null -> vm.closeSubject()
            searchOpen -> searchOpen = false
            else -> currentTab = 0
        }
    }

    BackHandler(enabled = overlay) { goBack() }
    val message = vm.actionFeedback
    LaunchedEffect(message) {
        if (message != null) {
            snackHost.showSnackbar(message)
            vm.dismissFeedback()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Capture BOTH the decorative background and the live scrollable content.
        // Without this, real refraction samples transparent pixels.
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(
            Modifier.fillMaxSize().layerBackdrop(backdrop)
        ) {
            GlassBackdropBackground()
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when {
                person != null -> PersonPage(vm, back = { vm.closePerson() })
                subject != null -> SubjectPage(
                    vm = vm,
                    back = { vm.closeSubject() },
                    openSubject = vm::openSubject,
                    openPerson = vm::openPerson,
                    openLogin = openOAuthLab
                )
                searchOpen -> SearchPage(
                    vm = vm,
                    onBack = { searchOpen = false },
                    openSubject = vm::openSubject,
                    openPerson = vm::openPerson
                )
                else -> AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val sign = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(210)) + slideInHorizontally(
                            initialOffsetX = { fullWidth -> sign * fullWidth / 7 },
                            animationSpec = spring(dampingRatio = .86f, stiffness = 420f)
                        )).togetherWith(
                            fadeOut(tween(150)) + slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -sign * fullWidth / 10 },
                                animationSpec = spring(dampingRatio = .95f, stiffness = 550f)
                            )
                        )
                    },
                    label = "Spring liquid screen transition"
                ) { tab ->
                    when (tab) {
                    0 -> DiscoverPage(
                        vm = vm,
                        openSubject = vm::openSubject,
                        openSearch = { searchOpen = true },
                        goToTab = { currentTab = it }
                    )
                    1 -> CalendarPage(vm, openSubject = vm::openSubject)
                    2 -> SeasonPage(vm, openSubject = vm::openSubject)
                    3 -> LibraryPage(vm, openSubject = vm::openSubject, onLogin = openOAuthLab)
                    else -> ProfilePage(
                        vm = vm,
                        onLogin = openOAuthLab,
                        onOpenLibrary = { currentTab = 3 },
                        onOpenApiLab = openOAuthLab,
                        onOpenSearch = { searchOpen = true }
                    )
                    }
                }
            }
            }
        }
        if (!overlay) {
            LiquidNavigation(
                backdrop = backdrop,
                selected = currentTab,
                onSelected = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
        SnackbarHost(snackHost, modifier = Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding().padding(bottom = if (overlay) 10.dp else 94.dp))
        }
    }
}
