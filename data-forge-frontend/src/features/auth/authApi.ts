import { apiRequest } from "@/shared/api/apiClient";
import { JwtResponse, LoginRequest } from "@/features/auth/authTypes";

export function login(request: LoginRequest) {
  return apiRequest<JwtResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
    skipAuth: true,
  });
}
