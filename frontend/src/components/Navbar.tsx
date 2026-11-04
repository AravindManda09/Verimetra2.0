import React from 'react';
import { ShieldCheck, Database, GitCompare, Activity } from 'lucide-react';

export const Navbar: React.FC = () => {
  return (
    <header className="bg-slate-900 border-b border-slate-800 text-white sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="bg-primary-600 p-2 rounded-lg text-white shadow-md">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-xl tracking-tight text-white">VeriMetra</span>
              <span className="text-xs bg-primary-950 text-primary-300 border border-primary-700 font-semibold px-2 py-0.5 rounded">v1.0 MVP</span>
            </div>
            <p className="text-xs text-slate-400">Legal Metrology Compliance & Incremental Revalidation</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2 text-xs bg-slate-800 px-3 py-1.5 rounded-full border border-slate-700">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span className="text-slate-300 font-medium">System Online</span>
          </div>
          <div className="text-xs text-slate-400 border-l border-slate-700 pl-4">
            India Legal Metrology (PC) Rules, 2011
          </div>
        </div>
      </div>
    </header>
  );
};
