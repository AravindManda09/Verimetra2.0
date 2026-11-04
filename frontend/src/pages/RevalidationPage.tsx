import React, { useEffect, useState } from 'react';
import {
  RefreshCw,
  GitBranch,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Layers,
  Sparkles,
  AlertCircle,
  FileCheck,
} from 'lucide-react';
import {
  getRules,
  runIncrementalRevalidation,
  runFullRevalidation,
  getRevalidationRuns,
  getRevalidationCandidates,
} from '../api/client';
import { ComplianceRule, RevalidationRun, RevalidationCandidate } from '../types';
import toast from 'react-hot-toast';

export const RevalidationPage: React.FC = () => {
  const [rules, setRules] = useState<ComplianceRule[]>([]);
  const [selectedRuleId, setSelectedRuleId] = useState<number | ''>('');
  const [selectedVersionId, setSelectedVersionId] = useState<number | ''>('');
  const [recentRuns, setRecentRuns] = useState<RevalidationRun[]>([]);
  const [latestRun, setLatestRun] = useState<RevalidationRun | null>(null);
  const [candidates, setCandidates] = useState<RevalidationCandidate[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchRulesAndRuns();
  }, []);

  const fetchRulesAndRuns = async () => {
    try {
      const [rulesRes, runsRes] = await Promise.all([
        getRules(),
        getRevalidationRuns({ size: 10 }),
      ]);
      setRules(rulesRes.data);
      setRecentRuns(runsRes.data.content);

      if (rulesRes.data.length > 0) {
        const first = rulesRes.data[0];
        setSelectedRuleId(first.id);
        if (first.activeVersion) {
          setSelectedVersionId(first.activeVersion.id);
        }
      }

      if (runsRes.data.content.length > 0) {
        handleSelectRun(runsRes.data.content[0]);
      }
    } catch {
      toast.error('Failed to load revalidation context');
    }
  };

  const handleSelectRule = (ruleId: number) => {
    setSelectedRuleId(ruleId);
    const rule = rules.find((r) => r.id === ruleId);
    if (rule?.activeVersion) {
      setSelectedVersionId(rule.activeVersion.id);
    }
  };

  const handleSelectRun = async (run: RevalidationRun) => {
    setLatestRun(run);
    try {
      const res = await getRevalidationCandidates(run.id);
      setCandidates(res.data);
    } catch {
      setCandidates([]);
    }
  };

  const handleRunIncremental = async () => {
    if (!selectedRuleId || !selectedVersionId) {
      toast.error('Please select rule and version');
      return;
    }
    try {
      setLoading(true);
      toast.loading('Executing dependency-aware incremental revalidation (Phase A + Phase B)...', { id: 'reval' });
      const res = await runIncrementalRevalidation({
        ruleId: Number(selectedRuleId),
        newVersionId: Number(selectedVersionId),
      });
      toast.success('Incremental revalidation complete!', { id: 'reval' });
      setLatestRun(res.data);
      handleSelectRun(res.data);
      const runsRes = await getRevalidationRuns({ size: 10 });
      setRecentRuns(runsRes.data.content);
    } catch (err: any) {
      toast.error('Revalidation failed: ' + (err.response?.data?.message || err.message), { id: 'reval' });
    } finally {
      setLoading(false);
    }
  };

  const handleRunFull = async () => {
    if (!selectedRuleId || !selectedVersionId) {
      toast.error('Please select rule and version');
      return;
    }
    try {
      setLoading(true);
      toast.loading('Executing Full Revalidation baseline across entire catalogue...', { id: 'reval' });
      const res = await runFullRevalidation({
        ruleId: Number(selectedRuleId),
        newVersionId: Number(selectedVersionId),
      });
      toast.success('Full Revalidation baseline complete!', { id: 'reval' });
      setLatestRun(res.data);
      handleSelectRun(res.data);
      const runsRes = await getRevalidationRuns({ size: 10 });
      setRecentRuns(runsRes.data.content);
    } catch (err: any) {
      toast.error('Full revalidation failed: ' + (err.response?.data?.message || err.message), { id: 'reval' });
    } finally {
      setLoading(false);
    }
  };

  const currentRule = rules.find((r) => r.id === selectedRuleId);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-2 border-b border-slate-200">
        <div className="flex items-center space-x-2">
          <span className="text-xs bg-purple-100 text-purple-800 font-bold px-2 py-0.5 rounded">Core Research Contribution</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight mt-1">
          Dependency-Aware Incremental Revalidation
        </h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Selectively re-evaluate affected product declarations when regulatory rules change, discovering newly applicable commodities
        </p>
      </div>

      {/* Two-Phase Strategy Visual Architecture */}
      <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white p-6 rounded-2xl shadow-md space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-300 flex items-center">
            <Sparkles className="w-4 h-4 mr-2 text-indigo-400" />
            Two-Phase Candidate Selection Architecture
          </h3>
          <span className="text-xs bg-indigo-900/80 text-indigo-200 border border-indigo-700 px-2 py-0.5 rounded font-mono">
            Guaranteed Complete & Deterministic
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="bg-slate-800/80 p-4 rounded-xl border border-slate-700 space-y-2">
            <div className="flex items-center justify-between text-indigo-300 font-bold">
              <span>PHASE A</span>
              <span className="text-[10px] bg-indigo-950 px-2 py-0.5 rounded">Dependency Graph</span>
            </div>
            <h4 className="font-semibold text-slate-100 text-sm">Previously Associated</h4>
            <p className="text-slate-400 leading-relaxed">
              Finds products previously evaluated under the modified rule version using active dependency mappings.
            </p>
          </div>

          <div className="bg-slate-800/80 p-4 rounded-xl border border-slate-700 space-y-2">
            <div className="flex items-center justify-between text-purple-300 font-bold">
              <span>PHASE B</span>
              <span className="text-[10px] bg-purple-950 px-2 py-0.5 rounded">Applicability Expansion</span>
            </div>
            <h4 className="font-semibold text-slate-100 text-sm">Newly Applicable Products</h4>
            <p className="text-slate-400 leading-relaxed">
              Detects existing catalogue products that become newly subject when a rule's categories expand (e.g. FOOD &rarr; FOOD + COSMETICS).
            </p>
          </div>

          <div className="bg-slate-800/80 p-4 rounded-xl border border-slate-700 space-y-2">
            <div className="flex items-center justify-between text-emerald-300 font-bold">
              <span>SAFETY FALLBACK</span>
              <span className="text-[10px] bg-emerald-950 px-2 py-0.5 rounded">Correctness Guard</span>
            </div>
            <h4 className="font-semibold text-slate-100 text-sm">Automatic Fallback</h4>
            <p className="text-slate-400 leading-relaxed">
              If dependency integrity cannot be established with 100% confidence, safely falls back to Full Revalidation without stale reuse.
            </p>
          </div>
        </div>
      </div>

      {/* Control Bar: Select Rule & Trigger Execution */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex flex-col sm:flex-row items-center gap-3 w-full sm:w-auto text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Target Regulatory Rule:</label>
            <select
              value={selectedRuleId}
              onChange={(e) => handleSelectRule(Number(e.target.value))}
              className="px-3 py-2 border border-slate-300 rounded-md bg-white font-medium"
            >
              {rules.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.ruleCode} — {r.ruleName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Rule Version to Revalidate:</label>
            <select
              value={selectedVersionId}
              onChange={(e) => setSelectedVersionId(Number(e.target.value))}
              className="px-3 py-2 border border-slate-300 rounded-md bg-white font-mono"
            >
              {currentRule?.versions?.map((v) => (
                <option key={v.id} value={v.id}>
                  v{v.versionNumber} ({v.status}) — Applies to: {v.applicableCategories?.join(', ')}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex items-center space-x-3 w-full sm:w-auto">
          <button
            onClick={handleRunFull}
            disabled={loading}
            className="flex-1 sm:flex-none inline-flex items-center justify-center px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-md border border-slate-300 shadow-sm"
          >
            <Layers className="w-4 h-4 mr-1.5 text-slate-600" />
            Run Full (Baseline)
          </button>

          <button
            onClick={handleRunIncremental}
            disabled={loading}
            className="flex-1 sm:flex-none inline-flex items-center justify-center px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white text-xs font-bold rounded-md shadow-sm"
          >
            <RefreshCw className={`w-4 h-4 mr-1.5 ${loading ? 'animate-spin' : ''}`} />
            Run Incremental Revalidation
          </button>
        </div>
      </div>

      {/* Execution Results Summary */}
      {latestRun && (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-6">
          <div className="flex items-start justify-between pb-4 border-b border-slate-200">
            <div>
              <div className="flex items-center space-x-2">
                <span className={`px-2.5 py-0.5 rounded font-bold text-xs ${
                  latestRun.runType === 'INCREMENTAL' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'
                }`}>
                  {latestRun.runType} REVALIDATION RUN #{latestRun.id}
                </span>
                <span className="text-xs text-slate-500 font-mono">
                  {latestRun.triggerRuleCode} (v{latestRun.newVersionNumber || 1})
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Change: <strong>{latestRun.changedAttributes}</strong>
              </p>
            </div>

            <div className="flex items-center space-x-3">
              {latestRun.usedFallback ? (
                <span className="text-xs bg-amber-100 text-amber-800 px-2.5 py-1 rounded-md font-bold flex items-center">
                  <AlertCircle className="w-4 h-4 mr-1" /> Safe Fallback Triggered
                </span>
              ) : (
                <span className="text-xs bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded-md font-bold flex items-center">
                  <CheckCircle2 className="w-4 h-4 mr-1" /> Verified Incremental Reuse
                </span>
              )}
            </div>
          </div>

          {/* Metric KPI Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 text-xs">
            <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
              <span className="text-slate-400 font-medium block">Total Catalogue</span>
              <strong className="text-lg text-slate-900 mt-0.5 block">{latestRun.totalProductsConsidered}</strong>
            </div>

            <div className="bg-indigo-50 p-3 rounded-lg border border-indigo-200">
              <span className="text-indigo-600 font-medium block">Phase A (Previous)</span>
              <strong className="text-lg text-indigo-950 mt-0.5 block">{latestRun.previouslyAssociatedCount}</strong>
            </div>

            <div className="bg-purple-50 p-3 rounded-lg border border-purple-200">
              <span className="text-purple-600 font-medium block">Phase B (Newly Applicable)</span>
              <strong className="text-lg text-purple-950 mt-0.5 block">{latestRun.newlyApplicableCount}</strong>
            </div>

            <div className="bg-emerald-50 p-3 rounded-lg border border-emerald-200">
              <span className="text-emerald-600 font-medium block">Re-evaluated</span>
              <strong className="text-lg text-emerald-950 mt-0.5 block">{latestRun.evaluationsPerformed}</strong>
            </div>

            <div className="bg-blue-50 p-3 rounded-lg border border-blue-200">
              <span className="text-blue-600 font-medium block">Safely Reused</span>
              <strong className="text-lg text-blue-950 mt-0.5 block">{latestRun.evaluationsReused}</strong>
            </div>

            <div className="bg-slate-900 text-white p-3 rounded-lg">
              <span className="text-slate-400 font-medium block">Total Execution Time</span>
              <strong className="text-lg text-emerald-400 font-mono mt-0.5 block">{latestRun.totalDurationMs} ms</strong>
            </div>
          </div>

          {/* Timing Breakdown */}
          <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 text-xs flex flex-wrap items-center justify-between gap-4 font-mono">
            <div>Candidate Selection: <strong>{latestRun.candidateSelectionMs || 0} ms</strong></div>
            <div>Evaluation Time: <strong>{latestRun.evaluationMs || 0} ms</strong></div>
            <div>Persistence Time: <strong>{latestRun.persistenceMs || 0} ms</strong></div>
            <div>Findings Generated: <strong>{latestRun.findingsGenerated}</strong></div>
          </div>

          {/* Candidate Products Breakdown */}
          <div className="space-y-3">
            <h4 className="text-sm font-bold text-slate-900">
              Discovered Candidate Products ({candidates.length})
            </h4>

            <div className="border border-slate-200 rounded-lg overflow-hidden">
              <table className="min-w-full text-xs divide-y divide-slate-200">
                <thead className="bg-slate-50 font-semibold text-slate-600">
                  <tr>
                    <th className="px-4 py-2 text-left">Product ID</th>
                    <th className="px-4 py-2 text-left">Discovery Reason</th>
                    <th className="px-4 py-2 text-center">Evaluated?</th>
                    <th className="px-4 py-2 text-center">Reused?</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {candidates.length > 0 ? (
                    candidates.map((c) => (
                      <tr key={c.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2 font-mono font-bold text-slate-900">Product #{c.productId}</td>
                        <td className="px-4 py-2">
                          <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                            c.candidateReason === 'NEWLY_APPLICABLE'
                              ? 'bg-purple-100 text-purple-800 border border-purple-300'
                              : 'bg-indigo-100 text-indigo-800'
                          }`}>
                            {c.candidateReason}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-center">
                          {c.wasEvaluated ? (
                            <span className="text-emerald-600 font-semibold">&#10003; Yes</span>
                          ) : (
                            <span className="text-slate-400">No</span>
                          )}
                        </td>
                        <td className="px-4 py-2 text-center">
                          {c.wasReused ? <span className="text-blue-600 font-semibold">&#10003; Yes</span> : <span className="text-slate-400">No</span>}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-center text-slate-400">
                        No candidates recorded for this run.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Revalidation Run History */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200">
          <h3 className="text-sm font-bold text-slate-900">Revalidation Execution History</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
            <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3">Run ID</th>
                <th className="px-4 py-3">Strategy</th>
                <th className="px-4 py-3">Rule</th>
                <th className="px-4 py-3">Candidates (Phase A / B)</th>
                <th className="px-4 py-3">Evaluated / Reused</th>
                <th className="px-4 py-3">Duration</th>
                <th className="px-4 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {recentRuns.map((r) => (
                <tr
                  key={r.id}
                  onClick={() => handleSelectRun(r)}
                  className={`cursor-pointer hover:bg-slate-50 ${latestRun?.id === r.id ? 'bg-primary-50/50' : ''}`}
                >
                  <td className="px-4 py-3 font-mono font-bold text-slate-900">#{r.id}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded font-bold ${
                      r.runType === 'INCREMENTAL' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                    }`}>
                      {r.runType}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono">{r.triggerRuleCode || 'Rule'}</td>
                  <td className="px-4 py-3">
                    <strong>{r.candidateCount}</strong> ({r.previouslyAssociatedCount} / {r.newlyApplicableCount})
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-semibold text-emerald-700">{r.evaluationsPerformed}</span> / <span className="text-slate-500">{r.evaluationsReused}</span>
                  </td>
                  <td className="px-4 py-3 font-mono">{r.totalDurationMs} ms</td>
                  <td className="px-4 py-3 text-right">
                    <button className="text-primary-600 font-semibold">Inspect &rarr;</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
