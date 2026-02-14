package com.xlrr.roambendom.network

import androidx.compose.runtime.mutableStateListOf
import okhttp3.Dns
import java.net.InetAddress

object RBDDns : Dns {
    val mainNH = listOf(
        InetAddress.getByName("104.26.4.188"),
        InetAddress.getByName("104.26.5.188")
    )

    val imgNH = listOf(
        InetAddress.getByName("185.23.214.98"),
        InetAddress.getByName("213.152.165.53"),
        InetAddress.getByName("109.202.100.226"),
        InetAddress.getByName("213.152.165.54"),
        InetAddress.getByName("77.247.178.1"),
        InetAddress.getByName("109.202.100.218")
    )

    val mainPIXIV = listOf(
        InetAddress.getByName("210.140.139.154"),
        InetAddress.getByName("210.140.139.155"),
        InetAddress.getByName("210.140.139.156"),
        InetAddress.getByName("210.140.139.157"),
        InetAddress.getByName("210.140.139.158"),
        InetAddress.getByName("210.140.139.159"),
        InetAddress.getByName("210.140.139.160"),
        InetAddress.getByName("210.140.139.161"),
        InetAddress.getByName("210.140.139.162"),
    )

    val imgPIXIV = listOf(
        InetAddress.getByName("210.140.92.141"),
        InetAddress.getByName("210.140.92.142"),
        InetAddress.getByName("210.140.92.143"),
        InetAddress.getByName("210.140.92.144"),
        InetAddress.getByName("210.140.92.145"),
        InetAddress.getByName("210.140.92.146"),
        InetAddress.getByName("210.140.92.148"),
        InetAddress.getByName("210.140.92.149"),
        InetAddress.getByName("210.140.139.131"),
        InetAddress.getByName("210.140.139.132"),
        InetAddress.getByName("210.140.139.133"),
        InetAddress.getByName("210.140.139.134"),
        InetAddress.getByName("210.140.139.135"),
        InetAddress.getByName("210.140.139.136"),
    )

    override fun lookup(hostname: String): List<InetAddress> {
        if (Regex("[it]\\d.nhentai.net").matches(hostname)) {
            return imgNH
        }
        else if (hostname.endsWith("nhentai.net")) {
            return mainNH
        }
        if (hostname.endsWith("pixiv.net")) {
            return mainPIXIV
        }
        if (hostname.endsWith("pximg.net")) {
            return imgPIXIV
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}
