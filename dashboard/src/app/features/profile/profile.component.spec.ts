import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userServiceSpy: {
    getMe: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
    changePassword: ReturnType<typeof vi.fn>;
  };

  const mockUser: UserResponse = {
    id: 1,
    email: 'admin@collectpro.local',
    firstName: 'Aïcha',
    lastName: 'Keita',
    role: 'ADMIN_PRINCIPAL',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    userServiceSpy = {
      getMe: vi.fn().mockReturnValue(of(mockUser)),
      updateProfile: vi.fn().mockReturnValue(of({ ...mockUser, firstName: 'Nouveau' })),
      changePassword: vi.fn().mockReturnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent, NoopAnimationsModule],
      providers: [{ provide: UserService, useValue: userServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the current user and pre-fill the profile form', () => {
    expect(component).toBeTruthy();
    expect(userServiceSpy.getMe).toHaveBeenCalled();
    expect(component.currentUser()?.email).toBe('admin@collectpro.local');
    expect(component.profileForm.value.firstName).toBe('Aïcha');
  });

  it('should not submit the profile form when invalid', () => {
    component.profileForm.patchValue({ firstName: '', lastName: '' });

    component.onUpdateProfile();

    expect(userServiceSpy.updateProfile).not.toHaveBeenCalled();
  });

  it('should update the profile when the form is valid', () => {
    component.profileForm.patchValue({ firstName: 'Nouveau', lastName: 'Keita', phone: '' });

    component.onUpdateProfile();

    expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({
      firstName: 'Nouveau',
      lastName: 'Keita',
      phone: ''
    });
    expect(component.profileSaving()).toBe(false);
  });

  it('should flag a mismatch between the new password and its confirmation', () => {
    component.passwordForm.patchValue({
      currentPassword: 'old-password',
      newPassword: 'NewPassword1',
      confirmPassword: 'Different1'
    });

    expect(component.passwordForm.hasError('passwordMismatch')).toBe(true);
  });

  it('should not submit the password form when it is invalid', () => {
    component.passwordForm.patchValue({
      currentPassword: 'old-password',
      newPassword: 'NewPassword1',
      confirmPassword: 'Different1'
    });

    component.onChangePassword();

    expect(userServiceSpy.changePassword).not.toHaveBeenCalled();
  });

  it('should submit a password change when the form is valid', () => {
    component.passwordForm.patchValue({
      currentPassword: 'old-password',
      newPassword: 'NewPassword1',
      confirmPassword: 'NewPassword1'
    });

    component.onChangePassword();

    expect(userServiceSpy.changePassword).toHaveBeenCalledWith({
      currentPassword: 'old-password',
      newPassword: 'NewPassword1'
    });
  });

  it('should classify password strength from weak to strong', () => {
    component.passwordForm.patchValue({ newPassword: 'abc' });
    expect(component.getPasswordStrengthClass()).toBe('weak');

    component.passwordForm.patchValue({ newPassword: 'abcdefgh1' });
    expect(component.getPasswordStrengthClass()).toBe('medium');

    component.passwordForm.patchValue({ newPassword: 'Abcdefgh1!' });
    expect(component.getPasswordStrengthClass()).toBe('strong');
  });
});
