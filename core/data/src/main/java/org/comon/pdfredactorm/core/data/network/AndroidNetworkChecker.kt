package org.comon.pdfredactorm.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.pdfredactorm.core.common.network.NetworkChecker
import javax.inject.Inject

/**
 * Android ConnectivityManager를 사용한 NetworkChecker 구현체
 */
class AndroidNetworkChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkChecker {
    
    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
