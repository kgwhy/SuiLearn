package com.suilearn.ui.model

import com.suilearn.core.common.AppResult

internal fun <T> AppResult<T>.dataOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> null
}
