(() => {
  let english = false;
  const han = /[\u3400-\u9fff]/;
  const replacements = new Map([
    ["多场活动扫码抽奖系统", "Multi-event QR Lucky Draw"],
    ["未登录", "Not signed in"], ["已登录：", "Signed in: "], ["报名页", "Registration"],
    ["中奖结果", "Winners"], ["大屏展示", "Draw Screen"], ["新建活动", "New Event"],
    ["编辑活动", "Edit Event"], ["还没有活动。请先创建一个活动。", "No events yet. Create your first event."],
    ["未命名活动", "Untitled Event"], ["中奖人数", "Winners"], ["报名链接", "Registration link"],
    ["结果链接", "Results link"], ["大屏链接", "Screen link"], ["抽奖二维码", "Lucky draw QR code"],
    ["创建活动后会生成报名链接和二维码目标地址。", "Create an event to generate its registration link and QR code."],
    ["每个活动可以配置不同问题，支持单选、多选、问答和评分。", "Configure each event with single-choice, multiple-choice, text, and score questions."],
    ["已复制", "Copied"], ["复制", "Copy"], ["已保存：", "Saved: "], ["暂无报名记录。", "No registrations yet."], ["暂无中奖记录。", "No winners yet."],
    ["暂无操作记录。", "No activity records yet."], ["姓名", "Name"], ["职位", "Job Title"], ["邮箱", "Email"],
    ["满意度", "Satisfaction"], ["单选答案", "Single-choice answer"], ["后续交流", "Follow-up topic"],
    ["状态", "Status"], ["来源", "Source"], ["时间", "Time"], ["操作人", "Operator"], ["动作", "Action"],
    ["目标", "Target"], ["报名成功", "Registration submitted"], ["提交报名", "Submit registration"],
    ["请选择", "Select"], ["抽奖尚未完成，请关注会议画面或稍后刷新。", "The draw is not complete. Please check back soon."],
    ["抽奖进行中", "Drawing from registered participants..."], ["开奖中...", "Revealing..."], ["抽奖中...", "Drawing..."],
    ["确认抽奖", "Confirm Draw"], ["开始抽奖", "Start Draw"], ["下一位中奖者", "Next Winner"],
    ["换一位", "Pick Another"], ["恭喜中奖", "Congratulations"], ["抽奖尚未开始", "Waiting for the draw"],
    ["大屏不可用", "Screen unavailable"], ["结果页不可用", "Results unavailable"], ["报名暂不可用", "Registration unavailable"],
    ["类型", "Type"], ["必填", "Required"], ["问题文案", "Question text"], ["选项", "Options"],
    ["问题", "Question"], ["删除", "Delete"], ["编辑", "Edit"], ["查看报名", "View registrations"],
    ["开始抽奖", "Start draw"], ["查看中奖", "View winners"], ["操作记录", "Activity log"], ["导出 Excel", "Export Excel"],
    ["复制活动", "Copy event"], ["登录", "Log in"], ["退出", "Log out"], ["保存活动", "Save event"], ["清空", "Reset"]
  ]);

  function englishText(value) {
    if (!english || !value || !han.test(value)) return value;
    const bilingualParts = value.split(/\s*\/\s*/).map(part => part.trim()).filter(Boolean);
    const bilingualEnglish = bilingualParts.filter(part => !han.test(part) && /[A-Za-z]/.test(part));
    if (bilingualEnglish.length) return [...new Set(bilingualEnglish)].join(" / ");
    let text = value;
    for (const [source, target] of replacements) text = text.split(source).join(target);
    return text.replace(/[\u3400-\u9fff]+/g, "").replace(/[、。]+/g, "").replace(/\s{2,}/g, " ").trim();
  }

  function translate(root = document.body) {
    if (!english || !root) return;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
      acceptNode(node) {
        return node.parentElement && !["SCRIPT", "STYLE"].includes(node.parentElement.tagName) && han.test(node.nodeValue)
          ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
      }
    });
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);
    nodes.forEach(node => { node.nodeValue = englishText(node.nodeValue); });
    root.querySelectorAll("[alt], [title], [placeholder]").forEach(element => {
      ["alt", "title", "placeholder"].forEach(attribute => {
        if (element.hasAttribute(attribute)) element.setAttribute(attribute, englishText(element.getAttribute(attribute)));
      });
    });
  }

  async function load() {
    try {
      const response = await fetch("/api/config", { credentials: "same-origin" });
      const config = response.ok ? await response.json() : {};
      english = config.locale === "en";
    } catch {
      english = false;
    }
    if (english) {
      document.documentElement.lang = "en";
      document.title = "J_Sys Lucky Draw";
      translate(document.documentElement);
      new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType === Node.TEXT_NODE && han.test(node.nodeValue)) node.nodeValue = englishText(node.nodeValue);
        if (node.nodeType === Node.ELEMENT_NODE) translate(node);
      }))).observe(document.body, { childList: true, subtree: true });
    }
  }

  window.JSysLocale = { load, isEnglish: () => english, toEnglish: englishText, translate };
})();
