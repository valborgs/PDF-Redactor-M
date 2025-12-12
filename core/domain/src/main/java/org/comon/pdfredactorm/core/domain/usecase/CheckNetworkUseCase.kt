package org.comon.pdfredactorm.core.domain.usecase

import org.comon.pdfredactorm.core.common.network.NetworkChecker
import javax.inject.Inject

/**
 * 네트워크 연결 상태를 확인하는 UseCase
 * 
 * ViewModel에서 네트워크가 필요한 작업 전에 호출하여
 * 연결 상태에 따라 적절한 분기 처리를 수행합니다.
 */
class CheckNetworkUseCase @Inject constructor(
    private val networkChecker: NetworkChecker
) {
    /**
     * 현재 인터넷 연결이 가능한지 확인합니다.
     * @return 인터넷 연결이 가능하면 true, 아니면 false
     */
    operator fun invoke(): Boolean = networkChecker.isNetworkAvailable()
}
