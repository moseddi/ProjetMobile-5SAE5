import { Outlet } from 'react-router-dom';
import AssistantWidget from '../components/AssistantWidget';
import Navbar from '../components/Navbar';
import './Layout.css';

const Layout = () => {
  return (
    <div className="layout">
      <Navbar />
      <main className="main-content">
        <Outlet />
      </main>
      <footer className="footer">
        <div className="container">
          <p>&copy; 2024 HangOut. All rights reserved.</p>
        </div>
      </footer>
      <AssistantWidget />
    </div>
  );
};

export default Layout;
