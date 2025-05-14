import { useState, useEffect } from 'react';
import './ThemeToggle.css';

const ThemeToggle = () => {
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const theme = localStorage.getItem('theme') || 'light';
    setIsDark(theme === 'dark');
    document.documentElement.setAttribute('data-theme', theme);
  }, []);

  const toggleTheme = () => {
    const newTheme = isDark ? 'light' : 'dark';
    setIsDark(!isDark);
    localStorage.setItem('theme', newTheme);
    document.documentElement.setAttribute('data-theme', newTheme);
  };

  return (
    <button 
      className="theme-toggle" 
      onClick={toggleTheme}
      aria-label="Toggle theme"
    >
      {isDark ? (
        <span className="theme-toggle-icon">☀️</span>
      ) : (
        <span className="theme-toggle-icon">🌙</span>
      )}
    </button>
  );
};

export default ThemeToggle;