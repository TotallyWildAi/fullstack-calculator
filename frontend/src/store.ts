import { configureStore } from '@reduxjs/toolkit'
import authReducer from './api/authSlice'
import { calculatorApi } from './api/calculatorApi'

const store = configureStore({
  reducer: {
    auth: authReducer,
    [calculatorApi.reducerPath]: calculatorApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(calculatorApi.middleware),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

export default store
