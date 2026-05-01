import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { RootState } from '../store'

interface AuthState {
  token: string | null
  refreshToken: string | null
}

const initialState: AuthState = {
  token: null,
  refreshToken: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload
    },
    setRefreshToken: (state, action: PayloadAction<string>) => {
      state.refreshToken = action.payload
    },
    setTokens: (state, action: PayloadAction<{ access_token: string; refresh_token: string }>) => {
      state.token = action.payload.access_token
      state.refreshToken = action.payload.refresh_token
    },
    clearToken: (state) => {
      state.token = null
      state.refreshToken = null
    },
  },
})

export const { setToken, setRefreshToken, setTokens, clearToken } = authSlice.actions

export const selectToken = (state: RootState): string | null => state.auth.token
export const selectRefreshToken = (state: RootState): string | null => state.auth.refreshToken

export default authSlice.reducer
