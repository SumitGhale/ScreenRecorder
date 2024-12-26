# ScreenRecorder
This Screen Recorder App is a feature-rich application built for Android that allows users to record their device's screen seamlessly. It incorporates various advanced Android concepts and provides a floating assistive touch button for quick and easy access to screen recording functionalities.

Features 🚀
Screen Recording with Media Projection API
Utilizes Android's MediaProjection API to capture screen content.

High-Quality Video Recording
Employs MediaRecorder to encode screen data into high-quality video files.

User-Friendly Assistive Touch Button
A floating assistive touch button, inspired by popular accessibility tools, makes starting, stopping, and managing recordings effortless.

Background Floating Button
Implemented using an overlay window with WindowManager and customizable LayoutParams.

Notification Integration
Displays notifications with PendingIntent actions, allowing users to control recordings directly from the notification bar.

Permission Management
Handles user permissions effectively, ensuring compliance with Android’s security rules.

Technologies and Concepts Explored 🛠️
During the development of this app, the following Android technologies and concepts were explored and implemented:

MediaProjection API: To capture screen data.
MediaRecorder: For encoding and saving recorded video data.
Notification Management: To inform users of ongoing recording and provide actionable controls via notifications.
Overlay Window: Created a floating button for quick actions, utilizing WindowManager and custom layouts.
User Permission Handling: Implemented Android's permission workflow to obtain user consent for screen recording.
PendingIntent: Integrated to perform actions like opening the app or controlling recordings from notifications.
How It Works 🖥️
Start Recording: The user grants screen recording permissions and initiates recording.
Floating Controls: The assistive touch button remains accessible in the background, providing controls to stop and save recording.
Notification Feedback: Notifications update the user about the recording status and provide actionable options.
Stop and Save: Once recording is complete, the video is saved to the device storage for playback or sharing.
