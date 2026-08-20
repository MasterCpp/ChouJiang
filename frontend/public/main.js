const loginView = document.querySelector("#loginView");
const registerView = document.querySelector("#registerView");
const settingsView = document.querySelector("#settingsView");
const adminView = document.querySelector("#adminView");
const joinView = document.querySelector("#joinView");
const resultView = document.querySelector("#resultView");
const screenView = document.querySelector("#screenView");
const loginForm = document.querySelector("#loginForm");
const registerForm = document.querySelector("#registerForm");
const settingsForm = document.querySelector("#settingsForm");
const eventForm = document.querySelector("#eventForm");
const eventList = document.querySelector("#eventList");
const loginMessage = document.querySelector("#loginMessage");
const registerMessage = document.querySelector("#registerMessage");
const settingsMessage = document.querySelector("#settingsMessage");
const eventMessage = document.querySelector("#eventMessage");
const sessionLabel = document.querySelector("#sessionLabel");
const logoutButton = document.querySelector("#logoutButton");
const settingsButton = document.querySelector("#settingsButton");
const showRegisterButton = document.querySelector("#showRegisterButton");
const backToLoginButton = document.querySelector("#backToLoginButton");
const backToAdminButton = document.querySelector("#backToAdminButton");
const loginTitle = document.querySelector("#loginTitle");
const loginIdentityLabel = document.querySelector("#loginIdentityLabel");
const newEventButton = document.querySelector("#newEventButton");
const resetFormButton = document.querySelector("#resetFormButton");
const formTitle = document.querySelector("#formTitle");
const topbar = document.querySelector(".topbar");
const topbarActions = document.querySelector(".topbar-actions");
const questionList = document.querySelector("#questionList");
const addQuestionButton = document.querySelector("#addQuestionButton");

let defaults = {
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

let currentQuestions = [];
let chineseAccountMode = false;

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
  const source = new FormData(form);
  const target = new URLSearchParams();
  const names = new Set();
  for (const name of source.keys()) {
    names.add(name);
  }
  names.forEach(name => {
    target.set(name, source.getAll(name).join("\n"));
  });
  return target.toString();
}

function showOnly(view) {
  [loginView, registerView, settingsView, adminView, joinView, resultView, screenView].forEach(item => item.classList.add("hidden"));
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
  settingsButton.classList.toggle("hidden", !chineseAccountMode);
  sessionLabel.textContent = `已登录：${username}`;
}

function showLogin() {
  showOnly(loginView);
  document.body.classList.remove("public-page");
  topbar.classList.remove("hidden");
  topbarActions.classList.remove("hidden");
  logoutButton.classList.add("hidden");
  settingsButton.classList.add("hidden");
  sessionLabel.textContent = "未登录";
}

function showRegister() {
  showOnly(registerView);
  document.body.classList.remove("public-page");
  topbar.classList.remove("hidden");
  topbarActions.classList.remove("hidden");
  logoutButton.classList.add("hidden");
  settingsButton.classList.add("hidden");
  sessionLabel.textContent = "注册分公司账号";
  registerMessage.textContent = "";
}

async function showSettings() {
  settingsMessage.textContent = "";
  try {
    const settings = await api("/api/admin/settings");
    settingsForm.elements.email.value = settings.email;
    settingsForm.elements.workspaceName.value = settings.workspaceName;
    settingsForm.elements.currentPassword.value = "";
    settingsForm.elements.newPassword.value = "";
    showOnly(settingsView);
    document.body.classList.remove("public-page");
    topbar.classList.remove("hidden");
    topbarActions.classList.remove("hidden");
    logoutButton.classList.remove("hidden");
    settingsButton.classList.remove("hidden");
    sessionLabel.textContent = `已登录：${settings.email}`;
  } catch (error) {
    loginMessage.textContent = error.message;
    showLogin();
  }
}

function configureAccountMode() {
  chineseAccountMode = !window.JSysLocale.isEnglishInstance();
  if (chineseAccountMode) {
    loginTitle.textContent = "账号登录";
    loginIdentityLabel.textContent = "邮箱";
    loginForm.elements.username.autocomplete = "email";
    showRegisterButton.classList.remove("hidden");
  } else {
    showRegisterButton.classList.add("hidden");
  }
}

function newQuestion(type = "text") {
  return {
    id: `q_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    type,
    label: "",
    required: true,
    options: type === "single" || type === "multiple" ? [""] : []
  };
}

function defaultQuestions() {
  return [
    {
      id: "score",
      type: "score",
      label: defaults.satisfactionQuestion,
      required: true,
      options: []
    },
    {
      id: "topic",
      type: "single",
      label: defaults.topicQuestion,
      required: true,
      options: defaults.topicOptions.split("\n").filter(Boolean)
    },
    {
      id: "future",
      type: "text",
      label: defaults.freeTextQuestion,
      required: true,
      options: []
    }
  ];
}

function b64UrlEncode(value) {
  const bytes = new TextEncoder().encode(String(value ?? ""));
  let binary = "";
  bytes.forEach(byte => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function serializeQuestions(questions) {
  return questions.map(question => [
    question.id,
    question.type,
    question.label,
    String(question.required),
    (question.options || []).join("\n")
  ].map(b64UrlEncode).join("|")).join("\n");
}

function syncLegacyQuestionFields() {
  const scoreQuestion = currentQuestions.find(question => question.type === "score") || currentQuestions[0] || {};
  const singleQuestion = currentQuestions.find(question => question.type === "single") || currentQuestions[1] || {};
  const textQuestion = currentQuestions.find(question => question.type === "text") || currentQuestions[currentQuestions.length - 1] || {};
  eventForm.elements.satisfactionQuestion.value = scoreQuestion.label || "Score / 评分";
  eventForm.elements.topicQuestion.value = singleQuestion.label || "Single choice / 单选题";
  eventForm.elements.topicOptions.value = (singleQuestion.options || ["Option"]).join("\n");
  eventForm.elements.freeTextQuestion.value = textQuestion.label || "Question / 问答题";
  eventForm.elements.questionsConfig.value = serializeQuestions(currentQuestions);
}

function hideLegacyQuestionFields() {
  ["satisfactionQuestion", "topicQuestion", "topicOptions", "freeTextQuestion"].forEach(name => {
    const field = eventForm.elements[name];
    if (field && field.closest("label")) {
      field.closest("label").classList.add("hidden");
    }
  });
}

function renderQuestionBuilder() {
  if (!questionList) {
    return;
  }
  if (!currentQuestions.length) {
    currentQuestions = defaultQuestions();
  }
  questionList.innerHTML = currentQuestions.map((question, index) => `
    <article class="question-card" data-question-index="${index}">
      <div class="question-card-head">
        <strong>问题 ${index + 1} / Question ${index + 1}</strong>
        <button type="button" class="danger-button" data-remove-question="${index}">删除 / Delete</button>
      </div>
      <div class="split">
        <label>
          类型 / Type
          <select data-question-field="type">
            <option value="single" ${question.type === "single" ? "selected" : ""}>单选 / Single choice</option>
            <option value="multiple" ${question.type === "multiple" ? "selected" : ""}>多选 / Multiple choice</option>
            <option value="text" ${question.type === "text" ? "selected" : ""}>问答 / Text</option>
            <option value="score" ${question.type === "score" ? "selected" : ""}>评分 / Score 1-10</option>
          </select>
        </label>
        <label class="checkbox-line">
          <input type="checkbox" data-question-field="required" ${question.required ? "checked" : ""} />
          必填 / Required
        </label>
      </div>
      <label>
        问题文案 / Question Text
        <input data-question-field="label" value="${escapeHtml(question.label)}" required />
      </label>
      ${question.type === "single" || question.type === "multiple" ? `
        <label>
          选项 / Options
          <textarea data-question-field="options" rows="4" required>${escapeHtml((question.options || []).join("\n"))}</textarea>
        </label>
      ` : ""}
    </article>
  `).join("");

  questionList.querySelectorAll("[data-question-field]").forEach(input => {
    input.addEventListener("input", updateQuestionFromControl);
    input.addEventListener("change", updateQuestionFromControl);
  });
  questionList.querySelectorAll("[data-remove-question]").forEach(button => {
    button.addEventListener("click", () => {
      currentQuestions.splice(Number(button.dataset.removeQuestion), 1);
      renderQuestionBuilder();
    });
  });
  syncLegacyQuestionFields();
}

function updateQuestionFromControl(event) {
  const card = event.currentTarget.closest("[data-question-index]");
  const question = currentQuestions[Number(card.dataset.questionIndex)];
  const field = event.currentTarget.dataset.questionField;
  if (field === "required") {
    question.required = event.currentTarget.checked;
  } else if (field === "options") {
    question.options = event.currentTarget.value.split(/\r?\n/).map(item => item.trim()).filter(Boolean);
  } else {
    question[field] = event.currentTarget.value;
    if (field === "type") {
      question.options = question.type === "single" || question.type === "multiple" ? (question.options && question.options.length ? question.options : [""]) : [];
      renderQuestionBuilder();
      return;
    }
  }
  syncLegacyQuestionFields();
}

function fillDefaults() {
  eventForm.reset();
  eventForm.elements.id.value = "";
  formTitle.textContent = "新建活动 / New Event";
  for (const [key, value] of Object.entries(defaults)) {
    eventForm.elements[key].value = value;
  }
  currentQuestions = defaultQuestions();
  renderQuestionBuilder();
  hideLegacyQuestionFields();
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
  currentQuestions = event.questions && event.questions.length ? event.questions.map(question => ({
    id: question.id,
    type: question.type,
    label: question.label,
    required: question.required,
    options: question.options || []
  })) : defaultQuestions();
  renderQuestionBuilder();
  hideLegacyQuestionFields();
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
          <h3 data-user-content>${escapeHtml(event.title || "Untitled Event")}</h3>
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
          <button type="button" data-copy-event="${event.id}" data-title="${escapeHtml(event.title || "Untitled Event")}" class="secondary">复制活动 / Copy</button>
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

  eventList.querySelectorAll("[data-copy-event]").forEach(button => {
    button.addEventListener("click", async () => {
      const confirmed = window.confirm(window.JSysLocale.isEnglish()
        ? `Copy event “${button.dataset.title}”?\nOnly settings will be copied. Registrations and winners will not be copied.`
        : `复制活动「${button.dataset.title}」吗？\nOnly event settings will be copied. Registrations and winners will not be copied.`);
      if (!confirmed) {
        return;
      }
      try {
        const copied = await api(`/api/admin/events/${button.dataset.copyEvent}/copy`, { method: "POST", body: "" });
        await loadEvents();
        const event = await api(`/api/admin/events/${copied.id}`);
        fillEvent(event);
        window.scrollTo({ top: 0, behavior: "smooth" });
      } catch (error) {
        alert(error.message);
      }
    });
  });

  eventList.querySelectorAll("[data-submissions]").forEach(button => {
    button.addEventListener("click", async () => {
      const eventId = button.dataset.submissions;
      const box = document.querySelector(`#submissions-${eventId}`);
      const event = await api(`/api/admin/events/${eventId}`);
      const submissions = await api(`/api/admin/events/${eventId}/submissions`);
      box.classList.toggle("hidden");
      box.innerHTML = renderSubmissions(submissions, event.questions || [], eventId);
      bindSubmissionDeleteButtons(box, eventId);
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
      const confirmed = window.confirm(window.JSysLocale.isEnglish()
        ? `Delete event “${button.dataset.title}”?\nThis will delete the event, submissions, winners, and operation records.`
        : `确定删除活动「${button.dataset.title}」吗？\nThis will delete the event, submissions, winners, and operation records.`);
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
  box.querySelectorAll("[data-delete-winner]").forEach(button => {
    button.addEventListener("click", async () => {
      const confirmed = window.confirm(window.JSysLocale.isEnglish() ? "Delete this winner record?" : "确定删除这条中奖记录吗？/ Delete this winner record?");
      if (!confirmed) {
        return;
      }
      try {
        await api(`/api/admin/events/${eventId}/winners/${button.dataset.deleteWinner}`, { method: "DELETE" });
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

function renderSubmissions(submissions, questions = [], eventId = "") {
  if (!submissions.length) {
    return `<p class="empty">No registrations yet. / 暂无报名记录</p>`;
  }
  const visibleQuestions = questions.length ? questions : [
    { id: "score", label: "Satisfaction Score" },
    { id: "topic", label: "Single-choice Answer" },
    { id: "future", label: "Future Discussion" }
  ];

  return `
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Job Title</th>
          <th>Email</th>
          ${visibleQuestions.map(question => `<th data-user-content>${escapeHtml(question.label)}</th>`).join("")}
          <th>操作 / Action</th>
        </tr>
      </thead>
      <tbody>
        ${submissions.map(item => `
          <tr>
            <td data-user-content>${escapeHtml(item.name)}</td>
            <td data-user-content>${escapeHtml(item.jobTitle)}</td>
            <td data-user-content>${escapeHtml(item.email)}</td>
            ${visibleQuestions.map(question => `<td>${renderAnswerCell((item.answers && item.answers[question.id]) || legacyAnswer(item, question.id), question)}</td>`).join("")}
            <td><button type="button" class="danger-button" data-delete-submission="${item.id}" data-name="${escapeHtml(item.name || item.email)}" data-event-id="${escapeHtml(eventId)}">删除 / Delete</button></td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function renderAnswerCell(answer, question) {
  if (question.type === "multiple") {
    const values = String(answer || "").split(/\r?\n/).map(item => item.trim()).filter(Boolean);
    if (!values.length) {
      return "";
    }
    return `<ul class="answer-list">${values.map(value => `<li data-user-content>${escapeHtml(value)}</li>`).join("")}</ul>`;
  }
  return `<span data-user-content>${escapeHtml(answer || "")}</span>`;
}

function legacyAnswer(item, questionId) {
  if (questionId === "score") {
    return item.satisfactionScore || "";
  }
  if (questionId === "topic") {
    return item.topicAnswer || "";
  }
  if (questionId === "future") {
    return item.futureQuestion || "";
  }
  return "";
}

function bindSubmissionDeleteButtons(box, eventId) {
  box.querySelectorAll("[data-delete-submission]").forEach(button => {
    button.addEventListener("click", async () => {
      const confirmed = window.confirm(window.JSysLocale.isEnglish()
        ? `Delete registration for “${button.dataset.name}”?\nRelated winner records for this participant will also be removed.`
        : `确定删除报名人员「${button.dataset.name}」吗？\nRelated winner records for this participant will also be removed.`);
      if (!confirmed) {
        return;
      }
      try {
        await api(`/api/admin/events/${eventId}/submissions/${button.dataset.deleteSubmission}`, { method: "DELETE", body: "" });
        const event = await api(`/api/admin/events/${eventId}`);
        const submissions = await api(`/api/admin/events/${eventId}/submissions`);
        box.innerHTML = renderSubmissions(submissions, event.questions || [], eventId);
        bindSubmissionDeleteButtons(box, eventId);
        const winnersBox = document.querySelector(`#winners-${eventId}`);
        if (winnersBox && !winnersBox.classList.contains("hidden")) {
          await refreshWinners(eventId);
        }
      } catch (error) {
        alert(error.message);
      }
    });
  });
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
            <td data-user-content>${escapeHtml(winner.name)}</td>
            <td data-user-content>${escapeHtml(winner.email)}</td>
            <td>${escapeHtml(winner.status)}</td>
            <td>${escapeHtml(winner.source)}</td>
            <td>${formatAdminTime(winner.createdAt)}</td>
            <td>
              ${winner.status === "valid" ? `<button type="button" class="secondary" data-void="${winner.id}">作废</button>` : ""}
              ${winner.status === "voided" ? `<button type="button" class="secondary" data-redraw="${winner.id}">补抽</button>` : ""}
              <button type="button" class="danger-button" data-delete-winner="${winner.id}">删除</button>
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
            <td>${formatAdminTime(operation.createdAt)}</td>
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

function formatAdminTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return escapeHtml(value);
  }
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23"
  }).formatToParts(date);
  const values = Object.fromEntries(parts
    .filter(part => part.type !== "literal")
    .map(part => [part.type, part.value]));
  return `${values.year}-${values.month}-${values.day} ${values.hour}:${values.minute}:${values.second} UTC+8`;
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
    <h2 data-user-content>${escapeHtml(event.title)}</h2>
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

function renderDynamicQuestionInput(question) {
  const name = `answer_${question.id}`;
  const required = question.required ? "required" : "";
  if (question.type === "score") {
    return `
      <label>
        <span data-user-content>${escapeHtml(question.label)}</span>
        <select name="${escapeHtml(name)}" ${required}>
          <option value="">请选择 / Select</option>
          ${Array.from({ length: 10 }, (_, index) => `<option value="${index + 1}">${index + 1}</option>`).join("")}
        </select>
      </label>
    `;
  }
  if (question.type === "single") {
    return `
      <fieldset>
        <legend data-user-content>${escapeHtml(question.label)}</legend>
        ${(question.options || []).map(option => `
          <label class="radio-line">
            <input type="radio" name="${escapeHtml(name)}" value="${escapeHtml(option)}" ${required} />
            <span data-user-content>${escapeHtml(option)}</span>
          </label>
        `).join("")}
      </fieldset>
    `;
  }
  if (question.type === "multiple") {
    return `
      <fieldset>
        <legend data-user-content>${escapeHtml(question.label)}</legend>
        ${(question.options || []).map(option => `
          <label class="radio-line">
            <input type="checkbox" name="${escapeHtml(name)}" value="${escapeHtml(option)}" />
            <span data-user-content>${escapeHtml(option)}</span>
          </label>
        `).join("")}
      </fieldset>
    `;
  }
  return `<label><span data-user-content>${escapeHtml(question.label)}</span> <textarea name="${escapeHtml(name)}" rows="4" ${required}></textarea></label>`;
}

function renderJoinForm(event) {
  showPublic(joinView);
  sessionLabel.textContent = "Registration";
  logoutButton.classList.add("hidden");

  if (event.status !== "active") {
    renderJoinUnavailable("This event is not open for registration. / 本场活动暂未开放报名。");
    return;
  }

  const questions = event.questions && event.questions.length ? event.questions : defaultQuestions();
  joinView.innerHTML = `
    <h2 data-user-content>${escapeHtml(event.title)}</h2>
    <form id="joinForm" class="form-grid">
      <label>姓名 / Name <input name="name" required /></label>
      <label>职位 / Job Title <input name="jobTitle" required /></label>
      <label>邮箱 / Email <input name="email" type="email" required /></label>
      ${questions.map(renderDynamicQuestionInput).join("")}
      <p class="privacy" data-user-content>${escapeHtml(event.privacyNotice)}</p>
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
        <h2 data-user-content>${escapeHtml(event.title)}</h2>
        <p class="waiting">抽奖尚未完成，请关注会议画面或稍后刷新。</p>
        <p class="waiting">The draw has not finished yet. Please watch the meeting screen or refresh later.</p>
      `;
      return;
    }
    resultView.innerHTML = `
      <h2 data-user-content>${escapeHtml(event.title)}</h2>
      <p class="waiting">中奖结果 / Winners</p>
      <ul class="winner-list">
        ${result.winners.map(winner => `<li data-user-content>${escapeHtml(winner.name)} <span>${escapeHtml(winner.email)}</span></li>`).join("")}
      </ul>
    `;
  } catch (error) {
    resultView.innerHTML = `<h2>结果页不可用 / Result unavailable</h2><p class="message">${escapeHtml(error.message)}</p>`;
  }
}

function renderScreenCompleted(event, result, adminSession) {
  const winnerCount = result.winners.length;
  const canDrawMore = Boolean(adminSession) && winnerCount < event.winningCount;
  const canPickAnother = Boolean(adminSession) && winnerCount > 0;
  screenView.innerHTML = `
    <div class="screen-stage">
      <p class="eyebrow">Lucky Draw</p>
      <h2 data-user-content>${escapeHtml(event.title)}</h2>
      <div id="rollingName" class="rolling-name" data-user-content>Ready</div>
      <p class="screen-progress">已抽 ${winnerCount} / ${event.winningCount} · Winners ${winnerCount} / ${event.winningCount}</p>
      <div id="screenCongrats" class="screen-congrats hidden">恭喜中奖 / Congratulations</div>
      <ul id="screenWinners" class="screen-winners hidden">
        ${result.winners.map(winner => `<li data-user-content>${escapeHtml(winner.name)}<span>${escapeHtml(winner.email)}</span></li>`).join("")}
      </ul>
      ${canDrawMore || canPickAnother ? `
        <div class="screen-actions hidden" id="screenNextActions">
          ${canDrawMore ? `<button id="screenDrawButton" type="button" class="screen-draw-button">下一位中奖者 / Next Winner</button>` : ""}
          ${canPickAnother ? `<button id="screenPickAnotherButton" type="button" class="screen-secondary-button">换一位 / Pick Another</button>` : ""}
          <p id="screenDrawMessage" class="screen-note"></p>
        </div>
      ` : ""}
    </div>
  `;

  const rollingName = document.querySelector("#rollingName");
  const screenCongrats = document.querySelector("#screenCongrats");
  const screenWinners = document.querySelector("#screenWinners");
  const screenNextActions = document.querySelector("#screenNextActions");
  const names = result.winners.length ? result.winners.map(winner => winner.name) : ["Ready"];
  let tick = 0;
  const timer = setInterval(() => {
    rollingName.textContent = names[tick % names.length];
    tick += 1;
  }, 120);
  setTimeout(() => {
    clearInterval(timer);
    rollingName.classList.add("hidden");
    screenCongrats.classList.remove("hidden");
    screenWinners.classList.remove("hidden");
    if (screenNextActions) {
      screenNextActions.classList.remove("hidden");
      bindScreenDrawButton(event);
      bindScreenPickAnotherButton(event, result);
    }
  }, 1500);
}

function renderScreenRolling(event, names) {
  const rollingNames = names.length ? names : ["Ready"];
  screenView.innerHTML = `
    <div class="screen-stage">
      <p class="eyebrow">Lucky Draw</p>
      <h2 data-user-content>${escapeHtml(event.title)}</h2>
      <div id="rollingName" class="rolling-name" data-user-content>Ready</div>
      <div class="screen-actions">
        <button id="screenRevealButton" type="button" class="screen-draw-button">确认抽奖 / Confirm Draw</button>
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

function bindScreenDrawButton(event) {
  const drawButton = document.querySelector("#screenDrawButton");
  if (!drawButton) {
    return;
  }
  drawButton.addEventListener("click", async () => {
    const message = document.querySelector("#screenDrawMessage");
    drawButton.disabled = true;
    drawButton.textContent = "抽奖中... / Drawing...";
    if (message) {
      message.textContent = "";
    }
    let stopRolling = null;
    try {
      const [submissions, currentResult] = await Promise.all([
        api(`/api/admin/events/${event.id}/submissions`),
        api(`/api/events/${event.id}/results`)
      ]);
      const validWinnerSubmissionIds = new Set(currentResult.winners.map(winner => winner.submissionId));
      const names = submissions
        .filter(submission => !validWinnerSubmissionIds.has(submission.id))
        .map(submission => submission.name || submission.email || "Participant");
      stopRolling = renderScreenRolling(event, names);
      const revealButton = document.querySelector("#screenRevealButton");
      if (revealButton) {
        revealButton.addEventListener("click", async () => {
          revealButton.disabled = true;
          revealButton.textContent = "开奖中... / Revealing...";
          try {
            await api(`/api/admin/events/${event.id}/draw`, { method: "POST", body: "" });
            const latest = await api(`/api/events/${event.id}/results`);
            stopRolling();
            renderScreenCompleted(event, latest, true);
          } catch (error) {
            stopRolling();
            await renderScreenPage(event.id);
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
      await renderScreenPage(event.id);
      const nextMessage = document.querySelector("#screenDrawMessage");
      if (nextMessage) {
        nextMessage.textContent = error.message;
      } else {
        alert(error.message);
      }
    }
  });
}

function bindScreenPickAnotherButton(event, result) {
  const pickAnotherButton = document.querySelector("#screenPickAnotherButton");
  if (!pickAnotherButton || !result.winners.length) {
    return;
  }
  const currentWinner = result.winners[result.winners.length - 1];
  pickAnotherButton.addEventListener("click", async () => {
    const message = document.querySelector("#screenDrawMessage");
    pickAnotherButton.disabled = true;
    pickAnotherButton.textContent = "重新抽取中... / Picking...";
    if (message) {
      message.textContent = "";
    }
    try {
      await api(`/api/admin/events/${event.id}/winners/${currentWinner.id}/replace`, { method: "POST", body: "" });
      const latest = await api(`/api/events/${event.id}/results`);
      renderScreenCompleted(event, latest, true);
    } catch (error) {
      pickAnotherButton.disabled = false;
      pickAnotherButton.textContent = "换一位 / Pick Another";
      const friendlyMessage = friendlyPickAnotherError(error);
      if (message) {
        message.textContent = friendlyMessage;
      }
      alert(friendlyMessage);
    }
  });
}

function friendlyPickAnotherError(error) {
  if (String(error.message || "").includes("No replacement available")) {
    return "没有可替换候选人了，请保留当前中奖者或增加报名候选人。/ No replacement available.";
  }
  return error.message;
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
          <h2 data-user-content>${escapeHtml(event.title)}</h2>
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
                  renderScreenCompleted(event, latest, true);
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

    renderScreenCompleted(event, result, adminSession);
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

showRegisterButton.addEventListener("click", showRegister);
backToLoginButton.addEventListener("click", showLogin);
backToAdminButton.addEventListener("click", async () => {
  const user = await api("/api/admin/me").catch(() => null);
  if (!user) {
    showLogin();
    return;
  }
  showAdmin(user.username);
  await loadEvents();
});

registerForm.addEventListener("submit", async event => {
  event.preventDefault();
  registerMessage.textContent = "";
  try {
    const user = await api("/api/admin/register", {
      method: "POST",
      body: encodeForm(registerForm)
    });
    showAdmin(user.username);
    fillDefaults();
    await loadEvents();
  } catch (error) {
    registerMessage.textContent = error.message;
  }
});

settingsButton.addEventListener("click", showSettings);
settingsForm.addEventListener("submit", async event => {
  event.preventDefault();
  settingsMessage.textContent = "";
  try {
    const updated = await api("/api/admin/settings", {
      method: "PUT",
      body: encodeForm(settingsForm)
    });
    showLogin();
    loginMessage.textContent = `设置已保存（${updated.workspaceName}），请使用新密码重新登录。`;
  } catch (error) {
    settingsMessage.textContent = error.message;
  }
});

logoutButton.addEventListener("click", async () => {
  await api("/api/admin/logout", { method: "POST", body: "" }).catch(() => {});
  showLogin();
});

eventForm.addEventListener("submit", async event => {
  event.preventDefault();
  eventMessage.textContent = "";
  syncLegacyQuestionFields();

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
addQuestionButton.addEventListener("click", () => {
  currentQuestions.push(newQuestion("text"));
  renderQuestionBuilder();
});

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
  await window.JSysLocale.load();
  configureAccountMode();
  if (window.JSysLocale.isEnglish()) {
    defaults = Object.fromEntries(Object.entries(defaults).map(([key, value]) => [
      key,
      typeof value === "string" ? value.split("\n").map(window.JSysLocale.toEnglish).join("\n") : value
    ]));
  }
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
