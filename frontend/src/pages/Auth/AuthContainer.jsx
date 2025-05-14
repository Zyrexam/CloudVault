import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../../contexts/AuthContext"
import "./AuthContainer.css"

const AuthContainer = () => {
  const [isActive, setIsActive] = useState(false)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const { signup, login } = useAuth()
  const navigate = useNavigate()

  const handleToggle = () => {
    setIsActive(!isActive)
    setError("")
  }

  const handleSignUp = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const name = e.currentTarget.elements[0].value;
    const email = e.currentTarget.elements[1].value;
    const password = e.currentTarget.elements[2].value;

    try {
      await signup(email, password, name);
      navigate('/dashboard');
    } catch (error) {
      console.error("Error registering user:", error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSignIn = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const email = e.currentTarget.elements[0].value;
    const password = e.currentTarget.elements[1].value;

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (error) {
      console.error("Error signing in:", error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`auth-container ${isActive ? "active" : ""}`}>
      {/* Sign-In Form */}
      <div className={`signin-form ${isActive ? "active" : ""}`}>
        <form onSubmit={handleSignIn}>
          <h1>Sign In</h1>
          {error && <div className="error-message">{error}</div>}
          <div className="social-container">
            <a href="https://www.facebook.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-facebook-f"></i>
            </a>
            <a href="https://www.google.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-google"></i>
            </a>
            <a href="https://www.github.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-github"></i>
            </a>
          </div>
          <span className="form-subtitle">or use your email for login</span>
          <input type="email" placeholder="Email" required />
          <input type="password" placeholder="Password" required />
          <a href="#" className="forgot-password">
            Forget Your Password?
          </a>
          <button type="submit" className="primary-button" disabled={loading}>
            {loading ? "Signing In..." : "Sign In"}
          </button>
        </form>
      </div>

      {/* Sign-Up Form */}
      <div className={`signup-form ${isActive ? "active" : ""}`}>
        <form onSubmit={handleSignUp}>
          <h1>Create Account</h1>
          {error && <div className="error-message">{error}</div>}
          <div className="social-container">
            <a href="https://www.facebook.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-facebook-f"></i>
            </a>
            <a href="https://www.google.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-google"></i>
            </a>
            <a href="https://www.github.com" target="_blank" rel="noopener noreferrer" className="social-icon">
              <i className="fa-brands fa-github"></i>
            </a>
          </div>
          <span className="form-subtitle">or use your email for registration</span>
          <input type="text" placeholder="Name" required />
          <input type="email" placeholder="Email" required />
          <input type="password" placeholder="Password" required minLength="6" />
          <button type="submit" className="primary-button" disabled={loading}>
            {loading ? "Signing Up..." : "Sign Up"}
          </button>
        </form>
      </div>

      {/* Toggle Panel */}
      <div className={`toggle-container ${isActive ? "active" : ""}`}>
        <div className={`toggle-panel toggle-left ${isActive ? "active" : ""}`}>
          <h1>Welcome Back!</h1>
          <p>Enter your personal details to use all of the site's features</p>
          <button className="secondary-button" onClick={handleToggle}>
            Sign In
          </button>
        </div>
        <div className={`toggle-panel toggle-right ${isActive ? "active" : ""}`}>
          <h1>Hello, Friend!</h1>
          <p>Register to access all site features</p>
          <button className="secondary-button" onClick={handleToggle}>
            Sign Up
          </button>
        </div>
      </div>
    </div>
  )
}

export default AuthContainer
