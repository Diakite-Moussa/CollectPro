import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SyncLogListComponent } from './sync-log-list.component';
import { SyncLogService } from '../../core/services/sync-log.service';
import { AuthService } from '../../core/services/auth.service';
import { SyncLogPage } from '../../core/models/sync-log.model';

describe('SyncLogListComponent', () => {
  let component: SyncLogListComponent;
  let fixture: ComponentFixture<SyncLogListComponent>;
  let syncLogServiceSpy: { getSyncLogs: ReturnType<typeof vi.fn>; getMyAgents: ReturnType<typeof vi.fn> };
  let authServiceStub: { currentUser: ReturnType<typeof vi.fn> };

  const mockPage: SyncLogPage = {
    content: [
      {
        id: 1,
        agent: { id: 10, firstName: 'Fatou', lastName: 'Coulibaly' },
        localReference: 'local-1',
        result: 'SUCCESS',
        createdAt: '2026-08-01T09:00:00Z'
      }
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 25,
    first: true,
    last: true
  };

  function setup(role: string): void {
    syncLogServiceSpy = {
      getSyncLogs: vi.fn().mockReturnValue(of(mockPage)),
      getMyAgents: vi.fn().mockReturnValue(of([
        {
          id: 10,
          firstName: 'Fatou',
          lastName: 'Coulibaly',
          email: 'f@test.com',
          role: 'AGENT',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z'
        }
      ]))
    };
    authServiceStub = { currentUser: vi.fn().mockReturnValue({ id: 1, role }) };

    TestBed.configureTestingModule({
      imports: [SyncLogListComponent, NoopAnimationsModule],
      providers: [
        { provide: SyncLogService, useValue: syncLogServiceSpy },
        { provide: AuthService, useValue: authServiceStub }
      ]
    });

    fixture = TestBed.createComponent(SyncLogListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load sync logs and skip the agent list for a non-supervisor role', () => {
    setup('ADMIN_PRINCIPAL');
    expect(component).toBeTruthy();
    expect(component.isSupervisor).toBe(false);
    expect(syncLogServiceSpy.getMyAgents).not.toHaveBeenCalled();
    expect(syncLogServiceSpy.getSyncLogs).toHaveBeenCalledWith(null, 0, 25);
    expect(component.dataSource.data.length).toBe(1);
    expect(component.totalElements).toBe(1);
  });

  it('should load the agent list when the current user is a supervisor', () => {
    setup('SUPERVISOR');
    expect(component.isSupervisor).toBe(true);
    expect(syncLogServiceSpy.getMyAgents).toHaveBeenCalled();
    expect(component.myAgents.length).toBe(1);
  });

  it('should reset to the first page when the agent filter changes', () => {
    setup('SUPERVISOR');
    component.currentPage = 3;

    component.onAgentFilterChange(10);

    expect(component.selectedAgentId).toBe(10);
    expect(component.currentPage).toBe(0);
    expect(syncLogServiceSpy.getSyncLogs).toHaveBeenCalledWith(10, 0, 25);
  });

  it('should request the correct page on paginator change', () => {
    setup('ADMIN_PRINCIPAL');

    component.onPageChange({ pageIndex: 2, pageSize: 50, length: 1 } as any);

    expect(component.currentPage).toBe(2);
    expect(component.pageSize).toBe(50);
    expect(syncLogServiceSpy.getSyncLogs).toHaveBeenCalledWith(null, 2, 50);
  });
});
