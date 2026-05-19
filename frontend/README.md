# Hotel Booking System - Frontend

Modern Next.js frontend for the Hotel Booking System.

## Features

- 🔍 Hotel search with filters
- 📅 Date-based availability checking
- 💳 Online booking
- ⭐ Reviews and ratings
- 🗺️ Map view
- 🤖 AI chatbot assistant
- 👤 User authentication
- 📱 Responsive design

## Tech Stack

- Next.js 15
- TypeScript
- Tailwind CSS
- React Leaflet (Maps)
- Zustand (State Management)
- Axios

## Getting Started

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

## Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_SUPABASE_URL=your_supabase_url
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_key
```

## Pages

- `/` - Home page with search
- `/search` - Search results
- `/hotels/[id]` - Hotel details
- `/admin` - Admin dashboard

## API Integration

All API calls go through the API Gateway at `http://localhost:8080/api/v1`

## Deployment

Deploy to Azure Static Web Apps or Vercel.
