package com.suilearn.feature.statistics

sealed interface StatisticsEvent {
    object Refresh : StatisticsEvent
}
