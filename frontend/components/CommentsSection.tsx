'use client';

import { useEffect, useMemo, useState } from 'react';
import { useAuthStore } from '@/lib/auth-store';
import { api } from '@/lib/api';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
} from 'recharts';

const SERVICE_DIMENSIONS = [
  { key: 'cleanliness', label: 'Cleanliness' },
  { key: 'staff', label: 'Staff' },
  { key: 'facilities', label: 'Facilities' },
  { key: 'location', label: 'Location' },
  { key: 'valueForMoney', label: 'Value' },
] as const;

interface Comment {
  id: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  createdAt: string;
  serviceRatings?: Record<string, number>;
}

interface RatingDistribution {
  rating: number;
  count: number;
}

interface CommentsSectionProps {
  hotelId: string;
}

function defaultServiceRatings(): Record<string, number> {
  return Object.fromEntries(SERVICE_DIMENSIONS.map((d) => [d.key, 5]));
}

export default function CommentsSection({ hotelId }: CommentsSectionProps) {
  const { user } = useAuthStore();
  const [comments, setComments] = useState<Comment[]>([]);
  const [showGraph, setShowGraph] = useState(false);
  const [distribution, setDistribution] = useState<RatingDistribution[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState({
    rating: 5,
    comment: '',
    serviceRatings: defaultServiceRatings(),
  });

  useEffect(() => {
    fetchComments();
  }, [hotelId]);

  const fetchComments = async () => {
    try {
      setLoading(true);
      const data = await api.getCommentsByHotelId(hotelId);
      const list = (data || []) as Comment[];
      setComments(list);
      calculateDistribution(list);
    } catch (error) {
      console.error('Error fetching comments:', error);
      setComments([]);
    } finally {
      setLoading(false);
    }
  };

  const calculateDistribution = (commentsList: Comment[]) => {
    const dist: { [key: number]: number } = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };

    commentsList.forEach((c) => {
      const rating = Math.floor(c.rating);
      if (rating >= 1 && rating <= 5) {
        dist[rating]++;
      }
    });

    const distArray: RatingDistribution[] = Object.entries(dist).map(([rating, count]) => ({
      rating: parseInt(rating, 10),
      count,
    }));

    setDistribution(distArray);
  };

  const serviceRadarData = useMemo(() => {
    const rows: { dimension: string; score: number }[] = [];
    for (const d of SERVICE_DIMENSIONS) {
      let sum = 0;
      let n = 0;
      for (const c of comments) {
        const v = c.serviceRatings?.[d.key];
        if (typeof v === 'number' && v >= 1 && v <= 5) {
          sum += v;
          n++;
        }
      }
      if (n > 0) {
        rows.push({ dimension: d.label, score: Math.round((sum / n) * 10) / 10 });
      }
    }
    return rows;
  }, [comments]);

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      alert('Please login to leave a comment');
      return;
    }

    if (!newComment.comment.trim()) {
      alert('Please write a comment');
      return;
    }

    try {
      await api.createComment({
        hotelId,
        userId: user.id,
        userName: user.user_metadata?.full_name || user.email,
        rating: newComment.rating,
        comment: newComment.comment,
        serviceRatings: newComment.serviceRatings,
      });

      setNewComment({ rating: 5, comment: '', serviceRatings: defaultServiceRatings() });
      fetchComments();
    } catch (error) {
      console.error('Error creating comment:', error);
      alert('Failed to post comment');
    }
  };

  const averageRating =
    comments.length > 0 ? (comments.reduce((sum, c) => sum + c.rating, 0) / comments.length).toFixed(1) : '0.0';

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Comments & Ratings</h2>
          <p className="text-gray-600 mt-1">
            {comments.length} reviews • Average: {averageRating} ⭐
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowGraph(!showGraph)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition"
        >
          {showGraph ? '📊 Hide Graphs' : '📊 Show Graphs'}
        </button>
      </div>

      {showGraph && (
        <div className="mb-8 space-y-8">
          <div className="bg-gray-50 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Overall rating distribution</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={distribution}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="rating"
                  label={{ value: 'Rating (Stars)', position: 'insideBottom', offset: -5 }}
                />
                <YAxis
                  label={{ value: 'Number of Reviews', angle: -90, position: 'insideLeft' }}
                />
                <Tooltip />
                <Legend />
                <Bar dataKey="count" fill="#3B82F6" name="Number of Reviews" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-gray-50 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Service ratings (MongoDB field serviceRatings)</h3>
            <p className="text-sm text-gray-600 mb-4">
              Averages from per-review scores: cleanliness, staff, facilities, location, value.
            </p>
            {serviceRadarData.length === 0 ? (
              <p className="text-gray-600 text-center py-8">No service breakdown yet — reviews need sub-scores.</p>
            ) : (
              <ResponsiveContainer width="100%" height={340}>
                <RadarChart cx="50%" cy="50%" outerRadius="75%" data={serviceRadarData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="dimension" tick={{ fill: '#374151', fontSize: 12 }} />
                  <PolarRadiusAxis angle={45} domain={[0, 5]} tickCount={6} />
                  <Radar
                    name="Average (1–5)"
                    dataKey="score"
                    stroke="#2563EB"
                    fill="#3B82F6"
                    fillOpacity={0.45}
                  />
                  <Tooltip />
                  <Legend />
                </RadarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      )}

      {user && (
        <form onSubmit={handleSubmitComment} className="mb-6 p-4 bg-blue-50 rounded-lg">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">Leave a Review</h3>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">Overall rating</label>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setNewComment({ ...newComment, rating: star })}
                  className={`text-3xl ${star <= newComment.rating ? 'text-yellow-500' : 'text-gray-300'}`}
                >
                  ★
                </button>
              ))}
            </div>
          </div>

          <div className="mb-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
            {SERVICE_DIMENSIONS.map((d) => (
              <div key={d.key}>
                <label className="block text-xs font-medium text-gray-600 mb-1">{d.label} (1–5)</label>
                <input
                  type="range"
                  min={1}
                  max={5}
                  step={1}
                  value={newComment.serviceRatings[d.key] ?? 5}
                  onChange={(e) =>
                    setNewComment({
                      ...newComment,
                      serviceRatings: {
                        ...newComment.serviceRatings,
                        [d.key]: parseInt(e.target.value, 10),
                      },
                    })
                  }
                  className="w-full accent-blue-600"
                />
                <span className="text-xs text-gray-500">{newComment.serviceRatings[d.key] ?? 5} / 5</span>
              </div>
            ))}
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">Your Review</label>
            <textarea
              value={newComment.comment}
              onChange={(e) => setNewComment({ ...newComment, comment: e.target.value })}
              rows={4}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-gray-900"
              placeholder="Share your experience..."
            />
          </div>

          <button
            type="submit"
            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition"
          >
            Post Review
          </button>
        </form>
      )}

      <div className="space-y-4">
        {loading ? (
          <p className="text-gray-600 text-center py-8">Loading comments...</p>
        ) : comments.length === 0 ? (
          <p className="text-gray-600 text-center py-8">No reviews yet. Be the first to review!</p>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className="border-b border-gray-200 pb-4 last:border-0">
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="font-semibold text-gray-900">{comment.userName}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-yellow-500">
                      {'★'.repeat(Math.floor(comment.rating))}
                      {'☆'.repeat(5 - Math.floor(comment.rating))}
                    </span>
                    <span className="text-sm text-gray-600">{comment.rating}/5</span>
                  </div>
                </div>
                <span className="text-sm text-gray-500">
                  {new Date(comment.createdAt).toLocaleDateString()}
                </span>
              </div>
              <p className="text-gray-700">{comment.comment}</p>
              {comment.serviceRatings && Object.keys(comment.serviceRatings).length > 0 && (
                <ul className="mt-2 flex flex-wrap gap-2 text-xs text-gray-600">
                  {SERVICE_DIMENSIONS.map((d) => {
                    const v = comment.serviceRatings![d.key];
                    if (typeof v !== 'number') return null;
                    return (
                      <li key={d.key} className="bg-gray-100 px-2 py-0.5 rounded">
                        {d.label}: {v}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
