// HashTools.jsx
import React, { useState } from 'react';
import './HashTools.css';

function HashTools() {
  const [inputText, setInputText] = useState('');
  const [algorithm, setAlgorithm] = useState('sha256');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  
  const calculateHash = async () => {
    if (!inputText) {
      alert('Please enter some text to hash');
      return;
    }
    
    setLoading(true);
    
    try {
      const response = await fetch('http://localhost:8081/api/debug/HashTools/process', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          operation: algorithm,
          text: inputText
        })
      });
      
      const data = await response.json();
      setResult(data);
    } catch (error) {
      console.error('Error calculating hash:', error);
      setResult({ error: 'Failed to calculate hash' });
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="hash-tools-container">
      <h2>Hash Calculator</h2>
      
      <div className="form-group">
        <label htmlFor="algorithm">Select Algorithm:</label>
        <select 
          id="algorithm"
          value={algorithm}
          onChange={(e) => setAlgorithm(e.target.value)}
        >
          <option value="sha256">SHA-256</option>
          <option value="md5">MD5</option>
        </select>
      </div>
      
      <div className="form-group">
        <label htmlFor="input-text">Text to Hash:</label>
        <textarea
          id="input-text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Enter text to hash..."
          rows={5}
        />
      </div>
      
      <button 
        className="hash-button"
        onClick={calculateHash}
        disabled={loading}
      >
        {loading ? 'Calculating...' : 'Calculate Hash'}
      </button>
      
      {result && (
        <div className="result-container">
          <h3>Result:</h3>
          {result.success ? (
            <>
              <div className="result-item">
                <strong>Algorithm:</strong> {result.algorithm}
              </div>
              <div className="result-item">
                <strong>Input:</strong> {result.input}
              </div>
              <div className="result-item">
                <strong>Hash:</strong> <code>{result.hash}</code>
              </div>
            </>
          ) : (
            <div className="error-message">
              Error: {result.error}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default HashTools;