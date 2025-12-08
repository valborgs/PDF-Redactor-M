package org.comon.pdfredactorm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.feature.home.HomeScreen
import org.comon.pdfredactorm.feature.home.HomeScreenConfig
import org.comon.pdfredactorm.feature.home.navigation.HomeKey
import org.comon.pdfredactorm.feature.editor.EditorScreen
import org.comon.pdfredactorm.feature.editor.navigation.EditorKey

/**
 * Navigation3를 사용한 앱의 메인 네비게이션
 * 
 * - rememberSaveableBackStack: 백스택 상태 관리 (설정 변경/프로세스 종료 시에도 유지)
 * - NavDisplay: 백스택 상태에 따라 적절한 화면 표시
 * - entryProvider: 키 타입에 따른 컴포저블 매핑
 */
@Composable
fun AppNavHost() {
    val homeScreenConfig = HomeScreenConfig(
        appName = "PDF안심이",
        coffeeChatUrl = BuildConfig.COFFEE_CHAT_URL,
        nativeAdUnitId = BuildConfig.ADMOB_NATIVE_ID
    )

    // Navigation3: Saveable back stack with initial destination
    val backStack = rememberNavBackStack(HomeKey)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            // Home 화면
            entry<HomeKey> {
                HomeScreen(
                    config = homeScreenConfig,
                    onPdfClick = { pdfId ->
                        backStack.add(EditorKey(pdfId))
                    }
                )
            }

            // Editor 화면
            entry<EditorKey> { key ->
                EditorScreen(
                    pdfId = key.pdfId,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
