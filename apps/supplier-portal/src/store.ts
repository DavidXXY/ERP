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
      if (!localStorage.getItem(api.SUPPLIER_TOKEN_KEY)) return false;
      this.session = await api.getSession();
      localStorage.setItem(api.SUPPLIER_TOKEN_KEY, this.session.token);
      return true;
    },
    setSession(session: api.Session) {
      this.session = session;
      localStorage.setItem(api.SUPPLIER_TOKEN_KEY, session.token);
    },
    logout() {
      this.session = null;
      localStorage.removeItem(api.SUPPLIER_TOKEN_KEY);
    },
  },
});
