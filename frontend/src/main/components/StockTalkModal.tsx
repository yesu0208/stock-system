import { useEffect, useRef, useState } from 'react'
import Modal from '../../components/Modal'
import { useStockTalk } from '../hooks/useStockTalk'

interface Props {
    show: boolean
    onClose: () => void
    stockCode: string
    stockName: string
}

export default function StockTalkModal({ show, onClose, stockCode, stockName }: Props) {
    const { messages, participantCount, sendMessage } = useStockTalk(stockCode)
    const [input, setInput] = useState('')
    const listRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        listRef.current?.scrollTo({ top: listRef.current.scrollHeight })
    }, [messages])

    if (!show) return null

    const handleSend = () => {
        sendMessage(input)
        setInput('')
    }

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '360px', height: '480px', display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <h3 style={{ margin: 0 }}>{stockName} 종목톡</h3>
                    <span style={{ fontSize: '12px', color: '#888' }}>참여 {participantCount}명</span>
                </div>

                <div ref={listRef} style={{ flex: 1, overflowY: 'auto', border: '1px solid #262626', borderRadius: '6px', padding: '8px', marginBottom: '8px' }}>
                    {messages.map((m, i) => (
                        <div key={i} style={m.type === 'CHAT' ? styles.chatRow : styles.systemRow}>
                            {m.type === 'CHAT' ? (
                                <>
                                    <span style={{ color: '#4F9DFF', fontSize: '12px' }}>{m.senderNickname}</span>
                                    <div style={{ fontSize: '13px' }}>{m.content}</div>
                                </>
                            ) : (
                                <span style={{ fontSize: '11px', color: '#666' }}>{m.content}</span>
                            )}
                        </div>
                    ))}
                </div>

                <div style={{ display: 'flex', gap: '6px' }}>
                    <input
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSend()}
                        placeholder="메시지를 입력하세요"
                        maxLength={300}
                        style={styles.input}
                    />
                    <button onClick={handleSend} style={styles.sendButton}>전송</button>
                </div>
            </div>
        </Modal>
    )
}

const styles = {
    chatRow: { marginBottom: '8px' },
    systemRow: { textAlign: 'center' as const, marginBottom: '6px' },
    input: { flex: 1, padding: '8px', fontSize: '13px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF' },
    sendButton: { padding: '8px 14px', fontSize: '13px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
} as const
