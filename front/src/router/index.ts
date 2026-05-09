import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../pages/LoginPage.vue'
import RegisterPage from '../pages/RegisterPage.vue'
import ResetPasswordPage from '../pages/ResetPasswordPage.vue'
import HomePage from '../pages/HomePage.vue'
import ProfilePage from '../pages/ProfilePage.vue'
import ChatPage from '../pages/ChatPage.vue'
import FriendPage from '../pages/FriendPage.vue'
import GroupPage from '../pages/GroupPage.vue'
import MomentPage from '../pages/MomentPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', component: LoginPage, meta: { isPublic: true } },
    { path: '/register', component: RegisterPage, meta: { isPublic: true } },
    { path: '/reset-password', component: ResetPasswordPage, meta: { isPublic: true } },
    { path: '/home', component: HomePage, meta: { requiresAuth: true } },
    { path: '/friends', component: FriendPage, meta: { requiresAuth: true } },
    { path: '/chats', component: ChatPage, meta: { requiresAuth: true } },
    { path: '/groups', component: GroupPage, meta: { requiresAuth: true } },
    { path: '/moments', component: MomentPage, meta: { requiresAuth: true } },
    { path: '/profile', component: ProfilePage, meta: { requiresAuth: true } },
  ],
})

// Route guard: check authentication
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('authToken')
  
  // If route requires auth but no token, redirect to login
  if (to.meta.requiresAuth && !token) {
    next('/login')
  }
  // If route is public but user is logged in, redirect to home
  else if (to.meta.isPublic && token && (to.path === '/login' || to.path === '/register')) {
    next('/home')
  }
  else {
    next()
  }
})

export default router
