import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, setUserInfo, removeToken } from '@/utils/token'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<any>(getUserInfo())
  const token = ref<string | null>(null)

  function setInfo(info: any) {
    userInfo.value = info
    setUserInfo(info)
  }

  function logout() {
    userInfo.value = null
    token.value = null
    removeToken()
  }

  return { userInfo, token, setInfo, logout }
})
