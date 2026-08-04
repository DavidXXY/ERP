import { defineStore } from "pinia";
import { currentUserApi, loginApi, type CurrentUser } from "@/api/auth";
import { AUTH_TOKEN_KEY } from "@/api/http";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: sessionStorage.getItem(AUTH_TOKEN_KEY) || "",
    user: null as CurrentUser | null,
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    normalizeUser(user: CurrentUser) {
      const roleCodes = user.roleCodes ?? user.roles ?? [];
      return { ...user, roleCodes };
    },
    async login(username: string, password: string, mfaCode?: string) {
      this.token = "";
      this.user = null;
      sessionStorage.removeItem(AUTH_TOKEN_KEY);
      const response = await loginApi({ username, password, mfaCode });
      if (response.mfaRequired) return false;
      if (!response.token || !response.user) throw new Error("登录响应不完整");
      this.token = response.token;
      this.user = this.normalizeUser(response.user);
      this.initialized = true;
      sessionStorage.setItem(AUTH_TOKEN_KEY, response.token);
      return true;
    },
    async loadCurrentUser() {
      if (!this.token) {
        this.initialized = true;
        return;
      }
      this.user = this.normalizeUser(await currentUserApi());
      this.initialized = true;
    },
    logout() {
      this.token = "";
      this.user = null;
      this.initialized = true;
      sessionStorage.removeItem(AUTH_TOKEN_KEY);
    },
    can(permission: string) {
      const roles = this.user?.roleCodes ?? [];
      if (roles.includes("ADMIN")) return true;
      return this.user?.permissions?.includes(permission) ?? false;
    },
  },
});
