import { defineStore } from "pinia";
import type { CurrentUser } from "@/types/domain";
import { currentUserApi, loginApi, wechatLoginApi } from "@/api/auth";
import { TOKEN_KEY, USER_KEY, readStorage, removeStorage, writeStorage } from "@/utils/storage";

export const useAuthStore = defineStore("mobile-auth", {
  state: () => ({ token: "", user: null as CurrentUser | null, initialized: false }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    roleCodes: (state) => state.user?.roleCodes || state.user?.roles || [],
  },
  actions: {
    restore() {
      this.token = readStorage(TOKEN_KEY, "");
      this.user = readStorage<CurrentUser | null>(USER_KEY, null);
      this.initialized = true;
      if (this.token) void this.refresh();
    },
    async login(username: string, password: string) {
      this.applySession(await loginApi(username, password));
    },
    async loginWithWechat() {
      const code = await new Promise<string>((resolve, reject) => uni.login({ provider: "weixin", success: (r) => r.code ? resolve(r.code) : reject(new Error("未获取到微信登录凭证")), fail: reject }));
      this.applySession(await wechatLoginApi(code));
    },
    async refresh() {
      try {
        this.user = await currentUserApi();
        writeStorage(USER_KEY, this.user);
      } catch { this.logout(false); }
    },
    applySession(session: { token: string; user: CurrentUser }) {
      this.token = session.token;
      this.user = session.user;
      writeStorage(TOKEN_KEY, session.token);
      writeStorage(USER_KEY, session.user);
    },
    can(permission: string) {
      return this.roleCodes.includes("ADMIN") || Boolean(this.user?.permissions.includes(permission));
    },
    logout(navigate = true) {
      this.token = "";
      this.user = null;
      removeStorage(TOKEN_KEY);
      removeStorage(USER_KEY);
      if (navigate) uni.reLaunch({ url: "/pages/login/index" });
    },
  },
});
