import { useState } from 'react'
import { useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { useCalculateQuery } from '../api/calculatorApi'
import { selectToken } from '../api/authSlice'

export default function CalculatorPage() {
  const token = useSelector(selectToken)
  const navigate = useNavigate()
  const [a, setA] = useState<number | ''>('')
  const [b, setB] = useState<number | ''>('')
  const [op, setOp] = useState('add')
  const [shouldCalculate, setShouldCalculate] = useState(false)

  // Redirect to login if not authenticated
  if (!token) {
    navigate('/login', { replace: true })
    return null
  }

  // Only run query when shouldCalculate is true and inputs are valid
  const { data: result, isLoading } = useCalculateQuery(
    { a: Number(a), b: Number(b), op },
    { skip: !shouldCalculate || a === '' || b === '' }
  )

  const handleCalculate = (e: React.FormEvent) => {
    e.preventDefault()
    if (a !== '' && b !== '') {
      setShouldCalculate(true)
    }
  }

  return (
    <div className="calculator-page">
      <h1>Calculator</h1>
      <form onSubmit={handleCalculate}>
        <div>
          <label htmlFor="input-a">A:</label>
          <input
            id="input-a"
            data-testid="input-a"
            type="number"
            value={a}
            onChange={(e) => setA(e.target.value === '' ? '' : Number(e.target.value))}
            required
          />
        </div>
        <div>
          <label htmlFor="input-b">B:</label>
          <input
            id="input-b"
            data-testid="input-b"
            type="number"
            value={b}
            onChange={(e) => setB(e.target.value === '' ? '' : Number(e.target.value))}
            required
          />
        </div>
        <div>
          <label htmlFor="select-op">Operation:</label>
          <select
            id="select-op"
            data-testid="select-op"
            value={op}
            onChange={(e) => setOp(e.target.value)}
          >
            <option value="add">add</option>
            <option value="sub">sub</option>
            <option value="mul">mul</option>
            <option value="div">div</option>
          </select>
        </div>
        <button
          data-testid="calculate-button"
          type="submit"
          disabled={isLoading || a === '' || b === ''}
        >
          {isLoading ? 'Calculating...' : 'Calculate'}
        </button>
      </form>
      {result && (
        <div data-testid="result-display" className="result">
          Result: {result.result}
        </div>
      )}
    </div>
  )
}
