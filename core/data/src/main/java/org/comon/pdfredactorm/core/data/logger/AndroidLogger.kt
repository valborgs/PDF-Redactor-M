package org.comon.pdfredactorm.core.data.logger

import android.util.Log
import org.comon.pdfredactorm.core.common.logger.Logger
import javax.inject.Inject

/**
 * Android Log API를 사용한 Logger 구현체
 * 
 * Data Layer에 위치하며, Android 플랫폼의 Log API를 사용합니다.
 * 릴리스 빌드에서는 debug 로그를 출력하지 않습니다.
 * 
 * 모든 로그는 "PDFLogger" 태그를 사용합니다.
 * 
 * @param isDebugBuild 디버그 빌드 여부. true일 경우 debug 로그를 출력합니다.
 */
class AndroidLogger @Inject constructor(
    @param:IsDebugBuild private val isDebugBuild: Boolean
) : Logger {
    
    companion object {
        private const val TAG = "PDFLogger"
    }
    
    override fun debug(message: String) {
        // Debug 로그는 디버그 빌드에서만 출력
        if (isDebugBuild) {
            Log.d(TAG, message)
        }
    }
    
    override fun info(message: String) {
        Log.i(TAG, message)
    }
    
    override fun warning(message: String) {
        Log.w(TAG, message)
    }
    
    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}
