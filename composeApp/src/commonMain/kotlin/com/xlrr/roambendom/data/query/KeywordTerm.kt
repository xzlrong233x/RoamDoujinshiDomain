package com.xlrr.roambendom.data.query

open class KeywordTerm(
    val key: String,
    v: String,
    exclude: Boolean
) : BaseTerm(v, exclude) {
    override fun serializeValue(): String = "$key:${serializeKeyValue()}"

    protected open fun serializeKeyValue() = valueWithQuotation()
}