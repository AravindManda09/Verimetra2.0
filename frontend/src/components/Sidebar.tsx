import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  Image as ImageIcon,
  Scale,
  CheckCircle2,
  RefreshCw,
  FlaskConical,
  FileSpreadsheet,
} from 'lucide-react';

const navItems = [
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/products', label: 'Products', icon: Package },
  { to: '/evidence', label: 'Package Evidence', icon: ImageIcon },
  { to: '/rules', label: 'Rule Management', icon: Scale },
  { to: '/evaluations', label: 'Evaluations', icon: CheckCircle2 },
  { to: '/revalidation', label: 'Revalidation', icon: RefreshCw, badge: 'Research' },
  { to: '/experiments', label: 'Experiments', icon: FlaskConical },
  { to: '/reports', label: 'Reports', icon: FileSpreadsheet },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="w-64 bg-white border-r border-slate-200 min-h-[calc(100vh-4rem)] flex flex-col justify-between p-4">
      <div className="space-y-1">
        <div className="px-3 py-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Compliance Navigation
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700 font-semibold border-l-4 border-primary-600 pl-2'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`
              }
            >
              <div className="flex items-center space-x-3">
                <Icon className="w-5 h-5 text-slate-500" />
                <span>{item.label}</span>
              </div>
              {item.badge && (
                <span className="text-[10px] bg-primary-100 text-primary-700 font-bold px-2 py-0.5 rounded-full">
                  {item.badge}
                </span>
              )}
            </NavLink>
          );
        })}
      </div>

      <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs text-slate-600 space-y-1">
        <div className="font-semibold text-slate-800">VeriMetra MVP</div>
        <p className="text-[11px] text-slate-500 leading-relaxed">
          Evidence-based evaluation & dependency-aware incremental revalidation.
        </p>
      </div>
    </aside>
  );
};
