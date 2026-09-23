import { defineStore } from "pinia";
import * as api from "./api";

export const usePortalStore = defineStore("supplier-portal", {
  state: () => ({ session: null as api.Session | null, loading: false }),
  getters: {
    canQuote: (state) =>
      state.session?.account.status === "ACTIVE" &&
      !state.session?.account.mustChangePassword &&
      state.session?.supplier.admissionStatus === "APPROVED" &&
      state.session?.supplier.riskStatus !== "BLOCKED",
  },
  actions: {
    async restore() {
      if (!sessionStorage.getItem(api.SUPPLIER_TOKEN_KEY)) return false;
      this.session = await api.getSession();
      sessionStorage.setItem(api.SUPPLIER_TOKEN_KEY, this.session.token);
      return true;
    },
    setSession(session: api.Session) {
      this.session = session;
      sessionStorage.setItem(api.SUPPLIER_TOKEN_KEY, session.token);
    },
    logout() {
      this.session = null;
      sessionStorage.removeItem(api.SUPPLIER_TOKEN_KEY);
    },
  },
});
