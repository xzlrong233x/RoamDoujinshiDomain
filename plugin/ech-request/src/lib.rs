mod ech_client;

use std::fmt;

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

// ── 核心 OAuth 逻辑 ──

/// 执行 OAuth POST 请求，返回 "{access_token}|{refresh_token}"
fn do_oauth_request(form: &[(&str, &str)]) -> Result<String, String> {
    let (client, rt) = get_client()?;

    let headers: &[(&str, &str)] = &[
        ("app-os-version", "15.6"),
        ("app-os", "ios"),
        (
            "user-agent",
            "PixivIOSApp/7.13.3 (iOS 14.6; iPhone13,2)",
        ),
    ];

    let response = rt
        .block_on(client.post(OAUTH_HOST, OAUTH_PATH, &[], form, headers))
        .map_err(|e| format!("oauth request: {e}"))?;

    let body = response.text();

    Ok(body)
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

            let result =
                do_oauth_request(form).map_err(|e| OAuthError(e))?;

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

            let result =
                do_oauth_request(form).map_err(|e| OAuthError(e))?;

            Ok(JString::from_str(env, &result)?)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
