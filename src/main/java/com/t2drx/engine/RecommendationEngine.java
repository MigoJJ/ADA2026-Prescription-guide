package com.t2drx.engine;

import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import com.t2drx.model.Recommendation.RecommendedAgent;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    // --- ADA 2026 Sulfonylurea (SU) policy thresholds ---
    private static final int    SU_ELDERLY_CAUTION_AGE = 65;  // start-low caution in older adults
    private static final int    SU_ELDERLY_AVOID_AGE   = 75;  // suppress SU (frailty / fall-risk proxy)
    private static final double SU_EGFR_AVOID          = 30;  // severe CKD -> suppress SU
    private static final double SU_EGFR_CKD_DRUGSWITCH = 60;  // prefer glipizide/gliclazide, avoid glyburide
    private static final double SU_WEIGHT_CAUTION_BMI  = 30;  // weight-gain caution threshold

    public Recommendation evaluate(PatientData p) {
        Recommendation rec = new Recommendation();
        StringBuilder rationaleBuilder = new StringBuilder();
        StringBuilder caveatsBuilder = new StringBuilder();
        List<RecommendedAgent> selectedAgents = new ArrayList<>();

        boolean leftBranchTriggered = false;
        boolean rightBranchTriggered = false;

        rationaleBuilder.append("### Clinical Rationale (Based on ADA 2026 Standards of Care)\n\n");

        // --- CONTRAINDICATION / CAVEAT CHECKS ---
        if (p.getEGFR() < 30) {
            caveatsBuilder.append("- **Metformin Contraindication:** eGFR is ")
                    .append(p.getEGFR())
                    .append(" mL/min/1.73m² (< 30). Metformin should be discontinued or not initiated.\n");
            if (p.isOnMetformin()) {
                caveatsBuilder.append("  *Action: Discontinue existing Metformin therapy.*\n");
            }
        } else if (p.getEGFR() < 45) {
            caveatsBuilder.append("- **Metformin Dosage Caution:** eGFR is ")
                    .append(p.getEGFR())
                    .append(" mL/min/1.73m² (30-45). Limit Metformin dose to maximum 1000 mg daily and monitor kidney function closely.\n");
        }

        // --- LEFT BRANCH: ORGAN PROTECTION (CVD, HF, CKD) ---
        boolean hasCvdRisk = p.hasASCVD() || p.hasHighCVDRisk();
        boolean hasHf = p.hasHeartFailure();
        boolean hasCkdRisk = p.hasCKD() || (p.getEGFR() < 60) || (p.getUacr() >= 30);

        if (hasCvdRisk || hasHf || hasCkdRisk) {
            leftBranchTriggered = true;
            rationaleBuilder.append("#### Column A: Organ Protection Branch Triggered\n");
            rationaleBuilder.append("The patient exhibits high risk or established cardiovascular, renal, or heart failure comorbidities. ");
            rationaleBuilder.append("Therapeutic selection in this branch is **independent of background Metformin use or baseline HbA1c**.\n\n");

            // 1. Established ASCVD or High CVD Risk Indicators
            if (hasCvdRisk) {
                rationaleBuilder.append("- **Established ASCVD / High CV Risk:** ");
                if (p.hasASCVD()) {
                    rationaleBuilder.append("Patient has established Atherosclerotic Cardiovascular Disease (ASCVD). ");
                } else {
                    rationaleBuilder.append("Patient has high cardiovascular risk indicators (e.g., age \u2265 55 with multiple risk factors). ");
                }
                rationaleBuilder.append("A GLP-1 Receptor Agonist (GLP-1 RA) or an SGLT2 inhibitor (SGLT2i) with proven cardiovascular benefit is strongly recommended.\n");

                // Add agents
                if (!p.isOnGLP1() && !p.isOnGIPGLP1()) {
                    selectedAgents.add(createGlp1RaCvd(p.hasCostConcern(), p.prefersOral()));
                }
                if (!p.isOnSGLT2i()) {
                    selectedAgents.add(createSglt2iCvd(p.getEGFR(), p.hasCostConcern()));
                }
            }

            // 2. Heart Failure (HFrEF / HFpEF)
            if (hasHf) {
                rationaleBuilder.append("- **Heart Failure:** Patient has documented Heart Failure. An SGLT2i (Empagliflozin or Dapagliflozin) is the primary recommendation to reduce HF hospitalization and CV mortality.\n");
                
                if (!p.isOnSGLT2i()) {
                    selectedAgents.add(createSglt2iHf(p.getEGFR(), p.hasCostConcern()));
                }

                // 2026 Specific addition: HFpEF + Obesity
                if (p.isHFpEF() && p.getBmi() >= 30.0) {
                    rationaleBuilder.append("  *ADA 2026 Update (HFpEF + Obesity):* For patients with HFpEF and obesity (BMI \u2265 30), a GLP-1 RA (specifically Semaglutide) or a dual GIP/GLP-1 RA (Tirzepatide) is recommended to improve functional capacity and symptoms (based on STEP-HFpEF and SUMMIT trial data).\n");
                    if (!p.isOnGIPGLP1()) {
                        selectedAgents.add(createGipGlp1Hfpef(p.hasCostConcern()));
                    }
                }
            }

            // 3. Chronic Kidney Disease (CKD)
            if (hasCkdRisk) {
                rationaleBuilder.append("- **Chronic Kidney Disease (CKD):** Patient has CKD risk indicators ");
                if (p.getEGFR() < 60) {
                    rationaleBuilder.append("(eGFR: ").append(p.getEGFR()).append(" mL/min/1.73m\u00b2). ");
                }
                if (p.getUacr() >= 30) {
                    rationaleBuilder.append("(UACR: ").append(p.getUacr()).append(" mg/g). ");
                }
                rationaleBuilder.append("To delay CKD progression and reduce CV events, an SGLT2i is preferred (if eGFR \u2265 20).\n");

                if (!p.isOnSGLT2i()) {
                    if (p.getEGFR() >= 20) {
                        selectedAgents.add(createSglt2iCkd(p.getEGFR(), p.hasCostConcern()));
                    } else {
                        caveatsBuilder.append("- **SGLT2i Warning:** eGFR is < 20 mL/min/1.73m\u00b2. SGLT2i initiation is not recommended; however, if already on it, it can be continued down to dialysis.\n");
                    }
                }

                // GLP-1 RA for CKD (FLOW trial evidence)
                if (!p.isOnGLP1() && !p.isOnGIPGLP1()) {
                    rationaleBuilder.append("  *GLP-1 RA for Kidney Protection:* GLP-1 RA with proven CKD benefit (e.g., Semaglutide) is recommended as a secondary kidney-protective agent or if SGLT2i is contraindicated or not tolerated.\n");
                    selectedAgents.add(createGlp1RaCkd(p.hasCostConcern(), p.prefersOral()));
                }
            }
        }

        // --- RIGHT BRANCH: METABOLIC GOALS (GLYCEMIC & WEIGHT EFFICACY) ---
        boolean isAboveA1cTarget = p.getCurrentHbA1c() > p.getTargetHbA1c();
        boolean needsWeightMgmt = p.wantsWeightManagement() || p.getBmi() >= 27.0;

        if (!leftBranchTriggered || isAboveA1cTarget || needsWeightMgmt) {
            rightBranchTriggered = true;
            rationaleBuilder.append("#### Column B: Metabolic Goals Branch\n");
            
            if (isAboveA1cTarget) {
                rationaleBuilder.append("- **Glycemic Efficacy Goal:** Patient's current HbA1c (")
                        .append(p.getCurrentHbA1c())
                        .append("%) is above their personalized target (")
                        .append(p.getTargetHbA1c())
                        .append("%). A highly efficacious glucose-lowering regimen is indicated.\n");
            }

            if (needsWeightMgmt) {
                rationaleBuilder.append("- **Weight Management Goal:** Patient has a weight management goal or elevated BMI (")
                        .append(p.getBmi())
                        .append("). High-efficacy weight loss agents are prioritized to address obesity as a core driver of Type 2 Diabetes.\n");
            }

            // High Efficacy Weight Management selection
            if (needsWeightMgmt && !p.isOnGIPGLP1() && !p.isOnGLP1() && !p.hasCostConcern()) {
                rationaleBuilder.append("  *Weight Management Preference:* Tirzepatide (Very High efficacy) or Semaglutide (Very High efficacy) are the preferred first-line agents for weight loss.\n");
                selectedAgents.add(createGipGlp1Weight());
                selectedAgents.add(createGlp1RaWeight(p.prefersOral()));
            }

            // Metformin as baseline if eGFR >= 30 and not on it
            if (!p.isOnMetformin() && p.getEGFR() >= 30) {
                selectedAgents.add(createMetformin(p.getEGFR()));
            }

            // Glycemic Efficacy Agents (if above target)
            if (isAboveA1cTarget) {
                // If patient has cost concerns, prioritize low-cost oral agents (TZD, SU)
                if (p.hasCostConcern()) {
                    rationaleBuilder.append("  *Cost Considerations:* Given patient's cost concern, affordable generic options like Pioglitazone (TZD) or a Sulfonylurea are considered. Per ADA 2026, SU is a non-cardiorenal, low-cost glucose-lowering add-on and is deprioritized when frailty, advanced CKD, insulin use, hypoglycemia risk, or weight loss are priorities.\n");
                    if (!p.isOnTZD()) {
                        selectedAgents.add(createTzdCost());
                    }
                    if (isSuitableForSu(p)) {
                        selectedAgents.add(createSu(p.getEGFR(), p.getAge()));
                        appendSuAppropriatenessCaveats(p, caveatsBuilder);
                    } else {
                        appendSuSuppressionCaveat(p, caveatsBuilder);
                    }
                } else {
                    // Modern classes if not on them
                    if (!p.isOnGIPGLP1() && !p.isOnGLP1()) {
                        selectedAgents.add(createGipGlp1Glycemic());
                    }
                    if (!p.isOnSGLT2i() && p.getEGFR() >= 20) {
                        selectedAgents.add(createSglt2iGlycemic(p.getEGFR()));
                    }
                }

                // Severe Hyperglycemia check: HbA1c > 10% or > 2% above target
                if (p.getCurrentHbA1c() >= 10.0 || (p.getCurrentHbA1c() - p.getTargetHbA1c() >= 2.0)) {
                    rationaleBuilder.append("- **Severe Hyperglycemia / Glucose Toxicity:** HbA1c is significantly elevated (")
                            .append(p.getCurrentHbA1c())
                            .append("%). Early introduction of **Insulin therapy** (basal insulin) or dual combination therapy is recommended to rapidly achieve glycemic control and relieve glucose toxicity.\n");
                    if (!p.isOnInsulin()) {
                        selectedAgents.add(createInsulin(p.hasHypoglycemiaRisk()));
                    }
                }
            }
        }

        // --- LIVER HEALTH: MASLD / MASH RISK ---
        if (p.hasMASLDRisk()) {
            rationaleBuilder.append("#### Liver Health: MASLD/MASH Considerations\n");
            rationaleBuilder.append("- **MASLD/MASH Risk:** For patients with Type 2 Diabetes and Metabolic Dysfunction-Associated Steatotic Liver Disease (MASLD), preferred choices include **GLP-1 RAs**, **Dual GIP/GLP-1 RAs**, or **Pioglitazone (TZD)** due to demonstrated histological benefits in NASH/MASH trials.\n\n");
            
            boolean hasPreferredLiverAgent = false;
            for (RecommendedAgent agent : selectedAgents) {
                if (agent.getClassName().equals("GLP-1 RA") || agent.getClassName().equals("GIP/GLP-1 RA") || agent.getClassName().equals("TZD")) {
                    hasPreferredLiverAgent = true;
                    break;
                }
            }

            if (!hasPreferredLiverAgent) {
                if (!p.isOnGIPGLP1() && !p.isOnGLP1()) {
                    selectedAgents.add(createGlp1RaLiver(p.hasCostConcern(), p.prefersOral()));
                } else if (!p.isOnTZD()) {
                    selectedAgents.add(createTzdLiver());
                }
            }
        }

        // --- BARRIER AND PREFERENCE ADJUSTMENTS ---
        if (p.hasHypoglycemiaRisk()) {
            caveatsBuilder.append("- **Hypoglycemia Risk warning:** Avoid Sulfonylureas (SU) and use Insulin with high caution (prefer Analogs like Glargine U300 or Degludec, and pair with CGM if possible). Prefer GLP-1 RA, SGLT2i, Metformin, or TZD.\n");
            // Filter out SU if it was added due to cost
            selectedAgents.removeIf(agent -> agent.getClassName().equals("SU"));
        }

        if (p.prefersOral()) {
            // Highlight oral agents; if injectables are added, add a warning
            caveatsBuilder.append("- **Administration Preference (Oral):** Patient prefers oral medications. GLP-1 RA injectables and Insulin are included for clinical indications, but oral alternatives (such as oral Semaglutide or SGLT2i) should be explored first.\n");
        }

        // Deduplicate agents based on class type (keep the most specific / relevant)
        List<RecommendedAgent> uniqueAgents = deduplicateAgents(selectedAgents);

        // Populate Recommendation
        rec.setAgents(uniqueAgents);
        rec.setRationale(rationaleBuilder.toString());
        rec.setClinicalCaveats(caveatsBuilder.toString());

        // Determine general summary header
        if (leftBranchTriggered && rightBranchTriggered) {
            rec.setSummary("Dual-Benefit Regimen: Cardiovascular/Renal Protection & Metabolic/Weight Control");
        } else if (leftBranchTriggered) {
            rec.setSummary("Organ-Protective Regimen: Prioritized Cardiovascular & Renal Preservation");
        } else {
            rec.setSummary("Metabolic-Focused Regimen: Glycemic Control & Weight Optimization");
        }

        return rec;
    }

    // --- SULFONYLUREA (SU) SUITABILITY & CAVEATS (ADA 2026) ---

    /**
     * ADA 2026 SU avoidance gate. SU is a low-cost, non-cardiorenal glucose-lowering
     * add-on that is withheld when hypoglycemia risk, concurrent insulin, frailty
     * (age proxy), or advanced CKD are present. BMI is handled as a caution, not a gate.
     */
    private boolean isSuitableForSu(PatientData p) {
        return !p.isOnSU()
                && !p.hasHypoglycemiaRisk()
                && !p.isOnInsulin()
                && p.getAge() < SU_ELDERLY_AVOID_AGE
                && p.getEGFR() >= SU_EGFR_AVOID;
    }

    /**
     * Explains why SU was withheld for an otherwise cost-concerned, above-target patient.
     * Reason precedence mirrors ADA 2026 priority: hypoglycemia > insulin > frailty > advanced CKD.
     */
    private void appendSuSuppressionCaveat(PatientData p, StringBuilder caveatsBuilder) {
        if (p.isOnSU()) {
            return; // already prescribed; not a withheld-recommendation scenario
        }
        String reason;
        if (p.hasHypoglycemiaRisk()) {
            reason = "patient has elevated hypoglycemia risk";
        } else if (p.isOnInsulin()) {
            reason = "patient is on insulin (SU + insulin substantially increases hypoglycemia risk)";
        } else if (p.getAge() >= SU_ELDERLY_AVOID_AGE) {
            reason = "older adult (age ≥ " + SU_ELDERLY_AVOID_AGE + "): heightened frailty, falls, and hypoglycemia risk";
        } else if (p.getEGFR() < SU_EGFR_AVOID) {
            reason = "advanced CKD (eGFR < " + (int) SU_EGFR_AVOID + "): markedly increased hypoglycemia risk";
        } else {
            return;
        }
        caveatsBuilder.append("- **Sulfonylurea Withheld:** Despite cost considerations, a Sulfonylurea was not recommended because ")
                .append(reason)
                .append(". Prefer Metformin, TZD, or (if affordable) SGLT2i/GLP-1 RA.\n");
    }

    /**
     * When SU is recommended, surface ADA 2026 appropriateness cautions: regular meals,
     * CKD drug-choice (avoid glyburide), weight gain, and start-low dosing in older adults.
     */
    private void appendSuAppropriatenessCaveats(PatientData p, StringBuilder caveatsBuilder) {
        caveatsBuilder.append("- **Sulfonylurea Appropriateness (ADA 2026):** Suitable as a low-cost add-on only with regular meal intake; counsel on hypoglycemia and weight gain. SU provides glucose lowering but no cardiorenal benefit.\n");
        if (p.getEGFR() < SU_EGFR_CKD_DRUGSWITCH) {
            caveatsBuilder.append("  *CKD Drug Choice (eGFR < ").append((int) SU_EGFR_CKD_DRUGSWITCH)
                    .append("): prefer Gliclazide MR or Glipizide and avoid Glyburide/Glibenclamide; start at the lowest dose.*\n");
        }
        if (p.getAge() >= SU_ELDERLY_CAUTION_AGE) {
            caveatsBuilder.append("  *Older Adult (age ≥ ").append(SU_ELDERLY_CAUTION_AGE)
                    .append("): initiate at a low dose and monitor closely for hypoglycemia.*\n");
        }
        if (p.getBmi() >= SU_WEIGHT_CAUTION_BMI) {
            caveatsBuilder.append("  *Obesity (BMI ≥ ").append((int) SU_WEIGHT_CAUTION_BMI)
                    .append("): SU promotes weight gain; weigh against weight-neutral/loss alternatives.*\n");
        }
    }

    private List<RecommendedAgent> deduplicateAgents(List<RecommendedAgent> agents) {
        List<RecommendedAgent> unique = new ArrayList<>();
        for (RecommendedAgent candidate : agents) {
            boolean alreadyExists = false;
            for (RecommendedAgent existing : unique) {
                if (existing.getClassName().equals(candidate.getClassName())) {
                    alreadyExists = true;
                    // Keep the one with a more specific uiColorClass (e.g., cv or weight is better than standard)
                    if (candidate.getUiColorClass().contains("-cv") || candidate.getUiColorClass().contains("-weight")) {
                        unique.remove(existing);
                        unique.add(candidate);
                    }
                    break;
                }
            }
            if (!alreadyExists) {
                unique.add(candidate);
            }
        }
        return unique;
    }

    // --- FACTORY METHODS FOR AGENTS ---

    private RecommendedAgent createGlp1RaCvd(boolean cost, boolean oral) {
        return new RecommendedAgent(
                oral ? "Oral Semaglutide (Rybelsus)" : "GLP-1 Receptor Agonist (e.g. Semaglutide - Ozempic, Dulaglutide - Trulicity)",
                "GLP-1 RA",
                "Established ASCVD or High Cardiovascular Risk",
                "Very High weight loss efficacy",
                "Low risk of hypoglycemia",
                cost ? "High Cost (Access challenges)" : "Premium / Covered",
                oral ? "Oral, daily (strict fasting requirement)" : "Subcutaneous injection, weekly",
                "Superior reduction in MACE (Major Adverse Cardiovascular Events) and CV death.",
                "Gastrointestinal side effects (nausea, vomiting, diarrhea), pancreatitis history, thyroid C-cell tumor risk.",
                "card-cv"
        );
    }

    private RecommendedAgent createSglt2iCvd(double egfr, boolean cost) {
        return new RecommendedAgent(
                "SGLT2 Inhibitor (e.g. Empagliflozin - Jardiance, Dapagliflozin - Farxiga)",
                "SGLT2i",
                "Established ASCVD, HF, or CKD Protection",
                "Intermediate weight loss efficacy",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                "Oral, daily",
                "Proven to reduce cardiovascular events and heart failure hospitalizations. Kidney protective.",
                "Genitourinary tract infections, eGFR < 20 (initiation not recommended), necrotizing fasciitis of perineum, risk of diabetic ketoacidosis (DKA).",
                "card-cv"
        );
    }

    private RecommendedAgent createSglt2iHf(double egfr, boolean cost) {
        return new RecommendedAgent(
                "SGLT2 Inhibitor (Dapagliflozin or Empagliflozin)",
                "SGLT2i",
                "Heart Failure (HFrEF, HFpEF, HFmrEF)",
                "Intermediate weight loss efficacy",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                "Oral, daily",
                "Class 1A indication in Heart Failure. Dramatically reduces cardiovascular death and heart failure hospitalization, independent of ejection fraction.",
                "Risk of volume depletion, hypotension, GU infections, eGFR < 20 (initiation warning).",
                "card-cv"
        );
    }

    private RecommendedAgent createGipGlp1Hfpef(boolean cost) {
        return new RecommendedAgent(
                "Dual GIP/GLP-1 RA (Tirzepatide - Mounjaro)",
                "GIP/GLP-1 RA",
                "HFpEF with Obesity (ADA 2026 update)",
                "Very High weight loss efficacy (>15-20% body weight)",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                "Subcutaneous injection, weekly",
                "Recommended in ADA 2026 for patients with HFpEF and obesity. Highly effective in reducing symptoms, physical limitations, and improving quality of life (SUMMIT trial).",
                "Nausea, vomiting, diarrhea, delayed gastric emptying. Avoid in medullary thyroid carcinoma or MEN2 history.",
                "card-cv"
        );
    }

    private RecommendedAgent createSglt2iCkd(double egfr, boolean cost) {
        return new RecommendedAgent(
                "SGLT2 Inhibitor (Empagliflozin, Dapagliflozin, or Canagliflozin)",
                "SGLT2i",
                "Chronic Kidney Disease (CKD Progression Prevention)",
                "Intermediate weight loss efficacy",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                "Oral, daily",
                "Primary agent to reduce eGFR decline, end-stage kidney disease (ESKD), and cardiovascular mortality in patients with CKD.",
                "Ensure eGFR \u2265 20 mL/min/1.73m\u00b2 before initiation. Monitor for volume depletion.",
                "card-cv"
        );
    }

    private RecommendedAgent createGlp1RaCkd(boolean cost, boolean oral) {
        return new RecommendedAgent(
                oral ? "Oral Semaglutide" : "GLP-1 Receptor Agonist (Semaglutide - FLOW Trial Evidence)",
                "GLP-1 RA",
                "Chronic Kidney Disease Progression Prevention",
                "Very High weight loss efficacy",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                oral ? "Oral, daily" : "Subcutaneous injection, weekly",
                "Demonstrated to significantly reduce primary kidney disease progression, CV events, and death in patients with T2D and CKD (FLOW trial). Preferred alternative/add-on if SGLT2i is not tolerated.",
                "Requires careful titration to avoid severe GI symptoms.",
                "card-cv"
        );
    }

    private RecommendedAgent createGipGlp1Weight() {
        return new RecommendedAgent(
                "Dual GIP/GLP-1 RA (Tirzepatide - Mounjaro)",
                "GIP/GLP-1 RA",
                "Obesity & Glycemic Optimization",
                "Very High / Tier-1 Weight Loss (>20% weight loss)",
                "Low risk of hypoglycemia",
                "High Cost",
                "Subcutaneous injection, weekly",
                "The most powerful non-insulin agent for HbA1c reduction and weight management. Exceptional weight loss efficacy.",
                "GI side effects, dose-dependent titration required. Contraindicated in thyroid C-cell cancer history.",
                "card-weight"
        );
    }

    private RecommendedAgent createGlp1RaWeight(boolean oral) {
        return new RecommendedAgent(
                oral ? "Oral Semaglutide (Rybelsus)" : "GLP-1 Receptor Agonist (Semaglutide - Ozempic/Wegovy)",
                "GLP-1 RA",
                "Obesity & Glycemic Optimization",
                "Very High Weight Loss (15-18% body weight)",
                "Low risk of hypoglycemia",
                "High Cost",
                oral ? "Oral, daily" : "Subcutaneous injection, weekly",
                "Highly effective for both glycemic control and weight management. Semaglutide delivers robust, Tier-1 weight loss results.",
                "Nausea, constipation, vomiting. Monitor for pancreatitis.",
                "card-weight"
        );
    }

    private RecommendedAgent createMetformin(double egfr) {
        return new RecommendedAgent(
                "Metformin (Glucophage)",
                "Metformin",
                "Glycemic Efficacy Baseline",
                "Neutral to mild weight loss",
                "Low risk of hypoglycemia",
                "Extremely Low Cost (Generic)",
                "Oral, daily (with meals)",
                "First-line, standard of care for glucose lowering, cheap, safe, well-tolerated. High glycemic efficacy.",
                "Gastrointestinal upset (diarrhea, cramping) - mitigated by Extended Release (XR). Contraindicated if eGFR < 30. Risk of lactic acidosis.",
                "card-glycemia"
        );
    }

    private RecommendedAgent createGipGlp1Glycemic() {
        return new RecommendedAgent(
                "Dual GIP/GLP-1 RA (Tirzepatide)",
                "GIP/GLP-1 RA",
                "Glycemic Efficacy Focus",
                "Very High weight loss",
                "Low risk of hypoglycemia",
                "High Cost",
                "Subcutaneous injection, weekly",
                "Provides the highest glycemic efficacy of all non-insulin therapies, frequently normalizing HbA1c with low hypoglycemia risk.",
                "Requires slow titration. Avoid in pancreatitis.",
                "card-glycemia"
        );
    }

    private RecommendedAgent createSglt2iGlycemic(double egfr) {
        return new RecommendedAgent(
                "SGLT2 Inhibitor (Empagliflozin or Dapagliflozin)",
                "SGLT2i",
                "Glycemic Efficacy & Blood Pressure reduction",
                "Intermediate weight loss",
                "Low risk of hypoglycemia",
                "High Cost",
                "Oral, daily",
                "High glycemic efficacy with added benefits of mild blood pressure reduction and low hypoglycemia risk.",
                "Efficacy reduced if eGFR < 45. Monitor for polyuria and mycotic infections.",
                "card-glycemia"
                );
    }

    private RecommendedAgent createTzdCost() {
        return new RecommendedAgent(
                "Thiazolidinedione (Pioglitazone - Actos)",
                "TZD",
                "Low Cost / High Glycemic Efficacy",
                "Weight gain risk",
                "Low risk of hypoglycemia",
                "Extremely Low Cost (Generic)",
                "Oral, daily",
                "Highly effective in lowering HbA1c, improves insulin sensitivity. Generic and very cheap. Cardiovascular safety proven (reduces stroke/MI risk, but increases HF risk).",
                "Fluid retention/edema, contraindicated in class III/IV Heart Failure, risk of bone fractures and bladder cancer (rare).",
                "card-glycemia"
        );
    }

    private RecommendedAgent createSu(double egfr, int age) {
        boolean ckd = egfr < SU_EGFR_CKD_DRUGSWITCH;
        boolean elderly = age >= SU_ELDERLY_CAUTION_AGE;

        String name = ckd
                ? "Sulfonylurea (Gliclazide MR or Glipizide – preferred in CKD)"
                : "Sulfonylurea (Gliclazide MR or Glimepiride)";

        StringBuilder cautions = new StringBuilder(
                "Significant risk of hypoglycemia and weight gain. Has 'beta-cell burnout' effect over years. Requires regular meal intake.");
        if (ckd) {
            cautions.append(" In CKD (eGFR < ").append((int) SU_EGFR_CKD_DRUGSWITCH)
                    .append("), avoid Glyburide/Glibenclamide and use the lowest effective dose due to heightened hypoglycemia risk.");
        }
        if (elderly) {
            cautions.append(" In older adults (age ≥ ").append(SU_ELDERLY_CAUTION_AGE)
                    .append("), start low and monitor closely (falls, fracture, hypoglycemia unawareness).");
        }

        return new RecommendedAgent(
                name,
                "SU",
                "Low Cost / Rapid Glycemic Efficacy (non-cardiorenal)",
                "Weight gain risk",
                "High risk of hypoglycemia",
                "Extremely Low Cost (Generic)",
                "Oral, daily",
                "Extremely cheap and widely available. Rapidly lowers blood glucose by stimulating pancreatic beta-cell insulin secretion. Consider when cost/access limits GLP-1 RA, SGLT2i, or DPP-4i and hypoglycemia risk is low with regular meals.",
                cautions.toString(),
                "card-glycemia"
        );
    }

    private RecommendedAgent createInsulin(boolean hypoRisk) {
        return new RecommendedAgent(
                "Basal Insulin (e.g. Glargine U100/U300, Degludec, Detemir)",
                "Insulin",
                "Glucose Toxicity / Extreme Hyperglycemia",
                "Weight gain risk",
                "High risk of hypoglycemia",
                "Variable (Generics/Biosimilars available, but pens can be expensive)",
                "Subcutaneous injection, daily",
                "Unlimited glycemic lowering capacity. Indicated immediately for severe hyperglycemia or symptomatic insulin deficiency.",
                "Requires frequent blood glucose monitoring or CGM. Basal insulin analogs (Degludec, Glargine U300) have lower hypoglycemia risk than NPH.",
                "card-glycemia"
        );
    }

    private RecommendedAgent createGlp1RaLiver(boolean cost, boolean oral) {
        return new RecommendedAgent(
                oral ? "Oral Semaglutide" : "GLP-1 Receptor Agonist (e.g. Semaglutide, Liraglutide)",
                "GLP-1 RA",
                "Type 2 Diabetes with MASLD/MASH",
                "Very High weight loss",
                "Low risk of hypoglycemia",
                cost ? "High Cost" : "Premium / Covered",
                oral ? "Oral, daily" : "Subcutaneous injection, weekly",
                "Highly recommended in MASLD/MASH. Demonstrates resolution of steatohepatitis and prevents fibrosis progression.",
                "GI tolerability is the primary barrier.",
                "card-glycemia"
        );
    }

    private RecommendedAgent createTzdLiver() {
        return new RecommendedAgent(
                "Pioglitazone (Thiazolidinedione)",
                "TZD",
                "Type 2 Diabetes with MASLD/MASH (Low Cost Option)",
                "Weight gain risk",
                "Low risk of hypoglycemia",
                "Extremely Low Cost (Generic)",
                "Oral, daily",
                "Pioglitazone has strong randomized controlled trial evidence showing improvement in liver histology, reduction in steatosis, and resolution of MASH.",
                "Avoid if patient has heart failure or significant fluid retention.",
                "card-glycemia"
        );
    }
}
