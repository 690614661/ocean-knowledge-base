import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'

const request = axios.create({
  baseURL: '',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (user.token) {
        config.headers['token'] = user.token
      }
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.success === false) {
      message.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    // 忽略请求中止（如页面切换导致的轮询中断）
    if (axios.isCancel(error) || error.code === 'ERR_CANCELED' || error.message === 'Request aborted') {
      return Promise.reject(error)
    }
    if (error.response) {
      if (error.response.status === 401) {
        message.error('登录已过期，请重新登录')
        sessionStorage.removeItem('user')
        router.push('/login')
      } else if (error.response.status === 403) {
        message.error('无权限访问')
      } else {
        message.error(error.response.data?.message || '请求失败')
      }
    } else {
      message.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request