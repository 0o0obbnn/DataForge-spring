export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  username: string;
  expiresIn: number;
}

export interface AuthSession extends JwtResponse {
  mock: boolean;
}
