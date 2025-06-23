
# Smart Note App

Smart Note App is an integrated platform that combines a secure, Spring Boot–based backend with a modern Android client to offer a seamless note-taking experience. The app supports user registration, authentication (with CSRF protection), and full CRUD operations on notes and folders. With functionalities that include bookmarking and folder organization, Smart Note App is designed with robust security in mind and a focus on an intuitive mobile user interface.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Installation](#installation)
  - [Backend Setup](#backend-setup)
  - [Android Client Setup](#android-client-setup)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [Contact](#contact)

## Features

- **Secure Authentication:**  
  Uses Spring Security with CSRF protection along with role-based access controls for API endpoints.

- **RESTful API:**  
  CRUD endpoints for managing notes, folders, and users. Public and authenticated endpoints are clearly differentiated.

- **Cross-Platform Compatibility:**  
  The Android client communicates with the backend using Retrofit and OkHttp with support for CSRF token management.

- **User-Friendly Interface:**  
  Integrates Material Design components, RecyclerView, and ConstraintLayout for an engaging mobile experience.

- **Bookmarking & Folder Organization:**  
  Users can bookmark important notes and organize their notes into customizable folders.

- **Error Handling & Responsive UI:**  
  The Android client provides feedback using progress dialogs, proper toast messages, and network error handling for a smooth user experience.

## Technology Stack

- **Backend:**  
  - Java 11+, Spring Boot, Spring Security, Jakarta EE  
  - RESTful API design, CSRF token management, and CORS configuration

- **Android Client:**  
  - Java, AndroidX, Retrofit, OkHttp, Gson  
  - Material Design components, RecyclerView, ConstraintLayout

- **Build Tools:**  
  - Gradle, and (optionally) Maven for backend dependency management

## Installation

### Backend Setup

1. **Prerequisites:**
   - JDK 11 or later
   - Maven or Gradle (depending on your build preference)
   - An IDE (such as IntelliJ IDEA or Eclipse)

2. **Clone the Repository:**

   ```bash
   git clone https://github.com/yourusername/smartnoteapp.git
   cd smartnoteapp/EarnLearn
   ```

3. **Configure the Backend:**
   - The Spring Security configuration in `SecurityConfig.java` is pre-configured with CORS and CSRF settings.  
   - If connecting to a database, adjust your application properties or YAML file accordingly.

4. **Run the Backend Application:**

   If using Gradle:
   ```bash
   ./gradlew bootRun
   ```
   Or if using Maven:
   ```bash
   mvn spring-boot:run
   ```

   The backend should start on the default port (usually 8080) and will expose endpoints such as `/api/notes`, `/api/folders`, and `/api/users`.

### Android Client Setup

1. **Prerequisites:**
   - Android Studio (latest version recommended)
   - Android SDK with a minimum API level 24

2. **Import the Project:**

   - Open Android Studio.
   - Click on **File > Open** and navigate to the project’s Android module (e.g., the project folder containing `AndroidManifest.xml`).

3. **Configure the Client:**
   - Ensure that the `network_security_config.xml` allows cleartext traffic to the backend server (e.g., IP `192.168.195.152`). Adjust if your server domain or IP differs.
   - The `ApiClient.java` file manages CSRF tokens and uses a Java Net CookieJar to manage cookies.

4. **Build and Run:**

   - Sync the Gradle files.
   - Build your project and run it on an emulator or a connected Android device.

## API Endpoints

The backend provides a range of RESTful endpoints. Some key endpoints include:

- **Authentication & User Management:**
  - `POST /api/users/login` – Log in the user (supports both form-based login and API-based JSON responses).
  - `POST /api/users` – Create a new user (requires role designation via query parameter).

- **Notes:**
  - `GET /api/notes` – Get all notes for the authenticated user.
  - `POST /api/notes` – Create a new note.
  - `PUT /api/notes/{id}` – Update note information.
  - `DELETE /api/notes/{id}` – Delete a note.
  - `GET /api/notes/bookmarked` – Retrieve all bookmarked notes.
  - `GET /api/notes/search?keyword={keyword}` – Search notes.

- **Folders:**
  - `GET /api/folders` – Get all user folders.
  - `POST /api/folders` – Create a new folder.
  - `DELETE /api/folders/{id}` – Delete a folder.

> **Note:** All state-changing operations (POST, PUT, DELETE) require a valid CSRF token. For the Android client, the token is automatically added via the interceptor.

## Configuration

- **CORS & CSRF:**  
  The `SecurityConfig.java` file configures CORS (with allowed origins including `http://localhost:8080` and others) and handles CSRF tokens through cookies. Endpoints under `/api/public/**` are exempt from CSRF protection.

- **Retrofit & OkHttp:**  
  In the Android client, `ApiClient.java` sets up an OkHttp client with logging, cookie management, and an interceptor that adds custom headers (such as `"User-Agent": "smartnote-android"`) to every request.

- **Build Configuration:**  
  The project uses Gradle for both backend and Android modules. See the provided `build.gradle` files for dependencies and compile configurations.

## Contributing

Contributions are welcome! If you’d like to add features, fix bugs, or improve documentation:

1. Fork this repository.
2. Create a new branch for your changes.
3. Commit your changes with clear commit messages.
4. Push your branch and open a pull request.

Please adhere to our coding standards and include detailed descriptions for any new functionality.

## Contact

For any questions, suggestions, or feedback, please feel free to reach out:
- **GitHub:** [@ritesh3101999](https://github.com/ritesh3101999)

---
