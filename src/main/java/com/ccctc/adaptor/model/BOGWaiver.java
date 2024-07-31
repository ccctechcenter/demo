package com.ccctc.adaptor.model;

import com.ccctc.adaptor.util.CoverageIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Board of Governors Enrollment Fee Waiver.
 */
@ApiModel
@PrimaryKey({"misCode", "sisTermId", "sisPersonId"})
public class BOGWaiver implements Serializable {

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "ID of the student in the SIS", example = "person1")
    private String sisPersonId;

    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;
    @ApiModelProperty(value = "Identifier/code representing a term in the SIS", example = "2018SP")
    private String sisTermId;

    /*
     Eligibility fields
     */
    @ApiModelProperty(value = "Applicant’s marital status", example = "MARRIED")
    private MaritalStatus maritalStatus;
    @ApiModelProperty(value = "True/false value indicating whether applicant or parent is in a Registered Domestic Partnership")
    private Boolean regDomPartner;
    @ApiModelProperty(value = "True/false value indicating whether applicant was born before <23_year_date>")
    private Boolean bornBefore23Year;
    @ApiModelProperty(value = "True/false value indicating whether applicant is currently married or in a Registered Domestic Partnership")
    private Boolean marriedOrRDP;
    @ApiModelProperty(value = "True/false value indicating whether applicant is a veteran of the U.S. Armed Forces.")
    private Boolean usVeteran;
    @ApiModelProperty(value = "True/false value indicating whether applicant has dependents")
    private Boolean dependents;
    @ApiModelProperty(value = "True/false value indicating whether, at age 13 or older, applicant’s parents were deceased, applicant was in foster care, or applicant was a dependent/ward of the court.")
    private Boolean parentsDeceased;
    @ApiModelProperty(value = "True/false value indicating whether applicant is currently an emancipated minor as determined by a court.")
    private Boolean emancipatedMinor;
    @ApiModelProperty(value = "True/false value indicating whether applicant is in legal guardianship as determined by a court.")
    private Boolean legalGuardianship;
    @ApiModelProperty(value = "True/false value indicating whether, on or after July 1, 2016 applicant was determined to be an unaccompanied youth who was homeless by high school or school district homeless liaison.")
    private Boolean homelessYouthSchool;
    @ApiModelProperty(value = "True/false value indicating whether, on or after July 1, 2016, applicant was determined to be an unaccompanied youth who was homeless by the director of an emergency shelter program funded by the U.S. Department of Housing and Urban Development.")
    private Boolean homelessYouthHUD;
    @ApiModelProperty(value = "True/false value indicating whether, on or afterJuly 1, 2016, applicant was determined to be an unaccompanied youth who was homeless or were selfsupporting and at risk of being homeless by the director of a runaway or homeless youth basic center or transitional living program")
    private Boolean homelessYouthOther;
    @ApiModelProperty(value = "Whether applicant has been declared as a dependent by one or both parents in their 2017 US Tax Returns.", example = "PARENTS_NOT_FILE" )
    private DependentOnParentTaxesEnum dependentOnParentTaxes;
    @ApiModelProperty(value = "True/false value indicating whether applicant currently lives with one or both parents, and/or his/her RDP.")
    private Boolean livingWithParents;
    @ApiModelProperty(value = "Whether applicant is determined to be dependent or independent.", example = "INDEPENDENT, DEPENDENT")
    private DependencyStatus dependencyStatus;
    @ApiModelProperty(value = "True/false value indicating whether applicant has certification of waiver eligibility from Veterans Affairs.")
    private Boolean certVeteranAffairs;
    @ApiModelProperty(value = "True/false value indicating whether applicant has certification of waiver eligibility from the National Guard.")
    private Boolean certNationalGuard;
    @ApiModelProperty(value = "True/false value indicating whether applicant is eligible for waiver as a recipient of the Congressional Medal of Honor or as a child of a recipient.")
    private Boolean eligMedalHonor;
    @ApiModelProperty(value = "True/false value indicating whether applicant is eligible for waiver as a dependent of a victim of the September 11, 2001 terrorist attack.")
    private Boolean eligSept11;
    @ApiModelProperty(value = "True/false value indicating whether applicant is eligible for waiver as a dependent of a deceased law enforcement/fire suppression personnel killed in the line of duty")
    private Boolean eligPoliceFire;
    @ApiModelProperty(value = "True/false value indicating whether applicant is currently receiving TANF/CalWorks.")
    private Boolean tanfCalworks;
    @ApiModelProperty(value = "True/false value indicating whether applicant is currently receiving SSI/SSP.")
    private Boolean ssiSSP;
    @ApiModelProperty(value = "True/false value indicating whether applicant is currently receiving General Assistance")
    private Boolean generalAssistance;
    @ApiModelProperty(value = "True/false value indicating whether income from either TANF/CalWorks or SSI/SSP is a primary source of income for the applicant’s parents.")
    private Boolean parentsAssistance;
    @ApiModelProperty(value = "If applicant is dependent, how many persons in household?")
    private Integer depNumberHousehold;
    @ApiModelProperty(value = "If applicant is independent, how many persons in household?")
    private Integer indNumberHousehold;
    @ApiModelProperty(value = "If applicant is dependent, what is the adjusted gross income of parent(s) for year?")
    private Integer depGrossIncome;
    @ApiModelProperty(value = "If applicant is independent, what is adjusted gross income of applicant (and spouse) for year?")
    private Integer indGrossIncome;
    @ApiModelProperty(value = "If applicant is dependent, what is the other income of parent(s) for year?")
    private Integer depOtherIncome;
    @ApiModelProperty(value = "If applicant is independent, what is other income of applicant (and spouse) for year?")
    private Integer indOtherIncome;
    @ApiModelProperty(value = "If applicant is dependent, what is the total income of parent(s) for year?")
    private Integer depTotalIncome;
    @ApiModelProperty(value = "If applicant is independent, what is total income of applicant (and spouse) for year?")
    private Integer indTotalIncome;
    @ApiModelProperty(value = "Whether applicant is eligible for BOG fee waiver, and method (A, B and/or D) that determined eligibility", example = "METHOD_B")
    private Eligibility eligibility;
    @ApiModelProperty(value = "True/false value indicating whether applicant has been determined a resident of California by Admissions/Registrar")
    private Boolean determinedResidentCA;
    @ApiModelProperty(value = "True/false value indicating whether applicant has been determined to be AB540 eligible by Admission’s or Registrar’s Office.")
    private Boolean determinedAB540Eligible;
    @ApiModelProperty(value = "True/false value indicating whether applicant has been determined eligible for nonresident tuition exemption due to immigrant status.")
    private Boolean determinedNonResExempt;
    @ApiModelProperty(value = "True/false value indicating whether applicant has been determined homeless by the college Financial Aid Office.")
    private Boolean determinedHomeless;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public BOGWaiver() {
    }

    private BOGWaiver(Builder builder) {
        setMisCode(builder.misCode);
        setSisPersonId(builder.sisPersonId);
        setCccid(builder.cccid);
        setSisTermId(builder.sisTermId);
        setMaritalStatus(builder.maritalStatus);
        setRegDomPartner(builder.regDomPartner);
        setBornBefore23Year(builder.bornBefore23Year);
        setMarriedOrRDP(builder.marriedOrRDP);
        setUsVeteran(builder.usVeteran);
        setDependents(builder.dependents);
        setParentsDeceased(builder.parentsDeceased);
        setEmancipatedMinor(builder.emancipatedMinor);
        setLegalGuardianship(builder.legalGuardianship);
        setHomelessYouthSchool(builder.homelessYouthSchool);
        setHomelessYouthHUD(builder.homelessYouthHUD);
        setHomelessYouthOther(builder.homelessYouthOther);
        setDependentOnParentTaxes(builder.dependentOnParentTaxes);
        setLivingWithParents(builder.livingWithParents);
        setDependencyStatus(builder.dependencyStatus);
        setCertVeteranAffairs(builder.certVeteranAffairs);
        setCertNationalGuard(builder.certNationalGuard);
        setEligMedalHonor(builder.eligMedalHonor);
        setEligSept11(builder.eligSept11);
        setEligPoliceFire(builder.eligPoliceFire);
        setTanfCalworks(builder.tanfCalworks);
        setSsiSSP(builder.ssiSSP);
        setGeneralAssistance(builder.generalAssistance);
        setParentsAssistance(builder.parentsAssistance);
        setDepNumberHousehold(builder.depNumberHousehold);
        setIndNumberHousehold(builder.indNumberHousehold);
        setDepGrossIncome(builder.depGrossIncome);
        setIndGrossIncome(builder.indGrossIncome);
        setDepOtherIncome(builder.depOtherIncome);
        setIndOtherIncome(builder.indOtherIncome);
        setDepTotalIncome(builder.depTotalIncome);
        setIndTotalIncome(builder.indTotalIncome);
        setEligibility(builder.eligibility);
        setDeterminedResidentCA(builder.determinedResidentCA);
        setDeterminedAB540Eligible(builder.determinedAB540Eligible);
        setDeterminedNonResExempt(builder.determinedNonResExempt);
        setDeterminedHomeless(builder.determinedHomeless);
    }


    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisPersonId() {
        return sisPersonId;
    }

    public void setSisPersonId(String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }

    /**
     * Getter for property 'cccid'.
     *
     * @return Value for property 'cccid'.
     */
    public String getCccid() {
        return cccid;
    }

    /**
     * Setter for property 'cccid'.
     *
     * @param cccid Value to set for property 'cccid'.
     */
    public void setCccid(String cccid) {
        this.cccid = cccid;
    }

    /**
     * Getter for property 'sisTermId'.
     *
     * @return Value for property 'sisTermId'.
     */
    public String getSisTermId() {
        return sisTermId;
    }

    /**
     * Setter for property 'sisTermId'.
     *
     * @param sisTermId Value to set for property 'sisTermId'.
     */
    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    /**
     * Getter for property 'maritalStatus'.
     *
     * @return Value for property 'maritalStatus'.
     */
    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * Setter for property 'maritalStatus'.
     *
     * @param maritalStatus Value to set for property 'maritalStatus'.
     */
    public void setMaritalStatus(MaritalStatus maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    /**
     * Getter for property 'regDomPartner'.
     *
     * @return Value for property 'regDomPartner'.
     */
    public Boolean getRegDomPartner() {
        return regDomPartner;
    }

    /**
     * Setter for property 'regDomPartner'.
     *
     * @param regDomPartner Value to set for property 'regDomPartner'.
     */
    public void setRegDomPartner(Boolean regDomPartner) {
        this.regDomPartner = regDomPartner;
    }

    /**
     * Getter for property 'bornBefore23Year'.
     *
     * @return Value for property 'bornBefore23Year'.
     */
    public Boolean getBornBefore23Year() {
        return bornBefore23Year;
    }

    /**
     * Setter for property 'bornBefore23Year'.
     *
     * @param bornBefore23Year Value to set for property 'bornBefore23Year'.
     */
    public void setBornBefore23Year(Boolean bornBefore23Year) {
        this.bornBefore23Year = bornBefore23Year;
    }

    /**
     * Getter for property 'marriedOrRDP'.
     *
     * @return Value for property 'marriedOrRDP'.
     */
    public Boolean getMarriedOrRDP() {
        return marriedOrRDP;
    }

    /**
     * Setter for property 'marriedOrRDP'.
     *
     * @param marriedOrRDP Value to set for property 'marriedOrRDP'.
     */
    public void setMarriedOrRDP(Boolean marriedOrRDP) {
        this.marriedOrRDP = marriedOrRDP;
    }

    /**
     * Getter for property 'usVeteran'.
     *
     * @return Value for property 'usVeteran'.
     */
    public Boolean getUsVeteran() {
        return usVeteran;
    }

    /**
     * Setter for property 'usVeteran'.
     *
     * @param usVeteran Value to set for property 'usVeteran'.
     */
    public void setUsVeteran(Boolean usVeteran) {
        this.usVeteran = usVeteran;
    }

    /**
     * Getter for property 'dependents'.
     *
     * @return Value for property 'dependents'.
     */
    public Boolean getDependents() {
        return dependents;
    }

    /**
     * Setter for property 'dependents'.
     *
     * @param dependents Value to set for property 'dependents'.
     */
    public void setDependents(Boolean dependents) {
        this.dependents = dependents;
    }

    /**
     * Getter for property 'parentsDeceased'.
     *
     * @return Value for property 'parentsDeceased'.
     */
    public Boolean getParentsDeceased() {
        return parentsDeceased;
    }

    /**
     * Setter for property 'parentsDeceased'.
     *
     * @param parentsDeceased Value to set for property 'parentsDeceased'.
     */
    public void setParentsDeceased(Boolean parentsDeceased) {
        this.parentsDeceased = parentsDeceased;
    }

    /**
     * Getter for property 'emancipatedMinor'.
     *
     * @return Value for property 'emancipatedMinor'.
     */
    public Boolean getEmancipatedMinor() {
        return emancipatedMinor;
    }

    /**
     * Setter for property 'emancipatedMinor'.
     *
     * @param emancipatedMinor Value to set for property 'emancipatedMinor'.
     */
    public void setEmancipatedMinor(Boolean emancipatedMinor) {
        this.emancipatedMinor = emancipatedMinor;
    }

    /**
     * Getter for property 'legalGuardianship'.
     *
     * @return Value for property 'legalGuardianship'.
     */
    public Boolean getLegalGuardianship() {
        return legalGuardianship;
    }

    /**
     * Setter for property 'legalGuardianship'.
     *
     * @param legalGuardianship Value to set for property 'legalGuardianship'.
     */
    public void setLegalGuardianship(Boolean legalGuardianship) {
        this.legalGuardianship = legalGuardianship;
    }

    /**
     * Getter for property 'homelessYouthSchool'.
     *
     * @return Value for property 'homelessYouthSchool'.
     */
    public Boolean getHomelessYouthSchool() {
        return homelessYouthSchool;
    }

    /**
     * Setter for property 'homelessYouthSchool'.
     *
     * @param homelessYouthSchool Value to set for property 'homelessYouthSchool'.
     */
    public void setHomelessYouthSchool(Boolean homelessYouthSchool) {
        this.homelessYouthSchool = homelessYouthSchool;
    }

    /**
     * Getter for property 'homelessYouthHUD'.
     *
     * @return Value for property 'homelessYouthHUD'.
     */
    public Boolean getHomelessYouthHUD() {
        return homelessYouthHUD;
    }

    /**
     * Setter for property 'homelessYouthHUD'.
     *
     * @param homelessYouthHUD Value to set for property 'homelessYouthHUD'.
     */
    public void setHomelessYouthHUD(Boolean homelessYouthHUD) {
        this.homelessYouthHUD = homelessYouthHUD;
    }

    /**
     * Getter for property 'homelessYouthOther'.
     *
     * @return Value for property 'homelessYouthOther'.
     */
    public Boolean getHomelessYouthOther() {
        return homelessYouthOther;
    }

    /**
     * Setter for property 'homelessYouthOther'.
     *
     * @param homelessYouthOther Value to set for property 'homelessYouthOther'.
     */
    public void setHomelessYouthOther(Boolean homelessYouthOther) {
        this.homelessYouthOther = homelessYouthOther;
    }

    /**
     * Getter for property 'dependentOnParentTaxes'.
     *
     * @return Value for property 'dependentOnParentTaxes'.
     */
    public DependentOnParentTaxesEnum getDependentOnParentTaxes() {
        return dependentOnParentTaxes;
    }

    /**
     * Setter for property 'dependentOnParentTaxes'.
     *
     * @param dependentOnParentTaxes Value to set for property 'dependentOnParentTaxes'.
     */
    public void setDependentOnParentTaxes(DependentOnParentTaxesEnum dependentOnParentTaxes) {
        this.dependentOnParentTaxes = dependentOnParentTaxes;
    }

    /**
     * Getter for property 'livingWithParents'.
     *
     * @return Value for property 'livingWithParents'.
     */
    public Boolean getLivingWithParents() {
        return livingWithParents;
    }

    /**
     * Setter for property 'livingWithParents'.
     *
     * @param livingWithParents Value to set for property 'livingWithParents'.
     */
    public void setLivingWithParents(Boolean livingWithParents) {
        this.livingWithParents = livingWithParents;
    }

    /**
     * Getter for property 'dependencyStatus'.
     *
     * @return Value for property 'dependencyStatus'.
     */
    public DependencyStatus getDependencyStatus() {
        return dependencyStatus;
    }

    /**
     * Setter for property 'dependencyStatus'.
     *
     * @param dependencyStatus Value to set for property 'dependencyStatus'.
     */
    public void setDependencyStatus(DependencyStatus dependencyStatus) {
        this.dependencyStatus = dependencyStatus;
    }

    /**
     * Getter for property 'certVeteranAffairs'.
     *
     * @return Value for property 'certVeteranAffairs'.
     */
    public Boolean getCertVeteranAffairs() {
        return certVeteranAffairs;
    }

    /**
     * Setter for property 'certVeteranAffairs'.
     *
     * @param certVeteranAffairs Value to set for property 'certVeteranAffairs'.
     */
    public void setCertVeteranAffairs(Boolean certVeteranAffairs) {
        this.certVeteranAffairs = certVeteranAffairs;
    }

    /**
     * Getter for property 'certNationalGuard'.
     *
     * @return Value for property 'certNationalGuard'.
     */
    public Boolean getCertNationalGuard() {
        return certNationalGuard;
    }

    /**
     * Setter for property 'certNationalGuard'.
     *
     * @param certNationalGuard Value to set for property 'certNationalGuard'.
     */
    public void setCertNationalGuard(Boolean certNationalGuard) {
        this.certNationalGuard = certNationalGuard;
    }

    /**
     * Getter for property 'eligMedalHonor'.
     *
     * @return Value for property 'eligMedalHonor'.
     */
    public Boolean getEligMedalHonor() {
        return eligMedalHonor;
    }

    /**
     * Setter for property 'eligMedalHonor'.
     *
     * @param eligMedalHonor Value to set for property 'eligMedalHonor'.
     */
    public void setEligMedalHonor(Boolean eligMedalHonor) {
        this.eligMedalHonor = eligMedalHonor;
    }

    /**
     * Getter for property 'eligSept11'.
     *
     * @return Value for property 'eligSept11'.
     */
    public Boolean getEligSept11() {
        return eligSept11;
    }

    /**
     * Setter for property 'eligSept11'.
     *
     * @param eligSept11 Value to set for property 'eligSept11'.
     */
    public void setEligSept11(Boolean eligSept11) {
        this.eligSept11 = eligSept11;
    }

    /**
     * Getter for property 'eligPoliceFire'.
     *
     * @return Value for property 'eligPoliceFire'.
     */
    public Boolean getEligPoliceFire() {
        return eligPoliceFire;
    }

    /**
     * Setter for property 'eligPoliceFire'.
     *
     * @param eligPoliceFire Value to set for property 'eligPoliceFire'.
     */
    public void setEligPoliceFire(Boolean eligPoliceFire) {
        this.eligPoliceFire = eligPoliceFire;
    }

    /**
     * Getter for property 'tanfCalworks'.
     *
     * @return Value for property 'tanfCalworks'.
     */
    public Boolean getTanfCalworks() {
        return tanfCalworks;
    }

    /**
     * Setter for property 'tanfCalworks'.
     *
     * @param tanfCalworks Value to set for property 'tanfCalworks'.
     */
    public void setTanfCalworks(Boolean tanfCalworks) {
        this.tanfCalworks = tanfCalworks;
    }

    /**
     * Getter for property 'ssiSSP'.
     *
     * @return Value for property 'ssiSSP'.
     */
    public Boolean getSsiSSP() {
        return ssiSSP;
    }

    /**
     * Setter for property 'ssiSSP'.
     *
     * @param ssiSSP Value to set for property 'ssiSSP'.
     */
    public void setSsiSSP(Boolean ssiSSP) {
        this.ssiSSP = ssiSSP;
    }

    /**
     * Getter for property 'generalAssistance'.
     *
     * @return Value for property 'generalAssistance'.
     */
    public Boolean getGeneralAssistance() {
        return generalAssistance;
    }

    /**
     * Setter for property 'generalAssistance'.
     *
     * @param generalAssistance Value to set for property 'generalAssistance'.
     */
    public void setGeneralAssistance(Boolean generalAssistance) {
        this.generalAssistance = generalAssistance;
    }

    /**
     * Getter for property 'parentsAssistance'.
     *
     * @return Value for property 'parentsAssistance'.
     */
    public Boolean getParentsAssistance() {
        return parentsAssistance;
    }

    /**
     * Setter for property 'parentsAssistance'.
     *
     * @param parentsAssistance Value to set for property 'parentsAssistance'.
     */
    public void setParentsAssistance(Boolean parentsAssistance) {
        this.parentsAssistance = parentsAssistance;
    }

    /**
     * Getter for property 'depNumberHousehold'.
     *
     * @return Value for property 'depNumberHousehold'.
     */
    public Integer getDepNumberHousehold() {
        return depNumberHousehold;
    }

    /**
     * Setter for property 'depNumberHousehold'.
     *
     * @param depNumberHousehold Value to set for property 'depNumberHousehold'.
     */
    public void setDepNumberHousehold(Integer depNumberHousehold) {
        this.depNumberHousehold = depNumberHousehold;
    }

    /**
     * Getter for property 'indNumberHousehold'.
     *
     * @return Value for property 'indNumberHousehold'.
     */
    public Integer getIndNumberHousehold() {
        return indNumberHousehold;
    }

    /**
     * Setter for property 'indNumberHousehold'.
     *
     * @param indNumberHousehold Value to set for property 'indNumberHousehold'.
     */
    public void setIndNumberHousehold(Integer indNumberHousehold) {
        this.indNumberHousehold = indNumberHousehold;
    }

    /**
     * Getter for property 'depGrossIncome'.
     *
     * @return Value for property 'depGrossIncome'.
     */
    public Integer getDepGrossIncome() {
        return depGrossIncome;
    }

    /**
     * Setter for property 'depGrossIncome'.
     *
     * @param depGrossIncome Value to set for property 'depGrossIncome'.
     */
    public void setDepGrossIncome(Integer depGrossIncome) {
        this.depGrossIncome = depGrossIncome;
    }

    /**
     * Getter for property 'indGrossIncome'.
     *
     * @return Value for property 'indGrossIncome'.
     */
    public Integer getIndGrossIncome() {
        return indGrossIncome;
    }

    /**
     * Setter for property 'indGrossIncome'.
     *
     * @param indGrossIncome Value to set for property 'indGrossIncome'.
     */
    public void setIndGrossIncome(Integer indGrossIncome) {
        this.indGrossIncome = indGrossIncome;
    }

    /**
     * Getter for property 'depOtherIncome'.
     *
     * @return Value for property 'depOtherIncome'.
     */
    public Integer getDepOtherIncome() {
        return depOtherIncome;
    }

    /**
     * Setter for property 'depOtherIncome'.
     *
     * @param depOtherIncome Value to set for property 'depOtherIncome'.
     */
    public void setDepOtherIncome(Integer depOtherIncome) {
        this.depOtherIncome = depOtherIncome;
    }

    /**
     * Getter for property 'indOtherIncome'.
     *
     * @return Value for property 'indOtherIncome'.
     */
    public Integer getIndOtherIncome() {
        return indOtherIncome;
    }

    /**
     * Setter for property 'indOtherIncome'.
     *
     * @param indOtherIncome Value to set for property 'indOtherIncome'.
     */
    public void setIndOtherIncome(Integer indOtherIncome) {
        this.indOtherIncome = indOtherIncome;
    }

    /**
     * Getter for property 'depTotalIncome'.
     *
     * @return Value for property 'depTotalIncome'.
     */
    public Integer getDepTotalIncome() {
        return depTotalIncome;
    }

    /**
     * Setter for property 'depTotalIncome'.
     *
     * @param depTotalIncome Value to set for property 'depTotalIncome'.
     */
    public void setDepTotalIncome(Integer depTotalIncome) {
        this.depTotalIncome = depTotalIncome;
    }

    /**
     * Getter for property 'indTotalIncome'.
     *
     * @return Value for property 'indTotalIncome'.
     */
    public Integer getIndTotalIncome() {
        return indTotalIncome;
    }

    /**
     * Setter for property 'indTotalIncome'.
     *
     * @param indTotalIncome Value to set for property 'indTotalIncome'.
     */
    public void setIndTotalIncome(Integer indTotalIncome) {
        this.indTotalIncome = indTotalIncome;
    }

    /**
     * Getter for property 'eligibility'.
     *
     * @return Value for property 'eligibility'.
     */
    public Eligibility getEligibility() {
        return eligibility;
    }

    /**
     * Setter for property 'eligibility'.
     *
     * @param eligibility Value to set for property 'eligibility'.
     */
    public void setEligibility(Eligibility eligibility) {
        this.eligibility = eligibility;
    }

    /**
     * Getter for property 'determinedResidentCA'.
     *
     * @return Value for property 'determinedResidentCA'.
     */
    public Boolean getDeterminedResidentCA() {
        return determinedResidentCA;
    }

    /**
     * Setter for property 'determinedResidentCA'.
     *
     * @param determinedResidentCA Value to set for property 'determinedResidentCA'.
     */
    public void setDeterminedResidentCA(Boolean determinedResidentCA) {
        this.determinedResidentCA = determinedResidentCA;
    }

    /**
     * Getter for property 'determinedAB540Eligible'.
     *
     * @return Value for property 'determinedAB540Eligible'.
     */
    public Boolean getDeterminedAB540Eligible() {
        return determinedAB540Eligible;
    }

    /**
     * Setter for property 'determinedAB540Eligible'.
     *
     * @param determinedAB540Eligible Value to set for property 'determinedAB540Eligible'.
     */
    public void setDeterminedAB540Eligible(Boolean determinedAB540Eligible) {
        this.determinedAB540Eligible = determinedAB540Eligible;
    }

    /**
     * Getter for property 'determinedNonResExempt'.
     *
     * @return Value for property 'determinedNonResExempt'.
     */
    public Boolean getDeterminedNonResExempt() {
        return determinedNonResExempt;
    }

    /**
     * Setter for property 'determinedNonResExempt'.
     *
     * @param determinedNonResExempt Value to set for property 'determinedNonResExempt'.
     */
    public void setDeterminedNonResExempt(Boolean determinedNonResExempt) {
        this.determinedNonResExempt = determinedNonResExempt;
    }

    /**
     * Getter for property 'determinedHomeless'.
     *
     * @return Value for property 'determinedHomeless'.
     */
    public Boolean getDeterminedHomeless() {
        return determinedHomeless;
    }

    /**
     * Setter for property 'determinedHomeless'.
     *
     * @param determinedHomeless Value to set for property 'determinedHomeless'.
     */
    public void setDeterminedHomeless(Boolean determinedHomeless) {
        this.determinedHomeless = determinedHomeless;
    }

    /**
     * {@inheritDoc}
     */
    @CoverageIgnore
    @Override
    public String toString() {
        return "BOGWaiver{" +
                "cccid='" + cccid + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", maritalStatus=" + maritalStatus +
                ", regDomPartner=" + regDomPartner +
                ", bornBefore23Year=" + bornBefore23Year +
                ", marriedOrRDP=" + marriedOrRDP +
                ", usVeteran=" + usVeteran +
                ", dependents=" + dependents +
                ", parentsDeceased=" + parentsDeceased +
                ", emancipatedMinor=" + emancipatedMinor +
                ", legalGuardianship=" + legalGuardianship +
                ", homelessYouthSchool=" + homelessYouthSchool +
                ", homelessYouthHUD=" + homelessYouthHUD +
                ", homelessYouthOther=" + homelessYouthOther +
                ", dependentOnParentTaxes=" + dependentOnParentTaxes +
                ", livingWithParents=" + livingWithParents +
                ", dependencyStatus=" + dependencyStatus +
                ", certVeteranAffairs=" + certVeteranAffairs +
                ", certNationalGuard=" + certNationalGuard +
                ", eligMedalHonor=" + eligMedalHonor +
                ", eligSept11=" + eligSept11 +
                ", eligPoliceFire=" + eligPoliceFire +
                ", tanfCalworks=" + tanfCalworks +
                ", ssiSSP=" + ssiSSP +
                ", generalAssistance=" + generalAssistance +
                ", parentsAssistance=" + parentsAssistance +
                ", depNumberHousehold=" + depNumberHousehold +
                ", indNumberHousehold=" + indNumberHousehold +
                ", depGrossIncome=" + depGrossIncome +
                ", indGrossIncome=" + indGrossIncome +
                ", depOtherIncome=" + depOtherIncome +
                ", indOtherIncome=" + indOtherIncome +
                ", depTotalIncome=" + depTotalIncome +
                ", indTotalIncome=" + indTotalIncome +
                ", eligibility=" + eligibility +
                ", determinedResidentCA=" + determinedResidentCA +
                ", determinedAB540Eligible=" + determinedAB540Eligible +
                ", determinedNonResExempt=" + determinedNonResExempt +
                ", determinedHomeless=" + determinedHomeless +
                '}';
    }

    /*
        Enum
        2 - Parent(s) did not file
        1 - Yes
        0 - No
        */
    public enum DependentOnParentTaxesEnum {
        NO, YES, PARENTS_NOT_FILE
    }

    public enum DependencyStatus {DEPENDENT, INDEPENDENT}

    public enum Eligibility {
        METHOD_A, METHOD_B, METHOD_D, METHOD_A_AND_B, NOT_ELIGIBLE
    }


    public static final class Builder {
        private String misCode;
        private String sisPersonId;
        private String cccid;
        private String sisTermId;
        private MaritalStatus maritalStatus;
        private Boolean regDomPartner;
        private Boolean bornBefore23Year;
        private Boolean marriedOrRDP;
        private Boolean usVeteran;
        private Boolean dependents;
        private Boolean parentsDeceased;
        private Boolean emancipatedMinor;
        private Boolean legalGuardianship;
        private Boolean homelessYouthSchool;
        private Boolean homelessYouthHUD;
        private Boolean homelessYouthOther;
        private DependentOnParentTaxesEnum dependentOnParentTaxes;
        private Boolean livingWithParents;
        private DependencyStatus dependencyStatus;
        private Boolean certVeteranAffairs;
        private Boolean certNationalGuard;
        private Boolean eligMedalHonor;
        private Boolean eligSept11;
        private Boolean eligPoliceFire;
        private Boolean tanfCalworks;
        private Boolean ssiSSP;
        private Boolean generalAssistance;
        private Boolean parentsAssistance;
        private Integer depNumberHousehold;
        private Integer indNumberHousehold;
        private Integer depGrossIncome;
        private Integer indGrossIncome;
        private Integer depOtherIncome;
        private Integer indOtherIncome;
        private Integer depTotalIncome;
        private Integer indTotalIncome;
        private Eligibility eligibility;
        private Boolean determinedResidentCA;
        private Boolean determinedAB540Eligible;
        private Boolean determinedNonResExempt;
        private Boolean determinedHomeless;

        public Builder() {
        }

        public Builder misCode(String misCode) {
            this.misCode = misCode;
            return this;
        }

        public Builder sisPersonId(String sisPersonId) {
            this.sisPersonId = sisPersonId;
            return this;
        }

        public Builder cccid(String cccid) {
            this.cccid = cccid;
            return this;
        }

        public Builder sisTermId(String sisTermId) {
            this.sisTermId = sisTermId;
            return this;
        }

        public Builder maritalStatus(MaritalStatus maritalStatus) {
            this.maritalStatus = maritalStatus;
            return this;
        }

        public Builder regDomPartner(Boolean regDomPartner) {
            this.regDomPartner = regDomPartner;
            return this;
        }

        public Builder bornBefore23Year(Boolean bornBefore23Year) {
            this.bornBefore23Year = bornBefore23Year;
            return this;
        }

        public Builder marriedOrRDP(Boolean marriedOrRDP) {
            this.marriedOrRDP = marriedOrRDP;
            return this;
        }

        public Builder usVeteran(Boolean usVeteran) {
            this.usVeteran = usVeteran;
            return this;
        }

        public Builder dependents(Boolean dependents) {
            this.dependents = dependents;
            return this;
        }

        public Builder parentsDeceased(Boolean parentsDeceased) {
            this.parentsDeceased = parentsDeceased;
            return this;
        }

        public Builder emancipatedMinor(Boolean emancipatedMinor) {
            this.emancipatedMinor = emancipatedMinor;
            return this;
        }

        public Builder legalGuardianship(Boolean legalGuardianship) {
            this.legalGuardianship = legalGuardianship;
            return this;
        }

        public Builder homelessYouthSchool(Boolean homelessYouthSchool) {
            this.homelessYouthSchool = homelessYouthSchool;
            return this;
        }

        public Builder homelessYouthHUD(Boolean homelessYouthHUD) {
            this.homelessYouthHUD = homelessYouthHUD;
            return this;
        }

        public Builder homelessYouthOther(Boolean homelessYouthOther) {
            this.homelessYouthOther = homelessYouthOther;
            return this;
        }

        public Builder dependentOnParentTaxes(DependentOnParentTaxesEnum dependentOnParentTaxes) {
            this.dependentOnParentTaxes = dependentOnParentTaxes;
            return this;
        }

        public Builder livingWithParents(Boolean livingWithParents) {
            this.livingWithParents = livingWithParents;
            return this;
        }

        public Builder dependencyStatus(DependencyStatus dependencyStatus) {
            this.dependencyStatus = dependencyStatus;
            return this;
        }

        public Builder certVeteranAffairs(Boolean certVeteranAffairs) {
            this.certVeteranAffairs = certVeteranAffairs;
            return this;
        }

        public Builder certNationalGuard(Boolean certNationalGuard) {
            this.certNationalGuard = certNationalGuard;
            return this;
        }

        public Builder eligMedalHonor(Boolean eligMedalHonor) {
            this.eligMedalHonor = eligMedalHonor;
            return this;
        }

        public Builder eligSept11(Boolean eligSept11) {
            this.eligSept11 = eligSept11;
            return this;
        }

        public Builder eligPoliceFire(Boolean eligPoliceFire) {
            this.eligPoliceFire = eligPoliceFire;
            return this;
        }

        public Builder tanfCalworks(Boolean tanfCalworks) {
            this.tanfCalworks = tanfCalworks;
            return this;
        }

        public Builder ssiSSP(Boolean ssiSSP) {
            this.ssiSSP = ssiSSP;
            return this;
        }

        public Builder generalAssistance(Boolean generalAssistance) {
            this.generalAssistance = generalAssistance;
            return this;
        }

        public Builder parentsAssistance(Boolean parentsAssistance) {
            this.parentsAssistance = parentsAssistance;
            return this;
        }

        public Builder depNumberHousehold(Integer depNumberHousehold) {
            this.depNumberHousehold = depNumberHousehold;
            return this;
        }

        public Builder indNumberHousehold(Integer indNumberHousehold) {
            this.indNumberHousehold = indNumberHousehold;
            return this;
        }

        public Builder depGrossIncome(Integer depGrossIncome) {
            this.depGrossIncome = depGrossIncome;
            return this;
        }

        public Builder indGrossIncome(Integer indGrossIncome) {
            this.indGrossIncome = indGrossIncome;
            return this;
        }

        public Builder depOtherIncome(Integer depOtherIncome) {
            this.depOtherIncome = depOtherIncome;
            return this;
        }

        public Builder indOtherIncome(Integer indOtherIncome) {
            this.indOtherIncome = indOtherIncome;
            return this;
        }

        public Builder depTotalIncome(Integer depTotalIncome) {
            this.depTotalIncome = depTotalIncome;
            return this;
        }

        public Builder indTotalIncome(Integer indTotalIncome) {
            this.indTotalIncome = indTotalIncome;
            return this;
        }

        public Builder eligibility(Eligibility eligibility) {
            this.eligibility = eligibility;
            return this;
        }

        public Builder determinedResidentCA(Boolean determinedResidentCA) {
            this.determinedResidentCA = determinedResidentCA;
            return this;
        }

        public Builder determinedAB540Eligible(Boolean determinedAB540Eligible) {
            this.determinedAB540Eligible = determinedAB540Eligible;
            return this;
        }

        public Builder determinedNonResExempt(Boolean determinedNonResExempt) {
            this.determinedNonResExempt = determinedNonResExempt;
            return this;
        }

        public Builder determinedHomeless(Boolean determinedHomeless) {
            this.determinedHomeless = determinedHomeless;
            return this;
        }

        public BOGWaiver build() {
            return new BOGWaiver(this);
        }
    }
}