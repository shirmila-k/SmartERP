import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Sparkles } from 'lucide-react';

export default function DemoSwitcher() {
  const { currentUser, loginAs, allUsers } = useAuth();

  return (
    <div className="flex items-center gap-1.5 sm:gap-2 bg-[#E7F5DC] border border-[#CFE1B9] px-2 sm:px-3 py-1 sm:py-1.5 rounded-full text-xs text-[#1F2617]">
      <div className="flex items-center gap-1 font-semibold text-[#5F6D44]">
        <Sparkles size={13} className="text-[#728156] flex-shrink-0" />
        <span className="hidden sm:inline">Demo Switcher:</span>
      </div>
      <select
        value={currentUser ? currentUser.id : ''}
        onChange={(e) => loginAs(e.target.value)}
        className="bg-white border border-[#B6C99B] text-[#1F2617] rounded-md px-1.5 py-0.5 sm:px-2 sm:py-1 font-medium text-xs focus:outline-none focus:ring-1 focus:ring-[#728156] cursor-pointer max-w-[150px] sm:max-w-none truncate"
      >
        {allUsers.map((u) => (
          <option key={u.id} value={u.id}>
            {u.fullName} ({u.role})
          </option>
        ))}
      </select>
      {currentUser && (
        <span className="hidden md:inline-block px-2 py-0.5 bg-[#728156] text-white rounded font-bold text-[10px]">
          {currentUser.role}
        </span>
      )}
    </div>
  );
}
