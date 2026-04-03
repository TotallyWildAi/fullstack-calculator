import { useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { useCalculateQuery } from '../api/calculatorApi'
import { selectToken, clearToken } from '../api/authSlice'

export default function CalculatorPage() {
  const token = useSelector(selectToken)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [a, setA] = useState<number | ''>('')
  const [b, setB] = useState<number | ''>('')
  const [op, setOp] = useState('add')
  const [shouldCalculate, setShouldCalculate] = useState(false)

  if (!token) {
    navigate('/login', { replace: true })
    return null
  }

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

  const handleLogout = () => {
    dispatch(clearToken())
    navigate('/login', { replace: true })
  }

  return (
    <div className="card" style={{ position: 'relative' }}>
      <button className="logout-btn" onClick={handleLogout}>
        Sign out
      </button>
      <h1>Calculator</h1>
      <p className="subtitle">Perform arithmetic operations</p>
      <form onSubmit={handleCalculate}>
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="input-a">Value A</label>
            <input
              id="input-a"
              data-testid="input-a"
              type="number"
              placeholder="0"
              value={a}
              onChange={(e) => {
                setA(e.target.value === '' ? '' : Number(e.target.value))
                setShouldCalculate(false)
              }}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="input-b">Value B</label>
            <input
              id="input-b"
              data-testid="input-b"
              type="number"
              placeholder="0"
              value={b}
              onChange={(e) => {
                setB(e.target.value === '' ? '' : Number(e.target.value))
                setShouldCalculate(false)
              }}
              required
            />
          </div>
        </div>
        <div className="form-group">
          <label htmlFor="select-op">Operation</label>
          <select
            id="select-op"
            data-testid="select-op"
            value={op}
            onChange={(e) => {
              setOp(e.target.value)
              setShouldCalculate(false)
            }}
          >
            <option value="add">Add (+)</option>
            <option value="sub">Subtract (-)</option>
            <option value="mul">Multiply (*)</option>
            <option value="div">Divide (/)</option>
          </select>
        </div>
        <button
          data-testid="calculate-button"
          className="btn btn-primary"
          type="submit"
          disabled={isLoading || a === '' || b === ''}
        >
          {isLoading ? 'Calculating...' : 'Calculate'}
        </button>
      </form>
      {result && (
        <div data-testid="result-display" className="result-display">
          <span className="result-label">Result: </span>
          <span className="result-value">{result.result}</span>
        </div>
      )}
    </div>
  )
}
