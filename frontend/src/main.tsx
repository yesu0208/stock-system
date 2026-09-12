import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import { ToastProvider } from './main/context/ToastContext'
import { MsgProvider } from './main/context/MsgContext'

/**
 * - react-router-dom의 BrowserRouter 제거 (더 이상 라우팅을 쓰지 않음)
 * - ToastProvider/MsgProvider는 로그인 여부와 무관하게 항상 떠 있어야
 *   하므로(로그인/회원가입 화면에서도 에러 토스트가 필요) 최상단에 배치
 * - STOMP에 의존하는 Provider들(Realtime/Account/Market/Alert/WatchList)은
 *   로그인 상태에서만 의미가 있으므로 App.tsx 내부, isLoggedIn === true
 *   분기 안에서만 마운트
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
    <ToastProvider>
        <MsgProvider>
            <App />
        </MsgProvider>
    </ToastProvider>
)
