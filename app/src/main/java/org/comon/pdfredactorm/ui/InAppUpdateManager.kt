package org.comon.pdfredactorm.ui

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isImmediateUpdateAllowed

class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    
    /**
     * 업데이트 확인 및 필요한 경우 즉시 업데이트 프로세스 시작
     */
    fun checkForUpdates(
        activity: Activity,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onComplete: () -> Unit
    ) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val appUpdateInfo = task.result
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // 즉시 업데이트(IMMEDIATE)가 가능한지 확인
                    if (appUpdateInfo.isImmediateUpdateAllowed) {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                updateLauncher,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                            )
                        } catch (e: Exception) {
                            // 업데이트 플로우 시작 실패 시에도 앱 진입을 위해 완료 처리
                        }
                    }
                }
            }
            // 업데이트가 없거나, 에러가 발생했거나, 업데이트 플로우가 시작되었든 간에
            // 스플래시 화면 제어를 위해 완료 콜백 호출
            onComplete()
        }
    }

    /**
     * 앱이 Resume 될 때 미완료된 업데이트 확인 (Immediate의 경우 필수)
     */
    fun onResumeCheck(activity: Activity, updateLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // 이미 진행 중인 업데이트가 있다면 다시 시작 (Immediate)
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }
}
