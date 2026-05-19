'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import dynamic from 'next/dynamic';
import { useAuthStore } from '@/lib/auth-store';
import { api } from '@/lib/api';

const MapView = dynamic(() => import('@/components/MapView'), {
  ssr: false,
  loading: () => <div className="h-96 bg-gray-100 animate-pulse rounded-lg" />
});

interface Hotel {
  hotelId: string;
  name: string;
  city: string;
  address: string;
  starRating: number;
  basePrice: number;
  discountedPrice: number;
  amenities: string[];
  mainImage: string;
  latitude: number;
  longitude: number;
}

function SearchResultsContent() {
  const router = useRouter();
  const urlParams = useSearchParams();
  const { user } = useAuthStore();
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [searchParams, setSearchParams] = useState({
    destination: urlParams.get('destination') || '',
    checkIn: urlParams.get('checkIn') || '',
    checkOut: urlParams.get('checkOut') || '',
    guests: urlParams.get('guests') || '2'
  });

  const [searchError, setSearchError] = useState('');

  const guestsAsNumber = (raw: string) => {
    const n = parseInt(raw, 10);
    return Number.isFinite(n) ? n : 1;
  };

  const doSearch = async (params: typeof searchParams) => {
    setLoading(true);
    setHasSearched(true);
    try {
      const results = await api.searchHotels({
        destination: params.destination,
        checkInDate: params.checkIn,
        checkOutDate: params.checkOut,
        guests: guestsAsNumber(params.guests)
      });
      setHotels(Array.isArray(results) ? results : []);
    } catch (error) {
      console.error('Search error:', error);
      setHotels([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (searchParams.destination) {
      doSearch(searchParams);
    }
  }, []);

  const validateForm = (): string | null => {
    if (!searchParams.destination.trim()) return 'Please enter a destination.';
    if (searchParams.checkIn && searchParams.checkOut) {
      const checkInDate = new Date(searchParams.checkIn);
      const checkOutDate = new Date(searchParams.checkOut);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (checkInDate < today) return 'Check-in date cannot be in the past.';
      if (checkOutDate <= checkInDate) return 'Check-out date must be after check-in date.';
    }
    const g = parseInt(searchParams.guests, 10);
    if (!Number.isFinite(g) || g < 1) return 'Number of guests must be at least 1.';
    return null;
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setSearchError('');
    const error = validateForm();
    if (error) {
      setSearchError(error);
      return;
    }
    await doSearch(searchParams);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="container mx-auto px-4 py-4 flex flex-wrap items-center justify-between gap-2">
          <button
            onClick={() => router.push('/')}
            className="text-blue-600 hover:text-blue-700 font-semibold"
          >
            ← Back to Home
          </button>
          {user && (
            <button
              type="button"
              onClick={() => router.push('/bookings')}
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              My Bookings
            </button>
          )}
        </div>
      </header>

      {/* Search Bar */}
      <div className="bg-white border-b">
        <div className="container mx-auto px-4 py-6">
          {searchError && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {searchError}
            </div>
          )}
          <form onSubmit={handleSearch} className="flex gap-4 items-end">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Destination
              </label>
              <input
                type="text"
                value={searchParams.destination}
                onChange={(e) => setSearchParams({ ...searchParams, destination: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
                placeholder="City or hotel name"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Check-in
              </label>
              <input
                type="date"
                value={searchParams.checkIn}
                onChange={(e) => setSearchParams({ ...searchParams, checkIn: e.target.value })}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Check-out
              </label>
              <input
                type="date"
                value={searchParams.checkOut}
                onChange={(e) => setSearchParams({ ...searchParams, checkOut: e.target.value })}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
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
                value={searchParams.guests}
                onChange={(e) => setSearchParams({ ...searchParams, guests: e.target.value })}
                className="w-24 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition disabled:opacity-50"
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>
        </div>
      </div>

      {/* View Toggle */}
      <div className="container mx-auto px-4 py-4">
        <div className="flex justify-between items-center">
          <h2 className="text-2xl font-bold text-gray-900">
            {hotels.length} Hotels Found
            {user && <span className="ml-2 text-sm text-green-600">(15% discount applied)</span>}
          </h2>
          <div className="flex gap-2">
            <button
              onClick={() => setViewMode('list')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                viewMode === 'list'
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
              📋 List View
            </button>
            <button
              onClick={() => setViewMode('map')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                viewMode === 'map'
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
              🗺️ Show on Map
            </button>
          </div>
        </div>
      </div>

      {/* Results */}
      <div className="container mx-auto px-4 pb-8">
        {viewMode === 'list' ? (
          <div className="grid grid-cols-1 gap-4">
            {hotels.map((hotel) => (
              <div key={hotel.hotelId} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
                <div className="flex gap-6">
                  <img
                    src={hotel.mainImage || 'https://via.placeholder.com/200x150?text=Hotel'}
                    alt={hotel.name}
                    className="w-48 h-36 object-cover rounded-lg"
                  />
                  <div className="flex-1">
                    <h3 className="text-xl font-bold text-gray-900 mb-2">{hotel.name}</h3>
                    <p className="text-gray-600 mb-2">
                      {'⭐'.repeat(Math.floor(hotel.starRating))} {hotel.starRating} stars
                    </p>
                    <p className="text-gray-600 mb-3">
                      📍 {hotel.address}, {hotel.city}
                    </p>
                    <div className="flex flex-wrap gap-2 mb-3">
                      {hotel.amenities.slice(0, 4).map((amenity, idx) => (
                        <span key={idx} className="px-2 py-1 bg-blue-50 text-blue-700 text-sm rounded">
                          {amenity}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="text-right">
                    {user && hotel.discountedPrice < hotel.basePrice && (
                      <p className="text-gray-500 line-through text-sm">
                        ${hotel.basePrice.toFixed(2)}
                      </p>
                    )}
                    <p className="text-3xl font-bold text-blue-600 mb-2">
                      ${(user ? hotel.discountedPrice : hotel.basePrice).toFixed(2)}
                    </p>
                    <p className="text-gray-600 text-sm mb-4">per night</p>
                    <button 
                      onClick={() => router.push(`/hotels/${hotel.hotelId}`)}
                      className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition"
                    >
                      View Details
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <MapView hotels={hotels} />
        )}

        {hotels.length === 0 && !loading && hasSearched && (
          <div className="text-center py-16">
            <p className="text-gray-500 text-lg">
              No hotels found. Try searching for a destination!
            </p>
          </div>
        )}

        {!hasSearched && (
          <div className="text-center py-16">
            <p className="text-gray-500 text-lg">
              Enter a destination above to search for hotels.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default function SearchResultsPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Loading...</div>}>
      <SearchResultsContent />
    </Suspense>
  );
}
