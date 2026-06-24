package com.t2drx.controller;

import com.t2drx.App;
import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import com.t2drx.model.Recommendation.RecommendedAgent;
import com.t2drx.util.LanguageManager;
import com.t2drx.util.PdfPageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class ResultController {

    @FXML private Label summaryHeaderLabel;
    @FXML private TextArea rationaleTextArea;
    @FXML private VBox caveatsContainer;
    @FXML private Label caveatsLabel;
    @FXML private FlowPane cardsFlowPane;
    
    @FXML private Button backButton;
    @FXML private Button exportPdfButton;

    private PatientData currentPatient;
    private Recommendation currentRecommendation;

    @FXML
    public void initialize() {
        LanguageManager lm = LanguageManager.getInstance();
        backButton.setText(lm.getString("result.back"));
        backButton.setOnAction(event -> App.getInstance().showInputView());
        exportPdfButton.setText(lm.getString("result.exportPdf"));
        exportPdfButton.setOnAction(event -> handlePdfExport());
    }

    public void setResults(PatientData patientData, Recommendation recommendation) {
        this.currentPatient = patientData;
        this.currentRecommendation = recommendation;

        // Set summary header
        summaryHeaderLabel.setText(recommendation.getSummary());

        // Set rationale text area (read-only, wrapping)
        rationaleTextArea.setText(recommendation.getRationale());

        // Set caveats
        String caveats = recommendation.getClinicalCaveats();
        if (caveats == null || caveats.trim().isEmpty()) {
            caveatsContainer.setVisible(false);
            caveatsContainer.setManaged(false);
        } else {
            caveatsContainer.setVisible(true);
            caveatsContainer.setManaged(true);
            caveatsLabel.setText(caveats);
        }

        // Dynamically populate cards
        cardsFlowPane.getChildren().clear();
        for (RecommendedAgent agent : recommendation.getAgents()) {
            cardsFlowPane.getChildren().add(createAgentCard(agent));
        }
    }

    private Node createAgentCard(RecommendedAgent agent) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("drug-card", agent.getUiColorClass());
        card.setPrefWidth(420);
        card.setMinWidth(380);
        card.setPadding(new Insets(16));

        // Header / Name
        Label nameLabel = new Label(agent.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);

        Label classLabel = new Label(agent.getClassName() + " | " + agent.getClinicalIndication());
        classLabel.getStyleClass().add("card-subtitle");
        classLabel.setWrapText(true);

        card.getChildren().addAll(nameLabel, classLabel, new Separator());

        // Properties Grid (using VBox for simplicity and high readability)
        VBox propBox = new VBox(4);
        propBox.getChildren().add(createPropLabel("Weight Loss Efficacy: ", agent.getWeightEfficacy()));
        propBox.getChildren().add(createPropLabel("Hypoglycemia Risk: ", agent.getHypoglycemiaRisk()));
        propBox.getChildren().add(createPropLabel("Cost Category: ", agent.getCostCategory()));
        propBox.getChildren().add(createPropLabel("Administration: ", agent.getAdministration()));

        card.getChildren().add(propBox);
        card.getChildren().add(new Separator());

        // Reasons to Choose
        Label chooseHeader = new Label("Why this drug class?");
        chooseHeader.getStyleClass().add("section-mini-header");
        Label chooseText = new Label(agent.getReasonsToChoose());
        chooseText.setWrapText(true);
        chooseText.getStyleClass().add("card-body-text");

        card.getChildren().addAll(chooseHeader, chooseText);

        // Cautions
        Label cautionHeader = new Label("Precautions & Contraindications");
        cautionHeader.getStyleClass().add("section-mini-header-caution");
        Label cautionText = new Label(agent.getCautions());
        cautionText.setWrapText(true);
        cautionText.getStyleClass().add("card-caution-text");

        card.getChildren().addAll(new Separator(), cautionHeader, cautionText);

        return card;
    }

    private Node createPropLabel(String title, String value) {
        TextFlow flow = new TextFlow();
        Label tLabel = new Label(title);
        tLabel.setStyle("-fx-font-weight: bold; -fx-fill: -color-text-default;");
        Label vLabel = new Label(value);
        flow.getChildren().addAll(tLabel, vLabel);
        return flow;
    }

    private void handlePdfExport() {
        if (currentPatient == null || currentRecommendation == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Recommendation Report");
        fileChooser.setInitialFileName("T2D_Prescription_Advisor_Report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        File file = fileChooser.showSaveDialog(backButton.getScene().getWindow());
        if (file != null) {
            try {
                generatePdf(file);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("PDF report successfully exported to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to export PDF");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void generatePdf(File file) throws IOException {
        LanguageManager lm = LanguageManager.getInstance();
        try (PDDocument doc = new PDDocument()) {
            PdfPageManager pageManager = new PdfPageManager(doc);

            // Title
            pageManager.drawText(lm.getString("pdf.title"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            pageManager.drawLine(1.5f);

            // Subtitle
            pageManager.addVerticalSpace(10);
            pageManager.drawText(lm.getString("pdf.subtitle"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            // Patient Details
            pageManager.addVerticalSpace(15);
            pageManager.drawText(lm.getString("pdf.patientProfile") + ":",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            pageManager.addVerticalSpace(5);

            pageManager.drawText(lm.getString("pdf.age") + ": " + currentPatient.getAge() + "  |  " +
                    lm.getString("pdf.sex") + ": " + currentPatient.getSex() +
                    "  |  BMI: " + currentPatient.getBmi() + " kg/m\u00b2",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            pageManager.drawText(lm.getString("pdf.labs") + ": Current HbA1c: " + currentPatient.getCurrentHbA1c() +
                    "% (Target: " + currentPatient.getTargetHbA1c() + "%)" + "  |  eGFR: " + currentPatient.getEGFR() +
                    " mL/min  |  UACR: " + currentPatient.getUacr() + " mg/g",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            String comorbidities = (currentPatient.hasASCVD() ? "ASCVD, " : "") +
                    (currentPatient.hasHighCVDRisk() ? lm.getString("clinical.ascvdRisk") + ", " : "") +
                    (currentPatient.hasHeartFailure() ? lm.getString("clinical.heartFailure") + ", " : "") +
                    (currentPatient.hasCKD() ? lm.getString("clinical.ckd") + ", " : "") +
                    (currentPatient.hasMASLDRisk() ? lm.getString("clinical.masld") : lm.getString("pdf.none"));
            pageManager.drawText(lm.getString("pdf.comorbidities") + ": " + comorbidities,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            // Recommendation Summary
            pageManager.addVerticalSpace(15);
            pageManager.drawText(lm.getString("pdf.therapeuticSummary") + ":",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            pageManager.drawText(currentRecommendation.getSummary(),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

            // Recommended Agents
            pageManager.addVerticalSpace(10);
            pageManager.drawText(lm.getString("pdf.recommendedAgents") + ":",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            pageManager.addVerticalSpace(5);

            for (RecommendedAgent agent : currentRecommendation.getAgents()) {
                pageManager.checkAndCreateNewPageIfNeeded(35);

                pageManager.drawText("\u2022 " + agent.getName() + " (" + agent.getClassName() + ")",
                        50, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);

                String agentDetails = lm.getString("pdf.indication") + ": " + agent.getClinicalIndication() +
                        "  |  " + lm.getString("pdf.weightEfficacy") + ": " + agent.getWeightEfficacy() +
                        "  |  " + lm.getString("pdf.admin") + ": " + agent.getAdministration();
                pageManager.drawWrappedText(agentDetails, 500,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 90);

                pageManager.addVerticalSpace(8);
            }

            // Clinical Warnings & Caveats
            String caveats = currentRecommendation.getClinicalCaveats();
            if (caveats != null && !caveats.trim().isEmpty()) {
                pageManager.checkAndCreateNewPageIfNeeded(20);
                pageManager.addVerticalSpace(10);
                pageManager.drawText(lm.getString("pdf.warnings") + ":",
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                String[] lines = caveats.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String cleanedLine = line.replace("**", "").replace("*", "").replace("-", "\u2013");
                    pageManager.drawWrappedText(cleanedLine, 500,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 90);
                }
            }

            // Disclaimer
            pageManager.addVerticalSpace(15);
            pageManager.drawText(lm.getString("pdf.disclaimer"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);

            pageManager.close();
            doc.save(file);
        }
    }
}
