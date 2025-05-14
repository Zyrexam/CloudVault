import './About.css';

const About = () => {
  return (
    <div className="about-page">
      <section className="about-hero">
        <div className="about-hero-content">
          <h1>Welcome to CloudVault</h1>
          <p>Your Personal Cloud Storage Solution</p>
          <div className="hero-stats">
            <div className="stat-item">
              <span className="stat-number">99.9%</span>
              <span className="stat-label">Uptime</span>
            </div>
            <div className="stat-item">
              <span className="stat-number">256-bit</span>
              <span className="stat-label">Encryption</span>
            </div>
            <div className="stat-item">
              <span className="stat-number">24/7</span>
              <span className="stat-label">Support</span>
            </div>
          </div>
        </div>
      </section>

      <section className="about-content">
        <div className="about-section">
          <h2>Our Story</h2>
          <p>
            Founded with a vision to revolutionize cloud storage, CloudVault emerged from the need 
            for a more secure, user-friendly, and efficient way to store and share digital content. 
            We understand the importance of your data and are committed to providing the best-in-class 
            storage solution that you can trust.
          </p>
        </div>

        <div className="about-section features-section">
          <h2>Why Choose CloudVault?</h2>
          <div className="features-list">
            <div className="feature-item">
              <h3>🔒 Enterprise-Grade Security</h3>
              <p>
                Benefit from military-grade encryption, two-factor authentication, and 
                advanced threat protection. Your files are encrypted both in transit and at rest.
              </p>
            </div>
            <div className="feature-item">
              <h3>🌐 Global Accessibility</h3>
              <p>
                Access your files instantly from any device, anywhere in the world. 
                Our global CDN ensures lightning-fast access to your content.
              </p>
            </div>
            <div className="feature-item">
              <h3>🤝 Smart Collaboration</h3>
              <p>
                Share files and folders with customizable permissions. Track file 
                versions, comments, and changes in real-time.
              </p>
            </div>
            <div className="feature-item">
              <h3>⚡ Optimized Performance</h3>
              <p>
                Experience blazing-fast uploads and downloads with our intelligent 
                file chunking and parallel processing technology.
              </p>
            </div>
          </div>
        </div>

        <div className="about-section tech-section">
          <h2>Powered by Modern Technology</h2>
          <div className="tech-stack">
            <div className="tech-item">
              <h3>Modern Frontend</h3>
              <ul>
                <li>React.js for Dynamic UI</li>
                <li>Modern CSS Architecture</li>
                <li>Progressive Web App</li>
                <li>Responsive Design</li>
              </ul>
            </div>
            <div className="tech-item">
              <h3>Robust Backend</h3>
              <ul>
                <li>Node.js Microservices</li>
                <li>RESTful & GraphQL APIs</li>
                <li>JWT Authentication</li>
                <li>Real-time Updates</li>
              </ul>
            </div>
            <div className="tech-item">
              <h3>Cloud Infrastructure</h3>
              <ul>
                <li>Multi-region Storage</li>
                <li>Global CDN Network</li>
                <li>Auto-scaling System</li>
                <li>99.9% Uptime SLA</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="about-section commitment-section">
          <h2>Our Commitment</h2>
          <div className="commitments">
            <div className="commitment-item">
              <h3>🛡️ Security First</h3>
              <p>Your data security is our top priority. We employ industry-leading security measures.</p>
            </div>
            <div className="commitment-item">
              <h3>💡 Innovation</h3>
              <p>Constantly evolving our technology to provide cutting-edge solutions.</p>
            </div>
            <div className="commitment-item">
              <h3>🤝 Customer Success</h3>
              <p>Dedicated support team to ensure your success with CloudVault.</p>
            </div>
          </div>
        </div>

        <div className="about-section cta-section">
          <h2>Ready to Get Started?</h2>
          <p>
            Join thousands of satisfied users who trust CloudVault with their valuable data. 
            Experience the future of cloud storage today.
          </p>
          <div className="cta-buttons">
            <a href="/signup" className="btn-primary">Start Free Trial</a>
            <a href="/contact" className="btn-secondary">Talk to Sales</a>
          </div>
        </div>
      </section>
    </div>
  );
};

export default About; 