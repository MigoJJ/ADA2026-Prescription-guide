package com.t2drx.engine;

import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import com.t2drx.model.Recommendation.RecommendedAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new RecommendationEngine();
    }

    @Test
    public void testEstablishedASCVD() {
        PatientData patient = new PatientData();
        patient.setHasASCVD(true);
        patient.setCurrentHbA1c(8.0);
        patient.setTargetHbA1c(7.0);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        assertTrue(recommendation.getRationale().contains("Organ Protection Branch Triggered"));
        assertTrue(recommendation.getRationale().contains("Established ASCVD / High CV Risk"));
        
        List<RecommendedAgent> agents = recommendation.getAgents();
        assertFalse(agents.isEmpty());
        
        // At least one of SGLT2i or GLP-1 RA should be recommended
        boolean hasGlp1 = agents.stream().anyMatch(a -> a.getClassName().equals("GLP-1 RA"));
        boolean hasSglt2 = agents.stream().anyMatch(a -> a.getClassName().equals("SGLT2i"));
        assertTrue(hasGlp1 || hasSglt2, "Should recommend GLP-1 RA or SGLT2i for ASCVD");
    }

    @Test
    public void testHeartFailure() {
        PatientData patient = new PatientData();
        patient.setHasHeartFailure(true);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasSglt2 = agents.stream().anyMatch(a -> a.getClassName().equals("SGLT2i"));
        assertTrue(hasSglt2, "SGLT2i should be recommended for Heart Failure");
    }

    @Test
    public void testHeartFailureWithObesity() {
        PatientData patient = new PatientData();
        patient.setHasHeartFailure(true);
        patient.setHFpEF(true);
        patient.setBmi(35.0); // Obesity

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        assertTrue(recommendation.getRationale().contains("HFpEF + Obesity"));
        
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasGipGlp1 = agents.stream().anyMatch(a -> a.getClassName().equals("GIP/GLP-1 RA"));
        assertTrue(hasGipGlp1, "Dual GIP/GLP-1 RA should be recommended for HFpEF + Obesity");
    }

    @Test
    public void testCKDProgressionRisk() {
        PatientData patient = new PatientData();
        patient.setEGFR(40.0); // CKD eGFR < 60, triggers Metformin caution (30-44)
        patient.setUacr(350.0); // Severe albuminuria

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasSglt2 = agents.stream().anyMatch(a -> a.getClassName().equals("SGLT2i"));
        assertTrue(hasSglt2, "SGLT2i should be recommended for CKD");
        
        assertTrue(recommendation.getClinicalCaveats().contains("Metformin Dosage Caution"), 
                "Should include Metformin dosage caution for eGFR 30-45");
    }

    @Test
    public void testSevereKidneyDysfunctionMetforminContraindication() {
        PatientData patient = new PatientData();
        patient.setEGFR(25.0); // eGFR < 30
        patient.setOnMetformin(true);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        assertTrue(recommendation.getClinicalCaveats().contains("Metformin Contraindication"), 
                "Should flag Metformin contraindication for eGFR < 30");
        assertTrue(recommendation.getClinicalCaveats().contains("Discontinue existing Metformin therapy"), 
                "Should suggest discontinuing Metformin");
    }

    @Test
    public void testWeightManagementGoal() {
        PatientData patient = new PatientData();
        patient.setWeightManagementGoal(true);
        patient.setBmi(32.0);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasGipGlp1 = agents.stream().anyMatch(a -> a.getClassName().equals("GIP/GLP-1 RA"));
        boolean hasGlp1 = agents.stream().anyMatch(a -> a.getClassName().equals("GLP-1 RA"));
        assertTrue(hasGipGlp1 || hasGlp1, "Should recommend weight-loss focused agents (GLP-1 or GIP/GLP-1 RA)");
    }

    @Test
    public void testSevereHyperglycemia() {
        PatientData patient = new PatientData();
        patient.setCurrentHbA1c(10.5);
        patient.setTargetHbA1c(7.0);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        assertTrue(recommendation.getRationale().contains("Glucose Toxicity"), 
                "Should mention glucose toxicity for high HbA1c");
        
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasInsulin = agents.stream().anyMatch(a -> a.getClassName().equals("Insulin"));
        assertTrue(hasInsulin, "Should recommend Insulin for HbA1c >= 10%");
    }

    @Test
    public void testMASLDRisk() {
        PatientData patient = new PatientData();
        patient.setHasMASLDRisk(true);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        assertTrue(recommendation.getRationale().contains("MASLD/MASH Considerations"));
        
        List<RecommendedAgent> agents = recommendation.getAgents();
        boolean hasPreferredAgent = agents.stream().anyMatch(a -> 
                a.getClassName().equals("GLP-1 RA") || 
                a.getClassName().equals("GIP/GLP-1 RA") || 
                a.getClassName().equals("TZD")
        );
        assertTrue(hasPreferredAgent, "Should recommend GLP-1, GIP/GLP-1, or TZD for MASLD/MASH");
    }

    @Test
    public void testCostConcernAndHypoglycemiaRisk() {
        PatientData patient = new PatientData();
        patient.setCurrentHbA1c(8.5);
        patient.setHasCostConcern(true);
        patient.setHasHypoglycemiaRisk(true);

        Recommendation recommendation = engine.evaluate(patient);
        
        assertNotNull(recommendation);
        List<RecommendedAgent> agents = recommendation.getAgents();
        
        // For cost concern, Sulfonylurea (SU) and TZD are considered. 
        // But with high hypoglycemia risk, SU should be excluded.
        boolean hasTzd = agents.stream().anyMatch(a -> a.getClassName().equals("TZD"));
        boolean hasSu = agents.stream().anyMatch(a -> a.getClassName().equals("SU"));
        
        assertTrue(hasTzd, "Should recommend TZD as low-cost glycemic option");
        assertFalse(hasSu, "Should NOT recommend Sulfonylurea due to hypoglycemia risk");
    }
}
