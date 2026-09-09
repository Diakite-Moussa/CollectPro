import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

declare global {
  interface Window {
    SpeechRecognition: any;
    webkitSpeechRecognition: any;
  }
}

@Injectable({
  providedIn: 'root'
})
export class SpeechService {
  private recognition: any = null;
  private synth: SpeechSynthesis | null = null;
  private frenchVoice: SpeechSynthesisVoice | null = null;

  private readonly isListeningSubject = new BehaviorSubject<boolean>(false);
  readonly isListening$ = this.isListeningSubject.asObservable();

  private readonly isSpeakingSubject = new BehaviorSubject<boolean>(false);
  readonly isSpeaking$ = this.isSpeakingSubject.asObservable();

  private readonly interimTranscriptSubject = new BehaviorSubject<string>('');
  readonly interimTranscript$ = this.interimTranscriptSubject.asObservable();

  private readonly finalTranscriptSubject = new Subject<string>();
  readonly finalTranscript$ = this.finalTranscriptSubject.asObservable();

  private readonly errorSubject = new Subject<string>();
  readonly error$ = this.errorSubject.asObservable();

  constructor(private ngZone: NgZone) {
    this.initRecognition();
    this.initSynthesis();
  }

  get isSupported(): boolean {
    return !!(this.recognition && this.synth);
  }

  private initRecognition(): void {
    const SpeechRecognitionClass = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognitionClass) {
      return;
    }

    this.recognition = new SpeechRecognitionClass();
    this.recognition.lang = 'fr-FR';
    this.recognition.continuous = false;
    this.recognition.interimResults = true;
    this.recognition.maxAlternatives = 1;

    this.recognition.onstart = () => {
      this.ngZone.run(() => {
        this.isListeningSubject.next(true);
        this.interimTranscriptSubject.next('');
      });
    };

    this.recognition.onresult = (event: any) => {
      this.ngZone.run(() => {
        let interim = '';
        let final = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const item = event.results[i];
          if (item.isFinal) {
            final += item[0].transcript;
          } else {
            interim += item[0].transcript;
          }
        }

        if (interim) {
          this.interimTranscriptSubject.next(interim);
        }

        if (final) {
          this.interimTranscriptSubject.next('');
          this.finalTranscriptSubject.next(final.trim());
        }
      });
    };

    this.recognition.onerror = (event: any) => {
      this.ngZone.run(() => {
        this.isListeningSubject.next(false);
        if (event.error !== 'no-speech') {
          this.errorSubject.next(`Erreur micro : ${event.error}`);
        }
      });
    };

    this.recognition.onend = () => {
      this.ngZone.run(() => {
        this.isListeningSubject.next(false);
      });
    };
  }

  private initSynthesis(): void {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.synth = window.speechSynthesis;
      this.loadVoices();
      if (this.synth.onvoiceschanged !== undefined) {
        this.synth.onvoiceschanged = () => this.loadVoices();
      }
    }
  }

  private loadVoices(): void {
    if (!this.synth) return;
    const voices = this.synth.getVoices();
    this.frenchVoice = voices.find(v => v.lang.startsWith('fr') && (v.name.includes('Natural') || v.name.includes('Google') || v.name.includes('Neural')))
      || voices.find(v => v.lang.startsWith('fr'))
      || null;
  }

  startListening(): void {
    if (!this.recognition) {
      this.errorSubject.next("La reconnaissance vocale n'est pas supportée par votre navigateur.");
      return;
    }

    if (this.isListeningSubject.value) {
      return;
    }

    // Arrête la synthèse vocale en cours si l'utilisateur commence à parler
    this.stopSpeaking();

    try {
      this.recognition.start();
    } catch (e: any) {
      this.errorSubject.next(e.message || "Impossible de démarrer l'écoute");
    }
  }

  stopListening(): void {
    if (this.recognition && this.isListeningSubject.value) {
      try {
        this.recognition.stop();
      } catch {
        // Ignorer si déjà arrêté
      }
    }
  }

  speak(text: string): Promise<void> {
    return new Promise((resolve) => {
      if (!this.synth) {
        resolve();
        return;
      }

      this.stopSpeaking();

      // Nettoie d'éventuels résidus Markdown pour la prononciation
      const cleanText = text
        .replace(/[*_#`~]/g, '')
        .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
        .trim();

      if (!cleanText) {
        resolve();
        return;
      }

      const utterance = new SpeechSynthesisUtterance(cleanText);
      utterance.lang = 'fr-FR';
      if (this.frenchVoice) {
        utterance.voice = this.frenchVoice;
      }
      utterance.rate = 1.05;
      utterance.pitch = 1.0;

      utterance.onstart = () => {
        this.ngZone.run(() => this.isSpeakingSubject.next(true));
      };

      utterance.onend = () => {
        this.ngZone.run(() => {
          this.isSpeakingSubject.next(false);
          resolve();
        });
      };

      utterance.onerror = () => {
        this.ngZone.run(() => {
          this.isSpeakingSubject.next(false);
          resolve();
        });
      };

      this.synth.speak(utterance);
    });
  }

  stopSpeaking(): void {
    if (this.synth) {
      this.synth.cancel();
      this.isSpeakingSubject.next(false);
    }
  }
}
