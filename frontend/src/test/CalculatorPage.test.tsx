import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import CalculatorPage from '../pages/CalculatorPage'
import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../api/authSlice'
import { calculatorApi } from '../api/calculatorApi'
import * as calculatorApiModule from '../api/calculatorApi'

// Mock the calculatorApi module
vi.mock('../api/calculatorApi', async () => {
  const actual = await vi.importActual('../api/calculatorApi')
  return {
    ...actual,
    useCalculateQuery: vi.fn(),
  }
})

const createTestStore = (token: string | null = null) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      [calculatorApi.reducerPath]: calculatorApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(calculatorApi.middleware),
    preloadedState: {
      auth: { token },
      [calculatorApi.reducerPath]: calculatorApi.reducer(undefined, { type: '' }),
    },
  })
}

const renderWithProviders = (component: React.ReactElement, token: string | null = null) => {
  const testStore = createTestStore(token)
  return render(
    <Provider store={testStore}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </Provider>
  )
}

describe('CalculatorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('redirects to /login when not authenticated', () => {
    vi.mocked(calculatorApiModule.useCalculateQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, null)

    // Component should redirect, so calculator inputs should not be visible
    expect(screen.queryByTestId('input-a')).not.toBeInTheDocument()
  })

  it('renders calculator inputs when authenticated', () => {
    vi.mocked(calculatorApiModule.useCalculateQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, 'test-token')

    expect(screen.getByTestId('input-a')).toBeInTheDocument()
    expect(screen.getByTestId('input-b')).toBeInTheDocument()
    expect(screen.getByTestId('select-op')).toBeInTheDocument()
    expect(screen.getByTestId('calculate-button')).toBeInTheDocument()
  })

  it('displays result after calculation', async () => {
    const user = userEvent.setup()
    const mockResult = { a: 5, b: 3, op: 'add', result: 8 }

    vi.mocked(calculatorApiModule.useCalculateQuery).mockReturnValue({
      data: mockResult,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, 'test-token')

    const inputA = screen.getByTestId('input-a')
    const inputB = screen.getByTestId('input-b')
    const calculateButton = screen.getByTestId('calculate-button')

    await user.type(inputA, '5')
    await user.type(inputB, '3')
    await user.click(calculateButton)

    await waitFor(() => {
      expect(screen.getByTestId('result-display')).toBeInTheDocument()
      expect(screen.getByTestId('result-display')).toHaveTextContent('Result: 8')
    })
  })
})
