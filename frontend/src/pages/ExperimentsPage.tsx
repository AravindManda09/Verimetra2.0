import React, { useEffect, useState } from 'react';
import {
  FlaskConical,
  Play,
  Download,
  CheckCircle2,
  AlertTriangle,
  TrendingUp,
  Clock,
  Layers,
  RefreshCw,
  FileSpreadsheet,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { getExperiments, runExperiment } from '../api/client';
import { ExperimentRun } from '../types';
import toast from 'react-hot-toast';

export const ExperimentsPage: React.FC = () => {
  const [experiments, setExperiments] = useState<ExperimentRun[]>([]);
  const [selectedExperiment, setSelectedExperiment] = useState<ExperimentRun | null>(null);
  const [loading, setLoading] = useState(false);

  // Experiment Config
  const [catalogueSize, setCatalogueSize] = useState<number>(1000);
  const [impactRate, setImpactRate] = useState<number>(25);
  const [scenario, setScenario] = useState<string>('APPLICABILITY_EXPANSION');
  const [randomSeed, setRandomSeed] = useState<number>(42);

  useEffect(() => {
    fetchExperiments();
  }, []);

  const fetchExperiments = async () => {
    try {
      const res = await getExperiments({ size: 10 });
      setExperiments(res.data.content);
      if (res.data.content.length > 0 && !selectedExperiment) {
        setSelectedExperiment(res.data.content[0]);
      }
    } catch {
      toast.error('Failed to load experiments history');
    }
  };

  const handleRunExperiment = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      toast.loading(`Running benchmark experiment (N=${catalogueSize}, ${scenario})...`, { id: 'exp' });
      const res = await runExperiment({
        catalogueSize,
        impactRate,
        scenario,
        randomSeed,
      });
      toast.success('Experiment benchmark executed successfully!', { id: 'exp' });
      setSelectedExperiment(res.data);
      fetchExperiments();
    } catch (err: any) {
      toast.error('Experiment failed: ' + (err.response?.data?.message || err.message), { id: 'exp' });
    } finally {
      setLoading(false);
    }
  };

  const chartData = selectedExperiment
    ? [
        {
          name: 'Evaluations Count',
          Full: selectedExperiment.fullEvaluations,
          Incremental: selectedExperiment.incrEvaluations,
        },
        {
          name: 'Execution Time (ms)',
          Full: selectedExperiment.fullDurationMs,
          Incremental: selectedExperiment.incrTotalDurationMs,
        },
      ]
    : [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-2 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Experimental Evaluation & Benchmarking</h1>
        <p className="text-sm text-slate-500 mt-1">
          Measure correctness agreement and computational efficiency between Full and Incremental Revalidation
        </p>
      </div>

      {/* Config Form Card */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-200">
          <h3 className="text-base font-bold text-slate-900 flex items-center">
            <FlaskConical className="w-5 h-5 mr-2 text-primary-600" />
            Configure Synthetic Experiment Benchmark
          </h3>
          <span className="text-xs text-slate-400 font-mono">Reproducible Random Seed: {randomSeed}</span>
        </div>

        <form onSubmit={handleRunExperiment} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Catalogue Size (N)</label>
            <select
              value={catalogueSize}
              onChange={(e) => setCatalogueSize(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white font-medium"
            >
              <option value={100}>100 products (Fast smoke test)</option>
              <option value={1000}>1,000 products (Standard benchmark)</option>
              <option value={10000}>10,000 products (Stress benchmark)</option>
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Impact Rate (%)</label>
            <select
              value={impactRate}
              onChange={(e) => setImpactRate(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white font-medium"
            >
              <option value={5}>5% (Targeted change)</option>
              <option value={25}>25% (Moderate impact)</option>
              <option value={50}>50% (High impact)</option>
              <option value={90}>90% (Broad impact)</option>
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Revalidation Scenario</label>
            <select
              value={scenario}
              onChange={(e) => setScenario(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white font-medium"
            >
              <option value="APPLICABILITY_EXPANSION">Applicability Expansion (FOOD &rarr; +COSMETICS)</option>
              <option value="APPLICABILITY_NARROWING">Applicability Narrowing (Removed categories)</option>
              <option value="REQUIREMENT_MODIFICATION">Requirement Modification (New mandatory field)</option>
              <option value="THRESHOLD_MODIFICATION">Threshold Modification (Price / quantity constraint)</option>
            </select>
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 text-white font-bold rounded-md shadow-sm transition-colors flex items-center justify-center"
            >
              <Play className="w-4 h-4 mr-1.5" />
              {loading ? 'Executing...' : 'Run Experiment'}
            </button>
          </div>
        </form>
      </div>

      {/* Selected Experiment Results */}
      {selectedExperiment && (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-slate-200 gap-2">
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-xs font-bold text-slate-400">EXPERIMENT #{selectedExperiment.id}</span>
                <span className="text-xs bg-slate-100 px-2 py-0.5 rounded font-mono">N={selectedExperiment.catalogueSize}</span>
                <span className="text-xs bg-purple-100 text-purple-800 px-2 py-0.5 rounded font-bold">
                  {selectedExperiment.scenario}
                </span>
              </div>
              <h2 className="text-lg font-bold text-slate-900 mt-1">{selectedExperiment.experimentName}</h2>
            </div>

            <div className="flex items-center space-x-2">
              <a
                href={`/api/experiments/${selectedExperiment.id}/export/csv`}
                download
                className="inline-flex items-center px-3 py-1.5 border border-slate-300 rounded text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                <Download className="w-3.5 h-3.5 mr-1" /> Export CSV
              </a>
              <a
                href={`/api/experiments/${selectedExperiment.id}/export/json`}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center px-3 py-1.5 border border-slate-300 rounded text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                JSON
              </a>
            </div>
          </div>

          {/* Research Metrics Top Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-700">Correctness Agreement</span>
              <div className="text-2xl font-bold text-emerald-900 mt-1">
                {selectedExperiment.resultAgreementPct?.toFixed(1)}%
              </div>
              <span className="inline-block mt-1 text-[10px] bg-emerald-200 text-emerald-900 font-bold px-2 py-0.5 rounded">
                {selectedExperiment.correctnessStatus}
              </span>
            </div>

            <div className="p-4 bg-purple-50 border border-purple-200 rounded-xl">
              <span className="text-xs font-bold uppercase tracking-wider text-purple-700">Evaluation Work Reduction</span>
              <div className="text-2xl font-bold text-purple-900 mt-1">
                {selectedExperiment.evaluationReductionPct?.toFixed(1)}%
              </div>
              <p className="text-[11px] text-purple-700 mt-1">
                {selectedExperiment.incrEvaluations} evaluated vs {selectedExperiment.fullEvaluations} full
              </p>
            </div>

            <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-700">Time Reduction</span>
              <div className="text-2xl font-bold text-blue-900 mt-1">
                {selectedExperiment.timeReductionPct?.toFixed(1)}%
              </div>
              <p className="text-[11px] text-blue-700 mt-1">
                {selectedExperiment.incrTotalDurationMs} ms vs {selectedExperiment.fullDurationMs} ms
              </p>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-600">Candidate Overhead</span>
              <div className="text-2xl font-bold text-slate-900 mt-1">
                {selectedExperiment.incrCandidateSelectionMs} ms
              </div>
              <p className="text-[11px] text-slate-500 mt-1">
                Discovered {selectedExperiment.incrCandidates} candidates (A:{selectedExperiment.incrPreviouslyAssociated} + B:{selectedExperiment.incrNewlyApplicable})
              </p>
            </div>
          </div>

          {/* Comparative Chart */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-4">
                Strategy Comparison: Full vs Incremental
              </h4>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <XAxis dataKey="name" fontSize={11} stroke="#64748b" />
                    <YAxis fontSize={11} stroke="#64748b" />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="Full" fill="#94a3b8" name="Full Strategy" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Incremental" fill="#3b82f6" name="Incremental Strategy" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Canonical Agreement Breakdown */}
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3 text-xs">
              <h4 className="font-bold text-slate-800 uppercase tracking-wider">
                Canonical Result Agreement Breakdown
              </h4>
              <p className="text-slate-500 leading-relaxed text-[11px]">
                Compares canonicalized logical outcomes between independent Full and Incremental executions (independent of database IDs or timestamps).
              </p>

              <div className="space-y-2 pt-2">
                <div className="flex items-center justify-between p-2.5 bg-white rounded border border-slate-200">
                  <span className="font-medium text-slate-700">Matching Logical Outcomes</span>
                  <span className="font-mono font-bold text-emerald-600">{selectedExperiment.matchingOutcomes}</span>
                </div>
                <div className="flex items-center justify-between p-2.5 bg-white rounded border border-slate-200">
                  <span className="font-medium text-slate-700">Differing Outcomes</span>
                  <span className="font-mono font-bold text-red-600">{selectedExperiment.differingOutcomes}</span>
                </div>
                <div className="flex items-center justify-between p-2.5 bg-white rounded border border-slate-200">
                  <span className="font-medium text-slate-700">Missed Affected Products</span>
                  <span className="font-mono font-bold text-amber-600">{selectedExperiment.missingOutcomes}</span>
                </div>
                <div className="flex items-center justify-between p-2.5 bg-white rounded border border-slate-200">
                  <span className="font-medium text-slate-700">Extra Unexpected Outcomes</span>
                  <span className="font-mono font-bold text-slate-600">{selectedExperiment.extraOutcomes}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Historical Experiments Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200">
          <h3 className="text-sm font-bold text-slate-900">Historical Benchmark Runs</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
            <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Scenario</th>
                <th className="px-4 py-3">N (Catalogue)</th>
                <th className="px-4 py-3">Agreement %</th>
                <th className="px-4 py-3">Work Reduction</th>
                <th className="px-4 py-3">Time Reduction</th>
                <th className="px-4 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {experiments.map((exp) => (
                <tr
                  key={exp.id}
                  onClick={() => setSelectedExperiment(exp)}
                  className={`cursor-pointer hover:bg-slate-50 ${selectedExperiment?.id === exp.id ? 'bg-primary-50/50' : ''}`}
                >
                  <td className="px-4 py-3 font-mono font-bold text-slate-900">#{exp.id}</td>
                  <td className="px-4 py-3 font-medium text-slate-800">{exp.scenario}</td>
                  <td className="px-4 py-3 font-mono">{exp.catalogueSize}</td>
                  <td className="px-4 py-3 font-semibold text-emerald-600">{exp.resultAgreementPct?.toFixed(1)}%</td>
                  <td className="px-4 py-3 font-semibold text-purple-600">{exp.evaluationReductionPct?.toFixed(1)}%</td>
                  <td className="px-4 py-3 font-semibold text-blue-600">{exp.timeReductionPct?.toFixed(1)}%</td>
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
