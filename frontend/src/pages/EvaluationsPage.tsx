import React, { useEffect, useState } from 'react';
import {
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Clock,
  Filter,
  Eye,
  Check,
  ChevronDown,
} from 'lucide-react';
import { getEvaluations, updateFindingReview } from '../api/client';
import { Evaluation, EvaluationFinding } from '../types';
import toast from 'react-hot-toast';

export const EvaluationsPage: React.FC = () => {
  const [evaluations, setEvaluations] = useState<Evaluation[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [selectedEvaluation, setSelectedEvaluation] = useState<Evaluation | null>(null);

  useEffect(() => {
    fetchEvaluations();
  }, [page]);

  const fetchEvaluations = async () => {
    try {
      setLoading(true);
      const res = await getEvaluations({ page, size: 25 });
      setEvaluations(res.data.content);
      setTotalElements(res.data.totalElements);
    } catch {
      toast.error('Failed to load evaluations');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateReviewStatus = async (findingId: number, status: string) => {
    try {
      await updateFindingReview(findingId, status);
      toast.success(`Finding marked as ${status}`);
      fetchEvaluations();
      if (selectedEvaluation) {
        setSelectedEvaluation({
          ...selectedEvaluation,
          findings: selectedEvaluation.findings.map((f) =>
            f.id === findingId ? { ...f, reviewStatus: status as any } : f
          ),
        });
      }
    } catch {
      toast.error('Failed to update finding status');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLIANT':
        return <span className="bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded text-[11px] font-bold">COMPLIANT</span>;
      case 'NON_COMPLIANT':
        return <span className="bg-red-100 text-red-800 px-2 py-0.5 rounded text-[11px] font-bold">NON_COMPLIANT</span>;
      case 'NEEDS_REVIEW':
        return <span className="bg-amber-100 text-amber-800 px-2 py-0.5 rounded text-[11px] font-bold">NEEDS_REVIEW</span>;
      default:
        return <span className="bg-slate-100 text-slate-700 px-2 py-0.5 rounded text-[11px] font-bold">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="pb-2 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Compliance Evaluations</h1>
        <p className="text-sm text-slate-500 mt-1">
          Traceable record of all deterministic rule checks, finding explanations, and evidence references
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Evaluations Table */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Product</th>
                  <th className="px-4 py-3">Rule & Version</th>
                  <th className="px-4 py-3">Outcome</th>
                  <th className="px-4 py-3">Findings</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3 text-right">View</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-slate-400">Loading evaluations...</td>
                  </tr>
                ) : evaluations.length > 0 ? (
                  evaluations.map((ev) => (
                    <tr
                      key={ev.id}
                      onClick={() => setSelectedEvaluation(ev)}
                      className={`cursor-pointer hover:bg-blue-50/50 transition-colors ${
                        selectedEvaluation?.id === ev.id ? 'bg-primary-50/70 border-l-4 border-primary-600' : ''
                      }`}
                    >
                      <td className="px-4 py-3 font-medium text-slate-900">
                        <div className="truncate max-w-[150px]">{ev.productName || `Product #${ev.productId}`}</div>
                        <span className="text-[10px] text-slate-400">{ev.category}</span>
                      </td>
                      <td className="px-4 py-3 font-mono">
                        <span className="font-bold text-slate-800">{ev.ruleCode}</span>
                        <span className="text-slate-400 ml-1">v{ev.versionNumber}</span>
                      </td>
                      <td className="px-4 py-3">{getStatusBadge(ev.evaluationStatus)}</td>
                      <td className="px-4 py-3">
                        {ev.findings.length > 0 ? (
                          <span className="text-amber-700 font-bold bg-amber-50 px-2 py-0.5 rounded">
                            {ev.findings.length} issue(s)
                          </span>
                        ) : (
                          <span className="text-emerald-700">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-500 text-[10px]">{ev.evaluationType}</td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedEvaluation(ev);
                          }}
                          className="text-primary-600 font-semibold"
                        >
                          Details &rarr;
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-slate-400">
                      No evaluations recorded yet. Run an evaluation from the Products tab or Revalidation.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Evaluation Findings & Details Pane */}
        <div className="space-y-4">
          {selectedEvaluation ? (
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4 text-xs">
              <div className="pb-3 border-b border-slate-200">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-slate-400 font-bold">EVALUATION #{selectedEvaluation.id}</span>
                  {getStatusBadge(selectedEvaluation.evaluationStatus)}
                </div>
                <h3 className="text-base font-bold text-slate-900 mt-1">{selectedEvaluation.productName}</h3>
                <p className="text-slate-500 text-[11px] mt-0.5">
                  Rule: <strong>{selectedEvaluation.ruleCode} v{selectedEvaluation.versionNumber}</strong> ({selectedEvaluation.ruleName})
                </p>
                <div className="text-[10px] text-slate-400 mt-1">
                  Evaluated at: {new Date(selectedEvaluation.evaluatedAt).toLocaleString()} | Duration: {selectedEvaluation.durationMs || 0} ms
                </div>
              </div>

              {/* Findings List */}
              <div className="space-y-3">
                <h4 className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">
                  Evaluation Findings ({selectedEvaluation.findings.length})
                </h4>

                {selectedEvaluation.findings.length > 0 ? (
                  selectedEvaluation.findings.map((f) => (
                    <div key={f.id} className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          f.findingType === 'REGULATORY_REQUIREMENT' ? 'bg-purple-100 text-purple-800' : 'bg-amber-100 text-amber-800'
                        }`}>
                          {f.findingType}
                        </span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          f.severity === 'CRITICAL' ? 'bg-red-100 text-red-800' :
                          f.severity === 'MAJOR' ? 'bg-amber-100 text-amber-800' :
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {f.severity}
                        </span>
                      </div>

                      <div className="space-y-0.5 text-[11px]">
                        <div>Field: <strong className="font-mono">{f.fieldName}</strong></div>
                        {f.expectedValue && <div>Expected: <span className="font-mono text-slate-600">{f.expectedValue}</span></div>}
                        {f.actualValue && <div>Actual: <span className="font-mono text-slate-600">{f.actualValue}</span></div>}
                      </div>

                      <p className="text-slate-700 text-[11px] leading-relaxed bg-white p-2 rounded border border-slate-200">
                        {f.explanation}
                      </p>

                      <div className="flex items-center justify-between pt-1">
                        <span className="text-[10px] text-slate-400">
                          Review: <strong>{f.reviewStatus}</strong>
                        </span>
                        <div className="flex space-x-1">
                          <button
                            onClick={() => handleUpdateReviewStatus(f.id, 'ACKNOWLEDGED')}
                            className="px-2 py-0.5 bg-slate-200 hover:bg-slate-300 rounded text-[10px] font-medium"
                          >
                            Acknowledge
                          </button>
                          <button
                            onClick={() => handleUpdateReviewStatus(f.id, 'RESOLVED')}
                            className="px-2 py-0.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-[10px] font-medium"
                          >
                            Resolve
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-6 text-center text-emerald-600 bg-emerald-50 rounded-lg border border-emerald-200 font-medium">
                    <CheckCircle2 className="w-6 h-6 mx-auto mb-1 text-emerald-500" />
                    All checks satisfied. No non-compliance findings detected.
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="bg-white p-12 rounded-xl border border-slate-200 text-center text-slate-400 text-xs">
              Select an evaluation from the list to view its findings and evidence.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
