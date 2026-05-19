'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth-store';
import { isAdminUser } from '@/lib/is-admin';

export default function Home() {
  const router = useRouter();
  const { user, signOut, initialize, initialized } = useAuthStore();
  const [searchError, setSearchError] = useState('');
  const [searchForm, setSearchForm] = useState({
    destination: '',
    checkIn: '',
    checkOut: '',
    guests: '2'
  });

  useEffect(() => {
    if (!initialized) {
      initialize();
    }
  }, [initialized, initialize]);

  const handleLogout = async () => {
    await signOut();
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchError('');

    if (!searchForm.destination.trim()) {
      setSearchError('Please enter a destination.');
      return;
    }
    if (searchForm.checkIn && searchForm.checkOut) {
      const checkInDate = new Date(searchForm.checkIn);
      const checkOutDate = new Date(searchForm.checkOut);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (checkInDate < today) {
        setSearchError('Check-in date cannot be in the past.');
        return;
      }
      if (checkOutDate <= checkInDate) {
        setSearchError('Check-out date must be after check-in date.');
        return;
      }
    }
    const guestNum = parseInt(searchForm.guests, 10);
    if (!Number.isFinite(guestNum) || guestNum < 1) {
      setSearchError('Number of guests must be at least 1.');
      return;
    }

    const params = new URLSearchParams({
      destination: searchForm.destination,
      checkIn: searchForm.checkIn,
      checkOut: searchForm.checkOut,
      guests: String(guestNum),
    });
    router.push(`/search?${params.toString()}`);
  };

  return (
    <main className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      {/* Header with Auth */}
      <header className="bg-white shadow-sm">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <div className="flex items-center">
            <span className="text-2xl font-bold text-blue-600">🏨 HotelBooking</span>
          </div>
          
          <div className="flex items-center gap-4">
            {user ? (
              <>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-700">
                    Welcome, <span className="font-semibold">{user.user_metadata?.full_name || user.email}</span>
                  </span>
                  <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">
                    15% OFF
                  </span>
                </div>
                {isAdminUser(user) && (
                  <button
                    type="button"
                    onClick={() => router.push('/admin')}
                    className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white font-semibold rounded-lg transition duration-200"
                  >
                    Admin
                  </button>
                )}
                <button
                  onClick={handleLogout}
                  className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 font-semibold rounded-lg transition duration-200"
                >
                  Logout
                </button>
                <button
                  type="button"
                  onClick={() => router.push('/bookings')}
                  className="px-4 py-2 border border-blue-600 text-blue-600 hover:bg-blue-50 font-semibold rounded-lg transition duration-200"
                >
                  My Bookings
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={() => router.push('/login')}
                  className="px-4 py-2 text-blue-600 hover:text-blue-700 font-semibold transition duration-200"
                >
                  Login
                </button>
                <button
                  onClick={() => router.push('/register')}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition duration-200"
                >
                  Register
                </button>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-16">
        <div className="text-center mb-12">
          <h1 className="text-6xl font-bold text-gray-900 mb-4">
            Find Your Perfect Hotel
          </h1>
          <p className="text-xl text-gray-600">
            Search, compare, and book hotels worldwide
          </p>
        </div>

        <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-xl p-8">
          {searchError && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
              {searchError}
            </div>
          )}
          <form onSubmit={handleSearch} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Destination
                </label>
                <input
                  type="text"
                  value={searchForm.destination}
                  onChange={(e) => setSearchForm({ ...searchForm, destination: e.target.value })}
                  placeholder="Where are you going?"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Guests
                </label>
                <input
                  type="number"
                  min="1"
                  inputMode="numeric"
                  value={searchForm.guests}
                  onChange={(e) => setSearchForm({ ...searchForm, guests: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Check-in
                </label>
                <input
                  type="date"
                  value={searchForm.checkIn}
                  onChange={(e) => setSearchForm({ ...searchForm, checkIn: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Check-out
                </label>
                <input
                  type="date"
                  value={searchForm.checkOut}
                  onChange={(e) => setSearchForm({ ...searchForm, checkOut: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-4 px-6 rounded-lg transition duration-200 transform hover:scale-105"
            >
              Search Hotels
            </button>
          </form>
        </div>

        <div className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="text-center p-6 bg-white rounded-lg shadow-md">
            <div className="text-5xl mb-4">🏨</div>
            <h3 className="text-xl font-semibold mb-2 text-gray-900">10,000+ Hotels</h3>
            <p className="text-gray-600">Find hotels worldwide</p>
          </div>
          <div className="text-center p-6 bg-white rounded-lg shadow-md">
            <div className="text-5xl mb-4">💰</div>
            <h3 className="text-xl font-semibold mb-2 text-gray-900">Best Prices</h3>
            <p className="text-gray-600">15% discount for members</p>
          </div>
          <div className="text-center p-6 bg-white rounded-lg shadow-md">
            <div className="text-5xl mb-4">🤖</div>
            <h3 className="text-xl font-semibold mb-2 text-gray-900">AI Assistant</h3>
            <p className="text-gray-600">Chat with our AI helper</p>
          </div>
        </div>
      </div>

    </main>
  );
}
