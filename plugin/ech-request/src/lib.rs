mod ech_client;

use std::fmt;
use std::net::IpAddr;

use once_cell::sync::OnceCell;

use jni::objects::{JObject, JString};
use jni::EnvUnowned;

use ech_client::EchClient;

// ── 常量（来自 PixivTokenUtil） ──

const CLIENT_ID: &str = "MOBrBDS8blbauoSck0ZfDbtuzpyT";
const CLIENT_SECRET: &str = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj";
const REDIRECT_URI: &str = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback";
const OAUTH_HOST: &str = "oauth.secure.pixiv.net";
const OAUTH_PATH: &str = "/auth/token";
const ECH_PUBLIC_NAME: &str = "cloudflare-ech.com";

// ── 应用层错误 ──

#[derive(Debug)]
struct OAuthError(String);

impl fmt::Display for OAuthError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl std::error::Error for OAuthError {}

impl From<jni::errors::Error> for OAuthError {
    fn from(e: jni::errors::Error) -> Self {
        OAuthError(e.to_string())
    }
}

// ── 全局 EchClient + Tokio Runtime 单例 ──

static CLIENT: OnceCell<(EchClient, tokio::runtime::Runtime)> = OnceCell::new();

fn get_client() -> Result<&'static (EchClient, tokio::runtime::Runtime), String> {
    CLIENT.get_or_try_init(|| {
        let rt = tokio::runtime::Runtime::new().map_err(|e| format!("tokio runtime: {e}"))?;
        let client = rt
            .block_on(EchClient::new(ECH_PUBLIC_NAME))
            .map_err(|e| format!("ech client init: {e}"))?;
        Ok((client, rt))
    })
}

fn parse_https_url(url: &str) -> Result<(String, String), String> {
    let rest = url
        .strip_prefix("https://")
        .ok_or("only https urls are supported")?;
    // 域名在第一个 '/' 或 '?'（没有路径只有查询串时）处结束
    let end = rest.find(['/', '?']).unwrap_or(rest.len());
    let domain = rest[..end].to_ascii_lowercase();
    if domain.is_empty() {
        return Err(format!("no domain in url: {url}"));
    }

    let path = if end < rest.len() {
        if rest.as_bytes()[end] == b'?' {
            format!("/{}", &rest[end..])
        } else {
            rest[end..].to_owned()
        }
    } else {
        "/".to_owned()
    };
    Ok((domain, path))
}

// ── 核心 OAuth 逻辑 ──

/// 执行 OAuth POST 请求，返回 "{access_token}|{refresh_token}"
fn do_oauth_request(form: &[(&str, &str)]) -> Result<String, String> {
    let (client, rt) = get_client()?;

    let headers: &[(&str, &str)] = &[
        (
            "user-agent",
            "PixivIOSApp/7.13.3 (iOS 14.6; iPhone13,2)",
        ),
        ("app-os-version", "15.6"),
        ("app-os", "ios"),
        (
            "accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        ),
        ("accept-language", "en-US,en;q=0.5"),
    ];

    let response = rt
        .block_on(client.post(OAUTH_HOST, OAUTH_PATH, &[], form, headers))
        .map_err(|e| format!("oauth request: {e}"))?;

    let body = response.text();

    Ok(body)
}

/// 通用请求用的浏览器 UA
const BROWSER_UA: &str = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36 Edg/152.0.0.0";

/// 整个地址列表都失败后重试的次数
const RETRY: usize = 2;

/// 解析逗号/空格分隔的 IP 列表（Kotlin 侧传来的 ECH 连接地址候补）
fn parse_ips(list: &str) -> Vec<IpAddr> {
    list.split([',', ' ', '\n', '\t'])
        .filter_map(|s| s.trim().parse().ok())
        .collect()
}

/// 通用 HTTP 请求：`form` 为 `None` 时 GET，否则把 `form` 原样作为 JSON 请求体 POST。
/// 非 2xx 一律视为失败（目前不跟随重定向），错误交给 JNI 抛异常。
///
/// 连接被丢弃（超时）时会重试，HTTP 错误码不重试。
fn do_http(url: &str, form: Option<&str>, candidates: &[IpAddr]) -> Result<String, String> {
    let (client, rt) = get_client()?;

    let (domain, path) = parse_https_url(url)?;

    let headers: &[(&str, &str)] = &[("user-agent", BROWSER_UA), ("accept", "*/*")];

    let mut last_err = String::new();
    for _ in 0..RETRY {
        let sent = rt.block_on(async {
            match form {
                Some(body) => {
                    client
                        .post_body(&domain, &path, &[], body, "application/json", headers, candidates)
                        .await
                }
                None => client.get(&domain, &path, &[], headers, candidates).await,
            }
        });

        match sent {
            Ok(response) if (200..300).contains(&response.status) => return Ok(response.text()),
            Ok(response) => {
                // 带上实际连上的地址和一点响应体，方便分辨是哪个环节挡的
                let snippet: String = response
                    .text()
                    .chars()
                    .take(120)
                    .map(|c| if c.is_control() { ' ' } else { c })
                    .collect();
                return Err(format!(
                    "http {} from {url} via {}: {snippet}",
                    response.status,
                    response.peer.ip()
                ));
            }
            Err(e) => last_err = format!("http request: {e}"),
        }
    }

    Err(last_err)
}

// ── JNI 导出 ──

/// `EchRequestRustClass.refreshOAuthTokenImpl(refreshToken)` — 私有 JNI 绑定
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xlrr_roambendom_third_EchRequestRustClass_refreshOAuthTokenImpl<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _obj: JObject<'local>,
    refresh_token: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, OAuthError> {
            let token: String = refresh_token.to_string();

            let form: &[(&str, &str)] = &[
                ("client_id", CLIENT_ID),
                ("client_secret", CLIENT_SECRET),
                ("grant_type", "refresh_token"),
                ("refresh_token", &token),
            ];

            let result = do_oauth_request(form).map_err(OAuthError)?;

            Ok(JString::from_str(env, &result)?)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

/// `EchRequestRustClass.requestOAuthTokenImpl(code, codeVerifier)` — 私有 JNI 绑定
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xlrr_roambendom_third_EchRequestRustClass_requestOAuthTokenImpl<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _obj: JObject<'local>,
    code: JString<'local>,
    code_verifier: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, OAuthError> {
            let code_str: String = code.to_string();
            let verifier: String = code_verifier.to_string();

            let form: &[(&str, &str)] = &[
                ("client_id", CLIENT_ID),
                ("client_secret", CLIENT_SECRET),
                ("code", &code_str),
                ("code_verifier", &verifier),
                ("grant_type", "authorization_code"),
                ("include_policy", "true"),
                ("redirect_uri", REDIRECT_URI),
            ];

            let result = do_oauth_request(form).map_err(OAuthError)?;

            Ok(JString::from_str(env, &result)?)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

/// `EchRequestRustClass.baseHttpGetImpl(url, candidates)` — 通用 GET，url 里自带查询参数
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xlrr_roambendom_third_EchRequestRustClass_baseHttpGetImpl<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _obj: JObject<'local>,
    url: JString<'local>,
    candidates: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, OAuthError> {
            let url: String = url.to_string();
            let candidates = parse_ips(&candidates.to_string());

            let result = do_http(&url, None, &candidates).map_err(OAuthError)?;

            Ok(JString::from_str(env, &result)?)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

/// `EchRequestRustClass.baseHttpPostImpl(url, form, candidates)` — 通用 POST，form 原样作为 JSON 请求体
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xlrr_roambendom_third_EchRequestRustClass_baseHttpPostImpl<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _obj: JObject<'local>,
    url: JString<'local>,
    form: JString<'local>,
    candidates: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, OAuthError> {
            let url: String = url.to_string();
            let form: String = form.to_string();
            let candidates = parse_ips(&candidates.to_string());

            let result = do_http(&url, Some(&form), &candidates).map_err(OAuthError)?;

            Ok(JString::from_str(env, &result)?)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
