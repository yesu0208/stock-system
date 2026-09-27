import ModalV2 from '../../components/ModalV2'
import './HolidayModal.css'

interface Props {
    open: boolean
    onClose: () => void
}

export default function HolidayModal({ open, onClose }: Props) {
    return (
        <ModalV2 open={open} title="휴무일 지정" onClose={onClose}>
            <div className="holiday-modal__content">
            </div>
        </ModalV2>
    )
}
