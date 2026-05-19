import { supabase } from './supabase';

/** API Gateway (recommended local): http://localhost:8080/api/v1 */
const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1').replace(
  /\/$/,
  ''
);

/**
 * AI service is registered at /api/v1/chat on port 8086.
 * Gateway uses /api/v1/ai/** — if you fix that route, set NEXT_PUBLIC_AI_API_URL to gateway + path.
 */
const AI_API_ROOT = (
  process.env.NEXT_PUBLIC_AI_API_URL || 'http://localhost:8086/api/v1'
).replace(/\/$/, '');

async function getAuthHeaders() {
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (session?.access_token) {
    headers['Authorization'] = `Bearer ${session.access_token}`;
  }

  return headers;
}

export const api = {
  searchHotels: async (params: Record<string, unknown>) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/search`, {
      method: 'POST',
      headers,
      body: JSON.stringify(params),
    });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Search failed (${response.status}): ${text.slice(0, 200)}`);
    }
    return response.json();
  },

  getHotelById: async (id: string) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/hotels/${id}`, { headers });
    if (!response.ok) {
      throw new Error(`Hotel not found (${response.status})`);
    }
    return response.json();
  },

  getHotelsPage: async (page = 0, size = 50) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/hotels?page=${page}&size=${size}&sortBy=name`, {
      headers,
    });
    if (!response.ok) {
      throw new Error(`Hotels list failed (${response.status})`);
    }
    return response.json() as Promise<{
      success?: boolean;
      data?: { content?: Array<{ id: string; name: string; city: string }> };
    }>;
  },

  adminSetRoomAvailability: async (body: {
    roomId: string;
    startDate: string;
    endDate: string;
    totalCapacity: number;
    availableCapacity: number;
  }) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/admin/availability`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const msg =
        typeof (data as { message?: string }).message === 'string'
          ? (data as { message: string }).message
          : `Admin availability failed (${response.status})`;
      throw new Error(msg);
    }
    return data as { data?: unknown[]; message?: string };
  },

  getHotelDetails: async (id: string) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/search/${id}`, { headers });
    if (!response.ok) {
      throw new Error(`Hotel details failed (${response.status})`);
    }
    return response.json();
  },

  getRoomsByHotelId: async (hotelId: string) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/rooms/hotel/${hotelId}`, { headers });
    if (!response.ok) {
      throw new Error(`Rooms failed (${response.status})`);
    }
    return response.json();
  },

  createBooking: async (bookingData: Record<string, unknown>) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/bookings`, {
      method: 'POST',
      headers,
      body: JSON.stringify(bookingData),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const msg =
        typeof (data as { message?: string }).message === 'string'
          ? (data as { message: string }).message
          : `Booking failed (${response.status})`;
      throw new Error(msg);
    }
    return data;
  },

  getBookingsByUser: async (userId: string, page = 0, size = 20) => {
    const headers = await getAuthHeaders();
    const response = await fetch(
      `${API_BASE_URL}/bookings/user/${userId}?page=${page}&size=${size}`,
      { headers }
    );
    if (!response.ok) {
      throw new Error(`Could not load bookings (${response.status})`);
    }
    // Backend returns Spring Page<Booking> — extract the content array
    const data = await response.json();
    return (data?.content ?? data) as Array<{
      id: string;
      userId: string;
      hotelId: string;
      roomId: string;
      checkInDate: string;
      checkOutDate: string;
      numGuests: number;
      totalPrice: number;
      finalPrice: number;
      status: string;
      createdAt?: string;
    }>;
  },

  getCommentsByHotelId: async (hotelId: string) => {
    const headers = await getAuthHeaders();
    // Use /all endpoint for the full (non-paginated) list needed by the rating graph
    const response = await fetch(`${API_BASE_URL}/comments/hotel/${hotelId}/all`, { headers });
    if (!response.ok) {
      throw new Error(`Comments failed (${response.status})`);
    }
    return response.json();
  },

  createComment: async (commentData: Record<string, unknown>) => {
    const headers = await getAuthHeaders();
    const response = await fetch(`${API_BASE_URL}/comments`, {
      method: 'POST',
      headers,
      body: JSON.stringify(commentData),
    });
    if (!response.ok) {
      throw new Error(`Comment failed (${response.status})`);
    }
    return response.json();
  },

  chatWithAI: async (message: string, conversationId?: string) => {
    const headers = await getAuthHeaders();
    const {
      data: { user },
    } = await supabase.auth.getUser();
    const response = await fetch(`${AI_API_ROOT}/chat`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        message,
        conversationId,
        ...(user?.id ? { userId: user.id } : {}),
      }),
    });
    if (!response.ok) {
      throw new Error(`AI chat failed (${response.status})`);
    }
    return response.json();
  },
};
