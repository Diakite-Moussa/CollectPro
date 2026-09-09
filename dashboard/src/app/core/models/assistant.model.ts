export interface AssistantChatRequest {
  message: string;
  confirm?: boolean | null;
  pendingActionId?: string | null;
  history?: { sender: string; text: string }[];
}

export interface AssistantChatResponse {
  reply: string;
  requiresConfirmation: boolean;
  pendingActionId?: string | null;
  reportId?: number | null;
  navigateTo?: string | null;
  exportFormat?: 'excel' | 'csv' | null;
}

export type AssistantState = 'IDLE' | 'LISTENING' | 'PROCESSING' | 'SPEAKING' | 'CONFIRMING';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: Date;
  requiresConfirmation?: boolean;
  pendingActionId?: string | null;
  reportId?: number | null;
  navigateTo?: string | null;
  exportFormat?: 'excel' | 'csv' | null;
}

