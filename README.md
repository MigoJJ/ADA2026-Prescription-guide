claude --resume 2596350f-592a-4118-ac30-57821a200c37

# ADA 2026 Type 2 Diabetes Prescription Advisor

A modern, clinical decision support system (CDSS) developed as a JavaFX desktop application on Ubuntu. It implements the latest **ADA 2026 pharmacological treatment guidelines for Type 2 Diabetes**, prioritizing cardiovascular/renal organ protection alongside metabolic (glycemia and weight) optimization.

## Key Features

- **Double-Column Decision Flow:** Fully evaluates both **Column A (Cardiovascular & Kidney Risk)** and **Column B (Metabolic/Glycemic & Weight Goals)** independently.
- **Interactive UI Forms:** Collecting comprehensive clinical factors including:
  - Patient demographics (Age, Biological Sex, BMI)
  - Cardiovascular comorbidities (ASCVD, High CV risk indicators)
  - Heart Failure sub-types (HFrEF / HFpEF with obesity)
  - Chronic Kidney Disease details (eGFR, UACR)
  - Liver Health (MASLD/MASH screening-positive risk)
  - Existing therapies & patient barriers (Hypoglycemia risk, cost concern, oral preference)
- **EHR-Style Dashboard Results:** Beautifully styled "drug cards" utilizing specialized color codes that mimic clinical guides:
  - **Red / Maroon:** Cardiovascular & Kidney Preservation
  - **Purple:** Weight Optimization Focus
  - **Green / Teal:** Glycemic Efficacy & Baseline Control
- **Interactive Warning Caveats:** Highly visible yellow-alert banners capturing critical safety checks (e.g., Metformin contraindications or dosage warnings based on eGFR levels).
- **PDF Report Exporter:** Direct generation of medical recommendation reports incorporating patient details, clinical rationale, recommended drug cards, and clinical caveats using Apache PDFBox.

---

## Architectural Layout

The application utilizes a clean, decoupled MVC (Model-View-Controller) design combined with a modern modular Java structure:

```
t2d-rx-advisor/
├── pom.xml                               # Maven Project build configuration
├── README.md                             # Project overview and run guides
├── src/main/java/
│   ├── module-info.java                  # Modular definitions & open boundaries
│   └── com/t2drx/
│       ├── App.java                      # App entry point & stage coordinator
│       ├── model/
│       │   ├── PatientData.java          # Patient clinical profile bean
│       │   └── Recommendation.java       # Recommendation payload & agent structures
│       ├── engine/
│       │   └── RecommendationEngine.java # Core ADA 2026 decision tree
│       └── controller/
│           ├── InputController.java      # Patient form handler & event bindings
│           └── ResultController.java     # EHR dashboard renderer & PDF generator
└── src/main/resources/
    ├── com/t2drx/view/
    │   ├── InputView.fxml                # Patient form markup
    │   └── ResultView.fxml               # Dashboard results panel markup
    └── styles/
        └── app.css                       # Clinical Theme and Card styles
```

---

## Technical Stack & Requirements

- **Java Development Kit (JDK):** Version 21 or higher (OpenJDK 25 in the environment)
- **Build System:** Apache Maven 3.9+
- **Primary Framework:** JavaFX 21 (Controls, Graphics, FXML)
- **Dependencies:** Apache PDFBox 3.0.1 (PDF reports), JUnit Jupiter 5.10.1 (Testing)

---

## Build, Test & Run Guide

To build and run the application from an Ubuntu terminal:

### 1. Run Unit Tests
To execute the automated JUnit 5 clinical decision tree tests:
```bash
mvn test
```

### 2. Run the Desktop Application
To compile, package, and start the JavaFX graphical interface:
```bash
mvn javafx:run
```

### 3. Build Executable Fat-JAR
To compile and assemble the application into a distribution JAR:
```bash
mvn package
```

---

## Clinical Decision Logic Highlights (ADA 2026 Updates)

1. **Independent Organ Protection:** If a patient has ASCVD, High CVD Risk, Heart Failure, or CKD, the app recommends a GLP-1 RA or SGLT2i with proven benefit, **independent of baseline HbA1c or Metformin use**.
2. **CKD Progression:** If CKD is present, an **SGLT2i** is preferred to delay renal progression down to an eGFR of 20 mL/min/1.73m². If contraindicated, a **GLP-1 RA** (e.g. Semaglutide based on the 2024/2025 FLOW trial evidence) is recommended.
3. **HFpEF + Obesity (STEP-HFpEF / SUMMIT Trial):** If a patient has HFpEF and Obesity, a **dual GIP/GLP-1 RA (Tirzepatide)** or **GLP-1 RA (Semaglutide)** is recommended specifically for symptom reduction.
4. **Metformin Renal Adjustment:** Automatic safety warning triggers when eGFR < 45 (limit Metformin to 1000mg/day) and absolute contraindication triggers when eGFR < 30 (discontinue Metformin).
5. **MASLD/MASH:** Screen-positive patients receive priority recommendations for **GLP-1 RAs, GIP/GLP-1 RAs, or Pioglitazone (TZD)** based on histological liver benefit.
