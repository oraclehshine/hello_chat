import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import UsersPage from './pages/UsersPage.vue'
import GroupsPage from './pages/GroupsPage.vue'
import SensitiveWordsPage from './pages/SensitiveWordsPage.vue'
import AnnouncementsPage from './pages/AnnouncementsPage.vue'
import AuditLogsPage from './pages/AuditLogsPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/dashboard', component: DashboardPage },
    { path: '/users', component: UsersPage },
    { path: '/groups', component: GroupsPage },
    { path: '/sensitive-words', component: SensitiveWordsPage },
    { path: '/announcements', component: AnnouncementsPage },
    { path: '/audit-logs', component: AuditLogsPage },
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('adminToken')
  if (!to.meta.public && !token) {
    next('/login')
    return
  }
  if (to.meta.public && token) {
    next('/dashboard')
    return
  }
  next()
})

export default router

