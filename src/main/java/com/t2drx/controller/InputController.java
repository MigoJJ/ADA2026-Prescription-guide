package com.t2drx.controller;

import com.t2drx.App;
import com.t2drx.engine.RecommendationEngine;
import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import com.t2drx.util.LanguageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InputController {

    // Demographics
    @FXML private Spinner<Integer> ageSpinner;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private Spinner<Double> bmiSpinner;
    @FXML private CheckBox masldCheckBox;

    // Comorbidities
    @FXML private CheckBox ascvdCheckBox;
    @FXML private CheckBox highCvdRiskCheckBox;
    @FXML private CheckBox hfCheckBox;
    @FXML private RadioButton hfrefRadio;
    @FXML private RadioButton hfpefRadio;
    @FXML private ToggleGroup hfTypeGroup;
    @FXML private CheckBox ckdCheckBox;
    @FXML private Spinner<Double> egfrSpinner;
    @FXML private Spinner<Double> uacrSpinner;

    // Metabolic Goals & Preferences
    @FXML private Spinner<Double> currentA1cSpinner;
    @FXML private Spinner<Double> targetA1cSpinner;
    @FXML private CheckBox weightGoalCheckBox;
    @FXML private CheckBox hypoRiskCheckBox;
    @FXML private CheckBox costConcernCheckBox;
    @FXML private CheckBox oralCheckBox;

    // Current Meds
    @FXML private CheckBox onMetformin;
    @FXML private CheckBox onSglt2i;
    @FXML private CheckBox onGlp1;
    @FXML private CheckBox onDpp4;
    @FXML private CheckBox onSu;
    @FXML private CheckBox onInsulin;
    @FXML private CheckBox onTzd;

    @FXML private Button evaluateButton;
    @FXML private ComboBox<String> languageComboBox;

    @FXML
    public void initialize() {
        LanguageManager lm = LanguageManager.getInstance();

        // Initialize Language ComboBox
        languageComboBox.setItems(FXCollections.observableArrayList("English", "한국어"));
        languageComboBox.setValue("English");
        languageComboBox.setOnAction(event -> handleLanguageChange());

        // Initialize Sex ComboBox
        sexComboBox.setItems(FXCollections.observableArrayList(
            lm.getString("input.male"),
            lm.getString("input.female"),
            lm.getString("input.other")
        ));
        sexComboBox.setValue(lm.getString("input.male"));

        // Initialize Spinners with appropriate ranges
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(18, 120, 65));
        bmiSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(10.0, 60.0, 29.5, 0.5));

        egfrSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(5.0, 150.0, 75.0, 5.0));
        uacrSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 5000.0, 15.0, 5.0));

        currentA1cSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(4.0, 18.0, 8.2, 0.1));
        targetA1cSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(5.5, 9.0, 7.0, 0.1));

        // Bindings for Heart Failure subtypes
        hfrefRadio.disableProperty().bind(hfCheckBox.selectedProperty().not());
        hfpefRadio.disableProperty().bind(hfCheckBox.selectedProperty().not());

        hfCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                hfrefRadio.setSelected(false);
                hfpefRadio.setSelected(false);
            } else {
                hfrefRadio.setSelected(true);
            }
        });

        // Trigger action
        evaluateButton.setOnAction(event -> handleEvaluation());

        updateUiLabels();
    }

    private void handleLanguageChange() {
        String selected = languageComboBox.getValue();
        if ("한국어".equals(selected)) {
            LanguageManager.getInstance().setKorean();
        } else {
            LanguageManager.getInstance().setEnglish();
        }
        updateUiLabels();
    }

    private void updateUiLabels() {
        LanguageManager lm = LanguageManager.getInstance();

        evaluateButton.setText(lm.getString("input.evaluate"));

        // Update sex combo box values
        String currentSex = sexComboBox.getValue();
        sexComboBox.setItems(FXCollections.observableArrayList(
            lm.getString("input.male"),
            lm.getString("input.female"),
            lm.getString("input.other")
        ));

        // Restore previous selection or default
        if (currentSex != null && sexComboBox.getItems().contains(currentSex)) {
            sexComboBox.setValue(currentSex);
        } else {
            sexComboBox.setValue(lm.getString("input.male"));
        }
    }

    private void handleEvaluation() {
        PatientData patient = new PatientData();
        
        // Demographic data
        patient.setAge(ageSpinner.getValue());
        patient.setSex(sexComboBox.getValue());
        patient.setBmi(bmiSpinner.getValue());
        patient.setHasMASLDRisk(masldCheckBox.isSelected());

        // Comorbidities
        patient.setHasASCVD(ascvdCheckBox.isSelected());
        patient.setHasHighCVDRisk(highCvdRiskCheckBox.isSelected());
        patient.setHasHeartFailure(hfCheckBox.isSelected());
        if (hfCheckBox.isSelected()) {
            patient.setHFrEF(hfrefRadio.isSelected());
            patient.setHFpEF(hfpefRadio.isSelected());
        }
        patient.setHasCKD(ckdCheckBox.isSelected());
        patient.setEGFR(egfrSpinner.getValue());
        patient.setUacr(uacrSpinner.getValue());

        // Metabolic Goals & Barriers
        patient.setCurrentHbA1c(currentA1cSpinner.getValue());
        patient.setTargetHbA1c(targetA1cSpinner.getValue());
        patient.setWeightManagementGoal(weightGoalCheckBox.isSelected());
        patient.setHasHypoglycemiaRisk(hypoRiskCheckBox.isSelected());
        patient.setHasCostConcern(costConcernCheckBox.isSelected());
        patient.setPrefersOral(oralCheckBox.isSelected());

        // Current Meds
        patient.setOnMetformin(onMetformin.isSelected());
        patient.setOnSGLT2i(onSglt2i.isSelected());
        patient.setOnGLP1(onGlp1.isSelected());
        patient.setOnDPP4(onDpp4.isSelected());
        patient.setOnSU(onSu.isSelected());
        patient.setOnInsulin(onInsulin.isSelected());
        patient.setOnTZD(onTzd.isSelected());

        // Evaluate via engine
        RecommendationEngine engine = new RecommendationEngine();
        Recommendation rec = engine.evaluate(patient);

        // Transition to results screen
        App.getInstance().showResultView(patient, rec);
    }
}
