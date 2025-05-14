import { useState } from 'react';
import './Contact.css';

const Contact = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    message: ''
  });

  const [submitStatus, setSubmitStatus] = useState({
    submitted: false,
    success: false,
    message: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    // Here you would typically send the form data to your backend
    setSubmitStatus({
      submitted: true,
      success: true,
      message: 'Thank you for your message! We will get back to you soon.'
    });
    // Reset form after successful submission
    setFormData({
      name: '',
      email: '',
      subject: '',
      message: ''
    });
  };

  return (
    <div className="contact-page">
      <section className="contact-hero">
        <div className="contact-hero-content">
          <h1>Get in Touch</h1>
          <p>Have questions? We'd love to hear from you.</p>
        </div>
      </section>

      <div className="contact-container">
        <div className="contact-info">
          <h2>Contact Information</h2>
          <div className="info-items">
            <div className="info-item">
              <span className="info-icon">📍</span>
              <div className="info-content">
                <h3>Our Location</h3>
                <p>123 Cloud Street<br />Tech Valley, CV 12345</p>
              </div>
            </div>
            <div className="info-item">
              <span className="info-icon">📧</span>
              <div className="info-content">
                <h3>Email Us</h3>
                <p>support@cloudvault.com<br />sales@cloudvault.com</p>
              </div>
            </div>
            <div className="info-item">
              <span className="info-icon">📱</span>
              <div className="info-content">
                <h3>Call Us</h3>
                <p>+1 (555) 123-4567<br />Mon - Fri, 9am - 6pm</p>
              </div>
            </div>
          </div>
        </div>

        <div className="contact-form-container">
          <h2>Send us a Message</h2>
          {submitStatus.submitted && (
            <div className={`form-message ${submitStatus.success ? 'success' : 'error'}`}>
              {submitStatus.message}
            </div>
          )}
          <form onSubmit={handleSubmit} className="contact-form">
            <div className="form-group">
              <label htmlFor="name">Name</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
                placeholder="Your name"
              />
            </div>
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                placeholder="your@email.com"
              />
            </div>
            <div className="form-group">
              <label htmlFor="subject">Subject</label>
              <input
                type="text"
                id="subject"
                name="subject"
                value={formData.subject}
                onChange={handleChange}
                required
                placeholder="How can we help?"
              />
            </div>
            <div className="form-group">
              <label htmlFor="message">Message</label>
              <textarea
                id="message"
                name="message"
                value={formData.message}
                onChange={handleChange}
                required
                placeholder="Your message here..."
                rows="5"
              />
            </div>
            <button type="submit" className="submit-button">
              Send Message
            </button>
          </form>
        </div>
      </div>

      <section className="faq-section">
        <h2>Frequently Asked Questions</h2>
        <div className="faq-grid">
          <div className="faq-item">
            <h3>How secure is CloudVault?</h3>
            <p>We use industry-standard encryption and security measures to protect your data. All files are encrypted both in transit and at rest.</p>
          </div>
          <div className="faq-item">
            <h3>What file types can I store?</h3>
            <p>CloudVault supports all file types, including documents, images, videos, and more. There are no restrictions on file formats.</p>
          </div>
          <div className="faq-item">
            <h3>Is there a file size limit?</h3>
            <p>Free accounts can upload files up to 2GB in size. Premium accounts can upload files up to 10GB.</p>
          </div>
          <div className="faq-item">
            <h3>How do I share files?</h3>
            <p>You can easily share files by generating secure links or inviting others via email. You control the access permissions.</p>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Contact; 