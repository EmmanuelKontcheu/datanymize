import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Central API service for all HTTP requests to the Datanymize backend.
 * 
 * Provides methods for:
 * - Connection management
 * - Schema operations
 * - PII scanning
 * - Configuration management
 * - Anonymization
 * - Export
 * - Audit logging
 * - Authentication
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // ============ Generic HTTP Methods ============

  get(endpoint: string, params?: any): Observable<any> {
    let httpParams = new HttpParams();
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key]) {
          httpParams = httpParams.set(key, params[key]);
        }
      });
    }
    return this.http.get(`${this.apiUrl}${endpoint}`, { params: httpParams });
  }

  post(endpoint: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}${endpoint}`, data);
  }

  put(endpoint: string, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}${endpoint}`, data);
  }

  delete(endpoint: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}${endpoint}`);
  }

  // ============ Connection Management ============

  createConnection(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/connections`, data);
  }

  listConnections(): Observable<any> {
    return this.http.get(`${this.apiUrl}/connections`);
  }

  getConnection(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/connections/${id}`);
  }

  testConnection(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/connections/${id}/test`, {});
  }

  updateConnection(id: string, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/connections/${id}`, data);
  }

  deleteConnection(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/connections/${id}`);
  }

  // ============ Schema Management ============

  extractSchema(connectionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/schemas/extract`, { connectionId });
  }

  syncSchema(sourceConnectionId: string, targetConnectionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/schemas/sync`, {
      sourceConnectionId,
      targetConnectionId
    });
  }

  getSchema(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/schemas/${id}`);
  }

  compareSchemas(sourceSchemaId: string, targetSchemaId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/schemas/compare`, {
      sourceSchemaId,
      targetSchemaId
    });
  }

  // ============ PII Detection ============

  startPiiScan(connectionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/pii-scans`, { connectionId });
  }

  getPiiScanStatus(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/pii-scans/${id}`);
  }

  getPiiScanResults(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/pii-scans/${id}/results`);
  }

  overridePiiClassification(id: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/pii-scans/${id}/override`, data);
  }

  // ============ Configuration Management ============

  createConfiguration(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/configurations`, data);
  }

  getConfiguration(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/configurations/${id}`);
  }

  updateConfiguration(id: string, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/configurations/${id}`, data);
  }

  getConfigurationVersions(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/configurations/${id}/versions`);
  }

  restoreConfigurationVersion(id: string, versionNumber: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/configurations/${id}/restore`, { versionNumber });
  }

  // ============ Anonymization ============

  startAnonymization(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/anonymizations`, data);
  }

  getAnonymizationStatus(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/anonymizations/${id}`);
  }

  getAnonymizationProgress(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/anonymizations/${id}/progress`);
  }

  cancelAnonymization(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/anonymizations/${id}/cancel`, {});
  }

  getAnonymizationResults(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/anonymizations/${id}/results`);
  }

  // ============ Export ============

  startExport(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/exports`, data);
  }

  getExportStatus(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/exports/${id}`);
  }

  downloadExport(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/exports/${id}/download`);
  }

  getExportProgress(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/exports/${id}/progress`);
  }

  // ============ Audit Logs ============

  listAuditLogs(params?: any): Observable<any> {
    let httpParams = new HttpParams();
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key]) {
          httpParams = httpParams.set(key, params[key]);
        }
      });
    }
    return this.http.get(`${this.apiUrl}/audit-logs`, { params: httpParams });
  }

  getAuditLog(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/audit-logs/${id}`);
  }

  exportAuditLogs(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/audit-logs/export`, data);
  }

  // ============ Authentication ============

  login(username: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/login`, { username, password });
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {});
  }

  refreshToken(refreshToken: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/refresh`, { refreshToken });
  }

  getCurrentUser(): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/me`);
  }
}
