export interface User {
  id: string;
  email: string;
  role: "COUPLE" | "VENDOR";
  emailVerified: boolean;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: "COUPLE" | "VENDOR";
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  tokenType: string;
}
