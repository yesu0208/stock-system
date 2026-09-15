import ModalV2 from "../../../components/ModalV2";

interface Props {
    open: boolean
    onClose: () => void
}

export default function AdvancedOrder({ open, onClose }: Props) {
    return (
        <ModalV2 open={open} title="고급 주문" onClose={onClose}>
            <div style={{ padding: 20, textAlign: 'center', color: '#8d929b', fontSize: 13 }}>
                <p>OTOCO / 트레일링스탑 주문은 준비 중입니다.</p>
            </div>
        </ModalV2>
    )
}
