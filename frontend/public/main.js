const loginView = document.querySelector("#loginView");
const adminView = document.querySelector("#adminView");
const joinView = document.querySelector("#joinView");
const resultView = document.querySelector("#resultView");
const screenView = document.querySelector("#screenView");
const loginForm = document.querySelector("#loginForm");
const eventForm = document.querySelector("#eventForm");
const eventList = document.querySelector("#eventList");
const loginMessage = document.querySelector("#loginMessage");
const eventMessage = document.querySelector("#eventMessage");
const sessionLabel = document.querySelector("#sessionLabel");
const logoutButton = document.querySelector("#logoutButton");
const newEventButton = document.querySelector("#newEventButton");
const resetFormButton = document.querySelector("#resetFormButton");
const formTitle = document.querySelector("#formTitle");
const topbar = document.querySelector(".topbar");
const topbarActions = document.querySelector(".topbar-actions");

const defaults = {
  title: "",
  satisfactionQuestion: "您对今日主题分享的整体满意程度？ / Overall satisfaction with today's sharing?",
  topicQuestion: "您今天最满意哪方面的分享？ / Which topic are you most satisfied with today?",
  topicOptions: [
    "兆易创新公司全景与台达服务体系 / Company overview and Delta service system",
    "Flash 技术趋势与 Roadmap 更新 / Flash technology trends and roadmap",
    "DRAM 产业现状与未来产品蓝图 / DRAM industry and product roadmap",
    "模拟产品在能源行业的应用分析 / Analog products in energy applications",
    "GD32 MCU 产品全景呈现与开发生态支持 / GD32 MCU portfolio and ecosystem",
    "GD32 MCU 高性能产品技术路线与竞争优势解读 / GD32 MCU high-performance roadmap",
    "GD32 MCU 数据中心与通信电源技术方案与落地实践 / Data center and telecom power solutions",
    "GD32 MCU 助力光储充市场高效应用 / Optical-storage-charging applications",
    "IAR 工具助力 GD32 MCU 高效开发(TBD) / IAR tools for GD32 MCU development"
  ].join("\n"),
  freeTextQuestion: "您期待后续哪方面的深入交流？ / What would you like to discuss further?",
  privacyNotice: "提交信息仅用于本场活动抽奖和会后沟通。/ Your information is used only for this event lucky draw and follow-up communication.",
  winningCount: "1",
  status: "active"
};

async function api(path, options = {}) {
  const response = await fetch(path, {
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  let body = {};
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { error: text };
    }
  }

  if (!response.ok) {
    const message = body.errors ? body.errors.join("\n") : body.error || `Request failed: ${response.status}`;
    throw new Error(message);
  }

  return body;
}

function encodeForm(form) {
  return new URLSearchParams(new FormData(form)).toString();
}

function showOnly(view) {
  [loginView, adminView, joinView, resultView, screenView].forEach(item => item.classList.add("hidden"));
  view.classList.remove("hidden");
}

function showPublic(view) {
  showOnly(view);
  topbar.classList.add("hidden");
  topbarActions.classList.add("hidden");
  logoutButton.classList.add("hidden");
  document.body.classList.add("public-page");
}

function showAdmin(username) {
  showOnly(adminView);
  document.body.classList.remove("public-page");
  topbar.classList.remove("hidden");
  topbarActions.classList.remove("hidden");
  logoutButton.classList.remove("hidden");
  sessionLabel.textContent = `已登录：${username}`;
}

function showLogin() {
  showOnly(loginView);
  document.body.classList.remove("public-page");
  topbar.classList.remove("hidden");
  topbarActions.classList.remove("hidden");
  logoutButton.classList.add("hidden");
  sessionLabel.textContent = "未登录";
}

function fillDefaults() {
  eventForm.reset();
  eventForm.elements.id.value = "";
  formTitle.textContent = "新建活动 / New Event";
  for (const [key, value] of Object.entries(defaults)) {
    eventForm.elements[key].value = value;
  }
}

function fillEvent(event) {
  formTitle.textContent = "编辑活动 / Edit Event";
  eventForm.elements.id.value = event.id;
  eventForm.elements.title.value = event.title;
  eventForm.elements.satisfactionQuestion.value = event.satisfactionQuestion;
  eventForm.elements.topicQuestion.value = event.topicQuestion;
  eventForm.elements.topicOptions.value = event.topicOptions.join("\n");
  eventForm.elements.freeTextQuestion.value = event.freeTextQuestion;
  eventForm.elements.privacyNotice.value = event.privacyNotice;
  eventForm.elements.winningCount.value = event.winningCount;
  eventForm.elements.status.value = event.status;
  eventMessage.textContent = "";
}

function absoluteUrl(path) {
  return `${window.location.origin}${path}`;
}

function renderEvents(events) {
  if (!events.length) {
    eventList.innerHTML = `<p class="empty">还没有活动。请先创建一个活动。</p>`;
    return;
  }

  eventList.innerHTML = events.map(event => {
    const link = absoluteUrl(event.registrationPath);
    const resultLink = absoluteUrl(`/results/${event.id}`);
    const screenLink = absoluteUrl(`/screen/${event.id}`);
    const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=132x132&data=${encodeURIComponent(link)}`;
    return `
      <article class="event-card">
        <div class="event-title-row">
          <h3>${escapeHtml(event.title || "未命名活动 / Untitled Event")}</h3>
          <span class="status-badge ${escapeHtml(event.status)}">${escapeHtml(event.status)}</span>
        </div>
        <div class="event-body">
          <div class="event-meta">
            <div class="meta-row"><span>ID</span><code title="${escapeHtml(event.id)}">${escapeHtml(event.id)}</code></div>
            <div class="meta-row"><span>中奖人数</span><strong>${event.winningCount}</strong></div>
            <div class="meta-row link-row">
              <span>报名链接</span>
              <a href="${link}" title="${link}" target="_blank" rel="noreferrer">${link}</a>
              <button type="button" class="copy-btn" data-copy="${escapeHtml(link)}">复制</button>
            </div>
            <div class="meta-row link-row">
              <span>结果链接</span>
              <a href="${resultLink}" title="${resultLink}" target="_blank" rel="noreferrer">${resultLink}</a>
              <button type="button" class="copy-btn" data-copy="${escapeHtml(resultLink)}">复制</button>
            </div>
            <div class="meta-row link-row">
              <span>大屏链接</span>
              <a href="${screenLink}" title="${screenLink}" target="_blank" rel="noreferrer">${screenLink}</a>
              <button type="button" class="copy-btn" data-copy="${escapeHtml(screenLink)}">复制</button>
            </div>
          </div>
          <div class="qr-card">
            <img src="${qrUrl}" alt="抽奖二维码" />
            <span>抽奖二维码</span>
          </div>
        </div>
        <div class="form-actions">
          <button type="button" data-edit="${event.id}" class="secondary">✏️ 编辑</button>
          <button type="button" data-submissions="${event.id}" class="secondary">👥 查看报名</button>
          <button type="button" data-draw="${event.id}">🎲 开始抽奖</button>
          <button type="button" data-winners="${event.id}" class="secondary">🏆 查看中奖</button>
          <button type="button" data-operations="${event.id}" class="secondary">📋 操作记录</button>
          <a class="small-link-button" href="/api/admin/events/${event.id}/export">导出 Excel</a>
          <button type="button" data-delete="${event.id}" data-title="${escapeHtml(event.title || "Untitled Event")}" class="danger-button">删除</button>
        </div>
        <div id="submissions-${event.id}" class="submission-box hidden"></div>
        <div id="winners-${event.id}" class="submission-box hidden"></div>
        <div id="operations-${event.id}" class="submission-box hidden"></div>
      </article>
    `;
  }).join("");

  eventList.querySelectorAll("[data-edit]").forEach(button => {
    button.addEventListener("click", async () => {
      const event = await api(`/api/admin/events/${button.dataset.edit}`);
      fillEvent(event);
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  });

  eventList.querySelectorAll("[data-copy]").forEach(button => {
    button.addEventListener("click", async () => {
      await copyText(button.dataset.copy);
      const previous = button.textContent;
      button.textContent = "已复制";
      setTimeout(() => {
        button.textContent = previous;
      }, 1200);
    });
  });

  eventList.querySelectorAll("[data-submissions]").forEach(button => {
    button.addEventListener("click", async () => {
      const eventId = button.dataset.submissions;
      const box = document.querySelector(`#submissions-${eventId}`);
      const submissions = await api(`/api/admin/events/${eventId}/submissions`);
      box.classList.toggle("hidden");
      box.innerHTML = renderSubmissions(submissions);
    });
  });

  eventList.querySelectorAll("[data-draw]").forEach(button => {
    button.addEventListener("click", () => {
      window.location.href = `/screen/${button.dataset.draw}`;
    });
  });

  eventList.querySelectorAll("[data-winners]").forEach(button => {
    button.addEventListener("click", async () => {
      const box = document.querySelector(`#winners-${button.dataset.winners}`);
      box.classList.toggle("hidden");
      await refreshWinners(button.dataset.winners);
    });
  });

  eventList.querySelectorAll("[data-operations]").forEach(button => {
    button.addEventListener("click", async () => {
      const eventId = button.dataset.operations;
      const box = document.querySelector(`#operations-${eventId}`);
      const operations = await api(`/api/admin/events/${eventId}/operations`);
      box.classList.toggle("hidden");
      box.innerHTML = renderOperations(operations);
    });
  });

  eventList.querySelectorAll("[data-delete]").forEach(button => {
    button.addEventListener("click", async () => {
      const confirmed = window.confirm(`确定删除活动「${button.dataset.title}」吗？\nThis will delete the event, submissions, winners, and operation records.`);
      if (!confirmed) {
        return;
      }
      try {
        await api(`/api/admin/events/${button.dataset.delete}`, { method: "DELETE", body: "" });
        if (eventForm.elements.id.value === button.dataset.delete) {
          fillDefaults();
        }
        await loadEvents();
      } catch (error) {
        alert(error.message);
      }
    });
  });
}

async function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.style.position = "fixed";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  textarea.remove();
}

async function refreshWinners(eventId) {
  const box = document.querySelector(`#winners-${eventId}`);
  const winners = await api(`/api/admin/events/${eventId}/winners`);
  box.classList.remove("hidden");
  box.innerHTML = renderWinners(winners);
  box.querySelectorAll("[data-void]").forEach(button => {
    button.addEventListener("click", async () => {
      await api(`/api/admin/events/${eventId}/winners/${button.dataset.void}/void`, { method: "POST", body: "" });
      await refreshWinners(eventId);
    });
  });
  box.querySelectorAll("[data-redraw]").forEach(button => {
    button.addEventListener("click", async () => {
      try {
        await api(`/api/admin/events/${eventId}/winners/${button.dataset.redraw}/redraw`, { method: "POST", body: "" });
        await refreshWinners(eventId);
      } catch (error) {
        alert(error.message);
      }
    });
  });
}

function renderSubmissions(submissions) {
  if (!submissions.length) {
    return `<p class="empty">暂无报名记录。</p>`;
  }

  return `
    <table>
      <thead>
        <tr>
          <th>姓名</th>
          <th>职位</th>
          <th>邮箱</th>
          <th>满意度</th>
          <th>单选答案</th>
          <th>后续交流</th>
        </tr>
      </thead>
      <tbody>
        ${submissions.map(item => `
          <tr>
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(item.jobTitle)}</td>
            <td>${escapeHtml(item.email)}</td>
            <td>${item.satisfactionScore}</td>
            <td>${escapeHtml(item.topicAnswer)}</td>
            <td>${escapeHtml(item.futureQuestion)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function renderWinners(winners) {
  if (!winners.length) {
    return `<p class="empty">暂无中奖记录。</p>`;
  }

  return `
    <table>
      <thead>
        <tr>
          <th>姓名</th>
          <th>邮箱</th>
          <th>状态</th>
          <th>来源</th>
          <th>时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        ${winners.map(winner => `
          <tr>
            <td>${escapeHtml(winner.name)}</td>
            <td>${escapeHtml(winner.email)}</td>
            <td>${escapeHtml(winner.status)}</td>
            <td>${escapeHtml(winner.source)}</td>
            <td>${escapeHtml(winner.createdAt)}</td>
            <td>
              ${winner.status === "valid" ? `<button type="button" class="secondary" data-void="${winner.id}">作废</button>` : ""}
              ${winner.status === "voided" ? `<button type="button" class="secondary" data-redraw="${winner.id}">补抽</button>` : ""}
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function renderOperations(operations) {
  if (!operations.length) {
    return `<p class="empty">暂无操作记录。</p>`;
  }

  return `
    <table>
      <thead>
        <tr>
          <th>动作</th>
          <th>目标</th>
          <th>操作人</th>
          <th>时间</th>
        </tr>
      </thead>
      <tbody>
        ${operations.map(operation => `
          <tr>
            <td>${escapeHtml(operation.action)}</td>
            <td>${escapeHtml(operation.targetId)}</td>
            <td>${escapeHtml(operation.operator)}</td>
            <td>${escapeHtml(operation.createdAt)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

async function loadEvents() {
  const events = await api("/api/admin/events");
  renderEvents(events);
}

function renderJoinUnavailable(message) {
  showPublic(joinView);
  sessionLabel.textContent = "报名页";
  logoutButton.classList.add("hidden");
  joinView.innerHTML = `
    <h2>报名暂不可用 / Registration unavailable</h2>
    <p class="message">${escapeHtml(message)}</p>
  `;
}

function renderJoinForm(event) {
  showPublic(joinView);
  sessionLabel.textContent = "报名页";
  logoutButton.classList.add("hidden");

  if (event.status !== "active") {
    renderJoinUnavailable("This event is not open for registration. / 本场活动暂未开放报名。");
    return;
  }

  joinView.innerHTML = `
    <h2>${escapeHtml(event.title)}</h2>
    <form id="joinForm" class="form-grid">
      <label>姓名 / Name <input name="name" required /></label>
      <label>职位 / Job Title <input name="jobTitle" required /></label>
      <label>邮箱 / Email <input name="email" type="email" required /></label>
      <label>
        ${escapeHtml(event.satisfactionQuestion)}
        <select name="satisfactionScore" required>
          <option value="">请选择 / Select</option>
          ${Array.from({ length: 10 }, (_, index) => `<option value="${index + 1}">${index + 1}</option>`).join("")}
        </select>
      </label>
      <fieldset>
        <legend>${escapeHtml(event.topicQuestion)}</legend>
        ${event.topicOptions.map(option => `
          <label class="radio-line">
            <input type="radio" name="topicAnswer" value="${escapeHtml(option)}" required />
            <span>${escapeHtml(option)}</span>
          </label>
        `).join("")}
      </fieldset>
      <label>${escapeHtml(event.freeTextQuestion)} <textarea name="futureQuestion" rows="4" required></textarea></label>
      <p class="privacy">${escapeHtml(event.privacyNotice)}</p>
      <button type="submit">提交报名 / Submit</button>
    </form>
    <p id="joinMessage" class="message"></p>
  `;

  document.querySelector("#joinForm").addEventListener("submit", async submitEvent => {
    submitEvent.preventDefault();
    const message = document.querySelector("#joinMessage");
    message.textContent = "";
    try {
      await api(`/api/events/${event.id}/submissions`, {
        method: "POST",
        body: encodeForm(submitEvent.currentTarget)
      });
      joinView.innerHTML = `
        <h2>报名成功 / Registration submitted</h2>
        <p>您已加入本场活动抽奖名单，请关注会议大屏或查看中奖结果页。</p>
        <p>You are in the lucky draw pool. Please watch the meeting screen or check the winner page.</p>
        <a class="button-link" href="/results/${event.id}">查看中奖结果 / View Winners</a>
      `;
    } catch (error) {
      message.textContent = error.message;
    }
  });
}

async function renderResultPage(eventId) {
  showPublic(resultView);
  sessionLabel.textContent = "中奖结果";
  logoutButton.classList.add("hidden");
  try {
    const event = await api(`/api/events/${eventId}`);
    const result = await api(`/api/events/${eventId}/results`);
    if (result.state === "waiting") {
      resultView.innerHTML = `
        <h2>${escapeHtml(event.title)}</h2>
        <p class="waiting">抽奖尚未完成，请关注会议画面或稍后刷新。</p>
        <p class="waiting">The draw has not finished yet. Please watch the meeting screen or refresh later.</p>
      `;
      return;
    }
    resultView.innerHTML = `
      <h2>${escapeHtml(event.title)}</h2>
      <p class="waiting">中奖结果 / Winners</p>
      <ul class="winner-list">
        ${result.winners.map(winner => `<li>${escapeHtml(winner.name)} <span>${escapeHtml(winner.email)}</span></li>`).join("")}
      </ul>
    `;
  } catch (error) {
    resultView.innerHTML = `<h2>结果页不可用 / Result unavailable</h2><p class="message">${escapeHtml(error.message)}</p>`;
  }
}

function renderScreenCompleted(event, result) {
  screenView.innerHTML = `
    <div class="screen-stage">
      <p class="eyebrow">Lucky Draw</p>
      <h2>${escapeHtml(event.title)}</h2>
      <div id="rollingName" class="rolling-name">Ready</div>
      <ul id="screenWinners" class="screen-winners hidden">
        ${result.winners.map(winner => `<li>${escapeHtml(winner.name)}<span>${escapeHtml(winner.email)}</span></li>`).join("")}
      </ul>
    </div>
  `;

  const rollingName = document.querySelector("#rollingName");
  const screenWinners = document.querySelector("#screenWinners");
  const names = result.winners.map(winner => winner.name);
  let tick = 0;
  const timer = setInterval(() => {
    rollingName.textContent = names[tick % names.length];
    tick += 1;
  }, 120);
  setTimeout(() => {
    clearInterval(timer);
    rollingName.classList.add("hidden");
    screenWinners.classList.remove("hidden");
  }, 1500);
}

function renderScreenRolling(event, names) {
  const rollingNames = names.length ? names : ["Ready"];
  screenView.innerHTML = `
    <div class="screen-stage">
      <p class="eyebrow">Lucky Draw</p>
      <h2>${escapeHtml(event.title)}</h2>
      <div id="rollingName" class="rolling-name">Ready</div>
      <div class="screen-actions">
        <button id="screenRevealButton" type="button" class="screen-draw-button">公布中奖名单 / Reveal Winners</button>
      </div>
      <p class="screen-note">抽奖进行中 / Drawing from registered participants...</p>
    </div>
  `;

  const rollingName = document.querySelector("#rollingName");
  let tick = 0;
  const timer = setInterval(() => {
    rollingName.textContent = rollingNames[tick % rollingNames.length];
    tick += 1;
  }, 90);
  return () => clearInterval(timer);
}

async function renderScreenPage(eventId) {
  showPublic(screenView);
  sessionLabel.textContent = "大屏展示";
  logoutButton.classList.add("hidden");
  try {
    const event = await api(`/api/events/${eventId}`);
    const result = await api(`/api/events/${eventId}/results`);
    const adminSession = await api("/api/admin/me").catch(() => null);

    if (result.state === "waiting") {
      screenView.innerHTML = `
        <div class="screen-stage">
          <p class="eyebrow">Lucky Draw</p>
          <h2>${escapeHtml(event.title)}</h2>
          <p class="screen-waiting">抽奖尚未开始 / Waiting for the draw</p>
          ${adminSession ? `
            <div class="screen-actions">
              <button id="screenDrawButton" type="button" class="screen-draw-button">开始抽奖 / Start Draw</button>
              <p id="screenDrawMessage" class="screen-note"></p>
            </div>
          ` : `
            <p class="screen-note">管理员登录后可在本页开始抽奖。/ Admin login is required to start the draw here.</p>
          `}
        </div>
      `;
      const drawButton = document.querySelector("#screenDrawButton");
      if (drawButton) {
        drawButton.addEventListener("click", async () => {
          const message = document.querySelector("#screenDrawMessage");
          drawButton.disabled = true;
          drawButton.textContent = "抽奖中... / Drawing...";
          message.textContent = "";
          let stopRolling = null;
          try {
            const submissions = await api(`/api/admin/events/${eventId}/submissions`);
            const names = submissions.map(submission => submission.name || submission.email || "Participant");
            stopRolling = renderScreenRolling(event, names);
            const revealButton = document.querySelector("#screenRevealButton");
            if (revealButton) {
              revealButton.addEventListener("click", async () => {
                revealButton.disabled = true;
                revealButton.textContent = "开奖中... / Revealing...";
                try {
                  await api(`/api/admin/events/${eventId}/draw`, { method: "POST", body: "" });
                  const latest = await api(`/api/events/${eventId}/results`);
                  stopRolling();
                  renderScreenCompleted(event, latest);
                } catch (error) {
                  stopRolling();
                  await renderScreenPage(eventId);
                  const nextMessage = document.querySelector("#screenDrawMessage");
                  if (nextMessage) {
                    nextMessage.textContent = error.message;
                  } else {
                    alert(error.message);
                  }
                }
              });
            }
          } catch (error) {
            if (stopRolling) {
              stopRolling();
            }
            await renderScreenPage(eventId);
            const nextMessage = document.querySelector("#screenDrawMessage");
            if (nextMessage) {
              nextMessage.textContent = error.message;
            } else {
              alert(error.message);
            }
          }
        });
      }
      return;
    }

    renderScreenCompleted(event, result);
  } catch (error) {
    screenView.innerHTML = `<div class="screen-stage"><h2>大屏不可用 / Screen unavailable</h2><p class="message">${escapeHtml(error.message)}</p></div>`;
  }
}

loginForm.addEventListener("submit", async event => {
  event.preventDefault();
  loginMessage.textContent = "";

  try {
    const user = await api("/api/admin/login", {
      method: "POST",
      body: encodeForm(loginForm)
    });
    showAdmin(user.username);
    fillDefaults();
    await loadEvents();
  } catch (error) {
    loginMessage.textContent = error.message;
  }
});

logoutButton.addEventListener("click", async () => {
  await api("/api/admin/logout", { method: "POST", body: "" }).catch(() => {});
  showLogin();
});

eventForm.addEventListener("submit", async event => {
  event.preventDefault();
  eventMessage.textContent = "";

  const id = eventForm.elements.id.value;
  const path = id ? `/api/admin/events/${id}` : "/api/admin/events";
  const method = id ? "PUT" : "POST";

  try {
    const saved = await api(path, {
      method,
      body: encodeForm(eventForm)
    });
    eventMessage.textContent = `已保存：${saved.title}`;
    fillEvent(saved);
    await loadEvents();
  } catch (error) {
    eventMessage.textContent = error.message;
  }
});

newEventButton.addEventListener("click", fillDefaults);
resetFormButton.addEventListener("click", fillDefaults);

async function bootAdmin() {
  try {
    const user = await api("/api/admin/me");
    showAdmin(user.username);
    fillDefaults();
    await loadEvents();
  } catch {
    showLogin();
    fillDefaults();
  }
}

async function boot() {
  const joinMatch = window.location.pathname.match(/^\/join\/([^/]+)$/);
  const resultMatch = window.location.pathname.match(/^\/results\/([^/]+)$/);
  const screenMatch = window.location.pathname.match(/^\/screen\/([^/]+)$/);

  if (joinMatch) {
    try {
      const event = await api(`/api/events/${joinMatch[1]}`);
      renderJoinForm(event);
    } catch (error) {
      renderJoinUnavailable(error.message);
    }
    return;
  }

  if (resultMatch) {
    await renderResultPage(resultMatch[1]);
    return;
  }

  if (screenMatch) {
    await renderScreenPage(screenMatch[1]);
    return;
  }

  await bootAdmin();
}

boot();
