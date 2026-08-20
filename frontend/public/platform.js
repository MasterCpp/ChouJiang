const platformLoginView = document.querySelector("#platformLoginView");
const platformAccountsView = document.querySelector("#platformAccountsView");
const platformLoginForm = document.querySelector("#platformLoginForm");
const platformLoginMessage = document.querySelector("#platformLoginMessage");
const platformAccounts = document.querySelector("#platformAccounts");
const platformMessage = document.querySelector("#platformMessage");
const platformActions = document.querySelector("#platformActions");
const platformSession = document.querySelector("#platformSession");

async function platformApi(path, options = {}) {
  const response = await fetch(path, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/x-www-form-urlencoded", ...(options.headers || {}) },
    ...options
  });
  const text = await response.text();
  let body = {};
  if (text) {
    try { body = JSON.parse(text); } catch { body = { error: text }; }
  }
  if (!response.ok) throw new Error(body.error || `请求失败：${response.status}`);
  return body;
}

function formBody(form) {
  return new URLSearchParams(new FormData(form)).toString();
}

function showPlatformLogin(message = "") {
  platformLoginView.classList.remove("hidden");
  platformAccountsView.classList.add("hidden");
  platformActions.classList.add("hidden");
  platformLoginMessage.textContent = message;
}

function accountCard(account) {
  const isActive = account.status === "active";
  const action = isActive ? "disable" : "enable";
  const actionLabel = isActive ? "禁用" : "重新启用";
  return `
    <article class="event-card">
      <div class="event-title-row">
        <h3 data-user-content>${escapeHtml(account.workspaceName)}</h3>
        <span class="status-badge ${escapeHtml(account.status)}">${escapeHtml(account.status)}</span>
      </div>
      <div class="event-meta">
        <div class="meta-row"><span>邮箱</span><strong>${escapeHtml(account.email)}</strong></div>
      </div>
      <div class="form-actions">
        <button type="button" data-account-action="${action}" data-account-id="${escapeHtml(account.id)}">${actionLabel}</button>
        <button type="button" class="secondary" data-account-action="password" data-account-id="${escapeHtml(account.id)}">设置新密码</button>
      </div>
    </article>`;
}

function escapeHtml(value) {
  return String(value ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

async function loadAccounts() {
  platformMessage.textContent = "";
  try {
    const accounts = await platformApi("/api/platform/accounts");
    platformLoginView.classList.add("hidden");
    platformAccountsView.classList.remove("hidden");
    platformActions.classList.remove("hidden");
    platformAccounts.innerHTML = accounts.length ? accounts.map(accountCard).join("") : "<p class=\"empty\">还没有分公司账号。</p>";
    platformAccounts.querySelectorAll("[data-account-action]").forEach(button => button.addEventListener("click", () => manageAccount(button)));
    window.JSysLocale?.translate(platformAccountsView);
  } catch (error) {
    showPlatformLogin(error.message);
  }
}

async function manageAccount(button) {
  const { accountAction, accountId } = button.dataset;
  try {
    if (accountAction === "password") {
      const newPassword = window.prompt("请输入线下交付给 Owner 的新密码（8–128 位）：");
      if (newPassword === null) return;
      await platformApi(`/api/platform/accounts/${encodeURIComponent(accountId)}/password`, { method: "POST", body: new URLSearchParams({ newPassword }).toString() });
      platformMessage.textContent = "新密码已设置；请通过线下方式交付给该账号 Owner。";
    } else {
      await platformApi(`/api/platform/accounts/${encodeURIComponent(accountId)}/${accountAction}`, { method: "POST", body: "" });
      platformMessage.textContent = accountAction === "disable" ? "账号已禁用，现有会话和公开活动页已立即关闭。" : "账号已重新启用。";
    }
    await loadAccounts();
  } catch (error) {
    platformMessage.textContent = error.message;
  }
}

platformLoginForm.addEventListener("submit", async event => {
  event.preventDefault();
  platformLoginMessage.textContent = "";
  try {
    const platform = await platformApi("/api/platform/login", { method: "POST", body: formBody(platformLoginForm) });
    platformSession.textContent = `已登录：${platform.email}`;
    await loadAccounts();
  } catch (error) {
    platformLoginMessage.textContent = error.message;
  }
});

document.querySelector("#platformRefresh").addEventListener("click", loadAccounts);
document.querySelector("#platformLogout").addEventListener("click", async () => {
  await platformApi("/api/platform/logout", { method: "POST", body: "" }).catch(() => {});
  showPlatformLogin();
});

window.JSysLocale.load().then(loadAccounts);
