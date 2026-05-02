<h1 align="center">SewaTrack</h1>

**SewaTrack** is a modern, AI-powered civic engagement platform designed to bridge the gap between citizens and city administration. It empowers users to report civic issues like potholes, garbage, or faulty streetlights, and allows administrators to manage these complaints efficiently through a streamlined workflow.

## 🚀 Key Features

### 👤 For Citizens
- **Smart Reporting:** Report civic issues with photos and descriptions.
- **AI-Powered Analysis:** Automatically categorizes issues and analyzes descriptions using Gemini AI.
- **Real-time Tracking:** Follow the status of your complaints from "Submitted" to "Resolved."
- **Rating System:** Provide feedback and ratings on resolved issues to ensure service quality.
- **Notifications:** Receive instant updates when an employee is assigned or an issue is fixed.

### 👷 For Employees
- **Task Management:** View assigned civic tasks in a dedicated dashboard.
- **Progress Updates:** Update task status to "In Progress" or "Resolved."
- **Resolution Notes:** Add notes and completion details for the records.
- **Admin Chat:** Communicate directly with administrators for guidance or help.

### 🛡️ For Administrators
- **Comprehensive Dashboard:** High-level overview of city-wide issues, budget utilization, and resolution rates.
- **Employee Assignment:** Assign specific tasks to field employees based on the issue type.
- **Budgeting:** Estimate and set costs for civic projects.
- **Employee Management:** Monitor employee performance and task load.
- **Direct Messaging:** Multi-way chat system between Admin, Citizens, and Employees.

---

## 🛠️ Tech Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Kotlin)
- **Backend:** [Firebase](https://firebase.google.com/)
    - **Authentication:** Email/Password & Google Sign-in.
    - **Cloud Firestore:** Real-time NoSQL database.
    - **Cloud Storage:** Secure image hosting for reports.
- **AI/ML:**
    - **Google Gemini AI:** For intelligent description analysis.
    - **ML Kit:** Image labeling for automated issue categorization.
- **Local Storage:** [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) for app settings and user preferences.
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/) for optimized image fetching and caching.
- **Architecture:** Clean Architecture with Repository Pattern and Coroutines.

---

## 🏗️ Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- A Firebase Project.
- A Google AI (Gemini) API Key.

### Setup Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/anurag-yv/SewaTrack.git
   ```
2. **Firebase Setup:**
   - Add your `google-services.json` to the `app/` directory.
   - Enable Auth, Firestore, and Storage in your Firebase console.
3. **API Keys:**
   - Add your Gemini API key in the appropriate configuration file (e.g., `local.properties` or a Constants file).
4. **Build & Run:**
   - Sync project with Gradle files and run on an emulator or physical device.

---

## 🌓 App Preview
- **Modern UI:** Clean, Material 3 design.
- **Dark Mode:** Full support for system-wide dark/light themes.
- **Performance Optimized:** Heavy operations like image processing and Base64 decoding are handled in background threads to ensure 60FPS UI performance.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

<p align="center">
  <strong>SewaTrack</strong> — <em>Connect. Report. Improve.</em>
</p>
