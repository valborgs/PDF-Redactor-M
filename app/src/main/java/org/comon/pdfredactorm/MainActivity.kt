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
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import org.comon.pdfredactorm.core.ui.ads.AdBanner
import org.comon.pdfredactorm.presentation.navigation.AppNavHost
import org.comon.pdfredactorm.core.designsystem.theme.PDFRedactorMTheme

import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        // AdMob SDK 초기화
        MobileAds.initialize(this)

        enableEdgeToEdge()
        setContent {
            PDFRedactorMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // NavGraph가 남은 공간을 차지
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                        }

                        // 하단 배너 광고
                        // 광고 로딩 전에는 공간을 차지하지 않고,
                        // 광고 로딩 후에만 광고 높이만큼 공간 차지
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