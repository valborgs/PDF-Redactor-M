package org.comon.pdfredactorm.core.ui.ads

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import androidx.compose.foundation.layout.navigationBarsPadding

/**
 * AdMob 배너광고 컴포저블
 *
 * 광고 로딩 전에는 공간을 차지하지 않고,
 * 광고 로딩 후에만 광고 높이만큼 공간을 차지합니다.
 * 애니메이션으로 부드럽게 나타납니다.
 *
 * @param adUnitId 광고 단위 ID
 * @param modifier Modifier (기본값: fillMaxWidth())
 */
@Composable
fun AdBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    // Preview 모드에서는 광고를 표시하지 않음
    if (LocalInspectionMode.current) {
        return
    }

    // 광고를 한 번만 생성하고, 로딩 전후 visibility만 변경
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.BANNER)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // 초기에는 보이지 않음 (크기는 0)
                visibility = View.GONE

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        // 광고 로딩 완료 시 표시
                        visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        // 광고 로딩 실패 시 숨김
                        visibility = View.GONE
                    }
                }

                val adRequest = AdRequest.Builder().build()
                loadAd(adRequest)
            }
        },
        update = { _ -> }
    )
}
