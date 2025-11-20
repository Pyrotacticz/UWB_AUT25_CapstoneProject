# Realistic Illumination in Augmented Reality (AR)

This repository contains two projects housed in a single directory. Each
project is independent and includes its own source code and usage instructions.

Note: This is the capstone project for UWB MSCSSE program. This project
is entired used for educational purposes.

NOTE FOR STUDENTS:
MAKE SURE TO KEEP UP WITH YOUR PROFESSOR. DO IT LIKE YOU ARE DOING A PROJECT BY YOURSELF
EXCEPT EVERY WEEK YOU NEED TO UPDATE YOUR PROFESSOR WITH ANY STATUS CHANGES OR UPDATES.
GOOD LUCK. I BELIEVE IN YOU.

NOTE FOR USAGE:
I think these repos can be copied to the respective locations and opened as new projects
in Unity and Android. Unity may require the download of the Vuforia framework and Android
requires the OpenCV library.

------------------------------------------------------------------------

## 📁 Repository Structure

    root/
    │── MainARDevice/
    │   ├── Vuforia2022.3.62/
    │   └── ...
    │
    │── SamplingDevice/
    │   ├── app/
    │   ├── gradle/
    │   ├── openCV/
    │   └── ...
    │
    └── README.md  <-- (this file)

------------------------------------------------------------------------

# 📦 Main AR Device

The Main AR Device handles the integration of the AR object into the
real-world environment and sampling the localized area of which the AR object
is to be placed in.

## ✨ Features

-   Detect Planes
-   Place AR Objects
-   Integrate Realistic Illumination to AR Objects.

## 🧩 Configuration

    Frameworks and Versions used:
        Unity 2022.3.62 for Android
        Vuforia Engine 11.1.3

    Models and Files used:
        Coffee Cup PBR by AK STUDIO ART
        Corkboard & Desk by Cloube Works
        Detailed ballpoint pens by Simon Serge Pasi
        Household props by Maximalist
        Realistic Mobile Car #26 by Surdov Vadym
        Shadow Receiver URP by Dev Dunk Studio
        White Porcelain Dish Set Demo by Stefan
        Wooden english antique chair by diapant
        Wooden Robots SP by DJINGAREY

------------------------------------------------------------------------

# 📦 Sampling Device

The Sampling Device handles sampling the real-world environment capturing
detected bright spots and communicating that information to the
Main AR Device.

## ✨ Features

-   Real-time image capture
-   Light source estimation processing
-   Data communication to Main AR Device

## 🧩 Configuration

    Applications used:
        Android SDK
        OpenCV Library

------------------------------------------------------------------------

# 🤝 Contributing

1.  Fork the repo
2.  Create a feature branch
3.  Commit your changes
4.  Open a pull request

------------------------------------------------------------------------

# 🗺 Roadmap

-   [ ] Improve documentation
-   [ ] Improve functionality
-   [ ] Help other students

------------------------------------------------------------------------

# 📄 License

Distributed under the MIT License.
See `LICENSE` for more information.

------------------------------------------------------------------------

# ❤️ Acknowledgements

-   Tools, frameworks, or libraries used
-   Contributors or inspiration sources
