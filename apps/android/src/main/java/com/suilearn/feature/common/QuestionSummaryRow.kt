package com.suilearn.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suilearn.ui.model.QuestionSummaryUiModel

@Composable
fun QuestionSummaryRow(
    model: QuestionSummaryUiModel,
    leadingIcon: ImageVector,
    trailingAction: String?,
    onTrailingAction: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(leadingIcon, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(model.stem, fontWeight = FontWeight.SemiBold)
                if (model.categoryName.isNotBlank()) {
                    Text(model.categoryName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (model.knowledgePointNames.isNotEmpty()) {
                    Text(model.knowledgePointNames.joinToString("、"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${model.questionId} | ${model.auxText}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingAction != null && onTrailingAction != null) {
                OutlinedButton(onClick = onTrailingAction) {
                    Text(trailingAction)
                }
            }
        }
    }
}
