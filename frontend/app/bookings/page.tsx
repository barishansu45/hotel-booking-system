'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth-store';
import { api } from '@/lib/api';

type BookingRow = {
  id: string;
  hotelId: string;
  roomId: string;
  checkInDate: string;
  checkOutDate: string;
  numGuests: number;
  finalPrice: number;
  status: string;
  createdAt?: string;
};

export default function MyBookingsPage() {
  const router = useRouter();
  const { user, initialized, initialize } = useAuthStore();
  const [bookings, setBookings] = useState<BookingRow[]>([]);
  const [hotelNames, setHotelNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!initialized) {
      void initialize();
    }
  }, [initialized, initialize]);

  useEffect(() => {
    if (!initialized) return;
    if (!user) {
      router.replace('/login');
      return;
    }

    let cancelled = false;

    (async () => {
      setLoading(true);
      setError('');
      try {
        const list = await api.getBookingsByUser(user.id);
        if (cancelled) return;
        setBookings(Array.isArray(list) ? list : []);

        const names: Record<string, string> = {};
        const uniqueHotelIds = [...new Set(list.map((b) => b.hotelId))];
        await Promise.all(
          uniqueHotelIds.map(async (hid) => {
            try {
              const resp = await api.getHotelById(hid);
              const name = resp?.data?.name;
              if (name) names[hid] = name;
              else names[hid] = 'Otel';
            } catch {
              names[hid] = 'Otel';
            }
          })
        );
        if (!cancelled) setHotelNames(names);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Liste yüklenemedi.');
          setBookings([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [initialized, user, router]);

  if (!initialized || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <p className="text-gray-600">Yükleniyor…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => router.push('/')}
            className="text-blue-600 hover:text-blue-700 font-semibold"
          >
            ← Ana sayfa
          </button>
          <h1 className="text-lg font-bold text-gray-900">My Bookings</h1>
          <span className="w-24" />
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-3xl">
        {loading && <p className="text-gray-600">Rezervasyonlar yükleniyor…</p>}
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {error}
          </div>
        )}

        {!loading && bookings.length === 0 && !error && (
          <div className="bg-white rounded-lg shadow p-8 text-center text-gray-600">
            <p>Henüz kayıtlı rezervasyon yok.</p>
            <button
              type="button"
              onClick={() => router.push('/')}
              className="mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              Otel ara
            </button>
          </div>
        )}

        <ul className="space-y-4">
          {bookings.map((b) => (
            <li key={b.id} className="bg-white rounded-lg shadow-md p-6">
              <div className="flex flex-wrap justify-between gap-2">
                <div>
                  <h2 className="text-xl font-semibold text-gray-900">
                    {hotelNames[b.hotelId] || 'Otel'}
                  </h2>
                  <p className="text-sm text-gray-500 mt-1">
                    Giriş: {b.checkInDate} — Çıkış: {b.checkOutDate}
                  </p>
                  <p className="text-sm text-gray-600 mt-1">
                    Misafir: {b.numGuests} · Durum:{' '}
                    <span className="font-medium text-green-700">{b.status}</span>
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-bold text-blue-600">
                    ${Number(b.finalPrice).toFixed(2)}
                  </p>
                  <p className="text-xs text-gray-500">toplam</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => router.push(`/hotels/${b.hotelId}`)}
                className="mt-4 text-sm text-blue-600 hover:underline"
              >
                Otel detayı
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
