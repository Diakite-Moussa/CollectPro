import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MissionFormDialogComponent } from './mission-form-dialog.component';

describe('MissionFormDialogComponent', () => {
  let component: MissionFormDialogComponent;
  let dialogRefSpy: { close: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialogRefSpy = { close: vi.fn() };

    TestBed.configureTestingModule({
      imports: [MissionFormDialogComponent],
      providers: [
        FormBuilder,
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ]
    });

    const fixture = TestBed.createComponent(MissionFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with invalid form when empty', () => {
    expect(component.missionForm.valid).toBe(false);
  });

  it('should be valid when required name is provided', () => {
    component.missionForm.controls['name'].setValue('Mission Test');
    expect(component.missionForm.valid).toBe(true);
  });

  it('should close dialog with payload on onSave', () => {
    component.missionForm.patchValue({
      name: 'Mission Alpha',
      description: 'Test description',
      latitude: 12.5,
      longitude: -8.0,
      radiusMeters: 500,
      expectedCollectesCount: 100
    });

    component.onSave();

    expect(dialogRefSpy.close).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Mission Alpha',
        description: 'Test description',
        latitude: 12.5,
        longitude: -8.0,
        radiusMeters: 500,
        expectedCollectesCount: 100
      })
    );
  });
});
