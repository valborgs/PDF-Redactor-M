package org.comon.pdfredactorm.core.common.network

/**
 * 네트워크 연결 상태를 확인하는 인터페이스
 */
interface NetworkChecker {
    /**
     * 현재 인터넷 연결이 가능한지 확인합니다.
     * @return 인터넷 연결이 가능하면 true, 아니면 false
     */
    fun isNetworkAvailable(): Boolean
}
