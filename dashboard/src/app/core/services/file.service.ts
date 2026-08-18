import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Un <a href> ou <img src> brut ne passe pas par authInterceptor et n'envoie donc
 * pas le header Authorization. Pour tout fichier protégé (ex: /files/attachments/{id}),
 * il faut le récupérer en Blob via HttpClient, puis générer une URL objet locale.
 */
@Injectable({
  providedIn: 'root'
})
export class FileService {
  constructor(private http: HttpClient) {}

  /** @param relativeUrl chemin renvoyé par le backend, ex: "/files/attachments/12" */
  downloadFile(relativeUrl: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}${relativeUrl}`, { responseType: 'blob' });
  }
}
