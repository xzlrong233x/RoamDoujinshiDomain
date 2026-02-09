package com.xlrr.roambendom.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator(start: Any) {
    val backStack: SnapshotStateList<Any> = mutableStateListOf(start)

    fun replace(vararg routes: Any) {
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.addAll(routes)
        }
    }

    fun navigateTo(route: Any) {
        Snapshot.withMutableSnapshot {
            backStack.removeLastOrNull()
            backStack.add(route)
        }
    }

    fun push(route: Any) {
        backStack.add(route)
    }

    fun back() {
        Snapshot.withMutableSnapshot {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        }
    }

    fun defaultBack(default: Any = Routes.Root.Default) {
        Snapshot.withMutableSnapshot {
            if (backStack.size > 1) back()
            else navigateTo(default)
        }
    }
}