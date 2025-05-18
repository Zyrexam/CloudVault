import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { auth } from '../../config/firebase';
import './Home.css';

const Home = () => {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = React.useState(false);

  React.useEffect(() => {
    // Check authentication state
    const unsubscribe = auth.onAuthStateChanged((user) => {
      if (user) {
        setIsLoggedIn(true);
      } else {
        setIsLoggedIn(false);
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  // Modify the CTA section to show different content based on auth state
  const renderCTASection = () => {
    if (isLoggedIn) {
      return (
        <div className="cta-section">
          <h2>Welcome Back!</h2>
          <p>Continue managing your files securely.</p>
          <div className="cta-buttons">
            <Link to="/dashboard" className="btn-primary">Go to Dashboard</Link>
            <Link to="/contact" className="btn-secondary">Need Help?</Link>
          </div>
        </div>
      );
    }

    return (
      <div className="cta-section">
        <h2>Ready to Get Started?</h2>
        <p>Join thousands of users who trust CloudVault with their data.</p>
        <div className="cta-buttons">
          <Link to="/auth" className="btn-primary">Sign Up Now</Link>
          <Link to="/contact" className="btn-secondary">Contact Us</Link>
        </div>
      </div>
    );
  };

  const renderHeroButtons = () => {
    if (isLoggedIn) {
      return (
        <div className="hero-buttons">
          <Link to="/dashboard" className="btn-primary">Go to Dashboard</Link>
          <Link to="/about" className="btn-secondary">Learn more →</Link>
        </div>
      );
    }else{
    return (
      <div className="hero-buttons">
        <Link to="/auth" className="btn-primary">Get Started</Link>
        <Link to="/about" className="btn-secondary">Learn more →</Link>
      </div>
    );
  }
  };


  useEffect(() => {
    const token = localStorage.getItem('token'); // Adjust this to your auth logic
    if (token) {
      navigate('/dashboard');
    }
  }, [navigate]);

  return (
    <div className="home">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-content">
          <h1>Secure Cloud Storage for Your Files</h1>
          <p>Store, share, and manage your files securely in the cloud. Access your data from anywhere, anytime.</p>
          {renderHeroButtons()}
        </div>
      </section>

      {/* Features Section */}
      <section className="features">
        <div className="features-header">
          <h2>Everything you need to store files securely</h2>
          <p>CloudVault provides a secure and reliable way to store your files in the cloud. 
             With advanced encryption and easy access, your data is always protected.</p>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">🔒</div>
            <h3>Secure Encryption</h3>
            <p>Your files are encrypted using industry-standard encryption protocols, 
               ensuring your data remains private and secure.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">🌐</div>
            <h3>Easy Access</h3>
            <p>Access your files from any device with an internet connection. 
               No complex setup required.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">🔄</div>
            <h3>File Sharing</h3>
            <p>Share files securely with others using encrypted links and 
               customizable access permissions.</p>
          </div>
        </div>

        {renderCTASection()}
      </section>
    </div>
  );
};

export default Home;