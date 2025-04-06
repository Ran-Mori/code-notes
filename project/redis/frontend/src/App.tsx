import { useState } from 'react'
import { Button, TextField, Container, Typography, Box, Paper, Alert } from '@mui/material'
import axios from 'axios'

type OperationType = 'insert' | 'queryAll' | 'queryByKey' | 'deleteAll' | 'deleteByKey' | null;

interface KeyValue {
  [key: string]: string
}

function App() {
  const [operation, setOperation] = useState<OperationType>(null)
  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [result, setResult] = useState<KeyValue>({})
  const [message, setMessage] = useState('')

  const clearInputs = () => {
    setKey('')
    setValue('')
    setMessage('')
  }

  const handleSetKeyValue = async () => {
    if (!key || !value) {
      setMessage('请输入key和value')
      return
    }
    try {
      await axios.post('http://localhost:8080/api/key', { key, value })
      setResult({ [key]: value })
      setMessage('插入成功！')
      clearInputs()
    } catch (error) {
      setMessage('操作失败：' + (error as Error).message)
    }
  }

  const handleGetAllKeys = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/keys')
      setResult(response.data)
      setMessage(Object.keys(response.data).length ? '查询成功！' : '数据库为空')
    } catch (error) {
      setMessage('操作失败：' + (error as Error).message)
    }
  }

  const handleGetValueByKey = async () => {
    if (!key) {
      setMessage('请输入要查询的key')
      return
    }
    try {
      const response = await axios.get(`http://localhost:8080/api/key/${key}`)
      setResult(response.data)
      setMessage('查询成功！')
    } catch (error) {
      if ((error as any).response?.status === 404) {
        setMessage('未找到该key')
        setResult({})
      } else {
        setMessage('操作失败：' + (error as Error).message)
      }
    }
  }

  const handleDeleteAllKeys = async () => {
    try {
      await axios.delete('http://localhost:8080/api/keys')
      setResult({})
      setMessage('所有数据已删除！')
    } catch (error) {
      setMessage('操作失败：' + (error as Error).message)
    }
  }

  const handleDeleteKey = async () => {
    if (!key) {
      setMessage('请输入要删除的key')
      return
    }
    try {
      await axios.delete(`http://localhost:8080/api/key/${key}`)
      setResult({})
      setMessage(`成功删除key: ${key}`)
      clearInputs()
    } catch (error) {
      setMessage('操作失败：' + (error as Error).message)
    }
  }

  const renderInputArea = () => {
    switch (operation) {
      case 'insert':
        return (
          <Box sx={{ mb: 2 }}>
            <TextField
              fullWidth
              label="Key"
              value={key}
              onChange={(e) => setKey(e.target.value)}
              margin="normal"
            />
            <TextField
              fullWidth
              label="Value"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              margin="normal"
            />
            <Button variant="contained" color="primary" onClick={handleSetKeyValue} sx={{ mt: 2 }}>
              确认插入
            </Button>
          </Box>
        )
      case 'queryByKey':
      case 'deleteByKey':
        return (
          <Box sx={{ mb: 2 }}>
            <TextField
              fullWidth
              label="Key"
              value={key}
              onChange={(e) => setKey(e.target.value)}
              margin="normal"
            />
            <Button 
              variant="contained" 
              color="primary" 
              onClick={operation === 'queryByKey' ? handleGetValueByKey : handleDeleteKey}
              sx={{ mt: 2 }}
            >
              确认{operation === 'queryByKey' ? '查询' : '删除'}
            </Button>
          </Box>
        )
      default:
        return null
    }
  }

  const handleOperationClick = (op: OperationType) => {
    setOperation(op)
    clearInputs()
    setResult({})
    
    if (op === 'queryAll') {
      handleGetAllKeys()
    } else if (op === 'deleteAll') {
      handleDeleteAllKeys()
    }
  }

  return (
    <Container maxWidth="sm">
      <Box sx={{ my: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom align="center">
          Redis Demo
        </Typography>

        {/* 操作按钮区 */}
        <Paper elevation={3} sx={{ p: 2, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            选择操作
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button 
              variant={operation === 'insert' ? 'contained' : 'outlined'} 
              color="success" 
              onClick={() => handleOperationClick('insert')}
            >
              插入键值对
            </Button>
            <Button 
              variant={operation === 'queryAll' ? 'contained' : 'outlined'} 
              onClick={() => handleOperationClick('queryAll')}
            >
              查询所有
            </Button>
            <Button 
              variant={operation === 'queryByKey' ? 'contained' : 'outlined'} 
              onClick={() => handleOperationClick('queryByKey')}
            >
              根据key查询
            </Button>
            <Button 
              variant={operation === 'deleteAll' ? 'contained' : 'outlined'} 
              color="error" 
              onClick={() => handleOperationClick('deleteAll')}
            >
              删除所有
            </Button>
            <Button 
              variant={operation === 'deleteByKey' ? 'contained' : 'outlined'} 
              color="error" 
              onClick={() => handleOperationClick('deleteByKey')}
            >
              删除key
            </Button>
          </Box>
        </Paper>

        {/* 输入区域 */}
        {renderInputArea()}

        {/* 消息提示 */}
        {message && (
          <Alert 
            severity={message.includes('失败') ? 'error' : 'success'} 
            sx={{ mb: 2 }}
            onClose={() => setMessage('')}
          >
            {message}
          </Alert>
        )}

        {/* 结果显示区 */}
        <Paper elevation={3} sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            操作结果
          </Typography>
          <Box sx={{ 
            backgroundColor: '#f5f5f5', 
            p: 2, 
            borderRadius: 1,
            minHeight: '100px',
            maxHeight: '300px',
            overflowY: 'auto'
          }}>
            {Object.entries(result).length > 0 ? (
              Object.entries(result).map(([k, v]) => (
                <Box key={k} sx={{ mb: 1 }}>
                  <Typography component="span" sx={{ fontWeight: 'bold' }}>
                    {k}:
                  </Typography>
                  <Typography component="span" sx={{ ml: 1 }}>
                    {v}
                  </Typography>
                </Box>
              ))
            ) : (
              <Typography color="text.secondary">
                暂无数据
              </Typography>
            )}
          </Box>
        </Paper>
      </Box>
    </Container>
  )
}

export default App
