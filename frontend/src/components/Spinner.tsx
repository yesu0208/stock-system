import "./Spinner.css";

interface Props {
    /** 스피너 지름(px) */
    size?: number;
    /** 테두리 두께(px) */
    thickness?: number;
    /** 부모 영역 가운데 정렬 래퍼를 씌울지 여부 */
    center?: boolean;
    /** center=true 일 때 래퍼의 최소 높이(px) */
    minHeight?: number;
}

export default function Spinner({
                                    size = 24,
                                    thickness = 3,
                                    center = true,
                                    minHeight,
                                }: Props) {
    const spinner = (
        <span
            className="spinner"
            role="status"
            aria-label="불러오는 중"
            style={{
                width: size,
                height: size,
                borderWidth: thickness,
            }}
        />
    );

    if (!center) return spinner;

    return (
        <div
            className="spinner-center"
            style={minHeight !== undefined ? { minHeight } : undefined}
        >
            {spinner}
        </div>
    );
}
