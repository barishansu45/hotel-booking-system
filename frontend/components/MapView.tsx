'use client';

import { useEffect, useMemo, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface Hotel {
  hotelId: string;
  name: string;
  city: string;
  address: string;
  starRating: number;
  basePrice: number;
  discountedPrice: number;
  latitude: number;
  longitude: number;
}

interface MapViewProps {
  hotels: Hotel[];
}

const ICON_RETINA = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png';
const ICON_URL = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png';
const SHADOW_URL = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png';

function toNum(v: unknown): number {
  if (v == null) return NaN;
  const n = typeof v === 'number' ? v : parseFloat(String(v));
  return Number.isFinite(n) ? n : NaN;
}

export default function MapView({ hotels }: MapViewProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<L.Map | null>(null);

  const validHotels = useMemo(() => {
    return hotels
      .map((h) => {
        const lat = toNum(h.latitude);
        const lng = toNum(h.longitude);
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
        return { ...h, latitude: lat, longitude: lng };
      })
      .filter((h): h is Hotel => h !== null);
  }, [hotels]);

  const hotelsFingerprint = useMemo(
    () =>
      validHotels
        .map((h) => `${h.hotelId}:${h.latitude.toFixed(5)}:${h.longitude.toFixed(5)}`)
        .join('|'),
    [validHotels]
  );

  useEffect(() => {
    if (validHotels.length === 0) {
      if (mapInstance.current) {
        mapInstance.current.remove();
        mapInstance.current = null;
      }
      return;
    }
    if (!mapRef.current) return;

    // Avoid double-init
    if (mapInstance.current) {
      mapInstance.current.remove();
      mapInstance.current = null;
    }

    const defaultIcon = L.icon({
      iconRetinaUrl: ICON_RETINA,
      iconUrl: ICON_URL,
      shadowUrl: SHADOW_URL,
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
    });

    const center: [number, number] = [
      validHotels.reduce((sum, h) => sum + h.latitude, 0) / validHotels.length,
      validHotels.reduce((sum, h) => sum + h.longitude, 0) / validHotels.length,
    ];

    const zoom = validHotels.length === 1 ? 13 : 6;
    const map = L.map(mapRef.current).setView(center, zoom);
    mapInstance.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);

    validHotels.forEach((hotel) => {
      const popupHtml = `
        <div style="min-width:180px">
          <h3 style="font-weight:700;margin:0 0 4px 0">${hotel.name}</h3>
          <p style="margin:0 0 4px 0;color:#666">${'⭐'.repeat(Math.floor(hotel.starRating))} ${hotel.starRating}</p>
          <p style="margin:0 0 4px 0;color:#666">📍 ${hotel.city}</p>
          <p style="font-size:18px;font-weight:700;color:#2563eb;margin:4px 0">
            $${(hotel.discountedPrice ?? hotel.basePrice).toFixed(2)}/night
          </p>
          <a href="/hotels/${hotel.hotelId}" style="display:inline-block;margin-top:6px;padding:6px 12px;background:#2563eb;color:white;text-decoration:none;border-radius:4px;font-size:13px;font-weight:600">
            View Details
          </a>
        </div>
      `;
      L.marker([hotel.latitude, hotel.longitude], { icon: defaultIcon })
        .addTo(map)
        .bindPopup(popupHtml);
    });

    // Fit bounds if multiple hotels
    if (validHotels.length > 1) {
      const bounds = L.latLngBounds(
        validHotels.map((h) => [h.latitude, h.longitude] as [number, number])
      );
      map.fitBounds(bounds, { padding: [50, 50] });
    }

    // Container is often 0×0 until layout; fix grey tiles after tab switch
    const fixSize = () => {
      map.invalidateSize({ pan: false });
    };
    requestAnimationFrame(fixSize);
    const t = window.setTimeout(fixSize, 250);

    return () => {
      window.clearTimeout(t);
      if (mapInstance.current) {
        mapInstance.current.remove();
        mapInstance.current = null;
      }
    };
  }, [hotelsFingerprint, validHotels.length]);

  if (validHotels.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow-md p-8 text-center">
        <p className="text-gray-600">
          No hotels with location data available. Search for hotels to see them on the map.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-4">
      <div
        ref={mapRef}
        style={{ height: '600px', width: '100%', borderRadius: '0.5rem' }}
      />
    </div>
  );
}
