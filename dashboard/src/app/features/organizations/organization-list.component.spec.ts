import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { OrganizationListComponent } from './organization-list.component';
import { OrganizationService } from '../../core/services/organization.service';
import { OrganizationResponse } from '../../core/models/organization.model';

describe('OrganizationListComponent', () => {
  let component: OrganizationListComponent;
  let fixture: ComponentFixture<OrganizationListComponent>;
  let orgServiceSpy: {
    getOrganizations: ReturnType<typeof vi.fn>;
    updateOrganizationStatus: ReturnType<typeof vi.fn>;
    createOrganization: ReturnType<typeof vi.fn>;
    updateOrganization: ReturnType<typeof vi.fn>;
    uploadLogo: ReturnType<typeof vi.fn>;
  };

  const mockOrg: OrganizationResponse = {
    id: 1,
    name: 'Croix-Rouge',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    orgServiceSpy = {
      getOrganizations: vi.fn().mockReturnValue(of([mockOrg])),
      updateOrganizationStatus: vi.fn().mockReturnValue(of({ ...mockOrg, status: 'INACTIVE' })),
      createOrganization: vi.fn().mockReturnValue(of(mockOrg)),
      updateOrganization: vi.fn().mockReturnValue(of(mockOrg)),
      uploadLogo: vi.fn().mockReturnValue(of(mockOrg))
    };

    await TestBed.configureTestingModule({
      imports: [OrganizationListComponent, NoopAnimationsModule],
      providers: [{ provide: OrganizationService, useValue: orgServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load organizations on init', () => {
    expect(component).toBeTruthy();
    expect(orgServiceSpy.getOrganizations).toHaveBeenCalled();
    expect(component.organizations.length).toBe(1);
    expect(component.organizations[0].name).toBe('Croix-Rouge');
  });

  it('should build an absolute URL for a relative logo path', () => {
    const url = component.getLogoUrl({ ...mockOrg, logoUrl: '/files/organizations/1/logo' });
    expect(url).toContain('/files/organizations/1/logo');
  });

  it('should keep an already-absolute logo URL untouched', () => {
    const absolute = 'https://cdn.example.com/logo.png';
    expect(component.getLogoUrl({ ...mockOrg, logoUrl: absolute })).toBe(absolute);
  });

  it('should return null when the organization has no logo', () => {
    expect(component.getLogoUrl(mockOrg)).toBeNull();
  });

  it('should toggle organization status to INACTIVE after confirmation', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.toggleOrganizationStatus(mockOrg);

    expect(orgServiceSpy.updateOrganizationStatus).toHaveBeenCalledWith(1, 'INACTIVE');
  });

  it('should toggle an INACTIVE organization back to ACTIVE', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const inactiveOrg: OrganizationResponse = { ...mockOrg, status: 'INACTIVE' };

    component.toggleOrganizationStatus(inactiveOrg);

    expect(orgServiceSpy.updateOrganizationStatus).toHaveBeenCalledWith(1, 'ACTIVE');
  });

  it('should not call the service when the confirmation is declined', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.toggleOrganizationStatus(mockOrg);

    expect(orgServiceSpy.updateOrganizationStatus).not.toHaveBeenCalled();
  });

  it('should upload a logo and refresh the organization row', () => {
    const file = new File(['dummy'], 'logo.png', { type: 'image/png' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    const event = { target: input } as unknown as Event;

    component.onLogoSelected(event, mockOrg);

    expect(orgServiceSpy.uploadLogo).toHaveBeenCalledWith(1, file);
    expect(component.uploadingLogoId).toBeNull();
  });
});
