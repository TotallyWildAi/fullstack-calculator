import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../store'

interface LoginRequest {
  username: string
  password: string
}

interface LoginResponse {
  token: string
}

interface CalculateRequest {
  a: number
  b: number
  op: string
}

interface CalculateResponse {
  a: number
  b: number
  op: string
  result: number
}

export const calculatorApi = createApi({
  reducerPath: 'calculatorApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      const state = getState() as RootState
      const token = state.auth.token
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  endpoints: (builder) => ({
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
    }),
    calculate: builder.query<CalculateResponse, CalculateRequest>({
      query: ({ a, b, op }) => ({
        url: '/calculate',
        method: 'GET',
        params: { a, b, op },
      }),
    }),
  }),
})

export const { useLoginMutation, useCalculateQuery, useLazyCalculateQuery } = calculatorApi
