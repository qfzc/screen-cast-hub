import { post, get } from './request'
import type { ApiResponse } from './request'

export interface LoginParams {
  account: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email?: string
}

export interface UserInfo {
  id: number
  name: string
  phone: string
  email: string
  avatar: string | null
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export const authApi = {
  login(params: LoginParams): Promise<ApiResponse<LoginResult>> {
    return post('/auth/login', params)
  },

  register(params: RegisterParams): Promise<ApiResponse<LoginResult>> {
    return post('/auth/register', params)
  },

  getCurrentUser(): Promise<ApiResponse<UserInfo>> {
    return get('/auth/me')
  },

  logout(): Promise<ApiResponse<void>> {
    return post('/auth/logout')
  }
}
