import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { AuditLogsComponent } from './audit-logs.component';
import { AuditLogService } from '../../core/services/audit-log.service';
import { AuditLogPage } from '../../core/models/audit-log.model';

describe('AuditLogsComponent', () => {
  let component: AuditLogsComponent;
  let fixture: ComponentFixture<AuditLogsComponent>;
  let auditLogServiceSpy: { getAuditLogs: ReturnType<typeof vi.fn> };

  const mockPage: AuditLogPage = {
    content: [
      {
        id: 1,
        action: 'COLLECTE_VALIDATED',
        entityType: 'Collecte',
        entityId: 42,
        details: 'Validation OK',
        createdAt: '2026-08-01T10:00:00Z',
        actor: { id: 5, firstName: 'Awa', lastName: 'Traoré', email: 'awa@test.com' }
      },
      {
        id: 2,
        action: 'USER_ACTIVATED',
        createdAt: '2026-08-02T10:00:00Z'
      }
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 25,
    first: true,
    last: true
  };

  beforeEach(async () => {
    auditLogServiceSpy = { getAuditLogs: vi.fn().mockReturnValue(of(mockPage)) };

    await TestBed.configureTestingModule({
      imports: [AuditLogsComponent, NoopAnimationsModule],
      providers: [{ provide: AuditLogService, useValue: auditLogServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the first page of logs and derive the available actions', () => {
    expect(component).toBeTruthy();
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalledWith(0, 25);
    expect(component.logs.length).toBe(2);
    expect(component.availableActions).toEqual(['COLLECTE_VALIDATED', 'USER_ACTIVATED']);
  });

  it('should filter logs by the selected action', () => {
    component.onActionFilterChange('USER_ACTIVATED');
    expect(component.filteredLogs.length).toBe(1);
    expect(component.filteredLogs[0].id).toBe(2);
  });

  it('should filter logs by free-text search across actor and details', () => {
    component.searchTerm = 'awa@test.com';
    component.applyFilter();
    expect(component.filteredLogs.length).toBe(1);
    expect(component.filteredLogs[0].id).toBe(1);
  });

  it('should return every log when the search term is cleared', () => {
    component.searchTerm = 'awa@test.com';
    component.applyFilter();
    component.searchTerm = '';
    component.applyFilter();
    expect(component.filteredLogs.length).toBe(2);
  });

  it('should request a new page from the service on pagination change', () => {
    component.onPageChange({ pageIndex: 1, pageSize: 10, length: 2 } as any);
    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(10);
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalledWith(1, 10);
  });

  it('should classify actions into the correct CSS class', () => {
    expect(component.getActionClass('COLLECTE_VALIDATED')).toBe('validate');
    expect(component.getActionClass('COLLECTE_REJECTED')).toBe('reject');
    expect(component.getActionClass('FORM_PUBLISHED')).toBe('create');
    expect(component.getActionClass('PASSWORD_RESET')).toBe('auth');
    expect(component.getActionClass('SOMETHING_ELSE')).toBe('default');
  });
});
