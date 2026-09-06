import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { vi, type Mock } from 'vitest';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
    let authServiceSpy: { currentUser: ReturnType<typeof signal> };
    let routerSpy: { navigate: Mock };

    beforeEach(() => {
        authServiceSpy = { currentUser: signal(null) };
        routerSpy = { navigate: vi.fn() };

        TestBed.configureTestingModule({
            providers: [
                { provide: AuthService, useValue: authServiceSpy },
                { provide: Router, useValue: routerSpy }
            ]
        });
    });

    function runGuard(routePath: string) {
        const route = { routeConfig: { path: routePath } } as any;
        return TestBed.runInInjectionContext(() => roleGuard(route, {} as any));
    }

    it('should allow access when route has no role restriction defined', () => {
        const result = runGuard('dashboard'); // absent de ROUTE_ROLES

        expect(result).toBe(true);
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should allow access when user role is in the allowed list', () => {
        authServiceSpy.currentUser.set({ id: 1, role: 'SUPER_ADMIN' } as any);

        const result = runGuard('organizations'); // ROUTE_ROLES: ['SUPER_ADMIN']

        expect(result).toBe(true);
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should deny access and redirect when user role is not in the allowed list', () => {
        authServiceSpy.currentUser.set({ id: 1, role: 'AGENT' } as any);

        const result = runGuard('organizations'); // ROUTE_ROLES: ['SUPER_ADMIN']

        expect(result).toBe(false);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should deny access and redirect when user is not connected', () => {
        authServiceSpy.currentUser.set(null);

        const result = runGuard('users'); // route restreinte

        expect(result).toBe(false);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
});
