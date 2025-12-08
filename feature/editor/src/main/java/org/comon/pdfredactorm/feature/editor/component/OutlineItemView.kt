package org.comon.pdfredactorm.feature.editor.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comon.pdfredactorm.core.model.PdfOutlineItem
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun OutlineItemView(
    item: PdfOutlineItem,
    level: Int,
    onClick: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item.pageIndex) }
                .padding(
                    start = (16 * level).dp + 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.outline_page_format, item.pageIndex + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Render children recursively
        item.children.forEach { child ->
            OutlineItemView(
                item = child,
                level = level + 1,
                onClick = onClick
            )
        }
    }
}
