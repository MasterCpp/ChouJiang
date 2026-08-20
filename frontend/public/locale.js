(() => {
  const han = /[\u3400-\u9fff]/;
  const storageKey = "jsys-ui-language";
  let english = false;
  let englishInstance = false;
  const originalText = new WeakMap();
  const originalAttributes = new WeakMap();
  const replacements = new Map([
    ["作废", "Void"], ["补抽", "Redraw"], ["多场活动扫码抽奖系统", "Multi-event QR Lucky Draw"],
    ["未登录", "Not signed in"], ["已登录：", "Signed in: "], ["报名页", "Registration"], ["中奖结果", "Winners"], ["大屏展示", "Draw Screen"],
    ["新建活动", "New Event"], ["编辑活动", "Edit Event"], ["账号登录", "Account login"], ["账号设置", "Account settings"],
    ["注册新的分公司账号", "Register a new branch account"], ["注册分公司账号", "Register branch account"], ["注册后立即启用；每个账号的数据和活动彼此隔离。", "Your workspace is enabled immediately and isolated from other branch accounts."],
    ["工作区名称", "Workspace name"], ["登录邮箱", "Login email"], ["当前密码", "Current password"], ["新密码", "New password"], ["密码（8–128 位）", "Password (8–128 characters)"],
    ["注册并进入工作区", "Register and enter workspace"], ["返回登录", "Back to login"], ["返回活动管理", "Back to events"], ["保存设置", "Save settings"],
    ["还没有活动。请先创建一个活动。", "No events yet. Create your first event."], ["未命名活动", "Untitled Event"], ["中奖人数", "Winners"], ["报名链接", "Registration link"], ["结果链接", "Results link"], ["大屏链接", "Screen link"], ["抽奖二维码", "Lucky draw QR code"],
    ["创建活动后会生成报名链接和二维码目标地址。", "Create an event to generate its registration link and QR code."], ["每个活动可以配置不同问题，支持单选、多选、问答和评分。", "Configure each event with single-choice, multiple-choice, text, and score questions."],
    ["已复制", "Copied"], ["复制", "Copy"], ["已保存：", "Saved: "], ["暂无报名记录。", "No registrations yet."], ["暂无中奖记录。", "No winners yet."], ["暂无操作记录。", "No activity records yet."],
    ["姓名", "Name"], ["职位", "Job title"], ["邮箱", "Email"], ["满意度", "Satisfaction"], ["单选答案", "Single-choice answer"], ["后续交流", "Follow-up topic"], ["状态", "Status"], ["来源", "Source"], ["时间", "Time"], ["操作人", "Operator"], ["动作", "Action"], ["目标", "Target"],
    ["报名成功", "Registration submitted"], ["提交报名", "Submit registration"], ["请选择", "Select"], ["抽奖尚未完成，请关注会议画面或稍后刷新。", "The draw is not complete. Please check back soon."], ["抽奖进行中", "Drawing from registered participants..."], ["开奖中...", "Revealing..."], ["抽奖中...", "Drawing..."], ["确认抽奖", "Confirm draw"], ["开始抽奖", "Start draw"], ["下一位中奖者", "Next winner"], ["换一位", "Pick another"], ["恭喜中奖", "Congratulations"], ["抽奖尚未开始", "Waiting for the draw"], ["大屏不可用", "Screen unavailable"], ["结果页不可用", "Results unavailable"], ["报名暂不可用", "Registration unavailable"],
    ["类型", "Type"], ["必填", "Required"], ["问题文案", "Question text"], ["选项", "Options"], ["问题", "Question"], ["删除", "Delete"], ["编辑", "Edit"], ["查看报名", "View registrations"], ["查看中奖", "View winners"], ["操作记录", "Activity log"], ["导出 Excel", "Export Excel"], ["复制活动", "Copy event"], ["登录", "Log in"], ["退出", "Log out"], ["保存活动", "Save event"], ["清空", "Reset"],
    ["平台管理员", "Platform administrator"], ["内部账号管理入口", "Internal account management"], ["分公司账号", "Branch accounts"], ["刷新", "Refresh"], ["禁用", "Disable"], ["重新启用", "Re-enable"], ["设置新密码", "Set new password"], ["此处只提供账号生命周期操作，不显示任何活动、报名、中奖或导出数据。", "This page only manages account lifecycle. It does not display event, registration, winner, or export data."], ["还没有分公司账号。", "No branch accounts yet."]
  ]);

  function englishText(value) {
    if (!value || !han.test(value)) return value;
    const bilingualParts = value.split(/\s*\/\s*/).map(part => part.trim()).filter(Boolean);
    const bilingualEnglish = bilingualParts.filter(part => !han.test(part) && /[A-Za-z]/.test(part));
    if (bilingualEnglish.length) return [...new Set(bilingualEnglish)].join(" / ");
    let text = value;
    for (const [source, target] of replacements) text = text.split(source).join(target);
    return text.replace(/\s{2,}/g, " ").trim();
  }

  function chineseText(value) {
    if (!value || !/[A-Za-z]/.test(value)) return value;
    const bilingualParts = value.split(/\s*\/\s*/).map(part => part.trim()).filter(Boolean);
    const chineseParts = bilingualParts.filter(part => han.test(part));
    return chineseParts.length ? [...new Set(chineseParts)].join(" / ") : value;
  }

  function isTranslationExcluded(node) {
    return node.parentElement && node.parentElement.closest("[data-user-content], [data-locale-control]");
  }

  function translateTextNode(node) {
    if (isTranslationExcluded(node) || !han.test(node.nodeValue)) return;
    if (!originalText.has(node)) originalText.set(node, node.nodeValue);
    node.nodeValue = englishText(originalText.get(node));
  }

  function restoreTextNode(node) {
    if (isTranslationExcluded(node)) return;
    if (!originalText.has(node)) originalText.set(node, node.nodeValue);
    node.nodeValue = chineseText(originalText.get(node));
  }

  function translate(root = document.body) {
    if (!english || !root) return;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);
    nodes.forEach(translateTextNode);
    root.querySelectorAll?.("[alt], [title], [placeholder]").forEach(element => {
      if (element.closest("[data-user-content], [data-locale-control]")) return;
      ["alt", "title", "placeholder"].forEach(attribute => {
        if (!element.hasAttribute(attribute)) return;
        if (!originalAttributes.has(element)) originalAttributes.set(element, new Map());
        const originals = originalAttributes.get(element);
        if (!originals.has(attribute)) originals.set(attribute, element.getAttribute(attribute));
        element.setAttribute(attribute, englishText(originals.get(attribute)));
      });
    });
    renderExplicitLocale(root);
  }

  function restore(root = document.body) {
    if (!root) return;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);
    nodes.forEach(restoreTextNode);
    root.querySelectorAll?.("[alt], [title], [placeholder]").forEach(element => {
      const originals = originalAttributes.get(element);
      if (originals) originals.forEach((value, attribute) => element.setAttribute(attribute, chineseText(value)));
    });
    renderExplicitLocale(root);
  }

  function renderExplicitLocale(root) {
    root.querySelectorAll?.("[data-locale-zh][data-locale-en]").forEach(element => {
      element.textContent = english ? element.dataset.localeEn : element.dataset.localeZh;
    });
  }

  function renderToggle() {
    let toggle = document.querySelector("#languageToggle");
    if (englishInstance) {
      toggle?.remove();
      return;
    }
    if (!toggle) {
      toggle = document.createElement("button");
      toggle.id = "languageToggle";
      toggle.type = "button";
      toggle.className = "language-switch";
      toggle.setAttribute("data-locale-control", "true");
      toggle.addEventListener("click", () => setLanguage(english ? "zh" : "en"));
      document.body.append(toggle);
    }
    toggle.textContent = "中文 / English";
    toggle.setAttribute("aria-label", english ? "Switch to Chinese" : "切换为 English");
    toggle.setAttribute("aria-pressed", String(english));
  }

  function setLanguage(next) {
    if (englishInstance) return;
    english = next === "en";
    localStorage.setItem(storageKey, english ? "en" : "zh");
    if (english) {
      document.documentElement.lang = "en";
      translate(document.documentElement);
    } else {
      document.documentElement.lang = "zh-CN";
      restore(document.documentElement);
    }
    renderToggle();
    document.dispatchEvent(new CustomEvent("jsyslocalechange", { detail: { language: english ? "en" : "zh" } }));
  }

  async function load() {
    try {
      const response = await fetch("/api/config", { credentials: "same-origin" });
      const config = response.ok ? await response.json() : {};
      englishInstance = config.locale === "en";
    } catch {
      englishInstance = false;
    }
    english = englishInstance || localStorage.getItem(storageKey) === "en";
    if (english) {
      document.documentElement.lang = "en";
      document.title = englishInstance ? "J_Sys Lucky Draw" : englishText(document.title);
      translate(document.documentElement);
    } else {
      restore(document.documentElement);
    }
    renderToggle();
    new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
      if (node.nodeType === Node.TEXT_NODE && english) translateTextNode(node);
      if (node.nodeType === Node.TEXT_NODE && !english) restoreTextNode(node);
      if (node.nodeType === Node.ELEMENT_NODE && english) translate(node);
      if (node.nodeType === Node.ELEMENT_NODE && !english) restore(node);
    }))).observe(document.body, { childList: true, subtree: true });
  }

  window.JSysLocale = { load, isEnglish: () => english, isEnglishInstance: () => englishInstance, toChinese: chineseText, toEnglish: englishText, translate, setLanguage };
})();
