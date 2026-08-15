import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CollecteService } from './collecte.service';
import { CollecteResponse } from '../models/collecte.model';

describe('CollecteService', () => {
  let service: CollecteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CollecteService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CollecteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('validateCollecte should send POST request', () => {
    const mockResponse: CollecteResponse = {
      id: 100,
      formVersionId: 1,
      agent: { id: 10, firstName: 'Jean', lastName: 'Dupont' },
      dataJson: '{}',
      status: 'VALIDATED',
      createdAt: '2026-08-13T12:00:00Z'
    };

    service.validateCollecte(100, 'RAS').subscribe(data => {
      expect(data.status).toBe('VALIDATED');
    });

    const req = httpMock.expectOne('http://localhost:8080/collectes/100/validate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ comment: 'RAS' });
    req.flush(mockResponse);
  });

  it('rejectCollecte should send POST request with comment', () => {
    const mockResponse: CollecteResponse = {
      id: 100,
      formVersionId: 1,
      agent: { id: 10, firstName: 'Jean', lastName: 'Dupont' },
      dataJson: '{}',
      status: 'REJECTED',
      validationComment: 'Photo absente',
      createdAt: '2026-08-13T12:00:00Z'
    };

    service.rejectCollecte(100, 'Photo absente').subscribe(data => {
      expect(data.status).toBe('REJECTED');
      expect(data.validationComment).toBe('Photo absente');
    });

    const req = httpMock.expectOne('http://localhost:8080/collectes/100/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ comment: 'Photo absente' });
    req.flush(mockResponse);
  });
});
