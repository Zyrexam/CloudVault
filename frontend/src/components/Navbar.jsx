import { useState, useEffect, useRef } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { auth } from '../config/firebase';
import { signOut } from 'firebase/auth';
import './Navbar.css';

const Navbar = () => {
  const [user, setUser] = useState(null);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const profileMenuRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    // Listen for auth state changes
    const unsubscribe = auth.onAuthStateChanged((user) => {
      setUser(user);
    });

    // Cleanup subscription
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    // Close profile menu when clicking outside
    const handleClickOutside = (event) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target)) {
        setShowProfileMenu(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSignOut = async () => {
    try {
      await signOut(auth);
      navigate('/');
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-left">
          <Link to="/" className="logo">
            CloudVault
          </Link>
          <div className="nav-links">
            <NavLink to="/about" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
              About
            </NavLink>
            <NavLink to="/contact" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
              Contact
            </NavLink>
            {user && (
              <NavLink to="/dashboard" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                Dashboard
              </NavLink>
            )}
          </div>
        </div>
        <div className="navbar-right">
          {user ? (
            <div className="user-menu" ref={profileMenuRef}>
              <div 
                className="user-profile"
                onClick={() => setShowProfileMenu(!showProfileMenu)}
              >
                {user.photoURL ? (
                  <img src={user.photoURL} alt="Profile" className="profile-image" />
                ) : (
                  <div className="profile-initial">
                    {user.displayName ? user.displayName[0].toUpperCase() : 'U'}
                  </div>
                )}
                <span className="user-name">{user.displayName || 'User'}</span>
                <i className="fas fa-chevron-down"></i>
              </div>
              {showProfileMenu && (
                <div className="profile-menu">
                  <div className="profile-menu-header">
                    <div className="profile-menu-user">
                      {user.photoURL ? (
                        <img src={user.photoURL} alt="Profile" className="profile-menu-image" />
                      ) : (
                        <div className="profile-menu-initial">
                          {user.displayName ? user.displayName[0].toUpperCase() : 'U'}
                        </div>
                      )}
                      <div className="profile-menu-info">
                        <span className="profile-menu-name">{user.displayName || 'User'}</span>
                        <span className="profile-menu-email">{user.email}</span>
                      </div>
                    </div>
                  </div>
                  <div className="profile-menu-items">
                    <button className="profile-menu-item signout" onClick={handleSignOut}>
                      <i className="fas fa-sign-out-alt"></i>
                      Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/auth" className="btn-signin">Sign In</Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar; 