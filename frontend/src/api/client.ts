import axios from 'axios';
import {
  Product,
  ProductDeclaration,
  PackageEvidence,
  OcrExtraction,
  ExtractedField,
  ComplianceRule,
  RuleVersion,
  Evaluation,
  EvaluationFinding,
  RevalidationRun,
  RevalidationCandidate,
  ExperimentRun,
} from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

export const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Products
export const getProducts = (params?: { category?: string; search?: string; page?: number; size?: number }) =>
  api.get<{ content: Product[]; totalElements: number; totalPages: number }>('/products', { params });

export const getProduct = (id: number) => api.get<Product>(`/products/${id}`);

export const createProduct = (data: Partial<Product>) => api.post<Product>('/products', data);

export const updateProduct = (id: number, data: Partial<Product>) => api.put<Product>(`/products/${id}`, data);

export const getProductDeclarations = (productId: number) =>
  api.get<ProductDeclaration[]>(`/products/${productId}/declarations`);

export const updateDeclarations = (productId: number, data: { source: string; fields: { fieldName: string; fieldValue: string }[] }) =>
  api.post<ProductDeclaration[]>(`/products/${productId}/declarations`, data);

export const getCategoryStats = () => api.get<{ category: string; count: number }[]>('/products/categories/stats');

// Evidence & OCR
export const uploadEvidence = (productId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post<PackageEvidence>(`/evidence/upload/${productId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const getEvidenceList = () => api.get<PackageEvidence[]>('/evidence');

export const getEvidenceByProduct = (productId: number) =>
  api.get<PackageEvidence[]>(`/evidence/product/${productId}`);

export const runOcrExtraction = (evidenceId: number) =>
  api.post<OcrExtraction>(`/evidence/${evidenceId}/extract`);

export const getOcrExtraction = (evidenceId: number) =>
  api.get<OcrExtraction>(`/evidence/${evidenceId}/extraction`);

export const confirmExtraction = (
  evidenceId: number,
  data: {
    productId: number;
    fields: { extractedFieldId?: number; fieldName: string; confirmedValue: string; action: string }[];
  }
) => api.post<ExtractedField[]>(`/evidence/${evidenceId}/confirm`, data);

// Rules
export const getRules = () => api.get<ComplianceRule[]>('/rules');

export const getRule = (id: number) => api.get<ComplianceRule>(`/rules/${id}`);

export const createRule = (data: any) => api.post<ComplianceRule>('/rules', data);

export const createRuleVersion = (ruleId: number, data: any) =>
  api.post<RuleVersion>(`/rules/${ruleId}/versions`, data);

export const getRuleVersions = (ruleId: number) => api.get<RuleVersion[]>(`/rules/${ruleId}/versions`);

export const activateRuleVersion = (ruleId: number, versionId: number) =>
  api.post<RuleVersion>(`/rules/${ruleId}/versions/${versionId}/activate`);

export const compareRuleVersions = (ruleId: number, version1: number, version2: number) =>
  api.get<any>(`/rules/${ruleId}/versions/compare`, { params: { version1, version2 } });

// Evaluations
export const runEvaluation = (data: { productId: number; ruleVersionIds?: number[] }) =>
  api.post<Evaluation[]>('/evaluations/run', data);

export const getProductEvaluations = (productId: number) =>
  api.get<Evaluation[]>(`/evaluations/product/${productId}`);

export const getEvaluations = (params?: { page?: number; size?: number }) =>
  api.get<{ content: Evaluation[]; totalElements: number }>('/evaluations', { params });

export const getEvaluation = (id: number) => api.get<Evaluation>(`/evaluations/${id}`);

export const getEvaluationFindings = (id: number) =>
  api.get<EvaluationFinding[]>(`/evaluations/${id}/findings`);

export const updateFindingReview = (findingId: number, status: string) =>
  api.patch<EvaluationFinding>(`/evaluations/findings/${findingId}/review`, null, { params: { status } });

// Revalidation
export const runIncrementalRevalidation = (data: { ruleId: number; newVersionId: number }) =>
  api.post<RevalidationRun>('/revalidation/incremental', data);

export const runFullRevalidation = (data: { ruleId: number; newVersionId: number }) =>
  api.post<RevalidationRun>('/revalidation/full', data);

export const getRevalidationRuns = (params?: { page?: number; size?: number }) =>
  api.get<{ content: RevalidationRun[]; totalElements: number }>('/revalidation/runs', { params });

export const getRevalidationRun = (id: number) =>
  api.get<RevalidationRun>(`/revalidation/runs/${id}`);

export const getRevalidationCandidates = (runId: number) =>
  api.get<RevalidationCandidate[]>(`/revalidation/runs/${runId}/candidates`);

// Experiments
export const runExperiment = (data: {
  experimentName?: string;
  catalogueSize: number;
  impactRate: number;
  scenario: string;
  randomSeed?: number;
}) => api.post<ExperimentRun>('/experiments/run', data);

export const getExperiments = (params?: { page?: number; size?: number }) =>
  api.get<{ content: ExperimentRun[]; totalElements: number }>('/experiments', { params });

export const getExperiment = (id: number) => api.get<ExperimentRun>(`/experiments/${id}`);

// Dashboard
export const getDashboardSummary = () => api.get<any>('/dashboard/summary');

export const getDashboardCharts = () => api.get<any>('/dashboard/charts');
