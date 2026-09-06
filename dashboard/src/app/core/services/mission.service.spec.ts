import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MissionService } from './mission.service';
import { MissionResponse, MissionResponsePage, MissionProgressResponse } from '../models/mission.model';
import { environment } from '../../../environments/environment';

describe('MissionService', () => {
    let service: MissionService;
    let httpMock: HttpTestingController;
    const apiUrl = `${environment.apiUrl}/missions`;

    const mockMission: MissionResponse = {
        id: 1,
        name: 'Mission Test',
        organizationId: 10,
        status: 'ACTIVE',
        createdBy: { id: 5, firstName: 'Jean', lastName: 'Dupont' },
        forms: [],
        agents: [],
        createdAt: '2026-08-13T12:00:00Z'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                MissionService,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });

        service = TestBed.inject(MissionService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('getMissions should send GET request to base URL', () => {
        service.getMissions().subscribe(data => {
            expect(data).toEqual([mockMission]);
        });

        const req = httpMock.expectOne(apiUrl);
        expect(req.request.method).toBe('GET');
        req.flush([mockMission]);
    });

    it('getMission should send GET request with id', () => {
        service.getMission(1).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1`);
        expect(req.request.method).toBe('GET');
        req.flush(mockMission);
    });

    it('createMission should send POST request with body', () => {
        const request = { name: 'Nouvelle mission' };

        service.createMission(request).subscribe(data => {
            expect(data.name).toBe('Mission Test');
        });

        const req = httpMock.expectOne(apiUrl);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(request);
        req.flush(mockMission);
    });

    it('updateMission should send PUT request with id and body', () => {
        const request = { name: 'Mission modifiée' };

        service.updateMission(1, request).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1`);
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(request);
        req.flush(mockMission);
    });

    it('cancelMission should send PATCH request to cancel endpoint', () => {
        service.cancelMission(1).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1/cancel`);
        expect(req.request.method).toBe('PATCH');
        expect(req.request.body).toEqual({});
        req.flush(mockMission);
    });

    it('assignAgents should send POST request with agentIds', () => {
        const request = { agentIds: [1, 2, 3] };

        service.assignAgents(1, request).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1/agents`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(request);
        req.flush(mockMission);
    });

    it('unassignAgent should send DELETE request with mission and agent id', () => {
        service.unassignAgent(1, 2).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1/agents/2`);
        expect(req.request.method).toBe('DELETE');
        req.flush(mockMission);
    });

    it('assignForms should send POST request with formIds', () => {
        const request = { formIds: [1, 2] };

        service.assignForms(1, request).subscribe(data => {
            expect(data.id).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1/forms`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(request);
        req.flush(mockMission);
    });

    it('getMissionsProgress should send GET request to progress endpoint', () => {
        const mockProgress: MissionProgressResponse[] = [{
            missionId: 1,
            missionName: 'Mission Test',
            receivedCollectesCount: 5,
            activeAgentsCount: 2,
            assignedAgentsCount: 3
        }];

        service.getMissionsProgress().subscribe(data => {
            expect(data).toEqual(mockProgress);
        });

        const req = httpMock.expectOne(`${apiUrl}/progress`);
        expect(req.request.method).toBe('GET');
        req.flush(mockProgress);
    });

    it('getMissionProgress should send GET request with id to progress endpoint', () => {
        const mockProgress: MissionProgressResponse = {
            missionId: 1,
            missionName: 'Mission Test',
            receivedCollectesCount: 5,
            activeAgentsCount: 2,
            assignedAgentsCount: 3
        };

        service.getMissionProgress(1).subscribe(data => {
            expect(data.missionId).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/1/progress`);
        expect(req.request.method).toBe('GET');
        req.flush(mockProgress);
    });

    it('getMissionsPaged should send GET request with page and size params', () => {
        const mockPage: MissionResponsePage = {
            content: [mockMission],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 25,
            first: true,
            last: true
        };

        service.getMissionsPaged(0, 25).subscribe(data => {
            expect(data.content.length).toBe(1);
        });

        const req = httpMock.expectOne(`${apiUrl}/list?page=0&size=25`);
        expect(req.request.method).toBe('GET');
        req.flush(mockPage);
    });

    it('getMissionsPaged should use default page and size when not provided', () => {
        service.getMissionsPaged().subscribe();

        const req = httpMock.expectOne(`${apiUrl}/list?page=0&size=25`);
        expect(req.request.method).toBe('GET');
        req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 25, first: true, last: true });
    });
});