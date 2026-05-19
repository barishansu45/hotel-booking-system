'use client';

import { useState } from 'react';
import ChatWidget from './ChatWidget';

export default function GlobalChat() {
  const [chatOpen, setChatOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setChatOpen(true)}
        className="fixed bottom-8 right-8 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white p-4 rounded-full shadow-2xl transition duration-200 hover:scale-110 z-40"
        aria-label="Open AI Assistant"
      >
        <div className="flex items-center gap-2">
          <span className="text-2xl">🤖</span>
          <span className="font-semibold hidden sm:inline">AI Assistant</span>
        </div>
      </button>

      <ChatWidget isOpen={chatOpen} onClose={() => setChatOpen(false)} />
    </>
  );
}
