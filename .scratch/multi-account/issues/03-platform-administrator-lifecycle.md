# 03 — Platform Administrator 与账号生命周期

**What to build:** 提供不在普通登录页出现的内部 Platform Administrator 入口，用于查看账号基础信息、禁用/重新启用账号和线下密码重置，而不接触任何业务数据。

**Blocked by:** 02 — 分公司账号注册与业务隔离.

**Status:** ready-for-agent

- [ ] 首次部署从受保护配置创建 Platform Administrator，后续重启不覆盖其密码。
- [ ] Platform Administrator 只能查看工作区名称、邮箱和状态，并执行禁用、重新启用、设置新密码。
- [ ] 禁用立即撤销账号会话并关闭其公开活动页面；重新启用后恢复，数据与审计记录保留。
- [ ] Platform Administrator 无法访问活动、报名、中奖、导出或业务审计。
