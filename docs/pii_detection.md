# PII Detection Feature (개인정보 탐지 기능)

## 개요

PDF Redactor M은 PDF 문서에서 개인정보(PII - Personally Identifiable Information)를 자동으로 탐지하는 기능을 제공합니다. 이 기능은 정규표현식 기반 패턴 매칭을 사용하여 다양한 유형의 개인정보를 식별합니다.

## 지원하는 개인정보 유형

### 1. 주민번호 (Resident Registration Number)
- **타입**: `RedactionType.RRN`
- **패턴**: `\d{6}[-\s]?\d{7}`
- **예시**:
  - 123456-1234567
  - 123456 1234567
  - 1234561234567
- **설명**: 6자리 생년월일 + 하이픈(선택) + 7자리 등록번호

### 2. 개인전화번호 (Mobile Phone Number)
- **타입**: `RedactionType.PHONE_NUMBER`
- **패턴**: `010[-\s]?\d{4}[-\s]?\d{4}`
- **예시**:
  - 010-1234-5678
  - 010 1234 5678
  - 01012345678
- **설명**: 010으로 시작하는 한국 휴대전화 번호

### 3. 이메일 (Email Address)
- **타입**: `RedactionType.EMAIL`
- **패턴**: `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}`
- **예시**:
  - user@example.com
  - john.doe@company.co.kr
  - info+test@domain.org
- **설명**: 표준 이메일 주소 형식

### 4. 생년월일 (Date of Birth)
- **타입**: `RedactionType.BIRTH_DATE`
- **패턴**: `(19|20)\d{2}[-./\s]?(0[1-9]|1[0-2])[-./\s]?(0[1-9]|[12]\d|3[01])`
- **예시**:
  - 1990-01-15
  - 1985.12.31
  - 20001225
  - 1995/06/20
- **설명**: 1900년대 또는 2000년대의 날짜를 YYYY-MM-DD, YYYY.MM.DD, YYYYMMDD 등의 형식으로 탐지

### 5. 주소 (Physical Address)
- **타입**: `RedactionType.ADDRESS`
- **패턴**: `(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)[^\n]{0,50}(시|구|군|읍|면|동|로|길)[\s\d-]*`
- **예시**:
  - 서울특별시 강남구 테헤란로 123
  - 경기도 성남시 분당구 정자동
  - 부산광역시 해운대구 센텀로 45
- **설명**: 한국 행정구역을 포함하는 주소 패턴 (시/도로 시작하고 시/구/군/동/로/길 포함)

## 아키텍처

PII 탐지 기능은 Clean Architecture 원칙을 따라 세 개의 레이어로 구성됩니다:

### Domain Layer (도메인 레이어)

#### Models
- **`DetectedPii`** (`domain/model/DetectedPii.kt`): 탐지된 개인정보 데이터 모델
  ```kotlin
  data class DetectedPii(
      val text: String,           // 탐지된 텍스트
      val type: RedactionType,    // PII 타입
      val pageIndex: Int,         // 페이지 인덱스
      val x: Float,               // X 좌표 (UI 좌표계)
      val y: Float,               // Y 좌표 (UI 좌표계)
      val width: Float,           // 너비
      val height: Float           // 높이
  )
  ```

- **`RedactionType`** (`domain/model/RedactionMask.kt`): PII 타입 열거형
  ```kotlin
  enum class RedactionType {
      MANUAL,         // 수동 마스킹
      PHONE_NUMBER,   // 전화번호
      EMAIL,          // 이메일
      RRN,           // 주민번호
      BIRTH_DATE,    // 생년월일
      ADDRESS        // 주소
  }
  ```

#### Repository Interface
- **`PdfRepository`** (`domain/repository/PdfRepository.kt`):
  ```kotlin
  suspend fun detectPii(file: File, pageIndex: Int): List<DetectedPii>
  suspend fun detectPiiInAllPages(file: File): List<DetectedPii>
  ```

#### Use Cases
- **`DetectPiiUseCase`** (`domain/usecase/DetectPiiUseCase.kt`): PII 탐지 비즈니스 로직
  - `detectInPage()`: 특정 페이지에서 PII 탐지
  - `detectInAllPages()`: 모든 페이지에서 PII 탐지
  - `convertToRedactionMasks()`: DetectedPii를 RedactionMask로 변환

### Data Layer (데이터 레이어)

#### PII Detection Components
- **`PiiPatterns`** (`data/pii/PiiPatterns.kt`): PII 탐지 정규표현식 패턴 정의
  - 각 PII 타입별 정규표현식 패턴 관리
  - `detectAll()` 메서드로 모든 패턴 매칭 수행

- **`PiiTextStripper`** (`data/pii/PiiTextStripper.kt`): PDFBox TextStripper 확장
  - PDF에서 텍스트와 위치 정보 추출
  - `TextWithPosition` 데이터로 텍스트의 좌표 및 크기 정보 제공

#### Repository Implementation
- **`PdfRepositoryImpl`** (`data/repository/PdfRepositoryImpl.kt`):
  - PDFBox를 사용한 PDF 텍스트 추출
  - PII 패턴 매칭 및 좌표 계산
  - PDF 좌표계(bottom-left)를 UI 좌표계(top-left)로 변환

## 사용 방법

### 1. 의존성 주입
```kotlin
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val detectPiiUseCase: DetectPiiUseCase
) : ViewModel()
```

### 2. 특정 페이지에서 PII 탐지
```kotlin
viewModelScope.launch {
    detectPiiUseCase.detectInPage(pdfFile, pageIndex).onSuccess { detectedPiiList ->
        // 탐지된 PII 처리
        detectedPiiList.forEach { pii ->
            println("Found ${pii.type}: ${pii.text} at page ${pii.pageIndex}")
        }
    }.onFailure { exception ->
        // 오류 처리
        println("PII detection failed: ${exception.message}")
    }
}
```

### 3. 모든 페이지에서 PII 탐지
```kotlin
viewModelScope.launch {
    detectPiiUseCase.detectInAllPages(pdfFile).onSuccess { detectedPiiList ->
        // 탐지된 모든 PII 처리
        val groupedByType = detectedPiiList.groupBy { it.type }
        groupedByType.forEach { (type, piiList) ->
            println("$type: ${piiList.size} items found")
        }
    }
}
```

### 4. DetectedPii를 RedactionMask로 변환
```kotlin
viewModelScope.launch {
    detectPiiUseCase.detectInAllPages(pdfFile).onSuccess { detectedPiiList ->
        // RedactionMask로 변환하여 자동 마스킹 적용
        val redactionMasks = detectPiiUseCase.convertToRedactionMasks(detectedPiiList)

        // 마스킹 저장
        saveRedactionsUseCase(pdfDocument.id, redactionMasks)
    }
}
```

## 좌표 시스템

### PDF 좌표계 vs UI 좌표계
- **PDF 좌표계**: 원점이 페이지 좌측 하단 (bottom-left origin)
- **UI 좌표계**: 원점이 페이지 좌측 상단 (top-left origin)

### 좌표 변환
PdfRepositoryImpl에서 자동으로 좌표 변환이 수행됩니다:
```kotlin
val pageHeight = page.mediaBox.height
val uiY = pageHeight - pdfY - height
```

## 성능 고려사항

### 페이지별 탐지 vs 전체 페이지 탐지
- **페이지별 탐지** (`detectInPage`):
  - 사용자가 현재 보고 있는 페이지만 처리
  - 빠른 응답 시간
  - 실시간 탐지에 적합

- **전체 페이지 탐지** (`detectInAllPages`):
  - 모든 페이지를 순회하며 처리
  - 대용량 PDF의 경우 시간이 오래 걸릴 수 있음
  - 백그라운드 작업으로 처리 권장

### 최적화 방안
1. **페이지네이션**: 현재 페이지만 탐지
2. **캐싱**: 탐지 결과를 메모리나 DB에 캐시
3. **코루틴 활용**: IO 작업을 비동기로 처리
4. **Progress 표시**: 전체 페이지 탐지 시 진행 상황 표시

## 제한사항

### 1. 텍스트 위치 정확도
- 현재 구현은 텍스트 위치를 추정(estimation) 방식으로 계산
- 복잡한 레이아웃에서는 위치가 부정확할 수 있음
- 개선: PDFBox의 TextPosition을 더 세밀하게 활용

### 2. 이미지 내 텍스트
- 스캔된 PDF나 이미지 내 텍스트는 탐지 불가
- OCR(Optical Character Recognition)이 필요
- 향후 Tesseract 등의 OCR 라이브러리 통합 고려

### 3. 패턴 매칭 한계
- 정규표현식 기반이므로 형식이 다른 경우 탐지 실패
- 예: "일구구영년 일월 십오일" 같은 한글 날짜는 탐지 불가
- 개선: ML/AI 기반 탐지 알고리즘 도입

### 4. 오탐지 (False Positives)
- 주소가 아닌데 "시", "구", "동" 등이 포함된 경우
- 생년월일이 아닌 일반 날짜 (예: 계약일, 만료일)
- 개선: 컨텍스트 분석 및 머신러닝 모델 활용

## 확장 가능성

### 추가 PII 타입
새로운 PII 타입을 추가하려면:

1. `RedactionType`에 새 타입 추가:
   ```kotlin
   enum class RedactionType {
       // 기존 타입들...
       PASSPORT_NUMBER,    // 여권번호
       BANK_ACCOUNT       // 계좌번호
   }
   ```

2. `PiiPatterns`에 새 패턴 추가:
   ```kotlin
   PiiPattern(
       type = RedactionType.PASSPORT_NUMBER,
       regex = Regex("""[A-Z]\d{8}"""),
       description = "Passport Number (여권번호)"
   )
   ```

### 커스텀 패턴
사용자가 직접 패턴을 정의할 수 있도록 확장:
- UI에서 정규표현식 입력
- 커스텀 패턴을 데이터베이스에 저장
- 런타임에 동적으로 패턴 추가

### ML/AI 기반 탐지
- TensorFlow Lite를 사용한 온디바이스 ML 모델
- 서버 기반 NER(Named Entity Recognition) API 연동
- 컨텍스트를 고려한 더 정확한 탐지

## 테스트

### 단위 테스트
```kotlin
@Test
fun `detect phone number in text`() {
    val text = "연락처: 010-1234-5678"
    val matches = PiiPatterns.detectAll(text)

    assertEquals(1, matches.size)
    assertEquals(RedactionType.PHONE_NUMBER, matches[0].first)
    assertEquals("010-1234-5678", matches[0].second.value)
}
```

### 통합 테스트
```kotlin
@Test
fun `detect PII in sample PDF`() = runBlocking {
    val samplePdf = File("test_data/sample_with_pii.pdf")
    val result = detectPiiUseCase.detectInAllPages(samplePdf)

    result.onSuccess { detectedPiiList ->
        assertTrue(detectedPiiList.isNotEmpty())
    }
}
```

## 보안 고려사항

1. **메모리 관리**: 탐지된 PII 텍스트를 메모리에 오래 보관하지 않음
2. **로깅**: 민감한 정보를 로그에 출력하지 않음
3. **권한**: 파일 접근 권한 확인
4. **데이터 암호화**: 탐지 결과를 저장할 경우 암호화 적용

## 참고 자료

- [PDFBox-Android Documentation](https://github.com/TomRoush/PdfBox-Android)
- [Kotlin Regex Documentation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/)
- [개인정보보호법 (Personal Information Protection Act)](https://www.law.go.kr/)
