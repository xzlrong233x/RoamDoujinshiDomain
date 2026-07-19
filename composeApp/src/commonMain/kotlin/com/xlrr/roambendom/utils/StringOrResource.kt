package com.xlrr.roambendom.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class StringOrResource {
    var string: String = ""
        private set
    private var resource: StringResource? = null
    private val list = arrayListOf<Any>()
    private constructor(string: String) {
        this.string = string
    }
    private constructor(resource: StringResource) {
        this.resource = resource
    }

    @Composable
    fun getComposeOrString(): String {
        resource?.let { return stringResource(it).format(*list.toTypedArray()) }
        return string
    }

    @Composable
    fun isNotEmpty(): Boolean {
        return getComposeOrString().isNotEmpty()
    }

    fun argv(vararg args: Any): StringOrResource {
        list.addAll(args)
        return this
    }

    fun clear(): StringOrResource {
        list.clear()
        return this
    }

    companion object {
        fun new(string: String): StringOrResource {
            return StringOrResource(string)
        }

        fun new(resource: StringResource): StringOrResource {
            return StringOrResource(resource)
        }

        val EMPTY = StringOrResource("")
    }
}

fun String.orResource(vararg argv: Any): StringOrResource {
    return StringOrResource.new(this).argv(*argv)
}

fun StringResource.orResource(vararg argv: Any): StringOrResource {
    return StringOrResource.new(this).argv(*argv)
}