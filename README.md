# SentryOne Emergency SOS 🚨 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE) [![GitHub release (latest by date)](https://img.shields.io/github/v/release/yourusername/sentry-one)](https://github.com/yourusername/sentry-one/releases)

**Your personal safety companion - Instant emergency alerts with one tap or shake.**

SentryOne is an Android app designed to provide immediate emergency assistance with GPS location sharing. With features like one-tap SOS, shake detection, and automated SMS alerts to your emergency contacts, help is always just moments away.

**Home Screen**  
<img width="300" alt="Home Fragment" src="https://github.com/user-attachments/assets/9fa27131-ebea-4a64-b3a3-036bd02d3a2a" />
<img width="300" alt="Home Fragment" src="https://github.com/user-attachments/assets/9ef74f7d-858c-473a-b969-f49517cc98a0" />


**Contacts Management**  
<img width="300" alt="Contacts Fragment" src="https://github.com/user-attachments/assets/5417a60b-7b4a-4e36-8c2d-ec49313b1e82" />
<img width="300" alt="Contacts Fragment" src="https://github.com/user-attachments/assets/785535b8-8300-4e10-a428-bf41a0331e55" />

**Alert History **  
<img width="300" alt="History Fragment" src="https://github.com/user-attachments/assets/4b77f45a-0053-4ec2-83b4-bebdc919b94b" />
<img width="300" alt="History Fragment" src="https://github.com/user-attachments/assets/863cde7f-26d3-4e0e-aca8-f2406166c67b" />


**Settings Fragment**  
<img width="300" alt="Settings Fragment" src="https://github.com/user-attachments/assets/d5dd99ea-dbac-498c-bcba-22cc31b2e7bc" />
<img width="300" alt="Settings Fragment" src="https://github.com/user-attachments/assets/ec852cf9-15cd-4584-a296-a93100214759" />


## 🚀 Key Features

* **🆘 One-Tap SOS:** Immediate emergency alert with current location
* **👋 Shake Detection:** Trigger alerts by shaking your device (uses accelerometer)
* **📌 Location Tracking:** Real-time latitude/longitude display
* **📞 Smart Contacts:** 
  - Auto-complete contact selection
  - Swipe-to-delete functionality
  - Room database storage
* **🕒 Alert History:** 
  - Complete log of all triggered alerts
  - Timestamps with location data
* **⚙️ Customizable Settings:**
  - Dark/Light mode toggle
  - Haptic feedback on triggers
  - Configurable SMS messages
* **📱 Modern Architecture:**
  - ViewModel with ViewModelFactory
  - Coroutines for async operations
  - Bottom Navigation with 4 fragments
<!--
## 📲 Installation

1. **Download the APK:** Get the [latest release](https://github.com/jatinJK007/sentry-one/releases/latest/download/sentry-one.apk)
2. **Install on Android:** Enable "Install Unknown Sources" if needed
3. **Set Up:**
   - Grant necessary permissions (contacts, location, SMS)
   - Add emergency contacts in the Contacts tab
   - Configure settings to your preference
-->
## 🛠️ Technical Implementation

* **Architecture Components:**
  - Room Database for local storage
  - ViewModel with LiveData
  - RecyclerView with swipe gestures
* **Permissions Handling:**
  - Runtime permissions for contacts/location
  - SMS sending capability
* **Sensor Integration:**
  - Accelerometer for shake detection
  - Location services for GPS data
* **UI Features:**
  - Custom switch controls
  - Haptic feedback engine
  - Dark/light theme support

## 🔗 Resources

* [Room Database Guide](https://developer.android.com/training/data-storage/room)
* [Android Location Services](https://developer.android.com/training/location)
* [SMS Manager Documentation](https://developer.android.com/reference/android/telephony/SmsManager)
