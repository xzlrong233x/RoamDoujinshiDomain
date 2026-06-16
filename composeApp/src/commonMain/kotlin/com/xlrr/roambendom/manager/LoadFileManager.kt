package com.xlrr.roambendom.manager

import io.github.vinceglb.filekit.utils.div
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

abstract class LoadFileManager(private val path: String) {
    private var _init = false
    private var dataPath = Path("")

    fun init(dp: String) {
        if (_init) return
        if (!SystemFileSystem.exists(Path(dp))) SystemFileSystem.createDirectories(Path(dp))
        val p = Path(dp).div(path)
        dataPath = p
        if (SystemFileSystem.exists(p)) {
            load(p)
        }
        _init = true
    }

    abstract fun load(p:Path)
    fun isInit(): Boolean = _init
    fun save() {
        val buf = SystemFileSystem.sink(dataPath).buffered()
        writeTo(buf)
        buf.flush()
        buf.close()
    }
    abstract fun writeTo(buffer: Sink)
}