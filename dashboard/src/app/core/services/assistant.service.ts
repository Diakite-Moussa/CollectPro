import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssistantChatRequest, AssistantChatResponse } from '../models/assistant.model';

@Injectable({
  providedIn: 'root'
})
export class AssistantService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/assistant/chat`;

  /**
   * Envoie un message texte ou une confirmation d'action à l'assistant IA.
   */
  chat(
    message: string,
    confirm?: boolean | null,
    pendingActionId?: string | null,
    history?: { sender: string; text: string }[]
  ): Observable<AssistantChatResponse> {
    const payload: AssistantChatRequest = {
      message,
      confirm: confirm !== undefined ? confirm : null,
      pendingActionId: pendingActionId || null,
      history: history || undefined
    };
    return this.http.post<AssistantChatResponse>(this.apiUrl, payload);
  }
}
