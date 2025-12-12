package org.comon.pdfredactorm.core.data.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.pdfredactorm.core.common.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * Firebase Analytics를 사용한 AnalyticsTracker 구현체
 * 
 * Data Layer에 위치하며, Firebase Analytics SDK를 사용합니다.
 */
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsTracker {
    
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }
    
    override fun logEvent(eventName: String, params: Map<String, Any>?) {
        val bundle = params?.toBundle()
        firebaseAnalytics.logEvent(eventName, bundle)
    }
    
    override fun logScreenView(screenName: String, screenClass: String?) {
        val params = bundleOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to (screenClass ?: screenName)
        )
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }
    
    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
    
    /**
     * Map을 Bundle로 변환
     */
    private fun Map<String, Any>.toBundle(): Bundle {
        return bundleOf(*this.map { (key, value) ->
            when (value) {
                is String -> key to value
                is Int -> key to value
                is Long -> key to value
                is Double -> key to value
                is Boolean -> key to value
                else -> key to value.toString()
            }
        }.toTypedArray())
    }
}
