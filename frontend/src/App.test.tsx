import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { expect, test } from 'vitest';
import { App } from './App';
import { messages } from './messages';

test('opens the form from the root path', () => {
  render(
    <MemoryRouter initialEntries={['/']}>
      <App />
    </MemoryRouter>,
  );

  expect(screen.getByRole('heading', { name: messages.app.title })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: messages.closure.form.title })).toBeInTheDocument();
});
