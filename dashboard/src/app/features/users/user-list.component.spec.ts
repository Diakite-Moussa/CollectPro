import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { UserListComponent } from './user-list.component';
import { UserService } from '../../core/services/user.service';
import { PermissionService } from '../../core/services/permission.service';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse, UserResponsePage } from '../../core/models/user.model';

describe('UserListComponent', () => {
  let component: UserListComponent;
  let fixture: ComponentFixture<UserListComponent>;
  let userServiceSpy: {
    getUsers: ReturnType<typeof vi.fn>;
    updateUserStatus: ReturnType<typeof vi.fn>;
    resendInvitation: ReturnType<typeof vi.fn>;
    createUser: ReturnType<typeof vi.fn>;
    assignSupervisor: ReturnType<typeof vi.fn>;
  };
  let authServiceStub: { currentUser: ReturnType<typeof vi.fn> };

  const mockUser: UserResponse = {
    id: 1,
    email: 'agent@test.com',
    firstName: 'Jean',
    lastName: 'Dupont',
    role: 'AGENT',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z'
  };

  const mockPage: UserResponsePage = {
    content: [mockUser],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 25,
    first: true,
    last: true
  };

  function setup(role: string): void {
    userServiceSpy = {
      getUsers: vi.fn().mockReturnValue(of(mockPage)),
      updateUserStatus: vi.fn().mockReturnValue(of(mockUser)),
      resendInvitation: vi.fn().mockReturnValue(of(void 0)),
      createUser: vi.fn().mockReturnValue(of(mockUser)),
      assignSupervisor: vi.fn().mockReturnValue(of(void 0))
    };

    authServiceStub = {
      currentUser: vi.fn().mockReturnValue({ id: 99, role })
    };

    TestBed.configureTestingModule({
      imports: [UserListComponent, NoopAnimationsModule],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: PermissionService, useValue: {} },
        { provide: AuthService, useValue: authServiceStub }
      ]
    });

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create and load the first page of users on init', () => {
    setup('SUPER_ADMIN');
    expect(component).toBeTruthy();
    expect(userServiceSpy.getUsers).toHaveBeenCalledWith(0, 25);
    expect(component.users.length).toBe(1);
    expect(component.totalElements).toBe(1);
  });

  it('should grant supervisor-assignment and disable rights to a SUPER_ADMIN', () => {
    setup('SUPER_ADMIN');
    expect(component.canAssignSupervisor).toBe(true);
    expect(component.canDisableUser).toBe(true);
    expect(component.canManagePermissions).toBe(true);
  });

  it('should not grant management rights to a plain AGENT', () => {
    setup('AGENT');
    expect(component.canAssignSupervisor).toBe(false);
    expect(component.canDisableUser).toBe(false);
    expect(component.canManagePermissions).toBe(false);
  });

  it('should reload the correct page when the paginator changes', () => {
    setup('SUPER_ADMIN');
    component.onPageChange({ pageIndex: 2, pageSize: 10, length: 30 } as any);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(10);
    expect(userServiceSpy.getUsers).toHaveBeenCalledWith(2, 10);
  });

  it('should toggle a user status after confirmation', () => {
    setup('SUPER_ADMIN');
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(window, 'alert').mockImplementation(() => {});

    component.toggleUserStatus(mockUser);

    expect(userServiceSpy.updateUserStatus).toHaveBeenCalledWith(1, 'DISABLED');
  });

  it('should not toggle a user status when confirmation is declined', () => {
    setup('SUPER_ADMIN');
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.toggleUserStatus(mockUser);

    expect(userServiceSpy.updateUserStatus).not.toHaveBeenCalled();
  });

  it('should resend an invitation for an invited user', () => {
    setup('SUPER_ADMIN');
    vi.spyOn(window, 'alert').mockImplementation(() => {});
    const invited: UserResponse = { ...mockUser, status: 'INVITED' };

    component.resend(invited);

    expect(userServiceSpy.resendInvitation).toHaveBeenCalledWith(1);
    expect(component.resendingId).toBeNull();
  });
});
