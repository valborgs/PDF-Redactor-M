package org.comon.pdfredactorm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.comon.pdfredactorm.BuildConfig
import org.comon.pdfredactorm.R
import org.comon.pdfredactorm.core.ui.ScopedViewModelContainer
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
fun AppNavHost(
    navViewModel: NavViewModel = hiltViewModel()
) {
    val homeScreenConfig = HomeScreenConfig(
        appName = stringResource(R.string.app_name),
        coffeeChatUrl = navViewModel.getCoffeeChatUrl(),
        nativeAdUnitId = BuildConfig.ADMOB_NATIVE_ID
    )

    // Navigation3: Saveable back stack with initial destination
    val backStack = rememberNavBackStack(HomeKey())

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            // Home 화면
            entry<HomeKey> { key ->
                HomeScreen(
                    config = homeScreenConfig,
                    showRedeemDialogInitial = key.showRedeemDialog,
                    onRedeemDialogConsumed = {
                        // 중요: 다이얼로그가 표시되었으므로 백스택의 키 상태를 업데이트하여 
                        // 나중에 에디터에서 돌아올 때 다시 뜨지 않도록 함 (이벤트 소비)
                        val index = backStack.indexOf(key)
                        if (index != -1) {
                            backStack[index] = key.copy(showRedeemDialog = false)
                        }
                    },
                    onPdfClick = { pdfId ->
                        backStack.add(EditorKey(pdfId))
                    }
                )
            }

            // Editor 화면
            entry<EditorKey> { key ->
                ScopedViewModelContainer {
                    EditorScreen(
                        pdfId = key.pdfId,
                        onBackClick = {
                            // 단순 remove 대신 HomeKey로 돌아가거나, 특정 상태를 전달해야 함
                            // Navigation3에서 결과를 전달하는 가장 깔끔한 방법은 백스택을 조작하는 것
                            backStack.removeLastOrNull()
                        },
                        onBackToHomeWithRedeem = {
                            // 기기 미매칭 에러 후 홈으로 돌아갈 때 리딤 다이얼로그를 띄우도록 백스택 리셋
                            backStack.clear()
                            backStack.add(HomeKey(showRedeemDialog = true))
                        }
                    )
                }
            }
        }
    )
}

