package com.xlrr.roambendom.data.query

open class OpTerm(
    k: String,
    v: Int,
    val operator: TermOperator,
    exclude: Boolean
) : KeywordTerm(k, v.toString(), exclude) {
    override fun serializeKeyValue(): String = "$operator${valueWithQuotation()}"
}

enum class TermOperator {
    GT,
    GTE,
    LT,
    LTE;

    override fun toString(): String {
        return when (this) {
            GT -> ">"
            GTE -> ">="
            LT -> "<"
            LTE -> "<="
        }
    }
}