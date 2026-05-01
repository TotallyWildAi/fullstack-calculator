import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { RootState } from '../store'

interface AuthState {
  token: string | null
  refresh_token: string | null
}

const initialState: AuthState = {
  token: null,
  refresh_token: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<{ token: string; refresh_token: string }>) => {
      state.token = action.payload.token
      state.refresh_token = action.payload.refresh_token
    },
    clearToken: (state) => {
      state.token = null
      state.refresh_token = null
    },
  },
})

export const { setToken, clearToken } = authSlice.actions

export const selectToken = (state: RootState): string | null => state.auth.token
export const selectRefreshToken = (state: RootState): string | null => state.auth.refresh_token

export default authSlice.reducer
