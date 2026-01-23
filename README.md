# 🎓 Graduation Project: Learning Journey System

## 🚀 Access the Application

**URL:** [http://152.42.184.216.nip.io:3000](http://152.42.184.216.nip.io:3000)

---

## ⚠️ Important Demo Instructions

To ensure a smooth testing experience, please follow these guidelines regarding authentication.

### 1. Do NOT use "Sign in with Google" (OAuth2)

* **Status:** Disabled for this Demo Environment.
* **Reason:** Google's strict OAuth 2.0 security policies do not allow production redirects to raw IP addresses or dynamic DNS services (like `nip.io`). A purchased Top-Level Domain (TLD) with SSL (HTTPS) is required for this feature to function in a live environment.

### 2. Do NOT use "Sign Up" (Create New Account)

* **Status:** Disabled for this Demo Environment.
* **Reason:** The system requires email verification to activate new accounts. Currently, the Cloud Provider (DigitalOcean) blocks outbound SMTP traffic (Ports 587/465) on new instances to prevent spam. Therefore, verification emails cannot be delivered to new users at this time.

---

## 🔑 Demo Account Credentials

Please use the following **pre-verified administrator account** to access and evaluate all system features (Project Management, Calendar, Real-time Collaboration).

| Field | Value |
| --- | --- |
| **Email** | `tranhung174303@gmail.com` |
| **Password** | `Sktt1faker@` |

---

### ✅ Recommended Testing Flow

1. Navigate to the URL provided above.
2. Enter the **Demo Account Credentials** in the login form.
3. Click **"Sign In"**.
4. You will be redirected to the main dashboard to begin your review.

---

### 💡 Tip for your Presentation:

If a teacher asks *why* these features are disabled, you can confidently answer:

> *"The code for OAuth and Email works perfectly in the local environment. These are infrastructure restrictions imposed by Google and DigitalOcean for security on public IPs, which is standard for non-enterprise deployments."*