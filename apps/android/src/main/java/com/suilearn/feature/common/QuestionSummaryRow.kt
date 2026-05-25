package com.suilearn.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.model.QuestionSummaryUiModel

@Composable
fun QuestionSummaryRow(
    model: QuestionSummaryUiModel,
    leadingIcon: ImageVector,
    trailingAction: String?,
    onTrailingAction: (() -> Unit)?,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(icon = leadingIcon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(model.stem, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (model.categoryName.isNotBlank()) {
                    Text(model.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (model.knowledgePointNames.isNotEmpty()) {
                    Text(model.knowledgePointNames.joinToString("、"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${model.questionId} | ${model.auxText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if ((trailingAction != null && onTrailingAction != null) || (secondaryAction != null && onSecondaryAction != null)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trailingAction != null && onTrailingAction != null) {
                        AppOutlinedActionButton(text = trailingAction, onClick = onTrailingAction)
                    }
                    if (secondaryAction != null && onSecondaryAction != null) {
                        AppOutlinedActionButton(text = secondaryAction, onClick = onSecondaryAction)
                    }
                }
            }
        }
    }
}
