/**
 * WebSocket 客户端
 * 用于接收实时通知（点赞、评论等）
 * 自动管理连接生命周期和断线重连
 */
class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string = ''
  private reconnectTimer: any = null
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 10
  private reconnectInterval: number = 3000
  private listeners: Map<string, Set<(data: any) => void>> = new Map()
  private isDestroyed: boolean = false

  /**
   * 建立 WebSocket 连接
   * @param token JWT token，用于认证
   */
  connect(token: string) {
    if (this.isDestroyed) return
    if (this.ws && this.ws.readyState === WebSocket.OPEN) return

    // 根据当前页面协议选择 ws 或 wss
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    this.url = `${protocol}//${host}/ws?token=${token}`

    this.doConnect()
  }

  private doConnect() {
    if (this.isDestroyed) return
    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        this.reconnectAttempts = 0
      }

      this.ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          this.dispatch(msg.type, msg.data)
        } catch {}
      }

      this.ws.onclose = () => {
        this.scheduleReconnect()
      }

      this.ws.onerror = () => {
        this.ws?.close()
      }
    } catch {
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect() {
    if (this.isDestroyed) return
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return

    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      this.doConnect()
    }, this.reconnectInterval)
  }

  /**
   * 注册事件监听
   * @param event 事件类型（notification, unread_count 等）
   * @param callback 回调函数
   */
  on(event: string, callback: (data: any) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(callback)
  }

  /**
   * 移除事件监听
   */
  off(event: string, callback: (data: any) => void) {
    this.listeners.get(event)?.delete(callback)
  }

  private dispatch(event: string, data: any) {
    this.listeners.get(event)?.forEach(callback => {
      try { callback(data) } catch {}
    })
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.isDestroyed = true
    clearTimeout(this.reconnectTimer)
    this.ws?.close()
    this.ws = null
  }
}

// 导出单例
const wsClient = new WebSocketClient()
export default wsClient
