import "./LoginBackground.css";

const PARTICLES = [
    { left: 3, duration: 16, delay: -5 },
    { left: 6, duration: 14, delay: 0 },
    { left: 10, duration: 19, delay: -10 },
    { left: 15, duration: 18, delay: -3 },
    { left: 19, duration: 13, delay: -8 },
    { left: 24, duration: 12, delay: -7 },
    { left: 28, duration: 20, delay: -12 },
    { left: 33, duration: 20, delay: -1 },
    { left: 38, duration: 15, delay: -6 },
    { left: 44, duration: 15, delay: -9 },
    { left: 49, duration: 18, delay: -2 },
    { left: 55, duration: 17, delay: -4 },
    { left: 60, duration: 14, delay: -13 },
    { left: 66, duration: 13, delay: -11 },
    { left: 70, duration: 21, delay: -3 },
    { left: 74, duration: 19, delay: -6 },
    { left: 78, duration: 16, delay: -9 },
    { left: 83, duration: 16, delay: -2 },
    { left: 88, duration: 12, delay: -7 },
    { left: 92, duration: 21, delay: -8 },
    { left: 96, duration: 17, delay: -1 },
];

export default function LoginBackground() {
    return (
        <div className="login-bg" aria-hidden="true">
            <div className="login-bg__grid" />

            <div className="login-bg__particles">
                {PARTICLES.map((p, i) => (
                    <span
                        key={i}
                        className="login-bg__particle"
                        style={{
                            left: `${p.left}%`,
                            animationDuration: `${p.duration}s`,
                            animationDelay: `${p.delay}s`,
                        }}
                    />
                ))}
            </div>
        </div>
    );
}
