import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { vi, type Mock } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
    let authServiceSpy: { isAuthenticated: Mock };
    let routerSpy: { createUrlTree: Mock };

    beforeEach(() => {
        authServiceSpy = { isAuthenticated: vi.fn() };
        routerSpy = { createUrlTree: vi.fn() };

        TestBed.configureTestingModule({
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                { provide: Router, useValue: routerSpy }
            ]
        });
    });

    function runGuard() {
        return TestBed.runInInjectionContext(() =>
            authGuard({} as any, {} as any)
        );
    }

    it('should allow access when user is authenticated', () => {
        authServiceSpy.isAuthenticated.mockReturnValue(true);

        const result = runGuard();

        expect(result).toBe(true);
        expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
    });

    it('should redirect to /login when user is not authenticated', () => {
        authServiceSpy.isAuthenticated.mockReturnValue(false);
        const fakeUrlTree = {} as UrlTree;
        routerSpy.createUrlTree.mockReturnValue(fakeUrlTree);

        const result = runGuard();

        expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
        expect(result).toBe(fakeUrlTree);
    });
});
