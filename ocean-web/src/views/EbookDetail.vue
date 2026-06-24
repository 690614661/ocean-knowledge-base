<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="detail-layout">
      <!-- 目录树侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">📖 文档目录</span>
        </div>
        <div class="sidebar-tree" v-if="docTree.length > 0">
          <a-tree
            :tree-data="docTree"
            :field-names="{ title: 'name', key: 'id', children: 'children' }"
            :selected-keys="[selectedDocId]"
            @select="onDocSelect"
            class="doc-tree"
          />
        </div>
        <div class="sidebar-empty" v-else>
          <span>暂无文档</span>
        </div>
      </div>

      <!-- 文档内容 -->
      <div class="doc-content-area">
        <template v-if="currentDoc">
          <div class="doc-header">
            <h1 class="doc-title">{{ currentDoc.name }}</h1>
            <div class="doc-meta">
              <span class="meta-item">👁 {{ currentDoc.viewCount }} 阅读</span>
              <span class="meta-divider">|</span>
              <span class="meta-item">👍 {{ currentDoc.voteCount || 0 }} 点赞</span>
              <span class="meta-divider">|</span>
              <span class="meta-item">📅 {{ currentDoc.updateTime?.slice(0, 10) }}</span>
            </div>
          </div>
          <div class="doc-body" v-html="currentDoc.content"></div>
          <div class="doc-actions">
            <a-button
              v-if="user.token"
              type="primary"
              size="large"
              class="vote-btn"
              :class="{ 'voted': voted }"
              @click="handleVote"
            >
              <span class="vote-icon">{{ voted ? '💙' : '🤍' }}</span>
              {{ voted ? '已点赞' : '点赞' }}
              <span class="vote-count">({{ currentDoc.voteCount || 0 }})</span>
            </a-button>
            <a-button
              v-if="user.token"
              size="large"
              class="fav-btn"
              :class="{ 'favorited': favorited }"
              @click="handleFavorite"
              :loading="favLoading"
            >
              <span class="fav-icon">{{ favorited ? '⭐' : '☆' }}</span>
              {{ favorited ? '已收藏' : '收藏' }}
              <span class="vote-count">({{ currentDoc.favoriteCount || 0 }})</span>
            </a-button>
            <div class="action-hint" v-else>
              <a-button type="link" @click="$router.push('/login')">登录</a-button>后即可点赞和收藏
            </div>
          </div>

          <!-- 评论区 -->
          <div class="doc-comments">
            <h3 class="comments-title">💬 评论 ({{ comments.length }})</h3>

            <!-- 评论输入框 -->
            <div class="comment-input-area" v-if="user.token">
              <a-textarea
                v-model:value="newComment"
                :rows="3"
                placeholder="写下你的评论..."
                class="comment-textarea"
              />
              <div class="comment-input-actions">
                <span class="reply-hint" v-if="replyTo">回复 @{{ replyTo.userName }}：</span>
                <a-button v-if="replyTo" size="small" @click="cancelReply">取消回复</a-button>
                <a-button type="primary" size="small" class="comment-submit-btn" @click="submitComment" :loading="commentLoading">
                  发表评论
                </a-button>
              </div>
            </div>
            <div class="comment-login-hint" v-else>
              <a-button type="link" @click="$router.push('/login')">登录</a-button>后即可发表评论
            </div>

            <!-- 评论列表 -->
            <div class="comments-list" v-if="comments.length > 0">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-header">
                  <a-avatar :size="28" class="comment-avatar" v-if="comment.userAvatar" :src="comment.userAvatar" />
                  <a-avatar :size="28" class="comment-avatar" v-else>{{ comment.userName?.charAt(0) }}</a-avatar>
                  <span class="comment-user">{{ comment.userName }}</span>
                  <span class="comment-time">{{ comment.createTime?.slice(0, 16) }}</span>
                  <a-button v-if="user.userId === comment.userId" type="link" danger size="small"
                    class="comment-delete" @click="deleteComment(comment.id)">删除</a-button>
                </div>
                <div class="comment-body">{{ comment.content }}</div>
                <div class="comment-actions">
                  <a-button type="link" size="small" @click="setReplyTo(comment)" v-if="user.token">
                    回复
                  </a-button>
                </div>
                <!-- 子评论 -->
                <div v-if="comment.children && comment.children.length > 0" class="comment-replies">
                  <div v-for="child in comment.children" :key="child.id" class="comment-item reply-item">
                    <div class="comment-header">
                      <a-avatar :size="24" class="comment-avatar" v-if="child.userAvatar" :src="child.userAvatar" />
                      <a-avatar :size="24" class="comment-avatar" v-else>{{ child.userName?.charAt(0) }}</a-avatar>
                      <span class="comment-user">{{ child.userName }}</span>
                      <span v-if="child.replyToUserName" class="reply-to">回复 @{{ child.replyToUserName }}</span>
                      <span class="comment-time">{{ child.createTime?.slice(0, 16) }}</span>
                      <a-button v-if="user.userId === child.userId" type="link" danger size="small"
                        class="comment-delete" @click="deleteComment(child.id)">删除</a-button>
                    </div>
                    <div class="comment-body">{{ child.content }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="comments-empty">
              <span>暂无评论，快来抢沙发吧~</span>
            </div>
          </div>
        </template>
        <div class="doc-empty" v-else>
          <div class="empty-icon">📚</div>
          <p>请从左侧目录选择文档</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'
import { docApi, favoriteApi, commentApi } from '../api'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'EbookDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    const user = computed(() => store.state.user)
    const docTree = ref<any[]>([])
    const currentDoc = ref<any>(null)
    const selectedDocId = ref<number>(0)
    const voted = ref(false)
    const favorited = ref(false)
    const favLoading = ref(false)

    // 评论
    const comments = ref<any[]>([])
    const newComment = ref('')
    const commentLoading = ref(false)
    const replyTo = ref<any>(null)

    const stripTreeProps = (tree: any[]): any[] => {
      return tree.map(node => {
        const { parent, ...rest } = node
        if (rest.children && rest.children.length > 0) {
          rest.children = stripTreeProps(rest.children)
        }
        return rest
      })
    }

    const loadDocs = async () => {
      const ebookId = Number(route.params.id)
      if (!ebookId) { router.push('/'); return }
      const res: any = await docApi.list(ebookId)
      docTree.value = stripTreeProps(res.content || [])
    }

    const animateContentIn = () => {
      const els = document.querySelectorAll('.doc-header, .doc-body, .doc-actions')
      if (els.length) {
        els.forEach((el, i) => {
          anime.set(el, { opacity: 0, translateY: 20 })
          anime({
            targets: el,
            opacity: [0, 1],
            translateY: [20, 0],
            duration: 500,
            delay: 80 + i * 100,
            easing: 'easeOutCubic'
          })
        })
      }
    }

    const animateSidebarIn = () => {
      const items = document.querySelectorAll('.doc-tree .ant-tree-treenode')
      if (items.length) {
        anime({
          targets: items,
          opacity: [0, 1],
          translateX: [-15, 0],
          duration: 400,
          delay: anime.stagger(50, { start: 300 }),
          easing: 'easeOutCubic'
        })
      }
    }

    const onDocSelect = async (selectedKeys: any[]) => {
      if (selectedKeys.length === 0) return
      const docId = selectedKeys[0]
      selectedDocId.value = docId
      try {
        const res: any = await docApi.detail(docId)
        currentDoc.value = res.content
        voted.value = false
        favorited.value = false
        // 检查收藏状态
        if (user.value.token) {
          try {
            const favRes: any = await favoriteApi.check(docId)
            favorited.value = favRes.content?.favorited || false
          } catch {}
        }
        // 加载评论
        loadComments(docId)
        nextTick(() => animateContentIn())
      } catch {
        message.error('文档加载失败')
      }
    }

    // ===== 评论功能 =====

    const loadComments = async (docId: number) => {
      try {
        const res: any = await commentApi.list(docId)
        comments.value = res.content || []
      } catch {}
    }

    const setReplyTo = (comment: any) => {
      replyTo.value = { id: comment.id, userId: comment.userId, userName: comment.userName }
    }

    const cancelReply = () => {
      replyTo.value = null
    }

    const submitComment = async () => {
      if (!newComment.value.trim()) {
        message.warning('请输入评论内容')
        return
      }
      commentLoading.value = true
      try {
        await commentApi.save({
          docId: currentDoc.value.id,
          content: newComment.value,
          parentId: replyTo.value?.id || null,
          replyToUserId: replyTo.value?.userId || null,
          replyToUserName: replyTo.value?.userName || null
        })
        message.success('评论成功')
        newComment.value = ''
        replyTo.value = null
        loadComments(currentDoc.value.id)
      } catch {
        message.error('评论发表失败')
      } finally {
        commentLoading.value = false
      }
    }

    const deleteComment = async (id: number) => {
      try {
        await commentApi.delete(id)
        message.success('评论已删除')
        loadComments(currentDoc.value.id)
      } catch {
        message.error('删除失败')
      }
    }

    const handleVote = async () => {
      if (!currentDoc.value) return
      try {
        await docApi.vote(currentDoc.value.id)
        message.success('👍 点赞成功！')
        currentDoc.value.voteCount++
        voted.value = true
      } catch {}
    }

    const handleFavorite = async () => {
      if (!currentDoc.value || !user.value.token) return
      favLoading.value = true
      try {
        const res: any = await favoriteApi.toggle(currentDoc.value.id)
        const isFav = res.content?.favorited || false
        favorited.value = isFav
        if (currentDoc.value.favoriteCount !== undefined) {
          currentDoc.value.favoriteCount += isFav ? 1 : -1
          if (currentDoc.value.favoriteCount < 0) currentDoc.value.favoriteCount = 0
        }
        message.success(isFav ? '⭐ 收藏成功！' : '已取消收藏')
      } catch (e: any) {
        if (e?.response?.data?.message) {
          message.error(e.response.data.message)
        }
      } finally {
        favLoading.value = false
      }
    }

    onMounted(() => {
      loadDocs()
      // 目录树节点延迟动画
      setTimeout(() => animateSidebarIn(), 500)
    })

    return { docTree, currentDoc, selectedDocId, user, voted, favorited, favLoading,
      comments, newComment, commentLoading, replyTo,
      onDocSelect, handleVote, handleFavorite, setReplyTo, cancelReply, submitComment, deleteComment }
  }
})
</script>

<style scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  padding: 24px 0;
  min-height: calc(100vh - 200px);
}

.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  align-self: flex-start;
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.sidebar-tree {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.doc-tree {
  font-size: 14px;
}

.doc-tree :deep(.ant-tree-node-content-wrapper) {
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.doc-tree :deep(.ant-tree-node-content-wrapper:hover) {
  background: #e6f4ff;
  color: #1677ff;
}

.doc-tree :deep(.ant-tree-node-selected) {
  background: #e6f4ff !important;
  color: #1677ff !important;
  font-weight: 600;
}

.sidebar-empty {
  padding: 48px 20px;
  text-align: center;
  color: #999;
}

.doc-content-area {
  flex: 1;
  min-width: 0;
}

.doc-header {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.doc-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.doc-meta {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-divider {
  color: #e0e0e0;
}

.doc-body {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  line-height: 1.8;
  font-size: 15px;
  color: #333;
}

.doc-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 16px 0;
}

.doc-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.doc-body :deep(td),
.doc-body :deep(th) {
  border: 1px solid #e0e0e0;
  padding: 10px 14px;
}

.doc-body :deep(th) {
  background: #f8f9fa;
  font-weight: 600;
}

.doc-body :deep(p) {
  margin: 12px 0;
}

.doc-body :deep(h1), .doc-body :deep(h2), .doc-body :deep(h3) {
  color: #1a1a2e;
  margin: 24px 0 12px;
}

.doc-body :deep(blockquote) {
  border-left: 4px solid #1677ff;
  padding: 12px 20px;
  margin: 16px 0;
  background: #f8f9ff;
  border-radius: 0 8px 8px 0;
}

.doc-body :deep(code) {
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.doc-body :deep(pre) {
  background: #1e1e2e;
  color: #c9d1d9;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
}

.doc-actions {
  margin-top: 20px;
  text-align: center;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.vote-btn {
  border-radius: 24px;
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  border: none;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);
  transition: all 0.3s ease;
  margin-right: 12px;
}

.vote-btn:hover {
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.45);
  transform: translateY(-1px);
}

.vote-btn.voted {
  background: #f0f0f0;
  color: #999;
  box-shadow: none;
}

.fav-btn {
  border-radius: 24px;
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  border: 1px solid #e8ecf0;
  background: white;
  color: #666;
  transition: all 0.3s ease;
}

.fav-btn:hover {
  border-color: #faad14;
  color: #faad14;
}

.fav-btn.favorited {
  border-color: #faad14;
  background: #fffbe6;
  color: #faad14;
}

.fav-icon {
  margin-right: 4px;
  font-size: 16px;
}

.vote-icon {
  margin-right: 4px;
}

.vote-count {
  font-weight: 400;
  opacity: 0.8;
}

.action-hint {
  font-size: 14px;
  color: #999;
}

.doc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #999;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* 评论区样式 */
.doc-comments {
  margin-top: 20px;
  background: white;
  border-radius: 16px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.comments-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 20px;
}

.comment-input-area {
  margin-bottom: 20px;
}

.comment-textarea {
  border-radius: 12px;
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.reply-hint {
  font-size: 13px;
  color: #1677ff;
}

.comment-submit-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
}

.comment-login-hint {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
  border: 1px dashed #e8ecf0;
  border-radius: 12px;
  margin-bottom: 20px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: #fff;
  font-weight: 600;
  font-size: 12px;
}

.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.reply-to {
  font-size: 12px;
  color: #1677ff;
}

.comment-time {
  font-size: 12px;
  color: #bbb;
}

.comment-delete {
  margin-left: auto;
  font-size: 12px;
}

.comment-body {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.comment-actions {
  margin-top: 4px;
}

.comment-replies {
  margin-top: 12px;
  margin-left: 24px;
  padding-left: 16px;
  border-left: 3px solid #e8ecf0;
  background: #fafafa;
  border-radius: 0 8px 8px 0;
  padding: 8px 16px;
}

.reply-item {
  padding: 8px 0;
  border-bottom: none;
}

.comments-empty {
  text-align: center;
  padding: 32px;
  color: #bbb;
}
</style>