import './App.css'

import React, { useEffect, useState } from 'react'

function App() {
  const [language, setLanguage] = useState('Java')
  const [languages, setLanguages] = useState([
    'Java',
    'Python',
    'JavaScript'
  ])
  const [code, setCode] = useState('// Write your code here')
  const [output, setOutput] = useState('Output will appear here')
  const [backendStatus, setBackendStatus] = useState('Connecting...')
  const handleLanguageChange = (selectedLanguage) => {
  setLanguage(selectedLanguage)

  const samplePrograms = {
    Java: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, World!");\n    }\n}',
    Python: 'print("Hello, World!")',
    JavaScript: 'console.log("Hello, World!");'
  }

  setCode(samplePrograms[selectedLanguage])
}

  useEffect(() => {
    fetch('http://localhost:8081/api/languages')
      .then(response => {
        if (!response.ok) {
          throw new Error('Backend connection failed')
        }
        return response.json()
      })
      .then(data => {
        setLanguages(data)
        setBackendStatus('Connected')
      })
      .catch(error => {
        console.error(error)
        setBackendStatus('Disconnected')
      })
  }, [])

  const runCode = () => {
    setOutput('Code execution will be implemented in the next development stage.')
  }

  return (
    <div style={{
      background: '#111827',
      color: 'white',
      minHeight: '100vh',
      padding: '25px',
      fontFamily: 'Arial',
      textAlign: 'center'
    }}>
      <h1>PolyglotMesh</h1>
      <p>Multi-Language Code Execution Platform</p>

      <p>
        Backend Status: {' '}
        <strong style={{
          color: backendStatus === 'Connected' ? '#22c55e' : '#f59e0b'
        }}>
          {backendStatus}
        </strong>
      </p>

      <hr />

      <h2>Select Programming Language</h2>

      <select
        value={language}
        onChange={(e) => handleLanguageChange(e.target.value)}
        style={{
          padding: '12px',
          width: '220px',
          fontSize: '16px'
        }}
      >
        {languages.map(lang => (
          <option key={lang} value={lang}>
            {lang}
          </option>
        ))}
      </select>

      <h2>Code Editor</h2>

      <textarea
        value={code}
        onChange={(e) => setCode(e.target.value)}
        placeholder="Write your code here"
        style={{
          width: '95%',
          height: '250px',
          background: '#1f2937',
          color: 'white',
          padding: '15px',
          fontSize: '16px'
        }}
      />

      <br />
      <br />

      <button
        onClick={runCode}
        style={{
          padding: '12px 25px',
          background: '#2563eb',
          color: 'white',
          border: 'none',
          cursor: 'pointer'
        }}
      >
        Run Code
      </button>

      <h2>Output Console</h2>

      <div style={{
        background: '#1f2937',
        padding: '20px',
        minHeight: '80px'
      }}>
        {output}
      </div>
    </div>
  )
}

export default App
