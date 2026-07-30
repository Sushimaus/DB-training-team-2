import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { withAuth } from '../withAuth.jsx';
import { AuthContext } from '@context/AuthContext.jsx';

function MockProtectedComponent({ title }) {
  return <div>Protected Content: {title}</div>;
}

describe('withAuth HOC', () => {
  it('sets correct displayName on the wrapped component', () => {
    const Wrapped = withAuth(MockProtectedComponent);
    expect(Wrapped.displayName).toBe('withAuth(MockProtectedComponent)');
  });

  it('redirects to /login when user is not authenticated', () => {
    const Wrapped = withAuth(MockProtectedComponent);
    render(
      <AuthContext.Provider value={{ user: null, login: vi.fn(), logout: vi.fn() }}>
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route path="/dashboard" element={<Wrapped title="Test" />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.queryByText('Protected Content: Test')).toBeNull();
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('renders wrapped component with props when user is authenticated', () => {
    const Wrapped = withAuth(MockProtectedComponent);
    const mockUser = { token: 'fake-jwt', role: 'TRADER' };

    render(
      <AuthContext.Provider value={{ user: mockUser, login: vi.fn(), logout: vi.fn() }}>
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route path="/dashboard" element={<Wrapped title="Test Dashboard" />} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByText('Protected Content: Test Dashboard')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).toBeNull();
  });
});
