package org.comon.pdfredactorm.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    val pages = getHelpPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "사용 설명서",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기"
                            )
                        }
                    }

                    Divider()

                    // Content area with HorizontalPager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        HelpPageContent(helpPage = pages[page])
                    }

                    // Page indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pages.size) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    Divider()

                    // Navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous button
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            ) {
                                Text("이전")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Next button
                        if (pagerState.currentPage < pages.size - 1) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            ) {
                                Text("다음")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpPageContent(helpPage: HelpPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = helpPage.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = helpPage.content,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

data class HelpPage(
    val title: String,
    val content: String
)

fun getHelpPages(): List<HelpPage> {
    return listOf(
        HelpPage(
            title = "PDF Redactor에 오신 것을 환영합니다",
            content = """
                PDF Redactor는 PDF 문서의 민감한 정보를 안전하게 가릴 수 있는 앱입니다.
                
                주요 기능:
                • PDF 파일 불러오기
                • 텍스트 및 이미지 영역 마스킹
                • 수정된 PDF 저장
                
                이 가이드를 통해 앱 사용 방법을 배워보세요.
            """.trimIndent()
        ),
        HelpPage(
            title = "1. PDF 파일 열기",
            content = """
                PDF 파일을 열려면:
                
                1. 홈 화면 오른쪽 하단의 '+' 버튼을 탭합니다.
                2. 기기에서 PDF 파일을 선택합니다.
                3. 파일이 로드되면 편집 화면으로 이동합니다.
                
                최근에 작업한 프로젝트는 홈 화면에 목록으로 표시됩니다.
            """.trimIndent()
        ),
        HelpPage(
            title = "2. 영역 마스킹하기",
            content = """
                민감한 정보를 가리려면:
                
                1. PDF 페이지에서 가리고 싶은 영역을 터치합니다.
                2. 손가락을 드래그하여 마스킹 영역을 그립니다.
                3. 손가락을 떼면 해당 영역이 검은색으로 가려집니다.
                
                여러 영역을 마스킹할 수 있으며, 각 영역은 개별적으로 관리됩니다.
            """.trimIndent()
        ),
        HelpPage(
            title = "3. 페이지 탐색",
            content = """
                여러 페이지가 있는 PDF를 탐색하려면:
                
                • 화면 하단의 페이지 번호를 확인합니다.
                • 좌우 화살표 버튼으로 이전/다음 페이지로 이동합니다.
                • 각 페이지마다 독립적으로 마스킹 작업을 수행할 수 있습니다.
            """.trimIndent()
        ),
        HelpPage(
            title = "4. PDF 저장하기",
            content = """
                마스킹 작업을 완료한 후:
                
                1. 상단의 저장 버튼을 탭합니다.
                2. 수정된 PDF가 기기에 저장됩니다.
                3. 저장 위치는 시스템 알림으로 확인할 수 있습니다.
                
                원본 파일은 변경되지 않으며, 새로운 파일로 저장됩니다.
            """.trimIndent()
        ),
        HelpPage(
            title = "도움이 필요하신가요?",
            content = """
                추가 지원이 필요하시면:
                
                • 앱 정보에서 버전 정보를 확인하세요.
                • 문제가 발생하면 앱을 재시작해보세요.
                • 프로젝트를 삭제하려면 홈 화면에서 휴지통 아이콘을 탭하세요.
                
                문의사항은 voll1212@gmail.com으로 연락주세요.
                
                PDF Redactor를 사용해주셔서 감사합니다!
            """.trimIndent()
        )
    )
}
