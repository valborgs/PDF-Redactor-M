package org.comon.pdfredactorm.feature.home

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.pdfredactorm.core.common.analytics.AnalyticsTracker
import org.comon.pdfredactorm.core.common.logger.Logger
import org.comon.pdfredactorm.core.domain.usecase.LoadPdfUseCase
import org.comon.pdfredactorm.core.domain.usecase.ValidateCodeUseCase
import org.comon.pdfredactorm.core.domain.usecase.CheckNetworkUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.DeletePdfDocumentUseCase
import org.comon.pdfredactorm.core.domain.usecase.pdf.GetRecentProjectsUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetAppUuidUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetFirstLaunchUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.GetProStatusUseCase
import org.comon.pdfredactorm.core.domain.usecase.settings.SetFirstLaunchUseCase
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val getRecentProjectsUseCase: GetRecentProjectsUseCase,
    private val deletePdfDocumentUseCase: DeletePdfDocumentUseCase,
    private val loadPdfUseCase: LoadPdfUseCase,
    private val validateCodeUseCase: ValidateCodeUseCase,
    private val getFirstLaunchUseCase: GetFirstLaunchUseCase,
    private val setFirstLaunchUseCase: SetFirstLaunchUseCase,
    private val getProStatusUseCase: GetProStatusUseCase,
    private val getAppUuidUseCase: GetAppUuidUseCase,
    private val checkNetworkUseCase: CheckNetworkUseCase,
    private val logger: Logger,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    // Internal mutable states
    private val _isLoading = MutableStateFlow(false)
    private val _isFirstLaunch = MutableStateFlow(false)

    // Side Effect Channel
    private val _sideEffect = Channel<HomeSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    // Combined UI State
    val uiState: StateFlow<HomeUiState> = combine(
        _isLoading,
        _isFirstLaunch,
        getRecentProjectsUseCase(),
        getProStatusUseCase()
    ) { isLoading, isFirstLaunch, recentProjects, isProEnabled ->
        HomeUiState(
            isLoading = isLoading,
            isFirstLaunch = isFirstLaunch,
            recentProjects = recentProjects,
            isProEnabled = isProEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            val isFirstLaunch = getFirstLaunchUseCase()
            if (isFirstLaunch) {
                _isFirstLaunch.value = true
                setFirstLaunchUseCase(false)
            }
        }
    }

    /**
     * MVI Event Handler
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ConsumeFirstLaunch -> consumeFirstLaunch()
            is HomeEvent.LoadPdf -> loadPdf(event.file)
            is HomeEvent.ValidateCode -> validateCode(event.email, event.code)
            is HomeEvent.DeleteProject -> deleteProject(event.pdfId)
            is HomeEvent.CoffeeChatClicked -> onCoffeeChatClicked()
        }
    }

    private fun onCoffeeChatClicked() {
        analyticsTracker.logEvent("open_coffeechat")
    }

    private fun consumeFirstLaunch() {
        _isFirstLaunch.value = false
    }

    private fun loadPdf(file: File) {
        viewModelScope.launch {
            logger.info("User selected PDF file: ${file.name}")
            val result = loadPdfUseCase(file)
            result.onSuccess { document ->
                _sideEffect.send(HomeSideEffect.NavigateToEditor(document.id))
            }.onFailure {
                logger.warning("PDF load failed")
            }
        }
    }

    private fun validateCode(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 네트워크 연결 확인
            if (!checkNetworkUseCase()) {
                logger.info("Network unavailable for code validation")
                _sideEffect.send(HomeSideEffect.ShowNetworkError)
                _isLoading.value = false
                return@launch
            }
            
            try {
                val result = validateCodeUseCase(email, code)
                result.onSuccess {
                    analyticsTracker.logEvent("pro_activated")
                    _sideEffect.send(
                        HomeSideEffect.ShowValidationResult(
                            Result.success(application.getString(R.string.pro_activation_success))
                        )
                    )
                }.onFailure { e ->
                    _sideEffect.send(HomeSideEffect.ShowValidationResult(Result.failure(e)))
                }
            } catch (e: Exception) {
                _sideEffect.send(HomeSideEffect.ShowValidationResult(Result.failure(e)))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun deleteProject(pdfId: String) {
        viewModelScope.launch {
            logger.info("User deleted project: $pdfId")
            deletePdfDocumentUseCase(pdfId)
        }
    }

    // Native Ad Preloading
    private val _preloadedNativeAd = MutableStateFlow<NativeAd?>(null)
    val preloadedNativeAd: StateFlow<NativeAd?> = _preloadedNativeAd

    /**
     * 네이티브 광고를 사전 로드합니다.
     * 홈 화면 진입 시 호출되어 종료 다이얼로그에서 즉시 광고를 표시할 수 있도록 합니다.
     */
    // INTERNET 권한은 이미 app 모듈에서 선언해놓았으나 lint가 여기 모듈에서 권한을 못찾고 있으므로 @SupressLint 어노테이션 추가
    @SuppressLint("MissingPermission")
    fun preloadNativeAd(adUnitId: String) {
        // 이미 로드된 광고가 있으면 새로 로드하지 않음
        if (_preloadedNativeAd.value != null) return

        val adLoader = AdLoader.Builder(application, adUnitId)
            .forNativeAd { ad: NativeAd ->
                // 기존 광고가 있으면 destroy
                _preloadedNativeAd.value?.destroy()
                _preloadedNativeAd.value = ad
                logger.info("Native ad preloaded successfully")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logger.warning("Native ad failed to preload: ${adError.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun onCleared() {
        super.onCleared()
        _preloadedNativeAd.value?.destroy()
        _preloadedNativeAd.value = null
    }
}

