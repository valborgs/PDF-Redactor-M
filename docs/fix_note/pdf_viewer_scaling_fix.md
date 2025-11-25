# PDF 뷰어 스케일링 개선: 다양한 비율의 문서 호환성 확보

## 1. 개요 (Overview)

**문제 상황**:
PDF 뷰어에서 세로형(Portrait) 문서는 정상적으로 표시되었으나, PPT 슬라이드와 같이 가로로 긴(Landscape) 문서나 화면보다 폭이 넓은 문서를 열었을 때, **화면의 오른쪽 부분이 잘려서 보이지 않는 문제**가 발생했습니다. 1배율(기본 상태)에서는 드래그가 불가능하여 사용자가 내용을 확인하기 위해 강제로 축소하거나 확대해야 하는 불편함이 있었습니다.

**목표**:
문서의 가로/세로 비율이나 단말기의 화면 비율과 관계없이, **초기 로딩 시 문서의 전체 내용이 화면에 온전하게 표시(Fit to Screen)** 되도록 개선합니다.

---

## 2. 원인 파악 (Root Cause Analysis)

코드를 분석한 결과, 두 가지 주요 원인이 식별되었습니다.

1.  **고정된 비트맵 렌더링 방식 (`EditorViewModel`)**:
    *   기존 로직은 PDF 페이지를 비트맵으로 변환할 때, 무조건 **너비를 2048px로 고정**하고 높이를 비율에 맞춰 계산했습니다.
    *   이는 세로 문서에는 적합하지만, 가로 문서의 경우 높이가 너무 작아지거나, 반대로 세로가 매우 긴 문서의 경우 너비가 고정되어 해상도 손실이 발생할 수 있는 구조였습니다.

2.  **단순한 화면 스케일링 로직 (`EditorScreen`)**:
    *   화면에 비트맵을 표시할 때, **높이(Height)만을 기준**으로 스케일(`fitScale`)을 계산했습니다.
    *   `canvasHeight / bitmapHeight` 공식만 사용했기 때문에, 문서의 너비가 화면보다 넓은 경우(가로 문서)에는 너비가 화면 밖으로 벗어나는 현상이 발생했습니다.

---

## 3. 문제 해결 (Solution)

문서의 전체가 항상 화면 안에 들어오도록(Contain), **너비와 높이 중 더 제한적인 쪽을 기준**으로 스케일링하도록 로직을 변경했습니다.

### 3.1. 비트맵 생성 최적화 (`EditorViewModel`)
PDF 페이지를 비트맵으로 렌더링할 때, 가로/세로 중 **더 긴 쪽을 최대 크기(2048px)로 제한**하고, 원본 비율(Aspect Ratio)을 유지하도록 수정했습니다.

*   **가로 문서**: 너비를 2048px로 제한, 높이는 비율에 맞춰 계산.
*   **세로 문서**: 높이를 2048px로 제한, 너비는 비율에 맞춰 계산.

### 3.2. 화면 스케일링 로직 개선 (`EditorScreen`)
화면에 표시할 때, 너비 기준 스케일과 높이 기준 스케일을 모두 계산한 뒤, **더 작은 값(`minOf`)을 적용**했습니다.

*   `widthScale = canvasWidth / bitmapWidth`
*   `heightScale = canvasHeight / bitmapHeight`
*   `fitScale = min(widthScale, heightScale)`

이로써 가로가 긴 문서는 너비에 맞춰지고, 세로가 긴 문서는 높이에 맞춰져 **어떤 비율의 문서든 잘림 없이 전체가 표시**됩니다.

---

## 4. 코드 비교 (Code Comparison)

### 4.1. EditorViewModel.kt (비트맵 렌더링)

**Before (수정 전)**: 너비 고정 방식
```kotlin
// 무조건 너비를 2048로 고정
val width = 2048
val height = (width.toFloat() / page.width * page.height).toInt()
```

**After (수정 후)**: 최대 차원(Max Dimension) 제한 방식
```kotlin
val maxDimension = 2048
val pageAspectRatio = page.width.toFloat() / page.height.toFloat()

val width: Int
val height: Int

if (page.width > page.height) {
    // 가로가 더 긴 경우: 너비를 제한
    width = minOf(maxDimension, page.width * 2)
    height = (width / pageAspectRatio).toInt()
} else {
    // 세로가 더 긴 경우: 높이를 제한
    height = minOf(maxDimension, page.height * 2)
    width = (height * pageAspectRatio).toInt()
}
```

### 4.2. EditorScreen.kt (화면 표시)

**Before (수정 전)**: 높이 기준 맞춤
```kotlin
// 캔버스 높이와 비트맵 높이만 비교
val fitScale = remember(bitmap, canvasSize) {
    if (canvasSize.height > 0 && bitmap.height > 0) {
        canvasSize.height.toFloat() / bitmap.height.toFloat()
    } else {
        1f
    }
}
```

**After (수정 후)**: 전체 맞춤 (Contain)
```kotlin
// 너비 비율과 높이 비율 중 더 작은 값을 선택 (전체 표시 보장)
val fitScale = remember(bitmap, canvasSize) {
    if (canvasSize.height > 0 && canvasSize.width > 0 && bitmap.height > 0 && bitmap.width > 0) {
        val widthScale = canvasSize.width.toFloat() / bitmap.width.toFloat()
        val heightScale = canvasSize.height.toFloat() / bitmap.height.toFloat()
        
        // minOf를 사용하여 화면 밖으로 나가는 것을 방지
        minOf(widthScale, heightScale)
    } else {
        1f
    }
}
```

---

## 5. 결과 (Result)

이 개선을 통해 다음과 같은 효과를 얻었습니다.

1.  **호환성 향상**: A4 용지뿐만 아니라 PPT 슬라이드(16:9), 영수증 등 다양한 비율의 PDF 문서를 완벽하게 지원합니다.
2.  **사용자 경험(UX) 개선**: 문서를 열자마자 전체 내용을 한눈에 파악할 수 있어, 불필요한 축소/이동 조작이 제거되었습니다.
3.  **렌더링 효율성**: 문서 비율에 맞춰 비트맵 크기를 최적화하여 메모리 사용과 화질 간의 균형을 맞췄습니다.
