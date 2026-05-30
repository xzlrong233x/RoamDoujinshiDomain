package com.xlrr.roambendom.data.query

open class BaseTerm(
    val value: String = "",
    var exclude: Boolean = false
) {
    open fun serialize() = "${if (exclude) '-' else ""}${serializeValue()}"
    protected open fun serializeValue() = valueWithQuotation()
    fun valueWithQuotation() = if (value.contains(' ')) "\"$value\"" else value
}