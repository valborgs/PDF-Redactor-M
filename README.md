# PDF Redactor M

안드로이드 환경에서 PDF 파일의 개인정보(PII)를 탐지하고 마스킹(Redaction)할 수 있는 애플리케이션입니다.

## 📱 프로젝트 개요
- **목표**: 모바일 기기에서 손쉽게 PDF 내 민감 정보를 가리고 안전하게 공유/저장.
- **주요 기능**:
    - PDF 파일 불러오기 및 뷰어
    - 정규식 기반 PII 자동 탐지 (전화번호, 주민번호 등)
    - 터치 드래그를 통한 수동 마스킹
    - 마스킹 된 PDF 내보내기 (Flattening/Redaction)
    - 작업 내역 자동 저장 (Room DB)

## 🛠 기술 스택
- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose (Material3)
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Local DB**: Room
- **Network**: Retrofit (Future proofing)
- **PDF Engine**: PdfBox-Android (Primary), Android PdfRenderer (Viewer)

## 📂 문서
- [개발 계획서](docs/development_plan.md)

## 🚀 현재 진행 상황
- [x] 프로젝트 초기화 및 Gradle 설정 (Kotlin 2.2.21, Version Catalog)
- [x] 개발 계획서 및 기술 스택 선정 완료
- [ ] Clean Architecture 레이어 구현 (진행 예정)
