import { computed, reactive } from "vue";
import { clearAuth, getToken, getUser, setAuth, type AuthUser } from "@/utils/storage";

const state = reactive({
  token: getToken(),
  user: getUser() as AuthUser | null
});

export function useUserStore() {
  const isLoggedIn = computed(() => Boolean(state.token));
  return {
    state,
    isLoggedIn,
    setLogin(token: string, user: AuthUser) {
      state.token = token;
      state.user = user;
      setAuth(token, user);
    },
    logoutLocal() {
      state.token = "";
      state.user = null;
      clearAuth();
    }
  };
}
