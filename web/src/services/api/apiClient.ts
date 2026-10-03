import axios, { AxiosInstance, AxiosError } from 'axios';
import { toast } from 'react-toastify';

const API_BASE_URL = (import.meta as any).env.VITE_API_BASE_URL || 'http://localhost:8080/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });

    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        this.handleError(error);
        return Promise.reject(error);
      }
    );
  }

  private handleError(error: AxiosError) {
    if (error.response) {
      const status = error.response.status;
      const data: any = error.response.data;

      switch (status) {
        case 400:
          toast.error(data.message || 'Bad request');
          break;
        case 401:
          toast.error('Unauthorized. Please login again.');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          window.location.href = '/login';
          break;
        case 404:
          toast.error(data.message || 'Resource not found');
          break;
        case 500:
          toast.error('Server error. Please try again later.');
          break;
        default:
          toast.error(data.message || 'An error occurred');
      }
    } else if (error.request) {
      toast.error('Network error. Please check your connection.');
    } else {
      toast.error('An unexpected error occurred');
    }
  }

  public get<T>(url: string, params?: any): Promise<T> {
    return this.client.get<T>(url, { params }).then((res) => res.data);
  }

  public post<T>(url: string, data?: any, params?: any): Promise<T> {
    return this.client.post<T>(url, data, { params }).then((res) => res.data);
  }

  public put<T>(url: string, data?: any): Promise<T> {
    return this.client.put<T>(url, data).then((res) => res.data);
  }

  public delete<T>(url: string): Promise<T> {
    return this.client.delete<T>(url).then((res) => res.data);
  }
}

export const apiClient = new ApiClient();
