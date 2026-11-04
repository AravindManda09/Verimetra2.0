import React, { useEffect, useState } from 'react';
import {
  Scale,
  Plus,
  GitCompare,
  History,
  CheckCircle2,
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  X,
  Play,
} from 'lucide-react';
import {
  getRules,
  createRule,
  createRuleVersion,
  activateRuleVersion,
  compareRuleVersions,
  getRuleVersions,
} from '../api/client';
import { ComplianceRule, RuleVersion } from '../types';
import toast from 'react-hot-toast';

export const RulesPage: React.FC = () => {
  const [rules, setRules] = useState<ComplianceRule[]>([]);
  const [selectedRule, setSelectedRule] = useState<ComplianceRule | null>(null);
  const [ruleVersions, setRuleVersions] = useState<RuleVersion[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [showCreateRuleModal, setShowCreateRuleModal] = useState(false);
  const [showNewVersionModal, setShowNewVersionModal] = useState(false);
  const [showCompareModal, setShowCompareModal] = useState(false);
  const [compareData, setCompareData] = useState<any>(null);
  const [compareV1, setCompareV1] = useState<number | ''>('');
  const [compareV2, setCompareV2] = useState<number | ''>('');

  // New Version Form
  const [newVersion, setNewVersion] = useState({
    requirementType: 'MANDATORY_DECLARATION',
    requiredField: 'mrp',
    validationOperator: 'NOT_EMPTY',
    expectedValue: '',
    severity: 'CRITICAL',
    effectiveFrom: new Date().toISOString().split('T')[0],
    changeSummary: '',
    applicableCategories: ['FOOD', 'COSMETICS'], // Default demo: expanding to cosmetics
  });

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    try {
      setLoading(true);
      const res = await getRules();
      setRules(res.data);
      if (res.data.length > 0 && !selectedRule) {
        handleSelectRule(res.data[0]);
      }
    } catch {
      toast.error('Failed to load rules');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectRule = async (rule: ComplianceRule) => {
    setSelectedRule(rule);
    try {
      const res = await getRuleVersions(rule.id);
      setRuleVersions(res.data);
      if (res.data.length >= 2) {
        setCompareV1(res.data[1].id);
        setCompareV2(res.data[0].id);
      }
    } catch {
      toast.error('Failed to load version history');
    }
  };

  const handleCreateNewVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRule) return;

    try {
      toast.loading('Creating new immutable rule version...', { id: 'ver' });
      await createRuleVersion(selectedRule.id, newVersion);
      toast.success('Version created! You can now activate it.', { id: 'ver' });
      setShowNewVersionModal(false);
      handleSelectRule(selectedRule);
      fetchRules();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create version', { id: 'ver' });
    }
  };

  const handleActivateVersion = async (versionId: number) => {
    if (!selectedRule) return;
    try {
      toast.loading('Activating version and superseding previous...', { id: 'act' });
      await activateRuleVersion(selectedRule.id, versionId);
      toast.success('Version activated! System is now ready for Incremental Revalidation.', { id: 'act' });
      handleSelectRule(selectedRule);
      fetchRules();
    } catch (err: any) {
      toast.error('Failed to activate version', { id: 'act' });
    }
  };

  const handleRunCompare = async () => {
    if (!selectedRule || !compareV1 || !compareV2) return;
    try {
      const res = await compareRuleVersions(selectedRule.id, Number(compareV1), Number(compareV2));
      setCompareData(res.data);
      setShowCompareModal(true);
    } catch {
      toast.error('Failed to compare versions');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Compliance Rule Repository</h1>
          <p className="text-sm text-slate-500 mt-1">
            Versioned regulatory requirements under India Legal Metrology (PC) Rules, 2011
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Rule List */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Defined Compliance Rules</h3>
          <div className="space-y-2">
            {rules.map((r) => (
              <div
                key={r.id}
                onClick={() => handleSelectRule(r)}
                className={`p-4 rounded-xl border text-xs cursor-pointer transition-all ${
                  selectedRule?.id === r.id
                    ? 'bg-primary-50/70 border-primary-500 shadow-sm'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-mono font-bold text-primary-700">{r.ruleCode}</span>
                  {r.isIllustrative ? (
                    <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-medium">
                      Analytical / Illustrative
                    </span>
                  ) : (
                    <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded font-bold">
                      Verified Legal Rule
                    </span>
                  )}
                </div>
                <h4 className="font-bold text-slate-900 text-sm">{r.ruleName}</h4>
                <p className="text-slate-500 text-[11px] mt-1 line-clamp-2">{r.description}</p>
                <div className="mt-2 text-[11px] text-slate-400">
                  Active Version: <strong>v{r.activeVersion?.versionNumber || 1}</strong>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Selected Rule Details & Version History */}
        <div className="lg:col-span-2 space-y-6">
          {selectedRule && (
            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-6">
              {/* Rule Header */}
              <div className="flex items-start justify-between pb-4 border-b border-slate-200">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-mono font-bold text-xs bg-slate-100 px-2 py-0.5 rounded">{selectedRule.ruleCode}</span>
                    <span className="text-xs text-slate-400">Legal Reference:</span>
                    <span className="text-xs font-semibold text-slate-700">{selectedRule.legalReference || 'Internal Check'}</span>
                  </div>
                  <h2 className="text-xl font-bold text-slate-900 mt-1">{selectedRule.ruleName}</h2>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">{selectedRule.description}</p>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => {
                      setNewVersion({
                        requirementType: selectedRule.activeVersion?.requirementType || 'MANDATORY_DECLARATION',
                        requiredField: selectedRule.activeVersion?.requiredField || 'mrp',
                        validationOperator: selectedRule.activeVersion?.validationOperator || 'NOT_EMPTY',
                        expectedValue: selectedRule.activeVersion?.expectedValue || '',
                        severity: selectedRule.activeVersion?.severity || 'CRITICAL',
                        effectiveFrom: new Date().toISOString().split('T')[0],
                        changeSummary: 'Expanded applicability to include COSMETICS category',
                        applicableCategories: ['FOOD', 'COSMETICS'],
                      });
                      setShowNewVersionModal(true);
                    }}
                    className="inline-flex items-center px-3 py-2 bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold rounded-md shadow-sm"
                  >
                    <Plus className="w-3.5 h-3.5 mr-1" />
                    Create Version
                  </button>
                </div>
              </div>

              {/* Version Comparison Selector */}
              {ruleVersions.length >= 2 && (
                <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
                  <div className="flex items-center space-x-2">
                    <GitCompare className="w-4 h-4 text-primary-600" />
                    <span className="font-semibold text-slate-800">Compare Rule Versions:</span>
                    <select
                      value={compareV1}
                      onChange={(e) => setCompareV1(Number(e.target.value))}
                      className="px-2 py-1 border border-slate-300 rounded bg-white"
                    >
                      {ruleVersions.map((v) => (
                        <option key={v.id} value={v.id}>
                          v{v.versionNumber} ({v.status})
                        </option>
                      ))}
                    </select>
                    <span>vs</span>
                    <select
                      value={compareV2}
                      onChange={(e) => setCompareV2(Number(e.target.value))}
                      className="px-2 py-1 border border-slate-300 rounded bg-white"
                    >
                      {ruleVersions.map((v) => (
                        <option key={v.id} value={v.id}>
                          v{v.versionNumber} ({v.status})
                        </option>
                      ))}
                    </select>
                  </div>
                  <button
                    onClick={handleRunCompare}
                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-900 text-white font-medium rounded shadow-sm"
                  >
                    Compare Diff &rarr;
                  </button>
                </div>
              )}

              {/* Version History Table */}
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-slate-900 flex items-center">
                  <History className="w-4 h-4 mr-1.5 text-slate-500" />
                  Version History (Immutable Snapshots)
                </h4>

                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="min-w-full text-xs divide-y divide-slate-200">
                    <thead className="bg-slate-50 font-semibold text-slate-600">
                      <tr>
                        <th className="px-3 py-2 text-left">Version</th>
                        <th className="px-3 py-2 text-left">Type & Field</th>
                        <th className="px-3 py-2 text-left">Operator / Expected</th>
                        <th className="px-3 py-2 text-left">Applicable Categories</th>
                        <th className="px-3 py-2 text-left">Status</th>
                        <th className="px-3 py-2 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {ruleVersions.map((v) => (
                        <tr key={v.id} className="hover:bg-slate-50">
                          <td className="px-3 py-2.5 font-bold text-slate-900">
                            v{v.versionNumber}
                            <div className="text-[10px] text-slate-400 font-normal">{v.effectiveFrom}</div>
                          </td>
                          <td className="px-3 py-2.5">
                            <div className="font-semibold text-slate-800">{v.requirementType}</div>
                            <div className="text-[11px] text-slate-500 font-mono">Field: {v.requiredField}</div>
                          </td>
                          <td className="px-3 py-2.5 font-mono">
                            <span className="font-semibold text-primary-700">{v.validationOperator}</span>
                            {v.expectedValue && <span className="text-slate-500 ml-1">[{v.expectedValue}]</span>}
                          </td>
                          <td className="px-3 py-2.5">
                            <div className="flex flex-wrap gap-1">
                              {v.applicableCategories.map((c) => (
                                <span key={c} className="text-[10px] bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded font-medium">
                                  {c}
                                </span>
                              ))}
                            </div>
                          </td>
                          <td className="px-3 py-2.5">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              v.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' :
                              v.status === 'SUPERSEDED' ? 'bg-amber-100 text-amber-800' :
                              'bg-slate-100 text-slate-600'
                            }`}>
                              {v.status}
                            </span>
                          </td>
                          <td className="px-3 py-2.5 text-right">
                            {v.status !== 'ACTIVE' ? (
                              <button
                                onClick={() => handleActivateVersion(v.id)}
                                className="text-xs text-primary-600 hover:text-primary-800 font-semibold"
                              >
                                Activate &rarr;
                              </button>
                            ) : (
                              <span className="text-xs text-emerald-600 font-semibold flex items-center justify-end">
                                <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Active
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Create Version Modal */}
      {showNewVersionModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900">Create New Version for {selectedRule?.ruleCode}</h3>
              <button onClick={() => setShowNewVersionModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateNewVersion} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Requirement Type</label>
                  <select
                    value={newVersion.requirementType}
                    onChange={(e) => setNewVersion({ ...newVersion, requirementType: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white"
                  >
                    <option value="MANDATORY_DECLARATION">MANDATORY_DECLARATION</option>
                    <option value="VALUE_CONSTRAINT">VALUE_CONSTRAINT</option>
                    <option value="CROSS_SOURCE_CONSISTENCY">CROSS_SOURCE_CONSISTENCY</option>
                    <option value="FORMAT_REQUIREMENT">FORMAT_REQUIREMENT</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Required Field</label>
                  <input
                    type="text"
                    required
                    value={newVersion.requiredField}
                    onChange={(e) => setNewVersion({ ...newVersion, requiredField: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Validation Operator</label>
                  <select
                    value={newVersion.validationOperator}
                    onChange={(e) => setNewVersion({ ...newVersion, validationOperator: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white font-mono"
                  >
                    <option value="NOT_EMPTY">NOT_EMPTY</option>
                    <option value="EQUALS">EQUALS</option>
                    <option value="LESS_THAN">LESS_THAN</option>
                    <option value="GREATER_THAN">GREATER_THAN</option>
                    <option value="CROSS_SOURCE_MATCH">CROSS_SOURCE_MATCH</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Expected Value / Threshold</label>
                  <input
                    type="text"
                    value={newVersion.expectedValue}
                    onChange={(e) => setNewVersion({ ...newVersion, expectedValue: e.target.value })}
                    placeholder="Optional threshold"
                    className="w-full px-3 py-2 border border-slate-300 rounded-md"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Applicable Categories (Check all that apply — Expansion Test)
                </label>
                <div className="flex gap-4 p-2.5 bg-slate-50 border border-slate-200 rounded-md">
                  {['FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD'].map((cat) => (
                    <label key={cat} className="flex items-center space-x-1.5 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newVersion.applicableCategories.includes(cat)}
                        onChange={(e) => {
                          const cats = e.target.checked
                            ? [...newVersion.applicableCategories, cat]
                            : newVersion.applicableCategories.filter((c) => c !== cat);
                          setNewVersion({ ...newVersion, applicableCategories: cats });
                        }}
                        className="rounded text-primary-600"
                      />
                      <span className="font-medium text-slate-700">{cat}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Change Summary / Notes</label>
                <textarea
                  rows={2}
                  required
                  value={newVersion.changeSummary}
                  onChange={(e) => setNewVersion({ ...newVersion, changeSummary: e.target.value })}
                  placeholder="e.g. Expanded applicability to COSMETICS as per amendment"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>

              <div className="pt-3 border-t border-slate-200 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowNewVersionModal(false)}
                  className="px-3 py-2 border border-slate-300 rounded-md text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-md shadow-sm"
                >
                  Create Version
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Compare Modal */}
      {showCompareModal && compareData && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900">
                Version Diff: v{compareData.version1.versionNumber} vs v{compareData.version2.versionNumber}
              </h3>
              <button onClick={() => setShowCompareModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              {compareData.hasChanges ? (
                compareData.changes.map((c: any, idx: number) => (
                  <div key={idx} className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-1">
                    <span className="font-bold text-slate-800 uppercase tracking-wider">{c.field}</span>
                    <div className="grid grid-cols-2 gap-2 mt-1">
                      <div className="p-2 bg-red-50 text-red-800 rounded border border-red-200">
                        <span className="font-semibold block text-[10px] text-red-600">Old Value (v{compareData.version1.versionNumber}):</span>
                        {JSON.stringify(c.oldValue)}
                      </div>
                      <div className="p-2 bg-emerald-50 text-emerald-800 rounded border border-emerald-200">
                        <span className="font-semibold block text-[10px] text-emerald-600">New Value (v{compareData.version2.versionNumber}):</span>
                        {JSON.stringify(c.newValue)}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-slate-500">No attribute differences between selected versions.</div>
              )}
            </div>

            <div className="pt-3 border-t border-slate-200 flex justify-end">
              <button
                onClick={() => setShowCompareModal(false)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white font-medium rounded-md"
              >
                Close Diff
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
