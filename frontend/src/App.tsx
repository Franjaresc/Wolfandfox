import { NavLink, Navigate, Route, Routes } from 'react-router';
import { RouteClosureFormPage } from './features/route-closures/RouteClosureFormPage';
import { RouteClosureListPage } from './features/route-closures/RouteClosureListPage';
import { messages } from './messages';

export function App() {
  return (
    <>
      <header className="app-header">
        <h1>{messages.app.title}</h1>
        <nav aria-label={messages.app.navigation}>
          <NavLink to="/closures" end>
            {messages.app.closures}
          </NavLink>
          <NavLink to="/closures/new">{messages.app.closeRoute}</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/closures" replace />} />
          <Route path="/closures" element={<RouteClosureListPage />} />
          <Route path="/closures/new" element={<RouteClosureFormPage />} />
        </Routes>
      </main>
    </>
  );
}
