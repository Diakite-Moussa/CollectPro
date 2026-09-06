import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CollecteListComponent } from './collecte-list.component';
import { CollecteService } from '../../core/services/collecte.service';
import { AuthService } from '../../core/services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { CollecteResponse, CollecteResponsePage } from '../../core/models/collecte.model';
import { MatTabChangeEvent } from '@angular/material/tabs';

describe('CollecteListComponent', () => {
  let component: CollecteListComponent;
  let fixture: ComponentFixture<CollecteListComponent>;
  let collecteServiceSpy: {
    getTeamCollectes: ReturnType<typeof vi.fn>;
    getOrganizationCollectes: ReturnType<typeof vi.fn>;
    validateCollecte: ReturnType<typeof vi.fn>;
    rejectCollecte: ReturnType<typeof vi.fn>;
    exportCsv: ReturnType<typeof vi.fn>;
    exportExcel: ReturnType<typeof vi.fn>;
  };
  let authServiceStub: { currentUser: ReturnType<typeof vi.fn> };
  let dialogSpy: { open: ReturnType<typeof vi.fn> };

  const mockCollectes: CollecteResponse[] = [
    {
      id: 1,
      formVersionId: 1,
      agent: { id: 10, firstName: 'Moussa', lastName: 'Diakité' },
      dataJson: '{"q1": "val1"}',
      status: 'PENDING_VALIDATION',
      latitude: 12.65,
      longitude: -8.01,
      outsideMissionZone: false,
      createdAt: '2026-08-15T10:00:00Z'
    },
    {
      id: 2,
      formVersionId: 1,
      agent: { id: 11, firstName: 'Fatou', lastName: 'Koné' },
      dataJson: '{"q1": "val2"}',
      status: 'VALIDATED',
      latitude: 12.70,
      longitude: -8.05,
      outsideMissionZone: true,
      createdAt: '2026-08-15T11:00:00Z'
    },
    {
      id: 3,
      formVersionId: 1,
      agent: { id: 12, firstName: 'Ali', lastName: 'Traoré' },
      dataJson: '{"q1": "val3"}',
      status: 'REJECTED',
      latitude: undefined,
      longitude: undefined,
      createdAt: '2026-08-15T12:00:00Z'
    }
  ];

  const mockPage: CollecteResponsePage = {
    content: mockCollectes,
    totalElements: 3,
    totalPages: 1,
    number: 0,
    size: 25,
    first: true,
    last: true
  };

  function setup(role = 'ADMIN_PRINCIPAL') {
    collecteServiceSpy = {
      getTeamCollectes: vi.fn().mockReturnValue(of(mockPage)),
      getOrganizationCollectes: vi.fn().mockReturnValue(of(mockPage)),
      validateCollecte: vi.fn(),
      rejectCollecte: vi.fn(),
      exportCsv: vi.fn().mockReturnValue(of(new Blob())),
      exportExcel: vi.fn().mockReturnValue(of(new Blob()))
    };

    authServiceStub = {
      currentUser: vi.fn().mockReturnValue({ id: 1, role })
    };

    dialogSpy = {
      open: vi.fn()
    };

    TestBed.configureTestingModule({
      imports: [CollecteListComponent, NoopAnimationsModule],
      providers: [
        { provide: CollecteService, useValue: collecteServiceSpy },
        { provide: AuthService, useValue: authServiceStub },
        { provide: MatDialog, useValue: dialogSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CollecteListComponent);
    component = fixture.componentInstance;
  }

  it('should initialize and load organization collectes when user is ADMIN (Liste tab, without mounting MapView)', () => {
    setup('ADMIN_PRINCIPAL');
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isSupervisor).toBe(false);
    expect(collecteServiceSpy.getOrganizationCollectes).toHaveBeenCalledWith(0, 25);
    expect(component.collectes.length).toBe(3);
    expect(component.totalElements).toBe(3);
  });

  it('should call getTeamCollectes when user is SUPERVISOR', () => {
    setup('SUPERVISOR');
    fixture.detectChanges();

    expect(component.isSupervisor).toBe(true);
    expect(collecteServiceSpy.getTeamCollectes).toHaveBeenCalledWith(0, 25);
  });

  it('should build markers only for collectes with valid GPS coordinates and handle outsideMissionZone', () => {
    setup();
    fixture.detectChanges();

    // 2 collectes ont des coordonnées GPS, la 3ème non
    expect(component.collecteMarkers.length).toBe(2);

    const marker1 = component.collecteMarkers.find(m => m.id === 1);
    expect(marker1).toBeDefined();
    expect(marker1?.lat).toBe(12.65);
    expect(marker1?.lng).toBe(-8.01);
    expect(marker1?.color).toBe('#f59e0b'); // PENDING_VALIDATION

    const marker2 = component.collecteMarkers.find(m => m.id === 2);
    expect(marker2).toBeDefined();
    expect(marker2?.color).toBe('#dc2626'); // outsideMissionZone -> red
    expect(marker2?.label).toContain('Hors zone de mission');
  });

  it('should update pagination and reload collectes on onPageChange', () => {
    setup();
    fixture.detectChanges();

    component.onPageChange({ pageIndex: 1, pageSize: 10, length: 3 });

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(10);
    expect(collecteServiceSpy.getOrganizationCollectes).toHaveBeenCalledWith(1, 10);
  });

  it('should trigger resize event on window when switching to Carte tab', () => {
    setup();
    fixture.detectChanges();

    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    const mockEvent = {
      index: 1,
      tab: { textLabel: 'Carte' }
    } as unknown as MatTabChangeEvent;

    component.onTabChange(mockEvent);

    return new Promise<void>((resolve) => {
      setTimeout(() => {
        expect(dispatchSpy).toHaveBeenCalled();
        dispatchSpy.mockRestore();
        resolve();
      }, 50);
    });
  });
});
