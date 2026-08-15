import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';

export type FormFieldType =
  | 'text'
  | 'textarea'
  | 'number'
  | 'date'
  | 'select'
  | 'photo'
  | 'document';

export interface FormFieldDefinition {
  key: string;
  label: string;
  type: FormFieldType;
  required: boolean;
  options?: string[];
}

@Component({
  selector: 'app-form-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatCardModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FormBuilderComponent),
      multi: true
    }
  ],
  template: `
    <div class="builder">
      <div class="builder-header">
        <span class="builder-title">Champs du formulaire</span>
        <button mat-stroked-button type="button" color="primary" (click)="addField()">
          <mat-icon>add</mat-icon> Ajouter un champ
        </button>
      </div>

      <p *ngIf="fields.length === 0" class="empty-hint">
        Aucun champ. Cliquez sur « Ajouter un champ » pour commencer.
      </p>

      <mat-card *ngFor="let field of fields; let i = index" class="field-card">
        <div class="field-row">
          <mat-form-field appearance="outline" class="flex-2">
            <mat-label>Libellé</mat-label>
            <input matInput [(ngModel)]="field.label" (ngModelChange)="onFieldChange(field, i)" required>
          </mat-form-field>

          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Type</mat-label>
            <mat-select [(ngModel)]="field.type" (ngModelChange)="onTypeChange(field, i)">
              <mat-option value="text">Texte court</mat-option>
              <mat-option value="textarea">Texte long</mat-option>
              <mat-option value="number">Nombre</mat-option>
              <mat-option value="date">Date</mat-option>
              <mat-option value="select">Liste déroulante</mat-option>
              <mat-option value="photo">Photo</mat-option>
              <mat-option value="document">Document</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-checkbox [(ngModel)]="field.required" (ngModelChange)="emitChange()" class="required-check">
            Obligatoire
          </mat-checkbox>

          <button mat-icon-button type="button" color="warn" (click)="removeField(i)" title="Supprimer">
            <mat-icon>delete</mat-icon>
          </button>
        </div>

        <mat-form-field *ngIf="field.type === 'select'" appearance="outline" class="w-full">
          <mat-label>Options (séparées par des virgules)</mat-label>
          <input matInput
                 [ngModel]="field.options?.join(', ')"
                 (ngModelChange)="setOptions(field, $event)"
                 placeholder="Ex: Oui, Non, Ne sait pas">
        </mat-form-field>

        <span class="field-key">Clé technique : {{ field.key }}</span>
      </mat-card>
    </div>
  `,
  styles: [`
    .builder { display: flex; flex-direction: column; gap: 0.75rem; }
    .builder-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .builder-title { font-weight: 600; color: #334155; }
    .empty-hint { color: #64748b; font-size: 0.9rem; margin: 0; }
    .field-card { padding: 12px 16px !important; }
    .field-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      align-items: center;
    }
    .flex-1 { flex: 1; min-width: 140px; }
    .flex-2 { flex: 2; min-width: 180px; }
    .w-full { width: 100%; }
    .required-check { margin: 0 4px; }
    .field-key {
      display: block;
      font-size: 0.75rem;
      color: #94a3b8;
      font-family: monospace;
      margin-top: 4px;
    }
  `]
})
export class FormBuilderComponent implements ControlValueAccessor {
  fields: FormFieldDefinition[] = [];
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(schemaJson: string | null): void {
    if (!schemaJson) {
      this.fields = [];
      return;
    }
    try {
      const parsed = JSON.parse(schemaJson);
      const rawFields = parsed.fields ?? [];
      this.fields = rawFields.map((f: Record<string, unknown>, index: number) => ({
        key: (f['key'] ?? f['name'] ?? `field_${index + 1}`) as string,
        label: (f['label'] ?? f['key'] ?? `Champ ${index + 1}`) as string,
        type: (f['type'] ?? 'text') as FormFieldType,
        required: (f['required'] as boolean) ?? false,
        options: f['options'] as string[] | undefined
      }));
    } catch {
      this.fields = [];
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  addField(): void {
    const index = this.fields.length + 1;
    this.fields.push({
      key: `field_${index}`,
      label: `Nouveau champ ${index}`,
      type: 'text',
      required: false
    });
    this.emitChange();
  }

  removeField(index: number): void {
    this.fields.splice(index, 1);
    this.emitChange();
  }

  onFieldChange(field: FormFieldDefinition, index: number): void {
    if (!field.key || field.key.startsWith('field_')) {
      field.key = this.slugify(field.label) || `field_${index + 1}`;
    }
    this.emitChange();
  }

  onTypeChange(field: FormFieldDefinition, index: number): void {
    if (field.type === 'select' && !field.options?.length) {
      field.options = ['Option 1', 'Option 2'];
    }
    if (field.type !== 'select') {
      delete field.options;
    }
    this.emitChange();
  }

  setOptions(field: FormFieldDefinition, value: string): void {
    field.options = value.split(',').map((o) => o.trim()).filter(Boolean);
    this.emitChange();
  }

  emitChange(): void {
    const schema = {
      version: 1,
      fields: this.fields.map((f) => {
        const entry: Record<string, unknown> = {
          key: f.key,
          label: f.label,
          type: f.type,
          required: f.required
        };
        if (f.type === 'select' && f.options?.length) {
          entry['options'] = f.options;
        }
        return entry;
      })
    };
    this.onChange(JSON.stringify(schema, null, 2));
    this.onTouched();
  }

  private slugify(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_|_$/g, '')
      .slice(0, 40);
  }
}
