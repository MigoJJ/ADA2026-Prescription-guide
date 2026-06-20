package com.t2drx.model;

public class PatientData {
    // Demographics
    private int age = 50;
    private String sex = "Male";
    private double bmi = 28.0;

    // Comorbidities
    private boolean hasASCVD = false;
    private boolean hasHighCVDRisk = false;
    private boolean hasHeartFailure = false;
    private boolean isHFrEF = false;
    private boolean isHFpEF = false;
    private boolean hasCKD = false;

    // CKD Details
    private double eGFR = 90.0; // mL/min/1.73m²
    private double uacr = 10.0; // mg/g (Urine Albumin to Creatinine Ratio)

    // Metabolic Goals
    private double currentHbA1c = 8.5;
    private double targetHbA1c = 7.0;
    private boolean weightManagementGoal = false;

    // Liver
    private boolean hasMASLDRisk = false;

    // Current Medications
    private boolean onMetformin = false;
    private boolean onSGLT2i = false;
    private boolean onGLP1 = false;
    private boolean onGIPGLP1 = false;
    private boolean onDPP4 = false;
    private boolean onSU = false;
    private boolean onInsulin = false;
    private boolean onTZD = false;

    // Barriers & Preferences
    private boolean hasHypoglycemiaRisk = false;
    private boolean hasCostConcern = false;
    private boolean prefersOral = false;

    // Constructor
    public PatientData() {}

    // Getters and Setters
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public boolean hasASCVD() { return hasASCVD; }
    public void setHasASCVD(boolean hasASCVD) { this.hasASCVD = hasASCVD; }

    public boolean hasHighCVDRisk() { return hasHighCVDRisk; }
    public void setHasHighCVDRisk(boolean hasHighCVDRisk) { this.hasHighCVDRisk = hasHighCVDRisk; }

    public boolean hasHeartFailure() { return hasHeartFailure; }
    public void setHasHeartFailure(boolean hasHeartFailure) { this.hasHeartFailure = hasHeartFailure; }

    public boolean isHFrEF() { return isHFrEF; }
    public void setHFrEF(boolean HFrEF) { isHFrEF = HFrEF; }

    public boolean isHFpEF() { return isHFpEF; }
    public void setHFpEF(boolean HFpEF) { isHFpEF = HFpEF; }

    public boolean hasCKD() { return hasCKD; }
    public void setHasCKD(boolean hasCKD) { this.hasCKD = hasCKD; }

    public double getEGFR() { return eGFR; }
    public void setEGFR(double eGFR) { this.eGFR = eGFR; }

    public double getUacr() { return uacr; }
    public void setUacr(double uacr) { this.uacr = uacr; }

    public double getCurrentHbA1c() { return currentHbA1c; }
    public void setCurrentHbA1c(double currentHbA1c) { this.currentHbA1c = currentHbA1c; }

    public double getTargetHbA1c() { return targetHbA1c; }
    public void setTargetHbA1c(double targetHbA1c) { this.targetHbA1c = targetHbA1c; }

    public boolean wantsWeightManagement() { return weightManagementGoal; }
    public void setWeightManagementGoal(boolean weightManagementGoal) { this.weightManagementGoal = weightManagementGoal; }

    public boolean hasMASLDRisk() { return hasMASLDRisk; }
    public void setHasMASLDRisk(boolean hasMASLDRisk) { this.hasMASLDRisk = hasMASLDRisk; }

    public boolean isOnMetformin() { return onMetformin; }
    public void setOnMetformin(boolean onMetformin) { this.onMetformin = onMetformin; }

    public boolean isOnSGLT2i() { return onSGLT2i; }
    public void setOnSGLT2i(boolean onSGLT2i) { this.onSGLT2i = onSGLT2i; }

    public boolean isOnGLP1() { return onGLP1; }
    public void setOnGLP1(boolean onGLP1) { this.onGLP1 = onGLP1; }

    public boolean isOnGIPGLP1() { return onGIPGLP1; }
    public void setOnGIPGLP1(boolean onGIPGLP1) { this.onGIPGLP1 = onGIPGLP1; }

    public boolean isOnDPP4() { return onDPP4; }
    public void setOnDPP4(boolean onDPP4) { this.onDPP4 = onDPP4; }

    public boolean isOnSU() { return onSU; }
    public void setOnSU(boolean onSU) { this.onSU = onSU; }

    public boolean isOnInsulin() { return onInsulin; }
    public void setOnInsulin(boolean onInsulin) { this.onInsulin = onInsulin; }

    public boolean isOnTZD() { return onTZD; }
    public void setOnTZD(boolean onTZD) { this.onTZD = onTZD; }

    public boolean hasHypoglycemiaRisk() { return hasHypoglycemiaRisk; }
    public void setHasHypoglycemiaRisk(boolean hasHypoglycemiaRisk) { this.hasHypoglycemiaRisk = hasHypoglycemiaRisk; }

    public boolean hasCostConcern() { return hasCostConcern; }
    public void setHasCostConcern(boolean hasCostConcern) { this.hasCostConcern = hasCostConcern; }

    public boolean prefersOral() { return prefersOral; }
    public void setPrefersOral(boolean prefersOral) { this.prefersOral = prefersOral; }
}
