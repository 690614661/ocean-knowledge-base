import request from './request'

export const ebookApi = {
  list: (params: any) => request.get('/api/ebook/list', { params }),
  save: (data: any) => request.post('/api/ebook/save', data),
  delete: (id: number | string) => request.delete(`/api/ebook/delete/${id}`),
  deleteBatch: (ids: number[]) => request.post('/api/ebook/delete/batch', { ids })
}

export const categoryApi = {
  tree: () => request.get('/api/category/list'),
  all: () => request.get('/api/category/all'),
  save: (data: any) => request.post('/api/category/save', data),
  delete: (id: number | string) => request.delete(`/api/category/delete/${id}`)
}

export const docApi = {
  list: (ebookId: number | string) => request.get('/api/doc/list', { params: { ebookId } }),
  detail: (id: number | string) => request.get(`/api/doc/${id}`),
  save: (data: any) => request.post('/api/doc/save', data),
  delete: (id: number | string) => request.delete(`/api/doc/delete/${id}`),
  vote: (id: number | string) => request.post(`/api/doc/vote/${id}`),
  deleteBatch: (ids: number[]) => request.post('/api/doc/delete/batch', { ids })
}

export const userApi = {
  login: (data: any) => request.post('/api/user/login', data),
  logout: () => request.get('/api/user/logout'),
  list: (params: any) => request.get('/api/user/list', { params }),
  save: (data: any) => request.post('/api/user/save', data),
  delete: (id: number | string) => request.delete(`/api/user/delete/${id}`),
  deleteBatch: (ids: number[]) => request.post('/api/user/delete/batch', { ids }),
  resetPassword: (data: any) => request.post('/api/user/reset-password', data),
  profile: () => request.get('/api/user/profile'),
  updateProfile: (data: any) => request.post('/api/user/profile', data),
  changePassword: (data: any) => request.post('/api/user/change-password', data),
  history: (params: any) => request.get('/api/user/history', { params }),
  sendCode: (email: string) => request.post(`/api/user/send-code?email=${email}`),
  register: (data: any) => request.post('/api/user/register', data)
}

export const snapshotApi = {
  getStatistic: () => request.get('/api/snapshot/get-statistic')
}

export const fileApi = {
  upload: (formData: FormData) => request.post('/api/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const searchApi = {
  search: (params: any) => request.get('/api/search', { params })
}

export const noteApi = {
  myList: (params: any) => request.get('/api/note/list', { params }),
  publicList: (params: any) => request.get('/api/note/public', { params }),
  detail: (id: number | string) => request.get(`/api/note/${id}`),
  save: (data: any) => request.post('/api/note/save', data),
  delete: (id: number | string) => request.delete(`/api/note/delete/${id}`),
  vote: (id: number | string) => request.post(`/api/note/vote/${id}`),
  deleteBatch: (ids: number[]) => request.post('/api/note/delete/batch', { ids })
}

export const aiApi = {
  chat: (data: any) => request.post('/api/ai/chat', data, { timeout: 120000 }),
  generate: (data: any) => request.post('/api/ai/generate', data),
  conversations: (params?: any) => request.get('/api/ai/conversations', { params }),
  messages: (id: string) => request.get(`/api/ai/conversations/${id}/messages`),
  delete: (id: string) => request.delete(`/api/ai/conversations/${id}`),
  usage: () => request.get('/api/ai/usage'),
  chatStream: (data: any) => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    return fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': user.token || ''
      },
      body: JSON.stringify(data)
    })
  }
}

export const favoriteApi = {
  toggle: (docId: number | string) => request.post(`/api/favorite/toggle/${docId}`),
  check: (docId: number | string) => request.get(`/api/favorite/check/${docId}`),
  noteToggle: (noteId: number | string) => request.post(`/api/favorite/note/toggle/${noteId}`),
  noteCheck: (noteId: number | string) => request.get(`/api/favorite/note/check/${noteId}`),
  list: (params: any) => request.get('/api/favorite/list', { params })
}