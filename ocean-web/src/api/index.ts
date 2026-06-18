import request from './request'

export const ebookApi = {
  list: (params: any) => request.get('/api/ebook/list', { params }),
  save: (data: any) => request.post('/api/ebook/save', data),
  delete: (id: number | string) => request.delete(`/api/ebook/delete/${id}`)
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
  vote: (id: number | string) => request.post(`/api/doc/vote/${id}`)
}

export const userApi = {
  login: (data: any) => request.post('/api/user/login', data),
  logout: () => request.get('/api/user/logout'),
  list: (params: any) => request.get('/api/user/list', { params }),
  save: (data: any) => request.post('/api/user/save', data),
  delete: (id: number | string) => request.delete(`/api/user/delete/${id}`),
  resetPassword: (data: any) => request.post('/api/user/reset-password', data)
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
  vote: (id: number | string) => request.post(`/api/note/vote/${id}`)
}

export const aiApi = {
  chat: (data: any) => request.post('/api/ai/chat', data),
  generate: (data: any) => request.post('/api/ai/generate', data),
  conversations: (params?: any) => request.get('/api/ai/conversations', { params }),
  messages: (id: string) => request.get(`/api/ai/conversations/${id}/messages`),
  delete: (id: string) => request.delete(`/api/ai/conversations/${id}`),
  usage: () => request.get('/api/ai/usage')
}