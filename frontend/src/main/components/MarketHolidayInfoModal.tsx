import ModalV2 from '../../components/ModalV2'
import './MarketHolidayInfoModal.css'

interface Props {
    open: boolean
    onClose: () => void
}

export default function MarketHolidayInfoModal({ open, onClose }: Props) {
    return (
        <ModalV2 open={open} title="휴장 정보" onClose={onClose}>
            <div className="holiday-info-modal__content">
                {/* TODO: 휴장 정보 달력 */}
            </div>
        </ModalV2>
    )
}
