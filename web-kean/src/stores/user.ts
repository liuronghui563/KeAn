import { defineStore } from "pinia";
import { clearAuth, getToken, getUser, setAuth, type AdminUser } from "@/utils/storage";

export const useUserStore = defineStore("admin-user", {
  state: () => ({
    token: getToken(),
    user: getUser() as AdminUser | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setLogin(token: string, user: AdminUser) {
      this.token = token;
      this.user = user;
      setAuth(token, user);
    },
    logoutLocal() {
      this.token = "";
      this.user = null;
      clearAuth();
    }
  }
});
