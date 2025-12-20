package org.comon.pdfredactorm.feature.home.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation3 - Home 화면 네비게이션 키
 */
@Serializable
data class HomeKey(val showRedeemDialog: Boolean = false) : NavKey
