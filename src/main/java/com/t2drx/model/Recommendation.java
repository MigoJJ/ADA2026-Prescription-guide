package com.t2drx.model;

import java.util.ArrayList;
import java.util.List;

public class Recommendation {
    private String summary;
    private String rationale;
    private String clinicalCaveats;
    private List<RecommendedAgent> agents = new ArrayList<>();

    public Recommendation() {
        this.summary = "";
        this.rationale = "";
        this.clinicalCaveats = "";
    }

    public Recommendation(String summary, String rationale, String clinicalCaveats) {
        this.summary = summary;
        this.rationale = rationale;
        this.clinicalCaveats = clinicalCaveats;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getClinicalCaveats() { return clinicalCaveats; }
    public void setClinicalCaveats(String clinicalCaveats) { this.clinicalCaveats = clinicalCaveats; }

    public List<RecommendedAgent> getAgents() { return agents; }
    public void setAgents(List<RecommendedAgent> agents) { this.agents = agents; }

    public void addAgent(RecommendedAgent agent) {
        this.agents.add(agent);
    }

    public static class RecommendedAgent {
        private String name;
        private String className;
        private String clinicalIndication;
        private String weightEfficacy;
        private String hypoglycemiaRisk;
        private String costCategory;
        private String administration;
        private String reasonsToChoose;
        private String cautions;
        private String uiColorClass; // CSS class for visual styling (e.g., card-cv, card-weight, card-glycemia)

        public RecommendedAgent(String name, String className, String clinicalIndication, 
                                String weightEfficacy, String hypoglycemiaRisk, String costCategory, 
                                String administration, String reasonsToChoose, String cautions, 
                                String uiColorClass) {
            this.name = name;
            this.className = className;
            this.clinicalIndication = clinicalIndication;
            this.weightEfficacy = weightEfficacy;
            this.hypoglycemiaRisk = hypoglycemiaRisk;
            this.costCategory = costCategory;
            this.administration = administration;
            this.reasonsToChoose = reasonsToChoose;
            this.cautions = cautions;
            this.uiColorClass = uiColorClass;
        }

        // Getters
        public String getName() { return name; }
        public String getClassName() { return className; }
        public String getClinicalIndication() { return clinicalIndication; }
        public String getWeightEfficacy() { return weightEfficacy; }
        public String getHypoglycemiaRisk() { return hypoglycemiaRisk; }
        public String getCostCategory() { return costCategory; }
        public String getAdministration() { return administration; }
        public String getReasonsToChoose() { return reasonsToChoose; }
        public String getCautions() { return cautions; }
        public String getUiColorClass() { return uiColorClass; }
    }
}
