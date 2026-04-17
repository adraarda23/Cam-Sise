# Frontend Integration Guide

> **Palet & Separatör Havuz Yönetim Sistemi - Frontend Entegrasyon Rehberi**
> API Endpoint Kataloğu, Authentication Flow ve React Implementation Örnekleri

---

## 📋 İçindekiler

1. [Authentication & Authorization](#1-authentication--authorization)
2. [API Endpoint Catalog by Role](#2-api-endpoint-catalog-by-role)
3. [Frontend Architecture Recommendation](#3-frontend-architecture-recommendation)
4. [Setup & Configuration](#4-setup--configuration)
5. [Authentication Implementation](#5-authentication-implementation)
6. [API Service Layer](#6-api-service-layer)
7. [Component Examples](#7-component-examples)
8. [Map Integration](#8-map-integration)
9. [Error Handling](#9-error-handling)
10. [Environment Variables](#10-environment-variables)

---

## 1. Authentication & Authorization

### 1.1 Login Flow

```
┌─────────┐         POST /api/auth/login          ┌─────────┐
│ Client  │ ─────────────────────────────────────> │  Server │
│         │  { username, password }                │         │
│         │                                        │         │
│         │ <───────────────────────────────────── │         │
│         │  { token, username, role,              │         │
│         │    fullName, fillerId }                │         │
└─────────┘                                        └─────────┘
     │
     ├─ Store token in localStorage
     ├─ Store user info in state/context
     └─ Redirect to role-based dashboard
```

### 1.2 Request/Response DTOs

**Login Request:**
```typescript
interface LoginRequest {
  username: string;
  password: string;
}
```

**Login Response:**
```typescript
interface LoginResponse {
  token: string;           // JWT token
  username: string;        // "admin"
  role: 'ADMIN' | 'COMPANY_STAFF' | 'CUSTOMER';
  fullName: string;        // "Admin User"
  fillerId?: number;       // Only for CUSTOMER role
}
```

**Example Request:**
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Example Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN",
  "fullName": "System Administrator",
  "fillerId": null
}
```

### 1.3 Roles & Permissions

| Role | Description | Access Level |
|------|-------------|--------------|
| **ADMIN** | System administrator | Full access to all resources |
| **COMPANY_STAFF** | Havuz operatörü personeli | Manage fillers, stocks, routes, approve requests |
| **CUSTOMER** | Dolumcu kullanıcısı | View own stock, create manual requests |

---

## 2. API Endpoint Catalog by Role

### 2.1 ADMIN Endpoints

#### Pool Operators (Full CRUD)
```
POST   /api/pool-operators              - Register new pool operator
GET    /api/pool-operators              - List all pool operators
GET    /api/pool-operators/{id}         - Get pool operator details
POST   /api/pool-operators/{id}/activate   - Activate pool operator
POST   /api/pool-operators/{id}/deactivate - Deactivate pool operator
PUT    /api/pool-operators/{id}/contact    - Update contact info
```

#### Full Read Access
- All Fillers
- All Collection Requests
- All Collection Plans
- All Stocks
- All Loss Records

---

### 2.2 COMPANY_STAFF Endpoints

#### Fillers Management
```
POST   /api/fillers                     - Register new filler
GET    /api/fillers                     - List all fillers
GET    /api/fillers/{id}                - Get filler details
POST   /api/fillers/{id}/activate       - Activate filler
POST   /api/fillers/{id}/deactivate     - Deactivate filler
PUT    /api/fillers/{id}/contact        - Update contact info
PUT    /api/fillers/{id}/location       - Update GPS coordinates
```

**Example: Register Filler**
```typescript
const registerFiller = async (data: RegisterFillerRequest) => {
  const response = await axios.post('/api/fillers', {
    poolOperatorId: 1,
    name: "Coca-Cola Bursa Dolum",
    street: "OSB 5. Cadde No:12",
    city: "Bursa",
    province: "Osmangazi",
    postalCode: "16200",
    country: "Türkiye",
    latitude: 40.2108,
    longitude: 29.0138,
    contactPhone: "02241234567",
    contactEmail: "bursa@cocacola.com",
    contactPersonName: "Ahmet Yılmaz",
    taxId: "1234567890"
  });
  return response.data;
};
```

#### Stock Management
```
POST   /api/inventory/stocks/record-inflow      - Record stock inflow
POST   /api/inventory/stocks/record-collection  - Record stock collection
GET    /api/inventory/stocks                    - List all stocks
GET    /api/inventory/stocks/filler/{fillerId}  - Get filler stocks
PUT    /api/inventory/stocks/{id}/threshold     - Update threshold
```

**Example: Record Stock Inflow**
```typescript
interface RecordInflowRequest {
  fillerId: number;
  assetType: 'PALLET' | 'SEPARATOR';
  quantity: number;
  referenceId?: string;
}

const recordInflow = async (data: RecordInflowRequest) => {
  const response = await axios.post('/api/inventory/stocks/record-inflow', data);
  return response.data;
};
```

#### Collection Requests Management
```
GET    /api/logistics/collection-requests                  - List all requests
GET    /api/logistics/collection-requests/{id}             - Get request details
POST   /api/logistics/collection-requests/{id}/approve     - Approve request
POST   /api/logistics/collection-requests/{id}/reject      - Reject request
POST   /api/logistics/collection-requests/{id}/cancel      - Cancel request
```

**Example: Approve Collection Request**
```typescript
const approveRequest = async (requestId: number, approvingUserId: number) => {
  const response = await axios.post(
    `/api/logistics/collection-requests/${requestId}/approve`,
    { approvingUserId }
  );
  return response.data;
};
```

#### Route Optimization (CVRP) - ⭐ CORE FEATURE
```
POST   /api/logistics/optimize                   - Generate optimized plan (single vehicle)
POST   /api/logistics/optimize/custom            - Generate plan for specific requests
POST   /api/logistics/optimize/multi-vehicle     - Generate multi-vehicle routes ⚡
```

**Example: Multi-Vehicle Route Optimization**
```typescript
interface OptimizeRouteRequest {
  depotId: number;
  plannedDate?: string;  // "2026-04-20" (default: tomorrow)
}

interface MultiVehicleOptimizeRequest {
  depotId: number;
  plannedDate?: string;
  maxVehicles?: number;  // default: 10
}

interface CollectionPlan {
  id: number;
  depotId: number;
  vehicleId?: number;
  plannedDate: string;
  status: 'PLANNED' | 'VEHICLE_ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  totalDistance: { kilometers: number };
  estimatedDuration: { minutes: number };
  routeStopsJson: string;  // JSON array of route stops
  totalCapacityPallets: number;
  totalCapacitySeparators: number;
  createdAt: string;
}

interface MultiVehicleOptimizeResponse {
  plans: CollectionPlan[];
  vehiclesUsed: number;
  totalDistanceKm: number;
  totalPallets: number;
  totalSeparators: number;
}

// Usage
const optimizeMultiVehicleRoutes = async () => {
  const response = await axios.post<MultiVehicleOptimizeResponse>(
    '/api/logistics/optimize/multi-vehicle',
    {
      depotId: 1,
      plannedDate: '2026-04-20',
      maxVehicles: 5
    }
  );

  console.log(`Generated ${response.data.vehiclesUsed} routes`);
  console.log(`Total distance: ${response.data.totalDistanceKm} km`);
  console.log(`Total pallets: ${response.data.totalPallets}`);

  return response.data;
};
```

#### Collection Plans Management
```
GET    /api/logistics/collection-plans               - List all plans
GET    /api/logistics/collection-plans/{id}          - Get plan details
POST   /api/logistics/collection-plans/{id}/assign-vehicle  - Assign vehicle
POST   /api/logistics/collection-plans/{id}/start            - Start collection
POST   /api/logistics/collection-plans/{id}/complete         - Complete collection
POST   /api/logistics/collection-plans/{id}/cancel           - Cancel plan
```

---

### 2.3 CUSTOMER Endpoints

#### My Filler Info
```
GET    /api/fillers/{id}                - Get own filler details (fillerId from login)
```

#### My Stock
```
GET    /api/inventory/stocks/filler/{fillerId}  - View own stock levels
```

**Example: Get My Stock**
```typescript
const getMyStock = async (fillerId: number) => {
  const response = await axios.get(`/api/inventory/stocks/filler/${fillerId}`);
  return response.data;
};

// Response example
interface FillerStock {
  id: number;
  fillerId: number;
  assetType: 'PALLET' | 'SEPARATOR';
  currentQuantity: number;
  threshold: number;
  lossRatePercentage: number;
  lastInflowDate?: string;
  lastCollectionDate?: string;
}
```

#### My Loss Records
```
PUT    /api/inventory/loss-records/{id}/update  - Update own loss rate
```

#### Manual Collection Requests
```
POST   /api/logistics/collection-requests/manual         - Create manual request
GET    /api/logistics/collection-requests/filler/{fillerId}  - View own requests
POST   /api/logistics/collection-requests/{id}/cancel    - Cancel own request
```

**Example: Create Manual Collection Request**
```typescript
interface CreateManualRequestRequest {
  fillerId: number;
  assetType: 'PALLET' | 'SEPARATOR';
  estimatedQuantity: number;
  requestingUserId?: number;
}

const createManualRequest = async (data: CreateManualRequestRequest) => {
  const response = await axios.post(
    '/api/logistics/collection-requests/manual',
    data
  );
  return response.data;
};

// Usage
const requestCollection = async (fillerId: number) => {
  const request = await createManualRequest({
    fillerId: fillerId,
    assetType: 'PALLET',
    estimatedQuantity: 50,
    requestingUserId: 1
  });

  console.log(`Request created with status: ${request.status}`);
  // status: PENDING → APPROVED → ASSIGNED_TO_PLAN → COLLECTED
};
```

---

## 3. Frontend Architecture Recommendation

### 3.1 Tech Stack

```
├── React 18           (UI Library)
├── TypeScript         (Type Safety)
├── Vite              (Build Tool - Fast HMR)
├── React Router v6   (Routing)
├── Axios             (HTTP Client)
├── Tailwind CSS      (Styling) or Material-UI
├── Leaflet.js        (Map Visualization)
├── React Query       (Server State Management - Optional but Recommended)
├── Zustand/Context   (Client State Management)
```

### 3.2 Folder Structure

```
frontend/
├── public/
├── src/
│   ├── api/
│   │   ├── axiosConfig.ts          # Axios instance + interceptors
│   │   ├── authApi.ts              # Authentication endpoints
│   │   ├── fillerApi.ts            # Filler endpoints
│   │   ├── stockApi.ts             # Stock endpoints
│   │   ├── collectionApi.ts        # Collection request endpoints
│   │   ├── routeApi.ts             # Route optimization endpoints
│   │   └── index.ts                # Export all APIs
│   ├── components/
│   │   ├── auth/
│   │   │   ├── LoginForm.tsx
│   │   │   └── ProtectedRoute.tsx
│   │   ├── dashboard/
│   │   │   ├── StaffDashboard.tsx
│   │   │   ├── CustomerDashboard.tsx
│   │   │   └── AdminDashboard.tsx
│   │   ├── filler/
│   │   │   ├── FillerList.tsx
│   │   │   ├── FillerForm.tsx
│   │   │   └── FillerCard.tsx
│   │   ├── route/
│   │   │   ├── RouteOptimizationForm.tsx
│   │   │   ├── RouteList.tsx
│   │   │   └── RouteMap.tsx
│   │   ├── stock/
│   │   │   ├── StockOverview.tsx
│   │   │   └── StockCard.tsx
│   │   ├── request/
│   │   │   ├── RequestList.tsx
│   │   │   ├── RequestForm.tsx
│   │   │   └── RequestApprovalModal.tsx
│   │   └── map/
│   │       ├── MapContainer.tsx
│   │       ├── DepotMarker.tsx
│   │       ├── FillerMarker.tsx
│   │       └── RoutePolyline.tsx
│   ├── hooks/
│   │   ├── useAuth.ts              # Authentication hook
│   │   ├── useRole.ts              # Role-based access hook
│   │   └── useApi.ts               # Generic API hook
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── FillersPage.tsx
│   │   ├── StocksPage.tsx
│   │   ├── CollectionRequestsPage.tsx
│   │   ├── RouteOptimizationPage.tsx
│   │   └── NotFoundPage.tsx
│   ├── types/
│   │   ├── api.types.ts            # API request/response types
│   │   ├── domain.types.ts         # Domain entity types
│   │   └── auth.types.ts           # Auth types
│   ├── utils/
│   │   ├── storage.ts              # localStorage utilities
│   │   └── constants.ts            # API routes, constants
│   ├── context/
│   │   └── AuthContext.tsx         # Auth state context
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── .env
├── .env.example
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 4. Setup & Configuration

### 4.1 Project Setup

```bash
# Create Vite + React + TypeScript project
npm create vite@latest pallet-management-frontend -- --template react-ts
cd pallet-management-frontend

# Install dependencies
npm install axios react-router-dom
npm install -D @types/node

# Install UI library (choose one)
npm install @mui/material @emotion/react @emotion/styled
# OR
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Install map library
npm install react-leaflet leaflet
npm install -D @types/leaflet

# Optional: Install React Query for better server state management
npm install @tanstack/react-query

# Start dev server
npm run dev
```

### 4.2 Environment Variables

Create `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Pallet Management System
```

Create `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Pallet Management System
```

---

## 5. Authentication Implementation

### 5.1 Axios Configuration

**File: `src/api/axiosConfig.ts`**

```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add JWT token to all requests
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('jwt_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - Handle 401 errors (token expired)
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - logout user
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_info');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 5.2 Auth API Service

**File: `src/api/authApi.ts`**

```typescript
import apiClient from './axiosConfig';
import { LoginRequest, LoginResponse } from '../types/auth.types';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', credentials);
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_info');
  },

  getCurrentUser: (): LoginResponse | null => {
    const userInfo = localStorage.getItem('user_info');
    return userInfo ? JSON.parse(userInfo) : null;
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('jwt_token');
  },
};
```

### 5.3 Auth Types

**File: `src/types/auth.types.ts`**

```typescript
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: 'ADMIN' | 'COMPANY_STAFF' | 'CUSTOMER';
  fullName: string;
  fillerId?: number;
}

export interface User {
  username: string;
  role: 'ADMIN' | 'COMPANY_STAFF' | 'CUSTOMER';
  fullName: string;
  fillerId?: number;
}
```

### 5.4 Auth Context

**File: `src/context/AuthContext.tsx`**

```typescript
import React, { createContext, useContext, useState, useEffect } from 'react';
import { LoginRequest, LoginResponse, User } from '../types/auth.types';
import { authApi } from '../api/authApi';

interface AuthContextType {
  user: User | null;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check if user is already logged in
    const currentUser = authApi.getCurrentUser();
    if (currentUser) {
      setUser({
        username: currentUser.username,
        role: currentUser.role,
        fullName: currentUser.fullName,
        fillerId: currentUser.fillerId,
      });
    }
    setIsLoading(false);
  }, []);

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);

    // Store token and user info
    localStorage.setItem('jwt_token', response.token);
    localStorage.setItem('user_info', JSON.stringify(response));

    setUser({
      username: response.username,
      role: response.role,
      fullName: response.fullName,
      fillerId: response.fillerId,
    });
  };

  const logout = () => {
    authApi.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        isAuthenticated: !!user,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### 5.5 Protected Route Component

**File: `src/components/auth/ProtectedRoute.tsx`**

```typescript
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

interface ProtectedRouteProps {
  allowedRoles?: Array<'ADMIN' | 'COMPANY_STAFF' | 'CUSTOMER'>;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};
```

### 5.6 Login Component

**File: `src/components/auth/LoginForm.tsx`**

```typescript
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const LoginForm: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login({ username, password });
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Giriş başarısız. Lütfen tekrar deneyin.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-lg shadow-md">
        <div>
          <h2 className="text-center text-3xl font-extrabold text-gray-900">
            Palet Yönetim Sistemi
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Lütfen giriş yapın
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          <div className="rounded-md shadow-sm space-y-4">
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                Kullanıcı Adı
              </label>
              <input
                id="username"
                name="username"
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="mt-1 appearance-none rounded block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                placeholder="Kullanıcı adı"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                Şifre
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 appearance-none rounded block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                placeholder="Şifre"
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={isLoading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-gray-400"
            >
              {isLoading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
            </button>
          </div>
        </form>

        <div className="text-center text-xs text-gray-500">
          <p>Test Kullanıcıları:</p>
          <p>Admin: admin / password123</p>
          <p>Staff: staff / password123</p>
          <p>Customer: customer / password123</p>
        </div>
      </div>
    </div>
  );
};
```

---

## 6. API Service Layer

### 6.1 Route Optimization API

**File: `src/api/routeApi.ts`**

```typescript
import apiClient from './axiosConfig';

export interface OptimizeRouteRequest {
  depotId: number;
  plannedDate?: string;
}

export interface MultiVehicleOptimizeRequest {
  depotId: number;
  plannedDate?: string;
  maxVehicles?: number;
}

export interface CollectionPlan {
  id: number;
  depotId: number;
  vehicleId?: number;
  plannedDate: string;
  status: string;
  totalDistance: { kilometers: number };
  estimatedDuration: { minutes: number };
  routeStopsJson: string;
  totalCapacityPallets: number;
  totalCapacitySeparators: number;
}

export interface MultiVehicleOptimizeResponse {
  plans: CollectionPlan[];
  vehiclesUsed: number;
  totalDistanceKm: number;
  totalPallets: number;
  totalSeparators: number;
}

export const routeApi = {
  optimizeSingleVehicle: async (data: OptimizeRouteRequest): Promise<CollectionPlan> => {
    const response = await apiClient.post('/api/logistics/optimize', data);
    return response.data;
  },

  optimizeMultiVehicle: async (data: MultiVehicleOptimizeRequest): Promise<MultiVehicleOptimizeResponse> => {
    const response = await apiClient.post('/api/logistics/optimize/multi-vehicle', data);
    return response.data;
  },

  getCollectionPlans: async (): Promise<CollectionPlan[]> => {
    const response = await apiClient.get('/api/logistics/collection-plans');
    return response.data;
  },

  getCollectionPlanById: async (id: number): Promise<CollectionPlan> => {
    const response = await apiClient.get(`/api/logistics/collection-plans/${id}`);
    return response.data;
  },

  assignVehicle: async (planId: number, vehicleId: number): Promise<CollectionPlan> => {
    const response = await apiClient.post(`/api/logistics/collection-plans/${planId}/assign-vehicle`, {
      vehicleId
    });
    return response.data;
  },

  startCollection: async (planId: number): Promise<CollectionPlan> => {
    const response = await apiClient.post(`/api/logistics/collection-plans/${planId}/start`);
    return response.data;
  },

  completeCollection: async (
    planId: number,
    actualPallets: number,
    actualSeparators: number
  ): Promise<CollectionPlan> => {
    const response = await apiClient.post(`/api/logistics/collection-plans/${planId}/complete`, {
      actualPalletsCollected: actualPallets,
      actualSeparatorsCollected: actualSeparators
    });
    return response.data;
  },
};
```

### 6.2 Collection Request API

**File: `src/api/collectionApi.ts`**

```typescript
import apiClient from './axiosConfig';

export interface CreateManualRequestRequest {
  fillerId: number;
  assetType: 'PALLET' | 'SEPARATOR';
  estimatedQuantity: number;
  requestingUserId?: number;
}

export interface CollectionRequest {
  id: number;
  fillerId: number;
  assetType: 'PALLET' | 'SEPARATOR';
  estimatedQuantity: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ASSIGNED_TO_PLAN' | 'COLLECTED' | 'CANCELLED';
  requestedAt: string;
  approvedAt?: string;
  rejectedReason?: string;
}

export const collectionApi = {
  createManualRequest: async (data: CreateManualRequestRequest): Promise<CollectionRequest> => {
    const response = await apiClient.post('/api/logistics/collection-requests/manual', data);
    return response.data;
  },

  approveRequest: async (requestId: number, approvingUserId: number): Promise<CollectionRequest> => {
    const response = await apiClient.post(`/api/logistics/collection-requests/${requestId}/approve`, {
      approvingUserId
    });
    return response.data;
  },

  rejectRequest: async (requestId: number, reason: string): Promise<CollectionRequest> => {
    const response = await apiClient.post(`/api/logistics/collection-requests/${requestId}/reject`, {
      reason
    });
    return response.data;
  },

  cancelRequest: async (requestId: number): Promise<CollectionRequest> => {
    const response = await apiClient.post(`/api/logistics/collection-requests/${requestId}/cancel`);
    return response.data;
  },

  getAll: async (): Promise<CollectionRequest[]> => {
    const response = await apiClient.get('/api/logistics/collection-requests');
    return response.data;
  },

  getByFiller: async (fillerId: number): Promise<CollectionRequest[]> => {
    const response = await apiClient.get(`/api/logistics/collection-requests/filler/${fillerId}`);
    return response.data;
  },
};
```

---

## 7. Component Examples

### 7.1 Route Optimization Form Component

**File: `src/components/route/RouteOptimizationForm.tsx`**

```typescript
import React, { useState } from 'react';
import { routeApi, MultiVehicleOptimizeResponse } from '../../api/routeApi';

export const RouteOptimizationForm: React.FC = () => {
  const [depotId, setDepotId] = useState<number>(1);
  const [plannedDate, setPlannedDate] = useState<string>('');
  const [maxVehicles, setMaxVehicles] = useState<number>(10);
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<MultiVehicleOptimizeResponse | null>(null);
  const [error, setError] = useState<string>('');

  const handleOptimize = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setResult(null);

    try {
      const response = await routeApi.optimizeMultiVehicle({
        depotId,
        plannedDate: plannedDate || undefined,
        maxVehicles,
      });
      setResult(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Optimizasyon başarısız oldu');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-md">
      <h2 className="text-2xl font-bold mb-4">Rota Optimizasyonu (CVRP)</h2>

      <form onSubmit={handleOptimize} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Depo ID</label>
          <input
            type="number"
            value={depotId}
            onChange={(e) => setDepotId(Number(e.target.value))}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">
            Planlanan Tarih (opsiyonel)
          </label>
          <input
            type="date"
            value={plannedDate}
            onChange={(e) => setPlannedDate(e.target.value)}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">
            Maksimum Araç Sayısı
          </label>
          <input
            type="number"
            value={maxVehicles}
            onChange={(e) => setMaxVehicles(Number(e.target.value))}
            min={1}
            max={20}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-indigo-600 text-white py-2 px-4 rounded-md hover:bg-indigo-700 disabled:bg-gray-400"
        >
          {isLoading ? 'Optimize Ediliyor...' : 'Rotaları Optimize Et'}
        </button>
      </form>

      {error && (
        <div className="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {result && (
        <div className="mt-6 space-y-4">
          <h3 className="text-xl font-semibold">Optimizasyon Sonucu</h3>

          <div className="grid grid-cols-2 gap-4">
            <div className="bg-blue-50 p-4 rounded">
              <p className="text-sm text-gray-600">Kullanılan Araç</p>
              <p className="text-2xl font-bold">{result.vehiclesUsed}</p>
            </div>

            <div className="bg-green-50 p-4 rounded">
              <p className="text-sm text-gray-600">Toplam Mesafe</p>
              <p className="text-2xl font-bold">{result.totalDistanceKm.toFixed(2)} km</p>
            </div>

            <div className="bg-yellow-50 p-4 rounded">
              <p className="text-sm text-gray-600">Toplam Palet</p>
              <p className="text-2xl font-bold">{result.totalPallets}</p>
            </div>

            <div className="bg-purple-50 p-4 rounded">
              <p className="text-sm text-gray-600">Toplam Ayırıcı</p>
              <p className="text-2xl font-bold">{result.totalSeparators}</p>
            </div>
          </div>

          <div className="border-t pt-4">
            <h4 className="font-semibold mb-2">Oluşturulan Rotalar:</h4>
            {result.plans.map((plan, index) => (
              <div key={plan.id} className="bg-gray-50 p-3 rounded mb-2">
                <p className="font-medium">Rota #{index + 1}</p>
                <p className="text-sm">Mesafe: {plan.totalDistance.kilometers.toFixed(2)} km</p>
                <p className="text-sm">Süre: {plan.estimatedDuration.minutes} dakika</p>
                <p className="text-sm">Palet: {plan.totalCapacityPallets}, Ayırıcı: {plan.totalCapacitySeparators}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
```

### 7.2 Manual Collection Request Form (CUSTOMER)

**File: `src/components/request/ManualRequestForm.tsx`**

```typescript
import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { collectionApi } from '../../api/collectionApi';

export const ManualRequestForm: React.FC = () => {
  const { user } = useAuth();
  const [assetType, setAssetType] = useState<'PALLET' | 'SEPARATOR'>('PALLET');
  const [quantity, setQuantity] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user?.fillerId) {
      setError('Dolumcu bilgisi bulunamadı');
      return;
    }

    setIsLoading(true);
    setError('');
    setSuccess(false);

    try {
      await collectionApi.createManualRequest({
        fillerId: user.fillerId,
        assetType,
        estimatedQuantity: quantity,
      });

      setSuccess(true);
      setQuantity(0);

      // Reset success message after 3 seconds
      setTimeout(() => setSuccess(false), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Talep oluşturulamadı');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-md">
      <h2 className="text-2xl font-bold mb-4">Manuel Toplama Talebi</h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Asset Tipi</label>
          <select
            value={assetType}
            onChange={(e) => setAssetType(e.target.value as 'PALLET' | 'SEPARATOR')}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
          >
            <option value="PALLET">Palet</option>
            <option value="SEPARATOR">Ayırıcı</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">
            Tahmini Miktar
          </label>
          <input
            type="number"
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
            min={1}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
            required
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-indigo-600 text-white py-2 px-4 rounded-md hover:bg-indigo-700 disabled:bg-gray-400"
        >
          {isLoading ? 'Talep Oluşturuluyor...' : 'Talep Oluştur'}
        </button>
      </form>

      {success && (
        <div className="mt-4 bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded">
          Talep başarıyla oluşturuldu! Onay bekliyor.
        </div>
      )}

      {error && (
        <div className="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}
    </div>
  );
};
```

---

## 8. Map Integration

### 8.1 Leaflet Setup

```bash
npm install react-leaflet leaflet
npm install -D @types/leaflet
```

**Import Leaflet CSS in `src/main.tsx`:**

```typescript
import 'leaflet/dist/leaflet.css';
```

### 8.2 Route Map Component

**File: `src/components/map/RouteMap.tsx`**

```typescript
import React from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup } from 'react-leaflet';
import L from 'leaflet';
import { CollectionPlan } from '../../api/routeApi';

// Fix Leaflet default icon issue
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

interface RouteStop {
  sequence: number;
  fillerId: number;
  fillerName: string;
  latitude: number;
  longitude: number;
  estimatedPallets: number;
  estimatedSeparators: number;
}

interface RouteMapProps {
  plan: CollectionPlan;
  depotCoordinates: [number, number];  // [lat, lng]
}

export const RouteMap: React.FC<RouteMapProps> = ({ plan, depotCoordinates }) => {
  const routeStops: RouteStop[] = JSON.parse(plan.routeStopsJson);

  // Extract coordinates for polyline
  const routeCoordinates: [number, number][] = [
    depotCoordinates,
    ...routeStops.map(stop => [stop.latitude, stop.longitude] as [number, number]),
    depotCoordinates, // Return to depot
  ];

  // Create custom icon for depot
  const depotIcon = new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
  });

  return (
    <MapContainer
      center={depotCoordinates}
      zoom={10}
      style={{ height: '500px', width: '100%' }}
      className="rounded-lg shadow-md"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Depot Marker */}
      <Marker position={depotCoordinates} icon={depotIcon}>
        <Popup>
          <strong>Depo</strong>
          <br />
          Başlangıç/Bitiş Noktası
        </Popup>
      </Marker>

      {/* Filler Markers */}
      {routeStops.map((stop) => (
        <Marker
          key={stop.fillerId}
          position={[stop.latitude, stop.longitude]}
        >
          <Popup>
            <strong>{stop.fillerName}</strong>
            <br />
            Sıra: {stop.sequence}
            <br />
            Palet: {stop.estimatedPallets}
            <br />
            Ayırıcı: {stop.estimatedSeparators}
          </Popup>
        </Marker>
      ))}

      {/* Route Polyline */}
      <Polyline
        positions={routeCoordinates}
        color="blue"
        weight={3}
        opacity={0.7}
      />
    </MapContainer>
  );
};
```

---

## 9. Error Handling

### 9.1 Global Error Handler

**File: `src/utils/errorHandler.ts`**

```typescript
import { AxiosError } from 'axios';

export const handleApiError = (error: unknown): string => {
  if (error instanceof AxiosError) {
    // Server responded with error
    if (error.response) {
      const status = error.response.status;
      const message = error.response.data?.message || error.response.data?.error;

      switch (status) {
        case 400:
          return message || 'Geçersiz istek. Lütfen bilgilerinizi kontrol edin.';
        case 401:
          return 'Oturum süreniz dolmuş. Lütfen tekrar giriş yapın.';
        case 403:
          return 'Bu işlem için yetkiniz yok.';
        case 404:
          return 'İstenen kaynak bulunamadı.';
        case 500:
          return 'Sunucu hatası. Lütfen daha sonra tekrar deneyin.';
        default:
          return message || 'Bir hata oluştu.';
      }
    }

    // Network error
    if (error.request) {
      return 'Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edin.';
    }
  }

  return 'Beklenmeyen bir hata oluştu.';
};
```

---

## 10. Environment Variables

### 10.1 .env File

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Pallet Management System
VITE_ENABLE_DEVTOOLS=true
```

### 10.2 .env.production

```env
VITE_API_BASE_URL=https://api.yourcompany.com
VITE_APP_NAME=Pallet Management System
VITE_ENABLE_DEVTOOLS=false
```

---

## 📚 Additional Resources

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs
- **Backend README:** ../README.md
- **CVRP Documentation:** ./CVRP_IMPLEMENTATION_SUMMARY.md

---

## ✅ Checklist: Frontend Implementation

### Phase 1: Setup & Auth (2-3 hours)
- [ ] Create Vite + React + TypeScript project
- [ ] Install dependencies (axios, react-router, etc.)
- [ ] Setup Axios config with interceptors
- [ ] Implement Auth Context
- [ ] Create Login Page
- [ ] Create Protected Route component
- [ ] Test login/logout flow

### Phase 2: COMPANY_STAFF Dashboard (8-10 hours)
- [ ] Create dashboard layout
- [ ] Implement Filler list & registration
- [ ] Implement Stock overview
- [ ] Implement Collection Request approval flow
- [ ] **Implement Route Optimization form (CVRP trigger)**
- [ ] Display optimized routes
- [ ] Add filters & search

### Phase 3: CUSTOMER Dashboard (6-8 hours)
- [ ] Create customer dashboard layout
- [ ] Display own stock levels
- [ ] Implement manual request form
- [ ] Display request history
- [ ] Show request status

### Phase 4: Map Integration (6-8 hours)
- [ ] Install Leaflet.js
- [ ] Create map component
- [ ] Display depot marker
- [ ] Display filler markers
- [ ] Draw route polylines
- [ ] Add popups with info

### Phase 5: Polish & Deploy (4-6 hours)
- [ ] Error handling
- [ ] Loading states
- [ ] Responsive design
- [ ] Build for production
- [ ] Deploy to hosting

---

**Total Estimated Time: 26-35 hours**

Bu rehber ile frontend'i hızlıca kaldırabilirsiniz! 🚀
