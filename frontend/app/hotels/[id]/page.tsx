'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth-store';
import { api } from '@/lib/api';
import CommentsSection from '@/components/CommentsSection';

interface HotelDetail {
  id: string;
  name: string;
  city: string;
  address: string;
  description: string;
  starRating: number;
  basePrice: number;
  discountedPrice: number;
  amenities: string[];
  mainImage: string;
  images: string[];
  latitude: number;
  longitude: number;
}

const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=600&fit=crop';

interface RoomOption {
  id: string;
  roomType: string;
  maxGuests: number;
}

export default function HotelDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuthStore();
  const [hotel, setHotel] = useState<HotelDetail | null>(null);
  const [rooms, setRooms] = useState<RoomOption[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState('');
  const [bookingSubmitting, setBookingSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [bookingForm, setBookingForm] = useState({
    checkIn: '',
    checkOut: '',
    guests: '2',
  });

  useEffect(() => {
    const fetchHotel = async () => {
      try {
        const [hotelResp, searchResp] = await Promise.all([
          api.getHotelById(params.id as string),
          api.getHotelDetails(params.id as string),
        ]);

        const hotelData = hotelResp?.data || {};
        const searchData = searchResp || {};

        const merged: HotelDetail = {
          id: hotelData.id,
          name: hotelData.name || searchData.name || 'Hotel',
          city: hotelData.city || searchData.city || '',
          address: hotelData.address || searchData.address || '',
          description: hotelData.description || '',
          starRating: hotelData.starRating ?? searchData.starRating ?? 0,
          basePrice: searchData.basePrice ?? 0,
          discountedPrice: searchData.discountedPrice ?? searchData.basePrice ?? 0,
          amenities: hotelData.amenities || searchData.amenities || [],
          mainImage:
            searchData.mainImage ||
            (Array.isArray(hotelData.images) && hotelData.images[0]) ||
            FALLBACK_IMAGE,
          images: hotelData.images || [],
          latitude: hotelData.latitude ?? searchData.latitude ?? 0,
          longitude: hotelData.longitude ?? searchData.longitude ?? 0,
        };

        setHotel(merged);

        try {
          const roomsResp = await api.getRoomsByHotelId(params.id as string);
          const list = roomsResp?.data;
          const arr = Array.isArray(list) ? list : [];
          const opts: RoomOption[] = arr.map((r: { id: string; roomType: string; maxGuests: number }) => ({
            id: r.id,
            roomType: r.roomType,
            maxGuests: r.maxGuests,
          }));
          setRooms(opts);
          setSelectedRoomId(opts[0]?.id ?? '');
        } catch {
          setRooms([]);
          setSelectedRoomId('');
        }
      } catch (error) {
        console.error('Error fetching hotel:', error);
      } finally {
        setLoading(false);
      }
    };

    if (params.id) {
      fetchHotel();
    }
  }, [params.id]);

  const handleBooking = async () => {
    if (!user) {
      alert('Please login to book a hotel.');
      router.push('/login');
      return;
    }
    if (!hotel) return;
    if (!selectedRoomId) {
      alert('Bu otelde rezervasyon için oda bulunamadı.');
      return;
    }
    if (!bookingForm.checkIn || !bookingForm.checkOut) {
      alert('Please select check-in and check-out dates.');
      return;
    }
    const guestNum = parseInt(bookingForm.guests, 10);
    if (!Number.isFinite(guestNum) || guestNum < 1) {
      alert('Misafir sayısı en az 1 olmalı.');
      return;
    }
    const selectedRoom = rooms.find((r) => r.id === selectedRoomId);
    if (selectedRoom && guestNum > selectedRoom.maxGuests) {
      alert(`Seçilen oda en fazla ${selectedRoom.maxGuests} misafir için uygun.`);
      return;
    }
    const nights = Math.max(
      1,
      Math.ceil(
        (new Date(bookingForm.checkOut).getTime() -
          new Date(bookingForm.checkIn).getTime()) /
          (1000 * 60 * 60 * 24)
      )
    );
    const pricePerNight = user ? hotel.discountedPrice : hotel.basePrice;
    const total = pricePerNight * nights;

    setBookingSubmitting(true);
    try {
      await api.createBooking({
        userId: user.id,
        hotelId: hotel.id,
        roomId: selectedRoomId,
        checkInDate: bookingForm.checkIn,
        checkOutDate: bookingForm.checkOut,
        numGuests: guestNum,
        totalPrice: total.toFixed(2),
        specialRequests: null,
      });
      alert(`Rezervasyon kaydedildi: ${hotel.name}\nToplam: $${total.toFixed(2)}`);
      router.push('/bookings');
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Rezervasyon başarısız.');
    } finally {
      setBookingSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading hotel details...</p>
        </div>
      </div>
    );
  }

  if (!hotel) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-600 text-lg">Hotel not found</p>
          <button
            onClick={() => router.push('/search')}
            className="mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Back to Search
          </button>
        </div>
      </div>
    );
  }

  const showDiscount =
    user && hotel.discountedPrice > 0 && hotel.discountedPrice < hotel.basePrice;
  const displayPrice = user ? hotel.discountedPrice : hotel.basePrice;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between gap-2">
          <button
            onClick={() => router.back()}
            className="text-blue-600 hover:text-blue-700 font-semibold"
          >
            ← Back
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

      <div className="container mx-auto px-4 py-8">
        {/* Hotel Image */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden mb-6">
          <img
            src={hotel.mainImage}
            alt={hotel.name}
            className="w-full h-96 object-cover"
            onError={(e) => {
              (e.target as HTMLImageElement).src = FALLBACK_IMAGE;
            }}
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Hotel Info */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h1 className="text-4xl font-bold text-gray-900 mb-4">{hotel.name}</h1>

              <div className="flex items-center gap-4 mb-4">
                <span className="text-yellow-500">
                  {'⭐'.repeat(Math.floor(hotel.starRating))}
                </span>
                <span className="text-gray-600">{hotel.starRating} stars</span>
              </div>

              <p className="text-gray-600 mb-4">
                📍 {hotel.address}
                {hotel.city ? `, ${hotel.city}` : ''}
              </p>

              <p className="text-gray-700 leading-relaxed mb-6">
                {hotel.description || 'Welcome to our hotel. Enjoy your stay!'}
              </p>

              <h3 className="text-xl font-semibold text-gray-900 mb-3">Amenities</h3>
              <div className="flex flex-wrap gap-2">
                {(hotel.amenities || []).map((amenity, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-2 bg-blue-50 text-blue-700 rounded-lg text-sm"
                  >
                    {amenity}
                  </span>
                ))}
              </div>
            </div>

            {/* Comments Section */}
            <CommentsSection hotelId={params.id as string} />
          </div>

          {/* Booking Card */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-4">
              <div className="mb-6">
                {showDiscount && (
                  <p className="text-gray-500 line-through text-lg">
                    ${hotel.basePrice.toFixed(2)}
                  </p>
                )}
                <div className="flex items-center gap-2">
                  <p className="text-4xl font-bold text-blue-600">
                    ${displayPrice.toFixed(2)}
                  </p>
                  {showDiscount && (
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">
                      15% OFF
                    </span>
                  )}
                </div>
                <p className="text-gray-600 mt-1">per night</p>
              </div>

              <div className="space-y-4 mb-6">
                {rooms.length > 0 && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Oda</label>
                    <select
                      value={selectedRoomId}
                      onChange={(e) => setSelectedRoomId(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900 bg-white"
                    >
                      {rooms.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.roomType} (en fazla {r.maxGuests} misafir)
                        </option>
                      ))}
                    </select>
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Check-in
                  </label>
                  <input
                    type="date"
                    value={bookingForm.checkIn}
                    onChange={(e) =>
                      setBookingForm({ ...bookingForm, checkIn: e.target.value })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Check-out
                  </label>
                  <input
                    type="date"
                    value={bookingForm.checkOut}
                    onChange={(e) =>
                      setBookingForm({ ...bookingForm, checkOut: e.target.value })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
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
                    value={bookingForm.guests}
                    onChange={(e) =>
                      setBookingForm({
                        ...bookingForm,
                        guests: e.target.value,
                      })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
                  />
                </div>
              </div>

              <button
                type="button"
                disabled={bookingSubmitting || rooms.length === 0}
                onClick={() => void handleBooking()}
                className="w-full px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition"
              >
                {bookingSubmitting ? 'Kaydediliyor…' : 'Book Now'}
              </button>

              {!user && (
                <p className="text-center text-sm text-gray-600 mt-4">
                  <a href="/login" className="text-blue-600 hover:underline">
                    Sign in
                  </a>{' '}
                  to get 15% discount
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
