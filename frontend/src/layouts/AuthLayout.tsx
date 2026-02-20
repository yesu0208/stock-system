import type { ReactNode } from 'react'

interface Props {
    children: ReactNode
}

export default function AuthLayout({ children }: Props) {
    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <h1 style={styles.title}>모의투자 서비스</h1>
            </header>

            <main style={styles.main}>{children}</main>

            <footer style={styles.footer}>
                <p>© 2026 Arile. All rights reserved.</p>
            </footer>
        </div>
    )
}

const styles = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column' as const,
        backgroundColor: '#121212',
        color: '#FFFFFF',
    },
    header: {
        padding: '20px',
        textAlign: 'center' as const,
        borderBottom: '1px solid #333',
    },
    title: {
        margin: 0,
        fontSize: '24px',
    },
    main: {
        flex: 1,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
    },
    footer: {
        padding: '12px',
        textAlign: 'center' as const,
        borderTop: '1px solid #333',
        fontSize: '12px',
        color: '#AAAAAA',
    },
}
