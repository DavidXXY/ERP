import { defineStore } from "pinia";
import { currentUserApi, loginApi, type CurrentUser } from "@/api/auth";
import { AUTH_TOKEN_KEY } from "@/api/http";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(AUTH_TOKEN_KEY) || "",
    user: null as CurrentUser | null,
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    async login(username: string, password: string) {
      const response = await loginApi({ username, password });
      this.token = response.token;
      this.user = response.user;
      this.initialized = true;
      localStorage.setItem(AUTH_TOKEN_KEY, response.token);
    },
    async loadCurrentUser() {
      if (!this.token) {
        this.initialized = true;
        return;
      }
      this.user = await currentUserApi();
      this.initialized = true;
    },
    logout() {
      this.token = "";
      this.user = null;
      this.initialized = true;
      localStorage.removeItem(AUTH_TOKEN_KEY);
    },
    can(permission: string) {
      const roles = this.user?.roleCodes ?? [];
      if (roles.includes('ADMIN')) return true;
      return this.user?.permissions?.includes(permission) ?? false;
    },
  },
});

