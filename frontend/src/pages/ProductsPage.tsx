import React, { useEffect, useState } from 'react';
import {
  Package,
  Plus,
  Search,
  Filter,
  CheckCircle2,
  AlertTriangle,
  FileText,
  Play,
  X,
  ExternalLink,
} from 'lucide-react';
import {
  getProducts,
  createProduct,
  getProductDeclarations,
  getProductEvaluations,
  runEvaluation,
  getEvidenceByProduct,
} from '../api/client';
import { Product, ProductDeclaration, Evaluation, PackageEvidence } from '../types';
import toast from 'react-hot-toast';

export const ProductsPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [category, setCategory] = useState<string>('');
  const [search, setSearch] = useState<string>('');
  const [loading, setLoading] = useState(true);

  // Modal & Drawer
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [declarations, setDeclarations] = useState<ProductDeclaration[]>([]);
  const [evaluations, setEvaluations] = useState<Evaluation[]>([]);
  const [evidenceList, setEvidenceList] = useState<PackageEvidence[]>([]);
  const [drawerLoading, setDrawerLoading] = useState(false);

  // New product form
  const [newProduct, setNewProduct] = useState({
    productName: '',
    sku: '',
    category: 'FOOD',
    manufacturerName: '',
    countryOfOrigin: 'India',
    description: '',
  });

  useEffect(() => {
    fetchProducts();
  }, [category, search]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const res = await getProducts({
        category: category || undefined,
        search: search || undefined,
        size: 50,
      });
      setProducts(res.data.content);
    } catch (err: any) {
      toast.error('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectProduct = async (product: Product) => {
    setSelectedProduct(product);
    setDrawerLoading(true);
    try {
      const [declRes, evalRes, evRes] = await Promise.all([
        getProductDeclarations(product.id),
        getProductEvaluations(product.id),
        getEvidenceByProduct(product.id),
      ]);
      setDeclarations(declRes.data);
      setEvaluations(evalRes.data);
      setEvidenceList(evRes.data);
    } catch (err) {
      toast.error('Failed to load product details');
    } finally {
      setDrawerLoading(false);
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await createProduct(newProduct as any);
      toast.success('Product registered successfully!');
      setShowCreateModal(false);
      setNewProduct({
        productName: '',
        sku: '',
        category: 'FOOD',
        manufacturerName: '',
        countryOfOrigin: 'India',
        description: '',
      });
      fetchProducts();
      handleSelectProduct(res.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to register product');
    }
  };

  const handleRunEvaluation = async (productId: number) => {
    try {
      toast.loading('Running deterministic evaluation against active rules...', { id: 'eval' });
      const res = await runEvaluation({ productId });
      setEvaluations(res.data);
      toast.success(`Evaluation complete! ${res.data.length} rules checked.`, { id: 'eval' });
    } catch (err) {
      toast.error('Failed to run evaluation', { id: 'eval' });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Product Catalogue</h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage commodities, listing declarations, packaging evidence, and compliance status
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="mt-3 sm:mt-0 inline-flex items-center px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold rounded-md shadow-sm"
        >
          <Plus className="w-4 h-4 mr-1.5" />
          Register Product
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
          <input
            type="text"
            placeholder="Search by product name or SKU..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs text-slate-500 font-medium">Category:</span>
          {['', 'FOOD', 'COSMETICS', 'ELECTRONICS', 'HOUSEHOLD'].map((cat) => (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                category === cat
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {cat || 'All'}
            </button>
          ))}
        </div>
      </div>

      {/* Product Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3">ID / SKU</th>
                <th className="px-5 py-3">Product Name</th>
                <th className="px-5 py-3">Category</th>
                <th className="px-5 py-3">Manufacturer</th>
                <th className="px-5 py-3">Origin</th>
                <th className="px-5 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center text-slate-400">
                    Loading products...
                  </td>
                </tr>
              ) : products.length > 0 ? (
                products.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => handleSelectProduct(p)}
                    className={`cursor-pointer hover:bg-blue-50/50 transition-colors ${
                      selectedProduct?.id === p.id ? 'bg-primary-50/60' : ''
                    }`}
                  >
                    <td className="px-5 py-3 font-mono text-slate-600">
                      <div className="font-semibold text-slate-900">#{p.id}</div>
                      <div className="text-[11px] text-slate-400">{p.sku || 'N/A'}</div>
                    </td>
                    <td className="px-5 py-3 font-medium text-slate-900">{p.productName}</td>
                    <td className="px-5 py-3">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                        p.category === 'FOOD' ? 'bg-emerald-100 text-emerald-800' :
                        p.category === 'COSMETICS' ? 'bg-pink-100 text-pink-800' :
                        p.category === 'ELECTRONICS' ? 'bg-blue-100 text-blue-800' :
                        'bg-purple-100 text-purple-800'
                      }`}>
                        {p.category}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-600">{p.manufacturerName || 'Not specified'}</td>
                    <td className="px-5 py-3 text-slate-600">{p.countryOfOrigin || 'India'}</td>
                    <td className="px-5 py-3 text-right">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleSelectProduct(p);
                        }}
                        className="text-xs text-primary-600 hover:text-primary-800 font-semibold"
                      >
                        Inspect &rarr;
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center text-slate-400">
                    No products found matching your filter criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Details Slide-Over / Drawer */}
      {selectedProduct && (
        <div className="bg-white rounded-xl border border-slate-300 shadow-lg p-6 space-y-6">
          <div className="flex items-start justify-between pb-4 border-b border-slate-200">
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-xs font-bold text-slate-400">PRODUCT DETAILS #{selectedProduct.id}</span>
                <span className="text-xs bg-slate-100 px-2 py-0.5 rounded font-mono">{selectedProduct.sku}</span>
              </div>
              <h2 className="text-xl font-bold text-slate-900 mt-1">{selectedProduct.productName}</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Category: <strong>{selectedProduct.category}</strong> | Manufacturer: <strong>{selectedProduct.manufacturerName || 'N/A'}</strong> | Origin: <strong>{selectedProduct.countryOfOrigin || 'India'}</strong>
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => handleRunEvaluation(selectedProduct.id)}
                className="inline-flex items-center px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-md shadow-sm"
              >
                <Play className="w-3.5 h-3.5 mr-1" />
                Run Compliance Check
              </button>
              <button
                onClick={() => setSelectedProduct(null)}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-md hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {drawerLoading ? (
            <div className="py-8 text-center text-slate-400">Loading product details...</div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Declarations (Listing vs Package) */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-800">Declared Information</h3>
                  <span className="text-xs text-slate-400">Listing vs Package Evidence</span>
                </div>

                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="min-w-full text-xs divide-y divide-slate-200">
                    <thead className="bg-slate-50 font-semibold text-slate-600">
                      <tr>
                        <th className="px-3 py-2">Field</th>
                        <th className="px-3 py-2">Source</th>
                        <th className="px-3 py-2">Value</th>
                        <th className="px-3 py-2">Normalized</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {declarations.length > 0 ? (
                        declarations.map((d) => (
                          <tr key={d.id} className="hover:bg-slate-50">
                            <td className="px-3 py-2 font-mono font-medium text-slate-800">{d.fieldName}</td>
                            <td className="px-3 py-2">
                              <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                                d.source === 'PACKAGE' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'
                              }`}>
                                {d.source}
                              </span>
                            </td>
                            <td className="px-3 py-2 font-medium">{d.fieldValue}</td>
                            <td className="px-3 py-2 text-slate-500 font-mono">
                              {d.normalizedValue ? `${d.normalizedValue} ${d.normalizationUnit || ''}` : '—'}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={4} className="px-3 py-4 text-center text-slate-400">
                            No declarations recorded. Upload packaging image in the Evidence tab.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Evidence thumbnails */}
                {evidenceList.length > 0 && (
                  <div>
                    <h4 className="text-xs font-semibold text-slate-700 mb-2">Packaging Evidence ({evidenceList.length})</h4>
                    <div className="flex space-x-3 overflow-x-auto pb-2">
                      {evidenceList.map((ev) => (
                        <div key={ev.id} className="border border-slate-200 rounded p-2 text-xs bg-slate-50 w-44 flex-shrink-0">
                          <div className="font-semibold truncate text-slate-800">{ev.originalFilename}</div>
                          <div className="text-[10px] text-slate-400">{(ev.fileSizeBytes / 1024).toFixed(1)} KB</div>
                          <span className="inline-block mt-1 text-[10px] bg-emerald-100 text-emerald-800 px-1.5 py-0.5 rounded font-medium">
                            {ev.uploadStatus}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Evaluation History */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-800">Compliance Evaluations ({evaluations.length})</h3>
                  <span className="text-xs text-slate-400">Legal Metrology Rules</span>
                </div>

                <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                  {evaluations.length > 0 ? (
                    evaluations.map((ev) => (
                      <div key={ev.id} className="p-3 border border-slate-200 rounded-lg bg-slate-50 space-y-2">
                        <div className="flex items-center justify-between">
                          <div>
                            <span className="text-xs font-bold text-slate-900">{ev.ruleCode}</span>
                            <span className="text-xs text-slate-500 ml-1.5">v{ev.versionNumber}</span>
                          </div>
                          <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                            ev.evaluationStatus === 'COMPLIANT' ? 'bg-emerald-100 text-emerald-800' :
                            ev.evaluationStatus === 'NON_COMPLIANT' ? 'bg-red-100 text-red-800' :
                            ev.evaluationStatus === 'NEEDS_REVIEW' ? 'bg-amber-100 text-amber-800' :
                            'bg-slate-200 text-slate-700'
                          }`}>
                            {ev.evaluationStatus}
                          </span>
                        </div>

                        <p className="text-xs text-slate-600">{ev.ruleName}</p>

                        {ev.findings.length > 0 && (
                          <div className="mt-2 space-y-1 bg-white p-2.5 rounded border border-slate-200">
                            {ev.findings.map((f) => (
                              <div key={f.id} className="text-xs">
                                <div className="flex items-center justify-between">
                                  <span className="font-semibold text-slate-800">{f.findingType}</span>
                                  <span className={`text-[10px] font-bold px-1.5 py-0.2 rounded ${
                                    f.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                                    f.severity === 'MAJOR' ? 'bg-amber-100 text-amber-700' :
                                    'bg-blue-100 text-blue-700'
                                  }`}>
                                    {f.severity}
                                  </span>
                                </div>
                                <p className="text-slate-600 text-[11px] mt-0.5">{f.explanation}</p>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    ))
                  ) : (
                    <div className="p-6 text-center text-slate-400 bg-slate-50 rounded-lg border border-dashed border-slate-300">
                      Product has not been evaluated yet. Click "Run Compliance Check" above.
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Register Product Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900">Register New Product</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Product Name *</label>
                <input
                  type="text"
                  required
                  value={newProduct.productName}
                  onChange={(e) => setNewProduct({ ...newProduct, productName: e.target.value })}
                  placeholder="e.g. Organic Brown Rice"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md focus:ring-1 focus:ring-primary-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">SKU</label>
                  <input
                    type="text"
                    value={newProduct.sku}
                    onChange={(e) => setNewProduct({ ...newProduct, sku: e.target.value })}
                    placeholder="e.g. FOOD-009"
                    className="w-full px-3 py-2 border border-slate-300 rounded-md"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Category *</label>
                  <select
                    value={newProduct.category}
                    onChange={(e) => setNewProduct({ ...newProduct, category: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md bg-white"
                  >
                    <option value="FOOD">FOOD</option>
                    <option value="COSMETICS">COSMETICS</option>
                    <option value="ELECTRONICS">ELECTRONICS</option>
                    <option value="HOUSEHOLD">HOUSEHOLD</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Manufacturer Name</label>
                <input
                  type="text"
                  value={newProduct.manufacturerName}
                  onChange={(e) => setNewProduct({ ...newProduct, manufacturerName: e.target.value })}
                  placeholder="e.g. Nature Best Foods Ltd"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Country of Origin</label>
                <input
                  type="text"
                  value={newProduct.countryOfOrigin}
                  onChange={(e) => setNewProduct({ ...newProduct, countryOfOrigin: e.target.value })}
                  placeholder="e.g. India"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Description</label>
                <textarea
                  rows={2}
                  value={newProduct.description}
                  onChange={(e) => setNewProduct({ ...newProduct, description: e.target.value })}
                  placeholder="Commodity declaration notes"
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>

              <div className="pt-3 border-t border-slate-200 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-3 py-2 border border-slate-300 rounded-md text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-md shadow-sm"
                >
                  Register
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
