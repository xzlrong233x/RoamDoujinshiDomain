package com.xlrr.roambendom.network

import androidx.compose.runtime.mutableStateListOf
import okhttp3.Dns
import java.net.InetAddress

object RBDDns : Dns {
    val mainNH = mutableListOf(
        InetAddress.getByName("104.26.4.188"),
        InetAddress.getByName("104.26.5.188")
    )

    val imgNH = mutableStateListOf(
        InetAddress.getByName("185.23.214.98"),
        InetAddress.getByName("213.152.165.53"),
        InetAddress.getByName("109.202.100.226"),
        InetAddress.getByName("213.152.165.54"),
        InetAddress.getByName("77.247.178.1"),
        InetAddress.getByName("109.202.100.218")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        if (Regex("[it]\\d.nhentai.net").matches(hostname)) {
            imgNH.shuffle()
            return imgNH
        }
        else if (hostname.endsWith("nhentai.net")) {
            mainNH.shuffle()
            return mainNH
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}
