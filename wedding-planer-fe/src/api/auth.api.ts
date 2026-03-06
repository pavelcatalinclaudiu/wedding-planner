import apiClient from "./client";
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
} from "@/types/user.types";

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>("/auth/login", data),

  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>("/auth/register", data),

  logout: () => apiClient.post("/auth/logout"),

  refresh: (refreshToken: string) =>
    apiClient.post<{ accessToken: string; refreshToken: string }>(
      "/auth/refresh",
      {
        refreshToken,
      },
    ),

  verifyEmail: (token: string) =>
    apiClient.post("/auth/verify-email", { token }),

  requestPasswordReset: (email: string) =>
    apiClient.post("/auth/reset-password", { email }),

  resetPassword: (token: string, password: string) =>
    apiClient.post("/auth/reset-password", { token, password }),

  me: () => apiClient.get<User>("/auth/me"),

  resendVerification: (email: string) =>
    apiClient.post("/auth/resend-verification", { email }),
};
