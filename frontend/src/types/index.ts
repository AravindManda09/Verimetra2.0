export interface Product {
  id: number;
  productName: string;
  sku?: string;
  category: 'FOOD' | 'COSMETICS' | 'ELECTRONICS' | 'HOUSEHOLD';
  manufacturerName?: string;
  countryOfOrigin?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductDeclaration {
  id: number;
  productId: number;
  source: 'LISTING' | 'PACKAGE';
  fieldName: string;
  fieldValue: string;
  normalizedValue?: string;
  normalizationUnit?: string;
  confirmed: boolean;
  confirmedAt?: string;
}

export interface PackageEvidence {
  id: number;
  productId: number;
  originalFilename: string;
  storedFilename: string;
  contentType: string;
  fileSizeBytes: number;
  uploadStatus: 'UPLOADED' | 'PROCESSING' | 'EXTRACTED' | 'FAILED';
  createdAt: string;
}

export interface ExtractedField {
  id: number;
  extractionId: number;
  fieldName: string;
  suggestedValue: string;
  confidence?: number;
  reviewStatus: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'EDITED';
  confirmedValue?: string;
  reviewedAt?: string;
}

export interface OcrExtraction {
  id: number;
  evidenceId: number;
  rawText: string;
  extractionStatus: string;
  errorMessage?: string;
  processingTimeMs?: number;
  createdAt: string;
  extractedFields: ExtractedField[];
}

export interface RuleVersion {
  id: number;
  ruleId: number;
  versionNumber: number;
  requirementType: string;
  requiredField: string;
  validationOperator: string;
  expectedValue?: string;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  effectiveFrom: string;
  effectiveTo?: string;
  status: 'DRAFT' | 'ACTIVE' | 'SUPERSEDED' | 'DEPRECATED';
  changeSummary?: string;
  createdAt: string;
  applicableCategories: string[];
}

export interface ComplianceRule {
  id: number;
  ruleCode: string;
  ruleName: string;
  description?: string;
  legalReference?: string;
  sourceUrl?: string;
  sourceVerifiedDate?: string;
  isIllustrative: boolean;
  createdAt: string;
  updatedAt: string;
  activeVersion?: RuleVersion;
  versions?: RuleVersion[];
}

export interface EvaluationFinding {
  id: number;
  evaluationId: number;
  findingType: 'REGULATORY_REQUIREMENT' | 'CROSS_SOURCE_CONSISTENCY';
  fieldName: string;
  expectedValue?: string;
  actualValue?: string;
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'INFO';
  explanation: string;
  ruleReference?: string;
  ruleVersionId?: number;
  evidenceId?: number;
  reviewStatus: 'UNREVIEWED' | 'ACKNOWLEDGED' | 'DISPUTED' | 'RESOLVED';
  createdAt: string;
}

export interface Evaluation {
  id: number;
  productId: number;
  productName: string;
  category: string;
  ruleVersionId: number;
  ruleCode: string;
  ruleName: string;
  versionNumber: number;
  evaluationStatus: 'COMPLIANT' | 'NON_COMPLIANT' | 'NEEDS_REVIEW' | 'NOT_APPLICABLE' | 'INSUFFICIENT_DATA';
  evaluationType: string;
  triggeredBy?: string;
  revalidationRunId?: number;
  evaluatedAt: string;
  durationMs?: number;
  findings: EvaluationFinding[];
}

export interface RevalidationRun {
  id: number;
  runType: 'FULL' | 'INCREMENTAL';
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'FALLBACK';
  triggerRuleId?: number;
  triggerRuleCode?: string;
  triggerRuleName?: string;
  previousVersionId?: number;
  previousVersionNumber?: number;
  newVersionId?: number;
  newVersionNumber?: number;
  changedAttributes?: string;
  totalProductsConsidered: number;
  previouslyAssociatedCount: number;
  newlyApplicableCount: number;
  candidateCount: number;
  evaluationsPerformed: number;
  evaluationsReused: number;
  findingsGenerated: number;
  candidateSelectionMs?: number;
  evaluationMs?: number;
  persistenceMs?: number;
  totalDurationMs?: number;
  usedFallback: boolean;
  fallbackReason?: string;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}

export interface RevalidationCandidate {
  id: number;
  revalidationRunId: number;
  productId: number;
  candidateReason: 'PREVIOUSLY_ASSOCIATED' | 'NEWLY_APPLICABLE' | 'DECLARATION_CHANGED' | 'FULL_SCAN';
  wasEvaluated: boolean;
  wasReused: boolean;
}

export interface ExperimentRun {
  id: number;
  experimentName: string;
  catalogueSize: number;
  impactRate: number;
  scenario: string;
  randomSeed: number;
  status: string;
  fullTotalProducts: number;
  fullApplicablePairs: number;
  fullEvaluations: number;
  fullFindings: number;
  fullDurationMs: number;
  incrCandidates: number;
  incrPreviouslyAssociated: number;
  incrNewlyApplicable: number;
  incrEvaluations: number;
  incrReused: number;
  incrFindings: number;
  incrCandidateSelectionMs: number;
  incrEvaluationMs: number;
  incrPersistenceMs: number;
  incrTotalDurationMs: number;
  evaluationReductionPct: number;
  timeReductionPct: number;
  resultAgreementPct: number;
  missedAffectedCount: number;
  unnecessaryReevals: number;
  matchingOutcomes: number;
  differingOutcomes: number;
  missingOutcomes: number;
  extraOutcomes: number;
  correctnessStatus: string;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}
