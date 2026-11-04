import React, { useEffect, useState } from 'react';
import {
  Upload,
  FileText,
  CheckCircle2,
  AlertCircle,
  Eye,
  RefreshCw,
  Edit2,
  Trash2,
  Check,
  Plus,
} from 'lucide-react';
import {
  getProducts,
  getEvidenceList,
  uploadEvidence,
  runOcrExtraction,
  getOcrExtraction,
  confirmExtraction,
} from '../api/client';
import { Product, PackageEvidence, OcrExtraction, ExtractedField } from '../types';
import toast from 'react-hot-toast';

export const EvidencePage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [evidenceList, setEvidenceList] = useState<PackageEvidence[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<number | ''>('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  // Active Evidence Inspection
  const [activeEvidence, setActiveEvidence] = useState<PackageEvidence | null>(null);
  const [activeExtraction, setActiveExtraction] = useState<OcrExtraction | null>(null);
  const [extracting, setExtracting] = useState(false);

  // Field Review State
  const [reviewFields, setReviewFields] = useState<{
    id?: number;
    fieldName: string;
    suggestedValue: string;
    confirmedValue: string;
    action: 'CONFIRM' | 'EDIT' | 'REJECT';
    confidence?: number;
  }[]>([]);

  // Add field form
  const [newFieldName, setNewFieldName] = useState('mrp');
  const [newFieldValue, setNewFieldValue] = useState('');

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      const [prodRes, evRes] = await Promise.all([
        getProducts({ size: 100 }),
        getEvidenceList(),
      ]);
      setProducts(prodRes.data.content);
      setEvidenceList(evRes.data);
      if (prodRes.data.content.length > 0) {
        setSelectedProductId(prodRes.data.content[0].id);
      }
    } catch (err) {
      toast.error('Failed to load evidence data');
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile || !selectedProductId) {
      toast.error('Please select a product and file');
      return;
    }

    try {
      setUploading(true);
      const res = await uploadEvidence(Number(selectedProductId), uploadFile);
      toast.success('Package image uploaded successfully!');
      setUploadFile(null);
      const updatedList = await getEvidenceList();
      setEvidenceList(updatedList.data);
      handleInspectEvidence(res.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleInspectEvidence = async (evidence: PackageEvidence) => {
    setActiveEvidence(evidence);
    try {
      const res = await getOcrExtraction(evidence.id);
      if (res.status === 200 && res.data) {
        setActiveExtraction(res.data);
        populateReviewFields(res.data.extractedFields);
      } else {
        setActiveExtraction(null);
        setReviewFields([]);
      }
    } catch {
      setActiveExtraction(null);
      setReviewFields([]);
    }
  };

  const populateReviewFields = (fields: ExtractedField[]) => {
    setReviewFields(
      fields.map((f) => ({
        id: f.id,
        fieldName: f.fieldName,
        suggestedValue: f.suggestedValue,
        confirmedValue: f.confirmedValue || f.suggestedValue,
        action: f.reviewStatus === 'REJECTED' ? 'REJECT' : f.reviewStatus === 'EDITED' ? 'EDIT' : 'CONFIRM',
        confidence: f.confidence,
      }))
    );
  };

  const handleRunOcr = async (evidenceId: number) => {
    try {
      setExtracting(true);
      toast.loading('Invoking OCR extraction engine...', { id: 'ocr' });
      const res = await runOcrExtraction(evidenceId);
      setActiveExtraction(res.data);
      populateReviewFields(res.data.extractedFields);
      toast.success('OCR extraction complete!', { id: 'ocr' });

      // Refresh list
      const updated = await getEvidenceList();
      setEvidenceList(updated.data);
    } catch (err: any) {
      toast.error('OCR processing error: ' + (err.response?.data?.message || err.message), { id: 'ocr' });
    } finally {
      setExtracting(false);
    }
  };

  const handleAddField = () => {
    if (!newFieldValue.trim()) return;
    setReviewFields([
      ...reviewFields,
      {
        fieldName: newFieldName,
        suggestedValue: newFieldValue,
        confirmedValue: newFieldValue,
        action: 'CONFIRM',
        confidence: 1.0,
      },
    ]);
    setNewFieldValue('');
  };

  const handleConfirmAll = async () => {
    if (!activeEvidence) return;
    try {
      toast.loading('Saving confirmed declarations to product...', { id: 'confirm' });
      const payload = {
        productId: activeEvidence.productId,
        fields: reviewFields.map((f) => ({
          extractedFieldId: f.id,
          fieldName: f.fieldName,
          confirmedValue: f.confirmedValue,
          action: f.action,
        })),
      };

      await confirmExtraction(activeEvidence.id, payload);
      toast.success('Declarations confirmed and saved as PACKAGE declarations!', { id: 'confirm' });
      handleInspectEvidence(activeEvidence);
    } catch (err) {
      toast.error('Failed to confirm extractions', { id: 'confirm' });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-2 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Package Evidence & OCR Extraction</h1>
        <p className="text-sm text-slate-500 mt-1">
          Upload product packaging images, extract text with local OCR, review suggestions, and confirm package declarations
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload & Evidence List */}
        <div className="space-y-6">
          {/* Upload Card */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center">
              <Upload className="w-4 h-4 mr-2 text-primary-600" />
              Upload Package Image
            </h3>

            <form onSubmit={handleUpload} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Target Product</label>
                <select
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white"
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      #{p.id} — {p.productName} ({p.category})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Select File (JPG, PNG, WEBP)</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                  className="w-full text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
                />
              </div>

              <button
                type="submit"
                disabled={uploading || !uploadFile}
                className="w-full py-2 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 text-white font-semibold rounded-md shadow-sm transition-colors"
              >
                {uploading ? 'Uploading...' : 'Upload Evidence'}
              </button>
            </form>
          </div>

          {/* Evidence List */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-200">
              <h3 className="text-sm font-bold text-slate-900">Uploaded Evidence ({evidenceList.length})</h3>
            </div>
            <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto">
              {evidenceList.length > 0 ? (
                evidenceList.map((ev) => (
                  <div
                    key={ev.id}
                    onClick={() => handleInspectEvidence(ev)}
                    className={`p-3 text-xs cursor-pointer hover:bg-blue-50/50 transition-colors ${
                      activeEvidence?.id === ev.id ? 'bg-primary-50/70 border-l-4 border-primary-600' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-900 truncate max-w-[180px]">{ev.originalFilename}</span>
                      <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                        ev.uploadStatus === 'EXTRACTED' ? 'bg-emerald-100 text-emerald-800' :
                        ev.uploadStatus === 'PROCESSING' ? 'bg-blue-100 text-blue-800' :
                        'bg-slate-100 text-slate-600'
                      }`}>
                        {ev.uploadStatus}
                      </span>
                    </div>
                    <div className="text-[11px] text-slate-400 mt-1 flex justify-between">
                      <span>Product #{ev.productId}</span>
                      <span>{(ev.fileSizeBytes / 1024).toFixed(1)} KB</span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-slate-400 text-xs">No packaging evidence uploaded yet.</div>
              )}
            </div>
          </div>
        </div>

        {/* OCR Inspection & Review Panel */}
        <div className="lg:col-span-2 space-y-6">
          {activeEvidence ? (
            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-6">
              <div className="flex items-start justify-between pb-3 border-b border-slate-200">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-bold text-slate-400">EVIDENCE #{activeEvidence.id}</span>
                    <span className="text-xs bg-slate-100 px-2 py-0.5 rounded font-mono">Product #{activeEvidence.productId}</span>
                  </div>
                  <h2 className="text-lg font-bold text-slate-900 mt-0.5">{activeEvidence.originalFilename}</h2>
                </div>

                <button
                  onClick={() => handleRunOcr(activeEvidence.id)}
                  disabled={extracting}
                  className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white text-xs font-semibold rounded-md shadow-sm"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${extracting ? 'animate-spin' : ''}`} />
                  {extracting ? 'Running OCR...' : 'Run OCR Extraction'}
                </button>
              </div>

              {/* Raw OCR Output */}
              {activeExtraction && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Raw Extracted Text</h4>
                    <span className="text-[11px] text-slate-400 font-mono">
                      Status: {activeExtraction.extractionStatus} | Time: {activeExtraction.processingTimeMs || 0} ms
                    </span>
                  </div>
                  <div className="bg-slate-900 text-emerald-400 font-mono text-xs p-3 rounded-lg max-h-36 overflow-y-auto whitespace-pre-wrap">
                    {activeExtraction.rawText || '(No text extracted from image)'}
                  </div>
                </div>
              )}

              {/* Human-in-the-Loop Review Table */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">Extracted Declaration Review</h4>
                    <p className="text-xs text-slate-500">
                      OCR suggestions are provisional. Review, correct, and confirm before compliance evaluation.
                    </p>
                  </div>
                </div>

                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="min-w-full text-xs divide-y divide-slate-200">
                    <thead className="bg-slate-50 font-semibold text-slate-600">
                      <tr>
                        <th className="px-3 py-2 text-left">Declaration Field</th>
                        <th className="px-3 py-2 text-left">OCR Suggested Value</th>
                        <th className="px-3 py-2 text-left">Confirmed Value</th>
                        <th className="px-3 py-2 text-center">Confidence</th>
                        <th className="px-3 py-2 text-center">Review Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {reviewFields.length > 0 ? (
                        reviewFields.map((f, idx) => (
                          <tr key={idx} className="hover:bg-slate-50">
                            <td className="px-3 py-2.5 font-mono font-medium text-slate-800">{f.fieldName}</td>
                            <td className="px-3 py-2.5 text-slate-600 font-mono">{f.suggestedValue}</td>
                            <td className="px-3 py-2.5">
                              <input
                                type="text"
                                value={f.confirmedValue}
                                onChange={(e) => {
                                  const updated = [...reviewFields];
                                  updated[idx].confirmedValue = e.target.value;
                                  updated[idx].action = 'EDIT';
                                  setReviewFields(updated);
                                }}
                                className="w-full px-2 py-1 text-xs border border-slate-300 rounded focus:ring-1 focus:ring-primary-500"
                              />
                            </td>
                            <td className="px-3 py-2.5 text-center font-mono">
                              {f.confidence != null ? `${(f.confidence * 100).toFixed(0)}%` : '—'}
                            </td>
                            <td className="px-3 py-2.5 text-center">
                              <select
                                value={f.action}
                                onChange={(e) => {
                                  const updated = [...reviewFields];
                                  updated[idx].action = e.target.value as any;
                                  setReviewFields(updated);
                                }}
                                className={`px-2 py-1 text-xs rounded font-bold border ${
                                  f.action === 'CONFIRM' ? 'bg-emerald-50 text-emerald-700 border-emerald-300' :
                                  f.action === 'EDIT' ? 'bg-blue-50 text-blue-700 border-blue-300' :
                                  'bg-red-50 text-red-700 border-red-300'
                                }`}
                              >
                                <option value="CONFIRM">Confirm</option>
                                <option value="EDIT">Edit</option>
                                <option value="REJECT">Reject</option>
                              </select>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={5} className="px-3 py-6 text-center text-slate-400">
                            No extracted fields. Run OCR or manually add declaration values below.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Add Custom Declaration */}
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 flex flex-col sm:flex-row items-center gap-2 text-xs">
                  <span className="font-semibold text-slate-700 flex-shrink-0">Add Declaration:</span>
                  <select
                    value={newFieldName}
                    onChange={(e) => setNewFieldName(e.target.value)}
                    className="px-2.5 py-1.5 border border-slate-300 rounded bg-white text-xs"
                  >
                    <option value="mrp">mrp</option>
                    <option value="net_quantity">net_quantity</option>
                    <option value="manufacturer_name">manufacturer_name</option>
                    <option value="country_of_origin">country_of_origin</option>
                    <option value="product_name">product_name</option>
                    <option value="manufacture_date">manufacture_date</option>
                  </select>
                  <input
                    type="text"
                    placeholder="Value (e.g. 240.00)"
                    value={newFieldValue}
                    onChange={(e) => setNewFieldValue(e.target.value)}
                    className="flex-1 px-3 py-1.5 border border-slate-300 rounded text-xs"
                  />
                  <button
                    type="button"
                    onClick={handleAddField}
                    className="px-3 py-1.5 bg-slate-800 text-white rounded font-medium hover:bg-slate-900"
                  >
                    Add
                  </button>
                </div>

                <div className="pt-3 border-t border-slate-200 flex justify-end">
                  <button
                    type="button"
                    onClick={handleConfirmAll}
                    disabled={reviewFields.length === 0}
                    className="inline-flex items-center px-4 py-2 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 text-white text-xs font-semibold rounded-md shadow-sm"
                  >
                    <CheckCircle2 className="w-4 h-4 mr-1.5" />
                    Confirm & Save as Package Declarations
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white p-12 rounded-xl border border-slate-200 text-center text-slate-400">
              <Eye className="w-10 h-10 mx-auto text-slate-300 mb-3" />
              <h3 className="text-base font-semibold text-slate-700">No Evidence Selected</h3>
              <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                Select an evidence item from the list on the left to inspect OCR output and review declarations.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
