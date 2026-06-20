package com.t2drx.controller;

import com.t2drx.App;
import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import com.t2drx.model.Recommendation.RecommendedAgent;
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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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
        backButton.setOnAction(event -> App.getInstance().showInputView());
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
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                // Initialize positions
                float y = 740;
                float margin = 50;
                float width = page.getMediaBox().getWidth() - (2 * margin);

                // Helper for text printing to avoid PDFBox boilerplate
                content.beginText();
                content.newLineAtOffset(margin, y);
                
                // Title
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                content.showText("ADA 2026 Type 2 Diabetes Prescription Advisor");
                content.endText();
                
                // Line separator
                y -= 15;
                content.setLineWidth(1.5f);
                content.moveTo(margin, y);
                content.lineTo(margin + width, y);
                content.stroke();
                
                // Subtitle
                y -= 25;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.showText("Patient Recommendation Report - Powered by ADA 2026 Decision Logic");
                content.endText();

                // Patient Details block
                y -= 30;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                content.showText("PATIENT PROFILE:");
                content.endText();

                y -= 15;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.showText("Age: " + currentPatient.getAge() + "  |  Sex: " + currentPatient.getSex() + "  |  BMI: " + currentPatient.getBmi() + " kg/m2");
                content.endText();

                y -= 15;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.showText("Labs: Current HbA1c: " + currentPatient.getCurrentHbA1c() + "% (Target: " + currentPatient.getTargetHbA1c() + "%)" +
                        "  |  eGFR: " + currentPatient.getEGFR() + " mL/min  |  UACR: " + currentPatient.getUacr() + " mg/g");
                content.endText();

                y -= 15;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.showText("Comorbidities: " + 
                        (currentPatient.hasASCVD() ? "ASCVD, " : "") + 
                        (currentPatient.hasHighCVDRisk() ? "High CV Risk, " : "") + 
                        (currentPatient.hasHeartFailure() ? "Heart Failure, " : "") + 
                        (currentPatient.hasCKD() ? "CKD, " : "") + 
                        (currentPatient.hasMASLDRisk() ? "MASLD Risk" : "None specified"));
                content.endText();

                // Recommendation Summary
                y -= 35;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                content.showText("THERAPEUTIC REGIMEN SUMMARY:");
                content.endText();

                y -= 18;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                content.showText(currentRecommendation.getSummary());
                content.endText();

                // Detailed recommendations list
                y -= 25;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                content.showText("RECOMMENDED AGENTS:");
                content.endText();

                for (RecommendedAgent agent : currentRecommendation.getAgents()) {
                    y -= 20;
                    if (y < 80) { // Simple page break
                        break; // truncate or handle page overflow
                    }
                    content.beginText();
                    content.newLineAtOffset(margin + 15, y);
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                    content.showText("\u2022 " + agent.getName() + " (" + agent.getClassName() + ")");
                    content.endText();

                    y -= 14;
                    content.beginText();
                    content.newLineAtOffset(margin + 30, y);
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                    content.showText("Indication: " + agent.getClinicalIndication() + "  |  Weight Efficacy: " + agent.getWeightEfficacy() + "  |  Admin: " + agent.getAdministration());
                    content.endText();
                }

                // Caveats
                String cvs = currentRecommendation.getClinicalCaveats();
                if (cvs != null && !cvs.trim().isEmpty()) {
                    y -= 35;
                    if (y > 100) {
                        content.beginText();
                        content.newLineAtOffset(margin, y);
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                        content.showText("CLINICAL WARNINGS & CAVEATS:");
                        content.endText();

                        String[] lines = cvs.split("\n");
                        for (String line : lines) {
                            if (line.trim().isEmpty()) continue;
                            y -= 14;
                            content.beginText();
                            content.newLineAtOffset(margin + 10, y);
                            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                            // Strip basic markdown indicators
                            String cleanedLine = line.replace("**", "").replace("*", "").replace("-", "\u2013");
                            // Truncate line if it's too long for a single PDF row
                            if (cleanedLine.length() > 95) {
                                cleanedLine = cleanedLine.substring(0, 92) + "...";
                            }
                            content.showText(cleanedLine);
                            content.endText();
                        }
                    }
                }

                // Disclaimer
                y = 50;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
                content.showText("Disclaimer: This tool provides recommendations based on ADA 2026 Guidelines. Clinical judgement must always be exercised.");
                content.endText();
            }

            doc.save(file);
        }
    }
}
