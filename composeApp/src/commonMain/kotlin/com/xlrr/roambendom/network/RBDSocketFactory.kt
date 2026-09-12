package com.xlrr.roambendom.network

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class RBDSocketFactory(val oriFactory: SSLSocketFactory) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<out String?>? = oriFactory.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<out String?>? =oriFactory.supportedCipherSuites

    override fun createSocket(
        s: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean
    ): Socket? {
        val socket = oriFactory.createSocket(s, host, port, autoClose) as? SSLSocket
        if (socket != null) {
            val sslParams = socket.sslParameters
            if (host?.contains("pixiv.net") == true
                || host?.contains("pximg.net") == true
                || host?.matches(Regex("[it]\\d\\.nhentai\\.net")) == true) {
                sslParams.serverNames = listOf(SNIHostName("i.do.not.want.sni"))
            //部分手机在发现一个无sni请求时，似乎会通过某些手段获取ip的域名，并将其填回请求中再发送
            }
            socket.sslParameters = sslParams
        }
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket? = oriFactory.createSocket(host, port)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int
    ): Socket? = oriFactory.createSocket(host, port, localHost, localPort)

    override fun createSocket(host: InetAddress?, port: Int): Socket? = oriFactory.createSocket(host, port)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket? = oriFactory.createSocket(address, port, localAddress, localPort)

}