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

// Mock react-router-dom to capture useNavigate calls
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

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
  render(
    <Provider store={testStore}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </Provider>
  )
  return testStore
}

describe('CalculatorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockNavigate.mockClear()
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

  it('clears token and navigates to login when Sign out is clicked', async () => {
    const user = userEvent.setup()

    vi.mocked(calculatorApiModule.useCalculateQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any)

    const store = renderWithProviders(<CalculatorPage />, 'test-token-123')

    // Verify token is present before logout
    expect(store.getState().auth.token).toBe('test-token-123')

    // Find and click the Sign out button
    const signOutButton = screen.getByRole('button', { name: /sign out/i })
    await user.click(signOutButton)

    // Verify token is cleared from Redux store
    expect(store.getState().auth.token).toBeNull()

    // Verify useNavigate was called with '/login' and replace: true
    expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true })

    // Verify calculator inputs are no longer visible (component redirected)
    await waitFor(() => {
      expect(screen.queryByTestId('input-a')).not.toBeInTheDocument()
    })
  })

  it('hides input-b and sends correct payload for sqrt', async () => {
    const user = userEvent.setup()
    const mockUseCalculateQuery = vi.mocked(calculatorApiModule.useCalculateQuery)
    const mockResult = { a: 9, b: 0, op: 'sqrt', result: 3 }

    mockUseCalculateQuery.mockReturnValue({
      data: mockResult,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, 'test-token')

    // Initially, input-b should be visible (default op is 'add')
    expect(screen.getByTestId('input-b')).toBeInTheDocument()

    // Select sqrt operation
    const selectOp = screen.getByTestId('select-op')
    await user.selectOptions(selectOp, 'sqrt')

    // After selecting sqrt, input-b should not be visible
    expect(screen.queryByTestId('input-b')).not.toBeInTheDocument()

    // Type value for input-a
    const inputA = screen.getByTestId('input-a')
    await user.type(inputA, '9')

    // Click calculate button
    const calculateButton = screen.getByTestId('calculate-button')
    await user.click(calculateButton)

    // Verify that useCalculateQuery was called with the correct parameters
    // The mock is called with { a: 9, b: 0, op: 'sqrt' } as the first argument
    expect(mockUseCalculateQuery).toHaveBeenCalled()
    const callArgs = mockUseCalculateQuery.mock.calls[mockUseCalculateQuery.mock.calls.length - 1]
    expect(callArgs[0]).toEqual({ a: 9, b: 0, op: 'sqrt' })
  })

  it('hides input-b and sends correct payload for log', async () => {
    const user = userEvent.setup()
    const mockUseCalculateQuery = vi.mocked(calculatorApiModule.useCalculateQuery)
    const mockResult = { a: 100, b: 0, op: 'log', result: 2 }

    mockUseCalculateQuery.mockReturnValue({
      data: mockResult,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, 'test-token')

    // Select log operation
    const selectOp = screen.getByTestId('select-op')
    await user.selectOptions(selectOp, 'log')

    // After selecting log, input-b should not be visible
    expect(screen.queryByTestId('input-b')).not.toBeInTheDocument()

    // Type value for input-a
    const inputA = screen.getByTestId('input-a')
    await user.type(inputA, '100')

    // Click calculate button
    const calculateButton = screen.getByTestId('calculate-button')
    await user.click(calculateButton)

    // Verify that useCalculateQuery was called with the correct parameters
    expect(mockUseCalculateQuery).toHaveBeenCalled()
    const callArgs = mockUseCalculateQuery.mock.calls[mockUseCalculateQuery.mock.calls.length - 1]
    expect(callArgs[0]).toEqual({ a: 100, b: 0, op: 'log' })
  })

  it('shows input-b for pow operation', async () => {
    const user = userEvent.setup()
    const mockUseCalculateQuery = vi.mocked(calculatorApiModule.useCalculateQuery)

    mockUseCalculateQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any)

    renderWithProviders(<CalculatorPage />, 'test-token')

    // Select pow operation
    const selectOp = screen.getByTestId('select-op')
    await user.selectOptions(selectOp, 'pow')

    // After selecting pow, input-b should still be visible
    expect(screen.getByTestId('input-b')).toBeInTheDocument()
  })
})
