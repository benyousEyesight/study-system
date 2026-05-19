const TOKEN_KEY = 'study_access_token'
const REFRESH_KEY = 'study_refresh_token'
const USER_KEY = 'study_user_info'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_KEY, token)
}

export function getUserInfo(): any {
  const info = localStorage.getItem(USER_KEY)
  return info ? JSON.parse(info) : null
}

export function setUserInfo(info: any): void {
  localStorage.setItem(USER_KEY, JSON.stringify(info))
}
