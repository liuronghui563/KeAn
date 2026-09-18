import { createRouter, createWebHistory } from "vue-router";
import { getToken, getUser } from "@/utils/storage";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: () => import("@/views/login/index.vue"),
      meta: { public: true }
    },
    {
      path: "/",
      component: () => import("@/layouts/AdminLayout.vue"),
      redirect: "/dashboard",
      children: [
        {
          path: "dashboard",
          name: "dashboard",
          component: () => import("@/views/dashboard/index.vue"),
          meta: { title: "Dashboard" }
        }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const token = getToken();
  if (!to.meta.public && !token) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && token) {
    const user = getUser();
    if (user?.role === "ADMIN") {
      return { path: "/dashboard" };
    }
  }
  return true;
});

export default router;
