import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Package,
  Scale,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  FlaskConical,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { getDashboardSummary, getDashboardCharts } from '../api/client';
import toast from 'react-hot-toast';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export const OverviewPage: React.FC = () => {
  const [summary, setSummary] = useState<any>(null);
  const [charts, setCharts] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [sumRes, chartRes] = await Promise.all([
        getDashboardSummary(),
        getDashboardCharts(),
      ]);
      setSummary(sumRes.data);
      setCharts(chartRes.data);
    } catch (err: any) {
      toast.error('Failed to load dashboard metrics');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-slate-500 font-medium">
        <RefreshCw className="w-6 h-6 animate-spin mr-2 text-primary-600" />
        Loading dashboard metrics...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Compliance Intelligence Dashboard</h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time regulatory compliance posture under Legal Metrology (PC) Rules
          </p>
        </div>
        <div className="mt-3 sm:mt-0 flex space-x-2">
          <Link
            to="/revalidation"
            className="inline-flex items-center px-3.5 py-2 border border-slate-300 shadow-sm text-xs font-semibold rounded-md text-slate-700 bg-white hover:bg-slate-50"
          >
            <RefreshCw className="w-4 h-4 mr-1.5 text-slate-500" />
            Revalidation
          </Link>
          <Link
            to="/experiments"
            className="inline-flex items-center px-3.5 py-2 border border-transparent shadow-sm text-xs font-semibold rounded-md text-white bg-primary-600 hover:bg-primary-700"
          >
            <FlaskConical className="w-4 h-4 mr-1.5" />
            Run Benchmarks
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Products</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{summary?.totalProducts ?? 0}</h3>
              <p className="text-xs text-slate-400 mt-0.5">Across 4 categories</p>
            </div>
            <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
              <Package className="w-6 h-6" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Rules</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{summary?.activeRuleVersions ?? 0}</h3>
              <p className="text-xs text-slate-400 mt-0.5">{summary?.totalRules ?? 0} total rule definitions</p>
            </div>
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
              <Scale className="w-6 h-6" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Evaluations Run</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{summary?.totalEvaluations ?? 0}</h3>
              <p className="text-xs text-slate-400 mt-0.5">Deterministic checks</p>
            </div>
            <div className="p-3 bg-purple-50 text-purple-600 rounded-lg">
              <CheckCircle2 className="w-6 h-6" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Findings / Issues</p>
              <h3 className="text-2xl font-bold text-amber-600 mt-1">{summary?.totalFindings ?? 0}</h3>
              <p className="text-xs text-amber-700 font-medium mt-0.5">{summary?.unreviewedFindings ?? 0} unreviewed</p>
            </div>
            <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
              <AlertTriangle className="w-6 h-6" />
            </div>
          </div>
        </div>
      </div>

      {/* Visual Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Products by Category */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <h3 className="text-base font-semibold text-slate-900 mb-4">Products by Regulatory Category</h3>
          <div className="h-64">
            {charts?.productsByCategory?.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={charts.productsByCategory}>
                  <XAxis dataKey="name" stroke="#64748b" fontSize={12} />
                  <YAxis stroke="#64748b" fontSize={12} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Products" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-400 text-sm">No category data yet</div>
            )}
          </div>
        </div>

        {/* Findings by Severity */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <h3 className="text-base font-semibold text-slate-900 mb-4">Findings Distribution by Severity</h3>
          <div className="h-64">
            {charts?.findingsBySeverity?.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={charts.findingsBySeverity}
                    dataKey="count"
                    nameKey="severity"
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                    label={(entry) => `${entry.severity}: ${entry.count}`}
                  >
                    {charts.findingsBySeverity.map((_: any, index: number) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-400 text-sm">No findings recorded</div>
            )}
          </div>
        </div>
      </div>

      {/* Recent Revalidation Runs */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-slate-900">Recent Revalidation Runs</h3>
            <p className="text-xs text-slate-500">History of Full and Incremental Revalidation executions</p>
          </div>
          <Link to="/revalidation" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center">
            View All <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3">Run ID</th>
                <th className="px-5 py-3">Strategy</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Candidates</th>
                <th className="px-5 py-3">Evaluations</th>
                <th className="px-5 py-3">Reused</th>
                <th className="px-5 py-3">Duration</th>
                <th className="px-5 py-3">Fallback?</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {charts?.recentRevalidations?.length > 0 ? (
                charts.recentRevalidations.map((run: any) => (
                  <tr key={run.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3 font-medium text-slate-900">#{run.id}</td>
                    <td className="px-5 py-3">
                      <span className={`px-2 py-0.5 rounded font-semibold ${
                        run.runType === 'INCREMENTAL' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                      }`}>
                        {run.runType}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-medium">
                        {run.status}
                      </span>
                    </td>
                    <td className="px-5 py-3">{run.candidateCount}</td>
                    <td className="px-5 py-3 font-semibold">{run.evaluationsPerformed}</td>
                    <td className="px-5 py-3 text-slate-500">{run.evaluationsReused}</td>
                    <td className="px-5 py-3 font-mono">{run.totalDurationMs} ms</td>
                    <td className="px-5 py-3">
                      {run.usedFallback ? (
                        <span className="text-amber-700 bg-amber-50 px-2 py-0.5 rounded font-medium">Yes</span>
                      ) : (
                        <span className="text-slate-400">No</span>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} className="px-5 py-8 text-center text-slate-400">
                    No revalidation runs executed yet. Visit the Revalidation tab to run.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
