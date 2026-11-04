import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { OverviewPage } from './pages/OverviewPage';
import { ProductsPage } from './pages/ProductsPage';
import { EvidencePage } from './pages/EvidencePage';
import { RulesPage } from './pages/RulesPage';
import { EvaluationsPage } from './pages/EvaluationsPage';
import { RevalidationPage } from './pages/RevalidationPage';
import { ExperimentsPage } from './pages/ExperimentsPage';
import { ReportsPage } from './pages/ReportsPage';

function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <div className="min-h-screen bg-slate-50 flex flex-col font-sans text-slate-800 antialiased selection:bg-primary-500 selection:text-white">
        <Navbar />
        <div className="flex flex-1">
          <Sidebar />
          <main className="flex-1 p-6 lg:p-8 max-w-7xl mx-auto w-full overflow-y-auto">
            <Routes>
              <Route path="/" element={<OverviewPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/evidence" element={<EvidencePage />} />
              <Route path="/rules" element={<RulesPage />} />
              <Route path="/evaluations" element={<EvaluationsPage />} />
              <Route path="/revalidation" element={<RevalidationPage />} />
              <Route path="/experiments" element={<ExperimentsPage />} />
              <Route path="/reports" element={<ReportsPage />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;
