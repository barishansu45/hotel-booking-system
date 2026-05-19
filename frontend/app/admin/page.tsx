'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth-store';
import { isAdminUser } from '@/lib/is-admin';
import { api } from '@/lib/api';

type HotelRow = { id: string; name: string; city: string };
type RoomRow = { id: string; roomType: string; maxGuests: number };

export default function AdminPage() {
  const router = useRouter();
  const { user, initialize, initialized } = useAuthStore();
  const [hotels, setHotels] = useState<HotelRow[]>([]);
  const [rooms, setRooms] = useState<RoomRow[]>([]);
  const [hotelId, setHotelId] = useState('');
  const [roomId, setRoomId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [totalCapacity, setTotalCapacity] = useState('5');
  const [availableCapacity, setAvailableCapacity] = useState('5');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!initialized) initialize();
  }, [initialized, initialize]);

  useEffect(() => {
    if (!initialized) return;
    if (!user) return;
    if (!isAdminUser(user)) return;

    let cancelled = false;
    (async () => {
      try {
        const page = await api.getHotelsPage(0, 500);
        const content = (page?.data?.content ?? []) as Array<{
          id: string;
          name: string;
          city: string;
        }>;
        if (!cancelled) {
          setHotels(content.map((h) => ({ id: h.id, name: h.name, city: h.city })));
        }
      } catch (e) {
        console.error(e);
        if (!cancelled) setStatus('Could not load hotels (is the API gateway + hotel-service running?).');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [initialized, user]);

  useEffect(() => {
    if (!hotelId) {
      setRooms([]);
      setRoomId('');
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const resp = await api.getRoomsByHotelId(hotelId);
        const list = (resp?.data ?? []) as RoomRow[];
        if (!cancelled) {
          setRooms(list);
          setRoomId((prev) => (list.some((r) => r.id === prev) ? prev : list[0]?.id ?? ''));
        }
      } catch (e) {
        console.error(e);
        if (!cancelled) {
          setRooms([]);
          setRoomId('');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [hotelId]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus('');
    if (!roomId || !startDate || !endDate) {
      setStatus('Room and date range are required.');
      return;
    }
    const tc = parseInt(totalCapacity, 10);
    const ac = parseInt(availableCapacity, 10);
    if (!Number.isFinite(tc) || !Number.isFinite(ac) || tc < 1 || ac < 0 || ac > tc) {
      setStatus('Capacities must be valid (available ≤ total, total ≥ 1).');
      return;
    }
    setLoading(true);
    try {
      const res = await api.adminSetRoomAvailability({
        roomId,
        startDate,
        endDate,
        totalCapacity: tc,
        availableCapacity: ac,
      });
      const n = Array.isArray(res?.data) ? res.data.length : 0;
      setStatus(`Saved ${n} availability row(s).`);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Request failed';
      setStatus(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!initialized) {
    return (
      <main className="min-h-screen flex items-center justify-center bg-slate-50">
        <p className="text-gray-600">Loading…</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-4">
        <p className="text-gray-700">Please log in to access admin tools.</p>
        <button
          type="button"
          onClick={() => router.push('/login')}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg font-semibold"
        >
          Login
        </button>
      </main>
    );
  }

  if (!isAdminUser(user)) {
    return (
      <main className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-4 px-6 text-center">
        <p className="text-gray-800 font-medium">This area requires an admin account.</p>
        <p className="text-gray-600 text-sm max-w-lg">
          In Supabase Dashboard, set <code className="bg-gray-100 px-1 rounded">app_metadata.role</code> to{' '}
          <code className="bg-gray-100 px-1 rounded">admin</code> for your user, then sign out and sign in again so
          the JWT includes the new claim.
        </p>
        <button
          type="button"
          onClick={() => router.push('/')}
          className="px-4 py-2 border border-gray-300 rounded-lg font-semibold text-gray-800 hover:bg-gray-100"
        >
          Home
        </button>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 py-10 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Admin — room availability</h1>
          <button
            type="button"
            onClick={() => router.push('/')}
            className="text-sm text-blue-600 hover:underline font-medium"
          >
            ← Home
          </button>
        </div>

        <p className="text-gray-600 text-sm mb-6">
          Sets inventory per night via <code className="bg-white px-1 py-0.5 rounded border">POST /api/v1/admin/availability</code>.
          Requires a JWT with role <code className="bg-white px-1 py-0.5 rounded border">admin</code>.
        </p>

        <form onSubmit={onSubmit} className="bg-white rounded-xl shadow p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Hotel</label>
            <select
              value={hotelId}
              onChange={(e) => setHotelId(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900"
            >
              <option value="">Select hotel</option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name} — {h.city}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Room</label>
            <select
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              disabled={!hotelId || rooms.length === 0}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900 disabled:bg-gray-100"
            >
              {rooms.length === 0 ? (
                <option value="">No rooms (pick a hotel)</option>
              ) : (
                rooms.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.roomType} (max {r.maxGuests} guests)
                  </option>
                ))
              )}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Start date</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">End date</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Total capacity / night</label>
              <input
                type="number"
                min={1}
                value={totalCapacity}
                onChange={(e) => setTotalCapacity(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Available / night</label>
              <input
                type="number"
                min={0}
                value={availableCapacity}
                onChange={(e) => setAvailableCapacity(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900"
              />
            </div>
          </div>

          {status && (
            <p
              className={`text-sm whitespace-pre-wrap ${status.startsWith('Saved') ? 'text-green-700' : 'text-red-600'}`}
            >
              {status}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold"
          >
            {loading ? 'Saving…' : 'Apply to date range'}
          </button>
        </form>
      </div>
    </main>
  );
}
