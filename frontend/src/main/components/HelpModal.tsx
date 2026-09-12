import Modal from '../../components/Modal'

interface Props {
    show: boolean
    onClose: () => void
}

const SECTIONS: { title: string; body: string }[] = [
    { title: '기본 주문', body: '종목을 선택하고 가격·수량을 입력해 매수/매도할 수 있습니다. 레버리지 배율을 선택하면 증거금만으로 더 큰 포지션을 매수할 수 있습니다.' },
    { title: '자동주문 / OTOCO / 트레일링 스탑', body: '자동주문은 감시가 도달 시 지정가로 체결됩니다. OTOCO는 진입 조건과 익절/손절을 한 번에 예약하고, 트레일링 스탑은 고점·저점 대비 일정 비율만큼 움직이면 자동으로 체결됩니다.' },
    { title: '관심종목 / 목표가 알림', body: '★ 버튼으로 관심종목에 추가하고, 🔔 알림으로 목표가 도달 시 알림을 받을 수 있습니다.' },
    { title: '토론방 / 종목톡', body: '종목별 게시판(토론방)에 글을 쓰고 댓글/좋아요를 남길 수 있습니다. 종목톡은 실시간 채팅입니다.' },
    { title: '랭크', body: '수익률에 따라 브론즈부터 마스터까지 랭크가 매겨집니다. 헤더의 닉네임 옆에서 현재 랭크를 확인할 수 있습니다.' },
]

export default function HelpModal({ show, onClose }: Props) {
    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '400px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>도움말</h3>
                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {SECTIONS.map(s => (
                        <div key={s.title} style={{ marginBottom: '14px' }}>
                            <div style={{ fontSize: '13px', fontWeight: 600, color: '#4F9DFF', marginBottom: '4px' }}>{s.title}</div>
                            <div style={{ fontSize: '13px', color: '#CCC', lineHeight: 1.6 }}>{s.body}</div>
                        </div>
                    ))}
                </div>
                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
