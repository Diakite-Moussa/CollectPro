import { Component, OnInit, OnDestroy, inject, signal, effect, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { AssistantService } from '../../core/services/assistant.service';
import { SpeechService } from '../../core/services/speech.service';
import { ReportService } from '../../core/services/report.service';
import { CollecteService } from '../../core/services/collecte.service';
import { AudioFeedbackService } from '../../core/services/audio-feedback.service';
import { AssistantChatResponse, AssistantState, ChatMessage } from '../../core/models/assistant.model';

@Component({
  selector: 'app-voice-assistant',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './voice-assistant.component.html',
  styleUrls: ['./voice-assistant.component.scss']
})
export class VoiceAssistantComponent implements OnInit, OnDestroy {
  private readonly assistantService = inject(AssistantService);
  private readonly reportService = inject(ReportService);
  private readonly collecteService = inject(CollecteService);
  private readonly router = inject(Router);
  private readonly audioFeedback = inject(AudioFeedbackService);
  readonly speechService = inject(SpeechService);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

  readonly isOpen = signal<boolean>(false);
  readonly state = signal<AssistantState>('IDLE');
  readonly messages = signal<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'assistant',
      text: 'Bonjour ! Je suis votre assistant CollectPro. Vous pouvez me poser des questions sur vos missions, collectes, statistiques ou effectuer des actions sécurisées.',
      timestamp: new Date()
    }
  ]);
  readonly interimTranscript = signal<string>('');
  readonly pendingConfirmation = signal<{ actionId: string | null; summary: string } | null>(null);
  readonly speechEnabled = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly copiedMessageId = signal<string | null>(null);

  readonly quickPrompts = [
    '📊 Stats du mois',
    '📋 Mes missions',
    '👥 Utilisateurs',
    '📝 Formulaires',
    '⏳ Collectes en attente',
    '📄 Rapport organisation',
    '📑 Rapport mission',
    '🚀 Lancer mission',
    '📥 Exporter collectes',
    '🧭 Aller aux missions'
  ];

  textInput = '';

  private subs = new Subscription();

  ngOnInit(): void {
    // Écoute de l'état d'enregistrement micro
    this.subs.add(
      this.speechService.isListening$.subscribe(isListening => {
        if (isListening) {
          this.state.set('LISTENING');
          this.errorMessage.set(null);
          this.audioFeedback.playListeningStart();
        } else if (this.state() === 'LISTENING') {
          this.state.set('IDLE');
          this.audioFeedback.playListeningStop();
        }
      })
    );

    // Écoute de l'état de parole TTS
    this.subs.add(
      this.speechService.isSpeaking$.subscribe(isSpeaking => {
        if (isSpeaking) {
          this.state.set('SPEAKING');
        } else if (this.state() === 'SPEAKING') {
          this.state.set(this.pendingConfirmation() ? 'CONFIRMING' : 'IDLE');
        }
      })
    );

    // Transcription partielle en temps réel
    this.subs.add(
      this.speechService.interimTranscript$.subscribe(interim => {
        this.interimTranscript.set(interim);
      })
    );

    // Transcription finale validée par le moteur vocal
    this.subs.add(
      this.speechService.finalTranscript$.subscribe(finalText => {
        this.handleVocalInput(finalText);
      })
    );

    // Gestion des erreurs micro
    this.subs.add(
      this.speechService.error$.subscribe(err => {
        this.errorMessage.set(err);
        setTimeout(() => this.errorMessage.set(null), 5000);
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.speechService.stopSpeaking();
    this.speechService.stopListening();
  }

  togglePanel(): void {
    this.isOpen.update(v => !v);
    if (this.isOpen()) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  toggleListening(): void {
    if (!this.isOpen()) {
      this.isOpen.set(true);
    }

    if (this.state() === 'LISTENING') {
      this.speechService.stopListening();
    } else {
      this.speechService.startListening();
    }
  }

  toggleSpeechOutput(): void {
    this.speechEnabled.update(v => !v);
    if (!this.speechEnabled()) {
      this.speechService.stopSpeaking();
    }
  }

  handleVocalInput(text: string): void {
    if (!text || !text.trim()) return;

    const trimmed = text.trim();
    const pending = this.pendingConfirmation();

    // Détection vocale simple de confirmation si une action est en attente
    if (pending) {
      const lower = trimmed.toLowerCase();
      if (['oui', 'confirmer', 'valider', 'd\'accord', 'ok', 'oui confirme'].some(k => lower.includes(k))) {
        this.executeConfirmation(true);
        return;
      }
      if (['non', 'annuler', 'refuser', 'stop', 'pas maintenant'].some(k => lower.includes(k))) {
        this.executeConfirmation(false);
        return;
      }
    }

    // Sinon message normal
    this.sendMessage(trimmed);
  }

  selectQuickPrompt(promptText: string): void {
    if (this.state() === 'PROCESSING') return;
    const cleanPrompt = promptText.replace(/^[^\wÀ-ÿ]+/i, '').trim();
    this.sendMessage(cleanPrompt);
  }

  sendTextMessage(): void {
    if (!this.textInput.trim() || this.state() === 'PROCESSING') return;
    const msg = this.textInput.trim();
    this.textInput = '';

    const pending = this.pendingConfirmation();
    if (pending) {
      const lower = msg.toLowerCase();
      if (['oui', 'confirmer', 'valider', 'd\'accord', 'ok', 'oui confirme'].some(k => lower.includes(k))) {
        this.executeConfirmation(true);
        return;
      }
      if (['non', 'annuler', 'refuser', 'stop', 'pas maintenant'].some(k => lower.includes(k))) {
        this.executeConfirmation(false);
        return;
      }
    }

    this.sendMessage(msg);
  }

  private sendMessage(text: string): void {
    const history = this.messages()
      .slice(-4)
      .map(m => ({ sender: m.sender, text: m.text }));

    this.addMessage('user', text);
    this.state.set('PROCESSING');
    this.errorMessage.set(null);

    const pending = this.pendingConfirmation();

    this.assistantService.chat(text, null, pending?.actionId, history).subscribe({
      next: (response: AssistantChatResponse) => {
        this.handleAssistantResponse(response);
      },
      error: (err) => {
        this.state.set('IDLE');
        const errMsg = err?.error?.message || "Erreur de communication avec l'assistant IA.";
        this.addMessage('assistant', `⚠️ ${errMsg}`);
        if (this.speechEnabled()) {
          this.speechService.speak(errMsg);
        }
      }
    });
  }

  executeConfirmation(confirm: boolean): void {
    const pending = this.pendingConfirmation();
    if (!pending) return;

    const history = this.messages()
      .slice(-4)
      .map(m => ({ sender: m.sender, text: m.text }));

    this.state.set('PROCESSING');
    const actionLabel = confirm ? 'Confirmation de l\'action...' : 'Annulation de l\'action...';
    this.addMessage('user', confirm ? 'Oui, je confirme.' : 'Non, annuler.');

    this.assistantService.chat(actionLabel, confirm, pending.actionId, history).subscribe({
      next: (response: AssistantChatResponse) => {
        this.pendingConfirmation.set(null);
        this.handleAssistantResponse(response);
      },
      error: (err) => {
        this.state.set('IDLE');
        this.pendingConfirmation.set(null);
        const errMsg = err?.error?.message || "Erreur lors de l'exécution de l'action.";
        this.addMessage('assistant', `⚠️ ${errMsg}`);
        if (this.speechEnabled()) {
          this.speechService.speak(errMsg);
        }
      }
    });
  }

  downloadReport(reportId: number): void {
    this.reportService.downloadReport(reportId).subscribe({
      next: (blob) => {
        this.reportService.triggerBrowserDownload(blob, `rapport-organisation-${reportId}.pdf`);
      },
      error: () => {
        this.errorMessage.set("Erreur lors du téléchargement du rapport PDF.");
      }
    });
  }

  downloadExport(format: 'excel' | 'csv' = 'excel'): void {
    if (format === 'csv') {
      this.collecteService.exportCsv().subscribe({
        next: (blob) => {
          this.collecteService.triggerBrowserDownload(blob, 'collectes_export.csv');
        },
        error: () => {
          this.errorMessage.set("Erreur lors du téléchargement de l'export CSV.");
        }
      });
    } else {
      this.collecteService.exportExcel().subscribe({
        next: (blob) => {
          this.collecteService.triggerBrowserDownload(blob, 'collectes_export.xlsx');
        },
        error: () => {
          this.errorMessage.set("Erreur lors du téléchargement de l'export Excel.");
        }
      });
    }
  }

  navigateToRoute(route: string): void {
    this.router.navigateByUrl(route);
  }

  private handleAssistantResponse(response: AssistantChatResponse): void {
    if (response.requiresConfirmation) {
      this.pendingConfirmation.set({
        actionId: response.pendingActionId || null,
        summary: response.reply
      });
      this.state.set('CONFIRMING');
    } else {
      this.pendingConfirmation.set(null);
      this.state.set('IDLE');
    }

    this.addMessage(
      'assistant',
      response.reply,
      response.requiresConfirmation,
      response.pendingActionId,
      response.reportId,
      response.navigateTo,
      response.exportFormat
    );

    // Navigation automatique si demandée
    if (response.navigateTo) {
      this.router.navigateByUrl(response.navigateTo);
    }

    // Export automatique si demandé
    if (response.exportFormat) {
      this.downloadExport(response.exportFormat);
      this.audioFeedback.playSuccess();
    }

    // Téléchargement automatique si un nouveau rapport vient d'être généré
    if (response.reportId) {
      this.downloadReport(response.reportId);
      this.audioFeedback.playSuccess();
    }

    if (this.speechEnabled()) {
      this.speechService.speak(response.reply);
    }
  }

  clearMessages(): void {
    this.messages.set([
      {
        id: 'welcome',
        sender: 'assistant',
        text: 'Historique effacé. Comment puis-je vous aider ?',
        timestamp: new Date()
      }
    ]);
    this.pendingConfirmation.set(null);
    this.state.set('IDLE');
  }

  copyMessage(msg: ChatMessage): void {
    if (!navigator?.clipboard) return;
    navigator.clipboard.writeText(msg.text).then(() => {
      this.copiedMessageId.set(msg.id);
      setTimeout(() => this.copiedMessageId.set(null), 2000);
    });
  }

  private addMessage(
    sender: 'user' | 'assistant',
    text: string,
    requiresConfirmation = false,
    pendingActionId?: string | null,
    reportId?: number | null,
    navigateTo?: string | null,
    exportFormat?: 'excel' | 'csv' | null
  ): void {
    const msg: ChatMessage = {
      id: `${Date.now()}-${Math.random()}`,
      sender,
      text,
      timestamp: new Date(),
      requiresConfirmation,
      pendingActionId,
      reportId,
      navigateTo,
      exportFormat
    };

    this.messages.update(list => [...list, msg]);
    setTimeout(() => this.scrollToBottom(), 50);
  }

  private scrollToBottom(): void {
    if (this.messagesContainer?.nativeElement) {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    }
  }
}

