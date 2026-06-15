import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/ebook/:id',
    name: 'EbookDetail',
    component: () => import('../views/EbookDetail.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/Search.vue')
  },
  {
    path: '/notes',
    name: 'NoteList',
    component: () => import('../views/NoteList.vue')
  },
  {
    path: '/note/edit/:id?',
    name: 'NoteEdit',
    component: () => import('../views/NoteEdit.vue')
  },
  {
    path: '/ai',
    name: 'AiChat',
    component: () => import('../views/AiChat.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'ebook', name: 'AdminEbook', component: () => import('../views/admin/EbookManage.vue') },
      { path: 'category', name: 'AdminCategory', component: () => import('../views/admin/CategoryManage.vue') },
      { path: 'doc', name: 'AdminDoc', component: () => import('../views/admin/DocManage.vue') },
      { path: 'user', name: 'AdminUser', component: () => import('../views/admin/UserManage.vue') }
    ]
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('../views/error/NotFound.vue')
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('../views/error/ServerError.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStr = sessionStorage.getItem('user')
  const user = userStr ? JSON.parse(userStr) : {}

  if (to.path.startsWith('/admin')) {
    if (!user.token) {
      next('/')
      return
    }
    if (user.name !== '管理员' && user.role !== 'admin') {
      next('/')
      return
    }
  }

  if (to.path === '/ai' && !user.token) {
    next('/login')
    return
  }

  if (to.path.startsWith('/note/edit') && !user.token) {
    next('/login')
    return
  }

  next()
})

export default router