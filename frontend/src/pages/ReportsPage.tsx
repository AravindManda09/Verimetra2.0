import React, { useEffect, useState } from 'react';
import {
  FileSpreadsheet,
  Download,
  FileCheck,
  CheckCircle2,
  Calendar,
  Layers,
  ArrowDownToLine,
} from 'lucide-react';
import { getDashboardSummary, getRevalidationRuns, getExperiments } from '../api/client';
import toast from 'react-hot-toast';

export const ReportsPage: React.FC = () => {
  const [summary, setSummary] = useState<any>(null);
  const [runs, setRuns] = useState<any[]>([]);
  const [experiments, setExperiments] = useState<any[]>([]);
  const [selectedRunId, setSelectedRunId] = useState<number | ''>('');
  const [selectedExpId, setSelectedExpId] = useState<number | ''>('');

  useEffect(() => {
    fetchReportData();
  }, []);

  const fetchReportData = async () => {
    try {
      const [sumRes, runRes, expRes] = await Promise.all([
        getDashboardSummary(),
        getRevalidationRuns({ size: 10 }),
        getExperiments({ size: 10 }),
      ]);
      setSummary(sumRes.data);
      setRuns(runRes.data.content);
      setExperiments(expRes.data.content);
      if (runRes.data.content.length > 0) setSelectedRunId(runRes.data.content[0].id);
      if (expRes.data.content.length > 0) setSelectedExpId(expRes.data.content[0].id);
    } catch {
      toast.error('Failed to load report data');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-2 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Compliance Reports & Export Center</h1>
        <p className="text-sm text-slate-500 mt-1">
          Generate and export audit-ready Legal Metrology compliance reports, revalidation logs, and experimental benchmarks
        </p>
      </div>

      {/* Available Reports Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Compliance Master Report */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="p-3 bg-blue-50 text-blue-600 rounded-lg w-fit">
              <FileCheck className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Product Compliance Report</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Export entire catalogue compliance findings, listing vs packaging declaration integrity, and status across all commodities.
            </p>
          </div>

          <div className="pt-4 border-t border-slate-100">
            <a
              href="/api/reports/compliance/csv"
              download="verimetra-compliance-report.csv"
              className="w-full inline-flex items-center justify-center px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold rounded-md shadow-sm transition-colors"
            >
              <Download className="w-4 h-4 mr-1.5" /> Download CSV
            </a>
          </div>
        </div>

        {/* Revalidation Audit Report */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="p-3 bg-purple-50 text-purple-600 rounded-lg w-fit">
              <Layers className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Revalidation Execution Audit</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Detailed audit log of Phase A (previously associated) and Phase B (newly applicable) candidate selection, execution times, and fallback history.
            </p>
            {runs.length > 0 && (
              <select
                value={selectedRunId}
                onChange={(e) => setSelectedRunId(Number(e.target.value))}
                className="w-full text-xs border border-slate-300 rounded px-2 py-1.5 bg-white font-mono"
              >
                {runs.map((r) => (
                  <option key={r.id} value={r.id}>
                    Run #{r.id} ({r.runType} — {r.triggerRuleCode || 'Rule'})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="pt-4 border-t border-slate-100">
            <a
              href={`/api/reports/revalidation/${selectedRunId}/csv`}
              download={`revalidation-run-${selectedRunId}.csv`}
              className="w-full inline-flex items-center justify-center px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-md shadow-sm transition-colors"
            >
              <Download className="w-4 h-4 mr-1.5" /> Download Run CSV
            </a>
          </div>
        </div>

        {/* Benchmark Experiment Report */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg w-fit">
              <FileSpreadsheet className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Benchmark Experiment Report</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Export canonical correctness comparison metrics, agreement percentages, evaluation work reduction, and timing overhead.
            </p>
            {experiments.length > 0 && (
              <select
                value={selectedExpId}
                onChange={(e) => setSelectedExpId(Number(e.target.value))}
                className="w-full text-xs border border-slate-300 rounded px-2 py-1.5 bg-white font-mono"
              >
                {experiments.map((exp) => (
                  <option key={exp.id} value={exp.id}>
                    Exp #{exp.id} (N={exp.catalogueSize}, {exp.scenario})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="pt-4 border-t border-slate-100">
            <a
              href={`/api/experiments/${selectedExpId}/export/csv`}
              download={`experiment-${selectedExpId}.csv`}
              className="w-full inline-flex items-center justify-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-md shadow-sm transition-colors"
            >
              <Download className="w-4 h-4 mr-1.5" /> Download Benchmark CSV
            </a>
          </div>
        </div>
      </div>

      {/* System Summary Audit Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
        <h3 className="text-sm font-bold text-slate-900">System Compliance Audit Snapshot</h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs font-mono">
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-slate-500 block text-[11px]">Total Products Monitored</span>
            <strong className="text-slate-900 text-sm mt-0.5 block">{summary?.totalProducts || 0}</strong>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-slate-500 block text-[11px]">Active Legal Rule Versions</span>
            <strong className="text-slate-900 text-sm mt-0.5 block">{summary?.activeRuleVersions || 0}</strong>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-slate-500 block text-[11px]">Total Evaluations Performed</span>
            <strong className="text-slate-900 text-sm mt-0.5 block">{summary?.totalEvaluations || 0}</strong>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-slate-500 block text-[11px]">Unreviewed Findings</span>
            <strong className="text-amber-600 text-sm mt-0.5 block">{summary?.unreviewedFindings || 0}</strong>
          </div>
        </div>
      </div>
    </div>
  );
};
