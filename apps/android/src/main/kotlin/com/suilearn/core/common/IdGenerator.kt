package com.suilearn.core.common

fun interface IdGenerator {
    fun newId(): String
}

object UuidIdGenerator : IdGenerator {
    override fun newId(): String = java.util.UUID.randomUUID().toString()
}
