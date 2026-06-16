import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import EbookDetail from '../views/EbookDetail.vue'
import Search from '../views/Search.vue'
import NoteList from '../views/NoteList.vue'
import NoteEdit from '../views/NoteEdit.vue'
import AiChat from '../views/AiChat.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminDashboard from '../views/admin/Dashboard.vue'
import AdminEbook from '../views/admin/EbookManage.vue'
import AdminCategory from '../views/admin/CategoryManage.vue'
import AdminDoc from '../views/admin/DocManage.vue'
import AdminUser from '../views/admin/UserManage.vue'
import NotFound from '../views/error/NotFound.vue'
import ServerError from '../views/error/ServerError.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/ebook/:id', name: 'EbookDetail', component: EbookDetail },
  { path: '/search', name: 'Search', component: Search },
  { path: '/notes', name: 'NoteList', component: NoteList },
  { path: '/note/edit/:id?', name: 'NoteEdit', component: NoteEdit },
  { path: '/ai', name: 'AiChat', component: AiChat },
  { path: '/admin', name: 'Admin', component: AdminLayout,
    children: [
      { path: '', name: 'AdminDashboard', component: AdminDashboard },
      { path: 'ebook', name: 'AdminEbook', component: AdminEbook },
      { path: 'category', name: 'AdminCategory', component: AdminCategory },
      { path: 'doc', name: 'AdminDoc', component: AdminDoc },
      { path: 'user', name: 'AdminUser', component: AdminUser }
    ]
  },
  { path: '/404', name: 'NotFound', component: NotFound },
  { path: '/500', name: 'ServerError', component: ServerError },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const u = JSON.parse(sessionStorage.getItem('user') || '{}')
  if (to.path.startsWith('/admin')) { if (!u.token) { next('/'); return } if (u.role !== 'admin') { next('/'); return } }
  if (to.path === '/ai' && !u.token) { next('/login'); return }
  if (to.path.startsWith('/note/edit') && !u.token) { next('/login'); return }
  next()
})
export default router
