use std::error::Error;
use std::io::{Read, Write};
use std::net::{IpAddr, Ipv4Addr, SocketAddr, TcpStream};
use std::sync::Arc;

use rustls::{ClientConfig, ClientConnection, KeyLogFile, RootCertStore, Stream};
use rustls::client::{EchConfig, EchMode};
use rustls::crypto::aws_lc_rs::default_provider;
use rustls::crypto::aws_lc_rs::hpke::ALL_SUPPORTED_SUITES;
use rustls::pki_types::{EchConfigListBytes, ServerName};
use hickory_resolver::config::{ResolverConfig, ServerGroup};
use hickory_resolver::net::runtime::TokioRuntimeProvider;
use hickory_resolver::proto::rr::rdata::svcb::{SvcParamKey, SvcParamValue};
use hickory_resolver::proto::rr::{RData, RecordType};
use hickory_resolver::TokioResolver;

/// HTTP 响应结构
#[derive(Debug)]
pub struct EchResponse {
    /// HTTP 状态码
    pub status: u16,
    /// 响应头列表
    pub headers: Vec<(String, String)>,
    /// 响应体原始字节
    pub body: Vec<u8>,
}

impl EchResponse {
    /// 将 body 作为 UTF-8 字符串返回
    pub fn text(&self) -> String {
        String::from_utf8_lossy(&self.body).to_string()
    }

    /// 查找指定名称的响应头值
    pub fn header(&self, name: &str) -> Option<&str> {
        let name_lower = name.to_lowercase();
        self.headers
            .iter()
            .find(|(k, _)| k.to_lowercase() == name_lower)
            .map(|(_, v)| v.as_str())
    }
}

/// ECH HTTP 客户端
///
/// 通过 ECH (Encrypted Client Hello) 与 Cloudflare 托管域名建立加密 TLS 连接。
/// 使用 `cloudflare-ech.com` 作为外层 SNI，内层真实 SNI 被加密保护。
pub struct EchClient {
    tls_config: Arc<ClientConfig>,
    resolver: TokioResolver,
    ech_public_name: String,
}

impl EchClient {
    /// 创建新的 ECH 客户端
    ///
    /// 自动从 DNS 获取 ECH 配置（HPKE 公钥），构建带 ECH 的 TLS 配置。
    ///
    /// # 参数
    /// - `ech_public_name`: ECH 公钥发布域名，Cloudflare 托管域名通常用 `cloudflare-ech.com`
    pub async fn new(ech_public_name: &str) -> Result<Self, Box<dyn Error>> {
        let resolver = Self::build_dns_resolver()?;

        // 从 DNS HTTPS 记录获取 ECH 配置
        let ech_configs = Self::lookup_ech_configs(&resolver, ech_public_name).await?;
        if ech_configs.is_empty() {
            return Err(format!("no ECH configs found for {ech_public_name}").into());
        }

        let ech_config = ech_configs
            .into_iter()
            .find_map(|list| EchConfig::new(list, ALL_SUPPORTED_SUITES).ok())
            .ok_or("no supported ECH configs (HPKE suite mismatch)")?;
        let ech_mode = EchMode::from(ech_config);

        let root_store = RootCertStore {
            roots: webpki_roots::TLS_SERVER_ROOTS.into(),
        };

        let mut config = ClientConfig::builder_with_provider(default_provider().into())
            .with_ech(ech_mode)
            .map_err(|e| format!("failed to enable ECH: {e}"))?
            .with_root_certificates(root_store)
            .with_no_client_auth();

        config.key_log = Arc::new(KeyLogFile::new());

        Ok(Self {
            tls_config: Arc::new(config),
            resolver,
            ech_public_name: ech_public_name.to_owned(),
        })
    }

    /// 对目标域名发起 GET 请求
    ///
    /// # 参数
    /// - `domain`: 目标域名（内层 SNI）
    /// - `path`: 请求路径（如 `/api/v2/galleries/popular`）
    /// - `query`: URL 查询参数，传空切片表示无参数
    /// - `headers`: 额外的 HTTP 请求头，传空切片表示仅用默认头
    pub async fn get(
        &self,
        domain: &str,
        path: &str,
        query: &[(&str, &str)],
        headers: &[(&str, &str)],
    ) -> Result<EchResponse, Box<dyn Error>> {
        let h: Vec<(String, String)> = headers
            .iter()
            .map(|(k, v)| (k.to_string(), v.to_string()))
            .collect();
        let request = self.build_request("GET", domain, path, query, &h, None)?;
        self.send_request(domain, &request).await
    }

    /// 对目标域名发起 POST 请求（`application/x-www-form-urlencoded`）
    ///
    /// # 参数
    /// - `domain`: 目标域名
    /// - `path`: 请求路径
    /// - `query`: URL 查询参数，传空切片表示无参数
    /// - `form`: 表单键值对，会自动进行 URL 编码
    /// - `headers`: 额外的 HTTP 请求头，传空切片表示仅用默认头
    pub async fn post(
        &self,
        domain: &str,
        path: &str,
        query: &[(&str, &str)],
        form: &[(&str, &str)],
        headers: &[(&str, &str)],
    ) -> Result<EchResponse, Box<dyn Error>> {
        let body: String = form
            .iter()
            .map(|(k, v)| format!("{}={}", urlencoding(k), urlencoding(v)))
            .collect::<Vec<_>>()
            .join("&");

        let mut all_headers: Vec<(String, String)> = vec![
            ("Content-Type".to_owned(), "application/x-www-form-urlencoded".to_owned()),
            ("Content-Length".to_owned(), body.len().to_string()),
        ];
        all_headers.extend(headers.iter().map(|(k, v)| (k.to_string(), v.to_string())));

        let request =
            self.build_request("POST", domain, path, query, &all_headers, Some(&body))?;
        self.send_request(domain, &request).await
    }

    // ── 内部方法 ──

    /// 构造完整的 HTTP/1.1 请求报文
    fn build_request(
        &self,
        method: &str,
        domain: &str,
        path: &str,
        query: &[(&str, &str)],
        headers: &[(String, String)],
        body: Option<&str>,
    ) -> Result<String, Box<dyn Error>> {
        // 构建含查询参数的完整路径
        let full_path = if query.is_empty() {
            path.to_owned()
        } else {
            let qs: String = query
                .iter()
                .map(|(k, v)| format!("{}={}", urlencoding(k), urlencoding(v)))
                .collect::<Vec<_>>()
                .join("&");
            format!("{path}?{qs}")
        };

        let mut req = format!(
            "{method} {full_path} HTTP/1.1\r\n\
             Host: {domain}\r\n\
             User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\r\n\
             Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n\
             Accept-Language: en-US,en;q=0.5\r\n"
        );

        // 追加自定义请求头
        for (name, value) in headers {
            req.push_str(&format!("{name}: {value}\r\n"));
        }

        req.push_str("Connection: close\r\n");
        req.push_str("Accept-Encoding: identity\r\n");
        req.push_str("\r\n");

        // 追加请求体（POST 等）
        if let Some(b) = body {
            req.push_str(b);
        }

        Ok(req)
    }

    /// 核心：建立 ECH 连接并发送 HTTP 请求
    async fn send_request(&self, domain: &str, request: &str) -> Result<EchResponse, Box<dyn Error>> {
        let server_name: ServerName<'static> = domain
            .to_owned()
            .try_into()
            .map_err(|_| format!("invalid domain: {domain}"))?;

        // 解析 ECH public name 的 IP（外层连接的 IP）
        let addr_lookup = self.resolver.lookup_ip(&self.ech_public_name).await?;
        let ip = addr_lookup
            .iter()
            .find(|ip| ip.is_ipv4())
            .or_else(|| addr_lookup.iter().next())
            .ok_or("no IP for ECH public name")?;

        let sock_addr = SocketAddr::new(ip, 443);
        let mut sock = TcpStream::connect(sock_addr)?;

        let mut conn = ClientConnection::new(self.tls_config.clone(), server_name)?;
        let mut tls = Stream::new(&mut conn, &mut sock);

        tls.write_all(request.as_bytes())?;

        let mut body = Vec::new();
        tls.read_to_end(&mut body)?;

        Self::parse_http_response(&body)
    }

    /// 解析 HTTP 响应
    fn parse_http_response(raw: &[u8]) -> Result<EchResponse, Box<dyn Error>> {
        let text = String::from_utf8_lossy(raw);

        // 分离 header 和 body
        let Some(header_end) = text.find("\r\n\r\n") else {
            return Err("invalid HTTP response: no header terminator".into());
        };

        let header_section = &text[..header_end];
        let body = raw[header_end + 4..].to_vec();

        // 解析状态行
        let mut lines = header_section.lines();
        let status_line = lines.next().ok_or("empty HTTP response")?;

        // 从 "HTTP/1.1 200 OK" 中提取状态码
        let status = status_line
            .split_whitespace()
            .nth(1)
            .and_then(|s| s.parse::<u16>().ok())
            .ok_or("invalid HTTP status line")?;

        // 解析响应头
        let headers: Vec<(String, String)> = lines
            .filter_map(|line| {
                let (name, value) = line.split_once(':')?;
                Some((name.trim().to_owned(), value.trim().to_owned()))
            })
            .collect();

        Ok(EchResponse {
            status,
            headers,
            body,
        })
    }

    /// 构建 DNS 解析器（优先 Cloudflare UDP DNS）
    fn build_dns_resolver() -> Result<TokioResolver, Box<dyn Error>> {
        static CF_IPS: &[IpAddr] = &[
            IpAddr::V4(Ipv4Addr::new(1, 1, 1, 1)),
            IpAddr::V4(Ipv4Addr::new(1, 0, 0, 1)),
        ];

        Ok(TokioResolver::builder_with_config(
            ResolverConfig::udp_and_tcp(&ServerGroup {
                ips: CF_IPS,
                server_name: "cloudflare-dns.com",
                path: "/dns-query",
            }),
            TokioRuntimeProvider::default(),
        )
        .build()?)
    }

    /// 从域名的 HTTPS DNS 记录中获取 ECH 配置列表
    async fn lookup_ech_configs(
        resolver: &TokioResolver,
        domain: &str,
    ) -> Result<Vec<EchConfigListBytes<'static>>, Box<dyn Error>> {
        let lookup = resolver.lookup(domain, RecordType::HTTPS).await?;
        let mut ech_config_lists = Vec::new();

        for r in lookup.answers() {
            let RData::HTTPS(svcb) = &r.data else {
                continue;
            };

            for (key, value) in &svcb.svc_params {
                if let (SvcParamKey::EchConfigList, SvcParamValue::EchConfigList(e)) = (key, value) {
                    ech_config_lists.push(EchConfigListBytes::from(e.clone().0));
                }
            }
        }

        Ok(ech_config_lists)
    }
}

/// 简单的 URL 编码
fn urlencoding(s: &str) -> String {
    let mut result = String::with_capacity(s.len());
    for b in s.bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                result.push(b as char);
            }
            b' ' => result.push('+'),
            _ => result.push_str(&format!("%{:02X}", b)),
        }
    }
    result
}
