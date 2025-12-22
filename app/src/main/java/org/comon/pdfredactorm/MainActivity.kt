package org.comon.pdfredactorm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import org.comon.pdfredactorm.core.ui.ads.AdBanner
import org.comon.pdfredactorm.presentation.navigation.AppNavHost
import org.comon.pdfredactorm.core.designsystem.theme.PDFRedactorMTheme
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import org.comon.pdfredactorm.ui.InAppUpdateManager
import android.widget.Toast
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var inAppUpdateManager: InAppUpdateManager
    private var isUpdateCheckComplete = false

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // 업데이트가 취소되거나 실패한 경우 처리
            Toast.makeText(this, "업데이트가 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value || !isUpdateCheckComplete
        }

        // AdMob SDK 초기화
        MobileAds.initialize(this)

        // 앱 업데이트 체크
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdates(this, updateLauncher) {
            isUpdateCheckComplete = true
        }

        enableEdgeToEdge()
        setContent {
            val isProEnabled by mainViewModel.isProEnabled.collectAsStateWithLifecycle()
            
            PDFRedactorMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // NavGraph가 남은 공간을 차지
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Navigation3: NavController 대신 내부에서 백스택 관리
                            AppNavHost()
                        }

                        // 하단 배너 광고 (Pro 활성화 시 비활성화)
                        if (!isProEnabled) {
                            AdBanner(
                                adUnitId = BuildConfig.ADMOB_BANNER_ID,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onResumeCheck(this, updateLauncher)
        }
    }
}