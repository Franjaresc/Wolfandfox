import { render, screen } from '@testing-library/react';
import { expect, test } from 'vitest';
import { App } from './App';
import { messages } from './messages';

test('shows the application title', () => {
  render(<App />);

  expect(screen.getByRole('heading', { name: messages.app.title })).toBeInTheDocument();
});
